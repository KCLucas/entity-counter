package com.kclucas.entitycounter.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class EntityCounterClient implements ClientModInitializer {

	public static final String MOD_ID = "entitycounter";

	private static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	private static KeyMapping toggleKey;
	private static boolean enabled = false;
	private static EntityScan scan = EntityScan.NONE;

	public static boolean isEnabled() {
		return enabled;
	}

	public static EntityScan getScan() {
		return scan;
	}

	@Override
	public void onInitializeClient() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.entitycounter.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"), EntityCounterHud::render);
	}

	private void onClientTick(Minecraft client) {
		while (toggleKey.consumeClick()) {
			enabled = !enabled;
			if (client.player != null) {
				client.player.displayClientMessage(Component.translatable(
						enabled ? "entitycounter.toggle.on" : "entitycounter.toggle.off"), true);
			}
		}

		if (!enabled || client.player == null || client.level == null) {
			scan = EntityScan.NONE;
			return;
		}

		scan = EntityScan.compute(client);
	}
}
