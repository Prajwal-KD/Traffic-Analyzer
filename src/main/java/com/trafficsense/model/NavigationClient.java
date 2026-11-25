package com.trafficsense.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Separate car navigation system client embedded in each car.
 * Receives route updates from the active routing engine and exposes ETA,
 * next turn direction, and human-readable explanation for reroutes.
 */
public class NavigationClient {
    private final Car car;
    private List<Lane> plannedRoute = new ArrayList<>();
    private double estimatedTimeOfArrival = 0.0; // remaining seconds
    private String nextTurn = "Calculating...";
    private String rerouteReason = "Initial route calculated";
    private double lastPlannedCost = 0.0;

    public NavigationClient(Car car) {
        this.car = car;
    }

    public Car getCar() { return car; }

    public synchronized List<Lane> getPlannedRoute() {
        return Collections.unmodifiableList(plannedRoute);
    }

    public synchronized double getETA() {
        return estimatedTimeOfArrival;
    }

    public synchronized String getNextTurn() {
        return nextTurn;
    }

    public synchronized String getRerouteReason() {
        return rerouteReason;
    }

    public synchronized double getLastPlannedCost() {
        return lastPlannedCost;
    }

    /**
     * Called by routing engines to deliver route updates.
     */
    public synchronized void updateRoute(List<Lane> newRoute, double estimatedSeconds, String reason) {
        this.plannedRoute = new ArrayList<>(newRoute);
        this.estimatedTimeOfArrival = estimatedSeconds;
        this.lastPlannedCost = estimatedSeconds;
        if (reason != null && !reason.isBlank()) {
            this.rerouteReason = reason;
        }
        updateNextTurnInstruction();
    }

    public synchronized void setETA(double eta) {
        this.estimatedTimeOfArrival = Math.max(0.0, eta);
    }

    public synchronized void updateNextTurnInstruction() {
        if (plannedRoute.isEmpty()) {
            nextTurn = "Arriving at destination";
            return;
        }
        Lane currentLane = car.getCurrentLane();
        if (currentLane == null) {
            nextTurn = "Proceed onto " + plannedRoute.get(0).getId();
            return;
        }

        int currentIndex = plannedRoute.indexOf(currentLane);
        if (currentIndex < 0) {
            nextTurn = "Route deviation - enter " + plannedRoute.get(0).getId();
        } else if (currentIndex == plannedRoute.size() - 1) {
            nextTurn = "Arriving at " + currentLane.getToJunction().getId();
        } else {
            Lane nextLane = plannedRoute.get(currentIndex + 1);
            nextTurn = "At " + currentLane.getToJunction().getId() + " proceed to " + nextLane.getToJunction().getId();
        }
    }

    public synchronized void advanceLane() {
        updateNextTurnInstruction();
    }
}
