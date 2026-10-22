package net.ixdarklord.wheredidiputit.memory;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The containers remembered in one dimension of one world or server. */
public final class DimensionMemory {
    /** Bumped when the file layout changes, so older files can be read differently. 2: items carry a long count. */
    private static final int FORMAT = 2;

    private final Map<BlockPos, ContainerRecord> records = new HashMap<>();
    // Containers that are entities, which move: by their UUID.
    private final Map<UUID, ContainerRecord> entities = new HashMap<>();
    private boolean dirty;
    // Counts changes, so what was worked out from the records can tell when it is out of date.
    private int version;

    public @Nullable ContainerRecord get(BlockPos pos) {
        return this.records.get(pos);
    }

    public @Nullable ContainerRecord getEntity(UUID entity) {
        return this.entities.get(entity);
    }

    /** Every record, blocks and entities. */
    public Collection<ContainerRecord> records() {
        if (this.entities.isEmpty()) return Collections.unmodifiableCollection(this.records.values());
        List<ContainerRecord> all = new ArrayList<>(this.records.size() + this.entities.size());
        all.addAll(this.records.values());
        all.addAll(this.entities.values());
        return Collections.unmodifiableList(all);
    }

    /** The containers that are entities. */
    public Collection<ContainerRecord> entityRecords() {
        return Collections.unmodifiableCollection(this.entities.values());
    }

    public void put(ContainerRecord record) {
        this.store(record);
        this.changed();
    }

    private void store(ContainerRecord record) {
        if (record.entity() != null) {
            this.entities.put(record.entity(), record);
        } else {
            this.records.put(record.pos(), record);
        }
    }

    /** Forgets the block container at {@code pos}. */
    public void remove(BlockPos pos) {
        if (this.records.remove(pos) != null) this.changed();
    }

    /** Forgets a record, block or entity. */
    public void remove(ContainerRecord record) {
        boolean removed = record.entity() != null ? this.entities.remove(record.entity()) != null : this.records.remove(record.pos()) != null;
        if (removed) this.changed();
    }

    /**
     * Takes in another memory's records (an import): each container's record is kept from whichever saw it last.
     *
     * @return how many records were taken in
     */
    public int mergeNewer(DimensionMemory other) {
        int taken = 0;
        for (ContainerRecord record : other.records()) {
            ContainerRecord known = record.entity() != null ? this.getEntity(record.entity()) : this.get(record.pos());
            if (known == null || record.seenAt() > known.seenAt()) {
                this.store(record);
                taken++;
            }
        }
        if (taken > 0) this.changed();
        return taken;
    }

    /** Forgets every container last opened before {@code cutoff} (milliseconds since the epoch). */
    public void forgetOlderThan(long cutoff) {
        boolean blocks = this.records.values().removeIf(record -> record.seenAt() < cutoff);
        boolean entities = this.entities.values().removeIf(record -> record.seenAt() < cutoff);
        if (blocks || entities) this.changed();
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

    /** Writes the records to a tag and marks them saved. Runs on the client thread, where the records change. */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Format", FORMAT);
        ListTag containers = new ListTag();
        for (ContainerRecord record : this.records()) {
            containers.add(record.save());
        }
        tag.put("Containers", containers);
        this.dirty = false;
        return tag;
    }

    public static DimensionMemory load(CompoundTag tag) {
        DimensionMemory memory = new DimensionMemory();
        int format = tag.contains("Format", Tag.TAG_INT) ? tag.getInt("Format") : 1;
        ListTag containers = tag.getList("Containers", Tag.TAG_COMPOUND);
        for (int i = 0; i < containers.size(); i++) ContainerRecord.load(containers.getCompound(i), format).ifPresent(memory::store);
        return memory;
    }
}
