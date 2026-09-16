package com.wave100.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;

/**
 * Key conflict context that is only active while the local player is riding a
 * Wave motorcycle. Bindings in this context (headlight, engine) therefore only
 * trigger on the bike and stay free for other uses elsewhere.
 */
public enum VehicleKeyConflictContext implements IKeyConflictContext {

    INSTANCE;

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        Entity vehicle = mc.player.getVehicle();
        return vehicle != null && vehicle.isAlive() && vehicle instanceof com.wave100.entity.WaveMotorcycleEntity;
    }

    @Override
    public boolean conflictsDefault() {
        // the engine/headlight keys sit on F/R by default, which overlap vanilla
        // in-game bindings - we deliberately suppress those while riding
        return true;
    }
}
