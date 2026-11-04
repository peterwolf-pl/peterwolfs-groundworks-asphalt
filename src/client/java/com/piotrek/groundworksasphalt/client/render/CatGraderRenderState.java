package com.piotrek.groundworksasphalt.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Client-side render state for the 6-wheeled Cat Motor Grader (Równiarka Drogowa).
 */
public class CatGraderRenderState extends EntityRenderState {

    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    // Wheel rotation and steering
    public float wheelSpin;
    public float steerAngle;
    public float wheelLean;

    // Moldboard / Blade articulation
    public float bladeHeight;
    public float bladeAngle;
    public float bladeRoll;
    public float bladeShift;

    // Rear ripper
    public float ripperLowered;

    // Machine operating states
    public boolean isEngineRunning;
    public boolean isGrading;
    public boolean workLightsOn;

    // Safety beacon
    public float beaconSpin;
    public boolean beaconFlash;
}
