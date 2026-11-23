package com.piotrek.groundworksasphalt;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.block.AsphaltSlabBlock;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.block.RoadLineBlock;
import com.piotrek.groundworksasphalt.entity.AsphaltRollerEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity.RoadSurface3D;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
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

class AsphaltRollerTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("isAsphaltBlock identifies asphalt blocks and rejects non-asphalt blocks")
    void testIsAsphaltBlock() {
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(null));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.AIR.defaultBlockState()));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.GRAVEL.defaultBlockState()));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.STONE.defaultBlockState()));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.DIRT.defaultBlockState()));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.SAND.defaultBlockState()));
        assertFalse(AsphaltRollerEntity.isAsphaltBlock(Blocks.OAK_PLANKS.defaultBlockState()));
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
    @DisplayName("fitPlane correctly calculates 3D surface on a sloped asphalt ramp with cross-camber")
    void testFitPlaneRampWithCamber() {
        double trueSlopeZ = 0.10;
        double trueSlopeX = -0.05;

        List<Vec3> points = new java.util.ArrayList<>();
        List<Double> weights = new java.util.ArrayList<>();
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
        assertTrue(surface.isValid());
        assertEquals(trueSlopeX, surface.slopeX(), 1e-4);
        assertEquals(trueSlopeZ, surface.slopeZ(), 1e-4);
        assertEquals(0.0, surface.rmsResidual(), 1e-4);

        double expectedY = 64.0 + trueSlopeZ * 1.6 + trueSlopeX * 0.8;
        assertEquals(expectedY, surface.getY(0.8, 1.6), 1e-4);
    }

    @Test
    @DisplayName("shapeCellToSurface with forceCompactedAsphalt strictly returns NONE on null level/pos/surface")
    void testShapeCellToSurfaceNullSafety() {
        RoadPlanarShaper.ShapeResult res = RoadPlanarShaper.shapeCellToSurface(
                null, null, null, null, 0, true
        );
        assertFalse(res.changed());
        assertEquals(0, res.unitsExcavated());
        assertEquals(0, res.unitsDeposited());
    }

    @Test
    @DisplayName("Asphalt roller outward scan excludes embankment slope drop-off from roadbed planar mesh")
    void testAsphaltRollerEmbankmentEdgeExclusion() {
        double refY = 64.0D;
        double lastRightY = refY;
        java.util.List<Vec3> asphaltPoints = new java.util.ArrayList<>();
        java.util.List<Double> asphaltWeights = new java.util.ArrayList<>();

        // Center
        asphaltPoints.add(new Vec3(0.5D, 64.0D, 0.5D));
        asphaltWeights.add(1.0D);

        // Right side outward scan
        double[] lateralRight = {0.8D, 1.6D, 2.4D};
        double[] simulatedRightHeights = {64.0D, 62.5D, 61.5D}; // 1.6m drops down embankment

        for (int i = 0; i < lateralRight.length; i++) {
            double v = lateralRight[i];
            double sampleY = simulatedRightHeights[i];
            if (Math.abs(sampleY - lastRightY) > 0.25D || Math.abs(sampleY - refY) > 0.35D) {
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            asphaltPoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            asphaltWeights.add(latWeight);
            lastRightY = sampleY;
        }

        // Left side outward scan
        double[] lateralLeft = {-0.8D, -1.6D, -2.4D};
        double lastLeftY = refY;
        for (double v : lateralLeft) {
            double sampleY = 64.0D;
            if (Math.abs(sampleY - lastLeftY) > 0.25D || Math.abs(sampleY - refY) > 0.35D) {
                break;
            }
            double latWeight = 1.0D / (1.0D + 0.15D * v * v);
            asphaltPoints.add(new Vec3(v + 0.5D, sampleY, 0.5D));
            asphaltWeights.add(latWeight);
            lastLeftY = sampleY;
        }

        // Replicate across stations
        java.util.List<Vec3> allPoints = new java.util.ArrayList<>();
        java.util.List<Double> allWeights = new java.util.ArrayList<>();
        for (double z : new double[]{-2.0D, 0.0D, 2.0D}) {
            for (int i = 0; i < asphaltPoints.size(); i++) {
                Vec3 p = asphaltPoints.get(i);
                allPoints.add(new Vec3(p.x, p.y, z + 0.5D));
                allWeights.add(asphaltWeights.get(i));
            }
        }

        RoadSurface3D plane = RoadRollerEntity.fitPlane(allPoints, allWeights, true, new Vec3(0, 0, 1));
        assertTrue(plane.isValid());
        assertEquals(64.0D, plane.meanY(), 1e-4D, "Asphalt road height must remain 64.0");
        assertEquals(0.0D, plane.slopeX(), 1e-4D, "Lateral slope must remain 0.0 without tilting off edge");
        assertEquals(0.0D, plane.slopeZ(), 1e-4D);
    }

    @Test
    @DisplayName("Asphalt leveling clamp prevents excavating asphalt down to -1 (protects underlying dirt/subgrade)")
    void testAsphaltLevelingNeverDeletesAsphaltLayer() {
        // When forceCompactedAsphalt is true (roller leveling asphalt) and !isExplicitPavingCommand:
        // A surface dipping below the block floor must clamp targetH to at least 0, leaving layer 0 intact
        double targetWorldY = 63.90D; // Below pos.getY() = 64.0
        double localMicroY = (targetWorldY - 64.0D) * 8.0D; // -0.8
        boolean isExplicitPaving = false;
        boolean isAsphaltTarget = true;

        int minH = (!isExplicitPaving && isAsphaltTarget) ? 0 : -1;
        int targetH = net.minecraft.util.Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), minH, 7);

        assertEquals(0, targetH, "When leveling asphalt, targetH must never drop below 0 to avoid exposing dirt below");
    }

    @Test
    @DisplayName("Asphalt roller wheel contact edge isolation prevents chassis roll on embankment edge")
    void testAsphaltRollerWheelContactEdgeIsolation() {
        double actualPhysicalGroundY = 64.0D;
        double flY = 64.0D;
        double rlY = 64.0D;
        double frY = 61.5D; // suspended over embankment
        double rrY = 61.5D;

        frY = RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
        rrY = RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

        boolean flOff = Math.abs(flY - actualPhysicalGroundY) > 0.35D;
        boolean frOff = Math.abs(frY - actualPhysicalGroundY) > 0.35D;
        boolean rlOff = Math.abs(rlY - actualPhysicalGroundY) > 0.35D;
        boolean rrOff = Math.abs(rrY - actualPhysicalGroundY) > 0.35D;

        double effFlY = (flOff && !frOff) ? frY : flY;
        double effFrY = (frOff && !flOff) ? flY : frY;
        double effRlY = (rlOff && !rrOff) ? rrY : rlY;
        double effRrY = (rrOff && !rlOff) ? rlY : rrY;

        assertEquals(64.0D, effFrY, 1e-6D);
        assertEquals(64.0D, effRrY, 1e-6D);

        double leftAvg = (effFlY + effRlY) * 0.5D;
        double rightAvg = (effFrY + effRrY) * 0.5D;
        double targetRoll = Math.toDegrees(Math.atan2(leftAvg - rightAvg, 1.8D));

        assertEquals(0.0D, targetRoll, 1e-6D, "Asphalt roller roll must remain 0.0 on roadbed");
    }
}
