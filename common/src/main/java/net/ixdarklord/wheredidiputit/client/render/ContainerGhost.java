package net.ixdarklord.wheredidiputit.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * The faces a container's ghost is drawn with: its block model's own quads, each with its texture, so the ghost looks
 * like the block. Chests have no block model (a block entity renderer draws them): theirs is built from the game's
 * own chest model parts (single, or the left and right halves of a double chest) posed as {@code ChestRenderer} poses
 * them, in the chest's own texture. Any other block without model quads gets a box of its shape in its particle
 * texture.
 * <p>
 * Each face names its sprite's own texture file ({@code textures/block/barrel_side.png}) with UVs inside it, not the
 * block atlas: CoolCatLib's world effects pass draws nothing with the atlas bound (tried 2026-10-21).
 */
public final class ContainerGhost {
    /** How far each face is pushed out of the block, so the block itself hides its ghost while nothing else does. */
    private static final double LIFT = 0.012;

    private ContainerGhost() {
    }

    /**
     * One face of the ghost: corners in the order {@code VfxDraw.quad} maps a UV rectangle onto (a at (u0, v0), b at
     * (u0, v1), c at (u1, v1), d at (u1, v0)), its texture, and the way it faces, to leave out faces turned away from
     * the camera.
     */
    public record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Identifier texture, float u0, float v0, float u1, float v1, Vec3 normal) {
        public Vec3 center() {
            return this.a.add(this.c).scale(0.5);
        }
    }

    /** The ghost of the blocks at {@code positions}, as they look now; empty for blocks that aren't loaded. */
    public static List<Face> build(Level level, List<BlockPos> positions, AABB shape) {
        List<Face> faces = new ArrayList<>();
        boolean modelled = false;
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock && level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                chest(faces, pos, state, chest);
                modelled = true;
                continue;
            }
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(state.getSeed(pos)), parts);
            for (BlockStateModelPart part : parts) {
                for (Direction direction : Direction.values()) {
                    for (BakedQuad quad : part.getQuads(direction)) modelled |= add(faces, pos, quad);
                }
                for (BakedQuad quad : part.getQuads(null)) modelled |= add(faces, pos, quad);
            }
        }
        if (!modelled && !positions.isEmpty() && level.isLoaded(positions.getFirst())) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet()
                    .get(level.getBlockState(positions.getFirst())).particleMaterial().sprite();
            box(faces, shape, sprite);
        }
        return faces;
    }

    // A model quad, its corners put in UV order. Quads whose UVs aren't a rectangle (none in vanilla) are left out.
    private static boolean add(List<Face> faces, BlockPos pos, BakedQuad quad) {
        float[] u = new float[4];
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            u[i] = UVPair.unpackU(quad.packedUV(i));
            v[i] = UVPair.unpackV(quad.packedUV(i));
        }
        float u0 = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
        float u1 = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
        float v0 = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
        float v1 = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
        int a = corner(u, v, u0, v0);
        int b = corner(u, v, u0, v1);
        int c = corner(u, v, u1, v1);
        int d = corner(u, v, u1, v0);
        if (a < 0 || b < 0 || c < 0 || d < 0) return false;
        // The atlas UVs again, inside the sprite's own texture.
        TextureAtlasSprite sprite = quad.materialInfo().sprite();
        float su = sprite.getU1() - sprite.getU0();
        float sv = sprite.getV1() - sprite.getV0();
        Vec3 normal = Vec3.atLowerCornerOf(quad.direction().getUnitVec3i());
        Vec3 lift = normal.scale(LIFT);
        faces.add(new Face(world(pos, quad.position(a), lift), world(pos, quad.position(b), lift), world(pos, quad.position(c), lift),
                world(pos, quad.position(d), lift), texture(sprite), (u0 - sprite.getU0()) / su, (v0 - sprite.getV0()) / sv,
                (u1 - sprite.getU0()) / su, (v1 - sprite.getV0()) / sv, normal));
        return true;
    }

    // A chest's ghost: the faces of its model's cubes (lid closed), in its texture, turned to its facing.
    private static void chest(List<Face> faces, BlockPos pos, BlockState state, ChestBlockEntity chest) {
        ChestType type = state.getValue(ChestBlock.TYPE);
        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(ChestRenderer.LAYERS.select(type));
        Identifier texture = texture(Sheets.chooseSprite(chestMaterial(state, chest), type).texture());
        PoseStack poseStack = new PoseStack();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        poseStack.mulPose(ChestRenderer.modelTransformation(state.getValue(ChestBlock.FACING)));
        root.visit(poseStack, (pose, path, index, cube) -> {
            for (ModelPart.Polygon polygon : cube.polygons) {
                float[] u = new float[4];
                float[] v = new float[4];
                Vec3[] corners = new Vec3[4];
                for (int i = 0; i < 4; i++) {
                    ModelPart.Vertex vertex = polygon.vertices()[i];
                    u[i] = vertex.u();
                    v[i] = vertex.v();
                    Vector3f at = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f());
                    corners[i] = new Vec3(at.x(), at.y(), at.z());
                }
                Vector3f n = pose.transformNormal(polygon.normal(), new Vector3f());
                Vec3 normal = new Vec3(n.x(), n.y(), n.z());
                face(faces, corners, u, v, texture, normal);
            }
        });
    }

    // Which texture the chest renderer gives a chest.
    private static ChestRenderState.ChestMaterialType chestMaterial(BlockState state, ChestBlockEntity chest) {
        if (state.getBlock() instanceof CopperChestBlock copper) {
            return switch (copper.getState()) {
                case UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
                case EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
                case WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
                case OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            };
        }
        if (ChestRenderer.xmasTextures()) return ChestRenderState.ChestMaterialType.CHRISTMAS;
        return chest instanceof TrappedChestBlockEntity ? ChestRenderState.ChestMaterialType.TRAPPED : ChestRenderState.ChestMaterialType.REGULAR;
    }

    // A face from four corners and their UVs, put in UV order and lifted out along its normal.
    private static void face(List<Face> faces, Vec3[] corners, float[] u, float[] v, Identifier texture, Vec3 normal) {
        float u0 = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
        float u1 = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
        float v0 = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
        float v1 = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
        int a = corner(u, v, u0, v0);
        int b = corner(u, v, u0, v1);
        int c = corner(u, v, u1, v1);
        int d = corner(u, v, u1, v0);
        if (a < 0 || b < 0 || c < 0 || d < 0) return;
        Vec3 lift = normal.scale(LIFT);
        faces.add(new Face(corners[a].add(lift), corners[b].add(lift), corners[c].add(lift), corners[d].add(lift), texture, u0, v0, u1, v1, normal));
    }

    private static int corner(float[] u, float[] v, float cu, float cv) {
        for (int i = 0; i < 4; i++) {
            if (Math.abs(u[i] - cu) < 1.0E-6F && Math.abs(v[i] - cv) < 1.0E-6F) return i;
        }
        return -1;
    }

    private static Vec3 world(BlockPos pos, Vector3fc local, Vec3 lift) {
        return new Vec3(pos.getX() + local.x(), pos.getY() + local.y(), pos.getZ() + local.z()).add(lift);
    }

    // A sprite's own texture file: minecraft:block/barrel_side is minecraft:textures/block/barrel_side.png.
    private static Identifier texture(TextureAtlasSprite sprite) {
        return texture(sprite.contents().name());
    }

    private static Identifier texture(Identifier sprite) {
        return Identifier.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
    }

    // The six faces of a box in one sprite, for blocks with no model of their own.
    private static void box(List<Face> faces, AABB box, TextureAtlasSprite sprite) {
        AABB b = box.inflate(LIFT);
        Identifier texture = texture(sprite);
        Vec3 p000 = new Vec3(b.minX, b.minY, b.minZ), p100 = new Vec3(b.maxX, b.minY, b.minZ);
        Vec3 p010 = new Vec3(b.minX, b.maxY, b.minZ), p110 = new Vec3(b.maxX, b.maxY, b.minZ);
        Vec3 p001 = new Vec3(b.minX, b.minY, b.maxZ), p101 = new Vec3(b.maxX, b.minY, b.maxZ);
        Vec3 p011 = new Vec3(b.minX, b.maxY, b.maxZ), p111 = new Vec3(b.maxX, b.maxY, b.maxZ);
        // Each side from its top-left corner, seen from outside, so the texture stands upright.
        faces.add(new Face(p110, p100, p000, p010, texture, 0, 0, 1, 1, new Vec3(0, 0, -1)));
        faces.add(new Face(p011, p001, p101, p111, texture, 0, 0, 1, 1, new Vec3(0, 0, 1)));
        faces.add(new Face(p010, p000, p001, p011, texture, 0, 0, 1, 1, new Vec3(-1, 0, 0)));
        faces.add(new Face(p111, p101, p100, p110, texture, 0, 0, 1, 1, new Vec3(1, 0, 0)));
        faces.add(new Face(p010, p011, p111, p110, texture, 0, 0, 1, 1, new Vec3(0, 1, 0)));
        faces.add(new Face(p001, p000, p100, p101, texture, 0, 0, 1, 1, new Vec3(0, -1, 0)));
    }
}
