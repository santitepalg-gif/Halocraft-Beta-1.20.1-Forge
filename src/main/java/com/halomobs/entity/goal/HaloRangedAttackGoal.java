package com.halomobs.entity.goal;

import com.halomobs.entity.HaloMobEntity;
import com.halomobs.entity.HaloWeapon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Ataque a distancia en ráfagas (como los "ranged_attack" del addon: burst_shots / burst_interval /
 * attack_interval). Se mantiene a media distancia, retrocede si el objetivo se acerca y hace strafe.
 * No usa campos propios del mob en el constructor (registerGoals corre dentro del constructor de Mob).
 */
public class HaloRangedAttackGoal extends Goal {
    private final HaloMobEntity mob;
    private int cooldown, burstLeft, burstTimer, strafeTime;
    private boolean strafeRight = true;

    public HaloRangedAttackGoal(HaloMobEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        LivingEntity t = mob.getTarget();
        HaloWeapon w = mob.getWeapon();
        return t != null && t.isAlive() && w != null && w.isRanged();
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() {
        mob.setAggressive(true);
        cooldown = 8 + mob.getRandom().nextInt(12);
        burstLeft = 0;
    }

    @Override public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        burstLeft = 0;
    }

    @Override public void tick() {
        LivingEntity t = mob.getTarget();
        HaloWeapon w = mob.getWeapon();
        if (t == null || w == null) return;

        double dist = Math.sqrt(mob.distanceToSqr(t));
        boolean see = mob.getSensing().hasLineOfSight(t);
        mob.getLookControl().setLookAt(t, 40f, 40f);

        double preferred = Math.min(w.range * 0.6, 16.0);
        double tooClose = Math.min(5.0, w.range * 0.3);

        if (!see || dist > preferred + 4) {
            mob.getNavigation().moveTo(t, 1.15);
        } else if (dist < tooClose) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 10, 4, t.position());
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, 1.25);
        } else {
            mob.getNavigation().stop();
            if (--strafeTime <= 0) {
                strafeTime = 20 + mob.getRandom().nextInt(30);
                strafeRight = mob.getRandom().nextBoolean();
            }
            mob.getMoveControl().strafe(dist < preferred - 2 ? -0.35f : 0.0f, strafeRight ? 0.5f : -0.5f);
            mob.lookAt(t, 30f, 30f);
        }

        if (burstLeft > 0) {
            if (--burstTimer <= 0) {
                if (see) mob.shootWeapon(t, w);
                burstLeft--;
                burstTimer = Math.max(1, w.burstDelay);
                if (burstLeft == 0) cooldown = nextCooldown(w);
            }
        } else if (--cooldown <= 0 && see && dist <= w.range) {
            burstLeft = Math.max(1, w.burst);
            burstTimer = 0;
        }
    }

    private int nextCooldown(HaloWeapon w) {
        int span = Math.max(1, w.cooldownMax - w.cooldownMin + 1);
        return w.cooldownMin + mob.getRandom().nextInt(span);
    }
}
