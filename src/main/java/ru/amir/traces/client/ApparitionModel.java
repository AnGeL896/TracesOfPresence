package ru.amir.traces.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import ru.amir.traces.ApparitionEntity;
import ru.amir.traces.TracesOfPresence;

public final class ApparitionModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(TracesOfPresence.MOD_ID, "apparition"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private boolean deathReplay;

    public ApparitionModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        ApparitionEntity apparition = entity instanceof ApparitionEntity echo ? echo : null;
        deathReplay = apparition != null && apparition.isDeathReplay();
        float idleLook = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.045F) * 0.27F;
        head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) + idleLook;
        float idlePitch = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.035F) * 0.06F;
        head.xRot = headPitch * ((float) Math.PI / 180.0F) + idlePitch;
        float walking = apparition != null ? apparition.getWalkBlend(limbSwing) : 0.0F;
        boolean sprinting = deathReplay && apparition.isReplaySprinting();
        float sway = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.055F) * 0.04F;
        float gaitPhase = deathReplay
                ? apparition.getReplayLimbPhase(limbSwing) * 0.6662F
                : ageInTicks * 0.28F;
        float step = Mth.sin(gaitPhase) * walking;
        float idleArmDrift = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.07F) * 0.035F;
        head.zRot = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.04F) * 0.02F;
        body.zRot = sway;
        boolean crouching = deathReplay && apparition.isReplayCrouching();
        body.xRot = crouching ? 0.22F : (sprinting ? -0.10F : -0.035F * walking);
        body.y = crouching ? 1.5F : 0.0F;
        head.y = crouching ? -1.0F : 0.0F;
        float armSwing = sprinting ? 0.28F : 0.24F;
        float secondaryArmDrift = deathReplay ? 0.0F : Mth.sin(ageInTicks * 0.07F + 1.2F) * 0.035F;
        leftArm.xRot = -0.10F - step * armSwing + idleArmDrift;
        rightArm.xRot = -0.08F + step * armSwing + secondaryArmDrift;
        leftArm.zRot = 0.08F + sway;
        rightArm.zRot = -0.08F + sway;
        leftLeg.xRot = step * 0.34F;
        rightLeg.xRot = -leftLeg.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                deathReplay ? 1.0F : 0.72F, deathReplay ? 0.30F : 0.88F, deathReplay ? 0.24F : 1.0F, alpha);
    }
}
