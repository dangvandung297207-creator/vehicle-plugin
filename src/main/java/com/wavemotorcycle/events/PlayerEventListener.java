package com.wavemotorcycle.events;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.motorcycle.Keys;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import com.wavemotorcycle.util.MathUtil;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

/** Player-side events: joining, quitting, death, teleport, input edges and interaction. */
public final class PlayerEventListener implements Listener {

    private final WaveMotorcyclePlugin plugin;

    public PlayerEventListener(WaveMotorcyclePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.packManager().sendPack(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        MotorcycleController bike = plugin.manager().ofPlayer(p);
        if (bike != null) {
            bike.dismount(MotorcycleController.DismountReason.QUIT);
            plugin.manager().queueSave(bike.state());
        }
        plugin.packManager().onQuit(p.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        MotorcycleController bike = plugin.manager().ofPlayer(p);
        if (bike != null) {
            bike.dismount(MotorcycleController.DismountReason.DEATH);
            plugin.manager().queueSave(bike.state());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        MotorcycleController bike = plugin.manager().ofPlayer(p);
        if (bike == null) {
            return;
        }
        // Any teleport while riding detaches the player (the bike stays where it is).
        bike.dismount(MotorcycleController.DismountReason.TELEPORT);
        p.sendMessage(plugin.cfg().msgC("msg.teleport_dismount"));
    }

    @EventHandler
    public void onSprintToggle(PlayerToggleSprintEvent event) {
        Player p = event.getPlayer();
        MotorcycleController bike = plugin.manager().ofPlayer(p);
        if (bike != null) {
            bike.onSprintToggle(p, event.isSprinting());
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        plugin.packManager().onStatus(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR) {
            return;
        }
        Player p = event.getPlayer();
        MotorcycleController riding = plugin.manager().ofPlayer(p);
        if (riding != null) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == plugin.cfg().fuelItem) {
                riding.refuel();
                p.sendMessage(plugin.cfg().msgC("msg.refueled"));
                event.setCancelled(true);
            } else if (riding.toggleHeadlight()) {
                event.setCancelled(true);
            }
            return;
        }
        // Try to mount the nearest bike.
        MotorcycleController near = plugin.manager().nearest(p.getEyeLocation(), plugin.cfg().mountRange);
        if (near != null) {
            if (near.mount(p)) {
                event.setCancelled(true);
            }
            return;
        }
        // Try to spawn from a key.
        ItemStack item = event.getItem();
        if (item != null && plugin.manager().keyItem().isKey(item)) {
            UUID bound = plugin.manager().keyItem().boundBike(item);
            if (bound != null && plugin.manager().byId(bound) != null) {
                p.sendMessage(plugin.cfg().msgC("msg.bike_exists"));
                event.setCancelled(true);
                return;
            }
            Location here = p.getLocation().clone();
            here.add(new Vector(MathUtil.dirX(here.getYaw()) * 1.8, 0, MathUtil.dirZ(here.getYaw()) * 1.8));
            here.setX(here.getX() + 0.5);
            here.setZ(here.getZ() + 0.5);
            here.setPitch(0f);
            plugin.manager().spawn(here, p, bound);
            if (plugin.cfg().consumeKeyOnSpawn) {
                plugin.manager().keyItem().consumeOne(p, item);
            }
            event.setCancelled(true);
        }
    }

    /** True when the entity is one of our invisible controller stands. */
    public static boolean isController(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer().has(Keys.IS_CONTROLLER, PersistentDataType.STRING);
    }
}
