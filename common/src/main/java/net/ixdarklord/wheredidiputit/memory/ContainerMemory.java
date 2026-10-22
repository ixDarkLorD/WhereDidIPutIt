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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The client's memory of containers, for the world or server it is in: opened when joining, saved while playing and
 * when leaving. A record whose block the server says was broken or replaced is dropped at once ({@code ClientLevelMixin});
 * a slow sweep of loaded positions catches the rest (records from before the chunk was loaded, chests joined into
 * double chests).
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
            current = WorldMemory.forCurrentWorld(Minecraft.getInstance());
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

    /** The memory of the world or server the client is in now, or null outside one (or in one that can't be told apart). */
    public static @Nullable WorldMemory world() {
        return current;
    }

    static void record(ResourceKey<Level> dimension, ContainerRecord record) {
        DimensionMemory memory = dimension(dimension);
        if (memory != null) memory.put(record);
    }

    /** The server says the block at {@code pos} is now {@code state}: a container remembered there that isn't it any more is forgotten. */
    public static void onServerBlockState(ClientLevel level, BlockPos pos, BlockState state) {
        DimensionMemory memory = dimension(level.dimension());
        if (memory == null) return;
        ContainerRecord record = memory.get(pos);
        if (record != null && !BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(record.block())) memory.remove(pos);
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

        List<ContainerRecord> gone = new ArrayList<>();
        List<ContainerRecord> moved = new ArrayList<>();
        for (ContainerRecord record : memory.records()) {
            if (record.entity() != null) {
                checkEntity(level, record, gone, moved);
                continue;
            }
            if (!level.isLoaded(record.pos())) continue;
            BlockPos key = ContainerKeys.keyOf(level, record.pos());
            boolean sameBlock = BuiltInRegistries.BLOCK.getKey(level.getBlockState(record.pos()).getBlock()).equals(record.block());
            if (key == null || !sameBlock) {
                gone.add(record);
            } else if (!key.equals(record.pos())) {
                // A chest placed beside it made it a double chest remembered under the other half: keep what it held.
                gone.add(record);
                if (memory.get(key) == null) moved.add(record.movedTo(key));
            }
        }
        gone.forEach(memory::remove);
        moved.forEach(memory::put);
    }

    // An entity's record follows it while it is loaded, and goes when a donkey or llama no longer carries a chest. One
    // out of sight keeps its last place: the client can't tell an entity that left from one that died.
    private static void checkEntity(ClientLevel level, ContainerRecord record, List<ContainerRecord> gone, List<ContainerRecord> moved) {
        Entity entity = EntityLookup.get(level, record.entity());
        if (entity == null) return;
        if (!ContainerWatcher.isContainer(entity)) {
            gone.add(record);
        } else if (entity.blockPosition().distManhattan(record.pos()) >= 2) {
            moved.add(record.movedTo(entity.blockPosition()));
        }
    }
}
