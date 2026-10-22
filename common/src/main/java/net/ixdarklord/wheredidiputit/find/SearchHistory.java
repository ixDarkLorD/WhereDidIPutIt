package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the last searches found, item by item: so an item the player searched for has a tooltip even when the
 * containers it was found in were never opened (live search looks in those). Kept for the most recent
 * {@link #MAX_ITEMS} items while the player stays in the world.
 */
public final class SearchHistory {
    private static final int MAX_ITEMS = 64;

    // Most recently searched last; the oldest goes when there are too many.
    private static final Map<Item, Search> SEARCHES = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Item, Search> eldest) {
            return this.size() > MAX_ITEMS;
        }
    };

    private SearchHistory() {
    }

    /** A container a search found the item in: where it is, what it is called, and how many it held. */
    public record Place(BlockPos pos, Component name, long count) {
    }

    /** One search's finds, where and when. */
    public record Search(ResourceKey<Level> dimension, List<Place> places, long at) {
        public long total() {
            return this.places.stream().mapToLong(Place::count).sum();
        }
    }

    public static void init() {
        ClientPlayerEvents.LEAVE.register(player -> SEARCHES.clear());
    }

    /** A search for {@code query} found these; an empty find is not kept. */
    static void record(ResourceKey<Level> dimension, ItemStack query, List<Place> places) {
        if (!places.isEmpty()) SEARCHES.put(query.getItem(), new Search(dimension, List.copyOf(places), Util.getMillis()));
    }

    /** The last search's finds for this item in this dimension, or null. */
    public static @Nullable Search find(ResourceKey<Level> dimension, ItemStack stack) {
        Search search = SEARCHES.get(stack.getItem());
        return search != null && search.dimension == dimension ? search : null;
    }
}
