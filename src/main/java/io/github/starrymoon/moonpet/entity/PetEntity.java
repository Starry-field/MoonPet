package io.github.starrymoon.moonpet.entity;

import io.github.starrymoon.moonpet.ai.PetBrain;
import io.github.starrymoon.moonpet.config.MoonPetConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class PetEntity extends PathfinderMob {
	private UUID ownerUuid;
	private final PetBrain brain;

	public PetEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
		super(entityType, level);
		this.brain = new PetBrain(this);
		this.setPersistenceRequired();
		this.setCustomNameVisible(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0D)
			.add(Attributes.MOVEMENT_SPEED, 0.30D)
			.add(Attributes.FOLLOW_RANGE, 32.0D)
			.add(Attributes.ATTACK_DAMAGE, 4.0D);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			this.setCustomName(Component.literal(getPetName()));
			brain.tick();
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		return super.hurtServer(level, damageSource, amount);
	}

	@Override
	public void die(DamageSource damageSource) {
		super.die(damageSource);
		ServerPlayer owner = getOwnerPlayer();
		if (owner != null) {
			owner.sendSystemMessage(Component.literal("[" + getPetName() + "]: Ouch... I'll be back soon!"));
		}
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	public void setOwner(ServerPlayer player) {
		this.ownerUuid = player.getUUID();
	}

	public ServerPlayer getOwnerPlayer() {
		if (!(this.level() instanceof ServerLevel serverLevel) || ownerUuid == null) {
			return null;
		}

		return serverLevel.getPlayerByUUID(ownerUuid) instanceof ServerPlayer player ? player : null;
	}

	public UUID getOwnerUuid() {
		return ownerUuid;
	}

	public PetBrain getPetBrain() {
		return brain;
	}

	public String getPetName() {
		return MoonPetConfig.get().petName();
	}

	public void teleportNearOwner(ServerPlayer owner) {
		this.moveTo(owner.getX() + 1.0D, owner.getY(), owner.getZ() + 1.0D, owner.getYRot(), owner.getXRot());
		this.getNavigation().stop();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (ownerUuid != null) {
			tag.putUUID("Owner", ownerUuid);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.hasUUID("Owner")) {
			ownerUuid = tag.getUUID("Owner");
		}
	}
}
