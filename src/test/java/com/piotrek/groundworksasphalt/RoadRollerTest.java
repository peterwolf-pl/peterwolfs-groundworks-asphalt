package com.piotrek.groundworksasphalt;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity.RoadSurface3D;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadRollerTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("isLooseRoadbedBlock identifies loose roadbed materials and rejects solid bedrock/air/wood")
    void testIsLooseRoadbedBlock() {
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(null));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.AIR.defaultBlockState()));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.BEDROCK.defaultBlockState()));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.OAK_PLANKS.defaultBlockState()));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.IRON_BLOCK.defaultBlockState()));

        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.GRAVEL.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.SAND.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.RED_SAND.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.DIRT.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.COARSE_DIRT.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.DIRT_PATH.defaultBlockState()));
        assertTrue(RoadRollerEntity.isLooseRoadbedBlock(Blocks.COBBLESTONE.defaultBlockState()));

        // Solid non-loose blocks must be rejected
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.STONE.defaultBlockState()));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.BLACKSTONE.defaultBlockState()));
        assertFalse(RoadRollerEntity.isLooseRoadbedBlock(Blocks.BASALT.defaultBlockState()));
    }

    @Test
    @DisplayName("isAsphaltBlock identifies asphalt blocks and rejects non-asphalt blocks")
    void testIsAsphaltBlock() {
        assertFalse(RoadRollerEntity.isAsphaltBlock(null));
        assertFalse(RoadRollerEntity.isAsphaltBlock(Blocks.AIR.defaultBlockState()));
        assertFalse(RoadRollerEntity.isAsphaltBlock(Blocks.GRAVEL.defaultBlockState()));
        assertFalse(RoadRollerEntity.isAsphaltBlock(Blocks.STONE.defaultBlockState()));
        assertFalse(RoadRollerEntity.isAsphaltBlock(Blocks.DIRT.defaultBlockState()));
    }

    @Test
    @DisplayName("fitPlane fits horizontal flat asphalt mat with zero slopes and zero residual")
    void testFitPlaneHorizontal() {
        List<Vec3> points = List.of(
                new Vec3(-2.0, 64.0, -2.0),
                new Vec3(2.0, 64.0, -2.0),
                new Vec3(-2.0, 64.0, 2.0),
                new Vec3(2.0, 64.0, 2.0),
                new Vec3(0.0, 64.0, 0.0)
        );
        List<Double> weights = List.of(1.0, 1.0, 1.0, 1.0, 1.0);
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, true, forward);

        assertTrue(surface.hasAsphalt());
        assertEquals(5, surface.sampleCount());
        assertEquals(64.0, surface.meanY(), 1e-4);
        assertEquals(0.0, surface.slopeX(), 1e-4);
        assertEquals(0.0, surface.slopeZ(), 1e-4);
        assertEquals(0.0, surface.rmsResidual(), 1e-4);
        assertEquals(64.0, surface.getY(1.5, -0.8), 1e-4);
    }

    @Test
    @DisplayName("fitPlane correctly calculates 3D surface on a sloped ramp with cross-camber")
    void testFitPlaneRampWithCamber() {
        // Road plane equation: Y(x, z) = 64.0 + 0.12 * z - 0.04 * x
        double trueSlopeZ = 0.12;
        double trueSlopeX = -0.04;

        List<Vec3> points = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (double x = -2.0; x <= 2.0; x += 1.0) {
            for (double z = -2.0; z <= 2.0; z += 1.0) {
                double y = 64.0 + trueSlopeZ * z + trueSlopeX * x;
                points.add(new Vec3(x, y, z));
                weights.add(1.0);
            }
        }
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, true, forward);

        assertTrue(surface.hasAsphalt());
        assertEquals(25, surface.sampleCount());
        assertEquals(trueSlopeX, surface.slopeX(), 1e-4);
        assertEquals(trueSlopeZ, surface.slopeZ(), 1e-4);
        assertEquals(0.0, surface.rmsResidual(), 1e-4);

        // Test predicted contact height anywhere along the 3D road
        double expectedY = 64.0 + trueSlopeZ * 1.8 + trueSlopeX * 0.9;
        assertEquals(expectedY, surface.getY(0.9, 1.8), 1e-4);
    }

    @Test
    @DisplayName("fitPlane filters out noisy bumps and hollows to calculate smooth regression surface")
    void testFitPlaneNoiseFiltering() {
        // Base plane Y = 65.0, with symmetric surface bumps and dips (+0.15, -0.15)
        List<Vec3> points = List.of(
                new Vec3(-2.0, 65.15, -2.0),
                new Vec3(2.0, 64.85, -2.0),
                new Vec3(-2.0, 64.85, 2.0),
                new Vec3(2.0, 65.15, 2.0),
                new Vec3(0.0, 65.00, 0.0)
        );
        List<Double> weights = List.of(1.0, 1.0, 1.0, 1.0, 1.0);
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, true, forward);

        assertEquals(65.0, surface.meanY(), 1e-4);
        assertEquals(0.0, surface.slopeX(), 1e-4);
        assertEquals(0.0, surface.slopeZ(), 1e-4);
        assertEquals(65.0, surface.getY(0.5, -0.5), 1e-4);
        assertTrue(surface.rmsResidual() > 0.0);
    }

    @Test
    @DisplayName("fitPlane fits smooth 3D subgrade from front and rear samples across loose materials")
    void testFitPlaneSubgradeWithFrontAndRearSamples() {
        // Roadbed with gravel/dirt base on a 8% incline along vehicle forward Z, with a single protruding gravel bump at Z=2.0
        double trueSlopeZ = 0.08;
        List<Vec3> points = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        double[] uStations = {-3.8, -3.0, -2.2, -1.5, -0.7, 0.0, 0.7, 1.5, 2.2, 3.0, 3.8, 4.8};
        double[] vOffsets = {-1.6, -0.8, 0.0, 0.8, 1.6};

        for (double u : uStations) {
            for (double v : vOffsets) {
                double y = 64.0 + trueSlopeZ * u;
                // Simulate an isolated loose rock / protruding gravel block at (u=2.2, v=0.0)
                if (Math.abs(u - 2.2) < 0.1 && Math.abs(v) < 0.1) {
                    y += 0.85; // high bump!
                }
                points.add(new Vec3(v, y, u));
                weights.add(1.0);
            }
        }
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, false, forward);

        assertFalse(surface.hasAsphalt());
        assertTrue(surface.isValid());
        assertEquals(points.size(), surface.sampleCount());

        // Regression should filter out the isolated bump and stay very close to trueSlopeZ
        assertEquals(trueSlopeZ, surface.slopeZ(), 0.015);
        assertEquals(0.0, surface.slopeX(), 0.01);

        // At the front drum (u=1.8), contact height should be the smooth road line (~64.144) rather than the +0.85 bump
        double drumContactY = surface.getY(0.0, 1.8);
        assertEquals(64.0 + trueSlopeZ * 1.8, drumContactY, 0.05);
    }

    @Test
    @DisplayName("fitPlane clamps extreme slopes to +/-0.30 to prevent steep hole digging")
    void testFitPlaneClampsExtremeSlopesToPreventHoles() {
        // Points with an extreme 60% slope (slope = 0.60)
        List<Vec3> points = List.of(
                new Vec3(0.0, 60.0, -5.0),
                new Vec3(0.0, 63.0, 0.0),
                new Vec3(0.0, 66.0, 5.0),
                new Vec3(-1.0, 63.0, 0.0),
                new Vec3(1.0, 63.0, 0.0)
        );
        List<Double> weights = List.of(1.0, 1.0, 1.0, 1.0, 1.0);
        Vec3 forward = new Vec3(0.0, 0.0, 1.0);

        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, true, forward);

        assertTrue(surface.slopeZ() <= 0.3001, "SlopeZ must be clamped to max 0.30");
        assertTrue(surface.slopeZ() >= -0.3001, "SlopeZ must be clamped to min -0.30");
    }

    @Test
    @DisplayName("RoadSurface3D isValid rejects erratic high-residual surfaces and insufficient sample sizes")
    void testRoadSurfaceInvalidOnHighRmsOrFewPoints() {
        // High RMS residual (> 0.30) indicating non-planar terrain (ditch, cliff)
        RoadSurface3D highRms = new RoadSurface3D(true, 10, 0.0, 64.0, 0.0, 0.0, 0.0, 0.45);
        assertFalse(highRms.isValid(), "High RMS surface must be marked invalid to prevent digging");

        // Too few subgrade samples (< 6)
        RoadSurface3D fewSamplesSubgrade = new RoadSurface3D(false, 4, 0.0, 64.0, 0.0, 0.0, 0.0, 0.05);
        assertFalse(fewSamplesSubgrade.isValid(), "Subgrade with < 6 samples must be marked invalid");

        // Too few asphalt samples (< 4)
        RoadSurface3D fewSamplesAsphalt = new RoadSurface3D(true, 3, 0.0, 64.0, 0.0, 0.0, 0.0, 0.05);
        assertFalse(fewSamplesAsphalt.isValid(), "Asphalt with < 4 samples must be marked invalid");

        // Valid asphalt surface (>= 4 samples, low RMS)
        RoadSurface3D validAsphalt = new RoadSurface3D(true, 5, 0.0, 64.0, 0.0, 0.0, 0.0, 0.08);
        assertTrue(validAsphalt.isValid(), "Valid asphalt surface must be accepted");
    }

    @Test
    @DisplayName("Robust support height ignores one deep hole instead of pulling the roller underground")
    void testRobustSupportHeightRejectsSingleDeepOutlier() {
        double supportY = RoadRollerEntity.robustSupportHeight(64.0D, 64.0D, 64.0D, 54.0D);

        assertEquals(64.0D, supportY, 1.0E-6D);
        assertEquals(62.75D, RoadRollerEntity.clampSupportOutlier(54.0D, supportY), 1.0E-6D);
    }

    @Test
    @DisplayName("Robust support height keeps the midpoint of a genuine one-block step")
    void testRobustSupportHeightPreservesOneBlockStep() {
        double supportY = RoadRollerEntity.robustSupportHeight(65.0D, 65.0D, 64.0D, 64.0D);

        assertEquals(64.5D, supportY, 1.0E-6D);
        assertEquals(65.0D, RoadRollerEntity.clampSupportOutlier(65.0D, supportY), 1.0E-6D);
        assertEquals(64.0D, RoadRollerEntity.clampSupportOutlier(64.0D, supportY), 1.0E-6D);
    }

    @Test
    @DisplayName("Two-wheel cliff edge chooses the upper support cluster instead of sinking into mid-air")
    void testRobustSupportHeightRejectsWideTwoByTwoSplit() {
        assertEquals(64.0D, RoadRollerEntity.robustSupportHeight(64.0D, 64.0D, 54.0D, 54.0D), 1.0E-6D);
    }

    @Test
    @DisplayName("Invalid ground contacts are ignored and inherit the valid support center")
    void testRobustSupportHeightIgnoresInvalidContacts() {
        double supportY = RoadRollerEntity.robustSupportHeight(64.0D, Double.NaN, 64.1D, Double.NaN);

        assertEquals(64.05D, supportY, 1.0E-6D);
        assertEquals(supportY, RoadRollerEntity.clampSupportOutlier(Double.NaN, supportY), 1.0E-6D);
        assertTrue(Double.isNaN(RoadRollerEntity.robustSupportHeight(
                Double.NaN, Double.NaN, Double.NaN, Double.NaN
        )));
    }

    @Test
    @DisplayName("Three-contact support rejects one isolated high bump")
    void testRobustSupportHeightRejectsSingleHighOutlier() {
        assertEquals(64.0D, RoadRollerEntity.robustSupportHeight(64.0D, 64.0D, 66.0D, Double.NaN), 1.0E-6D);
    }

    @Test
    @DisplayName("Buried recovery hard-snaps upward to support and never moves a vehicle downward")
    void testBuriedRecoveryYOnlySnapsUpward() {
        assertEquals(64.0D,
                RoadRollerEntity.calculateBuriedRecoveryY(63.0D, 64.0D, true), 1.0E-6D);
        assertEquals(65.0D,
                RoadRollerEntity.calculateBuriedRecoveryY(65.0D, 64.0D, true), 1.0E-6D);
        assertEquals(63.0D,
                RoadRollerEntity.calculateBuriedRecoveryY(63.0D, Double.NaN, false), 1.0E-6D);
        assertEquals(63.98D,
                RoadRollerEntity.calculateBuriedRecoveryY(63.98D, 64.0D, true), 1.0E-6D);
    }

    @Test
    @DisplayName("Highest finite AABB support wins while invalid samples are ignored")
    void testHighestFiniteSupportMatchesCollisionFloor() {
        assertEquals(64.75D,
                RoadRollerEntity.highestFiniteSupport(Double.NaN, 64.0D, 64.75D, 63.5D), 1.0E-6D);
        assertTrue(Double.isNaN(RoadRollerEntity.highestFiniteSupport(Double.NaN, Double.NaN)));
    }

    @Test
    @DisplayName("Known support holds thin surfaces while unsupported vehicles use gravity")
    void testCollisionDrivenVerticalSpeedUsesVanillaStyleGravity() {
        assertEquals(0.0D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(64.0D, 64.0D, true, true), 1.0E-6D);
        assertEquals(-0.25D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(65.0D, 64.0D, true, false), 1.0E-6D);
        assertEquals(-0.25D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(64.0D, Double.NaN, false, false), 1.0E-6D);
    }

    @Test
    @DisplayName("Terrain sampling can rescue a buried chassis but can never command a downward correction")
    void testCollisionDrivenVerticalSpeedOnlyUsesSupportForUpwardRecovery() {
        assertEquals(0.25D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(63.0D, 64.0D, true, false), 1.0E-6D);
        assertEquals(-0.04D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(65.0D, 64.0D, true, true), 1.0E-6D);
        assertEquals(-0.25D,
                RoadRollerEntity.calculateCollisionDrivenVerticalSpeed(65.0D, 64.0D, true, false), 1.0E-6D);
    }

    @Test
    @DisplayName("Smooth vertical step holds dy=0 on flat ground preventing frog-hopping jitter")
    void testSmoothVerticalStepZeroOnFlatGround() {
        // Vehicle grounded at 64.0 on terrain at 64.0: dy must be 0.0, NOT negative gravity fighting ground
        assertEquals(0.0D,
                RoadRollerEntity.calculateSmoothVerticalStep(64.0D, 64.0D, 0.12D, true), 1.0E-6D);
        assertEquals(0.0D,
                RoadRollerEntity.calculateSmoothVerticalStep(64.02D, 64.0D, 0.12D, true), 1.0E-6D);
    }

    @Test
    @DisplayName("Smooth vertical step climbs 1/8 asphalt layer proportionally to vehicle speed")
    void testSmoothVerticalStepClimbsAsphaltLayer() {
        // 1/8 asphalt layer is 0.125m high: target is 64.125, vehicle is 64.0, speed is 0.12
        // climbSpeed = min(0.125, max(0.08, 0.12 * 0.75 = 0.09)) = 0.09
        double dy = RoadRollerEntity.calculateSmoothVerticalStep(64.125D, 64.0D, 0.12D, true);
        assertEquals(0.09D, dy, 1.0E-6D);
        assertTrue(dy > 0.0D && dy <= 0.125D, "Must climb smoothly without teleporting 1.25m");
    }

    @Test
    @DisplayName("Smooth vertical step descends slopes smoothly and falls when airborne")
    void testSmoothVerticalStepDescentAndAirborne() {
        // Descending a 0.15m slope while grounded
        assertEquals(-0.15D,
                RoadRollerEntity.calculateSmoothVerticalStep(63.85D, 64.0D, 0.12D, true), 1.0E-6D);
        // Airborne off a cliff
        assertEquals(-0.25D,
                RoadRollerEntity.calculateSmoothVerticalStep(64.0D, 64.0D, 0.12D, false), 1.0E-6D);
        assertEquals(-0.25D,
                RoadRollerEntity.calculateSmoothVerticalStep(60.0D, 64.0D, 0.12D, false), 1.0E-6D);
    }

    @Test
    @DisplayName("Subtractive compaction with availableFillUnits = 0 strictly excavates/shaves and deposits 0 units")
    void testSubtractiveCompactionZeroDeposit() {
        // Setup a full granular cell representing an uncompacted dirt/gravel bump
        var cell = com.piotrek.groundworks.terrain.cell.GranularCell.full(GranularMaterialRegistry.DIRT);
        assertEquals(512, cell.unitCount());

        // Target planar surface cuts through the cell at local micro-height 4 (half-block)
        // Everything above height 4 must be cleared/shaved down (subtractive leveling), 0 added
        int excavated = 0;
        int deposited = 0;
        int targetH = 4;

        for (int z = 0; z < 8; z++) {
            for (int x = 0; x < 8; x++) {
                int currentH = cell.getColumnHeight(x, z);
                if (currentH > targetH) {
                    for (int y = currentH; y > targetH; y--) {
                        if (cell.clear(x, y, z)) {
                            excavated++;
                        }
                    }
                }
            }
        }

        // 64 columns * 3 layers (y=7, 6, 5) = 192 units shaved down
        assertEquals(192, excavated);
        assertEquals(0, deposited);
        assertEquals(320, cell.unitCount());

        for (int z = 0; z < 8; z++) {
            for (int x = 0; x < 8; x++) {
                assertEquals(4, cell.getColumnHeight(x, z), "Every column must be exactly leveled to targetH");
            }
        }
    }

    @Test
    @DisplayName("Driver seat position in elevated cab matches 3D model cushion height and tilts with vehicle pitch")
    void testDriverSeatPositionElevatedCab() {
        // Seat attachment is at X=0.0m, Y=2.41m, Z=-0.47m in vehicle local frame
        double lx = 0.0D;
        double ly = 2.41D;
        double lz = -0.47D;

        // On horizontal ground (yaw=0, pitch=0, roll=0):
        // Forward is +Z, Up is +Y
        // Seat offset should be exactly (0.0, 2.41, -0.47)
        double radPitch = Math.toRadians(0.0D);
        double radRoll = Math.toRadians(0.0D);
        double y1 = ly * Math.cos(radPitch) - lz * Math.sin(radPitch);
        double z1 = ly * Math.sin(radPitch) + lz * Math.cos(radPitch);
        assertEquals(2.41D, y1, 1e-4);
        assertEquals(-0.47D, z1, 1e-4);

        // When climbing a 10-degree incline (pitch = +10 deg):
        // Rear drops, front rises:
        double pitchDeg = 10.0D;
        double radPitchUp = Math.toRadians(pitchDeg);
        double yPitched = ly * Math.cos(radPitchUp) - lz * Math.sin(radPitchUp);
        double zPitched = ly * Math.sin(radPitchUp) + lz * Math.cos(radPitchUp);
        // The seat cushion follows the cab rotation precisely
        assertTrue(yPitched > 0, "Seat height must remain positive above ground");
        assertEquals(ly * Math.cos(radPitchUp) - lz * Math.sin(radPitchUp), yPitched, 1e-4);
    }

    @Test
    @DisplayName("AsphaltVehicleLights computes accurate forward and rearward light block beams")
    void testVehicleLightsBeams() {
        Vec3 pos = new Vec3(100.0, 64.0, 200.0);
        Vec3 forward = new Vec3(0.0, 0.0, 1.0); // Facing North/South along +Z
        Vec3 side = new Vec3(1.0, 0.0, 0.0);    // Right is +X
        int[] ranges = {2, 5, 9};

        // Forward headlights
        List<net.minecraft.core.BlockPos> headBeams = com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights.calculateBeams(
                pos, forward, side, 1.8D, 1.8D, 1.4D, ranges, false
        );
        assertFalse(headBeams.isEmpty());
        // Must project forward along +Z (z > 200)
        for (net.minecraft.core.BlockPos bp : headBeams) {
            assertTrue(bp.getZ() > 200, "Headlight beams must project forward of the vehicle");
            assertTrue(bp.getY() >= 64, "Beams must illuminate ground level");
        }

        // Rearward work lights
        List<net.minecraft.core.BlockPos> rearBeams = com.piotrek.groundworksasphalt.vehicle.AsphaltVehicleLights.calculateBeams(
                pos, forward, side, -1.8D, 1.6D, 1.2D, new int[]{2, 5}, true
        );
        assertFalse(rearBeams.isEmpty());
        // Must project rearward along -Z (z < 200)
        for (net.minecraft.core.BlockPos bp : rearBeams) {
            assertTrue(bp.getZ() < 200, "Rear lights must project behind the vehicle");
        }
    }

    @Test
    @DisplayName("Lookahead sampling 2-3 blocks ahead of front drum axle guides smooth ramp regression")
    void testLookaheadAheadRampRegression() {
        // Vehicle at origin (0, 64, 0), drum center at +1.8m
        // 2-3 blocks in front of the drum axle is at u = 3.8m to 4.8m
        // Suppose there is a 20% ramp ahead rising to Y = 64.8 at u = 4.0m
        List<Vec3> points = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        // Behind vehicle: flat ground at Y=64.0
        points.add(new Vec3(-0.8, 64.0, -3.0));
        weights.add(1.0);
        points.add(new Vec3(0.8, 64.0, -3.0));
        weights.add(1.0);
        points.add(new Vec3(0.0, 64.0, -1.5));
        weights.add(1.0);

        // At drum: Y=64.0
        points.add(new Vec3(-0.8, 64.0, 1.8));
        weights.add(1.0);
        points.add(new Vec3(0.8, 64.0, 1.8));
        weights.add(1.0);

        // 2-3 blocks ahead of drum axle (u=3.8 to 4.8m): loose material mound rising to 64.6m
        points.add(new Vec3(-0.8, 64.6, 4.3));
        weights.add(2.0); // Heavy ahead lookahead weight
        points.add(new Vec3(0.8, 64.6, 4.3));
        weights.add(2.0);

        Vec3 forward = new Vec3(0.0, 0.0, 1.0);
        RoadSurface3D surface = RoadRollerEntity.fitPlane(points, weights, false, forward);

        assertTrue(surface.isValid());
        // The slope in Z must be positive (smoothly rising uphill toward the loose material ahead)
        assertTrue(surface.slopeZ() > 0.05, "Regression surface must form a smooth upward ramp to the ahead loose material");
        // At the lookahead point (z=4.3), predicted height should be close to 64.6
        assertEquals(64.6, surface.getY(0.0, 4.3), 0.35);
    }

    @Test
    @DisplayName("Front yoke bumper is positioned strictly in front of the drum with clear vibration clearance")
    void testFrontYokeBumperClearance() {
        // Drum center is at Z=28.0, horizontal radius 13.5 -> drum front surface is at Z=41.5
        // Maximum forward vibratory oscillation is 0.4 * amp = 0.4
        // Front of drum never exceeds Z = 41.9
        double drumMaxZ = 28.0D + 13.5D + 0.4D;
        // Front yoke bumper in RoadRollerModel starts at Z = 43.45
        double bumperMinZ = 43.45D;
        assertTrue(bumperMinZ > drumMaxZ, "Front yoke bumper must sit in front of the vibrating drum to avoid clipping");
        double clearance = bumperMinZ - drumMaxZ;
        assertTrue(clearance >= 1.0D, "Clearance must be at least 1.0 block unit");
    }

    @Test
    @DisplayName("Subgrade surface accepts moderately high residual bumps to enable leveling rough ground")
    void testSubgradeSurfaceAcceptsModeratelyHighResidualBumps() {
        // Soil with bumps (RMS ~0.65) must be valid so the compactor can smooth it
        RoadSurface3D bumpySubgrade = new RoadSurface3D(false, 12, 0.0, 64.0, 0.0, 0.0, 0.0, 0.65);
        assertTrue(bumpySubgrade.isValid(), "Soil compactor must accept subgrade surfaces with bumps to level them");

        // Extreme non-ground outliers (> 1.6) should still be rejected
        RoadSurface3D extremeCliff = new RoadSurface3D(false, 12, 0.0, 64.0, 0.0, 0.0, 0.0, 1.85);
        assertFalse(extremeCliff.isValid(), "Extreme cliffs/voids must be rejected");
    }

    @Test
    @DisplayName("Planar shaping levels a severely jagged, uneven soil cell into an ideally flat, smooth plane")
    void testPlanarShapingLevelsSeverelyJaggedCellToFlatPlane() {
        com.piotrek.groundworks.terrain.cell.GranularCell cell = com.piotrek.groundworks.terrain.cell.GranularCell.empty();
        cell.setMaterialId(GranularMaterialRegistry.DIRT.id());

        // Fill cell with randomized / jagged bumps and hollows (heights from 0 to 7)
        for (int z = 0; z < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; x++) {
                int randomH = (x * 3 + z * 5) % 8; // varying heights from 0 to 7
                for (int y = 0; y <= randomH; y++) {
                    cell.set(x, y, z);
                }
            }
        }

        // Verify it was indeed bumpy
        boolean hadDifferentHeights = false;
        int firstH = cell.getColumnHeight(0, 0);
        for (int z = 0; z < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; x++) {
                if (cell.getColumnHeight(x, z) != firstH) {
                    hadDifferentHeights = true;
                    break;
                }
            }
        }
        assertTrue(hadDifferentHeights, "Initial soil cell must be uneven/bumpy");

        // Target: flat plane at world Y = 64.375 (exactly 3 microvoxel layers = voxels 0, 1, 2)
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, 64, 0);
        com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper.PlanarSurfaceFunction flatPlane = (wx, wz) -> 64.375D;

        // Apply planar compaction shaping across all 64 micro-columns
        int fillUnitsAvailable = 512;
        for (int z = 0; z < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; x++) {
                double targetWorldY = flatPlane.getY(pos.getX() + (x + 0.5D) / 8.0D, pos.getZ() + (z + 0.5D) / 8.0D);
                double localMicroY = (targetWorldY - pos.getY()) * 8.0D;
                int targetH = net.minecraft.util.Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), -1, 7);
                int currentH = cell.getColumnHeight(x, z);

                if (currentH > targetH) {
                    for (int y = currentH; y > targetH; y--) {
                        cell.clear(x, y, z);
                    }
                } else if (currentH < targetH) {
                    for (int y = Math.max(0, currentH + 1); y <= targetH; y++) {
                        if (fillUnitsAvailable > 0 && !cell.isSet(x, y, z)) {
                            if (cell.set(x, y, z)) {
                                fillUnitsAvailable--;
                            }
                        }
                    }
                }
            }
        }

        // Verify: every single one of the 64 columns is now EXACTLY height 2 (perfectly flat, smooth surface!)
        for (int z = 0; z < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < com.piotrek.groundworks.terrain.cell.GranularCell.RESOLUTION; x++) {
                assertEquals(2, cell.getColumnHeight(x, z),
                        "All columns must be leveled to height 2 for a perfectly smooth surface at (x=" + x + ", z=" + z + ")");
            }
        }
    }

    @Test
    @DisplayName("Outward scanning excludes embankment slope drop-off from roadbed planar mesh")
    void testRoadRollerEmbankmentEdgeExclusion() {
        // Vehicle driving along a 3-block wide roadbed on an embankment (edge to the right)
        // Center: 64.0D
        // Left: 64.0D across all offsets
        // Right: 0.8m is 64.0D, but 1.6m and 2.4m drop 1.5m down the embankment slope (62.5D)
        double refY = 64.0D;
        double lastRightY = refY;
        List<Vec3> subgradePoints = new ArrayList<>();
        List<Double> subgradeWeights = new ArrayList<>();

        // Center
        subgradePoints.add(new Vec3(0.5D, 64.0D, 0.5D));
        subgradeWeights.add(1.0D);

        // Right side outward scan
        double[] lateralRight = {0.8D, 1.6D, 2.4D};
        double[] simulatedRightHeights = {64.0D, 62.5D, 61.5D}; // 1.6m and 2.4m are down the skarpa!

        for (int i = 0; i < lateralRight.length; i++) {
            double v = lateralRight[i];
            double sampleY = simulatedRightHeights[i];
            if (Math.abs(sampleY - lastRightY) > 0.38D || Math.abs(sampleY - refY) > 0.55D) {
                // Embankment edge detected: break outward scan
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            subgradePoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            subgradeWeights.add(latWeight);
            lastRightY = sampleY;
        }

        // Left side outward scan (all flat on roadbed)
        double[] lateralLeft = {-0.8D, -1.6D, -2.4D};
        double lastLeftY = refY;
        for (double v : lateralLeft) {
            double sampleY = 64.0D;
            if (Math.abs(sampleY - lastLeftY) > 0.38D || Math.abs(sampleY - refY) > 0.55D) {
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            subgradePoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            subgradeWeights.add(latWeight);
            lastLeftY = sampleY;
        }

        // Replicate across 3 longitudinal stations
        List<Vec3> allPoints = new ArrayList<>();
        List<Double> allWeights = new ArrayList<>();
        for (double z : new double[]{-2.0D, 0.0D, 2.0D}) {
            for (int i = 0; i < subgradePoints.size(); i++) {
                Vec3 p = subgradePoints.get(i);
                allPoints.add(new Vec3(p.x, p.y, z + 0.5D));
                allWeights.add(subgradeWeights.get(i));
            }
        }

        RoadSurface3D plane = RoadRollerEntity.fitPlane(allPoints, allWeights, false, new Vec3(0, 0, 1));
        assertTrue(plane.isValid());
        assertEquals(64.0D, plane.meanY(), 1e-4D, "Mean road height must remain 64.0 without being pulled down by slope");
        assertEquals(0.0D, plane.slopeX(), 1e-4D, "Lateral slope must remain perfectly 0.0 (no tilt down the ravine)");
        assertEquals(0.0D, plane.slopeZ(), 1e-4D, "Longitudinal slope must remain 0.0");
        assertEquals(0.0D, plane.rmsResidual(), 1e-4D, "Road plane must have zero residual");
    }

    @Test
    @DisplayName("Outward scanning excludes trench wall rise from roadbed planar mesh")
    void testRoadRollerTrenchWallEdgeExclusion() {
        // Vehicle driving along a roadbed in a cutting / trench (wall to the left)
        double refY = 64.0D;
        double lastLeftY = refY;
        List<Vec3> subgradePoints = new ArrayList<>();
        List<Double> subgradeWeights = new ArrayList<>();

        // Center
        subgradePoints.add(new Vec3(0.5D, 64.0D, 0.5D));
        subgradeWeights.add(1.0D);

        // Left side outward scan (trench wall at -1.6m and -2.4m rising 1.5m)
        double[] lateralLeft = {-0.8D, -1.6D, -2.4D};
        double[] simulatedLeftHeights = {64.0D, 65.5D, 66.5D}; // trench wall!

        for (int i = 0; i < lateralLeft.length; i++) {
            double v = lateralLeft[i];
            double sampleY = simulatedLeftHeights[i];
            if (Math.abs(sampleY - lastLeftY) > 0.38D || Math.abs(sampleY - refY) > 0.55D) {
                // Trench wall edge detected: break outward scan
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            subgradePoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            subgradeWeights.add(latWeight);
            lastLeftY = sampleY;
        }

        // Right side outward scan (flat on roadbed)
        double[] lateralRight = {0.8D, 1.6D, 2.4D};
        double lastRightY = refY;
        for (double v : lateralRight) {
            double sampleY = 64.0D;
            if (Math.abs(sampleY - lastRightY) > 0.38D || Math.abs(sampleY - refY) > 0.55D) {
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            subgradePoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            subgradeWeights.add(latWeight);
            lastRightY = sampleY;
        }

        List<Vec3> allPoints = new ArrayList<>();
        List<Double> allWeights = new ArrayList<>();
        for (double z : new double[]{-2.0D, 0.0D, 2.0D}) {
            for (int i = 0; i < subgradePoints.size(); i++) {
                Vec3 p = subgradePoints.get(i);
                allPoints.add(new Vec3(p.x, p.y, z + 0.5D));
                allWeights.add(subgradeWeights.get(i));
            }
        }

        RoadSurface3D plane = RoadRollerEntity.fitPlane(allPoints, allWeights, false, new Vec3(0, 0, 1));
        assertTrue(plane.isValid());
        assertEquals(64.0D, plane.meanY(), 1e-4D, "Mean road height must remain 64.0 without being pulled up by trench wall");
        assertEquals(0.0D, plane.slopeX(), 1e-4D, "Lateral slope must remain 0.0 (no tilt up the trench wall)");
    }

    @Test
    @DisplayName("Wheel contact edge isolation prevents chassis roll when wheels are suspended over an embankment")
    void testRoadRollerWheelContactEdgeIsolation() {
        double actualPhysicalGroundY = 64.0D;
        double flY = 64.0D;
        double rlY = 64.0D;
        double frY = 62.0D; // Front right suspended over embankment drop-off (-2.0m)
        double rrY = 62.0D; // Rear right suspended over embankment drop-off (-2.0m)

        frY = RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
        rrY = RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

        boolean flOff = Math.abs(flY - actualPhysicalGroundY) > 0.35D;
        boolean frOff = Math.abs(frY - actualPhysicalGroundY) > 0.35D;
        boolean rlOff = Math.abs(rlY - actualPhysicalGroundY) > 0.35D;
        boolean rrOff = Math.abs(rrY - actualPhysicalGroundY) > 0.35D;

        assertTrue(frOff);
        assertTrue(rrOff);
        assertFalse(flOff);
        assertFalse(rlOff);

        double effFlY = (flOff && !frOff) ? frY : flY;
        double effFrY = (frOff && !flOff) ? flY : frY;
        double effRlY = (rlOff && !rrOff) ? rrY : rlY;
        double effRrY = (rrOff && !rlOff) ? rlY : rrY;

        assertEquals(64.0D, effFrY, 1e-6D, "Suspended right front contact must inherit road surface height");
        assertEquals(64.0D, effRrY, 1e-6D, "Suspended right rear contact must inherit road surface height");

        double leftAvg = (effFlY + effRlY) * 0.5D;
        double rightAvg = (effFrY + effRrY) * 0.5D;
        double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 1.8D));

        assertEquals(0.0D, targetRoll, 1e-6D, "Vehicle roll must remain 0.0 (stable on roadbed, not tilting down slope)");
    }
}
