package com.wave100.registry;

import com.wave100.WaveMod;
import com.wave100.item.WaveItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registration. The single item is the deployable motorcycle itself.
 */
public final class WaveItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WaveMod.MODID);

    public static final DeferredItem<WaveItem> WAVE_MOTORCYCLE =
            ITEMS.register("wave_motorcycle", () -> new WaveItem(new Item.Properties().stacksTo(1)));

    private WaveItems() {
    }
}
