package net.ixdarklord.wheredidiputit.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;

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
    static {
        BUILDER.pop();
        BUILDER.push("memory", "What is remembered about the containers you open.");
    }
    public static final ConfigValue<Boolean> LAST_SEEN_TOOLTIP = BUILDER.bool("lastSeenTooltip", true)
            .comment("Holding the tooltip key on an item shows where you last saw it.").enabledWhen(ENABLED).build();
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
