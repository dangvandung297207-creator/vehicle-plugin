package com.wavemotorcycle.motorcycle;

import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.input.MotorcycleInput;
import com.wavemotorcycle.motorcycle.model.MotorcycleAnimationManager;
import com.wavemotorcycle.motorcycle.model.MotorcycleModel;
import com.wavemotorcycle.motorcycle.physics.MotorcyclePhysics;
import com.wavemotorcycle.motorcycle.physics.MotorcyclePhysics.StepResult;
import com.wavemotorcycle.motorcycle.physics.TerrainSurface;
import com.wavemotorcycle.motorcycle.sound.MotorcycleSoundManager;
import com.wavemotorcycle.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

/**
 * Runtime state and per-tick update for one spawned motorcycle.
 *
 * <p>The chassis reference point is the ground contact at the middle of the
 * wheelbase; {@code +Z} is forward, {@code +Y} up, and yaw follows Minecraft
 * conventions. The controller owns the physics step, input, engine/fuel/damage
 * state and delegates rendering to the {@link MotorcycleModel}.
 */
public final class MotorcycleController {

    public enum DismountReason { SNEAK_TAP, VANILLA, QUIT, DEATH, TELEPORT, DESTROYED }

    public static final double WHEELBASE = 1.35;
    public static final double HALF_WHEELBASE = WHEELBASE / 2.0;
    public static final double WHEEL_RADIUS = 0.33;

    private final MotorcycleManager manager;
    private final Motorcycle state;
    private World world;
    private final MotorcycleModel model;
    private final MotorcycleAnimationManager anim = new MotorcycleAnimationManager();
    private final MotorcycleSoundManager sounds;

    // Chassis kinematics
    private double x;
    private double y;
    private double z;
    private float yaw;
    private double speed;
    private double vy;
    private boolean airborne;
    private double wheelieAngle;
    private WheelieState wheelieState = WheelieState.NORMAL;
    private double steerAngle;
    private double wheelSpin; // degrees, wraps 0..360

    // Engine / fuel / damage
    private EngineState engine = EngineState.OFF;
    private int engineTicks;
    private double fuel;
    private double health;

    // Rider + input edges
    private Player rider;
    private boolean wasBraking;
    private boolean brakeLightActive;
    private int lastSprintUpTick = -100;
    private boolean pendingWheelieTap;
    private boolean brakingFlag;
    private boolean throttlingFlag;

    // Runtime bookkeeping
    private int tickCount;
    private boolean dirty;
    private boolean dormant;
    private boolean destroyed;
    private boolean visualDirty = true;

    public MotorcycleController(MotorcycleManager manager, Motorcycle state, World world,
                                double x, double y, double z, float yaw, ConfigManager cfg) {
        this.manager = manager;
        this.state = state;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.fuel = state.fuel();
        this.health = state.health();
        this.model = new MotorcycleModel(this, cfg);
        this.sounds = new MotorcycleSoundManager(cfg);
    }

    // ------------------------------------------------------------------
    // Accessors used by physics / model / animation
    // ------------------------------------------------------------------

    public Motorcycle state() {
        return state;
    }

    public MotorcycleManager manager() {
        return manager;
    }

    public World world() {
        return world;
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

    public float yaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Location location() {
        return new Location(world, x, y, z);
    }

    public double speed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double vy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public boolean airborne() {
        return airborne;
    }

    public void setAirborne(boolean airborne) {
        this.airborne = airborne;
    }

    public double wheelieAngle() {
        return wheelieAngle;
    }

    public void setWheelieAngle(double wheelieAngle) {
        this.wheelieAngle = wheelieAngle;
    }

    public WheelieState wheelieState() {
        return wheelieState;
    }

    public void setWheelieState(WheelieState wheelieState) {
        this.wheelieState = wheelieState;
    }

    public double steerAngle() {
        return steerAngle;
    }

    public void setSteerAngle(double steerAngle) {
        this.steerAngle = steerAngle;
    }

    public double wheelSpin() {
        return wheelSpin;
    }

    public EngineState engineState() {
        return engine;
    }

    public boolean engineRunning() {
        return engine == EngineState.RUNNING;
    }

    public boolean fuelOk() {
        return !manager.cfg().fuelEnabled || fuel > 0.001;
    }

    public double fuel() {
        return fuel;
    }

    public double health() {
        return health;
    }

    public boolean headlightOn() {
        return state.headlightOn();
    }

    public Player rider() {
        return rider;
    }

    public int tickCount() {
        return tickCount;
    }

    public double maxSpeed() {
        return manager.cfg().maxSpeed;
    }

    public double wheelieMaxAngle() {
        return manager.cfg().wheelieMaxAngle;
    }

    public double maxSteerAngle() {
        return manager.cfg().maxSteerAngle;
    }

    public boolean isBraking() {
        return brakingFlag;
    }

    public boolean isThrottling() {
        return throttlingFlag;
    }

    public MotorcycleModel model() {
        return model;
    }

    public MotorcycleSoundManager soundManager() {
        return sounds;
    }

    public MotorcycleAnimationManager animation() {
        return anim;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean visualDirty() {
        return visualDirty;
    }

    public void markVisualDirty(boolean value) {
        this.visualDirty = value;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDormant() {
        return dormant;
    }

    // ------------------------------------------------------------------
    // Coordinate helpers
    // ------------------------------------------------------------------

    /**
     * Transforms a chassis-frame point into world space using the current pose
     * (wheelie pitch around the rear contact, then yaw).
     */
    public double[] chassisPointToWorld(double lx, double ly, double lz) {
        return chassisPointToWorld(x, y, z, yaw, wheelieAngle, lx, ly, lz);
    }

    /** Pure transform for an explicit pose (used at spawn time). */
    public double[] chassisPointToWorld(double bx, double by, double bz, float byaw, double wheelieDeg,
                                        double lx, double ly, double lz) {
        double relY = ly;
        double relZ = lz + HALF_WHEELBASE; // relative to the rear contact point
        double py = MathUtil.pitchY(relY, relZ, wheelieDeg);
        double pz = MathUtil.pitchZ(relY, relZ, wheelieDeg) - HALF_WHEELBASE;
        double mx = manager.cfg().mirrorX ? -lx : lx;
        double wx = MathUtil.rotX(mx, pz, byaw);
        double wz = MathUtil.rotZ(mx, pz, byaw);
        return new double[]{bx + wx, by + py, bz + wz};
    }

    public Location chassisPointToLocation(double lx, double ly, double lz) {
        return chassisPointToLocation(lx, ly, lz, 0.0, 0.0);
    }

    /**
     * Chassis point to location, with optional extra pitch (animation lean) and
     * extra vertical dip applied on top of the wheelie transform.
     */
    public Location chassisPointToLocation(double lx, double ly, double lz, double extraPitchDeg, double extraDipY) {
        double[] p = chassisPointToWorld(x, y, z, yaw, wheelieAngle + extraPitchDeg, lx, ly + extraDipY, lz);
        Location loc = new Location(world, p[0], p[1], p[2]);
        loc.setYaw((float) (yaw + manager.cfg().modelYawOffset));
        loc.setPitch(0f);
        return loc;
    }

    // ------------------------------------------------------------------
    // Mounting
    // ------------------------------------------------------------------

    public boolean mount(Player player) {
        if (rider != null || destroyed || world == null) {
            return false;
        }
        if (!model.isAlive()) {
            return false;
        }
        ArmorStand stand = model.controllerStand();
        if (stand == null || !stand.isValid() || !stand.getWorld().equals(player.getWorld())) {
            return false;
        }
        if (!stand.addPassenger(player)) {
            return false;
        }
        rider = player;
        player.setFallDistance(0f);
        player.sendMessage(manager.cfg().msgC("msg.mounted"));
        return true;
    }

    public void dismount(DismountReason reason) {
        Player p = rider;
        rider = null;
        if (p != null && p.isOnline() && model != null && model.isAlive()) {
            ArmorStand stand = model.controllerStand();
            if (stand != null && stand.isValid() && p.getPassenger() == stand) {
                // Nudge the rider along the direction of travel.
                double push = Math.min(0.5, Math.abs(speed) * 0.8);
                p.setVelocity(new org.bukkit.util.Vector(
                        MathUtil.dirX(yaw) * push, 0.02, MathUtil.dirZ(yaw) * push));
                p.eject();
            }
        }
        if (reason == DismountReason.DESTROYED) {
            return;
        }
        dirty = true;
    }

    /** Sprint-state edge detection (SPACE): a double tap within ~350ms triggers a wheelie. */
    public void onSprintToggle(Player player, boolean nowSprinting) {
        if (rider != player || !player.isOnline()) {
            return;
        }
        if (nowSprinting) {
            if (tickCount - lastSprintUpTick <= 7) {
                pendingWheelieTap = true;
            }
            lastSprintUpTick = tickCount;
        }
    }

    /** Refuels the bike to full capacity. */
    public void refuel() {
        fuel = manager.cfg().fuelCapacity;
        dirty = true;
        sounds.playRefuel(this);
    }

    public boolean toggleHeadlight() {
        if (!manager.cfg().headlightEnabled || model == null || !model.isAlive()) {
            return false;
        }
        boolean on = !headlightOn();
        state.headlightOn(on);
        model.setHeadlight(on);
        dirty = true;
        sounds.playLightToggle(this);
        return on;
    }

    // ------------------------------------------------------------------
    // Main per-tick update
    // ------------------------------------------------------------------

    /**
     * Called by the manager for every active bike: chunk/world dormancy handling
     * plus the physics + presentation tick.
     */
    void managerTick() {
        if (destroyed) {
            return;
        }
        World w = world;
        if (w == null || Bukkit.getWorld(w.getName()) == null) {
            if (model.isAlive()) {
                model.remove();
            }
            dormant = true;
            return;
        }
        int cx = (int) Math.floor(x) >> 4;
        int cz = (int) Math.floor(z) >> 4;
        if (!w.isChunkLoaded(cx, cz)) {
            if (model.isAlive()) {
                model.remove();
            }
            dormant = true;
            dirty = true;
            return;
        }
        if (dormant) {
            respawnModel();
            dormant = false;
        }
        if (!isActive()) {
            return; // Parked with nobody around: frozen, no network traffic.
        }
        tickOnce(manager.cfg().updateInterval);
    }

    private boolean isActive() {
        if (rider != null) {
            return true;
        }
        double r2 = manager.cfg().activationRadius * manager.cfg().activationRadius;
        for (Player p : world.getPlayers()) {
            if (p.getWorld() == world && p.getLocation().distanceSquared(location()) < r2) {
                return true;
            }
        }
        return false;
    }

    private void respawnModel() {
        model.spawn();
        model.setHeadlight(headlightOn());
        anim.reset();
        markVisualDirty(true);
    }

    private void tickOnce(double dt) {
        if (destroyed) {
            return;
        }
        MotorcycleInput in = readInput();
        updateEngine(dt);
        updateFuel(dt);

        StepResult r = manager.physics().step(this, in, manager.terrain(), dt);

        // Wheel rotation follows speed (forward/reverse aware).
        wheelSpin = (wheelSpin + (speed / WHEEL_RADIUS) * MathUtil.RAD_TO_DEG * dt) % 360.0;
        if (wheelSpin < 0) {
            wheelSpin += 360.0;
        }

        if (r.collided) {
            onCollision(r.impactSpeed);
        }
        if (r.landed) {
            onLanding(r.landImpact);
        }
        if (r.fellIntoVoid) {
            fallIntoVoid();
            return;
        }

        boolean braking = in.brake && Math.abs(speed) > 0.03;
        if (braking != brakeLightActive) {
            model.setBrakeLight(braking);
            if (braking) {
                sounds.playBrake(this);
            }
            brakeLightActive = braking;
        }
        brakingFlag = braking;
        throttlingFlag = in.throttle;

        anim.update(this, dt);
        model.update();
        sounds.tick(this);
        effects(dt);
        checkEnvironment();

        if (rider != null) {
            rider.setFallDistance(0f);
        }
        if (tickCount % 600 == 0 && dirty) {
            dirty = false;
            manager.queueSave(state);
        }
        tickCount++;

        if (y < world.getMinHeight() - 30) {
            destroy();
        }
    }

    private MotorcycleInput readInput() {
        MotorcycleInput in = MotorcycleInput.of();
        Player p = rider;
        if (p == null || !p.isOnline()) {
            return in;
        }
        in.throttle = p.isSprinting() && engineRunning() && fuelOk();
        in.brake = p.isSneaking();
        in.hasSteer = true;
        in.viewYaw = p.getLocation().getYaw();
        in.wheelieTap = pendingWheelieTap;
        pendingWheelieTap = false;

        // SHIFT tap while stopped: quick dismount.
        if (in.brake && !wasBraking && Math.abs(speed) < 0.05) {
            dismount(DismountReason.SNEAK_TAP);
            return MotorcycleInput.EMPTY;
        }
        wasBraking = in.brake;
        return in;
    }

    private void updateEngine(double dt) {
        boolean wantRun = rider != null && fuelOk() && health() > 15;
        switch (engine) {
            case OFF:
                if (wantRun) {
                    engine = EngineState.STARTING;
                    engineTicks = 0;
                    sounds.playEngineStart(this);
                }
                break;
            case STARTING:
                engineTicks += dt;
                if (!wantRun) {
                    engine = EngineState.STOPPING;
                    engineTicks = 0;
                } else if (engineTicks >= 20) {
                    engine = EngineState.RUNNING;
                }
                break;
            case RUNNING:
                if (!fuelOk()) {
                    engine = EngineState.STOPPING;
                    engineTicks = 0;
                    if (rider != null) {
                        rider.sendMessage(manager.cfg().msgC("msg.fuel_empty"));
                    }
                } else if (rider == null && Math.abs(speed) < 0.02) {
                    engine = EngineState.STOPPING;
                    engineTicks = 0;
                } else if (health() <= 15) {
                    engine = EngineState.STOPPING;
                    engineTicks = 0;
                    if (rider != null) {
                        rider.sendMessage(manager.cfg().msgC("msg.engine_failure"));
                    }
                }
                break;
            case STOPPING:
                engineTicks += dt;
                if (engineTicks >= 15 || !fuelOk() || health() <= 15) {
                    engine = EngineState.OFF;
                    sounds.playEngineStop(this);
                }
                break;
            default:
                break;
        }
    }

    private void updateFuel(double dt) {
        if (!manager.cfg().fuelEnabled) {
            return;
        }
        if (engine == EngineState.RUNNING) {
            fuel = Math.max(0, fuel - manager.cfg().fuelPerSecond / 20.0 * dt);
            dirty = true;
        }
    }

    private void onCollision(double impact) {
        if (impact < 0.12) {
            return;
        }
        sounds.playCollision(this);
        Location front = location().add(MathUtil.dirX(yaw) * 0.3, 0.5, MathUtil.dirZ(yaw) * 0.3);
        world.spawnParticle(Particle.CRIT, front, 8, 0.25, 0.35, 0.25);
        if (manager.cfg().collisionDamage) {
            damage((impact - 0.1) * 20.0);
        }
        if (wheelieAngle > 3) {
            wheelieAngle = 0;
            wheelieState = WheelieState.NORMAL;
        }
        dirty = true;
    }

    private void onLanding(double impact) {
        sounds.playLanding(this);
        anim.onLanding();
        world.spawnParticle(Particle.SMOKE, location(), 6, 0.8, 0.3, 0.8);
        if (manager.cfg().fallDamage) {
            damage(Math.max(0.0, (impact - 0.25) * 18.0));
        }
        if (wheelieAngle > 18) {
            // Landing hard out of a wheelie: lose some speed and drop the front.
            speed *= 0.6;
            wheelieAngle = 0;
            wheelieState = WheelieState.NORMAL;
        }
    }

    private void effects(double dt) {
        // Exhaust smoke (heavier when damaged).
        if (engineRunning()) {
            int interval = health() > 70 ? 4 : health() > 40 ? 3 : 2;
            if (tickCount % interval == 0) {
                Location muffler = chassisPointToLocation(0.24, 0.45, -0.9);
                world.spawnParticle(manager.cfg().exhaustParticle, muffler, health() > 40 ? 1 : 2, 0.06, 0.15, 0.06);
                if (health() < 40 && tickCount % 6 == 0) {
                    world.spawnParticle(Particle.LAVA, muffler, 1, 0.05, 0.1, 0.05);
                }
            }
        }
        // Wheel dust at speed.
        if (manager.cfg().dustEnabled && Math.abs(speed) > 0.3 && !airborne()) {
            if (tickCount % manager.cfg().dustInterval == 0) {
                Location rear = chassisPointToLocation(0, 0.05, -0.6);
                world.spawnParticle(Particle.SMOKE, rear, 1, 0.25, 0.1, 0.25);
            }
        }
        // Headlight beam.
        if (manager.cfg().beamEnabled && headlightOn()) {
            if (tickCount % manager.cfg().beamInterval == 0) {
                double dx = MathUtil.dirX(yaw);
                double dz = MathUtil.dirZ(yaw);
                for (int i = 1; i <= 2; i++) {
                    Location beam = new Location(world, x + dx * i, y + 0.85, z + dz * i);
                    world.spawnParticle(manager.cfg().beamParticle, beam, 1, 0.12, 0.15, 0.12);
                }
            }
        }
    }

    private void checkEnvironment() {
        if (tickCount % 4 != 0) {
            return;
        }
        ConfigManager cfg = manager.cfg();
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        for (int i = 0; i < 2; i++) {
            Block b = world.getBlockAt(bx, (int) Math.floor(y) + i + 1, bz);
            Material m = b.getType();
            if (m == Material.LAVA || m == Material.FIRE || m == Material.SOUL_FIRE
                    || m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE) {
                if (cfg.fireDamage && cfg.damageEnabled) {
                    damage(m == Material.LAVA ? 0.5 : 0.15);
                }
                Location l = location().add(0, 0.5 + i * 0.6, 0);
                world.spawnParticle(Particle.FLAME, l, 2, 0.2, 0.4, 0.2);
            } else if (m.isSolid() && i == 1) {
                // A block was placed over the bike (squeezed).
                if (cfg.damageEnabled) {
                    damage(0.3);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Damage / destruction
    // ------------------------------------------------------------------

    public void damage(double amount) {
        if (!manager.cfg().damageEnabled || destroyed) {
            return;
        }
        health -= amount;
        dirty = true;
        if (health <= 0) {
            health = 0;
            destroy();
        }
    }

    public void destroy() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        if (rider != null) {
            dismount(DismountReason.DESTROYED);
        }
        sounds.playDestroyed(this);
        Location l = location();
        world.spawnParticle(Particle.LAVA, l, 18, 0.6, 0.8, 0.6);
        world.spawnParticle(Particle.SMOKE, l, 20, 0.7, 0.9, 0.7);
        dropKey();
        if (state.owner() != null) {
            Player owner = Bukkit.getPlayer(state.owner());
            if (owner != null) {
                owner.sendMessage(manager.cfg().msgC("msg.destroyed"));
            }
        }
        manager.remove(this, false);
    }

    private void fallIntoVoid() {
        if (rider != null) {
            dismount(DismountReason.DESTROYED);
        }
        dropKey();
        manager.remove(this, false);
        destroyed = true;
    }

    private void dropKey() {
        manager.keyItem().dropAt(location(), state.uuid());
    }

    // ------------------------------------------------------------------
    // World lifecycle / persistence
    // ------------------------------------------------------------------

    public void handleWorldUnload() {
        if (model.isAlive()) {
            model.remove();
        }
        dormant = true;
        syncToState();
        dirty = true;
    }

    public void handleWorldLoad() {
        dormant = false;
        if (!model.isAlive()) {
            respawnModel();
        }
    }

    public void syncToState() {
        state.position(x, y, z);
        state.yaw(yaw);
        state.fuel(fuel);
        state.health(health);
        state.headlightOn(headlightOn());
        state.engine(EngineState.OFF); // bikes always restart cold
        if (world != null) {
            state.worldName(world.getName());
        }
    }

    /** Re-creates the physics object after a config reload. */
    public void updatePhysics() {
        // Physics/terrain instances are owned by the manager and recreated there;
        // this hook exists so the manager can also refresh per-bike derived state.
        markVisualDirty(true);
    }

    /** Removes the rider (death/quit) without a dismount nudge. */
    public void dropRiderSilently() {
        if (rider != null) {
            rider = null;
        }
        dirty = true;
    }
}
