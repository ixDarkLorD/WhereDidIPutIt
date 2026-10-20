package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.function.Predicate;

/** Decides which stored stacks count as the item searched for, and how many of them a container holds. */
public final class ItemMatcher {
    /** Shulker boxes in bundles in shulker boxes: deeper than the game allows, so this never cuts anything real short. */
    private static final int MAX_DEPTH = 3;

    private final Predicate<ItemStack> test;
    private final boolean nested;

    private ItemMatcher(Predicate<ItemStack> test, boolean nested) {
        this.test = test;
        this.nested = nested;
    }

    /**
     * @param query  the stack searched for
     * @param mode   how alike a stack must be
     * @param nested whether stacks inside shulker boxes and bundles count too
     */
    public static ItemMatcher of(ItemStack query, MatchMode mode, boolean nested) {
        ItemStack copy = query.copy();
        Predicate<ItemStack> test = switch (mode) {
            case EXACT -> stack -> ItemStack.isSameItemSameComponents(stack, copy);
            case IGNORE_DATA -> stack -> stack.getItem() == copy.getItem();
            case ITEM_TAG -> {
                List<TagKey<Item>> tags = copy.typeHolder().tags().toList();
                // An item in no tag has nothing to share: fall back to the item itself.
                yield stack -> stack.getItem() == copy.getItem() || tags.stream().anyMatch(stack.typeHolder()::is);
            }
        };
        return new ItemMatcher(test, nested);
    }

    /** How many matching items the stacks hold, counting inside shulker boxes and bundles when asked to. */
    public int count(List<ItemStack> stacks) {
        int count = 0;
        for (ItemStack stack : stacks) {
            count += this.count(stack, 0);
        }
        return count;
    }

    private int count(ItemStack stack, int depth) {
        int count = this.test.test(stack) ? stack.getCount() : 0;
        if (!this.nested || depth >= MAX_DEPTH) return count;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            count += container.nonEmptyItemCopyStream().mapToInt(inner -> this.count(inner, depth + 1)).sum() * stack.getCount();
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            count += bundle.itemCopyStream().mapToInt(inner -> this.count(inner, depth + 1)).sum() * stack.getCount();
        }
        return count;
    }
}
