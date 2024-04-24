package com.barrowspotential;

import lombok.NonNull;
import lombok.val;
import net.runelite.api.NPC;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

import javax.inject.Inject;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class BarrowsPotentialHighlight
{
	private final Set<Integer> npcHighlights = ConcurrentHashMap.newKeySet();
	private final Set<Integer> npcHighlightsOptimal = ConcurrentHashMap.newKeySet();

	private final AtomicReference<Color> highlightColor = new AtomicReference<>( null );
	private final AtomicReference<Color> highlightColorOptimal = new AtomicReference<>( null );

	private final NpcOverlayService overlayService;

	@Inject
	public BarrowsPotentialHighlight( @NonNull final NpcOverlayService overlayService )
	{
		this.overlayService = overlayService;
	}

	public BarrowsPotentialHighlight connect()
	{
		overlayService.registerHighlighter( this::getNpcHighlight );

		return this;
	}

	public BarrowsPotentialHighlight dispose()
	{
		overlayService.registerHighlighter( this::getNpcHighlight );

		return this;
	}

	private Color getNpcHighlightColor( final NPC npc )
	{
		if ( npcHighlightsOptimal.contains( npc.getId() ) )
		{
			return highlightColorOptimal.get();
		}

		if ( npcHighlights.contains( npc.getId() ) )
		{
			return highlightColor.get();
		}

		return null;
	}

	private HighlightedNpc getNpcHighlight( NPC npc )
	{
		final Color color = getNpcHighlightColor( npc );

		if ( color != null )
		{
			return HighlightedNpc.builder()
				.hull( true )
				.highlightColor( color )
				.npc( npc )
				.build();
		}

		return null;
	}

	public BarrowsPotentialHighlight setHighlightColor( final Color color )
	{
		highlightColor.set( color );

		return this;
	}

	public BarrowsPotentialHighlight setHighlightOptimalColor( final Color color )
	{
		highlightColorOptimal.set( color );

		return this;
	}

	public BarrowsPotentialHighlight add( final Monster monster )
	{
		for ( val npcId : monster.getNpcIDs() )
		{
			npcHighlights.add( npcId );
		}

		return this;
	}

	public BarrowsPotentialHighlight addOptimal( final Monster monster )
	{
		for ( val npcId : monster.getNpcIDs() )
		{
			npcHighlightsOptimal.add( npcId );
		}

		return this;
	}

	public BarrowsPotentialHighlight addAll( final Iterable<Monster> monsters )
	{
		for ( val monster : monsters )
		{
			add( monster );
		}

		return this;
	}

	public BarrowsPotentialHighlight addAllOptimal( final Iterable<Monster> monsters )
	{
		for ( val monster : monsters )
		{
			addOptimal( monster );
		}

		return this;
	}

	public BarrowsPotentialHighlight clear()
	{
		npcHighlights.clear();
		npcHighlightsOptimal.clear();

		return this;
	}

	public BarrowsPotentialHighlight rebuild()
	{
		overlayService.rebuild();

		return this;
	}
}
