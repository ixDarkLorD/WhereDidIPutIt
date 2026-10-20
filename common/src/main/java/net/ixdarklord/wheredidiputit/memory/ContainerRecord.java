package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What one container held the last time it was opened. Stacks keep their components, so shulker boxes and bundles
 * inside keep their own contents; whether a search looks into them is decided when searching.
 *
 * @param pos    the container's position; a double chest's lower half (see {@link ContainerWatcher#keyOf})
 * @param block  the block's id, to notice when it is replaced by something else
 * @param seenAt when it was last opened, in milliseconds since the epoch
 * @param items  its non-empty stacks
 */
public record ContainerRecord(BlockPos pos, Identifier block, long seenAt, List<ItemStack> items) {
    public CompoundTag save(RegistryOps<Tag> ops) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Pos", new int[]{this.pos.getX(), this.pos.getY(), this.pos.getZ()});
        tag.putString("Block", this.block.toString());
        tag.putLong("SeenAt", this.seenAt);
        ListTag items = new ListTag();
        for (ItemStack stack : this.items) {
            ItemStack.CODEC.encodeStart(ops, stack).ifSuccess(items::add);
        }
        tag.put("Items", items);
        return tag;
    }

    /** Reads a record, skipping stacks that no longer load (a removed mod's items); empty if the record itself is broken. */
    public static Optional<ContainerRecord> load(CompoundTag tag, RegistryOps<Tag> ops) {
        int[] pos = tag.getIntArray("Pos").orElse(null);
        Identifier block = Identifier.tryParse(tag.getStringOr("Block", ""));
        if (pos == null || pos.length != 3 || block == null) {
            WhereDidIPutIt.LOGGER.warn("Skipping a broken container record: {}", tag);
            return Optional.empty();
        }
        List<ItemStack> items = new ArrayList<>();
        for (Tag item : tag.getListOrEmpty("Items")) {
            ItemStack.CODEC.parse(ops, item).ifSuccess(items::add);
        }
        return Optional.of(new ContainerRecord(new BlockPos(pos[0], pos[1], pos[2]), block, tag.getLongOr("SeenAt", 0L), List.copyOf(items)));
    }

    /** The ops records are written with: NBT, resolving item and component ids against the connection's registries. */
    public static RegistryOps<Tag> ops(HolderLookup.Provider registries) {
        return registries.createSerializationContext(NbtOps.INSTANCE);
    }
}
