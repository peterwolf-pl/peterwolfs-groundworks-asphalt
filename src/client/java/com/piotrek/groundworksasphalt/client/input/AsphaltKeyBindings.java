package com.piotrek.groundworksasphalt.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * Keybindings for asphalt vehicle attachments and operations.
 */
public final class AsphaltKeyBindings {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(GroundworksAsphaltMod.id("controls"));

    public static KeyMapping KEY_TOOL_LIFT_UP;
    public static KeyMapping KEY_TOOL_LIFT_DOWN;
    public static KeyMapping KEY_TOOL_AUX_LEFT;
    public static KeyMapping KEY_TOOL_AUX_RIGHT;
    public static KeyMapping KEY_ACTION_1;
    public static KeyMapping KEY_ACTION_2;

    private AsphaltKeyBindings() {}

    public static void register() {
        KEY_TOOL_LIFT_UP = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.tool_lift_up",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_UP,
                CATEGORY
        ));

        KEY_TOOL_LIFT_DOWN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.tool_lift_down",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_DOWN,
                CATEGORY
        ));

        KEY_TOOL_AUX_LEFT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.tool_aux_left",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_LEFT,
                CATEGORY
        ));

        KEY_TOOL_AUX_RIGHT = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.tool_aux_right",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RIGHT,
                CATEGORY
        ));

        KEY_ACTION_1 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.action_1",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_V,
                CATEGORY
        ));

        KEY_ACTION_2 = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.pw_groundworks_asphalt.action_2",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_B,
                CATEGORY
        ));
    }
}
