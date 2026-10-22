package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.ContainerWatcher;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.network.FindResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.UUID;

/**
 * The find keys: hovering an item in a screen and pressing one points out where it is. {@link #FIND} (F) the nearest
 * remembered container holding it, {@link #FIND_ALL} (G) every one within the search distance. Outside screens the keys
 * do nothing, so sharing F with the game's offhand swap only takes that swap away inside screens.
 */
public final class FindKey {
    /** The keys' category in the controls screen, a translation key (1.21.1 categories are plain names). */
    public static final String CATEGORY = "key.category." + WhereDidIPutIt.MOD_ID + ".main";
    public static final KeyMapping FIND = new KeyMapping("key.wheredidiputit.find", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);
    public static final KeyMapping FIND_ALL = new KeyMapping("key.wheredidiputit.find_all", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    /** Every container holding something like the hovered item ({@link MatchMode#SIMILAR}); unbound by default. */
    public static final KeyMapping FIND_SIMILAR = new KeyMapping("key.wheredidiputit.find_similar", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    /** The nearest container with room for the hovered item ({@link SpaceFinder}); unbound by default. */
    public static final KeyMapping FIND_SPACE = new KeyMapping("key.wheredidiputit.find_space", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    /** Where the missing ingredients to craft the hovered item are ({@link RecipeNeeds}); unbound by default. */
    public static final KeyMapping FIND_INGREDIENTS = new KeyMapping("key.wheredidiputit.find_ingredients", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);

    /** Presses closer together than this are the key repeating while held, not new presses. */
    private static final long REPEAT_MILLIS = 250L;

    private static long lastPress;

    private FindKey() {
    }

    public static void init() {
        KeyMappingRegistry.register(FIND);
        KeyMappingRegistry.register(FIND_ALL);
        KeyMappingRegistry.register(FIND_SIMILAR);
        KeyMappingRegistry.register(FIND_SPACE);
        KeyMappingRegistry.register(FIND_INGREDIENTS);
    }

    /**
     * A key was pressed in a screen with {@code hovered} under the mouse; true when it was a find key and is used up.
     * Called from {@code AbstractContainerScreenMixin} and, for other screens, {@code KeyboardHandlerMixin}.
     */
    public static boolean onKeyPressed(int keyCode, int scanCode, ItemStack hovered) {
        if (hovered.isEmpty() || !WDIPIClientConfig.ENABLED.get()) return false;
        boolean all = FIND_ALL.matches(keyCode, scanCode);
        boolean similar = FIND_SIMILAR.matches(keyCode, scanCode);
        boolean space = FIND_SPACE.matches(keyCode, scanCode);
        boolean ingredients = FIND_INGREDIENTS.matches(keyCode, scanCode);
        if (!all && !similar && !space && !ingredients && !FIND.matches(keyCode, scanCode)) return false;
        long now = Util.getMillis();
        if (now - lastPress < REPEAT_MILLIS) return true;
        lastPress = now;
        if (ingredients) {
            RecipeNeeds.find(hovered);
        } else if (space) {
            findSpace(hovered);
        } else if (similar) {
            find(hovered, true, MatchMode.SIMILAR);
        } else {
            find(hovered, all);
        }
        return true;
    }

    /** Points out the nearest container holding {@code query}, or all of them: asks the server when it can, else memory. */
    public static void find(ItemStack query, boolean all) {
        find(query, all, WDIPIClientConfig.MATCH_MODE.get());
    }

    /** The same with a match mode of its own (the find similar key's). */
    public static void find(ItemStack query, boolean all, MatchMode mode) {
        if (LiveSearch.usable()) {
            LiveSearch.request(query, all, mode);
        } else {
            findInMemory(Minecraft.getInstance(), query, all, mode);
        }
    }

    /** Points out the nearest remembered container with room for {@code item}, one already holding some first. */
    public static void findSpace(ItemStack item) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        DimensionMemory memory = level != null ? ContainerMemory.dimension(level.dimension()) : null;
        if (player == null || level == null || memory == null) return;
        List<Found> found = new ArrayList<>();
        for (SpaceFinder.Room room : SpaceFinder.search(memory, item, player.getEyePosition(), WDIPIClientConfig.MAX_SEARCH_DISTANCE.get())) {
            found.add(new Found(room.record().pos(), item.copyWithCount(1), room.room(), room.distance(), room.record().seenAt(), room.record().entity()));
        }
        show(minecraft, level, item, false, found, Component.translatable("gui.wheredidiputit.no_room", item.getHoverName()));
    }

    /** Answers from memory alone: no server with the mod, live search off, or the server didn't answer. */
    static void findInMemory(Minecraft minecraft, ItemStack query, boolean all, MatchMode mode) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        List<Found> found = new ArrayList<>();
        for (SearchHit hit : remembered(level, query, mode, player.getEyePosition(), maxDistance)) {
            found.add(new Found(hit.record().pos(), hit.shown(), hit.count(), hit.distance(), hit.record().seenAt(), hit.record().entity()));
        }
        remember(level, query, mode, found);
        show(minecraft, level, query, all, found, maxDistance);
    }

    /**
     * Answers with what the server found round the player, which is up to date, and what is remembered beyond its
     * radius. Storage terminals stay remembered results even within it: live search doesn't see into networks.
     */
    static void findWithLive(Minecraft minecraft, ItemStack query, boolean all, MatchMode mode, FindResultPayload answer) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        Vec3 eyes = player.getEyePosition();
        List<Found> found = new ArrayList<>();
        Set<BlockPos> live = new HashSet<>();
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        for (FindResultPayload.Hit hit : answer.hits()) {
            // The server doesn't know what the player left out of searches.
            ContainerRecord known = memory != null ? memory.get(hit.pos()) : null;
            if (known != null && known.ignored()) continue;
            double distance = Vec3.atCenterOf(hit.pos()).distanceTo(eyes);
            if (distance > maxDistance) continue;
            live.add(hit.pos());
            found.add(new Found(hit.pos(), hit.shown(), hit.count(), distance, ContainerHighlight.LIVE, null));
        }
        for (SearchHit hit : remembered(level, query, mode, eyes, maxDistance)) {
            // Beyond the server's reach, a storage network (live search can't look into one) or an entity (it looks
            // at blocks only): what is remembered is all there is.
            boolean beyond = hit.distance() > answer.radius();
            if ((beyond || hit.record().network() || hit.record().entity() != null) && !live.contains(hit.record().pos())) {
                found.add(new Found(hit.record().pos(), hit.shown(), hit.count(), hit.distance(), hit.record().seenAt(), hit.record().entity()));
            }
        }
        found.sort(Comparator.comparingDouble(Found::distance));
        remember(level, query, mode, found);
        show(minecraft, level, query, all, found, maxDistance);
    }

    // What a plain find turned up, for the item's tooltip (SearchHistory). A "similar" find matched other items, so it
    // says nothing about this one.
    private static void remember(ClientLevel level, ItemStack query, MatchMode mode, List<Found> found) {
        if (mode == MatchMode.SIMILAR) return;
        List<SearchHistory.Place> places = new ArrayList<>(found.size());
        for (Found hit : found) {
            Entity entity = hit.entity() != null ? ContainerWatcher.entity(level, hit.entity()) : null;
            Component name = entity != null ? entity.getName() : level.getBlockState(hit.pos()).getBlock().getName();
            places.add(new SearchHistory.Place(hit.pos(), name, hit.count()));
        }
        SearchHistory.record(level.dimension(), query, places);
    }

    private static List<SearchHit> remembered(ClientLevel level, ItemStack query, MatchMode mode, Vec3 from, int maxDistance) {
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        if (memory == null) return List.of();
        ItemMatcher matcher = ItemMatcher.of(query, mode, WDIPIClientConfig.INDEX_NESTED_CONTENTS.get());
        return ContainerFinder.search(memory, matcher, from, maxDistance);
    }

    // Points out the containers found (nearest first), or says nothing has the item.
    private static void show(Minecraft minecraft, ClientLevel level, ItemStack query, boolean all, List<Found> found, int maxDistance) {
        show(minecraft, level, query, all, found, FindMessages.notFound(query, maxDistance));
    }

    private static void show(Minecraft minecraft, ClientLevel level, ItemStack query, boolean all, List<Found> found, Component notFound) {
        if (found.isEmpty()) {
            tell(minecraft, notFound, false);
            return;
        }
        List<Found> shown = all ? found : found.subList(0, 1);
        highlight(minecraft, level, shown.stream()
                .map(hit -> new ContainerHighlight.Found(hit.pos(), hit.item().isEmpty() ? query : hit.item(), hit.count(), hit.seenAt(), hit.entity())).toList());
    }

    /** Points out containers, closing the open screen first when the settings say so. */
    static void highlight(Minecraft minecraft, ClientLevel level, List<ContainerHighlight.Found> found) {
        if (WDIPIClientConfig.CLOSE_SCREEN_ON_SEARCH.get()) close(minecraft);
        ContainerHighlight.show(level, found);
    }

    /** Says something on the action bar and over the open screen: with a low note when it is a failure, a soft one when not. */
    static void tell(Minecraft minecraft, Component message, boolean good) {
        minecraft.gui.setOverlayMessage(message, false);
        ScreenNotice.show(message);
        minecraft.getSoundManager().play(good ? SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2F, 0.6F)
                : SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F));
    }

    // A container found, from memory or from the server.
    private record Found(BlockPos pos, ItemStack item, long count, double distance, long seenAt, @Nullable UUID entity) {
    }

    private static void close(Minecraft minecraft) {
        // Through the player, so the server closes the open menu too, also when a recipe screen was opened over it.
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        } else {
            minecraft.setScreen(null);
        }
    }
}
