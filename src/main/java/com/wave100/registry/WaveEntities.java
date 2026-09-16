package com.wave100.registry;

import com.wave100.WaveMod;
import com.wave100.entity.WaveMotorcycleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Entity type registration for the Wave motorcycle.
 *
 * <p>{@code setUpdateInterval(1)} makes the server sync position every tick so
 * the ridden vehicle looks perfectly smooth, while remaining fully
 * server-authoritative (minecart-style, not boat-style).</p>
 */
public final class WaveEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, WaveMod.MODID);

    public static final DeferredHolder<EntityType<WaveMotorcycleEntity>, EntityType<WaveMotorcycleEntity>> WAVE_MOTORCYCLE =
            ENTITY_TYPES.register("wave_motorcycle", () -> EntityType.Builder
                    .<WaveMotorcycleEntity>of(WaveMotorcycleEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.35F)
                    .clientTrackingRange(10)
                    .setUpdateInterval(1)
                    .build("wave_motorcycle"));

    private WaveEntities() {
    }
}
