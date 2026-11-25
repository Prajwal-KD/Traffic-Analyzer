package com.trafficsense.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TopologyTest {
    private TrafficNetwork network;

    @BeforeEach
    public void setUp() {
        network = new TrafficNetwork();
    }

    @Test
    public void testExactlyFifteenRoads() {
        assertEquals(15, network.getRoads().size(), "Locality must contain EXACTLY 15 roads!");
    }

    @Test
    public void testJunctionsCount() {
        // 9 grid junctions (J11..J33) + ENTRY + EXIT = 11 junctions
        assertEquals(11, network.getJunctions().size(), "Network must contain 11 junctions (9 grid + 2 gates)!");
        assertNotNull(network.getJunction("ENTRY"));
        assertNotNull(network.getJunction("EXIT"));
        for (int r = 1; r <= 3; r++) {
            for (int c = 1; c <= 3; c++) {
                assertNotNull(network.getJunction("J" + r + c));
            }
        }
    }

    @Test
    public void testBidirectionalLanesCount() {
        // 15 bidirectional roads = 30 directed lanes
        assertEquals(30, network.getLanes().size(), "15 bidirectional roads must yield 30 directed lanes!");
    }

    @Test
    public void testSpecialRoadsAttributes() {
        Road mainIn = network.getRoad("R_ENTRY_J11");
        assertNotNull(mainIn, "Main entry road R_ENTRY_J11 must exist");
        assertTrue(mainIn.getCapacity() >= 12, "Main entry road must have high capacity");

        Road mainOut = network.getRoad("R_J33_EXIT");
        assertNotNull(mainOut, "Main exit road R_J33_EXIT must exist");
        assertTrue(mainOut.getCapacity() >= 12, "Main exit road must have high capacity");

        Road diagonal = network.getRoad("R_J13_J31");
        assertNotNull(diagonal, "Diagonal shortcut road R_J13_J31 must exist");
        assertTrue(diagonal.getCapacity() <= 5, "Diagonal shortcut must have lower capacity");
        assertTrue(diagonal.getSpeedLimit() <= 45, "Diagonal shortcut must have lower speed limit");
    }
}
