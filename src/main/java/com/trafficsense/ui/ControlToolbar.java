package com.trafficsense.ui;

import com.trafficsense.routing.*;
import com.trafficsense.sim.TrafficSimulation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Top control toolbar providing simulation orchestration, speed multiplier controls,
 * continuous respawn toggle, manual car injection, and benchmark launching.
 */
public class ControlToolbar extends JToolBar {
    private final TrafficSimulation simulation;
    private final Frame parentFrame;

    private final JComboBox<RoutingEngineType> cbEngine;
    private final JButton btnPlayPause;
    private final JCheckBox chkRespawn;
    private final JButton btnAddCar;
    private final JButton btnReset;
    private final JButton btnBenchmark;

    public ControlToolbar(TrafficSimulation simulation, Frame parentFrame) {
        this.simulation = simulation;
        this.parentFrame = parentFrame;

        setFloatable(false);
        setBackground(new Color(36, 46, 56));
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

        // 1. Engine Selector
        JLabel lblEngine = new JLabel("Engine:");
        lblEngine.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblEngine.setForeground(new Color(180, 195, 210));
        add(lblEngine);

        cbEngine = new JComboBox<>(RoutingEngineType.values());
        cbEngine.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbEngine.setBackground(new Color(48, 60, 72));
        cbEngine.setForeground(Color.WHITE);
        cbEngine.setSelectedItem(simulation.getRoutingEngine().getType());
        cbEngine.addActionListener(e -> {
            RoutingEngineType type = (RoutingEngineType) cbEngine.getSelectedItem();
            if (type == null) return;
            RoutingEngine newEngine = switch (type) {
                case NAIVE -> new NaiveRoutingEngine();
                case SELFISH_DYNAMIC -> new SelfishDynamicRoutingEngine();
                case COORDINATED_PREDICTIVE -> new CoordinatedPredictiveRoutingEngine();
            };
            simulation.setRoutingEngine(newEngine);
        });
        add(cbEngine);

        addSeparator();

        // 2. Play / Pause Button
        btnPlayPause = createStyledButton("❚❚ Pause", new Color(52, 73, 94));
        btnPlayPause.addActionListener(e -> {
            boolean isPaused = !simulation.isPaused();
            simulation.setPaused(isPaused);
            btnPlayPause.setText(isPaused ? "▶ Resume" : "❚❚ Pause");
            btnPlayPause.setBackground(isPaused ? new Color(39, 174, 96) : new Color(52, 73, 94));
        });
        add(btnPlayPause);

        // 3. Speed Multiplier
        JLabel lblSpeed = new JLabel("Speed:");
        lblSpeed.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSpeed.setForeground(new Color(180, 195, 210));
        add(lblSpeed);

        ButtonGroup speedGroup = new ButtonGroup();
        double[] speeds = {0.5, 1.0, 2.0, 4.0};
        for (double spd : speeds) {
            JToggleButton btnSpeed = new JToggleButton(spd + "x", spd == 1.0);
            btnSpeed.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            btnSpeed.setBackground(new Color(48, 60, 72));
            btnSpeed.setForeground(Color.WHITE);
            btnSpeed.setFocusPainted(false);
            btnSpeed.addActionListener(e -> simulation.setSpeedMultiplier(spd));
            speedGroup.add(btnSpeed);
            add(btnSpeed);
        }

        addSeparator();

        // 4. Continuous Respawn Checkbox
        chkRespawn = new JCheckBox("Continuous Respawn", simulation.isContinuousRespawn());
        chkRespawn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkRespawn.setForeground(new Color(220, 230, 240));
        chkRespawn.setOpaque(false);
        chkRespawn.addActionListener(e -> simulation.setContinuousRespawn(chkRespawn.isSelected()));
        add(chkRespawn);

        addSeparator();

        // 5. Add Car
        btnAddCar = createStyledButton("+ Add Vehicle", new Color(41, 128, 185));
        btnAddCar.addActionListener(e -> simulation.spawnCar());
        add(btnAddCar);

        // 6. Reset
        btnReset = createStyledButton("↺ Reset", new Color(127, 140, 141));
        btnReset.addActionListener(e -> simulation.resetScenario(42L));
        add(btnReset);

        addSeparator();

        // 7. Benchmark
        btnBenchmark = createStyledButton("⚡ Run Benchmark", new Color(142, 68, 173));
        btnBenchmark.addActionListener(e -> {
            BenchmarkDialog dialog = new BenchmarkDialog(parentFrame);
            dialog.setVisible(true);
        });
        add(btnBenchmark);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(5, 10, 5, 10));
        return btn;
    }
}
