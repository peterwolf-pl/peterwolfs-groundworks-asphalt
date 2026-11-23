package com.piotrek.groundworksasphalt.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piotrek.groundworksasphalt.client.model.AsphaltPaverModel;
import com.piotrek.groundworksasphalt.entity.AsphaltPaverEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Minecraft 26.3 entity renderer for the heavy tracked Asphalt Paver (Rozściełacz Asfaltu).
 */
public class AsphaltPaverRenderer extends EntityRenderer<AsphaltPaverEntity, AsphaltPaverRenderState> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "pw_groundworks_asphalt",
            "textures/entity/asphalt_paver.png"
    );

    public static final ModelLayerLocation ASPHALT_PAVER_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("pw_groundworks_asphalt", "asphalt_paver"),
            "main"
    );

    private final AsphaltPaverModel model;
    private final Map<AsphaltPaverEntity, TrackAnimationState> trackAnimations = new WeakHashMap<>();

    private static final class TrackAnimationState {
        float lastRenderTick = Float.NaN;
        float leftTravel;
        float rightTravel;
    }

    public AsphaltPaverRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new AsphaltPaverModel(context.bakeLayer(ASPHALT_PAVER_LAYER));
        this.shadowRadius = 2.8F;
    }

    @Override
    public AsphaltPaverRenderState createRenderState() {
        return new AsphaltPaverRenderState();
    }

    @Override
    public void extractRenderState(AsphaltPaverEntity entity, AsphaltPaverRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);

        state.baseYaw = entity.getYRot(partialTick);
        state.basePitch = entity.getVehiclePitch();
        state.baseRoll = entity.getVehicleRoll();

        state.hopperFold = entity.getHopperFold();
        state.screedHeight = entity.getScreedHeight();
        state.asphaltFillRatio = entity.getAsphaltFillRatio();

        state.leftTrackSpeed = entity.getTrackLeftSpeed();
        state.rightTrackSpeed = entity.getTrackRightSpeed();

        // Continuous crawler track travel integration on client
        TrackAnimationState trackAnimation =
                this.trackAnimations.computeIfAbsent(entity, ignored -> new TrackAnimationState());
        float renderTick = entity.tickCount + partialTick;
        if (Float.isNaN(trackAnimation.lastRenderTick)) {
            trackAnimation.lastRenderTick = renderTick;
        } else {
            float deltaTicks = Math.max(0.0F, Math.min(5.0F, renderTick - trackAnimation.lastRenderTick));
            trackAnimation.leftTravel += state.leftTrackSpeed * 14.0F * deltaTicks;
            trackAnimation.rightTravel += state.rightTrackSpeed * 14.0F * deltaTicks;
            trackAnimation.leftTravel = wrapTrackTravel(trackAnimation.leftTravel);
            trackAnimation.rightTravel = wrapTrackTravel(trackAnimation.rightTravel);
            trackAnimation.lastRenderTick = renderTick;
        }
        state.leftTrackTravel = trackAnimation.leftTravel;
        state.rightTrackTravel = trackAnimation.rightTravel;

        state.isPaving = entity.isPaving();
        state.isEngineRunning = entity.isEngineRunning();
        state.workLightsOn = entity.isWorkLightsOn();

        state.beaconSpin = (entity.tickCount + partialTick) * 0.75F;
        state.beaconFlash = state.isEngineRunning && ((entity.tickCount / 4) % 2 == 0);
    }

    private static float wrapTrackTravel(float travel) {
        float loop = 360.0F;
        if (travel > loop || travel < -loop) {
            travel %= loop;
        }
        return travel;
    }

    @Override
    public void submit(
            AsphaltPaverRenderState state,
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
            stack.rotateDegrees(Axis.ZP, -state.baseRoll);
        }

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
