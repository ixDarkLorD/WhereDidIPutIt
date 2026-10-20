package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;

/**
 * A remembered container that holds the item searched for.
 *
 * @param record   the container
 * @param count    how many of the item it held when last seen
 * @param distance from the player when searched, in blocks
 */
public record SearchHit(ContainerRecord record, int count, double distance) {
}
