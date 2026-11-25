package com.trafficsense.ui;

import com.trafficsense.model.Car;
import com.trafficsense.model.Lane;
import com.trafficsense.model.NavigationClient;
import com.trafficsense.model.Road;
import com.trafficsense.sim.TrafficSimulation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Modern metrics dashboard displaying live simulation telemetry and
 * detailed real-time NavigationClient HUD for the inspected vehicle.
 */
public class MetricsPanel extends JPanel {
    private final TrafficSimulation simulation;

    // Metrics labels
    private final JLabel lblEngine = new JLabel("-");
    private final JLabel lblSimTime = new JLabel("0.0 s");
    private final JLabel lblCarsCount = new JLabel("0 / 25");
    private final JLabel lblStuckCars = new JLabel("0");
    private final JLabel lblAvgSpeed = new JLabel("0.0 km/h");
    private final JLabel lblAvgTripTime = new JLabel("0.0 s");
    private final JLabel lblThroughput = new JLabel("0.0 cars/min");
    private final JLabel lblMostCongested = new JLabel("-");
    private final JProgressBar pbCongestion = new JProgressBar(0, 100);

    // Navigation Client HUD labels
    private final JLabel lblHudCarId = new JLabel("No car selected");
    private final JLabel lblHudStatus = new JLabel("-");
    private final JLabel lblHudSpeed = new JLabel("-");
    private final JLabel lblHudEta = new JLabel("-");
    private final JLabel lblHudNextTurn = new JLabel("-");
    private final JTextArea txtHudRerouteReason = new JTextArea("Click or drag any vehicle to inspect its navigation telemetry.");
    private final JLabel lblHudRoute = new JLabel("-");

    public MetricsPanel(TrafficSimulation simulation) {
        this.simulation = simulation;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(340, 700));
        setBackground(new Color(25, 32, 38));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Section 1: System Telemetry
        contentPanel.add(createHeaderLabel("SYSTEM TELEMETRY"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        gridPanel.setOpaque(false);

        gridPanel.add(createMetricCard("ACTIVE ENGINE", lblEngine, new Color(52, 152, 219)));
        gridPanel.add(createMetricCard("SIMULATION TIME", lblSimTime, new Color(155, 89, 182)));
        gridPanel.add(createMetricCard("TOTAL VEHICLES", lblCarsCount, new Color(46, 204, 113)));
        gridPanel.add(createMetricCard("STUCK VEHICLES", lblStuckCars, new Color(231, 76, 60)));
        gridPanel.add(createMetricCard("AVERAGE SPEED", lblAvgSpeed, new Color(241, 196, 15)));
        gridPanel.add(createMetricCard("AVG TRIP TIME", lblAvgTripTime, new Color(230, 126, 34)));
        gridPanel.add(createMetricCard("THROUGHPUT", lblThroughput, new Color(26, 188, 156)));

        JPanel cardCongestion = createMetricCard("PEAK CONGESTION", lblMostCongested, new Color(255, 107, 129));
        pbCongestion.setPreferredSize(new Dimension(120, 8));
        pbCongestion.setForeground(new Color(231, 76, 60));
        pbCongestion.setBackground(new Color(40, 50, 60));
        pbCongestion.setBorderPainted(false);
        cardCongestion.add(pbCongestion, BorderLayout.SOUTH);
        gridPanel.add(cardCongestion);

        contentPanel.add(gridPanel);
        contentPanel.add(Box.createVerticalStrut(18));

        // Section 2: Vehicle Navigation HUD
        contentPanel.add(createHeaderLabel("NAVIGATION CLIENT HUD"));
        contentPanel.add(Box.createVerticalStrut(8));

        JPanel hudCard = new JPanel();
        hudCard.setLayout(new BoxLayout(hudCard, BoxLayout.Y_AXIS));
        hudCard.setBackground(new Color(33, 43, 52));
        hudCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(50, 65, 78), 1),
                new EmptyBorder(10, 10, 10, 10)
        ));

        hudCard.add(createHudRow("Vehicle ID:", lblHudCarId, true));
        hudCard.add(Box.createVerticalStrut(6));
        hudCard.add(createHudRow("Status:", lblHudStatus, false));
        hudCard.add(Box.createVerticalStrut(6));
        hudCard.add(createHudRow("Current Speed:", lblHudSpeed, false));
        hudCard.add(Box.createVerticalStrut(6));
        hudCard.add(createHudRow("ETA to Dest:", lblHudEta, false));
        hudCard.add(Box.createVerticalStrut(6));
        hudCard.add(createHudRow("Next Turn:", lblHudNextTurn, false));
        hudCard.add(Box.createVerticalStrut(6));
        hudCard.add(createHudRow("Route Path:", lblHudRoute, false));
        hudCard.add(Box.createVerticalStrut(8));

        JLabel lblReasonTitle = new JLabel("Reroute Explanation:");
        lblReasonTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblReasonTitle.setForeground(new Color(180, 195, 210));
        hudCard.add(lblReasonTitle);
        hudCard.add(Box.createVerticalStrut(4));

        txtHudRerouteReason.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtHudRerouteReason.setForeground(new Color(220, 235, 245));
        txtHudRerouteReason.setBackground(new Color(24, 30, 36));
        txtHudRerouteReason.setLineWrap(true);
        txtHudRerouteReason.setWrapStyleWord(true);
        txtHudRerouteReason.setEditable(false);
        txtHudRerouteReason.setBorder(new EmptyBorder(6, 6, 6, 6));
        hudCard.add(txtHudRerouteReason);

        contentPanel.add(hudCard);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

        // Update timer at 10 Hz
        javax.swing.Timer updateTimer = new javax.swing.Timer(100, e -> updateMetrics());
        updateTimer.start();
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(149, 175, 192));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(new Color(33, 43, 52));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, accentColor),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 9));
        titleLbl.setForeground(new Color(150, 165, 180));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueLabel.setForeground(Color.WHITE);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createHudRow(String labelText, JLabel valueLabel, boolean isBold) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(new Color(160, 175, 190));
        lbl.setPreferredSize(new Dimension(85, 16));

        valueLabel.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 11));
        valueLabel.setForeground(isBold ? new Color(241, 196, 15) : Color.WHITE);

        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    public void updateSelectedCar(Car car) {
        if (car == null) {
            lblHudCarId.setText("None (Click a vehicle)");
            lblHudStatus.setText("-");
            lblHudSpeed.setText("-");
            lblHudEta.setText("-");
            lblHudNextTurn.setText("-");
            lblHudRoute.setText("-");
            txtHudRerouteReason.setText("Click or drag any vehicle to inspect its navigation telemetry.");
            return;
        }

        NavigationClient nav = car.getNavigationClient();
        lblHudCarId.setText(car.getId() + " (" + car.getOrigin().getId() + " -> " + car.getDestination().getId() + ")");
        lblHudStatus.setText(car.getStatus().name());
        lblHudSpeed.setText(String.format("%.1f km/h", car.getSpeed() * 3.6));
        lblHudEta.setText(String.format("%.1f s", nav.getETA()));
        lblHudNextTurn.setText(nav.getNextTurn());
        txtHudRerouteReason.setText(nav.getRerouteReason());

        List<Lane> route = car.getRoute();
        if (route != null && !route.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < route.size(); i++) {
                sb.append(route.get(i).getId());
                if (i < route.size() - 1) sb.append(" → ");
            }
            lblHudRoute.setText(sb.toString());
        } else {
            lblHudRoute.setText("No route active");
        }
    }

    private void updateMetrics() {
        if (simulation.getRoutingEngine() != null) {
            lblEngine.setText(simulation.getRoutingEngine().getType().name());
        }
        lblSimTime.setText(String.format("%.1f s", simulation.getSimTime()));
        lblCarsCount.setText(simulation.getCars().size() + " / " + TrafficSimulation.MAX_CARS);

        int stuck = simulation.getStuckCarsCount();
        lblStuckCars.setText(String.valueOf(stuck));
        lblStuckCars.setForeground(stuck > 0 ? new Color(231, 76, 60) : Color.WHITE);

        lblAvgSpeed.setText(String.format("%.1f km/h", simulation.getAverageActiveSpeed()));
        lblAvgTripTime.setText(String.format("%.1f s", simulation.getAverageTripTime()));
        lblThroughput.setText(String.format("%.1f cars/min", simulation.getThroughputCarsPerMinute()));

        Road mostCongested = simulation.getMostCongestedRoad();
        if (mostCongested != null) {
            double ratio = mostCongested.getMaxCongestionRatio();
            int pct = (int) Math.round(ratio * 100.0);
            lblMostCongested.setText(mostCongested.getId() + " (" + pct + "%)");
            pbCongestion.setValue(Math.min(100, pct));
        } else {
            lblMostCongested.setText("None (0%)");
            pbCongestion.setValue(0);
        }
    }
}
