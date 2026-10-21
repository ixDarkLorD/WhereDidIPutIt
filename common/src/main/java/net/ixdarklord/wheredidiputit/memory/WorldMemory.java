package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Everything remembered for one singleplayer save or one server, kept in the game folder (never in the world save):
 * {@code wheredidiputit/sp/<save folder>/} or {@code wheredidiputit/mp/<host_port>/}, one gzipped NBT file per
 * dimension ({@code <namespace>/<path>.dat}, slashes in the path becoming underscores). Dimensions load the first time
 * they are needed.
 */
public final class WorldMemory {
    /** One writer thread, so a dimension's saves land in the order they were made. */
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, WhereDidIPutIt.MOD_NAME + " writer");
        thread.setDaemon(true);
        return thread;
    });

    private final Path folder;
    private final RegistryOps<Tag> ops;
    private final Map<ResourceKey<Level>, DimensionMemory> dimensions = new HashMap<>();

    private WorldMemory(Path folder, HolderLookup.Provider registries) {
        this.folder = folder;
        this.ops = ContainerRecord.ops(registries);
    }

    /** The memory for the world or server the client is in now; empty when it can't be told apart from others (e.g. no server address). */
    public static @Nullable WorldMemory forCurrentWorld(Minecraft minecraft, HolderLookup.Provider registries) {
        Path root = Platform.getGameFolder().resolve(WhereDidIPutIt.MOD_ID);
        IntegratedServer singleplayer = minecraft.getSingleplayerServer();
        if (singleplayer != null) {
            Path save = singleplayer.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName();
            return new WorldMemory(root.resolve("sp").resolve(sanitize(save.toString())), registries);
        }
        ServerData server = minecraft.getCurrentServer();
        if (server != null) {
            if (server.isRealm()) return new WorldMemory(root.resolve("realms").resolve(sanitize(server.name)), registries);
            ServerAddress address = ServerAddress.parseString(server.ip);
            return new WorldMemory(root.resolve("mp").resolve(sanitize(address.getHost() + "_" + address.getPort())), registries);
        }
        return null;
    }

    /** This world's or server's folder name, as files and commands show it. */
    public String name() {
        return this.folder.getFileName().toString();
    }

    public DimensionMemory dimension(ResourceKey<Level> dimension) {
        return this.dimensions.computeIfAbsent(dimension, this::load);
    }

    /** Saves every dimension with unsaved changes, writing the files off the client thread. */
    public void saveDirty() {
        this.dimensions.forEach((key, memory) -> {
            if (!memory.isDirty()) return;
            CompoundTag tag = memory.save(this.ops);
            Path file = this.file(key);
            WRITER.execute(() -> write(tag, file));
        });
    }

    /** Where exports are kept: {@code wheredidiputit/exports/} in the game folder. */
    public static Path exportsFolder() {
        return Platform.getGameFolder().resolve(WhereDidIPutIt.MOD_ID).resolve("exports");
    }

    /**
     * Copies this world's memory to {@code exports/<name>/}, after its unsaved changes are written: both on the writer
     * thread, in that order.
     *
     * @return the folder it goes to
     */
    public Path export(String name) {
        this.saveDirty();
        Path target = exportsFolder().resolve(sanitize(name));
        WRITER.execute(() -> {
            try {
                copyFolder(this.folder, target);
            } catch (IOException e) {
                WhereDidIPutIt.LOGGER.error("Couldn't export the remembered containers to {}", target, e);
            }
        });
        return target;
    }

    /**
     * Merges {@code exports/<name>/} into this world's memory: for each container, whichever record saw it last. Loaded
     * dimensions take it in place; the others' files are merged and written.
     *
     * @return how many records were taken in
     * @throws IOException when there is no such export, or a file of it can't be read
     */
    public int importFrom(String name) throws IOException {
        Path source = exportsFolder().resolve(sanitize(name));
        if (!Files.isDirectory(source)) throw new NoSuchFileException(source.toString());
        List<Path> files;
        try (var walk = Files.walk(source)) {
            files = walk.filter(file -> Files.isRegularFile(file) && file.getFileName().toString().endsWith(".dat")).toList();
        }
        int taken = 0;
        for (Path file : files) {
            DimensionMemory incoming = DimensionMemory.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), this.ops);
            Path target = this.folder.resolve(source.relativize(file).toString());
            DimensionMemory loaded = this.dimensions.entrySet().stream().filter(entry -> this.file(entry.getKey()).equals(target))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            if (loaded != null) {
                taken += loaded.mergeNewer(incoming);
                continue;
            }
            DimensionMemory stored = this.read(target);
            taken += stored.mergeNewer(incoming);
            CompoundTag tag = stored.save(this.ops);
            WRITER.execute(() -> write(tag, target));
        }
        return taken;
    }

    private static void copyFolder(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) return;
        try (var walk = Files.walk(from)) {
            for (Path file : walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".dat")).toList()) {
                Path copy = to.resolve(from.relativize(file).toString());
                Files.createDirectories(copy.getParent());
                Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private DimensionMemory load(ResourceKey<Level> dimension) {
        return this.read(this.file(dimension));
    }

    private DimensionMemory read(Path file) {
        if (!Files.isRegularFile(file)) return new DimensionMemory();
        try {
            return DimensionMemory.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()), this.ops);
        } catch (IOException | RuntimeException e) {
            WhereDidIPutIt.LOGGER.error("Couldn't read the remembered containers in {}; starting over", file, e);
            return new DimensionMemory();
        }
    }

    private Path file(ResourceKey<Level> dimension) {
        Identifier id = dimension.identifier();
        return this.folder.resolve(sanitize(id.getNamespace())).resolve(sanitize(id.getPath()) + ".dat");
    }

    /** Writes to a temporary file first, so a crash mid-write never leaves a half-written file behind. */
    private static void write(CompoundTag tag, Path file) {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            NbtIo.writeCompressed(tag, temporary);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            WhereDidIPutIt.LOGGER.error("Couldn't save the remembered containers to {}", file, e);
        }
    }

    /** A single file or folder name, safe on every system and never leaving its folder. */
    private static String sanitize(String name) {
        String safe = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_").replace("..", "_");
        return safe.isBlank() ? "_" : safe;
    }
}
