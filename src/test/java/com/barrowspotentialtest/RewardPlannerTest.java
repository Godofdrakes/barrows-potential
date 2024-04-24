package com.barrowspotentialtest;

import com.barrowspotential.Monster;
import com.barrowspotential.RewardPlan;
import com.barrowspotential.RewardPlanner;
import com.barrowspotential.RewardTarget;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class RewardPlannerTest extends TestCase
{
	private final Logger logger = LoggerFactory.getLogger( RewardPlannerTest.class );

	private static RewardPlan AllBrothers()
	{
		Map<Monster,Integer> map = new HashMap<>();
		map.put( Monster.Ahrim, 1 );
		map.put( Monster.Dharok, 1 );
		map.put( Monster.Guthan, 1 );
		map.put( Monster.Karil, 1 );
		map.put( Monster.Torag, 1 );
		map.put( Monster.Verac, 1 );
		return new RewardPlan( map );
	}

	private static RewardPlan AllBrothersOptimalBloodRune()
	{
		// The optimal plan is 1 giant crypt spider, 1 skeleton, and 1 crypt spider (880 points)
		// 3 bloodworms and 1 crypt spider is also 880 points but is more steps, and therefor not optimal

		Map<Monster,Integer> map = new HashMap<>();
		map.put( Monster.Ahrim, 1 );
		map.put( Monster.Dharok, 1 );
		map.put( Monster.Guthan, 1 );
		map.put( Monster.Karil, 1 );
		map.put( Monster.Torag, 1 );
		map.put( Monster.Verac, 1 );
		map.put( Monster.GiantCryptSpider, 1 );
		map.put( Monster.Skeleton, 1 );
		map.put( Monster.CryptSpider, 1 );
		return new RewardPlan( map );
	}

	private static void LogPlan( Logger logger, RewardPlan plan )
	{
		assert ( plan != null );

		int steps = 0;
		int score = 0;

		for ( Map.Entry<Monster,Integer> entry : plan.monsters.entrySet() )
		{
			int count = entry.getValue();
			int value = entry.getKey().getCombatLevel() * count;
			String displayName = entry.getKey().getDisplayName();
			logger.info( "x{} {} ({})", count, displayName, value );

			steps += count;
			score += value;

			if ( entry.getKey().isBrother() )
			{
				score += 2;
			}
		}

		logger.info( "Steps: {}", steps );
		logger.info( "Score: {}", score );
	}

	private static RewardPlan RunPlanner( Logger logger, RewardPlanner planner, int maxIterations )
	{
		int iteration = 0;

		RewardPlan plan = null;

		for ( ; iteration < maxIterations; ++iteration )
		{
			plan = planner.search();
			if ( plan != null )
				break;
		}

		logger.info( "Ran {}/{} iterations", iteration, maxIterations );

		return plan;
	}

	// assert that for the given base plan there is an optimal plan to reach the target
	private static RewardPlan assertOptimalPlan( RewardPlanner planner,
		Logger logger,
		RewardTarget rewardTarget,
		RewardPlan basePlan )
	{
		assert planner != null;

		logger.info( "searching for optimal plan" );

		planner.reset( basePlan, rewardTarget.getMaxValue() );
		planner.setTargetMonsters( EnumSet.allOf( Monster.class ) );

		// allow a lot of iterations here since this isn't runtime code
		RewardPlan plan = RunPlanner( logger, planner, 1000 );

		// do not accept partial solution

		assertNotNull( plan );

		LogPlan( logger, plan );

		assertEquals( rewardTarget.getMaxValue(), plan.GetRewardPotential() );

		return plan;
	}

	// assert that for the given base plan there is a valid plan to reach the target within 20 iterations
	private static RewardPlan assertValidPlan( RewardPlanner planner,
		Logger logger,
		RewardTarget rewardTarget,
		RewardPlan basePlan )
	{
		assert planner != null;

		logger.info( "searching for valid plan" );

		planner.reset( basePlan, rewardTarget.getMaxValue() );

		// at runtime the number of iterations is limited, test that here
		RewardPlan plan = RunPlanner( logger, planner, 20 );

		if ( plan == null )
		{
			plan = planner.takeBest();
		}

		assertNotNull( plan );

		LogPlan( logger, plan );

		assertTrue( "planned potential < minimum value",
			plan.GetRewardPotential() >= rewardTarget.getMinValue() );
		assertTrue( "planned potential > maximum value value",
			plan.GetRewardPotential() <= rewardTarget.getMaxValue() );

		return plan;
	}

	public void testBaseIsBest()
	{
		RewardPlan basePlan = RewardPlan.Default;

		RewardPlanner planner = new RewardPlanner();

		planner.reset( basePlan, RewardTarget.Coins.getMaxValue() );

		// At minimum the best plan should be the base plan
		assertEquals( basePlan, planner.takeBest() );
	}

	public void testOptimalKillAll()
	{
		RewardPlanner planner = new RewardPlanner();

		RewardPlan allBrothers = AllBrothers();
		RewardPlan plan = assertOptimalPlan( planner, logger, RewardTarget.BloodRune, allBrothers );

		assertEquals( "plan does not match known optimal plan",
			AllBrothersOptimalBloodRune(),
			plan );

		assertValidPlan( planner, logger, RewardTarget.DragonMed, allBrothers );
	}

	public void testOptimalKillSingle()
	{
		RewardPlanner planner = new RewardPlanner();

		RewardPlan combatLevel98 = RewardPlan.Create( Monster.Ahrim );
		RewardPlan combatLevel115 = RewardPlan.Create( Monster.Dharok );

		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel115 );
	}

	public void testOptimalKillPartial()
	{
		RewardPlanner planner = new RewardPlanner();

		RewardPlan combatLevel98 = RewardPlan.Create(
			//Monster.Ahrim, // combat level 98
			Monster.Dharok,
			Monster.Guthan,
			Monster.Karil,
			Monster.Torag,
			Monster.Verac
		);
		RewardPlan combatLevel115 = RewardPlan.Create(
			Monster.Ahrim,
			//Monster.Dharok, // combat level 115
			Monster.Guthan,
			Monster.Karil,
			Monster.Torag,
			Monster.Verac
		);

		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel115 );
	}

	// this is here because I was trying to figure out why restricting to skele+blood was planning blood x4
	// it's because blood x4 is 876, skele x2 + blood x1 is only 874. blood x4 is "more optimal"
	// leaving it here because it's useful for debugging if I decide to change the heuristic
	public void testSkeletonBloodworm()
	{
		RewardPlanner planner = new RewardPlanner();

		planner.setTargetMonsters( EnumSet.of(
			Monster.Skeleton,
			Monster.Bloodworm
		) );

		RewardPlan basePlan = RewardPlan.Create(
			Monster.Ahrim,
			Monster.Dharok,
			Monster.Guthan,
			Monster.Karil,
			Monster.Torag,
			Monster.Verac
		);

		assertValidPlan( planner, logger, RewardTarget.BloodRune, basePlan );
	}
}