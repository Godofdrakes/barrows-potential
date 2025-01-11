package com.barrowspotential;

import com.google.common.collect.ImmutableMap;
import lombok.NonNull;
import lombok.val;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.annotation.Nonnegative;
import javax.inject.Inject;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BarrowsPotentialOverlay extends Overlay
{
	// Brothers are already tracked by the game/built-in plugin. We don't need to add to that.
	private static final Set<Monster> ignoredNpcs = Monster.brothers;

	private static final int REWARD_POTENTIAL_MAX = 1012;

	private final PanelComponent panelComponent = new PanelComponent();

	private final TitleComponent titleComponent = TitleComponent.builder()
			.text( "Barrows Potential" )
			.color( Color.white )
			.build();

	private final AtomicBoolean isVisible = new AtomicBoolean( false );
	private final AtomicBoolean isInCrypt = new AtomicBoolean( false );
	private final OverlayManager overlayManager;

	// https://stackoverflow.com/questions/3964211/when-to-use-atomicreference-in-java
	// Supposedly reference assignment is already atomic so use of AtomicReference shouldn't be necessary.
	// Use of volatile keyword ensures compiler does not make read/write optimizations.

	private volatile ProgressBarComponent progressBarComponent = new ProgressBarComponent();
	private volatile Map<Monster, Integer> plan = ImmutableMap.of();

	@Inject
	public BarrowsPotentialOverlay( @NonNull OverlayManager overlayManager )
	{
		this.setPriority( 1.f );
		this.setPosition( OverlayPosition.TOP_LEFT );
		this.panelComponent.setPreferredSize( new Dimension( 150, 0 ) );
		this.overlayManager = overlayManager;
	}

	public BarrowsPotentialOverlay connect()
	{
		overlayManager.add( this );

		return this;
	}

	public BarrowsPotentialOverlay dispose()
	{
		overlayManager.remove( this );

		return this;
	}

	public BarrowsPotentialOverlay clear()
	{
		plan = ImmutableMap.of();

		return this;
	}

	public BarrowsPotentialOverlay setOptimalMonsters( @NonNull Map<Monster, Integer> monsters )
	{
		plan = ImmutableMap.copyOf( monsters );

		return this;
	}

	public BarrowsPotentialOverlay setRewardDisplay(
			@Nonnegative int rewardPotential,
			@NonNull RewardTarget rewardTarget )
	{
		val component = new ProgressBarComponent();

		component.setMinimum( 0 );
		component.setMaximum( Math.min( REWARD_POTENTIAL_MAX, rewardTarget.getMaxValue() ) );
		component.setValue( rewardPotential );
		component.setForegroundColor( Color.blue );

		component.setLabelDisplayMode( ProgressBarComponent.LabelDisplayMode.TEXT_ONLY );
		component.setCenterLabel( Integer.toString( rewardPotential ) );

		if ( rewardPotential > rewardTarget.getMaxValue() )
		{
			component.setFontColor( Color.red );
		}
		else if ( rewardPotential >= rewardTarget.getMinValue() )
		{
			component.setFontColor( Color.white );
		}
		else
		{
			component.setFontColor( Color.yellow );
		}

		progressBarComponent = component;

		return this;
	}

	public BarrowsPotentialOverlay setVisibility( boolean value )
	{
		isVisible.set( value );

		return this;
	}

	public BarrowsPotentialOverlay setIsInCrypt( boolean value )
	{
		isInCrypt.set( value );

		return this;
	}

	@Override
	public Dimension render( Graphics2D graphics )
	{
		if ( !isVisible.get() || !isInCrypt.get() )
		{
			return null;
		}

		val currentPlan = plan;

		if ( currentPlan.isEmpty() )
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add( titleComponent );
		panelComponent.getChildren().add( progressBarComponent );

		for ( final Map.Entry<Monster, Integer> entry : currentPlan.entrySet() )
		{
			if ( ignoredNpcs.contains( entry.getKey() ) )
			{
				continue;
			}

			panelComponent.getChildren().add( LineComponent.builder()
					.left( String.format( "x%d:", entry.getValue() ) )
					.right( entry.getKey().getDisplayName() )
					.build() );
		}

		return panelComponent.render( graphics );
	}
}