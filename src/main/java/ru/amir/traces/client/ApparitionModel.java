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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.texture.OverlayTexture;
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

    @Override public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                    float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float) Math.PI / 180.0F);
        head.xRot = headPitch * ((float) Math.PI / 180.0F);
        float dissolve = Mth.clamp((ageInTicks - 100.0F) / 60.0F, 0.0F, 1.0F);
        float sway = Mth.sin(ageInTicks * 0.045F) * (0.035F + dissolve * 0.025F);
        head.zRot = Mth.sin(ageInTicks * 0.03F) * 0.018F;
        body.zRot = sway;
        leftArm.zRot = 0.06F + sway * 0.8F;
        rightArm.zRot = -0.06F + sway * 0.8F;
        leftLeg.xRot = Mth.sin(ageInTicks * 0.035F) * 0.018F;
        rightLeg.xRot = -leftLeg.xRot;
    }

    @Override public void renderToBuffer(PoseStack pose, VertexConsumer consumer, int packedLight, int packedOverlay,
                                         float red, float green, float blue, float alpha) {
        root.render(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0.72F, 0.88F, 1.0F, alpha);
    }
}
