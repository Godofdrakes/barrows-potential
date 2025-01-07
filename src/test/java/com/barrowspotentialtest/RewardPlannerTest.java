package com.barrowspotentialtest;

import com.barrowspotential.*;
import com.google.common.collect.ImmutableMap;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class RewardPlannerTest extends TestCase
{
	private static final int maxIterations = 20;

	private static final int planTolerance = 0;

	private final Logger logger = LoggerFactory.getLogger( RewardPlannerTest.class );

	private static RewardPlan AllBrothers()
	{
		return RewardPlan.create( Monster.brothers );
	}

	private static RewardPlan AllBrothersOptimalBloodRune()
	{
		// The optimal plan is 1 giant crypt spider, 1 skeleton, and 1 crypt spider (880 points)
		// 3 bloodworms and 1 crypt spider is also 880 points but is more steps, and therefor not optimal

		return AllBrothers()
				.append( Monster.GiantCryptSpider )
				.append( Monster.Skeleton )
				.append( Monster.CryptSpider );
	}

	private static RewardPlan AllBrothersSkeletonBloodwormBloodRune()
	{
		// The smallest plan to reach blood rune is 2 skeletons and 1 bloodworm if prioritising the smallest (874).
		// Without tolerance configured, it is 4x bloodworm (876).
		// This is calculated from plan target, not reward potential.

		if ( planTolerance >= 6 )
		{
			return AllBrothers()
					.append( Monster.Skeleton )
					.append( Monster.Skeleton )
					.append( Monster.Bloodworm );
		}

		return AllBrothers()
				.append( Monster.Bloodworm )
				.append( Monster.Bloodworm )
				.append( Monster.Bloodworm )
				.append( Monster.Bloodworm );
	}

	private static void LogPlan( @Nonnull Logger logger, @Nonnull RewardPlan plan )
	{
		final int score = plan.getRewardPotential();
		final int steps = plan.getMonsters().values().stream()
				.mapToInt( i -> i )
				.sum();

		for ( final Map.Entry<Monster, Integer> entry : plan.getMonsters().entrySet() )
		{
			final int count = entry.getValue();
			final int value = entry.getKey().getCombatLevel() * count;
			final String displayName = entry.getKey().getDisplayName();
			logger.info( "x{} {} ({})", count, displayName, value );
		}

		logger.info( "Steps: {}", steps );
		logger.info( "Score: {}", score );
	}

	private static void LogTarget( @Nonnull Logger logger, @Nonnull RewardTarget rewardTarget )
	{
		logger.info( "Target    : {}", rewardTarget.getDisplayName() );
		logger.info( "Target Min: {}", rewardTarget.getMinValue() );
		logger.info( "Target Max: {}", rewardTarget.getMaxValue() );
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

		assertEquals( rewardTarget.getMaxValue(), plan.getRewardPotential() );

		return plan;
	}

	// assert that for the given base plan there is a valid plan to reach the target within 20 iterations
	private static RewardPlan assertValidPlan(
			@Nonnull RewardPlanner planner,
			@Nonnull Logger logger,
			@Nonnull RewardTarget rewardTarget,
			@Nonnull RewardPlan basePlan )
	{
		logger.info( "searching for valid plan" );

		planner.reset( basePlan, rewardTarget.getMaxValue() );

		// at runtime the number of iterations is limited, test that here
		RewardPlan plan = RunPlanner( logger, planner, RewardPlannerTest.maxIterations );

		if ( plan == null )
		{
			logger.info( "taking partial plan" );

			plan = planner.takeBest();
		}

		assertNotNull( plan );

		LogPlan( logger, plan );
		LogTarget( logger, rewardTarget );

		assertTrue( "planned potential < minimum value",
				plan.getRewardPotential() >= rewardTarget.getMinValue() );
		assertTrue( "planned potential > maximum value value",
				plan.getRewardPotential() <= rewardTarget.getMaxValue() );

		return plan;
	}

	public void testBaseIsBest()
	{
		logger.info("Test case: testBaseIsBest");

		RewardPlan basePlan = new RewardPlan( ImmutableMap.of() );

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		planner.reset( basePlan, RewardTarget.Coins.getMaxValue() );

		// At minimum the best plan should be the base plan
		assertEquals( basePlan, planner.takeBest() );
	}

	public void testOptimalKillAll()
	{
		logger.info("Test case: testOptiomalKillAll");

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		RewardPlan allBrothers = AllBrothers();

		RewardPlan plan = null;

		plan = assertOptimalPlan( planner, logger, RewardTarget.BloodRune, allBrothers );

		assertEquals( "plan does not match known optimal plan",
				AllBrothersOptimalBloodRune(),
				plan );

		assertValidPlan( planner, logger, RewardTarget.DragonMed, allBrothers );
	}

	public void testOptimalKillSingle()
	{
		logger.info("Test case: testOptimalKillSingle");

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		RewardPlan combatLevel98 = RewardPlan.create( Monster.Ahrim );
		RewardPlan combatLevel115 = RewardPlan.create( Monster.Dharok );

		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertOptimalPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.BloodRune, combatLevel115 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel98 );
		assertValidPlan( planner, logger, RewardTarget.DragonMed, combatLevel115 );
	}

	public void testOptimalKillPartial()
	{
		logger.info("Test case: testOptimalKillPartial");

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		RewardPlan combatLevel98 = RewardPlan.create(
				//Monster.Ahrim, // combat level 98
				Monster.Dharok,
				Monster.Guthan,
				Monster.Karil,
				Monster.Torag,
				Monster.Verac
		);

		RewardPlan combatLevel115 = RewardPlan.create(
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
		logger.info("Test case: testSkeletonBloodworm");

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		planner.setTargetMonsters( EnumSet.of(
				Monster.Ahrim,
				Monster.Dharok,
				Monster.Guthan,
				Monster.Karil,
				Monster.Torag,
				Monster.Verac,
				Monster.Skeleton,
				Monster.Bloodworm
		) );

		RewardPlan basePlan = RewardPlan.create( Monster.brothers );

		RewardPlan plan = assertValidPlan( planner, logger, RewardTarget.BloodRune, basePlan );

		// Confirm plan is as expected for max bloodrunes when bloodworms and skeletons are selected
		assertEquals(plan, RewardPlannerTest.AllBrothersSkeletonBloodwormBloodRune());
	}

	// If there is a brother missing always plan for it first.
	// Avoids case where plan goes over score because we did mob then brother instead of brother than mob.
	public void testOneMissing()
	{
		logger.info("Test case: testOneMissing");

		int potential = 0;

		RewardPlanner planner = new RewardPlanner();
		planner.setSmallerPlanTolerance(planTolerance);

		planner.setTargetMonsters( EnumSet.of(
				Monster.Ahrim,
				Monster.Dharok,
				Monster.Guthan,
				Monster.Karil,
				Monster.Torag,
				Monster.Verac,
				Monster.Bloodworm,
				Monster.GiantCryptRat,
				Monster.Skeleton,
				Monster.GiantCryptSpider
		) );

		final Map<Monster, Integer> map = new HashMap<>();

		map.put( Monster.Ahrim, 1 ); // 98
//		map.put( Monster.Dharok, 1 ); // 115
		map.put( Monster.Guthan, 1 ); // 115
		map.put( Monster.Karil, 1 ); // 98
		map.put( Monster.Torag, 1 ); // 115
		map.put( Monster.Verac, 1 ); // 115

		for (Map.Entry<Monster, Integer> entry : map.entrySet())
		{
			Monster monster = entry.getKey();
			potential += monster.getRewardPotential();
		}

		// reward potential of
		final RewardPlan basePlan = new RewardPlan( map, potential );

		assertValidPlan( planner, logger, RewardTarget.BloodRune, basePlan );
	}
}