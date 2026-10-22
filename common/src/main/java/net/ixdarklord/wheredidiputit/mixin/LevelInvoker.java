package net.ixdarklord.wheredidiputit.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A level's entities by UUID: 1.20.1's {@code Level} keeps its entity getter to itself (26.1 has {@code getEntity(UUID)}). */
@Mixin(Level.class)
public interface LevelInvoker {
    @Invoker("getEntities")
    LevelEntityGetter<Entity> wheredidiputit$getEntities();
}
