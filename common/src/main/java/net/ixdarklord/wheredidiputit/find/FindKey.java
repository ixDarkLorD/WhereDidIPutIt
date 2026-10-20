package net.ixdarklord.wheredidiputit.find;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The find key: hovering an item in a screen and pressing it points out the nearest remembered container holding it.
 * Pressing it again on the same item moves on to the next-nearest. Outside screens the key does nothing, so sharing
 * F with the game's offhand swap only takes that swap away inside screens.
 */
public final class FindKey {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(WhereDidIPutIt.id("main"));
    public static final KeyMapping FIND = new KeyMapping("key.wheredidiputit.find", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F, CATEGORY);

    /** How long after a search pressing the key on the same item cycles instead of searching anew. */
    private static final long CYCLE_WINDOW_MILLIS = 120_000L;
    /** Presses closer together than this are the key repeating while held, not new presses. */
    private static final long REPEAT_MILLIS = 250L;

    private static @Nullable Session session;
    private static long lastPress;

    private FindKey() {
    }

    public static void init() {
        KeyMappingRegistry.register(FIND);
    }

    /**
     * A key was pressed in a screen with {@code hovered} under the mouse; true when it was the find key and is used up.
     * Called from {@code AbstractContainerScreenMixin} and, for other screens, {@code KeyboardHandlerMixin}.
     */
    public static boolean onKeyPressed(KeyEvent event, ItemStack hovered) {
        if (hovered.isEmpty() || !FIND.matches(event) || !WDIPIClientConfig.ENABLED.get()) return false;
        long now = Util.getMillis();
        if (now - lastPress < REPEAT_MILLIS) return true;
        lastPress = now;
        find(Minecraft.getInstance(), hovered);
        return true;
    }

    private static void find(Minecraft minecraft, ItemStack query) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return;
        DimensionMemory memory = ContainerMemory.dimension(level.dimension());
        MatchMode mode = WDIPIClientConfig.MATCH_MODE.get();
        int maxDistance = WDIPIClientConfig.MAX_SEARCH_DISTANCE.get();
        Vec3 from = player.getEyePosition();

        // Searched afresh every time, so what changed since counts; pressing again on the same item moves on to the
        // container after the one shown last.
        List<SearchHit> hits = memory == null ? List.of()
                : ContainerFinder.search(memory, ItemMatcher.of(query, mode, WDIPIClientConfig.INDEX_NESTED_CONTENTS.get()), from, maxDistance);
        if (hits.isEmpty()) {
            session = null;
            Component message = FindMessages.notFound(query, maxDistance);
            minecraft.gui.setOverlayMessage(message, false);
            ScreenNotice.show(message);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F));
            return;
        }
        int index = 0;
        Session last = session;
        if (last != null && last.continues(query, mode, level.dimension())) {
            for (int i = 0; i < hits.size(); i++) {
                if (hits.get(i).record().pos().equals(last.shown)) index = (i + 1) % hits.size();
            }
        }
        SearchHit hit = hits.get(index);
        session = new Session(query.copy(), mode, level.dimension(), hit.record().pos());

        if (WDIPIClientConfig.CLOSE_SCREEN_ON_SEARCH.get()) close(minecraft);
        ContainerHighlight.show(level, hit.record().pos());
        if (WDIPIClientConfig.ACTION_BAR_INFO.get()) {
            minecraft.gui.setOverlayMessage(FindMessages.found(hit, index, hits.size(), from), false);
        }
    }

    private static void close(Minecraft minecraft) {
        // Through the player, so the server closes the open menu too, also when a recipe screen was opened over it.
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        } else {
            minecraft.setScreen(null);
        }
    }

    /**
     * The last search, kept so pressing the key again on the same item steps on from it.
     *
     * @param shown the container it pointed out
     */
    private record Session(ItemStack query, MatchMode mode, ResourceKey<Level> dimension, BlockPos shown, long time) {
        private Session(ItemStack query, MatchMode mode, ResourceKey<Level> dimension, BlockPos shown) {
            this(query, mode, dimension, shown, Util.getMillis());
        }

        private boolean continues(ItemStack query, MatchMode mode, ResourceKey<Level> dimension) {
            return this.mode == mode && this.dimension == dimension && ItemStack.isSameItemSameComponents(this.query, query)
                    && Util.getMillis() - this.time < CYCLE_WINDOW_MILLIS;
        }
    }
}
