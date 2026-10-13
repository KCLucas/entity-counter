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
import org.jspecify.annotations.Nullable;

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

	/** Eine Zeile der Anzeige. Ohne {@code entity} wird stattdessen {@code icon} als Bild gezeichnet. */
	public record Row(@Nullable Entity entity, ItemStack icon, Component name, int count) {
	}

	/** Wo die Anzeige zuletzt gezeichnet wurde, in GUI-Pixeln. */
	public record Bounds(int x, int y, int width, int height) {
		public boolean contains(double px, double py) {
			return px >= this.x && px < this.x + this.width && py >= this.y && py < this.y + this.height;
		}
	}

	public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!EntityCounterClient.isEnabled()) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (isHidden(client)) {
			return;
		}

		EntityCounterConfig config = EntityCounterClient.getConfig();
		EntityScan scan = EntityCounterClient.getScan();
		BlockPos pos = scan.pos();

		// Ohne Ziel bleibt die Zeile als Hinweis stehen, sonst zeigt sie die Koordinaten (abschaltbar).
		Component subtitle = null;
		if (pos == null) {
			subtitle = Component.translatable("entitycounter.hud.no_target");
		} else if (config.showCoordinates) {
			subtitle = Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
		}

		List<Row> rows = new ArrayList<>();
		for (EntityScan.Entry entry : scan.entries()) {
			rows.add(new Row(entry.sample(), ItemStack.EMPTY, entry.name(), entry.count()));
		}

		renderPanel(graphics, client, subtitle, rows, pos != null, scan.total(), config.hudX, config.hudY);
	}

	/**
	 * Ausgeblendet wird die Anzeige bei F1, beim Verschieben (der Screen zeigt dort selbst ein Beispiel) und wenn
	 * das volle F3-Menü offen ist. Einzelne Debug-Einträge, die über F3+F6 dauerhaft eingeblendet sind
	 * (z. B. Chunk-Grenzen oder "Looking at"), zählen bewusst nicht dazu – sonst wäre die Anzeige für alle,
	 * die so etwas immer anhaben, nie zu sehen.
	 */
	public static boolean isHidden(Minecraft client) {
		return client.options.hideGui || client.debugEntries.isOverlayVisible()
				|| client.screen instanceof HudPositionScreen;
	}

	/**
	 * Zeichnet die Anzeige an der Position {@code fractionX}/{@code fractionY} (Anteil des freien Platzes, 0 bis 1)
	 * und gibt zurück, wo sie gelandet ist.
	 */
	public static Bounds renderPanel(GuiGraphics graphics, Minecraft client, @Nullable Component subtitle,
			List<Row> rows, boolean hasTarget, int totalCount, float fractionX, float fractionY) {
		Font font = client.font;
		Component title = Component.translatable("entitycounter.hud.title");

		List<Component> texts = new ArrayList<>();
		for (Row row : rows) {
			texts.add(Component.empty()
					.append(row.name())
					.append(Component.literal(": "))
					.append(Component.literal(Integer.toString(row.count())).withColor(COLOR_COUNT)));
		}

		boolean showEmpty = hasTarget && rows.isEmpty();
		boolean showTotal = rows.size() > 1;
		Component empty = Component.translatable("entitycounter.hud.empty");
		Component total = Component.translatable("entitycounter.hud.total", totalCount);

		int width = Math.max(font.width(title), subtitle != null ? font.width(subtitle) : 0);
		for (Component text : texts) {
			width = Math.max(width, ICON_SIZE + 4 + font.width(text));
		}
		if (showEmpty) {
			width = Math.max(width, font.width(empty));
		}
		if (showTotal) {
			width = Math.max(width, font.width(total));
		}

		int height = LINE_HEIGHT * (subtitle != null ? 2 : 1) + rows.size() * ROW_HEIGHT
				+ (showEmpty ? LINE_HEIGHT : 0) + (showTotal ? LINE_HEIGHT + 2 : 0);
		if (!rows.isEmpty()) {
			height += 2;
		}

		int boxWidth = width + PADDING * 2;
		int boxHeight = height + PADDING * 2 - 2;
		int left = MARGIN + Math.round(fractionX * Math.max(0, graphics.guiWidth() - boxWidth - MARGIN * 2));
		int top = MARGIN + Math.round(fractionY * Math.max(0, graphics.guiHeight() - boxHeight - MARGIN * 2));
		Bounds bounds = new Bounds(left, top, boxWidth, boxHeight);

		graphics.fill(left, top, left + boxWidth, top + boxHeight, COLOR_BACKGROUND);

		int x = left + PADDING;
		int y = top + PADDING;
		graphics.drawString(font, title, x, y, COLOR_TITLE, true);
		y += LINE_HEIGHT;
		if (subtitle != null) {
			graphics.drawString(font, subtitle, x, y, COLOR_MUTED, true);
			y += LINE_HEIGHT;
		}

		if (showEmpty) {
			graphics.drawString(font, empty, x, y, COLOR_MUTED, true);
			return bounds;
		}
		if (rows.isEmpty()) {
			return bounds;
		}

		y += 2;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			if (row.entity() != null) {
				renderIcon(graphics, client, row.entity(), x, y);
			} else {
				graphics.renderItem(row.icon(), x, y);
			}
			graphics.drawString(font, texts.get(i), x + ICON_SIZE + 4, y + (ICON_SIZE - 8) / 2, COLOR_TEXT, true);
			y += ROW_HEIGHT;
		}

		if (showTotal) {
			y += 2;
			graphics.drawString(font, total, x, y, COLOR_MUTED, true);
		}
		return bounds;
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
