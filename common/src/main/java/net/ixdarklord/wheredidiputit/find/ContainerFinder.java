package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.ixdarklord.wheredidiputit.memory.DimensionMemory;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Searches the remembered containers of a dimension. */
public final class ContainerFinder {
    private ContainerFinder() {
    }

    /** The containers holding a matching item within {@code maxDistance} of {@code from}, nearest first. */
    public static List<SearchHit> search(DimensionMemory memory, ItemMatcher matcher, Vec3 from, double maxDistance) {
        List<SearchHit> hits = new ArrayList<>();
        for (ContainerRecord record : memory.records()) {
            double distance = Vec3.atCenterOf(record.pos()).distanceTo(from);
            if (distance > maxDistance) continue;
            long count = matcher.count(record.items());
            if (count > 0) hits.add(new SearchHit(record, count, distance));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::distance));
        return hits;
    }
}
