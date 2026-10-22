package net.ixdarklord.wheredidiputit.forge.compat;

import com.tom.storagemod.gui.AbstractStorageTerminalScreen;
import com.tom.storagemod.gui.StorageTerminalMenu;
import com.tom.storagemod.util.StoredItemStack;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tom's Simple Storage terminals (Storage Terminal, Crafting Terminal): what the terminal's menu was last sent. Tom's
 * ships a jar per loader built from the same source, so this class is the same in the Fabric and Forge modules;
 * keep the two alike. Its grid isn't made of real slots: {@code getSlotUnderMouse()} stands in a slot for the entry
 * under the mouse.
 */
public final class TomsStorageTerminal implements StorageTerminals.Terminal {
    public static final String MOD_ID = "toms_storage";

    @Override
    public boolean handles(AbstractContainerScreen<?> screen) {
        return screen instanceof AbstractStorageTerminalScreen<?>;
    }

    @Override
    public List<StoredItem> contents(AbstractContainerScreen<?> screen) {
        StorageTerminalMenu menu = (StorageTerminalMenu) screen.getMenu();
        List<StoredItem> items = new ArrayList<>();
        for (StoredItemStack stack : menu.itemListClient) {
            if (stack.getQuantity() > 0) items.add(new StoredItem(stack.getStack(), stack.getQuantity()));
        }
        return items;
    }

    @Override
    public ItemStack hovered(AbstractContainerScreen<?> screen) {
        Slot slot = ((AbstractStorageTerminalScreen<?>) screen).getSlotUnderMouse();
        return slot != null ? slot.getItem() : ItemStack.EMPTY;
    }
}
