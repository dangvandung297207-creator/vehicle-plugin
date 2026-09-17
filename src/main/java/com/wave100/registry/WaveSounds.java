package com.wave100.registry;

import com.wave100.WaveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * All sound events used by the Wave 100.
 *
 * <p>The {@code sounds.json} maps every event onto vanilla placeholder audio
 * (using fully qualified names) so the mod never references a missing asset.
 * The structure is ready for real recorded engine samples: drop OGG files into
 * {@code assets/wave100/sounds/} and point the entries at them.</p>
 */
public final class WaveSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, WaveMod.MODID);

    public static final Supplier<SoundEvent> ENGINE_START   = register("wave_engine_start");
    public static final Supplier<SoundEvent> ENGINE_IDLE    = register("wave_engine_idle");
    public static final Supplier<SoundEvent> ENGINE_RUNNING = register("wave_engine_running");
    public static final Supplier<SoundEvent> ENGINE_STOP    = register("wave_engine_stop");
    public static final Supplier<SoundEvent> BRAKE          = register("wave_brake");
    public static final Supplier<SoundEvent> COLLISION      = register("wave_collision");
    public static final Supplier<SoundEvent> CRASH          = register("wave_crash");
    public static final Supplier<SoundEvent> WHEELIE        = register("wave_wheelie");
    public static final Supplier<SoundEvent> LIGHT_ON       = register("wave_light_on");
    public static final Supplier<SoundEvent> LIGHT_OFF      = register("wave_light_off");
    public static final Supplier<SoundEvent> REFUEL         = register("wave_refuel");
    public static final Supplier<SoundEvent> REPAIR         = register("wave_repair");
    public static final Supplier<SoundEvent> DESTROYED      = register("wave_destroyed");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(WaveMod.id(name)));
    }

    private WaveSounds() {
    }
}
