package com.trafficsense.routing;

import com.trafficsense.model.Car;
import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;

import java.util.ArrayList;
import java.util.List;

/**
 * 2. SELFISH_DYNAMIC Engine:
 * Dijkstra on live BPR travel times, rerouting every 2 seconds of sim time.
 * Hysteresis: only switches route if the alternative is at least 10% faster,
 * preventing herding instability.
 */
public class SelfishDynamicRoutingEngine implements RoutingEngine {
    private static final double REROUTE_INTERVAL = 2.0; // every 2 sim seconds
    private static final double HYSTERESIS_THRESHOLD = 0.90; // must be at least 10% faster

    private double lastRerouteSimTime = -10.0;

    @Override
    public RoutingEngineType getType() {
        return RoutingEngineType.SELFISH_DYNAMIC;
    }

    @Override
    public List<Lane> computeRoute(Junction origin, Junction destination, TrafficNetwork network, double currentSimTime, Car car) {
        DijkstraRouter.PathResult result = DijkstraRouter.findPath(
                origin,
                destination,
                network,
                currentSimTime,
                (lane, time) -> lane.getBprTravelTime(),
                null
        );
        return result.getLanes();
    }

    @Override
    public void onSimulationTick(double simTime, double dt, TrafficNetwork network, List<Car> activeCars) {
        if (simTime - lastRerouteSimTime >= REROUTE_INTERVAL) {
            lastRerouteSimTime = simTime;
            evaluateAllCars(network, activeCars, simTime, false);
        }
    }

    @Override
    public void onNetworkChanged(TrafficNetwork network, List<Car> activeCars) {
        evaluateAllCars(network, activeCars, 0.0, true);
    }

    private void evaluateAllCars(TrafficNetwork network, List<Car> activeCars, double simTime, boolean forceRerouteBlocked) {
        for (Car car : activeCars) {
            if (car.isBeingDragged() || car.getStatus() == Car.Status.ARRIVED) continue;
            Lane currentLane = car.getCurrentLane();
            if (currentLane == null) continue;

            Junction nextJunction = currentLane.getToJunction();
            if (nextJunction.equals(car.getDestination())) continue; // already at destination edge

            List<Lane> currentRoute = car.getRoute();
            int currIndex = currentRoute.indexOf(currentLane);

            // Compute remaining time on current planned route
            double remainingCurrentTime = 0.0;
            double currentLaneRemainingFrac = Math.max(0.0, (currentLane.getLength() - car.getPosition()) / currentLane.getLength());
            remainingCurrentTime += currentLaneRemainingFrac * currentLane.getBprTravelTime();

            boolean currentRouteHasBlockage = currentLane.isBlocked();
            Lane mostCongestedLaneOnOldRoute = currentLane;
            double maxCongestion = currentLane.getCongestionRatio();

            if (currIndex >= 0) {
                for (int i = currIndex + 1; i < currentRoute.size(); i++) {
                    Lane l = currentRoute.get(i);
                    if (l.isBlocked()) currentRouteHasBlockage = true;
                    remainingCurrentTime += l.getBprTravelTime();
                    if (l.getCongestionRatio() > maxCongestion) {
                        maxCongestion = l.getCongestionRatio();
                        mostCongestedLaneOnOldRoute = l;
                    }
                }
            } else {
                currentRouteHasBlockage = true;
            }

            // Compute alternative route from nextJunction to destination
            DijkstraRouter.PathResult candidate = DijkstraRouter.findPath(
                    nextJunction,
                    car.getDestination(),
                    network,
                    simTime,
                    (lane, time) -> lane.getBprTravelTime(),
                    null
            );

            if (candidate.getLanes().isEmpty() && !nextJunction.equals(car.getDestination())) {
                continue; // no alternate path available
            }

            double candidateTotalTime = (currentLaneRemainingFrac * currentLane.getBprTravelTime()) + candidate.getTotalCost();

            // Check if switch should occur:
            // 1. If current route is blocked, switch immediately.
            // 2. Hysteresis: candidate route must be at least 10% faster (<= 0.90 * remainingCurrentTime).
            boolean shouldSwitch = currentRouteHasBlockage ||
                    (candidateTotalTime <= remainingCurrentTime * HYSTERESIS_THRESHOLD);

            if (shouldSwitch && !isRouteEquivalent(currentRoute, currIndex, candidate.getLanes())) {
                List<Lane> newRoute = new ArrayList<>();
                if (currIndex >= 0) {
                    for (int i = 0; i <= currIndex; i++) {
                        newRoute.add(currentRoute.get(i));
                    }
                } else {
                    newRoute.add(currentLane);
                }
                newRoute.addAll(candidate.getLanes());

                double savedSeconds = Math.max(0.0, remainingCurrentTime - candidateTotalTime);
                String reason;
                if (currentRouteHasBlockage) {
                    reason = "Rerouted: road closure avoided";
                } else {
                    String congestedDesc = mostCongestedLaneOnOldRoute.getId();
                    reason = "Rerouted: " + congestedDesc + " congested, saves " + Math.round(savedSeconds) + " s";
                }

                car.setRoute(newRoute, candidateTotalTime, reason);
            } else {
                // Update live ETA
                car.getNavigationClient().setETA(remainingCurrentTime);
            }
        }
    }

    private boolean isRouteEquivalent(List<Lane> currentRoute, int currIndex, List<Lane> candidateFromNext) {
        if (currIndex < 0) return false;
        int remainingLength = currentRoute.size() - (currIndex + 1);
        if (remainingLength != candidateFromNext.size()) return false;
        for (int i = 0; i < candidateFromNext.size(); i++) {
            if (!currentRoute.get(currIndex + 1 + i).equals(candidateFromNext.get(i))) {
                return false;
            }
        }
        return true;
    }
}
