package com.trafficsense.ui;

import com.trafficsense.model.TrafficNetwork;
import com.trafficsense.routing.CoordinatedPredictiveRoutingEngine;
import com.trafficsense.sim.TrafficSimulation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window integrating the interactive Java2D canvas,
 * telemetry metrics dashboard, and control toolbar.
 */
public class MainFrame extends JFrame {
    private final TrafficSimulation simulation;
    private final TrafficCanvas canvas;
    private final MetricsPanel metricsPanel;
    private final ControlToolbar toolbar;

    public MainFrame() {
        super("TrafficSense - Smart Traffic Simulation & Predictive Routing Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 840);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);

        // Initialize simulation with CoordinatedPredictiveRoutingEngine by default
        TrafficNetwork network = new TrafficNetwork();
        this.simulation = new TrafficSimulation(network, new CoordinatedPredictiveRoutingEngine(), 42L);

        // UI Components
        this.canvas = new TrafficCanvas(simulation);
        this.metricsPanel = new MetricsPanel(simulation);
        this.toolbar = new ControlToolbar(simulation, this);

        // Wire canvas car selection to metrics HUD
        this.canvas.setOnCarSelectedListener(metricsPanel::updateSelectedCar);

        // Layout
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(metricsPanel, BorderLayout.EAST);

        // Graceful shutdown on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                simulation.shutdown();
            }
        });

        // Launch simulation loop
        simulation.start();
    }
}
