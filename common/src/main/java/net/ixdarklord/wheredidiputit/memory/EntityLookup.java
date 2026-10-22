package net.ixdarklord.wheredidiputit.memory;

import net.ixdarklord.wheredidiputit.mixin.LevelInvoker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Finds a loaded entity by its UUID, as 26.1's {@code Level.getEntity(UUID)} does. */
public final class EntityLookup {
    private EntityLookup() {
    }

    public static @Nullable Entity get(Level level, UUID uuid) {
        return ((LevelInvoker) level).wheredidiputit$getEntities().get(uuid);
    }
}
