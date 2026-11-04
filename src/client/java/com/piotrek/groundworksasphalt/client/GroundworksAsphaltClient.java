package com.piotrek.groundworksasphalt.client;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.client.input.AsphaltInputHandler;
import com.piotrek.groundworksasphalt.client.input.AsphaltKeyBindings;
import com.piotrek.groundworksasphalt.client.model.AsphaltPaverModel;
import com.piotrek.groundworksasphalt.client.model.CatGraderModel;
import com.piotrek.groundworksasphalt.client.model.RoadRollerModel;
import com.piotrek.groundworksasphalt.client.render.AsphaltPaverRenderer;
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

        // Register entity renderers
        EntityRendererRegistry.register(GroundworksAsphaltMod.CAT_GRADER, CatGraderRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ASPHALT_PAVER, AsphaltPaverRenderer::new);
        EntityRendererRegistry.register(GroundworksAsphaltMod.ROAD_ROLLER, RoadRollerRenderer::new);

        // Register custom keybindings
        AsphaltKeyBindings.register();

        // Register tick handler for vehicle controls
        ClientTickEvents.END_CLIENT_TICK.register(AsphaltInputHandler::clientTick);
    }
}
