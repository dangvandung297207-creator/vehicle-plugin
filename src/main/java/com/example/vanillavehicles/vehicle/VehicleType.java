package com.example.vanillavehicles.vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All 34 built-in vehicle types with their default tuning.
 *
 * <p>Units: speed in meters/second, acceleration/brake/friction in
 * meters/second^2, steering in degrees per tick at full lock and low speed,
 * sizes in meters. Every value can be overridden in config.yml.</p>
 */
public enum VehicleType {

    CAR("car", "Compact Car", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            24, 11, 16, 8, 2.6, 3.0, 100, 3.6, 1.8, 1.5,
            "A small everyday car with horn, lights and 4 seats."),
    SPORTS_CAR("sports_car", "Sports Car", VehicleCategory.GROUND, VehicleTier.FAST, PhysicsType.CAR,
            34, 18, 18, 9, 3.0, 2.2, 80, 3.8, 1.9, 1.1,
            "Low, fast and Grippy. Supports a configurable boost."),
    MUSCLE_CAR("muscle_car", "Muscle Car", VehicleCategory.GROUND, VehicleTier.FAST, PhysicsType.CAR,
            30, 16, 15, 8, 2.4, 2.6, 120, 4.2, 2.0, 1.3,
            "Heavy American-style performance car with burnout effect."),
    PICKUP("pickup", "Pickup Truck", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            21, 10, 14, 7, 2.2, 3.2, 140, 4.6, 2.0, 1.8,
            "Pickup with an open cargo bed and small trunk inventory."),
    VAN("van", "Delivery Van", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.HEAVY,
            19, 8, 13, 6, 2.0, 3.4, 160, 5.0, 2.1, 2.3,
            "Delivery van with a large cargo inventory."),
    MINIVAN("minivan", "Minivan", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            20, 9, 14, 7, 2.2, 3.2, 130, 4.4, 2.0, 1.9,
            "Family minivan with 6 comfortable seats."),
    SUV("suv", "SUV", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            22, 10, 14, 7, 2.1, 3.2, 150, 4.4, 2.0, 2.0,
            "Tall 4x4 with high ride height and step-up climbing."),
    BUS("bus", "Bus", VehicleCategory.GROUND, VehicleTier.HEAVY, PhysicsType.HEAVY,
            16, 6, 11, 5, 1.6, 3.6, 250, 9.0, 2.4, 2.8,
            "City bus with room for 12 passengers."),
    TAXI("taxi", "Taxi", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            23, 11, 16, 8, 2.6, 3.0, 100, 3.6, 1.8, 1.5,
            "Yellow cab with roof sign and a built-in fare meter."),
    POLICE_CAR("police_car", "Police Car", VehicleCategory.GROUND, VehicleTier.FAST, PhysicsType.CAR,
            29, 15, 17, 9, 2.8, 2.4, 120, 3.8, 1.9, 1.4,
            "Interceptor with flashing lights and toggleable siren."),
    AMBULANCE("ambulance", "Ambulance", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.HEAVY,
            24, 10, 14, 7, 2.0, 3.2, 170, 5.4, 2.1, 2.4,
            "Rescue vehicle with rear cabin, lights and siren."),
    FIRE_TRUCK("fire_truck", "Fire Truck", VehicleCategory.GROUND, VehicleTier.HEAVY, PhysicsType.HEAVY,
            17, 7, 12, 5, 1.7, 3.5, 280, 7.0, 2.4, 2.8,
            "Fire engine with siren and an extendable ladder."),
    GARBAGE_TRUCK("garbage_truck", "Garbage Truck", VehicleCategory.GROUND, VehicleTier.HEAVY, PhysicsType.HEAVY,
            14, 6, 11, 5, 1.7, 3.6, 260, 6.4, 2.4, 2.7,
            "Refuse truck with an animated dumping container."),
    CONSTRUCTION_TRUCK("construction_truck", "Construction Truck", VehicleCategory.UTILITY, VehicleTier.HEAVY, PhysicsType.CONSTRUCTION,
            15, 6, 11, 5, 1.8, 3.5, 240, 6.0, 2.4, 2.7,
            "Tipper truck for the construction site."),
    MILITARY_TRUCK("military_truck", "Military Truck", VehicleCategory.GROUND, VehicleTier.HEAVY, PhysicsType.HEAVY,
            18, 7, 12, 6, 1.9, 3.4, 300, 6.2, 2.4, 2.7,
            "Armored 6x6 transport with canvas cover."),
    TANK("tank", "Tank", VehicleCategory.GROUND, VehicleTier.HEAVY, PhysicsType.CONSTRUCTION,
            9, 5, 10, 4, 2.0, 4.0, 500, 5.2, 2.8, 2.2,
            "Tracked vehicle with rotating turret. Visual cannon only."),
    GOLF_CART("golf_cart", "Golf Cart", VehicleCategory.GROUND, VehicleTier.SLOW, PhysicsType.CAR,
            10, 6, 10, 4, 2.8, 3.0, 60, 2.4, 1.3, 1.5,
            "Tiny open runabout for resorts and malls."),
    ATV("atv", "ATV", VehicleCategory.GROUND, VehicleTier.NORMAL, PhysicsType.CAR,
            19, 11, 13, 7, 3.0, 3.0, 90, 2.2, 1.3, 1.2,
            "Quad bike with off-road step-up climbing."),
    MOTORCYCLE("motorcycle", "Motorcycle", VehicleCategory.GROUND, VehicleTier.FAST, PhysicsType.BIKE,
            32, 17, 16, 6, 3.4, 2.4, 70, 2.2, 0.8, 1.2,
            "Fast two-wheeler that leans into corners."),
    SCOOTER("scooter", "Scooter", VehicleCategory.GROUND, VehicleTier.SLOW, PhysicsType.BIKE,
            12, 7, 11, 4, 3.0, 3.0, 60, 1.9, 0.7, 1.3,
            "Small city scooter, easy to handle."),
    TRACTOR("tractor", "Tractor", VehicleCategory.UTILITY, VehicleTier.SLOW, PhysicsType.CONSTRUCTION,
            7, 5, 9, 4, 2.2, 3.0, 180, 3.6, 2.0, 2.3,
            "Farm tractor with big rear wheels and attachments."),
    FORKLIFT("forklift", "Forklift", VehicleCategory.UTILITY, VehicleTier.SLOW, PhysicsType.CONSTRUCTION,
            6, 5, 10, 4, 2.6, 3.5, 120, 2.8, 1.4, 2.0,
            "Warehouse forklift with a working lift."),
    EXCAVATOR("excavator", "Excavator", VehicleCategory.UTILITY, VehicleTier.SLOW, PhysicsType.CONSTRUCTION,
            5, 4, 9, 3, 2.2, 4.0, 220, 4.6, 2.4, 2.6,
            "Tracked digger with mouse-controlled boom and bucket."),
    BULLDOZER("bulldozer", "Bulldozer", VehicleCategory.UTILITY, VehicleTier.SLOW, PhysicsType.CONSTRUCTION,
            6, 5, 9, 3, 2.2, 4.0, 260, 4.4, 2.6, 2.4,
            "Tracked dozer with a wide front blade."),
    RACING_KART("racing_kart", "Racing Kart", VehicleCategory.GROUND, VehicleTier.VERY_FAST, PhysicsType.CAR,
            26, 20, 20, 8, 4.2, 2.0, 60, 1.8, 1.3, 0.8,
            "Tiny kart with drift mechanic and boost."),
    SPEEDBOAT("speedboat", "Speedboat", VehicleCategory.WATER, VehicleTier.VERY_FAST, PhysicsType.BOAT,
            30, 14, 10, 6, 2.4, 1.8, 90, 5.0, 2.0, 1.4,
            "Fast planing hull with spray and roar."),
    FISHING_BOAT("fishing_boat", "Fishing Boat", VehicleCategory.WATER, VehicleTier.NORMAL, PhysicsType.BOAT,
            14, 7, 8, 5, 1.8, 2.4, 120, 5.4, 2.2, 1.8,
            " sturdy fishing boat with rod holders and crates."),
    YACHT("yacht", "Yacht", VehicleCategory.WATER, VehicleTier.NORMAL, PhysicsType.BOAT,
            16, 6, 7, 4, 1.0, 2.2, 200, 10.0, 3.4, 3.2,
            "Luxury yacht with sun deck and 8 seats."),
    HELICOPTER("helicopter", "Helicopter", VehicleCategory.AIR, VehicleTier.NORMAL, PhysicsType.AIRCRAFT,
            26, 12, 10, 8, 2.6, 2.0, 150, 6.5, 2.2, 2.6,
            "Arcade helicopter: hover, strafe and mouse flight."),
    PLANE("plane", "Small Plane", VehicleCategory.AIR, VehicleTier.FAST, PhysicsType.AIRCRAFT,
            40, 10, 8, 0, 1.8, 1.2, 120, 7.5, 3.0, 2.4,
            "Bush plane with throttle, takeoff and landing."),
    FIGHTER_JET("fighter_jet", "Fighter Jet", VehicleCategory.AIR, VehicleTier.VERY_FAST, PhysicsType.AIRCRAFT,
            55, 22, 12, 0, 2.2, 1.0, 140, 9.0, 3.2, 2.6,
            "Afterburning jet with boost and banking."),
    LOCOMOTIVE("locomotive", "Locomotive", VehicleCategory.RAIL, VehicleTier.HEAVY, PhysicsType.TRAIN,
            22, 5, 9, 8, 0.45, 1.2, 400, 7.0, 2.4, 3.0,
            "Steam-style engine pulling articulated carriages."),
    PASSENGER_TRAIN("passenger_train", "Passenger Train", VehicleCategory.RAIL, VehicleTier.HEAVY, PhysicsType.TRAIN,
            24, 5, 9, 8, 0.4, 1.2, 350, 7.0, 2.4, 3.0,
            "Express consist with up to 20+ seats."),
    MINECART_RACER("minecart_racer", "Minecart Racer", VehicleCategory.RAIL, VehicleTier.NORMAL, PhysicsType.TRAIN,
            18, 12, 14, 8, 3.2, 2.5, 80, 2.0, 1.4, 1.6,
            "Single-seat rail rocket for stunt tracks.");

    private static final Map<String, VehicleType> BY_ID = new HashMap<>();

    static {
        for (VehicleType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final String id;
    private final String displayName;
    private final VehicleCategory category;
    private final VehicleTier tier;
    private final PhysicsType physics;
    private final double maxSpeed;
    private final double acceleration;
    private final double brake;
    private final double reverseSpeed;
    private final double steering;
    private final double friction;
    private final double maxHealth;
    private final double length;
    private final double width;
    private final double height;
    private final String description;

    VehicleType(String id, String displayName, VehicleCategory category, VehicleTier tier,
                PhysicsType physics, double maxSpeed, double acceleration, double brake,
                double reverseSpeed, double steering, double friction, double maxHealth,
                double length, double width, double height, String description) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.tier = tier;
        this.physics = physics;
        this.maxSpeed = maxSpeed;
        this.acceleration = acceleration;
        this.brake = brake;
        this.reverseSpeed = reverseSpeed;
        this.steering = steering;
        this.friction = friction;
        this.maxHealth = maxHealth;
        this.length = length;
        this.width = width;
        this.height = height;
        this.description = description;
    }

    public static VehicleType byId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (VehicleType type : values()) {
            ids.add(type.id);
        }
        return ids;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public VehicleTier getTier() {
        return tier;
    }

    public PhysicsType getPhysics() {
        return physics;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public double getAcceleration() {
        return acceleration;
    }

    public double getBrake() {
        return brake;
    }

    public double getReverseSpeed() {
        return reverseSpeed;
    }

    public double getSteering() {
        return steering;
    }

    public double getFriction() {
        return friction;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getLength() {
        return length;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public String getDescription() {
        return description;
    }
}
