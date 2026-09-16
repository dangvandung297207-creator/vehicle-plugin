package com.wave100.network;

import com.wave100.WaveMod;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Network channel setup and server-side handling of driver input.
 */
@EventBusSubscriber(modid = WaveMod.MODID)
public final class WaveNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(WaveInputPayload.TYPE, WaveInputPayload.STREAM_CODEC, WaveNetwork::handleInput);
    }

    /**
     * Applies driver input to the motorcycle the sending player is riding.
     * Anything else is ignored, so a modified client cannot drive a vehicle it
     * is not sitting on.
     */
    private static void handleInput(WaveInputPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof WaveMotorcycleEntity bike && bike.isDriver(player)) {
            bike.applyDriverInput(payload);
        }
    }

    private WaveNetwork() {
    }
}
