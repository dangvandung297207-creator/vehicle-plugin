package com.wave100.entity;

import com.wave100.WaveConfig;
import com.wave100.network.WaveInputPayload;
import com.wave100.registry.WaveItems;
import com.wave100.registry.WaveSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The Wave 100 motorcycle.
 *
 * <p>A fully custom vehicle entity: velocity-based physics, a 4-speed
 * automatic gearbox, RPM simulation, fuel, durability, wheelie balance, lights
 * and ownership - all simulated on the server and synchronized to clients via
 * {@link SynchedEntityData}. The bike is <b>server authoritative</b>
 * (minecart-style): clients only send input.</p>
 */
public class WaveMotorcycleEntity extends Entity {

    // ------------------------------------------------------------------
    // Synchronized state (visible to clients for rendering + HUD)
    // ------------------------------------------------------------------
    private static final EntityDataAccessor<Integer> DATA_ENGINE_STATE =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_RPM =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_GEAR =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HEADLIGHT_ON =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BRAKE_LIGHT_ON =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LOCKED =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CRASHED =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_WHEELIE_ANGLE =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_STEERING_ANGLE =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_LEAN_ANGLE =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SUSPENSION =
            SynchedEntityData.defineId(WaveMotorcycleEntity.class, EntityDataSerializers.FLOAT);

    // ------------------------------------------------------------------
    // Tuning constants (world-space, 1 block = 1 meter)
    // ------------------------------------------------------------------

    /** Rider seat attachment height above ground (blocks). */
    private static final double SEAT_ATTACHMENT_Y = 0.74;
    /** Rider seat attachment offset from the entity center; +Z is rear. */
    private static final double SEAT_ATTACHMENT_Z = 0.30;
    /** Rear wheel axle local Z, used as the wheelie pivot (blocks). */
    private static final double REAR_AXLE_Z = 0.62;
    /** Rear wheel axle height (blocks). */
    private static final double REAR_AXLE_Y = 0.28;
    /** Radians of wheel rotation per block travelled (r = 0.28). */
    private static final double WHEEL_ROT_PER_BLOCK = 1.0 / (2.0 * Math.PI * 0.28);
    /** Local position of the exhaust tip for smoke particles. */
    private static final Vec3 EXHAUST_TIP = new Vec3(0.32, 0.55, 1.05);
    /** Local position of the headlight lens (used by the renderer). */
    public static final Vec3 HEADLIGHT_POS = new Vec3(0.0, 0.82, -0.45);
    /** How much of the lean angle shifts the rider sideways (subtle). */
    private static final double RIDER_LEAN_FACTOR = 0.35;

    /** Cooldown (ticks) between brake screech sounds. */
    private static final int BRAKE_SOUND_COOLDOWN = 14;
    /** Ticks the engine tolerates being submerged before stalling. */
    private static final int UNDERWATER_STALL_TICKS = 60;

    // ------------------------------------------------------------------
    // Server-side simulation state
    // ------------------------------------------------------------------
    private final WavePhysics physics = new WavePhysics();
    private final WavePhysics.Input driverInput = new WavePhysics.Input();

    private EngineState engineState = EngineState.OFF;
    private int engineTimer;
    private int rpm;
    private int gear = 1;
    private float fuel = 100.0F;
    private float health = 100.0F;
    private boolean locked;
    @Nullable
    private UUID ownerUUID;

    private WheelieState wheelieState = WheelieState.NORMAL;
    private int crashTimer;
    private int underwaterTimer;
    private int brakeSoundCooldown;
    private boolean wasOnGround = true;
    private double preMoveSpeed;

    // ------------------------------------------------------------------
    // Client-side visual interpolation state (derived from synced data)
    // ------------------------------------------------------------------
    private float visSteering, prevVisSteering;
    private float visLean, prevVisLean;
    private float visWheelie, prevVisWheelie;
    private float visSuspension, prevVisSuspension;
    private float visStand, prevVisStand;
    private float visCrash, prevVisCrash;
    private float wheelSpin, prevWheelSpin;

    public WaveMotorcycleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** Climbs slabs and stairs like a lightweight bike should. */
    @Override
    public float maxUpStep() {
        return 0.55F;
    }

    // ==================================================================
    // Tick
    // ==================================================================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    private void clientTick() {
        this.prevVisSteering = this.visSteering;
        this.prevVisLean = this.visLean;
        this.prevVisWheelie = this.visWheelie;
        this.prevVisSuspension = this.visSuspension;
        this.prevVisStand = this.visStand;
        this.prevVisCrash = this.visCrash;
        this.prevWheelSpin = this.wheelSpin;

        this.visSteering = Mth.lerp(0.55F, this.visSteering, this.entityData.get(DATA_STEERING_ANGLE));
        this.visLean = Mth.lerp(0.55F, this.visLean, this.entityData.get(DATA_LEAN_ANGLE));
        this.visWheelie = Mth.lerp(0.55F, this.visWheelie, this.entityData.get(DATA_WHEELIE_ANGLE));
        this.visSuspension = Mth.lerp(0.55F, this.visSuspension, this.entityData.get(DATA_SUSPENSION));
        this.visStand = Mth.lerp(0.15F, this.visStand, this.getPassengers().isEmpty() ? 1.0F : 0.0F);
        this.visCrash = Mth.lerp(0.2F, this.visCrash, this.entityData.get(DATA_CRASHED) ? 1.0F : 0.0F);

        // continuous wheel rotation from the synced speed
        this.wheelSpin += this.entityData.get(DATA_SPEED) * WHEEL_ROT_PER_BLOCK;
    }

    private void serverTick() {
        if (this.isRemoved()) {
            return;
        }
        this.applyGravity();

        LivingEntity driver = getDriver();
        boolean crashed = this.entityData.get(DATA_CRASHED);

        // ----- engine, gears, rpm, fuel -----
        tickEngine(driver);

        // ----- nobody driving (or crashed): drop all input -----
        boolean enginePower = engineDeliversPower();
        float powerFactor = WaveEngine.healthPowerFactor(health, (float) WaveConfig.Damage.maxHealth());
        if (driver == null || crashed) {
            driverInput.clear();
        }

        // ----- wheelie balance -----
        WheelieState newState = physics.tickWheelie(driverInput, wheelieState, enginePower, crashed);
        if (newState == WheelieState.CRASH && wheelieState != WheelieState.CRASH) {
            onWheelieCrash();
        } else if (wheelieState == WheelieState.NORMAL && newState == WheelieState.LIFTING) {
            playSoundAt(WaveSounds.WHEELIE.get(), 0.7F, 1.0F);
        }
        wheelieState = newState;

        // ----- driving physics -----
        double maxSpeed = WaveConfig.Vehicle.maxSpeed();
        boolean onGround = onGround();
        if (crashed) {
            physics.crashSlide();
        } else {
            physics.tickDriving(driverInput, onGround, enginePower, gear, powerFactor);
        }
        double speedFrac = Math.min(1.0, Math.abs(physics.speed) / maxSpeed);
        physics.tickSuspension(driverInput, onGround, speedFrac, enginePower);
        if (onGround && Math.abs(physics.speed) > 0.1) {
            physics.terrainJitter(speedFrac);
        }

        if (!crashed) {
            this.setYRot(this.getYRot() + physics.yawRate);
        }

        // ----- world movement -----
        applyMovement();

        // ----- post-move reactions -----
        handleCollisions();
        handleLanding();
        handleEnvironment();
        pushNearbyEntities();
        tickAmbience();

        // ----- timers -----
        if (crashed) {
            if (--this.crashTimer <= 0 && WaveConfig.Wheelie.autoRecover()) {
                recoverFromCrash();
            }
        }
        if (this.brakeSoundCooldown > 0) {
            this.brakeSoundCooldown--;
        }

        // ----- sync visual state -----
        syncVisualState();
    }

    private void applyMovement() {
        float yawRad = this.getYRot() * ((float) Math.PI / 180F);
        double dx = -Mth.sin(yawRad) * physics.speed;
        double dz = Mth.cos(yawRad) * physics.speed;
        double dy = this.getDeltaMovement().y;

        if (this.isInWater()) {
            physics.speed *= 0.92;
            dy *= 0.8;
        }

        this.setDeltaMovement(dx, dy, dz);
        this.wasOnGround = this.onGround();
        this.preMoveSpeed = Math.abs(physics.speed);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    // ==================================================================
    // Engine / gears / fuel
    // ==================================================================

    private void tickEngine(@Nullable LivingEntity driver) {
        boolean fuelOk = fuelAvailable();
        float healthFrac = health / (float) WaveConfig.Damage.maxHealth();

        switch (engineState) {
            case STARTING -> {
                if (--engineTimer <= 0) {
                    engineState = EngineState.IDLE;
                    playSoundAt(WaveSounds.ENGINE_IDLE.get(), 0.8F, 1.0F);
                }
            }
            case IDLE, RUNNING -> {
                if (!fuelOk || healthFrac <= 0.15F) {
                    stopEngine(true);
                } else if (driver != null) {
                    engineState = driverInput.forward ? EngineState.RUNNING : EngineState.IDLE;
                } else {
                    engineState = EngineState.IDLE;
                }
            }
            case STOPPING -> {
                if (--engineTimer <= 0) {
                    engineState = EngineState.OFF;
                    rpm = 0;
                }
            }
            case OFF -> {
            }
        }

        // RPM simulation with smooth tracking
        boolean simOn = engineState == EngineState.IDLE || engineState == EngineState.RUNNING;
        int target = WaveEngine.targetRpm(physics.speed, simOn && driverInput.forward ? 1.0F : 0.0F, gear, simOn);
        rpm += (int) ((target - rpm) * 0.25);
        rpm = Mth.clamp(rpm, 0, WaveConfig.Engine.maxRpm());

        // automatic gearbox with hysteresis + shift dip
        int newGear = WaveEngine.pickGear(gear, physics.speed);
        if (newGear != gear) {
            gear = newGear;
            rpm = Math.max(WaveConfig.Engine.idleRpm(), rpm - 650);
        }

        // fuel consumption (idle burns a little, throttle burns more)
        if (simOn && WaveConfig.Fuel.enabled()) {
            float load = 0.35F + 0.65F * (driverInput.forward ? 1.0F : 0.0F);
            float rpmFactor = 1.0F + rpm / (float) WaveConfig.Engine.maxRpm() * 0.4F;
            fuel = Math.max(0.0F, fuel - (float) WaveConfig.Fuel.consumption() * load * rpmFactor);
            if (fuel <= 0.0F && engineState != EngineState.STOPPING) {
                stopEngine(true);
            }
        }
    }

    public void startEngine() {
        if (engineState != EngineState.OFF || !fuelAvailable()) {
            return;
        }
        if (health / (float) WaveConfig.Damage.maxHealth() <= 0.15F) {
            return; // engine failure - too damaged
        }
        engineState = EngineState.STARTING;
        engineTimer = WaveConfig.Engine.startTicks();
        playSoundAt(WaveSounds.ENGINE_START.get(), 0.9F, 0.9F);
    }

    public void stopEngine(boolean sputter) {
        if (!engineState.isOn()) {
            return;
        }
        engineState = EngineState.STOPPING;
        engineTimer = WaveConfig.Engine.stopTicks();
        playSoundAt(WaveSounds.ENGINE_STOP.get(), 0.9F, sputter ? 1.1F : 0.9F);
        if (sputter && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.7, this.getZ(), 4, 0.1, 0.1, 0.1, 0.01);
        }
    }

    public void toggleEngine() {
        if (engineState.isOn()) {
            stopEngine(false);
        } else {
            startEngine();
        }
    }

    private boolean engineDeliversPower() {
        return (engineState == EngineState.IDLE || engineState == EngineState.RUNNING)
                && fuelAvailable()
                && health / (float) WaveConfig.Damage.maxHealth() > 0.15F;
    }

    private boolean fuelAvailable() {
        return !WaveConfig.Fuel.enabled() || fuel > 0.0F;
    }

    // ==================================================================
    // Collisions / landings / environment
    // ==================================================================

    private void handleCollisions() {
        if (this.horizontalCollision) {
            double impact = this.preMoveSpeed;
            if (impact > WaveConfig.Damage.collisionMinSpeed()) {
                physics.impact(impact * 1.6);
                if (WaveConfig.Damage.enabled() && WaveConfig.Damage.collisions()) {
                    applyDamage((float) ((impact - WaveConfig.Damage.collisionMinSpeed()) * 55.0), false);
                }
                playSoundAt(WaveSounds.COLLISION.get(), 0.9F, (float) (1.3 - impact * 0.4));
                if (this.level() instanceof ServerLevel serverLevel) {
                    Vec3 front = localToWorld(0, 0.5, -0.8);
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            front.x, front.y, front.z, 6, 0.2, 0.2, 0.2, 0.02);
                }
                LivingEntity driver = getDriver();
                if (driver != null && impact > 0.5) {
                    driver.hurt(this.damageSources().generic(), (float) (impact * 7.0));
                }
                // a hard hit while on the back wheel ends badly
                if (physics.wheelieAngle > 5.0F && impact > 0.3) {
                    onWheelieCrash();
                }
            }
            physics.speed = 0.0;
            physics.yawRate = 0.0F;
        }
    }

    private void handleLanding() {
        if (!this.wasOnGround && this.onGround()) {
            double fall = this.fallDistance;
            physics.impact(fall * 0.16);
            this.fallDistance = 0.0F;
            if (this.level() instanceof ServerLevel serverLevel) {
                if (fall > WaveConfig.Damage.safeFall() && WaveConfig.Damage.enabled()
                        && WaveConfig.Damage.falls()) {
                    applyDamage((float) ((fall - WaveConfig.Damage.safeFall()) * 6.0), false);
                    playSoundAt(WaveSounds.COLLISION.get(), 1.0F, 0.8F);
                } else if (fall > 1.2) {
                    playSoundAt(WaveSounds.COLLISION.get(), 0.5F, 1.4F);
                }
                if (fall > 1.2) {
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            this.getX(), this.getY() + 0.05, this.getZ(), 8, 0.4, 0.02, 0.4, 0.02);
                }
            }
            // landing while wheelie-ing slams the front down
            if (physics.wheelieAngle > 2.0F) {
                physics.wheelieVelocity -= 0.8F;
            }
        }
    }

    private void handleEnvironment() {
        if (this.isInWater()) {
            if (this.isUnderWater() && engineState.isOn()) {
                if (++this.underwaterTimer > UNDERWATER_STALL_TICKS) {
                    this.underwaterTimer = 0;
                    stopEngine(true);
                }
            }
        } else {
            this.underwaterTimer = 0;
        }

        if (this.isInLava()) {
            if (WaveConfig.Damage.enabled()) {
                applyDamage(1.5F, false);
            }
        } else if (this.getRemainingFireTicks() > 0 && WaveConfig.Damage.enabled()) {
            applyDamage(0.15F, false);
        }
    }

    private void pushNearbyEntities() {
        if (Math.abs(physics.speed) < 0.2 || this.tickCount % 2 != 0) {
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        AABB box = this.getBoundingBox().expandTowards(motion.x * 2.0, 0.2, motion.z * 2.0);
        List<Entity> list = this.level().getEntities(this, box);
        for (Entity entity : list) {
            if (entity.getVehicle() == this || !entity.isPushable()) {
                continue;
            }
            entity.push(motion.x * 5.0, 0.25, motion.z * 5.0);
            entity.hurtMarked = true;
            if (Math.abs(physics.speed) > 0.5 && entity instanceof LivingEntity living) {
                living.hurt(this.damageSources().generic(), (float) (Math.abs(physics.speed) * 6.0));
            }
        }
    }

    // ==================================================================
    // Crash handling
    // ==================================================================

    private void onWheelieCrash() {
        if (this.entityData.get(DATA_CRASHED)) {
            return;
        }
        this.entityData.set(DATA_CRASHED, true);
        this.crashTimer = WaveConfig.Wheelie.recoverTicks();
        this.physics.speed *= 0.4;
        this.physics.wheelieAngle = 0.0F;
        this.physics.wheelieVelocity = 0.0F;
        this.wheelieState = WheelieState.CRASH;

        // throw the rider off
        LivingEntity driver = getDriver();
        if (driver != null) {
            driver.stopRiding();
            double yaw = Math.toRadians(this.getYRot());
            driver.push(Math.sin(yaw) * 0.4, 0.45, -Math.cos(yaw) * 0.4);
            driver.hurt(this.damageSources().generic(), 5.0F);
        }

        if (WaveConfig.Damage.enabled()) {
            applyDamage(8.0F, false);
        }
        playSoundAt(WaveSounds.CRASH.get(), 1.0F, 0.9F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    this.getX(), this.getY() + 0.5, this.getZ(), 10, 0.3, 0.2, 0.3, 0.03);
            serverLevel.sendParticles(ParticleTypes.POOF,
                    this.getX(), this.getY() + 0.2, this.getZ(), 10, 0.4, 0.1, 0.4, 0.02);
        }
    }

    public void recoverFromCrash() {
        this.entityData.set(DATA_CRASHED, false);
        this.wheelieState = WheelieState.NORMAL;
        this.physics.wheelieAngle = 0.0F;
        this.physics.wheelieVelocity = 0.0F;
    }

    // ==================================================================
    // Ambience: particles + one-shot sounds
    // ==================================================================

    private void tickAmbience() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean running = engineState == EngineState.RUNNING || engineState == EngineState.IDLE;

        // exhaust puffs while running, more under load
        if (running && this.tickCount % (engineState == EngineState.RUNNING ? 5 : 11) == 0) {
            Vec3 tip = localToWorld(EXHAUST_TIP.x, EXHAUST_TIP.y, EXHAUST_TIP.z);
            serverLevel.sendParticles(ParticleTypes.SMOKE, tip.x, tip.y, tip.z, 1, 0.02, 0.02, 0.02, 0.005);
        }

        // damage smoke scales with lost durability
        float frac = health / (float) WaveConfig.Damage.maxHealth();
        if (WaveConfig.Damage.enabled() && frac < 0.70F) {
            int interval = frac < 0.15F ? 5 : frac < 0.40F ? 12 : 30;
            if (this.tickCount % interval == 0) {
                Vec3 engine = localToWorld(0.0, 0.6, 0.0);
                serverLevel.sendParticles(ParticleTypes.SMOKE, engine.x, engine.y, engine.z,
                        frac < 0.4 ? 2 : 1, 0.08, 0.08, 0.08, 0.01);
            }
        }

        // brake screech
        if (driverInput.brakeLight && this.brakeSoundCooldown <= 0 && Math.abs(physics.speed) > 0.3) {
            playSoundAt(WaveSounds.BRAKE.get(), 0.55F, 1.1F);
            this.brakeSoundCooldown = BRAKE_SOUND_COOLDOWN;
        }
    }

    private void playSoundAt(SoundEvent sound, float volume, float pitch) {
        if (!WaveConfig.Sound.enabled() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), sound,
                SoundSource.NEUTRAL, (float) (volume * WaveConfig.Sound.volume()), pitch);
    }

    // ==================================================================
    // Damage / destruction
    // ==================================================================

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        // the driver cannot accidentally wreck their own ride
        if (source.getEntity() == getFirstPassenger()) {
            return false;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false; // custom landing logic handles falls
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_PROJECTILE)) {
            applyDamage(amount, true);
            this.hurtMarked = true;
            return true;
        }
        if (amount > 0.1F) {
            applyDamage(amount * 0.5F, true);
            return true;
        }
        return false;
    }

    /** Applies durability damage and destroys the bike at zero. */
    public void applyDamage(float amount, boolean effects) {
        if (!WaveConfig.Damage.enabled() || amount <= 0.0F) {
            return;
        }
        this.health = Math.max(0.0F, this.health - amount);
        if (effects) {
            playSoundAt(WaveSounds.COLLISION.get(), 0.6F, 1.2F);
        }
        if (this.health <= 0.0F) {
            destroyVehicle(true);
        }
    }

    private void destroyVehicle(boolean withEffects) {
        if (this.isRemoved()) {
            return;
        }
        if (withEffects) {
            playSoundAt(WaveSounds.DESTROYED.get(), 1.0F, 1.0F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.POOF,
                        this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        this.getX(), this.getY() + 0.5, this.getZ(), 15, 0.4, 0.4, 0.4, 0.03);
            }
        }
        if (WaveConfig.Damage.dropItem()) {
            this.spawnAtLocation(new ItemStack(WaveItems.WAVE_MOTORCYCLE.get()));
        }
        this.discard();
    }

    // ==================================================================
    // Riding
    // ==================================================================

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions entityDimensions, float scale) {
        return new Vec3(0.0, SEAT_ATTACHMENT_Y, SEAT_ATTACHMENT_Z);
    }

    /**
     * Positions the rider on the seat. The seat point is rotated by the lean
     * and wheelie angles so the rider moves with the machine instead of
     * floating. Runs on both logical sides with matching angles.
     */
    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!this.hasPassenger(passenger)) {
            return;
        }
        // servers use the authoritative angles, clients their smoothed ones
        float leanDeg = this.level().isClientSide ? getRenderLean(1.0F) : physics.lean;
        float pitchDeg = this.level().isClientSide ? getRenderWheelie(1.0F) : physics.wheelieAngle;
        double lean = Math.toRadians(leanDeg);
        double pitch = Math.toRadians(pitchDeg);

        // seat point in local space (+Z rear, +Y up)
        double sy = SEAT_ATTACHMENT_Y;
        double sz = SEAT_ATTACHMENT_Z;

        // wheelie pitch: rotate (sy, sz) around the rear axle
        double dy = sy - REAR_AXLE_Y;
        double dz = sz - REAR_AXLE_Z;
        double py = dy * Math.cos(pitch) - dz * Math.sin(pitch) + REAR_AXLE_Y;
        double pz = dz * Math.cos(pitch) + dy * Math.sin(pitch) + REAR_AXLE_Z;

        // lean roll around the ground contact line: leaning left shifts the
        // seat toward +X (local left) and drops it slightly
        double lx = py * Math.sin(lean) * RIDER_LEAN_FACTOR;
        double ly = py * Math.cos(lean);

        Vec3 world = localToWorld(lx, ly, pz);
        Vec3 attachment = passenger.getVehicleAttachmentPoint(this);
        moveFunction.accept(passenger, world.x - attachment.x, world.y - attachment.y, world.z - attachment.z);
        clampRiderYaw(passenger);
    }

    /** Gently pulls the rider's facing toward the direction of travel. */
    protected void clampRiderYaw(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float delta = Mth.wrapDegrees(this.getYRot() - passenger.getYRot());
        float clamped = Mth.clamp(delta, -70.0F, 70.0F);
        passenger.yRotO += clamped - delta;
        passenger.setYRot(passenger.getYRot() + clamped - delta);
        passenger.setYHeadRot(passenger.getYRot() + clamped);
    }

    @Override
    protected Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // try the left side first, then right, then diagonal, then behind, then above
        double[][] candidates = {
                {1.15, 0.0, 0.25}, {-1.15, 0.0, 0.25}, {0.9, 0.0, -0.9}, {-0.9, 0.0, -0.9},
                {0.0, 0.0, 1.5}, {0.0, 1.0, 0.0}
        };
        AABB passengerBox = passenger.getBoundingBox();
        for (double[] c : candidates) {
            Vec3 world = localToWorld(c[0], c[1], c[2]);
            if (this.level().noCollision(passenger, passengerBox.move(
                    world.x - passenger.getX(), world.y - passenger.getY() + 0.1, world.z - passenger.getZ()))) {
                return world;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    // ==================================================================
    // Interaction
    // ==================================================================

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (this.entityData.get(DATA_CRASHED) && !WaveConfig.Wheelie.autoRecover()) {
            recoverFromCrash();
            player.sendSystemMessage(Component.translatable("message.wave100.recovered"));
            return InteractionResult.SUCCESS;
        }

        // sneak + right click: service the bike
        if (player.isSecondaryUseActive()) {
            if (held.is(Items.COAL) || held.is(Items.CHARCOAL)) {
                return tryRefuel(player, held);
            }
            if (held.is(Items.IRON_INGOT)) {
                return tryRepair(player, held);
            }
            sendInfo(player);
            return InteractionResult.SUCCESS;
        }

        if (this.locked && !isOwner(player)) {
            player.sendSystemMessage(Component.translatable("message.wave100.locked"));
            return InteractionResult.FAIL;
        }

        if (this.getFirstPassenger() != null) {
            player.sendSystemMessage(Component.translatable("message.wave100.occupied"));
            return InteractionResult.FAIL;
        }

        if (this.isInWater()) {
            player.sendSystemMessage(Component.translatable("message.wave100.in_water"));
            return InteractionResult.FAIL;
        }

        if (!player.isPassenger()) {
            claimOwnership(player);
            player.startRiding(this);
            if (WaveConfig.Engine.autoStart()) {
                startEngine();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult tryRefuel(Player player, ItemStack held) {
        if (!WaveConfig.Fuel.enabled()) {
            player.sendSystemMessage(Component.translatable("message.wave100.fuel_disabled"));
            return InteractionResult.FAIL;
        }
        float capacity = (float) WaveConfig.Fuel.capacity();
        if (fuel >= capacity - 0.01F) {
            player.sendSystemMessage(Component.translatable("message.wave100.tank_full"));
            return InteractionResult.FAIL;
        }
        fuel = Math.min(capacity, fuel + (float) WaveConfig.Fuel.perCoal());
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        playSoundAt(WaveSounds.REFUEL.get(), 0.8F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.wave100.refueled",
                (int) (fuel / capacity * 100.0F)));
        return InteractionResult.SUCCESS;
    }

    private InteractionResult tryRepair(Player player, ItemStack held) {
        float max = (float) WaveConfig.Damage.maxHealth();
        if (!WaveConfig.Damage.enabled() || health >= max - 0.01F) {
            player.sendSystemMessage(Component.translatable("message.wave100.repair_unneeded"));
            return InteractionResult.FAIL;
        }
        health = Math.min(max, health + (float) WaveConfig.Fuel.repairPerIron());
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        playSoundAt(WaveSounds.REPAIR.get(), 0.8F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.wave100.repaired",
                (int) (health / max * 100.0F)));
        return InteractionResult.SUCCESS;
    }

    private void sendInfo(Player player) {
        sendInfoTo(player);
    }

    /** Sends the service-info screen (fuel, durability, owner, lock state). */
    public void sendInfoTo(CommandSourceStack source) {
        float maxH = (float) WaveConfig.Damage.maxHealth();
        float cap = (float) WaveConfig.Fuel.capacity();
        source.sendSuccess(() -> Component.translatable("message.wave100.info_header"), false);
        source.sendSuccess(() -> Component.translatable("message.wave100.info_fuel",
                (int) Math.round(fuel / cap * 100.0F)), false);
        source.sendSuccess(() -> Component.translatable("message.wave100.info_durability",
                (int) Math.round(health / maxH * 100.0F)), false);
        if (WaveConfig.Ownership.enabled() && ownerUUID != null) {
            source.sendSuccess(() -> Component.translatable("message.wave100.info_owner",
                    ownerUUID.toString()), false);
        }
        if (WaveConfig.Ownership.lockable()) {
            source.sendSuccess(() -> Component.translatable("message.wave100.info_locked",
                    this.locked ? Component.translatable("message.wave100.on")
                            : Component.translatable("message.wave100.off")), false);
        }
    }

    /** Player-facing info chat (used for sneak + right click). */
    public void sendInfoTo(Player player) {
        float maxH = (float) WaveConfig.Damage.maxHealth();
        float cap = (float) WaveConfig.Fuel.capacity();
        player.sendSystemMessage(Component.translatable("message.wave100.info_header"));
        player.sendSystemMessage(Component.translatable("message.wave100.info_fuel",
                (int) Math.round(fuel / cap * 100.0F)));
        player.sendSystemMessage(Component.translatable("message.wave100.info_durability",
                (int) Math.round(health / maxH * 100.0F)));
        if (WaveConfig.Ownership.enabled()) {
            if (ownerUUID != null) {
                Player owner = this.level() instanceof ServerLevel serverLevel
                        ? serverLevel.getPlayerByUUID(ownerUUID) : null;
                player.sendSystemMessage(Component.translatable("message.wave100.info_owner",
                        owner != null ? owner.getDisplayName() : Component.literal("offline")));
            }
            if (WaveConfig.Ownership.lockable()) {
                player.sendSystemMessage(Component.translatable("message.wave100.info_locked",
                        this.locked ? Component.translatable("message.wave100.on")
                                : Component.translatable("message.wave100.off")));
            }
        }
        player.sendSystemMessage(Component.translatable("message.wave100.info_hint"));
    }

    // ==================================================================
    // Ownership / locking
    // ==================================================================

    public void claimOwnership(Player player) {
        if (WaveConfig.Ownership.enabled() && ownerUUID == null) {
            ownerUUID = player.getUUID();
        }
    }

    public void setOwner(@Nullable UUID owner) {
        this.ownerUUID = owner;
    }

    /** Used by the spawn item / commands. */
    public void setFuel(float fuel) {
        this.fuel = Mth.clamp(fuel, 0.0F, (float) WaveConfig.Fuel.capacity());
    }

    /** Used by the spawn item / commands. */
    public void setHealth(float health) {
        this.health = Mth.clamp(health, 0.0F, (float) WaveConfig.Damage.maxHealth());
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public boolean isOwner(Player player) {
        return !WaveConfig.Ownership.enabled() || ownerUUID == null || ownerUUID.equals(player.getUUID());
    }

    public void setLocked(boolean locked) {
        if (WaveConfig.Ownership.lockable()) {
            this.locked = locked;
        }
    }

    // ==================================================================
    // Driver input (from the network)
    // ==================================================================

    public void applyDriverInput(WaveInputPayload payload) {
        if (this.isRemoved()) {
            return;
        }
        driverInput.forward = payload.has(WaveInputPayload.FORWARD);
        driverInput.back = payload.has(WaveInputPayload.BACK);
        driverInput.left = payload.has(WaveInputPayload.LEFT);
        driverInput.right = payload.has(WaveInputPayload.RIGHT);
        driverInput.wheelie = payload.has(WaveInputPayload.WHEELIE);
        if (payload.has(WaveInputPayload.TOGGLE_ENGINE)) {
            toggleEngine();
        }
        if (payload.has(WaveInputPayload.TOGGLE_HEADLIGHT)) {
            toggleHeadlight();
        }
    }

    public boolean isDriver(Player player) {
        return this.getFirstPassenger() == player;
    }

    @Nullable
    public LivingEntity getDriver() {
        if (!this.getPassengers().isEmpty() && this.getPassengers().get(0) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    public void toggleHeadlight() {
        if (!WaveConfig.Lights.headlight()) {
            return;
        }
        boolean on = !this.entityData.get(DATA_HEADLIGHT_ON);
        this.entityData.set(DATA_HEADLIGHT_ON, on);
        playSoundAt(on ? WaveSounds.LIGHT_ON.get() : WaveSounds.LIGHT_OFF.get(), 0.6F, 1.0F);
    }

    // ==================================================================
    // Sync + save
    // ==================================================================

    private void syncVisualState() {
        this.entityData.set(DATA_ENGINE_STATE, engineState.ordinal());
        this.entityData.set(DATA_SPEED, (float) physics.speed);
        this.entityData.set(DATA_RPM, rpm);
        this.entityData.set(DATA_GEAR, gear);
        this.entityData.set(DATA_FUEL, fuel);
        this.entityData.set(DATA_HEALTH, health);
        this.entityData.set(DATA_LOCKED, locked);
        this.entityData.set(DATA_BRAKE_LIGHT_ON, driverInput.brakeLight && WaveConfig.Lights.brakeLight());
        this.entityData.set(DATA_WHEELIE_ANGLE, physics.wheelieAngle);
        this.entityData.set(DATA_STEERING_ANGLE, physics.steering);
        this.entityData.set(DATA_LEAN_ANGLE, physics.lean);
        this.entityData.set(DATA_SUSPENSION, physics.suspension);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ENGINE_STATE, EngineState.OFF.ordinal());
        builder.define(DATA_SPEED, 0.0F);
        builder.define(DATA_RPM, 0);
        builder.define(DATA_GEAR, 1);
        builder.define(DATA_FUEL, 100.0F);
        builder.define(DATA_HEALTH, 100.0F);
        builder.define(DATA_HEADLIGHT_ON, false);
        builder.define(DATA_BRAKE_LIGHT_ON, false);
        builder.define(DATA_LOCKED, false);
        builder.define(DATA_CRASHED, false);
        builder.define(DATA_WHEELIE_ANGLE, 0.0F);
        builder.define(DATA_STEERING_ANGLE, 0.0F);
        builder.define(DATA_LEAN_ANGLE, 0.0F);
        builder.define(DATA_SUSPENSION, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Fuel", fuel);
        tag.putFloat("Health", health);
        tag.putFloat("Speed", (float) physics.speed);
        tag.putBoolean("Headlight", this.entityData.get(DATA_HEADLIGHT_ON));
        tag.putBoolean("Locked", locked);
        if (ownerUUID != null) {
            tag.putUUID("Owner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        float maxH = (float) WaveConfig.Damage.maxHealth();
        float cap = (float) WaveConfig.Fuel.capacity();
        // clamp everything: never trust saved data
        fuel = Mth.clamp(tag.getFloat("Fuel"), 0.0F, cap);
        health = Mth.clamp(tag.getFloat("Health"), 0.0F, maxH);
        physics.speed = Mth.clamp(tag.getFloat("Speed"), -0.5, WaveConfig.Vehicle.maxSpeed());
        this.entityData.set(DATA_HEADLIGHT_ON, tag.getBoolean("Headlight") && WaveConfig.Lights.headlight());
        locked = tag.getBoolean("Locked") && WaveConfig.Ownership.lockable();
        ownerUUID = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (health <= 0.0F) {
            health = 1.0F; // never load an already-destroyed bike
        }
        // the engine always starts cold after a reload
        engineState = EngineState.OFF;
        rpm = 0;
        gear = 1;
        wheelieState = WheelieState.NORMAL;
        this.entityData.set(DATA_CRASHED, false);
    }

    // ==================================================================
    // Entity plumbing
    // ==================================================================

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    protected void onBelowWorld() {
        // fell into the void: drop the item (config) and remove
        if (WaveConfig.Damage.dropItem()) {
            this.spawnAtLocation(new ItemStack(WaveItems.WAVE_MOTORCYCLE.get()));
        }
        this.discard();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(WaveItems.WAVE_MOTORCYCLE.get());
    }

    @Override
    protected double getDefaultGravity() {
        return WaveConfig.Vehicle.gravity();
    }

    // ==================================================================
    // Client-side render accessors (visual interpolation)
    // ==================================================================

    public float getRenderSteering(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisSteering, visSteering);
    }

    public float getRenderLean(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisLean, visLean);
    }

    public float getRenderWheelie(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisWheelie, visWheelie);
    }

    public float getRenderSuspension(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisSuspension, visSuspension);
    }

    /** 1 = side stand down (parked), 0 = folded away. */
    public float getRenderStand(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisStand, visStand);
    }

    /** 1 = crashed / lying on the side. */
    public float getRenderCrash(float partialTicks) {
        return Mth.lerp(partialTicks, prevVisCrash, visCrash);
    }

    public float getRenderWheelSpin(float partialTicks) {
        return Mth.lerp(partialTicks, prevWheelSpin, wheelSpin);
    }

    public EngineState getEngineState() {
        return EngineState.values()[this.entityData.get(DATA_ENGINE_STATE)];
    }

    public int getRpm() {
        return this.entityData.get(DATA_RPM);
    }

    public int getGear() {
        return this.entityData.get(DATA_GEAR);
    }

    public float getFuel() {
        return this.entityData.get(DATA_FUEL);
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH);
    }

    public float getMaxHealth() {
        return (float) WaveConfig.Damage.maxHealth();
    }

    public float getMaxFuel() {
        return (float) WaveConfig.Fuel.capacity();
    }

    public boolean isHeadlightOn() {
        return this.entityData.get(DATA_HEADLIGHT_ON);
    }

    public boolean isBrakeLightOn() {
        return this.entityData.get(DATA_BRAKE_LIGHT_ON);
    }

    public boolean isCrashed() {
        return this.entityData.get(DATA_CRASHED);
    }

    /** Current forward speed in blocks per tick (synced). */
    public float getSyncedSpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    /** Speed in km/h for the HUD. */
    public float getSpeedKmh() {
        return getSyncedSpeed() * 72.0F;
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    /**
     * Transforms a local-space point (front = -Z, left = +X) to world space.
     */
    public Vec3 localToWorld(double lx, double ly, double lz) {
        float yawRad = this.getYRot() * ((float) Math.PI / 180F);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        return new Vec3(
                this.getX() + lx * cos + lz * sin,
                this.getY() + ly,
                this.getZ() - lx * sin + lz * cos);
    }
}
