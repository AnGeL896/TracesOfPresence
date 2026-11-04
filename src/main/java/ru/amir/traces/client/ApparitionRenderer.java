package ru.amir.traces.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<String, Entity> killerVisuals = new HashMap<>();

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
        renderKiller(entity, partialTick, pose, buffers, packedLight);
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private void renderKiller(ApparitionEntity echo, float partialTick, PoseStack pose,
                              MultiBufferSource buffers, int packedLight) {
        float cueTime = echo.getDeathCueTime(partialTick);
        String typeId = echo.getKillerType();
        boolean finalCue = cueTime >= 0.0F && cueTime < 40.0F;
        if (typeId.isEmpty() || (!finalCue && !echo.isKillerVisible())) return;
        Entity visual = killerVisuals.get(typeId);
        if (visual == null) {
            ResourceLocation id = ResourceLocation.tryParse(typeId);
            EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null || type == EntityType.PLAYER || type == ru.amir.traces.ModEntities.APPARITION.get()) return;
            visual = type.create(echo.level());
            if (visual == null) return;
            visual.setNoGravity(true);
            visual.setSilent(true);
            visual.setInvulnerable(true);
            if (visual instanceof Mob mob) mob.setNoAi(true);
            killerVisuals.put(typeId, visual);
        }
        float x = echo.getKillerX(), y = echo.getKillerY(), z = echo.getKillerZ();
        double toEchoX = echo.getX() - x;
        double toEchoZ = echo.getZ() - z;
        float yaw = Math.abs(toEchoX) + Math.abs(toEchoZ) < 0.001D
                ? echo.getKillerYaw()
                : (float) Math.toDegrees(Math.atan2(-toEchoX, toEchoZ));
        visual.setPos(x, y, z);
        visual.setYRot(yaw);
        visual.yRotO = yaw;
        if (visual instanceof LivingEntity living) {
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
        }
        float scale = echo.isCreeperDeath() && finalCue
                ? 0.75F + 0.30F * Mth.clamp(cueTime / 38.0F, 0.0F, 1.0F) : 1.0F;
        pose.pushPose();
        pose.translate(x - echo.getX(), y - echo.getY(), z - echo.getZ());
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getEntityRenderDispatcher().render(visual, 0.0D, 0.0D, 0.0D,
                yaw, partialTick, pose, buffers, packedLight);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ApparitionEntity entity) {
        if (entity.isDeathReplay()) return RED_TEXTURE;
        return entity.isGreenEcho() ? GREEN_TEXTURE : BLUE_TEXTURE;
    }
}
