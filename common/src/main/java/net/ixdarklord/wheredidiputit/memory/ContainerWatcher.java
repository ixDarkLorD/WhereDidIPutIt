package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.Guidance;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Notices the player opening a container and records what it holds when they close it, so their own changes count; a
 * storage mod's terminal is recorded with everything its network showed ({@link StorageTerminals}).
 * Which block was opened comes from the last block the player used ({@code MultiPlayerGameModeMixin}): a container
 * screen opening shortly after using a block with a block entity is that block's. Entities work the same (a chest
 * boat, a minecart with a chest, a donkey with one: right-clicked, or ridden when the inventory key opens theirs), and
 * are remembered by their UUID.
 */
public final class ContainerWatcher {
    /** How long after using a block a container screen still counts as that block's (the server's reply can lag). */
    private static final long USE_WINDOW_MILLIS = 2000;

    private static @Nullable BlockPos usedPos;
    private static @Nullable UUID usedEntity;
    private static @Nullable ResourceKey<Level> usedDimension;
    private static long usedAt;

    private static @Nullable AbstractContainerScreen<?> watched;
    private static @Nullable BlockPos watchedPos;
    private static @Nullable UUID watchedEntity;
    private static @Nullable ResourceKey<Level> watchedDimension;
    // Whether the container open now is left out of searches: kept when it is recorded again on closing.
    private static boolean watchedIgnored;

    private ContainerWatcher() {
    }

    /** The player used a block (right-clicked it); called from {@code MultiPlayerGameModeMixin}. */
    public static void onUseBlock(Level level, BlockPos pos) {
        usedPos = pos.immutable();
        usedEntity = null;
        usedDimension = level.dimension();
        usedAt = Util.getMillis();
    }

    /** The player used an entity (right-clicked it); called from {@code MultiPlayerGameModeMixin}. */
    public static void onUseEntity(Entity entity) {
        if (!isContainer(entity)) return;
        usedEntity = entity.getUUID();
        usedPos = null;
        usedDimension = entity.level().dimension();
        usedAt = Util.getMillis();
    }

    /** Whether an entity holds things of its own: chest boats, minecarts with a chest or hopper, and donkeys, mules and llamas with a chest. */
    public static boolean isContainer(Entity entity) {
        return entity instanceof ContainerEntity || entity instanceof AbstractChestedHorse horse && horse.hasChest();
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
        usedEntity = null;
    }

    private static void startWatching(Minecraft minecraft, AbstractContainerScreen<?> screen) {
        BlockPos pos = usedPos;
        UUID entity = usedEntity;
        usedPos = null;
        usedEntity = null;
        if (minecraft.level == null || minecraft.player == null) return;
        boolean recent = Util.getMillis() - usedAt <= USE_WINDOW_MILLIS && minecraft.level.dimension() == usedDimension;
        // Riding a chest boat or a donkey with a chest, the inventory key opens its inventory: it is the one ridden.
        if (!recent && minecraft.player.getVehicle() instanceof Entity vehicle && isContainer(vehicle)) {
            entity = vehicle.getUUID();
            pos = null;
            recent = true;
        }
        if (!recent) return;
        DimensionMemory memory = ContainerMemory.dimension(minecraft.level.dimension());
        ContainerRecord known;
        if (entity != null) {
            Entity opened = minecraft.level.getEntity(entity);
            if (opened == null) return;
            watchedEntity = entity;
            watchedPos = opened.blockPosition();
            known = memory != null ? memory.getEntity(entity) : null;
        } else {
            if (pos == null) return;
            BlockPos key = ContainerKeys.keyOf(minecraft.level, pos);
            if (key == null) return;
            watchedEntity = null;
            watchedPos = key;
            known = memory != null ? memory.get(key) : null;
        }
        Guidance.onReached(watchedPos);
        watched = screen;
        watchedDimension = minecraft.level.dimension();
        watchedIgnored = known != null && known.ignored();
    }

    private static void stopWatching(Minecraft minecraft) {
        AbstractContainerScreen<?> screen = watched;
        BlockPos pos = watchedPos;
        UUID entity = watchedEntity;
        ResourceKey<Level> dimension = watchedDimension;
        watched = null;
        watchedPos = null;
        watchedEntity = null;
        watchedDimension = null;
        if (screen == null || pos == null || minecraft.player == null || minecraft.level == null) return;
        if (minecraft.level.dimension() != dimension || !WDIPIClientConfig.ENABLED.get()) return;

        Identifier type;
        if (entity != null) {
            // Gone (killed, broken) while it was open: nothing to remember.
            Entity opened = minecraft.level.getEntity(entity);
            if (opened == null || !isContainer(opened)) return;
            pos = opened.blockPosition();
            type = BuiltInRegistries.ENTITY_TYPE.getKey(opened.getType());
        } else {
            // Broken while it was open: nothing to remember, and the old record goes with the block.
            if (ContainerKeys.keyOf(minecraft.level, pos) == null) return;
            type = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(pos).getBlock());
        }

        boolean network = StorageTerminals.of(screen) != null;
        List<StoredItem> items = contents(screen, minecraft.player.getInventory());
        // A screen with only the player's own slots stores nothing (a block that opens some other menu).
        if (items == null) return;

        Component title = screen.getTitle();
        Component label = title.getString().isBlank() ? null : title.copy();
        ContainerRecord record = new ContainerRecord(pos, type, label, System.currentTimeMillis(), List.copyOf(items), network,
                watchedIgnored, storageSlots(screen, minecraft.player.getInventory()), entity);
        ContainerMemory.record(dimension, record);
        TeamMemory.share(dimension, record);
    }

    /** Where the remembered container {@code screen} shows is (an entity's when it was opened), or null when it isn't one being watched. */
    public static @Nullable BlockPos openContainer(AbstractContainerScreen<?> screen) {
        return watched == screen ? watchedPos : null;
    }

    /**
     * Leaves the container open now out of searches, or takes it back in; null when no remembered container is open
     * ({@code screen} isn't the one being watched), else whether it is now left out.
     */
    public static @Nullable Boolean toggleIgnored(AbstractContainerScreen<?> screen) {
        if (watched != screen || watchedPos == null || watchedDimension == null) return null;
        watchedIgnored = !watchedIgnored;
        DimensionMemory memory = ContainerMemory.dimension(watchedDimension);
        ContainerRecord known = memory == null ? null : watchedEntity != null ? memory.getEntity(watchedEntity) : memory.get(watchedPos);
        if (known != null) memory.put(known.withIgnored(watchedIgnored));
        return watchedIgnored;
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

    // How many slots a plain storage menu has (chests, shulker boxes, hoppers, dispensers and droppers), whose every slot
    // takes any item; 0 for others (a furnace's slots aren't room for anything) and for storage terminals.
    private static int storageSlots(AbstractContainerScreen<?> screen, Inventory inventory) {
        if (!(screen.getMenu() instanceof ChestMenu || screen.getMenu() instanceof ShulkerBoxMenu || screen.getMenu() instanceof HopperMenu
                || screen.getMenu() instanceof DispenserMenu)) {
            return 0;
        }
        int slots = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != inventory) slots++;
        }
        return slots;
    }

    private static boolean isPlayerInventory(AbstractContainerScreen<?> screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }
}
