package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Finds room for an item in the remembered containers: in stacks of it that aren't full and in empty slots, as they
 * were when last seen. Only containers whose slot count is known ({@link ContainerRecord#slots}: chests, shulker boxes,
 * hoppers, dispensers), and never those left out of searches.
 */
public final class SpaceFinder {
    private SpaceFinder() {
    }

    /**
     * A container with room.
     *
     * @param room     how many more of the item fit
     * @param holds    whether it already holds some, which keeps like with like
     * @param distance from the player, in blocks
     */
    public record Room(ContainerRecord record, long room, boolean holds, double distance) {
    }

    /** The containers within {@code maxDistance} with room for {@code item}: those already holding it first, then nearest first. */
    public static List<Room> search(DimensionMemory memory, ItemStack item, Vec3 from, double maxDistance) {
        List<Room> rooms = new ArrayList<>();
        int stackSize = item.getMaxStackSize();
        for (ContainerRecord record : memory.records()) {
            if (record.ignored() || record.slots() <= 0) continue;
            double distance = Vec3.atCenterOf(record.pos()).distanceTo(from);
            if (distance > maxDistance) continue;
            long room = 0;
            boolean holds = false;
            for (StoredItem stored : record.items()) {
                if (!ItemStack.isSameItemSameTags(stored.stack(), item)) continue;
                holds = true;
                room += Math.max(0, stackSize - stored.count());
            }
            // Each remembered entry of plain storage is one slot.
            room += (long) Math.max(0, record.slots() - record.items().size()) * stackSize;
            if (room > 0) rooms.add(new Room(record, room, holds, distance));
        }
        rooms.sort(Comparator.comparing((Room room) -> !room.holds()).thenComparingDouble(Room::distance));
        return rooms;
    }
}
