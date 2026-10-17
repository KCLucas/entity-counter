package com.kclucas.entitycounter.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.kclucas.entitycounter.client.onboarding.KcFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class EntityCounterConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(EntityCounterClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = KcFiles.file(EntityCounterClient.MOD_ID, "config.json", "entitycounter.json");

	/** Den gezählten Block in der Welt grün markieren. */
	public boolean highlightBlock = true;
	public TargetMode targetMode = TargetMode.AUTO;
	/** Die Koordinaten des gezählten Blocks im HUD anzeigen. */
	public boolean showCoordinates = true;
	/**
	 * Position der Anzeige als Anteil des freien Platzes: 0 = ganz links/oben, 1 = ganz rechts/unten.
	 * So bleibt sie bei jeder Fenstergröße und GUI-Skalierung an derselben Ecke bzw. Kante.
	 */
	public float hudX = 0.0F;
	public float hudY = 0.0F;

	public static EntityCounterConfig load() {
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				EntityCounterConfig config = GSON.fromJson(reader, EntityCounterConfig.class);
				if (config != null) {
					if (config.targetMode == null) {
						config.targetMode = TargetMode.AUTO;
					}
					return config;
				}
			} catch (IOException | JsonParseException e) {
				LOGGER.warn("Konnte {} nicht lesen, nutze Standardwerte", PATH, e);
			}
		}
		return new EntityCounterConfig();
	}

	public void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("Konnte {} nicht speichern", PATH, e);
		}
	}
}
