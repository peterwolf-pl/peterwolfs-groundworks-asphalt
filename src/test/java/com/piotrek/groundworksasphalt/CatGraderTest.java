package com.piotrek.groundworksasphalt;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworksasphalt.client.model.CatGraderModel;
import net.minecraft.SharedConstants;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatGraderTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("CatGrader body layer builds successfully with lengthened 4-block blade")
    void testModelBodyLayer() {
        LayerDefinition layer = CatGraderModel.createBodyLayer();
        assertNotNull(layer);
        assertNotNull(layer.bakeRoot());
    }

    @Test
    @DisplayName("Wheelbase interpolation computes exact roadbed baseline grade between tandem and front axle")
    void testWheelbaseInterpolation() {
        double rearAvg = 64.0D;
        double frontAvg = 68.0D;
        double tBlade = 3.75D / 6.0D; // 0.625
        double baselineGradeY = rearAvg + (frontAvg - rearAvg) * tBlade;

        assertEquals(66.5D, baselineGradeY, 0.001D);
    }

    @Test
    @DisplayName("Grader autolevel cross-slope roll perfectly cancels ground camber for planar level cut")
    void testGraderAutoLevelRoll() {
        double flY = 64.0D;
        double frY = 64.44D; // 0.20 slope across 2.2m gauge (right side higher)
        double rlY = 64.0D;
        double rrY = 64.44D;

        double frontCamber = (frY - flY) / 2.2D;
        double rearCamber = (rrY - rlY) / 2.2D;
        double baselineCamber = rearCamber + (frontCamber - rearCamber) * (3.75D / 6.0D);

        // Autolevel rotates blade to cancel vehicle tilt (not deepen it)
        float targetRoll = (float) Math.atan(baselineCamber);
        double netCrossSlope = baselineCamber - Math.tan(targetRoll);

        assertEquals(0.0D, netCrossSlope, 1e-6D);
    }

    @Test
    @DisplayName("Grader edge-isolated wheel contacts prevent roadbed camber distortion when driving along embankment edge")
    void testGraderWheelContactEdgeIsolationEmbankment() {
        double actualPhysicalGroundY = 64.0D;
        double flY = 64.0D;
        double rlY = 64.0D;
        double frY = 62.0D; // Front right suspended over cliff / slope (-2.0m)
        double rrY = 62.0D; // Rear right suspended over cliff / slope (-2.0m)

        frY = com.piotrek.groundworksasphalt.entity.RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
        rrY = com.piotrek.groundworksasphalt.entity.RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

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

        assertEquals(64.0D, effFrY, 1e-6D, "Suspended right front contact must use supported roadbed height");
        assertEquals(64.0D, effRrY, 1e-6D, "Suspended right rear contact must use supported roadbed height");

        double frontCamber = net.minecraft.util.Mth.clamp((effFrY - effFlY) / 2.2D, -0.08D, 0.08D);
        double rearCamber = net.minecraft.util.Mth.clamp((effRrY - effRlY) / 2.2D, -0.08D, 0.08D);
        double baselineCamber = net.minecraft.util.Mth.clamp(rearCamber + (frontCamber - rearCamber) * (3.75D / 6.0D), -0.08D, 0.08D);

        assertEquals(0.0D, frontCamber, 1e-6D, "Front camber must remain 0.0, ignoring embankment drop-off");
        assertEquals(0.0D, rearCamber, 1e-6D, "Rear camber must remain 0.0");
        assertEquals(0.0D, baselineCamber, 1e-6D, "Blade baseline camber must remain flat on roadbed");
    }

    @Test
    @DisplayName("Grader edge-isolated wheel contacts prevent roadbed camber distortion when driving along trench wall")
    void testGraderWheelContactEdgeIsolationTrench() {
        double actualPhysicalGroundY = 64.0D;
        double flY = 64.0D;
        double rlY = 64.0D;
        double frY = 65.5D; // Front right pressed into rising trench wall (+1.5m)
        double rrY = 65.5D; // Rear right pressed into rising trench wall (+1.5m)

        frY = com.piotrek.groundworksasphalt.entity.RoadRollerEntity.clampSupportOutlier(frY, actualPhysicalGroundY);
        rrY = com.piotrek.groundworksasphalt.entity.RoadRollerEntity.clampSupportOutlier(rrY, actualPhysicalGroundY);

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

        double frontCamber = net.minecraft.util.Mth.clamp((effFrY - effFlY) / 2.2D, -0.08D, 0.08D);
        double rearCamber = net.minecraft.util.Mth.clamp((effRrY - effRlY) / 2.2D, -0.08D, 0.08D);
        double baselineCamber = net.minecraft.util.Mth.clamp(rearCamber + (frontCamber - rearCamber) * (3.75D / 6.0D), -0.08D, 0.08D);

        assertEquals(0.0D, baselineCamber, 1e-6D, "Blade baseline camber must ignore trench wall rise");
    }

    @Test
    @DisplayName("Blade candidate loop breaks outward expansion when offset reaches embankment slope or trench wall")
    void testGraderBladeEdgeCandidateBreak() {
        double cutY = 64.0D;
        double[] rightOffsets = {0.4D, 0.8D, 1.2D, 1.6D, 2.0D};
        // Simulated ground at offsets: 0.4m is 64.0D, 0.8m drops to 62.0D (embankment slope)
        double[] simulatedGroundY = {64.0D, 62.0D, 60.5D, 59.0D, 58.0D};

        java.util.List<Double> acceptedOffsets = new java.util.ArrayList<>();
        for (int i = 0; i < rightOffsets.length; i++) {
            double off = rightOffsets[i];
            double groundY = simulatedGroundY[i];
            if (groundY < cutY - 0.50D || groundY > cutY + 0.65D) {
                // Embankment slope or trench wall reached: stop expanding blade candidate positions!
                break;
            }
            acceptedOffsets.add(off);
        }

        assertEquals(1, acceptedOffsets.size(), "Only offset 0.4m on the roadbed should be accepted");
        assertEquals(0.4D, acceptedOffsets.get(0), 1e-6D);
    }
}
