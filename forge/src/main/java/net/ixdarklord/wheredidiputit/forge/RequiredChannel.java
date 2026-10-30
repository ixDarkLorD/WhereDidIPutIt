package net.ixdarklord.wheredidiputit.forge;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraftforge.network.ChannelBuilder;

/**
 * Makes Forge require the mod on both sides (author, 2026-10-22). CoolCatLib's own channels accept a side without them,
 * so a side without the mod would be let in; this one channel doesn't, so Forge's own network check turns away a client
 * without the mod from a server with it, and the other way round, with its own message. Nothing is ever sent on it.
 */
public final class RequiredChannel {
    private static final int VERSION = 1;

    private RequiredChannel() {
    }

    public static void register() {
        ChannelBuilder.named(WhereDidIPutIt.id("required")).networkProtocolVersion(VERSION).simpleChannel();
    }
}
