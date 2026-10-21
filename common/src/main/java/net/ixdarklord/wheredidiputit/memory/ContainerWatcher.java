package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Notices the player opening a container and records what it holds when they close it, so their own changes count; a
 * storage mod's terminal is recorded with everything its network showed ({@link StorageTerminals}).
 * Which block was opened comes from the last block the player used ({@code MultiPlayerGameModeMixin}): a container
 * screen opening shortly after using a block with a block entity is that block's.
 */
public final class ContainerWatcher {
    /** How long after using a block a container screen still counts as that block's (the server's reply can lag). */
    private static final long USE_WINDOW_MILLIS = 2000;

    private static @Nullable BlockPos usedPos;
    private static @Nullable ResourceKey<Level> usedDimension;
    private static long usedAt;

    private static @Nullable AbstractContainerScreen<?> watched;
    private static @Nullable BlockPos watchedPos;
    private static @Nullable ResourceKey<Level> watchedDimension;

    private ContainerWatcher() {
    }

    /** The player used a block (right-clicked it); called from {@code MultiPlayerGameModeMixin}. */
    public static void onUseBlock(Level level, BlockPos pos) {
        usedPos = pos.immutable();
        usedDimension = level.dimension();
        usedAt = Util.getMillis();
    }

    static void tick(Minecraft minecraft) {
        if (watched != null && minecraft.screen != watched) stopWatching(minecraft);
        if (watched == null && minecraft.screen instanceof AbstractContainerScreen<?> screen && !isPlayerInventory(screen)) {
            startWatching(minecraft, screen);
        }
    }

    /** Records the container still open, if any; called when leaving the world. */
    static void flush(Minecraft minecraft) {
        if (watched != null) stopWatching(minecraft);
        usedPos = null;
    }

    private static void startWatching(Minecraft minecraft, AbstractContainerScreen<?> screen) {
        BlockPos pos = usedPos;
        usedPos = null;
        if (pos == null || minecraft.level == null || Util.getMillis() - usedAt > USE_WINDOW_MILLIS) return;
        if (minecraft.level.dimension() != usedDimension) return;
        BlockPos key = ContainerKeys.keyOf(minecraft.level, pos);
        if (key == null) return;
        watched = screen;
        watchedPos = key;
        watchedDimension = usedDimension;
    }

    private static void stopWatching(Minecraft minecraft) {
        AbstractContainerScreen<?> screen = watched;
        BlockPos pos = watchedPos;
        ResourceKey<Level> dimension = watchedDimension;
        watched = null;
        watchedPos = null;
        watchedDimension = null;
        if (screen == null || pos == null || minecraft.player == null || minecraft.level == null) return;
        if (minecraft.level.dimension() != dimension || !WDIPIClientConfig.ENABLED.get()) return;
        // Broken while it was open: nothing to remember, and the old record goes with the block.
        if (ContainerKeys.keyOf(minecraft.level, pos) == null) return;

        boolean network = StorageTerminals.of(screen) != null;
        List<StoredItem> items = contents(screen, minecraft.player.getInventory());
        // A screen with only the player's own slots stores nothing (a block that opens some other menu).
        if (items == null) return;

        BlockState state = minecraft.level.getBlockState(pos);
        Component title = screen.getTitle();
        Component label = title.getString().isBlank() ? null : title.copy();
        ContainerRecord record = new ContainerRecord(pos, BuiltInRegistries.BLOCK.getKey(state.getBlock()), label, System.currentTimeMillis(), List.copyOf(items), network);
        ContainerMemory.record(dimension, record);
    }

    /** What the screen shows: a storage terminal's whole network, else its slots that aren't the player's; null if it has none. */
    private static @Nullable List<StoredItem> contents(AbstractContainerScreen<?> screen, Inventory inventory) {
        StorageTerminals.Terminal terminal = StorageTerminals.of(screen);
        if (terminal != null) {
            try {
                return terminal.contents(screen);
            } catch (RuntimeException | LinkageError e) {
                WhereDidIPutIt.LOGGER.warn("Couldn't read the storage terminal {}", screen.getClass().getName(), e);
                return null;
            }
        }
        List<StoredItem> items = new ArrayList<>();
        boolean hasContainerSlots = false;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory) continue;
            hasContainerSlots = true;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) items.add(StoredItem.of(stack));
        }
        return hasContainerSlots ? items : null;
    }

    private static boolean isPlayerInventory(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
}
