package com.barrowspotential;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup( "barrowspotential" )
public interface BarrowsPotentialConfig extends Config
{
    @ConfigSection(
        name = "Planner",
        description = "",
        position = 0
    )
    String plannerSection = "plannerSection";

    @ConfigSection(
        name = "Highlight",
        description = "",
        position = 1
    )
    String highlightSection = "highlightSection";

    @ConfigItem(
        keyName = "rewardTarget",
        name = "Reward Target",
        description = "",
        section = plannerSection,
        position = 0
    )
    default RewardTarget rewardTarget() { return RewardTarget.BloodRune; }

    @ConfigItem(
        keyName = "killAhrim",
        name = "Kill Ahrim",
        description = "Do you plan on killing Ahrim every run?",
        section = plannerSection,
        position = 1
    )
    default boolean killAhrim() { return true; }

    @ConfigItem(
        keyName = "killDharok",
        name = "Kill Dharok",
        description = "Do you plan on killing Dharok every run?",
        section = plannerSection,
        position = 2
    )
    default boolean killDharok() { return true; }

    @ConfigItem(
        keyName = "killGuthan",
        name = "Kill Guthan",
        description = "Do you plan on killing Guthan every run?",
        section = plannerSection,
        position = 3
    )
    default boolean killGuthan() { return true; }

    @ConfigItem(
        keyName = "killKaril",
        name = "Kill Karil",
        description = "Do you plan on killing Karil every run?",
        section = plannerSection,
        position = 4
    )
    default boolean killKaril() { return true; }

    @ConfigItem(
        keyName = "killTorag",
        name = "Kill Torag",
        description = "Do you plan on killing Torag every run?",
        section = plannerSection,
        position = 5
    )
    default boolean killTorag() { return true; }

    @ConfigItem(
        keyName = "killVerac",
        name = "Kill Verac",
        description = "Do you plan on killing Verac every run?",
        section = plannerSection,
        position = 6
    )
    default boolean killVerac() { return true; }

    // @todo enum flags would make this killX stuff a lot more concise

    @ConfigItem(
        keyName = "killCryptRat",
        name = "Kill Crypt Rats",
        description = "",
        section = plannerSection,
        position = 7
    )
    default boolean killCryptRat() { return true; }

    @ConfigItem(
        keyName = "killBloodworm",
        name = "Kill Bloodworms",
        description = "",
        section = plannerSection,
        position = 8
    )
    default boolean killBloodworm() { return true; }

    @ConfigItem(
        keyName = "killCryptSpider",
        name = "Kill Crypt Spiders",
        description = "",
        section = plannerSection,
        position = 9
    )
    default boolean killCryptSpider() { return true; }

    @ConfigItem(
        keyName = "killGiantCryptRat",
        name = "Kill Giant Crypt Rats",
        description = "",
        section = plannerSection,
        position = 10
    )
    default boolean killGiantCryptRat() { return true; }

    @ConfigItem(
        keyName = "killSkeleton",
        name = "Kill Skeletons",
        description = "",
        section = plannerSection,
        position = 11
    )
    default boolean killSkeleton() { return true; }

    @ConfigItem(
        keyName = "killGiantCryptSpider",
        name = "Kill Giant Crypt Spiders",
        description = "",
        section = plannerSection,
        position = 12
    )
    default boolean killGiantCryptSpider() { return true; }

    @ConfigItem(
        keyName = "highlightNpcs",
        name = "Highlight NPCs",
        description = "Highlight any NPC that would not exceed the target score",
        section = highlightSection,
        position = 0
    )
    default boolean highlightNpc() { return true; }

    @ConfigItem(
        keyName = "highlightColor",
        name = "Highlight Color",
        description = "",
        section = highlightSection,
        position = 1
    )
    default Color highlightColor() { return Color.gray; }

    @ConfigItem(
        keyName = "highlightOptimal",
        name = "Highlight Optimal NPCs",
        description = "Highlight the NPCs that would best reach the target score",
        section = highlightSection,
        position = 2
    )
    default boolean highlightOptimal() { return true; }

    @ConfigItem(
        keyName = "overlayOptimal",
        name = "Overlay Optimal NPCs",
        description = "List optimal NPCs in an overlay",
        section = highlightSection,
        position = 3
    )
    default boolean overlayOptimal() { return true; }

    @ConfigItem(
        keyName = "optimalColor",
        name = "Optimal Color",
        description = "",
        section = highlightSection,
        position = 4
    )
    default Color optimalColor() { return Color.white; }
}
