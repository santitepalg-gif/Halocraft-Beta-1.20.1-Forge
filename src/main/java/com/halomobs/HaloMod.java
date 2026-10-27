package com.halomobs;

import com.halomobs.entity.HaloMobEntity;
import com.halomobs.entity.HaloMobKind;
import com.halomobs.registry.ModEntities;
import com.halomobs.registry.ModItems;
import com.halomobs.registry.ModTabs;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HaloMod.MODID)
public class HaloMod {
    public static final String MODID = "halomobs";

    public HaloMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModTabs.TABS.register(bus);
        bus.addListener(this::registerAttributes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        for (HaloMobKind kind : HaloMobKind.values()) {
            event.put(ModEntities.TYPES.get(kind).get(), HaloMobEntity.createAttributes(kind).build());
        }
    }
}
