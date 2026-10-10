package com.kclucas.entitycounter.test;

import com.kclucas.entitycounter.client.EntityCounterClient;
import com.kclucas.entitycounter.client.EntityScan;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
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
			server.runCommand("fill -3 200 -3 3 200 9 minecraft:stone");
			server.runCommand("fill -1 201 4 1 202 6 minecraft:glass");
			server.runCommand("setblock 0 201 5 minecraft:air");
			server.runCommand("setblock 0 202 5 minecraft:air");
			server.runCommand("tp @a 0.5 201 0.5 0 15");
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
			context.takeScreenshot("entitycounter-1-armadillos");

			for (int i = 0; i < 3; i++) {
				server.runCommand("summon minecraft:chicken " + AT + " {NoAI:1b}");
			}
			server.runCommand("summon minecraft:slime " + AT + " {NoAI:1b,Size:0}");
			server.runCommand("summon minecraft:minecart " + AT + " {NoGravity:1b}");
			server.runCommand("summon minecraft:item " + AT
					+ " {Item:{id:\"minecraft:diamond\",count:1},PickupDelay:32767,NoGravity:1b}");
			context.waitTicks(40);

			scan = context.computeOnClient(c -> EntityCounterClient.getScan());
			expect(scan, 21, 5);
			context.takeScreenshot("entitycounter-2-gemischt");

			// Vom Block weg in den Himmel schauen: kein Ziel mehr.
			server.runCommand("tp @a 0.5 201 0.5 180 -60");
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

	private static void expect(EntityScan scan, int total, int types) {
		if (!CHAMBER.equals(scan.pos()) || scan.total() != total || scan.entries().size() != types) {
			throw new AssertionError("Erwartet " + total + " Entities in " + types + " Typen bei " + CHAMBER
					+ ", bekommen: " + scan);
		}
	}
}
