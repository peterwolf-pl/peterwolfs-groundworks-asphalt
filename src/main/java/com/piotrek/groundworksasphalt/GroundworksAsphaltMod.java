package com.piotrek.groundworksasphalt;

import com.piotrek.groundworksasphalt.block.AsphaltSlabBlock;
import com.piotrek.groundworksasphalt.block.CompactedAsphaltBlock;
import com.piotrek.groundworksasphalt.block.FreshAsphaltBlock;
import com.piotrek.groundworksasphalt.block.RoadLineBlock;
import com.piotrek.groundworksasphalt.command.CatGraderCommand;
import com.piotrek.groundworksasphalt.entity.AsphaltPaverEntity;
import com.piotrek.groundworksasphalt.entity.AsphaltRollerEntity;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.item.AsphaltMixItem;
import com.piotrek.groundworksasphalt.item.AsphaltPaverItem;
import com.piotrek.groundworksasphalt.item.AsphaltRollerItem;
import com.piotrek.groundworksasphalt.item.CatGraderItem;
import com.piotrek.groundworksasphalt.item.RoadRollerItem;
import com.piotrek.groundworksasphalt.network.AsphaltInputPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Peterwolf's Groundworks Asphalt core mod initializer.
 */
public class GroundworksAsphaltMod implements ModInitializer {

    public static final String MOD_ID = "pw_groundworks_asphalt";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceKey<Block> blockKey(String path) {
        return ResourceKey.create(Registries.BLOCK, id(path));
    }

    public static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    public static ResourceKey<EntityType<?>> entityKey(String path) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id(path));
    }

    // ── Blocks & Keys ─────────────────────────────────────────────────
    public static final ResourceKey<Block> FRESH_ASPHALT_KEY = blockKey("fresh_asphalt");
    public static final FreshAsphaltBlock FRESH_ASPHALT = Registry.register(
            BuiltInRegistries.BLOCK,
            FRESH_ASPHALT_KEY,
            new FreshAsphaltBlock(BlockBehaviour.Properties.of()
                    .setId(FRESH_ASPHALT_KEY)
                    .strength(1.2F, 2.0F)
                    .sound(SoundType.GRAVEL)
            )
    );

    public static final ResourceKey<Block> COMPACTED_ASPHALT_KEY = blockKey("compacted_asphalt");
    public static final CompactedAsphaltBlock COMPACTED_ASPHALT = Registry.register(
            BuiltInRegistries.BLOCK,
            COMPACTED_ASPHALT_KEY,
            new CompactedAsphaltBlock(BlockBehaviour.Properties.of()
                    .setId(COMPACTED_ASPHALT_KEY)
                    .strength(2.8F, 6.0F)
                    .sound(SoundType.STONE)
                    .friction(0.55F) // Smooth fast road surface
            )
    );

    public static final ResourceKey<Block> ASPHALT_SLAB_KEY = blockKey("asphalt_slab");
    public static final AsphaltSlabBlock ASPHALT_SLAB = Registry.register(
            BuiltInRegistries.BLOCK,
            ASPHALT_SLAB_KEY,
            new AsphaltSlabBlock(BlockBehaviour.Properties.of()
                    .setId(ASPHALT_SLAB_KEY)
                    .strength(2.8F, 6.0F)
                    .sound(SoundType.STONE)
                    .friction(0.55F)
            )
    );

    public static final ResourceKey<Block> ASPHALT_WHITE_LINE_KEY = blockKey("asphalt_white_line");
    public static final RoadLineBlock ASPHALT_WHITE_LINE = Registry.register(
            BuiltInRegistries.BLOCK,
            ASPHALT_WHITE_LINE_KEY,
            new RoadLineBlock(BlockBehaviour.Properties.of()
                    .setId(ASPHALT_WHITE_LINE_KEY)
                    .strength(2.8F, 6.0F)
                    .sound(SoundType.STONE)
                    .friction(0.55F)
            )
    );

    public static final ResourceKey<Block> ASPHALT_YELLOW_LINE_KEY = blockKey("asphalt_yellow_line");
    public static final RoadLineBlock ASPHALT_YELLOW_LINE = Registry.register(
            BuiltInRegistries.BLOCK,
            ASPHALT_YELLOW_LINE_KEY,
            new RoadLineBlock(BlockBehaviour.Properties.of()
                    .setId(ASPHALT_YELLOW_LINE_KEY)
                    .strength(2.8F, 6.0F)
                    .sound(SoundType.STONE)
                    .friction(0.55F)
            )
    );

    // ── Items & Keys ──────────────────────────────────────────────────
    public static final ResourceKey<Item> FRESH_ASPHALT_ITEM_KEY = itemKey("fresh_asphalt");
    public static final BlockItem FRESH_ASPHALT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            FRESH_ASPHALT_ITEM_KEY,
            new BlockItem(FRESH_ASPHALT, new Item.Properties().setId(FRESH_ASPHALT_ITEM_KEY))
    );

    public static final ResourceKey<Item> COMPACTED_ASPHALT_ITEM_KEY = itemKey("compacted_asphalt");
    public static final BlockItem COMPACTED_ASPHALT_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            COMPACTED_ASPHALT_ITEM_KEY,
            new BlockItem(COMPACTED_ASPHALT, new Item.Properties().setId(COMPACTED_ASPHALT_ITEM_KEY))
    );

    public static final ResourceKey<Item> ASPHALT_SLAB_ITEM_KEY = itemKey("asphalt_slab");
    public static final BlockItem ASPHALT_SLAB_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_SLAB_ITEM_KEY,
            new BlockItem(ASPHALT_SLAB, new Item.Properties().setId(ASPHALT_SLAB_ITEM_KEY))
    );

    public static final ResourceKey<Item> ASPHALT_WHITE_LINE_ITEM_KEY = itemKey("asphalt_white_line");
    public static final BlockItem ASPHALT_WHITE_LINE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_WHITE_LINE_ITEM_KEY,
            new BlockItem(ASPHALT_WHITE_LINE, new Item.Properties().setId(ASPHALT_WHITE_LINE_ITEM_KEY))
    );

    public static final ResourceKey<Item> ASPHALT_YELLOW_LINE_ITEM_KEY = itemKey("asphalt_yellow_line");
    public static final BlockItem ASPHALT_YELLOW_LINE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_YELLOW_LINE_ITEM_KEY,
            new BlockItem(ASPHALT_YELLOW_LINE, new Item.Properties().setId(ASPHALT_YELLOW_LINE_ITEM_KEY))
    );

    public static final ResourceKey<Item> ASPHALT_MIX_KEY = itemKey("asphalt_mix");
    public static final AsphaltMixItem ASPHALT_MIX = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_MIX_KEY,
            new AsphaltMixItem(new Item.Properties().setId(ASPHALT_MIX_KEY).stacksTo(64))
    );

    public static final ResourceKey<Item> CAT_GRADER_ITEM_KEY = itemKey("cat_grader");
    public static final CatGraderItem CAT_GRADER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            CAT_GRADER_ITEM_KEY,
            new CatGraderItem(new Item.Properties().setId(CAT_GRADER_ITEM_KEY).stacksTo(1))
    );

    public static final ResourceKey<Item> ASPHALT_PAVER_ITEM_KEY = itemKey("asphalt_paver");
    public static final AsphaltPaverItem ASPHALT_PAVER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_PAVER_ITEM_KEY,
            new AsphaltPaverItem(new Item.Properties().setId(ASPHALT_PAVER_ITEM_KEY).stacksTo(1))
    );

    public static final ResourceKey<Item> ROAD_ROLLER_ITEM_KEY = itemKey("road_roller");
    public static final RoadRollerItem ROAD_ROLLER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ROAD_ROLLER_ITEM_KEY,
            new RoadRollerItem(new Item.Properties().setId(ROAD_ROLLER_ITEM_KEY).stacksTo(1))
    );

    public static final ResourceKey<Item> ASPHALT_ROLLER_ITEM_KEY = itemKey("asphalt_roller");
    public static final AsphaltRollerItem ASPHALT_ROLLER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ASPHALT_ROLLER_ITEM_KEY,
            new AsphaltRollerItem(new Item.Properties().setId(ASPHALT_ROLLER_ITEM_KEY).stacksTo(1))
    );

    public static final ResourceKey<Item> ROAD_STAKE_ITEM_KEY = itemKey("road_stake");
    public static final com.piotrek.groundworksasphalt.item.RoadStakeItem ROAD_STAKE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            ROAD_STAKE_ITEM_KEY,
            new com.piotrek.groundworksasphalt.item.RoadStakeItem(new Item.Properties().setId(ROAD_STAKE_ITEM_KEY).stacksTo(16))
    );

    // ── Entity Types ──────────────────────────────────────────────────
    public static final ResourceKey<EntityType<?>> CAT_GRADER_KEY = entityKey("cat_grader");
    public static final EntityType<CatGraderEntity> CAT_GRADER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CAT_GRADER_KEY,
            EntityType.Builder.<CatGraderEntity>of(CatGraderEntity::new, MobCategory.MISC)
                    .sized(2.8F, 2.6F)
                    .clientTrackingRange(10)
                    .build(CAT_GRADER_KEY)
    );

    public static final ResourceKey<EntityType<?>> ASPHALT_PAVER_KEY = entityKey("asphalt_paver");
    public static final EntityType<AsphaltPaverEntity> ASPHALT_PAVER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ASPHALT_PAVER_KEY,
            EntityType.Builder.<AsphaltPaverEntity>of(AsphaltPaverEntity::new, MobCategory.MISC)
                    .sized(3.0F, 2.8F)
                    .clientTrackingRange(10)
                    .build(ASPHALT_PAVER_KEY)
    );

    public static final ResourceKey<EntityType<?>> ROAD_ROLLER_KEY = entityKey("road_roller");
    public static final EntityType<RoadRollerEntity> ROAD_ROLLER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ROAD_ROLLER_KEY,
            EntityType.Builder.<RoadRollerEntity>of(RoadRollerEntity::new, MobCategory.MISC)
                    .sized(2.4F, 2.6F)
                    .clientTrackingRange(10)
                    .build(ROAD_ROLLER_KEY)
    );

    public static final ResourceKey<EntityType<?>> ASPHALT_ROLLER_KEY = entityKey("asphalt_roller");
    public static final EntityType<AsphaltRollerEntity> ASPHALT_ROLLER = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ASPHALT_ROLLER_KEY,
            EntityType.Builder.<AsphaltRollerEntity>of(AsphaltRollerEntity::new, MobCategory.MISC)
                    .sized(2.4F, 2.6F)
                    .clientTrackingRange(10)
                    .build(ASPHALT_ROLLER_KEY)
    );

    public static final ResourceKey<EntityType<?>> ROAD_STAKE_KEY = entityKey("road_stake");
    public static final EntityType<com.piotrek.groundworksasphalt.entity.RoadStakeEntity> ROAD_STAKE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ROAD_STAKE_KEY,
            EntityType.Builder.<com.piotrek.groundworksasphalt.entity.RoadStakeEntity>of(com.piotrek.groundworksasphalt.entity.RoadStakeEntity::new, MobCategory.MISC)
                    .sized(0.4F, 1.4F)
                    .clientTrackingRange(10)
                    .build(ROAD_STAKE_KEY)
    );

    // ── Sound Events ──────────────────────────────────────────────────
    public static final SoundEvent ROLLER_VIBRATE = registerSound("roller_vibrate");
    public static final SoundEvent PAVER_SCREED = registerSound("paver_screed");
    public static final SoundEvent GRADER_SCRAPE = registerSound("grader_scrape");
    public static final SoundEvent VEHICLE_HORN = registerSound("vehicle_horn");

    private static SoundEvent registerSound(String name) {
        Identifier id = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    // ── Creative Tabs ─────────────────────────────────────────────────
    public static final ResourceKey<CreativeModeTab> GROUNDWORKS_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("pw_groundworks", "main")
    );

    public static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("tools_and_utilities")
    );

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Peterwolf's Groundworks Asphalt (Fabric 26.3, Java 25)...");

        // 1. Networking C2S
        PayloadTypeRegistry.serverboundPlay().register(
                AsphaltInputPayload.TYPE, AsphaltInputPayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                AsphaltInputPayload.TYPE, (payload, context) -> {
                    context.server().execute(() -> {
                        ServerPlayer player = context.player();
                        if (player.getVehicle() instanceof RoadRollerEntity roller) {
                            roller.setControlInputs(
                                    payload.throttle(), payload.steer(),
                                    payload.toolLift(), payload.toolAux(),
                                    payload.action1(), payload.action2(),
                                    payload.horn()
                            );
                        } else if (player.getVehicle() instanceof AsphaltRollerEntity roller) {
                            roller.setControlInputs(
                                    payload.throttle(), payload.steer(),
                                    payload.toolLift(), payload.toolAux(),
                                    payload.action1(), payload.action2(),
                                    payload.horn()
                            );
                        } else if (player.getVehicle() instanceof AsphaltPaverEntity paver) {
                            paver.setControlInputs(
                                    payload.throttle(), payload.steer(),
                                    payload.toolLift(), payload.toolAux(),
                                    payload.action1(), payload.action2(),
                                    payload.horn()
                            );
                        } else if (player.getVehicle() instanceof CatGraderEntity grader) {
                            grader.setControlInputs(
                                    payload.throttle(), payload.steer(),
                                    payload.toolLift(), payload.toolAux(),
                                    payload.action1(), payload.action2(),
                                    payload.horn(), payload.auxMode()
                            );
                        }

                        if (payload.toggleLights()) {
                            if (player.getVehicle() instanceof RoadRollerEntity roller) {
                                roller.setLightsOn(!roller.isLightsOn());
                            } else if (player.getVehicle() instanceof AsphaltRollerEntity roller) {
                                roller.setLightsOn(!roller.isLightsOn());
                            } else if (player.getVehicle() instanceof AsphaltPaverEntity paver) {
                                paver.setWorkLightsOn(!paver.isWorkLightsOn());
                            } else if (player.getVehicle() instanceof CatGraderEntity grader) {
                                grader.setLightsOn(!grader.isLightsOn());
                            }
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.2F);
                        }
                    });
                }
        );

        // 2. Creative Tab Item Registration
        CreativeModeTabEvents.modifyOutputEvent(GROUNDWORKS_TAB).register(output -> {
            output.accept(CAT_GRADER_ITEM);
            output.accept(ASPHALT_PAVER_ITEM);
            output.accept(ROAD_ROLLER_ITEM);
            output.accept(ASPHALT_ROLLER_ITEM);
            output.accept(ROAD_STAKE_ITEM);
            output.accept(ASPHALT_MIX);
            output.accept(FRESH_ASPHALT_ITEM);
            output.accept(COMPACTED_ASPHALT_ITEM);
            output.accept(ASPHALT_SLAB_ITEM);
            output.accept(ASPHALT_WHITE_LINE_ITEM);
            output.accept(ASPHALT_YELLOW_LINE_ITEM);
        });

        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_TAB).register(output -> {
            output.accept(CAT_GRADER_ITEM);
            output.accept(ASPHALT_PAVER_ITEM);
            output.accept(ROAD_ROLLER_ITEM);
            output.accept(ASPHALT_ROLLER_ITEM);
            output.accept(ROAD_STAKE_ITEM);
            output.accept(ASPHALT_MIX);
            output.accept(FRESH_ASPHALT_ITEM);
            output.accept(COMPACTED_ASPHALT_ITEM);
            output.accept(ASPHALT_SLAB_ITEM);
            output.accept(ASPHALT_WHITE_LINE_ITEM);
            output.accept(ASPHALT_YELLOW_LINE_ITEM);
        });

        // 3. Server Commands
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    CatGraderCommand.register(dispatcher);
                    com.piotrek.groundworksasphalt.command.RoadCommand.register(dispatcher);
                }
        );

        LOGGER.info("Groundworks Asphalt registered {} blocks, {} items, 4 vehicles.", 5, 10);
    }
}
