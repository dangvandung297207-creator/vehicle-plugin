package com.wavemotorcycle.motorcycle.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wavemotorcycle.motorcycle.EngineState;
import com.wavemotorcycle.motorcycle.Motorcycle;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

/**
 * JSON persistence for motorcycles ({@code plugins/WaveMotorcycle/data/motorcycles.json}).
 *
 * <p>Writes are atomic (temp file + rename) so a crash cannot corrupt the file.
 */
public final class MotorcyclePersistence {

    private static final Gson GSON = new GsonBuilder().create();

    private final File file;

    public MotorcyclePersistence(Plugin plugin) {
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Could not create data directory " + dir.getAbsolutePath());
        }
        this.file = new File(dir, "motorcycles.json");
    }

    public List<Motorcycle> load(Plugin plugin) {
        List<Motorcycle> bikes = new ArrayList<>();
        if (!file.exists()) {
            return bikes;
        }
        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            JsonArray arr = root.getAsJsonArray("bikes");
            if (arr == null) {
                return bikes;
            }
            for (JsonElement el : arr) {
                try {
                    JsonObject o = el.getAsJsonObject();
                    Motorcycle m = new Motorcycle(UUID.fromString(o.get("uuid").getAsString()));
                    if (o.has("owner")) {
                        m.owner(UUID.fromString(o.get("owner").getAsString()));
                    }
                    m.worldName(o.get("world").getAsString());
                    m.position(o.get("x").getAsDouble(), o.get("y").getAsDouble(), o.get("z").getAsDouble());
                    m.yaw(o.get("yaw").getAsFloat());
                    m.fuel(o.has("fuel") ? o.get("fuel").getAsDouble() : 100.0);
                    m.health(o.has("health") ? o.get("health").getAsDouble() : 100.0);
                    m.headlightOn(o.has("headlight") && o.get("headlight").getAsBoolean());
                    if (o.has("engine")) {
                        try {
                            m.engine(EngineState.valueOf(o.get("engine").getAsString()));
                        } catch (IllegalArgumentException ignored) {
                            m.engine(EngineState.OFF);
                        }
                    }
                    m.created(o.has("created") ? o.get("created").getAsLong() : System.currentTimeMillis());
                    bikes.add(m);
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipping corrupted motorcycle record: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to read motorcycles.json: " + e.getMessage());
            try {
                File backup = new File(file.getParentFile(), "motorcycles.json.bak");
                Files.move(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().severe("Moved unreadable file to " + backup.getName());
            } catch (IOException ignored) {
            }
        }
        return bikes;
    }

    public void save(Plugin plugin, List<Motorcycle> bikes) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray arr = new JsonArray();
        for (Motorcycle m : bikes) {
            JsonObject o = new JsonObject();
            o.addProperty("uuid", m.uuid().toString());
            if (m.owner() != null) {
                o.addProperty("owner", m.owner().toString());
            }
            o.addProperty("world", m.worldName());
            o.addProperty("x", m.x());
            o.addProperty("y", m.y());
            o.addProperty("z", m.z());
            o.addProperty("yaw", m.yaw());
            o.addProperty("fuel", m.fuel());
            o.addProperty("health", m.health());
            o.addProperty("headlight", m.headlightOn());
            o.addProperty("engine", m.engine().name());
            o.addProperty("created", m.created());
            arr.add(o);
        }
        root.add("bikes", arr);
        try {
            File tmp = new File(file.getParentFile(), "motorcycles.json.tmp");
            Files.writeString(tmp.toPath(), GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save motorcycles.json: " + e.getMessage());
        }
    }
}
