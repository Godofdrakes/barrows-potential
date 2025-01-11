# Barrows Potential

## Config

| Config Item            | Default    | Info                                                              |
|------------------------|------------|-------------------------------------------------------------------|
| Reward Target          | Blood Rune | The reward you are targeting. This drives the planner.            |
| Reward Plan            | <all>      | Do you plan on killing \<monster\>? This drives the planner.      |
| Highlight NPCs         | True       | Highlight all NPCs that would get you closer to the reward target |
| Highlight Optimal NPCs | True       | Highlight the optimal NPCs to reach your reward target            |
| Overlay Optimal NPCs   | True       | Overlay the list of optimal NPCs to kill                          |


## Usage

**TLDR: Set item you want, Set brothers/monsters you want to kill, kill brothers, kill highlighted NPCs where possible**

Once you enter the crypt the plugin will highlight all NPCs that would not put you over the ideal reward potential for your item. For example if you select blood runes it will not highlight NPCs that would put you over 880. In addition the plugin will attempt to generate an "optimal" plan that reaches the ideal reward potential in the fewest possible kills. These NPCs are highlighted in a different color as well as listed in an overlay. If you do not follow the optimal plan (can't find the npc, killed the wrong npc on accident, ect.) the planner will adapt, generating a new plan automatically.

If all you want to do is follow a known plan deselect all crypt monsters you don't want to target and disable both "optimal" config settings.

## Known Issues

If you are targeting **only** Ahrim or Karil the plugin will create a plan with a score of 865 (Giant crypt spider x9, Crypt spider x1) despite a plan of 880 being possible (Giant crypt spider x7, Crypt spider x1, Giant crypt rat x1, Crypt rat x1, Bloodworm x1). This is due to the plugin requiring 64 iterations to reach the optimal plan but it is limited to 20 for performance reasons. If this is a concern you should start by killing Giant crypt spiders. Once you kill enough the plugin will correct iteself.

## How It Works

The plugin uses an implementation of the A* algorithm to search for a plan that maximizes reward potential. Each iteration of the search will select the plan that is nearest to the reward target and then generate new plans that attempt to get even closer while not going past the upper bounds of the reward target. When you kill a brother or NPC in the crypt the planner is run for 20 iterations. If an optimal plan cannot be found after that the plugin takes the best sub-opimtal plan the planner could find.

## Changelog

### Version 1

* Release

### Version 2 

* Added change notification for plugin updates
  * Happens once per update
* Added support for selecting which monsters to consider for the planner
  * If you just want to do "Skeleton x2, Blood x1" every time you can do that now

### Version 3

* Fixed case where overlay would be visible outside the crypt

### Version 4

* Rewrote overlay and highlight implementation
  * Fixed case where overlay/highlight could be visible while the plugin was disabled
  * Overlay label now matches plugin name so people know who's to blame if it breaks (me)

### Version 5

* Now suggests the traditional "Skeleton x2, Bloodworm x1" plan instead of "Bloodworm x4"
  * Thanks to @Tybo24