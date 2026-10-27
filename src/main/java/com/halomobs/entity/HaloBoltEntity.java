package com.halomobs.entity;

import com.halomobs.registry.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Proyectil de las armas Halo (plasma, agujas, balas, granadas). Un solo tipo de entidad:
 * el arma que lo disparó (ordinal de {@link HaloWeapon}) define daño, gravedad, color y explosión.
 * No daña a otros mobs de Halo (sin fuego amigo) y las explosiones no rompen bloques.
 */
public class HaloBoltEntity extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> WEAPON =
            SynchedEntityData.defineId(HaloBoltEntity.class, EntityDataSerializers.INT);
    private int life;

    public HaloBoltEntity(EntityType<? extends HaloBoltEntity> type, Level level) { super(type, level); }

    public HaloBoltEntity(Level level, LivingEntity owner, HaloWeapon weapon) {
        super(ModEntities.BOLT.get(), owner, level);
        setWeapon(weapon);
    }

    @Override protected void defineSynchedData() { entityData.define(WEAPON, HaloWeapon.PLASMA_PISTOL.ordinal()); }

    public void setWeapon(HaloWeapon w) { entityData.set(WEAPON, w.ordinal()); }

    public HaloWeapon getWeapon() {
        HaloWeapon w = HaloWeapon.byOrdinal(entityData.get(WEAPON));
        return w == null ? HaloWeapon.PLASMA_PISTOL : w;
    }

    @Override protected float getGravity() { return getWeapon().gravity; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && ++life > 100) { discard(); return; }
        if (level().isClientSide) {
            int c = getWeapon().color;
            Vector3f col = new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f);
            level().addParticle(new DustParticleOptions(col, 0.9f), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return !(e instanceof HaloMobEntity) && super.canHitEntity(e);   // sin fuego amigo entre mobs Halo
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        HaloWeapon w = getWeapon();
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource src = owner instanceof LivingEntity le ? damageSources().mobProjectile(this, le)
                                                             : damageSources().thrown(this, owner);
        if (w.damage > 0) {
            target.invulnerableTime = 0;   // que las ráfagas rápidas no se pierdan en los frames de invulnerabilidad
            target.hurt(src, w.damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        HaloWeapon w = getWeapon();
        if (w.explosionRadius > 0) explode(w);
        else level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.3f, 1.8f);
        discard();
    }

    private void explode(HaloWeapon w) {
        double r = w.explosionRadius;
        Entity owner = getOwner();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(r),
                x -> !(x instanceof HaloMobEntity))) {
            double d = e.distanceTo(this);
            if (d > r) continue;
            float f = (float) (1.0 - d / r);
            e.invulnerableTime = 0;
            e.hurt(damageSources().explosion(this, owner), Math.max(1f, w.explosionDamage * f));
            Vec3 push = e.position().subtract(position()).normalize().scale(0.6 * f);
            e.push(push.x, 0.3 * f, push.z);
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8f, 1.3f);
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putString("HaloWeapon", getWeapon().id);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        HaloWeapon w = HaloWeapon.byId(tag.getString("HaloWeapon"));
        if (w != null) setWeapon(w);
    }
}
