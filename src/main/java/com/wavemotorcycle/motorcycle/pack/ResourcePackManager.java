package com.wavemotorcycle.motorcycle.pack;

import com.wavemotorcycle.WaveMotorcyclePlugin;
import com.wavemotorcycle.config.ConfigManager;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

/**
 * Sends the 3D-model resource pack to players using Paper's modern
 * {@link Player#sendResourcePacks(ResourcePackRequest)} API and tracks per-player
 * pack status from {@link PlayerResourcePackStatusEvent}.
 */
public final class ResourcePackManager {

    private final WaveMotorcyclePlugin plugin;
    private final ConfigManager cfg;
    private final Map<UUID, Status> statuses = new HashMap<>();

    public ResourcePackManager(WaveMotorcyclePlugin plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public boolean canSend() {
        return cfg.packEnabled && cfg.packUrl != null && !cfg.packUrl.isBlank();
    }

    private boolean hasValidSha1() {
        return cfg.packSha1 != null && cfg.packSha1.matches("[0-9a-fA-F]{40}");
    }

    public void sendPack(Player player) {
        if (!canSend()) {
            plugin.getLogger().warning("Resource pack disabled or no URL configured - not sending a pack to " + player.getName());
            return;
        }
        if (!hasValidSha1()) {
            plugin.getLogger().warning("resource-pack.sha1 is missing or not a 40-char SHA-1 - not sending a pack to " + player.getName());
            return;
        }
        try {
            UUID id = UUID.nameUUIDFromBytes(cfg.packUrl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                    .required(cfg.packRequired)
                    .replace(true)
                    .prompt(Component.text(cfg.msg("pack.prompt")))
                    .packs(ResourcePackInfo.resourcePackInfo(id, URI.create(cfg.packUrl.trim()), cfg.packSha1.toLowerCase(java.util.Locale.ROOT)));
            player.sendResourcePacks(request);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send resource pack to " + player.getName() + ": " + e.getMessage());
        }
    }

    public void onStatus(PlayerResourcePackStatusEvent event) {
        Status status = event.getStatus();
        if (status == Status.SUCCESSFULLY_LOADED) {
            statuses.put(event.getPlayer().getUniqueId(), status);
        } else {
            statuses.put(event.getPlayer().getUniqueId(), status);
            plugin.getLogger().info("Resource pack status for " + event.getPlayer().getName() + ": " + status);
            event.getPlayer().sendMessage(cfg.msgC("pack.status_" + status.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    public Status statusOf(UUID player) {
        return statuses.get(player);
    }

    public void onQuit(UUID player) {
        statuses.remove(player);
    }
}
