package com.wave100;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * All tunable gameplay values for the Wave 100 motorcycle.
 *
 * <p>Values are read live from the config file (NeoForge auto-reloads on file
 * change), so {@code /wave reload} or editing the TOML takes effect without a
 * restart.</p>
 */
public final class WaveConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ------------------------------------------------------------------
    // vehicle
    // ------------------------------------------------------------------
    private static final ModConfigSpec.DoubleValue MAX_SPEED = BUILDER
            .comment("Maximum forward speed in blocks per tick (0.85 blocks/tick = 61 km/h).")
            .defineInRange("vehicle.maxSpeed", 0.85, 0.05, 3.0);

    private static final ModConfigSpec.DoubleValue ACCELERATION = BUILDER
            .comment("Base forward acceleration in blocks/tick^2. Strong at low speed, weaker near max speed.")
            .defineInRange("vehicle.acceleration", 0.028, 0.001, 1.0);

    private static final ModConfigSpec.DoubleValue BRAKING = BUILDER
            .comment("Deceleration while braking, blocks/tick^2. Responsive but not instantaneous.")
            .defineInRange("vehicle.braking", 0.055, 0.005, 1.0);

    private static final ModConfigSpec.DoubleValue FRICTION = BUILDER
            .comment("Rolling friction: fraction of speed lost per tick when the throttle is released.")
            .defineInRange("vehicle.friction", 0.018, 0.0, 0.5);

    private static final ModConfigSpec.DoubleValue REVERSE_SPEED = BUILDER
            .comment("Maximum reverse speed in blocks/tick.")
            .defineInRange("vehicle.reverseSpeed", 0.15, 0.01, 1.0);

    private static final ModConfigSpec.DoubleValue STEERING_ANGLE = BUILDER
            .comment("Maximum steering angle in degrees (handlebar travel).")
            .defineInRange("vehicle.steeringAngle", 28.0, 5.0, 60.0);

    private static final ModConfigSpec.DoubleValue STEERING_RESPONSE = BUILDER
            .comment("How fast the handlebars move between angles, degrees per tick.")
            .defineInRange("vehicle.steeringResponse", 7.0, 1.0, 30.0);

    private static final ModConfigSpec.DoubleValue HIGH_SPEED_STEER_FACTOR = BUILDER
            .comment("Steering effectiveness multiplier at maximum speed (1.0 = full, lower = stable at speed).")
            .defineInRange("vehicle.highSpeedSteerFactor", 0.35, 0.1, 1.0);

    private static final ModConfigSpec.DoubleValue LEAN_ANGLE = BUILDER
            .comment("Maximum body lean into corners, degrees.")
            .defineInRange("vehicle.leanAngle", 28.0, 0.0, 45.0);

    private static final ModConfigSpec.DoubleValue GRAVITY = BUILDER
            .comment("Gravity applied while airborne, blocks/tick^2 (vanilla entities use 0.04).")
            .defineInRange("vehicle.gravity", 0.055, 0.01, 0.2);

    private static final ModConfigSpec.DoubleValue STEP_HEIGHT = BUILDER
            .comment("Maximum step height the motorcycle can climb (0.5 = slabs, stairs).")
            .defineInRange("vehicle.stepHeight", 0.55, 0.0, 1.0);

    private static final ModConfigSpec.DoubleValue AIR_CONTROL = BUILDER
            .comment("Steering effectiveness while airborne (0 = none).")
            .defineInRange("vehicle.airControl", 0.25, 0.0, 1.0);

    // ------------------------------------------------------------------
    // engine
    // ------------------------------------------------------------------
    private static final ModConfigSpec.IntValue IDLE_RPM = BUILDER
            .comment("Engine idle RPM.")
            .defineInRange("engine.idleRpm", 1200, 500, 4000);

    private static final ModConfigSpec.IntValue MAX_RPM = BUILDER
            .comment("Maximum engine RPM.")
            .defineInRange("engine.maxRpm", 8000, 3000, 16000);

    private static final ModConfigSpec.IntValue START_TICKS = BUILDER
            .comment("Ticks the starter motor cranks before the engine catches.")
            .defineInRange("engine.startTicks", 14, 1, 100);

    private static final ModConfigSpec.IntValue STOP_TICKS = BUILDER
            .comment("Ticks the engine takes to spin down when switched off.")
            .defineInRange("engine.stopTicks", 8, 1, 100);

    private static final ModConfigSpec.BooleanValue AUTO_START = BUILDER
            .comment("Automatically start the engine when the owner mounts.")
            .define("engine.autoStart", false);

    // ------------------------------------------------------------------
    // wheelie
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue WHEELIE_ENABLED = BUILDER
            .comment("Enable SPACE wheelies.")
            .define("wheelie.enabled", true);

    private static final ModConfigSpec.DoubleValue WHEELIE_MAX_ANGLE = BUILDER
            .comment("Maximum safe wheelie angle in degrees. Beyond this the rider loses control.")
            .defineInRange("wheelie.maxAngle", 30.0, 10.0, 60.0);

    private static final ModConfigSpec.DoubleValue WHEELIE_LIFT_FORCE = BUILDER
            .comment("Angular acceleration while lifting the front wheel, degrees/tick^2.")
            .defineInRange("wheelie.liftForce", 0.30, 0.01, 2.0);

    private static final ModConfigSpec.DoubleValue WHEELIE_BALANCE_FORCE = BUILDER
            .comment("Self-balancing force while SPACE is held, degrees/tick^2 per degree of error.")
            .defineInRange("wheelie.balanceForce", 0.055, 0.005, 0.5);

    private static final ModConfigSpec.DoubleValue WHEELIE_GRAVITY = BUILDER
            .comment("How hard the front wheel falls when SPACE is released, degrees/tick^2.")
            .defineInRange("wheelie.gravityTorque", 0.16, 0.01, 1.0);

    private static final ModConfigSpec.BooleanValue WHEELIE_AUTO_RECOVER = BUILDER
            .comment("Automatically stand the motorcycle back up after a crash.")
            .define("wheelie.autoRecover", true);

    private static final ModConfigSpec.IntValue CRASH_RECOVER_TICKS = BUILDER
            .comment("Ticks a crashed motorcycle stays down before auto recovery.")
            .defineInRange("wheelie.crashRecoverTicks", 80, 20, 600);

    // ------------------------------------------------------------------
    // fuel
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue FUEL_ENABLED = BUILDER
            .comment("Enable the fuel system.")
            .define("fuel.enabled", true);

    private static final ModConfigSpec.DoubleValue FUEL_CAPACITY = BUILDER
            .comment("Fuel tank capacity.")
            .defineInRange("fuel.capacity", 100.0, 1.0, 10000.0);

    private static final ModConfigSpec.DoubleValue FUEL_CONSUMPTION = BUILDER
            .comment("Fuel consumed per tick at full throttle (0.01 ≈ 8 minutes of hard riding per tank).")
            .defineInRange("fuel.consumption", 0.010, 0.0, 10.0);

    private static final ModConfigSpec.DoubleValue FUEL_PER_COAL = BUILDER
            .comment("Fuel added per coal/charcoal used with sneak + right click.")
            .defineInRange("fuel.fuelPerCoal", 25.0, 1.0, 1000.0);

    private static final ModConfigSpec.DoubleValue REPAIR_PER_IRON = BUILDER
            .comment("Durability repaired per iron ingot used with sneak + right click.")
            .defineInRange("fuel.repairPerIron", 15.0, 1.0, 1000.0);

    // ------------------------------------------------------------------
    // damage
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue DAMAGE_ENABLED = BUILDER
            .comment("Enable the durability system.")
            .define("damage.enabled", true);

    private static final ModConfigSpec.BooleanValue COLLISION_DAMAGE = BUILDER
            .comment("Take damage from high speed collisions.")
            .define("damage.collisionDamage", true);

    private static final ModConfigSpec.BooleanValue FALL_DAMAGE = BUILDER
            .comment("Take damage from hard landings.")
            .define("damage.fallDamage", true);

    private static final ModConfigSpec.DoubleValue MAX_HEALTH = BUILDER
            .comment("Maximum durability of the motorcycle.")
            .defineInRange("damage.maxHealth", 100.0, 1.0, 1000.0);

    private static final ModConfigSpec.DoubleValue SAFE_FALL_DISTANCE = BUILDER
            .comment("Fall distance (blocks) below which landings are safe.")
            .defineInRange("damage.safeFallDistance", 5.0, 0.0, 50.0);

    private static final ModConfigSpec.DoubleValue COLLISION_MIN_SPEED = BUILDER
            .comment("Impact speed (blocks/tick) above which collisions damage the motorcycle.")
            .defineInRange("damage.collisionMinSpeed", 0.32, 0.05, 3.0);

    private static final ModConfigSpec.BooleanValue DROP_ITEM_ON_DESTROY = BUILDER
            .comment("Drop the motorcycle item when the vehicle is destroyed.")
            .define("damage.dropItemOnDestroy", true);

    // ------------------------------------------------------------------
    // lights
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue HEADLIGHT = BUILDER
            .comment("Enable the toggleable headlight (F key).")
            .define("lights.headlight", true);

    private static final ModConfigSpec.BooleanValue BRAKE_LIGHT = BUILDER
            .comment("Enable the brake light.")
            .define("lights.brakeLight", true);

    private static final ModConfigSpec.BooleanValue HEADLIGHT_BEAM = BUILDER
            .comment("Render a visible headlight beam cone while the light is on.")
            .define("lights.headlightBeam", true);

    // ------------------------------------------------------------------
    // sound
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue SOUND_ENABLED = BUILDER
            .comment("Master switch for all motorcycle sounds.")
            .define("sound.enabled", true);

    private static final ModConfigSpec.DoubleValue SOUND_VOLUME = BUILDER
            .comment("Global volume multiplier for motorcycle sounds.")
            .defineInRange("sound.volume", 1.0, 0.0, 4.0);

    // ------------------------------------------------------------------
    // ownership
    // ------------------------------------------------------------------
    private static final ModConfigSpec.BooleanValue OWNERSHIP_ENABLED = BUILDER
            .comment("Track who placed the motorcycle.")
            .define("ownership.enabled", true);

    private static final ModConfigSpec.BooleanValue OWNERSHIP_LOCKABLE = BUILDER
            .comment("Allow owners to lock their motorcycle with /wave lock.")
            .define("ownership.lockable", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WaveConfig() {
    }

    // ------------------------------------------------------------------
    // accessors (grouped)
    // ------------------------------------------------------------------
    public static final class Vehicle {
        public static double maxSpeed()        { return MAX_SPEED.get(); }
        public static double acceleration()    { return ACCELERATION.get(); }
        public static double braking()         { return BRAKING.get(); }
        public static double friction()        { return FRICTION.get(); }
        public static double reverseSpeed()    { return REVERSE_SPEED.get(); }
        public static double steeringAngle()   { return STEERING_ANGLE.get(); }
        public static double steeringResponse(){ return STEERING_RESPONSE.get(); }
        public static double highSpeedSteer()  { return HIGH_SPEED_STEER_FACTOR.get(); }
        public static double leanAngle()       { return LEAN_ANGLE.get(); }
        public static double gravity()         { return GRAVITY.get(); }
        public static double stepHeight()      { return STEP_HEIGHT.get(); }
        public static double airControl()      { return AIR_CONTROL.get(); }

        private Vehicle() {
        }
    }

    public static final class Engine {
        public static int idleRpm()   { return IDLE_RPM.get(); }
        public static int maxRpm()    { return MAX_RPM.get(); }
        public static int startTicks(){ return START_TICKS.get(); }
        public static int stopTicks() { return STOP_TICKS.get(); }
        public static boolean autoStart() { return AUTO_START.get(); }

        private Engine() {
        }
    }

    public static final class Wheelie {
        public static boolean enabled()     { return WHEELIE_ENABLED.get(); }
        public static double maxAngle()     { return WHEELIE_MAX_ANGLE.get(); }
        public static double liftForce()    { return WHEELIE_LIFT_FORCE.get(); }
        public static double balanceForce() { return WHEELIE_BALANCE_FORCE.get(); }
        public static double gravityTorque(){ return WHEELIE_GRAVITY.get(); }
        public static boolean autoRecover() { return WHEELIE_AUTO_RECOVER.get(); }
        public static int recoverTicks()    { return CRASH_RECOVER_TICKS.get(); }

        private Wheelie() {
        }
    }

    public static final class Fuel {
        public static boolean enabled()    { return FUEL_ENABLED.get(); }
        public static double capacity()    { return FUEL_CAPACITY.get(); }
        public static double consumption() { return FUEL_CONSUMPTION.get(); }
        public static double perCoal()     { return FUEL_PER_COAL.get(); }
        public static double repairPerIron(){ return REPAIR_PER_IRON.get(); }

        private Fuel() {
        }
    }

    public static final class Damage {
        public static boolean enabled()        { return DAMAGE_ENABLED.get(); }
        public static boolean collisions()     { return COLLISION_DAMAGE.get(); }
        public static boolean falls()          { return FALL_DAMAGE.get(); }
        public static double maxHealth()       { return MAX_HEALTH.get(); }
        public static double safeFall()        { return SAFE_FALL_DISTANCE.get(); }
        public static double collisionMinSpeed(){ return COLLISION_MIN_SPEED.get(); }
        public static boolean dropItem()       { return DROP_ITEM_ON_DESTROY.get(); }

        private Damage() {
        }
    }

    public static final class Lights {
        public static boolean headlight()  { return HEADLIGHT.get(); }
        public static boolean brakeLight() { return BRAKE_LIGHT.get(); }
        public static boolean beam()       { return HEADLIGHT_BEAM.get(); }

        private Lights() {
        }
    }

    public static final class Sound {
        public static boolean enabled() { return SOUND_ENABLED.get(); }
        public static double volume()   { return SOUND_VOLUME.get(); }

        private Sound() {
        }
    }

    public static final class Ownership {
        public static boolean enabled()  { return OWNERSHIP_ENABLED.get(); }
        public static boolean lockable() { return OWNERSHIP_LOCKABLE.get(); }

        private Ownership() {
        }
    }
}
