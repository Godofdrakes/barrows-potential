package com.barrowspotential;

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
            int l = _planner._fScore.get( lhs );
            int r = _planner._fScore.get( rhs );
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

    protected AStar( int gScoreInfinite, Comparator<Integer> comparer )
    {
        assert ( comparer != null );

        _gScoreInfinite = gScoreInfinite;
        _comparator = comparer;

        NodeCompare nodeCompare = new NodeCompare( this );
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

    public final void reset( TNode start, TGoal goal )
    {
        assert ( start != null );
        assert ( goal != null );

        _openSet.clear();
        _gScore.clear();
        _fScore.clear();
        _hScore.clear();

        int score = getHScore( start, goal );
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

        TNode current = _openSet.remove();
        if ( isGoal( current, _goal ) )
        {
            // Found perfect solution
            return current;
        }

        if ( _comparator.compare( _hScore.get( _best ), _hScore.get( current ) ) > 0 )
        {
            // This isn't part of the basic A* setup
            // We want to support partial plans here since we don't _need_ a perfect match, we just prefer it
            // If, after a number of iterations, no perfect plan is found the code can just take the best partial plan
            _best = current;
        }

        for ( TNode neighbor : getNeighbors( current, _goal ) )
        {
            int gScoreTemp = _gScore.get( current ) + getDScore( current, neighbor );
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

    // estimated cost to reach goal from current
    protected abstract int getHScore( TNode current, TGoal goal );

    // the weight of the edge from current to neighbor
    protected abstract int getDScore( TNode current, TNode neighbor );

    // does the current node reach the goal node
    protected abstract boolean isGoal( TNode current, TGoal goal );

    // get a list of all nodes the current node can lead into
    protected abstract Collection<TNode> getNeighbors( TNode current, TGoal goal );
}
