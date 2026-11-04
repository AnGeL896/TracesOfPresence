package ru.amir.traces.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import ru.amir.traces.ApparitionEntity;
import ru.amir.traces.TracesOfPresence;

public final class ApparitionRenderer extends EntityRenderer<ApparitionEntity> {
    private static final ResourceLocation BLUE_TEXTURE = new ResourceLocation(
            TracesOfPresence.MOD_ID, "textures/entity/apparition_blue.png");
    private static final ResourceLocation GREEN_TEXTURE = new ResourceLocation(
            TracesOfPresence.MOD_ID, "textures/entity/apparition_green.png");
    private static final ResourceLocation RED_TEXTURE = new ResourceLocation(
            TracesOfPresence.MOD_ID, "textures/entity/apparition_red.png");
    private final ApparitionModel<ApparitionEntity> model;

    public ApparitionRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ApparitionModel<>(context.bakeLayer(ApparitionModel.LAYER));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ApparitionEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        float age = entity.tickCount + partialTick;
        float bob = Mth.sin(age * 0.11F) * 0.035F;
        pose.translate(0.0D, 1.5D + bob, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entity.getYRot()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        float headYaw = entity.isDeathReplay()
                ? Mth.wrapDegrees(entity.getReplayHeadYaw() - entity.getYRot())
                : 0.0F;
        float headPitch = entity.isDeathReplay() ? entity.getXRot() : 0.0F;
        model.setupAnim(entity, partialTick, 0.0F, age, headYaw, headPitch);
        int glowLight = entity.getGlowIntensity(partialTick) > 0.02F
                ? LightTexture.pack(15, 15)
                : packedLight;
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity))), glowLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, entity.getFadeAlpha(partialTick));
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ApparitionEntity entity) {
        if (entity.isDeathReplay()) return RED_TEXTURE;
        return entity.isGreenEcho() ? GREEN_TEXTURE : BLUE_TEXTURE;
    }
}
