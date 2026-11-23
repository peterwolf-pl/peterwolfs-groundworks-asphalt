package com.piotrek.groundworksasphalt.client.model;

import com.piotrek.groundworksasphalt.client.render.RoadRollerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Photorealistic 16-Ton Heavy Road Roller / Asphalt Compactor Model.
 *
 * <p>Features:
 * <ul>
 *   <li>Front massive vibrating cylindrical steel drum (rotating with speed, vibrating during compaction)</li>
 *   <li>Heavy articulated drum yoke frame with mud scrapers and water sprinkler system</li>
 *   <li>Central articulated steering hitch with twin hydraulic steering cylinders (up to 35° steering)</li>
 *   <li>Panoramic ROPS/FOPS operator cab with curved tinted glass, wipers, mirrors, seat, and console</li>
 *   <li>Rear tractor unit with aerodynamic sloping engine hood, side air grilles, and exhaust stack</li>
 *   <li>Heavy rear ballast counterweight bumper and dual pneumatic industrial chevron-tread tires</li>
 * </ul>
 */
public class RoadRollerModel extends EntityModel<RoadRollerRenderState> {

    private final ModelPart rearChassis;
    private final ModelPart frontAssembly;
    private final ModelPart drum;
    private final ModelPart rearLeftWheel;
    private final ModelPart rearRightWheel;
    private final ModelPart beacon;
    private final ModelPart wiper;

    public RoadRollerModel(ModelPart root) {
        super(root);
        this.rearChassis = root.getChild("rear_chassis");
        this.frontAssembly = root.getChild("front_assembly");
        this.drum = this.frontAssembly.getChild("drum");
        this.rearLeftWheel = this.rearChassis.getChild("rear_left_wheel");
        this.rearRightWheel = this.rearChassis.getChild("rear_right_wheel");
        this.beacon = this.rearChassis.getChild("cab").getChild("beacon");
        this.wiper = this.rearChassis.getChild("cab").getChild("wiper");
    }

    @Override
    public void setupAnim(RoadRollerRenderState state) {
        super.setupAnim(state);

        // 1. Central Articulated Steering (front assembly pivots on Y)
        this.frontAssembly.yRot = state.steerAngle;

        // 2. Drive Rotation (rear wheels and front drum)
        this.rearLeftWheel.xRot = -state.wheelRotation;
        this.rearRightWheel.xRot = -state.wheelRotation;
        this.drum.xRot = -state.drumRotation;

        // 3. Vibratory Compaction Oscillation
        if (state.engineRunning && (state.isCompacting || state.isVibrating)) {
            float freq = state.ageInTicks * 4.0F;
            float amp = state.vibrationAmplitude;
            this.drum.y = 8.0F + (float) Math.sin(freq) * amp;
            this.drum.z = 28.0F + (float) Math.cos(freq) * (amp * 0.4F);
        } else {
            this.drum.y = 8.0F;
            this.drum.z = 28.0F;
        }

        // 4. Emergency Amber Strobe Beacon
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

        // --- Part: REAR_CHASSIS ---
        PartDefinition rearChassis = root.addOrReplaceChild("rear_chassis", CubeListBuilder.create() /* PartPose.ZERO */
                        // Left chassis longitudinal beam
                        .texOffs(2, 2).addBox(-11.000F, -2.000F, -46.000F, 5.000F, 8.000F, 44.000F)
                        // Right chassis longitudinal beam
                        .texOffs(102, 2).addBox(6.000F, -2.000F, -46.000F, 5.000F, 8.000F, 44.000F)
                        // Rear cross member
                        .texOffs(212, 220).addBox(-6.000F, -1.960F, -45.960F, 12.000F, 7.920F, 6.000F)
                        // Mid cross member
                        .texOffs(250, 220).addBox(-6.000F, -1.960F, -22.000F, 12.000F, 7.920F, 6.000F)
                        // Front cross member
                        .texOffs(174, 240).addBox(-6.000F, -1.960F, -5.960F, 12.000F, 7.920F, 3.920F)
                        // Rear heavy ballast counterweight bumper
                        .texOffs(68, 220).addBox(-15.000F, -2.050F, -51.000F, 30.000F, 9.100F, 4.950F)
                        // Rear bumper lower bevel guard
                        .texOffs(372, 256).addBox(-14.000F, 7.070F, -49.500F, 28.000F, 2.900F, 3.400F)
                        // Rear tow hitch eye block
                        .texOffs(10, 269).addBox(-2.500F, 0.500F, -53.000F, 5.000F, 4.000F, 1.950F)
                        // Rear central differential carrier
                        .texOffs(324, 220).addBox(-4.500F, 5.000F, -33.500F, 9.000F, 6.000F, 7.000F)
                        // Left rear axle trumpet tube
                        .texOffs(112, 256).addBox(-16.000F, 6.200F, -31.800F, 11.450F, 3.600F, 3.600F)
                        // Right rear axle trumpet tube
                        .texOffs(146, 256).addBox(4.550F, 6.200F, -31.800F, 11.450F, 3.600F, 3.600F)
                        // Hitch upper ear
                        .texOffs(210, 256).addBox(-3.500F, -3.000F, -2.000F, 7.000F, 2.900F, 5.000F)
                        // Hitch lower ear
                        .texOffs(236, 256).addBox(-3.500F, 4.100F, -2.000F, 7.000F, 2.900F, 5.000F)
                        // Vertical kingpin pivot pin
                        .texOffs(430, 220).addBox(-1.200F, -3.500F, 0.800F, 2.400F, 11.000F, 2.000F)
                        // Left steer cylinder rear anchor
                        .texOffs(280, 256).addBox(-10.500F, 0.500F, -4.500F, 3.000F, 4.000F, 4.000F)
                        // Right steer cylinder rear anchor
                        .texOffs(296, 256).addBox(7.500F, 0.500F, -4.500F, 3.000F, 4.000F, 4.000F)
                        // Left tail light cluster
                        .texOffs(352, 269).addBox(-13.500F, 0.000F, -51.100F, 4.000F, 2.500F, 0.800F)
                        // Right tail light cluster
                        .texOffs(364, 269).addBox(9.500F, 0.000F, -51.100F, 4.000F, 2.500F, 0.800F)
                ,
                PartPose.ZERO
        );

        // --- Part: ENGINE_HOOD ---
        PartDefinition engineHood = rearChassis.addOrReplaceChild("engine_hood", CubeListBuilder.create() /* PartPose.ZERO */
                        // Engine lower compartment housing
                        .texOffs(82, 107).addBox(-12.000F, -14.000F, -45.800F, 24.000F, 11.900F, 27.600F)
                        // Bonnet top front slope
                        .texOffs(66, 194).addBox(-11.500F, -18.500F, -34.000F, 23.000F, 4.450F, 15.750F)
                        // Bonnet top rear slope
                        .texOffs(140, 220).addBox(-11.450F, -16.500F, -45.500F, 22.900F, 2.450F, 11.450F)
                        // Left radiator intake louver panel
                        .texOffs(2, 194).addBox(-12.080F, -12.000F, -42.000F, 0.600F, 8.000F, 14.000F)
                        // Right radiator intake louver panel
                        .texOffs(34, 194).addBox(11.480F, -12.000F, -42.000F, 0.600F, 8.000F, 14.000F)
                        // Top engine hot air exhaust grille
                        .texOffs(72, 256).addBox(-6.000F, -18.560F, -28.000F, 12.000F, 0.500F, 7.000F)
                        // Exhaust muffler canister
                        .texOffs(58, 256).addBox(8.000F, -24.000F, -25.500F, 2.500F, 5.400F, 2.500F)
                        // Vertical exhaust tailpipe
                        .texOffs(402, 240).addBox(8.500F, -32.000F, -25.000F, 1.500F, 7.950F, 1.500F)
                        // Exhaust hinged rain cap
                        .texOffs(310, 269).addBox(8.200F, -32.800F, -25.800F, 2.100F, 0.750F, 2.600F)
                        // Air intake snorkel tube
                        .texOffs(304, 240).addBox(-10.200F, -27.000F, -25.000F, 1.400F, 8.400F, 1.500F)
                        // Cyclone air pre-cleaner canister
                        .texOffs(312, 256).addBox(-11.000F, -30.500F, -26.000F, 3.000F, 3.450F, 3.500F)
                ,
                PartPose.ZERO
        );

        // --- Part: CAB ---
        PartDefinition cab = rearChassis.addOrReplaceChild("cab", CubeListBuilder.create() /* PartPose.ZERO */
                        // Cab subframe platform floor
                        .texOffs(388, 152).addBox(-13.500F, -13.500F, -18.000F, 27.000F, 2.000F, 20.000F)
                        // Left access staircase spine
                        .texOffs(382, 194).addBox(-17.500F, -8.000F, -11.000F, 3.950F, 12.000F, 6.000F)
                        // Left top step tread
                        .texOffs(456, 240).addBox(-18.000F, -8.500F, -12.000F, 4.420F, 1.000F, 7.950F)
                        // Left lower step tread
                        .texOffs(484, 240).addBox(-18.000F, 0.000F, -12.000F, 4.420F, 1.000F, 7.950F)
                        // Left stair yellow safety handrail front post
                        .texOffs(442, 220).addBox(-18.500F, -18.000F, -12.060F, 0.700F, 10.400F, 1.500F)
                        // Left stair yellow safety handrail rear post
                        .texOffs(450, 220).addBox(-18.500F, -18.000F, -5.440F, 0.700F, 10.400F, 1.480F)
                        // Right access staircase spine
                        .texOffs(404, 194).addBox(13.550F, -8.000F, -11.000F, 3.950F, 12.000F, 6.000F)
                        // Right top step tread
                        .texOffs(2, 256).addBox(13.580F, -8.500F, -12.000F, 4.420F, 1.000F, 7.950F)
                        // Right lower step tread
                        .texOffs(30, 256).addBox(13.580F, 0.000F, -12.000F, 4.420F, 1.000F, 7.950F)
                        // Right stair yellow safety handrail front post
                        .texOffs(458, 220).addBox(17.800F, -18.000F, -12.060F, 0.700F, 10.400F, 1.500F)
                        // Right stair yellow safety handrail rear post
                        .texOffs(466, 220).addBox(17.800F, -18.000F, -5.440F, 0.700F, 10.400F, 1.480F)
                        // Front cab lower sill
                        .texOffs(42, 269).addBox(-13.000F, -16.000F, 0.500F, 26.000F, 2.450F, 1.500F)
                        // Rear cab lower sill
                        .texOffs(100, 269).addBox(-13.000F, -16.000F, -18.000F, 26.000F, 2.450F, 1.500F)
                        // Left door sill
                        .texOffs(302, 194).addBox(-13.000F, -16.000F, -16.450F, 1.500F, 2.450F, 16.900F)
                        // Right door sill
                        .texOffs(342, 194).addBox(11.500F, -16.000F, -16.450F, 1.500F, 2.450F, 16.900F)
                        // Front-left ROPS A-pillar
                        .texOffs(146, 194).addBox(-13.000F, -39.000F, 0.200F, 2.000F, 22.950F, 1.800F)
                        // Front-right ROPS A-pillar
                        .texOffs(156, 194).addBox(11.000F, -39.000F, 0.200F, 2.000F, 22.950F, 1.800F)
                        // Rear-left ROPS C-pillar
                        .texOffs(166, 194).addBox(-13.000F, -39.000F, -18.000F, 2.000F, 22.950F, 1.800F)
                        // Rear-right ROPS C-pillar
                        .texOffs(176, 194).addBox(11.000F, -39.000F, -18.000F, 2.000F, 22.950F, 1.800F)
                        // Left B-pillar door post
                        .texOffs(186, 194).addBox(-13.050F, -39.000F, -8.500F, 1.600F, 22.950F, 2.000F)
                        // Right B-pillar door post
                        .texOffs(196, 194).addBox(11.450F, -39.000F, -8.500F, 1.600F, 22.950F, 2.000F)
                        // Front curved windshield glass
                        .texOffs(206, 194).addBox(-10.950F, -38.800F, 0.800F, 21.900F, 22.750F, 1.000F)
                        // Rear panoramic window glass
                        .texOffs(254, 194).addBox(-10.950F, -38.800F, -17.800F, 21.900F, 22.750F, 1.000F)
                        // Left door window glass
                        .texOffs(250, 152).addBox(-12.800F, -38.800F, -6.450F, 1.000F, 22.750F, 6.600F)
                        // Left rear quarter glass
                        .texOffs(210, 152).addBox(-12.800F, -38.800F, -16.150F, 1.000F, 22.750F, 7.600F)
                        // Right door window glass
                        .texOffs(268, 152).addBox(11.800F, -38.800F, -6.450F, 1.000F, 22.750F, 6.600F)
                        // Right rear quarter glass
                        .texOffs(230, 152).addBox(11.800F, -38.800F, -16.150F, 1.000F, 22.750F, 7.600F)
                        // Main cab aerodynamic roof cap
                        .texOffs(286, 152).addBox(-14.000F, -42.000F, -19.000F, 28.000F, 2.950F, 22.000F)
                        // Roof HVAC climate control pod
                        .texOffs(426, 194).addBox(-8.000F, -45.500F, -14.000F, 16.000F, 3.450F, 12.000F)
                        // Left front roof LED work light
                        .texOffs(274, 269).addBox(-10.000F, -41.500F, 3.050F, 6.000F, 2.000F, 1.150F)
                        // Right front roof LED work light
                        .texOffs(292, 269).addBox(4.000F, -41.500F, 3.050F, 6.000F, 2.000F, 1.150F)
                        // Left rear roof LED work light
                        .texOffs(92, 280).addBox(-10.000F, -41.500F, -19.800F, 6.000F, 2.000F, 0.750F)
                        // Right rear roof LED work light
                        .texOffs(108, 280).addBox(4.000F, -41.500F, -19.800F, 6.000F, 2.000F, 0.750F)
                        // Left mirror stalk
                        .texOffs(324, 269).addBox(-16.500F, -36.500F, 0.500F, 3.450F, 1.500F, 1.500F)
                        // Left panoramic mirror head
                        .texOffs(378, 240).addBox(-18.500F, -39.000F, 0.000F, 1.950F, 7.000F, 2.500F)
                        // Right mirror stalk
                        .texOffs(338, 269).addBox(13.050F, -36.500F, 0.500F, 3.450F, 1.500F, 1.500F)
                        // Right panoramic mirror head
                        .texOffs(390, 240).addBox(16.550F, -39.000F, 0.000F, 1.950F, 7.000F, 2.500F)
                        // Operator seat pedestal base
                        .texOffs(208, 240).addBox(-3.500F, -15.000F, -11.000F, 7.000F, 1.500F, 7.000F)
                        // Operator ergonomic seat cushion
                        .texOffs(270, 240).addBox(-4.000F, -17.500F, -11.500F, 8.000F, 2.500F, 8.000F)
                        // Operator seat high backrest
                        .texOffs(390, 220).addBox(-3.500F, -27.000F, -11.500F, 7.000F, 9.500F, 2.500F)
                        // Operator seat headrest
                        .texOffs(26, 269).addBox(-2.500F, -30.500F, -11.200F, 5.000F, 3.500F, 1.900F)
                        // Left armrest
                        .texOffs(474, 256).addBox(-5.000F, -20.500F, -9.000F, 0.950F, 1.500F, 5.000F)
                        // Right armrest
                        .texOffs(488, 256).addBox(4.050F, -20.500F, -9.000F, 0.950F, 1.500F, 5.000F)
                        // Steering column pedestal
                        .texOffs(412, 220).addBox(-2.500F, -20.000F, -2.000F, 5.000F, 6.480F, 3.000F)
                        // Dashboard digital instrument display
                        .texOffs(180, 256).addBox(-4.500F, -22.500F, -3.500F, 9.000F, 2.450F, 4.500F)
                        // Steering wheel
                        .texOffs(252, 269).addBox(-3.500F, -23.500F, -2.500F, 7.000F, 0.950F, 3.000F)
                        // Right joystick control console
                        .texOffs(456, 256).addBox(4.200F, -19.500F, -7.500F, 2.300F, 1.500F, 4.500F)
                ,
                PartPose.ZERO
        );

        // --- Part: BEACON ---
        PartDefinition beacon = cab.addOrReplaceChild("beacon", CubeListBuilder.create() /* PartPose.ZERO */
                        // Emergency amber rotating beacon
                        .texOffs(438, 256).addBox(-2.000F, -48.500F, -9.000F, 4.000F, 2.950F, 4.000F)
                ,
                PartPose.ZERO
        );

        // --- Part: WIPER ---
        PartDefinition wiper = cab.addOrReplaceChild("wiper", CubeListBuilder.create() /* PartPose.ZERO */
                        // Front windshield wiper arm and blade
                        .texOffs(72, 280).addBox(-1.000F, -26.000F, 1.850F, 8.000F, 1.500F, 0.600F)
                ,
                PartPose.ZERO
        );

        // --- Part: REAR_LEFT_WHEEL ---
        PartDefinition rearLeftWheel = rearChassis.addOrReplaceChild("rear_left_wheel", CubeListBuilder.create() /* PartPose.offset(-18.5F, 8.0F, -30.0F) */
                        // Left wheel vertical carcass
                        .texOffs(188, 107).addBox(-5.500F, -14.000F, -6.000F, 11.000F, 28.000F, 12.000F)
                        // Left wheel horizontal carcass
                        .texOffs(364, 58).addBox(-5.450F, -6.050F, -14.000F, 10.900F, 12.100F, 28.000F)
                        // Left wheel diagonal 1 carcass
                        .texOffs(310, 2).addBox(-5.400F, -12.050F, -10.050F, 10.800F, 24.100F, 20.100F)
                        // Left wheel diagonal 2 carcass
                        .texOffs(2, 58).addBox(-5.350F, -10.100F, -12.000F, 10.700F, 20.200F, 24.000F)
                        // Left wheel steel rim disc
                        .texOffs(134, 152).addBox(-5.560F, -8.000F, -8.000F, 1.800F, 16.000F, 16.000F)
                        // Left wheel planetary hub
                        .texOffs(24, 220).addBox(-7.500F, -4.000F, -4.000F, 1.930F, 8.000F, 8.000F)
                        // Left wheel planetary hub center cap
                        .texOffs(328, 256).addBox(-8.500F, -2.000F, -2.000F, 0.980F, 4.000F, 4.000F)
                ,
                PartPose.offset(-18.5F, 8.0F, -30.0F)
        );

        // --- Part: REAR_RIGHT_WHEEL ---
        PartDefinition rearRightWheel = rearChassis.addOrReplaceChild("rear_right_wheel", CubeListBuilder.create() /* PartPose.offset(18.5F, 8.0F, -30.0F) */
                        // Right wheel vertical carcass
                        .texOffs(236, 107).addBox(-5.500F, -14.000F, -6.000F, 11.000F, 28.000F, 12.000F)
                        // Right wheel horizontal carcass
                        .texOffs(2, 107).addBox(-5.450F, -6.050F, -14.000F, 10.900F, 12.100F, 28.000F)
                        // Right wheel diagonal 1 carcass
                        .texOffs(376, 2).addBox(-5.400F, -12.050F, -10.050F, 10.800F, 24.100F, 20.100F)
                        // Right wheel diagonal 2 carcass
                        .texOffs(74, 58).addBox(-5.350F, -10.100F, -12.000F, 10.700F, 20.200F, 24.000F)
                        // Right wheel steel rim disc
                        .texOffs(172, 152).addBox(3.760F, -8.000F, -8.000F, 1.800F, 16.000F, 16.000F)
                        // Right wheel planetary hub
                        .texOffs(46, 220).addBox(5.570F, -4.000F, -4.000F, 1.930F, 8.000F, 8.000F)
                        // Right wheel planetary hub center cap
                        .texOffs(340, 256).addBox(7.520F, -2.000F, -2.000F, 0.980F, 4.000F, 4.000F)
                ,
                PartPose.offset(18.5F, 8.0F, -30.0F)
        );

        // --- Part: FRONT_ASSEMBLY ---
        PartDefinition frontAssembly = root.addOrReplaceChild("front_assembly", CubeListBuilder.create() /* PartPose.ZERO */
                        // Front hitch articulation tongue
                        .texOffs(358, 220).addBox(-3.000F, 0.000F, 0.500F, 6.000F, 4.000F, 9.000F)
                        // Tongue pivot sleeve
                        .texOffs(262, 256).addBox(-2.500F, -0.500F, 1.000F, 5.000F, 5.000F, 3.000F)
                        // Left steering cylinder barrel
                        .texOffs(314, 240).addBox(-8.500F, 1.500F, 1.000F, 2.000F, 2.000F, 7.500F)
                        // Left steering chrome piston rod
                        .texOffs(158, 269).addBox(-7.800F, 2.100F, -2.500F, 0.600F, 0.800F, 3.450F)
                        // Right steering cylinder barrel
                        .texOffs(336, 240).addBox(6.500F, 1.500F, 1.000F, 2.000F, 2.000F, 7.500F)
                        // Right steering chrome piston rod
                        .texOffs(170, 269).addBox(7.200F, 2.100F, -2.500F, 0.600F, 0.800F, 3.450F)
                        // Yoke rear cross beam
                        .texOffs(2, 240).addBox(-18.000F, 3.000F, 9.500F, 36.000F, 6.000F, 5.450F)
                        // Yoke central reinforcement arch
                        .texOffs(412, 240).addBox(-8.000F, -1.000F, 10.000F, 16.000F, 3.950F, 4.500F)
                        // Left yoke longitudinal side arm
                        .texOffs(2, 152).addBox(-19.500F, 3.000F, 14.950F, 3.000F, 9.000F, 28.500F)
                        // Left drum axle bearing housing
                        .texOffs(238, 240).addBox(-20.500F, 5.000F, 25.000F, 0.950F, 6.000F, 6.000F)
                        // Left drum rubber vibration isolator mounts
                        .texOffs(484, 194).addBox(-17.500F, 4.050F, 24.050F, 1.250F, 7.900F, 7.900F)
                        // Right yoke longitudinal side arm
                        .texOffs(68, 152).addBox(16.500F, 3.000F, 14.950F, 3.000F, 9.000F, 28.500F)
                        // Right drum axle bearing housing
                        .texOffs(254, 240).addBox(19.550F, 5.000F, 25.000F, 0.950F, 6.000F, 6.000F)
                        // Right drum rubber vibration isolator mounts
                        .texOffs(2, 220).addBox(16.250F, 4.050F, 24.050F, 1.250F, 7.900F, 7.900F)
                        // Hydraulic vibration exciter motor
                        .texOffs(358, 240).addBox(18.500F, 5.500F, 25.500F, 3.500F, 5.000F, 5.000F)
                        // Yoke front cross beam bumper
                        .texOffs(88, 240).addBox(-18.000F, 3.000F, 43.450F, 36.000F, 6.000F, 5.600F)
                        // Front mud scraper blade
                        .texOffs(376, 269).addBox(-16.200F, 10.500F, 41.800F, 32.400F, 2.000F, 0.500F)
                        // Front mud scraper left spring bracket
                        .texOffs(502, 256).addBox(-16.800F, 6.000F, 41.800F, 1.000F, 5.000F, 1.650F)
                        // Front mud scraper right spring bracket
                        .texOffs(2, 269).addBox(15.800F, 6.000F, 41.800F, 1.000F, 5.000F, 1.650F)
                        // Rear mud scraper blade
                        .texOffs(2, 280).addBox(-16.200F, 10.500F, 15.480F, 32.400F, 2.000F, 1.000F)
                        // Rear mud scraper left spring bracket
                        .texOffs(352, 256).addBox(-16.800F, 6.000F, 15.000F, 1.000F, 5.000F, 3.000F)
                        // Rear mud scraper right spring bracket
                        .texOffs(362, 256).addBox(15.800F, 6.000F, 15.000F, 1.000F, 5.000F, 3.000F)
                        // Water spray manifold bar
                        .texOffs(182, 269).addBox(-16.000F, 2.000F, 42.000F, 32.000F, 1.200F, 1.200F)
                ,
                PartPose.ZERO
        );

        // --- Part: DRUM ---
        PartDefinition drum = frontAssembly.addOrReplaceChild("drum", CubeListBuilder.create() /* PartPose.offset(0.0F, 8.0F, 28.0F) */
                        // Drum vertical cylinder core
                        .texOffs(404, 107).addBox(-16.000F, -13.500F, -5.600F, 32.000F, 27.000F, 11.200F)
                        // Drum horizontal cylinder core
                        .texOffs(284, 107).addBox(-15.950F, -5.650F, -13.500F, 31.900F, 11.300F, 27.000F)
                        // Drum diagonal 1 cylinder facet
                        .texOffs(146, 58).addBox(-15.900F, -11.550F, -9.550F, 31.800F, 23.100F, 19.100F)
                        // Drum diagonal 2 cylinder facet
                        .texOffs(252, 58).addBox(-15.850F, -9.600F, -11.500F, 31.700F, 19.200F, 23.000F)
                        // Left welded heavy drum end plate
                        .texOffs(202, 2).addBox(-16.060F, -12.500F, -12.500F, 0.500F, 25.000F, 25.000F)
                        // Right welded heavy drum end plate
                        .texOffs(256, 2).addBox(15.560F, -12.500F, -12.500F, 0.500F, 25.000F, 25.000F)
                        // Left drum axle hub boss
                        .texOffs(288, 220).addBox(-16.500F, -3.500F, -3.500F, 0.440F, 7.000F, 7.000F)
                        // Right drum axle hub boss
                        .texOffs(306, 220).addBox(16.060F, -3.500F, -3.500F, 0.440F, 7.000F, 7.000F)
                ,
                PartPose.offset(0.0F, 8.0F, 28.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }
}
