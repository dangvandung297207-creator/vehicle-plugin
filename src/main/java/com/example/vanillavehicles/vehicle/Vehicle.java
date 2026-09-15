package com.example.vanillavehicles.vehicle;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.animation.VehicleAnimator;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.event.VehicleDamageEvent;
import com.example.vanillavehicles.event.VehicleDestroyEvent;
import com.example.vanillavehicles.event.VehicleEnterEvent;
import com.example.vanillavehicles.event.VehicleExitEvent;
import com.example.vanillavehicles.event.VehicleMoveEvent;
import com.example.vanillavehicles.input.InputManager;
import com.example.vanillavehicles.input.InputState;
import com.example.vanillavehicles.model.ModelMath;
import com.example.vanillavehicles.model.ModelPart;
import com.example.vanillavehicles.model.PartFlag;
import com.example.vanillavehicles.physics.PhysicsEngine;
import com.example.vanillavehicles.seat.Seat;
import com.example.vanillavehicles.seat.SeatInstance;
import com.example.vanillavehicles.sound.SoundProfile;
import com.example.vanillavehicles.storage.CargoHolder;
import com.example.vanillavehicles.util.MessageUtil;
import com.example.vanillavehicles.util.ParticleUtil;
import com.example.vanillavehicles.util.SoundUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One live vehicle: physics state, model entities, seats, sounds, cargo,
 * carriages and animation channels. Tick-driven by the central update loop.
 */
public class Vehicle {

    /** Per-tick visual animation state (plain fields, no logic). */
    public static class AnimState {
        /** Accumulated travel distance in meters (wheels/tracks derive angles from it). */
        public double wheelSpin;
        public double steerVis;
        public double rotorAngle;
        public double trackPhase;
        public double turretYaw;
        public double barrelPitch;
        public int lightBits;
        public double rollApplied;
        public double bobOffset;
    }

    private final VanillaVehicles plugin;
    private final UUID id = UUID.randomUUID();
    private final VehicleType type;
    private final VehicleDefinition definition;
    private final VehicleStats stats;
    private final SoundProfile soundProfile;
    private final Location loc;

    private VehicleState state = VehicleState.ACTIVE;
    private double heading;
    private double pitchUp;
    private double rollVis;
    private double speed;
    private double throttle;
    private double targetSpeed;
    private double vy;
    private boolean grounded = true;
    private boolean inWater;
    private double health;
    private final UUID owner;
    private final String ownerName;

    private final List<SeatInstance> seatInstances = new ArrayList<>();
    private final Map<String, Display> parts = new LinkedHashMap<>();
    private Interaction hitbox;
    private Inventory cargo;
    private final TaxiMeter taxiMeter;
    private final List<TrainCarriage> carriages = new ArrayList<>();
    private final Deque<Location> pathHistory = new ArrayDeque<>();

    private final Map<String, Double> channelCurrent = new HashMap<>();
    private final Map<String, Double> channelTarget = new HashMap<>();
    private final Map<String, Long> pulseRevert = new HashMap<>();
    private final AnimState anim = new AnimState();

    private double lastSteer;
    private boolean braking;
    private boolean drifting;
    private boolean burnouting;
    private boolean lightsOn;
    private boolean sirenOn;
    private boolean boosting;
    private long boostEndTick;
    private long boostCooldownTick;
    private boolean exiting;

    private double lookYaw;
    private double lookPitch;
    private int trackHalfCount;
    private long lastRepairTick;

    public Vehicle(VanillaVehicles plugin, VehicleType type, VehicleDefinition definition,
                   VehicleStats stats, Location location, UUID owner, String ownerName) {
        this.plugin = plugin;
        this.type = type;
        this.definition = definition;
        this.stats = stats;
        this.soundProfile = new SoundProfile(stats);
        this.loc = location.clone();
        this.heading = location.getYaw();
        this.health = stats.maxHealth;
        this.owner = owner;
        this.ownerName = ownerName == null ? "Console" : ownerName;
        this.taxiMeter = type == VehicleType.TAXI
                ? new TaxiMeter(plugin.getPluginConfig().getTaxiRate()) : null;
    }

    // ------------------------------------------------------------------
    // Basic accessors
    // ------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public VehicleType getType() {
        return type;
    }

    public VehicleDefinition getDefinition() {
        return definition;
    }

    public VehicleStats getStats() {
        return stats;
    }

    public SoundProfile getSoundProfile() {
        return soundProfile;
    }

    public VehicleState getState() {
        return state;
    }

    /** Live origin location (ground contact point at the vehicle center). */
    public Location getLocation() {
        return loc;
    }

    public World getWorld() {
        return loc.getWorld();
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }

    public double getPitchUp() {
        return pitchUp;
    }

    public void setPitchUp(double pitchUp) {
        this.pitchUp = pitchUp;
    }

    public double getRollVis() {
        return rollVis;
    }

    public void setRollVis(double rollVis) {
        this.rollVis = rollVis;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getThrottle() {
        return throttle;
    }

    public void setThrottle(double throttle) {
        this.throttle = ModelMath.clamp(throttle, 0, 1);
    }

    public double getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public boolean isInWater() {
        return inWater;
    }

    public void setInWater(boolean inWater) {
        this.inWater = inWater;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(stats.maxHealth, health));
    }

    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public double getLookYaw() {
        return lookYaw;
    }

    public double getLookPitch() {
        return lookPitch;
    }

    public double getLastSteer() {
        return lastSteer;
    }

    public void setLastSteer(double lastSteer) {
        this.lastSteer = lastSteer;
    }

    public boolean isBraking() {
        return braking;
    }

    public void setBraking(boolean braking) {
        this.braking = braking;
    }

    public boolean isReversing() {
        return speed < -0.5;
    }

    public boolean isDrifting() {
        return drifting;
    }

    public void setDrifting(boolean drifting) {
        this.drifting = drifting;
    }

    public boolean isBurnouting() {
        return burnouting;
    }

    public void setBurnouting(boolean burnouting) {
        this.burnouting = burnouting;
    }

    public boolean isLightsOn() {
        return lightsOn;
    }

    public boolean isSirenOn() {
        return sirenOn;
    }

    public boolean isBoosting() {
        return boosting;
    }

    public boolean isExiting() {
        return exiting;
    }

    public AnimState getAnim() {
        return anim;
    }

    public Map<String, Display> getParts() {
        return parts;
    }

    public List<SeatInstance> getSeatInstances() {
        return seatInstances;
    }

    public List<TrainCarriage> getCarriages() {
        return carriages;
    }

    public int getTrackHalfCount() {
        return trackHalfCount;
    }

    public TaxiMeter getTaxiMeter() {
        return taxiMeter;
    }

    public Inventory getCargo() {
        return cargo;
    }

    public boolean useMouseSteering() {
        return plugin.getPluginConfig().isMouseSteering();
    }

    // ------------------------------------------------------------------
    // Spawn / tick / remove
    // ------------------------------------------------------------------

    public void spawn() {
        World world = getWorld();
        if (world == null) {
            return;
        }
        pathHistory.clear();
        Location origin = originLoc();
        for (ModelPart part : definition.getModel().getParts()) {
            Display display = DisplayFactory.spawnPart(plugin, this, part, origin);
            if (display != null) {
                parts.put(part.name, display);
            }
        }
        int tracks = 0;
        for (ModelPart part : definition.getModel().getParts()) {
            if (part.has(PartFlag.TRACK)) {
                tracks++;
            }
        }
        trackHalfCount = Math.max(1, tracks / 2);

        int index = 0;
        for (Seat seat : definition.getModel().getSeats()) {
            ArmorStand stand = DisplayFactory.spawnSeat(plugin, this, index, origin);
            SeatInstance instance = new SeatInstance(seat, index);
            instance.stand = stand;
            seatInstances.add(instance);
            index++;
        }
        hitbox = DisplayFactory.spawnHitbox(plugin, this, origin);
        if (stats.cargoSize > 0) {
            createCargo();
        }
        if (stats.carriages > 0 && definition.getCarriageModel() != null) {
            for (int i = 0; i < stats.carriages; i++) {
                TrainCarriage carriage = new TrainCarriage(plugin, this, i, definition.getCarriageModel());
                carriage.spawn();
                carriage.place(behindLoc(i), heading);
                carriages.add(carriage);
            }
        }
        updateTransforms();
        plugin.getVehicleManager().indexVehicle(this);
    }

    private Location behindLoc(int carriageIndex) {
        double yawRad = Math.toRadians(heading);
        double dist = TrainCarriage.SPACING * (carriageIndex + 1);
        return new Location(getWorld(), loc.getX() + Math.sin(yawRad) * dist,
                loc.getY(), loc.getZ() - Math.cos(yawRad) * dist, (float) heading, 0f);
    }

    @SuppressWarnings("deprecation")
    private void createCargo() {
        int size = Math.max(9, Math.min(54, ((stats.cargoSize + 8) / 9) * 9));
        CargoHolder holder = new CargoHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, size, Component.text("Vehicle Cargo"));
        holder.setInventory(inventory);
        cargo = inventory;
    }

    private Location originLoc() {
        return new Location(getWorld(), loc.getX(), loc.getY(), loc.getZ(),
                (float) heading, (float) -pitchUp);
    }

    /** Main per-tick update, called by the central loop. */
    public void tick(long now) {
        if (state == VehicleState.DESTROYED) {
            return;
        }
        World world = getWorld();
        if (world == null) {
            return;
        }
        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }
        validateRiders();
        Player driver = getDriver();
        InputState input = driver == null ? null : plugin.getInputManager().getState(driver);
        if (input != null && input.hasLook) {
            lookYaw = input.lookYaw;
            lookPitch = input.lookPitch;
        }
        updateBoost(now);

        boolean parked = driver == null && Math.abs(speed) < 0.05
                && vy == 0 && !sirenOn && channelsSettled();
        if (!parked) {
            Location before = loc.clone();
            PhysicsEngine.update(this, input, 0.05);
            if (before.getWorld() == world && loc.distanceSquared(before) > 0.000001) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(this, before, loc.clone()));
                if (taxiMeter != null) {
                    taxiMeter.addDistance(Math.sqrt(loc.distanceSquared(before)));
                }
            }
            pushHistory();
            updateCarriages();
            updateChannels();
            VehicleAnimator.update(this, input, now);
            updateTransforms();
            plugin.getSoundManager().update(this, driver);
            if (input != null) {
                input.consumeJumpPulse();
            }
        }
        if (now - lastRepairTick > 100) {
            lastRepairTick = now;
            repair();
        }
        if (driver != null && plugin.isDebug(driver)) {
            driver.sendActionBar(Component.text(debugLine()));
        }
    }

    private void updateBoost(long now) {
        if (boosting && now >= boostEndTick) {
            boosting = false;
            boostCooldownTick = now + stats.boostCooldownTicks;
        }
    }

    public long boostCooldownLeft() {
        long left = boostCooldownTick - plugin.getVehicleManager().getTick();
        return Math.max(0, left);
    }

    private void validateRiders() {
        VehicleManager manager = plugin.getVehicleManager();
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider == null) {
                continue;
            }
            if (!rider.isOnline() || rider.isDead()) {
                try {
                    rider.leaveVehicle();
                } catch (Exception ignored) {
                }
                manager.clearRider(rider);
            } else {
                manager.setRider(rider, this);
            }
        }
    }

    /** Teleports every entity of this vehicle to its current transform. */
    public void updateTransforms() {
        World world = getWorld();
        if (world == null) {
            return;
        }
        Location origin = originLoc();
        for (Display display : parts.values()) {
            if (display != null && display.isValid()) {
                display.teleport(origin);
            }
        }
        double scale = stats.modelScale;
        for (SeatInstance seat : seatInstances) {
            if (seat.stand == null || !seat.stand.isValid()) {
                continue;
            }
            Vector3f off = seat.seat.offset;
            Vector3f rotated = ModelMath.toWorld(new Vector3f(
                    (float) (off.x * scale), (float) (off.y * scale), (float) (off.z * scale)),
                    heading, -pitchUp, rollVis);
            seat.stand.teleport(new Location(world, loc.getX() + rotated.x,
                    loc.getY() + rotated.y, loc.getZ() + rotated.z, (float) heading, 0f));
        }
        if (hitbox != null && hitbox.isValid()) {
            hitbox.teleport(new Location(world, loc.getX(), loc.getY() + 0.2, loc.getZ(),
                    (float) heading, 0f));
        }
    }

    private void pushHistory() {
        Location stamped = loc.clone();
        stamped.setYaw((float) heading);
        pathHistory.addFirst(stamped);
        while (pathHistory.size() > 320) {
            pathHistory.removeLast();
        }
    }

    private void updateCarriages() {
        if (carriages.isEmpty()) {
            return;
        }
        double step = Math.max(0.6, Math.abs(speed) * 0.05);
        int gap = (int) ModelMath.clamp(Math.round(TrainCarriage.SPACING / step), 4, 300);
        for (int i = 0; i < carriages.size(); i++) {
            TrainCarriage carriage = carriages.get(i);
            Location entry = historyAt((i + 1) * gap);
            if (entry == null) {
                entry = behindLoc(i);
            }
            carriage.place(entry, entry.getYaw());
            carriage.animate(anim.wheelSpin);
        }
    }

    private Location historyAt(int skip) {
        int i = 0;
        for (Location location : pathHistory) {
            if (i == skip) {
                return location;
            }
            i++;
        }
        return null;
    }

    /** Respawns entities that went missing. */
    private void repair() {
        World world = getWorld();
        if (world == null) {
            return;
        }
        Location origin = originLoc();
        boolean moved = false;
        for (ModelPart part : definition.getModel().getParts()) {
            Display display = parts.get(part.name);
            if (display == null || !display.isValid()) {
                Display replacement = DisplayFactory.spawnPart(plugin, this, part, origin);
                if (replacement != null) {
                    parts.put(part.name, replacement);
                    plugin.getVehicleManager().indexEntity(replacement, this);
                    moved = true;
                }
            }
        }
        for (SeatInstance seat : seatInstances) {
            if (seat.stand == null || !seat.stand.isValid()) {
                ArmorStand stand = DisplayFactory.spawnSeat(plugin, this, seat.index, origin);
                if (stand != null) {
                    seat.stand = stand;
                    plugin.getVehicleManager().indexEntity(stand, this);
                    moved = true;
                }
            }
        }
        if (hitbox == null || !hitbox.isValid()) {
            hitbox = DisplayFactory.spawnHitbox(plugin, this, origin);
            if (hitbox != null) {
                plugin.getVehicleManager().indexEntity(hitbox, this);
            }
        }
        for (TrainCarriage carriage : carriages) {
            carriage.repair();
        }
        if (moved) {
            updateTransforms();
        }
    }

    // ------------------------------------------------------------------
    // Seats
    // ------------------------------------------------------------------

    private List<SeatInstance> allSeats() {
        List<SeatInstance> all = new ArrayList<>(seatInstances);
        for (TrainCarriage carriage : carriages) {
            all.addAll(carriage.getSeats());
        }
        return all;
    }

    public Player getDriver() {
        for (SeatInstance seat : seatInstances) {
            if (seat.seat.driver) {
                return seat.getRider();
            }
        }
        return null;
    }

    public List<Player> getPassengers() {
        List<Player> passengers = new ArrayList<>();
        Player driver = getDriver();
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider != null && (driver == null || !rider.getUniqueId().equals(driver.getUniqueId()))) {
                passengers.add(rider);
            }
        }
        return passengers;
    }

    public int riderCount() {
        int count = 0;
        for (SeatInstance seat : allSeats()) {
            if (seat.getRider() != null) {
                count++;
            }
        }
        return count;
    }

    public boolean isDriver(Player player) {
        Player driver = getDriver();
        return player != null && driver != null && driver.getUniqueId().equals(player.getUniqueId());
    }

    public boolean isRider(Player player) {
        if (player == null) {
            return false;
        }
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider != null && rider.getUniqueId().equals(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public int seatIndexOf(Player player) {
        if (player == null) {
            return -1;
        }
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider != null && rider.getUniqueId().equals(player.getUniqueId())) {
                return seat.index;
            }
        }
        return -1;
    }

    public boolean enter(Player player) {
        if (state != VehicleState.ACTIVE || player == null || !player.isOnline() || player.isDead()) {
            return false;
        }
        if (!player.hasPermission("vehicle.use")) {
            MessageUtil.send(player, "&cYou are not allowed to use vehicles.");
            plugin.getSoundManager().deny(player);
            return false;
        }
        if (isRider(player)) {
            return true;
        }
        if (player.getVehicle() != null) {
            MessageUtil.send(player, "&cDismount first.");
            return false;
        }
        SeatInstance target = null;
        for (SeatInstance seat : seatInstances) {
            if (seat.seat.driver && seat.isFree() && seat.stand != null && seat.stand.isValid()) {
                target = seat;
                break;
            }
        }
        if (target == null) {
            for (SeatInstance seat : allSeats()) {
                if (!seat.seat.driver && seat.isFree() && seat.stand != null && seat.stand.isValid()) {
                    target = seat;
                    break;
                }
            }
        }
        if (target == null) {
            MessageUtil.send(player, "&cThis vehicle is full.");
            plugin.getSoundManager().deny(player);
            return false;
        }
        VehicleEnterEvent event = new VehicleEnterEvent(this, player, target.index);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        target.stand.addPassenger(player);
        plugin.getVehicleManager().setRider(player, this);
        InputState inputState = plugin.getInputManager().getState(player);
        inputState.reset();
        int slot = player.getInventory().getHeldItemSlot();
        if (slot >= 0 && slot < InputManager.GEARS.length) {
            inputState.gear = InputManager.GEARS[slot];
        }
        if (target.seat.driver) {
            MessageUtil.send(player, controlsHint());
            if (taxiMeter != null) {
                taxiMeter.start();
            }
        } else {
            MessageUtil.send(player, "&7You boarded the " + type.getDisplayName() + ". &7Press &eQ &7to get out.");
        }
        return true;
    }

    private String controlsHint() {
        if (plugin.getInputManager().isEnhancedAvailable()) {
            return "&aDriving: &fW/S &7throttle &fA/D &7steer &fSpace &7lift/boost &fShift&7/&fQ &7exit &fF &7lights &fLMB &7horn &fRMB &7special";
        }
        return "&aDriving: &fhotbar &7= gears &fmouse &7= steering &fsneak &7= brake &fShift(slow)/Q &7exit &fF &7lights &fLMB &7horn &fRMB &7special";
    }

    /**
     * Exits a rider. Returns false when the exit was cancelled by an event.
     */
    public boolean exit(Player player) {
        if (player == null || exiting) {
            return false;
        }
        if (state != VehicleState.ACTIVE) {
            forceLeave(player);
            return true;
        }
        if (!isRider(player)) {
            return true;
        }
        exiting = true;
        try {
            boolean wasDriver = isDriver(player);
            VehicleExitEvent event = new VehicleExitEvent(this, player, seatIndexOf(player));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            forceLeave(player);
            if (wasDriver) {
                throttle = 0;
                targetSpeed = 0;
                if (taxiMeter != null) {
                    taxiMeter.stop();
                }
            }
            return true;
        } finally {
            exiting = false;
        }
    }

    private void forceLeave(Player player) {
        try {
            if (player.getVehicle() != null) {
                player.leaveVehicle();
            }
        } catch (Exception ignored) {
        }
        plugin.getVehicleManager().clearRider(player);
        plugin.getInputManager().clearState(player);
    }

    public void exitAll() {
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider != null) {
                exit(rider);
            }
        }
    }

    // ------------------------------------------------------------------
    // Damage / destroy
    // ------------------------------------------------------------------

    public void damage(double amount, Player damager) {
        if (state != VehicleState.ACTIVE || amount <= 0) {
            return;
        }
        VehicleDamageEvent event = new VehicleDamageEvent(this, damager, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        health -= event.getDamage();
        Player driver = getDriver();
        if (driver != null && health < stats.maxHealth * 0.3) {
            MessageUtil.send(driver, "&cWarning: vehicle integrity critical!");
        }
        if (health <= 0) {
            health = 0;
            destroy();
        }
    }

    /** Crash feedback shared by every physics profile. */
    public void onCrash(double impact) {
        if (state != VehicleState.ACTIVE) {
            return;
        }
        if (Math.abs(speed) > 4) {
            speed = -speed * 0.2;
        } else {
            speed = 0;
        }
        vy = 0;
        plugin.getSoundManager().crash(this, impact);
        Location nose = getWorldLoc(new Vector3f(0, 0.8f,
                (float) (stats.length / 2.0)));
        ParticleUtil.spawn(getWorld(), "POOF", nose, 12, 0.4, 0.4, 0.4, 0.05);
        ParticleUtil.spawn(getWorld(), "CRIT", nose, 8, 0.4, 0.4, 0.4, 0.05);
        if (impact > 6) {
            double resist = type.getTier() == VehicleTier.HEAVY ? 0.45 : 1.0;
            if (type.getPhysics() == com.example.vanillavehicles.vehicle.PhysicsType.BIKE) {
                resist = 1.3;
            }
            damage((impact - 5) * 2.5 * resist, null);
        }
    }

    public void destroy() {
        if (state == VehicleState.DESTROYED) {
            return;
        }
        VehicleDestroyEvent event = new VehicleDestroyEvent(this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            health = Math.max(1, health);
            return;
        }
        state = VehicleState.DESTROYED;
        plugin.getSoundManager().destroy(this);
        Location center = getWorldLoc(new Vector3f(0, 1, 0));
        ParticleUtil.spawn(getWorld(), "EXPLOSION", center, 3, 0.6, 0.6, 0.6, 0.1);
        ParticleUtil.spawn(getWorld(), "LARGE_SMOKE", center, 20, 0.6, 0.6, 0.6, 0.08);
        ParticleUtil.spawn(getWorld(), "FLAME", center, 15, 0.6, 0.6, 0.6, 0.08);
        dropCargo();
        exitAll();
        removeEntities();
        plugin.getVehicleManager().unregister(this);
    }

    /** Quiet removal with cargo refund (commands, admin cleanup). */
    public void removeQuiet() {
        state = VehicleState.DESTROYED;
        dropCargo();
        exitAll();
        removeEntities();
        plugin.getVehicleManager().unregister(this);
    }

    /** Quiet removal for shutdown (keeps cargo for persistence). */
    public void removeSilent() {
        state = VehicleState.DESTROYED;
        for (SeatInstance seat : allSeats()) {
            Player rider = seat.getRider();
            if (rider != null) {
                forceLeave(rider);
            }
        }
        removeEntities();
        plugin.getVehicleManager().unregister(this);
    }

    private void removeEntities() {
        for (Display display : parts.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        parts.clear();
        for (SeatInstance seat : seatInstances) {
            if (seat.stand != null && seat.stand.isValid()) {
                seat.stand.remove();
            }
        }
        if (hitbox != null && hitbox.isValid()) {
            hitbox.remove();
        }
        hitbox = null;
        for (TrainCarriage carriage : carriages) {
            carriage.remove();
        }
    }

    public List<Entity> getEntities() {
        List<Entity> entities = new ArrayList<>();
        entities.addAll(parts.values());
        for (SeatInstance seat : seatInstances) {
            if (seat.stand != null) {
                entities.add(seat.stand);
            }
        }
        if (hitbox != null) {
            entities.add(hitbox);
        }
        for (TrainCarriage carriage : carriages) {
            entities.addAll(carriage.entities());
        }
        return entities;
    }

    // ------------------------------------------------------------------
    // Driver actions
    // ------------------------------------------------------------------

    public void horn() {
        plugin.getSoundManager().horn(this);
    }

    public void toggleLights() {
        lightsOn = !lightsOn;
        Player driver = getDriver();
        if (driver != null) {
            MessageUtil.send(driver, lightsOn ? "&eHeadlights on." : "&7Headlights off.");
            plugin.getSoundManager().click(driver);
        }
    }

    public void toggleSiren() {
        Player driver = getDriver();
        if (!stats.siren) {
            if (driver != null) {
                MessageUtil.send(driver, "&cThis vehicle has no siren.");
                plugin.getSoundManager().deny(driver);
            }
            return;
        }
        sirenOn = !sirenOn;
        if (driver != null) {
            MessageUtil.send(driver, sirenOn ? "&cSiren on." : "&7Siren off.");
        }
    }

    public void boost() {
        Player driver = getDriver();
        if (!stats.boostEnabled) {
            if (driver != null) {
                MessageUtil.send(driver, "&cThis vehicle has no boost.");
                plugin.getSoundManager().deny(driver);
            }
            return;
        }
        long left = boostCooldownLeft();
        if (boosting || left > 0) {
            if (driver != null) {
                MessageUtil.send(driver, "&cBoost recharging (" + (left / 20 + 1) + "s).");
                plugin.getSoundManager().deny(driver);
            }
            return;
        }
        boosting = true;
        boostEndTick = plugin.getVehicleManager().getTick() + stats.boostDurationTicks;
        plugin.getSoundManager().boost(this);
        if (driver != null) {
            MessageUtil.send(driver, "&b&lBOOST!");
        }
    }

    /** Right-click special, unique per vehicle family. */
    public void special() {
        Player driver = getDriver();
        switch (type) {
            case POLICE_CAR:
            case AMBULANCE:
            case FIRE_TRUCK:
                toggleSiren();
                break;
            case SPORTS_CAR:
            case MUSCLE_CAR:
            case RACING_KART:
            case FIGHTER_JET:
                boost();
                break;
            case TAXI:
                if (driver != null && taxiMeter != null) {
                    MessageUtil.send(driver, String.format("&eFare: &f%.2f &7(%d blocks)",
                            taxiMeter.fare(), (int) taxiMeter.getDistance()));
                }
                horn();
                break;
            case GARBAGE_TRUCK:
                pulseChannel("dump", 60);
                if (driver != null) {
                    MessageUtil.send(driver, "&aDumping container...");
                }
                break;
            case FORKLIFT:
                toggleChannel("fork");
                break;
            case EXCAVATOR:
                pulseChannel("dig", 50);
                break;
            case TRACTOR:
                toggleChannel("attach");
                if (driver != null) {
                    MessageUtil.send(driver, "&aAttachment toggled.");
                }
                break;
            case TANK:
                cannonFx();
                break;
            case BUS:
            case LOCOMOTIVE:
            case PASSENGER_TRAIN:
            case MINECART_RACER:
                plugin.getSoundManager().whistle(this);
                break;
            default:
                horn();
                break;
        }
    }

    /**
     * Named mechanism control for construction / emergency vehicles.
     *
     * @return true when the action applied to this vehicle
     */
    public boolean tool(String action) {
        Player driver = getDriver();
        String act = action == null ? "" : action.toLowerCase();
        switch (act) {
            case "ladder":
                if (type != VehicleType.FIRE_TRUCK) {
                    return false;
                }
                toggleChannel("ladder");
                if (driver != null) {
                    MessageUtil.send(driver, "&aLadder toggled.");
                }
                return true;
            case "fork":
                if (type != VehicleType.FORKLIFT) {
                    return false;
                }
                toggleChannel("fork");
                return true;
            case "dump":
                if (type != VehicleType.GARBAGE_TRUCK && type != VehicleType.CONSTRUCTION_TRUCK) {
                    return false;
                }
                pulseChannel("dump", 60);
                if (driver != null) {
                    MessageUtil.send(driver, "&aDumping...");
                }
                return true;
            case "arm":
                if (type != VehicleType.EXCAVATOR) {
                    return false;
                }
                toggleChannel("armup");
                return true;
            case "bucket":
                if (type != VehicleType.EXCAVATOR && type != VehicleType.BULLDOZER
                        && type != VehicleType.FORKLIFT) {
                    return false;
                }
                pulseChannel("dig", 50);
                return true;
            case "turret":
                if (type != VehicleType.TANK) {
                    return false;
                }
                toggleChannel("turretlock");
                if (driver != null) {
                    MessageUtil.send(driver, getChannel("turretlock") > 0.5
                            ? "&aTurret locked forward." : "&aTurret follows your view.");
                }
                return true;
            default:
                return false;
        }
    }

    /**
     * Harmless visual cannon: smoke puff, flash and a thump. No damage, no
     * projectiles - pure vehicle simulation.
     */
    private void cannonFx() {
        Location muzzle = getWorldLoc(new Vector3f(0, 1.9f, 3.4f));
        ParticleUtil.spawn(getWorld(), "LARGE_SMOKE", muzzle, 14, 0.3, 0.3, 0.3, 0.1);
        ParticleUtil.spawn(getWorld(), "FLAME", muzzle, 6, 0.2, 0.2, 0.2, 0.05);
        SoundUtil.play(getWorld(), "ENTITY_FIREWORK_ROCKET_BLAST", muzzle, 1.2f, 0.6f);
        pulseChannel("recoil", 12);
    }

    // ------------------------------------------------------------------
    // Animation channels
    // ------------------------------------------------------------------

    public double getChannel(String name) {
        return channelCurrent.getOrDefault(name, 0.0);
    }

    public void setChannelTarget(String name, double target) {
        channelTarget.put(name, ModelMath.clamp(target, 0, 1));
    }

    public void toggleChannel(String name) {
        double target = channelTarget.getOrDefault(name, 0.0);
        channelTarget.put(name, target > 0.5 ? 0.0 : 1.0);
    }

    public void pulseChannel(String name, int ticks) {
        channelTarget.put(name, 1.0);
        pulseRevert.put(name, plugin.getVehicleManager().getTick() + ticks);
    }

    private void updateChannels() {
        long now = plugin.getVehicleManager().getTick();
        for (Map.Entry<String, Double> entry : channelTarget.entrySet()) {
            String name = entry.getKey();
            double target = entry.getValue();
            Long revert = pulseRevert.get(name);
            if (revert != null && now >= revert) {
                target = 0;
                entry.setValue(0.0);
                pulseRevert.remove(name);
            }
            double current = channelCurrent.getOrDefault(name, 0.0);
            channelCurrent.put(name, ModelMath.approach(current, target, 0.035));
        }
    }

    private boolean channelsSettled() {
        if (!pulseRevert.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Double> entry : channelTarget.entrySet()) {
            double current = channelCurrent.getOrDefault(entry.getKey(), 0.0);
            if (Math.abs(current - entry.getValue()) > 0.01) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Cargo
    // ------------------------------------------------------------------

    public void openCargo(Player player) {
        if (cargo == null) {
            MessageUtil.send(player, "&cThis vehicle has no cargo space.");
            plugin.getSoundManager().deny(player);
            return;
        }
        player.openInventory(cargo);
        SoundUtil.play(player, "BLOCK_CHEST_OPEN", 0.8f, 1.0f);
    }

    private void dropCargo() {
        if (cargo == null || getWorld() == null) {
            return;
        }
        Location drop = loc.clone().add(0, 1, 0);
        for (ItemStack item : cargo.getContents()) {
            if (item != null) {
                getWorld().dropItemNaturally(drop, item);
            }
        }
        cargo.clear();
    }

    public List<ItemStack> getCargoContents() {
        List<ItemStack> items = new ArrayList<>();
        if (cargo != null) {
            for (ItemStack item : cargo.getContents()) {
                if (item != null) {
                    items.add(item.clone());
                }
            }
        }
        return items;
    }

    public void setCargoContents(List<ItemStack> items) {
        if (cargo == null || items == null) {
            return;
        }
        int slot = 0;
        for (ItemStack item : items) {
            if (slot >= cargo.getSize()) {
                break;
            }
            if (item != null) {
                cargo.setItem(slot, item);
            }
            slot++;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Converts a local model offset into a world location. */
    public Location getWorldLoc(Vector3f local) {
        double scale = stats.modelScale;
        Vector3f rotated = ModelMath.toWorld(new Vector3f(
                (float) (local.x * scale), (float) (local.y * scale), (float) (local.z * scale)),
                heading, -pitchUp, rollVis);
        return new Location(getWorld(), loc.getX() + rotated.x,
                loc.getY() + rotated.y, loc.getZ() + rotated.z, (float) heading, 0f);
    }

    public String debugLine() {
        Player driver = getDriver();
        return String.format("ID %s | %s | %.1f m/s | HP %.0f/%.0f | driver %s | riders %d | %s%s",
                id.toString().substring(0, 8), type.getId(), speed, health, stats.maxHealth,
                driver == null ? "-" : driver.getName(), riderCount(),
                grounded ? "ground" : "air", inWater ? "+water" : "");
    }
}
