package com.trafficsense.benchmark;

import com.trafficsense.model.Road;
import com.trafficsense.model.TrafficNetwork;
import com.trafficsense.routing.CoordinatedPredictiveRoutingEngine;
import com.trafficsense.routing.NaiveRoutingEngine;
import com.trafficsense.routing.RoutingEngine;
import com.trafficsense.routing.SelfishDynamicRoutingEngine;
import com.trafficsense.sim.TrafficSimulation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Headless accelerated benchmark runner.
 * Evaluates the 3 engines under an identical seeded scenario for ~5 sim-minutes (300 s).
 */
public class BenchmarkRunner {
    public static final long BENCHMARK_SEED = 42L;
    public static final double BENCHMARK_DURATION_SEC = 300.0; // 5 sim-minutes
    public static final double BENCHMARK_DT = 0.1; // 100 ms accelerated step

    public interface ProgressCallback {
        void onProgress(String currentEngine, int engineIndex, int totalEngines, double percentComplete);
    }

    /**
     * Executes the benchmark across all three routing engines.
     */
    public static List<BenchmarkResult> runAllEngines(ProgressCallback callback) {
        List<RoutingEngine> engines = List.of(
                new NaiveRoutingEngine(),
                new SelfishDynamicRoutingEngine(),
                new CoordinatedPredictiveRoutingEngine()
        );

        List<BenchmarkResult> results = new ArrayList<>();

        for (int i = 0; i < engines.size(); i++) {
            RoutingEngine engine = engines.get(i);
            String engineName = engine.getType().getDisplayName();
            if (callback != null) {
                callback.onProgress(engineName, i, engines.size(), 0.0);
            }

            BenchmarkResult res = runSingleEngine(engine, i, engines.size(), callback);
            results.add(res);
        }

        return results;
    }

    public static void main(String[] args) {
        System.out.println("Running benchmark across all engines...");
        List<BenchmarkResult> results = runAllEngines((engine, idx, total, pct) -> {
            if (pct == 0.0) System.out.println("Starting " + engine + "...");
        });
        System.out.println("================ BENCHMARK RESULTS ================");
        for (BenchmarkResult res : results) {
            System.out.printf("%-45s | Trips: %3d | Throughput: %s | Avg Trip Time: %s | Avg Speed: %s | Peak Congestion: %s%n",
                    res.engineName(), res.completedTrips(), res.getFormattedThroughput(),
                    res.getFormattedAvgTripTime(), res.getFormattedAvgSpeed(), res.getFormattedMaxCongestion());
        }
    }

    public static BenchmarkResult runSingleEngine(RoutingEngine engine, int engineIdx, int totalEngines, ProgressCallback callback) {
        TrafficNetwork network = new TrafficNetwork();
        TrafficSimulation sim = new TrafficSimulation(network, engine, BENCHMARK_SEED);
        sim.setContinuousRespawn(true);

        double maxCongestionSeen = 0.0;
        double currentSimTime = 0.0;
        int stepCount = (int) (BENCHMARK_DURATION_SEC / BENCHMARK_DT);

        for (int step = 0; step < stepCount; step++) {
            sim.step(BENCHMARK_DT);
            currentSimTime += BENCHMARK_DT;

            // Track max congestion ratio across all roads
            for (Road road : network.getRoads().values()) {
                double ratio = road.getMaxCongestionRatio();
                if (ratio > maxCongestionSeen) {
                    maxCongestionSeen = ratio;
                }
            }

            if (callback != null && step % 100 == 0) {
                double percent = (double) step / stepCount;
                callback.onProgress(engine.getType().getDisplayName(), engineIdx, totalEngines, percent);
            }
        }

        int completed = sim.getTotalCompletedTrips();
        double throughput = (completed / currentSimTime) * 60.0;
        double avgTripTime = sim.getAverageTripTime();
        double avgSpeed = sim.getAverageActiveSpeed();
        int stuckCars = sim.getStuckCarsCount();

        return new BenchmarkResult(
                engine.getType().getDisplayName(),
                completed,
                throughput,
                avgTripTime,
                avgSpeed,
                stuckCars,
                maxCongestionSeen,
                currentSimTime
        );
    }
}
