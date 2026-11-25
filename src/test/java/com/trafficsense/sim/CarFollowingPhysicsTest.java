package com.trafficsense.sim;

import com.trafficsense.model.Car;
import com.trafficsense.model.Lane;
import com.trafficsense.model.TrafficNetwork;
import com.trafficsense.routing.NaiveRoutingEngine;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CarFollowingPhysicsTest {

    @Test
    public void testCarNeverOverlapsCarAhead() {
        TrafficNetwork network = new TrafficNetwork();
        TrafficSimulation sim = new TrafficSimulation(network, new NaiveRoutingEngine(), 42L);
        // Clear randomly spawned cars
        sim.getCars().forEach(Car::leaveCurrentLane);

        Lane lane = network.getRoad("R_ENTRY_J11").getForwardLane();

        // Create Leader car at position 40m
        Car leader = new Car("LEAD", Color.RED, network.getJunction("ENTRY"), network.getJunction("J11"), 0.0);
        leader.enterLane(lane, 40.0);
        leader.setSpeed(0.0); // stationary leader

        // Create Follower car right behind at position 20m
        Car follower = new Car("FOLLOWER", Color.BLUE, network.getJunction("ENTRY"), network.getJunction("J11"), 0.0);
        follower.enterLane(lane, 20.0);

        // Step simulation for multiple seconds
        for (int i = 0; i < 50; i++) {
            sim.step(0.05);
            // Verify bumper-to-bumper gap is maintained: follower pos + car length <= leader pos
            assertTrue(follower.getPosition() + follower.getLength() <= leader.getPosition() + 0.01,
                    "Follower car must never overlap leader car ahead on the lane!");
        }
    }
}
