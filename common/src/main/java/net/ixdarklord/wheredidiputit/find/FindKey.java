package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
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
        find(Minecraft.getInstance(), hovered, all);
        return true;
    }

    private static void find(Minecraft minecraft, ItemStack query, boolean all) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        ItemMatcher matcher = ItemMatcher.of(query, WDIPIClientConfig.MATCH_MODE.get(), WDIPIClientConfig.INDEX_NESTED_CONTENTS.get());
        List<SearchHit> hits = memory == null ? List.of() : ContainerFinder.search(memory, matcher, player.getEyePosition(), maxDistance);
        if (hits.isEmpty()) {
            Component message = FindMessages.notFound(query, maxDistance);
            minecraft.gui.setOverlayMessage(message, false);
            ScreenNotice.show(message);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F));
            return;
        }
        // Nearest first, so the find key takes the first.
        List<SearchHit> shown = all ? hits : hits.subList(0, 1);
        if (WDIPIClientConfig.CLOSE_SCREEN_ON_SEARCH.get()) close(minecraft);
        ContainerHighlight.show(level, shown.stream().map(hit -> new ContainerHighlight.Found(hit.record().pos(), query, hit.count())).toList());
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
