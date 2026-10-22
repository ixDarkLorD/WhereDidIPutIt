package net.ixdarklord.wheredidiputit.client.screen;

import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.ScreenNotice;
import net.ixdarklord.wheredidiputit.find.StoredStacks;
import net.ixdarklord.wheredidiputit.find.TooltipLines;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds an item by name, for items the player isn't holding or hovering: every item remembered in the dimension's
 * containers, narrowed by what is typed ({@code #tag}, {@code @mod} or part of a name). Left click points out the
 * nearest container holding one, right click all of them, as the two find keys do. Drawn in the screens' shared look
 * ({@link ScreenStyle}).
 */
public final class SearchScreen extends Screen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int SLOT = 18;
    private static final int PADDING = 8;
    private static final int PANEL_WIDTH = COLUMNS * SLOT + PADDING * 2 + 6;
    private static final int GRID_TOP = 54;
    private static final int PANEL_HEIGHT = GRID_TOP + ROWS * SLOT + 26;

    // What was typed last time, so reopening the screen picks up where it was.
    private static String lastQuery = "";

    private List<StoredStacks.Entry> all = List.of();
    private List<StoredStacks.Entry> shown = List.of();
    private EditBox search;
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
        this.search = new EditBox(this.font, this.left + PADDING + 5, this.top + 35, PANEL_WIDTH - PADDING * 2 - 10, 10, this.title);
        this.search.setBordered(false);
        this.search.setTextColor(ScreenStyle.TEXT);
        this.search.setHint(Component.translatable("gui.wheredidiputit.search.hint").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(64);
        this.search.setValue(lastQuery);
        this.search.setResponder(query -> {
            lastQuery = query;
            this.filter();
        });
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);
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
        return Math.max(0, this.rows() - ROWS);
    }

    private int rows() {
        return (this.shown.size() + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        ScreenStyle.panel(graphics, this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT);
        ScreenStyle.header(graphics, this.font, this.left, this.top, PANEL_WIDTH, this.title,
                Component.translatable("gui.wheredidiputit.search.count", this.shown.size()));
        ScreenStyle.field(graphics, this.left + PADDING, this.top + 31, PANEL_WIDTH - PADDING * 2, 16, this.search.isFocused());
        super.render(graphics, mouseX, mouseY, partialTick);

        int gridLeft = this.left + PADDING;
        int gridTop = this.top + GRID_TOP;
        int hovered = this.slotAt(mouseX, mouseY);
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = gridLeft + column * SLOT;
                int y = gridTop + row * SLOT;
                ScreenStyle.slot(graphics, x, y);
                int index = (this.scroll + row) * COLUMNS + column;
                if (index >= this.shown.size()) continue;
                StoredStacks.Entry entry = this.shown.get(index);
                ScreenStyle.slotItem(graphics, this.font, entry.stack(), x + 1, y + 1, ItemMarker.count(entry.count()));
                if (index == hovered) ScreenStyle.slotHover(graphics, x, y);
            }
        }
        ScreenStyle.scrollbar(graphics, gridLeft + COLUMNS * SLOT + 2, gridTop, ROWS * SLOT, this.scroll, ROWS, this.rows());
        if (this.shown.isEmpty()) {
            Component empty = Component.translatable(this.all.isEmpty() ? "gui.wheredidiputit.search.nothing" : "gui.wheredidiputit.search.no_match");
            int middle = gridLeft + COLUMNS * SLOT / 2;
            graphics.fill(gridLeft + 1, gridTop + ROWS * SLOT / 2 - 9, gridLeft + COLUMNS * SLOT - 1, gridTop + ROWS * SLOT / 2 + 9, 0xC00C1015);
            graphics.drawCenteredString(this.font, empty, middle, gridTop + ROWS * SLOT / 2 - 4, ScreenStyle.MUTED);
        }
        int hintY = this.top + PANEL_HEIGHT - PADDING - 11;
        ScreenStyle.hints(graphics, this.font, this.left + PANEL_WIDTH / 2, hintY,
                Component.translatable("gui.wheredidiputit.search.left_key"), Component.translatable("gui.wheredidiputit.search.left_action"),
                Component.translatable("gui.wheredidiputit.search.right_key"), Component.translatable("gui.wheredidiputit.search.right_action"));
        if (hovered >= 0) {
            StoredStacks.Entry entry = this.shown.get(hovered);
            List<Component> lines = new ArrayList<>(getTooltipFromItem(this.minecraft, entry.stack()));
            lines.add(Component.empty());
            lines.add(TooltipLines.line("stored", TooltipLines.stored(entry.count(), entry.containers())));
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
        ScreenNotice.extract(graphics, this.left + PANEL_WIDTH / 2, this.top);
    }

    // The index in the shown list of the slot under the mouse, or -1.
    private int slotAt(double mouseX, double mouseY) {
        int column = Mth.floor((mouseX - this.left - PADDING) / SLOT);
        int row = Mth.floor((mouseY - this.top - GRID_TOP) / SLOT);
        if (column < 0 || column >= COLUMNS || row < 0 || row >= ROWS || mouseX < this.left + PADDING || mouseY < this.top + GRID_TOP) return -1;
        int index = (this.scroll + row) * COLUMNS + column;
        return index < this.shown.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.slotAt(mouseX, mouseY);
        if (index >= 0 && (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
            FindKey.find(this.shown.get(index).stack(), button == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollY) {
        if (scrollY != 0.0) {
            this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, this.maxScroll());
            return true;
        }
        return super.mouseScrolled(x, y, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
