package net.ixdarklord.wheredidiputit.live;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerKeys;
import net.ixdarklord.wheredidiputit.network.QuickStackPayload;
import net.ixdarklord.wheredidiputit.network.QuickStackResultPayload;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quick stack, on the server (author, 2026-10-28): the player's main inventory (not the hotbar, the armour or the
 * offhand) goes into the containers round them that already hold the same item, as Terraria's quick stack. Only
 * containers they could open themselves are used, by the same rules as live search ({@link LiveSearchServer}): loaded
 * chunks, no unlooted loot chests, nothing locked, protected or tagged excluded. Each stack fills the nearest such
 * container's partial stacks first, then its empty slots, then the next one.
 */
public final class QuickStackServer {
    /** The main inventory's slots past the hotbar. */
    private static final int FIRST_SLOT = Inventory.getSelectionSize();
    private static final int LAST_SLOT = Inventory.INVENTORY_SIZE;
    private static final long COOLDOWN_MILLIS = 1000L;

    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();

    private QuickStackServer() {
    }

    public static void init() {
        PlayerEvents.LEAVE.register(player -> LAST_REQUEST.remove(player.getUUID()));
    }

    public static void onRequest(ServerPlayer player, QuickStackPayload request) {
        if (!WDIPIServerConfig.ALLOW_QUICK_STACK.get()) {
            reply(player, request, QuickStackResultPayload.Status.DISABLED, List.of());
            return;
        }
        long now = Util.getMillis();
        Long last = LAST_REQUEST.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_MILLIS) {
            reply(player, request, QuickStackResultPayload.Status.COOLDOWN, List.of());
            return;
        }
        LAST_REQUEST.put(player.getUUID(), now);
        reply(player, request, QuickStackResultPayload.Status.OK, stack(player, WDIPIServerConfig.QUICK_STACK_RADIUS.get()));
    }

    private static void reply(ServerPlayer player, QuickStackPayload request, QuickStackResultPayload.Status status, List<QuickStackResultPayload.Moved> moved) {
        Network.sendToPlayer(player, new QuickStackResultPayload(request.requestId(), status, moved));
    }

    private static List<QuickStackResultPayload.Moved> stack(ServerPlayer player, int radius) {
        ServerLevel level = player.serverLevel();
        Vec3 eyes = player.getEyePosition();
        List<Target> targets = containers(player, level, eyes, radius);
        // What went where, added up per container and item.
        Map<BlockPos, Map<ItemStack, Integer>> moved = new LinkedHashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = FIRST_SLOT; slot < LAST_SLOT; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            // Taken before inserting: a stack moved whole is left empty, and a copy of it would be air.
            ItemStack one = stack.copyWithCount(1);
            for (Target target : targets) {
                if (!holds(target.container, stack)) continue;
                int before = stack.getCount();
                insert(target.container, stack);
                int put = before - stack.getCount();
                if (put > 0) {
                    target.container.setChanged();
                    Map<ItemStack, Integer> here = moved.computeIfAbsent(target.key, k -> new LinkedHashMap<>());
                    ItemStack kind = here.keySet().stream().filter(known -> ItemStack.isSameItemSameComponents(known, one)).findFirst().orElse(one);
                    here.merge(kind, put, Integer::sum);
                }
                if (stack.isEmpty()) break;
            }
            inventory.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
        if (!moved.isEmpty()) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        List<QuickStackResultPayload.Moved> result = new ArrayList<>();
        moved.forEach((pos, items) -> items.forEach((item, count) -> result.add(new QuickStackResultPayload.Moved(pos, item, count))));
        return result;
    }

    // The containers within reach the player could open, nearest first.
    private static List<Target> containers(ServerPlayer player, ServerLevel level, Vec3 eyes, int radius) {
        BlockPos center = BlockPos.containing(eyes);
        List<Target> targets = new ArrayList<>();
        for (int chunkX = SectionPos.blockToSectionCoord(center.getX() - radius); chunkX <= SectionPos.blockToSectionCoord(center.getX() + radius); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius); chunkZ <= SectionPos.blockToSectionCoord(center.getZ() + radius); chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    double distance = Vec3.atCenterOf(blockEntity.getBlockPos()).distanceTo(eyes);
                    if (distance > radius || !LiveSearchServer.searchable(player, level, blockEntity)) continue;
                    BlockPos key = ContainerKeys.keyOf(level, blockEntity.getBlockPos());
                    if (key != null) targets.add(new Target((Container) blockEntity, key, distance));
                }
            }
        }
        targets.sort(Comparator.comparingDouble(Target::distance));
        return targets;
    }

    // Whether the container already holds some of the item: quick stack only adds to what is there.
    private static boolean holds(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (ItemStack.isSameItemSameComponents(container.getItem(slot), stack)) return true;
        }
        return false;
    }

    // Moves as much of the stack as fits, topping up partial stacks before using empty slots; shrinks the stack.
    private static void insert(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack there = container.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(there, stack) || !container.canPlaceItem(slot, stack)) continue;
            int room = Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize()) - there.getCount();
            if (room <= 0) continue;
            int put = Math.min(room, stack.getCount());
            there.grow(put);
            stack.shrink(put);
            container.setItem(slot, there);
        }
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack)) continue;
            int put = Math.min(Math.min(container.getMaxStackSize(stack), stack.getMaxStackSize()), stack.getCount());
            container.setItem(slot, stack.split(put));
        }
    }

    // A container in reach, and the key its moves are reported under (a double chest's lower half).
    private record Target(Container container, BlockPos key, double distance) {
    }
}
