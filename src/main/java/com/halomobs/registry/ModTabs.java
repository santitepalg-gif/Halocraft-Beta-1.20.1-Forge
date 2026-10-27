package com.halomobs.registry;

import com.halomobs.HaloMod;
import com.halomobs.entity.HaloMobKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HaloMod.MODID);

    public static final RegistryObject<CreativeModeTab> HALO = TABS.register("halo", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.halomobs"))
                    .icon(() -> new ItemStack(ModItems.EGGS.get(HaloMobKind.ELITE).get()))
                    .displayItems((params, out) -> {
                        ModItems.EGGS.values().forEach(e -> out.accept(e.get()));
                        ModItems.WEAPONS.values().forEach(e -> out.accept(e.get()));
                    })
                    .build());
}
