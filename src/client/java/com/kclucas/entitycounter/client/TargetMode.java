package com.kclucas.entitycounter.client;

import net.minecraft.network.chat.Component;

/** Welcher Block relativ zum Blick gezählt wird. */
public enum TargetMode {
	/** Sieht durch durchsichtige Blöcke hindurch und nimmt den ersten Block, in dem Entities stehen. */
	AUTO,
	/** Genau der Block, auf den das Fadenkreuz zeigt. */
	LOOKED_AT,
	/** Der Block vor dem anvisierten, also auf der Seite, auf die man schaut. */
	IN_FRONT,
	/** Der Block direkt hinter dem anvisierten. */
	BEHIND;

	public Component label() {
		return Component.translatable("entitycounter.mode." + name().toLowerCase());
	}

	public Component description() {
		return Component.translatable("entitycounter.mode." + name().toLowerCase() + ".desc");
	}
}
