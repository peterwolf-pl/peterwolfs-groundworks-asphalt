package com.piotrek.groundworksasphalt.client.input;

import org.lwjgl.sdl.SDLGamepad;
import java.nio.IntBuffer;

/**
 * Native Xbox / SDL3 Gamepad input controller for Peterwolf's Groundworks machinery.
 *
 * <p>Uses Minecraft 26.3's native SDL3 subsystem (LWJGL-SDL) for zero-dependency,
 * high-precision analog control, haptic rumble, and automatic device detection.
 */
public final class GamepadController {

    private static final float DEFAULT_DEADZONE = 0.12F;
    private static long activeGamepad = 0L;
    private static int activeInstanceId = -1;
    private static String activeGamepadName = "";

    private static final boolean[] PREV_BUTTONS = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT];
    private static final boolean[] CURR_BUTTONS = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT];
    private static final boolean[] JUST_PRESSED = new boolean[SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT];

    private GamepadController() {}

    /**
     * Polls the active gamepad. Automatically detects and opens connected gamepads.
     *
     * @return true if a valid gamepad is connected and active.
     */
    public static boolean poll() {
        if (!ensureGamepadOpen()) {
            clearState();
            return false;
        }

        SDLGamepad.SDL_UpdateGamepads();

        // Check if gamepad was disconnected
        if (!SDLGamepad.SDL_GamepadConnected(activeGamepad)) {
            closeActiveGamepad();
            clearState();
            return false;
        }

        // Poll button states and detect rising edges (clicks)
        for (int i = 0; i < SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT; i++) {
            boolean down = SDLGamepad.SDL_GetGamepadButton(activeGamepad, i);
            CURR_BUTTONS[i] = down;
            JUST_PRESSED[i] = down && !PREV_BUTTONS[i];
            PREV_BUTTONS[i] = down;
        }

        return true;
    }

    public static boolean isConnected() {
        return activeGamepad != 0L;
    }

    public static String getGamepadName() {
        return activeGamepadName;
    }

    private static boolean ensureGamepadOpen() {
        if (activeGamepad != 0L && SDLGamepad.SDL_GamepadConnected(activeGamepad)) {
            return true;
        }

        if (!SDLGamepad.SDL_HasGamepad()) {
            return false;
        }

        IntBuffer gamepads = SDLGamepad.SDL_GetGamepads();
        if (gamepads == null || gamepads.remaining() == 0) {
            return false;
        }

        int instanceId = gamepads.get(0);
        long handle = SDLGamepad.SDL_OpenGamepad(instanceId);
        if (handle != 0L) {
            activeGamepad = handle;
            activeInstanceId = instanceId;
            String name = SDLGamepad.SDL_GetGamepadName(handle);
            activeGamepadName = (name != null && !name.isBlank()) ? name : "Xbox / SDL Gamepad";
            return true;
        }

        return false;
    }

    private static void closeActiveGamepad() {
        if (activeGamepad != 0L) {
            SDLGamepad.SDL_CloseGamepad(activeGamepad);
            activeGamepad = 0L;
            activeInstanceId = -1;
            activeGamepadName = "";
        }
    }

    private static void clearState() {
        for (int i = 0; i < SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT; i++) {
            PREV_BUTTONS[i] = false;
            CURR_BUTTONS[i] = false;
            JUST_PRESSED[i] = false;
        }
    }

    /**
     * Reads an analog axis [-1.0 .. +1.0] with deadzone filtering and progressive response curve.
     * In SDL3, axis range is -32768 to 32767.
     */
    public static float getAxis(int axisId) {
        return getAxis(axisId, DEFAULT_DEADZONE);
    }

    public static float getAxis(int axisId, float deadzone) {
        if (activeGamepad == 0L) return 0.0F;

        short rawShort = SDLGamepad.SDL_GetGamepadAxis(activeGamepad, axisId);
        float raw = rawShort < 0 ? (rawShort / 32768.0F) : (rawShort / 32767.0F);

        if (Math.abs(raw) <= deadzone) {
            return 0.0F;
        }

        float sign = Math.signum(raw);
        float normalized = (Math.abs(raw) - deadzone) / (1.0F - deadzone);
        // Blended linear (50%) + quadratic (50%) curve for precision near center and power at limits
        return sign * (0.5F * normalized + 0.5F * normalized * normalized);
    }

    /**
     * Reads an analog trigger [0.0 .. 1.0].
     * In SDL3, triggers return 0 to 32767.
     */
    public static float getTrigger(int triggerAxisId) {
        if (activeGamepad == 0L) return 0.0F;

        short rawShort = SDLGamepad.SDL_GetGamepadAxis(activeGamepad, triggerAxisId);
        if (rawShort <= 0) return 0.0F;

        float normalized = rawShort / 32767.0F;
        if (normalized <= DEFAULT_DEADZONE) {
            return 0.0F;
        }
        return (normalized - DEFAULT_DEADZONE) / (1.0F - DEFAULT_DEADZONE);
    }

    /**
     * Checks if a button is currently held down.
     */
    public static boolean isButtonDown(int buttonId) {
        if (buttonId < 0 || buttonId >= SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT) return false;
        return CURR_BUTTONS[buttonId];
    }

    /**
     * Checks if a button was just clicked on this tick (rising edge).
     */
    public static boolean consumeButtonClick(int buttonId) {
        if (buttonId < 0 || buttonId >= SDLGamepad.SDL_GAMEPAD_BUTTON_COUNT) return false;
        boolean pressed = JUST_PRESSED[buttonId];
        JUST_PRESSED[buttonId] = false;
        return pressed;
    }

    /**
     * Triggers haptic vibration / rumble on the gamepad.
     *
     * @param lowFreqRumble Intensity of low frequency motor (0.0 to 1.0)
     * @param highFreqRumble Intensity of high frequency motor (0.0 to 1.0)
     * @param durationMs Duration in milliseconds
     */
    public static boolean rumble(float lowFreqRumble, float highFreqRumble, int durationMs) {
        if (activeGamepad == 0L) return false;
        short low = (short) (Math.clamp(lowFreqRumble, 0.0F, 1.0F) * 65535);
        short high = (short) (Math.clamp(highFreqRumble, 0.0F, 1.0F) * 65535);
        return SDLGamepad.SDL_RumbleGamepad(activeGamepad, low, high, durationMs);
    }
}
