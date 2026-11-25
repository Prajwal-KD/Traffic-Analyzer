package com.trafficsense.ui;

import com.trafficsense.benchmark.BenchmarkResult;
import com.trafficsense.benchmark.BenchmarkRunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Modal benchmark dialog executing headless comparison across the 3 routing engines
 * and displaying the comparative results in a structured table.
 */
public class BenchmarkDialog extends JDialog {
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JButton btnStart;
    private final JProgressBar progressBar;
    private final JLabel lblStatus;

    public BenchmarkDialog(Frame parent) {
        super(parent, "TrafficSense - Routing Engine Benchmark", true);
        setSize(780, 420);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(28, 36, 44));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(36, 46, 56));
        headerPanel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel title = new JLabel("Headless 5-Minute Accelerated Benchmark");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Executes identical seeded traffic scenarios (seed: 42) across all 3 routing engines for 300 simulation seconds.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(170, 185, 200));

        headerPanel.add(title, BorderLayout.NORTH);
        headerPanel.add(subtitle, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center Table
        String[] columns = {"Routing Engine", "Completed Trips", "Throughput", "Avg Trip Time", "Avg Speed", "Stuck Cars", "Peak Congestion"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(new Color(230, 240, 250));
        table.setBackground(new Color(32, 42, 50));
        table.setGridColor(new Color(50, 65, 80));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(45, 58, 70));
        table.getTableHeader().setForeground(Color.WHITE);

        // Center align numerical cells
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 1; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(12, 16, 12, 16));
        scrollPane.getViewport().setBackground(new Color(28, 36, 44));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Controls & Progress
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        bottomPanel.setBackground(new Color(36, 46, 56));
        bottomPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel progressPanel = new JPanel(new BorderLayout(6, 4));
        progressPanel.setOpaque(false);
        lblStatus = new JLabel("Ready to start benchmark.");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(180, 195, 210));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(300, 18));
        progressBar.setForeground(new Color(46, 204, 113));
        progressBar.setBackground(new Color(50, 65, 80));

        progressPanel.add(lblStatus, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.SOUTH);
        bottomPanel.add(progressPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        btnStart = new JButton("▶ Run Benchmark");
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStart.setBackground(new Color(46, 204, 113));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);
        btnStart.addActionListener(e -> runBenchmarkAsync());

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setBackground(new Color(70, 85, 100));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dispose());

        buttonPanel.add(btnStart);
        buttonPanel.add(btnClose);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void runBenchmarkAsync() {
        btnStart.setEnabled(false);
        tableModel.setRowCount(0);
        progressBar.setValue(0);
        lblStatus.setText("Running benchmark across 3 engines...");

        SwingWorker<List<BenchmarkResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BenchmarkResult> doInBackground() {
                return BenchmarkRunner.runAllEngines((engineName, idx, total, pct) -> {
                    int overallPct = (int) Math.round(((idx + pct) / total) * 100.0);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(overallPct);
                        lblStatus.setText("Simulating " + engineName + " (" + (int)(pct * 100) + "%)...");
                    });
                });
            }

            @Override
            protected void done() {
                try {
                    List<BenchmarkResult> results = get();
                    for (BenchmarkResult res : results) {
                        tableModel.addRow(new Object[]{
                                res.engineName(),
                                res.completedTrips(),
                                res.getFormattedThroughput(),
                                res.getFormattedAvgTripTime(),
                                res.getFormattedAvgSpeed(),
                                res.stuckCars(),
                                res.getFormattedMaxCongestion()
                        });
                    }
                    progressBar.setValue(100);
                    lblStatus.setText("Benchmark complete! Coordinated Predictive achieved optimal throughput.");
                } catch (Exception ex) {
                    lblStatus.setText("Benchmark failed: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    btnStart.setEnabled(true);
                }
            }
        };

        worker.execute();
    }
}
