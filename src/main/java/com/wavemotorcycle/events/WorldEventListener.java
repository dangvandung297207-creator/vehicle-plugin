package com.wavemotorcycle.events;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/** World load/unload handling for parked motorcycles. */
public final class WorldEventListener implements Listener {

    private final WaveMotorcyclePlugin plugin;

    public WorldEventListener(WaveMotorcyclePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUnload(WorldUnloadEvent event) {
        plugin.manager().onWorldUnload(event.getWorld());
    }

    @EventHandler
    public void onLoad(WorldLoadEvent event) {
        plugin.manager().onWorldLoad(event.getWorld());
    }
}
