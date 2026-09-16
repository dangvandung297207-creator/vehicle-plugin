package com.wave100.network;

import com.wave100.WaveMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client -> server driver input, sent only when the input state changes.
 *
 * <p>The server stays authoritative for all movement; this payload simply
 * mirrors the driver's keybinds (W/A/S/D, SPACE, plus engine/headlight toggle
 * clicks) once per change instead of every tick.</p>
 */
public record WaveInputPayload(byte flags) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<WaveInputPayload> TYPE =
            new CustomPacketPayload.Type<>(WaveMod.id("input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WaveInputPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE,
                    WaveInputPayload::flags,
                    WaveInputPayload::new);

    public static final byte FORWARD = 0x01;
    public static final byte BACK = 0x02;
    public static final byte LEFT = 0x04;
    public static final byte RIGHT = 0x08;
    public static final byte WHEELIE = 0x10;
    public static final byte TOGGLE_ENGINE = 0x20;
    public static final byte TOGGLE_HEADLIGHT = 0x40;

    public WaveInputPayload {
        // keep only the bits we know about
        flags = (byte) (flags & 0x7F);
    }

    public boolean has(byte bit) {
        return (flags & bit) != 0;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
