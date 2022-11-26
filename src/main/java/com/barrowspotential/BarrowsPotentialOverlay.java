package com.barrowspotential;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.Map;
import java.util.TreeMap;

public class BarrowsPotentialOverlay extends Overlay
{
    private static final int REWARD_POTENTIAL_MAX = 1012;

    // treemap makes display order deterministic
    private final Map<Monster,Integer> optimalMonsters = new TreeMap<>();

    private final PanelComponent panelComponent = new PanelComponent();
    private final ProgressBarComponent progressBarComponent = new ProgressBarComponent();

    public BarrowsPotentialOverlay()
    {
        progressBarComponent.setMinimum( 0 );
        progressBarComponent.setForegroundColor( Color.blue );
        progressBarComponent.setLabelDisplayMode( ProgressBarComponent.LabelDisplayMode.TEXT_ONLY );
        setPosition( OverlayPosition.TOP_LEFT );
        setPriority( OverlayPriority.HIGHEST );
    }

    @Override
    public Dimension render( Graphics2D graphics )
    {
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

        for ( Map.Entry<Monster,Integer> entry : optimalMonsters.entrySet() )
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

    public final void setOptimalMonsters( Map<Monster,Integer> monsters )
    {
        optimalMonsters.clear();
        optimalMonsters.putAll( monsters );
    }

    public final void setRewardDisplay( int rewardPotential, RewardTarget rewardTarget )
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
    }
}
