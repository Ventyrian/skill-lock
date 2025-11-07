package com.skilllock;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("skilllock")
public interface SkillLockConfig extends Config
{
    @ConfigItem(
            keyName = "attack",
            name = "Attack",
            description = ""
    )
    default boolean attack() { return false;}

    @ConfigItem(
            keyName = "hitpoints",
            name = "Hitpoints",
            description = ""
    )
    default boolean hitpoints() { return false;}

    @ConfigItem(
            keyName = "mining",
            name = "Mining",
            description = ""
    )
    default boolean mining() { return false;}

    @ConfigItem(
            keyName = "strength",
            name = "Strength",
            description = ""
    )
    default boolean strength() { return false;}

    @ConfigItem(
            keyName = "agility",
            name = "Agility",
            description = ""
    )
    default boolean agility() { return false;}

    @ConfigItem(
            keyName = "smithing",
            name = "Smithing",
            description = ""
    )
    default boolean smithing() { return false;}

    @ConfigItem(
            keyName = "defense",
            name = "Defense",
            description = ""
    )
    default boolean defense() { return false;}

    @ConfigItem(
            keyName = "herblore",
            name = "Herblore",
            description = ""
    )
    default boolean herblore() { return false;}

    @ConfigItem(
            keyName = "fishing",
            name = "Fishing",
            description = ""
    )
    default boolean fishing() { return false;}

    @ConfigItem(
            keyName = "ranged",
            name = "Ranged",
            description = ""
    )
    default boolean ranged() { return false;}

    @ConfigItem(
            keyName = "thieving",
            name = "Thieving",
            description = ""
    )
    default boolean thieving() { return false;}

    @ConfigItem(
            keyName = "cooking",
            name = "Cooking",
            description = ""
    )
    default boolean cooking() { return false;}

    @ConfigItem(
            keyName = "prayer",
            name = "Prayer",
            description = ""
    )
    default boolean prayer() { return false;}

    @ConfigItem(
            keyName = "crafting",
            name = "Crafting",
            description = ""
    )
    default boolean crafting() { return false;}

    @ConfigItem(
            keyName = "firemaking",
            name = "Firemaking",
            description = ""
    )
    default boolean firemaking() { return false;}

    @ConfigItem(
            keyName = "magic",
            name = "Magic",
            description = ""
    )
    default boolean magic() { return false;}

    @ConfigItem(
            keyName = "fletching",
            name = "Fletching",
            description = ""
    )
    default boolean fletching() { return false;}

    @ConfigItem(
            keyName = "woodcutting",
            name = "Woodcutting",
            description = ""
    )
    default boolean woodcutting() { return false;}

    @ConfigItem(
            keyName = "runecrafting",
            name = "Runecrafting",
            description = ""
    )
    default boolean runecrafting() { return false;}

    @ConfigItem(
            keyName = "slayer",
            name = "Slayer",
            description = ""
    )
    default boolean slayer() { return false;}

    @ConfigItem(
            keyName = "farming",
            name = "Farming",
            description = ""
    )
    default boolean farming() { return false;}

    @ConfigItem(
            keyName = "construction",
            name = "Construction",
            description = ""
    )
    default boolean construction() { return false;}

    @ConfigItem(
            keyName = "hunter",
            name = "Hunter",
            description = ""
    )
    default boolean hunter() { return false;}

    @ConfigItem(
            keyName = "sailing",
            name = "Sailing",
            description = ""
    )
    default boolean sailing() { return true;}

}
