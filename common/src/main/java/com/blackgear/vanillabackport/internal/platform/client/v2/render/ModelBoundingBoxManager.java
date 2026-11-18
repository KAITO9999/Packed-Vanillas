package com.blackgear.vanillabackport.internal.platform.client.v2.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * Calculates the rendered bounds of an item model in the FIXED display context.
 *
 * <p>This fills an API referenced by the upstream shelf renderer but absent from
 * the public Platform 1.20.1 source. Bounds are derived from the baked quads and
 * the same fixed-context transform used by ItemRenderer.</p>
 */
public enum ModelBoundingBoxManager {
    INSTANCE;

    private static final AABB EMPTY_MODEL_BOUNDS = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public AABB getModelBoundingBox(
        BakedModel model,
        ItemStack stack,
        ItemRenderer itemRenderer,
        Level level,
        int seed
    ) {
        if (model == null) {
            return EMPTY_MODEL_BOUNDS;
        }

        PoseStack transformStack = new PoseStack();
        ItemTransform fixedTransform = model.getTransforms().getTransform(ItemDisplayContext.FIXED);
        fixedTransform.apply(false, transformStack);
        Matrix4f transform = new Matrix4f(transformStack.last().pose());

        Bounds bounds = new Bounds();
        RandomSource random = RandomSource.create();

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            this.includeQuads(model.getQuads(null, direction, random), transform, bounds);
        }

        random.setSeed(seed);
        this.includeQuads(model.getQuads(null, null, random), transform, bounds);

        return bounds.hasVertices()
            ? new AABB(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
            : EMPTY_MODEL_BOUNDS;
    }

    private void includeQuads(List<BakedQuad> quads, Matrix4f transform, Bounds bounds) {
        if (quads == null || quads.isEmpty()) {
            return;
        }

        int stride = DefaultVertexFormat.BLOCK.getIntegerSize();
        if (stride < 3) {
            return;
        }

        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            for (int offset = 0; offset + 2 < vertices.length; offset += stride) {
                Vector4f position = new Vector4f(
                    Float.intBitsToFloat(vertices[offset]),
                    Float.intBitsToFloat(vertices[offset + 1]),
                    Float.intBitsToFloat(vertices[offset + 2]),
                    1.0F
                );
                position.mul(transform);
                bounds.include(position.x, position.y, position.z);
            }
        }
    }

    private static final class Bounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(double x, double y, double z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private boolean hasVertices() {
            return this.minX != Double.POSITIVE_INFINITY;
        }
    }
}
