package com.trafficsense.sim;

import com.trafficsense.model.*;
import com.trafficsense.routing.NaiveRoutingEngine;
import com.trafficsense.routing.RoutingEngine;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.concurrent.*;

/**
 * Core simulation engine orchestrating the fixed-timestep loop,
 * car-following physics, BPR congestion, junction throughput, and routing.
 */
public class TrafficSimulation {
    public static final int MAX_CARS = 25;
    public static final int INITIAL_CARS = 20;
    public static final double TICK_DT = 0.05; // 50 ms simulation timestep
    public static final double DEADLOCK_SAFEGUARD_SECONDS = 4.0;

    private final TrafficNetwork network;
    private final List<Car> cars = new CopyOnWriteArrayList<>();
    private RoutingEngine routingEngine;
    
    private double simTime = 0.0;
    private double speedMultiplier = 1.0;
    private volatile boolean paused = false;
    private volatile boolean continuousRespawn = true;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "TrafficSense-Sim-Thread");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> simulationTask;

    private final Random random;
    private int carCounter = 1;

    // Distinct vibrant car palette
    private static final Color[] CAR_COLORS = {
            new Color(231, 76, 60),   // Red
            new Color(52, 152, 219),  // Blue
            new Color(46, 204, 113),  // Emerald
            new Color(155, 89, 182),  // Amethyst
            new Color(241, 196, 15),  // Sunflower
            new Color(230, 126, 34),  // Carrot
            new Color(26, 188, 156),  // Turquoise
            new Color(52, 73, 94),    // Midnight Blue
            new Color(255, 107, 129), // Pink
            new Color(83, 82, 237),   // Royal
            new Color(255, 159, 26),  // Orange
            new Color(112, 161, 255)  // Sky
    };

    // Metric accumulators
    private final List<Double> completedTripDurations = new ArrayList<>();
    private final List<Double> completedTripSpeeds = new ArrayList<>();
    private int totalCompletedTrips = 0;
    private double windowStartTime = 0.0;
    private int windowTrips = 0;

    public TrafficSimulation(TrafficNetwork network, RoutingEngine routingEngine, long seed) {
        this.network = network;
        this.routingEngine = routingEngine != null ? routingEngine : new NaiveRoutingEngine();
        this.random = new Random(seed);
        resetScenario(seed);
    }

    public synchronized void start() {
        if (simulationTask == null || simulationTask.isCancelled()) {
            simulationTask = executor.scheduleAtFixedRate(this::tickLoop, 0, 50, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        if (simulationTask != null) {
            simulationTask.cancel(false);
            simulationTask = null;
        }
    }

    public void shutdown() {
        stop();
        executor.shutdownNow();
    }

    private void tickLoop() {
        try {
            if (paused) return;
            // Scale simulation steps according to speed multiplier
            double effectiveDt = TICK_DT * speedMultiplier;
            step(effectiveDt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a single simulation step with delta time dt (in seconds).
     */
    public synchronized void step(double dt) {
        simTime += dt;

        // Periodic engine updates (e.g. SelfishDynamic reroute timer, Predictive projections)
        if (routingEngine != null) {
            routingEngine.onSimulationTick(simTime, dt, network, cars);
        }

        // 1. Group cars by lane and sort them by position descending (front to back)
        Map<Lane, List<Car>> laneCars = new HashMap<>();
        for (Lane lane : network.getLanes().values()) {
            laneCars.put(lane, new ArrayList<>());
        }
        for (Car car : cars) {
            if (car.isBeingDragged() || car.getStatus() == Car.Status.ARRIVED) continue;
            Lane lane = car.getCurrentLane();
            if (lane != null && laneCars.containsKey(lane)) {
                laneCars.get(lane).add(car);
            }
        }
        for (List<Car> list : laneCars.values()) {
            list.sort((c1, c2) -> Double.compare(c2.getPosition(), c1.getPosition()));
        }

        // 2. Physics & Car-Following Update for each car
        List<Car> arrivedCars = new ArrayList<>();

        for (Lane lane : network.getLanes().values()) {
            List<Car> list = laneCars.get(lane);
            if (list == null || list.isEmpty()) continue;

            int laneLoad = list.size();
            double speedLimit = lane.getSpeedLimitMS();
            // BPR congestion-adjusted speed: freeFlowSpeed / (1 + 0.15 * (load / capacity)^4)
            double congestionFactor = 1.0 + 0.15 * Math.pow((double) laneLoad / Math.max(1, lane.getCapacity()), 4);
            double congestionSpeed = speedLimit / congestionFactor;

            boolean isLaneBlocked = lane.isBlocked();

            for (int i = 0; i < list.size(); i++) {
                Car car = list.get(i);
                if (car.isBeingDragged()) continue;

                car.setTotalTripTime(car.getTotalTripTime() + dt);

                if (isLaneBlocked) {
                    car.setSpeed(0.0);
                    car.setStatus(Car.Status.STUCK);
                    continue;
                }

                // Determine gap to car ahead
                double gapLimitedSpeed = speedLimit;
                if (i > 0) {
                    Car leader = list.get(i - 1);
                    double distanceToLeader = leader.getPosition() - car.getPosition() - car.getLength();
                    double allowedDistance = Math.max(0.0, distanceToLeader - car.getSafeGap());
                    gapLimitedSpeed = allowedDistance / dt;
                }

                // Desired speed = min(speed limit, congestion-adjusted speed, gap-limited speed)
                double targetSpeed = Math.min(speedLimit, Math.min(congestionSpeed, gapLimitedSpeed));
                targetSpeed = Math.max(0.0, targetSpeed);
                car.setSpeed(targetSpeed);

                // Update position
                double deltaDist = car.getSpeed() * dt;
                double newPos = car.getPosition() + deltaDist;
                car.addDistance(deltaDist);

                // Junction proximity & arrival check
                if (newPos >= lane.getLength()) {
                    // Reached end of lane (at the junction)
                    handleCarAtJunction(car, lane, dt, arrivedCars);
                } else {
                    car.setPosition(newPos);
                    if (car.getStatus() != Car.Status.ARRIVED && car.getStatus() != Car.Status.STUCK) {
                        car.setStatus(Car.Status.DRIVING);
                    }
                }
            }
        }

        // 3. Process arrived cars
        for (Car car : arrivedCars) {
            handleCarArrival(car);
        }

        // 4. Update rolling throughput window
        if (simTime - windowStartTime >= 60.0) {
            windowStartTime = simTime;
            windowTrips = 0;
        }
    }

    private void handleCarAtJunction(Car car, Lane currentLane, double dt, List<Car> arrivedList) {
        Junction junction = currentLane.getToJunction();
        List<Lane> route = car.getRoute();
        int currentIndex = route.indexOf(currentLane);

        // Check if car has reached its final destination junction
        boolean isDestinationReached = junction.equals(car.getDestination()) ||
                (currentIndex >= 0 && currentIndex == route.size() - 1);

        if (isDestinationReached) {
            car.setPosition(currentLane.getLength());
            car.setStatus(Car.Status.ARRIVED);
            arrivedList.add(car);
            return;
        }

        // Determine next lane along planned route
        Lane nextLane = null;
        if (currentIndex >= 0 && currentIndex + 1 < route.size()) {
            nextLane = route.get(currentIndex + 1);
        } else {
            // Recalculate route if off-track
            List<Lane> newPath = routingEngine.computeRoute(junction, car.getDestination(), network, simTime, car);
            if (newPath != null && !newPath.isEmpty()) {
                car.setRoute(newPath, estimatePathCost(newPath), "Route recalculated at " + junction.getId());
                nextLane = newPath.get(0);
            }
        }

        if (nextLane == null || nextLane.isBlocked()) {
            // Cannot proceed; attempt emergency rerouting
            List<Lane> altPath = routingEngine.computeRoute(junction, car.getDestination(), network, simTime, car);
            if (altPath != null && !altPath.isEmpty()) {
                car.setRoute(altPath, estimatePathCost(altPath), "Rerouted at " + junction.getId() + " - next road blocked");
                nextLane = altPath.get(0);
            } else {
                car.setStatus(Car.Status.STUCK);
                car.setSpeed(0.0);
                return;
            }
        }

        // Junction Throughput & Deadlock Safeguard
        car.addWaitingTimeAtJunction(dt);
        boolean safeguardTriggered = car.getWaitingTimeAtJunction() >= DEADLOCK_SAFEGUARD_SECONDS;

        // Check if junction can admit car (throughput rate)
        boolean junctionClear = junction.canDepart(simTime);
        // Check if target lane has room at entry
        boolean roomAtTarget = hasRoomAtLaneEntry(nextLane, car);

        if ((junctionClear && roomAtTarget) || safeguardTriggered) {
            // Advance into next lane!
            junction.recordDeparture(simTime);
            car.enterLane(nextLane, 0.0);
            car.setStatus(Car.Status.DRIVING);
            car.setWaitingTimeAtJunction(0.0);
        } else {
            // Wait at junction
            car.setPosition(currentLane.getLength());
            car.setSpeed(0.0);
            car.setStatus(Car.Status.WAITING_AT_JUNCTION);
        }
    }

    private boolean hasRoomAtLaneEntry(Lane lane, Car enteringCar) {
        List<Car> carsOnTarget = lane.getCars();
        if (carsOnTarget.isEmpty()) return true;
        // Find car closest to start of lane
        double minPos = Double.MAX_VALUE;
        for (Car c : carsOnTarget) {
            if (c.getPosition() < minPos) {
                minPos = c.getPosition();
            }
        }
        return minPos >= (enteringCar.getLength() + enteringCar.getSafeGap());
    }

    private void handleCarArrival(Car car) {
        totalCompletedTrips++;
        windowTrips++;
        double tripDuration = Math.max(1.0, car.getTotalTripTime());
        completedTripDurations.add(tripDuration);
        double avgSpeedKmH = (car.getDistanceTraveled() / tripDuration) * 3.6;
        completedTripSpeeds.add(avgSpeedKmH);

        if (continuousRespawn) {
            respawnCarAtEntry(car);
        } else {
            car.leaveCurrentLane();
            cars.remove(car);
        }
    }

    private void respawnCarAtEntry(Car car) {
        Junction entry = network.getJunction("ENTRY");
        Junction exit = network.getJunction("EXIT");

        // Pick destination (EXIT or random junction in network)
        Junction dest = (random.nextBoolean()) ? exit : pickRandomJunction(entry);

        car.leaveCurrentLane();
        car.setDestination(dest);
        car.setTripStartTime(simTime);
        car.setTotalTripTime(0.0);
        car.setSpeed(0.0);
        car.setStatus(Car.Status.DRIVING);

        Lane entryLane = network.getRoad("R_ENTRY_J11").getForwardLane();
        car.enterLane(entryLane, 0.0);

        List<Lane> route = routingEngine.computeRoute(entry, dest, network, simTime, car);
        if (route != null && !route.isEmpty()) {
            car.setRoute(route, estimatePathCost(route), "Continuous respawn route");
        }
    }

    private Junction pickRandomJunction(Junction exclude) {
        List<Junction> list = new ArrayList<>(network.getJunctions().values());
        list.remove(exclude);
        return list.get(random.nextInt(list.size()));
    }

    public synchronized void spawnCar() {
        if (cars.size() >= MAX_CARS) return;
        String carId = "C" + String.format("%02d", carCounter++);
        Color color = CAR_COLORS[(carCounter - 1) % CAR_COLORS.length];

        Junction origin = (random.nextDouble() < 0.6) ? network.getJunction("ENTRY") : pickRandomJunction(null);
        Junction dest = (random.nextDouble() < 0.6) ? network.getJunction("EXIT") : pickRandomJunction(origin);

        Car car = new Car(carId, color, origin, dest, simTime);
        
        // Pick an outgoing lane from origin
        List<Lane> outgoing = network.getOutgoingLanes(origin);
        if (outgoing.isEmpty()) return;
        Lane startLane = outgoing.get(random.nextInt(outgoing.size()));

        car.enterLane(startLane, random.nextDouble() * Math.min(20.0, startLane.getLength() * 0.3));
        List<Lane> route = routingEngine.computeRoute(origin, dest, network, simTime, car);
        if (route != null && !route.isEmpty()) {
            car.setRoute(route, estimatePathCost(route), "Initial trip route");
        }
        cars.add(car);

        if (routingEngine != null) {
            routingEngine.onNetworkChanged(network, cars);
        }
    }

    public synchronized void resetScenario(long seed) {
        cars.clear();
        completedTripDurations.clear();
        completedTripSpeeds.clear();
        totalCompletedTrips = 0;
        windowStartTime = 0.0;
        windowTrips = 0;
        simTime = 0.0;
        carCounter = 1;

        // Clear road closures
        for (Road road : network.getRoads().values()) {
            road.setBlocked(false);
        }
        for (Lane lane : network.getLanes().values()) {
            for (Car c : lane.getCars()) {
                lane.removeCar(c);
            }
        }

        // Spawn initial 20 cars spread across the network
        Junction entry = network.getJunction("ENTRY");
        Junction exit = network.getJunction("EXIT");

        for (int i = 0; i < INITIAL_CARS; i++) {
            String carId = "C" + String.format("%02d", carCounter++);
            Color color = CAR_COLORS[(i) % CAR_COLORS.length];

            Junction origin;
            Junction dest;
            if (i < 8) {
                // Main commuter flow: ENTRY -> EXIT
                origin = entry;
                dest = exit;
            } else if (i < 14) {
                // Diagonal shortcut cross traffic
                origin = (i % 2 == 0) ? network.getJunction("J13") : network.getJunction("J31");
                dest = (i % 2 == 0) ? network.getJunction("J31") : network.getJunction("J13");
            } else {
                origin = pickRandomJunction(exit);
                dest = pickRandomJunction(origin);
            }

            Car car = new Car(carId, color, origin, dest, simTime);

            // Compute route
            List<Lane> route = routingEngine.computeRoute(origin, dest, network, simTime, car);
            if (route != null && !route.isEmpty()) {
                Lane startLane = route.get(0);
                double startOffset = (i % 4) * 20.0;
                startOffset = Math.min(startOffset, startLane.getLength() * 0.8);
                car.enterLane(startLane, startOffset);
                car.setRoute(route, estimatePathCost(route), "Scenario launch route");
                cars.add(car);
            }
        }

        if (routingEngine != null) {
            routingEngine.onNetworkChanged(network, cars);
        }
    }

    public synchronized void toggleRoadBlock(Road road) {
        road.toggleBlocked();
        // Immediately notify routing engine and trigger reroute for all cars
        if (routingEngine != null) {
            routingEngine.onNetworkChanged(network, cars);
        }
        for (Car car : cars) {
            if (car.getCurrentLane() != null && car.getCurrentLane().getParentRoad().equals(road)) {
                if (road.isBlocked()) {
                    car.setStatus(Car.Status.STUCK);
                    car.setSpeed(0.0);
                } else {
                    car.setStatus(Car.Status.DRIVING);
                }
            }
        }
    }

    public synchronized void handleCarDragged(Car car, Point2D newPos) {
        car.setBeingDragged(true);
        car.setDragPosition(new Point2D.Double(newPos.getX(), newPos.getY()));
    }

    public synchronized void handleCarDropped(Car car, Point2D dropPos) {
        car.setBeingDragged(false);
        car.setDragPosition(null);

        // Snap to nearest lane and offset
        TrafficNetwork.SnapResult snap = network.snapPointToNearestLane(dropPos);
        if (snap.lane() != null) {
            car.enterLane(snap.lane(), snap.offset());
            // Immediate reroute from this new position
            Junction nextJunction = snap.lane().getToJunction();
            List<Lane> newPath = routingEngine.computeRoute(nextJunction, car.getDestination(), network, simTime, car);
            List<Lane> fullPath = new ArrayList<>();
            fullPath.add(snap.lane());
            if (newPath != null) {
                fullPath.addAll(newPath);
            }
            car.setRoute(fullPath, estimatePathCost(fullPath), "Manual reposition to " + snap.lane().getId());
            car.setStatus(Car.Status.DRIVING);

            if (routingEngine != null) {
                routingEngine.onNetworkChanged(network, cars);
            }
        }
    }

    public double estimatePathCost(List<Lane> path) {
        if (path == null) return 0.0;
        double cost = 0.0;
        for (Lane lane : path) {
            cost += lane.getBprTravelTime();
        }
        return cost;
    }

    // Getters and Setters
    public TrafficNetwork getNetwork() { return network; }
    public List<Car> getCars() { return Collections.unmodifiableList(cars); }
    public RoutingEngine getRoutingEngine() { return routingEngine; }

    public synchronized void setRoutingEngine(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
        if (this.routingEngine != null) {
            this.routingEngine.onNetworkChanged(network, cars);
        }
    }

    public double getSimTime() { return simTime; }
    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double mult) { this.speedMultiplier = mult; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isContinuousRespawn() { return continuousRespawn; }
    public void setContinuousRespawn(boolean respawn) { this.continuousRespawn = respawn; }

    public int getTotalCompletedTrips() { return totalCompletedTrips; }
    public List<Double> getCompletedTripDurations() { return completedTripDurations; }
    public List<Double> getCompletedTripSpeeds() { return completedTripSpeeds; }

    public double getAverageTripTime() {
        if (completedTripDurations.isEmpty()) return 0.0;
        double sum = 0;
        for (double d : completedTripDurations) sum += d;
        return sum / completedTripDurations.size();
    }

    public double getAverageActiveSpeed() {
        if (cars.isEmpty()) return 0.0;
        double sum = 0;
        int activeCount = 0;
        for (Car car : cars) {
            if (!car.isBeingDragged() && car.getStatus() != Car.Status.ARRIVED) {
                sum += car.getSpeed() * 3.6; // convert m/s to km/h
                activeCount++;
            }
        }
        return activeCount > 0 ? (sum / activeCount) : 0.0;
    }

    public int getStuckCarsCount() {
        int count = 0;
        for (Car car : cars) {
            if (car.getStatus() == Car.Status.STUCK) count++;
        }
        return count;
    }

    public double getThroughputCarsPerMinute() {
        if (simTime < 1.0) return 0.0;
        return (totalCompletedTrips / simTime) * 60.0;
    }

    public Road getMostCongestedRoad() {
        Road most = null;
        double maxRatio = -1.0;
        for (Road road : network.getRoads().values()) {
            double ratio = road.getMaxCongestionRatio();
            if (ratio > maxRatio) {
                maxRatio = ratio;
                most = road;
            }
        }
        return most;
    }
}
