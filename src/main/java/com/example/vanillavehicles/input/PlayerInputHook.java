package com.example.vanillavehicles.input;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Optional hook for Paper's PlayerInputEvent (real WASD state while riding).
 *
 * <p>The event does not exist on Paper 1.21.1, so it is wired purely through
 * reflection: on 1.21.1 this hook quietly disables itself and the fallback
 * control scheme is used, while newer servers automatically get full WASD.</p>
 */
public final class PlayerInputHook {

    private PlayerInputHook() {
    }

    public interface InputListener {
        void onInput(Player player, boolean forward, boolean backward, boolean left,
                     boolean right, boolean jump, boolean sneak, boolean sprint);
    }

    /**
     * Tries to register the hook. Returns true when the server provides the
     * event and the hook is active.
     */
    @SuppressWarnings("unchecked")
    public static boolean tryRegister(Plugin plugin, InputListener listener) {
        try {
            Class<?> eventClass = Class.forName("org.bukkit.event.player.PlayerInputEvent");
            if (!Event.class.isAssignableFrom(eventClass)) {
                return false;
            }
            Class<? extends Event> clazz = (Class<? extends Event>) eventClass;
            final Method getPlayer = eventClass.getMethod("getPlayer");
            final Method getInput = eventClass.getMethod("getInput");

            plugin.getServer().getPluginManager().registerEvent(
                    clazz,
                    new Listener() {
                    },
                    EventPriority.MONITOR,
                    (l, event) -> {
                        try {
                            Player player = (Player) getPlayer.invoke(event);
                            Object input = getInput.invoke(event);
                            if (player == null || input == null) {
                                return;
                            }
                            boolean forward = read(input, "isForward");
                            boolean backward = read(input, "isBackward");
                            boolean left = read(input, "isLeft");
                            boolean right = read(input, "isRight");
                            boolean jump = read(input, "isJump");
                            boolean sneak = read(input, "isSneak");
                            boolean sprint = read(input, "isSprint");
                            listener.onInput(player, forward, backward, left, right, jump, sneak, sprint);
                        } catch (Exception ignored) {
                        }
                    },
                    plugin);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean read(Object input, String method) {
        try {
            Method m = input.getClass().getMethod(method);
            Object value = m.invoke(input);
            return value instanceof Boolean && (Boolean) value;
        } catch (Exception ex) {
            return false;
        }
    }
}
