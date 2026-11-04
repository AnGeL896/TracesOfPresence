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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
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
    private final Map<Entity, Integer> killerAnimationTicks = new HashMap<>();
    private final Map<Entity, net.minecraft.world.phys.Vec3> killerPreviousPositions = new HashMap<>();

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
        if (entity.isDeathReplay()) {
            renderHeldItem(entity, entity.getReplayMainHand(), 0.34F, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    pose, buffers, glowLight);
            renderHeldItem(entity, entity.getReplayOffHand(), -0.34F, ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                    pose, buffers, glowLight);
        }
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
            if (visual instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setAggressive(true);
            }
            if (visual instanceof Skeleton skeleton) {
                skeleton.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                skeleton.startUsingItem(InteractionHand.MAIN_HAND);
            }
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
        visual.tickCount = echo.tickCount;
        if (visual instanceof LivingEntity living) {
            if (visual instanceof Mob mob) mob.setBaby(echo.isKillerBaby());
            living.setItemSlot(EquipmentSlot.MAINHAND, stackFor(echo.getKillerMainHand()));
            living.setItemSlot(EquipmentSlot.OFFHAND, stackFor(echo.getKillerOffHand()));
            living.setItemSlot(EquipmentSlot.HEAD, stackFor(echo.getKillerHead()));
            living.setItemSlot(EquipmentSlot.CHEST, stackFor(echo.getKillerChest()));
            living.setItemSlot(EquipmentSlot.LEGS, stackFor(echo.getKillerLegs()));
            living.setItemSlot(EquipmentSlot.FEET, stackFor(echo.getKillerFeet()));
            if (visual instanceof Skeleton skeleton && living.getMainHandItem().is(Items.BOW)) {
                skeleton.startUsingItem(InteractionHand.MAIN_HAND);
            }
            living.setYHeadRot(yaw);
            living.yHeadRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
            living.swinging = echo.isKillerSwinging();
            living.swingTime = echo.getKillerSwingTime();
            living.swingingArm = InteractionHand.MAIN_HAND;
            living.attackAnim = echo.getKillerAttackAnim();
            living.oAttackAnim = echo.getKillerAttackAnim();
            if (killerAnimationTicks.getOrDefault(visual, Integer.MIN_VALUE) != echo.tickCount) {
                net.minecraft.world.phys.Vec3 current = new net.minecraft.world.phys.Vec3(x, y, z);
                net.minecraft.world.phys.Vec3 previous = killerPreviousPositions.put(visual, current);
                double distance = previous == null ? 0.0D : current.distanceTo(previous);
                float gait = Mth.clamp((float) (distance * 4.0D), 0.0F, 1.0F);
                living.walkAnimation.update(gait, (float) (distance * 1.8D));
                killerAnimationTicks.put(visual, echo.tickCount);
            }
        }
        float scale = echo.isCreeperDeath() && finalCue
                ? 0.75F + 0.30F * Mth.clamp(cueTime / 38.0F, 0.0F, 1.0F) : 1.0F;
        pose.pushPose();
        pose.translate(x - echo.getX(), y - echo.getY(), z - echo.getZ());
        float cueFade = cueTime < 40.0F ? 0.0F : smoothStep(Mth.clamp((cueTime - 40.0F) / 30.0F, 0.0F, 1.0F));
        pose.scale(scale * (1.0F - cueFade), scale * (1.0F - cueFade), scale * (1.0F - cueFade));
        if (cueFade > 0.0F) {
            pose.translate(0.0D, 0.85D, 0.0D);
            pose.mulPose(Axis.ZP.rotation(cueFade * 1.25F));
            pose.translate(0.0D, -0.85D, 0.0D);
        }
        if (visual instanceof LivingEntity living) living.setInvisible(false);
        Minecraft.getInstance().getEntityRenderDispatcher().render(visual, 0.0D, 0.0D, 0.0D,
                yaw, partialTick, pose, buffers, packedLight);
        if (visual instanceof LivingEntity living && cueFade > 0.0F) {
            renderSpectralKiller(living, partialTick, yaw, cueFade, pose, buffers, packedLight);
        }
        pose.popPose();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void renderSpectralKiller(LivingEntity living, float partialTick, float yaw, float fade,
                                             PoseStack pose, MultiBufferSource buffers, int packedLight) {
        var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(living);
        if (!(renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer)) return;
        var mobModel = livingRenderer.getModel();
        float alpha = 1.0F - fade;
        pose.pushPose();
        pose.translate(0.0D, 1.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.scale(-1.0F, -1.0F, 1.0F);
        float age = living.tickCount + partialTick;
        mobModel.setupAnim(living, 0.0F, 0.0F, age, 0.0F, 0.0F);
        ResourceLocation texture = livingRenderer.getTextureLocation(living);
        mobModel.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(texture)),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.78F, 0.10F, 0.075F, alpha);
        pose.popPose();
    }

    private static float smoothStep(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static ItemStack stackFor(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR));
    }

    private static void renderHeldItem(ApparitionEntity entity, String itemId, float side,
                                       ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int packedLight) {
        ItemStack stack = stackFor(itemId);
        if (stack.isEmpty()) return;
        pose.pushPose();
        pose.translate(side, 0.82D, 0.02D);
        // THIRD_PERSON hand contexts provide the correct tool/block transform themselves.
        pose.scale(0.48F, 0.48F, 0.48F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, context, packedLight,
                OverlayTexture.NO_OVERLAY, pose, buffers, entity.level(), entity.getId());
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ApparitionEntity entity) {
        if (entity.isDeathReplay()) return RED_TEXTURE;
        return entity.isGreenEcho() ? GREEN_TEXTURE : BLUE_TEXTURE;
    }
}
