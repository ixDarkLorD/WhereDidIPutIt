package net.ixdarklord.wheredidiputit.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientGuiEvents;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.ixdarklord.wheredidiputit.client.render.ContainerHighlight;
import net.ixdarklord.wheredidiputit.config.WDIPIClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.ixdarklord.wheredidiputit.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arrows round the crosshair, one for each container being pointed out that isn't wholly inside the view, in the
 * highlight color; arrows that would overlap fan apart (as in Why Can't I Sleep?'s {@code BlockerArrows}). A
 * container wholly in view has only its outline and its item marker (author, 2026-10-21).
 */
public final class TargetArrow {
    // Why Can't I Sleep?'s arrow, drawn by tools/gen_arrow_texture.ps1: two frames side by side, solid then hollow,
    // white and grey so they take the highlight color. Only the solid one is drawn.
    private static final ResourceLocation ARROW = WhereDidIPutIt.id("textures/gui/arrow.png");
    private static final int SIZE = 16;
    private static final int SHEET_WIDTH = SIZE * 2;
    // How far from the crosshair the arrows sit, in GUI pixels.
    private static final float RADIUS = 32.0F;
    // The room one arrow takes along the ring, in GUI pixels: closer ones are fanned apart.
    private static final float SPACING = 15.0F;
    private static final float ARROW_Z = 400.0F;

    private TargetArrow() {
    }

    public static void init() {
        ClientGuiEvents.RENDER_HUD.register(TargetArrow::render);
    }

    private static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.options.hideGui || !WDIPIClientConfig.OFF_SCREEN_ARROW.get()) return;
        List<Arrow> arrows = new ArrayList<>();
        long now = Util.getMillis();
        for (ContainerHighlight.Target target : ContainerHighlight.current(minecraft.level)) {
            if (fullyInView(minecraft, graphics, target.box())) continue;
            ScreenProjection on = ScreenProjection.of(minecraft, graphics, target.box().getCenter());
            arrows.add(new Arrow(on.angle(), target.alpha(now)));
        }
        if (arrows.isEmpty()) return;
        fanOut(arrows, SPACING / RADIUS);

        int rgb = WDIPIClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        float radius = RADIUS + 2.0F * Mth.sin(now / 160.0F);
        for (Arrow arrow : arrows) {
            graphics.pose().pushPose();
            // In front of everything drawn earlier on the HUD (the item markers' items, drawn some 150 deep), so
            // nothing covers them: 1.20.1's GUI has no strata, only depth.
            graphics.pose().translate(graphics.guiWidth() / 2.0F, graphics.guiHeight() / 2.0F, ARROW_Z);
            graphics.pose().mulPose(Axis.ZP.rotation(arrow.angle));
            graphics.pose().translate(radius, 0.0F, 0.0F);
            blitTinted(graphics, ARROW, SIZE, SHEET_WIDTH, ARGB.color(Math.round(arrow.alpha * 255.0F), rgb));
            graphics.pose().popPose();
        }
    }

    /** The sheet's first frame, centred on the pose's origin, in {@code argb}: 1.20.1's blit takes its tint from the shader colour. */
    static void blitTinted(GuiGraphics graphics, ResourceLocation sheet, int size, int sheetWidth, int argb) {
        RenderSystem.enableBlend();
        graphics.setColor((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, (argb >>> 24) / 255.0F);
        graphics.blit(sheet, -size / 2, -size / 2, 0.0F, 0.0F, size, size, sheetWidth, size);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // Whether every corner of the box is inside the view.
    private static boolean fullyInView(Minecraft minecraft, GuiGraphics graphics, AABB box) {
        for (int i = 0; i < 8; i++) {
            Vec3 corner = new Vec3((i & 1) == 0 ? box.minX : box.maxX, (i & 2) == 0 ? box.minY : box.maxY, (i & 4) == 0 ? box.minZ : box.maxZ);
            if (!ScreenProjection.of(minecraft, graphics, corner).inView()) return false;
        }
        return true;
    }

    // Moves arrows closer than the spacing apart, evenly to both sides, so each one stays as near its true angle as it can.
    private static void fanOut(List<Arrow> arrows, float spacing) {
        int count = arrows.size();
        if (count < 2) return;
        spacing = Math.min(spacing, Mth.TWO_PI / count);
        arrows.sort(Comparator.comparingDouble(arrow -> arrow.angle));
        for (int pass = 0; pass < 8; pass++) {
            boolean moved = false;
            for (int i = 0; i < count; i++) {
                Arrow a = arrows.get(i);
                Arrow b = arrows.get((i + 1) % count);
                // The gap from one arrow to the next around the ring; the last one's neighbor is the first, a turn on.
                float gap = b.angle - a.angle + (i == count - 1 ? Mth.TWO_PI : 0.0F);
                if (gap >= spacing - 1.0E-3F) continue;
                float push = (spacing - gap) / 2.0F;
                a.angle -= push;
                b.angle += push;
                moved = true;
            }
            if (!moved) break;
        }
    }

    private static final class Arrow {
        private float angle;
        private final float alpha;

        private Arrow(float angle, float alpha) {
            this.angle = angle;
            this.alpha = alpha;
        }
    }
}
