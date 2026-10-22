package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Every different item stored in a dimension's remembered containers, with how many there are and in how many
 * containers: what the search screen lists. Containers left out of searches are left out here too.
 */
public final class StoredStacks {
    /** Shulker boxes in bundles in shulker boxes, as {@link ItemMatcher} looks. */
    private static final int MAX_DEPTH = 3;

    private StoredStacks() {
    }

    /**
     * @param stack      the item, a stack of one
     * @param count      how many are stored in all
     * @param containers how many containers hold some
     */
    public record Entry(ItemStack stack, long count, int containers) {
    }

    /** The items, by name; with {@code nested}, what is inside shulker boxes and bundles too. */
    public static List<Entry> collect(DimensionMemory memory, boolean nested) {
        Map<Item, List<Tally>> byItem = new IdentityHashMap<>();
        for (ContainerRecord record : memory.records()) {
            if (record.ignored()) continue;
            Set<Tally> here = new HashSet<>();
            for (StoredItem item : record.items()) {
                add(byItem, here, item.stack(), item.count(), nested, 0);
            }
            here.forEach(tally -> tally.containers++);
        }
        List<Entry> entries = new ArrayList<>();
        byItem.values().forEach(tallies -> tallies.forEach(tally -> entries.add(new Entry(tally.stack, tally.count, tally.containers))));
        entries.sort(Comparator.comparing((Entry entry) -> entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString()));
        return entries;
    }

    private static void add(Map<Item, List<Tally>> byItem, Set<Tally> here, ItemStack stack, long count, boolean nested, int depth) {
        if (stack.isEmpty() || count <= 0) return;
        List<Tally> tallies = byItem.computeIfAbsent(stack.getItem(), item -> new ArrayList<>());
        Tally tally = tallies.stream().filter(t -> ItemStack.isSameItemSameComponents(t.stack, stack)).findFirst().orElse(null);
        if (tally == null) {
            tally = new Tally(stack.copyWithCount(1));
            tallies.add(tally);
        }
        tally.count += count;
        here.add(tally);
        if (!nested || depth >= MAX_DEPTH) return;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) container.nonEmptyItemCopyStream().forEach(inner -> add(byItem, here, inner, inner.getCount() * count, true, depth + 1));
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) bundle.itemCopies().forEach(inner -> add(byItem, here, inner, inner.getCount() * count, true, depth + 1));
    }

    /**
     * Whether a stack fits what was typed: its name or id containing it, {@code #text} a tag containing the text,
     * {@code @text} a mod (namespace) containing it. Ignores case; an empty query fits everything.
     */
    public static boolean matches(ItemStack stack, String query) {
        String text = query.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) return true;
        if (text.startsWith("#")) {
            String tag = text.substring(1);
            return stack.typeHolder().tags().anyMatch(key -> key.location().toString().contains(tag));
        }
        if (text.startsWith("@")) return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().contains(text.substring(1));
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(text)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(text);
    }

    // One different item: identity matters in the set of those a container holds, so no equals here.
    private static final class Tally {
        private final ItemStack stack;
        private long count;
        private int containers;

        private Tally(ItemStack stack) {
            this.stack = stack;
        }
    }
}
