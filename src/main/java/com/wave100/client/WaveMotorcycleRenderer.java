package com.wave100.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wave100.WaveMod;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Renderer for the Wave motorcycle.
 *
 * <p>Two render passes:</p>
 * <ol>
 *   <li><b>Main pass</b> - the full model with the diffuse texture
 *       (cutout, no cull so thin panels look right from both sides).</li>
 *   <li><b>Emissive pass</b> - re-renders only the light parts (headlight
 *       lens, tail lens, beam cones) with the lights texture at full brightness
 *       so they glow in the dark. Beam cones are drawn translucently, and the
 *       tail lens doubles as the brake light.</li>
 * </ol>
 *
 * <p>Body poses applied here (after the entity yaw): crash roll around the
 * ground line, wheelie pitch around the rear axle, lean around the ground
 * line. Part-level animation (steering, wheels, suspension, stand) is done by
 * {@link WaveAnimationController}.</p>
 */
public class WaveMotorcycleRenderer extends EntityRenderer<WaveMotorcycleEntity> {

    private static final ResourceLocation TEXTURE =
            WaveMod.id("textures/entity/wave_motorcycle.png");
    private static final ResourceLocation LIGHTS_TEXTURE =
            WaveMod.id("textures/entity/wave_motorcycle_lights.png");

    /** Rear axle in model units (wheelie pivot). */
    private static final float REAR_AXLE_Y = 4.4F / 16F;
    private static final float REAR_AXLE_Z = 10.0F / 16F;

    /** How far the bike rolls over when crashed, degrees. */
    private static final float CRASH_ROLL_DEG = 76F;

    private final WaveModel model;

    public WaveMotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new WaveModel(context.bakeLayer(WaveModel.LAYER));
        this.shadowRadius = 0.45F;
    }

    @Override
    public void render(WaveMotorcycleEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        super.render(entity, entityYaw, partialTicks, pose, buffers, packedLight);

        pose.pushPose();
        // vanilla entity renderers orient the model themselves
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        // ---- whole-body poses ----
        float crash = WaveAnimationController.crashProgress(entity, partialTicks);
        if (crash > 0.005F) {
            // fallen over: roll onto the left side, nose slightly into the ground
            pose.translate(0F, crash * 0.05F, 0F);
            pose.mulPose(Axis.ZP.rotationDegrees(-crash * CRASH_ROLL_DEG));
            pose.mulPose(Axis.XP.rotationDegrees(crash * 8F));
        } else {
            float wheelie = WaveAnimationController.wheelieRadians(entity, partialTicks);
            if (wheelie > 0.005F || wheelie < -0.005F) {
                pose.translate(0F, REAR_AXLE_Y, REAR_AXLE_Z);
                pose.mulPose(Axis.XP.rotation(wheelie));
                pose.translate(0F, -REAR_AXLE_Y, -REAR_AXLE_Z);
            }
            float lean = WaveAnimationController.leanRadians(entity, partialTicks);
            if (lean > 0.0005F || lean < -0.0005F) {
                pose.mulPose(Axis.ZP.rotation(lean));
            }
        }

        // ---- part animation ----
        WaveAnimationController.apply(this.model, entity, partialTicks);

        // ---- main pass: the whole bike ----
        VertexConsumer main = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        this.model.root().render(pose, main, packedLight, OverlayTexture.NO_OVERLAY);

        // ---- emissive pass: lights ----
        VertexConsumer emissive = buffers.getBuffer(RenderType.entityTranslucentEmissive(LIGHTS_TEXTURE));
        int fullBright = LightTexture.FULL_BRIGHT;

        boolean headlight = entity.isHeadlightOn() && !entity.isCrashed();
        if (headlight) {
            // lens + headlight beam cones
            this.model.headlightLens.render(pose, emissive, fullBright, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
            this.model.beamInner.render(pose, emissive, fullBright, OverlayTexture.NO_OVERLAY, 0xB2FFFFFF);
            this.model.beamOuter.render(pose, emissive, fullBright, OverlayTexture.NO_OVERLAY, 0x54FFFFFF);
        }

        // tail light: dim whenever the headlight is on, bright when braking
        boolean braking = entity.isBrakeLightOn();
        if (braking || headlight) {
            int tailColor = braking ? 0xFFFFFFFF : 0x90FFFFFF;
            this.model.tailLens.render(pose, emissive, fullBright, OverlayTexture.NO_OVERLAY, tailColor);
        }

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WaveMotorcycleEntity entity) {
        return TEXTURE;
    }

    @SuppressWarnings("unused")
    private static float clampDegrees(float value, float max) {
        return Mth.clamp(value, -max, max);
    }
}
