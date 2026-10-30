package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Items scattered over several remembered containers (author, 2026-10-28): what the browser's tidy-up view lists, so
 * the player can gather each into one place. Each item's places are sorted by how many each holds; the first, holding
 * the most, is where the rest would best go. Containers left out of searches, storage networks (one network is one
 * place however many drives it has) and entities (they move) are left out.
 */
public final class Consolidation {
    private Consolidation() {
    }

    /**
     * @param stack  the item, a stack of one
     * @param total  how many in all
     * @param places where, most first
     */
    public record Scattered(ItemStack stack, long total, List<Place> places) {
        public Place target() {
            return this.places.get(0);
        }
    }

    public record Place(ContainerRecord record, long count) {
    }

    /** The items in two or more containers, those spread over the most first. */
    public static List<Scattered> find(DimensionMemory memory) {
        List<ItemStack> kinds = new ArrayList<>();
        List<List<Place>> placesByKind = new ArrayList<>();
        for (ContainerRecord record : memory.records()) {
            if (record.ignored() || record.network() || record.entity() != null) continue;
            List<ItemStack> seenHere = new ArrayList<>();
            for (StoredItem item : record.items()) {
                if (item.stack().isEmpty()) continue;
                int kind = indexOf(kinds, item.stack());
                if (kind < 0) {
                    kinds.add(item.stack());
                    placesByKind.add(new ArrayList<>());
                    kind = kinds.size() - 1;
                }
                // Several stacks of it in one container add up to one place.
                List<Place> places = placesByKind.get(kind);
                if (indexOf(seenHere, item.stack()) >= 0) {
                    Place last = places.get(places.size() - 1);
                    places.set(places.size() - 1, new Place(record, last.count + item.count()));
                } else {
                    seenHere.add(item.stack());
                    places.add(new Place(record, item.count()));
                }
            }
        }
        List<Scattered> scattered = new ArrayList<>();
        for (int i = 0; i < kinds.size(); i++) {
            List<Place> places = placesByKind.get(i);
            if (places.size() < 2) continue;
            places.sort(Comparator.comparingLong(Place::count).reversed());
            scattered.add(new Scattered(kinds.get(i), places.stream().mapToLong(Place::count).sum(), List.copyOf(places)));
        }
        scattered.sort(Comparator.comparingInt((Scattered entry) -> entry.places.size()).reversed()
                .thenComparing(entry -> entry.stack.getHoverName().getString()));
        return scattered;
    }

    private static int indexOf(List<ItemStack> stacks, ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (ItemStack.isSameItemSameTags(stacks.get(i), stack)) return i;
        }
        return -1;
    }
}
