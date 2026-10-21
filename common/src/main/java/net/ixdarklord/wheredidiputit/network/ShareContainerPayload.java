package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: I just closed this container, for my team to know (team memory, only when the server allows it).
 * The server checks the player was there and may interact with it before passing it on, and passes it only to their
 * team.
 */
public record ShareContainerPayload(SharedContainer container) implements CustomPacketPayload {
    public static final Type<ShareContainerPayload> TYPE = new Type<>(WhereDidIPutIt.id("share_container"));

    @Override
    public Type<ShareContainerPayload> type() {
        return TYPE;
    }
}
