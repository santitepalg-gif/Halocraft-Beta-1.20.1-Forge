package com.halomobs.client;

import com.halomobs.entity.HaloMobEntity;
import com.halomobs.entity.HaloMobKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class HaloMobRenderer extends GeoEntityRenderer<HaloMobEntity> {
    /** Las armas se dibujan 1.4x para que se lean bien en mobs de 2-3 bloques (igual en tools/preview_weapons.py). */
    private static final float WEAPON_SCALE = 1.4f;

    public HaloMobRenderer(EntityRendererProvider.Context ctx, HaloMobKind kind) {
        super(ctx, new HaloMobModel());
        this.shadowRadius = 0.5f * kind.scale;
        withScale(kind.scale);                         // escala igual que en el addon Bedrock
        addRenderLayer(new AutoGlowingGeoLayer<>(this)); // ojos / energia: <mob>_glowmask.png
        // Arma en la mano: el item se pinta en el hueso "rightItem" del modelo (el mismo donde el addon engancha las armas).
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable @Override
            protected ItemStack getStackForBone(GeoBone bone, HaloMobEntity mob) {
                if (!"rightItem".equals(bone.getName())) return null;
                ItemStack s = mob.getHeldWeaponStack();
                return s.isEmpty() ? null : s;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, HaloMobEntity mob) {
                return ItemDisplayContext.NONE;   // sin transformación "display": el modelo del arma ya está orientado
            }

            @Override
            protected void renderStackForBone(PoseStack ps, GeoBone bone, ItemStack stack, HaloMobEntity mob,
                                              MultiBufferSource buf, float partialTick, int light, int overlay) {
                ps.mulPose(Axis.XP.rotationDegrees(-90f));   // cañón (+Y del item) hacia delante del mob
                ps.scale(WEAPON_SCALE, WEAPON_SCALE, WEAPON_SCALE);
                super.renderStackForBone(ps, bone, stack, mob, buf, partialTick, light, overlay);
            }
        });
    }

    /** La animación "dead" ya tumba el modelo; evitamos el giro vanilla de 90°. */
    @Override
    protected float getDeathMaxRotation(HaloMobEntity animatable) { return 0f; }
}
