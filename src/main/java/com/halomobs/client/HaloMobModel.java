package com.halomobs.client;

import com.halomobs.HaloMod;
import com.halomobs.entity.HaloMobEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HaloMobModel extends GeoModel<HaloMobEntity> {
    @Override public ResourceLocation getModelResource(HaloMobEntity e) {
        return new ResourceLocation(HaloMod.MODID, "geo/entity/" + e.getKind().id + ".geo.json");
    }
    @Override public ResourceLocation getTextureResource(HaloMobEntity e) {
        return new ResourceLocation(HaloMod.MODID, "textures/entity/" + e.getKind().id + ".png");
    }
    @Override public ResourceLocation getAnimationResource(HaloMobEntity e) {
        return new ResourceLocation(HaloMod.MODID, "animations/entity/" + e.getKind().id + ".animation.json");
    }
}
