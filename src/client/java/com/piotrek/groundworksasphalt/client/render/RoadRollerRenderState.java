package com.piotrek.groundworksasphalt.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state for the 16T Heavy Road Roller / Asphalt Compactor.
 */
public class RoadRollerRenderState extends EntityRenderState {
    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    public float steerAngle;
    public float forwardSpeed;
    public float wheelRotation;
    public float drumRotation;

    public boolean isCompacting;
    public boolean isVibrating;
    public float vibrationAmplitude = 0.35F;

    public boolean engineRunning = true;
    public boolean braking;
    public boolean waterSpraying;
    public boolean workLightsOn;

    public float beaconSpin;
    public boolean beaconFlash;

    public float renderScale = 1.0F;
}
