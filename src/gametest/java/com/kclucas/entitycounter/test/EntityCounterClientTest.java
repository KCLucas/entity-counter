package com.kclucas.entitycounter.test;

import com.kclucas.entitycounter.client.EntityCounterClient;
import com.kclucas.entitycounter.client.EntityCounterConfigScreen;
import com.kclucas.entitycounter.client.EntityScan;
import com.kclucas.entitycounter.client.HudPositionScreen;
import com.kclucas.entitycounter.client.TargetMode;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import org.lwjgl.glfw.GLFW;

/**
 * Baut eine kleine Kammer hinter Glas, setzt Entities in genau einen Block und prüft,
 * dass der Entity Counter sie richtig zählt. Screenshots landen im Run-Ordner.
 */
public class EntityCounterClientTest implements FabricClientGameTest {

	private static final BlockPos CHAMBER = new BlockPos(0, 201, 5);
	private static final String AT = "0.5 201 5.5";

	@Override
	public void runTest(ClientGameTestContext context) {
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
			context.waitTicks(40);

			scan = context.computeOnClient(c -> EntityCounterClient.getScan());
			// 15 Armadillos + 3 Hühner + Slime + Lore + ein Stapel aus 5 Diamanten.
			expect(scan, 25, 5);
			EntityScan.Entry diamonds = scan.entries().stream()
					.filter(e -> e.type() == EntityType.ITEM).findFirst().orElseThrow();
			if (diamonds.count() != 5 || !diamonds.name().getString().equals("Diamond")) {
				throw new AssertionError("Erwartet \"Diamond: 5\", bekommen: "
						+ diamonds.name().getString() + ": " + diamonds.count());
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
