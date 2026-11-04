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
        float look = Mth.sin(ageInTicks * 0.12F) * 0.48F;
        head.yRot = netHeadYaw * ((float) Math.PI / 180.0F) + look;
        head.xRot = headPitch * ((float) Math.PI / 180.0F) + Mth.sin(ageInTicks * 0.08F) * 0.10F;
        float walking = entity instanceof ApparitionEntity apparition && apparition.isWalking() ? 1.0F : 0.0F;
        float sway = Mth.sin(ageInTicks * 0.09F) * 0.07F;
        float step = Mth.sin(ageInTicks * 0.62F) * walking;
        head.zRot = Mth.sin(ageInTicks * 0.07F) * 0.035F;
        body.zRot = sway;
        body.xRot = -0.08F * walking;
        leftArm.xRot = -0.18F - step * 0.42F + Mth.sin(ageInTicks * 0.18F) * 0.10F;
        rightArm.xRot = -0.12F + step * 0.42F + Mth.sin(ageInTicks * 0.18F + 1.2F) * 0.10F;
        leftArm.zRot = 0.10F + sway;
        rightArm.zRot = -0.10F + sway;
        leftLeg.xRot = step * 0.52F;
        rightLeg.xRot = -leftLeg.xRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0.72F, 0.88F, 1.0F, alpha);
    }
}
