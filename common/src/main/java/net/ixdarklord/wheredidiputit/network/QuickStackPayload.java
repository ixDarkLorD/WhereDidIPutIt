package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: put my inventory's items into the nearby containers that already hold them. The server decides
 * what moves: which containers, how far, and whether it allows it at all.
 *
 * @param requestId matched by the answer
 */
public record QuickStackPayload(int requestId) implements CustomPacketPayload {
    public static final Type<QuickStackPayload> TYPE = new Type<>(WhereDidIPutIt.id("quick_stack"));

    @Override
    public Type<QuickStackPayload> type() {
        return TYPE;
    }
}
