package com.trafficsense.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a bidirectional road with two lanes (one per direction).
 */
public class Road {
    private final String id;
    private final String name;
    private final Junction junctionA;
    private final Junction junctionB;
    private final double length;      // meters
    private final double speedLimit;  // km/h
    private final int capacity;       // vehicles per lane
    private final List<Point2D.Double> polyline;
    
    private final Lane forwardLane;   // A -> B
    private final Lane backwardLane;  // B -> A
    
    private volatile boolean blocked = false;

    public Road(String id, String name, Junction junctionA, Junction junctionB,
                double length, double speedLimit, int capacity, List<Point2D.Double> polyline) {
        this.id = id;
        this.name = name;
        this.junctionA = junctionA;
        this.junctionB = junctionB;
        this.length = length;
        this.speedLimit = speedLimit;
        this.capacity = capacity;
        this.polyline = new ArrayList<>(polyline);

        this.forwardLane = new Lane(id + "_AB", this, junctionA, junctionB, true);
        this.backwardLane = new Lane(id + "_BA", this, junctionB, junctionA, false);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Junction getJunctionA() { return junctionA; }
    public Junction getJunctionB() { return junctionB; }
    public double getLength() { return length; }
    public double getSpeedLimit() { return speedLimit; }
    public int getCapacity() { return capacity; }
    public List<Point2D.Double> getPolyline() { return Collections.unmodifiableList(polyline); }

    public Lane getForwardLane() { return forwardLane; }
    public Lane getBackwardLane() { return backwardLane; }

    public Lane getLaneInDirection(Junction from, Junction to) {
        if (junctionA.equals(from) && junctionB.equals(to)) return forwardLane;
        if (junctionB.equals(from) && junctionA.equals(to)) return backwardLane;
        return null;
    }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public void toggleBlocked() { this.blocked = !this.blocked; }

    public int getTotalLoad() {
        return forwardLane.getLoad() + backwardLane.getLoad();
    }

    public double getMaxCongestionRatio() {
        return Math.max(forwardLane.getCongestionRatio(), backwardLane.getCongestionRatio());
    }

    @Override
    public String toString() {
        return id + " (" + junctionA.getId() + " <-> " + junctionB.getId() + ")";
    }
}
