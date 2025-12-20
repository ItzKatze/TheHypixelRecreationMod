package gg.itzkatze.thehypixelrecreationmod.features.region;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class RegionRenderer {

    public static void render(PoseStack matrices, Camera camera, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        renderBeaconBeams(matrices, camera, tickDelta);
        renderBoundingBoxes(matrices, camera, tickDelta);
    }

    /* ==================== Bounding Boxes ==================== */
    private static void renderBoundingBoxes(PoseStack matrices, Camera camera, float tickDelta) {
        List<RegionBox> boxes = RegionTracker.getActiveBoxes();
        if (boxes.isEmpty()) return;

        Vec3 camPos = camera.position();
        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f matrix = matrices.last().pose();
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        for (RegionBox box : boxes) {

            int color = box.getColor();
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = 0.2f;

            AABB bb = new AABB(
                    box.corner1.getX(), box.corner1.getY(), box.corner1.getZ(),
                    box.corner2.getX() + 1.0, box.corner2.getY() + 1.0, box.corner2.getZ() + 1.0
            );

            // Render filled box
            VertexConsumer fillBuffer = bufferSource.getBuffer(RenderTypes.lines());
            addBox(matrix, fillBuffer, bb, r, g, b, a);
        }

        bufferSource.endBatch();
        matrices.popPose();
    }

    private static void addBox(Matrix4f matrix, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
        // 12 edges
        addLine(matrix, buffer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
        addLine(matrix, buffer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, r, g, b, a);

        addLine(matrix, buffer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
        addLine(matrix, buffer, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);

        addLine(matrix, buffer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, r, g, b, a);
        addLine(matrix, buffer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
        addLine(matrix, buffer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, r, g, b, a);
    }

    private static void addLine(Matrix4f matrix, VertexConsumer buffer, double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                float r, float g, float b, float a) {
        buffer.addVertex(matrix, (float) x1, (float) y1, (float) z1)
                .setLineWidth(5f)
                .setColor(r, g, b, a)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0f, 1f, 0f);
        buffer.addVertex(matrix, (float) x2, (float) y2, (float) z2)
                .setLineWidth(5f)
                .setColor(r, g, b, a)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setNormal(0f, 1f, 0f);
    }

    /* ==================== Beacon Beams ==================== */
    private static void renderBeaconBeams(PoseStack matrices, Camera camera, float tickDelta) {
        List<RegionBox> boxes = RegionTracker.getIncompleteBoxes();
        if (boxes.isEmpty()) return;

        Vec3 camPos = camera.position();
        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        Minecraft client = Minecraft.getInstance();
        Matrix4f matrix = matrices.last().pose();
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

        for (RegionBox box : boxes) {
            if (box.getLastCorner() != null) {
                renderSingleBeacon(matrix, bufferSource, box.getLastCorner(), box.getColor(), tickDelta, client);
            }
        }

        bufferSource.endBatch();
        matrices.popPose();
    }

    private static void renderSingleBeacon(Matrix4f matrix, MultiBufferSource.BufferSource bufferSource,
                                           BlockPos pos, int color, float tickDelta, Minecraft client) {
        float time = (client.level.getGameTime() + tickDelta) * 0.5f;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.7f;

        float x = pos.getX() + 0.5f;
        float y = pos.getY() + 1f;
        float z = pos.getZ() + 0.5f;
        float height = client.level.getMaxY() - y;

        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());

        for (int i = 0; i < 8; i++) {
            double angle = (time + i * Math.PI / 4) % (Math.PI * 2);
            float offsetX = (float) Math.cos(angle) * 0.15f;
            float offsetZ = (float) Math.sin(angle) * 0.15f;

            for (int segment = 0; segment < 10; segment++) {
                float y1 = y + (height / 10f) * segment;
                float y2 = y + (height / 10f) * (segment + 1);
                float pulse = 0.8f + 0.2f * (float) Math.sin(time * 0.5f + segment * 0.3f);

                float x1 = x + offsetX * pulse;
                float z1 = z + offsetZ * pulse;
                float x2 = x + offsetX * pulse * 0.7f;
                float z2 = z + offsetZ * pulse * 0.7f;

                addLine(matrix, buffer, x1, y1, z1, x2, y2, z2, r, g, b, a);
            }
        }
    }
}
