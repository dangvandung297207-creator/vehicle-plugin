package com.wave100.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Vehicle key bindings. Movement reuses the vanilla movement keys (WASD,
 * SPACE) while riding; only the toggles get their own bindings.
 *
 * <ul>
 *   <li>{@code R} - engine start/stop</li>
 *   <li>{@code F} - headlight on/off (vanilla offhand swap is suppressed
 *       while riding so the key is free)</li>
 * </ul>
 */
public final class WaveKeyBindings {

    public static final String CATEGORY = "key.categories.wave100";

    public static final KeyMapping TOGGLE_ENGINE = new KeyMapping(
            "key.wave100.toggle_engine",
            VehicleKeyConflictContext.INSTANCE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    public static final KeyMapping TOGGLE_HEADLIGHT = new KeyMapping(
            "key.wave100.toggle_headlight",
            VehicleKeyConflictContext.INSTANCE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY);

    private WaveKeyBindings() {
    }
}
