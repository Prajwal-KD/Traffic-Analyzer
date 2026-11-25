package com.trafficsense.model;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Hardcoded topology of EXACTLY 15 roads:
 * - 9 junctions in 3x3 grid (J11..J33)
 * - 12 internal roads between adjacent grid junctions
 * - 1 MAIN IN road: ENTRY -> J11
 * - 1 MAIN OUT road: J33 -> EXIT
 * - 1 diagonal shortcut road: J13 <-> J31 (narrow, lower capacity, short)
 * Total = 15 roads (each bidirectional with one lane per direction = 30 lanes).
 */
public class TrafficNetwork {
    private final Map<String, Junction> junctions = new LinkedHashMap<>();
    private final Map<String, Road> roads = new LinkedHashMap<>();
    private final Map<String, Lane> lanes = new LinkedHashMap<>();
    private final Map<Junction, List<Lane>> outgoingLanes = new HashMap<>();

    public TrafficNetwork() {
        initTopology();
    }

    private void initTopology() {
        // 1. Create Junctions
        // Gate nodes
        Junction entry = new Junction("ENTRY", "ENTRY GATE", 70.0, 160.0, true);
        Junction exit = new Junction("EXIT", "EXIT GATE", 870.0, 560.0, true);
        junctions.put(entry.getId(), entry);
        junctions.put(exit.getId(), exit);

        // 3x3 Grid junctions
        double startX = 260.0;
        double startY = 160.0;
        double stepX = 210.0;
        double stepY = 200.0;

        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 3; col++) {
                String id = "J" + row + col;
                double x = startX + (col - 1) * stepX;
                double y = startY + (row - 1) * stepY;
                Junction j = new Junction(id, id, x, y, false);
                junctions.put(id, j);
            }
        }

        // 2. Create the EXACTLY 15 Roads
        // (1) MAIN IN Road: ENTRY <-> J11 (high capacity feeder)
        addRoad("R_ENTRY_J11", "Main Entry Boulevard", "ENTRY", "J11", 260.0, 70.0, 14);

        // (2) MAIN OUT Road: J33 <-> EXIT (high capacity drain)
        addRoad("R_J33_EXIT", "Main Exit Expressway", "J33", "EXIT", 260.0, 70.0, 14);

        // (3) DIAGONAL SHORTCUT: J13 <-> J31 (narrow, lower capacity, short: 160m, speed 30km/h, cap 2)
        addRoad("R_J13_J31", "Diagonal Shortcut Alley", "J13", "J31", 160.0, 30.0, 2);

        // (4)-(9) Horizontal grid roads (6 roads)
        // North Ave (Eastbound alternative corridor)
        addRoad("R_J11_J12", "North Ave West", "J11", "J12", 200.0, 50.0, 4);
        addRoad("R_J12_J13", "North Ave East", "J12", "J13", 200.0, 50.0, 4);

        // Central cross roads
        addRoad("R_J21_J22", "Central Cross West", "J21", "J22", 210.0, 45.0, 4);
        addRoad("R_J22_J23", "Central Cross East", "J22", "J23", 210.0, 45.0, 4);

        // South Parkway (South corridor)
        addRoad("R_J31_J32", "South Parkway West", "J31", "J32", 200.0, 50.0, 4);
        addRoad("R_J32_J33", "South Parkway East", "J32", "J33", 200.0, 50.0, 4);

        // (10)-(15) Vertical grid roads (6 roads)
        // West corridor (Shortest physical distance: 190m each -> 780m total via South vs 800m via North)
        addRoad("R_J11_J21", "West Corridor North", "J11", "J21", 190.0, 50.0, 4);
        addRoad("R_J21_J31", "West Corridor South", "J21", "J31", 190.0, 50.0, 4);

        // Midtown Spine (center column medium)
        addRoad("R_J12_J22", "Midtown Spine North", "J12", "J22", 210.0, 45.0, 3);
        addRoad("R_J22_J32", "Midtown Spine South", "J22", "J32", 210.0, 45.0, 3);

        // East Link (East corridor)
        addRoad("R_J13_J23", "East Link North", "J13", "J23", 200.0, 50.0, 4);
        addRoad("R_J23_J33", "East Link South", "J23", "J33", 200.0, 50.0, 4);

        // Build outgoing lane index
        for (Junction j : junctions.values()) {
            outgoingLanes.put(j, new ArrayList<>());
        }
        for (Lane lane : lanes.values()) {
            outgoingLanes.get(lane.getFromJunction()).add(lane);
        }
    }

    private void addRoad(String id, String name, String junctionAId, String junctionBId,
                         double length, double speedLimit, int capacity) {
        Junction a = junctions.get(junctionAId);
        Junction b = junctions.get(junctionBId);
        if (a == null || b == null) {
            throw new IllegalArgumentException("Unknown junction: " + junctionAId + " or " + junctionBId);
        }
        List<Point2D.Double> poly = List.of(a.getLocation(), b.getLocation());
        Road road = new Road(id, name, a, b, length, speedLimit, capacity, poly);
        roads.put(id, road);
        lanes.put(road.getForwardLane().getId(), road.getForwardLane());
        lanes.put(road.getBackwardLane().getId(), road.getBackwardLane());
    }

    public Map<String, Junction> getJunctions() { return junctions; }
    public Map<String, Road> getRoads() { return roads; }
    public Map<String, Lane> getLanes() { return lanes; }
    public Junction getJunction(String id) { return junctions.get(id); }
    public Road getRoad(String id) { return roads.get(id); }
    public Lane getLane(String id) { return lanes.get(id); }

    public List<Lane> getOutgoingLanes(Junction junction) {
        return outgoingLanes.getOrDefault(junction, Collections.emptyList());
    }

    public Road findRoadAtPoint(Point2D point, double tolerance) {
        Road closest = null;
        double minDistance = Double.MAX_VALUE;
        for (Road road : roads.values()) {
            Point2D.Double pA = road.getJunctionA().getLocation();
            Point2D.Double pB = road.getJunctionB().getLocation();
            double dist = Line2D.ptSegDist(pA.x, pA.y, pB.x, pB.y, point.getX(), point.getY());
            if (dist <= tolerance && dist < minDistance) {
                minDistance = dist;
                closest = road;
            }
        }
        return closest;
    }

    public SnapResult snapPointToNearestLane(Point2D targetPoint) {
        Lane bestLane = null;
        double bestOffset = 0.0;
        double minDistance = Double.MAX_VALUE;

        for (Lane lane : lanes.values()) {
            Point2D.Double p0 = lane.getFromJunction().getLocation();
            Point2D.Double p1 = lane.getToJunction().getLocation();
            double segDx = p1.x - p0.x;
            double segDy = p1.y - p0.y;
            double segLenSq = segDx * segDx + segDy * segDy;
            if (segLenSq == 0) continue;

            double t = ((targetPoint.getX() - p0.x) * segDx + (targetPoint.getY() - p0.y) * segDy) / segLenSq;
            t = Math.max(0.0, Math.min(1.0, t));
            double projX = p0.x + t * segDx;
            double projY = p0.y + t * segDy;
            double dist = Math.hypot(targetPoint.getX() - projX, targetPoint.getY() - projY);

            if (dist < minDistance) {
                minDistance = dist;
                bestLane = lane;
                bestOffset = t * lane.getLength();
            }
        }
        return new SnapResult(bestLane, bestOffset, minDistance);
    }

    public record SnapResult(Lane lane, double offset, double distance) {}
}
