package io.github.starrymoon.moonpet.client;

import io.github.starrymoon.moonpet.MoonPet;
import io.github.starrymoon.moonpet.client.entity.PetEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class MoonPetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(MoonPet.PET_ENTITY_TYPE, PetEntityRenderer::new);
	}
}
