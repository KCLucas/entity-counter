package com.kclucas.entitycounter.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class EntityCounterHud {

	private static final int MARGIN = 4;
	private static final int PADDING = 4;
	private static final int ICON_SIZE = 16;
	private static final int ROW_HEIGHT = 18;
	private static final int LINE_HEIGHT = 10;

	private static final int COLOR_BACKGROUND = 0x90000000;
	private static final int COLOR_TITLE = 0xFFFFD75E;
	private static final int COLOR_TEXT = 0xFFFFFFFF;
	private static final int COLOR_MUTED = 0xFFAAAAAA;
	private static final int COLOR_COUNT = 0xFF7CFC7C;

	private EntityCounterHud() {
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!EntityCounterClient.isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		// Das F3-Menü liegt an derselben Stelle – dann Platz machen.
		if (client.options.hideGui || client.gui.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Font font = client.font;
		EntityScan scan = EntityCounterClient.getScan();
		BlockPos pos = scan.pos();

		Component title = Component.translatable("entitycounter.hud.title");
		Component subtitle = pos == null
				? Component.translatable("entitycounter.hud.no_target")
				: Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());

		List<Component> rows = new ArrayList<>();
		for (EntityScan.Entry entry : scan.entries()) {
			rows.add(Component.empty()
					.append(entry.type().getDescription())
					.append(Component.literal(": "))
					.append(Component.literal(Integer.toString(entry.count())).withColor(COLOR_COUNT)));
		}

		boolean showEmpty = pos != null && rows.isEmpty();
		boolean showTotal = rows.size() > 1;
		Component empty = Component.translatable("entitycounter.hud.empty");
		Component total = Component.translatable("entitycounter.hud.total", scan.total());

		int width = Math.max(font.width(title), font.width(subtitle));
		for (Component row : rows) {
			width = Math.max(width, ICON_SIZE + 4 + font.width(row));
		}
		if (showEmpty) {
			width = Math.max(width, font.width(empty));
		}
		if (showTotal) {
			width = Math.max(width, font.width(total));
		}

		int height = LINE_HEIGHT * 2 + rows.size() * ROW_HEIGHT
				+ (showEmpty ? LINE_HEIGHT : 0) + (showTotal ? LINE_HEIGHT + 2 : 0);
		if (!rows.isEmpty()) {
			height += 2;
		}

		int x = MARGIN + PADDING;
		int y = MARGIN + PADDING;
		graphics.fill(MARGIN, MARGIN, x + width + PADDING, y + height + PADDING - 2, COLOR_BACKGROUND);

		graphics.drawString(font, title, x, y, COLOR_TITLE, true);
		y += LINE_HEIGHT;
		graphics.drawString(font, subtitle, x, y, COLOR_MUTED, true);
		y += LINE_HEIGHT;

		if (showEmpty) {
			graphics.drawString(font, empty, x, y, COLOR_MUTED, true);
			return;
		}
		if (rows.isEmpty()) {
			return;
		}

		y += 2;
		for (int i = 0; i < rows.size(); i++) {
			renderIcon(graphics, client, scan.entries().get(i).sample(), x, y);
			graphics.drawString(font, rows.get(i), x + ICON_SIZE + 4, y + (ICON_SIZE - 8) / 2, COLOR_TEXT, true);
			y += ROW_HEIGHT;
		}

		if (showTotal) {
			y += 2;
			graphics.drawString(font, total, x, y, COLOR_MUTED, true);
		}
	}

	/** Zeichnet das Bild eines Entities: bei Items das Item selbst, sonst das 3D-Modell wie im Inventar. */
	private static void renderIcon(GuiGraphics graphics, Minecraft client, Entity entity, int x, int y) {
		if (entity instanceof ItemEntity itemEntity) {
			graphics.renderItem(itemEntity.getItem(), x, y);
			return;
		}

		EntityRenderState state;
		try {
			state = client.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1.0F);
		} catch (RuntimeException e) {
			renderSpawnEgg(graphics, entity, x, y);
			return;
		}

		state.lightCoords = LightTexture.FULL_BRIGHT;
		state.shadowPieces.clear();
		state.outlineColor = EntityRenderState.NO_OUTLINE;
		state.nameTag = null;
		state.displayFireAnimation = false;
		state.isInvisible = false;

		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F + 30.0F;
			living.yRot = 0.0F;
			living.xRot = 0.0F;
			if (living.scale != 0.0F) {
				living.boundingBoxWidth /= living.scale;
				living.boundingBoxHeight /= living.scale;
			}
			living.scale = 1.0F;
		}

		float size = Math.max(Math.max(state.boundingBoxWidth, state.boundingBoxHeight), 0.3F);
		float scale = (ICON_SIZE - 3) / size;

		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cameraTilt = new Quaternionf().rotateX((float) Math.toRadians(-10.0));
		rotation.mul(cameraTilt);
		Vector3f translation = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);

		graphics.submitEntityRenderState(state, scale, translation, rotation, cameraTilt,
				x, y, x + ICON_SIZE, y + ICON_SIZE);
	}

	private static void renderSpawnEgg(GuiGraphics graphics, Entity entity, int x, int y) {
		SpawnEggItem egg = SpawnEggItem.byId(entity.getType());
		if (egg != null) {
			graphics.renderItem(new ItemStack(egg), x, y);
		}
	}
}
