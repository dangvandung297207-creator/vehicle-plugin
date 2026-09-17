package com.wave100;

import com.wave100.command.WaveCommands;
import com.wave100.registry.WaveCreativeTabs;
import com.wave100.registry.WaveEntities;
import com.wave100.registry.WaveItems;
import com.wave100.registry.WaveSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wave 100 - a rideable Honda Wave 100 style underbone motorcycle.
 *
 * <p>Entry point. All gameplay systems live in dedicated classes:</p>
 * <ul>
 *   <li>{@code WaveMotorcycleEntity} - the vehicle entity (physics, state, riding)</li>
 *   <li>{@code WavePhysics} - velocity based motorcycle physics + wheelie balance</li>
 *   <li>{@code WaveEngine} - RPM + automatic gearbox simulation</li>
 *   <li>{@code WaveConfig} - all tunable gameplay values</li>
 *   <li>{@code WaveNetwork} - client input / server authority networking</li>
 *   <li>{@code WaveCommands} - /wave admin commands</li>
 *   <li>{@code client} package - rendering, model, animation, HUD, sounds (client only)</li>
 * </ul>
 */
@Mod(WaveMod.MODID)
public class WaveMod {
    public static final String MODID = "wave100";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public WaveMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Wave 100 motorcycle loading - vroom vroom");

        modContainer.registerConfig(ModConfig.Type.COMMON, WaveConfig.SPEC);

        WaveSounds.SOUND_EVENTS.register(modEventBus);
        WaveEntities.ENTITY_TYPES.register(modEventBus);
        WaveItems.ITEMS.register(modEventBus);
        WaveCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // game bus (server + client runtime events)
        NeoForge.EVENT_BUS.addListener(WaveCommands::register);
    }

    /** Creates a namespaced id, e.g. {@code wave100:wave_motorcycle}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
