package com.example.vanillavehicles.sound;

import com.example.vanillavehicles.vehicle.VehicleStats;

/**
 * Resolved vanilla sound bank of one vehicle instance. Names are resolved
 * safely at playback time, so unknown names are skipped instead of crashing.
 */
public class SoundProfile {

    public String engine;
    public float enginePitch;
    public String horn;
    public String brake;
    public String crash;
    public String siren;
    public String boost;
    public String whistle;

    public SoundProfile(VehicleStats stats) {
        this.engine = stats.engineSound;
        this.enginePitch = stats.enginePitch;
        this.horn = stats.hornSound;
        this.brake = stats.brakeSound;
        this.crash = stats.crashSound;
        this.siren = stats.sirenSound;
        this.boost = stats.boostSound;
        this.whistle = stats.whistleSound;
    }
}
