package net.ixdarklord.wheredidiputit.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.wheredidiputit.WhereDidIPutIt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Found containers' ghosts: their blocks drawn by the game's own renderers, with the player's own camera, into a
 * screen-sized target of their own with its own depth buffer, then laid over the world at one opacity wherever the world
 * is in front of them. Drawn opaque first, a model shows only its outside, as the real block does: faces drawn
 * see-through one by one showed its inside and far faces (the author, 2026-10-21). Drawn with the world's own
 * projection, a ghost is in perspective from every side, which a picture of the block from one direction was not (a
 * CoolCatLib scene viewport, tried and dropped the same day). Chests and other block entities' models come out as the
 * game draws them. They are unshaded, every face full bright ({@link #lights}), so they stand out through walls.
 * <p>
 * Each block is drawn a hair larger than itself ({@link #GROW}), so where the container is in plain sight its ghost is
 * just in front of it and the composite ({@code core/ghost}) leaves the real block showing.
 */
public final class ContainerGhost {
    /** How much larger than the block its ghost is drawn, about its middle: a hundredth of a block on each side. */
    private static final float GROW = 1.02F;
    private static final int FULL_BRIGHT = 0xF000F0;
    /**
     * How strong the ghosts' two lights are. A face gets {@code 0.4 + 0.6 * (light0 . n + light1 . n)} of its colour,
     * capped at 1 ({@code light.glsl}); two opposite lights this strong along a diagonal give any face of a block more
     * than that, so every face is full bright.
     */
    private static final float LIGHT_STRENGTH = 10.0F;
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
    private static final RenderPipeline COMPOSITE = RenderPipeline.builder()
            .withLocation(WhereDidIPutIt.id("pipeline/ghost"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(WhereDidIPutIt.id("core/ghost"))
            .withBindGroupLayout(BindGroupLayout.builder().withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("GhostSampler").build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("GhostDepthSampler").build())
            .withBindGroupLayout(BindGroupLayout.builder().withSampler("DepthSampler").build())
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .build();

    private static @Nullable TextureTarget target;
    private static @Nullable BlockModelResolver resolver;
    private static @Nullable GpuBuffer lights;
    // One model for each kind of block shown, made when it is first needed.
    private static final Map<BlockState, BlockModelRenderState> MODELS = new HashMap<>();

    private ContainerGhost() {
    }

    /** A container's ghost: its blocks as they were when it was found. */
    public record Ghost(List<Placed> blocks) {
    }

    public record Placed(BlockPos pos, BlockState state) {
    }

    /** Forgets the block models made so far, so a find after the game's models were reloaded draws the new ones. */
    public static void clearModels() {
        resolver = null;
        MODELS.clear();
    }

    /** The ghost of the blocks at {@code positions}, as they are now; null when none of them is loaded. */
    public static @Nullable Ghost build(Level level, List<BlockPos> positions) {
        List<Placed> blocks = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (level.isLoaded(pos)) blocks.add(new Placed(pos.immutable(), level.getBlockState(pos)));
        }
        return blocks.isEmpty() ? null : new Ghost(List.copyOf(blocks));
    }

    /**
     * Draws the ghosts over the world in {@code color} (their tint and opacity). Called from the world effects pass,
     * right after the level, while its projection and depth are still there.
     */
    public static void draw(List<Ghost> ghosts, int color) {
        if (ghosts.isEmpty() || (color >>> 24) == 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.gameRenderer.mainRenderTarget();
        if (main.getDepthTextureView() == null || main.getColorTextureView() == null) return;
        TextureTarget ghostTarget = target(main);
        drawBlocks(minecraft, ghostTarget, ghosts);
        composite(main, ghostTarget, color);
    }

    // The ghosts' blocks, opaque, with the camera the level was drawn with.
    private static void drawBlocks(Minecraft minecraft, TextureTarget ghostTarget, List<Ghost> ghosts) {
        // Depth is reversed since 26.2: cleared to 0, nearer is larger.
        RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(ghostTarget.getColorTexture(), new Vector4f(0), ghostTarget.getDepthTexture(), 0.0);
        CameraRenderState camera = minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        // The level has popped its view from the model-view stack by now: it goes back on for these blocks.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(camera.viewRotationMatrix);
        RenderSystem.outputColorTextureOverride = ghostTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = ghostTarget.getDepthTextureView();
        try {
            if (resolver == null) resolver = new BlockModelResolver(minecraft.getModelManager());
            BlockModelResolver models = resolver;
            RenderSystem.setShaderLights(lights());
            FeatureRenderDispatcher features = minecraft.gameRenderer.featureRenderDispatcher();
            SubmitNodeStorage storage = new SubmitNodeStorage();
            PoseStack pose = new PoseStack();
            for (Ghost ghost : ghosts) {
                for (Placed block : ghost.blocks()) {
                    BlockModelRenderState model = MODELS.computeIfAbsent(block.state(), state -> {
                        BlockModelRenderState made = new BlockModelRenderState();
                        models.update(made, state, CONTEXT);
                        return made;
                    });
                    if (model.isEmpty()) continue;
                    pose.pushPose();
                    // From the camera, in floats: the block's place less the camera's stays small anywhere in the world.
                    pose.translate(block.pos().getX() - camera.pos.x + 0.5, block.pos().getY() - camera.pos.y + 0.5, block.pos().getZ() - camera.pos.z + 0.5);
                    pose.scale(GROW, GROW, GROW);
                    pose.translate(-0.5F, -0.5F, -0.5F);
                    model.submit(pose, storage, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                    pose.popPose();
                }
            }
            features.renderAllFeatures(storage);
        } finally {
            minecraft.gameRenderer.lighting().setupFor(Lighting.Entry.LEVEL);
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            modelView.popMatrix();
        }
    }

    // The ghosts' lights: one each way along a diagonal, so no face of a block is turned away from both.
    private static GpuBufferSlice lights() {
        if (lights == null) {
            float strength = LIGHT_STRENGTH / (float) Math.sqrt(3.0);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer data = Std140Builder.onStack(stack, Lighting.UBO_SIZE)
                        .putVec3(strength, strength, strength)
                        .putVec3(-strength, -strength, -strength)
                        .get();
                lights = RenderSystem.getDevice().createBuffer(() -> "Where Did I Put It? ghost lights", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, Lighting.UBO_SIZE);
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(lights.slice(), data);
            }
        }
        return lights.slice();
    }

    // The ghosts' picture over the world, where the world is in front of them.
    private static void composite(RenderTarget main, TextureTarget ghostTarget, int color) {
        Vector4f modulator = new Vector4f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, (color >>> 24) / 255.0F);
        var transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(), modulator, new Vector3f(), new Matrix4f());
        GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "Where Did I Put It? ghosts", main.getColorTextureView(), Optional.empty())) {
            pass.setPipeline(COMPOSITE);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("GhostSampler", ghostTarget.getColorTextureView(), nearest);
            pass.bindTexture("GhostDepthSampler", ghostTarget.getDepthTextureView(), nearest);
            pass.bindTexture("DepthSampler", main.getDepthTextureView(), nearest);
            pass.draw(3, 1, 0, 0);
        }
    }

    // The ghosts' own target, as large as the screen's.
    private static TextureTarget target(RenderTarget main) {
        if (target == null) {
            target = new TextureTarget("Where Did I Put It? ghosts", main.width, main.height, true, GpuFormat.RGBA8_UNORM);
        } else if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height);
        }
        return target;
    }
}
