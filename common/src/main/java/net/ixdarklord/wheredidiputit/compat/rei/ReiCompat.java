package net.ixdarklord.wheredidiputit.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.overlay.OverlayListWidget;
import me.shedaniel.rei.api.client.overlay.ScreenOverlay;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.ixdarklord.coolcatcore.api.client.utils.MouseHelper;
import net.ixdarklord.wheredidiputit.find.HoveredStacks;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * REI: the find key works on the item REI has under the mouse: its entry list and favorites (its overlay), then
 * the slot under the mouse in its recipe screens, then whatever its screen registry knows of. Only loaded when REI
 * is installed.
 */
public final class ReiCompat {
    public static final String MOD_ID = "roughlyenoughitems";
    // How deep into nested widgets a recipe screen's slots are looked for.
    private static final int MAX_WIDGET_DEPTH = 8;

    private ReiCompat() {
    }

    public static void init() {
        HoveredStacks.register(ReiCompat::hovered);
    }

    private static ItemStack hovered(Screen screen) {
        Optional<ScreenOverlay> overlay = REIRuntime.getInstance().getOverlay();
        if (overlay.isPresent()) {
            ItemStack stack = item(overlay.get().getEntryList().getFocusedStack());
            if (stack.isEmpty()) stack = overlay.get().getFavoritesList().map(OverlayListWidget::getFocusedStack).map(ReiCompat::item).orElse(ItemStack.EMPTY);
            if (!stack.isEmpty()) return stack;
        }
        Point mouse = new Point(MouseHelper.getMouseX(), MouseHelper.getMouseY());
        // REI's recipe screens register no focused stack; their entries are slot widgets, found under the mouse.
        if (screen instanceof DisplayScreen) return slotUnder(screen, mouse, 0);
        return item(ScreenRegistry.getInstance().getFocusedStack(screen, mouse));
    }

    private static ItemStack slotUnder(ContainerEventHandler parent, Point mouse, int depth) {
        if (depth > MAX_WIDGET_DEPTH) return ItemStack.EMPTY;
        for (GuiEventListener child : parent.children()) {
            if (child instanceof Slot slot && slot.containsMouse(mouse)) {
                ItemStack stack = item(slot.getCurrentEntry());
                if (!stack.isEmpty()) return stack;
            }
            if (child instanceof ContainerEventHandler container) {
                ItemStack stack = slotUnder(container, mouse, depth + 1);
                if (!stack.isEmpty()) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack item(@Nullable EntryStack<?> entry) {
        if (entry == null || entry.isEmpty() || entry.getType() != VanillaEntryTypes.ITEM) return ItemStack.EMPTY;
        return entry.<ItemStack>castValue();
    }
}
