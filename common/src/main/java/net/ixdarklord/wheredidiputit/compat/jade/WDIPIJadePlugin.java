package net.ixdarklord.wheredidiputit.compat.jade;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.hud.Compass;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.ui.Rect2f;

/**
 * Tells the compass where Jade's box is each time Jade draws it, so the compass can keep out of its way (author,
 * 2026-10-28). Jade finds it by the annotation on NeoForge and by the {@code jade} entrypoint on Fabric, and only
 * when Jade is installed.
 */
@WailaPlugin(WhereDidIPutIt.MOD_ID)
public final class WDIPIJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addAfterRenderCallback((box, animation, graphics, accessor) -> {
            Rect2f rect = animation.rect;
            Compass.avoid(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
        });
    }
}
