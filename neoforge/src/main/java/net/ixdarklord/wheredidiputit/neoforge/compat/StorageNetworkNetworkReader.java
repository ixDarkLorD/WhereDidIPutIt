package net.ixdarklord.wheredidiputit.neoforge.compat;

import com.lothrazar.storagenetwork.api.DimPos;
import com.lothrazar.storagenetwork.block.TileConnectable;
import com.lothrazar.storagenetwork.block.inventory.TileInventory;
import com.lothrazar.storagenetwork.block.main.TileMain;
import com.lothrazar.storagenetwork.block.request.TileRequest;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search into Simple Storage Network networks, on the server: a request table, an inventory terminal or the master
 * is an access point, the master is the network (found through the terminal's link to it, only when its chunk is
 * loaded), and the master's network lists what it holds, merged, as its request table does.
 */
public final class StorageNetworkNetworkReader implements NetworkReaders.Reader {
    public static final String MOD_ID = "storagenetwork";

    @Override
    public @Nullable Object network(BlockEntity blockEntity) {
        if (blockEntity instanceof TileMain main) return main;
        if (!(blockEntity instanceof TileRequest) && !(blockEntity instanceof TileInventory)) return null;
        Level level = blockEntity.getLevel();
        DimPos master = ((TileConnectable) blockEntity).getMain();
        if (level == null || master == null || !master.getDimension().equals(DimPos.dimensionToString(level)) || !level.isLoaded(master.getBlockPos())) return null;
        return level.getBlockEntity(master.getBlockPos()) instanceof TileMain main ? main : null;
    }

    @Override
    public List<StoredItem> contents(Object network, BlockEntity accessPoint) {
        List<StoredItem> items = new ArrayList<>();
        for (ItemStack stack : ((TileMain) network).getNetwork().getStacks()) {
            if (!stack.isEmpty()) items.add(new StoredItem(stack, stack.getCount()));
        }
        return items;
    }
}
