package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.client.render.Guidance;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The gathering list (author, 2026-10-28): what the player means to fetch, and one route through the remembered
 * containers to fetch it. The key on a hovered item adds a stack of it when a remembered container holds some, else
 * what crafting it takes (from an unlocked recipe), else a stack of it anyway; shift-clicking an item in the search
 * screen adds a stack too. The list counts what the inventory holds against what is wanted, ticks lines off as they
 * fill, and points out the next container on the route, the nearest that still has something wanted, then the
 * nearest to that, and so on; the path line and compass lead there, and when it has been opened the next one is
 * shown. The key while playing (no screen) clears it.
 */
public final class GatherList {
    /** Unbound by default, as the mod's other added keys. */
    public static final KeyMapping KEY = new KeyMapping("key.wheredidiputit.gather", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), FindKey.CATEGORY);
    private static final int MAX_NEEDS = 12;
    private static final long REPEAT_MILLIS = 250L;
    /** Ticks between checks of the inventory, to tick lines off and move to the next stop. */
    private static final int CHECK_TICKS = 10;

    /**
     * One line of the list.
     *
     * @param shown  the item drawn for it, a stack of one
     * @param test   what counts towards it
     * @param wanted how many the player wants to carry
     */
    public record Need(ItemStack shown, Predicate<ItemStack> test, int wanted, Object key) {
    }

    /** A container on the route and what the player is to take there. */
    public record Stop(ContainerRecord record, ItemStack item, long count) {
    }

    private static final List<Need> NEEDS = new ArrayList<>();
    private static List<Stop> route = List.of();
    private static int ticks;
    private static int lastSnapshot;
    private static long lastPress;

    private GatherList() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
        ClientTickEvents.END.register(GatherList::tick);
        ClientPlayerEvents.LEAVE.register(player -> clear());
    }

    public static List<Need> needs() {
        return NEEDS;
    }

    public static List<Stop> route() {
        return route;
    }

    /** How many of a line's item the player carries. */
    public static int have(Need need) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;
        int have = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (need.test.test(stack)) have += stack.getCount();
        }
        return have;
    }

    /** The key pressed over a hovered item in a screen; true when it was used up. */
    public static boolean onKeyPressed(int keyCode, int scanCode, ItemStack hovered) {
        if (hovered.isEmpty() || !KEY.matches(keyCode, scanCode) || !WDIPIClientConfig.ENABLED.get()) return false;
        long now = Util.getMillis();
        if (now - lastPress < REPEAT_MILLIS) return true;
        lastPress = now;
        // An item already in storage is fetched itself (an iron ingot, not the nuggets for one); one that isn't, crafted.
        Map<Ingredient, Integer> recipe = stored(hovered) ? null : RecipeNeeds.requirements(hovered);
        if (recipe != null) {
            // Keyed by what the ingredient accepts: 1.20.1's ingredients aren't equal to one another.
            recipe.forEach((ingredient, count) -> add(shownFor(ingredient), ingredient::test, count, ingredient.toJson().toString()));
            FindKey.tell(Minecraft.getInstance(), Component.translatable("gui.wheredidiputit.gather.added_recipe", hovered.getHoverName()), true);
        } else {
            addStack(hovered);
        }
        replan();
        return true;
    }

    /** Adds a stack of an item (the search screen's shift-click, the key on an item no recipe makes). */
    public static void addStack(ItemStack item) {
        ItemStack one = item.copyWithCount(1);
        add(one, stack -> ItemStack.isSameItemSameTags(stack, one), item.getMaxStackSize(), new ItemKey(one));
        FindKey.tell(Minecraft.getInstance(), Component.translatable("gui.wheredidiputit.gather.added_item", item.getHoverName()), true);
        replan();
    }

    public static void clear() {
        if (!NEEDS.isEmpty()) Guidance.clear();
        NEEDS.clear();
        route = List.of();
    }

    // Whether a remembered container in this dimension holds the item.
    private static boolean stored(ItemStack item) {
        ClientLevel level = Minecraft.getInstance().level;
        DimensionMemory memory = level != null ? ContainerMemory.dimension(level.dimension()) : null;
        if (memory == null) return false;
        for (ContainerRecord record : memory.records()) {
            if (record.ignored()) continue;
            for (StoredItem stored : record.items()) {
                if (ItemStack.isSameItem(stored.stack(), item)) return true;
            }
        }
        return false;
    }

    // Adds to a line wanting the same thing, else a new line; the same ingredient from two recipes adds up.
    private static void add(ItemStack shown, Predicate<ItemStack> test, int count, Object key) {
        for (int i = 0; i < NEEDS.size(); i++) {
            Need need = NEEDS.get(i);
            if (need.key.equals(key)) {
                NEEDS.set(i, new Need(need.shown, need.test, need.wanted + count, need.key));
                return;
            }
        }
        if (NEEDS.size() < MAX_NEEDS) NEEDS.add(new Need(shown, test, count, key));
    }

    // An ingredient's picture: the first item it takes.
    private static ItemStack shownFor(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length > 0 ? items[0].copyWithCount(1) : ItemStack.EMPTY;
    }

    private static void tick(Minecraft minecraft) {
        while (KEY.consumeClick()) {
            if (minecraft.screen == null && !NEEDS.isEmpty()) {
                clear();
                FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.gather.cleared"), true);
            }
        }
        if (NEEDS.isEmpty() || minecraft.player == null || ++ticks < CHECK_TICKS) return;
        ticks = 0;
        // Only once the screen is shut: the player may still be taking things from the container.
        if (minecraft.screen != null) return;
        int snapshot = snapshot(minecraft.player);
        if (snapshot == lastSnapshot) return;
        lastSnapshot = snapshot;
        if (NEEDS.stream().allMatch(need -> have(need) >= need.wanted)) {
            clear();
            FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.gather.done"), true);
            return;
        }
        replan();
    }

    // Changes whenever the inventory's contents do.
    private static int snapshot(LocalPlayer player) {
        int hash = 1;
        for (ItemStack stack : player.getInventory().items) hash = hash * 31 + hash(stack) * 7 + stack.getCount();
        return hash;
    }

    /** Works out the route again from where the player is, and points out its first stop. */
    public static void replan() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        DimensionMemory memory = level != null ? ContainerMemory.dimension(level.dimension()) : null;
        if (player == null || level == null || memory == null) return;
        lastSnapshot = snapshot(player);
        List<Stop> stops = plan(memory, player.getEyePosition());
        Stop first = stops.isEmpty() ? null : stops.get(0);
        Stop before = route.isEmpty() ? null : route.get(0);
        route = stops;
        if (first == null) {
            if (!NEEDS.isEmpty()) FindKey.tell(minecraft, Component.translatable("gui.wheredidiputit.gather.none_stored"), false);
            return;
        }
        // Pointed out again only when the next stop changed, so it doesn't flash at every tick of the inventory.
        if (before == null || !before.record.key().equals(first.record.key()) || ContainerHighlight.current(level).isEmpty()) {
            ContainerHighlight.show(level, List.of(new ContainerHighlight.Found(first.record.pos(), first.item, first.count, first.record.seenAt(), first.record.entity())));
        }
    }

    // Greedy: the nearest container with something still wanted, then the nearest to that one, until nothing more is
    // wanted or nothing more is remembered.
    private static List<Stop> plan(DimensionMemory memory, Vec3 from) {
        List<Need> open = new ArrayList<>();
        List<long[]> left = new ArrayList<>();
        for (Need need : NEEDS) {
            int missing = need.wanted - have(need);
            if (missing > 0) {
                open.add(need);
                left.add(new long[]{missing});
            }
        }
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        Set<Object> visited = new HashSet<>();
        List<Stop> stops = new ArrayList<>();
        Vec3 at = from;
        while (true) {
            ContainerRecord best = null;
            double bestDistance = Double.MAX_VALUE;
            for (ContainerRecord record : memory.records()) {
                if (record.ignored() || visited.contains(record.key())) continue;
                double distance = Vec3.atCenterOf(record.pos()).distanceTo(at);
                if (distance >= bestDistance || Vec3.atCenterOf(record.pos()).distanceTo(from) > maxDistance) continue;
                if (useful(record, open, left)) {
                    best = record;
                    bestDistance = distance;
                }
            }
            if (best == null) return stops;
            visited.add(best.key());
            // Takes what it can here, the item it gives most of shown on the stop.
            ItemStack shown = ItemStack.EMPTY;
            long shownCount = 0;
            for (int i = 0; i < open.size(); i++) {
                long here = 0;
                for (StoredItem item : best.items()) {
                    if (open.get(i).test.test(item.stack())) here += item.count();
                }
                long take = Math.min(here, left.get(i)[0]);
                if (take <= 0) continue;
                left.get(i)[0] -= take;
                if (take > shownCount) {
                    shownCount = take;
                    shown = open.get(i).shown;
                }
            }
            stops.add(new Stop(best, shown, shownCount));
            at = Vec3.atCenterOf(best.pos());
        }
    }

    private static boolean useful(ContainerRecord record, List<Need> open, List<long[]> left) {
        for (int i = 0; i < open.size(); i++) {
            if (left.get(i)[0] <= 0) continue;
            Predicate<ItemStack> test = open.get(i).test;
            for (StoredItem item : record.items()) {
                if (test.test(item.stack())) return true;
            }
        }
        return false;
    }

    // An item and its NBT, hashed (1.20.1 has no ItemStack hash of its own).
    private static int hash(ItemStack stack) {
        return stack.isEmpty() ? 0 : 31 * stack.getItem().hashCode() + Objects.hashCode(stack.getTag());
    }

    // A line for one exact item: equal when the item and its NBT are.
    private record ItemKey(ItemStack stack) {
        @Override
        public boolean equals(Object other) {
            return other instanceof ItemKey key && ItemStack.isSameItemSameTags(key.stack, this.stack);
        }

        @Override
        public int hashCode() {
            return hash(this.stack);
        }
    }

    /** The next container on the route, or null; for the HUD. */
    public static @Nullable Stop next() {
        return route.isEmpty() ? null : route.get(0);
    }
}
