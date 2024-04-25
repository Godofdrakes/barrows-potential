package com.barrowspotential;

import lombok.NonNull;
import lombok.val;
import net.runelite.api.NPC;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BarrowsPotentialHighlight
{
	// Brothers are already highlighted by the game/built-in plugin. We don't need to add to that.
	private static final Set<Monster> ignoredNpcs = Monster.brothers;

	private final Set<Integer> npcHighlightsOptimal = ConcurrentHashMap.newKeySet();
	private final Set<Integer> npcHighlights = ConcurrentHashMap.newKeySet();
	private final NpcOverlayService overlayService;

	// https://stackoverflow.com/questions/3964211/when-to-use-atomicreference-in-java
	// Supposedly reference assignment is already atomic so use of AtomicReference shouldn't be necessary.
	// Use of volatile keyword ensures compiler does not make read/write optimizations.

	private volatile Color highlightColorOptimal = null;
	private volatile Color highlightColor = null;

	@Inject
	public BarrowsPotentialHighlight( @NonNull NpcOverlayService overlayService )
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
		overlayService.unregisterHighlighter( this::getNpcHighlight );

		return this;
	}

	private Color getNpcHighlightColor( @NonNull NPC npc )
	{
		// get-check-return pattern to avoid race conditions

		if ( npcHighlightsOptimal.contains( npc.getId() ) )
		{
			val color = this.highlightColorOptimal;

			if ( color != null )
			{
				return color;
			}
		}

		if ( npcHighlights.contains( npc.getId() ) )
		{
			val color = this.highlightColor;

			if ( color != null )
			{
				return color;
			}
		}

		return null;
	}

	private HighlightedNpc getNpcHighlight( @NonNull NPC npc )
	{
		val color = getNpcHighlightColor( npc );

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

	public BarrowsPotentialHighlight setHighlightColor( @Nullable Color color )
	{
		highlightColor = color;

		return this;
	}

	public BarrowsPotentialHighlight setHighlightOptimalColor( @Nullable Color color )
	{
		highlightColorOptimal = color;

		return this;
	}

	public BarrowsPotentialHighlight add( @NonNull Monster monster )
	{
		if ( !ignoredNpcs.contains( monster ) )
		{
			for ( val npcId : monster.getNpcIDs() )
			{
				npcHighlights.add( npcId );
			}
		}

		return this;
	}

	public BarrowsPotentialHighlight addOptimal( @NonNull Monster monster )
	{
		if ( !ignoredNpcs.contains( monster ) )
		{
			for ( val npcId : monster.getNpcIDs() )
			{
				npcHighlightsOptimal.add( npcId );
			}
		}

		return this;
	}

	public BarrowsPotentialHighlight addAll( @NonNull Iterable<Monster> monsters )
	{
		for ( val monster : monsters )
		{
			add( monster );
		}

		return this;
	}

	public BarrowsPotentialHighlight addAllOptimal( @NonNull Iterable<Monster> monsters )
	{
		for ( val monster : monsters )
		{
			addOptimal( monster );
		}

		return this;
	}

	public BarrowsPotentialHighlight clear()
	{
		npcHighlightsOptimal.clear();
		npcHighlights.clear();

		return this;
	}

	public BarrowsPotentialHighlight rebuild()
	{
		overlayService.rebuild();

		return this;
	}
}
