package com.trafficsense.routing;

import com.trafficsense.model.Car;
import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1. NAIVE Engine:
 * Static shortest-by-distance (Dijkstra on lane length).
 * Never reroutes during driving unless its current path is physically blocked by an accident/closure.
 */
public class NaiveRoutingEngine implements RoutingEngine {

    @Override
    public RoutingEngineType getType() {
        return RoutingEngineType.NAIVE;
    }

    @Override
    public List<Lane> computeRoute(Junction origin, Junction destination, TrafficNetwork network, double currentSimTime, Car car) {
        DijkstraRouter.PathResult result = DijkstraRouter.findPath(
                origin,
                destination,
                network,
                currentSimTime,
                (lane, time) -> lane.getLength(), // cost is purely physical distance
                (curr, target) -> curr.getLocation().distance(target.getLocation()) // Euclidean heuristic
        );
        return result.getLanes();
    }

    @Override
    public void onSimulationTick(double simTime, double dt, TrafficNetwork network, List<Car> activeCars) {
        // Naive engine never proactively reroutes on congestion
    }

    @Override
    public void onNetworkChanged(TrafficNetwork network, List<Car> activeCars) {
        // Only reroute cars whose currently planned path is blocked
        for (Car car : activeCars) {
            List<Lane> route = car.getRoute();
            boolean pathBlocked = false;
            for (Lane l : route) {
                if (l.isBlocked()) {
                    pathBlocked = true;
                    break;
                }
            }
            if (pathBlocked && car.getCurrentLane() != null) {
                Junction nextJunction = car.getCurrentLane().getToJunction();
                List<Lane> newPath = computeRoute(nextJunction, car.getDestination(), network, 0.0, car);
                if (!newPath.isEmpty()) {
                    List<Lane> fullRoute = new ArrayList<>();
                    fullRoute.add(car.getCurrentLane());
                    fullRoute.addAll(newPath);
                    double estCost = 0.0;
                    for (Lane l : fullRoute) estCost += l.getBprTravelTime();
                    car.setRoute(fullRoute, estCost, "Rerouted: road closure ahead");
                }
            }
        }
    }
}
