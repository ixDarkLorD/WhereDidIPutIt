package net.ixdarklord.wheredidiputit.live;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.memory.StoredItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server-side readers of storage mods' networks, for live search: one per installed mod, registered from the common
 * constructors (the loader modules for mods with loader-specific jars). A reader recognises a network's access point (a
 * terminal, or a controller) among a chunk's block entities, says which network it is, so a network reached through
 * several blocks is read once, and lists what it holds. A reader that throws is skipped, as on the client.
 */
public final class NetworkReaders {
    private static final List<Reader> READERS = new CopyOnWriteArrayList<>();

    private NetworkReaders() {
    }

    public interface Reader {
        /** The network {@code blockEntity} gives access to, as an object that is the same for every block of it; null when it isn't an access point. */
        @Nullable Object network(BlockEntity blockEntity);

        /** Everything the network holds, item by item with its amount. */
        List<StoredItem> contents(Object network, BlockEntity accessPoint);
    }

    /** A network found at an access point, and the reader that knows it. */
    public record Access(Reader reader, Object network) {
        /** What the network holds; empty when its reader fails. */
        public List<StoredItem> contents(BlockEntity accessPoint) {
            try {
                return this.reader.contents(this.network, accessPoint);
            } catch (RuntimeException | LinkageError e) {
                WhereDidIPutIt.LOGGER.debug("Couldn't read the storage network at {}", accessPoint.getBlockPos(), e);
                return List.of();
            }
        }
    }

    public static void register(Reader reader) {
        READERS.add(reader);
    }

    /** The network {@code blockEntity} gives access to, or null. */
    public static @Nullable Access of(BlockEntity blockEntity) {
        for (Reader reader : READERS) {
            try {
                Object network = reader.network(blockEntity);
                if (network != null) return new Access(reader, network);
            } catch (RuntimeException | LinkageError e) {
                WhereDidIPutIt.LOGGER.debug("Storage network reader {} failed at {}", reader.getClass().getName(), blockEntity.getBlockPos(), e);
            }
        }
        return null;
    }

    public static boolean any() {
        return !READERS.isEmpty();
    }
}
