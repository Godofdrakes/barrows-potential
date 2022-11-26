package com.barrowspotential;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RewardTarget
{
    Coins("Coins",1, 380),
    MindRune("Mind Rune",381, 505),
    ChaosRune("Chaos Rune", 506, 630),
    DeathRune("Death Rune", 631, 755),
    BloodRune("Blood Rune", 756, 880),
    BoltRack("Bolt Rack", 881, 1005),

    // The min-max range on these are so small that the planner could fail if we limit it to a max score
    KeyHalf("Crystal Key", 1006, Integer.MAX_VALUE),
    DragonMed("Dragon Med Helm", 1012, Integer.MAX_VALUE);

    private final String displayName;
    private final int minValue;
    private final int maxValue;

    @Override
    public String toString()
    {
        return getDisplayName();
    }
}
