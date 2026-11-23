package com.piotrek.groundworksasphalt.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksasphalt.client.model.RoadStakeModel;
import com.piotrek.groundworksasphalt.entity.RoadStakeEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class RoadStakeRenderer extends EntityRenderer<RoadStakeEntity, RoadStakeRenderState> {

    public static final ModelLayerLocation ROAD_STAKE_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath("pw_groundworks_asphalt", "road_stake"), "main");

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/yellow_concrete.png");

    private final RoadStakeModel model;

    public RoadStakeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RoadStakeModel(context.bakeLayer(ROAD_STAKE_LAYER));
    }

    @Override
    public RoadStakeRenderState createRenderState() {
        return new RoadStakeRenderState();
    }

    @Override
    public void extractRenderState(RoadStakeEntity entity, RoadStakeRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = entity.getYaw();
        state.age = entity.tickCount + partialTick;
        state.corridorWidth = entity.getCorridorWidth();
        state.crownPercent = entity.getCrownPercent();
    }

    @Override
    public void submit(RoadStakeRenderState state, PoseStack stack, SubmitNodeCollector collector, CameraRenderState camera) {
        stack.pushPose();
        stack.rotateDegrees(Axis.YP, 180.0F - state.yaw);
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);
        this.model.setupAnim(state);
        collector.submitModel(
                this.model,
                state,
                stack,
                RenderTypes.entityCutout(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );
        stack.popPose();
        super.submit(state, stack, collector, camera);
    }
}
