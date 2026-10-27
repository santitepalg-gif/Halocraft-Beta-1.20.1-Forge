package com.halomobs.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Locale;

/**
 * Armas del Covenant. Los valores salen del behavior pack Bedrock "halocraftb"
 * (entities/balas/*.json = daño/velocidad/gravedad/dispersión del proyectil,
 * entities/covenant/** = ráfagas y cadencia de los mobs, items/guns y items/melee).
 *
 * El daño de Bedrock está pensado para otra escala de vida, así que se multiplica por
 * {@link #DAMAGE_SCALE}. Cambia esa constante para balancear todas las armas de golpe.
 * Los tiempos están en ticks (Bedrock los da en segundos: x20).
 */
public enum HaloWeapon {
    //            id                tipo   daño bedrock  veloc grav   disp  rafaga retraso cdMin cdMax alcance  color     explosion(radio,daño) pellets sonido,tono                held
    PLASMA_PISTOL("plasma_pistol",   Type.RANGED, 11.5f, 1.6f, 0.000f, 1.8f, 4, 2, 14, 24, 24, 0x5EC8FF, 0f, 0f, 1, SoundEvents.BLAZE_SHOOT, 1.9f, 0.40f, true),
    PLASMA_RIFLE("plasma_rifle",     Type.RANGED, 13.5f, 1.8f, 0.000f, 1.5f, 6, 2, 12, 20, 28, 0x5EC8FF, 0f, 0f, 1, SoundEvents.BLAZE_SHOOT, 1.7f, 0.40f, true),
    NEEDLER("needler",               Type.RANGED, 13.5f, 2.0f, 0.000f, 1.7f, 5, 2, 12, 18, 28, 0xFF6AD5, 0f, 0f, 1, SoundEvents.CROSSBOW_SHOOT, 1.6f, 0.30f, true),
    PLASMA_REPEATER("plasma_repeater", Type.RANGED, 15f, 2.0f, 0.000f, 1.0f, 8, 2, 14, 22, 30, 0x4EA8FF, 0f, 0f, 1, SoundEvents.BLAZE_SHOOT, 1.5f, 0.35f, true),
    CARBINE("carbine",               Type.RANGED, 22.5f, 3.2f, 0.000f, 0.5f, 3, 4, 18, 24, 40, 0xE8F7FF, 0f, 0f, 1, SoundEvents.FIREWORK_ROCKET_BLAST, 1.4f, 0.30f, true),
    BEAM_RIFLE("beam_rifle",         Type.RANGED, 60f,  4.0f, 0.000f, 0.2f, 1, 0, 50, 70, 48, 0x7DFF9A, 0f, 0f, 1, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.8f, 0.45f, true),
    SPIKER("spiker",                 Type.RANGED, 19.5f, 1.8f, 0.012f, 3.0f, 6, 2, 14, 22, 24, 0xFFB070, 0f, 0f, 1, SoundEvents.CROSSBOW_SHOOT, 0.9f, 0.35f, true),
    MAULER("mauler",                 Type.RANGED, 40f,  1.6f, 0.010f, 5.0f, 2, 11, 22, 28, 12, 0xFFD070, 0f, 0f, 3, SoundEvents.GENERIC_EXPLODE, 2.0f, 0.30f, true),
    BRUTE_SHOT("brute_shot",         Type.RANGED, 30f,  1.3f, 0.030f, 1.2f, 2, 6, 28, 36, 28, 0xFFC060, 2.2f, 60f, 1, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8f, 0.55f, true),
    FUEL_ROD("fuel_rod",             Type.RANGED, 26f,  1.5f, 0.020f, 1.0f, 3, 5, 40, 60, 30, 0x7DFF5A, 2.6f, 120f, 1, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.7f, 0.65f, false),
    ENERGY_SWORD("energy_sword",     Type.MELEE,  0f,   0f,   0f,     0f,   0, 0, 0, 0, 0, 0x5EC8FF, 0f, 0f, 1, SoundEvents.BLAZE_SHOOT, 1.0f, 0f, true),
    /** Granada de plasma (habilidad de los mobs, no es un item). */
    PLASMA_GRENADE("plasma_grenade", Type.THROWN, 12f,  1.0f, 0.040f, 1.0f, 1, 0, 0, 0, 20, 0x5EC8FF, 2.4f, 40f, 1, SoundEvents.SNOWBALL_THROW, 0.7f, 0.50f, false);

    public enum Type { RANGED, MELEE, THROWN }

    /** Multiplicador global del daño de Bedrock -> Minecraft. */
    public static final float DAMAGE_SCALE = 0.22f;

    public final String id;
    public final Type type;
    public final float damage;           // por proyectil (ya escalado)
    public final float speed, gravity, inaccuracy;
    public final int burst, burstDelay, cooldownMin, cooldownMax;
    public final double range;
    public final int color;
    public final float explosionRadius, explosionDamage;
    public final int pellets;
    public final SoundEvent sound;
    public final float soundPitch;
    public final float boltSize;
    public final boolean held;           // true = tiene item/modelo en la mano

    HaloWeapon(String id, Type type, float dmg, float speed, float gravity, float inaccuracy, int burst, int burstDelay,
               int cdMin, int cdMax, double range, int color, float expRadius, float expDmg, int pellets,
               SoundEvent sound, float pitch, float size, boolean held) {
        this.id = id; this.type = type;
        this.damage = dmg * DAMAGE_SCALE;
        this.speed = speed; this.gravity = gravity; this.inaccuracy = inaccuracy;
        this.burst = burst; this.burstDelay = burstDelay; this.cooldownMin = cdMin; this.cooldownMax = cdMax;
        this.range = range; this.color = color;
        this.explosionRadius = expRadius; this.explosionDamage = expDmg * DAMAGE_SCALE;
        this.pellets = pellets; this.sound = sound; this.soundPitch = pitch; this.boltSize = size; this.held = held;
    }

    public boolean isRanged() { return type == Type.RANGED; }
    public boolean isMelee() { return type == Type.MELEE; }
    /** Daño extra cuerpo a cuerpo (solo la espada de energía). */
    public float meleeBonus() { return isMelee() ? 200f * DAMAGE_SCALE * 0.35f : 0f; }

    public static HaloWeapon byId(String id) {
        if (id == null) return null;
        for (HaloWeapon w : values()) if (w.id.equals(id.toLowerCase(Locale.ROOT))) return w;
        return null;
    }

    public static HaloWeapon byOrdinal(int i) {
        HaloWeapon[] all = values();
        return i >= 0 && i < all.length ? all[i] : null;
    }
}
