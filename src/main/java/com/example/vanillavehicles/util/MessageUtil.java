package com.example.vanillavehicles.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Small chat formatting helper. */
public final class MessageUtil {

    private MessageUtil() {
    }

    public static final String PREFIX = ChatColor.GOLD + "[Vehicles] " + ChatColor.GRAY;

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender target, String message) {
        if (target != null) {
            target.sendMessage(PREFIX + color(message));
        }
    }

    public static void sendRaw(CommandSender target, String message) {
        if (target != null) {
            target.sendMessage(color(message));
        }
    }
}
