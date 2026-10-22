package net.ixdarklord.wheredidiputit.forge.compat;

import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import com.refinedmods.refinedstorage.screen.grid.stack.ItemGridStack;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Refined Storage's grids (Grid, Crafting Grid, Pattern Grid, Wireless and Portable Grids). On 1.20.1 this is Refined
 * Storage 1.x, Forge only, with its own API (26.1.2 has Refined Storage 2, read from common). Reads the grid view's
 * every stack, not the ones a search in the grid narrowed it to; entries that can only be crafted are left out, as are
 * fluids.
 */
public final class RefinedStorageTerminal implements StorageTerminals.Terminal {
    public static final String MOD_ID = "refinedstorage";

    @Override
    public boolean handles(AbstractContainerScreen<?> screen) {
        return screen instanceof GridScreen;
    }

    @Override
    public List<StoredItem> contents(AbstractContainerScreen<?> screen) {
        List<StoredItem> items = new ArrayList<>();
        for (IGridStack stack : ((GridScreen) screen).getView().getAllStacks()) {
            if (stack instanceof ItemGridStack item && !item.isCraftable() && item.getQuantity() > 0) {
                items.add(new StoredItem(item.getStack(), item.getQuantity()));
            }
        }
        return items;
    }

    @Override
    public ItemStack hovered(AbstractContainerScreen<?> screen) {
        GridScreen grid = (GridScreen) screen;
        // The grid's own slots aren't real: it keeps the index of the shown stack under the mouse.
        List<IGridStack> shown = grid.getView().getStacks();
        int slot = grid.getSlotNumber();
        if (slot < 0 || slot >= shown.size()) return ItemStack.EMPTY;
        return shown.get(slot) instanceof ItemGridStack item ? item.getStack() : ItemStack.EMPTY;
    }
}
