package com.kclucas.entitycounter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;

/** Zeichnet den gezählten Block als grün leuchtenden Würfel: halbtransparente Flächen plus kräftiger Rahmen. */
public final class BlockHighlightRenderer {

	private static final int FILL_COLOR = 0x4A3CFF5A;
	private static final int LINE_COLOR = 0xFF3CFF5A;
	private static final float LINE_WIDTH = 3.0F;
	/** Minimal größer als der Block, damit die Flächen nicht mit seinen Seiten flimmern. */
	private static final double INFLATE = 0.004;

	private BlockHighlightRenderer() {
	}

	public static void render(WorldRenderContext context, BlockPos pos) {
		PoseStack poseStack = context.matrices();
		MultiBufferSource buffers = context.consumers();
		if (poseStack == null || buffers == null) {
			return;
		}

		Vec3 camera = context.worldState().cameraRenderState.pos;
		AABB box = new AABB(pos).inflate(INFLATE);
		float x1 = (float) (box.minX - camera.x), y1 = (float) (box.minY - camera.y), z1 = (float) (box.minZ - camera.z);
		float x2 = (float) (box.maxX - camera.x), y2 = (float) (box.maxY - camera.y), z2 = (float) (box.maxZ - camera.z);

		Matrix4f m = poseStack.last().pose();
		VertexConsumer fill = buffers.getBuffer(RenderTypes.debugFilledBox());
		quad(fill, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2); // unten
		quad(fill, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1); // oben
		quad(fill, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1); // Norden
		quad(fill, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2); // Süden
		quad(fill, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1); // Westen
		quad(fill, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2); // Osten

		VertexConsumer lines = buffers.getBuffer(RenderTypes.lines());
		ShapeRenderer.renderShape(poseStack, lines, Shapes.create(box),
				-camera.x, -camera.y, -camera.z, LINE_COLOR, LINE_WIDTH);
	}

	/** Eine Fläche, in beiden Drehrichtungen, damit sie von innen und außen sichtbar ist. */
	private static void quad(VertexConsumer v, Matrix4f m,
			float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		v.addVertex(m, ax, ay, az).setColor(FILL_COLOR);
		v.addVertex(m, bx, by, bz).setColor(FILL_COLOR);
		v.addVertex(m, cx, cy, cz).setColor(FILL_COLOR);
		v.addVertex(m, dx, dy, dz).setColor(FILL_COLOR);

		v.addVertex(m, dx, dy, dz).setColor(FILL_COLOR);
		v.addVertex(m, cx, cy, cz).setColor(FILL_COLOR);
		v.addVertex(m, bx, by, bz).setColor(FILL_COLOR);
		v.addVertex(m, ax, ay, az).setColor(FILL_COLOR);
	}
}
