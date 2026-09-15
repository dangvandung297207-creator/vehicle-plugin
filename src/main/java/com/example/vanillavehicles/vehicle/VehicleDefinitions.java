package com.example.vanillavehicles.vehicle;

import com.example.vanillavehicles.model.PartFlag;
import com.example.vanillavehicles.model.VehicleModel;

/**
 * Builds the models, seats and special parts of all 34 built-in vehicles.
 *
 * <p>Local axes: +X = vehicle left, +Y = up from the ground, +Z = forward.</p>
 */
public final class VehicleDefinitions {

    private VehicleDefinitions() {
    }

    public static void registerAll(VehicleRegistry registry) {
        registry.register(new VehicleDefinition(VehicleType.CAR, buildCar(), "MINECART"));
        registry.register(new VehicleDefinition(VehicleType.SPORTS_CAR, buildSportsCar(), "REDSTONE_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.MUSCLE_CAR, buildMuscleCar(), "ORANGE_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.PICKUP, buildPickup(), "CHEST"));
        registry.register(new VehicleDefinition(VehicleType.VAN, buildVan(), "BARREL"));
        registry.register(new VehicleDefinition(VehicleType.MINIVAN, buildMinivan(), "LIGHT_BLUE_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.SUV, buildSuv(), "GRAY_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.BUS, buildBus(), "YELLOW_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.TAXI, buildTaxi(), "GOLD_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.POLICE_CAR, buildPoliceCar(), "BLUE_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.AMBULANCE, buildAmbulance(), "WHITE_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.FIRE_TRUCK, buildFireTruck(), "RED_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.GARBAGE_TRUCK, buildGarbageTruck(), "GREEN_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.CONSTRUCTION_TRUCK, buildConstructionTruck(), "BRICKS"));
        registry.register(new VehicleDefinition(VehicleType.MILITARY_TRUCK, buildMilitaryTruck(), "MOSSY_COBBLESTONE"));
        registry.register(new VehicleDefinition(VehicleType.TANK, buildTank(), "BLACK_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.GOLF_CART, buildGolfCart(), "WHITE_WOOL"));
        registry.register(new VehicleDefinition(VehicleType.ATV, buildAtv(), "COAL_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.MOTORCYCLE, buildMotorcycle(), "END_ROD"));
        registry.register(new VehicleDefinition(VehicleType.SCOOTER, buildScooter(), "CYAN_CONCRETE"));
        registry.register(new VehicleDefinition(VehicleType.TRACTOR, buildTractor(), "HAY_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.FORKLIFT, buildForklift(), "LADDER"));
        registry.register(new VehicleDefinition(VehicleType.EXCAVATOR, buildExcavator(), "DIAMOND_SHOVEL"));
        registry.register(new VehicleDefinition(VehicleType.BULLDOZER, buildBulldozer(), "IRON_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.RACING_KART, buildRacingKart(), "FIREWORK_ROCKET"));
        registry.register(new VehicleDefinition(VehicleType.SPEEDBOAT, buildSpeedboat(), "OAK_BOAT"));
        registry.register(new VehicleDefinition(VehicleType.FISHING_BOAT, buildFishingBoat(), "FISHING_ROD"));
        registry.register(new VehicleDefinition(VehicleType.YACHT, buildYacht(), "OAK_CHEST_BOAT"));
        registry.register(new VehicleDefinition(VehicleType.HELICOPTER, buildHelicopter(), "ELYTRA"));
        registry.register(new VehicleDefinition(VehicleType.PLANE, buildPlane(), "QUARTZ_BLOCK"));
        registry.register(new VehicleDefinition(VehicleType.FIGHTER_JET, buildFighterJet(), "DRAGON_HEAD"));
        registry.register(new VehicleDefinition(VehicleType.LOCOMOTIVE, buildLocomotive(),
                buildCarriage("RED_CONCRETE"), "FURNACE"));
        registry.register(new VehicleDefinition(VehicleType.PASSENGER_TRAIN, buildPassengerTrain(),
                buildCarriage("WHITE_CONCRETE"), "POWERED_RAIL"));
        registry.register(new VehicleDefinition(VehicleType.MINECART_RACER, buildMinecartRacer(), "FURNACE_MINECART"));
    }

    // ------------------------------------------------------------------
    // Cars
    // ------------------------------------------------------------------

    private static VehicleModel buildCar() {
        VehicleModel m = new VehicleModel();
        m.box("body", "BLUE_CONCRETE", 0, 0.62, 0, 1.8, 0.62, 3.6);
        m.box("glass", "TINTED_GLASS", 0, 1.16, -0.25, 1.6, 0.5, 1.9);
        m.box("roof", "BLUE_CONCRETE", 0, 1.45, -0.25, 1.7, 0.1, 2.0);
        m.box("bumper_f", "GRAY_CONCRETE", 0, 0.35, 1.82, 1.9, 0.24, 0.22);
        m.box("bumper_r", "GRAY_CONCRETE", 0, 0.35, -1.82, 1.9, 0.24, 0.22);
        m.box("grille", "BLACK_CONCRETE", 0, 0.5, 1.81, 1.0, 0.18, 0.06);
        m.box("mirror_l", "BLACK_CONCRETE", 0.98, 1.05, 0.55, 0.12, 0.12, 0.2);
        m.box("mirror_r", "BLACK_CONCRETE", -0.98, 1.05, 0.55, 0.12, 0.12, 0.2);
        m.box("exhaust", "GRAY_CONCRETE", -0.5, 0.25, -1.85, 0.16, 0.16, 0.15);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.62, 0.62, 1.81, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.62, 0.62, 1.81, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.62, 0.62, -1.81, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.62, 0.62, -1.81, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("rev", "LIGHT_GRAY_CONCRETE", 0, 0.42, -1.82, 0.5, 0.14, 0.06, PartFlag.REVERSELIGHT);
        m.text("plate_f", "CAR-01", 0, 0.35, 1.95, 0.5, 0);
        m.wheel("wheel_fl", 0.85, 0.35, 1.15, 0.7, 0.3, true);
        m.wheel("wheel_fr", -0.85, 0.35, 1.15, 0.7, 0.3, true);
        m.wheel("wheel_rl", 0.85, 0.35, -1.15, 0.7, 0.3, false);
        m.wheel("wheel_rr", -0.85, 0.35, -1.15, 0.7, 0.3, false);
        m.seat(0.4, 0.35, 0.35, true);
        m.seat(-0.4, 0.35, 0.35, false);
        m.seatRow(0.35, -0.8, 0.4, -0.4);
        return m;
    }

    private static VehicleModel buildSportsCar() {
        VehicleModel m = new VehicleModel();
        m.box("body", "RED_CONCRETE", 0, 0.5, 0, 1.9, 0.5, 3.8);
        m.box("nose", "RED_CONCRETE", 0, 0.36, 1.95, 1.7, 0.3, 0.3);
        m.box("glass", "TINTED_GLASS", 0, 0.86, -0.3, 1.5, 0.4, 1.6);
        m.box("roof", "RED_CONCRETE", 0, 1.08, -0.3, 1.55, 0.08, 1.7);
        m.box("stripe", "WHITE_CONCRETE", 0, 0.76, 0.3, 0.5, 0.02, 2.2);
        m.box("spoil_l", "BLACK_CONCRETE", 0.6, 0.85, -1.8, 0.1, 0.3, 0.1);
        m.box("spoil_r", "BLACK_CONCRETE", -0.6, 0.85, -1.8, 0.1, 0.3, 0.1);
        m.box("wing", "BLACK_CONCRETE", 0, 1.02, -1.8, 1.7, 0.08, 0.4);
        m.box("diff", "BLACK_CONCRETE", 0, 0.25, -1.9, 1.6, 0.2, 0.15);
        m.box("exh_l", "IRON_BLOCK", 0.25, 0.28, -1.98, 0.14, 0.14, 0.12);
        m.box("exh_r", "IRON_BLOCK", -0.25, 0.28, -1.98, 0.14, 0.14, 0.12);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.6, 0.55, 1.9, 0.35, 0.15, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.6, 0.55, 1.9, 0.35, 0.15, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tailbar", "RED_CONCRETE", 0, 0.6, -1.92, 1.5, 0.15, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", "COAL_BLOCK", "GOLD_BLOCK", 0.9, 0.32, 1.2, 0.64, 0.34, true);
        m.wheel("wheel_fr", "COAL_BLOCK", "GOLD_BLOCK", -0.9, 0.32, 1.2, 0.64, 0.34, true);
        m.wheel("wheel_rl", "COAL_BLOCK", "GOLD_BLOCK", 0.9, 0.32, -1.2, 0.64, 0.36, false);
        m.wheel("wheel_rr", "COAL_BLOCK", "GOLD_BLOCK", -0.9, 0.32, -1.2, 0.64, 0.36, false);
        m.seat(0.42, 0.32, 0.1, true);
        m.seat(-0.42, 0.32, 0.1, false);
        return m;
    }

    private static VehicleModel buildMuscleCar() {
        VehicleModel m = new VehicleModel();
        m.box("body", "ORANGE_CONCRETE", 0, 0.6, 0, 2.0, 0.62, 4.2);
        m.box("scoop", "BLACK_CONCRETE", 0, 0.98, 1.2, 0.7, 0.16, 0.9);
        m.box("stripe_l", "BLACK_CONCRETE", 0.25, 0.92, 0.4, 0.25, 0.02, 3.2);
        m.box("stripe_r", "BLACK_CONCRETE", -0.25, 0.92, 0.4, 0.25, 0.02, 3.2);
        m.box("glass", "TINTED_GLASS", 0, 1.05, -0.5, 1.6, 0.45, 1.7);
        m.box("roof", "BLACK_CONCRETE", 0, 1.3, -0.5, 1.65, 0.1, 1.8);
        m.box("bumper_f", "IRON_BLOCK", 0, 0.35, 2.12, 2.0, 0.2, 0.18);
        m.box("bumper_r", "IRON_BLOCK", 0, 0.35, -2.12, 2.0, 0.2, 0.18);
        m.box("pipe_l", "IRON_BLOCK", 1.02, 0.3, 0, 0.08, 0.12, 2.4);
        m.box("pipe_r", "IRON_BLOCK", -1.02, 0.3, 0, 0.08, 0.12, 2.4);
        m.lamp("head_l1", "LIGHT_GRAY_CONCRETE", 0.4, 0.65, 2.11, 0.28, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_l2", "LIGHT_GRAY_CONCRETE", 0.8, 0.65, 2.11, 0.28, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r1", "LIGHT_GRAY_CONCRETE", -0.4, 0.65, 2.11, 0.28, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r2", "LIGHT_GRAY_CONCRETE", -0.8, 0.65, 2.11, 0.28, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.7, 0.65, -2.11, 0.4, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.7, 0.65, -2.11, 0.4, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.95, 0.4, 1.35, 0.8, 0.36, true);
        m.wheel("wheel_fr", -0.95, 0.4, 1.35, 0.8, 0.36, true);
        m.wheel("wheel_rl", 0.95, 0.4, -1.35, 0.8, 0.4, false);
        m.wheel("wheel_rr", -0.95, 0.4, -1.35, 0.8, 0.4, false);
        m.seat(0.45, 0.35, 0.4, true);
        m.seat(-0.45, 0.35, 0.4, false);
        m.seatRow(0.35, -0.8, 0.45, -0.45);
        return m;
    }

    private static VehicleModel buildPickup() {
        VehicleModel m = new VehicleModel();
        m.box("body", "GREEN_CONCRETE", 0, 0.6, 0, 2.0, 0.6, 4.6);
        m.box("cab", "GREEN_CONCRETE", 0, 1.25, 1.0, 1.9, 0.7, 1.8);
        m.box("shield", "GLASS", 0, 1.25, 1.92, 1.7, 0.6, 0.08);
        m.box("cabroof", "GREEN_CONCRETE", 0, 1.64, 1.0, 1.95, 0.1, 1.9);
        m.box("bed", "GRAY_CONCRETE", 0, 0.95, -1.15, 1.8, 0.1, 2.2);
        m.box("wall_l", "GREEN_CONCRETE", 0.92, 1.15, -1.15, 0.16, 0.5, 2.3);
        m.box("wall_r", "GREEN_CONCRETE", -0.92, 1.15, -1.15, 0.16, 0.5, 2.3);
        m.box("tailgate", "GREEN_CONCRETE", 0, 1.15, -2.25, 1.9, 0.5, 0.14);
        m.box("crate", "BARREL", 0.4, 1.2, -1.4, 0.6, 0.5, 0.6);
        m.box("bumper_f", "GRAY_CONCRETE", 0, 0.35, 2.32, 2.0, 0.22, 0.2);
        m.box("bumper_r", "GRAY_CONCRETE", 0, 0.35, -2.32, 2.0, 0.22, 0.2);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.65, 0.65, 2.31, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.65, 0.65, 2.31, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.65, 0.65, -2.31, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.65, 0.65, -2.31, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.95, 0.42, 1.45, 0.84, 0.34, true);
        m.wheel("wheel_fr", -0.95, 0.42, 1.45, 0.84, 0.34, true);
        m.wheel("wheel_rl", 0.95, 0.42, -1.45, 0.84, 0.34, false);
        m.wheel("wheel_rr", -0.95, 0.42, -1.45, 0.84, 0.34, false);
        m.seat(0.45, 0.7, 1.1, true);
        m.seat(-0.45, 0.7, 1.1, false);
        m.seatRow(0.7, 0.35, 0.45, -0.45);
        return m;
    }

    private static VehicleModel buildVan() {
        VehicleModel m = new VehicleModel();
        m.box("body", "WHITE_CONCRETE", 0, 1.15, 0, 2.1, 1.7, 5.0);
        m.box("shield", "GLASS", 0, 1.55, 2.45, 1.8, 0.6, 0.1);
        m.box("win_l", "GLASS", 1.06, 1.55, 1.5, 0.08, 0.6, 1.2);
        m.box("win_r", "GLASS", -1.06, 1.55, 1.5, 0.08, 0.6, 1.2);
        m.box("door", "LIGHT_GRAY_CONCRETE", 0, 1.15, -2.52, 1.9, 1.6, 0.08);
        m.box("stripe_l", "ORANGE_CONCRETE", 1.06, 0.9, 0, 0.06, 0.3, 4.6);
        m.box("stripe_r", "ORANGE_CONCRETE", -1.06, 0.9, 0, 0.06, 0.3, 4.6);
        m.box("beacon", "ORANGE_CONCRETE", 0, 2.05, 1.8, 0.3, 0.15, 0.3);
        m.box("bumper_f", "GRAY_CONCRETE", 0, 0.35, 2.52, 2.1, 0.22, 0.2);
        m.box("bumper_r", "GRAY_CONCRETE", 0, 0.35, -2.56, 2.1, 0.22, 0.2);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.7, 0.7, 2.51, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.7, 0.7, 2.51, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.7, 0.7, -2.56, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.7, 0.7, -2.56, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.text("logo_l", "FRESH", 1.1, 1.45, 0, 0.8, -90);
        m.text("logo_r", "FRESH", -1.1, 1.45, 0, 0.8, 90);
        m.wheel("wheel_fl", 1.0, 0.4, 1.6, 0.8, 0.32, true);
        m.wheel("wheel_fr", -1.0, 0.4, 1.6, 0.8, 0.32, true);
        m.wheel("wheel_rl", 1.0, 0.4, -1.6, 0.8, 0.32, false);
        m.wheel("wheel_rr", -1.0, 0.4, -1.6, 0.8, 0.32, false);
        m.seat(0.5, 0.75, 1.6, true);
        m.seat(-0.5, 0.75, 1.6, false);
        m.seat(0.5, 0.75, 0.6, false);
        return m;
    }

    private static VehicleModel buildMinivan() {
        VehicleModel m = new VehicleModel();
        m.box("body", "LIGHT_BLUE_CONCRETE", 0, 0.75, 0, 2.0, 0.9, 4.4);
        m.box("glass", "GLASS", 0, 1.4, 0, 1.9, 0.5, 3.9);
        m.box("roof", "LIGHT_BLUE_CONCRETE", 0, 1.68, 0, 1.95, 0.1, 4.0);
        m.box("bumper_f", "GRAY_CONCRETE", 0, 0.35, 2.22, 2.0, 0.22, 0.2);
        m.box("bumper_r", "GRAY_CONCRETE", 0, 0.35, -2.22, 2.0, 0.22, 0.2);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.6, 0.7, 2.21, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.6, 0.7, 2.21, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.6, 0.7, -2.21, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.6, 0.7, -2.21, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.95, 0.38, 1.4, 0.76, 0.3, true);
        m.wheel("wheel_fr", -0.95, 0.38, 1.4, 0.76, 0.3, true);
        m.wheel("wheel_rl", 0.95, 0.38, -1.4, 0.76, 0.3, false);
        m.wheel("wheel_rr", -0.95, 0.38, -1.4, 0.76, 0.3, false);
        m.seat(0.45, 0.55, 1.2, true);
        m.seat(-0.45, 0.55, 1.2, false);
        m.seatRow(0.55, 0.1, 0.45, -0.45);
        m.seatRow(0.55, -1.0, 0.45, -0.45);
        return m;
    }

    private static VehicleModel buildSuv() {
        VehicleModel m = new VehicleModel();
        m.box("body", "GRAY_CONCRETE", 0, 0.85, 0, 2.0, 0.8, 4.4);
        m.box("glass", "TINTED_GLASS", 0, 1.45, -0.2, 1.85, 0.5, 3.2);
        m.box("roof", "GRAY_CONCRETE", 0, 1.73, -0.2, 1.9, 0.1, 3.3);
        m.box("rail_l", "BLACK_CONCRETE", 0.7, 1.82, -0.2, 0.1, 0.08, 2.6);
        m.box("rail_r", "BLACK_CONCRETE", -0.7, 1.82, -0.2, 0.1, 0.08, 2.6);
        m.box("bull", "BLACK_CONCRETE", 0, 0.7, 2.25, 1.6, 0.4, 0.15);
        m.itemSized("spare", "BLACK_CONCRETE", 0, 1.0, -2.25, 0.7, 0.7, 0.3);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.65, 0.85, 2.21, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.65, 0.85, 2.21, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.75, 0.85, -2.21, 0.3, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.75, 0.85, -2.21, 0.3, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.95, 0.45, 1.4, 0.9, 0.34, true);
        m.wheel("wheel_fr", -0.95, 0.45, 1.4, 0.9, 0.34, true);
        m.wheel("wheel_rl", 0.95, 0.45, -1.4, 0.9, 0.34, false);
        m.wheel("wheel_rr", -0.95, 0.45, -1.4, 0.9, 0.34, false);
        m.seat(0.45, 0.7, 1.2, true);
        m.seat(-0.45, 0.7, 1.2, false);
        m.seatRow(0.7, 0.1, 0.45, -0.45);
        m.seat(0, 0.7, -1.0, false);
        return m;
    }

    // ------------------------------------------------------------------
    // Bus / taxi / emergency
    // ------------------------------------------------------------------

    private static VehicleModel buildBus() {
        VehicleModel m = new VehicleModel();
        m.box("body", "YELLOW_CONCRETE", 0, 1.4, 0, 2.4, 2.0, 9.0);
        m.box("winband_l", "GLASS", 1.21, 1.8, 0, 0.06, 0.7, 8.2);
        m.box("winband_r", "GLASS", -1.21, 1.8, 0, 0.06, 0.7, 8.2);
        m.box("shield", "GLASS", 0, 1.8, 4.51, 2.0, 0.7, 0.08);
        m.box("door_f", "GLASS", -1.21, 1.3, 3.2, 0.08, 1.6, 1.0);
        m.box("door_r", "GLASS", -1.21, 1.3, -1.5, 0.08, 1.6, 1.0);
        m.box("stripe_l", "BLACK_CONCRETE", 1.21, 0.9, 0, 0.06, 0.2, 8.6);
        m.box("stripe_r", "BLACK_CONCRETE", -1.21, 0.9, 0, 0.06, 0.2, 8.6);
        m.box("stop", "RED_CONCRETE", -1.25, 1.6, 2.6, 0.08, 0.5, 0.5);
        m.box("bumper_f", "GRAY_CONCRETE", 0, 0.45, 4.52, 2.4, 0.25, 0.2);
        m.box("bumper_r", "GRAY_CONCRETE", 0, 0.45, -4.52, 2.4, 0.25, 0.2);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.8, 0.9, 4.51, 0.36, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.8, 0.9, 4.51, 0.36, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.8, 0.9, -4.51, 0.36, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.8, 0.9, -4.51, 0.36, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.text("dest", "DOWNTOWN", 0, 2.35, 4.55, 0.9, 0);
        m.wheel("wheel_fl", 1.1, 0.5, 2.9, 1.0, 0.4, true);
        m.wheel("wheel_fr", -1.1, 0.5, 2.9, 1.0, 0.4, true);
        m.wheel("wheel_ml", 1.1, 0.5, -1.8, 1.0, 0.4, false);
        m.wheel("wheel_mr", -1.1, 0.5, -1.8, 1.0, 0.4, false);
        m.wheel("wheel_rl", 1.1, 0.5, -2.9, 1.0, 0.4, false);
        m.wheel("wheel_rr", -1.1, 0.5, -2.9, 1.0, 0.4, false);
        m.seat(0.55, 0.85, 3.4, true);
        m.seatRow(0.85, 2.3, 0.55, -0.55);
        m.seatRow(0.85, 1.3, 0.55, -0.55);
        m.seatRow(0.85, 0.3, 0.55, -0.55);
        m.seatRow(0.85, -0.7, 0.55, -0.55);
        m.seatRow(0.85, -1.7, 0.55, -0.55);
        m.seat(0, 0.85, -2.7, false);
        return m;
    }

    private static VehicleModel buildTaxi() {
        VehicleModel m = new VehicleModel();
        m.box("body", "YELLOW_CONCRETE", 0, 0.62, 0, 1.8, 0.62, 3.6);
        m.box("band_l", "BLACK_CONCRETE", 0.91, 0.62, 0, 0.04, 0.2, 3.4);
        m.box("band_r", "BLACK_CONCRETE", -0.91, 0.62, 0, 0.04, 0.2, 3.4);
        m.box("glass", "TINTED_GLASS", 0, 1.16, -0.25, 1.6, 0.5, 1.9);
        m.box("roof", "YELLOW_CONCRETE", 0, 1.45, -0.25, 1.7, 0.1, 2.0);
        m.box("signbase", "BLACK_CONCRETE", 0, 1.58, -0.25, 0.9, 0.2, 0.35);
        m.box("meter", "BLACK_CONCRETE", 0.35, 0.95, 0.9, 0.2, 0.15, 0.1);
        m.box("bumper_f", "BLACK_CONCRETE", 0, 0.35, 1.82, 1.9, 0.24, 0.22);
        m.box("bumper_r", "BLACK_CONCRETE", 0, 0.35, -1.82, 1.9, 0.24, 0.22);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.62, 0.62, 1.81, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.62, 0.62, 1.81, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.62, 0.62, -1.81, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.62, 0.62, -1.81, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.text("sign_f", "TAXI", 0, 1.58, -0.06, 0.55, 0);
        m.text("sign_r", "TAXI", 0, 1.58, -0.44, 0.55, 180);
        m.wheel("wheel_fl", 0.85, 0.35, 1.15, 0.7, 0.3, true);
        m.wheel("wheel_fr", -0.85, 0.35, 1.15, 0.7, 0.3, true);
        m.wheel("wheel_rl", 0.85, 0.35, -1.15, 0.7, 0.3, false);
        m.wheel("wheel_rr", -0.85, 0.35, -1.15, 0.7, 0.3, false);
        m.seat(0.4, 0.35, 0.35, true);
        m.seat(-0.4, 0.35, 0.35, false);
        m.seatRow(0.35, -0.8, 0.4, -0.4);
        return m;
    }

    private static VehicleModel buildPoliceCar() {
        VehicleModel m = new VehicleModel();
        m.box("body", "WHITE_CONCRETE", 0, 0.6, 0, 1.9, 0.6, 3.8);
        m.box("band_l", "BLACK_CONCRETE", 0.96, 0.6, 0.2, 0.04, 0.4, 1.6);
        m.box("band_r", "BLACK_CONCRETE", -0.96, 0.6, 0.2, 0.04, 0.4, 1.6);
        m.box("hood", "BLACK_CONCRETE", 0, 0.92, 1.2, 1.7, 0.04, 1.2);
        m.box("glass", "TINTED_GLASS", 0, 1.1, -0.3, 1.6, 0.5, 1.8);
        m.box("roof", "WHITE_CONCRETE", 0, 1.38, -0.3, 1.65, 0.1, 1.9);
        m.box("bar", "BLACK_CONCRETE", 0, 1.47, -0.3, 1.2, 0.1, 0.4);
        m.box("push", "BLACK_CONCRETE", 0, 0.6, 1.95, 1.6, 0.35, 0.12);
        m.lamp("em_l", "GRAY_CONCRETE", 0.35, 1.6, -0.3, 0.4, 0.18, 0.35, PartFlag.EMERGENCY);
        m.lamp("em_r", "GRAY_CONCRETE", -0.35, 1.6, -0.3, 0.4, 0.18, 0.35, PartFlag.EMERGENCY);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.62, 0.6, 1.91, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.62, 0.6, 1.91, 0.32, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.62, 0.6, -1.91, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.62, 0.6, -1.91, 0.32, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.text("side_l", "POLICE", 0.99, 0.62, 0.2, 0.6, -90);
        m.text("side_r", "POLICE", -0.99, 0.62, 0.2, 0.6, 90);
        m.wheel("wheel_fl", "COAL_BLOCK", "IRON_BLOCK", 0.9, 0.34, 1.2, 0.68, 0.32, true);
        m.wheel("wheel_fr", "COAL_BLOCK", "IRON_BLOCK", -0.9, 0.34, 1.2, 0.68, 0.32, true);
        m.wheel("wheel_rl", "COAL_BLOCK", "IRON_BLOCK", 0.9, 0.34, -1.2, 0.68, 0.32, false);
        m.wheel("wheel_rr", "COAL_BLOCK", "IRON_BLOCK", -0.9, 0.34, -1.2, 0.68, 0.32, false);
        m.seat(0.4, 0.33, 0.35, true);
        m.seat(-0.4, 0.33, 0.35, false);
        m.seatRow(0.33, -0.8, 0.4, -0.4);
        return m;
    }

    private static VehicleModel buildAmbulance() {
        VehicleModel m = new VehicleModel();
        m.box("cab", "WHITE_CONCRETE", 0, 0.9, 1.7, 2.1, 1.1, 1.9);
        m.box("box", "WHITE_CONCRETE", 0, 1.25, -0.9, 2.1, 1.9, 3.4);
        m.box("stripe_l", "RED_CONCRETE", 1.06, 1.0, -0.9, 0.05, 0.35, 3.3);
        m.box("stripe_r", "RED_CONCRETE", -1.06, 1.0, -0.9, 0.05, 0.35, 3.3);
        m.box("cross_l_h", "RED_CONCRETE", 1.07, 1.6, -0.9, 0.05, 0.3, 0.9);
        m.box("cross_l_v", "RED_CONCRETE", 1.07, 1.6, -0.9, 0.05, 0.9, 0.3);
        m.box("cross_r_h", "RED_CONCRETE", -1.07, 1.6, -0.9, 0.05, 0.3, 0.9);
        m.box("cross_r_v", "RED_CONCRETE", -1.07, 1.6, -0.9, 0.05, 0.9, 0.3);
        m.box("shield", "GLASS", 0, 1.15, 2.66, 1.8, 0.55, 0.08);
        m.box("rdoor", "LIGHT_GRAY_CONCRETE", 0, 1.25, -2.62, 1.9, 1.8, 0.08);
        m.box("rwin", "GLASS", 0, 1.7, -2.66, 1.6, 0.5, 0.06);
        m.box("bed", "WHITE_CONCRETE", 0.4, 0.7, -1.2, 0.8, 0.3, 1.6);
        m.box("bar", "WHITE_CONCRETE", 0, 1.5, 1.7, 1.2, 0.1, 0.4);
        m.lamp("em_fl", "GRAY_CONCRETE", 0.35, 1.63, 1.7, 0.35, 0.16, 0.32, PartFlag.EMERGENCY);
        m.lamp("em_fr", "GRAY_CONCRETE", -0.35, 1.63, 1.7, 0.35, 0.16, 0.32, PartFlag.EMERGENCY);
        m.lamp("em_rl", "GRAY_CONCRETE", 0.7, 2.25, -2.62, 0.3, 0.2, 0.1, PartFlag.EMERGENCY);
        m.lamp("em_rr", "GRAY_CONCRETE", -0.7, 2.25, -2.62, 0.3, 0.2, 0.1, PartFlag.EMERGENCY);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.7, 0.65, 2.66, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.7, 0.65, 2.66, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.85, 0.65, -2.66, 0.3, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.85, 0.65, -2.66, 0.3, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.text("amb_r", "AMBULANCE", 0, 0.62, -2.68, 0.55, 180);
        m.wheel("wheel_fl", 1.0, 0.42, 1.7, 0.84, 0.32, true);
        m.wheel("wheel_fr", -1.0, 0.42, 1.7, 0.84, 0.32, true);
        m.wheel("wheel_ml", 1.0, 0.42, -1.0, 0.84, 0.32, false);
        m.wheel("wheel_mr", -1.0, 0.42, -1.0, 0.84, 0.32, false);
        m.wheel("wheel_rl", 1.0, 0.42, -1.9, 0.84, 0.32, false);
        m.wheel("wheel_rr", -1.0, 0.42, -1.9, 0.84, 0.32, false);
        m.seat(0.5, 0.6, 1.7, true);
        m.seat(-0.5, 0.6, 1.7, false);
        m.seat(-0.5, 0.55, -1.2, false);
        m.seat(0.5, 0.55, -0.3, false);
        return m;
    }

    private static VehicleModel buildFireTruck() {
        VehicleModel m = new VehicleModel();
        m.box("cab", "RED_CONCRETE", 0, 1.1, 2.3, 2.4, 1.5, 2.2);
        m.box("shield", "GLASS", 0, 1.45, 3.42, 2.0, 0.6, 0.08);
        m.box("win_l", "GLASS", 1.21, 1.45, 2.3, 0.06, 0.6, 1.4);
        m.box("win_r", "GLASS", -1.21, 1.45, 2.3, 0.06, 0.6, 1.4);
        m.box("body", "RED_CONCRETE", 0, 1.3, -1.0, 2.4, 1.8, 4.6);
        m.box("comp_l", "GRAY_CONCRETE", 1.21, 1.3, -1.0, 0.06, 1.0, 3.6);
        m.box("comp_r", "GRAY_CONCRETE", -1.21, 1.3, -1.0, 0.06, 1.0, 3.6);
        m.box("stripe_l", "WHITE_CONCRETE", 1.21, 0.8, -1.0, 0.06, 0.2, 4.4);
        m.box("stripe_r", "WHITE_CONCRETE", -1.21, 0.8, -1.0, 0.06, 0.2, 4.4);
        m.box("grill", "BLACK_CONCRETE", 0, 0.8, 3.52, 1.6, 0.5, 0.1);
        m.box("bar", "BLACK_CONCRETE", 0, 1.92, 2.3, 1.3, 0.1, 0.4);
        m.slider("ladder", "LIGHT_GRAY_CONCRETE", "ladder", 0, 2.35, -1.0, 0.7, 0.18, 4.2);
        m.lamp("em_fl", "GRAY_CONCRETE", 0.4, 2.05, 2.3, 0.4, 0.18, 0.35, PartFlag.EMERGENCY);
        m.lamp("em_fr", "GRAY_CONCRETE", -0.4, 2.05, 2.3, 0.4, 0.18, 0.35, PartFlag.EMERGENCY);
        m.lamp("em_rl", "GRAY_CONCRETE", 0.8, 2.25, -3.32, 0.3, 0.2, 0.1, PartFlag.EMERGENCY);
        m.lamp("em_rr", "GRAY_CONCRETE", -0.8, 2.25, -3.32, 0.3, 0.2, 0.1, PartFlag.EMERGENCY);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.8, 0.85, 3.52, 0.36, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.8, 0.85, 3.52, 0.36, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.8, 0.85, -3.32, 0.36, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.8, 0.85, -3.32, 0.36, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.text("plate", "FIRE-1", 0, 0.5, 3.58, 0.5, 0);
        m.wheel("wheel_fl", 1.15, 0.5, 2.3, 1.0, 0.4, true);
        m.wheel("wheel_fr", -1.15, 0.5, 2.3, 1.0, 0.4, true);
        m.wheel("wheel_ml", 1.15, 0.5, -1.2, 1.0, 0.4, false);
        m.wheel("wheel_mr", -1.15, 0.5, -1.2, 1.0, 0.4, false);
        m.wheel("wheel_rl", 1.15, 0.5, -2.4, 1.0, 0.4, false);
        m.wheel("wheel_rr", -1.15, 0.5, -2.4, 1.0, 0.4, false);
        m.seat(0.55, 0.85, 2.5, true);
        m.seat(-0.55, 0.85, 2.5, false);
        m.seatRow(0.85, 1.5, 0.55, -0.55);
        return m;
    }

    // ------------------------------------------------------------------
    // Trucks / heavy
    // ------------------------------------------------------------------

    private static VehicleModel buildGarbageTruck() {
        VehicleModel m = new VehicleModel();
        m.box("cab", "GREEN_CONCRETE", 0, 1.0, 2.2, 2.4, 1.4, 2.0);
        m.box("shield", "GLASS", 0, 1.3, 3.22, 2.0, 0.55, 0.08);
        m.box("mech", "BLACK_CONCRETE", 0, 0.9, -2.75, 2.0, 0.8, 0.5);
        m.box("stripe_l", "WHITE_CONCRETE", 0.9, 0.75, -0.7, 0.05, 0.25, 3.4);
        m.box("stripe_r", "WHITE_CONCRETE", -0.9, 0.75, -0.7, 0.05, 0.25, 3.4);
        m.box("bumper", "GRAY_CONCRETE", 0, 0.4, 3.24, 2.3, 0.25, 0.2);
        m.hinge("container", "GRAY_CONCRETE", "dump", 0, 1.55, -0.7, 2.3, 1.9, 3.6, 0, 0.9, -2.5);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.8, 0.8, 3.22, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.8, 0.8, 3.22, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.8, 0.8, -3.02, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.8, 0.8, -3.02, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("warn_l", "ORANGE_CONCRETE", 0.8, 2.0, 3.15, 0.25, 0.18, 0.12, PartFlag.HEADLIGHT);
        m.lamp("warn_r", "ORANGE_CONCRETE", -0.8, 2.0, 3.15, 0.25, 0.18, 0.12, PartFlag.HEADLIGHT);
        m.text("san_l", "SANITATION", 1.18, 1.4, -0.7, 0.55, -90);
        m.text("san_r", "SANITATION", -1.18, 1.4, -0.7, 0.55, 90);
        m.wheel("wheel_fl", 1.15, 0.5, 2.2, 1.0, 0.4, true);
        m.wheel("wheel_fr", -1.15, 0.5, 2.2, 1.0, 0.4, true);
        m.wheel("wheel_ml", 1.15, 0.5, -1.2, 1.0, 0.4, false);
        m.wheel("wheel_mr", -1.15, 0.5, -1.2, 1.0, 0.4, false);
        m.wheel("wheel_rl", 1.15, 0.5, -2.3, 1.0, 0.4, false);
        m.wheel("wheel_rr", -1.15, 0.5, -2.3, 1.0, 0.4, false);
        m.seat(0.55, 0.8, 2.2, true);
        m.seat(-0.55, 0.8, 2.2, false);
        return m;
    }

    private static VehicleModel buildConstructionTruck() {
        VehicleModel m = new VehicleModel();
        m.box("cab", "ORANGE_CONCRETE", 0, 1.0, 2.0, 2.4, 1.4, 2.0);
        m.box("shield", "GLASS", 0, 1.3, 3.02, 2.0, 0.55, 0.08);
        m.box("stripe", "BLACK_CONCRETE", 0, 0.7, 3.04, 1.8, 0.2, 0.06);
        m.box("bumper", "GRAY_CONCRETE", 0, 0.4, 3.04, 2.3, 0.25, 0.2);
        m.hinge("bed", "YELLOW_CONCRETE", "dump", 0, 1.5, -0.6, 2.3, 1.5, 3.4, 0, 0.9, -2.3);
        m.hinge("load", "BROWN_CONCRETE", "dump", 0, 2.1, -0.6, 1.9, 0.4, 2.8, 0, 0.9, -2.3);
        m.box("beacon", "ORANGE_CONCRETE", 0, 1.85, 2.0, 0.3, 0.2, 0.3);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.8, 0.8, 3.02, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.8, 0.8, 3.02, 0.34, 0.22, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.8, 0.8, -2.82, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.8, 0.8, -2.82, 0.34, 0.22, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 1.15, 0.5, 2.0, 1.0, 0.4, true);
        m.wheel("wheel_fr", -1.15, 0.5, 2.0, 1.0, 0.4, true);
        m.wheel("wheel_ml", 1.15, 0.5, -1.0, 1.0, 0.4, false);
        m.wheel("wheel_mr", -1.15, 0.5, -1.0, 1.0, 0.4, false);
        m.wheel("wheel_rl", 1.15, 0.5, -2.1, 1.0, 0.4, false);
        m.wheel("wheel_rr", -1.15, 0.5, -2.1, 1.0, 0.4, false);
        m.seat(0.55, 0.8, 2.0, true);
        m.seat(-0.55, 0.8, 2.0, false);
        return m;
    }

    private static VehicleModel buildMilitaryTruck() {
        VehicleModel m = new VehicleModel();
        m.box("cab", "GREEN_CONCRETE", 0, 1.0, 2.1, 2.4, 1.4, 2.0);
        m.box("shield", "GLASS", 0, 1.3, 3.12, 1.0, 0.5, 0.08);
        m.box("shield_r", "GLASS", -0.55, 1.3, 3.12, 1.0, 0.5, 0.08);
        m.box("bedbox", "GREEN_CONCRETE", 0, 1.2, -0.8, 2.3, 1.0, 3.6);
        m.box("canvas", "BROWN_CONCRETE", 0, 1.95, -0.8, 2.2, 0.5, 3.5);
        m.box("bumper", "BLACK_CONCRETE", 0, 0.45, 3.14, 2.3, 0.3, 0.2);
        m.box("spare", "BLACK_CONCRETE", 0, 1.1, -2.65, 0.8, 0.8, 0.25);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.8, 0.85, 3.12, 0.3, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.8, 0.85, 3.12, 0.3, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.8, 0.85, -2.62, 0.3, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.8, 0.85, -2.62, 0.3, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 1.15, 0.5, 2.1, 1.0, 0.4, true);
        m.wheel("wheel_fr", -1.15, 0.5, 2.1, 1.0, 0.4, true);
        m.wheel("wheel_ml", 1.15, 0.5, -0.7, 1.0, 0.4, false);
        m.wheel("wheel_mr", -1.15, 0.5, -0.7, 1.0, 0.4, false);
        m.wheel("wheel_rl", 1.15, 0.5, -2.0, 1.0, 0.4, false);
        m.wheel("wheel_rr", -1.15, 0.5, -2.0, 1.0, 0.4, false);
        m.seat(0.55, 0.8, 2.1, true);
        m.seat(-0.55, 0.8, 2.1, false);
        m.seatRow(1.0, -0.2, 0.7, -0.7);
        m.seatRow(1.0, -1.4, 0.7, -0.7);
        return m;
    }

    private static VehicleModel buildTank() {
        VehicleModel m = new VehicleModel();
        m.track("l", 1.15, 0.45, 0, 0.6, 0.9, 9);
        m.track("r", -1.15, 0.45, 0, 0.6, 0.9, 9);
        m.box("guard_l", "GREEN_CONCRETE", 1.15, 0.95, 0, 0.65, 0.1, 4.6);
        m.box("guard_r", "GREEN_CONCRETE", -1.15, 0.95, 0, 0.65, 0.1, 4.6);
        m.box("hull", "GREEN_CONCRETE", 0, 0.75, 0, 2.0, 0.7, 4.6);
        m.box("glacis", "GREEN_CONCRETE", 0, 0.95, 2.4, 1.9, 0.4, 0.5);
        m.box("ant", "BLACK_CONCRETE", -0.7, 1.8, -1.5, 0.05, 1.0, 0.05);
        m.itemSized("turret", "GREEN_CONCRETE", 0, 1.35, -0.3, 1.7, 0.5, 2.4, PartFlag.TURRET);
        m.itemSized("hatch", "BLACK_CONCRETE", 0.5, 1.65, -0.6, 0.5, 0.12, 0.5, PartFlag.TURRET);
        m.barrel("barrel", "BLACK_CONCRETE", 0, 1.4, 1.2, 0.22, 0.22, 2.6, 0, 1.4, -0.3);
        m.barrel("muzzle", "BLACK_CONCRETE", 0, 1.4, 2.4, 0.3, 0.3, 0.3, 0, 1.4, -0.3);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0.7, 1.0, 2.62, 0.25, 0.2, 0.1, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", -0.7, 1.0, -2.32, 0.25, 0.2, 0.1, PartFlag.BRAKELIGHT);
        m.seat(0, 0.85, 1.6, true);
        m.seat(0, 0.85, -0.5, false);
        return m;
    }

    // ------------------------------------------------------------------
    // Small / two-wheel / off-road
    // ------------------------------------------------------------------

    private static VehicleModel buildGolfCart() {
        VehicleModel m = new VehicleModel();
        m.box("floor", "WHITE_CONCRETE", 0, 0.35, 0, 1.2, 0.15, 2.2);
        m.box("roof", "WHITE_CONCRETE", 0, 1.5, 0, 1.3, 0.08, 2.0);
        m.box("post_fl", "BLACK_CONCRETE", 0.55, 0.95, 0.9, 0.08, 1.1, 0.08);
        m.box("post_fr", "BLACK_CONCRETE", -0.55, 0.95, 0.9, 0.08, 1.1, 0.08);
        m.box("post_rl", "BLACK_CONCRETE", 0.55, 0.95, -0.9, 0.08, 1.1, 0.08);
        m.box("post_rr", "BLACK_CONCRETE", -0.55, 0.95, -0.9, 0.08, 1.1, 0.08);
        m.box("cowl", "WHITE_CONCRETE", 0, 0.55, 1.1, 1.1, 0.4, 0.5);
        m.box("bench", "BLACK_CONCRETE", 0, 0.55, -0.2, 1.0, 0.25, 0.9);
        m.box("col", "BLACK_CONCRETE", 0.3, 0.7, 0.7, 0.08, 0.4, 0.08);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.3, 0.6, 1.36, 0.18, 0.14, 0.06, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.3, 0.6, 1.36, 0.18, 0.14, 0.06, PartFlag.HEADLIGHT);
        m.wheel("wheel_fl", 0.6, 0.22, 0.75, 0.44, 0.22, true);
        m.wheel("wheel_fr", -0.6, 0.22, 0.75, 0.44, 0.22, true);
        m.wheel("wheel_rl", 0.6, 0.22, -0.75, 0.44, 0.22, false);
        m.wheel("wheel_rr", -0.6, 0.22, -0.75, 0.44, 0.22, false);
        m.seat(0.3, 0.6, 0.15, true);
        m.seat(-0.3, 0.6, 0.15, false);
        return m;
    }

    private static VehicleModel buildAtv() {
        VehicleModel m = new VehicleModel();
        m.box("body", "RED_CONCRETE", 0, 0.5, 0, 1.1, 0.35, 2.0);
        m.box("rack_f", "BLACK_CONCRETE", 0, 0.72, 0.85, 0.9, 0.08, 0.5);
        m.box("rack_r", "BLACK_CONCRETE", 0, 0.72, -0.85, 0.9, 0.08, 0.5);
        m.box("saddle", "BLACK_CONCRETE", 0, 0.72, -0.3, 0.5, 0.15, 0.9);
        m.box("bar", "BLACK_CONCRETE", 0, 0.95, 0.55, 0.7, 0.08, 0.08);
        m.box("guard_fl", "RED_CONCRETE", 0.6, 0.62, 0.7, 0.3, 0.08, 0.5);
        m.box("guard_fr", "RED_CONCRETE", -0.6, 0.62, 0.7, 0.3, 0.08, 0.5);
        m.box("guard_rl", "RED_CONCRETE", 0.6, 0.62, -0.7, 0.3, 0.08, 0.5);
        m.box("guard_rr", "RED_CONCRETE", -0.6, 0.62, -0.7, 0.3, 0.08, 0.5);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 0.6, 1.02, 0.4, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 0.55, -1.02, 0.3, 0.15, 0.06, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.6, 0.3, 0.7, 0.6, 0.3, true);
        m.wheel("wheel_fr", -0.6, 0.3, 0.7, 0.6, 0.3, true);
        m.wheel("wheel_rl", 0.6, 0.3, -0.7, 0.6, 0.3, false);
        m.wheel("wheel_rr", -0.6, 0.3, -0.7, 0.6, 0.3, false);
        m.seat(0, 0.75, -0.2, true);
        return m;
    }

    private static VehicleModel buildMotorcycle() {
        VehicleModel m = new VehicleModel();
        m.box("frame", "BLACK_CONCRETE", 0, 0.5, 0, 0.3, 0.3, 1.8);
        m.box("tank", "RED_CONCRETE", 0, 0.68, 0.2, 0.4, 0.25, 0.7);
        m.box("saddle", "BLACK_CONCRETE", 0, 0.66, -0.55, 0.35, 0.15, 0.6);
        m.box("fork", "IRON_BLOCK", 0, 0.55, 0.95, 0.12, 0.7, 0.12);
        m.box("fender", "RED_CONCRETE", 0, 0.72, 0.95, 0.3, 0.08, 0.4);
        m.box("bar", "BLACK_CONCRETE", 0, 0.95, 0.75, 0.6, 0.07, 0.07);
        m.box("pipe", "IRON_BLOCK", -0.2, 0.3, -0.3, 0.12, 0.12, 1.2);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 0.8, 1.05, 0.25, 0.25, 0.1, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 0.65, -1.0, 0.2, 0.15, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_f", 0, 0.35, 0.95, 0.7, 0.18, true);
        m.wheel("wheel_r", 0, 0.35, -0.75, 0.7, 0.22, false);
        m.seat(0, 0.72, -0.35, true);
        m.seat(0, 0.72, -0.8, false);
        return m;
    }

    private static VehicleModel buildScooter() {
        VehicleModel m = new VehicleModel();
        m.box("floor", "CYAN_CONCRETE", 0, 0.3, 0.1, 0.4, 0.12, 1.0);
        m.box("shield", "CYAN_CONCRETE", 0, 0.6, 0.75, 0.45, 0.6, 0.15);
        m.box("bar", "BLACK_CONCRETE", 0, 0.95, 0.7, 0.5, 0.07, 0.07);
        m.box("rear", "CYAN_CONCRETE", 0, 0.5, -0.55, 0.45, 0.4, 0.7);
        m.box("saddle", "BLACK_CONCRETE", 0, 0.75, -0.55, 0.4, 0.12, 0.6);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 0.75, 0.84, 0.2, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 0.55, -0.92, 0.18, 0.14, 0.06, PartFlag.BRAKELIGHT);
        m.wheel("wheel_f", 0, 0.25, 0.8, 0.5, 0.16, true);
        m.wheel("wheel_r", 0, 0.25, -0.65, 0.5, 0.16, false);
        m.seat(0, 0.78, -0.5, true);
        return m;
    }

    private static VehicleModel buildRacingKart() {
        VehicleModel m = new VehicleModel();
        m.box("floor", "RED_CONCRETE", 0, 0.25, 0, 1.2, 0.12, 1.7);
        m.box("nose", "RED_CONCRETE", 0, 0.32, 0.8, 0.7, 0.2, 0.4);
        m.box("pod_l", "RED_CONCRETE", 0.55, 0.3, -0.2, 0.25, 0.25, 0.9);
        m.box("pod_r", "RED_CONCRETE", -0.55, 0.3, -0.2, 0.25, 0.25, 0.9);
        m.box("back", "BLACK_CONCRETE", 0, 0.5, -0.75, 0.7, 0.5, 0.15);
        m.box("swheel", "BLACK_CONCRETE", 0, 0.5, 0.35, 0.4, 0.08, 0.08);
        m.box("engine", "GRAY_CONCRETE", 0.3, 0.4, -0.7, 0.3, 0.3, 0.3);
        m.box("wing", "BLACK_CONCRETE", 0, 0.75, -0.9, 1.1, 0.06, 0.3);
        m.lamp("tail", "RED_CONCRETE", 0, 0.35, -0.87, 0.3, 0.1, 0.05, PartFlag.BRAKELIGHT);
        m.text("num", "7", 0, 0.35, 1.01, 0.4, 0);
        m.wheel("wheel_fl", 0.6, 0.18, 0.6, 0.36, 0.24, true);
        m.wheel("wheel_fr", -0.6, 0.18, 0.6, 0.36, 0.24, true);
        m.wheel("wheel_rl", 0.6, 0.18, -0.6, 0.36, 0.28, false);
        m.wheel("wheel_rr", -0.6, 0.18, -0.6, 0.36, 0.28, false);
        m.seat(0, 0.35, -0.35, true);
        return m;
    }

    // ------------------------------------------------------------------
    // Utility / construction
    // ------------------------------------------------------------------

    private static VehicleModel buildTractor() {
        VehicleModel m = new VehicleModel();
        m.box("hood", "RED_CONCRETE", 0, 0.9, 1.1, 1.2, 0.7, 1.4);
        m.box("grill", "BLACK_CONCRETE", 0, 0.9, 1.82, 1.0, 0.5, 0.08);
        m.box("cab", "GLASS", 0, 1.5, -0.7, 1.6, 0.9, 1.5);
        m.box("roof", "RED_CONCRETE", 0, 2.0, -0.7, 1.7, 0.12, 1.6);
        m.box("fend_l", "RED_CONCRETE", 1.0, 1.2, -1.0, 0.3, 0.15, 1.4);
        m.box("fend_r", "RED_CONCRETE", -1.0, 1.2, -1.0, 0.3, 0.15, 1.4);
        m.box("stack", "BLACK_CONCRETE", 0.4, 1.5, 1.3, 0.15, 0.8, 0.15);
        m.box("hitch", "BLACK_CONCRETE", 0, 0.4, -1.8, 0.15, 0.15, 0.8);
        m.slider("mower", "LIME_CONCRETE", "attach", 0, 0.5, -2.2, 1.8, 0.3, 0.5);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.4, 1.0, 1.83, 0.25, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.4, 1.0, 1.83, 0.25, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 1.2, -1.48, 0.3, 0.2, 0.08, PartFlag.BRAKELIGHT);
        m.wheel("wheel_rl", 0.95, 0.65, -0.9, 1.3, 0.4, false);
        m.wheel("wheel_rr", -0.95, 0.65, -0.9, 1.3, 0.4, false);
        m.wheel("wheel_fl", 0.8, 0.35, 1.3, 0.7, 0.3, true);
        m.wheel("wheel_fr", -0.8, 0.35, 1.3, 0.7, 0.3, true);
        m.seat(0, 1.05, -0.7, true);
        return m;
    }

    private static VehicleModel buildForklift() {
        VehicleModel m = new VehicleModel();
        m.box("body", "YELLOW_CONCRETE", 0, 0.55, 0, 1.3, 0.6, 2.2);
        m.box("roof", "BLACK_CONCRETE", 0, 1.75, -0.3, 1.2, 0.1, 1.2);
        m.box("post_fl", "BLACK_CONCRETE", 0.55, 1.2, 0.2, 0.08, 1.0, 0.08);
        m.box("post_fr", "BLACK_CONCRETE", -0.55, 1.2, 0.2, 0.08, 1.0, 0.08);
        m.box("post_rl", "BLACK_CONCRETE", 0.55, 1.2, -0.8, 0.08, 1.0, 0.08);
        m.box("post_rr", "BLACK_CONCRETE", -0.55, 1.2, -0.8, 0.08, 1.0, 0.08);
        m.box("mast_l", "BLACK_CONCRETE", 0.45, 1.0, 1.05, 0.12, 1.8, 0.12);
        m.box("mast_r", "BLACK_CONCRETE", -0.45, 1.0, 1.05, 0.12, 1.8, 0.12);
        m.box("beacon", "ORANGE_CONCRETE", 0, 1.9, -0.3, 0.2, 0.15, 0.2);
        m.slider("forkc", "GRAY_CONCRETE", "fork", 0, 0.6, 1.1, 1.0, 0.5, 0.15);
        m.slider("fork_l", "IRON_BLOCK", "fork", 0.3, 0.45, 1.6, 0.12, 0.08, 1.0);
        m.slider("fork_r", "IRON_BLOCK", "fork", -0.3, 0.45, 1.6, 0.12, 0.08, 1.0);
        m.slider("crate", "OAK_PLANKS", "fork", 0, 0.75, 1.6, 0.7, 0.6, 0.7);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.4, 1.5, 1.08, 0.2, 0.16, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.4, 1.5, 1.08, 0.2, 0.16, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 0.6, -1.12, 0.4, 0.16, 0.06, PartFlag.BRAKELIGHT);
        m.wheel("wheel_fl", 0.6, 0.28, 0.8, 0.56, 0.28, false);
        m.wheel("wheel_fr", -0.6, 0.28, 0.8, 0.56, 0.28, false);
        m.wheel("wheel_rl", 0.6, 0.28, -0.8, 0.56, 0.28, true);
        m.wheel("wheel_rr", -0.6, 0.28, -0.8, 0.56, 0.28, true);
        m.seat(0, 0.95, -0.3, true);
        return m;
    }

    private static VehicleModel buildExcavator() {
        VehicleModel m = new VehicleModel();
        m.track("l", 1.0, 0.4, 0, 0.55, 0.8, 8);
        m.track("r", -1.0, 0.4, 0, 0.55, 0.8, 8);
        m.box("frame", "YELLOW_CONCRETE", 0, 0.95, 0, 1.8, 0.3, 3.6);
        m.box("cab", "GLASS", 0.45, 1.5, 0.6, 0.9, 0.9, 1.2);
        m.box("cabroof", "YELLOW_CONCRETE", 0.45, 2.0, 0.6, 1.0, 0.12, 1.3);
        m.box("cw", "YELLOW_CONCRETE", 0, 1.35, -1.5, 1.8, 0.8, 1.0);
        m.box("stack", "BLACK_CONCRETE", -0.5, 1.8, -1.2, 0.15, 0.6, 0.15);
        m.hinge("boom", "YELLOW_CONCRETE", "boom", 0, 2.2, 1.6, 0.5, 0.5, 1.8, 0, 1.5, 1.0);
        m.hinge("stick", "YELLOW_CONCRETE", "stick", 0, 2.3, 2.9, 0.35, 0.35, 1.6, 0, 2.9, 2.3);
        m.hinge("bucket", "BLACK_CONCRETE", "bucket", 0, 1.4, 3.7, 0.8, 0.6, 0.7, 0, 1.7, 3.5);
        m.lamp("work", "LIGHT_GRAY_CONCRETE", 0.45, 1.7, 1.22, 0.3, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail", "RED_CONCRETE", 0, 1.2, -2.02, 0.4, 0.2, 0.06, PartFlag.BRAKELIGHT);
        m.seat(0.45, 1.15, 0.6, true);
        return m;
    }

    private static VehicleModel buildBulldozer() {
        VehicleModel m = new VehicleModel();
        m.track("l", 1.1, 0.45, 0, 0.6, 0.9, 8);
        m.track("r", -1.1, 0.45, 0, 0.6, 0.9, 8);
        m.box("body", "YELLOW_CONCRETE", 0, 1.15, -0.3, 1.9, 0.9, 3.4);
        m.box("cab", "GLASS", 0, 1.95, -0.8, 1.5, 0.8, 1.4);
        m.box("roof", "YELLOW_CONCRETE", 0, 2.4, -0.8, 1.6, 0.12, 1.5);
        m.box("hood", "YELLOW_CONCRETE", 0, 1.3, 1.3, 1.7, 0.7, 1.2);
        m.box("blade", "GRAY_CONCRETE", 0, 0.6, 2.5, 2.8, 0.9, 0.25);
        m.box("arm_l", "BLACK_CONCRETE", 0.8, 0.6, 1.9, 0.2, 0.2, 1.2);
        m.box("arm_r", "BLACK_CONCRETE", -0.8, 0.6, 1.9, 0.2, 0.2, 1.2);
        m.box("rip", "BLACK_CONCRETE", 0, 0.5, -2.2, 1.6, 0.6, 0.2);
        m.box("stack", "BLACK_CONCRETE", 0.5, 1.9, 1.3, 0.15, 0.7, 0.15);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.5, 1.5, 1.92, 0.25, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.5, 1.5, 1.92, 0.25, 0.2, 0.08, PartFlag.HEADLIGHT);
        m.seat(0, 1.55, -0.8, true);
        return m;
    }

    // ------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------

    private static VehicleModel buildSpeedboat() {
        VehicleModel m = new VehicleModel();
        m.box("hull", "WHITE_CONCRETE", 0, 0.35, 0, 1.9, 0.7, 4.6);
        m.box("bow", "WHITE_CONCRETE", 0, 0.5, 2.5, 1.2, 0.5, 0.8);
        m.box("stripe_l", "RED_CONCRETE", 0.96, 0.45, 0, 0.06, 0.2, 4.4);
        m.box("stripe_r", "RED_CONCRETE", -0.96, 0.45, 0, 0.06, 0.2, 4.4);
        m.box("deck", "OAK_PLANKS", 0, 0.75, -0.5, 1.7, 0.1, 3.4);
        m.box("shield", "GLASS", 0, 1.0, 0.9, 1.5, 0.4, 0.08);
        m.box("console", "BLACK_CONCRETE", 0, 0.9, 0.5, 1.2, 0.3, 0.3);
        m.box("motor", "BLACK_CONCRETE", 0, 0.55, -2.4, 0.5, 0.7, 0.4);
        m.box("prop", "IRON_BLOCK", 0, 0.1, -2.55, 0.3, 0.3, 0.1);
        m.lamp("nav_l", "RED_CONCRETE", 0.95, 0.85, 0.8, 0.12, 0.12, 0.12, PartFlag.HEADLIGHT);
        m.lamp("nav_r", "GREEN_CONCRETE", -0.95, 0.85, 0.8, 0.12, 0.12, 0.12, PartFlag.HEADLIGHT);
        m.seat(0.45, 0.8, 0.1, true);
        m.seat(-0.45, 0.8, 0.1, false);
        m.seatRow(0.8, -1.0, 0.45, -0.45);
        return m;
    }

    private static VehicleModel buildFishingBoat() {
        VehicleModel m = new VehicleModel();
        m.box("hull", "BLUE_CONCRETE", 0, 0.4, 0, 2.1, 0.8, 5.0);
        m.box("rim", "OAK_PLANKS", 0, 0.85, 0, 2.2, 0.15, 5.1);
        m.box("deck", "OAK_PLANKS", 0, 0.55, -0.5, 1.9, 0.1, 3.8);
        m.box("cabin", "WHITE_CONCRETE", 0, 1.1, 1.2, 1.6, 0.9, 1.4);
        m.box("cwin_l", "GLASS", 0.81, 1.25, 1.2, 0.06, 0.4, 1.0);
        m.box("cwin_r", "GLASS", -0.81, 1.25, 1.2, 0.06, 0.4, 1.0);
        m.box("shield", "GLASS", 0, 1.25, 1.92, 1.4, 0.4, 0.06);
        m.box("cabroof", "BLUE_CONCRETE", 0, 1.6, 1.2, 1.7, 0.1, 1.5);
        m.box("mast", "OAK_FENCE", 0, 1.8, 0.2, 1, 1.2, 1);
        m.itemOriented("rod1", "STICK", 0.8, 1.3, -0.5, 0.15, 1.4, 0.15, 0, -20, 0);
        m.itemOriented("rod2", "STICK", -0.8, 1.3, -0.5, 0.15, 1.4, 0.15, 0, -20, 0);
        m.box("crate1", "BARREL", -0.6, 0.85, -1.5, 0.6, 0.6, 0.6);
        m.box("crate2", "CHEST", 0.6, 0.8, -1.8, 0.7, 0.5, 0.5);
        m.box("lamp", "LANTERN", 0, 1.75, 1.2, 0.5, 0.5, 0.5);
        m.box("motor", "BLACK_CONCRETE", 0, 0.5, -2.6, 0.5, 0.7, 0.4);
        m.lamp("nav_l", "RED_CONCRETE", 1.0, 0.95, 1.0, 0.12, 0.12, 0.12, PartFlag.HEADLIGHT);
        m.lamp("nav_r", "GREEN_CONCRETE", -1.0, 0.95, 1.0, 0.12, 0.12, 0.12, PartFlag.HEADLIGHT);
        m.seat(0, 0.8, 1.0, true);
        m.seat(-0.6, 0.65, -0.3, false);
        m.seat(0.6, 0.65, -0.3, false);
        m.seat(0, 0.65, -1.2, false);
        return m;
    }

    private static VehicleModel buildYacht() {
        VehicleModel m = new VehicleModel();
        m.box("hull", "WHITE_CONCRETE", 0, 0.5, 0, 3.2, 1.0, 9.4);
        m.box("bow", "WHITE_CONCRETE", 0, 0.65, 5.0, 2.0, 0.7, 1.0);
        m.box("stripe_l", "BLUE_CONCRETE", 1.61, 0.25, 0, 0.06, 0.2, 9.2);
        m.box("stripe_r", "BLUE_CONCRETE", -1.61, 0.25, 0, 0.06, 0.2, 9.2);
        m.box("deck", "OAK_PLANKS", 0, 1.05, -0.5, 3.0, 0.1, 8.0);
        m.box("cabin", "WHITE_CONCRETE", 0, 1.7, 0.5, 2.6, 1.1, 4.5);
        m.box("cwin_l", "GLASS", 1.31, 1.8, 0.5, 0.06, 0.5, 4.0);
        m.box("cwin_r", "GLASS", -1.31, 1.8, 0.5, 0.06, 0.5, 4.0);
        m.box("shield", "GLASS", 0, 1.8, 2.78, 2.2, 0.5, 0.08);
        m.box("fly", "OAK_PLANKS", 0, 2.3, -0.5, 2.4, 0.1, 2.5);
        m.box("rail_f", "WHITE_CONCRETE", 0, 2.75, 0.75, 2.4, 0.08, 0.08);
        m.box("rail_b", "WHITE_CONCRETE", 0, 2.75, -1.75, 2.4, 0.08, 0.08);
        m.box("rail_l", "WHITE_CONCRETE", 1.2, 2.75, -0.5, 0.08, 0.08, 2.5);
        m.box("rail_r", "WHITE_CONCRETE", -1.2, 2.75, -0.5, 0.08, 0.08, 2.5);
        m.box("mast", "WHITE_CONCRETE", 0, 2.8, -1.2, 0.15, 1.0, 0.15);
        m.itemSized("radar", "WHITE_CONCRETE", 0, 3.35, -1.2, 1.0, 0.08, 0.15, PartFlag.ROTOR);
        m.box("pad_l", "RED_CONCRETE", 0.7, 1.15, 3.2, 0.9, 0.12, 1.6);
        m.box("pad_r", "RED_CONCRETE", -0.7, 1.15, 3.2, 0.9, 0.12, 1.6);
        m.box("table", "OAK_PLANKS", 0, 1.35, -3.5, 0.8, 0.5, 0.8);
        m.box("swim", "OAK_PLANKS", 0, 0.55, -5.0, 2.4, 0.15, 0.8);
        m.lamp("nav_l", "RED_CONCRETE", 1.5, 1.4, 2.0, 0.14, 0.14, 0.14, PartFlag.HEADLIGHT);
        m.lamp("nav_r", "GREEN_CONCRETE", -1.5, 1.4, 2.0, 0.14, 0.14, 0.14, PartFlag.HEADLIGHT);
        m.seat(0.6, 1.2, 1.8, true);
        m.seat(-0.6, 1.2, 1.8, false);
        m.seatRow(1.1, -2.5, 1.0, -1.0);
        m.seatRow(1.1, -3.3, 1.0, -1.0);
        m.seat(0, 2.35, -0.5, false);
        m.seat(0.7, 2.35, -1.0, false);
        return m;
    }

    // ------------------------------------------------------------------
    // Air
    // ------------------------------------------------------------------

    private static VehicleModel buildHelicopter() {
        VehicleModel m = new VehicleModel();
        m.box("body", "RED_CONCRETE", 0, 1.0, 0.3, 1.9, 1.1, 3.2);
        m.box("nose", "GLASS", 0, 1.0, 2.0, 1.6, 0.8, 0.5);
        m.box("stripe_l", "WHITE_CONCRETE", 0.96, 1.0, 0.3, 0.05, 0.25, 3.0);
        m.box("stripe_r", "WHITE_CONCRETE", -0.96, 1.0, 0.3, 0.05, 0.25, 3.0);
        m.box("boom", "RED_CONCRETE", 0, 1.15, -2.5, 0.5, 0.5, 3.0);
        m.box("fin", "RED_CONCRETE", 0, 1.7, -3.9, 0.15, 1.0, 0.6);
        m.box("mast", "BLACK_CONCRETE", 0, 1.7, 0.3, 0.2, 0.4, 0.2);
        m.itemSized("rotor", "BLACK_CONCRETE", 0, 1.95, 0.3, 7.5, 0.06, 0.35, PartFlag.ROTOR);
        m.itemOriented("rotor2", "BLACK_CONCRETE", 0, 1.95, 0.3, 7.5, 0.06, 0.35, 90, 0, 0, PartFlag.ROTOR);
        m.itemSized("tail", "GRAY_CONCRETE", 0.3, 1.7, -3.9, 0.08, 1.4, 0.15, PartFlag.ROTOR);
        m.box("skid_l", "BLACK_CONCRETE", 0.9, 0.15, 0.3, 0.12, 0.12, 2.6);
        m.box("skid_r", "BLACK_CONCRETE", -0.9, 0.15, 0.3, 0.12, 0.12, 2.6);
        m.box("strut_fl", "BLACK_CONCRETE", 0.9, 0.32, 1.2, 0.1, 0.4, 0.1);
        m.box("strut_fr", "BLACK_CONCRETE", -0.9, 0.32, 1.2, 0.1, 0.4, 0.1);
        m.box("strut_rl", "BLACK_CONCRETE", 0.9, 0.32, -0.6, 0.1, 0.4, 0.1);
        m.box("strut_rr", "BLACK_CONCRETE", -0.9, 0.32, -0.6, 0.1, 0.4, 0.1);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 0.62, 2.1, 0.3, 0.2, 0.1, PartFlag.HEADLIGHT);
        m.lamp("beacon", "RED_CONCRETE", 0, 1.6, -0.5, 0.2, 0.15, 0.2, PartFlag.HEADLIGHT);
        m.lamp("tail_light", "WHITE_CONCRETE", 0, 1.15, -4.02, 0.15, 0.15, 0.08, PartFlag.HEADLIGHT);
        m.seat(0.45, 0.75, 0.9, true);
        m.seat(-0.45, 0.75, 0.9, false);
        m.seatRow(0.75, -0.6, 0.45, -0.45);
        return m;
    }

    private static VehicleModel buildPlane() {
        VehicleModel m = new VehicleModel();
        m.box("fuse", "WHITE_CONCRETE", 0, 1.0, 0, 1.2, 1.2, 6.0);
        m.box("nose", "RED_CONCRETE", 0, 1.0, 3.1, 1.0, 1.0, 0.5);
        m.box("spin", "RED_CONCRETE", 0, 1.0, 3.5, 0.3, 0.3, 0.3);
        m.itemSized("prop", "BLACK_CONCRETE", 0, 1.0, 3.45, 0.25, 2.4, 0.12, PartFlag.ROTOR);
        m.box("glass", "GLASS", 0, 1.55, 0.8, 1.0, 0.7, 1.6);
        m.box("wing", "WHITE_CONCRETE", 0, 1.15, 0.6, 8.0, 0.12, 1.4);
        m.box("tip_l", "RED_CONCRETE", 3.95, 1.15, 0.6, 0.2, 0.14, 1.4);
        m.box("tip_r", "RED_CONCRETE", -3.95, 1.15, 0.6, 0.2, 0.14, 1.4);
        m.box("stripe_l", "RED_CONCRETE", 0.61, 1.0, 0, 0.04, 0.25, 5.8);
        m.box("stripe_r", "RED_CONCRETE", -0.61, 1.0, 0, 0.04, 0.25, 5.8);
        m.box("hstab", "WHITE_CONCRETE", 0, 1.1, -2.8, 3.0, 0.1, 0.9);
        m.box("fin", "RED_CONCRETE", 0, 1.6, -2.8, 0.12, 1.0, 0.9);
        m.box("strut_n", "BLACK_CONCRETE", 0, 0.35, 2.2, 0.1, 0.4, 0.1);
        m.box("strut_l", "BLACK_CONCRETE", 0.8, 0.4, -0.2, 0.1, 0.5, 0.1);
        m.box("strut_r", "BLACK_CONCRETE", -0.8, 0.4, -0.2, 0.1, 0.5, 0.1);
        m.lamp("nav_l", "RED_CONCRETE", 3.9, 1.3, 0.6, 0.15, 0.15, 0.15, PartFlag.HEADLIGHT);
        m.lamp("nav_r", "GREEN_CONCRETE", -3.9, 1.3, 0.6, 0.15, 0.15, 0.15, PartFlag.HEADLIGHT);
        m.lamp("land", "LIGHT_GRAY_CONCRETE", 0, 0.9, 2.0, 0.3, 0.2, 0.1, PartFlag.HEADLIGHT);
        m.wheel("wheel_n", 0, 0.3, 2.2, 0.6, 0.2, true);
        m.wheel("wheel_l", 0.8, 0.35, -0.2, 0.7, 0.25, false);
        m.wheel("wheel_r", -0.8, 0.35, -0.2, 0.7, 0.25, false);
        m.seat(0, 0.6, 0.9, true);
        m.seat(0, 0.6, -0.3, false);
        return m;
    }

    private static VehicleModel buildFighterJet() {
        VehicleModel m = new VehicleModel();
        m.box("fuse", "GRAY_CONCRETE", 0, 1.1, 0, 1.3, 1.1, 7.0);
        m.box("nose", "LIGHT_GRAY_CONCRETE", 0, 1.1, 3.9, 0.7, 0.7, 1.2);
        m.box("canopy", "TINTED_GLASS", 0, 1.75, 1.2, 0.9, 0.5, 1.8);
        m.box("wing_l", "GRAY_CONCRETE", 1.8, 1.0, 0.2, 2.6, 0.1, 1.6);
        m.box("wing_r", "GRAY_CONCRETE", -1.8, 1.0, 0.2, 2.6, 0.1, 1.6);
        m.box("wing2_l", "GRAY_CONCRETE", 3.6, 1.0, -0.6, 1.6, 0.1, 1.0);
        m.box("wing2_r", "GRAY_CONCRETE", -3.6, 1.0, -0.6, 1.6, 0.1, 1.0);
        m.box("fin_l", "GRAY_CONCRETE", 0.6, 1.9, -2.9, 0.12, 0.9, 0.8);
        m.box("fin_r", "GRAY_CONCRETE", -0.6, 1.9, -2.9, 0.12, 0.9, 0.8);
        m.box("hstab", "GRAY_CONCRETE", 0, 1.1, -3.0, 3.2, 0.1, 0.8);
        m.box("int_l", "BLACK_CONCRETE", 0.75, 1.0, 1.6, 0.3, 0.6, 1.2);
        m.box("int_r", "BLACK_CONCRETE", -0.75, 1.0, 1.6, 0.3, 0.6, 1.2);
        m.box("burn", "BLACK_CONCRETE", 0, 1.1, -3.6, 0.9, 0.7, 0.4);
        m.boxBright("glow", "GLOWSTONE", 0, 1.1, -3.78, 0.6, 0.5, 0.1);
        m.box("miss_l", "WHITE_CONCRETE", 2.2, 0.85, 0.2, 0.25, 0.25, 1.4);
        m.box("miss_r", "WHITE_CONCRETE", -2.2, 0.85, 0.2, 0.25, 0.25, 1.4);
        m.lamp("nav_l", "RED_CONCRETE", 4.3, 1.05, -0.6, 0.14, 0.14, 0.14, PartFlag.HEADLIGHT);
        m.lamp("nav_r", "GREEN_CONCRETE", -4.3, 1.05, -0.6, 0.14, 0.14, 0.14, PartFlag.HEADLIGHT);
        m.wheel("wheel_n", 0, 0.3, 2.6, 0.6, 0.2, true);
        m.wheel("wheel_l", 0.9, 0.35, -0.5, 0.7, 0.25, false);
        m.wheel("wheel_r", -0.9, 0.35, -0.5, 0.7, 0.25, false);
        m.seat(0, 1.0, 1.2, true);
        return m;
    }

    // ------------------------------------------------------------------
    // Rail
    // ------------------------------------------------------------------

    private static VehicleModel buildLocomotive() {
        VehicleModel m = new VehicleModel();
        m.box("frame", "BLACK_CONCRETE", 0, 0.7, 0, 2.2, 0.4, 6.6);
        m.box("boiler", "BLACK_CONCRETE", 0, 1.5, 0.8, 1.8, 1.2, 4.0);
        m.box("smoke", "BLACK_CONCRETE", 0, 1.5, 2.9, 1.7, 1.1, 0.5);
        m.box("chim", "BLACK_CONCRETE", 0, 2.4, 2.4, 0.5, 0.8, 0.5);
        m.box("cap", "RED_CONCRETE", 0, 2.85, 2.4, 0.7, 0.2, 0.7);
        m.box("dome", "GOLD_BLOCK", 0, 2.25, 1.4, 0.6, 0.4, 0.6);
        m.box("cab", "RED_CONCRETE", 0, 1.6, -2.3, 2.2, 1.8, 1.8);
        m.box("cwin_l", "GLASS", 1.11, 1.9, -2.3, 0.06, 0.6, 1.0);
        m.box("cwin_r", "GLASS", -1.11, 1.9, -2.3, 0.06, 0.6, 1.0);
        m.box("cabroof", "BLACK_CONCRETE", 0, 2.55, -2.3, 2.3, 0.12, 1.9);
        m.box("cow1", "RED_CONCRETE", 0, 0.4, 3.5, 2.0, 0.3, 0.4);
        m.box("cow2", "RED_CONCRETE", 0, 0.65, 3.35, 2.0, 0.25, 0.3);
        m.box("stripe", "RED_CONCRETE", 0, 1.0, 0.8, 1.85, 0.15, 4.0);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 2.0, 3.2, 0.5, 0.5, 0.2, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.7, 1.0, -3.22, 0.25, 0.25, 0.1, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.7, 1.0, -3.22, 0.25, 0.25, 0.1, PartFlag.BRAKELIGHT);
        m.wheel("drv1_l", 1.05, 0.65, -0.8, 1.3, 0.3, false);
        m.wheel("drv1_r", -1.05, 0.65, -0.8, 1.3, 0.3, false);
        m.wheel("drv2_l", 1.05, 0.65, 0.2, 1.3, 0.3, false);
        m.wheel("drv2_r", -1.05, 0.65, 0.2, 1.3, 0.3, false);
        m.wheel("drv3_l", 1.05, 0.65, 1.2, 1.3, 0.3, false);
        m.wheel("drv3_r", -1.05, 0.65, 1.2, 1.3, 0.3, false);
        m.wheel("pony_f_l", 1.05, 0.4, 2.6, 0.8, 0.25, false);
        m.wheel("pony_f_r", -1.05, 0.4, 2.6, 0.8, 0.25, false);
        m.wheel("pony_r_l", 1.05, 0.4, -2.6, 0.8, 0.25, false);
        m.wheel("pony_r_r", -1.05, 0.4, -2.6, 0.8, 0.25, false);
        m.seat(0.5, 1.0, -2.3, true);
        m.seat(-0.5, 1.0, -2.3, false);
        return m;
    }

    private static VehicleModel buildPassengerTrain() {
        VehicleModel m = new VehicleModel();
        m.box("nose", "LIGHT_BLUE_CONCRETE", 0, 1.3, 3.0, 2.0, 1.6, 1.2);
        m.box("body", "WHITE_CONCRETE", 0, 1.35, -0.3, 2.3, 1.7, 5.6);
        m.box("shield", "TINTED_GLASS", 0, 1.7, 3.45, 1.7, 0.6, 0.25);
        m.box("stripe_l", "BLUE_CONCRETE", 1.16, 1.1, -0.3, 0.05, 0.3, 5.4);
        m.box("stripe_r", "BLUE_CONCRETE", -1.16, 1.1, -0.3, 0.05, 0.3, 5.4);
        m.box("win_l", "GLASS", 1.16, 1.8, -0.3, 0.05, 0.5, 4.6);
        m.box("win_r", "GLASS", -1.16, 1.8, -0.3, 0.05, 0.5, 4.6);
        m.box("roof", "LIGHT_GRAY_CONCRETE", 0, 2.25, -0.3, 2.2, 0.12, 5.5);
        m.box("panto", "BLACK_CONCRETE", 0, 2.6, -1.0, 1.4, 0.3, 0.8);
        m.lamp("head_l", "LIGHT_GRAY_CONCRETE", 0.6, 0.9, 3.61, 0.3, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("head_r", "LIGHT_GRAY_CONCRETE", -0.6, 0.9, 3.61, 0.3, 0.24, 0.08, PartFlag.HEADLIGHT);
        m.lamp("tail_l", "RED_CONCRETE", 0.6, 0.9, -3.11, 0.3, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.lamp("tail_r", "RED_CONCRETE", -0.6, 0.9, -3.11, 0.3, 0.24, 0.08, PartFlag.BRAKELIGHT);
        m.text("line", "EXPRESS", 0, 2.25, 3.62, 0.6, 0);
        m.wheel("bog1a_l", 1.0, 0.4, 2.0, 0.8, 0.25, false);
        m.wheel("bog1a_r", -1.0, 0.4, 2.0, 0.8, 0.25, false);
        m.wheel("bog1b_l", 1.0, 0.4, 2.8, 0.8, 0.25, false);
        m.wheel("bog1b_r", -1.0, 0.4, 2.8, 0.8, 0.25, false);
        m.wheel("bog2a_l", 1.0, 0.4, -2.0, 0.8, 0.25, false);
        m.wheel("bog2a_r", -1.0, 0.4, -2.0, 0.8, 0.25, false);
        m.wheel("bog2b_l", 1.0, 0.4, -2.8, 0.8, 0.25, false);
        m.wheel("bog2b_r", -1.0, 0.4, -2.8, 0.8, 0.25, false);
        m.seat(0, 1.0, 2.4, true);
        m.seatRow(1.0, 1.2, 0.55, -0.55);
        m.seatRow(1.0, 0.0, 0.55, -0.55);
        m.seat(0, 1.0, -1.2, false);
        return m;
    }

    private static VehicleModel buildCarriage(String color) {
        VehicleModel m = new VehicleModel();
        m.box("body", color, 0, 1.35, 0, 2.3, 1.7, 5.8);
        m.box("stripe_l", "BLUE_CONCRETE", 1.16, 1.1, 0, 0.05, 0.3, 5.6);
        m.box("stripe_r", "BLUE_CONCRETE", -1.16, 1.1, 0, 0.05, 0.3, 5.6);
        m.box("win_l", "GLASS", 1.16, 1.8, 0, 0.05, 0.5, 4.8);
        m.box("win_r", "GLASS", -1.16, 1.8, 0, 0.05, 0.5, 4.8);
        m.box("roof", "LIGHT_GRAY_CONCRETE", 0, 2.25, 0, 2.2, 0.12, 5.7);
        m.box("door_l", "GLASS", 1.16, 1.2, 0, 0.06, 1.3, 1.0);
        m.box("door_r", "GLASS", -1.16, 1.2, 0, 0.06, 1.3, 1.0);
        m.wheel("bog1a_l", 1.0, 0.4, 1.6, 0.8, 0.25, false);
        m.wheel("bog1a_r", -1.0, 0.4, 1.6, 0.8, 0.25, false);
        m.wheel("bog1b_l", 1.0, 0.4, 2.4, 0.8, 0.25, false);
        m.wheel("bog1b_r", -1.0, 0.4, 2.4, 0.8, 0.25, false);
        m.wheel("bog2a_l", 1.0, 0.4, -1.6, 0.8, 0.25, false);
        m.wheel("bog2a_r", -1.0, 0.4, -1.6, 0.8, 0.25, false);
        m.wheel("bog2b_l", 1.0, 0.4, -2.4, 0.8, 0.25, false);
        m.wheel("bog2b_r", -1.0, 0.4, -2.4, 0.8, 0.25, false);
        m.seatRow(1.0, 1.5, 0.55, -0.55);
        m.seatRow(1.0, 0.0, 0.55, -0.55);
        m.seatRow(1.0, -1.5, 0.55, -0.55);
        return m;
    }

    private static VehicleModel buildMinecartRacer() {
        VehicleModel m = new VehicleModel();
        m.box("tub", "BLACK_CONCRETE", 0, 0.6, 0, 1.3, 0.6, 1.8);
        m.box("rim", "GRAY_CONCRETE", 0, 0.95, 0, 1.4, 0.12, 1.9);
        m.box("stripe", "RED_CONCRETE", 0, 0.6, 0, 1.32, 0.15, 1.82);
        m.box("handle", "IRON_BLOCK", 0, 1.1, -1.0, 0.1, 0.5, 0.1);
        m.box("motor", "FURNACE", 0, 0.55, -0.75, 0.6, 0.5, 0.4);
        m.lamp("head", "LIGHT_GRAY_CONCRETE", 0, 0.7, 0.95, 0.3, 0.25, 0.1, PartFlag.HEADLIGHT);
        m.wheel("wheel_fl", 0.65, 0.25, 0.6, 0.5, 0.2, true);
        m.wheel("wheel_fr", -0.65, 0.25, 0.6, 0.5, 0.2, true);
        m.wheel("wheel_rl", 0.65, 0.25, -0.6, 0.5, 0.2, false);
        m.wheel("wheel_rr", -0.65, 0.25, -0.6, 0.5, 0.2, false);
        m.seat(0, 0.7, -0.2, true);
        return m;
    }
}
