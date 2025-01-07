package com.barrowspotential;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

// an implementation of A* used to plan which crypt monsters to kill to reach the desired reward potential.
// this only considers crypt monsters as possible nodes.
// any brothers the player wants to kill should be fed to Reset() as the starting plan.
@Slf4j
public final class RewardPlanner extends AStar<RewardPlan, Integer>
{
	@Nonnull
	private Set<Monster> targetMonsters = ImmutableSet.of();

	@Getter
	@Setter
	private int smallerPlanTolerance = 0;

	@Inject
	public RewardPlanner()
	{
		// inverse sort simplifies the math a bit
		super( Integer.MIN_VALUE, Comparator.reverseOrder() );
	}

	public void setTargetMonsters( @Nonnull Set<Monster> monsters )
	{
		targetMonsters = ImmutableSet.copyOf( monsters );
	}

	@Override
	protected int getSize( @Nonnull RewardPlan current )
	{
		return current.getSize();
	}

	@Override
	protected int getHScore( @Nonnull RewardPlan current, @Nonnull Integer target )
	{
		// hScore becomes the total reward potential of the current plan
		return current.getRewardPotential();
	}

	@Override
	protected int getDScore( @Nonnull RewardPlan current, @Nonnull RewardPlan neighbor )
	{
		// dScore becomes the increase in reward potential compared to the current plan
		return neighbor.getRewardPotential() - current.getRewardPotential();
	}

	@Override
	protected boolean isGoal( @Nonnull RewardPlan current, @Nonnull Integer target )
	{
		return current.getRewardPotential() >= target;
	}

	@Override
	protected Collection<RewardPlan> getNeighbors( @Nonnull RewardPlan current, @Nonnull Integer target )
	{
		// generate a list of all possible crypt monsters we could kill
		val neighbors = new ArrayList<RewardPlan>();

		for ( val brother : Monster.brothers )
		{
			if ( current.contains( brother ) )
			{
				// skip brothers that have already been killed
				continue;
			}

			if ( targetMonsters.contains( brother ) )
			{
				neighbors.add( current.append( brother ) );
			}
		}

		if ( !neighbors.isEmpty() )
		{
			// If there are any brothers remaining always plan for those first
			return neighbors;
		}

		for ( val monster : Monster.cryptMonsters )
		{
			val expectedRewardPotential = current.getRewardPotential() + monster.getCombatLevel();

			if ( expectedRewardPotential > target )
			{
				// skip crypt monsters that would put us over the target score
				continue;
			}

			if ( targetMonsters.contains( monster ) )
			{
				neighbors.add( current.append( monster ) );
			}
		}

		return neighbors;
	}

	public RewardPlan search( final int iterationsMax )
	{
		RewardPlan plan = null;

		int iterations = 0;

		// There's generally not a reason to let the planner run for a bunch of iterations
		// We generally know within ~20 iterations if a perfect plan (exactly the target score) is possible
		for ( ; iterations < iterationsMax; ++iterations )
		{
			plan = search();

			if ( plan != null )
				break;
		}

		log.debug( "planner ran for {} iterations", iterations );

		if ( plan == null )
		{
			// We failed to get a plan that _exactly_ matches the target within the iteration limit
			// Just take the best plan the planner could come up with
			log.debug( "taking partial plan" );

			plan = takeBest();
		}

		return plan;
	}
}