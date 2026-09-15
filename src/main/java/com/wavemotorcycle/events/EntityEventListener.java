package com.wavemotorcycle.events;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.motorcycle.Keys;
import com.wavemotorcycle.motorcycle.MotorcycleController;
import java.util.UUID;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.persistence.PersistentDataType;

/** Entity-side protection and interaction: dismounts, targeting, projectiles, explosions. */
public final class EntityEventListener implements Listener {

    private final WaveMotorcyclePlugin plugin;

    public EntityEventListener(WaveMotorcyclePlugin plugin) {
        this.plugin = plugin;
    }

    private MotorcycleController bikeOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        String s = entity.getPersistentDataContainer().get(Keys.BIKE_UUID, PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return plugin.manager().byId(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        // In EntityDismountEvent the vehicle is the event's entity; the
        // passenger that dismounted is getDismounted().
        MotorcycleController bike = bikeOf(event.getEntity());
        if (bike == null) {
            return;
        }
        if (event.getDismounted() instanceof Player p) {
            if (Math.abs(bike.speed()) > plugin.cfg().dismountMaxSpeed) {
                // Too fast to jump off safely.
                event.setCancelled(true);
                p.sendMessage(plugin.cfg().msgC("msg.slow_down"));
                return;
            }
        }
        bike.dismount(MotorcycleController.DismountReason.VANILLA);
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (bikeOf(event.getTarget()) != null) {
            event.setTarget(null);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        MotorcycleController bike = bikeOf(event.getHitEntity());
        if (bike == null) {
            return;
        }
        // Don't let projectiles stick to the invisible stand; treat it as a small impact.
        event.setCancelled(true);
        if (plugin.cfg().damageEnabled) {
            bike.damage(0.5);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (MotorcycleController bike : plugin.manager().all()) {
            if (bike.location().getWorld() == event.getLocation().getWorld()
                    && bike.location().distanceSquared(event.getLocation()) <= 25.0) {
                explodeAt(event.getLocation(), bike);
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        // Blocks near a bike can also damage it (TNT under the chassis etc.).
        org.bukkit.Location at = event.getBlock().getLocation();
        for (MotorcycleController bike : plugin.manager().all()) {
            if (bike.location().getWorld() == at.getWorld()
                    && bike.location().distanceSquared(at) <= 4.0 * 4.0) {
                explodeAt(at, bike);
            }
        }
    }

    private void explodeAt(org.bukkit.Location at, MotorcycleController bike) {
        double d = bike.location().distance(at);
        if (d > 5.0) {
            return;
        }
        double factor = Math.max(0, 1.0 - d / 5.0);
        if (plugin.cfg().damageEnabled) {
            bike.damage(factor * 25.0);
        }
        bike.location().getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, bike.location().add(0, 0.8, 0), 1);
        bike.markDirty();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        // The invisible controller takes no damage at all.
        if (PlayerEventListener.isController(entity)) {
            event.setCancelled(true);
            return;
        }
        // Riders are carried by the bike: vanilla fall damage does not apply.
        if (entity instanceof Player p && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (plugin.manager().ofPlayer(p) != null) {
                p.setFallDistance(0f);
                event.setCancelled(true);
            }
        }
    }
}
