package io.github.starrymoon.moonpet;

import com.mojang.brigadier.Command;
import io.github.starrymoon.moonpet.ai.DeepSeekClient;
import io.github.starrymoon.moonpet.chat.ChatHandler;
import io.github.starrymoon.moonpet.config.MoonPetConfig;
import io.github.starrymoon.moonpet.config.MoonPetConfig.MoonPetConfigData;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MoonPet implements ModInitializer {
	public static final String MOD_ID = "moonpet";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ResourceLocation PET_ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "pet");
	public static final ResourceKey<EntityType<?>> PET_KEY = ResourceKey.create(Registries.ENTITY_TYPE, PET_ID);
	public static final EntityType<PetEntity> PET_ENTITY_TYPE = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		PET_ID,
		EntityType.Builder.of(PetEntity::new, MobCategory.CREATURE)
			.sized(0.8F, 0.8F)
			.clientTrackingRange(8)
			.updateInterval(3)
			.build(PET_KEY)
	);
	public static MoonPetConfigData CONFIG;
	private static final ConcurrentHashMap<UUID, UUID> PETS_BY_OWNER = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		CONFIG = MoonPetConfig.load();
		FabricDefaultAttributeRegistry.register(PET_ENTITY_TYPE, PetEntity.createAttributes());
		registerCommands();
		registerDisconnectCleanup();
		new ChatHandler(new DeepSeekClient()).register();
		LOGGER.info("MoonPet initialized.");
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			Commands.literal("moonpet")
				.then(Commands.literal("spawn").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					spawnPet(player);
					context.getSource().sendSuccess(() -> Component.literal("MoonPet spawned."), false);
					return Command.SINGLE_SUCCESS;
				}))
				.then(Commands.literal("dismiss").executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					PetEntity pet = findPet(player);
					if (pet != null) {
						pet.discard();
						PETS_BY_OWNER.remove(player.getUUID());
					}
					context.getSource().sendSuccess(() -> Component.literal("MoonPet dismissed."), false);
					return Command.SINGLE_SUCCESS;
				}))
		));
	}

	private void registerDisconnectCleanup() {
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.player;
			PetEntity pet = findPet(player);
			if (pet != null) {
				pet.discard();
				PETS_BY_OWNER.remove(player.getUUID());
			}
		});
	}

	public static void spawnPet(ServerPlayer player) {
		PetEntity existingPet = findPet(player);
		if (existingPet != null) {
			existingPet.teleportNearOwner(player);
			return;
		}

		ServerLevel level = player.serverLevel();
		PetEntity pet = new PetEntity(PET_ENTITY_TYPE, level);
		pet.setOwner(player);
		pet.setPos(player.getX() + 1.5D, player.getY(), player.getZ() + 1.5D);
		pet.setCustomName(Component.literal(CONFIG.petName()));
		level.addFreshEntity(pet);
		PETS_BY_OWNER.put(player.getUUID(), pet.getUUID());
	}

	public static PetEntity findPet(ServerPlayer player) {
		UUID petUuid = PETS_BY_OWNER.get(player.getUUID());
		if (petUuid == null) {
			return null;
		}

		Level level = player.level();
		if (!(level instanceof ServerLevel serverLevel)) {
			return null;
		}

		if (serverLevel.getEntity(petUuid) instanceof PetEntity pet) {
			return pet;
		}

		PETS_BY_OWNER.remove(player.getUUID());
		return null;
	}
}
