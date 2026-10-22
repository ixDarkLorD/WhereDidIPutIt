package net.ixdarklord.wheredidiputit.client.screen;

import net.ixdarklord.coolcatcanvas.api.client.utils.RenderUtils;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.ixdarklord.wheredidiputit.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The look the mod's screens share: a dark panel with a soft shadow, a header underlined in the highlight color, inset
 * slots, slim scrollbars, rounded badges and fields. One place for the colors, so the screens stay alike.
 */
public final class ScreenStyle {
    public static final int TEXT = 0xFFE6EDF3;
    public static final int MUTED = 0xFF8B949E;
    public static final int FAINT = 0xFF5A626D;
    public static final int ROW = 0xFF171C24;
    public static final int ROW_HOVER = 0xFF1E2530;
    public static final int ROW_SELECTED = 0xFF233042;
    public static final int DANGER = 0xFFE5534B;
    public static final int HEADER_HEIGHT = 24;

    private static final int SHADOW = 0x38000000;
    private static final int PANEL_TOP = 0xF41A2029;
    private static final int PANEL_BOTTOM = 0xF40F1319;
    private static final int BORDER = 0xFF2F3743;
    private static final int SHINE = 0x18FFFFFF;
    private static final int HEADER = 0xFF1C232D;
    private static final int SLOT = 0xFF0C1015;
    private static final int SLOT_DARK = 0xFF080A0E;
    private static final int SLOT_LIGHT = 0xFF2A313B;
    private static final int FIELD = 0xFF0C1015;
    private static final int TRACK = 0x40FFFFFF;

    private ScreenStyle() {
    }

    /** The highlight color, opaque: the screens' accent, so they match what they point out. */
    public static int accent() {
        return 0xFF000000 | WDIPIClientConfig.HIGHLIGHT_COLOR.get();
    }

    /** A color with the accent mixed in, {@code amount} of the way. */
    public static int tint(int color, float amount) {
        return ARGB.srgbLerp(amount, color, accent());
    }

    /** The panel: a shadow, a body darkening downwards, a border and a faint shine along the top. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, SHADOW);
        RenderUtils.fillVerticalGradient(graphics, x, y, width, height, PANEL_TOP, PANEL_BOTTOM);
        graphics.renderOutline(x, y, width, height, BORDER);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, SHINE);
    }

    /** The header strip: the title on the left, a quieter note on the right, and an accent line under both. */
    public static void header(GuiGraphics graphics, Font font, int x, int y, int width, Component title, Component note) {
        graphics.fill(x + 1, y + 1, x + width - 1, y + HEADER_HEIGHT, HEADER);
        graphics.drawString(font, title, x + 8, y + 8, TEXT, false);
        graphics.drawString(font, note, x + width - 8 - font.width(note), y + 8, MUTED, false);
        RenderUtils.fillHorizontalGradient(graphics, x + 1, y + HEADER_HEIGHT, width - 2, 1, accent(), accent() & 0x00FFFFFF);
    }

    /** A slot as the game's look sets one in: darker along the top and left, lighter along the bottom and right. */
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, SLOT);
        graphics.fill(x, y, x + 18, y + 1, SLOT_DARK);
        graphics.fill(x, y, x + 1, y + 18, SLOT_DARK);
        graphics.fill(x, y + 17, x + 18, y + 18, SLOT_LIGHT);
        graphics.fill(x + 17, y, x + 18, y + 18, SLOT_LIGHT);
    }

    /** A slot under the mouse: a light wash over what it holds. */
    /**
     * A slot's item with its decorations and its count in the bottom right corner, as the game draws them, except that a
     * count wider than the item ("128", "1.2k") is drawn smaller so it stays inside the slot.
     */
    public static void slotItem(GuiGraphics graphics, Font font, ItemStack stack, int x, int y, String count) {
        graphics.renderItem(stack, x, y);
        // The durability bar and cooldown, with no count: an empty text draws nothing.
        graphics.renderItemDecorations(font, stack, x, y, "");
        int width = font.width(count);
        float scale = width > 16 ? Math.max(0.5F, 16.0F / width) : 1.0F;
        graphics.pose().pushPose();
        // Anchored where the game puts it (right edge at x + 17, bottom at y + 17), and in front of the item as its own is.
        graphics.pose().translate(x + 17, y + 17, 200.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, count, -width, -8, 0xFFFFFFFF, true);
        graphics.pose().popPose();
    }

    public static void slotHover(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x50FFFFFF);
    }

    /** A rectangle with its corners cut by a pixel, which reads as rounded at the GUI's scale. */
    public static void rounded(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x + 1, y, x + width - 1, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /** A small rounded label; returns its width, to set the next one after it. */
    public static int badge(GuiGraphics graphics, Font font, Component text, int x, int y, int background, int color) {
        int width = badgeWidth(font, text);
        rounded(graphics, x, y, width, 11, background);
        graphics.drawString(font, text, x + 4, y + 2, color, false);
        return width;
    }

    /** How wide {@link #badge} draws this text. */
    public static int badgeWidth(Font font, Component text) {
        return font.width(text) + 8;
    }

    /** The well a text field sits in, its border in the accent while it has focus. */
    public static void field(GuiGraphics graphics, int x, int y, int width, int height, boolean focused) {
        rounded(graphics, x - 1, y - 1, width + 2, height + 2, focused ? accent() : BORDER);
        rounded(graphics, x, y, width, height, FIELD);
    }

    /** A slim scrollbar, shown only when there is more than fits. */
    public static void scrollbar(GuiGraphics graphics, int x, int y, int height, int first, int shown, int total) {
        if (total <= shown) return;
        graphics.fill(x, y, x + 2, y + height, TRACK);
        int thumb = Math.max(8, height * shown / total);
        int top = y + Mth.floor((height - thumb) * (first / (float) Math.max(1, total - shown)));
        graphics.fill(x, top, x + 2, top + thumb, accent());
    }

    /** The space between hints set side by side. */
    private static final int HINT_GAP = 10;

    /** How wide a hint is: its keycap, then what it does. */
    public static int hintWidth(Font font, Component key, Component action) {
        return font.width(key) + 8 + 4 + font.width(action);
    }

    /** Hints side by side, as one row centred on {@code centerX}: each a key and what it does. */
    public static void hints(GuiGraphics graphics, Font font, int centerX, int y, Component... keysAndActions) {
        int width = -HINT_GAP;
        for (int i = 0; i < keysAndActions.length; i += 2) width += hintWidth(font, keysAndActions[i], keysAndActions[i + 1]) + HINT_GAP;
        int x = centerX - width / 2;
        for (int i = 0; i < keysAndActions.length; i += 2) x = hint(graphics, font, keysAndActions[i], keysAndActions[i + 1], x, y);
    }

    /** A key shown as a keycap in a hint ("LMB"), then what it does; returns where the next hint starts. */
    public static int hint(GuiGraphics graphics, Font font, Component key, Component action, int x, int y) {
        int width = badge(graphics, font, key, x, y, 0xFF2A313B, TEXT);
        graphics.drawString(font, action, x + width + 4, y + 2, MUTED, false);
        return x + width + 4 + font.width(action) + HINT_GAP;
    }
}
