package net.ixdarklord.wheredidiputit.forge.compat;

import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.blockentity.ControllerBlockEntity;
import com.refinedmods.refinedstorage.blockentity.grid.GridBlockEntity;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search into Refined Storage 1.x networks, on the server: a grid or the controller is an access point, its
 * {@link INetwork} is the network, and the network's item storage cache lists what it holds.
 */
public final class RefinedStorageNetworkReader implements NetworkReaders.Reader {
    public static final String MOD_ID = "refinedstorage";

    @Override
    public @Nullable Object network(BlockEntity blockEntity) {
        if (blockEntity instanceof GridBlockEntity grid) return grid.getNode().getNetwork();
        if (blockEntity instanceof ControllerBlockEntity controller) return controller.getNetwork();
        return null;
    }

    @Override
    public List<StoredItem> contents(Object network, BlockEntity accessPoint) {
        List<StoredItem> items = new ArrayList<>();
        for (StackListEntry<ItemStack> entry : ((INetwork) network).getItemStorageCache().getList().getStacks()) {
            ItemStack stack = entry.getStack();
            if (!stack.isEmpty()) items.add(new StoredItem(stack, stack.getCount()));
        }
        return items;
    }
}
