# TrafficSense 🚦
**Traffic Simulation + Prediction + Smart-Routing Engine with an Interactive Desktop UI**

TrafficSense is a high-performance, pure Java traffic simulation, real-time routing, and predictive load-balancing engine featuring an interactive Swing / Java2D desktop interface.

---

## 🌟 Key Highlights

* **15-Road Realistic Locality**:
  * 9 Junctions in a 3×3 grid (`J11` through `J33`) + `ENTRY` gate + `EXIT` gate (11 total nodes).
  * 15 bidirectional roads (30 directional lanes), including high-capacity feeder/drain expressways, 12 internal grid avenues, and a narrow **diagonal shortcut alley (`J13 ↔ J31`)**.
* **Microscopic Traffic Physics**:
  * Fixed 50 ms timestep simulation loop driven by a `ScheduledExecutorService`.
  * Collision-free car-following physics ensuring vehicles never overlap and respect gap limits.
  * Capacity-limited junction throughput (max 2 cars/second) with automatic deadlock safeguards.
  * Bureau of Public Roads (**BPR**) congestion function:
    $$\text{travelTime} = \text{freeFlowTime} \times \left(1 + 0.15 \times \left(\frac{\text{load}}{\text{capacity}}\right)^4\right)$$
* **3 Swappable Routing Engines**:
  1. **NAIVE**: Static shortest-by-distance (Dijkstra on length). Unaware of congestion; vulnerable to severe bottlenecks.
  2. **SELFISH DYNAMIC**: Reactive Dijkstra on live BPR travel times with a **10% hysteresis threshold** to mitigate herding oscillations.
  3. **COORDINATED PREDICTIVE (Smart Engine)**: Central coordinator projecting a rolling **60-second time-dependent load profile**. Dispatches vehicles sorted by distance-to-destination and reserves future capacity across parallel arteries before bottlenecks can form.
* **NavigationClient HUD**:
  * Every vehicle houses an autonomous `NavigationClient` exposing live ETA, next-turn guidance, and human-readable reroute explanations (e.g., *"Rerouted: R_J21_J31 congested, saves 14 s"*).
* **Interactive Java2D Desktop UI**:
  * 60 FPS double-buffered canvas with anti-aliasing.
  * Roads color-coded by congestion level: Green (<50%), Yellow (50–80%), Orange (80–100%), Red (>100%), and hazard striping for closures.
  * **Drag & Drop Vehicles**: Pick up any car with the mouse, drop it anywhere, and watch it snap to the nearest lane and instantly reroute.
  * **Road Accidents / Closures**: Click any road to toggle accident closures and force real-time rerouting.
  * **Vehicle Telemetry HUD**: Click or hover over any car to highlight its planned path and inspect its live telemetry.
* **Accelerated Headless Benchmark**:
  * Run an accelerated 5-minute (300 s) headless run using a fixed random seed (`42L`) across all 3 engines.
  * Displays a side-by-side comparison table showing completed trips, throughput, average trip time, average vehicle speed, and peak congestion.

---

## 📊 Benchmark Results (5-Minute Seeded Run)

| Routing Engine | Completed Trips | Throughput | Avg Trip Time | Avg Vehicle Speed | Peak Congestion | System Behavior |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| **Naive (Static Distance)** | **18** | **3.6 cars/min** | 34.0 s (early trips only) | **3.1 km/h** | **175%** | **Catastrophic Gridlock**: 100% of vehicles pile onto the shortest corridor and diagonal alley, causing gridlock where 18/20 vehicles never arrive. |
| **Selfish Dynamic (Live BPR)** | **73** | **14.6 cars/min** | 72.2 s | **39.0 km/h** | **125%** | **Herding Oscillations**: Reactive rerouting avoids total paralysis but suffers from herding waves back and forth between parallel avenues. |
| **Coordinated Predictive** | **79** | **15.8 cars/min** | **64.5 s** | **58.2 km/h** | **75%** | **Optimal Fluid Flow**: Disperses vehicles evenly across all corridors and alleys; **saves 7.7 s per trip** with **+49% higher speed** than Selfish Dynamic. |

---

## 🛠️ Requirements & Toolchain

* **Java**: JDK 25 (e.g. Eclipse Adoptium OpenJDK 25)
* **Build System**: Apache Maven 3.9+
* **Dependencies**: JUnit 5 (test scope only). Zero external runtime or UI libraries.

---

## 🚀 Building & Running

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Launch the Application (via Maven)
```bash
mvn exec:java
```

### 3. Build & Run Standalone Runnable JAR
```bash
mvn package
java -jar target/TrafficSense-1.0-SNAPSHOT.jar
```

### 4. Run Automated Unit Tests
```bash
mvn test
```

---

## 🎮 Desktop UI Controls

| Control | Description |
| :--- | :--- |
| **Engine Dropdown** | Switch between *Naive*, *Selfish Dynamic*, and *Coordinated Predictive* in real time. |
| **Play / Pause** | Toggle simulation loop execution. |
| **Speed Multipliers** | Run simulation at `0.5x`, `1.0x`, `2.0x`, or `4.0x` speed. |
| **Continuous Respawn** | Toggle continuous commuter spawning when vehicles reach `EXIT`. |
| **+ Add Vehicle** | Inject a new car into the locality (up to 25 cars limit). |
| **↺ Reset** | Reset locality to the deterministic baseline scenario. |
| **⚡ Run Benchmark** | Open the headless benchmark modal comparing all 3 engines. |
| **Click Road** | Toggle accident blockage (road closes, vehicles reroute). |
| **Drag & Drop Car** | Drag any car to any coordinate; snaps to the nearest lane and recalculates route. |
| **Click Car** | Inspect live NavigationClient telemetry in the sidebar HUD. |

---

## 📄 License
MIT License. Created for the TrafficSense Project.
