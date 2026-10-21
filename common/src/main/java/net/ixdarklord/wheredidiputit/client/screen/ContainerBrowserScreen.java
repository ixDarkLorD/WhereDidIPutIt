package net.ixdarklord.wheredidiputit.client.screen;

import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.find.FindMessages;
import net.ixdarklord.wheredidiputit.find.StoredStacks;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Every container remembered in the player's dimension, nearest first, narrowed by name or by what they hold. The one
 * picked shows what it held when last seen, and can be pointed out, left out of searches (or taken back in) or
 * forgotten.
 */
public final class ContainerBrowserScreen extends Screen {
    private static final int LIST_WIDTH = 190;
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_ROWS = 8;
    private static final int COLUMNS = 9;
    private static final int GRID_ROWS = 5;
    private static final int SLOT = 18;
    private static final int PADDING = 8;
    private static final int DETAIL_WIDTH = COLUMNS * SLOT + PADDING * 2;
    private static final int PANEL_WIDTH = LIST_WIDTH + DETAIL_WIDTH + PADDING;
    private static final int PANEL_HEIGHT = 40 + LIST_ROWS * ROW_HEIGHT + PADDING;
    private static final int PANEL_COLOR = 0xE0101418;
    private static final int BORDER_COLOR = 0xFF3C4450;
    private static final int ROW_COLOR = 0xFF20262E;
    private static final int SELECTED_COLOR = 0xFF34404E;
    private static final int HOVER_COLOR = 0x30FFFFFF;
    private static final int SLOT_COLOR = 0xFF2A3038;
    private static final int TEXT_COLOR = 0xFFE0E0E0;
    private static final int DIM_COLOR = 0xFF8A93A0;

    private static String lastQuery = "";

    private @Nullable DimensionMemory memory;
    private List<ContainerRecord> shown = List.of();
    private @Nullable BlockPos selected;
    private int listScroll;
    private int gridScroll;
    private int left;
    private int top;
    private Button showButton;
    private Button ignoreButton;
    private Button forgetButton;

    public ContainerBrowserScreen() {
        super(Component.translatable("gui.wheredidiputit.browser.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.memory = this.minecraft.level != null ? ContainerMemory.dimension(this.minecraft.level.dimension()) : null;
        EditBox box = new EditBox(this.font, this.left + PADDING, this.top + 20, LIST_WIDTH - PADDING, 16, this.title);
        box.setHint(Component.translatable("gui.wheredidiputit.browser.hint").withStyle(ChatFormatting.DARK_GRAY));
        box.setMaxLength(64);
        box.setValue(lastQuery);
        box.setResponder(query -> {
            lastQuery = query;
            this.refresh();
        });
        this.addRenderableWidget(box);
        this.setInitialFocus(box);

        int detailLeft = this.detailLeft();
        int buttonY = this.top + PANEL_HEIGHT - PADDING - 20;
        int buttonWidth = (DETAIL_WIDTH - PADDING * 2 - 4) / 3;
        this.showButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.wheredidiputit.browser.show"), button -> this.showSelected())
                .bounds(detailLeft + PADDING, buttonY, buttonWidth, 20).build());
        this.ignoreButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> this.toggleIgnored())
                .bounds(detailLeft + PADDING + buttonWidth + 2, buttonY, buttonWidth, 20).build());
        this.forgetButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.wheredidiputit.browser.forget"), button -> this.forgetSelected())
                .bounds(detailLeft + PADDING + (buttonWidth + 2) * 2, buttonY, buttonWidth, 20).build());
        this.refresh();
    }

    // The list again, after the query or memory changed.
    private void refresh() {
        List<ContainerRecord> matching = new ArrayList<>();
        if (this.memory != null) {
            for (ContainerRecord record : this.memory.records()) {
                if (this.fits(record)) matching.add(record);
            }
        }
        Vec3 from = this.minecraft.player != null ? this.minecraft.player.position() : Vec3.ZERO;
        matching.sort(Comparator.comparingDouble(record -> Vec3.atCenterOf(record.pos()).distanceToSqr(from)));
        this.shown = matching;
        this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, this.shown.size() - LIST_ROWS));
        if (this.selected() == null) this.selected = null;
        this.updateButtons();
    }

    // Whether a container fits the query: by its name, or by anything it holds.
    private boolean fits(ContainerRecord record) {
        String query = lastQuery.trim();
        if (query.isEmpty()) return true;
        if (!query.startsWith("#") && !query.startsWith("@") && FindMessages.name(record).getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
            return true;
        }
        for (StoredItem item : record.items()) {
            if (StoredStacks.matches(item.stack(), query)) return true;
        }
        return false;
    }

    private @Nullable ContainerRecord selected() {
        return this.selected != null && this.memory != null ? this.memory.get(this.selected) : null;
    }

    private void updateButtons() {
        ContainerRecord record = this.selected();
        boolean any = record != null;
        this.showButton.active = any;
        this.ignoreButton.active = any;
        this.forgetButton.active = any;
        this.ignoreButton.setMessage(Component.translatable(record != null && record.ignored() ? "gui.wheredidiputit.browser.include" : "gui.wheredidiputit.browser.leave_out"));
        this.gridScroll = 0;
    }

    private void showSelected() {
        ContainerRecord record = this.selected();
        if (record == null || this.minecraft.level == null) return;
        long total = record.items().stream().mapToLong(StoredItem::count).sum();
        ContainerHighlight.show(this.minecraft.level, List.of(new ContainerHighlight.Found(record.pos(), icon(record), total, record.seenAt())));
        this.onClose();
    }

    private void toggleIgnored() {
        ContainerRecord record = this.selected();
        if (record == null || this.memory == null) return;
        this.memory.put(record.withIgnored(!record.ignored()));
        this.refresh();
    }

    private void forgetSelected() {
        ContainerRecord record = this.selected();
        if (record == null || this.memory == null) return;
        this.memory.remove(record.pos());
        this.selected = null;
        this.refresh();
    }

    private static ItemStack icon(ContainerRecord record) {
        ItemStack stack = new ItemStack(BuiltInRegistries.BLOCK.getValue(record.block()).asItem());
        return stack.isEmpty() ? new ItemStack(Items.CHEST) : stack;
    }

    private int detailLeft() {
        return this.left + LIST_WIDTH + PADDING;
    }

    private int listTop() {
        return this.top + 40;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(this.left, this.top, this.left + PANEL_WIDTH, this.top + PANEL_HEIGHT, PANEL_COLOR);
        graphics.outline(this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT, BORDER_COLOR);
        graphics.text(this.font, this.title, this.left + PADDING, this.top + 7, TEXT_COLOR, false);
        graphics.text(this.font, Component.translatable("gui.wheredidiputit.browser.count", this.shown.size()), this.detailLeft() + PADDING,
                this.top + 7, DIM_COLOR, false);
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractList(graphics, mouseX, mouseY);
        this.extractDetail(graphics, mouseX, mouseY);
    }

    private void extractList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.left + PADDING;
        int hovered = this.rowAt(mouseX, mouseY);
        Vec3 from = this.minecraft.player != null ? this.minecraft.player.position() : Vec3.ZERO;
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.listScroll + row;
            if (index >= this.shown.size()) break;
            ContainerRecord record = this.shown.get(index);
            int y = this.listTop() + row * ROW_HEIGHT;
            graphics.fill(x, y, x + LIST_WIDTH - PADDING, y + ROW_HEIGHT - 2, record.pos().equals(this.selected) ? SELECTED_COLOR : ROW_COLOR);
            if (index == hovered) graphics.fill(x, y, x + LIST_WIDTH - PADDING, y + ROW_HEIGHT - 2, HOVER_COLOR);
            graphics.item(icon(record), x + 2, y + 2);
            int textColor = record.ignored() ? DIM_COLOR : TEXT_COLOR;
            Component name = FindMessages.name(record);
            if (record.ignored()) name = Component.translatable("gui.wheredidiputit.browser.left_out", name);
            graphics.text(this.font, this.font.plainSubstrByWidth(name.getString(), LIST_WIDTH - PADDING - 26), x + 22, y + 2, textColor, false);
            int distance = Mth.floor(Vec3.atCenterOf(record.pos()).distanceTo(from));
            Component age = ItemMarker.age(System.currentTimeMillis() - record.seenAt());
            Component detail = Component.translatable("gui.wheredidiputit.browser.row", distance,
                    age != null ? age : Component.translatable("gui.wheredidiputit.browser.just_now"));
            graphics.text(this.font, detail, x + 22, y + 11, DIM_COLOR, false);
        }
        if (this.shown.isEmpty()) {
            graphics.centeredText(this.font, Component.translatable("gui.wheredidiputit.browser.empty"), this.left + PADDING + (LIST_WIDTH - PADDING) / 2,
                    this.listTop() + LIST_ROWS * ROW_HEIGHT / 2 - 4, DIM_COLOR);
        }
    }

    private void extractDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ContainerRecord record = this.selected();
        int x = this.detailLeft() + PADDING;
        int y = this.listTop();
        if (record == null) {
            graphics.text(this.font, Component.translatable("gui.wheredidiputit.browser.pick"), x, y, DIM_COLOR, false);
            return;
        }
        BlockPos pos = record.pos();
        graphics.text(this.font, this.font.plainSubstrByWidth(FindMessages.name(record).getString(), DETAIL_WIDTH - PADDING * 2), x, y, TEXT_COLOR, false);
        graphics.text(this.font, Component.translatable("gui.wheredidiputit.browser.position", pos.getX(), pos.getY(), pos.getZ()), x, y + 10, DIM_COLOR, false);
        int gridTop = y + 24;
        StoredItem hoveredItem = null;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int slotX = x + column * SLOT;
                int slotY = gridTop + row * SLOT;
                graphics.fill(slotX, slotY, slotX + SLOT - 1, slotY + SLOT - 1, SLOT_COLOR);
                int index = (this.gridScroll + row) * COLUMNS + column;
                if (index >= record.items().size()) continue;
                StoredItem item = record.items().get(index);
                graphics.item(item.stack(), slotX, slotY);
                graphics.itemDecorations(this.font, item.stack(), slotX, slotY, ItemMarker.count(item.count()));
                if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) hoveredItem = item;
            }
        }
        if (record.items().isEmpty()) graphics.text(this.font, Component.translatable("gui.wheredidiputit.browser.empty_container"), x, gridTop + 4, DIM_COLOR, false);
        if (hoveredItem != null) graphics.setTooltipForNextFrame(this.font, hoveredItem.stack(), mouseX, mouseY);
    }

    // The index in the shown list of the row under the mouse, or -1.
    private int rowAt(double mouseX, double mouseY) {
        int x = this.left + PADDING;
        if (mouseX < x || mouseX >= x + LIST_WIDTH - PADDING) return -1;
        int row = Mth.floor((mouseY - this.listTop()) / ROW_HEIGHT);
        if (row < 0 || row >= LIST_ROWS) return -1;
        int index = this.listScroll + row;
        return index < this.shown.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int index = this.rowAt(event.x(), event.y());
        if (index >= 0) {
            this.selected = this.shown.get(index).pos();
            this.updateButtons();
            if (doubleClick) this.showSelected();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(x, y, scrollX, scrollY);
        int step = -(int) Math.signum(scrollY);
        if (x < this.detailLeft()) {
            this.listScroll = Mth.clamp(this.listScroll + step, 0, Math.max(0, this.shown.size() - LIST_ROWS));
        } else {
            ContainerRecord record = this.selected();
            int rows = record != null ? (record.items().size() + COLUMNS - 1) / COLUMNS : 0;
            this.gridScroll = Mth.clamp(this.gridScroll + step, 0, Math.max(0, rows - GRID_ROWS));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
