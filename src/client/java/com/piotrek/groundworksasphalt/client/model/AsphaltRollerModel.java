package com.piotrek.groundworksasphalt.client.model;

import com.piotrek.groundworksasphalt.client.render.AsphaltRollerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Photorealistic 14-Ton Tandem Vibratory Asphalt Roller Model (Bomag BW 184 AD / Hamm style).
 * <p>
 * Features:
 * <ul>
 *   <li>Dual massive vibrating cylindrical steel drums (front and rear) rotating with speed</li>
 *   <li>Front and rear heavy articulated fork yokes with water sprinkler spray manifolds</li>
 *   <li>Right-side asphalt edge cutter disc wheel on front drum yoke</li>
 *   <li>Central articulated pivot steering joint (up to 35° steering)</li>
 *   <li>Panoramic ROPS/FOPS operator cab with transparent glass, wipers, mirrors, seat, and console</li>
 *   <li>Side boarding ladder with 3 non-slip step treads</li>
 *   <li>Emergency rotating amber beacon on cab roof</li>
 * </ul>
 */
public class AsphaltRollerModel extends EntityModel<AsphaltRollerRenderState> {

    private final ModelPart centerChassis;
    private final ModelPart frontAssembly;
    private final ModelPart rearAssembly;
    private final ModelPart frontDrum;
    private final ModelPart rearDrum;
    private final ModelPart beacon;
    private final ModelPart wiper;

    public AsphaltRollerModel(ModelPart root) {
        super(root);
        this.centerChassis = root.getChild("center_chassis");
        this.frontAssembly = root.getChild("front_assembly");
        this.rearAssembly = this.centerChassis.getChild("rear_assembly");
        this.frontDrum = this.frontAssembly.getChild("front_drum");
        this.rearDrum = this.rearAssembly.getChild("rear_drum");
        this.beacon = this.centerChassis.getChild("cab").getChild("beacon");
        this.wiper = this.centerChassis.getChild("cab").getChild("wiper");
    }

    @Override
    public void setupAnim(AsphaltRollerRenderState state) {
        super.setupAnim(state);

        // 1. Central Articulated Steering (front assembly pivots on Y)
        this.frontAssembly.yRot = state.steerAngle;

        // 2. Drive Rotation (front drum and rear drum)
        this.frontDrum.xRot = -state.frontDrumRotation;
        this.rearDrum.xRot = -state.rearDrumRotation;

        // 3. Drums stay rock-steady (no oscillation/vibration jitter)
        this.frontDrum.y = 8.0F;
        this.frontDrum.z = 26.0F;
        this.rearDrum.y = 8.0F;
        this.rearDrum.z = -26.0F;

        // 4. Rotating Amber Strobe Beacon
        this.beacon.yRot = state.beaconSpin;

        // 5. Windshield Wiper
        if (state.waterSpraying) {
            this.wiper.zRot = (float) Math.sin(state.ageInTicks * 0.35F) * 0.55F;
        } else {
            this.wiper.zRot = 0.0F;
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── PART 1: CENTER CHASSIS & ENGINE HOUSING ───────────────────────
        PartDefinition centerChassis = root.addOrReplaceChild("center_chassis", CubeListBuilder.create()
                        // Central chassis left longitudinal frame beam
                        .texOffs(2, 2).addBox(-11.0F, -2.0F, -18.0F, 5.0F, 8.0F, 26.0F)
                        // Central chassis right longitudinal frame beam
                        .texOffs(102, 2).addBox(6.0F, -2.0F, -18.0F, 5.0F, 8.0F, 26.0F)
                        // Central engine lower compartment belly pan
                        .texOffs(190, 214).addBox(-6.0F, -1.9F, -16.0F, 12.0F, 7.8F, 22.0F)
                        // Front articulation hitch upper mounting lug
                        .texOffs(238, 234).addBox(-3.0F, -1.0F, 7.8F, 6.0F, 4.0F, 4.0F)
                        // Front articulation hitch lower mounting lug
                        .texOffs(254, 234).addBox(-3.0F, 3.5F, 7.8F, 6.0F, 4.0F, 4.0F)
                        // Articulation kingpin pivot pin
                        .texOffs(402, 234).addBox(-1.0F, -2.0F, 9.0F, 2.0F, 11.0F, 2.0F)
                        // Left hydraulic steering cylinder barrel
                        .texOffs(304, 234).addBox(-9.5F, 1.0F, 2.0F, 3.0F, 3.0F, 8.0F)
                        // Right hydraulic steering cylinder barrel
                        .texOffs(358, 234).addBox(6.5F, 1.0F, 2.0F, 3.0F, 3.0F, 8.0F)
                        // Engine hood / central equipment cover
                        .texOffs(82, 107).addBox(-11.5F, -14.0F, -17.5F, 23.0F, 12.0F, 24.0F)
                        // Left radiator intake louver panel (see Photo 1)
                        .texOffs(452, 152).addBox(-11.6F, -12.0F, -12.0F, 0.6F, 9.0F, 15.0F)
                        // Right radiator intake louver panel
                        .texOffs(2, 188).addBox(11.0F, -12.0F, -12.0F, 0.6F, 9.0F, 15.0F)
                        // Exhaust muffler canister & vertical stack
                        .texOffs(58, 250).addBox(8.0F, -22.0F, -4.0F, 2.5F, 8.0F, 2.5F)
                        .texOffs(402, 234).addBox(8.5F, -30.0F, -3.5F, 1.5F, 8.0F, 1.5F)
                ,
                PartPose.ZERO
        );

        // ── PART 2: OPERATOR CAB ──────────────────────────────────────────
        PartDefinition cab = centerChassis.addOrReplaceChild("cab", CubeListBuilder.create()
                        // Cab subframe platform floor
                        .texOffs(356, 152).addBox(-13.0F, -13.5F, -11.0F, 26.0F, 2.0F, 20.0F)
                        // Left access staircase spine (3 steps - see Photo 1 & 2)
                        .texOffs(350, 188).addBox(-17.0F, -8.0F, -4.0F, 4.0F, 12.0F, 6.0F)
                        .texOffs(456, 234).addBox(-17.5F, -8.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        .texOffs(484, 234).addBox(-17.5F, -3.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        .texOffs(456, 234).addBox(-17.5F, 2.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        // Left stair safety handrails
                        .texOffs(420, 214).addBox(-18.0F, -18.0F, -5.0F, 0.7F, 11.0F, 1.5F)
                        .texOffs(428, 214).addBox(-18.0F, -18.0F, 2.5F, 0.7F, 11.0F, 1.5F)
                        // Right access ladder steps
                        .texOffs(372, 188).addBox(13.0F, -8.0F, -4.0F, 4.0F, 12.0F, 6.0F)
                        .texOffs(2, 250).addBox(13.0F, -8.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        .texOffs(30, 250).addBox(13.0F, -3.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        .texOffs(2, 250).addBox(13.0F, 2.0F, -5.0F, 4.5F, 1.0F, 8.0F)
                        // Right stair safety handrails
                        .texOffs(436, 214).addBox(17.3F, -18.0F, -5.0F, 0.7F, 11.0F, 1.5F)
                        .texOffs(444, 214).addBox(17.3F, -18.0F, 2.5F, 0.7F, 11.0F, 1.5F)
                        // Front cab lower sill
                        .texOffs(48, 263).addBox(-12.5F, -16.0F, 7.5F, 25.0F, 2.5F, 1.5F)
                        // Rear cab lower sill
                        .texOffs(106, 263).addBox(-12.5F, -16.0F, -11.0F, 25.0F, 2.5F, 1.5F)
                        // Left & Right door sills
                        .texOffs(270, 188).addBox(-12.5F, -16.0F, -9.5F, 1.5F, 2.5F, 17.0F)
                        .texOffs(310, 188).addBox(11.0F, -16.0F, -9.5F, 1.5F, 2.5F, 17.0F)
                        // ROPS corner pillars (front-left, front-right, rear-left, rear-right)
                        .texOffs(114, 188).addBox(-12.5F, -39.0F, 7.2F, 2.0F, 23.0F, 1.8F)
                        .texOffs(124, 188).addBox(10.5F, -39.0F, 7.2F, 2.0F, 23.0F, 1.8F)
                        .texOffs(134, 188).addBox(-12.5F, -39.0F, -11.0F, 2.0F, 23.0F, 1.8F)
                        .texOffs(144, 188).addBox(10.5F, -39.0F, -11.0F, 2.0F, 23.0F, 1.8F)
                        // Transparent panoramic tinted glass
                        // Front windshield glass
                        .texOffs(174, 188).addBox(-10.5F, -38.8F, 7.8F, 21.0F, 22.7F, 0.8F)
                        // Rear window glass
                        .texOffs(222, 188).addBox(-10.5F, -38.8F, -10.8F, 21.0F, 22.7F, 0.8F)
                        // Left side window glass
                        .texOffs(218, 152).addBox(-12.3F, -38.8F, -8.5F, 0.8F, 22.7F, 15.5F)
                        // Right side window glass
                        .texOffs(236, 152).addBox(11.5F, -38.8F, -8.5F, 0.8F, 22.7F, 15.5F)
                        // Dark aerodynamically curved cab roof
                        .texOffs(254, 152).addBox(-13.5F, -42.0F, -12.0F, 27.0F, 3.0F, 22.0F)
                        // Roof climate HVAC pod
                        .texOffs(394, 188).addBox(-8.0F, -45.0F, -6.0F, 16.0F, 3.0F, 12.0F)
                        // Front roof LED working lights
                        .texOffs(280, 263).addBox(-10.0F, -41.5F, 10.0F, 6.0F, 2.0F, 1.0F)
                        .texOffs(298, 263).addBox(4.0F, -41.5F, 10.0F, 6.0F, 2.0F, 1.0F)
                        // Rear roof LED working lights
                        .texOffs(92, 274).addBox(-10.0F, -41.5F, -12.8F, 6.0F, 2.0F, 0.8F)
                        .texOffs(108, 274).addBox(4.0F, -41.5F, -12.8F, 6.0F, 2.0F, 0.8F)
                        // Dual external rear-view mirrors on stalk brackets
                        .texOffs(330, 263).addBox(-16.0F, -36.5F, 7.5F, 3.5F, 1.5F, 1.5F)
                        .texOffs(378, 234).addBox(-18.0F, -39.0F, 7.0F, 2.0F, 7.0F, 2.5F)
                        .texOffs(344, 263).addBox(12.5F, -36.5F, 7.5F, 3.5F, 1.5F, 1.5F)
                        .texOffs(390, 234).addBox(16.0F, -39.0F, 7.0F, 2.0F, 7.0F, 2.5F)
                        // Operator seat & steering console
                        .texOffs(208, 234).addBox(-3.5F, -15.0F, -4.0F, 7.0F, 1.5F, 7.0F)
                        .texOffs(270, 234).addBox(-4.0F, -17.5F, -4.5F, 8.0F, 2.5F, 8.0F)
                        .texOffs(368, 214).addBox(-3.5F, -27.0F, -4.5F, 7.0F, 9.5F, 2.5F)
                        .texOffs(14, 274).addBox(-4.0F, -19.0F, 3.0F, 8.0F, 5.5F, 3.5F)
                        .texOffs(38, 274).addBox(-3.0F, -20.5F, 4.0F, 6.0F, 1.5F, 6.0F)
                ,
                PartPose.ZERO
        );

        // Rotating Amber Strobe Beacon
        cab.addOrReplaceChild("beacon", CubeListBuilder.create()
                        .texOffs(316, 263).addBox(-1.5F, -47.0F, -1.5F, 3.0F, 2.0F, 3.0F)
                ,
                PartPose.ZERO
        );

        // Windshield Wiper
        cab.addOrReplaceChild("wiper", CubeListBuilder.create()
                        .texOffs(362, 274).addBox(-0.5F, -15.0F, 8.8F, 1.0F, 15.0F, 0.4F)
                ,
                PartPose.offset(0.0F, -17.0F, 0.0F)
        );

        // ── PART 3: REAR ASSEMBLY & REAR STEEL DRUM ───────────────────────
        PartDefinition rearAssembly = centerChassis.addOrReplaceChild("rear_assembly", CubeListBuilder.create()
                        // Rear yoke crossbeam
                        .texOffs(2, 234).addBox(-18.0F, 3.0F, -19.0F, 36.0F, 6.0F, 5.0F)
                        // Left rear yoke longitudinal side fork arm
                        .texOffs(78, 152).addBox(-19.5F, 3.0F, -38.0F, 3.0F, 9.0F, 20.0F)
                        .texOffs(238, 234).addBox(-20.5F, 5.0F, -29.0F, 1.0F, 6.0F, 6.0F)
                        // Right rear yoke longitudinal side fork arm
                        .texOffs(128, 152).addBox(16.5F, 3.0F, -38.0F, 3.0F, 9.0F, 20.0F)
                        .texOffs(254, 234).addBox(19.5F, 5.0F, -29.0F, 1.0F, 6.0F, 6.0F)
                        // Rear yoke end bumper
                        .texOffs(88, 234).addBox(-18.0F, 3.0F, -40.0F, 36.0F, 6.0F, 3.0F)
                        // Rear water sprinkler spray bar & mud scrapers
                        .texOffs(412, 234).addBox(-16.0F, 2.0F, -14.5F, 32.0F, 1.5F, 1.5F)
                        .texOffs(412, 234).addBox(-16.0F, 2.0F, -37.5F, 32.0F, 1.5F, 1.5F)
                        // Rear tail light clusters
                        .texOffs(370, 263).addBox(-16.0F, 4.0F, -40.5F, 4.0F, 2.5F, 0.8F)
                        .texOffs(370, 263).addBox(12.0F, 4.0F, -40.5F, 4.0F, 2.5F, 0.8F)
                ,
                PartPose.ZERO
        );

        // REAR STEEL DRUM (identical 16-facet smooth cylindrical steel drum)
        rearAssembly.addOrReplaceChild("rear_drum", CubeListBuilder.create()
                        .texOffs(404, 107).addBox(-16.0F, -13.5F, -5.6F, 32.0F, 27.0F, 11.2F)
                        .texOffs(284, 107).addBox(-15.95F, -5.65F, -13.5F, 31.9F, 11.3F, 27.0F)
                        .texOffs(146, 58).addBox(-15.9F, -11.55F, -9.55F, 31.8F, 23.1F, 19.1F)
                        .texOffs(252, 58).addBox(-15.85F, -9.6F, -11.5F, 31.7F, 19.2F, 23.0F)
                        .texOffs(202, 2).addBox(-16.05F, -12.5F, -12.5F, 0.5F, 25.0F, 25.0F)
                        .texOffs(256, 2).addBox(15.55F, -12.5F, -12.5F, 0.5F, 25.0F, 25.0F)
                        .texOffs(266, 214).addBox(-16.5F, -3.5F, -3.5F, 0.45F, 7.0F, 7.0F)
                        .texOffs(284, 214).addBox(16.05F, -3.5F, -3.5F, 0.45F, 7.0F, 7.0F)
                ,
                PartPose.offset(0.0F, 8.0F, -26.0F)
        );

        // ── PART 4: FRONT ASSEMBLY & FRONT STEEL DRUM ─────────────────────
        PartDefinition frontAssembly = root.addOrReplaceChild("front_assembly", CubeListBuilder.create()
                        // Articulation hitch receiver tongue
                        .texOffs(332, 250).addBox(-3.0F, -1.5F, 9.5F, 6.0F, 9.0F, 4.0F)
                        // Front yoke main transverse arch
                        .texOffs(2, 234).addBox(-18.0F, 3.0F, 13.5F, 36.0F, 6.0F, 5.0F)
                        // Left front yoke longitudinal side fork arm
                        .texOffs(78, 152).addBox(-19.5F, 3.0F, 18.0F, 3.0F, 9.0F, 20.0F)
                        .texOffs(238, 234).addBox(-20.5F, 5.0F, 23.0F, 1.0F, 6.0F, 6.0F)
                        // Right front yoke longitudinal side fork arm
                        .texOffs(128, 152).addBox(16.5F, 3.0F, 18.0F, 3.0F, 9.0F, 20.0F)
                        .texOffs(254, 234).addBox(19.5F, 5.0F, 23.0F, 1.0F, 6.0F, 6.0F)
                        // Front yoke bumper crossbeam
                        .texOffs(88, 234).addBox(-18.0F, 3.0F, 37.0F, 36.0F, 6.0F, 3.0F)
                        // Front water sprinkler spray bar & mud scrapers
                        .texOffs(412, 234).addBox(-16.0F, 2.0F, 14.0F, 32.0F, 1.5F, 1.5F)
                        .texOffs(412, 234).addBox(-16.0F, 2.0F, 37.0F, 32.0F, 1.5F, 1.5F)
                        // Front headlights / amber blinkers
                        .texOffs(452, 214).addBox(-16.0F, 4.0F, 40.2F, 4.0F, 2.5F, 0.8F)
                        .texOffs(452, 214).addBox(12.0F, 4.0F, 40.2F, 4.0F, 2.5F, 0.8F)
                        // Right-side asphalt edge cutter disc attachment (see Photo 2!)
                        .texOffs(304, 234).addBox(20.5F, 5.0F, 30.0F, 1.5F, 6.0F, 2.0F)
                        .texOffs(266, 214).addBox(21.0F, 8.0F, 29.0F, 0.8F, 6.0F, 6.0F)
                ,
                PartPose.ZERO
        );

        // FRONT STEEL DRUM (identical 16-facet smooth cylindrical steel drum)
        frontAssembly.addOrReplaceChild("front_drum", CubeListBuilder.create()
                        .texOffs(404, 107).addBox(-16.0F, -13.5F, -5.6F, 32.0F, 27.0F, 11.2F)
                        .texOffs(284, 107).addBox(-15.95F, -5.65F, -13.5F, 31.9F, 11.3F, 27.0F)
                        .texOffs(146, 58).addBox(-15.9F, -11.55F, -9.55F, 31.8F, 23.1F, 19.1F)
                        .texOffs(252, 58).addBox(-15.85F, -9.6F, -11.5F, 31.7F, 19.2F, 23.0F)
                        .texOffs(202, 2).addBox(-16.05F, -12.5F, -12.5F, 0.5F, 25.0F, 25.0F)
                        .texOffs(256, 2).addBox(15.55F, -12.5F, -12.5F, 0.5F, 25.0F, 25.0F)
                        .texOffs(266, 214).addBox(-16.5F, -3.5F, -3.5F, 0.45F, 7.0F, 7.0F)
                        .texOffs(284, 214).addBox(16.05F, -3.5F, -3.5F, 0.45F, 7.0F, 7.0F)
                ,
                PartPose.offset(0.0F, 8.0F, 26.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }
}
