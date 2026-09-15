package com.example.vanillavehicles.vehicle;

/**
 * Distance-based fare meter for the taxi. Deliberately decoupled from any
 * economy plugin: it only tracks distance and computes a fare, leaving
 * charging to a future integration through the API.
 */
public class TaxiMeter {

    private final double ratePerBlock;
    private double distance;
    private boolean running;

    public TaxiMeter(double ratePerBlock) {
        this.ratePerBlock = ratePerBlock;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void addDistance(double blocks) {
        if (running && blocks > 0) {
            distance += blocks;
        }
    }

    public double fare() {
        return distance * ratePerBlock;
    }

    public double getDistance() {
        return distance;
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        distance = 0;
    }
}
