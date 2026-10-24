package com.kclucas.entitycounter.test;

import com.kclucas.entitycounter.client.EntityCounterClient;
import com.kclucas.entitycounter.client.EntityCounterConfig;
import com.kclucas.entitycounter.client.EntityCounterConfigScreen;
import com.kclucas.entitycounter.client.EntityCounterHud;
import com.kclucas.entitycounter.client.EntityScan;
import com.kclucas.entitycounter.client.HudPositionScreen;
import com.kclucas.entitycounter.client.TargetMode;
import com.kclucas.entitycounter.client.onboarding.Onboarding;
import com.kclucas.entitycounter.client.onboarding.OnboardingScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Baut eine kleine Kammer hinter Glas, setzt Entities in genau einen Block und prüft,
 * dass der Entity Counter sie richtig zählt. Screenshots landen im Run-Ordner.
 */
public class EntityCounterClientTest implements FabricClientGameTest {

	private static final BlockPos CHAMBER = new BlockPos(0, 201, 5);
	private static final String AT = "0.5 201 5.5";

	@Override
	public void runTest(ClientGameTestContext context) {
		// Test-Client stumm: Lucas streamt oft nebenbei, Töne aus dem Testlauf sollen nicht durchkommen.
		context.runOnClient(c -> c.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0));
		context.getInput().resizeWindow(2560, 1440);
		context.runOnClient(c -> {
			c.options.guiScale().set(3);
			c.resizeDisplay();
		});
		testOnboarding(context);

		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerContext server = world.getServer();

			server.runCommand("gamemode creative @a");
			server.runCommand("time set noon");
			// WQHD-Auflösung, damit die Screenshots auch für die Mod-Seite taugen.
			context.getInput().resizeWindow(2560, 1440);
			context.runOnClient(c -> {
				c.options.fov().set(90);
				c.options.guiScale().set(3);
				c.resizeDisplay();
			});
			server.runCommand("fill -7 200 -4 7 200 9 minecraft:smooth_stone");
			server.runCommand("fill -7 200 7 7 205 9 minecraft:deepslate_bricks");
			server.runCommand("fill -7 201 7 7 201 7 minecraft:polished_deepslate");
			server.runCommand("fill -1 200 4 1 200 6 minecraft:polished_deepslate");
			server.runCommand("fill -1 201 4 1 202 6 minecraft:glass");
			server.runCommand("fill -1 203 4 1 203 6 minecraft:smooth_stone_slab");
			server.runCommand("setblock 0 201 5 minecraft:air");
			server.runCommand("setblock 0 202 5 minecraft:air");
			server.runCommand("setblock -3 201 6 minecraft:lantern");
			server.runCommand("setblock 3 201 6 minecraft:lantern");
			server.runCommand("tp @a 0.5 201 1.5 0 19");
			for (int i = 0; i < 15; i++) {
				server.runCommand("summon minecraft:armadillo " + AT + " {NoAI:1b,PersistenceRequired:1b}");
			}
			context.waitTicks(40);
			world.getClientWorld().waitForChunksRender();

			if (context.computeOnClient(c -> EntityCounterClient.isEnabled())) {
				throw new AssertionError("Counter sollte beim Start aus sein");
			}
			context.takeScreenshot("entitycounter-0-aus");

			context.getInput().pressKey(GLFW.GLFW_KEY_F7);
			context.waitTicks(10);
			if (!context.computeOnClient(c -> EntityCounterClient.isEnabled())) {
				throw new AssertionError("F7 hat den Counter nicht eingeschaltet");
			}

			EntityScan scan = context.computeOnClient(c -> EntityCounterClient.getScan());
			expect(scan, 15, 1);
			if (scan.entries().get(0).type() != EntityType.ARMADILLO || scan.entries().get(0).count() != 15) {
				throw new AssertionError("Erwartet 15 Armadillos, bekommen: " + scan);
			}
			calm(context);
			context.takeScreenshot("entitycounter-1-armadillos");

			// Dauerhaft eingeblendete Debug-Einträge (F3+F6) dürfen die Anzeige nicht verstecken, das volle F3-Menü schon.
			context.runOnClient(c -> {
				c.debugEntries.setStatus(DebugScreenEntries.CHUNK_BORDERS, DebugScreenEntryStatus.ALWAYS_ON);
				c.debugEntries.setStatus(DebugScreenEntries.LOOKING_AT_ENTITY, DebugScreenEntryStatus.ALWAYS_ON);
			});
			context.waitTicks(10);
			if (!context.computeOnClient(c -> c.gui.getDebugOverlay().showDebugScreen())
					|| context.computeOnClient(EntityCounterHud::isHidden)) {
				throw new AssertionError("Mit dauerhaft eingeblendeten Debug-Einträgen muss die Anzeige sichtbar bleiben");
			}
			context.takeScreenshot("entitycounter-1a-debug-eintraege-an");
			context.runOnClient(c -> c.debugEntries.setOverlayVisible(true));
			context.waitTicks(5);
			if (!context.computeOnClient(EntityCounterHud::isHidden)) {
				throw new AssertionError("Bei offenem F3-Menü sollte die Anzeige ausgeblendet sein");
			}
			context.runOnClient(c -> {
				c.debugEntries.setOverlayVisible(false);
				c.debugEntries.setStatus(DebugScreenEntries.CHUNK_BORDERS, DebugScreenEntryStatus.NEVER);
				c.debugEntries.setStatus(DebugScreenEntries.LOOKING_AT_ENTITY, DebugScreenEntryStatus.IN_OVERLAY);
			});
			context.waitTicks(5);

			// Die festen Zielmodi: der Spieler schaut auf den Glasblock direkt vor der Kammer.
			expectMode(context, TargetMode.LOOKED_AT, CHAMBER.north(), 0);
			expectMode(context, TargetMode.IN_FRONT, CHAMBER.north(2), 0);
			context.takeScreenshot("entitycounter-1b-block-davor");
			expectMode(context, TargetMode.BEHIND, CHAMBER, 15);
			expectMode(context, TargetMode.AUTO, CHAMBER, 15);

			// Mod Menu muss den Einstellungs-Screen der Mod finden.
			boolean registered = FabricLoader.getInstance().isModLoaded("modmenu")
					&& FabricLoader.getInstance().getEntrypoints("modmenu", Object.class).stream()
							.anyMatch(e -> e.getClass().getName().endsWith("entitycounter.client.ModMenuIntegration"));
			if (!registered) {
				throw new AssertionError("Mod-Menu-Einstiegspunkt wurde nicht geladen");
			}
			context.setScreen(() -> new EntityCounterConfigScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("entitycounter-1c-einstellungen");
			context.runOnClient(c -> EntityCounterClient.getConfig().targetMode = TargetMode.BEHIND);
			context.setScreen(() -> new EntityCounterConfigScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("entitycounter-1d-einstellungen-geaendert");
			context.runOnClient(c -> EntityCounterClient.getConfig().targetMode = TargetMode.AUTO);
			context.runOnClient(c -> c.setScreen(null));
			context.waitTicks(5);

			for (int i = 0; i < 3; i++) {
				server.runCommand("summon minecraft:chicken " + AT + " {NoAI:1b}");
			}
			server.runCommand("summon minecraft:slime " + AT + " {NoAI:1b,Size:0}");
			server.runCommand("summon minecraft:minecart " + AT + " {NoGravity:1b}");
			server.runCommand("summon minecraft:item " + AT
					+ " {Item:{id:\"minecraft:diamond\",count:5},PickupDelay:32767,NoGravity:1b}");
			// Zwei verschiedene Tränke: gleiches Item, andere Trankart - müssen getrennt gezählt werden.
			for (String potion : List.of("swiftness", "healing")) {
				server.runCommand("summon minecraft:item " + AT + " {Item:{id:\"minecraft:potion\",count:1,"
						+ "components:{\"minecraft:potion_contents\":{potion:\"minecraft:" + potion + "\"}}},"
						+ "PickupDelay:32767,NoGravity:1b}");
			}
			context.waitTicks(40);

			scan = context.computeOnClient(c -> EntityCounterClient.getScan());
			// 15 Armadillos + 3 Hühner + Slime + Lore + ein Stapel aus 5 Diamanten + zwei einzelne Tränke.
			expect(scan, 27, 7);
			EntityScan.Entry diamonds = scan.entries().stream()
					.filter(e -> e.name().getString().equals("Diamond")).findFirst().orElse(null);
			if (diamonds == null || diamonds.count() != 5) {
				throw new AssertionError("Erwartet \"Diamond: 5\", bekommen: " + scan.entries());
			}
			List<String> potions = scan.entries().stream()
					.filter(e -> e.type() == EntityType.ITEM && e.count() == 1)
					.map(e -> e.name().getString()).sorted().toList();
			if (!potions.equals(List.of("Potion of Healing", "Potion of Swiftness"))) {
				throw new AssertionError("Tränke nicht getrennt gezählt: " + potions);
			}
			calm(context);
			context.takeScreenshot("entitycounter-2-gemischt");

			// Anzeige verschieben: Screen öffnen und das Beispiel mit der Maus nach rechts unten ziehen.
			context.setScreen(() -> new HudPositionScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("entitycounter-2b-verschieben-start");
			context.getInput().setCursorPos(120, 90);
			context.waitTicks(2);
			context.getInput().holdMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(2);
			context.getInput().moveCursor(1400, 700);
			context.waitTicks(2);
			context.getInput().releaseMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(5);
			float movedX = context.computeOnClient(c -> EntityCounterClient.getConfig().hudX);
			float movedY = context.computeOnClient(c -> EntityCounterClient.getConfig().hudY);
			if (movedX < 0.3F || movedY < 0.3F || movedX > 0.95F || movedY > 0.95F) {
				throw new AssertionError("Ziehen hat die Anzeige nicht wie erwartet verschoben: " + movedX + " / " + movedY);
			}
			context.takeScreenshot("entitycounter-2c-verschieben-gezogen");
			context.runOnClient(c -> c.setScreen(null));
			context.waitTicks(10);
			context.takeScreenshot("entitycounter-2d-anzeige-verschoben");
			context.runOnClient(c -> {
				EntityCounterClient.getConfig().hudX = 0.0F;
				EntityCounterClient.getConfig().hudY = 0.0F;
			});

			// Vom Block weg in den Himmel schauen: kein Ziel mehr.
			server.runCommand("tp @a 0.5 201 1.5 180 -60");
			context.waitTicks(20);
			scan = context.computeOnClient(c -> EntityCounterClient.getScan());
			if (scan.pos() != null) {
				throw new AssertionError("Im Himmel sollte kein Block anvisiert sein: " + scan);
			}
			context.takeScreenshot("entitycounter-3-kein-ziel");

			context.getInput().pressKey(GLFW.GLFW_KEY_F7);
			context.waitTicks(10);
			if (context.computeOnClient(c -> EntityCounterClient.isEnabled())) {
				throw new AssertionError("F7 hat den Counter nicht ausgeschaltet");
			}
		}

		// Von Hand verstellte Position in der Datei: wird beim Laden auf den Bildschirm begrenzt.
		try {
			Path file = FabricLoader.getInstance().getConfigDir().resolve("KCMods/entitycounter/config.json");
			Files.writeString(file, "{\"hudX\": 5.0, \"hudY\": -2.0}");
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		EntityCounterConfig loaded = EntityCounterConfig.load();
		if (loaded.hudX != 1.0F || loaded.hudY != 0.0F) {
			throw new AssertionError("Position nicht begrenzt: " + loaded.hudX + " / " + loaded.hudY);
		}
	}

	/**
	 * Erster Start im Profil: Der Willkommens-Screen erscheint von selbst im Hauptmenü und führt durch beide
	 * angemeldeten Mods (Entity Counter und das im Test nachgestellte Pack Toggle).
	 */
	private static void testOnboarding(ClientGameTestContext context) {
		context.waitForScreen(OnboardingScreen.class);
		context.waitTicks(10);
		expectStep(context, -1);
		context.takeScreenshot("onboarding-1-willkommen");

		// Mit der Pfeiltaste durch alle Seiten: 3 von Entity Counter, 2 von Pack Toggle.
		for (int step = 0; step < 5; step++) {
			context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
			context.waitTicks(5);
			expectStep(context, step);
			context.takeScreenshot("onboarding-" + (step + 2) + "-schritt");
		}

		// Zurück geht auch, über die Mod-Grenze hinweg.
		context.getInput().pressKey(GLFW.GLFW_KEY_LEFT);
		context.getInput().pressKey(GLFW.GLFW_KEY_LEFT);
		context.waitTicks(5);
		expectStep(context, 2);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(5);
		expectStep(context, 4);

		// Auf der letzten Seite schließt "Weiter" den Screen und merkt sich beide Mods als gesehen.
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(10);
		if (context.computeOnClient(c -> c.screen instanceof OnboardingScreen)) {
			throw new AssertionError("Der Tutorial-Screen sollte nach der letzten Seite geschlossen sein");
		}
		String seen = readSeen();
		if (!seen.contains("\"entitycounter\": \"" + MOD_VERSION + "\"") || !seen.contains("packtoggle")) {
			throw new AssertionError("Nicht beide Mods mit ihrer Version als gesehen vermerkt: " + seen);
		}

		// Update: Zuletzt gesehen war 1.3.0 - jetzt kommt nur "Was ist neu" mit allen Versionen seitdem, keine Tour.
		writeSeen("{\"modVersions\": {\"entitycounter\": \"1.3.0\", \"packtoggle\": \"0\"}}");
		context.runOnClient(c -> Onboarding.showPending());
		context.waitForScreen(OnboardingScreen.class);
		context.waitTicks(5);
		expectStep(context, -1);
		context.takeScreenshot("onboarding-update-1-uebersicht");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(5);
		expectStep(context, 0);
		if (!context.computeOnClient(c -> c.screen instanceof OnboardingScreen screen && screen.isNewsStep())) {
			throw new AssertionError("Nach einem Update sollte die Seite \"Was ist neu\" kommen, nicht die Tour");
		}
		context.takeScreenshot("onboarding-update-2-was-ist-neu");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(10);
		if (context.computeOnClient(c -> c.screen instanceof OnboardingScreen)) {
			throw new AssertionError("Nach den Neuigkeiten sollte der Screen geschlossen sein");
		}
		if (!readSeen().contains("\"entitycounter\": \"" + MOD_VERSION + "\"")) {
			throw new AssertionError("Neue Version nicht als gesehen vermerkt: " + readSeen());
		}

		// Gleiche Version: nichts zu zeigen.
		context.runOnClient(c -> Onboarding.showPending());
		context.waitTicks(5);
		if (context.computeOnClient(c -> c.screen instanceof OnboardingScreen)) {
			throw new AssertionError("Ohne neue Version darf nichts erscheinen");
		}

		// Umstieg von der alten Datei config/kclucas-mods.json: Version unbekannt, also nur die neuesten Änderungen.
		try {
			Files.delete(Onboarding.seenFilePath());
			Files.writeString(FabricLoader.getInstance().getConfigDir().resolve("kclucas-mods.json"),
					"{\"tutorialsSeen\": {\"entitycounter\": 1, \"packtoggle\": 1}}");
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		context.runOnClient(c -> Onboarding.showPending());
		context.waitForScreen(OnboardingScreen.class);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(5);
		if (!context.computeOnClient(c -> c.screen instanceof OnboardingScreen screen && screen.isNewsStep())) {
			throw new AssertionError("Nach dem Umstieg sollten die Neuigkeiten kommen");
		}
		context.takeScreenshot("onboarding-update-3-umstieg");
		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		context.waitTicks(10);
		if (context.computeOnClient(c -> c.screen instanceof OnboardingScreen)
				|| !readSeen().contains("\"entitycounter\": \"" + MOD_VERSION + "\"")) {
			throw new AssertionError("Escape sollte schließen und die Version merken: " + readSeen());
		}
	}

	private static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(EntityCounterClient.MOD_ID)
			.orElseThrow().getMetadata().getVersion().getFriendlyString();

	private static String readSeen() {
		try {
			return Files.readString(Onboarding.seenFilePath());
		} catch (IOException e) {
			throw new AssertionError("config/KCMods/seen.json wurde nicht geschrieben", e);
		}
	}

	private static void writeSeen(String json) {
		try {
			Files.writeString(Onboarding.seenFilePath(), json);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	private static void expectStep(ClientGameTestContext context, int expected) {
		int actual = context.computeOnClient(c ->
				c.screen instanceof OnboardingScreen screen ? screen.getStepIndex() : Integer.MIN_VALUE);
		if (actual != expected) {
			throw new AssertionError("Tutorial: erwartet Seite " + expected + ", ist aber " + actual);
		}
	}

	/** Wartet, bis die Statusmeldung ausgeblendet ist, und leert den Chat – für aufgeräumte Screenshots. */
	private static void calm(ClientGameTestContext context) {
		context.waitTicks(80);
		context.runOnClient(c -> c.gui.getChat().clearMessages(false));
		context.waitTicks(5);
	}

	private static void expectMode(ClientGameTestContext context, TargetMode mode, BlockPos pos, int total) {
		context.runOnClient(c -> EntityCounterClient.getConfig().targetMode = mode);
		context.waitTicks(5);
		EntityScan scan = context.computeOnClient(c -> EntityCounterClient.getScan());
		if (!pos.equals(scan.pos()) || scan.total() != total) {
			throw new AssertionError("Modus " + mode + ": erwartet " + total + " bei " + pos + ", bekommen: " + scan);
		}
	}

	private static void expect(EntityScan scan, int total, int types) {
		if (!CHAMBER.equals(scan.pos()) || scan.total() != total || scan.entries().size() != types) {
			throw new AssertionError("Erwartet " + total + " Entities in " + types + " Typen bei " + CHAMBER
					+ ", bekommen: " + scan);
		}
	}
}
