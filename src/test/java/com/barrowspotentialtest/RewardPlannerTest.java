package com.barrowspotentialtest;

import com.barrowspotential.Monster;
import com.barrowspotential.RewardPlan;
import com.barrowspotential.RewardPlanner;
import com.barrowspotential.RewardTarget;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class RewardPlannerTest extends TestCase
{
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
        Map<Monster,Integer> map = new HashMap<>();
        map.put( Monster.Ahrim, 1 );
        map.put( Monster.Dharok, 1 );
        map.put( Monster.Guthan, 1 );
        map.put( Monster.Karil, 1 );
        map.put( Monster.Torag, 1 );
        map.put( Monster.Verac, 1 );

        // The optimal plan is 1 giant crypt spider, 1 skeleton, and 1 crypt spider (880 points)
        // 3 bloodworms and 1 crypt spider is also 880 points but is more steps, and therefor not optimal
        map.put( Monster.GiantCryptSpider, 1 );
        map.put( Monster.Skeleton, 1 );
        map.put( Monster.CryptSpider, 1 );
        return new RewardPlan( map );
    }

    private static void LogPlan( RewardPlan plan )
    {
        assert ( plan != null );

        int steps = 0;
        int score = 0;

        for ( Map.Entry<Monster,Integer> entry : plan.monsters.entrySet() )
        {
            int count = entry.getValue();
            int value = entry.getKey().getCombatLevel() * count;
            String displayName = entry.getKey().getDisplayName();
            System.out.printf( "x%d %s (%d)\n", count, displayName, value );

            steps += count;
            score += value;
        }

        System.out.printf( "Steps: %d\n", steps );
        System.out.printf( "Score: %d\n", score );
    }

    private static RewardPlan RunPlanner( RewardPlanner planner, int maxIterations )
    {
        int iteration = 0;

        RewardPlan plan = null;

        for ( ; iteration < maxIterations; ++iteration )
        {
            plan = planner.search();
            if ( plan != null )
                break;
        }

        System.out.printf( "Ran %d/%d iterations\n", iteration, maxIterations );

        return plan;
    }

    // assert that for the given base plan there is an optimal plan to reach the target
    private RewardPlan assertOptimalPlan( RewardTarget rewardTarget, RewardPlan basePlan )
    {
        System.out.println( "searching for optimal plan" );

        RewardPlanner planner = new RewardPlanner();

        planner.mode = rewardTarget.getMaxValue() < Integer.MAX_VALUE
            ? RewardPlanner.Mode.NEAREST
            : RewardPlanner.Mode.ANY;
        planner.reset( basePlan, rewardTarget.getMaxValue() < Integer.MAX_VALUE
            ? rewardTarget.getMaxValue()
            : rewardTarget.getMinValue() );

        // allow a lot of iterations here since this isn't runtime code
        RewardPlan plan = RunPlanner( planner, 1000 );

        // do not accept partial solution

        assertNotNull( plan );

        LogPlan( plan );

        assertEquals( rewardTarget.getMaxValue(), plan.GetRewardPotential() );

        System.out.println();
        return plan;
    }

    // assert that for the given base plan there is a valid plan to reach the target within 20 iterations
    private RewardPlan assertValidPlan( RewardTarget rewardTarget, RewardPlan basePlan )
    {
        System.out.println( "searching for valid plan" );

        RewardPlanner planner = new RewardPlanner();

        planner.mode = rewardTarget.getMaxValue() < Integer.MAX_VALUE
            ? RewardPlanner.Mode.NEAREST
            : RewardPlanner.Mode.ANY;
        planner.reset( basePlan, rewardTarget.getMaxValue() < Integer.MAX_VALUE
            ? rewardTarget.getMaxValue()
            : rewardTarget.getMinValue() );

        // at runtime the number of iterations is limited, test that here
        RewardPlan plan = RunPlanner( planner, 20 );

        if ( plan == null )
        {
            plan = planner.takeBest();
        }

        assertNotNull( plan );

        LogPlan( plan );

        assertTrue( "planned potential < minimum value", plan.GetRewardPotential() >= rewardTarget.getMinValue() );
        assertTrue( "planned potential > maximum value value",
            plan.GetRewardPotential() <= rewardTarget.getMaxValue() );

        System.out.println();
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
        RewardPlan allBrothers = AllBrothers();
        RewardPlan plan = assertOptimalPlan( RewardTarget.BloodRune, allBrothers );
        assertEquals( "plan does not match known optimal plan", AllBrothersOptimalBloodRune(), plan );

        assertValidPlan( RewardTarget.DragonMed, allBrothers );
    }

    public void testOptimalKillSingle()
    {
        RewardPlan combatLevel98 = RewardPlan.Create( Monster.Ahrim );
        RewardPlan combatLevel115 = RewardPlan.Create( Monster.Dharok );
        assertOptimalPlan( RewardTarget.BloodRune, combatLevel98 );
        assertOptimalPlan( RewardTarget.BloodRune, combatLevel115 );
        assertValidPlan( RewardTarget.BloodRune, combatLevel98 );
        assertValidPlan( RewardTarget.BloodRune, combatLevel115 );
        assertValidPlan( RewardTarget.DragonMed, combatLevel98 );
        assertValidPlan( RewardTarget.DragonMed, combatLevel115 );
    }

    public void testOptimalKillPartial()
    {
        RewardPlan combatLevel98 = RewardPlan.Create(
            //Monster.Ahrim, // combat level 98
            Monster.Dharok,
            Monster.Guthan,
            Monster.Karil,
            Monster.Torag,
            Monster.Verac
        );
        RewardPlan combatLevel115 = RewardPlan.Create(
            //Monster.Ahrim, // combat level 98
            Monster.Dharok,
            Monster.Guthan,
            Monster.Karil,
            Monster.Torag,
            Monster.Verac
        );

        assertOptimalPlan( RewardTarget.BloodRune, combatLevel98 );
        assertOptimalPlan( RewardTarget.BloodRune, combatLevel115 );
        assertValidPlan( RewardTarget.BloodRune, combatLevel98 );
        assertValidPlan( RewardTarget.BloodRune, combatLevel115 );
        assertValidPlan( RewardTarget.DragonMed, combatLevel98 );
        assertValidPlan( RewardTarget.DragonMed, combatLevel115 );
    }
}