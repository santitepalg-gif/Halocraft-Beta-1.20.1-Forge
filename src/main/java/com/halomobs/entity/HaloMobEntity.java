package com.halomobs.entity;

import com.halomobs.entity.goal.HaloRangedAttackGoal;
import com.halomobs.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HaloMobEntity extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final HaloMobKind kind;

    private static final EntityDataAccessor<Integer> WEAPON =
            SynchedEntityData.defineId(HaloMobEntity.class, EntityDataSerializers.INT);
    private static final UUID MELEE_BONUS_ID = UUID.fromString("5b6f2c1e-7d1a-4c52-9a0e-4f0c6a1e9b11");
    private int grenadeCooldown = 120;
    @Nullable private ItemStack heldCache = ItemStack.EMPTY;
    private int heldCacheWeapon = -2;

    public HaloMobEntity(EntityType<? extends HaloMobEntity> type, Level level) {
        super(type, level);
        var key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        this.kind = key == null ? HaloMobKind.ELITE : HaloMobKind.byId(key.getPath());
    }

    public HaloMobKind getKind() { return kind; }

    // ---------------------------------------------------------------- armas

    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(WEAPON, -1);
    }

    @Nullable public HaloWeapon getWeapon() { return HaloWeapon.byOrdinal(entityData.get(WEAPON)); }

    public void setWeapon(@Nullable HaloWeapon w) {
        entityData.set(WEAPON, w == null ? -1 : w.ordinal());
        if (level().isClientSide) return;
        var atk = getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk == null) return;
        atk.removeModifier(MELEE_BONUS_ID);
        if (w != null && w.meleeBonus() > 0)
            atk.addTransientModifier(new AttributeModifier(MELEE_BONUS_ID, "halo_weapon", w.meleeBonus(), AttributeModifier.Operation.ADDITION));
    }

    /** Item que se dibuja en la mano (cliente). Vacío si el arma viene en el propio modelo. */
    public ItemStack getHeldWeaponStack() {
        HaloWeapon w = getWeapon();
        int ord = w == null ? -1 : w.ordinal();
        if (ord != heldCacheWeapon) {
            heldCacheWeapon = ord;
            var reg = w == null ? null : ModItems.WEAPONS.get(w);
            heldCache = reg == null ? ItemStack.EMPTY : new ItemStack(reg.get());
        }
        return heldCache;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        if (getWeapon() == null) setWeapon(kind.pickWeapon(this.random));
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        HaloWeapon w = getWeapon();
        tag.putString("HaloWeapon", w == null ? "" : w.id);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HaloWeapon")) setWeapon(HaloWeapon.byId(tag.getString("HaloWeapon")));
    }

    /** Dispara el arma hacia el objetivo (la llama {@link HaloRangedAttackGoal}). */
    public void shootWeapon(LivingEntity target, HaloWeapon w) {
        if (level().isClientSide) return;
        Vec3 origin = muzzlePos();
        double dx = target.getX() - origin.x, dz = target.getZ() - origin.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double dy = target.getY(0.55) - origin.y + flat * (w.gravity > 0 ? 0.20 + w.gravity * 3.0 : 0.0);
        float diffMul = 1.0f + (3 - level().getDifficulty().getId()) * 0.25f;   // más impreciso en fácil
        for (int i = 0; i < w.pellets; i++) {
            HaloBoltEntity bolt = new HaloBoltEntity(level(), this, w);
            bolt.setPos(origin.x, origin.y, origin.z);
            bolt.shoot(dx, dy, dz, w.speed, w.inaccuracy * diffMul);
            level().addFreshEntity(bolt);
        }
        level().playSound(null, getX(), getY(), getZ(), w.sound, SoundSource.HOSTILE, 0.9f, w.soundPitch + (random.nextFloat() - 0.5f) * 0.2f);
    }

    private Vec3 muzzlePos() {
        Vec3 look = Vec3.directionFromRotation(0, this.yBodyRot).normalize();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        double s = kind.scale;
        return new Vec3(getX() + look.x * 0.7 * s + right.x * 0.35 * s,
                        getY() + kind.height * 0.55,
                        getZ() + look.z * 0.7 * s + right.z * 0.35 * s);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Granada de plasma: habilidad de los Covenant (el addon la daba a casi todos con "charge_shoot_trigger").
        if (!level().isClientSide && kind.grenades && isAlive()) {
            LivingEntity t = getTarget();
            if (grenadeCooldown > 0) grenadeCooldown--;
            else if (t != null && t.isAlive() && getSensing().hasLineOfSight(t)) {
                double d = distanceTo(t);
                if (d > 6 && d < 20) {
                    shootWeapon(t, HaloWeapon.PLASMA_GRENADE);
                    grenadeCooldown = 160 + random.nextInt(140);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource src, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(src, looting, recentlyHit);
        HaloWeapon w = getWeapon();
        var reg = w == null ? null : ModItems.WEAPONS.get(w);
        if (reg != null && random.nextFloat() < 0.12f + 0.03f * looting) spawnAtLocation(new ItemStack(reg.get()));
    }

    public static AttributeSupplier.Builder createAttributes(HaloMobKind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.ATTACK_DAMAGE, k.damage)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HaloRangedAttackGoal(this));   // solo actúa si el arma es a distancia
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Alarga la muerte a 2 s para que se vea la animación "dead". */
    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, this::mainPredicate));
        controllers.add(new AnimationController<>(this, "attack", 0, this::attackPredicate));
    }

    private PlayState mainPredicate(AnimationState<HaloMobEntity> state) {
        if (isDeadOrDying()) {
            if (kind.dead == null) return PlayState.STOP;
            return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(kind.dead));
        }
        String anim = state.isMoving() ? (getTarget() != null && kind.run != null ? kind.run : kind.walk) : kind.idle;
        if (anim == null) return PlayState.STOP;
        return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
    }

    private PlayState attackPredicate(AnimationState<HaloMobEntity> state) {
        if (kind.attack != null && this.swinging && !isDeadOrDying()
                && state.getController().getAnimationState() == AnimationController.State.STOPPED) {
            state.getController().forceAnimationReset();
            state.setAnimation(RawAnimation.begin().then(kind.attack, software.bernie.geckolib.core.animation.Animation.LoopType.PLAY_ONCE));
            this.swinging = false;
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
