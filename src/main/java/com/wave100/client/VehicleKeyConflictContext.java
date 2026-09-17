package com.wave100.client;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

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
        return mc.player.getVehicle() instanceof com.wave100.entity.WaveMotorcycleEntity;
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        // while riding, the in-game context is active too, so vanilla bindings
        // on the same physical key are real conflicts (we suppress the vanilla
        // offhand swap while riding - see WaveInputHandler)
        return other == this
                || other == KeyConflictContext.IN_GAME
                || other == KeyConflictContext.UNIVERSAL;
    }
}
