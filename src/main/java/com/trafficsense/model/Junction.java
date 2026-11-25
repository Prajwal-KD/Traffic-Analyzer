package com.trafficsense.model;

import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * Represents a junction or gate node in the traffic network.
 */
public class Junction {
    private final String id;
    private final String label;
    private final Point2D.Double location;
    private final boolean isGate;
    
    // Throughput control: minimum time (seconds) between successive car departures
    private double minDepartureInterval = 0.5; // up to 2 cars/sec
    private double lastDepartureTime = -100.0;
    
    public Junction(String id, String label, double x, double y, boolean isGate) {
        this.id = id;
        this.label = label;
        this.location = new Point2D.Double(x, y);
        this.isGate = isGate;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Point2D.Double getLocation() { return location; }
    public boolean isGate() { return isGate; }

    public double getMinDepartureInterval() { return minDepartureInterval; }
    public void setMinDepartureInterval(double interval) { this.minDepartureInterval = interval; }

    public synchronized boolean canDepart(double simTime) {
        return (simTime - lastDepartureTime) >= minDepartureInterval;
    }

    public synchronized void recordDeparture(double simTime) {
        this.lastDepartureTime = simTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Junction junction)) return false;
        return Objects.equals(id, junction.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
