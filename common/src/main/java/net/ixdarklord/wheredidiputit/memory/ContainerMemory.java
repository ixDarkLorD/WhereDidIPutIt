package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The client's memory of containers, for the world or server it is in: opened when joining, saved while playing and
 * when leaving. Records whose block was broken or replaced are dropped once their chunk is loaded and checked.
 */
public final class ContainerMemory {
    /** Ticks between checks that remembered containers are still there (and not too old to keep). */
    private static final int CHECK_INTERVAL = 40;
    /** Ticks between saves of unsaved changes. */
    private static final int SAVE_INTERVAL = 200;

    private static @Nullable WorldMemory current;
    private static int ticks;

    private ContainerMemory() {
    }

    public static void init() {
        ClientPlayerEvents.JOIN.register(player -> {
            current = WorldMemory.forCurrentWorld(Minecraft.getInstance(), player.connection.registryAccess());
            if (current == null) WhereDidIPutIt.LOGGER.warn("Can't tell this world or server apart from others; containers won't be remembered here");
        });
        ClientPlayerEvents.LEAVE.register(player -> {
            ContainerWatcher.flush(Minecraft.getInstance());
            if (current != null) current.saveDirty();
            current = null;
        });
        ClientTickEvents.END.register(ContainerMemory::tick);
    }

    /** The memory of a dimension of the current world or server, or {@code null} outside a world. */
    public static @Nullable DimensionMemory dimension(ResourceKey<Level> dimension) {
        return current != null ? current.dimension(dimension) : null;
    }

    static void record(ResourceKey<Level> dimension, ContainerRecord record) {
        DimensionMemory memory = dimension(dimension);
        if (memory != null) memory.put(record);
    }

    private static void tick(Minecraft minecraft) {
        if (current == null || minecraft.level == null) return;
        ContainerWatcher.tick(minecraft);
        ticks++;
        if (ticks % CHECK_INTERVAL == 0) check(minecraft.level);
        if (ticks % SAVE_INTERVAL == 0) current.saveDirty();
    }

    /** Drops what is no longer there in the player's dimension, and what was last seen too long ago. */
    private static void check(ClientLevel level) {
        DimensionMemory memory = dimension(level.dimension());
        if (memory == null) return;
        int days = WDIPIClientConfig.FORGET_AFTER_DAYS.get();
        if (days > 0) memory.forgetOlderThan(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days));

        List<BlockPos> gone = new ArrayList<>();
        List<ContainerRecord> moved = new ArrayList<>();
        for (ContainerRecord record : memory.records()) {
            if (!level.isLoaded(record.pos())) continue;
            BlockPos key = ContainerKeys.keyOf(level, record.pos());
            boolean sameBlock = BuiltInRegistries.BLOCK.getKey(level.getBlockState(record.pos()).getBlock()).equals(record.block());
            if (key == null || !sameBlock) {
                gone.add(record.pos());
            } else if (!key.equals(record.pos())) {
                // A chest placed beside it made it a double chest remembered under the other half: keep what it held.
                gone.add(record.pos());
                if (memory.get(key) == null) moved.add(new ContainerRecord(key, record.block(), record.label(), record.seenAt(), record.items(), record.network()));
            }
        }
        gone.forEach(memory::remove);
        moved.forEach(memory::put);
    }
}
