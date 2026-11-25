package com.trafficsense.routing;

public enum RoutingEngineType {
    NAIVE("Naive (Static Shortest Distance)"),
    SELFISH_DYNAMIC("Selfish Dynamic (Live BPR + 10% Hysteresis)"),
    COORDINATED_PREDICTIVE("Coordinated Predictive (Smart 60s Load Balancing)");

    private final String displayName;

    RoutingEngineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
