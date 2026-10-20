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
    /** Bumped when the file layout changes, so older files can be read differently. 2: items carry a long count. */
    private static final int FORMAT = 2;

    private final Map<BlockPos, ContainerRecord> records = new HashMap<>();
    private boolean dirty;
    // Counts changes, so what was worked out from the records can tell when it is out of date.
    private int version;

    public @Nullable ContainerRecord get(BlockPos pos) {
        return this.records.get(pos);
    }

    public Collection<ContainerRecord> records() {
        return Collections.unmodifiableCollection(this.records.values());
    }

    public void put(ContainerRecord record) {
        this.records.put(record.pos(), record);
        this.changed();
    }

    public void remove(BlockPos pos) {
        if (this.records.remove(pos) != null) this.changed();
    }

    /** Forgets every container last opened before {@code cutoff} (milliseconds since the epoch). */
    public void forgetOlderThan(long cutoff) {
        if (this.records.values().removeIf(record -> record.seenAt() < cutoff)) this.changed();
    }

    /** Goes up whenever a record is added, replaced or removed. */
    public int version() {
        return this.version;
    }

    private void changed() {
        this.dirty = true;
        this.version++;
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
        int format = tag.getIntOr("Format", 1);
        tag.getListOrEmpty("Containers").compoundStream()
                .forEach(container -> ContainerRecord.load(container, ops, format).ifPresent(record -> memory.records.put(record.pos(), record)));
        return memory;
    }
}
