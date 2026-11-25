package com.trafficsense.model;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a simulated vehicle navigating through the road network.
 */
public class Car {
    public enum Status {
        DRIVING,
        WAITING_AT_JUNCTION,
        ARRIVED,
        STUCK
    }

    private final String id;
    private final Color color;
    private final Junction origin;
    private Junction destination;
    
    private Lane currentLane;
    private double position; // meters along currentLane [0, lane.length]
    private double speed;    // m/s
    private Status status = Status.DRIVING;
    
    // Physics dimensions
    private final double length = 4.5; // car length in meters
    private final double safeGap = 2.5; // minimum bumper-to-bumper buffer in meters

    // Navigation and metrics
    private final NavigationClient navigationClient;
    private double tripStartTime;
    private double totalTripTime = 0.0;
    private double distanceTraveled = 0.0;
    private double waitingTimeAtJunction = 0.0;
    
    // Drag state for interactive UI
    private boolean isBeingDragged = false;
    private Point2D.Double dragPosition = null;

    public Car(String id, Color color, Junction origin, Junction destination, double simTime) {
        this.id = id;
        this.color = color;
        this.origin = origin;
        this.destination = destination;
        this.tripStartTime = simTime;
        this.navigationClient = new NavigationClient(this);
    }

    public String getId() { return id; }
    public Color getColor() { return color; }
    public Junction getOrigin() { return origin; }
    public Junction getDestination() { return destination; }
    public void setDestination(Junction dest) { this.destination = dest; }

    public Lane getCurrentLane() { return currentLane; }
    public double getPosition() { return position; }
    public void setPosition(double position) { this.position = position; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getLength() { return length; }
    public double getSafeGap() { return safeGap; }
    public NavigationClient getNavigationClient() { return navigationClient; }

    public double getTripStartTime() { return tripStartTime; }
    public void setTripStartTime(double simTime) { this.tripStartTime = simTime; }
    public double getTotalTripTime() { return totalTripTime; }
    public void setTotalTripTime(double time) { this.totalTripTime = time; }
    public double getDistanceTraveled() { return distanceTraveled; }
    public void addDistance(double delta) { this.distanceTraveled += delta; }

    public double getWaitingTimeAtJunction() { return waitingTimeAtJunction; }
    public void setWaitingTimeAtJunction(double t) { this.waitingTimeAtJunction = t; }
    public void addWaitingTimeAtJunction(double dt) { this.waitingTimeAtJunction += dt; }

    public boolean isBeingDragged() { return isBeingDragged; }
    public void setBeingDragged(boolean beingDragged) { this.isBeingDragged = beingDragged; }
    public Point2D.Double getDragPosition() { return dragPosition; }
    public void setDragPosition(Point2D.Double dragPosition) { this.dragPosition = dragPosition; }

    public synchronized void enterLane(Lane lane, double initialPosition) {
        if (this.currentLane != null) {
            this.currentLane.removeCar(this);
        }
        this.currentLane = lane;
        this.position = initialPosition;
        if (lane != null) {
            lane.addCar(this);
        }
        this.waitingTimeAtJunction = 0.0;
        this.navigationClient.advanceLane();
    }

    public synchronized void leaveCurrentLane() {
        if (this.currentLane != null) {
            this.currentLane.removeCar(this);
            this.currentLane = null;
        }
    }

    public Point2D.Double getVisualPosition() {
        if (isBeingDragged && dragPosition != null) {
            return dragPosition;
        }
        if (currentLane != null) {
            return currentLane.getPositionAtOffset(position);
        }
        if (origin != null) {
            return origin.getLocation();
        }
        return new Point2D.Double(0, 0);
    }

    public double getVisualHeading() {
        if (currentLane != null) {
            return currentLane.getHeading();
        }
        return 0.0;
    }

    public List<Lane> getRoute() {
        return navigationClient.getPlannedRoute();
    }

    public void setRoute(List<Lane> route, double estimatedSeconds, String reason) {
        navigationClient.updateRoute(route, estimatedSeconds, reason);
    }

    @Override
    public String toString() {
        return "Car[" + id + ", status=" + status + ", speed=" + String.format("%.1f", speed * 3.6) + "km/h]";
    }
}
