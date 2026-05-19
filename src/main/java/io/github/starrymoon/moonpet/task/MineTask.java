package io.github.starrymoon.moonpet.task;

import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

public class MineTask extends BaseTask {
	private final String requestedTarget;
	private final int targetAmount;
	private BlockPos currentTarget;
	private BlockPos lastNavigatedTo;
	private int minedCount;
	private final Set<BlockPos> skippedTargets = new HashSet<>();

	public MineTask(PetEntity pet, ServerPlayer owner, String requestedTarget, int amount) {
		super(pet, owner, requestedTarget);
		this.requestedTarget = requestedTarget;
		this.targetAmount = amount <= 0 ? 1 : amount;
	}

	@Override
	public void start() {
		currentTarget = findTarget();
		if (currentTarget == null) {
			say("I searched around but couldn't find any " + requestedTarget + ".");
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
		if (state.isAir() || !matchesTarget(state)) {
			lastNavigatedTo = null;
			currentTarget = findTarget();
			if (currentTarget == null) {
				finish();
			}
			return;
		}

		double distance = pet.distanceToSqr(currentTarget.getX() + 0.5D, currentTarget.getY() + 0.5D, currentTarget.getZ() + 0.5D);
		if (distance > 9.0D) {
			if (!currentTarget.equals(lastNavigatedTo)) {
				boolean started = pet.getNavigation().moveTo(currentTarget.getX() + 0.5D, currentTarget.getY(), currentTarget.getZ() + 0.5D, MoonPetConfig.get().workSpeed());
				if (!started) {
					skippedTargets.add(currentTarget);
					lastNavigatedTo = null;
					currentTarget = findTarget();
					if (currentTarget == null) finish();
				} else {
					lastNavigatedTo = currentTarget;
				}
			} else if (pet.getNavigation().getPath() == null) {
				// Navigation ended but pet is still too far — path failed or overshot; retry
				lastNavigatedTo = null;
			}
			return;
		}

		pet.getNavigation().stop();
		if (!level.destroyBlock(currentTarget, true, pet)) {
			skippedTargets.add(currentTarget);
			currentTarget = findTarget();
			if (currentTarget == null) finish();
			return;
		}
		minedCount++;
		skippedTargets.clear();
		lastNavigatedTo = null;
		if (minedCount >= targetAmount) {
			finish();
			return;
		}

		currentTarget = findTarget();
		if (currentTarget == null) {
			finish();
		}
	}

	@Override
	public void report() {
		if (minedCount > 0) {
			say("I mined " + minedCount + " " + requestedTarget + " for you.");
		}
	}

	private BlockPos findTarget() {
		BlockPos result = searchTarget(true);
		return result != null ? result : searchTarget(false);
	}

	private BlockPos searchTarget(boolean exposedOnly) {
		int range = MoonPetConfig.get().maxTaskRange();
		BlockPos origin = owner.blockPosition();
		ServerLevel level = (ServerLevel) pet.level();

		for (int radius = 1; radius <= range; radius++) {
			for (BlockPos pos : BlockPos.withinManhattan(origin, radius, 8, radius)) {
				BlockPos immutable = pos.immutable();
				if (skippedTargets.contains(immutable)) continue;
				if (!matchesTarget(level.getBlockState(pos))) continue;
				if (exposedOnly && !isExposed(level, pos)) continue;
				return immutable;
			}
		}

		return null;
	}

	private boolean isExposed(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos.above()).isAir()
			|| level.getBlockState(pos.north()).isAir()
			|| level.getBlockState(pos.south()).isAir()
			|| level.getBlockState(pos.east()).isAir()
			|| level.getBlockState(pos.west()).isAir();
	}

	private boolean matchesTarget(BlockState state) {
		Block block = state.getBlock();
		String normalized = requestedTarget == null ? "" : requestedTarget.toLowerCase();
		if (normalized.contains("stone")) {
			return state.is(Blocks.STONE);
		}
		if (normalized.contains("diamond")) {
			return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
		}
		if (normalized.contains("coal")) {
			return state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE);
		}
		if (normalized.contains("iron")) {
			return state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE);
		}

		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		return key != null && key.toString().contains(normalized.replace(' ', '_'))
			|| state.is(BlockTags.MINEABLE_WITH_PICKAXE) && normalized.contains("ore") && key != null && key.getPath().contains("ore");
	}
}
