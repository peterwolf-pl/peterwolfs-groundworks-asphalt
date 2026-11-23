package com.piotrek.groundworksasphalt.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksasphalt.client.model.CatGraderModel;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Minecraft 26.3 entity renderer for the Cat Motor Grader (Równiarka Drogowa).
 */
public class CatGraderRenderer extends EntityRenderer<CatGraderEntity, CatGraderRenderState> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "pw_groundworks_asphalt",
            "textures/entity/cat_grader.png"
    );

    public static final ModelLayerLocation CAT_GRADER_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("pw_groundworks_asphalt", "cat_grader"),
            "main"
    );

    private final CatGraderModel model;

    public CatGraderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new CatGraderModel(context.bakeLayer(CAT_GRADER_LAYER));
        this.shadowRadius = 2.8F;
    }

    @Override
    public CatGraderRenderState createRenderState() {
        return new CatGraderRenderState();
    }

    @Override
    public void extractRenderState(CatGraderEntity entity, CatGraderRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot(partialTick);
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.steerAngle = entity.getSteerAngle();
        state.wheelLean = entity.getWheelLean();
        state.wheelSpin = entity.getWheelRotation();

        state.bladeHeight = entity.getBladeHeight();
        state.bladeAngle = entity.getBladeAngle();
        state.bladeRoll = entity.getBladeRoll();
        state.bladeShift = entity.getBladeShift();

        state.ripperLowered = entity.getRipperLowered();
        state.isEngineRunning = entity.isEngineRunning();
        state.isGrading = entity.isGrading();
        state.workLightsOn = entity.isLightsOn();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    @Override
    public void submit(
            CatGraderRenderState state,
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
            // Movement roll is atan2(leftY - rightY): positive means left side is higher.
            // ZP runs before scale(-1, -1, 1) which mirrors X, so negate it so a wheel in a hole drops that side.
            stack.rotateDegrees(Axis.ZP, -state.baseRoll);
        }

        // Standard Minecraft entity model coordinate transform
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.5F, 0.0F);

        this.model.setupAnim(state);

        collector.submitModel(
                this.model,
                state,
                stack,
                RenderTypes.entityTranslucent(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );

        stack.popPose();
    }
}
