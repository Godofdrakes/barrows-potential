package com.barrowspotential;

import lombok.NonNull;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BarrowsPotentialOverlay extends Overlay
{
	private static final int REWARD_POTENTIAL_MAX = 1012;

	// treemap makes display order deterministic
	private final Map<Monster, Integer> plan = new TreeMap<>();

	private final PanelComponent panelComponent = new PanelComponent();
	private final ProgressBarComponent progressBarComponent = new ProgressBarComponent();

	private final AtomicBoolean isVisible = new AtomicBoolean( false );
	private final AtomicBoolean isInCrypt = new AtomicBoolean( false );

	private final OverlayManager overlayManager;

	@Inject
	public BarrowsPotentialOverlay( @NonNull final OverlayManager overlayManager )
	{
		progressBarComponent.setMinimum( 0 );
		progressBarComponent.setForegroundColor( Color.blue );
		progressBarComponent.setLabelDisplayMode( ProgressBarComponent.LabelDisplayMode.TEXT_ONLY );
		setPosition( OverlayPosition.TOP_LEFT );
		setPriority( 1.f );

		this.overlayManager = overlayManager;
	}

	public BarrowsPotentialOverlay connect()
	{
		this.overlayManager.add( this );

		return this;
	}

	public BarrowsPotentialOverlay dispose()
	{
		this.overlayManager.remove( this );

		return this;
	}

	@Override
	public Dimension render( Graphics2D graphics )
	{
		if ( !isVisible.get() || !isInCrypt.get() || plan.isEmpty() )
		{
			return null;
		}

		panelComponent.getChildren().clear();

		String panelTitle = "Optimal Barrows";

		panelComponent.getChildren().add( TitleComponent.builder()
			.text( panelTitle )
			.color( Color.white )
			.build() );

		panelComponent.setPreferredSize( new Dimension(
			150,
			0
		) );

		panelComponent.getChildren().add( progressBarComponent );

		for ( Map.Entry<Monster, Integer> entry : plan.entrySet() )
		{
			// the default barrows plugin already tracks brothers
			if ( entry.getKey().isBrother() )
				continue;

			panelComponent.getChildren().add( LineComponent.builder()
				.left( String.format( "x%d:", entry.getValue() ) )
				.right( entry.getKey().getDisplayName() )
				.build() );
		}

		return panelComponent.render( graphics );
	}

	public BarrowsPotentialOverlay clear()
	{
		plan.clear();

		return this;
	}

	public BarrowsPotentialOverlay setOptimalMonsters( Map<Monster, Integer> monsters )
	{
		plan.clear();
		plan.putAll( monsters );

		return this;
	}

	public BarrowsPotentialOverlay setRewardDisplay( int rewardPotential, RewardTarget rewardTarget )
	{
		progressBarComponent.setMaximum( Math.min( REWARD_POTENTIAL_MAX, rewardTarget.getMaxValue() ) );
		progressBarComponent.setValue( rewardPotential );
		progressBarComponent.setCenterLabel( Integer.toString( rewardPotential ) );

		if ( rewardPotential > rewardTarget.getMaxValue() )
		{
			progressBarComponent.setFontColor( Color.red );
		}
		else if ( rewardPotential >= rewardTarget.getMinValue() )
		{
			progressBarComponent.setFontColor( Color.white );
		}
		else
		{
			progressBarComponent.setFontColor( Color.yellow );
		}

		return this;
	}

	public BarrowsPotentialOverlay setVisibility( final boolean value )
	{
		isVisible.set( value );

		return this;
	}

	public BarrowsPotentialOverlay setIsInCrypt( final boolean value )
	{
		isInCrypt.set( value );

		return this;
	}
}
