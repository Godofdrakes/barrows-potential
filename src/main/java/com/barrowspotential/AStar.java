package com.barrowspotential;

import lombok.val;

import javax.annotation.Nonnull;
import java.util.*;

// A basic implementation of A*, as seen in https://en.wikipedia.org/wiki/A*_search_algorithm
public abstract class AStar<TNode, TGoal>
{
	// Used to glue the fScore and the priority queue together
	// ALWAYS REMOVE AND RE-ADD NODES WHEN THEIR FSCORE CHANGES
	private class NodeCompare implements Comparator<TNode>
	{
		private final AStar<TNode,TGoal> _planner;

		public NodeCompare( AStar<TNode,TGoal> planner )
		{
			_planner = planner;
		}

		@Override
		public int compare( TNode lhs, TNode rhs )
		{
			assert ( _planner._fScore.containsKey( lhs ) );
			assert ( _planner._fScore.containsKey( rhs ) );
			val l = _planner._fScore.get( lhs );
			val r = _planner._fScore.get( rhs );
			return _planner._comparator.compare( l, r );
		}
	}

	private final PriorityQueue<TNode> _openSet;
	private final Hashtable<TNode,Integer> _gScore;
	private final Hashtable<TNode,Integer> _fScore;
	private final Hashtable<TNode,Integer> _hScore;

	private final int _gScoreInfinite;
	private final Comparator<Integer> _comparator;

	private TGoal _goal;
	private TNode _best;

	protected AStar(
			int gScoreInfinite,
			@Nonnull Comparator<Integer> comparator )
	{
		_gScoreInfinite = gScoreInfinite;
		_comparator = comparator;

		val nodeCompare = new NodeCompare( this );

		_openSet = new PriorityQueue<>( nodeCompare );
		_gScore = new Hashtable<>();
		_fScore = new Hashtable<>();
		_hScore = new Hashtable<>();

		_goal = null;
		_best = null;
	}

	protected AStar()
	{
		// Default sort logic, as seen in pretty much any A* example
		this( Integer.MAX_VALUE, Comparator.naturalOrder() );
	}

	public final void reset(
			@Nonnull TNode start,
			@Nonnull TGoal goal )
	{
		_openSet.clear();
		_gScore.clear();
		_fScore.clear();
		_hScore.clear();

		val score = getHScore( start, goal );

		_openSet.add( start );
		_gScore.put( start, 0 );
		_hScore.put( start, score );
		_fScore.put( start, score );

		_goal = goal;
		_best = start;
	}

	public TNode search()
	{
		assert ( _goal != null );
		assert ( _best != null );

		if ( _openSet.isEmpty() )
		{
			// Exhausted all possible iterations
			return _best;
		}

		final TNode current = _openSet.remove();

		if ( isGoal( current, _goal ) )
		{
			// Found perfect solution
			return current;
		}

		if ( _comparator.compare( _fScore.get( _best ), _fScore.get( current ) ) > 0 )
		{
			// This isn't part of the basic A* setup
			// We want to support partial plans here since we don't _need_ a perfect match, we just prefer it
			// If, after a number of iterations, no perfect plan is found the code can just take the best partial plan
			_best = current;
		}

		for ( final TNode neighbor : getNeighbors( current, _goal ) )
		{
			int gScoreTemp = _gScore.get( current ) + getDScore( current, neighbor );

			gScoreTemp = getModifiedScore( _best, neighbor, _goal, gScoreTemp );

			int gScoreOld = _gScore.getOrDefault( neighbor, _gScoreInfinite );

			if ( _comparator.compare( gScoreTemp, gScoreOld ) >= 0 )
				continue;

			int hScore = getHScore( neighbor, _goal );

			_gScore.put( neighbor, gScoreTemp );
			_hScore.put( neighbor, hScore );
			_fScore.put( neighbor, gScoreTemp + hScore );

			// Priority queue calculates priority on add
			// The fScore might have changed, so remove and re-add it to force the queue to update
			_openSet.remove( neighbor );
			_openSet.add( neighbor );
		}

		return null;
	}

	public TNode takeBest()
	{
		return _best;
	}

	// gets the modified score to adjust plan's weight depending on potential tolerance
	protected int getModifiedScore(@Nonnull TNode best, @Nonnull TNode neighbor, @Nonnull TGoal goal, @Nonnull Integer gScoreTemp)
	{
		return gScoreTemp;
	}

	// estimated cost to reach goal from current
	protected abstract int getHScore( @Nonnull TNode current, @Nonnull TGoal goal );

	// the weight of the edge from current to neighbor
	protected abstract int getDScore( @Nonnull TNode current, @Nonnull TNode neighbor );

	// does the current node reach the goal node
	protected abstract boolean isGoal( @Nonnull TNode current, @Nonnull TGoal goal );

	// get a list of all nodes the current node can lead into
	protected abstract Collection<TNode> getNeighbors( @Nonnull TNode current, @Nonnull TGoal goal );
}