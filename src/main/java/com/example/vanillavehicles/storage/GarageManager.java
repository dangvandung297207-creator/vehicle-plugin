package com.example.vanillavehicles.storage;

import com.example.vanillavehicles.VanillaVehicles;
import com.example.vanillavehicles.entity.DisplayFactory;
import com.example.vanillavehicles.entity.VehicleTags;
import com.example.vanillavehicles.vehicle.Vehicle;
import com.example.vanillavehicles.vehicle.VehicleDefinition;
import com.example.vanillavehicles.vehicle.VehicleManager;
import com.example.vanillavehicles.vehicle.VehicleStats;
import com.example.vanillavehicles.vehicle.VehicleType;
import com.example.vanillavehicles.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Click-to-spawn garage GUI. Lists every enabled vehicle the player may use.
 * Kept modular so an economy layer can hook in later.
 */
public class GarageManager implements Listener {

    public static class GarageHolder implements InventoryHolder {
        private Inventory inventory;

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final VanillaVehicles plugin;

    public GarageManager(VanillaVehicles plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    public void open(Player player) {
        GarageHolder holder = new GarageHolder();
        Inventory inventory = org.bukkit.Bukkit.createInventory(holder, 54,
                plugin.getPluginConfig().getGarageTitle());
        holder.setInventory(inventory);

        for (VehicleDefinition definition : plugin.getRegistry().all()) {
            VehicleType type = definition.getType();
            VehicleStats stats = plugin.getPluginConfig().statsFor(type);
            if (!stats.enabled) {
                continue;
            }
            if (!stats.permission.isEmpty() && !player.hasPermission(stats.permission)) {
                continue;
            }
            Material icon = DisplayFactory.resolveMaterial(definition.getMenuIcon(), Material.MINECART);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + type.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + type.getDescription());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Top speed: " + ChatColor.WHITE
                        + String.format("%.0f m/s", stats.maxSpeed));
                lore.add(ChatColor.YELLOW + "Seats: " + ChatColor.WHITE
                        + definition.getModel().seatCount());
                lore.add(ChatColor.YELLOW + "Health: " + ChatColor.WHITE
                        + (int) stats.maxHealth);
                lore.add(ChatColor.YELLOW + "Class: " + ChatColor.WHITE
                        + type.getTier().getDisplayName());
                lore.add("");
                lore.add(ChatColor.AQUA + "Click to spawn");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(VehicleTags.SPAWNER_TYPE,
                        PersistentDataType.STRING, type.getId());
                item.setItemMeta(meta);
            }
            inventory.addItem(item);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "Close");
            close.setItemMeta(closeMeta);
        }
        inventory.setItem(49, close);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GarageHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        if (current.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        ItemMeta meta = current.getItemMeta();
        if (meta == null) {
            return;
        }
        String typeId = meta.getPersistentDataContainer()
                .get(VehicleTags.SPAWNER_TYPE, PersistentDataType.STRING);
        if (typeId == null) {
            return;
        }
        VehicleType type = VehicleType.byId(typeId);
        if (type == null) {
            return;
        }
        if (!player.hasPermission("vehicle.spawn")) {
            MessageUtil.send(player, "&cYou cannot spawn vehicles.");
            plugin.getSoundManager().deny(player);
            return;
        }
        VehicleManager manager = plugin.getVehicleManager();
        if (!manager.canSpawn(player, type)) {
            return;
        }
        Location base = player.getLocation();
        Vector direction = base.getDirection();
        Location spawn = new Location(base.getWorld(),
                base.getX() + direction.getX() * 3.5, base.getY(), base.getZ() + direction.getZ() * 3.5,
                base.getYaw(), 0f);
        Vehicle vehicle = manager.spawn(type, spawn, player);
        if (vehicle != null) {
            player.closeInventory();
            MessageUtil.send(player, "&aSpawned &f" + type.getDisplayName()
                    + "&a. Right-click it to board.");
            plugin.getSoundManager().chime(player);
        }
    }
}
