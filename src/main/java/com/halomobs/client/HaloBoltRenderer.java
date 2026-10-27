package com.halomobs.client;

import com.halomobs.HaloMod;
import com.halomobs.entity.HaloBoltEntity;
import com.halomobs.entity.HaloWeapon;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Dibuja el proyectil como un destello (billboard) teñido con el color del arma, siempre a máxima luz. */
public class HaloBoltRenderer extends EntityRenderer<HaloBoltEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(HaloMod.MODID, "textures/entity/bolt.png");
    private static final int FULL_BRIGHT = 15728880;

    public HaloBoltRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    @Override
    public void render(HaloBoltEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        HaloWeapon w = e.getWeapon();
        int c = w.color;
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        ps.pushPose();
        float s = w.boltSize * 2f;
        ps.scale(s, s, s);
        ps.mulPose(this.entityRenderDispatcher.cameraOrientation());
        ps.mulPose(Axis.YP.rotationDegrees(180f));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
        vertex(vc, m, n, -0.5f, -0.5f, 0, 1, r, g, b);
        vertex(vc, m, n,  0.5f, -0.5f, 1, 1, r, g, b);
        vertex(vc, m, n,  0.5f,  0.5f, 1, 0, r, g, b);
        vertex(vc, m, n, -0.5f,  0.5f, 0, 0, r, g, b);
        ps.popPose();
        super.render(e, yaw, partial, ps, buf, light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v, int r, int g, int b) {
        vc.vertex(m, x, y, 0f).color(r, g, b, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
          .uv2(FULL_BRIGHT).normal(n, 0f, 1f, 0f).endVertex();
    }

    @Override public ResourceLocation getTextureLocation(HaloBoltEntity e) { return TEX; }
}
