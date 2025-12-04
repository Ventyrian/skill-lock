package com.skilllock;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("skilllock")
public interface SkillLockConfig extends Config
{

    @ConfigSection(
            name = "Lock Skills",
            description = "Lock or unlock skills by using the associated checkbox or in game menu right click menu on skill",
            position = 0
    )
    String lockSkills = "lockSkills";

    @ConfigItem(
            keyName = "agility",
            name = "Agility",
            description = "Lock or Unlock the Agility skill",
            section = lockSkills,
            position = 1
    )
    default boolean agility()
    {
        return false;
    }

    @ConfigItem(
            keyName = "attack",
            name = "Attack",
            description = "Lock or Unlock the Attack skill",
            section = lockSkills,
            position = 2
    )
    default boolean attack()
    {
        return false;
    }

    @ConfigItem(
            keyName = "construction",
            name = "Construction",
            description = "Lock or Unlock the Construction skill",
            section = lockSkills,
            position = 3
    )
    default boolean construction()
    {
        return false;
    }

    @ConfigItem(
            keyName = "cooking",
            name = "Cooking",
            description = "Lock or Unlock the Cooking skill",
            section = lockSkills,
            position = 4
    )
    default boolean cooking()
    {
        return false;
    }

    @ConfigItem(
            keyName = "crafting",
            name = "Crafting",
            description = "Lock or Unlock the Crafting skill",
            section = lockSkills,
            position = 5
    )
    default boolean crafting()
    {
        return false;
    }

    @ConfigItem(
            keyName = "defense",
            name = "Defense",
            description = "Lock or Unlock the Defense skill",
            section = lockSkills,
            position = 6
    )
    default boolean defense()
    {
        return false;
    }

    @ConfigItem(
            keyName = "farming",
            name = "Farming",
            description = "Lock or Unlock the Farming skill",
            section = lockSkills,
            position = 7
    )
    default boolean farming()
    {
        return false;
    }

    @ConfigItem(
            keyName = "firemaking",
            name = "Firemaking",
            description = "Lock or Unlock the Firemaking skill",
            section = lockSkills,
            position = 8
    )
    default boolean firemaking()
    {
        return false;
    }

    @ConfigItem(
            keyName = "fishing",
            name = "Fishing",
            description = "Lock or Unlock the Fishing skill",
            section = lockSkills,
            position = 9
    )
    default boolean fishing()
    {
        return false;
    }

    @ConfigItem(
            keyName = "fletching",
            name = "Fletching",
            description = "Lock or Unlock the Fletching skill",
            section = lockSkills,
            position = 10
    )
    default boolean fletching()
    {
        return false;
    }

    @ConfigItem(
            keyName = "herblore",
            name = "Herblore",
            description = "Lock or Unlock the Herblore skill",
            section = lockSkills,
            position = 11
    )
    default boolean herblore()
    {
        return false;
    }

    @ConfigItem(
            keyName = "hitpoints",
            name = "Hitpoints",
            description = "Lock or Unlock the Hitpoints skill",
            section = lockSkills,
            position = 12
    )
    default boolean hitpoints()
    {
        return false;
    }

    @ConfigItem(
            keyName = "hunter",
            name = "Hunter",
            description = "Lock or Unlock the Hunter skill",
            section = lockSkills,
            position = 13
    )
    default boolean hunter()
    {
        return false;
    }

    @ConfigItem(
            keyName = "magic",
            name = "Magic",
            description = "Lock or Unlock the Magic skill",
            section = lockSkills,
            position = 14
    )
    default boolean magic()
    {
        return false;
    }

    @ConfigItem(
            keyName = "mining",
            name = "Mining",
            description = "Lock or Unlock the Mining skill",
            section = lockSkills,
            position = 15
    )
    default boolean mining()
    {
        return false;
    }

    @ConfigItem(
            keyName = "prayer",
            name = "Prayer",
            description = "Lock or Unlock the Prayer skill",
            section = lockSkills,
            position = 16
    )
    default boolean prayer()
    {
        return false;
    }

    @ConfigItem(
            keyName = "ranged",
            name = "Ranged",
            description = "Lock or Unlock the Ranged skill",
            section = lockSkills,
            position = 17
    )
    default boolean ranged()
    {
        return false;
    }

    @ConfigItem(
            keyName = "runecraft",
            name = "Runecraft",
            description = "Lock or Unlock the Runecraft skill",
            section = lockSkills,
            position = 18
    )
    default boolean runecraft()
    {
        return false;
    }

    @ConfigItem(
            keyName = "sailing",
            name = "Sailing",
            description = "Lock or Unlock the Sailing skill",
            section = lockSkills,
            position = 19
    )
    default boolean sailing()
    {
        return false;
    }

    @ConfigItem(
            keyName = "slayer",
            name = "Slayer",
            description = "Lock or Unlock the Slayer skill",
            section = lockSkills,
            position = 20
    )
    default boolean slayer()
    {
        return false;
    }

    @ConfigItem(
            keyName = "smithing",
            name = "Smithing",
            description = "Lock or Unlock the Smithing skill",
            section = lockSkills,
            position = 21
    )
    default boolean smithing()
    {
        return false;
    }

    @ConfigItem(
            keyName = "strength",
            name = "Strength",
            description = "Lock or Unlock the Strength skill",
            section = lockSkills,
            position = 22
    )
    default boolean strength()
    {
        return false;
    }

    @ConfigItem(
            keyName = "thieving",
            name = "Thieving",
            description = "Lock or Unlock the Thieving skill",
            section = lockSkills,
            position = 23
    )
    default boolean thieving()
    {
        return false;
    }

    @ConfigItem(
            keyName = "woodcutting",
            name = "Woodcutting",
            description = "Lock or Unlock the Woodcutting skill",
            section = lockSkills,
            position = 24
    )
    default boolean woodcutting()
    {
        return false;
    }

    @ConfigSection(
            name = "Set Levels",
            description = "Set a static level to be displayed in the skills menu, hold shift while hovering the skill to see your actual level",
            position = 25
    )
    String setLevels = "setLevels";

    @ConfigItem(
            keyName = "agility_level",
            name = "Agility Level",
            description = "Set a static Agility level",
            section = setLevels,
            position = 26
    )
    @Range(min = 0, max = 99)
    default int agility_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "attack_level",
            name = "Attack Level",
            description = "Set a static Attack level",
            section = setLevels,
            position = 27
    )
    @Range(min = 0, max = 99)
    default int attack_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "construction_level",
            name = "Construction Level",
            description = "Set a static Construction level",
            section = setLevels,
            position = 28
    )
    @Range(min = 0, max = 99)
    default int construction_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "cooking_level",
            name = "Cooking Level",
            description = "Set a static Cooking level",
            section = setLevels,
            position = 29
    )
    @Range(min = 0, max = 99)
    default int cooking_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "crafting_level",
            name = "Crafting Level",
            description = "Set a static Crafting level",
            section = setLevels,
            position = 30
    )
    @Range(min = 0, max = 99)
    default int crafting_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "defense_level",
            name = "Defense Level",
            description = "Set a static Defense level",
            section = setLevels,
            position = 31
    )
    @Range(min = 0, max = 99)
    default int defense_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "farming_level",
            name = "Farming Level",
            description = "Set a static Farming level",
            section = setLevels,
            position = 32
    )
    @Range(min = 0, max = 99)
    default int farming_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "firemaking_level",
            name = "Firemaking Level",
            description = "Set a static Firemaking level",
            section = setLevels,
            position = 33
    )
    @Range(min = 0, max = 99)
    default int firemaking_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "fishing_level",
            name = "Fishing Level",
            description = "Set a static Fishing level",
            section = setLevels,
            position = 34
    )
    @Range(min = 0, max = 99)
    default int fishing_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "fletching_level",
            name = "Fletching Level",
            description = "Set a static Fletching level",
            section = setLevels,
            position = 35
    )
    @Range(min = 0, max = 99)
    default int fletching_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "herblore_level",
            name = "Herblore Level",
            description = "Set a static Herblore level",
            section = setLevels,
            position = 36
    )
    @Range(min = 0, max = 99)
    default int herblore_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "hitpoints_level",
            name = "Hitpoints Level",
            description = "Set a static Hitpoints level",
            section = setLevels,
            position = 37
    )
    @Range(min = 0, max = 99)
    default int hitpoints_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "hunter_level",
            name = "Hunter Level",
            description = "Set a static Hunter level",
            section = setLevels,
            position = 38
    )
    @Range(min = 0, max = 99)
    default int hunter_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "magic_level",
            name = "Magic Level",
            description = "Set a static Magic level",
            section = setLevels,
            position = 39
    )
    @Range(min = 0, max = 99)
    default int magic_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "mining_level",
            name = "Mining Level",
            description = "Set a static Mining level",
            section = setLevels,
            position = 40
    )
    @Range(min = 0, max = 99)
    default int mining_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "prayer_level",
            name = "Prayer Level",
            description = "Set a static Prayer level",
            section = setLevels,
            position = 41
    )
    @Range(min = 0, max = 99)
    default int prayer_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "ranged_level",
            name = "Ranged Level",
            description = "Set a static Ranged level",
            section = setLevels,
            position = 42
    )
    @Range(min = 0, max = 99)
    default int ranged_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "runecraft_level",
            name = "Runecraft Level",
            description = "Set a static Runecraft level",
            section = setLevels,
            position = 43
    )
    @Range(min = 0, max = 99)
    default int runecraft_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "sailing_level",
            name = "Sailing Level",
            description = "Set a static Sailing level",
            section = setLevels,
            position = 44
    )
    @Range(min = 0, max = 99)
    default int sailing_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "slayer_level",
            name = "Slayer Level",
            description = "Set a static Slayer level",
            section = setLevels,
            position = 45
    )
    @Range(min = 0, max = 99)
    default int slayer_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "smithing_level",
            name = "Smithing Level",
            description = "Set a static Smithing level",
            section = setLevels,
            position = 46
    )
    @Range(min = 0, max = 99)
    default int smithing_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "strength_level",
            name = "Strength Level",
            description = "Set a static Strength level",
            section = setLevels,
            position = 47
    )
    @Range(min = 0, max = 99)
    default int strength_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "thieving_level",
            name = "Thieving Level",
            description = "Set a static Thieving level",
            section = setLevels,
            position = 48
    )
    @Range(min = 0, max = 99)
    default int thieving_level()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "woodcutting_level",
            name = "Woodcutting Level",
            description = "Set a static Woodcutting level",
            section = setLevels,
            position = 49
    )
    @Range(min = 0, max = 99)
    default int woodcutting_level()
    {
        return 0;
    }

}
