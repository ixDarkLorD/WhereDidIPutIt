package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.network.FindRequestPayload;
import net.ixdarklord.wheredidiputit.network.FindResultPayload;
import net.ixdarklord.wheredidiputit.network.HelloPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The client's half of live search: a server with the mod says hello on joining, and while it allows live search (its
 * synced config) the find keys ask it instead of only remembering. An answer that is refused (disabled, too soon) or
 * never comes falls back to memory, so a press always gets an answer. A press within the server's cooldown waits for it
 * to end before asking, rather than being refused and answered from memory alone.
 */
public final class LiveSearch {
    /** How long to wait for the server before answering from memory. */
    private static final long TIMEOUT_MILLIS = 1500L;
    /** Added to the server's cooldown so a request sent as it ends isn't refused for being a tick early. */
    private static final long COOLDOWN_MARGIN_MILLIS = 100L;

    private static boolean serverHasMod;
    private static int nextId;
    private static long lastSentAt;
    private static @Nullable Pending pending;

    private LiveSearch() {
    }

    public static void init() {
        ClientPlayerEvents.LEAVE.register(player -> {
            serverHasMod = false;
            pending = null;
            lastSentAt = 0L;
        });
        ClientTickEvents.END.register(minecraft -> {
            Pending waiting = pending;
            if (waiting == null) return;
            long now = Util.getMillis();
            if (!waiting.sent) {
                if (now >= waiting.sendAt) send(waiting);
            } else if (now - waiting.sendAt > TIMEOUT_MILLIS) {
                pending = null;
                FindKey.findInMemory(minecraft, waiting.query, waiting.all);
            }
        });
    }

    public static void onHello(HelloPayload payload) {
        serverHasMod = payload.protocol() == HelloPayload.PROTOCOL;
    }

    /** Whether a search should ask the server: it has the mod and allows live search. */
    public static boolean usable() {
        return serverHasMod && WDIPIServerConfig.ALLOW_LIVE_SEARCH.get();
    }

    /** Asks the server; the answer, or the fall back to memory, comes later. A newer request replaces a waiting one. */
    public static void request(ItemStack query, boolean all) {
        long now = Util.getMillis();
        long cooldownEnds = lastSentAt == 0L ? now
                : lastSentAt + WDIPIServerConfig.REQUEST_COOLDOWN.get() * 1000L + COOLDOWN_MARGIN_MILLIS;
        Pending request = new Pending(++nextId, query.copy(), all, Math.max(now, cooldownEnds));
        pending = request;
        if (request.sendAt <= now) send(request);
    }

    private static void send(Pending request) {
        request.sent = true;
        request.sendAt = Util.getMillis();
        lastSentAt = request.sendAt;
        Network.sendToServer(new FindRequestPayload(request.id, request.query.copyWithCount(1), WDIPIClientConfig.MATCH_MODE.get(),
                WDIPIClientConfig.INDEX_NESTED_CONTENTS.get(), WDIPIClientConfig.IGNORED_TAGS.get()));
    }

    public static void onResult(FindResultPayload payload) {
        Pending waiting = pending;
        // An answer to a request given up on, or replaced since, is dropped.
        if (waiting == null || !waiting.sent || waiting.id != payload.requestId()) return;
        pending = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.status() == FindResultPayload.Status.OK) {
            FindKey.findWithLive(minecraft, waiting.query, waiting.all, payload);
        } else {
            FindKey.findInMemory(minecraft, waiting.query, waiting.all);
        }
    }

    // A request waiting for the server's cooldown to end (not sent yet) or for its answer; sendAt is when it goes or went.
    private static final class Pending {
        private final int id;
        private final ItemStack query;
        private final boolean all;
        private long sendAt;
        private boolean sent;

        private Pending(int id, ItemStack query, boolean all, long sendAt) {
            this.id = id;
            this.query = query;
            this.all = all;
            this.sendAt = sendAt;
        }
    }
}
