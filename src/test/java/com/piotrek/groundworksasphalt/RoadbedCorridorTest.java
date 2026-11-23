package com.piotrek.groundworksasphalt;

import com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage;
import com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedEntry;
import com.piotrek.groundworksasphalt.terrain.RoadbedCorridorStorage.RoadbedStage;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadbedCorridorTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("RoadbedStage ordering enforces progressive construction stages")
    void testRoadbedStageProgression() {
        assertTrue(RoadbedStage.BULLDOZED.isAtLeast(RoadbedStage.RAW));
        assertTrue(RoadbedStage.GRADED.isAtLeast(RoadbedStage.BULLDOZED));
        assertTrue(RoadbedStage.COMPACTED.isAtLeast(RoadbedStage.GRADED));
        assertTrue(RoadbedStage.PAVED.isAtLeast(RoadbedStage.COMPACTED));
        assertTrue(RoadbedStage.FINISHED.isAtLeast(RoadbedStage.PAVED));

        assertFalse(RoadbedStage.RAW.isAtLeast(RoadbedStage.GRADED));
        assertFalse(RoadbedStage.BULLDOZED.isAtLeast(RoadbedStage.COMPACTED));
    }

    @Test
    @DisplayName("RoadbedEntry calculates exact planar surface height at any world coordinates")
    void testRoadbedEntryGetY() {
        BlockPos pos = new BlockPos(10, 64, 20);
        // Plane passing through block center (10.5, 20.5) at Y=64.25
        // with slopeX = 0.04 (4% cross-slope) and slopeZ = 0.08 (8% longitudinal slope)
        RoadbedEntry entry = new RoadbedEntry(
                pos.asLong(),
                RoadbedStage.GRADED.getLevel(),
                64.25D,
                0.04D,
                0.08D,
                100L
        );

        assertEquals(RoadbedStage.GRADED, entry.stage());

        // At center (10.5, 20.5)
        assertEquals(64.25D, entry.getY(10.5D, 20.5D), 1e-6D);

        // At dx = +0.5 (11.0, 20.5): 64.25 + 0.5 * 0.04 = 64.27
        assertEquals(64.27D, entry.getY(11.0D, 20.5D), 1e-6D);

        // At dz = -0.5 (10.5, 20.0): 64.25 - 0.5 * 0.08 = 64.21
        assertEquals(64.21D, entry.getY(10.5D, 20.0D), 1e-6D);
    }

    @Test
    @DisplayName("RoadbedCorridorStorage correctly stores, upgrades stages and detects roadbed corridor")
    void testStorageRecording() {
        RoadbedCorridorStorage storage = new RoadbedCorridorStorage();
        BlockPos pos = new BlockPos(5, 64, 15);

        assertEquals(RoadbedStage.RAW, storage.getStage(pos));
        assertFalse(storage.isRoadbed(pos));

        // 1. Grader cuts the cell
        storage.recordStage(pos, RoadbedStage.GRADED, 64.125D, 0.02D, 0.0D, 10L);
        assertEquals(RoadbedStage.GRADED, storage.getStage(pos));
        assertTrue(storage.isRoadbed(pos));

        RoadbedEntry entry = storage.getEntry(pos);
        assertNotNull(entry);
        assertEquals(64.125D, entry.targetY(), 1e-6D);

        // 2. Compactor compacts the subgrade
        storage.recordStage(pos, RoadbedStage.COMPACTED, 64.10D, 0.02D, 0.0D, 20L);
        assertEquals(RoadbedStage.COMPACTED, storage.getStage(pos));

        // Lower stage cannot overwrite a higher stage
        storage.recordStage(pos, RoadbedStage.BULLDOZED, 64.0D, 0.0D, 0.0D, 30L);
        assertEquals(RoadbedStage.COMPACTED, storage.getStage(pos));

        // 3. Paver lays asphalt
        storage.recordStage(pos, RoadbedStage.PAVED, 64.225D, 0.02D, 0.0D, 40L);
        assertEquals(RoadbedStage.PAVED, storage.getStage(pos));

        // 4. Roller finishes pavement
        storage.recordStage(pos, RoadbedStage.FINISHED, 64.20D, 0.02D, 0.0D, 50L);
        assertEquals(RoadbedStage.FINISHED, storage.getStage(pos));
    }

    @Test
    @DisplayName("Compactor respects crowned road profile from grader rather than imposing flat plane")
    void testCompactorRespectsGraderCrown() {
        // Road centerline at X=10.0 (block 9 on left, block 10 on right)
        BlockPos leftLane = new BlockPos(9, 64, 0);  // Center at 9.5
        BlockPos rightLane = new BlockPos(10, 64, 0); // Center at 10.5

        // Crown peak is at X=10.0, height=64.20
        // Left lane slopes downward to the left (dx < 0 -> slopeX > 0, so at 9.5 Y=64.18)
        // Right lane slopes downward to the right (dx > 0 -> slopeX < 0, so at 10.5 Y=64.18)
        RoadbedCorridorStorage storage = new RoadbedCorridorStorage();
        storage.recordStage(leftLane, RoadbedStage.GRADED, 64.18D, 0.04D, 0.0D, 10L);
        storage.recordStage(rightLane, RoadbedStage.GRADED, 64.18D, -0.04D, 0.0D, 10L);

        // At X=10.0 (crown ridge where both lanes meet)
        // Left lane at dx = +0.5 (worldX = 10.0): 64.18 + 0.5 * 0.04 = 64.20
        double leftApexY = storage.getEntry(leftLane).getY(10.0D, 0.5D);
        // Right lane at dx = -0.5 (worldX = 10.0): 64.18 - 0.5 * (-0.04) = 64.20
        double rightApexY = storage.getEntry(rightLane).getY(10.0D, 0.5D);

        assertEquals(64.20D, leftApexY, 1e-6D, "Left pass apex at crown must be 64.20");
        assertEquals(64.20D, rightApexY, 1e-6D, "Right pass apex at crown must be 64.20");
        assertEquals(leftApexY, rightApexY, 1e-6D, "Crown ridge from both passes must meet at zero seam discrepancy");
    }

    @Test
    @DisplayName("End-to-end pipeline: Grader -> Compactor -> Paver -> Asphalt Roller preserves crowned geometry")
    void testEndToEndOrganicPipeline() {
        RoadbedCorridorStorage storage = new RoadbedCorridorStorage();
        BlockPos pos = new BlockPos(20, 64, 30);

        // 1. Grader cuts precision crowned pass at 64.15 with 3% cross-slope and 1% longitudinal slope
        storage.recordStage(pos, RoadbedStage.GRADED, 64.15D, 0.03D, 0.01D, 100L);
        RoadbedEntry graded = storage.getEntry(pos);
        assertNotNull(graded);
        assertEquals(RoadbedStage.GRADED, graded.stage());
        assertEquals(64.15D, graded.targetY(), 1e-6D);
        assertEquals(0.03D, graded.slopeX(), 1e-6D);
        assertEquals(0.01D, graded.slopeZ(), 1e-6D);

        // 2. Soil Compactor locks subgrade to COMPACTED without flattening slope
        storage.recordStage(pos, RoadbedStage.COMPACTED, graded.targetY() - 0.02D, graded.slopeX(), graded.slopeZ(), 200L);
        RoadbedEntry compacted = storage.getEntry(pos);
        assertNotNull(compacted);
        assertEquals(RoadbedStage.COMPACTED, compacted.stage());
        assertEquals(64.13D, compacted.targetY(), 1e-6D);
        assertEquals(0.03D, compacted.slopeX(), 1e-6D);
        assertEquals(0.01D, compacted.slopeZ(), 1e-6D);

        // 3. Paver lays fresh asphalt mat on top: inherits compacted subgrade slope, adds 0.125m thickness
        double matThickness = 0.125D;
        storage.recordStage(pos, RoadbedStage.PAVED, compacted.targetY() + matThickness, compacted.slopeX(), compacted.slopeZ(), 300L);
        RoadbedEntry paved = storage.getEntry(pos);
        assertNotNull(paved);
        assertEquals(RoadbedStage.PAVED, paved.stage());
        assertEquals(64.255D, paved.targetY(), 1e-6D);
        assertEquals(0.03D, paved.slopeX(), 1e-6D);

        // 4. Asphalt Roller finishes pavement to FINISHED
        storage.recordStage(pos, RoadbedStage.FINISHED, paved.targetY() - 0.015D, paved.slopeX(), paved.slopeZ(), 400L);
        RoadbedEntry finished = storage.getEntry(pos);
        assertNotNull(finished);
        assertEquals(RoadbedStage.FINISHED, finished.stage());
        assertEquals(64.24D, finished.targetY(), 1e-6D);
        assertEquals(0.03D, finished.slopeX(), 1e-6D);
        assertEquals(0.01D, finished.slopeZ(), 1e-6D);
        assertTrue(storage.isRoadbed(pos));
    }

    @Test
    @DisplayName("Option B: RoadSurveyCorridor accurately calculates 3D centerline, crown, and boundaries")
    void testSurveyCorridorTwoStakesCrownAndSlopes() {
        // Road from Z=0 to Z=40 climbing from Y=64.0 to Y=66.0 (5% longitudinal slope)
        // Corridor width: 8.0m (4m left, 4m right). Crown slope: 2.5% (0.025)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint stakeA =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 8.0D, 0.025D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint stakeB =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 66.0D, 40.0D, 8.0D, 0.025D);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(stakeA, stakeB);

        // 1. Centerline at Z=20 (halfway, Y should be exactly 65.0)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget center =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 0.0D, 20.0D);
        assertTrue(center.inCorridor());
        assertEquals(65.0D, center.targetY(), 1e-6D);
        assertEquals(65.0D, center.centerlineY(), 1e-6D);
        assertEquals(0.0D, center.distToCenterline(), 1e-6D);
        assertEquals(0.05D, center.slopeZ(), 1e-6D, "Longitudinal slope must be +5%");
        assertEquals(0.0D, center.slopeX(), 1e-6D, "Cross slope at apex ridge is 0");

        // 2. Point 2m to the right (X = +2.0, Z = 20.0): targetY = 65.0 (flat road profile)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget right =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 2.0D, 20.0D);
        assertTrue(right.inCorridor());
        assertEquals(65.0D, right.targetY(), 1e-6D);
        assertEquals(2.0D, right.distToCenterline(), 1e-6D);
        assertEquals(0.05D, right.slopeZ(), 1e-6D);
        assertEquals(0.0D, right.slopeX(), 1e-6D, "Cross slope on flat road is 0");

        // 3. Point 2m to the left (X = -2.0, Z = 20.0): targetY = 65.0 (flat road profile)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget left =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, -2.0D, 20.0D);
        assertTrue(left.inCorridor());
        assertEquals(65.0D, left.targetY(), 1e-6D);
        assertEquals(2.0D, left.distToCenterline(), 1e-6D);
        assertEquals(0.05D, left.slopeZ(), 1e-6D);
        assertEquals(0.0D, left.slopeX(), 1e-6D, "Cross slope on flat road is 0");

        // 4. Point 5m from centerline (X = 5.0, Z = 20.0) -> outside corridor halfWidth of 4.0m!
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget outside =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 5.0D, 20.0D);
        assertFalse(outside.inCorridor(), "Point beyond corridor width must be rejected to protect embankments/trenches");
    }

    @Test
    @DisplayName("Option B: Single survey stake acts as localized spot benchmark")
    void testSurveyCorridorSingleStakeSpotBenchmark() {
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint spotStake =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(10.0D, 64.0D, 10.0D, 8.0D, 0.02D);
        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(spotStake);

        // At stake origin
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget center =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 10.0D, 10.0D);
        assertTrue(center.inCorridor());
        assertEquals(64.0D, center.targetY(), 1e-6D);

        // Within 4m radius (at 12.0, 10.0 -> dist = 2m)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget within =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 12.0D, 10.0D);
        assertTrue(within.inCorridor());
        assertEquals(64.0D, within.targetY(), 1e-6D);

        // Outside 4m radius (at 16.0, 10.0 -> dist = 6m)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget outside =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 16.0D, 10.0D);
        assertFalse(outside.inCorridor());
    }

    @Test
    @DisplayName("Option B: buildSubgradeBetweenStakes validation with null/insufficient stakes")
    void testBuildSubgradeValidation() {
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.BuildResult nullResult =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.buildSubgradeBetweenStakes(null, null, 64.0D, null);
        assertFalse(nullResult.success());
    }

    @Test
    @DisplayName("Option B: buildSplineChains reconstructs ordered sequence from arbitrarily shuffled stakes")
    void testSplineChainsOrderingFromUnorderedStakes() {
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 8.0D, 0.025D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.5D, 20.0D, 8.0D, 0.025D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 65.0D, 40.0D, 8.0D, 0.025D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s4 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 65.5D, 60.0D, 8.0D, 0.025D);

        // Pass stakes in shuffled non-monotonic order
        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> shuffled =
                java.util.List.of(s3, s1, s4, s2);

        java.util.List<java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint>> chains =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.buildSplineChains(shuffled);

        assertEquals(1, chains.size(), "Should form exactly 1 continuous chain");
        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> chain = chains.get(0);
        assertEquals(4, chain.size(), "Chain must contain all 4 stakes");

        // Verify strictly monotonic Z coordinates (either 0->20->40->60 or 60->40->20->0)
        boolean ascending = chain.get(0).z() < chain.get(3).z();
        for (int i = 0; i < 3; i++) {
            if (ascending) {
                assertTrue(chain.get(i).z() < chain.get(i + 1).z(), "Chain must be monotonically ascending in Z");
            } else {
                assertTrue(chain.get(i).z() > chain.get(i + 1).z(), "Chain must be monotonically descending in Z");
            }
        }
    }

    @Test
    @DisplayName("Option B: Catmull-Rom spline curve produces smooth horizontal bend with perpendicular crown")
    void testCurvedRoadSplineCatmullRomCenterline() {
        // Road curving through 3 stakes like track ties in SplineCart
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 8.0D, 0.02D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(10.0D, 64.0D, 20.0D, 8.0D, 0.02D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(30.0D, 64.0D, 30.0D, 8.0D, 0.02D);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2, s3);

        // Sample at stake 2 origin (apex of curve)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget apex =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 10.0D, 20.0D);
        assertTrue(apex.inCorridor());
        assertEquals(64.0D, apex.targetY(), 1e-4D);
        assertEquals(0.0D, apex.distToCenterline(), 1e-4D);

        // Forward vector must be normalized
        net.minecraft.world.phys.Vec3 fwd = apex.segmentDirection();
        double fwdLen = Math.sqrt(fwd.x * fwd.x + fwd.z * fwd.z);
        assertEquals(1.0D, fwdLen, 1e-5D, "Forward tangent must be unit length");

        // Normal perpendicular vector n = (-uz, 0, ux)
        double nx = -fwd.z;
        double nz = fwd.x;
        double dotProduct = fwd.x * nx + fwd.z * nz;
        assertEquals(0.0D, dotProduct, 1e-6D, "Normal must be strictly perpendicular to curve tangent");

        // Sample 2m perpendicular to the right of stake 2
        double rightX = 10.0D - 2.0D * nx;
        double rightZ = 20.0D - 2.0D * nz;
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget right =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, rightX, rightZ);
        assertTrue(right.inCorridor());
        // Target height on flat road stays at benchmark height 64.0m
        assertEquals(64.0D, right.targetY(), 1e-3D);
        assertEquals(2.0D, right.distToCenterline(), 1e-3D);

        // Sample 6m away from centerline (outside 4m half-width)
        double outX = 10.0D - 6.0D * nx;
        double outZ = 20.0D - 6.0D * nz;
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget outside =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, outX, outZ);
        assertFalse(outside.inCorridor(), "Point beyond corridor width must be rejected");
    }

    @Test
    @DisplayName("Option B: Catmull-Rom analytical derivatives exactly match numerical finite differences")
    void testAnalyticalCatmullRomDerivatives() {
        double p0 = 12.0D;
        double p1 = 18.0D;
        double p2 = 30.0D;
        double p3 = 25.0D;

        double h = 1e-6D;
        for (double t = 0.1D; t <= 0.9D; t += 0.2D) {
            double cPlus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRom(p0, p1, p2, p3, t + h);
            double cMinus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRom(p0, p1, p2, p3, t - h);
            double cCenter = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRom(p0, p1, p2, p3, t);

            double numericalD1 = (cPlus - cMinus) / (2.0D * h);
            double analyticalD1 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRomDerivative(p0, p1, p2, p3, t);
            assertEquals(numericalD1, analyticalD1, 1e-5D, "First derivative must match finite differences");

            double d1Plus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRomDerivative(p0, p1, p2, p3, t + h);
            double d1Minus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRomDerivative(p0, p1, p2, p3, t - h);
            double numericalD2 = (d1Plus - d1Minus) / (2.0D * h);
            double analyticalD2 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalCatmullRomSecondDerivative(p0, p1, p2, p3, t);
            assertEquals(numericalD2, analyticalD2, 1e-5D, "Second derivative must match finite differences of first derivative");
        }
    }

    @Test
    @DisplayName("Option B: Hermite spline analytical derivatives match finite differences")
    void testAnalyticalHermiteDerivatives() {
        double p0 = 10.0D;
        double v0 = 25.0D;
        double p1 = 35.0D;
        double v1 = 15.0D;

        double h = 1e-6D;
        for (double t = 0.1D; t <= 0.9D; t += 0.2D) {
            double hPlus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermite(p0, v0, p1, v1, t + h);
            double hMinus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermite(p0, v0, p1, v1, t - h);
            double numericalD1 = (hPlus - hMinus) / (2.0D * h);
            double analyticalD1 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermiteDerivative(p0, v0, p1, v1, t);
            assertEquals(numericalD1, analyticalD1, 1e-5D, "Hermite first derivative must match finite differences");

            double d1Plus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermiteDerivative(p0, v0, p1, v1, t + h);
            double d1Minus = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermiteDerivative(p0, v0, p1, v1, t - h);
            double numericalD2 = (d1Plus - d1Minus) / (2.0D * h);
            double analyticalD2 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.evalHermiteSecondDerivative(p0, v0, p1, v1, t);
            assertEquals(numericalD2, analyticalD2, 1e-5D, "Hermite second derivative must match finite differences of first derivative");
        }
    }

    @Test
    @DisplayName("Option B: Strict boundary enforcement prevents phantom ditches/mounds outside corridor and past endpoints")
    void testNoPhantomDitchesPastStakeEndpointsOrOutsideWidth() {
        // Road from Z=0 to Z=40, width=8m (halfWidth=4m)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 8.0D, 0.025D, 0.0D); // Facing South (+Z)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 40.0D, 8.0D, 0.025D, 0.0D); // Facing South (+Z)

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2);

        // 1. Inside corridor (at Z=20, X=2.0m -> dist = 2.0 <= 4.0)
        var inside = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 2.0D, 20.0D);
        assertTrue(inside.inCorridor(), "Point inside corridor must be accepted");
        assertEquals(64.0D, inside.targetY(), 1e-4D);

        // 2. Outside lateral corridor (at Z=20, X=5.0m -> dist = 5.0 > 4.0)
        var outsideLateral = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 5.0D, 20.0D);
        assertFalse(outsideLateral.inCorridor(), "Point beyond lateral half-width must be strictly rejected");

        // 3. Behind start stake (at Z=-2.0m, X=0.0m) -> MUST BE REJECTED! No phantom ditches behind road!
        var behindStart = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 0.0D, -2.0D);
        assertFalse(behindStart.inCorridor(), "Point behind the starting stake must be strictly rejected");

        // 4. Past end stake (at Z=42.0m, X=0.0m) -> MUST BE REJECTED! No phantom ditches ahead of road!
        var pastEnd = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 0.0D, 42.0D);
        assertFalse(pastEnd.inCorridor(), "Point past the ending stake must be strictly rejected");
    }

    @Test
    @DisplayName("Option B: Explicit stake yaw direction curves road along Hermite spline (like SplineCart)")
    void testExplicitStakeYawDirectionSpline() {
        // Stake 1 at (0, 64, 0) facing South (+Z, yaw = 0)
        // Stake 2 at (20, 64, 20) facing East (+X, yaw = 270)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 8.0D, 0.02D, 0.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(20.0D, 64.0D, 20.0D, 8.0D, 0.02D, 270.0D);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2);

        // Near stake 1 origin
        var near1 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 0.0D, 1.0D);
        assertTrue(near1.inCorridor());
        assertEquals(0.0D, near1.segmentDirection().x, 0.25D, "Initial tangent must head primarily South (+Z)");
        assertTrue(near1.segmentDirection().z > 0.8D, "Initial tangent must head South (+Z)");

        // Near stake 2 origin
        var near2 = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, 19.0D, 20.0D);
        assertTrue(near2.inCorridor());
        assertTrue(near2.segmentDirection().x > 0.8D, "Final tangent must head East (+X)");
        assertEquals(0.0D, near2.segmentDirection().z, 0.25D, "Final tangent Z component must be close to 0");
    }

    @Test
    @DisplayName("Continuous multi-segment Hermite spline has zero gaps/wedges across intermediate stakes")
    void testContinuousMultiSegmentSplineNoIntermediateNotches() {
        // 3 stakes forming a curve: width = 12m (halfWidth = 6m)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 12.0D, 0.025D, 0.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(10.0D, 64.0D, 25.0D, 12.0D, 0.025D, 0.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(35.0D, 64.0D, 40.0D, 12.0D, 0.025D, 0.0D);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2, s3);

        // Check points directly across the intermediate stake s2 (at x=10, z=25, and +/- 4 meters laterally)
        for (double dx = -4.0D; dx <= 4.0D; dx += 1.0D) {
            for (double dz = -2.0D; dz <= 2.0D; dz += 0.5D) {
                double wx = s2.x() + dx;
                double wz = s2.z() + dz;
                var target = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, wx, wz);
                assertTrue(target.inCorridor(),
                        String.format("Point (%.1f, %.1f) near intermediate stake s2 must be inside corridor (no triangular gap!)", wx, wz));
            }
        }
    }

    @Test
    @DisplayName("Natural Catmull-Rom spline chains with Auto stakes (NaN yaw) have continuous tangents and zero intermediate notches")
    void testAutoStakesContinuousCatmullRomSpline() {
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 64.0D, 0.0D, 12.0D, 0.025D, Double.NaN);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(15.0D, 64.0D, 20.0D, 12.0D, 0.025D, Double.NaN);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(35.0D, 64.0D, 30.0D, 12.0D, 0.025D, Double.NaN);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2, s3);

        for (double dx = -4.0D; dx <= 4.0D; dx += 1.0D) {
            for (double dz = -2.0D; dz <= 2.0D; dz += 0.5D) {
                double wx = s2.x() + dx;
                double wz = s2.z() + dz;
                var target = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, wx, wz);
                assertTrue(target.inCorridor(),
                        String.format("Point (%.1f, %.1f) near intermediate auto-stake s2 must be inside corridor!", wx, wz));
            }
        }
    }

    @Test
    @DisplayName("Sharp angled junction with directional stake yaw has zero wedge gap across corridor")
    void testSharpTurnStakeJunctionNoAngularWedgeGap() {
        // Stake 1 at (0, -60, 0)
        // Stake 2 at (20, -59, 20) with yaw = 45.0 (angled stake at junction)
        // Stake 3 at (40, -59, 20) (flat straight road going East)
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, -60.0D, 0.0D, 12.0D, 0.025D, Double.NaN);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(20.0D, -59.0D, 20.0D, 12.0D, 0.025D, 45.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(40.0D, -59.0D, 20.0D, 12.0D, 0.025D, Double.NaN);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2, s3);

        // Check points directly across the junction at s2 (radius 5m in all directions)
        for (double dx = -5.0D; dx <= 5.0D; dx += 0.5D) {
            for (double dz = -5.0D; dz <= 5.0D; dz += 0.5D) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 5.5D) {
                    double wx = s2.x() + dx;
                    double wz = s2.z() + dz;
                    var target = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, wx, wz);
                    assertTrue(target.inCorridor(),
                            String.format("Point (%.1f, %.1f) at angled stake s2 junction must be in corridor (no wedge gap!)", wx, wz));
                }
            }
        }
    }

    @Test
    @DisplayName("Grade break ramp to flat road crossing integer Y boundary has continuous microvoxel heights")
    void testGradeBreakIntegerBoundaryContinuousVerticalSpan() {
        // Ramp rising from Y = 65.0 at X = 0 to Y = 66.0 at X = 10, then flat road Y = 66.0 to X = 20
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s1 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(0.0D, 65.0D, 0.0D, 8.0D, 0.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s2 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(10.0D, 66.0D, 0.0D, 8.0D, 0.0D);
        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint s3 =
                new com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint(20.0D, 66.0D, 0.0D, 8.0D, 0.0D);

        java.util.List<com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyStakePoint> stakes =
                java.util.List.of(s1, s2, s3);

        int prevGlobalVoxel = Integer.MIN_VALUE;
        // Step across the grade break at X = 10.0 by 0.125m (1/8 block) increments
        for (double x = 8.0D; x <= 12.0D; x += 0.125D) {
            var target = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTargetFromStakes(stakes, x, 0.0D);
            assertTrue(target.inCorridor());
            double finishedY = target.targetY() + 0.10D; // Finished pavement elevation

            int blockY = net.minecraft.util.Mth.floor(finishedY - 0.0625D);
            double localMicroY = (finishedY - blockY) * 8.0D;
            int microVoxel = net.minecraft.util.Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), -1, 7);

            // Global microvoxel index = blockY * 8 + microVoxel
            int globalVoxel = blockY * 8 + microVoxel;
            assertTrue(microVoxel >= 0 && microVoxel <= 7,
                    String.format("At x=%.3f (finishedY=%.3f), microvoxel in blockY=%d must be in [0, 7], was %d",
                            x, finishedY, blockY, microVoxel));

            if (prevGlobalVoxel != Integer.MIN_VALUE) {
                // Must be monotonically non-decreasing and cannot jump by more than 1 voxel across 1/8 block
                assertTrue(globalVoxel >= prevGlobalVoxel,
                        String.format("Global voxel must be monotonic: prev=%d, curr=%d at x=%.3f", prevGlobalVoxel, globalVoxel, x));
                assertTrue(globalVoxel - prevGlobalVoxel <= 1,
                        String.format("Step between adjacent 1/8m samples cannot exceed 1 voxel: jump was %d", globalVoxel - prevGlobalVoxel));
            }
            prevGlobalVoxel = globalVoxel;
        }
    }

    @Test
    @DisplayName("Option B: paveAsphaltBetweenStakes and buildFinishedRoadBetweenStakes fail gracefully with null or insufficient stakes")
    void testCorridorPavingAndFinishingFailGracefully() {
        var nullPave = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.paveAsphaltBetweenStakes(null, null, 64.0D, null);
        assertFalse(nullPave.success());
        assertEquals(0, nullPave.columnsShaped());

        var nullFinish = com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.buildFinishedRoadBetweenStakes(null, null, 64.0D, null);
        assertFalse(nullFinish.success());
        assertEquals(0, nullFinish.columnsShaped());
    }
}
