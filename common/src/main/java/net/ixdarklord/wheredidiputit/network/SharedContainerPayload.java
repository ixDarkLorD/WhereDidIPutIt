package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to client: a teammate saw this container (team memory). Sent only while the server allows it. */
public record SharedContainerPayload(String from, SharedContainer container) implements CustomPacketPayload {
    public static final Type<SharedContainerPayload> TYPE = new Type<>(WhereDidIPutIt.id("shared_container"));

    @Override
    public Type<SharedContainerPayload> type() {
        return TYPE;
    }
}
