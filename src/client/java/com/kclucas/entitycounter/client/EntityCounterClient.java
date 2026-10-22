package com.kclucas.entitycounter.client;

import com.kclucas.entitycounter.client.onboarding.Onboarding;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class EntityCounterClient implements ClientModInitializer {

	public static final String MOD_ID = "entitycounter";

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static KeyMapping toggleKey;
	private static KeyMapping settingsKey;
	private static EntityCounterConfig config = new EntityCounterConfig();
	private static boolean enabled = false;
	private static EntityScan scan = EntityScan.NONE;

	public static boolean isEnabled() {
		return enabled;
	}

	public static EntityScan getScan() {
		return scan;
	}

	public static EntityCounterConfig getConfig() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		config = EntityCounterConfig.load();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.entitycounter.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));
		settingsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.entitycounter.settings", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"), EntityCounterHud::render);

		WorldRenderEvents.BEFORE_TRANSLUCENT.register(EntityCounterClient::highlightTarget);

		Onboarding.register(createTutorial());
	}

	/** Die kurze Tour beim ersten Start in einem Profil, dazu was in welcher Version neu ist (neueste zuerst). */
	private static Onboarding.Tutorial createTutorial() {
		Component toggleKeyName = Component.keybind("key.entitycounter.toggle").withStyle(ChatFormatting.YELLOW);
		return new Onboarding.Tutorial(MOD_ID,
				Component.literal("Entity Counter"),
				Component.translatable("entitycounter.tutorial.description"),
				Identifier.fromNamespaceAndPath(MOD_ID, "icon.png"),
				List.of(
						new Onboarding.Page(
								Component.translatable("entitycounter.tutorial.count.title"),
								Component.translatable("entitycounter.tutorial.count.text", toggleKeyName),
								Identifier.fromNamespaceAndPath(MOD_ID, "textures/tutorial/count.png")),
						new Onboarding.Page(
								Component.translatable("entitycounter.tutorial.modes.title"),
								Component.translatable("entitycounter.tutorial.modes.text"),
								Identifier.fromNamespaceAndPath(MOD_ID, "textures/tutorial/modes.png")),
						new Onboarding.Page(
								Component.translatable("entitycounter.tutorial.settings.title"),
								Component.translatable("entitycounter.tutorial.settings.text"),
								Identifier.fromNamespaceAndPath(MOD_ID, "textures/tutorial/settings.png"))),
				List.of(
						new Onboarding.Release("1.4.1", List.of(
								Component.translatable("entitycounter.news.1_4_1.items"),
								Component.translatable("entitycounter.news.1_4_1.vehicle"),
								Component.translatable("entitycounter.news.1_4_1.position"))),
						new Onboarding.Release("1.4.0", List.of(
								Component.translatable("entitycounter.news.1_4_0.folder"),
								Component.translatable("entitycounter.news.1_4_0.news"))),
						new Onboarding.Release("1.3.1", List.of(
								Component.translatable("entitycounter.news.1_3_1.debug"),
								Component.translatable("entitycounter.news.1_3_1.menu")))));
	}

	private void onClientTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			if (client.player != null) {
				client.player.displayClientMessage(Component.translatable(
						enabled ? "entitycounter.toggle.on" : "entitycounter.toggle.off"), true);
			}
		}
		while (settingsKey.consumeClick()) {
			if (client.screen == null) {
				client.setScreen(new EntityCounterConfigScreen(null));
			}
		}

		if (!enabled || client.player == null || client.level == null) {
			scan = EntityScan.NONE;
			return;
		}

		scan = EntityScan.compute(client, config.targetMode);
	}

	/** Markiert den gerade gezählten Block in der Welt. */
	private static void highlightTarget(WorldRenderContext context) {
		BlockPos pos = scan.pos();
		if (!enabled || !config.highlightBlock || pos == null) {
			return;
		}
		BlockHighlightRenderer.render(context, pos);
	}
}
