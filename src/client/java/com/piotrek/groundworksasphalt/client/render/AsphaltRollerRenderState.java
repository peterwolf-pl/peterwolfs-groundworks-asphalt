package com.piotrek.groundworksasphalt.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state for the 14T Tandem Vibratory Asphalt Roller (Bomag BW 184 AD style).
 */
public class AsphaltRollerRenderState extends EntityRenderState {
    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    public float steerAngle;
    public float forwardSpeed;
    public float frontDrumRotation;
    public float rearDrumRotation;

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
