package com.piotrek.groundworksasphalt.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksasphalt.entity.AsphaltPaverEntity;
import com.piotrek.groundworksasphalt.entity.AsphaltRollerEntity;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import com.piotrek.groundworksasphalt.network.AsphaltInputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.lwjgl.sdl.SDLGamepad;

/**
 * Gathers operator keyboard inputs each client tick and transmits control packets to the server.
 */
public final class AsphaltInputHandler {

    private static float lastThrottle;
    private static float lastSteer;
    private static float lastLift;
    private static float lastAux;
    private static boolean lastHorn;
    private static int keepaliveTicks;

    private AsphaltInputHandler() {}

    public static void clientTick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        Entity vehicle = client.player.getVehicle();
        if (vehicle instanceof RoadRollerEntity || vehicle instanceof AsphaltRollerEntity || vehicle instanceof AsphaltPaverEntity || vehicle instanceof CatGraderEntity) {
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
            boolean horn = (AsphaltKeyBindings.KEY_HORN != null && AsphaltKeyBindings.KEY_HORN.isDown())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_C));
            boolean auxMode = (AsphaltKeyBindings.KEY_BLADE_MODE != null && AsphaltKeyBindings.KEY_BLADE_MODE.consumeClick())
                    || (inGame && InputConstants.isKeyDown(InputConstants.KEY_Z));
            boolean toggleLights = (AsphaltKeyBindings.KEY_LIGHTS != null && AsphaltKeyBindings.KEY_LIGHTS.consumeClick());

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

            // ── GAMEPAD ANALOG INPUT OVERLAY ──
            boolean hasGamepad = GamepadController.poll();
            if (hasGamepad) {
                // Triggers: RT = Forward, LT = Reverse
                float gpThrottle = GamepadController.getTrigger(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER)
                                 - GamepadController.getTrigger(SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER);

                // Left Stick X = Steering
                float gpSteer = GamepadController.getAxis(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX);

                // Left Stick Y fallback for throttle if triggers are neutral
                float leftY = -GamepadController.getAxis(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY);
                if (Math.abs(leftY) > 0.05F && Math.abs(gpThrottle) < 0.05F) {
                    gpThrottle = leftY;
                }

                // Bumpers: LB / RB = Steering
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER)) gpSteer -= 1.0F;
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER)) gpSteer += 1.0F;

                // Right Stick: Y = Lift/Height, X = Aux (Tilt/Angle/Width)
                float gpLift = -GamepadController.getAxis(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY);
                float gpAux = GamepadController.getAxis(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX);

                // D-Pad micro adjustments
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP)) gpLift += 1.0F;
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN)) gpLift -= 1.0F;
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT)) gpAux -= 1.0F;
                if (GamepadController.isButtonDown(SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT)) gpAux += 1.0F;

                // Action buttons: A = Action 1 (e.g. vibration/heater toggle), X = Action 2 (e.g. spray/beacon)
                if (GamepadController.consumeButtonClick(SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH)) {
                    action1 = true;
                    GamepadController.rumble(0.25F, 0.4F, 60);
                }
                if (GamepadController.consumeButtonClick(SDLGamepad.SDL_GAMEPAD_BUTTON_WEST)) {
                    action2 = true;
                    GamepadController.rumble(0.2F, 0.3F, 50);
                }
                if (GamepadController.consumeButtonClick(SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH)) {
                    toggleLights = true;
                    GamepadController.rumble(0.15F, 0.25F, 40);
                }

                // Apply analog axes if active
                if (Math.abs(gpThrottle) > 0.01F) throttle = Math.clamp(gpThrottle, -1.0F, 1.0F);
                if (Math.abs(gpSteer) > 0.01F) steer = Math.clamp(gpSteer, -1.0F, 1.0F);
                if (Math.abs(gpLift) > 0.01F) lift = Math.clamp(gpLift, -1.0F, 1.0F);
                if (Math.abs(gpAux) > 0.01F) aux = Math.clamp(gpAux, -1.0F, 1.0F);
            }

            boolean changed = throttle != lastThrottle
                    || steer != lastSteer
                    || lift != lastLift
                    || aux != lastAux
                    || horn != lastHorn
                    || action1
                    || action2
                    || auxMode
                    || toggleLights;

            if (changed || --keepaliveTicks <= 0) {
                ClientPlayNetworking.send(new AsphaltInputPayload(
                        throttle, steer, lift, aux, action1, action2, horn, auxMode, toggleLights
                ));

                lastThrottle = throttle;
                lastSteer = steer;
                lastLift = lift;
                lastAux = aux;
                lastHorn = horn;
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
