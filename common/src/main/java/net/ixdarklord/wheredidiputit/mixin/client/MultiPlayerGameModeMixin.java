package net.ixdarklord.wheredidiputit.mixin.client;

import net.ixdarklord.wheredidiputit.memory.ContainerWatcher;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tells the container watcher which block or entity the player used, so the container screen that opens next is known
 * to be that one's.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void wheredidiputit$onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        ContainerWatcher.onUseBlock(player.level(), blockHit.getBlockPos());
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void wheredidiputit$onInteract(Player player, Entity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ContainerWatcher.onUseEntity(entity);
    }
}
