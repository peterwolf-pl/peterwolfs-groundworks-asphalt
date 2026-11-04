package com.piotrek.groundworksasphalt.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Client-side render state for the tracked heavy asphalt paver (Rozściełacz Asfaltu).
 */
public class AsphaltPaverRenderState extends EntityRenderState {

    public float baseYaw;
    public float basePitch;
    public float baseRoll;

    public float leftTrackSpeed;
    public float rightTrackSpeed;
    public float leftTrackTravel;
    public float rightTrackTravel;

    /**
     * Fold state of the hopper wings (0.0F = flat/open for truck loading, 1.0F = folded inward).
     */
    public float hopperFold;

    /**
     * Height position of the rear floating screed (0.0F = lowered on ground paving, 1.0F = raised for transport).
     */
    public float screedHeight;

    /**
     * Quantity of hot asphalt mix inside the hopper (0.0F to 1.0F).
     */
    public float asphaltFillRatio;

    public boolean isPaving;
    public boolean isEngineRunning;
    public boolean workLightsOn;

    public float beaconSpin;
    public boolean beaconFlash;
}
