package com.trafficsense.routing;

import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;

import java.util.*;

/**
 * High-performance Dijkstra and A* pathfinder on the traffic network graph.
 */
public class DijkstraRouter {

    @FunctionalInterface
    public interface EdgeCostFunction {
        /**
         * Cost of traversing the given lane, given the arrival time at the lane's start.
         * Returns Double.POSITIVE_INFINITY if the lane cannot be traversed.
         */
        double getCost(Lane lane, double arrivalTime);
    }

    @FunctionalInterface
    public interface HeuristicFunction {
        /**
         * Admissible heuristic estimate from current junction to target junction.
         */
        double estimate(Junction current, Junction target);
    }

    public static class PathResult {
        private final List<Lane> lanes;
        private final double totalCost;

        public PathResult(List<Lane> lanes, double totalCost) {
            this.lanes = lanes;
            this.totalCost = totalCost;
        }

        public List<Lane> getLanes() { return lanes; }
        public double getTotalCost() { return totalCost; }
    }

    private static class NodeRecord implements Comparable<NodeRecord> {
        final Junction junction;
        final double costSoFar;
        final double estimatedTotalCost;

        NodeRecord(Junction junction, double costSoFar, double estimatedTotalCost) {
            this.junction = junction;
            this.costSoFar = costSoFar;
            this.estimatedTotalCost = estimatedTotalCost;
        }

        @Override
        public int compareTo(NodeRecord o) {
            return Double.compare(this.estimatedTotalCost, o.estimatedTotalCost);
        }
    }

    /**
     * Finds the optimal path from origin to destination using A* / Dijkstra.
     */
    public static PathResult findPath(Junction origin, Junction destination, TrafficNetwork network,
                                      double startTime, EdgeCostFunction costFunc, HeuristicFunction heuristic) {
        if (origin == null || destination == null) {
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }
        if (origin.equals(destination)) {
            return new PathResult(Collections.emptyList(), 0.0);
        }

        Map<Junction, Double> minCostTo = new HashMap<>();
        Map<Junction, Lane> predecessorLane = new HashMap<>();
        PriorityQueue<NodeRecord> openSet = new PriorityQueue<>();
        Set<Junction> closedSet = new HashSet<>();

        minCostTo.put(origin, 0.0);
        double h0 = heuristic != null ? heuristic.estimate(origin, destination) : 0.0;
        openSet.add(new NodeRecord(origin, 0.0, h0));

        while (!openSet.isEmpty()) {
            NodeRecord current = openSet.poll();
            Junction u = current.junction;

            if (u.equals(destination)) {
                // Reconstruct path
                List<Lane> path = new LinkedList<>();
                Junction currJunc = destination;
                while (!currJunc.equals(origin)) {
                    Lane lane = predecessorLane.get(currJunc);
                    if (lane == null) break;
                    path.add(0, lane);
                    currJunc = lane.getFromJunction();
                }
                return new PathResult(path, current.costSoFar);
            }

            if (closedSet.contains(u)) continue;
            closedSet.add(u);

            double currentCost = current.costSoFar;
            double currentTime = startTime + currentCost;

            List<Lane> outgoing = network.getOutgoingLanes(u);
            for (Lane lane : outgoing) {
                if (lane.isBlocked()) continue;

                double edgeCost = costFunc.getCost(lane, currentTime);
                if (Double.isInfinite(edgeCost) || edgeCost < 0) continue;

                Junction v = lane.getToJunction();
                if (closedSet.contains(v)) continue;

                double newCost = currentCost + edgeCost;
                if (newCost < minCostTo.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    minCostTo.put(v, newCost);
                    predecessorLane.put(v, lane);
                    double h = heuristic != null ? heuristic.estimate(v, destination) : 0.0;
                    openSet.add(new NodeRecord(v, newCost, newCost + h));
                }
            }
        }

        return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY);
    }
}
