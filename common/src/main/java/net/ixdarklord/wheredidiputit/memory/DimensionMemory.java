package net.ixdarklord.wheredidiputit.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** The containers remembered in one dimension of one world or server. */
public final class DimensionMemory {
    /** Bumped when the file layout changes, so older files can be read differently. */
    private static final int FORMAT = 1;

    private final Map<BlockPos, ContainerRecord> records = new HashMap<>();
    private boolean dirty;

    public @Nullable ContainerRecord get(BlockPos pos) {
        return this.records.get(pos);
    }

    public Collection<ContainerRecord> records() {
        return Collections.unmodifiableCollection(this.records.values());
    }

    public void put(ContainerRecord record) {
        this.records.put(record.pos(), record);
        this.dirty = true;
    }

    public void remove(BlockPos pos) {
        if (this.records.remove(pos) != null) this.dirty = true;
    }

    /** Forgets every container last opened before {@code cutoff} (milliseconds since the epoch). */
    public void forgetOlderThan(long cutoff) {
        if (this.records.values().removeIf(record -> record.seenAt() < cutoff)) this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    /** Writes the records to a tag and marks them saved. Runs on the client thread: encoding stacks reads the registries. */
    public CompoundTag save(RegistryOps<Tag> ops) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Format", FORMAT);
        ListTag containers = new ListTag();
        for (ContainerRecord record : this.records.values()) {
            containers.add(record.save(ops));
        }
        tag.put("Containers", containers);
        this.dirty = false;
        return tag;
    }

    public static DimensionMemory load(CompoundTag tag, RegistryOps<Tag> ops) {
        DimensionMemory memory = new DimensionMemory();
        tag.getListOrEmpty("Containers").compoundStream()
                .forEach(container -> ContainerRecord.load(container, ops).ifPresent(record -> memory.records.put(record.pos(), record)));
        return memory;
    }
}
