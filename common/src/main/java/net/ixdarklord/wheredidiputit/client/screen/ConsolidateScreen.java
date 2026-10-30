package net.ixdarklord.wheredidiputit.client.screen;

import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.find.Consolidation;
import net.ixdarklord.wheredidiputit.find.FindMessages;
import net.ixdarklord.wheredidiputit.find.ScreenNotice;
import net.ixdarklord.wheredidiputit.find.TooltipLines;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The container browser's tidy-up view (author, 2026-10-28): every item scattered over two or more remembered
 * containers ({@link Consolidation}), spread over the most first, each with where it would best go (the container
 * already holding the most). Clicking one points out all its places, the target first, and says where to put it.
 * Drawn in the screens' shared look ({@link ScreenStyle}), and shrinks to a small window as the browser does.
 */
public final class ConsolidateScreen extends Screen {
    private static final int MAX_WIDTH = 300;
    private static final int MAX_HEIGHT = 250;
    private static final int MARGIN = 6;
    private static final int PADDING = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 32;
    private static final int FOOTER = 28;

    private final Screen parent;
    private List<Consolidation.Scattered> items = List.of();
    private int scroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int rows;

    public ConsolidateScreen(Screen parent) {
        super(Component.translatable("gui.wheredidiputit.consolidate.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(MAX_WIDTH, this.width - MARGIN * 2);
        this.panelHeight = Math.min(MAX_HEIGHT, this.height - MARGIN * 2);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.rows = Math.max(2, (this.panelHeight - LIST_TOP - FOOTER) / ROW_HEIGHT);
        DimensionMemory memory = this.minecraft.level != null ? ContainerMemory.dimension(this.minecraft.level.dimension()) : null;
        this.items = memory != null ? Consolidation.find(memory) : List.of();
        this.scroll = Mth.clamp(this.scroll, 0, Math.max(0, this.items.size() - this.rows));
        this.addRenderableWidget(new FlatButton(this.left + PADDING, this.top + this.panelHeight - PADDING - 16, 54, 16,
                Component.translatable("gui.wheredidiputit.consolidate.back"), FlatButton.Kind.PLAIN, this::onClose));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        ScreenStyle.panel(graphics, this.left, this.top, this.panelWidth, this.panelHeight);
        ScreenStyle.header(graphics, this.font, this.left, this.top, this.panelWidth, this.title,
                Component.translatable("gui.wheredidiputit.consolidate.count", this.items.size()));
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = this.left + PADDING;
        int rowWidth = this.panelWidth - PADDING * 2 - 4;
        int hovered = this.rowAt(mouseX, mouseY);
        for (int row = 0; row < this.rows; row++) {
            int index = this.scroll + row;
            if (index >= this.items.size()) break;
            Consolidation.Scattered item = this.items.get(index);
            int y = this.top + LIST_TOP + row * ROW_HEIGHT;
            ScreenStyle.rounded(graphics, x, y, rowWidth, ROW_HEIGHT - 2, index == hovered ? ScreenStyle.ROW_HOVER : ScreenStyle.ROW);
            graphics.renderItem(item.stack(), x + 3, y + 2);
            int textLeft = x + 24;
            int room = rowWidth - 26;
            graphics.drawString(this.font, this.font.plainSubstrByWidth(item.stack().getHoverName().getString(), room), textLeft, y + 2, ScreenStyle.TEXT, false);
            Component where = Component.translatable("gui.wheredidiputit.consolidate.row", TooltipLines.number(item.total()),
                    TooltipLines.containers(item.places().size()), FindMessages.name(item.target().record()));
            graphics.drawString(this.font, this.font.plainSubstrByWidth(where.getString(), room), textLeft, y + 11, ScreenStyle.FAINT, false);
        }
        if (this.items.isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.wheredidiputit.consolidate.empty"), this.left + this.panelWidth / 2,
                    this.top + LIST_TOP + this.rows * ROW_HEIGHT / 2 - 4, ScreenStyle.MUTED);
        }
        ScreenStyle.scrollbar(graphics, x + rowWidth + 2, this.top + LIST_TOP, this.rows * ROW_HEIGHT - 2, this.scroll, this.rows, this.items.size());
        Component key = Component.translatable("gui.wheredidiputit.consolidate.hint_key");
        Component action = Component.translatable("gui.wheredidiputit.consolidate.hint_action");
        int hintsLeft = this.left + PADDING + 54 + 6;
        if (ScreenStyle.hintWidth(this.font, key, action) <= this.left + this.panelWidth - PADDING - hintsLeft) {
            ScreenStyle.hints(graphics, this.font, (hintsLeft + this.left + this.panelWidth - PADDING) / 2, this.top + this.panelHeight - PADDING - 12, key, action);
        }
        ScreenNotice.extract(graphics, this.left + this.panelWidth / 2, this.top);
    }

    private int rowAt(double mouseX, double mouseY) {
        int x = this.left + PADDING;
        if (mouseX < x || mouseX >= x + this.panelWidth - PADDING * 2 - 4 || mouseY < this.top + LIST_TOP) return -1;
        int row = Mth.floor((mouseY - this.top - LIST_TOP) / ROW_HEIGHT);
        if (row >= this.rows) return -1;
        int index = this.scroll + row;
        return index < this.items.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.rowAt(mouseX, mouseY);
        if (index >= 0 && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.minecraft.level != null) {
            show(this.items.get(index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Points out every place the item is, the target first, and says to bring it there.
    private void show(Consolidation.Scattered item) {
        List<ContainerHighlight.Found> found = new ArrayList<>();
        for (Consolidation.Place place : item.places()) {
            found.add(new ContainerHighlight.Found(place.record().pos(), item.stack(), place.count(), place.record().seenAt()));
        }
        BlockPos target = item.target().record().pos();
        this.minecraft.setScreen(null);
        ContainerHighlight.show(this.minecraft.level, found, false);
        Component message = Component.translatable("gui.wheredidiputit.consolidate.into", item.stack().getHoverName(),
                FindMessages.name(item.target().record()), target.getX(), target.getY(), target.getZ());
        this.minecraft.gui.setOverlayMessage(message, false);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(x, y, scrollY);
        this.scroll = Mth.clamp(this.scroll - (int) Math.signum(scrollY), 0, Math.max(0, this.items.size() - this.rows));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
