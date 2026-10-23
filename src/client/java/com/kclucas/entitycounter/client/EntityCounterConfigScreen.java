package com.kclucas.entitycounter.client;

import com.kclucas.entitycounter.client.onboarding.Onboarding;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Einstellungen als Liste: pro Zeile links Name und Erklärung, rechts der Schalter und ein Zurücksetzen-Knopf,
 * gegliedert durch Abschnitts-Überschriften.
 */
public class EntityCounterConfigScreen extends Screen {

	private static final int CONTROL_WIDTH = 120;
	private static final int RESET_WIDTH = 20;
	private static final int GAP = 4;
	private static final int OPTION_HEIGHT = 38;
	private static final int SECTION_HEIGHT = 26;

	private static final int COLOR_ACCENT = 0xFF3CFF5A;
	private static final int COLOR_LABEL = 0xFFFFFFFF;
	private static final int COLOR_DESCRIPTION = 0xFF9AA0A6;
	private static final int COLOR_ROW_HOVER = 0x16FFFFFF;

	private static final Component ON = CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN);
	private static final Component OFF = CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED);

	private final @Nullable Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final List<Runnable> resetters = new ArrayList<>();
	private @Nullable OptionList list;

	public EntityCounterConfigScreen(@Nullable Screen parent) {
		super(Component.translatable("entitycounter.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		EntityCounterConfig config = EntityCounterClient.getConfig();
		EntityCounterConfig defaults = new EntityCounterConfig();
		this.resetters.clear();

		this.layout.addTitleHeader(this.title, this.font);

		OptionList list = new OptionList(this.minecraft, this.width, this.layout);
		list.add(new SectionRow(Component.translatable("entitycounter.config.section.target")));
		list.add(option("entitycounter.config.target_mode",
				TargetMode::description,
				CycleButton.<TargetMode>builder(TargetMode::label, config.targetMode).withValues(TargetMode.values()),
				defaults.targetMode, value -> config.targetMode = value));

		list.add(new SectionRow(Component.translatable("entitycounter.config.section.display")));
		list.add(toggle("entitycounter.config.highlight",
				config.highlightBlock, defaults.highlightBlock, value -> config.highlightBlock = value));
		list.add(toggle("entitycounter.config.coordinates",
				config.showCoordinates, defaults.showCoordinates, value -> config.showCoordinates = value));
		list.add(new PositionRow());

		list.add(new SectionRow(Component.translatable("entitycounter.config.section.help")));
		list.add(new TutorialRow());
		this.list = this.layout.addToContents(list);

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		footer.addChild(Button.builder(Component.translatable("entitycounter.config.reset_all"),
				button -> this.resetters.forEach(Runnable::run)).width(150).build());
		footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(150).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	private OptionRow<Boolean> toggle(String key, boolean value, boolean defaultValue, Consumer<Boolean> setter) {
		Component description = Component.translatable(key + ".desc");
		return option(key, v -> description, CycleButton.booleanBuilder(ON, OFF, value), defaultValue, setter);
	}

	private <T> OptionRow<T> option(String key, Function<T, Component> description,
			CycleButton.Builder<T> builder, T defaultValue, Consumer<T> setter) {
		Component label = Component.translatable(key);
		OptionRow<T> row = new OptionRow<>(label, description, defaultValue, setter);
		row.control = builder.displayOnlyValue().create(0, 0, CONTROL_WIDTH, 20, label, (button, value) -> row.apply(value));
		row.refresh();
		this.resetters.add(row::reset);
		return row;
	}

	@Override
	protected void repositionElements() {
		this.layout.arrangeElements();
		if (this.list != null) {
			this.list.updateSize(this.width, this.layout);
		}
	}

	@Override
	public void onClose() {
		EntityCounterClient.getConfig().save();
		this.minecraft.gui.setScreen(this.parent);
	}

	private class OptionList extends ContainerObjectSelectionList<Row> {

		OptionList(Minecraft minecraft, int width, HeaderAndFooterLayout layout) {
			super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), OPTION_HEIGHT);
		}

		void add(Row row) {
			addEntry(row, row.height());
		}

		@Override
		public int getRowWidth() {
			return Math.min(this.width - 40, 440);
		}

		@Override
		protected int scrollBarX() {
			return getRowRight() + 8;
		}
	}

	/** Kreispfeil als Pixelgrafik – als Schriftzeichen wäre er im Spiel kaum zu erkennen. */
	private static final String[] RESET_ICON = {
			"..###.#",
			".#...##",
			"#...###",
			"#......",
			"#.....#",
			".#...#.",
			"..###..",
	};

	private static void drawResetIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int row = 0; row < RESET_ICON.length; row++) {
			for (int col = 0; col < RESET_ICON[row].length(); col++) {
				if (RESET_ICON[row].charAt(col) == '#') {
					graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
				}
			}
		}
	}

	private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
		abstract int height();
	}

	/** Überschrift eines Abschnitts: Titel in der Akzentfarbe, links und rechts eine auslaufende Linie. */
	private class SectionRow extends Row {
		private final Component title;

		SectionRow(Component title) {
			this.title = title.copy().withStyle(ChatFormatting.BOLD);
		}

		@Override
		int height() {
			return SECTION_HEIGHT;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = EntityCounterConfigScreen.this.font;
			int textWidth = font.width(this.title);
			int centerX = getContentXMiddle();
			int textY = getContentBottom() - 12;
			graphics.text(font, this.title, centerX - textWidth / 2, textY, COLOR_ACCENT, true);

			int lineY = textY + 4;
			int inner = textWidth / 2 + 8;
			fadeLine(graphics, getContentX(), centerX - inner, lineY, true);
			fadeLine(graphics, centerX + inner, getContentRight(), lineY, false);
		}

		/** Waagerechte Linie, die zum Rand hin ausblendet. */
		private static void fadeLine(GuiGraphicsExtractor graphics, int from, int to, int y, boolean fadeIn) {
			int steps = 12;
			float span = (to - from) / (float) steps;
			for (int i = 0; i < steps; i++) {
				float strength = fadeIn ? (i + 1) / (float) steps : (steps - i) / (float) steps;
				int alpha = (int) (strength * 0xC0);
				int x1 = from + Math.round(span * i);
				int x2 = from + Math.round(span * (i + 1));
				graphics.fill(x1, y, x2, y + 1, (alpha << 24) | (COLOR_ACCENT & 0xFFFFFF));
			}
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of();
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of();
		}
	}

	/** Gemeinsames Aussehen aller Einstellungs-Zeilen: Text links, Bedienelement und Zurücksetzen-Knopf rechts. */
	private void renderOption(Row row, GuiGraphicsExtractor graphics, Component label, Component description,
			AbstractWidget control, @Nullable Button resetButton, int mouseX, int mouseY, boolean hovered,
			float partialTick) {
		if (hovered) {
			graphics.fill(row.getContentX() - 4, row.getContentY() - 1,
					row.getContentRight() + 4, row.getContentBottom() + 1, COLOR_ROW_HOVER);
		}

		// Auch Zeilen ohne Zurücksetzen-Knopf halten dessen Platz frei, damit alle Bedienelemente in einer Spalte stehen.
		int buttonY = row.getContentY() + (row.getContentHeight() - 20) / 2;
		int resetX = row.getContentRight() - RESET_WIDTH;
		control.setX(resetX - GAP - CONTROL_WIDTH);
		control.setY(buttonY);
		if (resetButton != null) {
			resetButton.setX(resetX);
			resetButton.setY(buttonY);
		}

		int textWidth = control.getX() - 10 - row.getContentX();
		List<FormattedCharSequence> lines = this.font.split(description, textWidth);
		int shown = Math.min(lines.size(), 2);
		int textY = row.getContentY() + (row.getContentHeight() - (11 + shown * 9)) / 2;
		graphics.text(this.font, label, row.getContentX(), textY, COLOR_LABEL, true);
		for (int i = 0; i < shown; i++) {
			graphics.text(this.font, lines.get(i), row.getContentX(), textY + 11 + i * 9, COLOR_DESCRIPTION, false);
		}

		control.extractRenderState(graphics, mouseX, mouseY, partialTick);
		if (resetButton != null) {
			resetButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
			drawResetIcon(graphics, resetButton.getX() + (RESET_WIDTH - 7) / 2, buttonY + 6,
					resetButton.active ? COLOR_LABEL : 0xFF6B6F75);
		}
	}

	private Button createResetButton(Runnable action) {
		return Button.builder(Component.empty(), button -> action.run())
				.size(RESET_WIDTH, 20)
				.tooltip(Tooltip.create(Component.translatable("entitycounter.config.reset")))
				.build();
	}

	/** Zeigt die kurze Tour durch die Mod noch einmal. */
	private class TutorialRow extends Row {
		private final Component label = Component.translatable("entitycounter.config.tutorial");
		private final Component description = Component.translatable("entitycounter.config.tutorial.desc");
		private final Button showButton = Button.builder(Component.translatable("entitycounter.config.tutorial.show"),
				button -> Onboarding.open(EntityCounterClient.MOD_ID)).size(CONTROL_WIDTH, 20).build();

		@Override
		int height() {
			return OPTION_HEIGHT;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			renderOption(this, graphics, this.label, this.description, this.showButton, null,
					mouseX, mouseY, hovered, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.showButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.showButton);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			consumer.accept(this.showButton);
		}
	}

	/** Öffnet den Screen, in dem man die Anzeige mit der Maus an ihren Platz zieht. */
	private class PositionRow extends Row {
		private final Component label = Component.translatable("entitycounter.config.position");
		private final Component description = Component.translatable("entitycounter.config.position.desc");
		private final Button editButton;
		private final Button resetButton;

		PositionRow() {
			this.editButton = Button.builder(Component.translatable("entitycounter.config.position.edit"),
					button -> EntityCounterConfigScreen.this.minecraft.gui.setScreen(
							new HudPositionScreen(EntityCounterConfigScreen.this)))
					.size(CONTROL_WIDTH, 20).build();
			this.resetButton = createResetButton(this::reset);
			EntityCounterConfigScreen.this.resetters.add(this::reset);
		}

		private void reset() {
			EntityCounterConfig config = EntityCounterClient.getConfig();
			config.hudX = 0.0F;
			config.hudY = 0.0F;
		}

		@Override
		int height() {
			return OPTION_HEIGHT;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			EntityCounterConfig config = EntityCounterClient.getConfig();
			this.resetButton.active = config.hudX != 0.0F || config.hudY != 0.0F;
			renderOption(this, graphics, this.label, this.description,
					this.editButton, this.resetButton, mouseX, mouseY, hovered, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.editButton, this.resetButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.editButton, this.resetButton);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			consumer.accept(this.editButton);
			consumer.accept(this.resetButton);
		}
	}

	/** Eine Einstellung: Name und Erklärung links, Schalter und Zurücksetzen-Knopf rechts. */
	private class OptionRow<T> extends Row {
		private final Component label;
		private final Function<T, Component> description;
		private final T defaultValue;
		private final Consumer<T> setter;
		private final Button resetButton;
		private CycleButton<T> control;

		OptionRow(Component label, Function<T, Component> description, T defaultValue, Consumer<T> setter) {
			this.label = label;
			this.description = description;
			this.defaultValue = defaultValue;
			this.setter = setter;
			this.resetButton = createResetButton(this::reset);
		}

		void apply(T value) {
			this.setter.accept(value);
			refresh();
		}

		void reset() {
			this.control.setValue(this.defaultValue);
			apply(this.defaultValue);
		}

		/** Der Zurücksetzen-Knopf ist nur aktiv, wenn es etwas zurückzusetzen gibt. */
		void refresh() {
			this.resetButton.active = !Objects.equals(this.control.getValue(), this.defaultValue);
		}

		@Override
		int height() {
			return OPTION_HEIGHT;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			renderOption(this, graphics, this.label, this.description.apply(this.control.getValue()),
					this.control, this.resetButton, mouseX, mouseY, hovered, partialTick);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(this.control, this.resetButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(this.control, this.resetButton);
		}

		@Override
		public void visitWidgets(Consumer<AbstractWidget> consumer) {
			consumer.accept(this.control);
			consumer.accept(this.resetButton);
		}
	}
}
