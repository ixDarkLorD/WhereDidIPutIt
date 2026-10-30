package net.ixdarklord.wheredidiputit.forge;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraftforge.network.NetworkRegistry;

/**
 * Makes Forge require the mod on both sides (author, 2026-10-22). CoolCatLib's own channels accept a side without them,
 * so a side without the mod would be let in; this one channel only accepts its own version (a side without the mod
 * reports it absent), so Forge's own network check turns either side away, with its own message. Nothing is ever sent
 * on it.
 */
public final class RequiredChannel {
    private static final String VERSION = "1";

    private RequiredChannel() {
    }

    public static void register() {
        NetworkRegistry.newSimpleChannel(WhereDidIPutIt.id("required"), () -> VERSION, VERSION::equals, VERSION::equals);
    }
}
