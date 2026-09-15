package com.wavemotorcycle.motorcycle;

import java.util.UUID;

/**
 * Persistent state of a single motorcycle.
 *
 * <p>This is the data that survives server restarts (see
 * {@link MotorcyclePersistence}). Transient runtime state (velocity, animation,
 * input) lives on the {@link MotorcycleController}.
 */
public final class Motorcycle {

    private final UUID uuid;
    private UUID owner;
    private String worldName;
    /** Chassis ground position (middle of the wheelbase). */
    private double x;
    private double y;
    private double z;
    /** Heading in degrees, Minecraft convention. */
    private float yaw;
    /** Remaining fuel (only meaningful when the fuel system is enabled). */
    private double fuel;
    /** Integrity 0..100, 100 = brand new. */
    private double health;
    private boolean headlightOn;
    private EngineState engine;
    private long created;

    public Motorcycle(UUID uuid) {
        this.uuid = uuid;
        this.fuel = 100.0;
        this.health = 100.0;
        this.engine = EngineState.OFF;
        this.created = System.currentTimeMillis();
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID owner() {
        return owner;
    }

    public void owner(UUID owner) {
        this.owner = owner;
    }

    public String worldName() {
        return worldName;
    }

    public void worldName(String worldName) {
        this.worldName = worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void position(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float yaw() {
        return yaw;
    }

    public void yaw(float yaw) {
        this.yaw = yaw;
    }

    public double fuel() {
        return fuel;
    }

    public void fuel(double fuel) {
        this.fuel = fuel;
    }

    public double health() {
        return health;
    }

    public void health(double health) {
        this.health = health;
    }

    public boolean headlightOn() {
        return headlightOn;
    }

    public void headlightOn(boolean headlightOn) {
        this.headlightOn = headlightOn;
    }

    public EngineState engine() {
        return engine;
    }

    public void engine(EngineState engine) {
        this.engine = engine;
    }

    public long created() {
        return created;
    }

    public void created(long created) {
        this.created = created;
    }
}
