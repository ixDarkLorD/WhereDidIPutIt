package net.ixdarklord.wheredidiputit.compat.jade;

import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.hud.Compass;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Tells the compass where Jade's box is each time Jade draws it, so the compass can keep out of its way (author,
 * 2026-10-28). Jade finds it by the annotation on Forge and by the {@code jade} entrypoint on Fabric, and only when
 * Jade is installed. Jade 11 (1.20.1) hands its after-render callback the box as a {@code Rect2i} in GUI pixels.
 */
@WailaPlugin(WhereDidIPutIt.MOD_ID)
public final class WDIPIJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addAfterRenderCallback((tooltip, rect, graphics, accessor) ->
                Compass.avoid(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()));
    }
}
