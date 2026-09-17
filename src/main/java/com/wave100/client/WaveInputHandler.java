package com.wave100.client;

import com.wave100.entity.WaveMotorcycleEntity;
import com.wave100.network.WaveInputPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Sends driver input to the server while riding.
 *
 * <p>Runs in {@link ClientTickEvent.Pre} (before vanilla handles keybinds) so
 * the vanilla offhand swap - which shares our default F key - can be consumed
 * and discarded while riding, leaving F free for the headlight.</p>
 *
 * <p>The payload is only sent when the input state actually changes; the
 * server clamps and validates everything anyway (server-authoritative).</p>
 */
@EventBusSubscriber(modid = com.wave100.WaveMod.MODID)
public final class WaveInputHandler {

    private static byte lastFlags = -1;

    private WaveInputHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            lastFlags = -1;
            return;
        }

        if (!(player.getVehicle() instanceof WaveMotorcycleEntity bike) || bike.isCrashed()) {
            lastFlags = -1;
            return;
        }

        // eat vanilla offhand-swap clicks while riding (F is the headlight)
        while (mc.options.keySwapOffhand.consumeClick()) {
            // discard
        }

        byte flags = 0;
        if (mc.options.keyUp.isDown()) {
            flags |= WaveInputPayload.FORWARD;
        }
        if (mc.options.keyDown.isDown()) {
            flags |= WaveInputPayload.BACK;
        }
        if (mc.options.keyLeft.isDown()) {
            flags |= WaveInputPayload.LEFT;
        }
        if (mc.options.keyRight.isDown()) {
            flags |= WaveInputPayload.RIGHT;
        }
        if (mc.options.keyJump.isDown()) {
            flags |= WaveInputPayload.WHEELIE;
        }
        if (WaveKeyBindings.TOGGLE_ENGINE.consumeClick()) {
            flags |= WaveInputPayload.TOGGLE_ENGINE;
        }
        if (WaveKeyBindings.TOGGLE_HEADLIGHT.consumeClick()) {
            flags |= WaveInputPayload.TOGGLE_HEADLIGHT;
        }

        if (flags != lastFlags) {
            PacketDistributor.sendToServer(new WaveInputPayload(flags));
            lastFlags = flags;
        }
    }
}
