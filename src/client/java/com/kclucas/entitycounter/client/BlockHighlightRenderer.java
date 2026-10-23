package com.kclucas.entitycounter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;

/** Zeichnet den gezählten Block als grün leuchtenden Würfel: halbtransparente Flächen plus kräftiger Rahmen. */
public final class BlockHighlightRenderer {

	private static final int FILL_COLOR = 0x4A3CFF5A;
	private static final int LINE_COLOR = 0xFF3CFF5A;
	private static final float LINE_WIDTH = 3.0F;
	/** Minimal größer als der Block, damit die Flächen nicht mit seinen Seiten flimmern. */
	private static final float INFLATE = 0.004F;

	private BlockHighlightRenderer() {
	}

	public static void render(LevelRenderContext context, BlockPos pos) {
		PoseStack poseStack = context.poseStack();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		SubmitNodeCollector collector = context.submitNodeCollector();

		// Alles relativ zur Blockecke zeichnen, die Verschiebung zur Kamera steckt in der Matrix.
		poseStack.pushPose();
		poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);

		float a = -INFLATE;
		float b = 1.0F + INFLATE;
		collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, fill) -> {
			Matrix4f m = pose.pose();
			quad(fill, m, a, a, a, b, a, a, b, a, b, a, a, b); // unten
			quad(fill, m, a, b, a, a, b, b, b, b, b, b, b, a); // oben
			quad(fill, m, a, a, a, a, b, a, b, b, a, b, a, a); // Norden
			quad(fill, m, a, a, b, b, a, b, b, b, b, a, b, b); // Süden
			quad(fill, m, a, a, a, a, a, b, a, b, b, a, b, a); // Westen
			quad(fill, m, b, a, a, b, b, a, b, b, b, b, a, b); // Osten
		});
		collector.submitShapeOutline(poseStack, Shapes.box(a, a, a, b, b, b), RenderTypes.lines(),
				LINE_COLOR, LINE_WIDTH, false);

		poseStack.popPose();
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
