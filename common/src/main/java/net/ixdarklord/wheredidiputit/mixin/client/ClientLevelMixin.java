package net.ixdarklord.wheredidiputit.mixin.client;

import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.memory.ContainerMemory;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells {@link ContainerMemory} and {@link ContainerHighlight} of every block the server says changed (single updates,
 * section updates, and its answer to a block the player broke), so a broken container is forgotten, and stops being
 * pointed out, at once rather than at the next sweep. The server's word only: a break the client merely predicted may
 * yet be refused.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    private void wheredidiputit$onServerBlockState(BlockPos pos, BlockState state, int updateFlags, CallbackInfo ci) {
        ContainerMemory.onServerBlockState((ClientLevel) (Object) this, pos, state);
        ContainerHighlight.onServerBlockState((ClientLevel) (Object) this, pos, state);
    }
}
