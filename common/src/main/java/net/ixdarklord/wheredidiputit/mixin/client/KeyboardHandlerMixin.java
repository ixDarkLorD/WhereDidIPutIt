package net.ixdarklord.wheredidiputit.mixin.client;

import net.ixdarklord.wheredidiputit.find.FindKey;
import net.ixdarklord.wheredidiputit.find.HoveredStacks;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The find key in screens that aren't container screens, such as recipe viewers' recipe screens, where only a recipe
 * viewer can say what is under the mouse. Container screens are {@code AbstractContainerScreenMixin}'s.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void wheredidiputit$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        Screen screen = this.minecraft.screen;
        if (action != GLFW.GLFW_PRESS || screen == null || screen instanceof AbstractContainerScreen<?>) return;
        if (handle != this.minecraft.getWindow().handle()) return;
        if (FindKey.onKeyPressed(event, HoveredStacks.fromViewers(screen))) ci.cancel();
    }
}
