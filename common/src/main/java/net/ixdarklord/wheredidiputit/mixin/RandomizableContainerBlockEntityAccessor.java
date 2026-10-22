package net.ixdarklord.wheredidiputit.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Whether a loot chest is still unlooted: 1.20.1 keeps its loot table to itself (26.1's {@code RandomizableContainer} has a getter). */
@Mixin(RandomizableContainerBlockEntity.class)
public interface RandomizableContainerBlockEntityAccessor {
    @Accessor("lootTable")
    @Nullable ResourceLocation wheredidiputit$getLootTable();
}
