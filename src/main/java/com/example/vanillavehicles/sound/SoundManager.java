package com.example.vanillavehicles.sound;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.util.SoundUtil;
import com.example.vanillavehicles.vehicle.Vehicle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttled vehicle audio: engine loops whose pitch follows speed, siren
 * wails, horns, crashes and UI clicks. Everything uses vanilla sounds.
 */
public class SoundManager {

    private final VanillaVehicles plugin;
    private final Map<UUID, Long> engineAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sirenAt = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> sirenPhase = new ConcurrentHashMap<>();

    public SoundManager(VanillaVehicles plugin) {
        this.plugin = plugin;
    }

    public void forget(Vehicle vehicle) {
        UUID id = vehicle.getId();
        engineAt.remove(id);
        sirenAt.remove(id);
        sirenPhase.remove(id);
    }

    /** Called every tick per active vehicle; internally throttled. */
    public void update(Vehicle vehicle, Player driver) {
        long now = plugin.getVehicleManager().getTick();
        if (driver != null) {
            long last = engineAt.getOrDefault(vehicle.getId(), -100L);
            int interval = engineInterval(vehicle);
            if (now - last >= interval) {
                engineAt.put(vehicle.getId(), now);
                playEngine(vehicle);
            }
        }
        if (vehicle.isSirenOn() && vehicle.getStats().siren) {
            long last = sirenAt.getOrDefault(vehicle.getId(), -100L);
            if (now - last >= 9) {
                sirenAt.put(vehicle.getId(), now);
                boolean phase = !sirenPhase.getOrDefault(vehicle.getId(), false);
                sirenPhase.put(vehicle.getId(), phase);
                SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().siren,
                        vehicle.getLocation(), 1.2f, phase ? 0.65f : 1.15f);
            }
        }
    }

    private int engineInterval(Vehicle vehicle) {
        switch (vehicle.getType().getPhysics()) {
            case AIRCRAFT:
                return 4;
            case BOAT:
                return 6;
            case TRAIN:
                return 7;
            default:
                return 5;
        }
    }

    private void playEngine(Vehicle vehicle) {
        double max = Math.max(1.0, vehicle.getStats().maxSpeed);
        double ratio = Math.min(1.0, Math.abs(vehicle.getSpeed()) / max);
        float pitch = (float) (vehicle.getSoundProfile().enginePitch * (0.75 + ratio * 0.7));
        if (vehicle.isBoosting()) {
            pitch *= 1.25f;
        }
        float volume = (float) (0.55 + ratio * 0.65);
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().engine,
                vehicle.getLocation(), volume, pitch);
    }

    public void horn(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().horn,
                vehicle.getLocation(), 1.4f, 1.0f);
    }

    public void whistle(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().whistle,
                vehicle.getLocation(), 1.4f, 0.9f);
    }

    public void brake(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().brake,
                vehicle.getLocation(), 0.7f, 1.3f);
    }

    public void crash(Vehicle vehicle, double impact) {
        float volume = (float) Math.min(1.5, 0.5 + impact / 12.0);
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().crash,
                vehicle.getLocation(), volume, 0.9f);
        if (impact > 12) {
            SoundUtil.play(vehicle.getWorld(), "ENTITY_GENERIC_EXPLODE",
                    vehicle.getLocation(), 0.6f, 1.4f);
        }
    }

    public void boost(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), vehicle.getSoundProfile().boost,
                vehicle.getLocation(), 1.4f, 1.0f);
    }

    public void destroy(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), "ENTITY_GENERIC_EXPLODE",
                vehicle.getLocation(), 1.5f, 0.8f);
        SoundUtil.play(vehicle.getWorld(), "BLOCK_ANVIL_LAND",
                vehicle.getLocation(), 1.0f, 0.6f);
    }

    public void click(Player player) {
        SoundUtil.play(player, "UI_BUTTON_CLICK", 0.8f, 1.0f);
    }

    public void deny(Player player) {
        SoundUtil.play(player, "ENTITY_VILLAGER_NO", 0.9f, 1.0f);
    }

    public void chime(Player player) {
        SoundUtil.play(player, "ENTITY_EXPERIENCE_ORB_PICKUP", 0.9f, 1.2f);
    }

    public void splash(Vehicle vehicle) {
        SoundUtil.play(vehicle.getWorld(), "ENTITY_GENERIC_SPLASH",
                vehicle.getLocation(), 0.8f, 1.0f);
    }
}
