package net.ixdarklord.wheredidiputit.forge.compat;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.me.common.MEStorageScreen;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Applied Energistics 2's terminals (ME Terminal, Crafting Terminal, Pattern Encoding...): everything stored in the
 * network, from the screen's client repository. Entries that can only be crafted are left out. Its grid's entries are
 * slots, so the hovered item needs nothing here.
 */
public final class Ae2Terminal implements StorageTerminals.Terminal {
    public static final String MOD_ID = "ae2";

    @Override
    public boolean handles(AbstractContainerScreen<?> screen) {
        return screen instanceof MEStorageScreen<?>;
    }

    @Override
    public List<StoredItem> contents(AbstractContainerScreen<?> screen) {
        IClientRepo repo = ((MEStorageScreen<?>) screen).getMenu().getClientRepo();
        if (repo == null) return List.of();
        List<StoredItem> items = new ArrayList<>();
        for (GridInventoryEntry entry : repo.getAllEntries()) {
            if (entry.getWhat() instanceof AEItemKey key && entry.getStoredAmount() > 0) {
                items.add(new StoredItem(key.toStack(), entry.getStoredAmount()));
            }
        }
        return items;
    }
}
