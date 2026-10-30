package net.ixdarklord.wheredidiputit.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;

import java.util.List;

/** Each player's own preferences. */
public final class WDIPIClientConfig {
    private static final ConfigBuilder BUILDER = Config.builder(WhereDidIPutIt.MOD_ID, ConfigScope.CLIENT)
            .comment("Where Did I Put It? client settings.");

    public static final ConfigValue<Boolean> ENABLED = BUILDER.bool("enabled", true)
            .comment("Off, containers are not remembered and the find key does nothing.").build();
    static {
        BUILDER.push("search", "Finding an item with the find key.");
    }
    public static final ConfigValue<Boolean> CLOSE_SCREEN_ON_SEARCH = BUILDER.bool("closeScreenOnSearch", true)
            .comment("Closes the open screen when you search, so you can see where the item is.").enabledWhen(ENABLED).build();
    public static final ConfigValue<MatchMode> MATCH_MODE = BUILDER.enumValue("matchMode", MatchMode.IGNORE_DATA)
            .comment("When a stored item counts as the one you search for: the exact item, the same item whatever its data (enchantments, name...), or any item sharing its tag (any log).").enabledWhen(ENABLED).build();
    public static final ConfigValue<List<String>> IGNORED_TAGS = BUILDER.stringList("ignoredTags", List.of(
                    "beacon_payment_items", "trim_materials", "trimmable_armor", "*_tool_materials", "repairs_*", "completes_find_tree_tutorial",
                    "*_food", "*_tempt_items", "*_taming_items", "*_preferred_weapons", "*_disliked_weapons", "piglin_*", "ignored_by_piglin_babies",
                    "villager_*", "*_dyes", "*_fuel", "*_books", "book_cloning_target", "noteblock_top_instruments", "decorated_pot_ingredients",
                    "breaks_decorated_pots", "cluster_max_harvestables", "creeper_*", "dampens_vibrations", "duplicates_allays", "*_equipment",
                    "*_wearables", "shearable_from_copper_golem", "smelts_to_glass", "soul_fire_base_blocks", "stone_crafting_materials",
                    "non_flammable_wood", "cauldron_can_remove_dye", "panda_eats_from_ground", "c:hidden_from_recipe_viewers"))
            .comment("Item tags the item tag match mode leaves out: they say what an item is used for, not what it is (beacon payments, tool materials, animal food...), so they would match unrelated items. A name without a namespace matches it in any namespace; * matches any part of a name.")
            .enabledWhen(MATCH_MODE, mode -> mode == MatchMode.ITEM_TAG).build();
    public static final ConfigValue<Integer> MAX_SEARCH_DISTANCE = BUILDER.intValue("maxSearchDistance", 64)
            .range(8, 512).comment("How far away, in blocks, a remembered container can be found.").enabledWhen(ENABLED).build();
    static {
        BUILDER.pop();
        BUILDER.push("highlight", "Pointing out the container that holds the item.");
    }
    public static final ConfigValue<Integer> HIGHLIGHT_DURATION = BUILDER.intValue("highlightDuration", 5)
            .range(1, 30).slider().comment("Seconds the container glows.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Integer> HIGHLIGHT_COLOR = BUILDER.color("highlightColor", 0xFFAA00)
            .comment("The color of the glowing outline.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> OFF_SCREEN_ARROW = BUILDER.bool("offScreenArrow", true)
            .comment("Draws an arrow around the crosshair pointing to the container when it is off-screen.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> ITEM_MARKER = BUILDER.bool("itemMarker", true)
            .comment("Shows the item you searched for over each highlighted container, with how many of it are there.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> GHOST = BUILDER.bool("ghost", true)
            .comment("Shows a see-through ghost of a highlighted container where walls hide it.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> PATH_LINE = BUILDER.bool("pathLine", true)
            .comment("After finding one container, draws a line from you to it, until you open it.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> COMPASS = BUILDER.bool("compass", true)
            .comment("After finding one container, keeps a small compass at the top of the screen pointing to it, until you open it.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Integer> COMPASS_X = BUILDER.intValue("compassX", 50)
            .range(0, 100).slider().comment("Where the compass sits across the screen, in percent of its width from the left (50 is the middle).").enabledWhen(COMPASS).build();
    public static final ConfigValue<Integer> COMPASS_Y = BUILDER.intValue("compassY", 6)
            .range(0, 200).slider().comment("How far down from the top of the screen the compass sits. It moves below Jade's box when that is in the way.").enabledWhen(COMPASS).build();
    public static final ConfigValue<Boolean> SOUND = BUILDER.bool("sound", true)
            .comment("Plays a soft chime from the direction of each container found.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> SHOW_AGE = BUILDER.bool("showAge", true)
            .comment("Shows how long ago a remembered container was last seen under its item marker, as what it holds may have changed since.").enabledWhen(ITEM_MARKER).build();
    static {
        BUILDER.pop();
        BUILDER.push("memory", "What is remembered about the containers you open.");
    }
    public static final ConfigValue<Boolean> LAST_SEEN_TOOLTIP = BUILDER.bool("lastSeenTooltip", true)
            .comment("Holding the tooltip key on an item shows where you last saw it.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> SORTING_HINTS = BUILDER.bool("sortingHints", true)
            .comment("With a container open, an item's tooltip says when a container nearby holds more of it, to keep like with like.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Boolean> INDEX_NESTED_CONTENTS = BUILDER.bool("indexNestedContents", true)
            .comment("Also remembers what is inside shulker boxes and bundles stored in containers.").enabledWhen(ENABLED).build();
    public static final ConfigValue<Integer> FORGET_AFTER_DAYS = BUILDER.intValue("forgetAfterDays", 0)
            .range(0, 3650).comment("Forgets a container not opened for this many days. 0 never forgets.").enabledWhen(ENABLED).build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WDIPIClientConfig() {
    }
}
