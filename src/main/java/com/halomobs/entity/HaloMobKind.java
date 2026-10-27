package com.halomobs.entity;

import net.minecraft.util.RandomSource;

import java.util.Locale;

import static com.halomobs.entity.HaloWeapon.*;

/**
 * Define cada mob: tamaño (hitbox), escala del modelo y stats.
 * Los nombres de animación son animation.halo.<id>.<idle|walk|run|attack|dead>
 * (generados por tools/import_halocraftr.py a partir del addon Bedrock).
 */
public enum HaloMobKind {
    //      id               ancho alto  escala vida dano vel    huevo1    huevo2
    // Armas por mob: pesos tomados de las variantes del behavior pack (entities/covenant/**).
    ELITE("elite",           0.8f, 2.5f, 0.85f, 60, 8,  0.30, 0x2A5CAA, 0xA8C8FF, true,
            w(PLASMA_RIFLE, 28), w(NEEDLER, 18), w(PLASMA_REPEATER, 14), w(CARBINE, 16), w(PLASMA_PISTOL, 14), w(ENERGY_SWORD, 10)),
    GRUNT("grunt",           0.5f, 1.4f, 0.70f, 20, 3,  0.26, 0xD0782A, 0xFFD49A, true,
            w(PLASMA_PISTOL, 55), w(NEEDLER, 45)),
    JACKAL("jackal",         0.5f, 1.65f, 0.70f, 24, 4, 0.30, 0x5C6B3A, 0xC9D8A0, false,
            w(PLASMA_PISTOL, 70), w(NEEDLER, 30)),
    SKIRMISHER("skirmisher", 0.5f, 1.65f, 0.70f, 26, 5, 0.34, 0x3F6B6B, 0xE0C060, false,
            w(CARBINE, 40), w(NEEDLER, 30), w(BEAM_RIFLE, 30)),
    HUNTER("hunter",         1.2f, 3.5f, 1.00f, 120, 14, 0.22, 0x3B3F6B, 0xE0B040, false,
            w(FUEL_ROD, 100)),            // el cañón ya viene en el modelo (hueso "gun"): sin item en la mano
    BRUTE_CAPITAN("brute_capitan", 0.8f, 2.4f, 0.85f, 70, 9, 0.28, 0x6B2A2A, 0xD08A5A, true,
            w(SPIKER, 25), w(MAULER, 25), w(BRUTE_SHOT, 20), w(PLASMA_RIFLE, 15), w(CARBINE, 15)),
    BRUTE_CACIQUE("brute_cacique", 1.1f, 3.2f, 1.00f, 90, 12, 0.28, 0x4A1F1F, 0xE0B070, false);  // martillo propio del modelo: solo melee

    public final String id;
    public final float width, height, scale;
    public final double health, damage, speed;
    public final String idle, walk, run, attack, dead;
    public final int eggPrimary, eggSecondary;
    /** Si lanza granadas de plasma (habilidad común del Covenant en el addon). */
    public final boolean grenades;
    private final Loadout[] loadout;

    private record Loadout(HaloWeapon weapon, int weight) {}
    private static Loadout w(HaloWeapon weapon, int weight) { return new Loadout(weapon, weight); }

    HaloMobKind(String id, float w, float h, float scale, double health, double damage, double speed, int e1, int e2,
                boolean grenades, Loadout... loadout) {
        this.grenades = grenades; this.loadout = loadout;
        this.id = id; this.width = w; this.height = h; this.scale = scale;
        this.health = health; this.damage = damage; this.speed = speed;
        this.idle = anim("idle"); this.walk = anim("walk"); this.run = anim("run");
        this.attack = anim("attack"); this.dead = anim("dead");
        this.eggPrimary = e1; this.eggSecondary = e2;
    }

    /** Elige un arma al azar según los pesos del mob; null si no usa armas. */
    public HaloWeapon pickWeapon(RandomSource random) {
        int total = 0;
        for (Loadout l : loadout) total += l.weight;
        if (total <= 0) return null;
        int r = random.nextInt(total);
        for (Loadout l : loadout) { r -= l.weight; if (r < 0) return l.weapon; }
        return null;
    }

    private String anim(String role) { return "animation.halo." + id + "." + role; }

    public static HaloMobKind byId(String id) {
        for (HaloMobKind k : values()) if (k.id.equals(id.toLowerCase(Locale.ROOT))) return k;
        return ELITE;
    }
}
