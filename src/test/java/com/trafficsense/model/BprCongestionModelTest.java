package com.trafficsense.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BprCongestionModelTest {
    private TrafficNetwork network;
    private Road testRoad;
    private Lane forwardLane;

    @BeforeEach
    public void setUp() {
        network = new TrafficNetwork();
        testRoad = network.getRoad("R_J11_J12");
        forwardLane = testRoad.getForwardLane();
    }

    @Test
    public void testFreeFlowTravelTime() {
        double length = testRoad.getLength();
        double speedMs = testRoad.getSpeedLimit() / 3.6;
        double expectedFreeFlow = length / speedMs;

        assertEquals(expectedFreeFlow, forwardLane.getFreeFlowTravelTime(), 0.001);
        assertEquals(expectedFreeFlow, forwardLane.getBprTravelTime(0), 0.001);
    }

    @Test
    public void testBprCongestionIncrease() {
        double freeFlow = forwardLane.getFreeFlowTravelTime();
        int capacity = forwardLane.getCapacity();

        // At load = capacity, travelTime = freeFlow * (1 + 0.15 * 1^4) = freeFlow * 1.15
        double atCapacity = forwardLane.getBprTravelTime(capacity);
        assertEquals(freeFlow * 1.15, atCapacity, 0.01);

        // At load = 2 * capacity, travelTime = freeFlow * (1 + 0.15 * 16) = freeFlow * 3.4
        double atDoubleCapacity = forwardLane.getBprTravelTime(capacity * 2);
        assertEquals(freeFlow * 3.4, atDoubleCapacity, 0.01);

        assertTrue(atDoubleCapacity > atCapacity);
        assertTrue(atCapacity > freeFlow);
    }

    @Test
    public void testBlockedRoadInfiniteCost() {
        testRoad.setBlocked(true);
        assertTrue(testRoad.isBlocked());
        assertTrue(forwardLane.isBlocked());
        assertEquals(Double.POSITIVE_INFINITY, forwardLane.getBprTravelTime());
        assertEquals(Double.POSITIVE_INFINITY, forwardLane.getBprTravelTime(0));

        testRoad.setBlocked(false);
        assertFalse(testRoad.isBlocked());
        assertFalse(Double.isInfinite(forwardLane.getBprTravelTime()));
    }
}
