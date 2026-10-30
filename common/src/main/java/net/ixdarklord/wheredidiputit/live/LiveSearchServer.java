package net.ixdarklord.wheredidiputit.live;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.find.ItemMatcher;
import net.ixdarklord.wheredidiputit.memory.ContainerKeys;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.ixdarklord.wheredidiputit.network.FindRequestPayload;
import net.ixdarklord.wheredidiputit.network.FindResultPayload;
import net.ixdarklord.wheredidiputit.network.HelloPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Answers live searches: the real contents of the containers round a player, so results are never out of date. The
 * server decides everything that matters: whether it is allowed, how far (its own radius, whatever the client wants),
 * how often (a cooldown per player), and which containers (loaded chunks only, ones the player may interact with,
 * not locked against them, not unlooted, not tagged {@code wheredidiputit:live_search_excluded}). It answers with
 * positions and counts of the matching item only, never a container's contents.
 */
public final class LiveSearchServer {
    public static final TagKey<Block> EXCLUDED = TagKey.create(Registries.BLOCK, WhereDidIPutIt.id("live_search_excluded"));
    /** More containers than this and only the nearest are answered. */
    private static final int MAX_HITS = 256;

    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private LiveSearchServer() {
    }

    public static void init() {
        PlayerEvents.JOIN.register(player -> {
            // Only to clients with the mod: a client without it doesn't know the payload.
            if (Network.canPlayerReceive(player, HelloPayload.TYPE)) Network.sendToPlayer(player, new HelloPayload(HelloPayload.PROTOCOL));
        });
        PlayerEvents.LEAVE.register(player -> LAST_REQUEST.remove(player.getUUID()));
    }

    public static void onRequest(ServerPlayer player, FindRequestPayload request) {
        if (!request.withinLimits() || request.query().isEmpty()) return;
        int radius = WDIPIServerConfig.LIVE_SEARCH_RADIUS.get();
        if (!WDIPIServerConfig.ALLOW_LIVE_SEARCH.get()) {
            reply(player, request, FindResultPayload.Status.DISABLED, radius, List.of());
            return;
        }
        long now = Util.getMillis();
        Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < WDIPIServerConfig.REQUEST_COOLDOWN.get() * 1000L) {
            reply(player, request, FindResultPayload.Status.COOLDOWN, radius, List.of());
            return;
        }
        LAST_REQUEST.put(player.getUUID(), now);
        boolean nested = request.nested() && WDIPIServerConfig.INCLUDE_SHULKER_CONTENTS.get();
        ItemMatcher matcher = ItemMatcher.of(request.query(), request.mode(), nested, request.ignoredTags());
        reply(player, request, FindResultPayload.Status.OK, radius, search(player.level(), player.getEyePosition(), matcher, radius, player));
    }

    private static void reply(ServerPlayer player, FindRequestPayload request, FindResultPayload.Status status, int radius, List<FindResultPayload.Hit> hits) {
        Network.sendToPlayer(player, new FindResultPayload(request.requestId(), status, radius, hits));
    }

    /**
     * The containers within {@code radius} of {@code eyes} in loaded chunks holding a matching item, nearest first, at
     * most {@link #MAX_HITS}. With a {@code player}, only those they could open themselves; without one (an operator's
     * command), all but unlooted loot chests.
     */
    public static List<FindResultPayload.Hit> search(ServerLevel level, Vec3 eyes, ItemMatcher matcher, int radius, @Nullable ServerPlayer player) {
        BlockPos center = BlockPos.containing(eyes);
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + radius);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
        // Both halves of a double chest add up under one key, so stacks are gathered per key first.
        Map<BlockPos, List<StoredItem>> contents = new LinkedHashMap<>();
        // A storage network reached through several blocks is read once, at the access point nearest the player.
        Map<Object, NetworkSite> networks = WDIPIServerConfig.INCLUDE_STORAGE_NETWORKS.get() && NetworkReaders.any() ? new IdentityHashMap<>() : null;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                // Loaded chunks only: a search never loads or generates the world.
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    double distance = Vec3.atCenterOf(pos).distanceTo(eyes);
                    if (distance > radius) continue;
                    NetworkReaders.Access access = networks != null ? NetworkReaders.of(blockEntity) : null;
                    if (access != null) {
                        if (reachable(player, level, blockEntity)) {
                            NetworkSite known = networks.get(access.network());
                            if (known == null || distance < known.distance) networks.put(access.network(), new NetworkSite(access, blockEntity, distance));
                        }
                        continue;
                    }
                    if (!searchable(player, level, blockEntity)) continue;
                    BlockPos key = ContainerKeys.keyOf(level, pos);
                    if (key == null) continue;
                    List<StoredItem> items = contents.computeIfAbsent(key, k -> new ArrayList<>());
                    Container container = (Container) blockEntity;
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        if (!stack.isEmpty()) items.add(StoredItem.of(stack));
                    }
                }
            }
        }
        if (networks != null) networks.values().forEach(site -> contents.put(site.accessPoint.getBlockPos(), site.access.contents(site.accessPoint)));
        List<FindResultPayload.Hit> hits = new ArrayList<>();
        contents.forEach((pos, items) -> {
            long count = matcher.count(items);
            if (count > 0) hits.add(new FindResultPayload.Hit(pos, BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()), count, matcher.most(items)));
        });
        hits.sort(Comparator.comparingDouble(hit -> Vec3.atCenterOf(hit.pos()).distanceTo(eyes)));
        return hits.size() > MAX_HITS ? List.copyOf(hits.subList(0, MAX_HITS)) : hits;
    }

    // A storage network's access point, as near the player as any of its blocks found.
    private record NetworkSite(NetworkReaders.Access access, BlockEntity accessPoint, double distance) {
    }

    // Whether the player could use this access point themselves: the same protection as for containers.
    private static boolean reachable(@Nullable ServerPlayer player, ServerLevel level, BlockEntity blockEntity) {
        if (player == null) return true;
        return !blockEntity.getBlockState().is(EXCLUDED) && player.mayInteract(level, blockEntity.getBlockPos());
    }

    // Whether this block entity is a container the player could look into themselves (any container for no player).
    static boolean searchable(@Nullable ServerPlayer player, ServerLevel level, BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container)) return false;
        // Reading an unlooted chest would roll its loot, and tell the player what it holds before they open it.
        if (blockEntity instanceof RandomizableContainer randomizable && randomizable.getLootTable() != null) return false;
        if (player == null) return true;
        if (blockEntity instanceof BaseContainerBlockEntity base && !base.canOpen(player)) return false;
        if (blockEntity.getBlockState().is(EXCLUDED)) return false;
        // Spawn protection and the world border; protection mods that hook it are respected too.
        return player.mayInteract(level, blockEntity.getBlockPos());
    }
}
