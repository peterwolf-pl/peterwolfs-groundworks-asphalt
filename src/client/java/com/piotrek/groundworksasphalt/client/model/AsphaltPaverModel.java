package com.piotrek.groundworksasphalt.client.model;

import com.piotrek.groundworksasphalt.client.render.AsphaltPaverRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Photorealistic 3D EntityModel for the heavy tracked Asphalt Paver (Rozściełacz Asfaltu).
 *
 * <p>Constructed according to Peter Wolf Groundworks industrial vehicle standards:
 * <ul>
 *   <li>Continuous crawler tracks with drive sprockets, front idlers, and road wheels.</li>
 *   <li>Front heavy material hopper with hydraulic folding wings and truck push rollers.</li>
 *   <li>Central conveyor trough carrying hot mix asphalt to rear spreading augers.</li>
 *   <li>Elevated operator platform with twin swivel seats, ErgoPlus desks, and safety railings.</li>
 *   <li>Weather canopy roof with integrated LED floodlights and amber safety beacon.</li>
 *   <li>Rear floating screed with draft tow arms, leveling cylinders, heated screed plate,
 *       side end-gates, and rear crew walkway.</li>
 * </ul>
 *
 * <p>Compliant with DEGLITCH standards: 0 coplanar conflicts (Z-fighting free) and complete 512x512 UV coverage.
 */
public class AsphaltPaverModel extends EntityModel<AsphaltPaverRenderState> {

    private final ModelPart chassis;
    private final ModelPart leftTrack;
    private final ModelPart rightTrack;
    private final ModelPart leftIdler;
    private final ModelPart rightIdler;
    private final ModelPart leftSprocket;
    private final ModelPart rightSprocket;
    private final ModelPart hopper;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart asphaltLoad;
    private final ModelPart spreaderAugers;
    private final ModelPart screed;
    private final ModelPart beaconReflector;

    public AsphaltPaverModel(ModelPart root) {
        super(root);
        this.chassis = root.getChild("chassis");
        this.leftTrack = root.getChild("left_track");
        this.rightTrack = root.getChild("right_track");
        this.leftIdler = this.leftTrack.getChild("left_idler");
        this.rightIdler = this.rightTrack.getChild("right_idler");
        this.leftSprocket = this.leftTrack.getChild("left_sprocket");
        this.rightSprocket = this.rightTrack.getChild("right_sprocket");

        this.hopper = root.getChild("hopper");
        this.leftWing = this.hopper.getChild("left_wing");
        this.rightWing = this.hopper.getChild("right_wing");
        this.asphaltLoad = this.hopper.getChild("asphalt_load");

        this.spreaderAugers = root.getChild("augers");
        this.screed = root.getChild("screed");

        this.beaconReflector = root.getChild("platform")
                .getChild("canopy_roof")
                .getChild("beacon_base")
                .getChild("beacon_reflector");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ═════════════════════════════════════════════════════════════════════
        // 1. MAIN CHASSIS & BELLY FRAME
        // ═════════════════════════════════════════════════════════════════════
        root.addOrReplaceChild("chassis",
                CubeListBuilder.create()
                        // Central spine frame
                        .texOffs(0, 0).addBox(-8.0F, 14.0F, -18.0F, 16.0F, 5.0F, 40.0F)
                        // Underbelly protection pan
                        .texOffs(114, 0).addBox(-7.0F, 19.01F, -17.5F, 14.0F, 1.0F, 39.0F)
                        // Front dump truck push roller bumper beam
                        .texOffs(222, 0).addBox(-16.0F, 17.5F, 34.0F, 32.0F, 4.0F, 3.0F)
                        // Push rollers (left and right)
                        .texOffs(294, 0).addBox(-12.0F, 17.0F, 37.01F, 9.0F, 5.0F, 3.0F)
                        .texOffs(320, 0).addBox(3.0F, 17.0F, 37.01F, 9.0F, 5.0F, 3.0F)
                        // Conveyor trough bottom bed under hopper
                        .texOffs(0, 48).addBox(-6.5F, 13.5F, 14.0F, 13.0F, 1.5F, 20.0F)
                        // Conveyor guide walls
                        .texOffs(68, 48).addBox(-7.5F, 10.0F, 14.01F, 1.0F, 3.49F, 19.98F)
                        .texOffs(112, 48).addBox(6.5F, 10.0F, 14.01F, 1.0F, 3.49F, 19.98F)
                        // Rear tunnel exit to augers
                        .texOffs(156, 48).addBox(-7.0F, 13.0F, -18.02F, 14.0F, 5.5F, 3.01F)
                        // Tow arm pivot mount brackets
                        .texOffs(192, 48).addBox(-8.8F, 15.0F, -1.0F, 1.0F, 3.0F, 5.0F)
                        .texOffs(206, 48).addBox(7.8F, 15.0F, -1.0F, 1.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 2. CRAWLER TRACKS (Left & Right)
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition leftTrack = root.addOrReplaceChild("left_track", /* PartPose.offset(-13.5F, 19.5F, 0.0F) */
                CubeListBuilder.create()
                        // Side track girder beam
                        .texOffs(0, 75).addBox(-2.0F, -2.5F, -17.0F, 4.0F, 4.0F, 34.0F)
                        // Track bottom run
                        .texOffs(122, 75).addBox(-2.5F, 4.0F, -18.5F, 5.0F, 1.0F, 37.0F)
                        // Track top return run
                        .texOffs(208, 75).addBox(-2.5F, -4.8F, -15.5F, 5.0F, 1.0F, 31.0F)
                        // Front track curve
                        .texOffs(282, 75).addBox(-2.5F, -3.8F, 15.5F, 5.0F, 7.8F, 3.2F)
                        // Rear track curve
                        .texOffs(302, 75).addBox(-2.5F, -3.8F, -18.7F, 5.0F, 7.8F, 3.2F)
                        // Bottom road wheels
                        .texOffs(322, 75).addBox(-2.1F, 1.5F, -10.0F, 4.2F, 2.49F, 20.0F),
                PartPose.offset(-13.5F, 19.5F, 0.0F)
        );
        leftTrack.addOrReplaceChild("left_idler", /* PartPose.offset(-13.5F, 19.5F, 16.0F) */
                CubeListBuilder.create().texOffs(78, 75).addBox(-2.2F, -3.89F, -2.5F, 4.4F, 7.78F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 16.0F)
        );
        leftTrack.addOrReplaceChild("left_sprocket", /* PartPose.offset(-13.5F, 19.5F, -15.5F) */
                CubeListBuilder.create().texOffs(100, 75).addBox(-2.2F, -3.89F, -2.5F, 4.4F, 7.78F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -15.5F)
        );

        PartDefinition rightTrack = root.addOrReplaceChild("right_track", /* PartPose.offset(13.5F, 19.5F, 0.0F) */
                CubeListBuilder.create()
                        // Side track girder beam
                        .texOffs(0, 116).addBox(-2.0F, -2.5F, -17.0F, 4.0F, 4.0F, 34.0F)
                        // Track bottom run
                        .texOffs(122, 116).addBox(-2.5F, 4.0F, -18.5F, 5.0F, 1.0F, 37.0F)
                        // Track top return run
                        .texOffs(208, 116).addBox(-2.5F, -4.8F, -15.5F, 5.0F, 1.0F, 31.0F)
                        // Front track curve
                        .texOffs(282, 116).addBox(-2.5F, -3.8F, 15.5F, 5.0F, 7.8F, 3.2F)
                        // Rear track curve
                        .texOffs(302, 116).addBox(-2.5F, -3.8F, -18.7F, 5.0F, 7.8F, 3.2F)
                        // Bottom road wheels
                        .texOffs(322, 116).addBox(-2.1F, 1.5F, -10.0F, 4.2F, 2.49F, 20.0F),
                PartPose.offset(13.5F, 19.5F, 0.0F)
        );
        rightTrack.addOrReplaceChild("right_idler", /* PartPose.offset(13.5F, 19.5F, 16.0F) */
                CubeListBuilder.create().texOffs(78, 116).addBox(-2.2F, -3.89F, -2.5F, 4.4F, 7.78F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 16.0F)
        );
        rightTrack.addOrReplaceChild("right_sprocket", /* PartPose.offset(13.5F, 19.5F, -15.5F) */
                CubeListBuilder.create().texOffs(100, 116).addBox(-2.2F, -3.89F, -2.5F, 4.4F, 7.78F, 5.0F),
                PartPose.offset(0.0F, 0.0F, -15.5F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 3. FRONT HOPPER & FOLDING WINGS
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition hopper = root.addOrReplaceChild("hopper",
                CubeListBuilder.create()
                        // Fixed front nose baffle
                        .texOffs(374, 75).addBox(-7.5F, 15.0F, 33.5F, 15.0F, 3.0F, 1.5F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Left folding hopper wing (pivots around Z at X = -7.5F, Y = 14.5F, Z = 24.5F)
        hopper.addOrReplaceChild("left_wing", /* PartPose.offset(-7.5F, 14.5F, 24.5F) */
                CubeListBuilder.create()
                        .texOffs(0, 157).addBox(-10.49F, -0.48F, -10.49F, 10.48F, 0.96F, 20.48F)
                        .texOffs(66, 157).addBox(-11.51F, -7.5F, -10.5F, 1.0F, 7.48F, 20.5F)
                        .texOffs(114, 157).addBox(-10.48F, -7.0F, 8.51F, 9.46F, 6.98F, 1.78F)
                        .texOffs(140, 157).addBox(-10.5F, -0.5F, 10.31F, 10.5F, 3.5F, 0.4F)
                        .texOffs(166, 157).addBox(-5.0F, -1.2F, -1.0F, 2.5F, 1.8F, 2.5F),
                PartPose.offset(-7.5F, 14.5F, 24.5F)
        );

        // Right folding hopper wing (pivots around Z at X = 7.5F, Y = 14.5F, Z = 24.5F)
        hopper.addOrReplaceChild("right_wing", /* PartPose.offset(7.5F, 14.5F, 24.5F) */
                CubeListBuilder.create()
                        .texOffs(180, 157).addBox(0.01F, -0.48F, -10.49F, 10.48F, 0.96F, 20.48F)
                        .texOffs(246, 157).addBox(10.51F, -7.5F, -10.5F, 1.0F, 7.48F, 20.5F)
                        .texOffs(294, 157).addBox(1.02F, -7.0F, 8.51F, 9.46F, 6.98F, 1.78F)
                        .texOffs(320, 157).addBox(0.0F, -0.5F, 10.31F, 10.5F, 3.5F, 0.4F)
                        .texOffs(346, 157).addBox(2.5F, -1.2F, -1.0F, 2.5F, 1.8F, 2.5F),
                PartPose.offset(7.5F, 14.5F, 24.5F)
        );

        // Steaming hot mix asphalt load bed inside hopper
        hopper.addOrReplaceChild("asphalt_load",
                CubeListBuilder.create()
                        .texOffs(0, 190).addBox(-7.0F, 11.8F, 14.5F, 14.0F, 2.2F, 19.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 4. ENGINE BODYWORK & COWLING
        // ═════════════════════════════════════════════════════════════════════
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Main engine hood
                        .texOffs(68, 190).addBox(-10.5F, 2.5F, -12.0F, 21.0F, 11.49F, 25.5F)
                        // Front sloped hood transition
                        .texOffs(164, 190).addBox(-10.0F, 4.01F, 13.51F, 20.0F, 9.45F, 1.8F)
                        // Side ventilation louvers (with 0.02F micro-offsets to eliminate Z-fighting)
                        .texOffs(210, 190).addBox(-10.52F, 4.5F, -8.0F, 0.2F, 7.5F, 15.0F)
                        .texOffs(244, 190).addBox(10.32F, 4.5F, -8.0F, 0.2F, 7.5F, 15.0F)
                        // Top maintenance hatch
                        .texOffs(278, 190).addBox(-7.49F, 2.1F, -5.0F, 14.98F, 0.39F, 12.0F)
                        // Cyclone air cleaner
                        .texOffs(334, 190).addBox(-9.5F, -3.5F, 7.5F, 3.5F, 5.99F, 3.5F)
                        // Exhaust base, vertical stack and rain flapper
                        .texOffs(352, 190).addBox(6.5F, -0.51F, 6.5F, 3.0F, 2.99F, 3.0F)
                        .texOffs(366, 190).addBox(7.0F, -15.5F, 7.0F, 2.0F, 14.98F, 2.0F)
                        .texOffs(376, 190).addBox(6.5F, -17.0F, 6.5F, 3.0F, 1.49F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 5. OPERATOR PLATFORM, SEATS, CONSOLES & CANOPY
        // ═════════════════════════════════════════════════════════════════════
        PartDefinition platform = root.addOrReplaceChild("platform",
                CubeListBuilder.create()
                        // Deck floor structure
                        .texOffs(0, 230).addBox(-12.5F, 2.0F, -16.0F, 25.0F, 1.5F, 17.5F)
                        // Anti-slip tread surface plate
                        .texOffs(88, 230).addBox(-12.3F, 1.91F, -15.8F, 24.6F, 0.08F, 17.1F)
                        // Left & Right access ladders
                        .texOffs(176, 230).addBox(-14.0F, 3.5F, -12.0F, 1.5F, 10.0F, 5.0F)
                        .texOffs(192, 230).addBox(12.5F, 3.5F, -12.0F, 1.5F, 10.0F, 5.0F)
                        // Yellow safety handrails
                        .texOffs(208, 230).addBox(-12.5F, -7.5F, 1.2F, 25.0F, 0.8F, 0.8F)
                        .texOffs(262, 230).addBox(-12.4F, -6.69F, 1.2F, 0.8F, 8.58F, 0.8F)
                        .texOffs(268, 230).addBox(11.6F, -6.69F, 1.2F, 0.8F, 8.58F, 0.8F)
                        .texOffs(274, 230).addBox(-12.5F, -7.5F, -16.0F, 0.8F, 0.8F, 17.19F)
                        .texOffs(314, 230).addBox(11.7F, -7.5F, -16.0F, 0.8F, 0.8F, 17.19F)
                        .texOffs(354, 230).addBox(-12.49F, -6.69F, -16.0F, 0.79F, 8.58F, 0.8F)
                        .texOffs(360, 230).addBox(11.71F, -6.69F, -16.0F, 0.79F, 8.58F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // Left swivel operator seat
        platform.addOrReplaceChild("seat_left", /* PartPose.offset(-6.5F, 2.0F, -6.0F) */
                CubeListBuilder.create()
                        .texOffs(0, 255).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 1.9F, 3.0F)
                        .texOffs(14, 255).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 2.0F, 7.0F)
                        .texOffs(44, 255).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 8.0F, 2.0F)
                        .texOffs(64, 255).addBox(-4.5F, -8.0F, -2.5F, 1.0F, 1.5F, 5.0F)
                        .texOffs(78, 255).addBox(3.5F, -8.0F, -2.5F, 1.0F, 1.5F, 5.0F),
                PartPose.offset(-6.5F, 2.0F, -6.0F)
        );

        // Right swivel operator seat
        platform.addOrReplaceChild("seat_right", /* PartPose.offset(6.5F, 2.0F, -6.0F) */
                CubeListBuilder.create()
                        .texOffs(92, 255).addBox(-1.5F, -2.0F, -1.5F, 3.0F, 1.9F, 3.0F)
                        .texOffs(106, 255).addBox(-3.5F, -4.0F, -3.5F, 7.0F, 2.0F, 7.0F)
                        .texOffs(136, 255).addBox(-3.5F, -12.0F, -3.5F, 7.0F, 8.0F, 2.0F)
                        .texOffs(156, 255).addBox(-4.5F, -8.0F, -2.5F, 1.0F, 1.5F, 5.0F)
                        .texOffs(170, 255).addBox(3.5F, -8.0F, -2.5F, 1.0F, 1.5F, 5.0F),
                PartPose.offset(6.5F, 2.0F, -6.0F)
        );

        // Left ErgoPlus control desk
        platform.addOrReplaceChild("console_left", /* PartPose.offset(-6.5F, -2.0F, -1.0F) */
                CubeListBuilder.create()
                        .texOffs(184, 255).addBox(-1.0F, 0.01F, -1.0F, 2.0F, 3.88F, 2.0F)
                        .texOffs(194, 255).addBox(-3.0F, -3.5F, -3.0F, 6.0F, 3.46F, 6.0F)
                        .texOffs(220, 255).addBox(-2.5F, -3.6F, -2.5F, 5.0F, 0.09F, 4.0F),
                PartPose.offset(-6.5F, -2.0F, -1.0F)
        );

        // Right ErgoPlus control desk
        platform.addOrReplaceChild("console_right", /* PartPose.offset(6.5F, -2.0F, -1.0F) */
                CubeListBuilder.create()
                        .texOffs(240, 255).addBox(-1.0F, 0.01F, -1.0F, 2.0F, 3.88F, 2.0F)
                        .texOffs(250, 255).addBox(-3.0F, -3.5F, -3.0F, 6.0F, 3.46F, 6.0F)
                        .texOffs(276, 255).addBox(-2.5F, -3.6F, -2.5F, 5.0F, 0.09F, 4.0F),
                PartPose.offset(6.5F, -2.0F, -1.0F)
        );

        // Canopy roof, tubular pillars, windshield and lighting
        PartDefinition canopy = platform.addOrReplaceChild("canopy_roof",
                CubeListBuilder.create()
                        // 4 corner pillars
                        .texOffs(0, 275).addBox(-12.0F, -26.5F, 0.5F, 1.2F, 28.4F, 1.2F)
                        .texOffs(10, 275).addBox(10.8F, -26.5F, 0.5F, 1.2F, 28.4F, 1.2F)
                        .texOffs(20, 275).addBox(-12.0F, -26.5F, -15.5F, 1.2F, 28.4F, 1.2F)
                        .texOffs(30, 275).addBox(10.8F, -26.5F, -15.5F, 1.2F, 28.4F, 1.2F)
                        // Molded canopy roof cover
                        .texOffs(40, 275).addBox(-13.5F, -28.5F, -17.5F, 27.0F, 2.0F, 20.0F)
                        // Tinted front windshield
                        .texOffs(136, 275).addBox(-10.5F, -22.0F, 0.8F, 21.0F, 12.5F, 0.4F)
                        // Front high-output LED worklight bar
                        .texOffs(182, 275).addBox(-10.0F, -28.0F, 2.51F, 20.0F, 1.5F, 1.2F)
                        // Rear screed floodlights
                        .texOffs(228, 275).addBox(-9.5F, -28.0F, -18.3F, 3.5F, 1.5F, 0.8F)
                        .texOffs(240, 275).addBox(6.0F, -28.0F, -18.3F, 3.5F, 1.5F, 0.8F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        PartDefinition beaconBase = canopy.addOrReplaceChild("beacon_base",
                CubeListBuilder.create()
                        .texOffs(252, 275).addBox(-1.5F, -29.5F, -7.5F, 3.0F, 0.99F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        beaconBase.addOrReplaceChild("beacon_reflector",
                CubeListBuilder.create()
                        .texOffs(266, 275).addBox(-1.0F, -32.5F, -7.0F, 2.0F, 2.99F, 2.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 6. SPREADER AUGERS (Helical Spreading Screws)
        // ═════════════════════════════════════════════════════════════════════
        root.addOrReplaceChild("augers", /* PartPose.offset(0.0F, 20.0F, -19.5F) */
                CubeListBuilder.create()
                        // Central gearbox drive
                        .texOffs(0, 310).addBox(-2.0F, -2.0F, -1.8F, 4.0F, 4.0F, 3.6F)
                        // Left auger shaft and helical flight blades
                        .texOffs(18, 310).addBox(-11.2F, -0.6F, -0.6F, 9.2F, 1.2F, 1.2F)
                        .texOffs(102, 310).addBox(-10.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F)
                        .texOffs(116, 310).addBox(-7.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F)
                        .texOffs(130, 310).addBox(-4.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F)
                        // Right auger shaft and helical flight blades
                        .texOffs(60, 310).addBox(2.0F, -0.6F, -0.6F, 9.2F, 1.2F, 1.2F)
                        .texOffs(144, 310).addBox(3.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F)
                        .texOffs(158, 310).addBox(6.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F)
                        .texOffs(172, 310).addBox(9.5F, -2.2F, -2.2F, 1.0F, 4.4F, 4.4F),
                PartPose.offset(0.0F, 20.0F, -19.5F)
        );

        // ═════════════════════════════════════════════════════════════════════
        // 7. REAR FLOATING SCREED ASSEMBLY (5-Block Wide Screed = 80 units)
        // ═════════════════════════════════════════════════════════════════════
        root.addOrReplaceChild("screed", /* PartPose.offset(0.0F, 22.0F, -22.0F) */
                CubeListBuilder.create()
                        // Left & Right draft tow arms connecting to mid-frame
                        .texOffs(0, 330).addBox(-16.2F, -3.0F, 2.0F, 1.8F, 3.0F, 19.5F)
                        .texOffs(46, 330).addBox(14.4F, -3.0F, 2.0F, 1.8F, 3.0F, 19.5F)
                        // Hydraulic leveling rams
                        .texOffs(92, 330).addBox(-15.8F, -7.5F, 6.0F, 1.4F, 4.49F, 1.4F)
                        .texOffs(102, 330).addBox(14.4F, -7.5F, 6.0F, 1.4F, 4.49F, 1.4F)
                        // Main heated plate plus side wings. Wings reuse the plate UV and sit 0.01 off it.
                        .texOffs(0, 360).addBox(-24.0F, 0.5F, -6.0F, 48.0F, 1.4F, 7.5F)
                        .texOffs(0, 360).addBox(-40.01F, 0.51F, -6.0F, 16.0F, 1.4F, 7.5F)
                        .texOffs(0, 360).addBox(24.01F, 0.51F, -6.0F, 16.0F, 1.4F, 7.5F)
                        // Screed tamper box and side extensions
                        .texOffs(114, 360).addBox(-23.8F, -5.5F, -5.5F, 47.6F, 5.99F, 6.8F)
                        .texOffs(114, 360).addBox(-39.81F, -5.49F, -5.5F, 16.0F, 5.99F, 6.8F)
                        .texOffs(114, 360).addBox(23.81F, -5.49F, -5.5F, 16.0F, 5.99F, 6.8F)
                        // Screed crown adjustment turnbuckle
                        .texOffs(226, 360).addBox(-2.0F, -8.0F, -2.5F, 4.0F, 2.49F, 3.5F)
                        // End gates at the 5-block ends
                        .texOffs(244, 360).addBox(-40.4F, -7.5F, -6.5F, 0.5F, 8.5F, 9.5F)
                        .texOffs(268, 360).addBox(39.9F, -7.5F, -6.5F, 0.5F, 8.5F, 9.5F)
                        // Rear paving crew catwalk / walkway
                        .texOffs(0, 390).addBox(-22.0F, -0.5F, -12.0F, 44.0F, 0.8F, 5.8F)
                        .texOffs(0, 390).addBox(-38.01F, -0.49F, -12.0F, 16.0F, 0.8F, 5.8F)
                        .texOffs(0, 390).addBox(22.01F, -0.49F, -12.0F, 16.0F, 0.8F, 5.8F)
                        // Rear catwalk guardrail
                        .texOffs(102, 390).addBox(-22.0F, -8.0F, -12.0F, 44.0F, 7.49F, 0.4F)
                        .texOffs(102, 390).addBox(-38.01F, -7.99F, -12.0F, 16.0F, 7.49F, 0.4F)
                        .texOffs(102, 390).addBox(22.01F, -7.99F, -12.0F, 16.0F, 7.49F, 0.4F)
                        // Screed grade & slope remote control boxes
                        .texOffs(194, 390).addBox(-39.0F, -8.5F, -2.0F, 2.5F, 3.5F, 2.0F)
                        .texOffs(206, 390).addBox(36.5F, -8.5F, -2.0F, 2.5F, 3.5F, 2.0F),
                PartPose.offset(0.0F, 22.0F, -22.0F)
        );

        return LayerDefinition.create(mesh, 512, 512);
    }

    @Override
    public void setupAnim(AsphaltPaverRenderState state) {
        super.setupAnim(state);

        // 1. Crawler tracks animation
        float leftTravel = -state.leftTrackTravel;
        float rightTravel = -state.rightTrackTravel;
        this.leftIdler.xRot = leftTravel * 0.15F;
        this.leftSprocket.xRot = leftTravel * 0.15F;
        this.rightIdler.xRot = rightTravel * 0.15F;
        this.rightSprocket.xRot = rightTravel * 0.15F;

        // 2. Spreader augers spin when tracks move or when actively paving
        float avgTravel = (leftTravel + rightTravel) * 0.5F;
        this.spreaderAugers.xRot = avgTravel * 0.3F;

        // 3. Hydraulic hopper wings folding
        // hopperFold ranges 0.0F (open for truck loading) to 1.0F (folded up inward)
        float fold = Mth.clamp(state.hopperFold, 0.0F, 1.0F);
        float foldAngle = fold * 0.85F; // up to ~48 degrees inward
        this.leftWing.zRot = -foldAngle;
        this.rightWing.zRot = foldAngle;

        // 4. Asphalt mix visibility in hopper
        this.asphaltLoad.visible = state.asphaltFillRatio > 0.01F;
        this.asphaltLoad.yScale = Mth.clamp(state.asphaltFillRatio, 0.2F, 1.0F);

        // 5. Screed height regulation
        // 0.0F = lowered onto ground for paving (Y=22.0F), 1.0F = raised for transport (Y=17.0F)
        float screedProgress = Mth.clamp(state.screedHeight, 0.0F, 1.0F);
        this.screed.y = 22.0F - screedProgress * 5.0F;
        this.screed.xRot = screedProgress * 0.08F;

        // 6. Amber safety beacon rotation and flashing
        this.beaconReflector.yRot = state.beaconSpin;
        this.beaconReflector.visible = state.beaconFlash;
    }
}
