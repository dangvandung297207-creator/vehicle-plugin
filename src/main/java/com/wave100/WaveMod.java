package com.wave100;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wave 100 - a rideable Honda Wave 100 style underbone motorcycle.
 *
 * <p>Entry point. All gameplay systems live in dedicated classes:</p>
 * <ul>
 *   <li>{@code WaveMotorcycleEntity} - the vehicle entity (physics, state, riding)</li>
 *   <li>{@code WavePhysics} - velocity based motorcycle physics</li>
 *   <li>{@code WaveConfig} - all tunable gameplay values</li>
 *   <li>{@code WaveNetwork} - client input / server authority networking</li>
 *   <li>client package - rendering, model, animation, HUD, sounds (client only)</li>
 * </ul>
 */
@Mod(WaveMod.MODID)
public class WaveMod {
    public static final String MODID = "wave100";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public WaveMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wave 100 motorcycle loading - vroom vroom");
    }
}
