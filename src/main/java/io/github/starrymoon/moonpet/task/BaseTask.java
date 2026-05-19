package io.github.starrymoon.moonpet.task;

import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public abstract class BaseTask implements Task {
	protected final PetEntity pet;
	protected final ServerPlayer owner;
	private final String targetName;
	private boolean done;

	protected BaseTask(PetEntity pet, ServerPlayer owner, String targetName) {
		this.pet = pet;
		this.owner = owner;
		this.targetName = targetName;
	}

	protected void finish() {
		done = true;
	}

	protected boolean isFinished() {
		return done;
	}

	protected void say(String message) {
		if (owner.connection != null) {
			owner.sendSystemMessage(Component.literal("[" + pet.getPetName() + "]: " + message));
		}
	}

	protected String targetName() {
		return targetName;
	}

	@Override
	public boolean isDone() {
		return done;
	}
}
