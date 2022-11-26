package com.barrowspotential;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RewardPlan
{
    public static RewardPlan Create( Monster... monsters )
    {
        Map<Monster,Integer> map = new HashMap<>();

        for ( Monster monster : monsters )
        {
            map.put( monster, map.getOrDefault( monster, 0 ) + 1 );
        }

        return new RewardPlan( map );
    }

    public static final RewardPlan Default = new RewardPlan( new HashMap<>() );

    public final Map<Monster,Integer> monsters;

    public RewardPlan( Map<Monster,Integer> monsters )
    {
        this.monsters = Collections.unmodifiableMap( monsters );
    }

    public RewardPlan Append( Monster monster )
    {
        Map<Monster,Integer> monsters = new HashMap<>( this.monsters );
        monsters.put( monster, this.monsters.getOrDefault( monster, 0 ) + 1 );
        return new RewardPlan( monsters );
    }

    public int GetRewardPotential()
    {
        int rewardPotential = 0;

        for ( Map.Entry<Monster,Integer> entry : this.monsters.entrySet() )
        {
            rewardPotential += entry.getKey().getCombatLevel() * entry.getValue();

            // each brother adds 2 to the final reward potential
            if (entry.getKey().isBrother())
            {
                rewardPotential += 2;
            }
        }

        return rewardPotential;
    }

    @Override
    public int hashCode()
    {
        return monsters.hashCode();
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null || obj.getClass() != RewardPlan.class )
            return false;

        RewardPlan other = (RewardPlan) obj;
        return monsters.equals( other.monsters );
    }

    @Override
    public String toString()
    {
        return String.format( "Potential: %d", GetRewardPotential() );
    }
}
