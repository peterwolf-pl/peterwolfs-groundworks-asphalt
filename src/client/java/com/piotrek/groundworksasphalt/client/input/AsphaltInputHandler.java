package com.piotrek.groundworksasphalt.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksasphalt.entity.AsphaltPaverEntity;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.network.AsphaltInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Gathers operator keyboard inputs each client tick and transmits control packets to the server.
 */
public final class AsphaltInputHandler {

    private static float lastThrottle;
    private static float lastSteer;
    private static float lastLift;
    private static float lastAux;
    private static int keepaliveTicks;

    private AsphaltInputHandler() {}

    public static void clientTick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        Entity vehicle = client.player.getVehicle();
        if (vehicle instanceof RoadRollerEntity || vehicle instanceof AsphaltPaverEntity || vehicle instanceof CatGraderEntity) {
            boolean inGame = client.mouseHandler != null && client.mouseHandler.isMouseGrabbed();

            boolean keyForward = (client.options.keyUp != null && client.options.keyUp.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.forward())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_W));
            boolean keyBackward = (client.options.keyDown != null && client.options.keyDown.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.backward())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_S));
            boolean keyLeft = (client.options.keyLeft != null && client.options.keyLeft.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.left())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_A));
            boolean keyRight = (client.options.keyRight != null && client.options.keyRight.isDown())
                    || (client.player.input != null && client.player.input.keyPresses.right())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_D));

            boolean liftUp = (AsphaltKeyBindings.KEY_TOOL_LIFT_UP != null && AsphaltKeyBindings.KEY_TOOL_LIFT_UP.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_UP));
            boolean liftDown = (AsphaltKeyBindings.KEY_TOOL_LIFT_DOWN != null && AsphaltKeyBindings.KEY_TOOL_LIFT_DOWN.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_DOWN));

            boolean auxLeft = (AsphaltKeyBindings.KEY_TOOL_AUX_LEFT != null && AsphaltKeyBindings.KEY_TOOL_AUX_LEFT.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_LEFT));
            boolean auxRight = (AsphaltKeyBindings.KEY_TOOL_AUX_RIGHT != null && AsphaltKeyBindings.KEY_TOOL_AUX_RIGHT.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_RIGHT));

            boolean action1 = AsphaltKeyBindings.KEY_ACTION_1 != null && AsphaltKeyBindings.KEY_ACTION_1.consumeClick();
            boolean action2 = AsphaltKeyBindings.KEY_ACTION_2 != null && AsphaltKeyBindings.KEY_ACTION_2.consumeClick();

            float throttle = 0.0F;
            float steer = 0.0F;
            float lift = 0.0F;
            float aux = 0.0F;

            if (keyForward) throttle += 1.0F;
            if (keyBackward) throttle -= 1.0F;
            if (keyLeft) steer -= 1.0F;
            if (keyRight) steer += 1.0F;

            if (liftUp) lift += 1.0F;
            if (liftDown) lift -= 1.0F;

            if (auxLeft) aux -= 1.0F;
            if (auxRight) aux += 1.0F;

            boolean changed = throttle != lastThrottle
                    || steer != lastSteer
                    || lift != lastLift
                    || aux != lastAux
                    || action1
                    || action2;

            if (changed || --keepaliveTicks <= 0) {
                ClientPlayNetworking.send(new AsphaltInputPayload(
                        throttle, steer, lift, aux, action1, action2
                ));

                lastThrottle = throttle;
                lastSteer = steer;
                lastLift = lift;
                lastAux = aux;
                keepaliveTicks = 4;
            }
        } else {
            lastThrottle = 0.0F;
            lastSteer = 0.0F;
            lastLift = 0.0F;
            lastAux = 0.0F;
            keepaliveTicks = 0;
        }
    }
}
