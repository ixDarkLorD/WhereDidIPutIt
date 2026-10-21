package net.ixdarklord.wheredidiputit.client.screen;

import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.ScreenNotice;
import net.ixdarklord.wheredidiputit.find.StoredStacks;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds an item by name, for items the player isn't holding or hovering: every item remembered in the dimension's
 * containers, narrowed by what is typed ({@code #tag}, {@code @mod} or part of a name). Left click points out the
 * nearest container holding one, right click all of them, as the two find keys do.
 */
public final class SearchScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int SLOT = 18;
    private static final int PADDING = 8;
    private static final int PANEL_WIDTH = COLUMNS * SLOT + PADDING * 2;
    private static final int PANEL_HEIGHT = 20 + 18 + ROWS * SLOT + 22;
    private static final int PANEL_COLOR = 0xE0101418;
    private static final int BORDER_COLOR = 0xFF3C4450;
    private static final int SLOT_COLOR = 0xFF2A3038;
    private static final int HOVER_COLOR = 0x60FFFFFF;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int HINT_COLOR = 0xFF8A93A0;

    // What was typed last time, so reopening the screen picks up where it was.
    private static String lastQuery = "";

    private List<StoredStacks.Entry> all = List.of();
    private List<StoredStacks.Entry> shown = List.of();
    private int scroll;
    private int left;
    private int top;

    public SearchScreen() {
        super(Component.translatable("gui.wheredidiputit.search.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        DimensionMemory memory = this.minecraft.level != null ? ContainerMemory.dimension(this.minecraft.level.dimension()) : null;
        this.all = memory != null ? StoredStacks.collect(memory, WDIPIClientConfig.INDEX_NESTED_CONTENTS.get()) : List.of();
        EditBox box = new EditBox(this.font, this.left + PADDING, this.top + 20, PANEL_WIDTH - PADDING * 2, 16, this.title);
        box.setHint(Component.translatable("gui.wheredidiputit.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        box.setMaxLength(64);
        box.setValue(lastQuery);
        box.setResponder(query -> {
            lastQuery = query;
            this.filter();
        });
        this.addRenderableWidget(box);
        this.setInitialFocus(box);
        this.filter();
    }

    private void filter() {
        List<StoredStacks.Entry> matching = new ArrayList<>();
        for (StoredStacks.Entry entry : this.all) {
            if (StoredStacks.matches(entry.stack(), lastQuery)) matching.add(entry);
        }
        this.shown = matching;
        this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, (this.shown.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, PANEL_COLOR);
        graphics.outline(this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT, BORDER_COLOR);
        graphics.text(this.font, this.title, this.left + PADDING, this.top + 7, TEXT_COLOR, false);
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int gridTop = this.gridTop();
        int hovered = this.slotAt(mouseX, mouseY);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = this.left + PADDING + column * SLOT;
                int y = gridTop + row * SLOT;
                graphics.fill(x, y, x + SLOT - 1, y + SLOT - 1, SLOT_COLOR);
                int index = (this.scroll + row) * COLUMNS + column;
                if (index >= this.shown.size()) continue;
                StoredStacks.Entry entry = this.shown.get(index);
                graphics.item(entry.stack(), x, y);
                graphics.itemDecorations(this.font, entry.stack(), x, y, ItemMarker.count(entry.count()));
                if (index == hovered) graphics.fill(x, y, x + 16, y + 16, HOVER_COLOR);
            }
        }
        if (this.shown.isEmpty()) {
            Component empty = Component.translatable(this.all.isEmpty() ? "gui.wheredidiputit.search.nothing" : "gui.wheredidiputit.search.no_match");
            graphics.centeredText(this.font, empty, this.left + PANEL_WIDTH / 2, gridTop + ROWS * SLOT / 2 - 4, HINT_COLOR);
        }
        graphics.centeredText(this.font, Component.translatable("gui.wheredidiputit.search.help"), this.left + PANEL_WIDTH / 2,
                this.top + PANEL_HEIGHT - 14, HINT_COLOR);
        if (hovered >= 0) {
            StoredStacks.Entry entry = this.shown.get(hovered);
            List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, entry.stack()));
            lines.add(Component.translatable("gui.wheredidiputit.search.stored", entry.count(), entry.containers()).withStyle(ChatFormatting.GRAY));
            graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
        ScreenNotice.extract(graphics, this.left + PANEL_WIDTH / 2, this.top);
    }

    private int gridTop() {
        return this.top + 40;
    }

    // The index in the shown list of the slot under the mouse, or -1.
    private int slotAt(double mouseX, double mouseY) {
        int column = Mth.floor((mouseX - this.left - PADDING) / SLOT);
        int row = Mth.floor((mouseY - this.gridTop()) / SLOT);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS) return -1;
        int index = (this.scroll + row) * COLUMNS + column;
        return index < this.shown.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = this.slotAt(event.x(), event.y());
        if (index >= 0 && (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT || event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            FindKey.find(this.shown.get(index).stack(), event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
