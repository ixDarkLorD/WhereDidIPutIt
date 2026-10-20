package net.ixdarklord.wheredidiputit.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.grid.view.ItemGridResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Refined Storage 2's grids (Grid, Crafting Grid, Wireless Grid). Its common module is loader-free, so this lives in
 * common. Reads the grid's whole repository, not its view, which a search in the grid may have narrowed.
 */
public final class RefinedStorageTerminal implements StorageTerminals.Terminal {
    public static final String MOD_ID = "refinedstorage";

    @Override
    public boolean handles(AbstractContainerScreen<?> screen) {
        return screen instanceof AbstractGridScreen<?>;
    }

    @Override
    public List<StoredItem> contents(AbstractContainerScreen<?> screen) {
        AbstractGridContainerMenu menu = (AbstractGridContainerMenu) screen.getMenu();
        List<StoredItem> items = new ArrayList<>();
        for (ResourceAmount amount : menu.getRepository().copyBackingList().copyState()) {
            if (amount.resource() instanceof ItemResource item) items.add(new StoredItem(item.toItemStack(), amount.amount()));
        }
        return items;
    }

    @Override
    public ItemStack hovered(AbstractContainerScreen<?> screen) {
        return ((AbstractGridScreen<?>) screen).getCurrentGridResource() instanceof ItemGridResource resource ? resource.getItemStack() : ItemStack.EMPTY;
    }
}
