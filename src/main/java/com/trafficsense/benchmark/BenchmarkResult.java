package com.trafficsense.benchmark;

/**
 * Encapsulates the benchmark performance metrics for a single routing engine.
 */
public record BenchmarkResult(
        String engineName,
        int completedTrips,
        double throughputCarsPerMin,
        double avgTripTimeSeconds,
        double avgSpeedKmH,
        int stuckCars,
        double maxCongestionRatio,
        double simDurationSeconds
) {
    public String getFormattedThroughput() {
        return String.format("%.1f cars/min", throughputCarsPerMin);
    }

    public String getFormattedAvgTripTime() {
        return String.format("%.1f s", avgTripTimeSeconds);
    }

    public String getFormattedAvgSpeed() {
        return String.format("%.1f km/h", avgSpeedKmH);
    }

    public String getFormattedMaxCongestion() {
        return String.format("%.0f%%", maxCongestionRatio * 100.0);
    }
}
