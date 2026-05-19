package io.github.starrymoon.moonpet.chat;

import io.github.starrymoon.moonpet.MoonPet;
import io.github.starrymoon.moonpet.ai.DeepSeekClient;
import io.github.starrymoon.moonpet.ai.PetAction;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ChatHandler {
	private final DeepSeekClient deepSeekClient;

	public ChatHandler(DeepSeekClient deepSeekClient) {
		this.deepSeekClient = deepSeekClient;
	}

	public void register() {
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String text = message.signedContent();
			if (!isForPet(text)) {
				return;
			}

			PetEntity pet = MoonPet.findPet(sender);
			if (pet == null) {
				sender.sendSystemMessage(Component.literal("[MoonPet]: Spawn your pet first with /moonpet spawn."));
				return;
			}

			String prompt = stripPetName(text);
			sender.sendSystemMessage(Component.literal("[" + pet.getPetName() + "]: Give me a moment to think..."));
			deepSeekClient.interpret(prompt).thenAccept(action -> applyAction(sender, pet, action));
		});
	}

	private void applyAction(ServerPlayer sender, PetEntity pet, PetAction action) {
		sender.getServer().execute(() -> {
			if (!sender.isAlive() || pet.isRemoved()) {
				return;
			}

			if (action.reply() != null && !action.reply().isBlank()) {
				sender.sendSystemMessage(Component.literal("[" + pet.getPetName() + "]: " + action.reply()));
			}

			pet.getPetBrain().assignTask(action);
		});
	}

	private boolean isForPet(String message) {
		String petName = MoonPet.CONFIG.petName();
		if (message == null) {
			return false;
		}

		String normalized = message.trim().toLowerCase();
		return normalized.startsWith(petName.toLowerCase() + ",")
			|| normalized.startsWith(petName.toLowerCase() + " ")
			|| normalized.startsWith("moonpet ");
	}

	private String stripPetName(String message) {
		String trimmed = message.trim();
		String petName = MoonPet.CONFIG.petName();

		if (trimmed.regionMatches(true, 0, petName, 0, petName.length())) {
			return trimmed.substring(petName.length()).replaceFirst("^[,\\s]+", "");
		}

		if (trimmed.regionMatches(true, 0, "moonpet", 0, "moonpet".length())) {
			return trimmed.substring("moonpet".length()).replaceFirst("^[,\\s]+", "");
		}

		return trimmed;
	}
}
