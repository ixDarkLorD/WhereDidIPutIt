package net.ixdarklord.wheredidiputit.memory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientCommandEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Client commands for moving what is remembered between worlds, servers or players: {@code /wdipi_client export [name]}
 * copies this world's memory to {@code wheredidiputit/exports/<name>/} in the game folder, and
 * {@code /wdipi_client import <name>} merges one in (for each container, whichever record saw it last). A friend's
 * export dropped into that folder imports the same way.
 */
public final class MemoryCommands {
    private MemoryCommands() {
    }

    public static void init() {
        ClientCommandEvents.REGISTER.register((dispatcher, context) -> dispatcher.register(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("wdipi_client")
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("export")
                        .executes(command -> export(defaultName()))
                        .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("name", StringArgumentType.word())
                                .executes(command -> export(StringArgumentType.getString(command, "name")))))
                .then(LiteralArgumentBuilder.<SharedSuggestionProvider>literal("import")
                        .then(RequiredArgumentBuilder.<SharedSuggestionProvider, String>argument("name", StringArgumentType.word())
                                .suggests((command, builder) -> SharedSuggestionProvider.suggest(exports(), builder))
                                .executes(command -> importFrom(StringArgumentType.getString(command, "name")))))));
    }

    private static int export(String name) {
        WorldMemory world = ContainerMemory.world();
        if (world == null) {
            ClientCommandEvents.sendError(Component.translatable("commands.wheredidiputit.memory.no_world"));
            return 0;
        }
        Path folder = world.export(name);
        ClientCommandEvents.sendFeedback(Component.translatable("commands.wheredidiputit.memory.exported", folder.getFileName().toString()));
        return 1;
    }

    private static int importFrom(String name) {
        WorldMemory world = ContainerMemory.world();
        if (world == null) {
            ClientCommandEvents.sendError(Component.translatable("commands.wheredidiputit.memory.no_world"));
            return 0;
        }
        try {
            int taken = world.importFrom(name);
            ClientCommandEvents.sendFeedback(Component.translatable("commands.wheredidiputit.memory.imported", taken, name));
            return Math.max(1, taken);
        } catch (NoSuchFileException e) {
            ClientCommandEvents.sendError(Component.translatable("commands.wheredidiputit.memory.no_export", name));
        } catch (IOException | RuntimeException e) {
            WhereDidIPutIt.LOGGER.error("Couldn't import the remembered containers from {}", name, e);
            ClientCommandEvents.sendError(Component.translatable("commands.wheredidiputit.memory.import_failed", name));
        }
        return 0;
    }

    // This world's name and today's date: an export a day keeps its own folder.
    private static String defaultName() {
        WorldMemory world = ContainerMemory.world();
        String base = world != null ? world.name() : "memory";
        return base + "_" + LocalDate.now();
    }

    private static List<String> exports() {
        Path folder = WorldMemory.exportsFolder();
        if (!Files.isDirectory(folder)) return List.of();
        try (Stream<Path> list = Files.list(folder)) {
            return list.filter(Files::isDirectory).map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
