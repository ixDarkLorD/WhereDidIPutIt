package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.TimeUnit;

/** The action bar lines a search shows: "Barrel, 14 blocks north-east, 32 seen 5 min ago". */
public final class FindMessages {
    private static final String[] COMPASS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private FindMessages() {
    }

    /** What the player is shown for a hit, {@code index} of {@code total} (both counted from 0 and 1 as shown). */
    public static Component found(SearchHit hit, int index, int total, Vec3 from) {
        ContainerRecord record = hit.record();
        Component block = record.label() != null ? record.label() : BuiltInRegistries.BLOCK.getValue(record.block()).getName();
        Component line = Component.translatable("gui.wheredidiputit.found", block, where(Vec3.atCenterOf(record.pos()).subtract(from)),
                hit.count(), ago(System.currentTimeMillis() - record.seenAt()));
        if (total > 1) line = Component.translatable("gui.wheredidiputit.found.cycle", line, index + 1, total);
        return line;
    }

    public static Component notFound(ItemStack query, int maxDistance) {
        return Component.translatable("gui.wheredidiputit.not_found", query.getHoverName(), maxDistance).withStyle(ChatFormatting.RED);
    }

    /** "14 blocks north-east", or straight above or below it, or right here. */
    private static Component where(Vec3 offset) {
        double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
        int distance = (int) Math.round(offset.length());
        if (horizontal < 2.0) {
            if (Math.abs(offset.y) < 2.0) return Component.translatable("gui.wheredidiputit.where.here");
            return Component.translatable("gui.wheredidiputit.where." + (offset.y > 0 ? "above" : "below"), distance);
        }
        // North is -Z and east is +X: 0 degrees is north, turning clockwise.
        double bearing = Math.toDegrees(Math.atan2(offset.x, -offset.z));
        String direction = COMPASS[Mth.positiveModulo((int) Math.round(bearing / 45.0), COMPASS.length)];
        return Component.translatable("gui.wheredidiputit.where.direction", distance, Component.translatable("gui.wheredidiputit.direction." + direction));
    }

    private static Component ago(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(Math.max(0L, millis));
        if (minutes < 1) return Component.translatable("gui.wheredidiputit.ago.just_now");
        if (minutes < 60) return Component.translatable("gui.wheredidiputit.ago.minutes", minutes);
        long hours = minutes / 60;
        if (hours < 48) return Component.translatable("gui.wheredidiputit.ago.hours", hours);
        return Component.translatable("gui.wheredidiputit.ago.days", hours / 24);
    }
}
