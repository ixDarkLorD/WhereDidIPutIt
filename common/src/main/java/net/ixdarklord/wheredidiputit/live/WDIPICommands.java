package net.ixdarklord.wheredidiputit.live;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.ixdarklord.coolcatcore.api.event.v2.common.CommandEvents;
import net.ixdarklord.wheredidiputit.config.MatchMode;
import net.ixdarklord.wheredidiputit.config.WDIPIServerConfig;
import net.ixdarklord.wheredidiputit.find.ItemMatcher;
import net.ixdarklord.wheredidiputit.network.FindResultPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * {@code /wdipi find <item> [radius]}: for operators (gamemaster level), every container in loaded chunks near the
 * command's position holding the item, nearest first, each line a click away from a teleport. Not bound by live search's
 * settings or a player's own reach, but unlooted loot chests stay unread, as reading one rolls its loot. Its text falls
 * back to English for clients without the mod.
 */
public final class WDIPICommands {
    private static final int DEFAULT_RADIUS = 32;
    private static final int MAX_RADIUS = 128;
    /** Lines listed in chat; the rest are counted. */
    private static final int MAX_LINES = 10;

    private WDIPICommands() {
    }

    public static void init() {
        CommandEvents.REGISTER.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("wdipi")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("find")
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(command -> find(command, DEFAULT_RADIUS))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                                        .executes(command -> find(command, IntegerArgumentType.getInteger(command, "radius"))))))));
    }

    private static int find(CommandContext<CommandSourceStack> command, int radius) throws CommandSyntaxException {
        CommandSourceStack source = command.getSource();
        ItemStack query = ItemArgument.getItem(command, "item").createItemStack(1, false);
        boolean nested = WDIPIServerConfig.INCLUDE_SHULKER_CONTENTS.get();
        ItemMatcher matcher = ItemMatcher.of(query, MatchMode.IGNORE_DATA, nested, List.of());
        Vec3 from = source.getPosition();
        List<FindResultPayload.Hit> hits = LiveSearchServer.search(source.getLevel(), from, matcher, radius, null);
        if (hits.isEmpty()) {
            source.sendFailure(Component.translatableWithFallback("commands.wheredidiputit.find.none", "No container within %s blocks has %s",
                    radius, query.getHoverName()));
            return 0;
        }
        long total = hits.stream().mapToLong(FindResultPayload.Hit::count).sum();
        source.sendSuccess(() -> Component.translatableWithFallback("commands.wheredidiputit.find.found", "%s %s in %s containers within %s blocks:",
                total, query.getHoverName(), hits.size(), radius), false);
        for (FindResultPayload.Hit hit : hits.subList(0, Math.min(MAX_LINES, hits.size()))) {
            int distance = (int) Math.round(Vec3.atCenterOf(hit.pos()).distanceTo(from));
            Component where = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", hit.pos().getX(), hit.pos().getY(), hit.pos().getZ()))
                    .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + hit.pos().getX() + " " + (hit.pos().getY() + 1) + " " + hit.pos().getZ()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
            source.sendSuccess(() -> Component.translatableWithFallback("commands.wheredidiputit.find.line", " %s %s: %s (%s blocks)",
                    where, hit.block().toString(), hit.count(), distance), false);
        }
        if (hits.size() > MAX_LINES) {
            source.sendSuccess(() -> Component.translatableWithFallback("commands.wheredidiputit.find.more", " and %s more", hits.size() - MAX_LINES), false);
        }
        return hits.size();
    }
}
