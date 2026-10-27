package com.halomobs.item;

import com.halomobs.entity.HaloBoltEntity;
import com.halomobs.entity.HaloWeapon;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Arma de fuego/plasma para el jugador: clic derecho dispara un proyectil (munición infinita). */
public class HaloWeaponItem extends Item {
    private final HaloWeapon weapon;

    public HaloWeaponItem(Properties props, HaloWeapon weapon) {
        super(props);
        this.weapon = weapon;
    }

    public HaloWeapon getWeapon() { return weapon; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            for (int i = 0; i < weapon.pellets; i++) {
                HaloBoltEntity bolt = new HaloBoltEntity(level, player, weapon);
                bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, weapon.speed, weapon.inaccuracy * 0.4f);
                level.addFreshEntity(bolt);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), weapon.sound, SoundSource.PLAYERS, 0.8f, weapon.soundPitch);
        }
        player.getCooldowns().addCooldown(this, Math.max(6, weapon.burstDelay * Math.max(1, weapon.burst / 2) + 6));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
