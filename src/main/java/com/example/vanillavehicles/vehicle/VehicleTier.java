package com.example.vanillavehicles.vehicle;

/** Speed / weight class used for balancing and garage display. */
public enum VehicleTier {
    SLOW("Slow"),
    NORMAL("Normal"),
    FAST("Fast"),
    VERY_FAST("Very Fast"),
    HEAVY("Heavy");

    private final String displayName;

    VehicleTier(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
