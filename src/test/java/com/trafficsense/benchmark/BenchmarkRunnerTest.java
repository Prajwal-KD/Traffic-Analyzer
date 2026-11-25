package com.trafficsense.benchmark;

import com.trafficsense.routing.CoordinatedPredictiveRoutingEngine;
import com.trafficsense.routing.NaiveRoutingEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BenchmarkRunnerTest {

    @Test
    public void testHeadlessBenchmarkExecution() {
        // Run a short benchmark evaluation
        BenchmarkResult result = BenchmarkRunner.runSingleEngine(
                new CoordinatedPredictiveRoutingEngine(),
                0,
                1,
                null
        );

        assertNotNull(result);
        assertEquals(300.0, result.simDurationSeconds(), 0.001);
        assertTrue(result.completedTrips() > 0, "Benchmark must complete trips over 300 simulation seconds");
        assertTrue(result.throughputCarsPerMin() > 0.0);
        assertTrue(result.avgTripTimeSeconds() > 0.0);
        assertTrue(result.avgSpeedKmH() > 0.0);
    }
}
