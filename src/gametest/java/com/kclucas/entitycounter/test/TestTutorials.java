package com.kclucas.entitycounter.test;

import com.kclucas.entitycounter.client.onboarding.Onboarding;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Meldet im Testlauf zusätzlich das Tutorial von Pack Toggle an, so als wären beide Mods installiert.
 * Damit lässt sich prüfen, dass sich mehrere Mods zu einem gemeinsamen Screen zusammenschließen.
 */
public class TestTutorials implements ClientModInitializer {

	private static final String NAMESPACE = "ectest";

	@Override
	public void onInitializeClient() {
		Onboarding.register(new Onboarding.Tutorial("packtoggle",
				Component.literal("Pack Toggle"),
				Component.literal("Switches all your resource packs off and back on in the right order."),
				Identifier.fromNamespaceAndPath(NAMESPACE, "icon.png"),
				1,
				List.of(
						new Onboarding.Page(
								Component.literal("Save your pack order"),
								Component.literal("Open Options > Resource Packs and click \"Save packs\". "
										+ "Pack Toggle remembers your active packs and their order."),
								Identifier.fromNamespaceAndPath(NAMESPACE, "textures/tutorial/save.png")),
						new Onboarding.Page(
								Component.literal("Take them out, put them back"),
								Component.literal("\"Remove packs\" leaves only the default look. \"Add packs\" "
										+ "brings everything back in the saved order. Click Done to apply."),
								Identifier.fromNamespaceAndPath(NAMESPACE, "textures/tutorial/switch.png")))));
	}
}
