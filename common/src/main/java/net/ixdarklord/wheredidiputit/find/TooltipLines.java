package net.ixdarklord.wheredidiputit.find;

import net.ixdarklord.wheredidiputit.client.hud.ItemMarker;
import net.ixdarklord.wheredidiputit.memory.ContainerRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

/**
 * How the mod's tooltip lines read, in one place so they stay alike: a gray label, then a white value ("Stored: 412 in
 * 2 containers"), numbers grouped by thousands, and "container" or "containers" as the count asks.
 */
public final class TooltipLines {
    private TooltipLines() {
    }

    /** A labelled line: {@code tooltip.wheredidiputit.label.<label>} in gray, then the value in white. */
    public static Component line(String label, Component value) {
        return Component.translatable("tooltip.wheredidiputit.label." + label).withStyle(ChatFormatting.GRAY)
                .append(" ").append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    /** "412 in 2 containers". */
    public static Component stored(long count, int containers) {
        return Component.translatable("tooltip.wheredidiputit.stored", number(count), containers(containers));
    }

    /** "1 container", "3 containers". */
    public static Component containers(int containers) {
        return containers == 1 ? Component.translatable("tooltip.wheredidiputit.containers.one")
                : Component.translatable("tooltip.wheredidiputit.containers.many", number(containers));
    }

    /** A container and where it is: "Chest at 0, -60, 0". */
    public static MutableComponent place(ContainerRecord record) {
        BlockPos pos = record.pos();
        return Component.translatable("tooltip.wheredidiputit.place", FindMessages.name(record), pos.getX(), pos.getY(), pos.getZ());
    }

    /** How long ago a container was seen: "2m ago", or "just now". */
    public static Component age(ContainerRecord record) {
        Component age = ItemMarker.age(System.currentTimeMillis() - record.seenAt());
        return age != null ? age : Component.translatable("gui.wheredidiputit.browser.just_now");
    }

    /** Several parts of one value, apart: "Barrel · 6 blocks away". */
    public static Component joined(Component... parts) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) joined.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            joined.append(parts[i]);
        }
        return joined;
    }

    /** A number grouped by thousands: 1,234. */
    public static String number(long number) {
        return String.format(Locale.ROOT, "%,d", number);
    }
}
