package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.network.FindResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * The find keys: hovering an item in a screen and pressing one points out where it is. {@link #FIND} (F) the nearest
 * remembered container holding it, {@link #FIND_ALL} (G) every one within the search distance. Outside screens the keys
 * do nothing, so sharing F with the game's offhand swap only takes that swap away inside screens.
 */
public final class FindKey {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(WhereDidIPutIt.id("main"));
    public static final KeyMapping FIND = new KeyMapping("key.wheredidiputit.find", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);
    public static final KeyMapping FIND_ALL = new KeyMapping("key.wheredidiputit.find_all", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);

    /** Presses closer together than this are the key repeating while held, not new presses. */
    private static final long REPEAT_MILLIS = 250L;

    private static long lastPress;

    private FindKey() {
    }

    public static void init() {
        KeyMappingRegistry.register(FIND);
        KeyMappingRegistry.register(FIND_ALL);
    }

    /**
     * A key was pressed in a screen with {@code hovered} under the mouse; true when it was a find key and is used up.
     * Called from {@code AbstractContainerScreenMixin} and, for other screens, {@code KeyboardHandlerMixin}.
     */
    public static boolean onKeyPressed(KeyEvent event, ItemStack hovered) {
        if (hovered.isEmpty() || !WDIPIClientConfig.ENABLED.get()) return false;
        boolean all = FIND_ALL.matches(event);
        if (!all && !FIND.matches(event)) return false;
        long now = Util.getMillis();
        if (now - lastPress < REPEAT_MILLIS) return true;
        lastPress = now;
        find(hovered, all);
        return true;
    }

    /** Points out the nearest container holding {@code query}, or all of them: asks the server when it can, else memory. */
    public static void find(ItemStack query, boolean all) {
        if (LiveSearch.usable()) {
            LiveSearch.request(query, all);
        } else {
            findInMemory(Minecraft.getInstance(), query, all);
        }
    }

    /** Answers from memory alone: no server with the mod, live search off, or the server didn't answer. */
    static void findInMemory(Minecraft minecraft, ItemStack query, boolean all) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        List<Found> found = new ArrayList<>();
        for (SearchHit hit : remembered(level, query, player.getEyePosition(), maxDistance)) {
            found.add(new Found(hit.record().pos(), hit.shown(), hit.count(), hit.distance(), hit.record().seenAt()));
        }
        show(minecraft, level, query, all, found, maxDistance);
    }

    /**
     * Answers with what the server found round the player, which is up to date, and what is remembered beyond its
     * radius. Storage terminals stay remembered results even within it: live search doesn't see into networks.
     */
    static void findWithLive(Minecraft minecraft, ItemStack query, boolean all, FindResultPayload answer) {
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
            found.add(new Found(hit.pos(), hit.shown(), hit.count(), distance, ContainerHighlight.LIVE));
        }
        for (SearchHit hit : remembered(level, query, eyes, maxDistance)) {
            boolean beyond = hit.distance() > answer.radius();
            if ((beyond || hit.record().network()) && !live.contains(hit.record().pos())) {
                found.add(new Found(hit.record().pos(), hit.shown(), hit.count(), hit.distance(), hit.record().seenAt()));
            }
        }
        found.sort(Comparator.comparingDouble(Found::distance));
        show(minecraft, level, query, all, found, maxDistance);
    }

    private static List<SearchHit> remembered(ClientLevel level, ItemStack query, Vec3 from, int maxDistance) {
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        if (memory == null) return List.of();
        ItemMatcher matcher = ItemMatcher.of(query, WDIPIClientConfig.MATCH_MODE.get(), WDIPIClientConfig.INDEX_NESTED_CONTENTS.get());
        return ContainerFinder.search(memory, matcher, from, maxDistance);
    }

    // Points out the containers found (nearest first), or says nothing has the item.
    private static void show(Minecraft minecraft, ClientLevel level, ItemStack query, boolean all, List<Found> found, int maxDistance) {
        if (found.isEmpty()) {
            Component message = FindMessages.notFound(query, maxDistance);
            minecraft.gui.setOverlayMessage(message, false);
            ScreenNotice.show(message);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F));
            return;
        }
        List<Found> shown = all ? found : found.subList(0, 1);
        if (WDIPIClientConfig.CLOSE_SCREEN_ON_SEARCH.get()) close(minecraft);
        ContainerHighlight.show(level, shown.stream()
                .map(hit -> new ContainerHighlight.Found(hit.pos(), hit.item().isEmpty() ? query : hit.item(), hit.count(), hit.seenAt())).toList());
    }

    // A container found, from memory or from the server.
    private record Found(BlockPos pos, ItemStack item, long count, double distance, long seenAt) {
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
