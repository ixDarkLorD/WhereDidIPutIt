package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        return of(query, mode, nested, WDIPIClientConfig.IGNORED_TAGS.get());
    }

    /**
     * The same with the tags the item tag mode leaves out given: on the server they come from the client asking, as
     * client settings aren't there.
     */
    public static ItemMatcher of(ItemStack query, MatchMode mode, boolean nested, List<String> ignoredTags) {
        ItemStack copy = query.copy();
        Predicate<ItemStack> test = switch (mode) {
            case EXACT -> stack -> ItemStack.isSameItemSameComponents(stack, copy);
            case IGNORE_DATA -> stack -> stack.getItem() == copy.getItem();
            case ITEM_TAG -> {
                List<Pattern> ignored = ignoredTags.stream().map(ItemMatcher::pattern).toList();
                List<TagKey<Item>> tags = copy.getTags().filter(tag -> !ignored(tag, ignored)).toList();
                // An item in no tag left has nothing to share: fall back to the item itself.
                yield stack -> stack.getItem() == copy.getItem() || tags.stream().anyMatch(stack::is);
            }
            case SIMILAR -> similar(copy);
        };
        return new ItemMatcher(test, nested);
    }

    // Alike in what they do: an enchantment in common, the same potion, else the same item.
    private static Predicate<ItemStack> similar(ItemStack query) {
        Set<Holder<Enchantment>> enchantments = enchantments(query);
        if (!enchantments.isEmpty()) return stack -> enchantments(stack).stream().anyMatch(enchantments::contains);
        PotionContents potion = query.get(DataComponents.POTION_CONTENTS);
        if (potion != null && potion.potion().isPresent()) {
            return stack -> {
                PotionContents other = stack.get(DataComponents.POTION_CONTENTS);
                return other != null && other.potion().equals(potion.potion());
            };
        }
        return stack -> stack.getItem() == query.getItem();
    }

    // An item's enchantments, on it or stored in it (an enchanted book).
    private static Set<Holder<Enchantment>> enchantments(ItemStack stack) {
        Set<Holder<Enchantment>> all = new HashSet<>();
        ItemEnchantments on = stack.get(DataComponents.ENCHANTMENTS);
        if (on != null) all.addAll(on.keySet());
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) all.addAll(stored.keySet());
        return all;
    }

    /** The matching item there is most of, as a stack of one (what the item marker shows); empty if none matches. */
    public ItemStack most(List<StoredItem> items) {
        ItemStack best = ItemStack.EMPTY;
        long bestCount = 0;
        for (StoredItem item : items) {
            if (this.test.test(item.stack()) && item.count() > bestCount) {
                best = item.stack();
                bestCount = item.count();
            }
        }
        // Only inside shulker boxes or bundles: the first one found there.
        if (best.isEmpty() && this.nested) {
            for (StoredItem item : items) {
                ItemStack inner = this.firstInside(item.stack(), 0);
                if (!inner.isEmpty()) return inner;
            }
        }
        return best.isEmpty() ? best : best.copyWithCount(1);
    }

    private ItemStack firstInside(ItemStack stack, int depth) {
        if (depth >= MAX_DEPTH) return ItemStack.EMPTY;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        List<ItemStack> inside = container != null ? container.nonEmptyStream().toList() : bundle != null ? bundle.itemCopyStream().toList() : List.of();
        for (ItemStack inner : inside) {
            if (this.test.test(inner)) return inner.copyWithCount(1);
            ItemStack deeper = this.firstInside(inner, depth + 1);
            if (!deeper.isEmpty()) return deeper;
        }
        return ItemStack.EMPTY;
    }

    // A tag pattern from the config: * matches any part of a name, and a name without a namespace matches any.
    private static Pattern pattern(String text) {
        String regex = Arrays.stream(text.trim().split("\\*", -1)).map(Pattern::quote).collect(Collectors.joining(".*"));
        return Pattern.compile(text.contains(":") ? regex : "[^:]*:" + regex);
    }

    private static boolean ignored(TagKey<Item> tag, List<Pattern> ignored) {
        String id = tag.location().toString();
        return ignored.stream().anyMatch(pattern -> pattern.matcher(id).matches());
    }

    /** How many matching items there are, counting inside shulker boxes and bundles when asked to. */
    public long count(List<StoredItem> items) {
        long count = 0;
        for (StoredItem item : items) {
            count += this.count(item.stack(), 0) * item.count();
        }
        return count;
    }

    // How many matching items one of this stack holds (itself, or what is inside it).
    private long count(ItemStack stack, int depth) {
        long count = this.test.test(stack) ? 1 : 0;
        if (!this.nested || depth >= MAX_DEPTH) return count;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            count += container.nonEmptyStream().mapToLong(inner -> this.count(inner, depth + 1) * inner.getCount()).sum();
        }
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            count += bundle.itemCopyStream().mapToLong(inner -> this.count(inner, depth + 1) * inner.getCount()).sum();
        }
        return count;
    }
}
