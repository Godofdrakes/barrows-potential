package com.barrowspotential;

import com.google.common.collect.ImmutableMap;
import lombok.val;

import javax.annotation.Nonnull;
import java.util.*;

public class RewardPlan
{
	public static RewardPlan Create( Monster... monsters )
	{
		val map = new HashMap<Monster,Integer>();

		for ( val monster : monsters )
		{
			map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		}

		return new RewardPlan( map );
	}

	public static RewardPlan Create( @Nonnull Collection<Monster> monsters )
	{
		val map = new HashMap<Monster,Integer>();

		for ( val monster : monsters )
		{
			map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		}

		return new RewardPlan( map );
	}

	public static final RewardPlan Default = new RewardPlan( new HashMap<>() );

	public final Map<Monster,Integer> monsters;

	public RewardPlan( @Nonnull Map<Monster,Integer> monsters )
	{
		this.monsters = ImmutableMap.copyOf( monsters );
	}

	public RewardPlan Append( Monster monster )
	{
		val monsters = new HashMap<>( this.monsters );
		monsters.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		return new RewardPlan( monsters );
	}

	public int GetRewardPotential()
	{
		int rewardPotential = 0;

		for ( Map.Entry<Monster,Integer> entry : this.monsters.entrySet() )
		{
			rewardPotential += entry.getKey().getCombatLevel() * entry.getValue();

			// each brother adds 2 to the final reward potential
			if ( entry.getKey().isBrother() )
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
