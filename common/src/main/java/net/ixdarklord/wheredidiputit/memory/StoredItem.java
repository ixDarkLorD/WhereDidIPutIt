package net.ixdarklord.wheredidiputit.memory;

import net.minecraft.world.item.ItemStack;

/**
 * Some of one item in a container. The amount is kept apart from the stack: a storage network can hold far more
 * than a stack's 99, and the stack codec allows no more.
 *
 * @param stack the item with its components, a stack of one
 * @param count how many there are
 */
public record StoredItem(ItemStack stack, long count) {
    public StoredItem {
        stack = stack.copyWithCount(1);
    }

    /** A slot's stack as it is. */
    public static StoredItem of(ItemStack stack) {
        return new StoredItem(stack, stack.getCount());
    }
}
