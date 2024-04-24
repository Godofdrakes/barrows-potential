package com.barrowspotential;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import java.util.*;
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
	private ChatMessageManager chatMessageManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BarrowsPotentialConfig config;

	@Inject
	private BarrowsPotentialHighlight npcOverlay;

	@Inject
	private BarrowsPotentialOverlay screenOverlay;

	private final AtomicBoolean updatePending = new AtomicBoolean( false );

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

	private RewardPlan getBasePlan()
	{
		val brothers = new ArrayList<Monster>();

		for ( val brother : Monster.brothers )
		{
			if ( isBrotherDefeated( brother ) )
			{
				brothers.add( brother );
			}
		}

		return RewardPlan.create( brothers, getRewardPotential() );
	}

	private void updatePlan()
	{
		assert ( client.isClientThread() );

		npcOverlay.clear();
		screenOverlay.clear();

		val rewardPotential = getRewardPotential();

		val targetReward = config.rewardTarget();
		val targetMonsters = config.rewardPlan();

		val targetPotentialClamped = Math.min( REWARD_POTENTIAL_MAX, targetReward.getMaxValue() );
		val targetPotentialMet = rewardPotential >= targetPotentialClamped;

		log.warn( "reward potential {}", rewardPotential );
		log.warn( "target potential {}", targetPotentialClamped );

		if ( !targetPotentialMet )
		{
			for ( val monster : targetMonsters )
			{
				if ( monster.isBrother() )
				{
					continue;
				}

				val expectedRewardPotential = rewardPotential + monster.getCombatLevel();

				if ( expectedRewardPotential > targetReward.getMaxValue() )
				{
					// Don't highlight monsters that would put us over the target
					continue;
				}

				npcOverlay.add( monster );
			}

			val planner = new RewardPlanner();

			// Find a plan that gets us from our current reward potential to the target
			// This plan must only include the monsters/brothers we have selected

			planner.reset( getBasePlan(), targetPotentialClamped );
			planner.setTargetMonsters( config.rewardPlan() );

			val plan = planner.search( PLANNER_ITERATIONS_MAX );

			if ( plan == null )
			{
				// In theory this will never happen
				// At the very least the base plan we fed in will be returned
				log.error( "plan was null" );
			}
			else
			{
				val planValue = plan.getRewardPotential();

				log.warn( "planned reward potential: {}", planValue );

				if ( planValue < targetReward.getMinValue() )
				{
					// In theory this will never happen
					log.warn( "plan does not meet target" );
				}

				for ( val entry : plan.getMonsters().entrySet() )
				{
					val count = entry.getValue();
					val name = entry.getKey().getDisplayName();
					val value = entry.getKey().getCombatLevel() * count;
					log.debug( "x{} {} ({})", count, name, value );

					npcOverlay.addOptimal( entry.getKey() );
				}

				screenOverlay.setOptimalMonsters( plan.getMonsters() );
			}
		}

		npcOverlay.rebuild();

		screenOverlay.setRewardDisplay( rewardPotential, config.rewardTarget() );

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
