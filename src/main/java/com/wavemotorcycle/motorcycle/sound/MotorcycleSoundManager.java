package com.wavemotorcycle.motorcycle.sound;

import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.motorcycle.EngineState;
import java.util.Locale;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.location.Location;

/**
 * Throttled sound playback for one motorcycle.
 *
 * <p>Engine sound is a short pulse played every few ticks (never every tick) with
 * pitch scaled to speed, giving the familiar idle/rev feel. All sounds are
 * configurable via resource-location strings in config.yml; unknown values fall
 * back to sensible vanilla defaults.
 */
public final class MotorcycleSoundManager {

    private final ConfigManager cfg;

    // Resolved sounds (lazy, cached).
    private boolean resolved;
    private Sound engineIdle;
    private Sound engineLow;
    private Sound engineHigh;
    private Sound engineStart;
    private Sound engineStop;
    private Sound brake;
    private Sound collision;
    private Sound wheelie;
    private Sound lightToggle;
    private Sound landing;
    private Sound destroyed;
    private Sound refuel;

    public MotorcycleSoundManager(ConfigManager cfg) {
        this.cfg = cfg;
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        engineIdle = resolve(cfg.soundEngineIdle, Sound.BLOCK_NOTE_BLOCK_BASS);
        engineLow = resolve(cfg.soundEngineLow, Sound.BLOCK_NOTE_BLOCK_BASS);
        engineHigh = resolve(cfg.soundEngineHigh, Sound.BLOCK_NOTE_BLOCK_BASS);
        engineStart = resolve(cfg.soundEngineStart, Sound.BLOCK_LAVA_EXTINGUISH);
        engineStop = resolve(cfg.soundEngineStop, Sound.BLOCK_FIRE_EXTINGUISH);
        brake = resolve(cfg.soundBrake, Sound.BLOCK_STONE_HIT);
        collision = resolve(cfg.soundCollision, Sound.BLOCK_STONE_HIT);
        wheelie = resolve(cfg.soundWheelie, Sound.BLOCK_NOTE_BLOCK_HAT);
        lightToggle = resolve(cfg.soundLight, Sound.BLOCK_LEVER_CLICK);
        landing = resolve(cfg.soundLanding, Sound.ENTITY_ARMOR_STAND_FALL);
        destroyed = resolve(cfg.soundDestroyed, Sound.ENTITY_TNT_PRIMED);
        refuel = resolve(cfg.soundRefuel, Sound.ENTITY_COW_MILK);
    }

    private static Sound resolve(String configured, Sound fallback) {
        if (configured == null || configured.isEmpty()) {
            return fallback;
        }
        String raw = configured.trim();
        String ns = "minecraft";
        String name = raw;
        int idx = raw.indexOf(':');
        if (idx >= 0) {
            ns = raw.substring(0, idx);
            name = raw.substring(idx + 1);
        }
        if (!name.contains(".")) {
            name = name.toLowerCase(Locale.ROOT).replace('_', '.');
        }
        try {
            Sound s = Registry.SOUNDS.get(Key.key(ns, name));
            return s != null ? s : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Per-tick engine pulse (throttled). */
    public void tick(MotorcycleController c) {
        if (c.engineState() != EngineState.RUNNING) {
            return;
        }
        resolve();
        if (c.tickCount() % cfg.engineSoundInterval != 0) {
            return;
        }
        double maxSpeed = Math.max(0.001, c.maxSpeed());
        double frac = Math.min(1.0, Math.abs(c.speed()) / maxSpeed);
        Sound sound = frac < 0.15 ? engineIdle : frac < 0.5 ? engineLow : engineHigh;
        float pitch = (float) (0.5 + 0.85 * frac + (c.isThrottling() ? 0.1 : 0.0));
        c.world().playSound(c.location(), sound, 0.45f, pitch);
    }

    public void playEngineStart(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), engineStart, 0.9f, 1.0f);
    }

    public void playEngineStop(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), engineStop, 0.8f, 1.0f);
    }

    public void playBrake(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location().add(0, 0.2, 0), brake, 0.5f, 0.8f);
    }

    public void playCollision(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location().add(0, 0.3, 0), collision, 0.8f, 0.6f);
    }

    public void playWheelie(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), wheelie, 0.6f, 1.2f);
    }

    public void playLightToggle(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), lightToggle, 0.8f, 1.0f);
    }

    public void playLanding(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location().add(0, 0.2, 0), landing, 0.7f, 0.9f);
    }

    public void playDestroyed(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), destroyed, 1.0f, 0.7f);
    }

    public void playRefuel(MotorcycleController c) {
        resolve();
        c.world().playSound(c.location(), refuel, 0.8f, 1.0f);
    }
}
