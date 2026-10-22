package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Pos", new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()});
        tag.putString("Block", this.block.toString());
        // 1.20.1 has no component codec: the label goes as its JSON, as the game keeps a renamed block's name.
        if (this.label != null) tag.putString("Label", Component.Serializer.toJson(this.label));
        tag.putLong("SeenAt", this.seenAt);
        if (this.network) tag.putBoolean("Network", true);
        if (this.ignored) tag.putBoolean("Ignored", true);
        if (this.slots > 0) tag.putInt("Slots", this.slots);
        if (this.entity != null) tag.putIntArray("Entity", UUIDUtil.uuidToIntArray(this.entity));
        ListTag items = new ListTag();
        for (StoredItem item : this.items) {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", item.stack().save(new CompoundTag()));
            entry.putLong("Count", item.count());
            items.add(entry);
        }
        tag.put("Items", items);
        return tag;
    }

    /**
     * Reads a record, skipping items that no longer load (a removed mod's); empty if the record itself is broken.
     *
     * @param format the file's format: 1 kept plain stacks with their counts, 2 an item and a count per entry
     */
    public static Optional<ContainerRecord> load(CompoundTag tag, int format) {
        int[] pos = tag.getIntArray("Pos");
        ResourceLocation block = ResourceLocation.tryParse(tag.getString("Block"));
        if (pos.length != 3 || block == null) {
            WhereDidIPutIt.LOGGER.warn("Skipping a broken container record: {}", tag);
            return Optional.empty();
        }
        Component label = tag.contains("Label", Tag.TAG_STRING) ? label(tag.getString("Label")) : null;
        List<StoredItem> items = new ArrayList<>();
        ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            // A stack that no longer loads (a removed mod's item) comes back empty.
            if (format < 2) {
                ItemStack stack = ItemStack.of(entry);
                if (!stack.isEmpty()) items.add(StoredItem.of(stack));
            } else if (entry.contains("Item", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.of(entry.getCompound("Item"));
                if (!stack.isEmpty()) items.add(new StoredItem(stack, entry.contains("Count") ? entry.getLong("Count") : 1L));
            }
        }
        int[] entity = tag.getIntArray("Entity");
        return Optional.of(new ContainerRecord(new BlockPos(pos[0], pos[1], pos[2]), block, label, tag.getLong("SeenAt"), List.copyOf(items),
                tag.getBoolean("Network"), tag.getBoolean("Ignored"), tag.getInt("Slots"), entity.length == 4 ? UUIDUtil.uuidFromIntArray(entity) : null));
    }

    private static @Nullable Component label(String json) {
        try {
            return Component.Serializer.fromJson(json);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
