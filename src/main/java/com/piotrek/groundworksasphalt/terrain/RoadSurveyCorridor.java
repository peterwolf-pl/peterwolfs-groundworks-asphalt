package com.piotrek.groundworksasphalt.terrain;

import com.piotrek.groundworks.GroundworksMod;
import com.piotrek.groundworks.api.GroundworksApi;
import com.piotrek.groundworks.api.material.GranularMaterial;
import com.piotrek.groundworks.api.material.GranularMaterialRegistry;
import com.piotrek.groundworks.block.entity.GranularBlockEntity;
import com.piotrek.groundworks.networking.GranularSyncHandler;
import com.piotrek.groundworks.terrain.cell.DirtyFlags;
import com.piotrek.groundworks.terrain.cell.GranularCell;
import com.piotrek.groundworks.terrain.conversion.BlockConverter;
import com.piotrek.groundworks.terrain.storage.GranularWorldStorage;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.entity.RoadStakeEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Survey Corridor Engineering Alignment (Option B).
 * Computes exact continuous 3D mathematical road geometry based on survey stakes and flags.
 */
public final class RoadSurveyCorridor {

    public static final double DEFAULT_SEARCH_RADIUS = 256.0D;
    public static final double MAX_SEGMENT_LENGTH = 80.0D;

    private RoadSurveyCorridor() {}

    public record SurveyStakePoint(
            double x,
            double y,
            double z,
            double corridorWidth,
            double crownSlope,
            double yawDeg
    ) {
        public SurveyStakePoint(double x, double y, double z, double corridorWidth, double crownSlope) {
            this(x, y, z, corridorWidth, crownSlope, Double.NaN);
        }

        public boolean hasExplicitYaw() {
            return !Double.isNaN(yawDeg);
        }

        public Vec3 forwardDir() {
            if (!hasExplicitYaw()) {
                return Vec3.ZERO;
            }
            double rad = Math.toRadians(yawDeg);
            return new Vec3(-Math.sin(rad), 0.0D, Math.cos(rad));
        }
    }

    public record SurveyTarget(
            boolean inCorridor,
            double targetY,
            double slopeX,
            double slopeZ,
            double centerlineY,
            double distToCenterline,
            Vec3 segmentDirection,
            double corridorHalfWidth
    ) {
        public static final SurveyTarget NONE = new SurveyTarget(
                false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, Vec3.ZERO, 0.0D
        );
    }

    /**
     * Finds all survey stakes (both native RoadStakeEntity and any bulldozer SurveyFlagEntity)
     * within radius of the given origin, transitively traversing connected road alignment chains.
     */
    public static List<SurveyStakePoint> findStakesNearby(Level level, Vec3 origin, double radius) {
        List<SurveyStakePoint> points = new ArrayList<>();
        if (level == null || origin == null) {
            return points;
        }

        double minY = (level != null) ? level.getMinY() : -64.0D;
        double maxY = (level != null) ? level.getMaxY() : 320.0D;

        Set<java.util.UUID> visitedStakeIds = new HashSet<>();
        Queue<Vec3> searchQueue = new ArrayDeque<>();
        searchQueue.add(origin);

        double stepRadius = Math.max(radius, MAX_SEGMENT_LENGTH + 16.0D);
        int maxStakes = 150;

        while (!searchQueue.isEmpty() && points.size() < maxStakes) {
            Vec3 center = searchQueue.poll();
            AABB searchBox = new AABB(
                    center.x - stepRadius, minY, center.z - stepRadius,
                    center.x + stepRadius, maxY, center.z + stepRadius
            );

            // 1. Native road survey stakes
            List<RoadStakeEntity> nativeStakes = level.getEntitiesOfClass(RoadStakeEntity.class, searchBox);
            for (RoadStakeEntity s : nativeStakes) {
                if (visitedStakeIds.add(s.getUUID())) {
                    points.add(new SurveyStakePoint(
                            s.getX(), s.getBenchmarkY(), s.getZ(),
                            s.getCorridorWidth(),
                            s.getCrownPercent() / 100.0D,
                            s.hasCustomYaw() ? (double) s.getYaw() : Double.NaN
                    ));
                    searchQueue.add(s.position());
                }
            }

            // 2. Compatibility with Bulldozer SurveyFlagEntity (if present in level)
            List<Entity> otherFlags = level.getEntities((Entity) null, searchBox, e -> {
                if (e instanceof RoadStakeEntity) {
                    return false;
                }
                String desc = e.getType().getDescriptionId();
                return desc.contains("survey_flag") || e.getClass().getSimpleName().contains("SurveyFlag");
            });
            for (Entity f : otherFlags) {
                if (visitedStakeIds.add(f.getUUID())) {
                    points.add(new SurveyStakePoint(
                            f.getX(), f.getY(), f.getZ(),
                            RoadStakeEntity.DEFAULT_CORRIDOR_WIDTH,
                            RoadStakeEntity.DEFAULT_CROWN_PERCENT / 100.0D,
                            Double.NaN
                    ));
                    searchQueue.add(f.position());
                }
            }

            // Subsequent hops along connected stakes only need MAX_SEGMENT_LENGTH reach
            stepRadius = MAX_SEGMENT_LENGTH + 8.0D;
        }

        return points;
    }

    /**
     * Samples the survey corridor target at world coordinates (worldX, worldZ).
     */
    public static SurveyTarget sampleTarget(Level level, double worldX, double worldZ) {
        if (level == null) {
            return SurveyTarget.NONE;
        }
        List<SurveyStakePoint> stakes = findStakesNearby(level, new Vec3(worldX, 64.0D, worldZ), DEFAULT_SEARCH_RADIUS);
        return sampleTargetFromStakes(stakes, worldX, worldZ);
    }

    /**
     * Pure geometric calculation given a list of survey stake points and world coordinates.
     * Evaluates continuous 3D Catmull-Rom spline curves across the stake alignment,
     * maintaining C1 tangent continuity and smooth vertical curves (like SplineCart tracks).
     */
    public static SurveyTarget sampleTargetFromStakes(List<SurveyStakePoint> stakes, double worldX, double worldZ) {
        if (stakes == null || stakes.isEmpty()) {
            return SurveyTarget.NONE;
        }

        // Single stake case: spot benchmark elevation within corridor radius
        if (stakes.size() == 1) {
            SurveyStakePoint p = stakes.get(0);
            double dx = worldX - p.x;
            double dz = worldZ - p.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            double halfW = p.corridorWidth / 2.0D;
            if (dist <= halfW) {
                double targetY = p.y;
                double slopeX = 0.0D;
                double slopeZ = 0.0D;
                return new SurveyTarget(true, targetY, slopeX, slopeZ, p.y, dist, Vec3.ZERO, halfW);
            }
            return SurveyTarget.NONE;
        }

        List<List<SurveyStakePoint>> chains = buildSplineChains(stakes);
        SurveyTarget bestTarget = null;
        double minDistanceToCorridor = Double.MAX_VALUE;

        for (List<SurveyStakePoint> chain : chains) {
            int m = chain.size();
            if (m < 2) continue;

            for (int k = 0; k < m - 1; k++) {
                SurveyStakePoint p1 = chain.get(k);
                SurveyStakePoint p2 = chain.get(k + 1);

                double chordDx = p2.x - p1.x;
                double chordDy = p2.y - p1.y;
                double chordDz = p2.z - p1.z;
                double chordLen = Math.sqrt(chordDx * chordDx + chordDz * chordDz);
                if (chordLen < 0.1D) {
                    continue;
                }

                // Fallback control points for Catmull-Rom tangent estimation
                SurveyStakePoint p0 = (k > 0) ? chain.get(k - 1) : new SurveyStakePoint(
                        2.0D * p1.x - p2.x, 2.0D * p1.y - p2.y, 2.0D * p1.z - p2.z, p1.corridorWidth, p1.crownSlope
                );
                SurveyStakePoint p3 = (k + 2 < m) ? chain.get(k + 2) : new SurveyStakePoint(
                        2.0D * p2.x - p1.x, 2.0D * p2.y - p1.y, 2.0D * p2.z - p1.z, p2.corridorWidth, p2.crownSlope
                );

                // Determine directional tangent vectors at p1 and p2 (Hermite / Catmull-Rom)
                Vec3 naturalDir1 = new Vec3(p2.x - p0.x, 0.0D, p2.z - p0.z);
                Vec3 naturalDir2 = new Vec3(p3.x - p1.x, 0.0D, p3.z - p1.z);

                Vec3 dir1 = (k == 0 && p1.hasExplicitYaw()) ? p1.forwardDir() : naturalDir1;
                Vec3 dir2 = (k == m - 2 && p2.hasExplicitYaw()) ? p2.forwardDir() : naturalDir2;

                double d1Len = Math.sqrt(dir1.x * dir1.x + dir1.z * dir1.z);
                dir1 = (d1Len > 1e-4D) ? dir1.scale(1.0D / d1Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                double d2Len = Math.sqrt(dir2.x * dir2.x + dir2.z * dir2.z);
                dir2 = (d2Len > 1e-4D) ? dir2.scale(1.0D / d2Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                // Align tangents with chord forward direction along the chain
                if (dir1.x * chordDx + dir1.z * chordDz < 0.0D) {
                    dir1 = dir1.scale(-1.0D);
                }
                if (dir2.x * chordDx + dir2.z * chordDz < 0.0D) {
                    dir2 = dir2.scale(-1.0D);
                }

                double v0x = dir1.x * chordLen;
                double v0z = dir1.z * chordLen;
                double v1x = dir2.x * chordLen;
                double v1z = dir2.z * chordLen;

                double v0y = chordDy;
                double v1y = chordDy;

                double halfWidth = (p1.corridorWidth + p2.corridorWidth) / 4.0D;

                // Coarse sampling to find closest parameter t in [0, 1]
                int sampleSteps = 16;
                double bestT = 0.0D;
                double bestDistSq = Double.MAX_VALUE;

                for (int step = 0; step <= sampleSteps; step++) {
                    double t = step / (double) sampleSteps;
                    double cx = evalHermite(p1.x, v0x, p2.x, v1x, t);
                    double cz = evalHermite(p1.z, v0z, p2.z, v1z, t);
                    double d2 = (cx - worldX) * (cx - worldX) + (cz - worldZ) * (cz - worldZ);
                    if (d2 < bestDistSq) {
                        bestDistSq = d2;
                        bestT = t;
                    }
                }

                // Refine parameter t using Newton-Raphson with gradient fallback
                double tOpt = bestT;
                for (int iter = 0; iter < 6; iter++) {
                    double cx = evalHermite(p1.x, v0x, p2.x, v1x, tOpt);
                    double cz = evalHermite(p1.z, v0z, p2.z, v1z, tOpt);
                    double tx = evalHermiteDerivative(p1.x, v0x, p2.x, v1x, tOpt);
                    double tz = evalHermiteDerivative(p1.z, v0z, p2.z, v1z, tOpt);
                    double ax = evalHermiteSecondDerivative(p1.x, v0x, p2.x, v1x, tOpt);
                    double az = evalHermiteSecondDerivative(p1.z, v0z, p2.z, v1z, tOpt);

                    double dx = cx - worldX;
                    double dz = cz - worldZ;

                    double dPrime = 2.0D * (dx * tx + dz * tz);
                    double dSecond = 2.0D * (tx * tx + tz * tz + dx * ax + dz * az);

                    if (Math.abs(dPrime) < 1e-12D) {
                        break;
                    }
                    if (dSecond > 1e-9D) {
                        double delta = dPrime / dSecond;
                        tOpt = Math.max(0.0D, Math.min(1.0D, tOpt - delta));
                    } else {
                        tOpt = Math.max(0.0D, Math.min(1.0D, tOpt - 0.01D * Math.signum(dPrime)));
                    }
                }

                double cx = evalHermite(p1.x, v0x, p2.x, v1x, tOpt);
                double cy = evalHermite(p1.y, v0y, p2.y, v1y, tOpt);
                double cz = evalHermite(p1.z, v0z, p2.z, v1z, tOpt);

                double tx = evalHermiteDerivative(p1.x, v0x, p2.x, v1x, tOpt);
                double ty = evalHermiteDerivative(p1.y, v0y, p2.y, v1y, tOpt);
                double tz = evalHermiteDerivative(p1.z, v0z, p2.z, v1z, tOpt);

                double tHoriz = Math.sqrt(tx * tx + tz * tz);
                if (tHoriz < 1e-5D) {
                    tx = chordDx;
                    tz = chordDz;
                    tHoriz = Math.max(1e-5D, chordLen);
                }

                double ux = tx / tHoriz;
                double uz = tz / tHoriz;
                double nx = -uz;
                double nz = ux;

                double dx = worldX - cx;
                double dz = worldZ - cz;
                double lateralDist = dx * nx + dz * nz;
                double absLateral = Math.abs(lateralDist);
                double longOffset = dx * ux + dz * uz;

                // 1. Perpendicular lateral corridor boundary check (true uniform-width road strip, no corner clipping)
                if (absLateral > halfWidth + 1e-4D) {
                    continue;
                }

                // 2. Strict longitudinal boundary checks only at road terminal endpoints
                if (k == 0 && tOpt <= 1e-4D && longOffset < 0.0D) {
                    continue;
                }
                if (k == m - 2 && tOpt >= 1.0D - 1e-4D && longOffset > 0.0D) {
                    continue;
                }

                // 3. For intermediate joint, avoid excessive overshoot past the joint
                if ((k > 0 && tOpt <= 1e-4D && longOffset < -halfWidth)
                        || (k < m - 2 && tOpt >= 1.0D - 1e-4D && longOffset > halfWidth)) {
                    continue;
                }

                double distToCenterline = Math.sqrt(dx * dx + dz * dz);
                // 4. Select the segment with minimum Euclidean distance to the curve centerline
                if (distToCenterline < minDistanceToCorridor) {
                    minDistanceToCorridor = distToCenterline;

                    double longSlope = ty / tHoriz;
                    double targetY = cy; // Completely flat cross-section (no crown slope across road)
                    double slopeX = ux * longSlope; // Pure longitudinal slope for ramps/inclines
                    double slopeZ = uz * longSlope; // Pure longitudinal slope for ramps/inclines

                    bestTarget = new SurveyTarget(
                            true, targetY, slopeX, slopeZ, cy, absLateral,
                            new Vec3(ux, 0.0D, uz), halfWidth
                    );
                }
            }
        }

        return (bestTarget != null) ? bestTarget : SurveyTarget.NONE;
    }

    public static double evalHermite(double p0, double v0, double p1, double v1, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2.0D * t3 - 3.0D * t2 + 1.0D;
        double h10 = t3 - 2.0D * t2 + t;
        double h01 = -2.0D * t3 + 3.0D * t2;
        double h11 = t3 - t2;
        return h00 * p0 + h10 * v0 + h01 * p1 + h11 * v1;
    }

    public static double evalHermiteDerivative(double p0, double v0, double p1, double v1, double t) {
        double t2 = t * t;
        double dh00 = 6.0D * t2 - 6.0D * t;
        double dh10 = 3.0D * t2 - 4.0D * t + 1.0D;
        double dh01 = -6.0D * t2 + 6.0D * t;
        double dh11 = 3.0D * t2 - 2.0D * t;
        return dh00 * p0 + dh10 * v0 + dh01 * p1 + dh11 * v1;
    }

    public static double evalHermiteSecondDerivative(double p0, double v0, double p1, double v1, double t) {
        double d2h00 = 12.0D * t - 6.0D;
        double d2h10 = 6.0D * t - 4.0D;
        double d2h01 = -12.0D * t + 6.0D;
        double d2h11 = 6.0D * t - 2.0D;
        return d2h00 * p0 + d2h10 * v0 + d2h01 * p1 + d2h11 * v1;
    }

    public static double evalCatmullRom(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5D * (
                (2.0D * p1) +
                (-p0 + p2) * t +
                (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) * t2 +
                (-p0 + 3.0D * p1 - 3.0D * p2 + p3) * t3
        );
    }

    public static double evalCatmullRomDerivative(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        return 0.5D * (
                (-p0 + p2) +
                2.0D * (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) * t +
                3.0D * (-p0 + 3.0D * p1 - 3.0D * p2 + p3) * t2
        );
    }

    public static double evalCatmullRomSecondDerivative(double p0, double p1, double p2, double p3, double t) {
        return 0.5D * (
                2.0D * (2.0D * p0 - 5.0D * p1 + 4.0D * p2 - p3) +
                6.0D * (-p0 + 3.0D * p1 - 3.0D * p2 + p3) * t
        );
    }

    /**
     * Reconstructs continuous ordered road alignment chains from an arbitrary collection of survey stakes.
     * Uses Minimum Spanning Forest and diameter topological traversal to guarantee monotonic sequential ordering.
     */
    public static List<List<SurveyStakePoint>> buildSplineChains(List<SurveyStakePoint> stakes) {
        List<List<SurveyStakePoint>> chains = new ArrayList<>();
        if (stakes == null || stakes.isEmpty()) {
            return chains;
        }
        if (stakes.size() == 1) {
            chains.add(List.of(stakes.get(0)));
            return chains;
        }

        // Deduplicate stakes placed practically on top of each other (< 0.25m)
        List<SurveyStakePoint> unique = new ArrayList<>();
        for (SurveyStakePoint s : stakes) {
            boolean duplicate = false;
            for (SurveyStakePoint u : unique) {
                double dx = s.x - u.x;
                double dy = s.y - u.y;
                double dz = s.z - u.z;
                if (dx * dx + dy * dy + dz * dz < 0.0625D) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                unique.add(s);
            }
        }

        int n = unique.size();
        if (n < 2) {
            if (n == 1) {
                chains.add(List.of(unique.get(0)));
            }
            return chains;
        }

        // Build candidate edges within MAX_SEGMENT_LENGTH
        record Edge(int u, int v, double weight) implements Comparable<Edge> {
            @Override
            public int compareTo(Edge o) {
                return Double.compare(this.weight, o.weight);
            }
        }

        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            SurveyStakePoint pA = unique.get(i);
            for (int j = i + 1; j < n; j++) {
                SurveyStakePoint pB = unique.get(j);
                double dx = pB.x - pA.x;
                double dz = pB.z - pA.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= MAX_SEGMENT_LENGTH) {
                    edges.add(new Edge(i, j, d));
                }
            }
        }

        Collections.sort(edges);

        // Disjoint Set Union for Kruskal's MST
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        class Dsu {
            int find(int x) {
                if (parent[x] != x) {
                    parent[x] = find(parent[x]);
                }
                return parent[x];
            }
        }
        Dsu dsu = new Dsu();

        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }

        for (Edge e : edges) {
            int rootU = dsu.find(e.u);
            int rootV = dsu.find(e.v);
            if (rootU != rootV) {
                parent[rootU] = rootV;
                adj.get(e.u).add(e.v);
                adj.get(e.v).add(e.u);
            }
        }

        // Traverse each connected component tree along its diameter
        boolean[] visited = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;

            List<Integer> component = new ArrayList<>();
            Queue<Integer> q = new ArrayDeque<>();
            q.add(i);
            visited[i] = true;
            while (!q.isEmpty()) {
                int curr = q.poll();
                component.add(curr);
                for (int neighbor : adj.get(curr)) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true;
                        q.add(neighbor);
                    }
                }
            }

            if (component.size() < 2) {
                continue;
            }

            // Two-pass BFS to find the diameter endpoints and path
            int endpointA = findFarthestNode(component.get(0), adj, n).node();
            FarthestPath pathB = findFarthestNode(endpointA, adj, n);
            int endpointB = pathB.node();

            List<SurveyStakePoint> chain = new ArrayList<>();
            int curr = endpointB;
            while (curr != -1) {
                chain.add(unique.get(curr));
                curr = pathB.parent()[curr];
            }

            if (chain.size() >= 2) {
                chains.add(chain);
            }
        }

        return chains;
    }

    private record FarthestPath(int node, double dist, int[] parent) {}

    private static FarthestPath findFarthestNode(int startNode, List<List<Integer>> adj, int totalNodes) {
        double[] dist = new double[totalNodes];
        Arrays.fill(dist, -1.0D);
        int[] parent = new int[totalNodes];
        Arrays.fill(parent, -1);

        Queue<Integer> q = new ArrayDeque<>();
        q.add(startNode);
        dist[startNode] = 0.0D;

        int farthestNode = startNode;
        double maxDist = 0.0D;

        while (!q.isEmpty()) {
            int u = q.poll();
            if (dist[u] > maxDist) {
                maxDist = dist[u];
                farthestNode = u;
            }
            for (int v : adj.get(u)) {
                if (dist[v] < 0.0D) {
                    dist[v] = dist[u] + 1.0D;
                    parent[v] = u;
                    q.add(v);
                }
            }
        }

        return new FarthestPath(farthestNode, maxDist, parent);
    }

    public record BuildResult(boolean success, int columnsShaped, int stakesFound, Component message) {
        public static BuildResult failure(Component message) {
            return new BuildResult(false, 0, 0, message);
        }

        public static BuildResult success(int columnsShaped, int stakesFound, Component message) {
            return new BuildResult(true, columnsShaped, stakesFound, message);
        }
    }

    /**
     * Builds an ideally smooth, continuous 3D mathematical road subgrade between all survey stakes/flags
     * located near the origin.
     *
     * @param level Server world
     * @param origin Execution position / player position
     * @param searchRadius Search radius for stakes (typically 96-128 blocks)
     * @param material Granular material to use (defaults to gravel)
     * @return Result of the operation with statistics
     */
    public static BuildResult buildSubgradeBetweenStakes(
            ServerLevel level,
            Vec3 origin,
            double searchRadius,
            @Nullable GranularMaterial material
    ) {
        if (level == null || origin == null) {
            return BuildResult.failure(Component.literal("§cBłąd: Nieprawidłowy poziom serwera."));
        }

        List<SurveyStakePoint> stakes = findStakesNearby(level, origin, searchRadius);
        if (stakes.size() < 2) {
            return BuildResult.failure(Component.literal(String.format(
                    "§cNie znaleziono co najmniej 2 tyczek/pachołków w promieniu %.0fm! (Znaleziono: %d)\n§7Postaw co najmniej 2 Tyczki Drogowe lub flagi, aby wyznaczyć trasę drogi.",
                    searchRadius, stakes.size()
            )));
        }

        GranularMaterial fillMaterial = (material != null) ? material : GranularMaterialRegistry.GRAVEL;
        Block fillBlock = (fillMaterial == GranularMaterialRegistry.DIRT) ? Blocks.DIRT
                : (fillMaterial == GranularMaterialRegistry.SAND) ? Blocks.SAND
                : (fillMaterial == GranularMaterialRegistry.COBBLESTONE) ? Blocks.COBBLESTONE
                : Blocks.GRAVEL;

        Set<Long> processedColumns = new HashSet<>();
        int columnsShaped = 0;

        List<List<SurveyStakePoint>> chains = buildSplineChains(stakes);
        if (chains.isEmpty()) {
            return BuildResult.failure(Component.literal("§cNie można wyznaczyć trasy z podanych tyczek (odległość pomiędzy nimi przekracza " + (int) MAX_SEGMENT_LENGTH + "m)."));
        }

        for (List<SurveyStakePoint> chain : chains) {
            int m = chain.size();
            for (int k = 0; k < m - 1; k++) {
                SurveyStakePoint p1 = chain.get(k);
                SurveyStakePoint p2 = chain.get(k + 1);

                double chordDx = p2.x - p1.x;
                double chordDz = p2.z - p1.z;
                double chordLen = Math.sqrt(chordDx * chordDx + chordDz * chordDz);
                if (chordLen < 0.1D) {
                    continue;
                }

                SurveyStakePoint p0 = (k > 0) ? chain.get(k - 1) : new SurveyStakePoint(
                        2.0D * p1.x - p2.x, 2.0D * p1.y - p2.y, 2.0D * p1.z - p2.z, p1.corridorWidth, p1.crownSlope
                );
                SurveyStakePoint p3 = (k + 2 < m) ? chain.get(k + 2) : new SurveyStakePoint(
                        2.0D * p2.x - p1.x, 2.0D * p2.y - p1.y, 2.0D * p2.z - p1.z, p2.corridorWidth, p2.crownSlope
                );

                Vec3 naturalDir1 = new Vec3(p2.x - p0.x, 0.0D, p2.z - p0.z);
                Vec3 naturalDir2 = new Vec3(p3.x - p1.x, 0.0D, p3.z - p1.z);

                Vec3 dir1 = (k == 0 && p1.hasExplicitYaw()) ? p1.forwardDir() : naturalDir1;
                Vec3 dir2 = (k == m - 2 && p2.hasExplicitYaw()) ? p2.forwardDir() : naturalDir2;

                double d1Len = Math.sqrt(dir1.x * dir1.x + dir1.z * dir1.z);
                dir1 = (d1Len > 1e-4D) ? dir1.scale(1.0D / d1Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                double d2Len = Math.sqrt(dir2.x * dir2.x + dir2.z * dir2.z);
                dir2 = (d2Len > 1e-4D) ? dir2.scale(1.0D / d2Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                if (dir1.x * chordDx + dir1.z * chordDz < 0.0D) {
                    dir1 = dir1.scale(-1.0D);
                }
                if (dir2.x * chordDx + dir2.z * chordDz < 0.0D) {
                    dir2 = dir2.scale(-1.0D);
                }

                double v0x = dir1.x * chordLen;
                double v0z = dir1.z * chordLen;
                double v1x = dir2.x * chordLen;
                double v1z = dir2.z * chordLen;

                double halfWidth = (p1.corridorWidth + p2.corridorWidth) / 4.0D;

                // Find the bounding box along the Hermite curve arc
                double segMinX = Double.MAX_VALUE;
                double segMaxX = -Double.MAX_VALUE;
                double segMinZ = Double.MAX_VALUE;
                double segMaxZ = -Double.MAX_VALUE;

                int arcSteps = 16;
                for (int s = 0; s <= arcSteps; s++) {
                    double t = s / (double) arcSteps;
                    double cx = evalHermite(p1.x, v0x, p2.x, v1x, t);
                    double cz = evalHermite(p1.z, v0z, p2.z, v1z, t);
                    if (cx < segMinX) segMinX = cx;
                    if (cx > segMaxX) segMaxX = cx;
                    if (cz < segMinZ) segMinZ = cz;
                    if (cz > segMaxZ) segMaxZ = cz;
                }

                int minX = Mth.floor(segMinX - halfWidth - 1.0D);
                int maxX = Mth.floor(segMaxX + halfWidth + 1.0D);
                int minZ = Mth.floor(segMinZ - halfWidth - 1.0D);
                int maxZ = Mth.floor(segMaxZ + halfWidth + 1.0D);

                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        SurveyTarget target = sampleTargetFromStakes(stakes, bx + 0.5D, bz + 0.5D);
                        if (!target.inCorridor()) {
                            continue;
                        }

                        long colKey = (((long) bx) << 32) | (bz & 0xFFFFFFFFL);
                        if (!processedColumns.add(colKey)) {
                            continue;
                        }

                        RoadPlanarShaper.PlanarSurfaceFunction surfaceFunc = (wx, wz) -> {
                            SurveyTarget pt = sampleTargetFromStakes(stakes, wx, wz);
                            return pt.inCorridor() ? pt.targetY() : target.targetY();
                        };

                        double minSubgradeY = Double.MAX_VALUE;
                        double maxSubgradeY = -Double.MAX_VALUE;
                        for (int sz = 0; sz < 8; sz++) {
                            for (int sx = 0; sx < 8; sx++) {
                                double wy = surfaceFunc.getY(bx + (sx + 0.5D) / 8.0D, bz + (sz + 0.5D) / 8.0D);
                                if (wy < minSubgradeY) minSubgradeY = wy;
                                if (wy > maxSubgradeY) maxSubgradeY = wy;
                            }
                        }
                        int minBlockY = Mth.floor(minSubgradeY - 0.0625D);
                        int maxBlockY = Mth.floor(maxSubgradeY + 0.0625D);

                        // 1. Clear overhead clearance strictly above maxBlockY (up to 4 blocks above subgrade)
                        // IMPORTANT: Do NOT destroy granular subgrade blocks - they may belong to adjacent grade-break column
                        for (int dy = 1; dy <= 4; dy++) {
                            BlockPos clearPos = new BlockPos(bx, maxBlockY + dy, bz);
                            BlockState clearState = level.getBlockState(clearPos);
                            boolean isGranular = clearState.is(GroundworksMod.GRANULAR_BLOCK);
                            boolean granularIsAsphalt = false;
                            if (isGranular) {
                                GranularCell clearCell = GroundworksApi.queryCell(level, clearPos);
                                granularIsAsphalt = clearCell != null && (
                                        clearCell.materialId() == GranularMaterialRegistry.ASPHALT.id()
                                        || clearCell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                            }
                            if (!clearState.isAir() && (clearState.canBeReplaced()
                                    || RoadRollerEntity.isVegetationOrReplaceable(clearState)
                                    || RoadRollerEntity.isLooseRoadbedBlock(clearState)
                                    || GroundworksApi.isDiggable(level, clearPos)
                                    || (isGranular && granularIsAsphalt))) {
                                level.destroyBlock(clearPos, false);
                            }
                        }

                        // 2. Foundation fill strictly below minBlockY if hollow or air (up to 8 blocks down)
                        for (int dy = 1; dy <= 8; dy++) {
                            BlockPos belowPos = new BlockPos(bx, minBlockY - dy, bz);
                            BlockState belowState = level.getBlockState(belowPos);
                            if (belowState.isAir() || belowState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(belowState)) {
                                level.setBlock(belowPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            } else {
                                break;
                            }
                        }

                        // Ensure solid foundation ceiling directly below minBlockY so voxel 0 never rests over hollow space
                        BlockPos supportPos = new BlockPos(bx, minBlockY - 1, bz);
                        BlockState supportState = level.getBlockState(supportPos);
                        if (supportState.isAir() || supportState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(supportState)) {
                            level.setBlock(supportPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }

                        // 3. Pre-initialize all blocks in column as FULL granular material (bottom-up)
                        // This ensures belowSupported check in shapeCellToSurface always passes for
                        // grade-break blocks (vanilla gravel canBeReplaced=true causes false negatives)
                        for (int by = minBlockY; by <= maxBlockY; by++) {
                            BlockPos cellPos = new BlockPos(bx, by, bz);
                            BlockState cellState = level.getBlockState(cellPos);
                            if (!cellState.is(GroundworksMod.GRANULAR_BLOCK)) {
                                // Convert to full granular block so shapeCellToSurface can shape it
                                GranularCell preCell = RoadRollerEntity.isLooseRoadbedBlock(cellState)
                                        ? BlockConverter.convert(level, cellPos, cellState)
                                        : null;
                                if (preCell == null) {
                                    preCell = GranularCell.full(fillMaterial);
                                    preCell.setMaterialId(fillMaterial.id());
                                }
                                level.setBlock(cellPos, GroundworksMod.GRANULAR_BLOCK.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                                BlockEntity preBe = level.getBlockEntity(cellPos);
                                if (preBe instanceof GranularBlockEntity preGbe) {
                                    preGbe.setMaterialId(fillMaterial.id());
                                    preGbe.setCell(preCell);
                                    preGbe.setChanged();
                                    level.sendBlockUpdated(cellPos, preBe.getBlockState(), preBe.getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                                }
                                GranularWorldStorage.get(level).putCell(cellPos, preCell);
                            }
                        }

                        // 4. Shape all blocks spanning the subgrade surface [minBlockY, maxBlockY]
                        for (int by = minBlockY; by <= maxBlockY; by++) {
                            BlockPos cellPos = new BlockPos(bx, by, bz);
                            RoadPlanarShaper.shapeCellToSurface(
                                    level,
                                    cellPos,
                                    surfaceFunc,
                                    fillMaterial,
                                    512,
                                    false,
                                    Integer.MAX_VALUE,
                                    RoadbedCorridorStorage.RoadbedStage.COMPACTED,
                                    target.slopeX(),
                                    target.slopeZ()
                            );
                        }

                        columnsShaped++;
                    }
                }
            }
        }

        if (columnsShaped == 0) {
            return BuildResult.failure(Component.literal("§cNie znaleziono żadnych bloków do uformowania w korytarzu tyczek."));
        }

        return BuildResult.success(columnsShaped, stakes.size(), Component.literal(String.format(
                "§6[Groundworks Road] §aZbudowano idealnie gładką podbudowę drogi! §7(Uformowano %d kolumn podbudowy pomiędzy %d tyczkami)",
                columnsShaped, stakes.size()
        )));
    }

    /**
     * Lays fresh asphalt mat (PAVED stage) on top of the road subgrade between survey stakes.
     *
     * @param level Server world
     * @param origin Execution position / player position
     * @param searchRadius Search radius for stakes (typically 96-128 blocks)
     * @param fallbackMaterial Granular material to use if subgrade foundation needs backfilling (defaults to gravel)
     * @return Result of the operation with statistics
     */
    public static BuildResult paveAsphaltBetweenStakes(
            ServerLevel level,
            Vec3 origin,
            double searchRadius,
            @Nullable GranularMaterial fallbackMaterial
    ) {
        if (level == null || origin == null) {
            return BuildResult.failure(Component.literal("§cBłąd: Nieprawidłowy poziom serwera."));
        }

        List<SurveyStakePoint> stakes = findStakesNearby(level, origin, searchRadius);
        if (stakes.size() < 2) {
            return BuildResult.failure(Component.literal(String.format(
                    "§cNie znaleziono co najmniej 2 tyczek/pachołków w promieniu %.0fm! (Znaleziono: %d)\n§7Postaw co najmniej 2 Tyczki Drogowe lub flagi, aby wyznaczyć trasę drogi.",
                    searchRadius, stakes.size()
            )));
        }

        GranularMaterial fillMaterial = (fallbackMaterial != null) ? fallbackMaterial : GranularMaterialRegistry.GRAVEL;
        Block fillBlock = (fillMaterial == GranularMaterialRegistry.DIRT) ? Blocks.DIRT
                : (fillMaterial == GranularMaterialRegistry.SAND) ? Blocks.SAND
                : (fillMaterial == GranularMaterialRegistry.COBBLESTONE) ? Blocks.COBBLESTONE
                : Blocks.GRAVEL;

        Set<Long> processedColumns = new HashSet<>();
        int columnsPaved = 0;

        List<List<SurveyStakePoint>> chains = buildSplineChains(stakes);
        if (chains.isEmpty()) {
            return BuildResult.failure(Component.literal("§cNie można wyznaczyć trasy z podanych tyczek."));
        }

        RoadbedCorridorStorage storage = RoadbedCorridorStorage.get(level);

        for (List<SurveyStakePoint> chain : chains) {
            int m = chain.size();
            for (int k = 0; k < m - 1; k++) {
                SurveyStakePoint p1 = chain.get(k);
                SurveyStakePoint p2 = chain.get(k + 1);

                double chordDx = p2.x - p1.x;
                double chordDz = p2.z - p1.z;
                double chordLen = Math.sqrt(chordDx * chordDx + chordDz * chordDz);
                if (chordLen < 0.1D) {
                    continue;
                }

                SurveyStakePoint p0 = (k > 0) ? chain.get(k - 1) : new SurveyStakePoint(
                        2.0D * p1.x - p2.x, 2.0D * p1.y - p2.y, 2.0D * p1.z - p2.z, p1.corridorWidth, p1.crownSlope
                );
                SurveyStakePoint p3 = (k + 2 < m) ? chain.get(k + 2) : new SurveyStakePoint(
                        2.0D * p2.x - p1.x, 2.0D * p2.y - p1.y, 2.0D * p2.z - p1.z, p2.corridorWidth, p2.crownSlope
                );

                Vec3 naturalDir1 = new Vec3(p2.x - p0.x, 0.0D, p2.z - p0.z);
                Vec3 naturalDir2 = new Vec3(p3.x - p1.x, 0.0D, p3.z - p1.z);

                Vec3 dir1 = (k == 0 && p1.hasExplicitYaw()) ? p1.forwardDir() : naturalDir1;
                Vec3 dir2 = (k == m - 2 && p2.hasExplicitYaw()) ? p2.forwardDir() : naturalDir2;

                double d1Len = Math.sqrt(dir1.x * dir1.x + dir1.z * dir1.z);
                dir1 = (d1Len > 1e-4D) ? dir1.scale(1.0D / d1Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                double d2Len = Math.sqrt(dir2.x * dir2.x + dir2.z * dir2.z);
                dir2 = (d2Len > 1e-4D) ? dir2.scale(1.0D / d2Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                if (dir1.x * chordDx + dir1.z * chordDz < 0.0D) {
                    dir1 = dir1.scale(-1.0D);
                }
                if (dir2.x * chordDx + dir2.z * chordDz < 0.0D) {
                    dir2 = dir2.scale(-1.0D);
                }

                double v0x = dir1.x * chordLen;
                double v0z = dir1.z * chordLen;
                double v1x = dir2.x * chordLen;
                double v1z = dir2.z * chordLen;

                double halfWidth = (p1.corridorWidth + p2.corridorWidth) / 4.0D;

                double segMinX = Double.MAX_VALUE;
                double segMaxX = -Double.MAX_VALUE;
                double segMinZ = Double.MAX_VALUE;
                double segMaxZ = -Double.MAX_VALUE;

                int arcSteps = 16;
                for (int s = 0; s <= arcSteps; s++) {
                    double t = s / (double) arcSteps;
                    double cx = evalHermite(p1.x, v0x, p2.x, v1x, t);
                    double cz = evalHermite(p1.z, v0z, p2.z, v1z, t);
                    if (cx < segMinX) segMinX = cx;
                    if (cx > segMaxX) segMaxX = cx;
                    if (cz < segMinZ) segMinZ = cz;
                    if (cz > segMaxZ) segMaxZ = cz;
                }

                int minX = Mth.floor(segMinX - halfWidth - 1.0D);
                int maxX = Mth.floor(segMaxX + halfWidth + 1.0D);
                int minZ = Mth.floor(segMinZ - halfWidth - 1.0D);
                int maxZ = Mth.floor(segMaxZ + halfWidth + 1.0D);

                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        SurveyTarget target = sampleTargetFromStakes(stakes, bx + 0.5D, bz + 0.5D);
                        if (!target.inCorridor()) {
                            continue;
                        }

                        long colKey = (((long) bx) << 32) | (bz & 0xFFFFFFFFL);
                        if (!processedColumns.add(colKey)) {
                            continue;
                        }

                        RoadPlanarShaper.PlanarSurfaceFunction paveSurfaceFunc = (wx, wz) -> {
                            SurveyTarget pt = sampleTargetFromStakes(stakes, wx, wz);
                            return pt.inCorridor() ? pt.targetY() + 0.125D : target.targetY() + 0.125D;
                        };

                        double minPavedY = Double.MAX_VALUE;
                        double maxPavedY = -Double.MAX_VALUE;
                        for (int sz = 0; sz < 8; sz++) {
                            for (int sx = 0; sx < 8; sx++) {
                                double wy = paveSurfaceFunc.getY(bx + (sx + 0.5D) / 8.0D, bz + (sz + 0.5D) / 8.0D);
                                if (wy < minPavedY) minPavedY = wy;
                                if (wy > maxPavedY) maxPavedY = wy;
                            }
                        }
                        int minBlockY = Mth.floor(minPavedY - 0.0625D);
                        int maxBlockY = Mth.floor(maxPavedY + 0.0625D);

                        // 1. Clear overhead clearance strictly above maxBlockY (up to 4 blocks above paved road)
                        // IMPORTANT: Do NOT destroy granular subgrade blocks - they may belong to adjacent grade-break column
                        for (int dy = 1; dy <= 4; dy++) {
                            BlockPos clearPos = new BlockPos(bx, maxBlockY + dy, bz);
                            BlockState clearState = level.getBlockState(clearPos);
                            boolean isGranular = clearState.is(GroundworksMod.GRANULAR_BLOCK);
                            boolean granularIsAsphalt = false;
                            if (isGranular) {
                                GranularCell clearCell = GroundworksApi.queryCell(level, clearPos);
                                granularIsAsphalt = clearCell != null && (
                                        clearCell.materialId() == GranularMaterialRegistry.ASPHALT.id()
                                        || clearCell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                            }
                            if (!clearState.isAir() && (clearState.canBeReplaced()
                                    || RoadRollerEntity.isVegetationOrReplaceable(clearState)
                                    || GroundworksApi.isDiggable(level, clearPos)
                                    || (isGranular && granularIsAsphalt))) {
                                level.destroyBlock(clearPos, false);
                            }
                        }

                        // 2. Ensure subgrade foundation below minBlockY (up to 8 blocks down)
                        for (int dy = 1; dy <= 8; dy++) {
                            BlockPos belowPos = new BlockPos(bx, minBlockY - dy, bz);
                            BlockState belowState = level.getBlockState(belowPos);
                            if (belowState.isAir() || belowState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(belowState)) {
                                level.setBlock(belowPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            } else {
                                break;
                            }
                        }

                        // Ensure solid foundation ceiling directly below minBlockY so voxel 0 never rests over hollow space
                        BlockPos supportPos = new BlockPos(bx, minBlockY - 1, bz);
                        BlockState supportState = level.getBlockState(supportPos);
                        if (supportState.isAir() || supportState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(supportState)) {
                            level.setBlock(supportPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }

                        // 3. Lay fresh asphalt mat across all blocks spanning [minBlockY, maxBlockY]
                        for (int by = minBlockY; by <= maxBlockY; by++) {
                            BlockPos cellPos = new BlockPos(bx, by, bz);
                            BlockState cellState = level.getBlockState(cellPos);
                            if (cellState.isAir() || cellState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(cellState)) {
                                level.setBlock(cellPos, GroundworksAsphaltMod.FRESH_ASPHALT.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            }
                            RoadPlanarShaper.shapeCellToSurface(
                                    level,
                                    cellPos,
                                    paveSurfaceFunc,
                                    GranularMaterialRegistry.ASPHALT,
                                    512,
                                    false,
                                    Integer.MAX_VALUE,
                                    RoadbedCorridorStorage.RoadbedStage.PAVED,
                                    target.slopeX(),
                                    target.slopeZ()
                            );
                        }

                        // 4. Smoke particle
                        double targetPavedY = target.targetY() + 0.125D;
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, bx + 0.5D, targetPavedY + 0.1D, bz + 0.5D, 1, 0.2D, 0.05D, 0.2D, 0.01D);

                        columnsPaved++;
                    }
                }
            }
        }

        if (columnsPaved == 0) {
            return BuildResult.failure(Component.literal("§cNie znaleziono żadnych bloków do rozłożenia asfaltu w korytarzu tyczek."));
        }

        level.playSound(null, origin.x, origin.y, origin.z, GroundworksAsphaltMod.PAVER_SCREED, SoundSource.BLOCKS, 0.9F, 1.0F);

        return BuildResult.success(columnsPaved, stakes.size(), Component.literal(String.format(
                "§6[Groundworks Road] §aRozłożono świeżą warstwę asfaltu na podbudowie! §7(Rozłożono %d kolumn asfaltu pomiędzy %d tyczkami)",
                columnsPaved, stakes.size()
        )));
    }

    /**
     * Builds a complete, finished, smooth asphalt road (FINISHED stage) between survey stakes,
     * including foundation backfill, dense asphalt pavement, and 3D microvoxel crown smoothing.
     *
     * @param level Server world
     * @param origin Execution position / player position
     * @param searchRadius Search radius for stakes (typically 96-128 blocks)
     * @param foundationMaterial Granular material to use for subgrade foundation (defaults to gravel)
     * @return Result of the operation with statistics
     */
    public static BuildResult buildFinishedRoadBetweenStakes(
            ServerLevel level,
            Vec3 origin,
            double searchRadius,
            @Nullable GranularMaterial foundationMaterial
    ) {
        if (level == null || origin == null) {
            return BuildResult.failure(Component.literal("§cBłąd: Nieprawidłowy poziom serwera."));
        }

        List<SurveyStakePoint> stakes = findStakesNearby(level, origin, searchRadius);
        if (stakes.size() < 2) {
            return BuildResult.failure(Component.literal(String.format(
                    "§cNie znaleziono co najmniej 2 tyczek/pachołków w promieniu %.0fm! (Znaleziono: %d)\n§7Postaw co najmniej 2 Tyczki Drogowe lub flagi, aby wyznaczyć trasę drogi.",
                    searchRadius, stakes.size()
            )));
        }

        GranularMaterial fillMaterial = (foundationMaterial != null) ? foundationMaterial : GranularMaterialRegistry.GRAVEL;
        Block fillBlock = (fillMaterial == GranularMaterialRegistry.DIRT) ? Blocks.DIRT
                : (fillMaterial == GranularMaterialRegistry.SAND) ? Blocks.SAND
                : (fillMaterial == GranularMaterialRegistry.COBBLESTONE) ? Blocks.COBBLESTONE
                : Blocks.GRAVEL;

        Set<Long> processedColumns = new HashSet<>();
        int columnsFinished = 0;

        List<List<SurveyStakePoint>> chains = buildSplineChains(stakes);
        if (chains.isEmpty()) {
            return BuildResult.failure(Component.literal("§cNie można wyznaczyć trasy z podanych tyczek."));
        }

        for (List<SurveyStakePoint> chain : chains) {
            int m = chain.size();
            for (int k = 0; k < m - 1; k++) {
                SurveyStakePoint p1 = chain.get(k);
                SurveyStakePoint p2 = chain.get(k + 1);

                double chordDx = p2.x - p1.x;
                double chordDz = p2.z - p1.z;
                double chordLen = Math.sqrt(chordDx * chordDx + chordDz * chordDz);
                if (chordLen < 0.1D) {
                    continue;
                }

                SurveyStakePoint p0 = (k > 0) ? chain.get(k - 1) : new SurveyStakePoint(
                        2.0D * p1.x - p2.x, 2.0D * p1.y - p2.y, 2.0D * p1.z - p2.z, p1.corridorWidth, p1.crownSlope
                );
                SurveyStakePoint p3 = (k + 2 < m) ? chain.get(k + 2) : new SurveyStakePoint(
                        2.0D * p2.x - p1.x, 2.0D * p2.y - p1.y, 2.0D * p2.z - p1.z, p2.corridorWidth, p2.crownSlope
                );

                Vec3 naturalDir1 = new Vec3(p2.x - p0.x, 0.0D, p2.z - p0.z);
                Vec3 naturalDir2 = new Vec3(p3.x - p1.x, 0.0D, p3.z - p1.z);

                Vec3 dir1 = (k == 0 && p1.hasExplicitYaw()) ? p1.forwardDir() : naturalDir1;
                Vec3 dir2 = (k == m - 2 && p2.hasExplicitYaw()) ? p2.forwardDir() : naturalDir2;

                double d1Len = Math.sqrt(dir1.x * dir1.x + dir1.z * dir1.z);
                dir1 = (d1Len > 1e-4D) ? dir1.scale(1.0D / d1Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                double d2Len = Math.sqrt(dir2.x * dir2.x + dir2.z * dir2.z);
                dir2 = (d2Len > 1e-4D) ? dir2.scale(1.0D / d2Len) : new Vec3(chordDx / chordLen, 0.0D, chordDz / chordLen);

                if (dir1.x * chordDx + dir1.z * chordDz < 0.0D) {
                    dir1 = dir1.scale(-1.0D);
                }
                if (dir2.x * chordDx + dir2.z * chordDz < 0.0D) {
                    dir2 = dir2.scale(-1.0D);
                }

                double v0x = dir1.x * chordLen;
                double v0z = dir1.z * chordLen;
                double v1x = dir2.x * chordLen;
                double v1z = dir2.z * chordLen;

                double halfWidth = (p1.corridorWidth + p2.corridorWidth) / 4.0D;

                double segMinX = Double.MAX_VALUE;
                double segMaxX = -Double.MAX_VALUE;
                double segMinZ = Double.MAX_VALUE;
                double segMaxZ = -Double.MAX_VALUE;

                int arcSteps = 16;
                for (int s = 0; s <= arcSteps; s++) {
                    double t = s / (double) arcSteps;
                    double cx = evalHermite(p1.x, v0x, p2.x, v1x, t);
                    double cz = evalHermite(p1.z, v0z, p2.z, v1z, t);
                    if (cx < segMinX) segMinX = cx;
                    if (cx > segMaxX) segMaxX = cx;
                    if (cz < segMinZ) segMinZ = cz;
                    if (cz > segMaxZ) segMaxZ = cz;
                }

                int minX = Mth.floor(segMinX - halfWidth - 1.0D);
                int maxX = Mth.floor(segMaxX + halfWidth + 1.0D);
                int minZ = Mth.floor(segMinZ - halfWidth - 1.0D);
                int maxZ = Mth.floor(segMaxZ + halfWidth + 1.0D);

                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        SurveyTarget target = sampleTargetFromStakes(stakes, bx + 0.5D, bz + 0.5D);
                        if (!target.inCorridor()) {
                            continue;
                        }

                        long colKey = (((long) bx) << 32) | (bz & 0xFFFFFFFFL);
                        if (!processedColumns.add(colKey)) {
                            continue;
                        }

                        // Finished road elevation (+0.10m compacted asphalt mat on subgrade)
                        RoadPlanarShaper.PlanarSurfaceFunction surfaceFunc = (wx, wz) -> {
                            SurveyTarget pt = sampleTargetFromStakes(stakes, wx, wz);
                            return pt.inCorridor() ? pt.targetY() + 0.10D : target.targetY() + 0.10D;
                        };

                        double minFinishedY = Double.MAX_VALUE;
                        double maxFinishedY = -Double.MAX_VALUE;
                        for (int sz = 0; sz < 8; sz++) {
                            for (int sx = 0; sx < 8; sx++) {
                                double wy = surfaceFunc.getY(bx + (sx + 0.5D) / 8.0D, bz + (sz + 0.5D) / 8.0D);
                                if (wy < minFinishedY) minFinishedY = wy;
                                if (wy > maxFinishedY) maxFinishedY = wy;
                            }
                        }
                        int minBlockY = Mth.floor(minFinishedY - 0.0625D);
                        int maxBlockY = Mth.floor(maxFinishedY + 0.0625D);

                        // 1. Clear overhead clearance strictly above maxBlockY (up to 4 blocks above finished road)
                        // IMPORTANT: Do NOT destroy granular subgrade blocks - they may belong to adjacent grade-break column
                        for (int dy = 1; dy <= 4; dy++) {
                            BlockPos clearPos = new BlockPos(bx, maxBlockY + dy, bz);
                            BlockState clearState = level.getBlockState(clearPos);
                            boolean isGranular = clearState.is(GroundworksMod.GRANULAR_BLOCK);
                            boolean granularIsAsphalt = false;
                            if (isGranular) {
                                GranularCell clearCell = GroundworksApi.queryCell(level, clearPos);
                                granularIsAsphalt = clearCell != null && (
                                        clearCell.materialId() == GranularMaterialRegistry.ASPHALT.id()
                                        || clearCell.materialId() == GranularMaterialRegistry.COMPACTED_ASPHALT.id());
                            }
                            if (!clearState.isAir() && (clearState.canBeReplaced()
                                    || RoadRollerEntity.isVegetationOrReplaceable(clearState)
                                    || RoadRollerEntity.isLooseRoadbedBlock(clearState)
                                    || GroundworksApi.isDiggable(level, clearPos)
                                    || (isGranular && granularIsAsphalt))) {
                                level.destroyBlock(clearPos, false);
                            }
                        }

                        // 2. Foundation fill strictly below minBlockY if hollow or air (up to 8 blocks down)
                        for (int dy = 1; dy <= 8; dy++) {
                            BlockPos belowPos = new BlockPos(bx, minBlockY - dy, bz);
                            BlockState belowState = level.getBlockState(belowPos);
                            if (belowState.isAir() || belowState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(belowState)) {
                                level.setBlock(belowPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            } else {
                                break;
                            }
                        }

                        // 3. Ensure solid foundation ceiling directly below minBlockY so voxel 0 never rests over hollow space
                        BlockPos supportPos = new BlockPos(bx, minBlockY - 1, bz);
                        BlockState supportState = level.getBlockState(supportPos);
                        if (supportState.isAir() || supportState.canBeReplaced() || RoadRollerEntity.isVegetationOrReplaceable(supportState)) {
                            level.setBlock(supportPos, fillBlock.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                        }

                        // 4. Shape all blocks spanning the finished asphalt surface [minBlockY, maxBlockY]
                        for (int by = minBlockY; by <= maxBlockY; by++) {
                            BlockPos cellPos = new BlockPos(bx, by, bz);
                            BlockState cellState = level.getBlockState(cellPos);
                            if (!RoadRollerEntity.isAsphaltBlock(cellState)) {
                                level.setBlock(cellPos, GroundworksAsphaltMod.COMPACTED_ASPHALT.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                            }
                            RoadPlanarShaper.shapeCellToSurface(
                                    level,
                                    cellPos,
                                    surfaceFunc,
                                    GranularMaterialRegistry.COMPACTED_ASPHALT,
                                    512,
                                    true, // forceCompactedAsphalt converts any raw asphalt/subgrade to finished asphalt
                                    Integer.MAX_VALUE,
                                    RoadbedCorridorStorage.RoadbedStage.FINISHED,
                                    target.slopeX(),
                                    target.slopeZ()
                            );
                        }

                        // 5. Particles
                        double targetFinishedY = target.targetY() + 0.10D;
                        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, GroundworksAsphaltMod.COMPACTED_ASPHALT.defaultBlockState()),
                                bx + 0.5D, targetFinishedY + 0.05D, bz + 0.5D, 1, 0.2D, 0.02D, 0.2D, 0.01D);

                        columnsFinished++;
                    }
                }
            }
        }

        if (columnsFinished == 0) {
            return BuildResult.failure(Component.literal("§cNie znaleziono żadnych bloków do uformowania gotowej drogi w korytarzu tyczek."));
        }

        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);

        return BuildResult.success(columnsFinished, stakes.size(), Component.literal(String.format(
                "§6[Groundworks Road] §aStworzono gotową drogę asfaltową! §7(Ukończono i wygładzono %d kolumn nawierzchni pomiędzy %d tyczkami)",
                columnsFinished, stakes.size()
        )));
    }
}
