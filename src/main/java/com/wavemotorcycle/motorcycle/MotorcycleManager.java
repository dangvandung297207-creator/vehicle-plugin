package com.wavemotorcycle.motorcycle;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.key.WaveKeyItem;
import com.wavemotorcycle.motorcycle.persistence.MotorcyclePersistence;
import com.wavemotorcycle.motorcycle.physics.MotorcyclePhysics;
import com.wavemotorcycle.motorcycle.physics.TerrainSurface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Central registry and update loop for all motorcycles.
 *
 * <p>One scheduled task drives every bike each {@code tick.update-interval} ticks.
 * Bikes that are parked and have no player within {@code tick.activation-radius}
 * are frozen (no physics, no network traffic). Bikes in unloaded chunks or
 * unloaded worlds are dormant: their entities are removed and re-spawned when the
 * chunk/world loads again.
 */
public final class MotorcycleManager {

    private final WaveMotorcyclePlugin plugin;
    private final ConfigManager cfg;
    private final Map<UUID, MotorcycleController> bikes = new HashMap<>();
    /** Saved bikes whose world was not loaded yet at startup. */
    private final Map<UUID, Motorcycle> pendingWorlds = new HashMap<>();
    private final List<UUID> saveQueue = new ArrayList<>();

    private MotorcyclePhysics physics;
    private TerrainSurface terrain;
    private WaveKeyItem keyItem;
    private MotorcyclePersistence persistence;
    private BukkitTask task;
    private int globalTick;

    public MotorcycleManager(WaveMotorcyclePlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void start() {
        physics = new MotorcyclePhysics(cfg);
        terrain = new TerrainSurface(cfg);
        keyItem = new WaveKeyItem(cfg);
        persistence = new MotorcyclePersistence(plugin);

        List<Motorcycle> saved = persistence.load(plugin);
        for (Motorcycle m : saved) {
            World world = m.worldName() != null ? Bukkit.getWorld(m.worldName()) : null;
            if (world == null) {
                pendingWorlds.put(m.uuid(), m);
                continue;
            }
            spawnFromState(m, world);
        }
        if (!pendingWorlds.isEmpty()) {
            plugin.getLogger().info(pendingWorlds.size() + " saved bike(s) waiting for their worlds to load.");
        }
        plugin.getLogger().info(saved.size() + " motorcycle(s) restored.");

        scheduleTask();
    }

    private void scheduleTask() {
        if (task != null) {
            task.cancel();
        }
        int interval = cfg.updateInterval;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // start() may not have run (e.g. onEnable failed before it); nothing to
        // persist or clean up in that case.
        if (persistence == null) {
            return;
        }
        saveAll();
        for (MotorcycleController c : new ArrayList<>(bikes.values())) {
            c.model().remove();
        }
        bikes.clear();
    }

    private void tick() {
        globalTick++;
        for (MotorcycleController c : new ArrayList<>(bikes.values())) {
            try {
                c.managerTick();
            } catch (Exception e) {
                plugin.getLogger().severe("Error updating motorcycle " + c.state().uuid() + ": " + e);
                c.model().remove();
                bikes.remove(c.state().uuid());
            }
        }
        // Flush queued saves periodically.
        if (globalTick % 150 == 0 && !saveQueue.isEmpty()) {
            flushSaves();
        }
    }

    // ------------------------------------------------------------------
    // Spawning / removal
    // ------------------------------------------------------------------

    /**
     * Spawns a brand new (or re-created) motorcycle.
     *
     * @param loc     position to spawn at
     * @param owner   the owner player (may be null)
     * @param existingId  if non-null the bike is re-created under this UUID (key bound to a destroyed bike)
     */
    public MotorcycleController spawn(Location loc, Player owner, UUID existingId) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }
        UUID id = existingId != null ? existingId : UUID.randomUUID();
        if (bikes.containsKey(id)) {
            plugin.getLogger().warning("Refusing to spawn motorcycle: UUID " + id + " already exists.");
            return null;
        }
        Motorcycle state = new Motorcycle(id);
        state.worldName(world.getName());
        state.position(loc.getX(), loc.getY(), loc.getZ());
        state.yaw(loc.getYaw());
        if (owner != null) {
            state.owner(owner.getUniqueId());
        }
        state.fuel(cfg.fuelCapacity);
        state.health(100.0);

        MotorcycleController controller = new MotorcycleController(this, state, world,
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), cfg);
        controller.model().spawn();
        controller.markDirty();
        bikes.put(id, controller);
        queueSave(state);
        return controller;
    }

    private void spawnFromState(Motorcycle state, World world) {
        if (bikes.containsKey(state.uuid())) {
            return;
        }
        MotorcycleController controller = new MotorcycleController(this, state, world,
                state.x(), state.y(), state.z(), state.yaw(), cfg);
        if (world.isChunkLoaded((int) Math.floor(state.x()) >> 4, (int) Math.floor(state.z()) >> 4)) {
            controller.model().spawn();
        } else {
            controller.markDirty();
        }
        bikes.put(state.uuid(), controller);
    }

    public void remove(MotorcycleController controller, boolean save) {
        UUID id = controller.state().uuid();
        bikes.remove(id);
        pendingWorlds.remove(id);
        controller.model().remove();
        if (save) {
            controller.syncToState();
        }
    }

    public void removeAll() {
        for (MotorcycleController c : new ArrayList<>(bikes.values())) {
            c.dismount(MotorcycleController.DismountReason.VANILLA);
            c.model().remove();
        }
        bikes.clear();
        pendingWorlds.clear();
        saveAll();
    }

    // ------------------------------------------------------------------
    // Lookups
    // ------------------------------------------------------------------

    public MotorcycleController byId(UUID id) {
        return bikes.get(id);
    }

    public List<MotorcycleController> all() {
        return new ArrayList<>(bikes.values());
    }

    public int size() {
        return bikes.size();
    }

    /** The bike the player is currently riding, if any. */
    public MotorcycleController ofPlayer(Player player) {
        org.bukkit.entity.Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return null;
        }
        String s = vehicle.getPersistentDataContainer().get(Keys.BIKE_UUID, org.bukkit.persistence.PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return bikes.get(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Nearest bike within radius (by squared distance) of the given location. */
    public MotorcycleController nearest(Location loc, double radius) {
        double r2 = radius * radius;
        MotorcycleController best = null;
        double bestDist = Double.MAX_VALUE;
        for (MotorcycleController c : bikes.values()) {
            if (c.world() != loc.getWorld()) {
                continue;
            }
            double d = c.location().distanceSquared(loc);
            if (d <= r2 && d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // World / chunk lifecycle
    // ------------------------------------------------------------------

    public void onWorldUnload(World world) {
        for (MotorcycleController c : new ArrayList<>(bikes.values())) {
            if (c.world() == world) {
                c.handleWorldUnload();
                queueSave(c.state());
            }
        }
    }

    public void onWorldLoad(World world) {
        for (Map.Entry<UUID, Motorcycle> e : new ArrayList<>(pendingWorlds.entrySet())) {
            if (world.getName().equals(e.getValue().worldName())) {
                spawnFromState(e.getValue(), world);
                pendingWorlds.remove(e.getKey());
            }
        }
        for (MotorcycleController c : new ArrayList<>(bikes.values())) {
            if (c.world() == world) {
                c.handleWorldLoad();
            }
        }
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public void queueSave(Motorcycle state) {
        if (!saveQueue.contains(state.uuid())) {
            saveQueue.add(state.uuid());
        }
    }

    public void saveAll() {
        if (persistence == null) {
            return;
        }
        List<Motorcycle> states = new ArrayList<>();
        for (MotorcycleController c : bikes.values()) {
            c.syncToState();
            states.add(c.state());
        }
        persistence.save(plugin, states);
        saveQueue.clear();
    }

    private void flushSaves() {
        if (!saveQueue.isEmpty()) {
            saveAll();
        }
    }

    // ------------------------------------------------------------------
    // Accessors / reload
    // ------------------------------------------------------------------

    public ConfigManager cfg() {
        return cfg;
    }

    public MotorcyclePhysics physics() {
        return physics;
    }

    public TerrainSurface terrain() {
        return terrain;
    }

    public WaveKeyItem keyItem() {
        return keyItem;
    }

    public int globalTick() {
        return globalTick;
    }

    public void reload() {
        physics = new MotorcyclePhysics(cfg);
        terrain = new TerrainSurface(cfg);
        keyItem = new WaveKeyItem(cfg);
        scheduleTask();
        for (MotorcycleController c : bikes.values()) {
            c.updatePhysics();
        }
    }
}
