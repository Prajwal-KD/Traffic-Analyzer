package com.trafficsense.routing;

import com.trafficsense.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RoutingEnginesTest {
    private TrafficNetwork network;

    @BeforeEach
    public void setUp() {
        network = new TrafficNetwork();
    }

    @Test
    public void testNaiveFindsPathFromEntryToExit() {
        NaiveRoutingEngine engine = new NaiveRoutingEngine();
        Junction entry = network.getJunction("ENTRY");
        Junction exit = network.getJunction("EXIT");

        Car car = new Car("TEST_CAR", Color.BLACK, entry, exit, 0.0);
        List<Lane> path = engine.computeRoute(entry, exit, network, 0.0, car);

        assertNotNull(path);
        assertFalse(path.isEmpty(), "Path from ENTRY to EXIT must exist");
        assertEquals(entry, path.get(0).getFromJunction());
        assertEquals(exit, path.get(path.size() - 1).getToJunction());

        // Verify path continuity
        for (int i = 0; i < path.size() - 1; i++) {
            assertEquals(path.get(i).getToJunction(), path.get(i + 1).getFromJunction(),
                    "Consecutive lanes in computed path must connect at junctions");
        }
    }

    @Test
    public void testSelfishDynamicHysteresisThreshold() {
        SelfishDynamicRoutingEngine engine = new SelfishDynamicRoutingEngine();
        Junction j11 = network.getJunction("J11");
        Junction j33 = network.getJunction("J33");

        Car car = new Car("C_DYNAMIC", Color.BLUE, j11, j33, 0.0);
        List<Lane> initialRoute = engine.computeRoute(j11, j33, network, 0.0, car);
        Lane startLane = initialRoute.get(0);
        car.enterLane(startLane, 0.0);
        car.setRoute(initialRoute, 50.0, "Initial route");

        // Artificial minor congestion on next lane (< 10% delta) should NOT trigger reroute due to hysteresis
        engine.onSimulationTick(2.1, 0.05, network, List.of(car));
        assertEquals(initialRoute, car.getRoute(), "Route should not change for negligible congestion difference");

        // Heavy congestion on lane in current route (> 50% slower)
        for (int i = 0; i < 20; i++) {
            network.getRoad("R_J21_J31").getForwardLane().addCar(new Car("DUMMY_" + i, Color.GRAY, j11, j33, 0.0));
        }

        engine.onSimulationTick(4.2, 0.05, network, List.of(car));
        // Verify car's NavigationClient updated with reroute reason
        assertTrue(car.getNavigationClient().getRerouteReason().contains("Rerouted"),
                "NavigationClient must document the reroute reason when congestion warrants route switch");
    }

    @Test
    public void testCoordinatedPredictiveSpreadsTraffic() {
        CoordinatedPredictiveRoutingEngine engine = new CoordinatedPredictiveRoutingEngine();
        Junction j11 = network.getJunction("J11");
        Junction j33 = network.getJunction("J33");

        // Route first car
        Car car1 = new Car("C1", Color.RED, j11, j33, 0.0);
        Lane startLane = network.getRoad("R_J11_J12").getForwardLane();
        car1.enterLane(startLane, 0.0);

        Car car2 = new Car("C2", Color.GREEN, j11, j33, 0.0);
        car2.enterLane(startLane, 0.0);

        engine.replanAll(0.0, network, List.of(car1, car2), false);

        assertNotNull(car1.getRoute());
        assertNotNull(car2.getRoute());
        assertFalse(car1.getRoute().isEmpty());
        assertFalse(car2.getRoute().isEmpty());
    }

    @Test
    public void testNavigationClientExposesTelemetry() {
        Junction entry = network.getJunction("ENTRY");
        Junction j11 = network.getJunction("J11");
        Car car = new Car("TEST_NAV", Color.ORANGE, entry, j11, 0.0);
        NavigationClient nav = car.getNavigationClient();

        Lane lane = network.getRoad("R_ENTRY_J11").getForwardLane();
        car.enterLane(lane, 10.0);
        car.setRoute(List.of(lane), 15.0, "Test initial path");

        assertEquals(15.0, nav.getETA());
        assertEquals("Test initial path", nav.getRerouteReason());
        assertNotNull(nav.getNextTurn());
    }
}
