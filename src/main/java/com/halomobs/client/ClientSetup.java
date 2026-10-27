package com.halomobs.client;

import com.halomobs.HaloMod;
import com.halomobs.entity.HaloMobKind;
import com.halomobs.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HaloMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BOLT.get(), HaloBoltRenderer::new);
        for (HaloMobKind kind : HaloMobKind.values()) {
            event.registerEntityRenderer(ModEntities.TYPES.get(kind).get(), ctx -> new HaloMobRenderer(ctx, kind));
        }
    }
}
