package io.github.starrymoon.moonpet.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.starrymoon.moonpet.MoonPet;
import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.config.MoonPetConfig.MoonPetConfigData;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DeepSeekClient {
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";
	private static final Gson GSON = new Gson();
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
		Thread thread = new Thread(runnable, "MoonPet-DeepSeek");
		thread.setDaemon(true);
		return thread;
	});

	private final OkHttpClient client;

	public DeepSeekClient() {
		MoonPetConfigData config = MoonPetConfig.get();
		this.client = new OkHttpClient.Builder()
			.callTimeout(Duration.ofSeconds(config.requestTimeoutSeconds()))
			.build();
	}

	public CompletableFuture<PetAction> interpret(String message) {
		MoonPetConfigData config = MoonPetConfig.get();
		if (!config.hasApiKey()) {
			return CompletableFuture.completedFuture(PetAction.fallbackChat("My brain isn't connected. Check the API key in config!"));
		}

		return CompletableFuture.supplyAsync(() -> doInterpret(message, config), EXECUTOR)
			.exceptionally(exception -> {
				MoonPet.LOGGER.error("Failed to interpret MoonPet command.", exception);
				return PetAction.fallbackChat("Hmm, I couldn't think of what to do. Try again?");
			});
	}

	private PetAction doInterpret(String message, MoonPetConfigData config) {
		int attempts = Math.max(1, config.retryCount() + 1);

		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				Request request = new Request.Builder()
					.url(API_URL)
					.addHeader("Authorization", "Bearer " + config.deepSeekApiKey())
					.addHeader("Content-Type", "application/json")
					.post(RequestBody.create(buildPayload(message, config.petName()), JSON))
					.build();

				try (Response response = client.newCall(request).execute()) {
					if (response.code() == 401) {
						return PetAction.fallbackChat("My brain isn't connected. Check the API key in config!");
					}

					if (response.code() == 429 && attempt < attempts) {
						sleepQuietly(5000L);
						continue;
					}

					if (!response.isSuccessful()) {
						throw new IOException("DeepSeek returned HTTP " + response.code());
					}

					String body = response.body() == null ? "" : response.body().string();
					return parseResponse(body);
				}
			} catch (Exception exception) {
				if (attempt >= attempts) {
					throw new RuntimeException(exception);
				}
			}
		}

		return PetAction.fallbackChat("Give me a moment to think...");
	}

	private String buildPayload(String message, String petName) {
		JsonObject root = new JsonObject();
		root.addProperty("model", "deepseek-chat");

		JsonArray messages = new JsonArray();
		JsonObject system = new JsonObject();
		system.addProperty("role", "system");
		system.addProperty(
			"content",
			"You are " + petName + ", an AI Minecraft pet. Respond ONLY as strict JSON with keys action,target,amount,reply. " +
				"Allowed actions: mine, farm, combat, follow, chat."
		);
		messages.add(system);

		JsonObject user = new JsonObject();
		user.addProperty("role", "user");
		user.addProperty("content", message);
		messages.add(user);

		root.add("messages", messages);
		root.addProperty("temperature", 0.2D);
		JsonObject responseFormat = new JsonObject();
		responseFormat.addProperty("type", "json_object");
		root.add("response_format", responseFormat);
		return GSON.toJson(root);
	}

	private PetAction parseResponse(String responseBody) {
		JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices == null || choices.isEmpty()) {
			return PetAction.fallbackChat("I'm not sure what to do with that.");
		}

		JsonObject firstChoice = choices.get(0).getAsJsonObject();
		JsonObject message = firstChoice.getAsJsonObject("message");
		String content = message == null ? null : message.get("content").getAsString();
		if (content == null || content.isBlank()) {
			return PetAction.fallbackChat("I'm not sure what to do with that.");
		}

		try {
			return CommandParser.parse(content);
		} catch (Exception exception) {
			MoonPet.LOGGER.warn("Failed to parse DeepSeek response: {}", content, exception);
			return PetAction.fallbackChat("I'm not sure what to do with that, but here's what I think: " + content);
		}
	}

	private void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}
