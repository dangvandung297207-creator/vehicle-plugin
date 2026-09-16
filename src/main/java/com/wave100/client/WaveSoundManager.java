package com.wave100.client;

import com.wave100.entity.EngineState;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-side engine sound manager.
 *
 * <p>Keeps exactly one {@link WaveEngineSound} alive per running motorcycle
 * near the player. The scan runs only every 10 ticks (and only when the sound
 * actually needs to start), so the cost is negligible with many bikes around.</p>
 */
@EventBusSubscriber(modid = com.wave100.WaveMod.MODID)
public final class WaveSoundManager {

    private static final double RANGE_SQ = 24.0 * 24.0;
    private static final int RESCAN_INTERVAL = 10;

    private static final Map<Integer, WaveEngineSound> ACTIVE = new HashMap<>();

    private WaveSoundManager() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            stopAll();
            return;
        }

        // cheap per-tick cleanup of finished instances
        if (level.getGameTime() % RESCAN_INTERVAL != 0L) {
            Iterator<Map.Entry<Integer, WaveEngineSound>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                WaveEngineSound sound = it.next().getValue();
                if (sound.isStopped() || !sound.bikeStillValid()) {
                    sound.stop();
                    it.remove();
                }
            }
            return;
        }

        // periodic rescan: start sounds for running bikes, stop the rest
        if (mc.player == null) {
            stopAll();
            return;
        }
        for (Map.Entry<Integer, WaveEngineSound> entry : ACTIVE.entrySet()) {
            WaveEngineSound sound = entry.getValue();
            if (!sound.bikeStillValid()) {
                sound.stop();
            }
        }

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof WaveMotorcycleEntity bike)) {
                continue;
            }
            if (bike.distanceToSqr(mc.player) > RANGE_SQ) {
                continue;
            }
            EngineState engine = bike.getEngineState();
            boolean running = (engine == EngineState.IDLE || engine == EngineState.RUNNING
                    || engine == EngineState.STARTING) && !bike.isCrashed();
            WaveEngineSound existing = ACTIVE.get(bike.getId());
            if (running && existing == null) {
                WaveEngineSound sound = new WaveEngineSound(bike, mc.player.getRandom());
                ACTIVE.put(bike.getId(), sound);
                mc.getSoundManager().play(sound);
            } else if (!running && existing != null) {
                existing.stop();
            }
        }

        ACTIVE.values().removeIf(WaveEngineSound::isStopped);
    }

    private static void stopAll() {
        for (WaveEngineSound sound : ACTIVE.values()) {
            sound.stop();
        }
        ACTIVE.clear();
    }
}
