package com.wavemotorcycle.motorcycle.key;

import com.wavemotorcycle.config.ConfigManager;
import com.wavemotorcycle.motorcycle.Keys;
import com.wavemotorcycle.motorcycle.model.MotorcycleModel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * The "motorcycle key" item.
 *
 * <p>Keys are identified by the plugin PDC marker (plus the custom model data, so
 * even stripped clones are recognized). A key dropped by a destroyed bike is bound
 * to that bike's UUID and re-spawns the same bike.
 */
public final class WaveKeyItem {

    private final ConfigManager cfg;

    public WaveKeyItem(ConfigManager cfg) {
        this.cfg = cfg;
    }

    public ItemStack create(UUID boundBike) {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.customName(Component.text(cfg.msg("key.name"))
                    .color(NamedTextColor.GOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(cfg.msg("key.lore_1")).color(NamedTextColor.GRAY));
            if (boundBike != null) {
                lore.add(Component.text(cfg.msg("key.lore_bound") + " " + boundBike).color(NamedTextColor.DARK_GRAY));
            } else {
                lore.add(Component.text(cfg.msg("key.lore_2")).color(NamedTextColor.GRAY));
            }
            meta.lore(lore);
            meta.setCustomModelData(MotorcycleModel.MODEL_DATA_KEY);
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            stack.setItemMeta(meta);
        }
        if (boundBike != null) {
            stack.getPersistentDataContainer().set(Keys.KEY_BOUND_BIKE, PersistentDataType.STRING, boundBike.toString());
        }
        stack.getPersistentDataContainer().set(Keys.KEY_IS_WAVE_KEY, PersistentDataType.STRING, "1");
        return stack;
    }

    public boolean isKey(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        if (stack.getPersistentDataContainer().has(Keys.KEY_IS_WAVE_KEY, PersistentDataType.STRING)) {
            return true;
        }
        return stack.getType() == Material.NETHER_STAR
                && stack.hasItemMeta()
                && stack.getItemMeta().hasCustomModelData()
                && stack.getItemMeta().getCustomModelData() == MotorcycleModel.MODEL_DATA_KEY;
    }

    /** The bike a key is bound to, or null for a fresh key. */
    public UUID boundBike(ItemStack stack) {
        if (!isKey(stack)) {
            return null;
        }
        String s = stack.getPersistentDataContainer().get(Keys.KEY_BOUND_BIKE, PersistentDataType.STRING);
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Drops a key at the given location (used when a bike is destroyed). */
    public void dropAt(Location location, UUID boundBike) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.dropItemNaturally(location, create(boundBike));
    }

    /** Removes one key from the given source's inventory (spawn consumption). */
    public boolean consumeOne(ProjectileSource source, ItemStack key) {
        if (source instanceof org.bukkit.entity.HumanEntity human) {
            ItemStack[] contents = human.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack s = contents[i];
                if (s != null && isKey(s) && s.isSimilar(key)) {
                    s.setAmount(s.getAmount() - 1);
                    if (s.getAmount() <= 0) {
                        human.getInventory().setItem(i, null);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
