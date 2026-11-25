package com.trafficsense.model;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a single directional lane along a Road.
 */
public class Lane {
    private final String id;
    private final Road parentRoad;
    private final Junction fromJunction;
    private final Junction toJunction;
    private final boolean isForward; // relative to road's canonical A -> B direction
    private final List<Car> cars = new CopyOnWriteArrayList<>();

    public Lane(String id, Road parentRoad, Junction fromJunction, Junction toJunction, boolean isForward) {
        this.id = id;
        this.parentRoad = parentRoad;
        this.fromJunction = fromJunction;
        this.toJunction = toJunction;
        this.isForward = isForward;
    }

    public String getId() { return id; }
    public Road getParentRoad() { return parentRoad; }
    public Junction getFromJunction() { return fromJunction; }
    public Junction getToJunction() { return toJunction; }
    public boolean isForward() { return isForward; }

    public double getLength() { return parentRoad.getLength(); }
    public double getSpeedLimitKmH() { return parentRoad.getSpeedLimit(); }
    public double getSpeedLimitMS() { return parentRoad.getSpeedLimit() / 3.6; }
    public int getCapacity() { return parentRoad.getCapacity(); }
    public boolean isBlocked() { return parentRoad.isBlocked(); }

    public int getLoad() {
        return cars.size();
    }

    public List<Car> getCars() {
        return Collections.unmodifiableList(cars);
    }

    public void addCar(Car car) {
        if (!cars.contains(car)) {
            cars.add(car);
        }
    }

    public void removeCar(Car car) {
        cars.remove(car);
    }

    /**
     * BPR formula:
     * travelTime = freeFlowTime * (1 + 0.15 * (load / capacity)^4)
     */
    public double getFreeFlowTravelTime() {
        return getLength() / Math.max(0.1, getSpeedLimitMS());
    }

    public double getBprTravelTime() {
        return getBprTravelTime(getLoad());
    }

    public double getBprTravelTime(int load) {
        if (isBlocked()) {
            return Double.POSITIVE_INFINITY;
        }
        double freeFlow = getFreeFlowTravelTime();
        double ratio = (double) load / Math.max(1, getCapacity());
        return freeFlow * (1.0 + 0.15 * Math.pow(ratio, 4));
    }

    public double getCongestionRatio() {
        return (double) getLoad() / Math.max(1, getCapacity());
    }

    /**
     * Returns the 2D world position along this lane for a given distance offset (meters)
     * with lateral offset to prevent overlap between opposite directions.
     */
    public Point2D.Double getPositionAtOffset(double offsetMeters) {
        List<Point2D.Double> polyline = parentRoad.getPolyline();
        if (polyline.isEmpty()) {
            return fromJunction.getLocation();
        }
        
        List<Point2D.Double> directedPoints = new ArrayList<>(polyline);
        if (!isForward) {
            Collections.reverse(directedPoints);
        }

        double totalLen = getLength();
        double clampedOffset = Math.max(0.0, Math.min(totalLen, offsetMeters));
        double fraction = totalLen > 0 ? (clampedOffset / totalLen) : 0.0;

        // Linear interpolation across segments
        if (directedPoints.size() == 2) {
            Point2D.Double p0 = directedPoints.get(0);
            Point2D.Double p1 = directedPoints.get(1);
            double dx = p1.x - p0.x;
            double dy = p1.y - p0.y;
            double dist = Math.hypot(dx, dy);
            double nx = 0, ny = 0;
            if (dist > 0.0001) {
                // Perpendicular vector for lane lateral offset (right-hand traffic: +5 pixels right)
                nx = -dy / dist * 5.0;
                ny = dx / dist * 5.0;
            }
            double x = p0.x + fraction * dx + nx;
            double y = p0.y + fraction * dy + ny;
            return new Point2D.Double(x, y);
        }

        // Multi-segment fallback
        Point2D.Double p0 = directedPoints.get(0);
        Point2D.Double p1 = directedPoints.get(directedPoints.size() - 1);
        double x = p0.x + fraction * (p1.x - p0.x);
        double y = p0.y + fraction * (p1.y - p0.y);
        return new Point2D.Double(x, y);
    }

    /**
     * Heading in radians for car orientation.
     */
    public double getHeading() {
        Point2D.Double pFrom = fromJunction.getLocation();
        Point2D.Double pTo = toJunction.getLocation();
        return Math.atan2(pTo.y - pFrom.y, pTo.x - pFrom.x);
    }

    @Override
    public String toString() {
        return id + " [" + fromJunction.getId() + " -> " + toJunction.getId() + "]";
    }
}
