package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * An arrow round the crosshair pointing to the container being highlighted while it is outside the view, in the
 * highlight color. Once it is in view its outline is enough.
 */
public final class TargetArrow {
    // A white arrowhead pointing right, drawn by tools/gen_arrow_texture.py and tinted when drawn.
    private static final Identifier ARROW = WhereDidIPutIt.id("textures/gui/arrow.png");
    private static final int SIZE = 16;
    // How far from the crosshair the arrow sits, in GUI pixels.
    private static final float RADIUS = 32.0F;

    private TargetArrow() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(TargetArrow::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !WDIPIClientConfig.OFF_SCREEN_ARROW.get()) return;
        ContainerHighlight.Target target = ContainerHighlight.current(minecraft.level);
        if (target == null) return;

        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 to = target.box().getCenter().subtract(camera.position());
        Vector3fc forward = camera.forwardVector();
        Vector3fc up = camera.upVector();
        Vector3fc left = camera.leftVector();
        // The container in the camera's own axes: ahead, to the right and up.
        double ahead = to.x * forward.x() + to.y * forward.y() + to.z * forward.z();
        double right = -(to.x * left.x() + to.y * left.y() + to.z * left.z());
        double above = to.x * up.x() + to.y * up.y() + to.z * up.z();
        // Half the view's angles, as tangents: the vertical one from the FOV, the horizontal one by the window's shape.
        double tanV = Math.tan(Math.toRadians(camera.getFov()) / 2.0);
        double tanH = tanV * graphics.guiWidth() / graphics.guiHeight();
        if (ahead > 0.0 && Math.abs(right) <= ahead * tanH && Math.abs(above) <= ahead * tanV) return;

        // On the screen up is negative; a container straight behind points down.
        float angle = Math.abs(right) < 1.0E-4 && Math.abs(above) < 1.0E-4 ? Mth.HALF_PI : (float) Math.atan2(-above, right);
        float alpha = target.alpha(Util.getMillis());
        float radius = RADIUS + 2.0F * Mth.sin(Util.getMillis() / 160.0F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F);
        graphics.pose().rotate(angle);
        graphics.pose().translate(radius, 0.0F);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, -SIZE / 2, -SIZE / 2, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE,
                ARGB.color(Math.round(alpha * 255.0F), WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF));
        graphics.pose().popMatrix();
    }
}
