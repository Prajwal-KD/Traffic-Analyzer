package com.trafficsense;

import com.trafficsense.ui.MainFrame;

import javax.swing.*;

/**
 * TrafficSense Application Entry Point.
 */
public class Main {
    public static void main(String[] args) {
        // Modern Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
