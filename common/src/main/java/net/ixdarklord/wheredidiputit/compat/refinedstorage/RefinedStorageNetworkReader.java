package net.ixdarklord.wheredidiputit.compat.refinedstorage;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search into Refined Storage 2 networks, on the server: a grid (terminal) or the controller is an access point,
 * the {@link Network} is the network, and its root storage lists what it holds.
 */
public final class RefinedStorageNetworkReader implements NetworkReaders.Reader {
    public static final String MOD_ID = "refinedstorage";

    @Override
    public @Nullable Object network(BlockEntity blockEntity) {
        if (!(blockEntity instanceof AbstractGridBlockEntity) && !(blockEntity instanceof ControllerBlockEntity)) return null;
        return ((AbstractBaseNetworkNodeContainerBlockEntity<?>) blockEntity).getNetworkForItem();
    }

    @Override
    public List<StoredItem> contents(Object network, BlockEntity accessPoint) {
        List<StoredItem> items = new ArrayList<>();
        for (ResourceAmount amount : ((Network) network).getComponent(StorageNetworkComponent.class).getAll()) {
            if (amount.resource() instanceof ItemResource item && amount.amount() > 0) items.add(new StoredItem(item.toItemStack(), amount.amount()));
        }
        return items;
    }
}
