package net.ixdarklord.wheredidiputit.config;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;

/**
 * What a server with the mod allows. CoolCatLib syncs it to every client that joins and whenever it changes. A client
 * on a server without the mod never uses these: it stays in memory mode.
 */
public final class WDIPIServerConfig {
    private static final ConfigBuilder BUILDER = Config.builder(WhereDidIPutIt.MOD_ID, ConfigScope.SERVER)
            .comment("Where Did I Put It? server settings. Live search lets players find items in containers they haven't opened since they changed.");

    static {
        BUILDER.push("liveSearch", "Searching the real container contents on the server.");
    }
    public static final ConfigValue<Boolean> ALLOW_LIVE_SEARCH = BUILDER.bool("allowLiveSearch", true)
            .comment("Whether players with the mod may ask the server to search the containers around them. Off, they use what they remember.").build();
    public static final ConfigValue<Integer> LIVE_SEARCH_RADIUS = BUILDER.intValue("liveSearchRadius", 32)
            .range(8, 128).slider().comment("How far, in blocks, the server searches around the player.").enabledWhen(ALLOW_LIVE_SEARCH).build();
    public static final ConfigValue<Integer> REQUEST_COOLDOWN = BUILDER.intValue("requestCooldown", 2)
            .range(0, 60).comment("Seconds a player waits between live searches.").enabledWhen(ALLOW_LIVE_SEARCH).build();
    public static final ConfigValue<Boolean> INCLUDE_SHULKER_CONTENTS = BUILDER.bool("includeShulkerContents", true)
            .comment("Whether live search also looks inside shulker boxes and bundles stored in containers.").enabledWhen(ALLOW_LIVE_SEARCH).build();
    public static final ConfigValue<Boolean> INCLUDE_STORAGE_NETWORKS = BUILDER.bool("includeStorageNetworks", true)
            .comment("Whether live search also looks into storage networks (Applied Energistics 2, Refined Storage, Simple Storage Network, Tom's Simple Storage) through their terminals and controllers nearby.").enabledWhen(ALLOW_LIVE_SEARCH).build();
    static {
        BUILDER.pop();
        BUILDER.push("sharing", "Sharing what players have seen.");
    }
    public static final ConfigValue<Boolean> SHARE_TEAM_MEMORY = BUILDER.bool("shareTeamMemory", false)
            .comment("Players on the same team share what they have seen in containers: each container one of them opens is remembered by their teammates who are online. Off, nobody learns what another player saw.").build();
    static {
        BUILDER.pop();
        BUILDER.push("quickStack", "Putting items from a player's inventory into nearby containers that already hold them.");
    }
    public static final ConfigValue<Boolean> ALLOW_QUICK_STACK = BUILDER.bool("allowQuickStack", true)
            .comment("Whether players may quick stack: their inventory's items (not the hotbar) go into the nearby containers that already hold the same item.").build();
    public static final ConfigValue<Integer> QUICK_STACK_RADIUS = BUILDER.intValue("quickStackRadius", 8)
            .range(2, 16).slider().comment("How far, in blocks, quick stack reaches round the player.").enabledWhen(ALLOW_QUICK_STACK).build();
    static {
        BUILDER.pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private WDIPIServerConfig() {
    }
}
