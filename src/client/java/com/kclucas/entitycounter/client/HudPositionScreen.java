package com.kclucas.entitycounter.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hier zieht man die Anzeige mit der Maus an ihren Platz. Der Hintergrund ist abgedunkelt und die Anzeige
 * zeigt ein Beispiel mit drei Mobs, damit man ihre ungefähre Größe sieht.
 */
public class HudPositionScreen extends Screen {

	private static final int MARGIN = 4;
	private static final int COLOR_DIM = 0x70000000;
	private static final int COLOR_OUTLINE = 0xFF3CFF5A;
	private static final int COLOR_OUTLINE_IDLE = 0xA0FFFFFF;
	private static final int COLOR_HINT = 0xFFFFFFFF;
	private static final int COLOR_HINT_MUTED = 0xFFB8BEC6;

	private final @Nullable Screen parent;
	private final List<EntityCounterHud.Row> sampleRows = new ArrayList<>();
	private EntityCounterHud.@Nullable Bounds bounds;
	private boolean dragging;
	private double grabX;
	private double grabY;

	public HudPositionScreen(@Nullable Screen parent) {
		super(Component.translatable("entitycounter.position.title"));
		this.parent = parent;

		addSample(EntityTypes.ARMADILLO, Items.ARMADILLO_SPAWN_EGG, 15);
		addSample(EntityTypes.CHICKEN, Items.CHICKEN_SPAWN_EGG, 3);
		addSample(EntityTypes.SLIME, Items.SLIME_SPAWN_EGG, 1);
	}

	/** In einer Welt wird das echte Mob-Modell gezeigt, im Hauptmenü ersatzweise das Spawn-Ei. */
	private void addSample(EntityType<?> type, Item fallbackIcon, int count) {
		Entity entity = null;
		Minecraft client = Minecraft.getInstance();
		if (client.level != null) {
			try {
				entity = type.create(client.level, EntitySpawnReason.LOAD);
			} catch (RuntimeException e) {
				entity = null;
			}
		}
		this.sampleRows.add(new EntityCounterHud.Row(entity, new ItemStack(fallbackIcon), type.getDescription(), count));
	}

	@Override
	protected void init() {
		int y = this.height - 28;
		addRenderableWidget(Button.builder(Component.translatable("entitycounter.position.reset"), button -> {
			EntityCounterConfig config = EntityCounterClient.getConfig();
			config.hudX = 0.0F;
			config.hudY = 0.0F;
		}).bounds(this.width / 2 - 154, y, 150, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
				.bounds(this.width / 2 + 4, y, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, COLOR_DIM);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 14, COLOR_HINT);
		graphics.centeredText(this.font, Component.translatable("entitycounter.position.hint"),
				this.width / 2, this.height / 2, COLOR_HINT_MUTED);

		EntityCounterConfig config = EntityCounterClient.getConfig();
		Component subtitle = config.showCoordinates ? Component.literal("12, 64, -8") : null;
		this.bounds = EntityCounterHud.renderPanel(graphics, this.minecraft, subtitle, this.sampleRows,
				true, 19, config.hudX, config.hudY);

		boolean hot = this.dragging || this.bounds.contains(mouseX, mouseY);
		graphics.outline(this.bounds.x() - 1, this.bounds.y() - 1,
				this.bounds.width() + 2, this.bounds.height() + 2, hot ? COLOR_OUTLINE : COLOR_OUTLINE_IDLE);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
				return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.bounds != null && this.bounds.contains(event.x(), event.y())) {
			this.dragging = true;
			this.grabX = event.x() - this.bounds.x();
			this.grabY = event.y() - this.bounds.y();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (this.dragging && this.bounds != null) {
			EntityCounterConfig config = EntityCounterClient.getConfig();
			config.hudX = toFraction(event.x() - this.grabX, this.width - this.bounds.width());
			config.hudY = toFraction(event.y() - this.grabY, this.height - this.bounds.height());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (this.dragging && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			this.dragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	/** Rechnet eine Pixel-Position in den Anteil des freien Platzes um (siehe {@link EntityCounterConfig#hudX}). */
	private static float toFraction(double position, int space) {
		int free = space - MARGIN * 2;
		if (free <= 0) {
			return 0.0F;
		}
		return Mth.clamp((float) ((position - MARGIN) / free), 0.0F, 1.0F);
	}

	@Override
	public void onClose() {
		EntityCounterClient.getConfig().save();
		this.minecraft.gui.setScreen(this.parent);
	}
}
