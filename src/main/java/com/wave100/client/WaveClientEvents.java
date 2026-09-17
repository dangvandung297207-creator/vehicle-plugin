package com.wave100.client;

import com.wave100.WaveMod;
import com.wave100.registry.WaveEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/**
 * Client-only registration on the MOD event bus: model layers, entity
 * renderer, key mappings and the HUD GUI layer. Nothing here loads on a
 * dedicated server (guarded by {@link Dist#CLIENT}).
 */
@EventBusSubscriber(modid = WaveMod.MODID, value = Dist.CLIENT)
public final class WaveClientEvents {

    private WaveClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WaveModel.LAYER, WaveModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WaveEntities.WAVE_MOTORCYCLE.get(), WaveMotorcycleRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(WaveKeyBindings.TOGGLE_ENGINE);
        event.register(WaveKeyBindings.TOGGLE_HEADLIGHT);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(WaveMod.id("hud"), WaveHUD::render);
    }
}
