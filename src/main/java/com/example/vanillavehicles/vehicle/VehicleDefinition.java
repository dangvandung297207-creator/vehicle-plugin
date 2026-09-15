package com.example.vanillavehicles.vehicle;

import com.example.vanillavehicles.model.VehicleModel;

/**
 * Static definition of a vehicle type: its model, seats, optional carriage
 * model for trains, and the garage icon.
 */
public class VehicleDefinition {

    private final VehicleType type;
    private final VehicleModel model;
    private final VehicleModel carriageModel;
    private final String menuIcon;

    public VehicleDefinition(VehicleType type, VehicleModel model, String menuIcon) {
        this(type, model, null, menuIcon);
    }

    public VehicleDefinition(VehicleType type, VehicleModel model,
                             VehicleModel carriageModel, String menuIcon) {
        this.type = type;
        this.model = model;
        this.carriageModel = carriageModel;
        this.menuIcon = menuIcon == null ? "MINECART" : menuIcon;
    }

    public VehicleType getType() {
        return type;
    }

    public VehicleModel getModel() {
        return model;
    }

    public VehicleModel getCarriageModel() {
        return carriageModel;
    }

    public String getMenuIcon() {
        return menuIcon;
    }
}
