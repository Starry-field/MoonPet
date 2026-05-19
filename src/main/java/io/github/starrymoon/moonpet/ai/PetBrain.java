package io.github.starrymoon.moonpet.ai;

import io.github.starrymoon.moonpet.entity.PetEntity;
import io.github.starrymoon.moonpet.task.CombatTask;
import io.github.starrymoon.moonpet.task.FarmTask;
import io.github.starrymoon.moonpet.task.FollowTask;
import io.github.starrymoon.moonpet.task.MineTask;
import io.github.starrymoon.moonpet.task.Task;
import net.minecraft.server.level.ServerPlayer;

public final class PetBrain {
	private final PetEntity pet;
	private Task activeTask;

	public PetBrain(PetEntity pet) {
		this.pet = pet;
	}

	public void tick() {
		ServerPlayer owner = pet.getOwnerPlayer();
		if (owner == null) {
			activeTask = null;
			return;
		}

		if (activeTask == null) {
			activeTask = new FollowTask(pet, owner);
			activeTask.start();
		}

		activeTask.tick();
		if (activeTask.isDone()) {
			activeTask.report();
			activeTask = new FollowTask(pet, owner);
			activeTask.start();
		}
	}

	public void assignTask(PetAction action) {
		ServerPlayer owner = pet.getOwnerPlayer();
		if (owner == null) {
			return;
		}

			activeTask = switch (action.normalizedAction()) {
			case "mine" -> new MineTask(pet, owner, action.target(), action.amount());
			case "farm" -> new FarmTask(pet, owner, action.target());
			case "combat" -> new CombatTask(pet, owner, action.target());
			case "follow" -> new FollowTask(pet, owner);
			default -> null;
		};

		if (activeTask != null) {
			activeTask.start();
		}
	}

	public boolean hasActiveTask() {
		return activeTask != null && !(activeTask instanceof FollowTask);
	}
}
