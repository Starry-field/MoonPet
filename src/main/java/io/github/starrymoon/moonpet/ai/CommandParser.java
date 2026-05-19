package io.github.starrymoon.moonpet.ai;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public final class CommandParser {
	private static final Gson GSON = new Gson();

	private CommandParser() {
	}

	public static PetAction parse(String jsonText) {
		JsonObject root = GSON.fromJson(jsonText, JsonObject.class);
		if (root == null) {
			throw new JsonParseException("DeepSeek returned empty JSON");
		}

		String action = getString(root, "action", "chat");
		String target = getOptionalString(root, "target");
		int amount = root.has("amount") && root.get("amount").isJsonPrimitive() ? root.get("amount").getAsInt() : 0;
		String reply = getString(root, "reply", "I'm listening.");
		return new PetAction(action, target, amount, reply);
	}

	private static String getString(JsonObject root, String key, String fallback) {
		String value = getOptionalString(root, key);
		return value == null || value.isBlank() ? fallback : value;
	}

	private static String getOptionalString(JsonObject root, String key) {
		if (!root.has(key) || root.get(key).isJsonNull()) {
			return null;
		}

		return root.get(key).getAsString();
	}
}
