package com.piotrek.groundworksasphalt.client;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.client.input.AsphaltInputHandler;
import com.piotrek.groundworksasphalt.client.input.AsphaltKeyBindings;
import com.piotrek.groundworksasphalt.client.model.AsphaltPaverModel;
import com.piotrek.groundworksasphalt.client.model.AsphaltRollerModel;
import com.piotrek.groundworksasphalt.client.model.CatGraderModel;
import com.piotrek.groundworksasphalt.client.model.RoadRollerModel;
import com.piotrek.groundworksasphalt.client.render.AsphaltHudOverlay;
import com.piotrek.groundworksasphalt.client.render.AsphaltPaverRenderer;
import com.piotrek.groundworksasphalt.client.render.AsphaltRollerRenderer;
import com.piotrek.groundworksasphalt.client.render.CatGraderRenderer;
import com.piotrek.groundworksasphalt.client.render.RoadRollerRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

/**
 * Peterwolf's Groundworks Asphalt client initializer.
 */
public class GroundworksAsphaltClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register entity model layers
        ModelLayerRegistry.registerModelLayer(CatGraderRenderer.CAT_GRADER_LAYER, CatGraderModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(AsphaltPaverRenderer.ASPHALT_PAVER_LAYER, AsphaltPaverModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(RoadRollerRenderer.ROAD_ROLLER_LAYER, RoadRollerModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(AsphaltRollerRenderer.ASPHALT_ROLLER_LAYER, AsphaltRollerModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(com.piotrek.groundworksasphalt.client.render.RoadStakeRenderer.ROAD_STAKE_LAYER, com.piotrek.groundworksasphalt.client.model.RoadStakeModel::createBodyLayer);

        // Register entity renderers
        EntityRendererRegistry.register(GroundworksAsphaltMod.CAT_GRADER, CatGraderRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ASPHALT_PAVER, AsphaltPaverRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ROAD_ROLLER, RoadRollerRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ASPHALT_ROLLER, AsphaltRollerRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ROAD_STAKE, com.piotrek.groundworksasphalt.client.render.RoadStakeRenderer::new);

        // Register in-cab instrument HUD overlay for all machinery
        AsphaltHudOverlay.register();

        // Register custom keybindings
        AsphaltKeyBindings.register();

        // Register tick handler for vehicle controls
        ClientTickEvents.END_CLIENT_TICK.register(AsphaltInputHandler::clientTick);
    }
}
