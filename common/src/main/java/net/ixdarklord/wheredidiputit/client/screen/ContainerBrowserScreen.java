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
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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
import java.util.UUID;

/**
 * Every container remembered in the player's dimension, nearest first (or most recently seen, or by name), narrowed by
 * name or by what they hold. The one picked shows what it held when last seen, and can be pointed out, skipped in
 * searches (or taken back in) or forgotten. Drawn in the screens' shared look ({@link ScreenStyle}).
 */
public final class ContainerBrowserScreen extends Screen {
    private static final int PANEL_WIDTH = 388;
    private static final int PANEL_HEIGHT = 250;
    private static final int PADDING = 8;
    private static final int LIST_WIDTH = 196;
    private static final int ROW_HEIGHT = 21;
    private static final int LIST_ROWS = 8;
    private static final int COLUMNS = 9;
    private static final int GRID_ROWS = 7;
    private static final int SLOT = 18;
    /** How many of a container's items its row shows, small, on its right. */
    private static final int PREVIEW_ITEMS = 3;
    /** Two clicks on a row this close together are a double click (1.21.1's screens don't tell). */
    private static final long DOUBLE_CLICK_MILLIS = 250L;

    /** How the list is ordered; the choice is kept while the game runs. */
    private enum Sort {
        NEAREST, RECENT, NAME;

        Sort next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        Component label() {
            return Component.translatable("gui.wheredidiputit.browser.sort." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    private static String lastQuery = "";
    private static Sort sort = Sort.NEAREST;

    private @Nullable DimensionMemory memory;
    private List<ContainerRecord> shown = List.of();
    private @Nullable BlockPos selected;
    private @Nullable UUID selectedEntity;
    private int listScroll;
    private int lastClickedRow = -1;
    private long lastClickAt;
    private int gridScroll;
    private int left;
    private int top;
    private EditBox search;
    private FlatButton sortButton;
    private FlatButton showButton;
    private FlatButton ignoreButton;
    private FlatButton forgetButton;

    public ContainerBrowserScreen() {
        super(Component.translatable("gui.wheredidiputit.browser.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.memory = this.minecraft.level != null ? ContainerMemory.dimension(this.minecraft.level.dimension()) : null;

        this.search = new EditBox(this.font, this.listLeft() + 5, this.top + 35, LIST_WIDTH - 66, 10, this.title);
        this.search.setBordered(false);
        this.search.setTextColor(ScreenStyle.TEXT);
        this.search.setHint(Component.translatable("gui.wheredidiputit.browser.hint").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(64);
        this.search.setValue(lastQuery);
        this.search.setResponder(query -> {
            lastQuery = query;
            this.refresh();
        });
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);
        this.sortButton = this.addRenderableWidget(new FlatButton(this.listLeft() + LIST_WIDTH - 54, this.top + 31, 54, 16, sort.label(), FlatButton.Kind.PLAIN, () -> {
            sort = sort.next();
            this.sortButton.setMessage(sort.label());
            this.refresh();
        }));

        int buttonY = this.top + PANEL_HEIGHT - PADDING - 16;
        int buttonWidth = (this.detailWidth() - 4) / 3;
        this.showButton = this.addRenderableWidget(new FlatButton(this.detailLeft(), buttonY, buttonWidth, 16,
                Component.translatable("gui.wheredidiputit.browser.show"), FlatButton.Kind.PRIMARY, this::showSelected));
        this.ignoreButton = this.addRenderableWidget(new FlatButton(this.detailLeft() + buttonWidth + 2, buttonY, buttonWidth, 16,
                Component.empty(), FlatButton.Kind.PLAIN, this::toggleIgnored));
        this.forgetButton = this.addRenderableWidget(new FlatButton(this.detailLeft() + (buttonWidth + 2) * 2, buttonY, buttonWidth, 16,
                Component.translatable("gui.wheredidiputit.browser.forget"), FlatButton.Kind.DANGER, this::forgetSelected));
        this.refresh();
    }

    // The list again, after the query, the order or the memory changed.
    private void refresh() {
        List<ContainerRecord> matching = new ArrayList<>();
        if (this.memory != null) {
            for (ContainerRecord record : this.memory.records()) {
                if (this.fits(record)) matching.add(record);
            }
        }
        Vec3 from = this.from();
        matching.sort(switch (sort) {
            case NEAREST -> Comparator.comparingDouble(record -> Vec3.atCenterOf(record.pos()).distanceToSqr(from));
            case RECENT -> Comparator.comparingLong(ContainerRecord::seenAt).reversed();
            case NAME -> Comparator.comparing(record -> FindMessages.name(record).getString().toLowerCase(Locale.ROOT));
        });
        this.shown = matching;
        this.listScroll = Mth.clamp(this.listScroll, 0, Math.max(0, this.shown.size() - LIST_ROWS));
        if (this.selected() == null) {
            this.selected = null;
            this.selectedEntity = null;
        }
        this.updateButtons();
    }

    // Whether a container fits the query: by its name, or by anything it holds.
    private boolean fits(ContainerRecord record) {
        String query = lastQuery.trim();
        if (query.isEmpty()) return true;
        if (!query.startsWith("#") && !query.startsWith("@")
                && FindMessages.name(record).getString().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
            return true;
        }
        for (StoredItem item : record.items()) {
            if (StoredStacks.matches(item.stack(), query)) return true;
        }
        return false;
    }

    private @Nullable ContainerRecord selected() {
        if (this.memory == null) return null;
        if (this.selectedEntity != null) return this.memory.getEntity(this.selectedEntity);
        return this.selected != null ? this.memory.get(this.selected) : null;
    }

    private boolean isSelected(ContainerRecord record) {
        return record.entity() != null ? record.entity().equals(this.selectedEntity) : this.selectedEntity == null && record.pos().equals(this.selected);
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
        ContainerHighlight.show(this.minecraft.level, List.of(new ContainerHighlight.Found(record.pos(), icon(record), total, record.seenAt(), record.entity())));
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
        this.memory.remove(record);
        this.selected = null;
        this.selectedEntity = null;
        this.refresh();
    }

    private static ItemStack icon(ContainerRecord record) {
        if (record.entity() != null) {
            // Chest boats and minecarts share their entity's id with their item; animals show their spawn egg.
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(record.block()));
            if (stack.isEmpty()) stack = new ItemStack(BuiltInRegistries.ITEM.get(record.block().withSuffix("_spawn_egg")));
            return stack.isEmpty() ? new ItemStack(Items.CHEST) : stack;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.BLOCK.get(record.block()).asItem());
        return stack.isEmpty() ? new ItemStack(Items.CHEST) : stack;
    }

    private Vec3 from() {
        return this.minecraft.player != null ? this.minecraft.player.position() : Vec3.ZERO;
    }

    private int listLeft() {
        return this.left + PADDING;
    }

    private int listTop() {
        return this.top + 54;
    }

    private int detailLeft() {
        return this.left + PADDING + LIST_WIDTH + 12;
    }

    private int detailWidth() {
        return PANEL_WIDTH - (this.detailLeft() - this.left) - PADDING;
    }

    // The panel goes with the background, under the widgets the screen draws over it.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        ScreenStyle.panel(graphics, this.left, this.top, PANEL_WIDTH, PANEL_HEIGHT);
        int total = this.memory != null ? this.memory.records().size() : 0;
        ScreenStyle.header(graphics, this.font, this.left, this.top, PANEL_WIDTH, this.title, Component.translatable("gui.wheredidiputit.browser.count", total));
        ScreenStyle.field(graphics, this.listLeft(), this.top + 31, LIST_WIDTH - 58, 16, this.search.isFocused());
        // The divider between the list and the picked container.
        graphics.fill(this.detailLeft() - 7, this.top + ScreenStyle.HEADER_HEIGHT + 8, this.detailLeft() - 6, this.top + PANEL_HEIGHT - PADDING, 0xFF2A313B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderList(graphics, mouseX, mouseY);
        this.renderDetail(graphics, mouseX, mouseY);
        // Centred under the list it is about.
        ScreenStyle.hints(graphics, this.font, this.listLeft() + (LIST_WIDTH - 4) / 2, this.top + PANEL_HEIGHT - PADDING - 12,
                Component.translatable("gui.wheredidiputit.browser.hint_key"), Component.translatable("gui.wheredidiputit.browser.hint_action"));
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.listLeft();
        int width = LIST_WIDTH - 4;
        int hovered = this.rowAt(mouseX, mouseY);
        Vec3 from = this.from();
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = this.listScroll + row;
            if (index >= this.shown.size()) break;
            ContainerRecord record = this.shown.get(index);
            int y = this.listTop() + row * ROW_HEIGHT;
            boolean picked = this.isSelected(record);
            ScreenStyle.rounded(graphics, x, y, width, ROW_HEIGHT - 2, picked ? ScreenStyle.ROW_SELECTED : index == hovered ? ScreenStyle.ROW_HOVER : ScreenStyle.ROW);
            if (picked) graphics.fill(x, y + 2, x + 2, y + ROW_HEIGHT - 4, ScreenStyle.accent());
            graphics.renderItem(icon(record), x + 4, y + 1);

            int textLeft = x + 24;
            int previewWidth = record.ignored() ? 0 : Math.min(PREVIEW_ITEMS, record.items().size()) * 9;
            Component name = FindMessages.name(record);
            int nameRoom = width - (textLeft - x) - (record.ignored() ? 50 : previewWidth + 4);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(name.getString(), nameRoom), textLeft, y + 2, record.ignored() ? ScreenStyle.MUTED : ScreenStyle.TEXT, false);
            int distance = Mth.floor(Vec3.atCenterOf(record.pos()).distanceTo(from));
            graphics.drawString(this.font, Component.translatable("gui.wheredidiputit.browser.row", distance, age(record)), textLeft, y + 11, ScreenStyle.FAINT, false);

            if (record.ignored()) {
                Component skipped = Component.translatable("gui.wheredidiputit.browser.skipped");
                ScreenStyle.badge(graphics, this.font, skipped, x + width - this.font.width(skipped) - 12, y + 4, 0xFF3A2F1E, 0xFFE3B341);
            } else {
                // A glimpse of what it holds: its first few items, half size.
                for (int i = 0; i < Math.min(PREVIEW_ITEMS, record.items().size()); i++) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x + width - previewWidth - 2 + i * 9, y + 5, 0.0F);
                    graphics.pose().scale(0.5F, 0.5F, 1.0F);
                    graphics.renderItem(record.items().get(i).stack(), 0, 0);
                    graphics.pose().popPose();
                }
            }
        }
        if (this.shown.isEmpty()) {
            Component empty = Component.translatable(this.memory == null || this.memory.records().isEmpty() ? "gui.wheredidiputit.search.nothing" : "gui.wheredidiputit.browser.empty");
            graphics.drawCenteredString(this.font, empty, x + width / 2, this.listTop() + LIST_ROWS * ROW_HEIGHT / 2 - 4, ScreenStyle.MUTED);
        }
        ScreenStyle.scrollbar(graphics, x + LIST_WIDTH - 2, this.listTop(), LIST_ROWS * ROW_HEIGHT - 2, this.listScroll, LIST_ROWS, this.shown.size());
    }

    private void renderDetail(GuiGraphics graphics, int mouseX, int mouseY) {
        ContainerRecord record = this.selected();
        int x = this.detailLeft();
        int y = this.top + 31;
        if (record == null) {
            int middle = x + this.detailWidth() / 2;
            int center = this.top + PANEL_HEIGHT / 2 - 8;
            graphics.drawCenteredString(this.font, Component.translatable("gui.wheredidiputit.browser.pick"), middle, center, ScreenStyle.MUTED);
            graphics.drawCenteredString(this.font, Component.translatable("gui.wheredidiputit.browser.pick_more"), middle, center + 11, ScreenStyle.FAINT);
            return;
        }
        ScreenStyle.slot(graphics, x, y);
        graphics.renderItem(icon(record), x + 1, y + 1);
        int textLeft = x + 24;
        graphics.drawString(this.font, this.font.plainSubstrByWidth(FindMessages.name(record).getString(), this.detailWidth() - 24), textLeft, y + 1, ScreenStyle.TEXT, false);
        BlockPos pos = record.pos();
        graphics.drawString(this.font, Component.translatable("gui.wheredidiputit.browser.position", pos.getX(), pos.getY(), pos.getZ()), textLeft, y + 10, ScreenStyle.MUTED, false);

        int badgeX = x;
        int badgeY = y + 23;
        int distance = Mth.floor(Vec3.atCenterOf(pos).distanceTo(this.from()));
        badgeX += ScreenStyle.badge(graphics, this.font, Component.translatable("gui.wheredidiputit.browser.blocks", distance), badgeX, badgeY, 0xFF262D37, ScreenStyle.TEXT) + 3;
        badgeX += ScreenStyle.badge(graphics, this.font, age(record), badgeX, badgeY, 0xFF262D37, ScreenStyle.TEXT) + 3;
        Component kind = record.entity() != null ? Component.translatable("gui.wheredidiputit.browser.entity")
                : record.network() ? Component.translatable("gui.wheredidiputit.browser.network")
                : record.slots() > 0 ? Component.translatable("gui.wheredidiputit.browser.slots", record.slots()) : null;
        if (kind != null) ScreenStyle.badge(graphics, this.font, kind, badgeX, badgeY, ScreenStyle.tint(0xFF262D37, 0.25F), ScreenStyle.TEXT);

        int gridTop = y + 40;
        graphics.drawString(this.font, Component.translatable("gui.wheredidiputit.browser.contents"), x, gridTop, ScreenStyle.TEXT, false);
        Component stacks = Component.translatable("gui.wheredidiputit.browser.stacks", record.items().size());
        graphics.drawString(this.font, stacks, x + this.detailWidth() - this.font.width(stacks), gridTop, ScreenStyle.FAINT, false);
        gridTop += 11;
        StoredItem hoveredItem = null;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int slotX = x + column * SLOT;
                int slotY = gridTop + row * SLOT;
                ScreenStyle.slot(graphics, slotX, slotY);
                int index = (this.gridScroll + row) * COLUMNS + column;
                if (index >= record.items().size()) continue;
                StoredItem item = record.items().get(index);
                graphics.renderItem(item.stack(), slotX + 1, slotY + 1);
                graphics.renderItemDecorations(this.font, item.stack(), slotX + 1, slotY + 1, ItemMarker.count(item.count()));
                if (mouseX >= slotX && mouseX < slotX + SLOT && mouseY >= slotY && mouseY < slotY + SLOT) {
                    ScreenStyle.slotHover(graphics, slotX, slotY);
                    hoveredItem = item;
                }
            }
        }
        int rows = (record.items().size() + COLUMNS - 1) / COLUMNS;
        ScreenStyle.scrollbar(graphics, x + COLUMNS * SLOT + 1, gridTop, GRID_ROWS * SLOT, this.gridScroll, GRID_ROWS, rows);
        if (record.items().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable("gui.wheredidiputit.browser.empty_container"), x + COLUMNS * SLOT / 2, gridTop + 2 * SLOT + 5, ScreenStyle.MUTED);
        }
        if (hoveredItem != null) graphics.renderTooltip(this.font, hoveredItem.stack(), mouseX, mouseY);
    }

    private static Component age(ContainerRecord record) {
        Component age = ItemMarker.age(System.currentTimeMillis() - record.seenAt());
        return age != null ? age : Component.translatable("gui.wheredidiputit.browser.just_now");
    }

    // The index in the shown list of the row under the mouse, or -1.
    private int rowAt(double mouseX, double mouseY) {
        int x = this.listLeft();
        if (mouseX < x || mouseX >= x + LIST_WIDTH - 4 || mouseY < this.listTop()) return -1;
        int row = Mth.floor((mouseY - this.listTop()) / ROW_HEIGHT);
        if (row >= LIST_ROWS) return -1;
        int index = this.listScroll + row;
        return index < this.shown.size() ? index : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = this.rowAt(mouseX, mouseY);
        if (index >= 0) {
            long now = Util.getMillis();
            boolean doubleClick = index == this.lastClickedRow && now - this.lastClickAt <= DOUBLE_CLICK_MILLIS;
            this.lastClickedRow = index;
            this.lastClickAt = now;
            ContainerRecord picked = this.shown.get(index);
            this.selected = picked.pos();
            this.selectedEntity = picked.entity();
            this.updateButtons();
            if (doubleClick) this.showSelected();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0.0) return super.mouseScrolled(x, y, scrollX, scrollY);
        int step = -(int) Math.signum(scrollY);
        if (x < this.detailLeft() - 6) {
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
