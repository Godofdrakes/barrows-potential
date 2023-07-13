package com.barrowspotential;

import java.util.*;

// an implementation of A* used to plan which crypt monsters to kill to reach the desired reward potential.
// this only considers crypt monsters as possible nodes.
// any brothers the player wants to kill should be fed to Reset() as the starting plan.
public class RewardPlanner extends AStar<RewardPlan,Integer>
{
	public enum Mode
	{
		// look for plans that do not exceed the goal value
		NEAREST,

		// look for any plan that meets or exceeds the goal value
		ANY,
	}

	public Mode mode = Mode.NEAREST;

	public Set<Monster> monstersToTarget = Monster.cryptMonsters;

	public RewardPlanner()
	{
		// inverse sort simplifies the math a bit
		super( Integer.MIN_VALUE, Comparator.reverseOrder() );
	}

	@Override
	protected int getHScore( RewardPlan current, Integer target )
	{
		// hScore becomes the total reward potential of the current plan
		return current.GetRewardPotential();
	}

	@Override
	protected int getDScore( RewardPlan current, RewardPlan neighbor )
	{
		// dScore becomes the increase in reward potential compared to the current plan
		return neighbor.GetRewardPotential() - current.GetRewardPotential();
	}

	@Override
	protected boolean isGoal( RewardPlan current, Integer target )
	{
		return current.GetRewardPotential() >= target;
	}

	@Override
	protected Collection<RewardPlan> getNeighbors( RewardPlan current, Integer target )
	{
		assert ( monstersToTarget != null );

		// generate a list of all possible crypt monsters we could kill
		ArrayList<RewardPlan> neighbors = new ArrayList<>();

		int currentValue = current.GetRewardPotential();

		for ( Monster monster : monstersToTarget )
		{
			// skip crypt monsters that would put us over the target score
			if ( mode == Mode.NEAREST && currentValue + monster.getCombatLevel() > target )
				continue;

			neighbors.add( current.Append( monster ) );
		}

		return neighbors;
	}
}
