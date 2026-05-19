package io.github.starrymoon.moonpet.task;

import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.PotatoBlock;
import net.minecraft.world.level.block.state.BlockState;

public class FarmTask extends BaseTask {
	private final String requestedTarget;
	private BlockPos currentTarget;
	private int harvestCount;

	public FarmTask(PetEntity pet, ServerPlayer owner, String requestedTarget) {
		super(pet, owner, requestedTarget);
		this.requestedTarget = requestedTarget == null ? "crops" : requestedTarget;
	}

	@Override
	public void start() {
		currentTarget = findCrop();
		if (currentTarget == null) {
			say("I couldn't find any ready crops nearby.");
			finish();
		}
	}

	@Override
	public void tick() {
		if (isFinished() || currentTarget == null) {
			return;
		}

		ServerLevel level = (ServerLevel) pet.level();
		BlockState state = level.getBlockState(currentTarget);
		if (!(state.getBlock() instanceof CropBlock cropBlock) || !cropBlock.isMaxAge(state)) {
			currentTarget = findCrop();
			if (currentTarget == null) {
				finish();
			}
			return;
		}

		double distance = pet.distanceToSqr(currentTarget.getX() + 0.5D, currentTarget.getY() + 0.5D, currentTarget.getZ() + 0.5D);
		if (distance > 4.0D) {
			pet.getNavigation().moveTo(currentTarget.getX() + 0.5D, currentTarget.getY(), currentTarget.getZ() + 0.5D, MoonPetConfig.get().workSpeed());
			return;
		}

		Block harvestedBlock = state.getBlock();
		level.destroyBlock(currentTarget, true, pet);
		level.setBlockAndUpdate(currentTarget, replantedState(harvestedBlock));
		harvestCount++;
		currentTarget = findCrop();
		if (currentTarget == null) {
			finish();
		}
	}

	@Override
	public void report() {
		if (harvestCount > 0) {
			say("I harvested and replanted " + harvestCount + " " + requestedTarget + ".");
		}
	}

	private BlockPos findCrop() {
		int range = MoonPetConfig.get().maxTaskRange();
		BlockPos origin = owner.blockPosition();
		ServerLevel level = (ServerLevel) pet.level();

		for (int radius = 1; radius <= range; radius++) {
			for (BlockPos pos : BlockPos.withinManhattan(origin, radius, 6, radius)) {
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
					return pos.immutable();
				}
			}
		}

		return null;
	}

	private BlockState replantedState(Block block) {
		if (block instanceof CarrotBlock) {
			return Blocks.CARROTS.defaultBlockState();
		}
		if (block instanceof PotatoBlock) {
			return Blocks.POTATOES.defaultBlockState();
		}
		if (block instanceof BeetrootBlock) {
			return Blocks.BEETROOTS.defaultBlockState();
		}
		return Blocks.WHEAT.defaultBlockState();
	}
}
