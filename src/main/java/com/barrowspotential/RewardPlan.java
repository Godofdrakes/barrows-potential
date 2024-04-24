package com.barrowspotential;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.val;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

@Getter
public final class RewardPlan
{
	public static RewardPlan create( @Nonnull Iterable<Monster> monsters, @Nonnegative int rewardPotential )
	{
		val map = new HashMap<Monster,Integer>();

		for ( val monster : monsters )
		{
			map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		}

		return new RewardPlan( map, rewardPotential );
	}

	public static RewardPlan create( @Nonnull Iterable<Monster> monsters )
	{
		val map = new HashMap<Monster,Integer>();

		for ( val monster : monsters )
		{
			map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		}

		return new RewardPlan( map );
	}

	public static RewardPlan create( @Nonnull Monster... monsters )
	{
		val map = new HashMap<Monster,Integer>();

		for ( val monster : monsters )
		{
			map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );
		}

		return new RewardPlan( map );
	}

	private static int calculateRewardPotential( @Nonnull Map<Monster,Integer> monsters )
	{
		return monsters.entrySet()
			.stream()
			.mapToInt( entry -> entry.getValue() * entry.getKey().getRewardPotential() )
			.sum();
	}

	private final ImmutableMap<Monster,Integer> monsters;

	private final int rewardPotential;

	public RewardPlan( @Nonnull Map<Monster,Integer> monsters, @Nonnegative int rewardPotential )
	{
		this.monsters = ImmutableMap.copyOf( monsters );
		this.rewardPotential = rewardPotential;
	}

	public RewardPlan( @Nonnull Map<Monster,Integer> monsters )
	{
		this.monsters = ImmutableMap.copyOf( monsters );
		this.rewardPotential = calculateRewardPotential( this.monsters );
	}

	public boolean contains( @Nonnull Monster monster )
	{
		return monsters.containsKey( monster );
	}

	public RewardPlan append( @Nonnull Monster monster )
	{
		val map = new HashMap<>( this.monsters );

		map.compute( monster, ( key, value ) -> value == null ? 1 : value + 1 );

		val newRewardPotential = this.rewardPotential + monster.getRewardPotential();

		return new RewardPlan( map, newRewardPotential );
	}

	@Override
	public int hashCode()
	{
		// Intentionally not including the rewardPotential in the hash.
		// We just want to quickly check if we've already encountered this plan.
		return monsters.hashCode();
	}

	@Override
	public boolean equals( Object obj )
	{
		// Intentionally not including the rewardPotential in the hash.
		// We just to quickly check if we've already encountered this plan.

		if ( obj instanceof RewardPlan )
		{
			return monsters.equals( ( (RewardPlan) obj ).monsters );
		}

		return false;
	}

	@Override
	public String toString()
	{
		return String.format( "Potential: %d", rewardPotential );
	}
}
