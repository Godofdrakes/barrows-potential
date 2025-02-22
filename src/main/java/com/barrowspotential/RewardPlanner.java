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
	protected int getModifiedScore( @Nonnull RewardPlan best, @Nonnull RewardPlan neighbor, @Nonnull Integer goal, @Nonnull Integer gScoreTemp )
	{
		final int tolerance = getSmallerPlanTolerance();

		if ( tolerance > 0 )
		{
			int i = getSize( best );
			int j = getSize( neighbor );

			// if the neighbor plan is bigger than the best plan and the reward potential difference
			// between the two plans is within the set tolerance, adjust the score of the bigger
			// plan lower. This allows the shorter plan within the tolerance to end up with a higher score.
			if ( j > i && Math.abs( getHScore( neighbor, goal ) - getHScore( best, goal )) < tolerance )
			{
				// using logarithmic scaling for calculating the weighting. Example:
				// best 874 (size 8), neighbor 876 (size 9). Tolerance 3.
				// 9-8+3=4. Log(4) = 1.3, Log(1.5) = 0.4 1.3/0.4 = 3.4. 3.4*3=10.2 rounded up.
				// this results in 11 being removed from the gScore for the bigger plan.
				// change the tolerance to 53 and it sets the plan to just 2 skeleton (822 potential)
				// which is the next smallest plan with the highest reward.
				gScoreTemp -= (int) Math.ceil((( Math.log( j - i + tolerance ) / Math.log( 1.5 )) * tolerance ));
			}
		}

		// regular logic runs if tolerance is not set or the
		// tolerance amounts/size conditions are not met
		return gScoreTemp;
	}

	private int getSize( @Nonnull RewardPlan current )
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