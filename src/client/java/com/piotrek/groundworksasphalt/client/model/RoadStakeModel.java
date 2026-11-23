package com.piotrek.groundworksasphalt.client.model;

import com.piotrek.groundworksasphalt.client.render.RoadStakeRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class RoadStakeModel extends EntityModel<RoadStakeRenderState> {

    private final ModelPart flag;

    public RoadStakeModel(ModelPart root) {
        super(root);
        this.flag = root.getChild("flag");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Wooden survey peg / pole
        root.addOrReplaceChild(
                "pole",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -28.0F, -1.0F, 2.0F, 28.0F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        // Yellow/orange surveyor ribbon / flag
        root.addOrReplaceChild(
                "flag",
                CubeListBuilder.create().texOffs(0, 0).addBox(1.0F, -26.0F, -0.5F, 10.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        // Direction pointer arrow shaft pointing forward
        root.addOrReplaceChild(
                "pointer",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -30.0F, -8.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        // Arrowhead tip
        root.addOrReplaceChild(
                "arrow_tip",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -30.0F, -11.0F, 5.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        // Transverse crossbar showing road width axis
        root.addOrReplaceChild(
                "crossbar",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -29.0F, -1.0F, 12.0F, 1.5F, 2.0F),
                PartPose.offset(0.0F, 24.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(RoadStakeRenderState state) {
        this.flag.zRot = Mth.sin(state.age * 0.18F) * 0.12F;
    }
}
