package com.wave100.client;

import com.wave100.entity.EngineState;
import com.wave100.entity.WaveMotorcycleEntity;
import com.wave100.registry.WaveSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Looping engine sound that follows a motorcycle and shifts pitch with RPM.
 *
 * <p>One instance per running bike; created and culled by
 * {@link WaveSoundManager}. The pitch curve maps idle (~1200 RPM) to ~0.7 and
 * redline (~8000 RPM) to ~1.45, which reads as "small engine revving out".</p>
 */
public class WaveEngineSound extends AbstractTickableSoundInstance {

    private final WaveMotorcycleEntity bike;

    WaveEngineSound(WaveMotorcycleEntity bike, RandomSource random) {
        super(pickEvent(bike), SoundSource.NEUTRAL, random);
        this.bike = bike;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6F;
        this.x = bike.getX();
        this.y = bike.getY();
        this.z = bike.getZ();
    }

    private static SoundEvent pickEvent(WaveMotorcycleEntity bike) {
        // a single looping voice; pitch does the talking
        return WaveSounds.ENGINE_RUNNING.get();
    }

    /** True while the followed bike still exists in the world. */
    public boolean bikeStillValid() {
        return !this.bike.isRemoved() && this.bike.isAlive();
    }

    /** Public way for the manager to end this sound ({@code stop()} is protected). */
    public void halt() {
        this.stop();
    }

    @Override
    public void tick() {
        if (this.bike.isRemoved() || !this.bike.isAlive()) {
            this.stop();
            return;
        }
        EngineState engine = this.bike.getEngineState();
        if (engine != EngineState.IDLE && engine != EngineState.RUNNING && engine != EngineState.STARTING) {
            this.stop();
            return;
        }

        this.x = this.bike.getX();
        this.y = this.bike.getY();
        this.z = this.bike.getZ();

        int rpm = this.bike.getRpm();
        float rpmFrac = Mth.clamp(rpm / 8000F, 0F, 1F);
        this.pitch = 0.55F + rpmFrac * 0.9F;

        // a touch louder with revs; quiet near silence when just idling
        float base = engine == EngineState.IDLE ? 0.42F : 0.55F;
        this.volume = base + rpmFrac * 0.35F;
    }
}
