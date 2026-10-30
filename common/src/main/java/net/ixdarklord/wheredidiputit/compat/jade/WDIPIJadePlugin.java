package net.ixdarklord.wheredidiputit.compat.jade;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.hud.Compass;
import net.minecraft.client.renderer.Rect2i;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Tells the compass where Jade's box is each time Jade draws it, so the compass can keep out of its way (author,
 * 2026-10-28). Jade finds it by the annotation on NeoForge and by the {@code jade} entrypoint on Fabric, and only
 * when Jade is installed (it has no Forge build for 1.21.1). Jade 15's {@code TooltipRect.rect} is the box as drawn on
 * screen, in GUI pixels, its scale already applied.
 */
@WailaPlugin(WhereDidIPutIt.MOD_ID)
public final class WDIPIJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addAfterRenderCallback((box, tooltip, graphics, accessor) -> {
            Rect2i rect = tooltip.rect;
            Compass.avoid(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        });
    }
}
