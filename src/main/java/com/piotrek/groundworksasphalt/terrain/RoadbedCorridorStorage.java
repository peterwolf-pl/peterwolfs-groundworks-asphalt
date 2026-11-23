package com.piotrek.groundworksasphalt.terrain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent Roadbed Corridor & Mesh Storage for Minecraft 26.3.
 *
 * <p>Tracks the cumulative engineering stages of the roadbed corridor as machines pass over it:
 * <ol>
 *   <li>{@link RoadbedStage#RAW}: Natural terrain / unconverted.</li>
 *   <li>{@link RoadbedStage#BULLDOZED}: Rough earthworks / excavation slot cleared by Bulldozer.</li>
 *   <li>{@link RoadbedStage#GRADED}: Precision planar slope and crown cut by Cat Grader moldboard.</li>
 *   <li>{@link RoadbedStage#COMPACTED}: Heavy subgrade consolidation locked by 16T Road Roller.</li>
 *   <li>{@link RoadbedStage#PAVED}: Uniform fresh asphalt mat laid by Asphalt Paver.</li>
 *   <li>{@link RoadbedStage#FINISHED}: Final compacted pavement finished by 14T Asphalt Roller.</li>
 * </ol>
 *
 * Enables multi-pass stitching (e.g. adjacent grader passes aligning to the road crown)
 * and guarantees that subsequent compaction passes preserve and harden the established road mesh
 * rather than wiping it out with floating regressions.
 */
public class RoadbedCorridorStorage extends SavedData {

    public static final String MOD_ID = "pw_groundworks_asphalt";
    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "roadbed_corridor");

    public enum RoadbedStage {
        RAW(0),
        BULLDOZED(1),
        GRADED(2),
        COMPACTED(3),
        PAVED(4),
        FINISHED(5);

        private final int level;

        RoadbedStage(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isAtLeast(RoadbedStage other) {
            return this.level >= other.level;
        }

        public static RoadbedStage byLevel(int level) {
            for (RoadbedStage stage : values()) {
                if (stage.level == level) return stage;
            }
            return RAW;
        }
    }

    public record RoadbedEntry(
            long pos,
            int stageId,
            double targetY,
            double slopeX,
            double slopeZ,
            long gameTime
    ) {
        public static final Codec<RoadbedEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.LONG.fieldOf("pos").forGetter(RoadbedEntry::pos),
                        Codec.INT.fieldOf("stage").forGetter(RoadbedEntry::stageId),
                        Codec.DOUBLE.fieldOf("targetY").forGetter(RoadbedEntry::targetY),
                        Codec.DOUBLE.optionalFieldOf("slopeX", 0.0D).forGetter(RoadbedEntry::slopeX),
                        Codec.DOUBLE.optionalFieldOf("slopeZ", 0.0D).forGetter(RoadbedEntry::slopeZ),
                        Codec.LONG.optionalFieldOf("time", 0L).forGetter(RoadbedEntry::gameTime)
                ).apply(instance, RoadbedEntry::new)
        );

        public RoadbedStage stage() {
            return RoadbedStage.byLevel(stageId);
        }

        public double getY(double worldX, double worldZ) {
            BlockPos bp = BlockPos.of(pos);
            double dx = worldX - (bp.getX() + 0.5D);
            double dz = worldZ - (bp.getZ() + 0.5D);
            return targetY + dx * slopeX + dz * slopeZ;
        }
    }

    public static final Codec<RoadbedCorridorStorage> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.list(RoadbedEntry.CODEC).fieldOf("entries").forGetter(RoadbedCorridorStorage::toEntries)
            ).apply(instance, RoadbedCorridorStorage::fromEntries)
    );

    public static final SavedDataType<RoadbedCorridorStorage> TYPE = new SavedDataType<>(
            TYPE_ID,
            RoadbedCorridorStorage::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<Long, RoadbedEntry> entries = new ConcurrentHashMap<>();

    public RoadbedCorridorStorage() {}

    public static RoadbedCorridorStorage get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<RoadbedEntry> toEntries() {
        return new ArrayList<>(entries.values());
    }

    private static RoadbedCorridorStorage fromEntries(List<RoadbedEntry> list) {
        RoadbedCorridorStorage storage = new RoadbedCorridorStorage();
        if (list != null) {
            for (RoadbedEntry entry : list) {
                storage.entries.put(entry.pos(), entry);
            }
        }
        return storage;
    }

    @Nullable
    public RoadbedEntry getEntry(BlockPos pos) {
        return entries.get(pos.asLong());
    }

    @Nullable
    public RoadbedEntry getEntry(long posLong) {
        return entries.get(posLong);
    }

    public RoadbedStage getStage(BlockPos pos) {
        RoadbedEntry entry = getEntry(pos);
        return entry != null ? entry.stage() : RoadbedStage.RAW;
    }

    public void recordStage(
            BlockPos pos,
            RoadbedStage stage,
            double targetY,
            double slopeX,
            double slopeZ,
            long gameTime
    ) {
        long key = pos.asLong();
        RoadbedEntry current = entries.get(key);
        // Upgrade stage or update geometry if at same or higher stage
        if (current == null || stage.isAtLeast(current.stage())) {
            entries.put(key, new RoadbedEntry(key, stage.getLevel(), targetY, slopeX, slopeZ, gameTime));
            this.setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) {
            this.setDirty();
        }
    }

    public boolean isRoadbed(BlockPos pos) {
        RoadbedEntry entry = getEntry(pos);
        return entry != null && entry.stage().isAtLeast(RoadbedStage.GRADED);
    }
}
