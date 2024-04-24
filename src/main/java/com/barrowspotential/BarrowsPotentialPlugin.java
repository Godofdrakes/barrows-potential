package com.barrowspotential;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@PluginDescriptor(
	name = "Barrows Potential",
	description = "Highlights optimal NPCs in the Barrows Crypts",
	tags = {"barrows", "overlay", "pve", "pvm"}
)
public class BarrowsPotentialPlugin extends Plugin
{
	private static final int CRYPT_REGION_ID = 14231;

	private static final int PLANNER_ITERATIONS_MAX = 20;

	private static final int REWARD_POTENTIAL_MAX = 1012;

	private static final int PLUGIN_VERSION = 4;
	private static final int PLUGIN_VERSION_RELEASE = 1;

	@Inject
	private Client client;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BarrowsPotentialConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BarrowsPotentialHighlight npcOverlay;

	@Inject
	private BarrowsPotentialOverlay screenOverlay;

	private final AtomicBoolean updatePending = new AtomicBoolean( false );

	private final RewardPlanner planner = new RewardPlanner();

	private void queueUpdatePlan()
	{
		if ( !updatePending.getAndSet( true ) )
		{
			clientThread.invokeLater( this::updatePlan );
		}
	}

	@Override
	protected void startUp()
	{
		npcOverlay.connect()
			.setHighlightColor( config.highlightNpc() ? config.highlightColor() : null )
			.setHighlightOptimalColor( config.highlightOptimal() ? config.optimalColor() : null );

		screenOverlay.connect()
			.setIsInCrypt( isInCrypt() )
			.setVisibility( config.overlayOptimal() );

		queueUpdatePlan();
	}

	@Override
	protected void shutDown()
	{
		npcOverlay.dispose();
		screenOverlay.dispose();
	}

	@Subscribe
	public void onVarbitChanged( VarbitChanged event )
	{
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
			queueUpdatePlan();
		}
	}

	@Subscribe
	public void onGameStateChanged( GameStateChanged event )
	{
		// runs when loading into the crypt, check region id
		if ( event.getGameState() == GameState.LOGGED_IN )
		{
			log.debug( "logged in" );

			updateCheck();

			screenOverlay.setIsInCrypt( isInCrypt() );

			queueUpdatePlan();
		}
		else
		{
			screenOverlay.setIsInCrypt( false );
		}
	}

	@Subscribe
	public void onConfigChanged( ConfigChanged event )
	{
		if ( event.getGroup().equals( "barrowspotential" ) )
		{
			log.debug( "config changed" );

			npcOverlay
				.rebuild()
				.setHighlightColor( config.highlightNpc() ? config.highlightColor() : null )
				.setHighlightOptimalColor( config.highlightOptimal() ? config.optimalColor() : null );

			screenOverlay
				.setVisibility( config.overlayOptimal() );

			clientThread.invokeLater( this::updatePlan );
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

		final Map<Monster,Integer> monsters = new HashMap<>();

		// add all brothers that we haven't yet defeated to the plan
		for ( final Monster brother : Monster.brothers )
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
		final Set<Monster> monsters = config.rewardPlan();

		monsters.retainAll( Monster.cryptMonsters );

		return monsters;
	}

	private void updatePlan()
	{
		assert ( client.isClientThread() );

		npcOverlay.clear();
		screenOverlay.clear();

		planner.monstersToTarget = getMonstersToTarget();

		final RewardTarget rewardTarget = config.rewardTarget();

		log.debug( "reward target {}", rewardTarget.getDisplayName() );

		final int rewardPotential = getRewardPotential();
		final int requiredPotential = rewardTarget.getMinValue();
		final int targetPotential = rewardTarget.getMaxValue();
		final int targetPotentialClamped = Math.min( REWARD_POTENTIAL_MAX, targetPotential );

		log.debug( "reward potential {}", rewardPotential );

		// Starting from the base plan ensures we don't go fill up on score
		// Before the player has killed all their target brothers
		final RewardPlan basePlan = getBasePlan();

		final int basePotential = basePlan.GetRewardPotential();

		final boolean rewardPotentialMet = rewardPotential + basePotential >= targetPotentialClamped;
		if ( !rewardPotentialMet )
		{
			// Highlight all monsters that don't exceed the goal

			for ( final Monster monster : planner.monstersToTarget )
			{
				if ( rewardPotential + basePotential + monster.getCombatLevel() > targetPotential )
					continue;

				npcOverlay.add( monster );
			}

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

				npcOverlay.addAllOptimal( plan.monsters.keySet() );

				screenOverlay
					.setRewardDisplay( rewardPotential, config.rewardTarget() )
					.setOptimalMonsters( plan.monsters );

				for ( final Map.Entry<Monster,Integer> entry : plan.monsters.entrySet() )
				{
					int count = entry.getValue();
					String name = entry.getKey().getDisplayName();
					int value = entry.getKey().getCombatLevel() * count;
					log.debug( "x{} {} ({})", count, name, value );
				}
			}
		}

		npcOverlay.rebuild();

		updatePending.set( false );
	}

	public int getRegionID()
	{
		final Player localPlayer = client.getLocalPlayer();

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

	// Check if the plugin has updated since the last time the user logged in
	// queue a message notifying of changes if so
	private void updateCheck()
	{
		Integer version = configManager.getRSProfileConfiguration(
			BarrowsPotentialConfig.CONFIG_GROUP,
			BarrowsPotentialConfig.CONFIG_VERSION,
			int.class );

		if ( version == null )
		{
			log.debug( "last plugin version unknown. assuming release." );

			version = PLUGIN_VERSION_RELEASE;
		}

		log.debug( "last plugin version: {}", version );
		log.debug( "latest plugin version: {}", PLUGIN_VERSION );

		if ( version >= PLUGIN_VERSION )
		{
			return;
		}

		log.debug( "plugin version changed. queuing update message." );

		final String message = "Barrows Potential has been updated. See Github page for details.";

		final String chatMessage = new ChatMessageBuilder()
			.append( ChatColorType.HIGHLIGHT )
			.append( message )
			.build();

		chatMessageManager.queue( QueuedMessage.builder()
			.type( ChatMessageType.CONSOLE )
			.runeLiteFormattedMessage( chatMessage )
			.build() );

		configManager.setRSProfileConfiguration(
			BarrowsPotentialConfig.CONFIG_GROUP,
			BarrowsPotentialConfig.CONFIG_VERSION,
			PLUGIN_VERSION );
	}

	@Provides
	BarrowsPotentialConfig provideConfig( ConfigManager configManager )
	{
		return configManager.getConfig( BarrowsPotentialConfig.class );
	}
}
