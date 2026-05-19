package io.github.starrymoon.moonpet.client.entity;

import io.github.starrymoon.moonpet.MoonPet;
import io.github.starrymoon.moonpet.entity.PetEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;

public class PetEntityRenderer extends MobRenderer<PetEntity, LivingEntityRenderState, PetEntityModel> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MoonPet.MOD_ID, "textures/entity/pet.png");

	public PetEntityRenderer(EntityRendererProvider.Context context) {
		super(context, new PetEntityModel(context.bakeLayer(ModelLayers.SLIME)), 0.5F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public ResourceLocation getTextureLocation(LivingEntityRenderState renderState) {
		return TEXTURE;
	}
}
