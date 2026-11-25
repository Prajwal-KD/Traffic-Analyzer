package com.trafficsense.routing;

import com.trafficsense.model.Car;
import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;

import java.util.List;

/**
 * Interface defining the routing engine strategy.
 */
public interface RoutingEngine {
    /**
     * Type identifier for the engine.
     */
    RoutingEngineType getType();

    /**
     * Compute route (ordered list of directional Lanes) from origin to destination.
     */
    List<Lane> computeRoute(Junction origin, Junction destination, TrafficNetwork network, double currentSimTime, Car car);

    /**
     * Hook called on each simulation tick (e.g., to handle periodic rerouting).
     */
    void onSimulationTick(double simTime, double dt, TrafficNetwork network, List<Car> activeCars);

    /**
     * Hook called immediately when an event occurs (road closure, car dragged, new car spawned).
     */
    void onNetworkChanged(TrafficNetwork network, List<Car> activeCars);
}
