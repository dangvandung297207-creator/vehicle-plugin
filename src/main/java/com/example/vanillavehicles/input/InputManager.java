package com.example.vanillavehicles.input;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects driver input from vanilla-client-safe sources.
 *
 * <p>Enhanced mode (newer Paper): real W/A/S/D + Space through
 * PlayerInputEvent. Fallback mode (Paper 1.21.1): hotbar slot selects a cruise
 * gear, the mouse steers (driver look yaw vs vehicle heading), sneak brakes,
 * Q exits, F toggles lights, left click honks, right click triggers the
 * vehicle special.</p>
 */
public class InputManager implements Listener {

    /** Hotbar slot -&gt; cruise gear mapping for fallback mode. */
    public static final double[] GEARS = {-0.3, 0.0, 0.15, 0.3, 0.45, 0.6, 0.75, 0.9, 1.0};

    private final VanillaVehicles plugin;
    private final Map<UUID, InputState> states = new ConcurrentHashMap<>();
    private boolean enhancedAvailable;

    public InputManager(VanillaVehicles plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enhancedAvailable = PlayerInputHook.tryRegister(plugin, this::onEnhancedInput);
        PlayerInputHook.tryRegisterJump(plugin, this::onJumpPulse);
        plugin.getLogger().info("Input mode: "
                + (enhancedAvailable ? "enhanced WASD available" : "fallback (gears + mouse steering)"));
    }

    public void shutdown() {
        states.clear();
    }

    public boolean isEnhancedAvailable() {
        return enhancedAvailable;
    }

    public InputState getState(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), key -> new InputState());
    }

    public void clearState(Player player) {
        if (player != null) {
            states.remove(player.getUniqueId());
        }
    }

    private void onEnhancedInput(Player player, boolean forward, boolean backward, boolean left,
                                 boolean right, boolean jump, boolean sneak, boolean sprint) {
        // Only track actual drivers; anything else would leak entries.
        if (!plugin.getVehicleManager().isDriver(player)) {
            return;
        }
        InputState state = getState(player);
        state.enhanced = true;
        state.lastEnhancedInput = System.currentTimeMillis();
        state.forward = (forward ? 1.0 : 0.0) - (backward ? 1.0 : 0.0);
        state.strafe = (right ? 1.0 : 0.0) - (left ? 1.0 : 0.0);
        state.jump = jump;
        state.sneak = sneak;
        state.sprint = sprint;
    }

    /** Tracks driver look direction (mouse steering / aircraft pitch). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.getVehicleManager().isDriver(player)) {
            return;
        }
        InputState state = getState(player);
        state.lookYaw = to.getYaw();
        state.lookPitch = to.getPitch();
        state.hasLook = true;
    }

    /** Sneak hold = brake in fallback mode (dismount is cancelled while moving). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVehicleManager().isDriver(player)) {
            return;
        }
        getState(player).brake = event.isSneaking();
    }

    /** Hotbar slot = cruise gear in fallback mode. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVehicleManager().isDriver(player)) {
            return;
        }
        InputState state = getState(player);
        if (state.enhanced && System.currentTimeMillis() - state.lastEnhancedInput < 1500) {
            return;
        }
        int slot = Math.max(0, Math.min(GEARS.length - 1, event.getNewSlot()));
        state.gear = GEARS[slot];
    }

    /** Q = exit vehicle. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle != null) {
            event.setCancelled(true);
            vehicle.exit(player);
        }
    }

    /** F = toggle headlights. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle != null && vehicle.isDriver(player)) {
            event.setCancelled(true);
            vehicle.toggleLights();
        }
    }

    /** Left click = horn, right click = vehicle special (siren, boost, tools...). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Vehicle vehicle = plugin.getVehicleManager().getVehicleOf(player);
        if (vehicle == null || !vehicle.isDriver(player)) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            vehicle.horn();
            event.setCancelled(true);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            vehicle.special();
            event.setCancelled(true);
        }
    }

    /** Bonus Space detection where the server fires it while riding. */
    private void onJumpPulse(Player player) {
        if (!plugin.getVehicleManager().isDriver(player)) {
            return;
        }
        getState(player).jumpPulse = true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        clearState(event.getPlayer());
    }
}
