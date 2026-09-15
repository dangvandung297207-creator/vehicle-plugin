package com.example.vanillavehicles.vehicle;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.collision.CollisionHandler;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.entity.VehicleTags;
import com.example.vanillavehicles.event.VehicleSpawnEvent;
import com.example.vanillavehicles.input.InputState;
import com.example.vanillavehicles.storage.VehicleStorage;
import com.example.vanillavehicles.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns every active vehicle and runs the ONE central update loop.
 * Also routes entering, dismounts, spawner items and persistence.
 */
public class VehicleManager implements Listener {

    private final VanillaVehicles plugin;
    private final VehicleRegistry registry;
    private final Map<UUID, Vehicle> vehicles = new LinkedHashMap<>();
    private final Map<UUID, Vehicle> entityIndex = new LinkedHashMap<>();
    private final Map<UUID, Vehicle> riderIndex = new LinkedHashMap<>();
    private long tick;

    public VehicleManager(VanillaVehicles plugin, VehicleRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    /** Starts the central 20 Hz update loop. */
    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    public long getTick() {
        return tick;
    }

    public Collection<Vehicle> getAll() {
        return Collections.unmodifiableCollection(vehicles.values());
    }

    private void tickAll() {
        tick++;
        for (Vehicle vehicle : new ArrayList<>(vehicles.values())) {
            try {
                vehicle.tick(tick);
            } catch (Exception ex) {
                plugin.getLogger().warning("Error ticking vehicle " + vehicle.getId() + ": " + ex.getMessage());
            }
        }
        if (tick % 5 == 0) {
            collideVehicles();
        }
        int saveMinutes = plugin.getPluginConfig().getSaveIntervalMinutes();
        if (saveMinutes > 0 && tick % (saveMinutes * 60L * 20L) == 0) {
            try {
                plugin.getStorage().saveAll(vehicles.values());
            } catch (Exception ex) {
                plugin.getLogger().warning("Autosave failed: " + ex.getMessage());
            }
        }
    }

    // ------------------------------------------------------------------
    // Spawning / removing
    // ------------------------------------------------------------------

    /** Finds a safe spawn position: ground for land, surface for boats, air kept. */
    public Location safeLocation(Location wanted, VehicleType type) {
        Location location = wanted.clone();
        World world = location.getWorld();
        if (world == null) {
            return location;
        }
        if (type.getPhysics() == PhysicsType.AIRCRAFT) {
            return location;
        }
        double surface = CollisionHandler.liquidSurface(world,
                location.getX(), location.getY() + 1, location.getZ());
        if (type.getPhysics() == PhysicsType.BOAT && surface != Double.NEGATIVE_INFINITY) {
            location.setY(surface - 0.2);
            return location;
        }
        double ground = CollisionHandler.groundLevel(world,
                location.getX(), location.getY() + 1, location.getZ());
        if (ground != Double.NEGATIVE_INFINITY) {
            location.setY(Math.max(location.getY(), ground));
            if (location.getY() - ground > 3) {
                location.setY(ground);
            }
        }
        return location;
    }

    public boolean canSpawn(Player player, VehicleType type) {
        if (!plugin.getPluginConfig().statsFor(type).enabled) {
            MessageUtil.send(player, "&cThat vehicle is disabled.");
            return false;
        }
        String permission = plugin.getPluginConfig().statsFor(type).permission;
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            MessageUtil.send(player, "&cYou lack permission for that vehicle.");
            plugin.getSoundManager().deny(player);
            return false;
        }
        int max = plugin.getPluginConfig().getMaxVehiclesPerPlayer();
        if (max > 0 && countByOwner(player.getUniqueId()) >= max
                && !player.hasPermission("vehicle.admin")) {
            MessageUtil.send(player, "&cVehicle limit reached (" + max + ").");
            plugin.getSoundManager().deny(player);
            return false;
        }
        int total = plugin.getPluginConfig().getMaxVehiclesTotal();
        if (total > 0 && vehicles.size() >= total) {
            MessageUtil.send(player, "&cToo many active vehicles on the server.");
            plugin.getSoundManager().deny(player);
            return false;
        }
        return true;
    }

    /** Spawns a vehicle, firing VehicleSpawnEvent. Returns null when denied. */
    public Vehicle spawn(VehicleType type, Location location, Player spawner) {
        if (type == null || location == null || location.getWorld() == null) {
            return null;
        }
        VehicleDefinition definition = registry.get(type);
        if (definition == null) {
            return null;
        }
        VehicleStats stats = plugin.getPluginConfig().statsFor(type);
        Location safe = safeLocation(location, type);
        Vehicle vehicle = new Vehicle(plugin, type, definition, stats, safe,
                spawner == null ? null : spawner.getUniqueId(),
                spawner == null ? "Console" : spawner.getName());
        vehicle.spawn();
        vehicles.put(vehicle.getId(), vehicle);
        VehicleSpawnEvent event = new VehicleSpawnEvent(vehicle, spawner);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            vehicle.removeSilent();
            return null;
        }
        return vehicle;
    }

    /** Restores a persisted vehicle without firing events. */
    public Vehicle spawnRestored(VehicleStorage.Record record) {
        VehicleType type = VehicleType.byId(record.typeId);
        World world = Bukkit.getWorld(record.world);
        if (type == null || world == null) {
            return null;
        }
        VehicleDefinition definition = registry.get(type);
        if (definition == null) {
            return null;
        }
        VehicleStats stats = plugin.getPluginConfig().statsFor(type);
        Location location = new Location(world, record.x, record.y, record.z, record.yaw, 0f);
        Vehicle vehicle = new Vehicle(plugin, type, definition, stats, location,
                record.owner, record.ownerName);
        vehicle.setHealth(record.health);
        vehicle.spawn();
        vehicle.setCargoContents(record.cargo);
        vehicles.put(vehicle.getId(), vehicle);
        return vehicle;
    }

    public void loadPersisted() {
        List<VehicleStorage.Record> records;
        try {
            records = plugin.getStorage().loadAll();
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not load saved vehicles: " + ex.getMessage());
            return;
        }
        int restored = 0;
        for (VehicleStorage.Record record : records) {
            try {
                if (spawnRestored(record) != null) {
                    restored++;
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Could not restore vehicle: " + ex.getMessage());
            }
        }
        if (restored > 0) {
            plugin.getLogger().info("Restored " + restored + " saved vehicles.");
        }
    }

    public void unregister(Vehicle vehicle) {
        vehicles.remove(vehicle.getId());
        entityIndex.values().removeIf(v -> v.getId().equals(vehicle.getId()));
        riderIndex.values().removeIf(v -> v.getId().equals(vehicle.getId()));
        plugin.getSoundManager().forget(vehicle);
    }

    public void removeAll() {
        for (Vehicle vehicle : new ArrayList<>(vehicles.values())) {
            vehicle.removeQuiet();
        }
    }

    public void removeAllSilent() {
        for (Vehicle vehicle : new ArrayList<>(vehicles.values())) {
            try {
                vehicle.removeSilent();
            } catch (Exception ignored) {
            }
        }
        vehicles.clear();
        entityIndex.clear();
        riderIndex.clear();
    }

    // ------------------------------------------------------------------
    // Indexes
    // ------------------------------------------------------------------

    public void indexVehicle(Vehicle vehicle) {
        for (Entity entity : vehicle.getEntities()) {
            if (entity != null) {
                entityIndex.put(entity.getUniqueId(), vehicle);
            }
        }
    }

    public void indexEntity(Entity entity, Vehicle vehicle) {
        if (entity != null && vehicle != null) {
            entityIndex.put(entity.getUniqueId(), vehicle);
        }
    }

    public void setRider(Player player, Vehicle vehicle) {
        riderIndex.put(player.getUniqueId(), vehicle);
    }

    public void clearRider(Player player) {
        if (player != null) {
            riderIndex.remove(player.getUniqueId());
        }
    }

    /** Returns the vehicle a player is riding, verified against live passengers. */
    public Vehicle getVehicleOf(Player player) {
        if (player == null) {
            return null;
        }
        Vehicle vehicle = riderIndex.get(player.getUniqueId());
        if (vehicle != null && vehicle.getState() == VehicleState.ACTIVE && vehicle.isRider(player)) {
            return vehicle;
        }
        if (vehicle != null) {
            riderIndex.remove(player.getUniqueId());
        }
        Entity mount = player.getVehicle();
        if (mount != null) {
            Vehicle byEntity = entityIndex.get(mount.getUniqueId());
            if (byEntity != null && byEntity.getState() == VehicleState.ACTIVE && byEntity.isRider(player)) {
                riderIndex.put(player.getUniqueId(), byEntity);
                return byEntity;
            }
        }
        return null;
    }

    public Vehicle getVehicleOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        Vehicle vehicle = entityIndex.get(entity.getUniqueId());
        if (vehicle != null && vehicle.getState() == VehicleState.ACTIVE) {
            return vehicle;
        }
        return null;
    }

    public boolean isDriver(Player player) {
        Vehicle vehicle = getVehicleOf(player);
        return vehicle != null && vehicle.isDriver(player);
    }

    public boolean isRider(Player player) {
        return getVehicleOf(player) != null;
    }

    public int countByOwner(UUID owner) {
        if (owner == null) {
            return 0;
        }
        int count = 0;
        for (Vehicle vehicle : vehicles.values()) {
            if (owner.equals(vehicle.getOwner())) {
                count++;
            }
        }
        return count;
    }

    public Vehicle nearestVehicle(Location location, double radius, Player ownerOnly) {
        Vehicle best = null;
        double bestSq = radius * radius;
        for (Vehicle vehicle : vehicles.values()) {
            if (ownerOnly != null && !ownerOnly.hasPermission("vehicle.admin")
                    && !ownerOnly.getUniqueId().equals(vehicle.getOwner())) {
                continue;
            }
            if (!vehicle.getWorld().equals(location.getWorld())) {
                continue;
            }
            double distSq = vehicle.getLocation().distanceSquared(location);
            if (distSq < bestSq) {
                bestSq = distSq;
                best = vehicle;
            }
        }
        return best;
    }

    public List<Vehicle> vehiclesOf(Player player) {
        List<Vehicle> mine = new ArrayList<>();
        for (Vehicle vehicle : vehicles.values()) {
            if (player.getUniqueId().equals(vehicle.getOwner())) {
                mine.add(vehicle);
            }
        }
        return mine;
    }

    // ------------------------------------------------------------------
    // Vehicle-vs-vehicle collision (runs 4x per second)
    // ------------------------------------------------------------------

    private void collideVehicles() {
        List<Vehicle> list = new ArrayList<>(vehicles.values());
        for (int i = 0; i < list.size(); i++) {
            Vehicle a = list.get(i);
            if (a.getState() != VehicleState.ACTIVE) {
                continue;
            }
            for (int j = i + 1; j < list.size(); j++) {
                Vehicle b = list.get(j);
                if (b.getState() != VehicleState.ACTIVE || !a.getWorld().equals(b.getWorld())) {
                    continue;
                }
                double dx = b.getLocation().getX() - a.getLocation().getX();
                double dz = b.getLocation().getZ() - a.getLocation().getZ();
                double dy = Math.abs(b.getLocation().getY() - a.getLocation().getY());
                if (dy > 3) {
                    continue;
                }
                double ra = a.getStats().length * 0.35 + 0.6;
                double rb = b.getStats().length * 0.35 + 0.6;
                double distSq = dx * dx + dz * dz;
                double minDist = ra + rb;
                if (distSq >= minDist * minDist || distSq < 0.0001) {
                    continue;
                }
                double dist = Math.sqrt(distSq);
                double overlap = minDist - dist;
                double nx = dx / dist;
                double nz = dz / dist;
                double massA = a.getStats().mass(a.getType().getTier());
                double massB = b.getStats().mass(b.getType().getTier());
                double total = massA + massB;
                // The lighter vehicle is pushed further.
                a.getLocation().add(-nx * overlap * (massB / total), 0, -nz * overlap * (massB / total));
                b.getLocation().add(nx * overlap * (massA / total), 0, nz * overlap * (massA / total));

                double relSpeed = Math.abs(a.getSpeed()) + Math.abs(b.getSpeed());
                if (relSpeed > 7) {
                    a.damage((relSpeed - 6) * 1.5 / massA, b.getDriver());
                    b.damage((relSpeed - 6) * 1.5 / massB, a.getDriver());
                    plugin.getSoundManager().crash(a, relSpeed);
                    a.setSpeed(a.getSpeed() * -0.25);
                    b.setSpeed(b.getSpeed() * -0.25);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Spawner items
    // ------------------------------------------------------------------

    public ItemStack createSpawnerItem(VehicleType type) {
        ItemStack item = new ItemStack(Material.MINECART);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.GOLD + type.getDisplayName() + " Spawner");
            meta.setLore(Arrays.asList(
                    org.bukkit.ChatColor.GRAY + "Right-click a block to spawn",
                    org.bukkit.ChatColor.GRAY + "a " + type.getDisplayName() + ".",
                    org.bukkit.ChatColor.DARK_GRAY + type.getId()));
            meta.getPersistentDataContainer().set(VehicleTags.SPAWNER_TYPE,
                    PersistentDataType.STRING, type.getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    // ------------------------------------------------------------------
    // Listeners
    // ------------------------------------------------------------------

    /** Right-click a vehicle body or seat to board it. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Vehicle vehicle = getVehicleOf(event.getRightClicked());
        if (vehicle == null) {
            return;
        }
        event.setCancelled(true);
        vehicle.enter(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        Vehicle vehicle = getVehicleOf(event.getDismounted());
        if (vehicle == null || vehicle.isExiting() || !vehicle.isRider(player)) {
            return;
        }
        if (shouldCancelDismount(vehicle, player)) {
            // Sneak doubles as the brake: stay seated while moving.
            event.setCancelled(true);
            return;
        }
        boolean exited = vehicle.exit(player);
        if (!exited) {
            event.setCancelled(true);
        }
    }

    private boolean shouldCancelDismount(Vehicle vehicle, Player player) {
        InputState state = plugin.getInputManager().getState(player);
        boolean enhanced = state.enhanced
                && System.currentTimeMillis() - state.lastEnhancedInput < 1500;
        if (enhanced) {
            return false;
        }
        if (vehicle.getType().getPhysics() == PhysicsType.AIRCRAFT) {
            // Aircraft: sneak is descend, Q (or /vehicle exit) leaves.
            return true;
        }
        return Math.abs(vehicle.getSpeed()) > 1.0;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Vehicle vehicle = riderIndex.get(player.getUniqueId());
        if (vehicle != null) {
            try {
                vehicle.exit(player);
            } catch (Exception ignored) {
            }
            clearRider(player);
        }
        plugin.getInputManager().clearState(player);
    }

    /** Spawner item use: right-click a block to deploy the vehicle. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (isRider(event.getPlayer())) {
            return;
        }
        if (event.getItem() == null || event.getClickedBlock() == null) {
            return;
        }
        ItemMeta meta = event.getItem().getItemMeta();
        if (meta == null || !meta.getPersistentDataContainer()
                .has(VehicleTags.SPAWNER_TYPE, PersistentDataType.STRING)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("vehicle.spawn")) {
            MessageUtil.send(player, "&cYou cannot spawn vehicles.");
            plugin.getSoundManager().deny(player);
            return;
        }
        String typeId = meta.getPersistentDataContainer()
                .get(VehicleTags.SPAWNER_TYPE, PersistentDataType.STRING);
        VehicleType type = VehicleType.byId(typeId);
        if (type == null) {
            return;
        }
        if (!canSpawn(player, type)) {
            return;
        }
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());
        Location spawn = new Location(block.getWorld(),
                block.getX() + 0.5, block.getY(), block.getZ() + 0.5,
                player.getLocation().getYaw(), 0f);
        Vehicle vehicle = spawn(type, spawn, player);
        if (vehicle == null) {
            return;
        }
        MessageUtil.send(player, "&aSpawned &f" + type.getDisplayName() + "&a.");
        plugin.getSoundManager().chime(player);
        if (plugin.getPluginConfig().isConsumeSpawner()) {
            ItemStack item = event.getItem();
            item.setAmount(item.getAmount() - 1);
        }
    }
}
