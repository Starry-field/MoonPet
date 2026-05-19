package io.github.starrymoon.moonpet.task;

import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.server.level.ServerPlayer;

public class FollowTask extends BaseTask {
	public FollowTask(PetEntity pet, ServerPlayer owner) {
		super(pet, owner, null);
	}

	@Override
	public void start() {
	}

	@Override
	public void tick() {
		if (!owner.isAlive()) {
			return;
		}

		double distance = pet.distanceTo(owner);
		if (distance > MoonPetConfig.get().teleportDistance()) {
			pet.teleportNearOwner(owner);
			return;
		}

		if (distance > 4.0D) {
			pet.getNavigation().moveTo(owner, MoonPetConfig.get().followSpeed());
		} else {
			pet.getNavigation().stop();
		}
	}

	@Override
	public void report() {
	}
}
