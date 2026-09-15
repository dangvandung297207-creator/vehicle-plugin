package com.wavemotorcycle.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/** Component / string helpers. */
public final class TextUtil {

    private static final LegacyComponentSerializer AMPERSAND =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private TextUtil() {
    }

    /** Deserializes a config string that may contain {@code &} color codes into a Component. */
    public static Component color(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return AMPERSAND.deserialize(input);
    }

    public static String plain(String input) {
        return input == null ? "" : input;
    }
}
