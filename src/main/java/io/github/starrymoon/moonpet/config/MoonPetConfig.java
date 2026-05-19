package io.github.starrymoon.moonpet.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.github.starrymoon.moonpet.MoonPet;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MoonPetConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("moonpet.config.json");
	private static MoonPetConfigData cached = defaultConfig();

	private MoonPetConfig() {
	}

	public static MoonPetConfigData load() {
		if (Files.notExists(CONFIG_PATH)) {
			save(defaultConfig());
			cached = defaultConfig();
			return cached;
		}

		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			cached = GSON.fromJson(reader, MoonPetConfigData.class);
			if (cached == null) {
				cached = defaultConfig();
			}
		} catch (IOException | JsonSyntaxException exception) {
			MoonPet.LOGGER.error("Failed to load MoonPet config, using defaults.", exception);
			cached = defaultConfig();
		}

		save(cached);
		return cached;
	}

	public static MoonPetConfigData get() {
		return cached;
	}

	public static void save(MoonPetConfigData config) {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException exception) {
			MoonPet.LOGGER.error("Failed to save MoonPet config.", exception);
		}
	}

	private static MoonPetConfigData defaultConfig() {
		return new MoonPetConfigData(
			"SET_ME",
			"Luna",
			64,
			10,
			2,
			1.15D,
			1.35D,
			32.0D
		);
	}

	public record MoonPetConfigData(
		String deepSeekApiKey,
		String petName,
		int maxTaskRange,
		int requestTimeoutSeconds,
		int retryCount,
		double followSpeed,
		double workSpeed,
		double teleportDistance
	) {
		public boolean hasApiKey() {
			return deepSeekApiKey != null && !deepSeekApiKey.isBlank() && !"SET_ME".equalsIgnoreCase(deepSeekApiKey);
		}
	}
}
