package net.ixdarklord.wheredidiputit.client.hud;

import net.ixdarklord.wheredidiputit.mixin.client.GameRendererInvoker;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Where a point in the world falls on the HUD: its GUI coordinates while it is in view, and the angle round the
 * crosshair that points to it, for arrows. The camera-axes maths of Why Can't I Sleep?'s {@code BlockerArrows}.
 *
 * @param inView whether the point is inside the view
 * @param x      its GUI x while in view
 * @param y      its GUI y while in view
 * @param angle  the angle round the crosshair towards it, in radians (screen y grows down)
 */
public record ScreenProjection(boolean inView, float x, float y, float angle) {
    public static ScreenProjection of(Minecraft minecraft, GuiGraphics graphics, Vec3 point) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 to = point.subtract(camera.getPosition());
        Vector3fc forward = camera.getLookVector();
        Vector3fc up = camera.getUpVector();
        Vector3fc left = camera.getLeftVector();
        // The point in the camera's own axes: ahead, to the right and up.
        double ahead = to.x * forward.x() + to.y * forward.y() + to.z * forward.z();
        double right = -(to.x * left.x() + to.y * left.y() + to.z * left.z());
        double above = to.x * up.x() + to.y * up.y() + to.z * up.z();
        // Half the view's angles, as tangents: the vertical one from the FOV, the horizontal one by the window's shape.
        double tanV = Math.tan(Math.toRadians(((GameRendererInvoker) minecraft.gameRenderer).wheredidiputit$getFov(camera, minecraft.getFrameTime(), true)) / 2.0);
        double tanH = tanV * graphics.guiWidth() / graphics.guiHeight();
        boolean inView = ahead > 0.0 && Math.abs(right) <= ahead * tanH && Math.abs(above) <= ahead * tanV;
        float x = 0.0F;
        float y = 0.0F;
        if (inView) {
            x = (float) ((1.0 + right / (ahead * tanH)) / 2.0 * graphics.guiWidth());
            y = (float) ((1.0 - above / (ahead * tanV)) / 2.0 * graphics.guiHeight());
        }
        // A point straight behind points down.
        float angle = Math.abs(right) < 1.0E-4 && Math.abs(above) < 1.0E-4 ? Mth.HALF_PI : (float) Math.atan2(-above, right);
        return new ScreenProjection(inView, x, y, angle);
    }
}
