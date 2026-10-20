package net.ixdarklord.wheredidiputit.neoforge.compat;

import com.lothrazar.storagenetwork.api.gui.GuiNetwork;
import net.ixdarklord.wheredidiputit.memory.StorageTerminals;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Storage Network's request tables and remotes: the network's stacks as the server last sent them. Its grid
 * is drawn by the mod, not made of slots, so the hovered item comes from it too.
 */
public final class StorageNetworkTerminal implements StorageTerminals.Terminal {
    public static final String MOD_ID = "storagenetwork";

    @Override
    public boolean handles(AbstractContainerScreen<?> screen) {
        return screen instanceof GuiNetwork;
    }

    @Override
    public List<StoredItem> contents(AbstractContainerScreen<?> screen) {
        List<StoredItem> items = new ArrayList<>();
        for (ItemStack stack : ((GuiNetwork) screen).getNetwork().getStacks()) {
            if (!stack.isEmpty()) items.add(StoredItem.of(stack));
        }
        return items;
    }

    @Override
    public ItemStack hovered(AbstractContainerScreen<?> screen) {
        return ((GuiNetwork) screen).getNetwork().getStackUnderMouse();
    }
}
