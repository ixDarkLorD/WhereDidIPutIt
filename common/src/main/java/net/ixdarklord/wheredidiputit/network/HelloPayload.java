package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client, on joining: the server has the mod and can answer live searches. Whether it allows them, and how
 * far and how often, the client reads from the server config CoolCatLib syncs. A client never told this stays in
 * memory mode.
 *
 * @param protocol the live search protocol the server speaks
 */
public record HelloPayload(int protocol) implements CustomPacketPayload {
    public static final int PROTOCOL = 1;
    public static final Type<HelloPayload> TYPE = new Type<>(WhereDidIPutIt.id("hello"));

    @Override
    public Type<HelloPayload> type() {
        return TYPE;
    }
}
