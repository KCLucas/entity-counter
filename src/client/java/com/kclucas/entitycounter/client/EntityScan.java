package com.kclucas.entitycounter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ergebnis eines Scans: der anvisierte Block und die Entities, die in ihm stehen, nach Typ gezählt.
 * Ein Entity zählt zu dem Block, in dem seine Position (Fußmitte) liegt – so wird jedes Entity
 * genau einem Block zugeordnet, auch wenn seine Hitbox in Nachbarblöcke ragt.
 */
public record EntityScan(@Nullable BlockPos pos, List<Entry> entries, int total) {

	public static final EntityScan NONE = new EntityScan(null, List.of(), 0);

	/** Wie weit der Blick reicht. Bewusst weiter als die Abbaureichweite, damit man Farmen aus Abstand prüfen kann. */
	private static final double RANGE = 32.0;
	private static final double STEP = 0.05;

	/** Ein Entity-Typ mit Anzahl; {@code sample} ist ein Vertreter davon für das Bild im HUD. */
	public record Entry(EntityType<?> type, Entity sample, int count) {
	}

	public static EntityScan compute(Minecraft client) {
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			return NONE;
		}

		Map<BlockPos, List<Entity>> byBlock = new HashMap<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == player || entity.isRemoved() || entity.isSpectator()) {
				continue;
			}
			byBlock.computeIfAbsent(entity.blockPosition(), p -> new ArrayList<>()).add(entity);
		}

		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 look = player.getViewVector(1.0F);

		// Blickstrahl Block für Block ablaufen. Durchsichtige Blöcke (Glas, Zäune, Trichter ...)
		// stoppen ihn nicht, erst ein massiver, undurchsichtiger Block beendet die Suche.
		BlockPos firstOccupied = null;
		BlockPos solid = null;
		double reach = RANGE;
		BlockPos last = null;
		for (double t = 0.0; t <= RANGE; t += STEP) {
			BlockPos cell = BlockPos.containing(eye.x + look.x * t, eye.y + look.y * t, eye.z + look.z * t);
			if (cell.equals(last)) {
				continue;
			}
			last = cell;
			if (level.getBlockState(cell).isSolidRender()) {
				solid = cell;
				reach = t;
				break;
			}
			if (firstOccupied == null && byBlock.containsKey(cell)) {
				firstOccupied = cell;
			}
		}

		// Ein direkt anvisiertes Entity gewinnt: gezählt wird der Block, in dem es steht.
		Vec3 end = eye.add(look.scale(reach));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(
				player, eye, end, searchBox, e -> !e.isSpectator(), reach * reach);

		BlockPos target;
		if (hit != null) {
			target = hit.getEntity().blockPosition();
		} else if (firstOccupied != null) {
			target = firstOccupied;
		} else {
			target = solid;
		}
		if (target == null) {
			return NONE;
		}

		Map<EntityType<?>, List<Entity>> byType = new LinkedHashMap<>();
		for (Entity entity : byBlock.getOrDefault(target, List.of())) {
			byType.computeIfAbsent(entity.getType(), t -> new ArrayList<>()).add(entity);
		}

		List<Entry> entries = new ArrayList<>();
		int total = 0;
		for (Map.Entry<EntityType<?>, List<Entity>> e : byType.entrySet()) {
			List<Entity> list = e.getValue();
			entries.add(new Entry(e.getKey(), list.get(0), list.size()));
			total += list.size();
		}
		entries.sort((a, b) -> {
			int byCount = Integer.compare(b.count(), a.count());
			return byCount != 0 ? byCount
					: a.type().getDescription().getString().compareTo(b.type().getDescription().getString());
		});

		return new EntityScan(target.immutable(), List.copyOf(entries), total);
	}
}
