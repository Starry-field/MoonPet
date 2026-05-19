package io.github.starrymoon.moonpet.task;

import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

public class CombatTask extends BaseTask {
	private final String requestedTarget;
	private LivingEntity target;

	public CombatTask(PetEntity pet, ServerPlayer owner, String requestedTarget) {
		super(pet, owner, requestedTarget);
		this.requestedTarget = requestedTarget == null ? "hostile mob" : requestedTarget;
	}

	@Override
	public void start() {
		target = findTarget();
		if (target == null) {
			say("I couldn't spot any " + requestedTarget + " nearby.");
			finish();
		}
	}

	@Override
	public void tick() {
		if (isFinished()) {
			return;
		}

		if (target == null || !target.isAlive()) {
			target = findTarget();
			if (target == null) {
				finish();
			}
			return;
		}

		pet.setTarget(target);
		double distance = pet.distanceTo(target);
		if (distance > 2.5D) {
			pet.getNavigation().moveTo(target, MoonPetConfig.get().workSpeed());
			return;
		}

		if (pet.getAttackAnim(0.0F) == 0.0F) {
			pet.doHurtTarget((ServerLevel) pet.level(), target);
		}
	}

	@Override
	public void report() {
		say("Combat task complete.");
	}

	private LivingEntity findTarget() {
		AABB bounds = pet.getBoundingBox().inflate(MoonPetConfig.get().maxTaskRange());
		List<Monster> monsters = pet.level().getEntitiesOfClass(Monster.class, bounds, entity -> entity.isAlive() && matches(entity));
		return monsters.stream()
			.min(Comparator.comparingDouble(pet::distanceToSqr))
			.map(entity -> (LivingEntity) entity)
			.orElse(null);
	}

	private boolean matches(Monster entity) {
		if (requestedTarget == null || requestedTarget.isBlank()) {
			return true;
		}

		String targetKey = entity.getType().toShortString().toLowerCase();
		return targetKey.contains(requestedTarget.toLowerCase().replace(' ', '_'));
	}
}
