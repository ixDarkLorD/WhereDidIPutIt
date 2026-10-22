package net.ixdarklord.wheredidiputit.find;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What an item holds inside it: a shulker box's (or any block item's kept block entity's) items, a bundle's. 26.1 keeps
 * them in the {@code container} and {@code bundle_contents} components; 1.20.1 keeps them in the stack's NBT, a block
 * item's under {@code BlockEntityTag.Items}, a bundle's under {@code Items}.
 */
public final class NestedItems {
    private NestedItems() {
    }

    /** The non-empty stacks inside {@code stack}, copies; empty when it holds nothing. */
    public static List<ItemStack> of(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return List.of();
        ListTag items;
        if (stack.getItem() instanceof BundleItem) {
            items = tag.getList("Items", Tag.TAG_COMPOUND);
        } else {
            CompoundTag blockEntity = BlockItem.getBlockEntityData(stack);
            if (blockEntity == null) return List.of();
            items = blockEntity.getList("Items", Tag.TAG_COMPOUND);
        }
        List<ItemStack> inside = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ItemStack inner = ItemStack.of(items.getCompound(i));
            if (!inner.isEmpty()) inside.add(inner);
        }
        return inside;
    }
}
