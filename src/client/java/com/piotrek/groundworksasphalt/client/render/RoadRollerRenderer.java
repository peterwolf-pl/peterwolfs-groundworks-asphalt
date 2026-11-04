package com.piotrek.groundworksasphalt.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksasphalt.client.model.RoadRollerModel;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Minecraft 26.3 entity renderer for the 16T Heavy Road Roller / Asphalt Compactor.
 */
public class RoadRollerRenderer extends EntityRenderer<RoadRollerEntity, RoadRollerRenderState> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "pw_groundworks_asphalt",
            "textures/entity/road_roller.png"
    );

    public static final ModelLayerLocation ROAD_ROLLER_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("pw_groundworks_asphalt", "road_roller"),
            "main"
    );

    private final RoadRollerModel model;

    public RoadRollerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RoadRollerModel(context.bakeLayer(ROAD_ROLLER_LAYER));
        this.shadowRadius = 2.4F;
    }

    @Override
    public RoadRollerRenderState createRenderState() {
        return new RoadRollerRenderState();
    }

    @Override
    public void extractRenderState(RoadRollerEntity entity, RoadRollerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot(partialTick);
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.steerAngle = entity.getSteerAngle();
        state.forwardSpeed = entity.getForwardSpeed();
        state.wheelRotation = entity.getWheelRotation();
        state.drumRotation = entity.getDrumRotation();

        state.isCompacting = entity.isCompacting();
        state.isVibrating = entity.isVibrating();
        state.engineRunning = entity.isEngineRunning();
        state.braking = entity.isBraking();
        state.waterSpraying = entity.isWaterSpraying();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.engineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    @Override
    public void submit(
            RoadRollerRenderState state,
            PoseStack stack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        stack.pushPose();

        stack.rotateDegrees(Axis.YP, -state.baseYaw);

        if (Math.abs(state.basePitch) > 0.01F) {
            stack.rotateDegrees(Axis.XP, state.basePitch);
        }
        if (Math.abs(state.baseRoll) > 0.01F) {
            stack.rotateDegrees(Axis.ZP, state.baseRoll);
        }

        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);

        model.setupAnim(state);

        collector.submitModel(
                model,
                state,
                stack,
                RenderTypes.entityCutout(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );

        stack.popPose();
    }
}
