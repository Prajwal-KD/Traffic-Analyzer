package com.trafficsense.routing;

import com.trafficsense.benchmark.BenchmarkResult;
import com.trafficsense.benchmark.BenchmarkRunner;
import com.trafficsense.model.TrafficNetwork;
import com.trafficsense.sim.TrafficSimulation;
import org.junit.jupiter.api.Test;

public class BenchmarkComparisonTuningTest {
    @Test
    public void runComparison() {
        System.out.println("Running test benchmark...");
        BenchmarkResult naive = BenchmarkRunner.runSingleEngine(new NaiveRoutingEngine(), 0, 3, null);
        BenchmarkResult selfish = BenchmarkRunner.runSingleEngine(new SelfishDynamicRoutingEngine(), 1, 3, null);
        BenchmarkResult predictive = BenchmarkRunner.runSingleEngine(new CoordinatedPredictiveRoutingEngine(), 2, 3, null);

        System.out.println("==========================================================================================");
        System.out.printf("%-40s | Trips: %3d | Throughput: %-13s | Avg Trip Time: %-8s | Avg Speed: %s%n",
                naive.engineName(), naive.completedTrips(), naive.getFormattedThroughput(),
                naive.getFormattedAvgTripTime(), naive.getFormattedAvgSpeed());
        System.out.printf("%-40s | Trips: %3d | Throughput: %-13s | Avg Trip Time: %-8s | Avg Speed: %s%n",
                selfish.engineName(), selfish.completedTrips(), selfish.getFormattedThroughput(),
                selfish.getFormattedAvgTripTime(), selfish.getFormattedAvgSpeed());
        System.out.printf("%-40s | Trips: %3d | Throughput: %-13s | Avg Trip Time: %-8s | Avg Speed: %s%n",
                predictive.engineName(), predictive.completedTrips(), predictive.getFormattedThroughput(),
                predictive.getFormattedAvgTripTime(), predictive.getFormattedAvgSpeed());
        System.out.println("==========================================================================================");
    }
}
