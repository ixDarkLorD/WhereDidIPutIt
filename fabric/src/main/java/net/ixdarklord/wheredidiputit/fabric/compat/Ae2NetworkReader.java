package net.ixdarklord.wheredidiputit.fabric.compat;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.parts.reporting.AbstractTerminalPart;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.ixdarklord.wheredidiputit.live.NetworkReaders;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Live search into Applied Energistics 2 networks, on the server: a cable bus with a terminal part (an ME terminal or
 * crafting terminal) or a controller is an access point, the grid is the network, and its storage service's cached
 * inventory lists what it holds.
 */
public final class Ae2NetworkReader implements NetworkReaders.Reader {
    public static final String MOD_ID = "ae2";

    @Override
    public @Nullable Object network(BlockEntity blockEntity) {
        if (blockEntity instanceof IPartHost host) {
            for (Direction side : Direction.values()) {
                if (host.getPart(side) instanceof AbstractTerminalPart terminal) {
                    IGridNode node = terminal.getGridNode();
                    if (node != null) return node.getGrid();
                }
            }
            return null;
        }
        if (blockEntity instanceof ControllerBlockEntity controller && controller.getGridNode() != null) return controller.getGridNode().getGrid();
        return null;
    }

    @Override
    public List<StoredItem> contents(Object network, BlockEntity accessPoint) {
        List<StoredItem> items = new ArrayList<>();
        for (Object2LongMap.Entry<AEKey> entry : ((IGrid) network).getStorageService().getCachedInventory()) {
            if (entry.getKey() instanceof AEItemKey item && entry.getLongValue() > 0) items.add(new StoredItem(item.toStack(), entry.getLongValue()));
        }
        return items;
    }
}
