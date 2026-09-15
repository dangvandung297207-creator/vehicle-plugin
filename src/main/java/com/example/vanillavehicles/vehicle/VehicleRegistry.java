package com.example.vanillavehicles.vehicle;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Holds all registered vehicle definitions by type id. */
public class VehicleRegistry {

    private final Map<String, VehicleDefinition> definitions = new LinkedHashMap<>();

    public void register(VehicleDefinition definition) {
        definitions.put(definition.getType().getId(), definition);
    }

    public VehicleDefinition get(VehicleType type) {
        return type == null ? null : definitions.get(type.getId());
    }

    public VehicleDefinition get(String id) {
        return definitions.get(id);
    }

    public Collection<VehicleDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public int size() {
        return definitions.size();
    }
}
