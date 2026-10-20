package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.minecraft.world.item.ItemStack;

/**
 * A remembered container that holds the item searched for.
 *
 * @param record   the container
 * @param count    how many of the item it held when last seen
 * @param distance from the player when searched, in blocks
 * @param shown    the matching item it holds most of, a stack of one: with a looser match mode not always the item
 *                 searched for (a birch log for an oak log)
 */
public record SearchHit(ContainerRecord record, long count, double distance, ItemStack shown) {
}
