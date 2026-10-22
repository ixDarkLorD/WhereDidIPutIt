package net.ixdarklord.wheredidiputit.forge.compat;

import com.tom.storagemod.tile.StorageTerminalBlockEntity;
import com.tom.storagemod.util.StoredItemStack;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search into Tom's Simple Storage networks, on the server: a storage terminal is an access point (its own
 * network: a network has few terminals) and lists what its network holds. That list is the terminal's own cache, which
 * asking for refreshes on its next tick: a terminal nobody has used may answer empty once, and its remembered contents
 * still answer then. The Fabric module has the same class; keep them alike.
 */
public final class TomsStorageNetworkReader implements NetworkReaders.Reader {
    public static final String MOD_ID = "toms_storage";

    @Override
    public @Nullable Object network(BlockEntity blockEntity) {
        return blockEntity instanceof StorageTerminalBlockEntity ? blockEntity : null;
    }

    @Override
    public List<StoredItem> contents(Object network, BlockEntity accessPoint) {
        List<StoredItem> items = new ArrayList<>();
        for (StoredItemStack stack : ((StorageTerminalBlockEntity) network).getStacks().keySet()) {
            if (stack.getQuantity() > 0) items.add(new StoredItem(stack.getStack(), stack.getQuantity()));
        }
        return items;
    }
}
