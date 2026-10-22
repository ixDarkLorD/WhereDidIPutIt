package net.ixdarklord.wheredidiputit.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;

/**
 * A texture drawn in one colour on the HUD, its alpha the colour's: 1.21.1's {@code GuiGraphics.blit} takes no colour,
 * so it goes through the shader colour, with blending on for the alpha.
 */
public final class TintedBlit {
    private TintedBlit() {
    }

    public static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth,
                            int textureHeight, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(ARGB32.red(argb) / 255.0F, ARGB32.green(argb) / 255.0F, ARGB32.blue(argb) / 255.0F, ARGB32.alpha(argb) / 255.0F);
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
