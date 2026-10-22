package net.ixdarklord.wheredidiputit.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.ixdarklord.coolcatcore.api.client.registry.KeyMappingRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.client.screen.ScreenStyle;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.HeldKeyMapping;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * While its key is held (unbound by default), the container being pointed out nearest the crosshair shows what it held
 * when last seen, beside its item marker: a grid of its slots, those holding the item found framed in the highlight
 * color. Only remembered containers have contents to show: one the server found but the player never opened has none.
 */
public final class ContentsPreview {
    public static final HeldKeyMapping KEY = new HeldKeyMapping("key.wheredidiputit.preview", InputConstants.UNKNOWN.getValue(), FindKey.CATEGORY);

    private static final int SLOT = 18;
    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 3;
    private static final int PADDING = 3;
    /** How near the crosshair, in GUI pixels, a marker must be to have its contents shown. */
    private static final float REACH = 120.0F;
    private static final int PANEL_COLOR = 0xC0101418;
    private static final int BORDER_COLOR = 0xFF3C4450;
    private static final int TEXT_COLOR = 0xFFC0C0C0;

    private ContentsPreview() {
    }

    public static void init() {
        KeyMappingRegistry.register(KEY);
        ClientGuiEvents.RENDER_HUD.register(ContentsPreview::render);
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !KEY.isHeld()) return;
        DimensionMemory memory = ContainerMemory.dimension(minecraft.level.dimension());
        if (memory == null) return;
        ContainerHighlight.Target nearest = null;
        ScreenProjection nearestOn = null;
        float best = REACH;
        for (ContainerHighlight.Target target : ContainerHighlight.current(minecraft.level)) {
            ScreenProjection on = ScreenProjection.of(minecraft, graphics, target.box().getCenter());
            if (!on.inView()) continue;
            float off = (float) Math.hypot(on.x() - graphics.guiWidth() / 2.0F, on.y() - graphics.guiHeight() / 2.0F);
            if (off < best) {
                best = off;
                nearest = target;
                nearestOn = on;
            }
        }
        if (nearest == null) return;
        ContainerRecord record = nearest.entity() != null ? memory.getEntity(nearest.entity()) : memory.get(nearest.pos());
        if (record == null) return;
        // Right of the marker and of the "seen ... ago" line under it.
        draw(graphics, minecraft, record, nearest.item(), Math.round(nearestOn.x()) + 26, Math.round(nearestOn.y()) - 8);
    }

    private static void draw(GuiGraphics graphics, Minecraft minecraft, ContainerRecord record, ItemStack found, int x, int y) {
        List<StoredItem> items = record.items();
        int shown = Math.min(items.size(), COLUMNS * MAX_ROWS);
        int columns = Math.max(1, Math.min(COLUMNS, shown));
        int rows = Math.max(1, (shown + COLUMNS - 1) / COLUMNS);
        int width = columns * SLOT + PADDING * 2;
        int height = rows * SLOT + PADDING * 2 + (items.size() > shown ? 10 : 0);
        // Kept on screen: to the left of the marker when there is no room to its right.
        if (x + width > graphics.guiWidth()) x -= width + 52;
        y = Math.max(2, Math.min(y, graphics.guiHeight() - height - 2));
        graphics.fill(x, y, x + width, y + height, PANEL_COLOR);
        graphics.renderOutline(x, y, width, height, BORDER_COLOR);
        int frame = 0xFF000000 | WDIPIClientConfig.HIGHLIGHT_COLOR.get();
        for (int i = 0; i < shown; i++) {
            StoredItem item = items.get(i);
            int slotX = x + PADDING + (i % COLUMNS) * SLOT;
            int slotY = y + PADDING + (i / COLUMNS) * SLOT;
            ScreenStyle.slotItem(graphics, minecraft.font, item.stack(), slotX + 1, slotY + 1, ItemMarker.count(item.count()));
            if (ItemStack.isSameItem(item.stack(), found)) graphics.renderOutline(slotX, slotY, SLOT, SLOT, frame);
        }
        if (items.isEmpty()) graphics.drawString(minecraft.font, Component.translatable("gui.wheredidiputit.browser.empty_container"), x + PADDING, y + PADDING + 4, TEXT_COLOR);
        if (items.size() > shown) {
            graphics.drawString(minecraft.font, Component.translatable("gui.wheredidiputit.preview.more", items.size() - shown), x + PADDING, y + height - 10, TEXT_COLOR);
        }
    }
}
