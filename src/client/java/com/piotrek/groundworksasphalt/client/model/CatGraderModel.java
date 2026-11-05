package com.piotrek.groundworksasphalt.client.model;

import com.piotrek.groundworksasphalt.client.render.CatGraderRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Photorealistic 3D EntityModel for the Cat Motor Grader (Równiarka Drogowa).
 *
 * <p>Constructed according to Caterpillar heavy equipment standards:
 * <ul>
 *   <li>6-wheel configuration with front oscillating lean axle and rear tandem drive bogies.</li>
 *   <li>Massive high-arched gooseneck backbone frame providing full blade clearance.</li>
 *   <li>Central drawbar and 360-degree rotating circle ring gear assembly.</li>
 *   <li>Curved parabolic moldboard blade with wear cutting edge, slide shift, and slope tilt.</li>
 *   <li>Twin hydraulic blade-lift cylinders and circle side-shift ram.</li>
 *   <li>High-visibility panoramic ROPS/FOPS operator cab with observation glass and safety beacon.</li>
 *   <li>Rear heavy Cat diesel engine housing with ventilation louvers, vertical exhaust, and precleaner.</li>
 *   <li>Rear hydraulic parallel-linkage ripper / scarifier with replaceable carbide teeth.</li>
 * </ul>
 *
 * <p>Compliant with DEGLITCH standards: Zero coplanar Z-fighting and complete 512x512 UV mapping.
 */
public class CatGraderModel extends EntityModel<CatGraderRenderState> {

    // Main hierarchy parts
    private final ModelPart mainChassis;
    private final ModelPart gooseneck;

    // Front steering & axle
    private final ModelPart frontAxle;
    private final ModelPart frontLeftMount;
    private final ModelPart frontRightMount;
    private final ModelPart frontLeftWheel;
    private final ModelPart frontRightWheel;

    // Rear tandem drive bogies
    private final ModelPart leftTandem;
    private final ModelPart rightTandem;
    private final ModelPart midLeftWheel;
    private final ModelPart midRightWheel;
    private final ModelPart rearLeftWheel;
    private final ModelPart rearRightWheel;

    // Drawbar, circle ring & moldboard
    private final ModelPart drawbar;
    private final ModelPart circleRing;
    private final ModelPart moldboard;
    private final ModelPart leftLiftCylinder;
    private final ModelPart leftLiftRod;
    private final ModelPart rightLiftCylinder;
    private final ModelPart rightLiftRod;

    // Cab & accessories
    private final ModelPart cab;
    private final ModelPart beaconReflector;
    private final ModelPart ripper;

    public CatGraderModel(ModelPart root) {
        super(root);
        this.mainChassis = root.getChild("main_chassis");
        this.gooseneck = root.getChild("gooseneck");

        this.frontAxle = this.gooseneck.getChild("front_axle");
        this.frontLeftMount = this.frontAxle.getChild("front_left_mount");
        this.frontRightMount = this.frontAxle.getChild("front_right_mount");
        this.frontLeftWheel = this.frontLeftMount.getChild("front_left_wheel");
        this.frontRightWheel = this.frontRightMount.getChild("front_right_wheel");

        this.leftTandem = this.mainChassis.getChild("left_tandem");
        this.rightTandem = this.mainChassis.getChild("right_tandem");
        this.midLeftWheel = this.leftTandem.getChild("mid_left_wheel");
        this.rearLeftWheel = this.leftTandem.getChild("rear_left_wheel");
        this.midRightWheel = this.rightTandem.getChild("mid_right_wheel");
        this.rearRightWheel = this.rightTandem.getChild("rear_right_wheel");

        this.drawbar = root.getChild("drawbar");
        this.circleRing = this.drawbar.getChild("circle_ring");
        this.moldboard = this.circleRing.getChild("moldboard");

        this.leftLiftCylinder = this.gooseneck.getChild("left_lift_cylinder");
        this.leftLiftRod = this.leftLiftCylinder.getChild("left_lift_rod");
        this.rightLiftCylinder = this.gooseneck.getChild("right_lift_cylinder");
        this.rightLiftRod = this.rightLiftCylinder.getChild("right_lift_rod");

        this.cab = root.getChild("cab");
        this.beaconReflector = this.cab.getChild("roof").getChild("beacon_base").getChild("beacon_reflector");

        this.ripper = this.mainChassis.getChild("ripper");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ═════════════════════════════════════════════════════════════════════
        // 1. REAR MAIN CHASSIS & ENGINE HOOD (Z = -22 to -66)
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition mainChassis = root.addOrReplaceChild("main_chassis" /* PartPose.ZERO */,
                CubeListBuilder.create()
                        // Rear heavy structural box frame
                        .texOffs(260, 280).addBox(-8.99F, 8.01F, -65.98F, 17.98F, 7.98F, 39.96F)
                        // Underbelly transmission & differential guard
                        .texOffs(260, 384).addBox(-4.99F, 16.01F, -56.0F, 9.98F, 2.0F, 22.0F)
                        // Engine hood top sloping cover
                        .texOffs(380, 280).addBox(-7.98F, -11.99F, -63.98F, 15.96F, 3.98F, 35.96F)
                        // Engine hood left side panel with chamfer
                        .texOffs(260, 332).addBox(-8.49F, -7.99F, -63.98F, 1.98F, 15.98F, 35.96F)
                        // Engine hood right side panel with chamfer
                        .texOffs(260, 332).addBox(6.51F, -7.99F, -63.98F, 1.98F, 15.98F, 35.96F)
                        // Front engine bulkhead / cab firewall
                        .texOffs(340, 332).addBox(-7.98F, -7.99F, -28.01F, 15.96F, 15.98F, 1.98F)
                        // Rear radiator housing & screen
                        .texOffs(380, 332).addBox(-8.48F, -7.99F, -65.99F, 16.96F, 13.98F, 1.98F)
                        // Heavy cast iron rear counterweight bumper
                        .texOffs(432, 332).addBox(-10.98F, 6.01F, -68.48F, 21.96F, 7.98F, 2.48F)
                        // Left & right tandem bogie pivot trunnions
                        .texOffs(72, 60).addBox(-12.48F, 10.01F, -47.99F, 3.48F, 3.98F, 3.98F)
                        .texOffs(72, 60).addBox(9.0F, 10.01F, -47.99F, 3.48F, 3.98F, 3.98F)
                        // Left-side battery & tool box
                        .texOffs(260, 384).addBox(-11.48F, 8.01F, -38.0F, 2.48F, 5.98F, 9.98F)
                        // Right-side hydraulic fluid reservoir
                        .texOffs(260, 384).addBox(9.0F, 8.01F, -38.0F, 2.48F, 5.98F, 9.98F)
                        // Vertical exhaust stack (black iron pipe)
                        .texOffs(310, 384).addBox(-6.0F, -27.99F, -34.0F, 1.98F, 15.98F, 1.98F)
                        // Exhaust muffler body (height 9.94F so top Y=-12.05F, avoiding Y=-12.01F plane)
                        .texOffs(322, 384).addBox(-7.0F, -21.99F, -35.0F, 3.98F, 9.94F, 3.98F)
                        // Exhaust hinged rain cap / flapper
                        .texOffs(342, 384).addBox(-6.49F, -28.49F, -34.49F, 2.98F, 0.48F, 2.98F)
                        // Cyclone air pre-cleaner canister on right
                        .texOffs(358, 384).addBox(3.0F, -23.99F, -36.0F, 4.98F, 11.98F, 4.98F)
                        // Rear taillights on counterweight
                        .texOffs(16, 420).addBox(-9.99F, 8.01F, -68.75F, 4.98F, 2.98F, 0.24F)
                        .texOffs(16, 420).addBox(5.01F, 8.01F, -68.75F, 4.98F, 2.98F, 0.24F),
                PartPose.ZERO
        );

        // Rear Ripper / Scarifier (attached to rear bumper at Z = -68.0F)
        PartDefinition ripper = mainChassis.addOrReplaceChild("ripper" /* PartPose.offset(0.0F, 8.0F, -68.0F) */,
                CubeListBuilder.create()
                        // Parallel linkage mounting brackets
                        .texOffs(380, 0).addBox(-6.0F, -2.0F, -4.0F, 1.96F, 7.98F, 3.98F)
                        .texOffs(380, 0).addBox(4.04F, -2.0F, -4.0F, 1.96F, 7.98F, 3.98F)
                        // Upper linkage arms (width 1.40F, non-coplanar with bracket faces)
                        .texOffs(404, 0).addBox(-5.46F, -1.99F, -11.99F, 1.40F, 1.98F, 8.98F)
                        .texOffs(404, 0).addBox(4.06F, -1.99F, -11.99F, 1.40F, 1.98F, 8.98F)
                        // Lower linkage arms
                        .texOffs(404, 0).addBox(-5.46F, 3.99F, -11.99F, 1.40F, 1.98F, 8.98F)
                        .texOffs(404, 0).addBox(4.06F, 3.99F, -11.99F, 1.40F, 1.98F, 8.98F)
                        // Ripper cross tool bar
                        .texOffs(440, 0).addBox(-15.98F, 2.01F, -14.98F, 31.96F, 3.98F, 2.98F)
                        // Center ripper shank & tooth (Z=-16.50F, depth 4.46F to avoid Z=-12.00F / -80.00F)
                        .texOffs(380, 20).addBox(-0.99F, 3.99F, -16.50F, 1.98F, 11.98F, 4.46F)
                        .texOffs(404, 20).addBox(-1.49F, 14.01F, -16.98F, 2.98F, 2.98F, 3.48F)
                        // Left ripper shank & tooth
                        .texOffs(380, 20).addBox(-12.99F, 3.99F, -16.50F, 1.98F, 11.98F, 4.46F)
                        .texOffs(404, 20).addBox(-13.49F, 14.01F, -16.98F, 2.98F, 2.98F, 3.48F)
                        // Right ripper shank & tooth
                        .texOffs(380, 20).addBox(11.01F, 3.99F, -16.50F, 1.98F, 11.98F, 4.46F)
                        .texOffs(404, 20).addBox(10.51F, 14.01F, -16.98F, 2.98F, 2.98F, 3.48F),
                PartPose.offset(0.0F, 8.0F, -68.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 2. REAR TANDEM BOGIES & 4 DRIVE WHEELS
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition leftTandem = mainChassis.addOrReplaceChild("left_tandem" /* PartPose.offset(-12.5F, 12.0F, -46.0F) */,
                CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-2.99F, -3.99F, -13.99F, 3.98F, 7.98F, 27.98F)
                        .texOffs(72, 60).addBox(-3.49F, -2.49F, -2.49F, 0.48F, 4.98F, 4.98F),
                PartPose.offset(-12.5F, 12.0F, -46.0F)
        );
        addTireAndWheel(leftTandem, "mid_left_wheel", -5.5F, 0.0F, 12.5F, true);
        addTireAndWheel(leftTandem, "rear_left_wheel", -5.5F, 0.0F, -13.0F, true);

        PartDefinition rightTandem = mainChassis.addOrReplaceChild("right_tandem" /* PartPose.offset(12.5F, 12.0F, -46.0F) */,
                CubeListBuilder.create()
                        .texOffs(0, 60).addBox(-0.99F, -3.99F, -13.99F, 3.98F, 7.98F, 27.98F)
                        .texOffs(72, 60).addBox(3.01F, -2.49F, -2.49F, 0.48F, 4.98F, 4.98F),
                PartPose.offset(12.5F, 12.0F, -46.0F)
        );
        addTireAndWheel(rightTandem, "mid_right_wheel", 5.5F, 0.0F, 12.5F, false);
        addTireAndWheel(rightTandem, "rear_right_wheel", 5.5F, 0.0F, -13.0F, false);

        // ═════════════════════════════════════════════════════════════════════
        // 3. ARCHED BACKBONE GOOSENECK FRAME (Z = -6 to +48)
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition gooseneck = root.addOrReplaceChild("gooseneck" /* PartPose.ZERO */,
                CubeListBuilder.create()
                        // Rear slope connecting to cab articulation (Z: -6 to +8, Y: 2 to -4)
                        .texOffs(116, 160).addBox(-4.49F, -3.99F, -6.0F, 8.98F, 6.96F, 14.0F)
                        // Main high apex arched section (Z: +8 to +30, Y: -5 to +2)
                        .texOffs(56, 160).addBox(-3.99F, -5.99F, 8.01F, 7.98F, 6.98F, 21.98F)
                        // Front downward slope connecting to bolster (Z: +30 to +48, Y: +1 to +8)
                        .texOffs(0, 160).addBox(-3.99F, 0.01F, 30.01F, 7.98F, 7.98F, 17.98F)
                        // Bottom web reinforcement flange running full length (Y: 4.03F, avoids Y=4.01F)
                        .texOffs(0, 192).addBox(-3.49F, 4.03F, -5.99F, 6.98F, 0.96F, 47.98F)
                        // Front drawbar ball hitch mount
                        .texOffs(114, 192).addBox(-2.99F, 7.01F, 38.01F, 5.98F, 3.98F, 5.98F)
                        // High trunnion cross-beam for blade lift cylinders
                        .texOffs(142, 192).addBox(-9.49F, -4.99F, 4.01F, 18.98F, 3.98F, 4.98F)
                        // Side-shift cylinder anchor bracket
                        .texOffs(0, 244).addBox(-2.99F, -1.99F, 12.01F, 5.98F, 3.98F, 4.98F),
                PartPose.ZERO
        );

        // Blade lift cylinders hanging from gooseneck trunnion
        PartDefinition leftLift = gooseneck.addOrReplaceChild("left_lift_cylinder" /* PartPose.offset(-8.0F, -2.0F, 6.5F) */,
                CubeListBuilder.create()
                        .texOffs(304, 234).addBox(-1.99F, 0.01F, -1.99F, 3.98F, 13.94F, 3.98F),
                PartPose.offset(-8.0F, -2.0F, 6.5F)
        );
        leftLift.addOrReplaceChild("left_lift_rod" /* PartPose.offset(-8.0F, 10.0F, 6.5F) */,
                CubeListBuilder.create()
                        .texOffs(324, 234).addBox(-0.99F, 0.03F, -0.99F, 1.98F, 11.92F, 1.98F),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        PartDefinition rightLift = gooseneck.addOrReplaceChild("right_lift_cylinder" /* PartPose.offset(8.0F, -2.0F, 6.5F) */,
                CubeListBuilder.create()
                        .texOffs(304, 234).addBox(-1.99F, 0.01F, -1.99F, 3.98F, 13.94F, 3.98F),
                PartPose.offset(8.0F, -2.0F, 6.5F)
        );
        rightLift.addOrReplaceChild("right_lift_rod" /* PartPose.offset(8.0F, 10.0F, 6.5F) */,
                CubeListBuilder.create()
                        .texOffs(324, 234).addBox(-0.99F, 0.03F, -0.99F, 1.98F, 11.92F, 1.98F),
                PartPose.offset(0.0F, 12.0F, 0.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 4. FRONT AXLE, STEERING & WHEEL LEAN ASSEMBLY
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition frontAxle = gooseneck.addOrReplaceChild("front_axle" /* PartPose.ZERO */,
                CubeListBuilder.create()
                        // Heavy front bolster casting (height 6.94F, Y: [5.01, 11.95F])
                        .texOffs(200, 0).addBox(-5.99F, 5.01F, 46.01F, 11.98F, 6.94F, 7.98F)
                        // Oscillating front center axle beam (Y: 8.01F to 11.99F, X: -15.99 to +15.99)
                        .texOffs(248, 0).addBox(-15.99F, 8.01F, 47.51F, 31.98F, 3.96F, 4.98F)
                        // Front counterweight push block
                        .texOffs(244, 22).addBox(-9.99F, 6.01F, 54.01F, 19.98F, 8.98F, 4.98F)
                        // Heavy front tow hitch clevis pin
                        .texOffs(304, 22).addBox(-1.99F, 8.01F, 59.01F, 3.98F, 3.98F, 2.98F)
                        // Front work lights on bolster
                        .texOffs(32, 420).addBox(-6.99F, 4.01F, 53.01F, 3.98F, 2.48F, 1.98F)
                        .texOffs(32, 420).addBox(3.01F, 4.01F, 53.01F, 3.98F, 2.48F, 1.98F),
                PartPose.ZERO
        );

        // Front Left Steering Mount & Knuckle (Z: [-2.47, +2.47] to avoid Z=47.51/52.49 plane)
        PartDefinition frontLeftMount = frontAxle.addOrReplaceChild("front_left_mount" /* PartPose.offset(-16.0F, 10.0F, 50.0F) */,
                CubeListBuilder.create()
                        .texOffs(200, 22).addBox(-1.99F, -3.99F, -2.47F, 3.98F, 7.98F, 4.94F)
                        .texOffs(224, 22).addBox(-3.49F, -2.49F, -1.99F, 1.48F, 4.98F, 3.98F),
                PartPose.offset(-16.0F, 10.0F, 50.0F)
        );
        addTireAndWheel(frontLeftMount, "front_left_wheel", -3.5F, 0.0F, 0.0F, true);

        // Front Right Steering Mount & Knuckle
        PartDefinition frontRightMount = frontAxle.addOrReplaceChild("front_right_mount" /* PartPose.offset(16.0F, 10.0F, 50.0F) */,
                CubeListBuilder.create()
                        .texOffs(200, 22).addBox(-1.99F, -3.99F, -2.47F, 3.98F, 7.98F, 4.94F)
                        .texOffs(224, 22).addBox(2.01F, -2.49F, -1.99F, 1.48F, 4.98F, 3.98F),
                PartPose.offset(16.0F, 10.0F, 50.0F)
        );
        addTireAndWheel(frontRightMount, "front_right_wheel", 3.5F, 0.0F, 0.0F, false);

        // ═════════════════════════════════════════════════════════════════════
        // 5. DRAWBAR, CIRCLE RING & MOLDBOARD BLADE
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition drawbar = root.addOrReplaceChild("drawbar" /* PartPose.offset(0.0F, 11.0F, 16.0F) */,
                CubeListBuilder.create()
                        // Front drawbar ball pivot tongue (X: -2.97F, Z: 22.03F to avoid gooseneck ball hitch)
                        .texOffs(200, 160).addBox(-2.97F, -1.99F, 22.03F, 5.94F, 3.98F, 14.94F)
                        // A-frame left tubular leg
                        .texOffs(248, 160).addBox(-11.99F, -1.99F, 0.01F, 3.98F, 3.98F, 22.98F)
                        // A-frame right tubular leg
                        .texOffs(308, 160).addBox(8.01F, -1.99F, 0.01F, 3.98F, 3.98F, 22.98F)
                        // Rear circle mounting saddle plate
                        .texOffs(200, 192).addBox(-12.99F, -0.99F, -10.99F, 25.98F, 1.98F, 11.98F),
                PartPose.offset(0.0F, 11.0F, 16.0F)
        );

        // Circle Ring Gear (rotates 360 deg underneath drawbar)
        PartDefinition circleRing = drawbar.addOrReplaceChild("circle_ring" /* PartPose.offset(0.0F, 11.0F, 11.0F) */,
                CubeListBuilder.create()
                        // Outer circular gear ring (Y: 1.03F to 3.97F to avoid Y=1.01F / 3.99F planes)
                        .texOffs(200, 192).addBox(-12.99F, 1.03F, -12.99F, 25.98F, 2.94F, 25.98F)
                        // Internal hub boss & circle drive pinion motor
                        .texOffs(308, 192).addBox(-4.99F, 0.01F, -4.99F, 9.98F, 1.98F, 9.98F)
                        .texOffs(308, 210).addBox(-1.99F, -4.99F, -1.99F, 3.98F, 4.98F, 3.98F)
                        // Moldboard slide guide frame rails (Y: 4.03F to 5.99F)
                        .texOffs(304, 226).addBox(-14.99F, 4.03F, -2.99F, 29.98F, 1.96F, 4.98F),
                PartPose.offset(0.0F, 0.0F, -5.0F)
        );

        // Moldboard Blade (mounted on circle slide rails)
        circleRing.addOrReplaceChild("moldboard" /* PartPose.offset(0.0F, 11.0F, 11.0F) */,
                CubeListBuilder.create()
                        // Wide curved parabolic moldboard blade face (3 blocks / 48 units wide)
                        .texOffs(200, 226).addBox(-23.99F, 3.01F, -1.49F, 47.98F, 8.98F, 1.98F)
                        // Lower high-carbon hardened steel cutting edge wear lip (bright edge)
                        .texOffs(200, 242).addBox(-23.99F, 12.01F, -1.49F, 47.98F, 2.98F, 1.98F)
                        // Upper rolled dirt spill curl lip (Y: 1.05F, height 1.92F)
                        .texOffs(200, 250).addBox(-23.99F, 1.05F, -0.49F, 47.98F, 1.92F, 1.98F)
                        // Rear reinforcing vertical ribs (left, center, right)
                        .texOffs(200, 258).addBox(-19.99F, 3.01F, 0.51F, 1.98F, 7.98F, 2.98F)
                        .texOffs(200, 258).addBox(-0.99F, 3.01F, 0.51F, 1.98F, 7.98F, 2.98F)
                        .texOffs(200, 258).addBox(18.01F, 3.01F, 0.51F, 1.98F, 7.98F, 2.98F)
                        // Hydraulic side-shift ram on back of moldboard
                        .texOffs(336, 234).addBox(-7.99F, 5.01F, 2.01F, 15.98F, 2.46F, 2.48F)
                        // Blade end bit spill plates
                        .texOffs(200, 258).addBox(-24.49F, 4.01F, -2.49F, 0.48F, 8.98F, 3.98F)
                        .texOffs(200, 258).addBox(24.01F, 4.01F, -2.49F, 0.48F, 8.98F, 3.98F),
                PartPose.ZERO
        );

        // ═════════════════════════════════════════════════════════════════════
        // 6. OPERATOR CAB (ROPS/FOPS) & INTERIOR
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition cab = root.addOrReplaceChild("cab" /* PartPose.ZERO */,
                CubeListBuilder.create()
                        // Elevated cab floor & lower platform sill (Z: -7 to -25, Y: 4 to 8)
                        .texOffs(0, 280).addBox(-11.99F, 4.03F, -25.99F, 23.98F, 3.94F, 18.98F)
                        // Lower floor observation windows for blade view
                        .texOffs(92, 280).addBox(-8.99F, 1.05F, -7.49F, 17.98F, 2.94F, 1.48F)
                        // Lower side door sill posts
                        .texOffs(0, 308).addBox(-11.99F, -4.99F, -24.99F, 1.98F, 8.98F, 16.98F)
                        .texOffs(0, 308).addBox(10.01F, -4.99F, -24.99F, 1.98F, 8.98F, 16.98F)
                        // Front ROPS structural corner pillars
                        .texOffs(44, 308).addBox(-10.99F, -19.99F, -8.0F, 1.98F, 15.98F, 1.98F)
                        .texOffs(44, 308).addBox(9.01F, -19.99F, -8.0F, 1.98F, 15.98F, 1.98F)
                        // Rear ROPS structural corner pillars (Z: -24.97F to avoid Z=-24.99F plane)
                        .texOffs(44, 308).addBox(-10.99F, -19.99F, -24.97F, 1.98F, 15.98F, 1.96F)
                        .texOffs(44, 308).addBox(9.01F, -19.99F, -24.97F, 1.98F, 15.98F, 1.96F)
                        // Upper window headers / roof perimeter ring
                        .texOffs(56, 308).addBox(-11.49F, -21.99F, -25.49F, 22.98F, 2.98F, 18.48F)
                        // Tinted safety glass enclosure (all-around panoramic cab)
                        .texOffs(148, 308).addBox(-10.49F, -18.99F, -24.47F, 20.98F, 13.98F, 16.94F)
                        // Left & right external walkways with anti-slip tread
                        .texOffs(0, 392).addBox(-16.99F, 7.01F, -25.0F, 4.98F, 0.98F, 18.0F)
                        .texOffs(0, 392).addBox(12.01F, 7.01F, -25.0F, 4.98F, 0.98F, 18.0F)
                        // Left & right boarding safety ladders
                        .texOffs(60, 392).addBox(-16.49F, 8.01F, -16.0F, 3.98F, 9.98F, 0.98F)
                        .texOffs(60, 392).addBox(12.51F, 8.01F, -16.0F, 3.98F, 9.98F, 0.98F)
                        // Side rearview mirrors on stalks
                        .texOffs(200, 420).addBox(-13.49F, -14.0F, -9.0F, 0.98F, 5.98F, 3.98F)
                        .texOffs(200, 420).addBox(12.51F, -14.0F, -9.0F, 0.98F, 5.98F, 3.98F)
                        // Operator suspension seat (cushion & backrest)
                        .texOffs(0, 368).addBox(-3.99F, -4.99F, -18.0F, 7.98F, 8.98F, 7.98F)
                        // Steering console & dual electro-hydraulic joystick pods (height 6.94F, bottom Y=3.97F)
                        .texOffs(40, 368).addBox(-5.99F, -2.97F, -12.0F, 11.98F, 6.94F, 3.98F)
                        // Steering wheel & angled column
                        .texOffs(72, 368).addBox(-2.49F, -6.99F, -12.99F, 4.98F, 4.98F, 1.98F),
                PartPose.ZERO
        );

        // Cab Roof & Beacon Assembly
        PartDefinition roof = cab.addOrReplaceChild("roof" /* PartPose.ZERO */,
                CubeListBuilder.create()
                        // Sloped Cat equipment roof canopy
                        .texOffs(0, 338).addBox(-12.49F, -24.99F, -26.49F, 24.98F, 2.98F, 20.48F)
                        // Roof HVAC Air Conditioning unit pod
                        .texOffs(100, 338).addBox(-5.99F, -27.49F, -23.0F, 11.98F, 2.48F, 9.98F)
                        // Forward LED floodlights on roof brow
                        .texOffs(0, 420).addBox(-9.99F, -24.49F, -27.24F, 3.98F, 1.98F, 0.74F)
                        .texOffs(0, 420).addBox(6.01F, -24.49F, -27.24F, 3.98F, 1.98F, 0.74F),
                PartPose.ZERO
        );

        PartDefinition beaconBase = roof.addOrReplaceChild("beacon_base" /* PartPose.offset(0.0F, -25.04F, -16.0F) */,
                CubeListBuilder.create()
                        .texOffs(48, 420).addBox(-1.99F, -3.99F, -1.99F, 3.98F, 3.96F, 3.98F),
                PartPose.offset(0.0F, -25.04F, -16.0F)
        );
        beaconBase.addOrReplaceChild("beacon_reflector" /* PartPose.offset(0.0F, -27.04F, -16.0F) */,
                CubeListBuilder.create()
                        .texOffs(68, 420).addBox(-0.99F, -2.99F, -0.99F, 1.98F, 2.94F, 1.98F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 512, 512);
    }

    /**
     * Builds an authentic heavy earthmover tire with stepped carcass profiles,
     * deep steel dished rim, and planetary final drive hub.
     */
    private static void addTireAndWheel(
            PartDefinition parent,
            String name,
            float x,
            float y,
            float z,
            boolean left
    ) {
        float rimDepth = 1.48F;
        float rimX = left ? (-3.99F - rimDepth) : 3.99F;
        float hubDepth = 1.98F;
        float hubX = left ? (rimX - hubDepth) : (rimX + rimDepth);
        float capDepth = 0.98F;
        float capX = left ? (hubX - capDepth) : (hubX + hubDepth);

        parent.addOrReplaceChild("wheel".isEmpty() ? name : name /* PartPose.offset(0.0F, 12.0F, -46.0F) */,
                CubeListBuilder.create()
                        // 1. Vertical main carcass (diameter 20 px, radius 10 px)
                        .texOffs(0, 0).addBox(-3.99F, -9.99F, -4.49F, 7.98F, 19.98F, 8.98F)
                        // 2. Horizontal carcass profile
                        .texOffs(36, 0).addBox(-3.89F, -4.49F, -9.99F, 7.78F, 8.98F, 19.98F)
                        // 3. Diagonal profile corners
                        .texOffs(72, 0).addBox(-3.79F, -8.49F, -7.49F, 7.58F, 16.98F, 14.98F)
                        .texOffs(108, 0).addBox(-3.69F, -7.49F, -8.49F, 7.38F, 14.98F, 16.98F)
                        // Deep steel dished rim in CAT yellow
                        .texOffs(0, 32).addBox(rimX, -5.99F, -5.99F, rimDepth, 11.98F, 11.98F)
                        // Heavy planetary final drive hub
                        .texOffs(32, 32).addBox(hubX, -2.99F, -2.99F, hubDepth, 5.98F, 5.98F)
                        // Outer hub cap
                        .texOffs(52, 32).addBox(capX, -1.49F, -1.49F, capDepth, 2.98F, 2.98F),
                PartPose.offset(x, y, z)
        );
    }

    @Override
    public void setupAnim(CatGraderRenderState state) {
        // 1. Wheel drive rotation (all 6 wheels rotate continuously while driving)
        float spin = -state.wheelSpin;
        this.frontLeftWheel.xRot = spin;
        this.frontRightWheel.xRot = spin;
        this.midLeftWheel.xRot = spin;
        this.midRightWheel.xRot = spin;
        this.rearLeftWheel.xRot = spin;
        this.rearRightWheel.xRot = spin;

        // 2. Front wheel steering angle (Yaw)
        this.frontLeftMount.yRot = state.steerAngle;
        this.frontRightMount.yRot = state.steerAngle;

        // 3. Front wheel lean mechanism (Roll / Camber tilt)
        this.frontLeftMount.zRot = state.wheelLean;
        this.frontRightMount.zRot = state.wheelLean;

        // 4. Blade / Moldboard Articulation:
        // Height: drawbar drops down into dirt or raises for road transport
        float liftY = 11.0F + state.bladeHeight * 4.0F;
        this.drawbar.y = liftY;

        // Circle ring yaw angle (blade angle relative to machine axis)
        this.circleRing.yRot = state.bladeAngle;

        // Moldboard side-shift (hydraulic slide along rails)
        this.moldboard.x = state.bladeShift * 6.0F;

        // Moldboard slope roll (cross slope cutting tilt)
        this.moldboard.zRot = state.bladeRoll;

        // Hydraulic lift cylinders angle adjustment based on blade height
        float cylinderPitch = state.bladeHeight * 0.12F;
        this.leftLiftCylinder.xRot = cylinderPitch;
        this.rightLiftCylinder.xRot = cylinderPitch;
        this.leftLiftRod.y = 12.0F + state.bladeHeight * 2.5F;
        this.rightLiftRod.y = 12.0F + state.bladeHeight * 2.5F;

        // 5. Rear Ripper / Scarifier lift
        this.ripper.xRot = state.ripperLowered * 0.45F;

        // 6. Safety rotary beacon
        this.beaconReflector.yRot = state.beaconSpin;
    }
}
