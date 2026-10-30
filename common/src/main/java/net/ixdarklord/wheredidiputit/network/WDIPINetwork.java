package net.ixdarklord.wheredidiputit.network;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.find.LiveSearch;
import net.ixdarklord.wheredidiputit.find.QuickStack;
import net.ixdarklord.wheredidiputit.live.LiveSearchServer;
import net.ixdarklord.wheredidiputit.live.QuickStackServer;
import net.ixdarklord.wheredidiputit.live.TeamSharingServer;
import net.ixdarklord.wheredidiputit.memory.TeamMemory;
import net.minecraft.server.level.ServerPlayer;

/**
 * The mod's payloads, registered from the common constructor on both sides. Clientbound handlers are lambdas into
 * client classes, so a dedicated server never loads them.
 */
public final class WDIPINetwork {
    private WDIPINetwork() {
    }

    public static void register() {
        Network.registerClientbound(HelloPayload.class, (payload, context) -> context.queue(() -> LiveSearch.onHello(payload)));
        Network.registerClientbound(FindResultPayload.class, (payload, context) -> context.queue(() -> LiveSearch.onResult(payload)));
        Network.registerServerbound(FindRequestPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) LiveSearchServer.onRequest(player, payload);
        }));
        Network.registerClientbound(SharedContainerPayload.class, (payload, context) -> context.queue(() -> TeamMemory.onShared(payload)));
        Network.registerServerbound(ShareContainerPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) TeamSharingServer.onShare(player, payload);
        }));
        Network.registerClientbound(QuickStackResultPayload.class, (payload, context) -> context.queue(() -> QuickStack.onResult(payload)));
        Network.registerServerbound(QuickStackPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) QuickStackServer.onRequest(player, payload);
        }));
    }
}
