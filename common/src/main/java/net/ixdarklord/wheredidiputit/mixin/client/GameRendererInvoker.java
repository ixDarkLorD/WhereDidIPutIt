package net.ixdarklord.wheredidiputit.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The field of view the level is drawn with (speed and effects included), which 1.20.1's {@code Camera} doesn't hold. */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("getFov")
    double wheredidiputit$getFov(Camera camera, float partialTick, boolean useFovSetting);
}
