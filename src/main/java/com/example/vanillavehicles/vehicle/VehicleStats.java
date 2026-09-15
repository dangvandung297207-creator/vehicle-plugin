package com.example.vanillavehicles.vehicle;

/**
 * Runtime tuning values for a vehicle type. Starts as a copy of the
 * {@link VehicleType} defaults and is then overlaid with config.yml values
 * by {@link com.example.vanillavehicles.config.PluginConfig}.
 */
public class VehicleStats {

    public boolean enabled = true;
    public String permission = "";

    public double maxSpeed;
    public double acceleration;
    public double brake;
    public double reverseSpeed;
    public double steering;
    public double friction;
    public double maxHealth;
    public double length;
    public double width;
    public double height;
    public double modelScale = 1.0;

    /** Allows rotating in place at (near) zero speed. */
    public boolean turnInPlace = false;
    /** Climbs single-block steps automatically. */
    public boolean stepUp = false;
    /** Extra visual lean in corners (bikes). */
    public double leanFactor = 0.0;
    /** Handbrake drift (kart). */
    public boolean drift = false;
    /** Burnout smoke when flooring it while braking. */
    public boolean burnout = false;

    public boolean boostEnabled = false;
    public double boostMultiplier = 1.5;
    public int boostDurationTicks = 60;
    public int boostCooldownTicks = 200;

    public boolean siren = false;
    public int cargoSize = 0;
    public int carriages = 0;

    public String engineSound = "ENTITY_MINECART_RIDING";
    public float enginePitch = 1.0f;
    public String hornSound = "ITEM_GOAT_HORN_SOUND_1";
    public String brakeSound = "BLOCK_DISPENSER_DISPENSE";
    public String crashSound = "BLOCK_WOOD_BREAK";
    public String sirenSound = "BLOCK_NOTE_BLOCK_PLING";
    public String boostSound = "ENTITY_FIREWORK_ROCKET_LAUNCH";
    public String whistleSound = "BLOCK_BELL_USE";

    public VehicleStats(VehicleType type) {
        this.maxSpeed = type.getMaxSpeed();
        this.acceleration = type.getAcceleration();
        this.brake = type.getBrake();
        this.reverseSpeed = type.getReverseSpeed();
        this.steering = type.getSteering();
        this.friction = type.getFriction();
        this.maxHealth = type.getMaxHealth();
        this.length = type.getLength();
        this.width = type.getWidth();
        this.height = type.getHeight();
        applyTypeDefaults(type);
    }

    public VehicleStats(VehicleStats other) {
        this.enabled = other.enabled;
        this.permission = other.permission;
        this.maxSpeed = other.maxSpeed;
        this.acceleration = other.acceleration;
        this.brake = other.brake;
        this.reverseSpeed = other.reverseSpeed;
        this.steering = other.steering;
        this.friction = other.friction;
        this.maxHealth = other.maxHealth;
        this.length = other.length;
        this.width = other.width;
        this.height = other.height;
        this.modelScale = other.modelScale;
        this.turnInPlace = other.turnInPlace;
        this.stepUp = other.stepUp;
        this.leanFactor = other.leanFactor;
        this.drift = other.drift;
        this.burnout = other.burnout;
        this.boostEnabled = other.boostEnabled;
        this.boostMultiplier = other.boostMultiplier;
        this.boostDurationTicks = other.boostDurationTicks;
        this.boostCooldownTicks = other.boostCooldownTicks;
        this.siren = other.siren;
        this.cargoSize = other.cargoSize;
        this.carriages = other.carriages;
        this.engineSound = other.engineSound;
        this.enginePitch = other.enginePitch;
        this.hornSound = other.hornSound;
        this.brakeSound = other.brakeSound;
        this.crashSound = other.crashSound;
        this.sirenSound = other.sirenSound;
        this.boostSound = other.boostSound;
        this.whistleSound = other.whistleSound;
    }

    private void applyTypeDefaults(VehicleType type) {
        switch (type.getPhysics()) {
            case BIKE:
                engineSound = "ENTITY_BEE_LOOP";
                enginePitch = 0.8f;
                leanFactor = 1.0;
                break;
            case HEAVY:
                engineSound = "BLOCK_BLASTFURNACE_FIRE_CRACKLE";
                enginePitch = 0.7f;
                break;
            case BOAT:
                engineSound = "ENTITY_BOAT_PADDLE_WATER";
                enginePitch = 0.9f;
                break;
            case AIRCRAFT:
                engineSound = "ITEM_ELYTRA_FLYING";
                enginePitch = 0.9f;
                break;
            case TRAIN:
                engineSound = "ENTITY_MINECART_INSIDE";
                enginePitch = 0.9f;
                break;
            case CONSTRUCTION:
                engineSound = "BLOCK_PISTON_EXTEND";
                enginePitch = 0.6f;
                break;
            case CAR:
            default:
                break;
        }
        switch (type) {
            case SPORTS_CAR:
                boostEnabled = true;
                boostMultiplier = 1.5;
                drift = true;
                enginePitch = 1.2f;
                break;
            case MUSCLE_CAR:
                burnout = true;
                boostEnabled = true;
                boostMultiplier = 1.35;
                boostCooldownTicks = 300;
                enginePitch = 0.75f;
                break;
            case RACING_KART:
                boostEnabled = true;
                boostMultiplier = 1.4;
                boostDurationTicks = 50;
                boostCooldownTicks = 160;
                drift = true;
                enginePitch = 1.35f;
                break;
            case FIGHTER_JET:
                boostEnabled = true;
                boostMultiplier = 1.6;
                boostDurationTicks = 80;
                boostCooldownTicks = 240;
                break;
            case TANK:
            case BULLDOZER:
            case EXCAVATOR:
                turnInPlace = true;
                break;
            case ATV:
            case SUV:
                stepUp = true;
                break;
            case MOTORCYCLE:
                stepUp = true;
                leanFactor = 1.4;
                break;
            case MILITARY_TRUCK:
                stepUp = true;
                cargoSize = 18;
                break;
            case SCOOTER:
                leanFactor = 1.0;
                break;
            case PICKUP:
                cargoSize = 9;
                break;
            case VAN:
            case MINIVAN:
                cargoSize = 27;
                break;
            case AMBULANCE:
                cargoSize = 18;
                siren = true;
                break;
            case CONSTRUCTION_TRUCK:
                cargoSize = 18;
                break;
            case POLICE_CAR:
            case FIRE_TRUCK:
                siren = true;
                break;
            case LOCOMOTIVE:
                carriages = 2;
                break;
            case PASSENGER_TRAIN:
                carriages = 4;
                break;
            case TAXI:
                hornSound = "ENTITY_VILLAGER_YES";
                break;
            default:
                break;
        }
    }

    /** Rough mass factor used for crash damage and vehicle pushing. */
    public double mass(VehicleTier tier) {
        switch (tier) {
            case HEAVY:
                return 3.0;
            case SLOW:
                return 1.6;
            case VERY_FAST:
                return 0.8;
            case FAST:
                return 1.0;
            case NORMAL:
            default:
                return 1.2;
        }
    }
}
