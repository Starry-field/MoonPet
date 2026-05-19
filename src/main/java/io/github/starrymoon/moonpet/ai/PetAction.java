package io.github.starrymoon.moonpet.ai;

import java.util.Locale;

public record PetAction(String action, String target, int amount, String reply) {
	public static PetAction fallbackChat(String reply) {
		return new PetAction("chat", null, 0, reply);
	}

	public String normalizedAction() {
		return action == null ? "chat" : action.toLowerCase(Locale.ROOT);
	}
}
