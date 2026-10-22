package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What one container held the last time it was opened. Stacks keep their components, so shulker boxes and bundles
 * inside keep their own contents; whether a search looks into them is decided when searching. A storage network's
 * terminal is remembered the same way, with everything the network showed.
 *
 * @param pos     the container's position; a double chest's lower half (see {@link ContainerKeys#keyOf})
 * @param block   the block's id, to notice when it is replaced by something else
 * @param label   the title of its screen ("Large Chest", a name given in an anvil, "ME Terminal"); null for none
 * @param seenAt  when it was last opened, in milliseconds since the epoch
 * @param items   what it held
 * @param network whether it is a storage mod's terminal, holding what its network showed: live search can't see into
 *                networks, so these stay remembered results even where the server answers
 * @param ignored whether the player left it out of searches (a trash chest, a farm's output): still remembered, never
 *                found
 * @param slots   how many slots it has, for finding room; 0 when unknown or not plain storage (a furnace's slots aren't
 *                room for anything)
 * @param entity  for a container that is an entity (a chest boat, a minecart with a chest, a donkey with one), its UUID;
 *                then {@code pos} is where it was last seen and {@code block} its entity type's id
 */
public record ContainerRecord(BlockPos pos, ResourceLocation block, @Nullable Component label, long seenAt, List<StoredItem> items, boolean network,
                              boolean ignored, int slots, @Nullable UUID entity) {
    /** What it is remembered under: its UUID for an entity, else its position. */
    public Object key() {
        return this.entity != null ? this.entity : this.pos;
    }

    /** The same record, left out of searches or not. */
    public ContainerRecord withIgnored(boolean ignored) {
        return new ContainerRecord(this.pos, this.block, this.label, this.seenAt, this.items, this.network, ignored, this.slots, this.entity);
    }

    /** The same record at another position (a chest that became a double chest remembered under its other half, an entity that moved). */
    public ContainerRecord movedTo(BlockPos pos) {
        return new ContainerRecord(pos, this.block, this.label, this.seenAt, this.items, this.network, this.ignored, this.slots, this.entity);
    }

    public CompoundTag save(RegistryOps<Tag> ops) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Pos", new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()});
        tag.putString("Block", this.block.toString());
        if (this.label != null) ComponentSerialization.CODEC.encodeStart(ops, this.label).ifSuccess(label -> tag.put("Label", label));
        tag.putLong("SeenAt", this.seenAt);
        if (this.network) tag.putBoolean("Network", true);
        if (this.ignored) tag.putBoolean("Ignored", true);
        if (this.slots > 0) tag.putInt("Slots", this.slots);
        if (this.entity != null) tag.putIntArray("Entity", UUIDUtil.uuidToIntArray(this.entity));
        ListTag items = new ListTag();
        for (StoredItem item : this.items) {
            ItemStack.CODEC.encodeStart(ops, item.stack()).ifSuccess(stack -> {
                CompoundTag entry = new CompoundTag();
                entry.put("Item", stack);
                entry.putLong("Count", item.count());
                items.add(entry);
            });
        }
        tag.put("Items", items);
        return tag;
    }

    /**
     * Reads a record, skipping items that no longer load (a removed mod's); empty if the record itself is broken.
     *
     * @param format the file's format: 1 kept plain stacks with their counts, 2 an item and a count per entry
     */
    public static Optional<ContainerRecord> load(CompoundTag tag, RegistryOps<Tag> ops, int format) {
        int[] pos = tag.getIntArray("Pos");
        ResourceLocation block = ResourceLocation.tryParse(tag.getString("Block"));
        if (pos.length != 3 || block == null) {
            WhereDidIPutIt.LOGGER.warn("Skipping a broken container record: {}", tag);
            return Optional.empty();
        }
        Component label = tag.get("Label") instanceof Tag labelTag ? ComponentSerialization.CODEC.parse(ops, labelTag).result().orElse(null) : null;
        List<StoredItem> items = new ArrayList<>();
        for (Tag item : tag.getList("Items", Tag.TAG_COMPOUND)) {
            if (format < 2) {
                ItemStack.CODEC.parse(ops, item).ifSuccess(stack -> items.add(StoredItem.of(stack)));
            } else if (item instanceof CompoundTag entry && entry.get("Item") instanceof Tag stack) {
                long count = entry.contains("Count") ? entry.getLong("Count") : 1L;
                ItemStack.CODEC.parse(ops, stack).ifSuccess(parsed -> items.add(new StoredItem(parsed, count)));
            }
        }
        return Optional.of(new ContainerRecord(new BlockPos(pos[0], pos[1], pos[2]), block, label, tag.getLong("SeenAt"), List.copyOf(items),
                tag.getBoolean("Network"), tag.getBoolean("Ignored"), tag.getInt("Slots"), entity(tag.getIntArray("Entity"))));
    }

    // An entity's UUID as saved, four ints; null for a block (none saved).
    private static @Nullable UUID entity(int[] uuid) {
        return uuid.length == 4 ? UUIDUtil.uuidFromIntArray(uuid) : null;
    }

    /** The ops records are written with: NBT, resolving item and component ids against the connection's registries. */
    public static RegistryOps<Tag> ops(HolderLookup.Provider registries) {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }
}
