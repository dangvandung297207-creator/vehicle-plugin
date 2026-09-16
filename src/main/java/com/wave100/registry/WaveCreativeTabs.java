package com.wave100.registry;

import com.wave100.WaveMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Creative tab holding the motorcycle item.
 */
public final class WaveCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WaveMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WAVE_TAB =
            CREATIVE_MODE_TABS.register("wave_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wave100"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> WaveItems.WAVE_MOTORCYCLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(WaveItems.WAVE_MOTORCYCLE.get()))
                    .build());

    private WaveCreativeTabs() {
    }
}
