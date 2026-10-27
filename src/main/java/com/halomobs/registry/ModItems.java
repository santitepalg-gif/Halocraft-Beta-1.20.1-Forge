package com.halomobs.registry;

import com.halomobs.HaloMod;
import com.halomobs.entity.HaloMobKind;
import com.halomobs.entity.HaloWeapon;
import com.halomobs.item.HaloWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HaloMod.MODID);
    public static final Map<HaloMobKind, RegistryObject<Item>> EGGS = new EnumMap<>(HaloMobKind.class);

    /** Armas Halo con item (las que se ven en la mano de los mobs y que ellos pueden soltar al morir). */
    public static final Map<HaloWeapon, RegistryObject<Item>> WEAPONS = new EnumMap<>(HaloWeapon.class);

    static {
        for (HaloWeapon w : HaloWeapon.values()) {
            if (!w.held) continue;
            WEAPONS.put(w, ITEMS.<Item>register(w.id, () -> w.isMelee()
                    ? new SwordItem(Tiers.DIAMOND, 4, -2.0f, new Item.Properties().stacksTo(1))
                    : new HaloWeaponItem(new Item.Properties().stacksTo(1), w)));
        }
        for (HaloMobKind kind : HaloMobKind.values()) {
            EGGS.put(kind, ITEMS.register(kind.id + "_spawn_egg", () ->
                    new ForgeSpawnEggItem(ModEntities.TYPES.get(kind), kind.eggPrimary, kind.eggSecondary, new Item.Properties())));
        }
    }
}
