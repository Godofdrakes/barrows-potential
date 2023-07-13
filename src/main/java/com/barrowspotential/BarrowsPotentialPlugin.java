package com.barrowspotential;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.npcoverlay.*;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "Barrows Potential",
	description = "Highlights optimal NPCs in the Barrows Crypts",
	tags = { "barrows", "overlay", "pve", "pvm" }
)
public class BarrowsPotentialPlugin extends Plugin
{
	private static final int CRYPT_REGION_ID = 14231;

	private static final int PLANNER_ITERATIONS_MAX = 20;

	private static final int REWARD_POTENTIAL_MAX = 1012;

	@Inject
	private Client client;

	@Inject
	private BarrowsPotentialConfig config;

	@Inject
	private BarrowsPotentialOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private NpcOverlayService npcOverlayService;

	@Inject
	private ClientThread clientThread;

	private boolean updateQueued = false;

	private final RewardPlanner planner = new RewardPlanner();

	private final HashSet<Monster> npcTargets = new HashSet<>();

	private final Map<Monster,Integer> optimalNpcTargets = new HashMap<>();

	private final HighlightedNpc.HighlightedNpcBuilder npcBuilder = HighlightedNpc.builder()
		.hull( true );

	private HighlightedNpc getHighlightForNpc( NPC npc )
	{
		Monster monster = Monster.cryptMonsterByNpcID.get( npc.getId() );
		if ( monster == null )
		{
			// This isn't a monster we would ever care about
			assert ( !Monster.cryptMonsterByNpcID.containsKey( npc.getId() ) );
			return null;
		}

		if ( optimalNpcTargets.containsKey( monster ) )
		{
			return npcBuilder
				.highlightColor( config.optimalColor() )
				.npc( npc )
				.build();
		}

		if ( npcTargets.contains( monster ) )
		{
			return npcBuilder
				.highlightColor( config.highlightColor() )
				.npc( npc )
				.build();
		}

		return null;
	}

	@Override
	protected void startUp()
	{
		npcOverlayService.registerHighlighter( this::getHighlightForNpc );
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove( overlay );
		npcOverlayService.unregisterHighlighter( this::getHighlightForNpc );
	}

	@Subscribe
	public void onVarbitChanged( VarbitChanged event )
	{
		// @todo: how to prevent this from running twice when you kill a brother?
		// BARROWS_REWARD_POTENTIAL runs and then the brother's varbit runs

		boolean wantsUpdate = event.getVarbitId() == Varbits.BARROWS_REWARD_POTENTIAL;

		if ( event.getVarbitId() == Varbits.BARROWS_REWARD_POTENTIAL )
		{
			log.debug( "reward potential changed" );
			wantsUpdate = true;
		}
		else
		{
			Monster monster = Monster.brothersByVarbit.get( event.getVarbitId() );
			if ( monster != null )
			{
				log.debug( "killed {}", monster.getDisplayName() );
				wantsUpdate = true;
			}
		}

		if ( wantsUpdate )
		{
			queueUpdate();
		}
	}

	@Subscribe
	public void onGameStateChanged( GameStateChanged event )
	{
		// runs when loading into the crypt, check region id
		if ( event.getGameState() == GameState.LOGGED_IN )
		{
			if ( isInCrypt() )
			{
				log.debug( "entered crypt" );
				queueUpdate();
				return;
			}

			overlayManager.remove( overlay );
		}
	}

	@Subscribe
	public void onConfigChanged( ConfigChanged event )
	{
		if ( event.getGroup().equals( "barrowspotential" ) )
		{
			log.debug( "config changed" );

			queueUpdate();
		}
	}

	private boolean isBrotherDefeated( Monster target )
	{
		assert target.isBrother();
		return client.getVarbitValue( target.getVarbit() ) == 1;
	}

	// get the user's base plan, which we will add crypt monsters to
	private RewardPlan getBasePlan()
	{
		final Set<Monster> configPlan = config.rewardPlan();

		Map<Monster,Integer> monsters = new HashMap<>();

		// add all brothers that we haven't yet defeated to the plan
		for ( Monster brother : Monster.brothers )
		{
			if ( !configPlan.contains( brother ) )
				continue;

			if ( isBrotherDefeated( brother ) )
				continue;

			monsters.put( brother, 1 );
		}

		return new RewardPlan( monsters );
	}

	// get the set of monsters the user wants to plan around
	private Set<Monster> getMonstersToTarget()
	{
		Set<Monster> monsters = new HashSet<>( config.rewardPlan() );

		monsters.retainAll( Monster.cryptMonsters );

		return monsters;
	}

	// queue an update to the plan if one hasn't been queued already
	private void queueUpdate()
	{
		if ( !updateQueued )
			clientThread.invokeLater( this::updatePlan );

		updateQueued = true;
	}

	private void updatePlan()
	{
		assert ( client.isClientThread() );

		updateQueued = false;

		npcTargets.clear();
		optimalNpcTargets.clear();
		overlayManager.remove( overlay );

		planner.monstersToTarget = getMonstersToTarget();

		RewardTarget rewardTarget = config.rewardTarget();

		log.debug( "reward target {}", rewardTarget.getDisplayName() );

		int rewardPotential = getRewardPotential();
		int requiredPotential = rewardTarget.getMinValue();
		int targetPotential = rewardTarget.getMaxValue();
		int targetPotentialClamped = Math.min( REWARD_POTENTIAL_MAX, targetPotential );

		log.debug( "reward potential {}", rewardPotential );

		// Starting from the base plan ensures we don't go fill up on score
		// Before the player has killed all their target brothers
		RewardPlan basePlan = getBasePlan();
		int basePotential = basePlan.GetRewardPotential();

		boolean rewardPotentialMet = rewardPotential + basePotential >= targetPotentialClamped;
		if ( rewardPotentialMet )
		{
			npcOverlayService.rebuild();
			return;
		}

		// Highlight all monsters that don't exceed the goal
		if ( config.highlightNpc() )
		{
			for ( Monster monster : planner.monstersToTarget )
			{
				if ( rewardPotential + basePotential + monster.getCombatLevel() > targetPotential )
					continue;

				npcTargets.add( monster );
			}
		}

		// Highlight the optimal monsters to reach the goal
		if ( config.highlightOptimal() || config.overlayOptimal() )
		{
			log.debug( "planner mode {}", planner.mode );

			if ( targetPotential < Integer.MAX_VALUE )
			{
				// try to maximize rewards without going over
				planner.mode = RewardPlanner.Mode.NEAREST;

				// instead of tracking past kills just make a plan on the fly
				// if the player follows the plan it will progress naturally
				// if the player breaks from the plan it will adapt and make a new one
				planner.reset( basePlan, targetPotential - rewardPotential );
			}
			else
			{
				// keys and helm don't scale with reward score
				// take any plan that gets us what we want
				planner.mode = RewardPlanner.Mode.ANY;
				planner.reset( basePlan, requiredPotential - rewardPotential );
			}

			RewardPlan plan = null;

			int iteration = 0;

			// There's generally not a reason to let the planner run for a bunch of iterations
			// Even with a single brother targeted the optimal plan is ~11 steps
			// We generally know within ~20 iterations if a perfect plan (exactly the target score) is possible
			for ( ; iteration < PLANNER_ITERATIONS_MAX; ++iteration )
			{
				plan = planner.search();
				if ( plan != null )
					break;
			}

			log.debug( "planner ran for {} iterations", iteration );

			if ( plan == null )
			{
				// We failed to get a plan that _exactly_ matches the target within the iteration limit
				// Just take the best plan the planner could come up with
				log.debug( "taking partial plan" );
				plan = planner.takeBest();
			}

			if ( plan == null )
			{
				// In theory this will never happen
				// At the very least the base plan we fed in will be returned
				log.error( "plan was null" );
			}
			else
			{
				int planValue = plan.GetRewardPotential() + rewardPotential;

				if ( planValue < requiredPotential )
				{
					// In theory this will never happen
					log.warn( "plan does not meet target" );
				}

				log.debug( "planned reward potential: {}", planValue );

				if ( config.highlightOptimal() )
				{
					optimalNpcTargets.putAll( plan.monsters );
				}

				if ( config.overlayOptimal() && !plan.equals( basePlan ) )
				{
					overlay.setOptimalMonsters( plan.monsters );
					overlay.setRewardDisplay( rewardPotential, config.rewardTarget() );
					overlayManager.add( overlay );
				}

				for ( Map.Entry<Monster,Integer> entry : plan.monsters.entrySet() )
				{
					int count = entry.getValue();
					String name = entry.getKey().getDisplayName();
					int value = entry.getKey().getCombatLevel() * count;
					log.debug( "x{} {} ({})", count, name, value );
				}
			}
		}

		npcOverlayService.rebuild();
	}

	public int getRegionID()
	{
		Player localPlayer = client.getLocalPlayer();
		if ( localPlayer == null )
		{
			return 0;
		}

		return localPlayer.getWorldLocation().getRegionID();
	}

	public boolean isInCrypt()
	{
		return getRegionID() == CRYPT_REGION_ID;
	}

	public int getRewardPotential()
	{
		int value = client.getVarbitValue( Varbits.BARROWS_REWARD_POTENTIAL );

		for ( Monster brother : Monster.brothers )
		{
			// each brother adds 2 to the final reward potential
			if ( isBrotherDefeated( brother ) )
			{
				value += 2;
			}
		}

		return value;
	}

	@Provides
	BarrowsPotentialConfig provideConfig( ConfigManager configManager )
	{
		return configManager.getConfig( BarrowsPotentialConfig.class );
	}
}
