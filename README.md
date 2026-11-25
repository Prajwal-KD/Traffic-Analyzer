# TrafficSense 🚦
### Pure Java Traffic Simulation, Predictive Routing, and Real-Time Congestion Balancing Engine

[![Java 25](https://img.shields.io/badge/Java-25%20(Adoptium%20OpenJDK)-orange.svg)](https://adoptium.net/)
[![Maven 3.9](https://img.shields.io/badge/Build-Maven%203.9+-blue.svg)](https://maven.apache.org/)
[![GUI Swing/Java2D](https://img.shields.io/badge/GUI-Swing%20%2F%20Java2D%20(60FPS)-brightgreen.svg)]()
[![Tests JUnit 5](https://img.shields.io/badge/Tests-14%2F14%20Passing-success.svg)](https://junit.org/junit5/)
[![License MIT](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

**TrafficSense** is a high-performance microscopic traffic simulation, real-time vehicle navigation, and predictive load-balancing engine built from scratch in pure **Java 25** using **Java Swing and Java2D**. 

Designed to demonstrate algorithmic solutions to urban traffic congestion and Braess's Paradox, the system models continuous bumper-to-bumper car-following physics, capacity-constrained junctions, dynamic accidents, and compares **three distinct routing paradigms** across an interactive desktop interface and an accelerated headless benchmark runner.

---

## 📑 Table of Contents

- [Key Highlights](#-key-highlights)
- [Project Architecture & Code Structure](#-project-architecture--code-structure)
- [Locality Topology (15 Roads, 11 Junctions)](#-locality-topology-15-roads-11-junctions)
- [Physics & Mathematical Modeling](#-physics--mathematical-modeling)
  - [BPR Congestion Function](#1-bpr-bureau-of-public-roads-congestion-function)
  - [Microscopic Car-Following Physics](#2-microscopic-car-following-physics)
  - [Junction Throughput & Deadlock Safeguard](#3-junction-throughput--deadlock-safeguard)
- [The Three Routing Engines](#-the-three-routing-engines)
  - [1. Naive Routing Engine](#1-naive-routing-engine-static-distance)
  - [2. Selfish Dynamic Routing Engine](#2-selfish-dynamic-routing-engine-reactive-bpr)
  - [3. Coordinated Predictive Routing Engine](#3-coordinated-predictive-routing-engine-central-smart-coordinator)
- [Autonomous NavigationClient HUD](#-autonomous-navigationclient-hud)
- [Interactive Desktop UI](#-interactive-desktop-ui)
- [Headless Benchmark Results](#-headless-benchmark-results)
- [Academic Context](#-academic-context)
- [Prerequisites & Build Instructions](#-prerequisites--build-instructions)
- [Running Automated Tests](#-running-automated-tests)
- [License](#-license)

---

## 🌟 Key Highlights

* **Pure Java (Zero External Runtime Libraries)**: Built entirely on native JDK 25 primitives, `java.util.concurrent`, and `javax.swing` / `java.awt.Graphics2D`.
* **Realistic Urban Locality**: 11 junctions and 15 bidirectional roads (30 directional lanes), featuring multi-lane arterial expressways, a 3×3 grid core, and a narrow high-risk **diagonal shortcut alley (`J13 ↔ J31`)**.
* **Microscopic 50 ms Timestep Simulation**: High-fidelity physics loop driven by a daemon `ScheduledExecutorService`, enforcing non-overlapping bumper-to-bumper spacing, junction throughput metering, and deceleration curves.
* **Three Swappable Routing Strategies**: Real-time switching between Static Shortest Path, Reactive Live BPR with Hysteresis, and Centralized 60-second Predictive Horizon Routing.
* **Autonomous In-Vehicle Telemetry**: Every car houses a dedicated `NavigationClient` reporting real-time ETA, next-turn guidance, and natural-language explanations for rerouting decisions.
* **Rich 60 FPS Java2D Visualizer**: Real-time double-buffered canvas with anti-aliasing, color-coded road stress, interactive vehicle drag-and-drop snapping, and mouse-click accident generation.
* **Accelerated Headless Benchmark**: Deterministic 300-second simulated test with fixed random seed (`42L`) to benchmark trip completion, network throughput, average velocity, and congestion patterns.

---

## 🏛️ Project Architecture & Code Structure

The project strictly adheres to Object-Oriented Programming (OOP) principles, separating concerns across clean, decoupled packages:

```
TrafficSense/
├── pom.xml                                      # Maven 3.9+ project descriptor (Java 25, JUnit 5, Shade JAR)
├── README.md                                    # Comprehensive engineering & user documentation
├── .gitignore                                   # Ignore build artifacts, temp files, and IDE configs
├── TrafficSense_Mini_Project_Report.doc         # Academic mini-project report (Word HTML/XML format)
├── TRAFFICSENSE_MINI_PROJECT_REPORT.md          # Full academic report in Markdown
├── generate_report.py                           # Python generator for academic Word documentation
│
└── src/
    ├── main/
    │   └── java/
    │       └── com/trafficsense/
    │           │
    │           ├── Main.java                    # Application entry point (Swing bootstrap & Look & Feel)
    │           │
    │           ├── model/                       # Core domain entities & road network model
    │           │   ├── Car.java                 # Vehicle state, physics, position, and navigation client
    │           │   ├── Junction.java            # Intersection node with coordinates and gate attributes
    │           │   ├── Lane.java                # Directed segment with capacity, speed limit, and BPR metrics
    │           │   ├── Road.java                # Bidirectional road container managing forward & reverse lanes
    │           │   ├── TrafficNetwork.java      # Network topology builder (11 junctions, 15 roads, 30 lanes)
    │           │   └── NavigationClient.java    # Embedded navigation HUD: ETA, next turn, reroute explanations
    │           │
    │           ├── routing/                     # Graph algorithms & routing implementations
    │           │   ├── RoutingEngine.java       # Unified strategy interface for route computation
    │           │   ├── RoutingEngineType.java   # Enumeration: NAIVE, SELFISH_DYNAMIC, COORDINATED_PREDICTIVE
    │           │   ├── DijkstraRouter.java      # Generic, optimized priority-queue Dijkstra implementation
    │           │   ├── NaiveRoutingEngine.java  # Static distance-only shortest path router
    │           │   ├── SelfishDynamicRoutingEngine.java # Live BPR router with 10% hysteresis threshold
    │           │   └── CoordinatedPredictiveRoutingEngine.java # 60-second rolling predictive coordinator
    │           │
    │           ├── sim/                         # Microscopic traffic simulation loop & physics
    │           │   └── TrafficSimulation.java   # 50 ms loop, car-following, junction queues, and event dispatch
    │           │
    │           ├── ui/                          # Swing & Java2D presentation layer
    │           │   ├── MainFrame.java           # Top-level window hosting canvas, toolbar, and metrics
    │           │   ├── TrafficCanvas.java       # 60 FPS double-buffered Java2D canvas with mouse interaction
    │           │   ├── ControlToolbar.java      # Simulation controls (engine, pause/play, speed, add car)
    │           │   ├── MetricsPanel.java        # Real-time metrics HUD & selected vehicle telemetry inspector
    │           │   └── BenchmarkDialog.java     # Modal UI running and displaying the 3-engine benchmark
    │           │
    │           └── benchmark/                   # Automated benchmarking engine
    │               ├── BenchmarkResult.java     # Metrics data class (completed trips, throughput, speeds, etc.)
    │               └── BenchmarkRunner.java     # Headless deterministic 300s runner with fixed seed
    │
    └── test/
        └── java/
            └── com/trafficsense/
                ├── model/
                │   ├── TopologyTest.java        # Asserts exactly 11 junctions, 15 roads, and 30 lanes
                │   └── BprCongestionModelTest.java # Verifies BPR delay exponent curve and capacities
                ├── routing/
                │   ├── RoutingEnginesTest.java  # Tests Dijkstra, hysteresis thresholds, and reroute logic
                │   └── BenchmarkComparisonTuningTest.java # Validates engine performance differentials
                ├── sim/
                │   └── CarFollowingPhysicsTest.java # Verifies safe headway, non-overlap, and junction stopping
                └── benchmark/
                    └── BenchmarkRunnerTest.java # Asserts deterministic headless benchmark execution
```

---

## 🗺️ Locality Topology (15 Roads, 11 Junctions)

The network models a typical urban sector fed by a major peripheral highway, exiting onto an expressway, with a dense inner grid and a hazardous shortcut alley:

```
  [ENTRY] 
     │ (R_ENTRY_J11: 260m, 70km/h, Cap 14)
     ▼
  [ J11 ] ──────── (R_J11_J12) ──────── [ J12 ] ──────── (R_J12_J13) ──────── [ J13 ]
     │                                     │                                     │   ▲
     │ (R_J11_J21)                         │ (R_J12_J22)                         │   │
     ▼                                     ▼                                     │   │ (R_J13_J31: 160m, 30km/h, Cap 2)
  [ J21 ] ──────── (R_J21_J22) ──────── [ J22 ] ──────── (R_J22_J23) ──────── [ J23 ] │  [DIAGONAL SHORTCUT ALLEY]
     │                                     │                                     │   │
     │ (R_J21_J31)                         │ (R_J22_J32)                         │   │
     ▼                                     ▼                                     ▼   ▼
  [ J31 ] ──────── (R_J31_J32) ──────── [ J32 ] ──────── (R_J32_J33) ──────── [ J33 ]
                                                                                   │
                                                     (R_J33_EXIT: 260m, 70km/h)    │
                                                                                   ▼
                                                                                [EXIT]
```

### Road Characteristics

| Road Identifier | Friendly Name | Connected Nodes | Length | Speed Limit | Capacity | Description |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| `R_ENTRY_J11` | Main Entry Boulevard | `ENTRY` ↔ `J11` | 260 m | 70 km/h | 14 cars | High-capacity primary inflow expressway. |
| `R_J33_EXIT` | Main Exit Expressway | `J33` ↔ `EXIT` | 260 m | 70 km/h | 14 cars | High-capacity primary outflow highway. |
| `R_J13_J31` | **Diagonal Shortcut Alley** | `J13` ↔ `J31` | **160 m** | **30 km/h** | **2 cars** | **Narrow shortcut alley with severe bottleneck risk.** |
| `R_J11_J12` | North Avenue West | `J11` ↔ `J12` | 210 m | 50 km/h | 6 cars | Northern cross-town artery. |
| `R_J12_J13` | North Avenue East | `J12` ↔ `J13` | 210 m | 50 km/h | 6 cars | Northern cross-town artery. |
| `R_J21_J22` | Central Avenue West | `J21` ↔ `J22` | 210 m | 50 km/h | 6 cars | Central arterial boulevard. |
| `R_J22_J23` | Central Avenue East | `J22` ↔ `J23` | 210 m | 50 km/h | 6 cars | Central arterial boulevard. |
| `R_J31_J32` | South Avenue West | `J31` ↔ `J32` | 210 m | 50 km/h | 6 cars | Southern cross-town artery. |
| `R_J32_J33` | South Avenue East | `J32` ↔ `J33` | 210 m | 50 km/h | 6 cars | Southern cross-town artery. |
| `R_J11_J21` | 1st Street North | `J11` ↔ `J21` | 200 m | 50 km/h | 6 cars | Western vertical boulevard. |
| `R_J21_J31` | 1st Street South | `J21` ↔ `J31` | 200 m | 50 km/h | 6 cars | Western vertical boulevard. |
| `R_J12_J22` | 2nd Street North | `J12` ↔ `J22` | 200 m | 50 km/h | 6 cars | Central vertical artery. |
| `R_J22_J32` | 2nd Street South | `J22` ↔ `J32` | 200 m | 50 km/h | 6 cars | Central vertical artery. |
| `R_J13_J23` | 3rd Street North | `J13` ↔ `J23` | 200 m | 50 km/h | 6 cars | Eastern vertical boulevard. |
| `R_J23_J33` | 3rd Street South | `J23` ↔ `J33` | 200 m | 50 km/h | 6 cars | Eastern vertical boulevard. |

---

## 📐 Physics & Mathematical Modeling

### 1. BPR (Bureau of Public Roads) Congestion Function

Dynamic travel time across any lane $e$ is modeled via the standard transportation engineering Bureau of Public Roads (BPR) volume-delay formulation:

$$T(e) = T_{\text{free}}(e) \times \left(1 + \alpha \cdot \left(\frac{V(e)}{C(e)}\right)^\beta\right)$$

Where:
- $T_{\text{free}}(e) = \frac{\text{Length}(e)}{\text{SpeedLimit}(e)}$ is the free-flow travel time.
- $V(e)$ is the current vehicle volume (number of cars occupying the lane).
- $C(e)$ is the design saturation capacity of the lane.
- $\alpha = 0.15$ and $\beta = 4.0$ (standard BPR curve parameters).

When closed due to an accident, the effective capacity drops to zero, and the cost returns `Double.POSITIVE_INFINITY`.

### 2. Microscopic Car-Following Physics

Each vehicle's longitudinal motion is evaluated on every 50 ms tick ($dt = 0.05\text{ s}$):
- **Free-flow Speed Adjustment**: Speed adjusts dynamically to lane congestion:
  $$v_{\text{target}} = \frac{\text{length}}{T_{\text{BPR}}}$$
- **Bumper-to-Bumper Collision Prevention**:
  For vehicle $i$ following leading vehicle $i-1$:
  $$\text{gap} = \text{pos}_{i-1} - \text{pos}_i - \text{CarLength} \quad (\text{CarLength} = 4.5\text{ m})$$
  If $\text{gap} < d_{\text{safe}}$ (where $d_{\text{safe}} = 2.5\text{ m}$), the following vehicle decelerates immediately to match or remain below the lead vehicle's speed, preventing collisions.

### 3. Junction Throughput & Deadlock Safeguard

- **Throughput Throttling**: Junctions enforce a maximum departure rate of **2 cars per second** to simulate intersection turning conflicts and cross-traffic delays.
- **Deadlock Safeguard**: If a vehicle remains stationary at an intersection queue for greater than **4.0 seconds**, the simulation activates an anti-deadlock escape mechanism to preserve network liveness.

---

## 🧠 The Three Routing Engines

TrafficSense implements three swappable routing engines via the `RoutingEngine` interface:

### 1. Naive Routing Engine (Static Distance)
- **Algorithm**: Dijkstra's shortest path purely on physical road length: $\text{Cost}(e) = \text{Length}(e)$.
- **Behavior**: Completely unaware of real-time or future traffic volume.
- **Flaw**: Because the diagonal shortcut alley `R_J13_J31` (160 m) is geometrically shorter than perimeter roads (200 m + 210 m), **100% of vehicles blindly flock to the narrow shortcut**. 
- **Result**: Catastrophic bottlenecking and complete gridlock where vehicles stall indefinitely.

### 2. Selfish Dynamic Routing Engine (Reactive BPR)
- **Algorithm**: Dijkstra's shortest path on instantaneous live BPR travel times: $\text{Cost}(e) = T_{\text{BPR}}(V_{\text{current}}(e))$.
- **10% Hysteresis Threshold**: To prevent frantic jittering, a vehicle only accepts an alternative route if the candidate's estimated travel time provides a meaningful improvement:
  $$T_{\text{candidate}} \le 0.90 \times T_{\text{current\_remaining}}$$
- **Flaw (Herding Oscillations)**: When an artery becomes congested, all approaching drivers react simultaneously and reroute to the alternative corridor. This suddenly overloads the second corridor while leaving the first empty, causing perpetual "herding waves" back and forth.

### 3. Coordinated Predictive Routing Engine (Central Smart Coordinator)
- **Algorithm**: Centralized time-dependent load coordinator maintaining a **rolling 60-second predictive load profile** partitioned into 1-second discrete bins ($t \in [0, 60]$).
- **Mechanism**:
  1. Vehicles are prioritized and sorted in descending order of distance-to-destination.
  2. Each vehicle's path is computed using time-expanded Dijkstra where the cost of traversing lane $e$ arriving at time $t$ is:
     $$\text{Cost}(e, t) = T_{\text{BPR}}\Big(\text{PredictedLoad}(e, t)\Big) + \Big(\text{PredictedLoad}(e, t) \times 2.0\text{ s}\Big)$$
  3. Once a route is assigned, the vehicle's future presence across each successive lane is **pre-booked in the global reservation schedule**.
  4. Subsequent vehicles automatically see this future congestion and are steered onto parallel arteries **before** any bottleneck can form.
- **Result**: Evenly balanced traffic across all 15 roads, high throughput, zero oscillations, and fluid traffic flow.

---

## 📟 Autonomous NavigationClient HUD

Every vehicle operates an embedded `NavigationClient` that interfaces directly with the central simulation and routing layer. It exposes:
- **Live ETA**: Dynamic estimated remaining seconds to destination.
- **Next Turn Direction**: Actionable turn-by-turn guidance (e.g. *"Turn left onto R_J12_J22"* or *"Proceed onto Main Entry Boulevard"*).
- **Natural Language Reroute Reasoning**: Transparent human-readable explanations whenever a reroute occurs (e.g. *"Rerouted: R_J21_J31 congested, saves 14 s"*).

---

## 🖥️ Interactive Desktop UI

The graphical user interface is built in double-buffered Java2D rendering at a smooth 60 frames per second:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ [Engine: Coordinated Predictive ▼] [▶ Play] [Speed: 1x ▼] [Respawn: ON] [+ Add] [↺] [⚡ Benchmark]│
├──────────────────────────────────────────────────────────┬─────────────────────────────┤
│                                                          │ 📊 REAL-TIME METRICS        │
│                                                          │ Engine: COORDINATED_PREDICT │
│                   TRAFFIC CANVAS                         │ Active Cars: 20             │
│                                                          │ Completed Trips: 42         │
│     [ENTRY] ─── (Green) ─── [J11] ─── [J12] ─── [J13]    │ Avg Speed: 57.8 km/h        │
│                               │         │         │      │ Avg Trip Time: 64.2 s       │
│                               │         │         │      ├─────────────────────────────┤
│                             [J21] ─── [J22] ─── [J23]    │ 🚗 VEHICLE TELEMETRY        │
│                               │         │         │      │ Selected: Car-7             │
│                               │         │         │      │ Speed: 48.5 km/h            │
│                             [J31] ─── [J32] ─── [J33]    │ Status: DRIVING             │
│                                                   │      │ ETA: 24.3 s                 │
│                                                 [EXIT]   │ Next Turn: Turn right       │
│                                                          │ Reason: Predictive balance  │
└──────────────────────────────────────────────────────────┴─────────────────────────────┘
```

### UI Controls & Interactivity

| Action | Control | Description |
| :--- | :--- | :--- |
| **Engine Selector** | Dropdown Menu | Dynamically switch between Naive, Selfish Dynamic, and Coordinated Predictive. |
| **Simulation Speed** | Dropdown Menu | Accelerate or decelerate simulation execution (`0.5x`, `1.0x`, `2.0x`, `4.0x`). |
| **Play / Pause** | Button | Freeze and resume the microscopic physics loop. |
| **Add Vehicle** | Button | Inject a commuter vehicle at `ENTRY` (up to network capacity of 25 cars). |
| **Reset Network** | Button | Reset network to the deterministic baseline state. |
| **Launch Benchmark**| Button | Opens modal benchmark runner to run a 300 s seeded comparative trial. |
| **Block Road (Accident)** | **Left-Click Road** | Toggle accident closure on any lane. Turns road to hazard stripes; forces rerouting. |
| **Drag & Drop Vehicle** | **Mouse Drag** | Drag any car anywhere on the canvas; snaps onto the nearest lane and recalculates path. |
| **Select Vehicle** | **Left-Click Car** | Selects vehicle, highlights its planned route in cyan, and displays live telemetry in HUD. |

### Color-Coded Congestion Legend
- 🟢 **Green (< 50% Capacity)**: Free-flow traffic.
- 🟡 **Yellow (50% – 80% Capacity)**: Moderate density.
- 🟠 **Orange (80% – 100% Capacity)**: Near capacity; speed reducing.
- 🔴 **Red (> 100% Capacity)**: Severe congestion and queuing.
- 🚧 **Black/Yellow Hazard Stripes**: Road closed due to simulated accident.

---

## 📊 Headless Benchmark Results

Deterministic 300-second (5-minute) simulation run with fixed random seed (`42L`) comparing all 3 routing engines on identical traffic demand:

| Metric | Naive (Static Distance) | Selfish Dynamic (Live BPR) | Coordinated Predictive | Advantage of Predictive |
| :--- | :---: | :---: | :---: | :--- |
| **Completed Trips** | 18 | 73 | **79** | **+8.2% more throughput** vs Dynamic (+338% vs Naive) |
| **Throughput (cars/min)** | 3.6 | 14.6 | **15.8** | **+1.2 cars/min** sustained departure rate |
| **Average Trip Time** | 34.0 s *(early trips only)* | 72.2 s | **64.5 s** | **Saves 7.7 seconds per trip** (-10.7% time) |
| **Average Vehicle Speed** | 3.1 km/h | 39.0 km/h | **58.2 km/h** | **+49.2% faster transit speed** |
| **Peak Lane Congestion** | **175%** *(Severe Bottleneck)* | 125% | **75%** | Prevents congestion from exceeding 100% |
| **Network Failure Rate** | **90% trapped in gridlock** | 0% | **0%** | Zero stalls; smooth, fluid circulation |

### Key Benchmark Takeaways
1. **The Static Shortest Path Trap**: Naive Dijkstra causes total paralysis. 18 early cars reach the destination before the diagonal alley (`R_J13_J31`) reaches 175% capacity, after which all subsequent cars are permanently gridlocked at 3.1 km/h.
2. **The Reactive Oscillation Trap**: Selfish Dynamic avoids permanent gridlock by rerouting around jams, but reactive decisions cause waves of congestion that oscillate between parallel avenues, degrading average vehicle speed to 39.0 km/h.
3. **The Predictive Coordination Advantage**: Coordinated Predictive foresees future arrivals across rolling 60-second horizons, distributes traffic across all corridors before jams form, keeps peak congestion at 75%, and delivers a **49.2% increase in vehicle velocity** and **7.7 seconds saved per commuter trip**.

---

## 🎓 Academic Context

This project was developed as an **Object-Oriented Programming with Java** Mini-Project at **Vidyavardhaka College of Engineering (VVCE)**:
- **Department**: Department of Computer Science & Engineering (Artificial Intelligence & Machine Learning)
- **Course**: OOPS with Java (7th Semester Mini-Project)
- **Faculty Guide**: Prof. Revathy B D, Assistant Professor, Dept. of CSE (AI & ML)
- **Academic Reports Included**:
  - `TrafficSense_Mini_Project_Report.doc`: Formatted academic report with Times New Roman styling, 1.15 line spacing, rubrics, certificate, and evaluation tables.
  - `TRAFFICSENSE_MINI_PROJECT_REPORT.md`: Comprehensive Markdown documentation matching academic submission guidelines.

---

## ⚙️ Prerequisites & Build Instructions

### Prerequisites
- **Java Development Kit**: JDK 25 (e.g. Eclipse Adoptium OpenJDK 25)
- **Build Tool**: Apache Maven 3.9+
- **OS**: Windows, macOS, or Linux

### 1. Clone the Repository
```bash
git clone https://github.com/Prajwal-KD/Traffic-Analyzer.git
cd Traffic-Analyzer
```

### 2. Compile the Source Code
```bash
mvn clean compile
```

### 3. Launch the Application via Maven
```bash
mvn exec:java
```

### 4. Build a Runnable Standalone Fat JAR
```bash
mvn package
```
This produces a self-contained executable JAR in `target/`:
```bash
java -jar target/TrafficSense-1.0-SNAPSHOT.jar
```

---

## 🧪 Running Automated Tests

TrafficSense includes a full suite of **14 unit and integration tests** verifying topology constraints, BPR congestion curves, car-following physics, Dijkstra hysteresis, and headless benchmarking:

```bash
mvn test
```

### Test Coverage Highlights
- `TopologyTest`: Verifies exactly 11 junctions, 15 roads, 30 lanes, and lane connectivity.
- `BprCongestionModelTest`: Validates non-linear BPR delay growth and road closure infinity costs.
- `CarFollowingPhysicsTest`: Verifies minimum bumper-to-bumper buffer ($2.5\text{ m}$) and lead-vehicle deceleration.
- `RoutingEnginesTest`: Validates the 10% hysteresis threshold and dynamic rerouting triggers.
- `BenchmarkRunnerTest`: Confirms headless 300s benchmark execution and metric consistency.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — free for academic, personal, and educational use.
