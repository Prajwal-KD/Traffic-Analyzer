package com.trafficsense.routing;

import com.trafficsense.model.Car;
import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;

import java.awt.geom.Point2D;
import java.util.*;

/**
 * 3. COORDINATED_PREDICTIVE Engine:
 * Central predictive coordinator that:
 * - Projects future lane loads over a 60-second rolling horizon,
 * - Assigns routes one car at a time (sorted by distance-to-destination),
 * - Updates predicted lane load profiles after each assignment for global load balancing,
 * - Employs time-dependent edge costs BPR(predictedLoad(t)),
 * - Reroutes periodically and immediately on network events (closures, drops, spawns).
 */
public class CoordinatedPredictiveRoutingEngine implements RoutingEngine {
    public static final int HORIZON_SECONDS = 60;
    private static final double REPLAN_INTERVAL = 3.0; // replan every 3 sim seconds

    private double lastReplanSimTime = -10.0;
    private final Map<String, int[]> predictedLoadProfiles = new HashMap<>();

    @Override
    public RoutingEngineType getType() {
        return RoutingEngineType.COORDINATED_PREDICTIVE;
    }

    @Override
    public synchronized List<Lane> computeRoute(Junction origin, Junction destination, TrafficNetwork network, double currentSimTime, Car car) {
        if (predictedLoadProfiles.isEmpty()) {
            initLoadProfiles(network);
        }

        DijkstraRouter.PathResult result = DijkstraRouter.findPath(
                origin,
                destination,
                network,
                currentSimTime,
                (lane, arrivalTime) -> {
                    int load = getPredictedLoad(lane.getId(), arrivalTime - currentSimTime);
                    return lane.getBprTravelTime(load) + (load * 2.0);
                },
                null
        );

        List<Lane> path = result.getLanes();
        if (path != null && !path.isEmpty()) {
            // Reserve future capacity along this computed route
            double projectedTime = currentSimTime;
            for (Lane lane : path) {
                int load = getPredictedLoad(lane.getId(), projectedTime - currentSimTime);
                double duration = lane.getBprTravelTime(load);
                applyLoadToProfile(lane.getId(), projectedTime - currentSimTime, projectedTime + duration - currentSimTime);
                projectedTime += duration;
            }
        }
        return path;
    }

    @Override
    public synchronized void onSimulationTick(double simTime, double dt, TrafficNetwork network, List<Car> activeCars) {
        if (simTime - lastReplanSimTime >= REPLAN_INTERVAL) {
            lastReplanSimTime = simTime;
            replanAll(simTime, network, activeCars, false);
        }
    }

    @Override
    public synchronized void onNetworkChanged(TrafficNetwork network, List<Car> activeCars) {
        lastReplanSimTime = -10.0; // trigger immediate replan
        replanAll(0.0, network, activeCars, true);
    }

    /**
     * Centralized coordinated re-planning over all active cars.
     */
    public synchronized void replanAll(double simTime, TrafficNetwork network, List<Car> activeCars, boolean networkChanged) {
        // 1. Reset all 60s predicted load tables
        initLoadProfiles(network);

        // 2. Sort cars by distance to destination descending (longer trips planned first)
        List<Car> sortedCars = new ArrayList<>();
        for (Car car : activeCars) {
            if (!car.isBeingDragged() && car.getStatus() != Car.Status.ARRIVED && car.getCurrentLane() != null) {
                sortedCars.add(car);
            }
        }
        sortedCars.sort((c1, c2) -> {
            double d1 = estimateRemainingDistance(c1);
            double d2 = estimateRemainingDistance(c2);
            return Double.compare(d2, d1); // descending
        });

        // 3. Incrementally route each car and update future load profiles
        for (Car car : sortedCars) {
            Lane currentLane = car.getCurrentLane();
            if (currentLane == null) continue;

            // Project time when car departs current lane
            double remainingDistOnCurrent = Math.max(0.0, currentLane.getLength() - car.getPosition());
            double speed = Math.max(2.5, car.getSpeed() > 0.5 ? car.getSpeed() : currentLane.getSpeedLimitMS() * 0.7);
            double timeOnCurrent = remainingDistOnCurrent / speed;
            double arrivalAtNextJunction = simTime + timeOnCurrent;

            // Apply load of current car on current lane for [0, timeOnCurrent]
            applyLoadToProfile(currentLane.getId(), 0.0, timeOnCurrent);

            Junction nextJunction = currentLane.getToJunction();
            if (nextJunction.equals(car.getDestination())) {
                car.getNavigationClient().setETA(timeOnCurrent);
                continue;
            }

            // Search for optimal time-dependent route from nextJunction to destination
            DijkstraRouter.PathResult result = DijkstraRouter.findPath(
                    nextJunction,
                    car.getDestination(),
                    network,
                    arrivalAtNextJunction,
                    (lane, arrivalTime) -> {
                        int load = getPredictedLoad(lane.getId(), arrivalTime - simTime);
                        return lane.getBprTravelTime(load) + (load * 2.0);
                    },
                    null
            );

            List<Lane> pathFromNext = result.getLanes();
            if (pathFromNext.isEmpty() && !nextJunction.equals(car.getDestination())) {
                continue;
            }

            // Assemble new complete route preserving history up to currentLane
            List<Lane> oldRoute = car.getRoute();
            int currIdx = oldRoute.indexOf(currentLane);
            List<Lane> fullRoute = new ArrayList<>();
            if (currIdx >= 0) {
                for (int i = 0; i <= currIdx; i++) {
                    fullRoute.add(oldRoute.get(i));
                }
            } else {
                fullRoute.add(currentLane);
            }
            fullRoute.addAll(pathFromNext);

            // Project car presence on future lanes and reserve future lane capacities
            double projectedTime = arrivalAtNextJunction;
            for (Lane futureLane : pathFromNext) {
                int futureLoad = getPredictedLoad(futureLane.getId(), projectedTime - simTime);
                double laneDuration = futureLane.getBprTravelTime(futureLoad);
                applyLoadToProfile(futureLane.getId(), projectedTime - simTime, projectedTime + laneDuration - simTime);
                projectedTime += laneDuration;
            }

            double totalEstTime = projectedTime - simTime;
            String reason;
            if (networkChanged) {
                reason = "Predictive replan: closure re-routed via " + (pathFromNext.isEmpty() ? "direct" : pathFromNext.get(0).getId());
            } else {
                reason = "Predictive coordination: load-balanced (ETA " + Math.round(totalEstTime) + " s)";
            }

            car.setRoute(fullRoute, totalEstTime, reason);
        }
    }

    private void initLoadProfiles(TrafficNetwork network) {
        predictedLoadProfiles.clear();
        for (Lane lane : network.getLanes().values()) {
            predictedLoadProfiles.put(lane.getId(), new int[HORIZON_SECONDS]);
        }
    }

    private int getPredictedLoad(String laneId, double relativeSeconds) {
        int[] profile = predictedLoadProfiles.get(laneId);
        if (profile == null) return 0;
        int bin = (int) Math.floor(relativeSeconds);
        if (bin < 0) bin = 0;
        if (bin >= HORIZON_SECONDS) return 0;
        return profile[bin];
    }

    private void applyLoadToProfile(String laneId, double startRelSec, double endRelSec) {
        int[] profile = predictedLoadProfiles.get(laneId);
        if (profile == null) return;
        int startBin = (int) Math.max(0, Math.floor(startRelSec));
        int endBin = (int) Math.min(HORIZON_SECONDS - 1, Math.floor(endRelSec));
        for (int b = startBin; b <= endBin; b++) {
            profile[b]++;
        }
    }

    private double estimateRemainingDistance(Car car) {
        double dist = 0.0;
        if (car.getCurrentLane() != null) {
            dist += Math.max(0.0, car.getCurrentLane().getLength() - car.getPosition());
            List<Lane> route = car.getRoute();
            int idx = route.indexOf(car.getCurrentLane());
            if (idx >= 0) {
                for (int i = idx + 1; i < route.size(); i++) {
                    dist += route.get(i).getLength();
                }
                return dist;
            }
        }
        Point2D.Double carPos = car.getVisualPosition();
        Point2D.Double destPos = car.getDestination().getLocation();
        return carPos.distance(destPos);
    }
}
