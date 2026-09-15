package com.example.vanillavehicles.vehicle;

/** Broad family a vehicle belongs to. */
public enum VehicleCategory {
    GROUND("Ground"),
    UTILITY("Utility"),
    WATER("Water"),
    AIR("Air"),
    RAIL("Rail");

    private final String displayName;

    VehicleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
