package com.kclucas.entitycounter.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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

	/**
	 * Eine Zeile der Anzeige: ein Entity-Typ mit Anzahl; {@code sample} ist ein Vertreter davon für das Bild.
	 * Liegende Items werden nach Item-Art getrennt geführt und zählen mit ihrer Stapelgröße.
	 */
	public record Entry(EntityType<?> type, Component name, Entity sample, int count) {
	}

	/** Gruppier-Schlüssel für liegende Items: gleich bei gleichem Item mit gleichen Komponenten, egal wie viele. */
	private record ItemKind(ItemStack stack) {
		@Override
		public boolean equals(Object other) {
			return other instanceof ItemKind kind && ItemStack.isSameItemSameComponents(this.stack, kind.stack);
		}

		@Override
		public int hashCode() {
			return ItemStack.hashItemAndComponents(this.stack);
		}
	}

	public static EntityScan compute(Minecraft client, TargetMode mode) {
		LocalPlayer player = client.player;
		ClientLevel level = client.level;
		if (player == null || level == null) {
			return NONE;
		}

		// Das eigene Boot oder Minecart zählt so wenig wie der Spieler selbst - sonst träfe der Blick beim Fahren
		// nach unten immer das eigene Fahrzeug.
		Entity ownVehicle = player.getRootVehicle();
		Map<BlockPos, List<Entity>> byBlock = new HashMap<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity == player || entity == ownVehicle || entity.isRemoved() || entity.isSpectator()) {
				continue;
			}
			byBlock.computeIfAbsent(entity.blockPosition(), p -> new ArrayList<>()).add(entity);
		}

		BlockPos target = mode == TargetMode.AUTO
				? findAutoTarget(player, level, byBlock, ownVehicle)
				: findCrosshairTarget(player, level, mode);
		if (target == null) {
			return NONE;
		}

		// Mehrere gleiche Items verschmelzen im Spiel zu einem einzigen Item-Entity mit Stapelgröße.
		// Deshalb gruppieren wir Items nach Item samt Komponenten (Trankart, Verzauberung, Inhalt ...)
		// und zählen die Stapelgröße statt der Entities.
		Map<Object, List<Entity>> groups = new LinkedHashMap<>();
		for (Entity entity : byBlock.getOrDefault(target, List.of())) {
			Object key = entity instanceof ItemEntity item ? new ItemKind(item.getItem()) : entity.getType();
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(entity);
		}

		List<Entry> entries = new ArrayList<>();
		int total = 0;
		for (List<Entity> group : groups.values()) {
			Entity first = group.get(0);
			Component name;
			int count;
			if (first instanceof ItemEntity item) {
				// Mit eigenem Namen (Amboss) steht der da, sonst der normale wie "Trank der Schnelligkeit".
				name = item.getItem().getHoverName();
				count = 0;
				for (Entity entity : group) {
					count += ((ItemEntity) entity).getItem().getCount();
				}
			} else {
				name = first.getType().getDescription();
				count = group.size();
			}
			entries.add(new Entry(first.getType(), name, first, count));
			total += count;
		}
		entries.sort((a, b) -> {
			int byCount = Integer.compare(b.count(), a.count());
			return byCount != 0 ? byCount : a.name().getString().compareTo(b.name().getString());
		});

		return new EntityScan(target.immutable(), List.copyOf(entries), total);
	}

	/** Der Block unter dem Fadenkreuz bzw. sein Nachbar davor oder dahinter – wie die normale Blockauswahl. */
	private static @Nullable BlockPos findCrosshairTarget(LocalPlayer player, ClientLevel level, TargetMode mode) {
		Vec3 eye = player.getEyePosition(1.0F);
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(RANGE));
		BlockHitResult hit = level.clip(new ClipContext(
				eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		return switch (mode) {
			case IN_FRONT -> hit.getBlockPos().relative(hit.getDirection());
			case BEHIND -> hit.getBlockPos().relative(hit.getDirection().getOpposite());
			default -> hit.getBlockPos();
		};
	}

	private static @Nullable BlockPos findAutoTarget(
			LocalPlayer player, ClientLevel level, Map<BlockPos, List<Entity>> byBlock, Entity ownVehicle) {
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
				player, eye, end, searchBox, e -> !e.isSpectator() && e != ownVehicle, reach * reach);

		if (hit != null) {
			return hit.getEntity().blockPosition();
		}
		return firstOccupied != null ? firstOccupied : solid;
	}
}
