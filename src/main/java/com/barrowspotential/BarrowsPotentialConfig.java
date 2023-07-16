package com.barrowspotential;

import net.runelite.client.config.*;

import java.awt.*;
import java.util.EnumSet;
import java.util.Set;

@ConfigGroup( BarrowsPotentialConfig.CONFIG_GROUP )
public interface BarrowsPotentialConfig extends Config
{
	String CONFIG_GROUP = "barrowspotential";

	String CONFIG_VERSION = "version";

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
		keyName = "rewardPlan",
		name = "Reward Plan",
		description = "Which monsters do you plan on defeating? Ctrl+Click to select.",
		section = plannerSection,
		position = 1
	)
	default Set<Monster> rewardPlan() { return EnumSet.allOf( Monster.class ); }

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
