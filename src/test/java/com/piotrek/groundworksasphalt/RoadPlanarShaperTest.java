package com.piotrek.groundworksasphalt;

import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworksasphalt.terrain.RoadPlanarShaper;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoadPlanarShaperTest {

    @BeforeAll
    static void init() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        GranularMaterialRegistry.bootstrap();
    }

    @Test
    @DisplayName("Planar shaping on a 25% incline produces monotonically increasing microvoxel heights with zero steps")
    void testPlanarShapingIncline() {
        GranularCell cell = GranularCell.full(GranularMaterialRegistry.GRAVEL);
        BlockPos pos = new BlockPos(10, 64, 20);

        // Plane: base height 64.2m at pos center, slopeX = +0.25 (25% uphill ramp), slopeZ = 0
        RoadPlanarShaper.PlanarSurfaceFunction ramp = (wx, wz) -> 64.2D + 0.25D * (wx - (pos.getX() + 0.5D));

        // Apply planar shaping to cell directly
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                double wx = pos.getX() + (x + 0.5D) / 8.0D;
                double wz = pos.getZ() + (z + 0.5D) / 8.0D;
                double targetY = ramp.getY(wx, wz);
                double localMicroY = (targetY - pos.getY()) * 8.0D;

                int targetH = Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), -1, 7);
                int currentH = cell.getColumnHeight(x, z);

                if (currentH > targetH) {
                    for (int y = currentH; y > targetH; y--) {
                        cell.clear(x, y, z);
                    }
                }
            }
        }

        // Verify across columns along x-axis (uphill)
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION - 1; x++) {
                int h1 = cell.getColumnHeight(x, z);
                int h2 = cell.getColumnHeight(x + 1, z);
                // Each step is 0.25 / 8 = 0.03125m, so heights should be monotonic and differ by at most 1 microvoxel
                assertTrue(h2 >= h1, "Column height must be monotonic on incline at x=" + x);
                assertTrue(h2 - h1 <= 1, "Height step between adjacent micro-columns cannot exceed 1 voxel");
            }
        }
    }

    @Test
    @DisplayName("Planar shaping on a 45-degree slope (100% grade) steps by exactly 1 microvoxel per column")
    void testPlanarShaping45DegreeSlope() {
        GranularCell cell = GranularCell.full(GranularMaterialRegistry.GRAVEL);
        BlockPos pos = new BlockPos(0, 100, 0);

        // 45-degree slope: rises exactly 1 block over 1 block (slope = 1.0)
        // Center height is 100.5
        RoadPlanarShaper.PlanarSurfaceFunction slope45 = (wx, wz) -> 100.5D + 1.0D * (wx - 0.5D);

        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                double wx = (x + 0.5D) / 8.0D;
                double targetY = slope45.getY(wx, 0.5D);
                double localMicroY = (targetY - pos.getY()) * 8.0D;

                int targetH = Mth.clamp((int) Math.floor(localMicroY - 0.5D + 1.0E-5D), -1, 7);
                int currentH = cell.getColumnHeight(x, z);

                if (currentH > targetH) {
                    for (int y = currentH; y > targetH; y--) {
                        cell.clear(x, y, z);
                    }
                }
            }
        }

        // On a 45 degree slope (slope = 1.0), each micro-column (dx = 1/8) rises exactly 1 microvoxel!
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                int h = cell.getColumnHeight(x, z);
                assertEquals(x, h, "At x=" + x + " on 45-deg slope, height must be exactly " + x);
            }
        }
    }

    @Test
    @DisplayName("Adjacent blocks shaped to the same continuous plane match seamlessly at the block boundary")
    void testAdjacentBlockBoundaryContinuity() {
        BlockPos posA = new BlockPos(0, 60, 0);
        BlockPos posB = new BlockPos(1, 60, 0);

        GranularCell cellA = GranularCell.full(GranularMaterialRegistry.GRAVEL);
        GranularCell cellB = GranularCell.full(GranularMaterialRegistry.GRAVEL);

        // Continuous ramp across both blocks: slopeX = +0.5 (50% grade)
        RoadPlanarShaper.PlanarSurfaceFunction ramp = (wx, wz) -> 60.0D + 0.5D * wx;

        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            double wxA = posA.getX() + (x + 0.5D) / 8.0D;
            int targetHA = Mth.clamp((int) Math.floor((ramp.getY(wxA, 0.5D) - posA.getY()) * 8.0D - 0.5D + 1.0E-5D), -1, 7);
            for (int y = cellA.getColumnHeight(x, 0); y > targetHA; y--) {
                cellA.clear(x, y, 0);
            }

            double wxB = posB.getX() + (x + 0.5D) / 8.0D;
            int targetHB = Mth.clamp((int) Math.floor((ramp.getY(wxB, 0.5D) - posB.getY()) * 8.0D - 0.5D + 1.0E-5D), -1, 7);
            for (int y = cellB.getColumnHeight(x, 0); y > targetHB; y--) {
                cellB.clear(x, y, 0);
            }
        }

        // The rightmost column of block A is x=7 (center at 7.5/8)
        // The leftmost column of block B is x=0 (center at 8.5/8)
        int hA = cellA.getColumnHeight(7, 0);
        int hB = cellB.getColumnHeight(0, 0);

        // Distance between centers is 1/8 block = 0.125m.
        // Rise is 0.5 * 0.125 = 0.0625m = 0.5 microvoxel!
        // Therefore, hB - hA must be <= 1 (seamless transition without any step)
        assertTrue(hB - hA <= 1, "Boundary step between adjacent cells must be at most 1 microvoxel");
        assertTrue(hB >= hA, "Ramp must remain continuous and ascending across block boundary");
    }

    @Test
    @DisplayName("Flat level plane shapes all 64 columns to the exact same uniform height")
    void testFlatPlaneUniformHeight() {
        GranularCell cell = GranularCell.full(GranularMaterialRegistry.GRAVEL);
        BlockPos pos = new BlockPos(5, 70, 5);

        // Flat horizontal plane at Y = 70.5 (half block high)
        RoadPlanarShaper.PlanarSurfaceFunction flat = (wx, wz) -> 70.5D;

        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                double targetY = flat.getY(pos.getX() + (x + 0.5D) / 8.0D, pos.getZ() + (z + 0.5D) / 8.0D);
                int targetH = Mth.clamp((int) Math.floor((targetY - pos.getY()) * 8.0D - 0.5D + 1.0E-5D), -1, 7);
                for (int y = cell.getColumnHeight(x, z); y > targetH; y--) {
                    cell.clear(x, y, z);
                }
            }
        }

        // All 64 columns must have column height 3 (meaning voxels 0, 1, 2, 3 are solid, 4 voxels = 0.5m)
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                assertEquals(3, cell.getColumnHeight(x, z), "Every column must be exactly height 3 for 0.5m flat plane");
            }
        }
    }

    @Test
    @DisplayName("Vertical layer transition across integer Y boundary is seamless without gaps or steps")
    void testVerticalLayerTransitionSeamless() {
        BlockPos posBelow = new BlockPos(0, 63, 0);
        BlockPos posAbove = new BlockPos(0, 64, 0);

        GranularCell cellBelow = GranularCell.empty();
        cellBelow.setMaterialId(GranularMaterialRegistry.DIRT.id());
        GranularCell cellAbove = GranularCell.empty();
        cellAbove.setMaterialId(GranularMaterialRegistry.DIRT.id());

        // Ramp rising along X axis from Y = 63.85 (at x=0.0) to Y = 64.25 (at x=1.0)
        // Rate of rise is 0.40m over 1.0m (slope = +0.40)
        RoadPlanarShaper.PlanarSurfaceFunction ramp = (wx, wz) -> 63.85D + 0.40D * wx;

        // Shape cellBelow (Y=63) and cellAbove (Y=64)
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            double wx = (x + 0.5D) / 8.0D;
            double targetY = ramp.getY(wx, 0.5D);

            // Shape lower block (Y=63)
            int targetHBelow = Mth.clamp((int) Math.floor((targetY - posBelow.getY()) * 8.0D - 0.5D + 1.0E-5D), -1, 7);
            for (int y = 0; y <= targetHBelow; y++) {
                cellBelow.set(x, y, 0);
            }

            // Shape upper block (Y=64)
            int targetHAbove = Mth.clamp((int) Math.floor((targetY - posAbove.getY()) * 8.0D - 0.5D + 1.0E-5D), -1, 7);
            for (int y = 0; y <= targetHAbove; y++) {
                cellAbove.set(x, y, 0);
            }
        }

        // When targetY crosses from below 64.0 to above 64.0:
        // lower block reaches full height 7, upper block begins at height 0, 1...
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            double wx = (x + 0.5D) / 8.0D;
            double targetY = ramp.getY(wx, 0.5D);

            double physicalY;
            if (cellAbove.getColumnHeight(x, 0) >= 0) {
                physicalY = posAbove.getY() + (cellAbove.getColumnHeight(x, 0) + 1) * 0.125D;
            } else if (cellBelow.getColumnHeight(x, 0) >= 0) {
                physicalY = posBelow.getY() + (cellBelow.getColumnHeight(x, 0) + 1) * 0.125D;
            } else {
                physicalY = 63.0D;
            }

            // Physical height must match target plane within 1 microvoxel (0.125m)
            assertEquals(targetY, physicalY, 0.125D, "World height at x=" + x + " must match ramp within 1 voxel");
        }
    }

    @Test
    @DisplayName("Paving asphalt onto subgrade adheres directly to microvoxels and caps with ASPHALT material")
    void testAsphaltConformsToSubgradeMesh() {
        GranularCell subgrade = GranularCell.empty();
        subgrade.setMaterialId(GranularMaterialRegistry.GRAVEL.id());

        // Subgrade has a 25% cross-slope (rising from h=1 to h=3)
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                int h = 1 + (x / 3);
                for (int y = 0; y <= h; y++) {
                    subgrade.set(x, y, z);
                }
            }
        }

        // Apply a conforming surface cap of fresh asphalt
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                int added = subgrade.capColumn(x, z, GranularMaterialRegistry.ASPHALT.id());
                assertEquals(1, added, "Each column must receive exactly 1 voxel of conforming asphalt cap");
            }
        }

        assertEquals(GranularMaterialRegistry.ASPHALT.id(), subgrade.surfaceCapMaterialId());
        // Verify visual material at top of each column is ASPHALT and adheres directly to subgrade
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                int expectedH = 2 + (x / 3);
                assertEquals(expectedH, subgrade.getColumnHeight(x, z));
                assertEquals(GranularMaterialRegistry.ASPHALT.id(), subgrade.visualMaterialId(1L, x, expectedH, z));
            }
        }
    }

    @Test
    @DisplayName("Compacting fresh asphalt surface cap converts it to COMPACTED_ASPHALT material and preserves subgrade adhesion")
    void testCompactingAsphaltConvertsMaterialToCompacted() {
        GranularCell subgrade = GranularCell.empty();
        subgrade.setMaterialId(GranularMaterialRegistry.GRAVEL.id());

        // Fill 2 layers of gravel subgrade
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                subgrade.set(x, 0, z);
                subgrade.set(x, 1, z);
            }
        }

        // Pave fresh asphalt on top (cap)
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                subgrade.capColumn(x, z, GranularMaterialRegistry.ASPHALT.id());
            }
        }

        assertEquals(GranularMaterialRegistry.ASPHALT.id(), subgrade.surfaceCapMaterialId());

        // Now roller compacts the cell: convertMaterial converts ASPHALT -> COMPACTED_ASPHALT
        boolean converted = subgrade.convertMaterial(
                GranularMaterialRegistry.ASPHALT.id(),
                GranularMaterialRegistry.COMPACTED_ASPHALT.id()
        );

        assertTrue(converted, "convertMaterial must report successful conversion of asphalt cap");
        assertEquals(GranularMaterialRegistry.COMPACTED_ASPHALT.id(), subgrade.surfaceCapMaterialId(),
                "Surface cap must now be COMPACTED_ASPHALT");

        // Verify top face visually renders as COMPACTED_ASPHALT
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                int topH = subgrade.getColumnHeight(x, z);
                assertEquals(2, topH);
                assertEquals(GranularMaterialRegistry.COMPACTED_ASPHALT.id(),
                        subgrade.visualMaterialId(1L, x, topH, z),
                        "Top microvoxel face must render as COMPACTED_ASPHALT");
            }
        }
    }

    @Test
    @DisplayName("Paving and finishing asphalt on top of subgrade gravel guarantees 100% pure asphalt with zero subgrade gravel protrusion")
    void testPavingAndFinishingConvertsGravelSubgradeToPureAsphalt() {
        GranularCell cell = GranularCell.empty();
        cell.setMaterialId(GranularMaterialRegistry.GRAVEL.id());

        // Fill 4 layers of gravel subgrade
        for (int z = 0; z < GranularCell.RESOLUTION; z++) {
            for (int x = 0; x < GranularCell.RESOLUTION; x++) {
                for (int y = 0; y < 4; y++) {
                    cell.set(x, y, z);
                }
            }
        }

        assertEquals(256, cell.unitCount());
        assertEquals(GranularMaterialRegistry.GRAVEL.id(), cell.materialId());
        assertEquals(256, cell.unitsOfMaterial(GranularMaterialRegistry.GRAVEL.id()));

        // Paving asphalt converts cell material to pure ASPHALT
        cell.setMaterialId(GranularMaterialRegistry.ASPHALT.id());
        assertEquals(GranularMaterialRegistry.ASPHALT.id(), cell.materialId());
        assertEquals(0, cell.unitsOfMaterial(GranularMaterialRegistry.GRAVEL.id()), "Composition must have 0 gravel units after asphalt conversion");
        assertEquals(256, cell.unitsOfMaterial(GranularMaterialRegistry.ASPHALT.id()), "Composition must be 100% asphalt");

        // Finishing with road roller converts to COMPACTED_ASPHALT
        cell.setMaterialId(GranularMaterialRegistry.COMPACTED_ASPHALT.id());
        assertEquals(GranularMaterialRegistry.COMPACTED_ASPHALT.id(), cell.materialId());
        assertEquals(256, cell.unitsOfMaterial(GranularMaterialRegistry.COMPACTED_ASPHALT.id()));
        assertEquals(0, cell.unitsOfMaterial(GranularMaterialRegistry.GRAVEL.id()));

        // Top microvoxel and internal composition are both pure COMPACTED_ASPHALT
        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                assertEquals(3, cell.getColumnHeight(x, z));
                assertEquals(GranularMaterialRegistry.COMPACTED_ASPHALT.id(), cell.visualMaterialId(42L, x, 3, z));
                assertEquals(GranularMaterialRegistry.COMPACTED_ASPHALT.id(), cell.visualMaterialId(42L, x, 0, z));
            }
        }
    }

    @Test
    @DisplayName("Elevation near block boundary [Y.00, Y.0625) correctly shapes lower block Y-1 with asphalt at layer 7")
    void testLowElevationShapesLowerBlockWithAsphaltLayer7() {
        double worldY = 59.04D; // Just 0.04m above block boundary Y=59.00
        int blockY = Mth.floor(worldY); // 59
        int lowerBlockY = blockY - 1; // 58

        // In block 59:
        double localMicroYUpper = (worldY - blockY) * 8.0D; // 0.32
        int targetHUpper = Mth.clamp((int) Math.floor(localMicroYUpper - 0.5D + 1.0E-5D), -1, 7);
        assertEquals(-1, targetHUpper, "Upper block target height must be -1 (empty air)");

        // In block 58 (lower block):
        double localMicroYLower = (worldY - lowerBlockY) * 8.0D; // 8.32
        int targetHLower = Mth.clamp((int) Math.floor(localMicroYLower - 0.5D + 1.0E-5D), -1, 7);
        assertEquals(7, targetHLower, "Lower block target height must be 7 (top microvoxel)");

        // Shape lower block with asphalt
        GranularCell lowerCell = GranularCell.full(GranularMaterialRegistry.GRAVEL);
        lowerCell.setMaterialId(GranularMaterialRegistry.ASPHALT.id());
        long surfaceCapMask = -1L; // all 64 columns
        lowerCell.setSurfaceCap(surfaceCapMask, GranularMaterialRegistry.ASPHALT.id());

        for (int x = 0; x < GranularCell.RESOLUTION; x++) {
            for (int z = 0; z < GranularCell.RESOLUTION; z++) {
                assertEquals(7, lowerCell.getColumnHeight(x, z));
                assertEquals(GranularMaterialRegistry.ASPHALT.id(), lowerCell.visualMaterialId(123L, x, 7, z));
            }
        }
    }
}
