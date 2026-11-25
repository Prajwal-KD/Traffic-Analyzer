package com.trafficsense.ui;

import com.trafficsense.model.Car;
import com.trafficsense.model.Junction;
import com.trafficsense.model.Lane;
import com.trafficsense.model.Road;
import com.trafficsense.sim.TrafficSimulation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

/**
 * High-performance double-buffered Java2D canvas rendering the traffic network,
 * congestion levels, vehicles, blockages, and supporting full mouse interactivity.
 */
public class TrafficCanvas extends JPanel {
    private final TrafficSimulation simulation;
    private Car selectedCar = null;
    private Car draggedCar = null;
    private Consumer<Car> onCarSelectedListener;

    // Palette
    private static final Color BG_COLOR = new Color(30, 39, 46);
    private static final Color ROAD_BASE = new Color(50, 60, 70);
    private static final Color ROAD_BORDER = new Color(70, 80, 95);
    private static final Color COLOR_LOW = new Color(46, 204, 113);    // < 50%
    private static final Color COLOR_MED = new Color(241, 196, 15);    // 50% - 80%
    private static final Color COLOR_HIGH = new Color(230, 126, 34);   // 80% - 100%
    private static final Color COLOR_CRIT = new Color(231, 76, 60);    // > 100%
    private static final Color COLOR_BLOCKED = new Color(192, 57, 43); // Blocked
    private static final Color SELECTION_RING = new Color(241, 196, 15);
    private static final Color ROUTE_HIGHLIGHT = new Color(243, 156, 18, 160);

    public TrafficCanvas(TrafficSimulation simulation) {
        this.simulation = simulation;
        setBackground(BG_COLOR);
        setDoubleBuffered(true);

        MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        // 60 FPS repaint timer
        javax.swing.Timer timer = new javax.swing.Timer(16, e -> repaint());
        timer.start();
    }

    public void setOnCarSelectedListener(Consumer<Car> listener) {
        this.onCarSelectedListener = listener;
    }

    public Car getSelectedCar() {
        return selectedCar;
    }

    public void setSelectedCar(Car car) {
        this.selectedCar = car;
        if (onCarSelectedListener != null) {
            onCarSelectedListener.accept(car);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Anti-aliasing hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Draw subtle background grid
        drawBackgroundGrid(g2);

        // 2. Draw roads (thick lines, congestion colors, blocked hazard markings)
        for (Road road : simulation.getNetwork().getRoads().values()) {
            drawRoad(g2, road);
        }

        // 3. Highlight selected car's route
        if (selectedCar != null && selectedCar.getRoute() != null) {
            drawSelectedRoute(g2, selectedCar.getRoute());
        }

        // 4. Draw junctions and gate badges
        for (Junction junction : simulation.getNetwork().getJunctions().values()) {
            drawJunction(g2, junction);
        }

        // 5. Draw cars
        for (Car car : simulation.getCars()) {
            if (car != draggedCar) {
                drawCar(g2, car);
            }
        }

        // 6. Draw dragged car on top
        if (draggedCar != null) {
            drawCar(g2, draggedCar);
        }

        // 7. Legend & Quick Hint overlay
        drawOverlayHints(g2);

        g2.dispose();
    }

    private void drawBackgroundGrid(Graphics2D g2) {
        g2.setColor(new Color(38, 48, 56));
        g2.setStroke(new BasicStroke(1.0f));
        int step = 40;
        int w = getWidth();
        int h = getHeight();
        for (int x = 0; x < w; x += step) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += step) {
            g2.drawLine(0, y, w, y);
        }
    }

    private void drawRoad(Graphics2D g2, Road road) {
        Point2D.Double pA = road.getJunctionA().getLocation();
        Point2D.Double pB = road.getJunctionB().getLocation();

        // Outer road border
        g2.setStroke(new BasicStroke(16.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ROAD_BORDER);
        g2.draw(new Line2D.Double(pA, pB));

        // Inner road surface
        g2.setStroke(new BasicStroke(12.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (road.isBlocked()) {
            // Blocked hazard appearance
            g2.setColor(COLOR_BLOCKED);
            g2.draw(new Line2D.Double(pA, pB));
            drawBlockedStripes(g2, pA, pB);
        } else {
            // Congestion colour-coded:
            // green < 50% load, yellow 50-80%, orange 80-100%, red > 100%
            double ratio = road.getMaxCongestionRatio();
            Color roadColor;
            if (ratio < 0.50) {
                roadColor = COLOR_LOW;
            } else if (ratio < 0.80) {
                roadColor = COLOR_MED;
            } else if (ratio <= 1.00) {
                roadColor = COLOR_HIGH;
            } else {
                roadColor = COLOR_CRIT;
            }
            g2.setColor(roadColor);
            g2.draw(new Line2D.Double(pA, pB));

            // Road center dashed separator
            g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{6.0f, 6.0f}, 0.0f));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.draw(new Line2D.Double(pA, pB));
        }

        // Road identifier and load badge
        double midX = (pA.x + pB.x) / 2.0;
        double midY = (pA.y + pB.y) / 2.0;

        // Perpendicular offset for label so it doesn't collide with road line
        double dx = pB.x - pA.x;
        double dy = pB.y - pA.y;
        double len = Math.hypot(dx, dy);
        double offX = -dy / len * 16.0;
        double offY = dx / len * 16.0;

        String labelText = road.getId() + " (" + road.getTotalLoad() + "/" + (road.getCapacity() * 2) + ")";
        if (road.isBlocked()) {
            labelText = "⚠ " + road.getId() + " [CLOSED]";
        }

        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(labelText);
        int textH = fm.getAscent();

        int pillX = (int) (midX + offX - textW / 2.0 - 4);
        int pillY = (int) (midY + offY - textH / 2.0 - 2);

        g2.setColor(new Color(20, 25, 30, 210));
        g2.fillRoundRect(pillX, pillY, textW + 8, textH + 6, 6, 6);
        g2.setColor(road.isBlocked() ? new Color(255, 107, 107) : new Color(220, 230, 240));
        g2.drawString(labelText, (int) (midX + offX - textW / 2.0), (int) (midY + offY + textH / 2.0 - 1));
    }

    private void drawBlockedStripes(Graphics2D g2, Point2D.Double pA, Point2D.Double pB) {
        Stroke origStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(10.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{8.0f, 8.0f}, 0.0f));
        g2.setColor(new Color(20, 20, 20, 220));
        g2.draw(new Line2D.Double(pA, pB));
        g2.setStroke(origStroke);
    }

    private void drawSelectedRoute(Graphics2D g2, List<Lane> route) {
        g2.setStroke(new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ROUTE_HIGHLIGHT);
        for (Lane lane : route) {
            Point2D.Double pFrom = lane.getFromJunction().getLocation();
            Point2D.Double pTo = lane.getToJunction().getLocation();
            g2.draw(new Line2D.Double(pFrom, pTo));
        }
    }

    private void drawJunction(Graphics2D g2, Junction junction) {
        Point2D.Double loc = junction.getLocation();
        double radius = junction.isGate() ? 16.0 : 12.0;

        // Outer glow/shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.fill(new Ellipse2D.Double(loc.x - radius - 2, loc.y - radius - 2, (radius + 2) * 2, (radius + 2) * 2));

        // Circle body
        Color fill;
        Color stroke;
        if (junction.getId().equals("ENTRY")) {
            fill = new Color(46, 204, 113);
            stroke = new Color(39, 174, 96);
        } else if (junction.getId().equals("EXIT")) {
            fill = new Color(155, 89, 182);
            stroke = new Color(142, 68, 173);
        } else {
            fill = new Color(236, 240, 241);
            stroke = new Color(189, 195, 199);
        }

        g2.setColor(fill);
        g2.fill(new Ellipse2D.Double(loc.x - radius, loc.y - radius, radius * 2, radius * 2));
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(stroke);
        g2.draw(new Ellipse2D.Double(loc.x - radius, loc.y - radius, radius * 2, radius * 2));

        // Junction text label
        g2.setFont(new Font("Segoe UI", Font.BOLD, junction.isGate() ? 11 : 10));
        g2.setColor(junction.isGate() ? Color.WHITE : new Color(44, 62, 80));
        FontMetrics fm = g2.getFontMetrics();
        int sw = fm.stringWidth(junction.getId());
        g2.drawString(junction.getId(), (float) (loc.x - sw / 2.0), (float) (loc.y + fm.getAscent() / 2.0 - 2));

        // Sub-label for gates
        if (junction.isGate()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
            g2.setColor(Color.WHITE);
            String sub = junction.getLabel();
            int subW = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, (float) (loc.x - subW / 2.0), (float) (loc.y + radius + 13));
        }
    }

    private void drawCar(Graphics2D g2, Car car) {
        Point2D.Double pos = car.getVisualPosition();
        double heading = car.getVisualHeading();

        AffineTransform old = g2.getTransform();
        g2.translate(pos.x, pos.y);
        g2.rotate(heading);

        // If selected, draw glowing ring
        if (car == selectedCar) {
            g2.setColor(SELECTION_RING);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new Ellipse2D.Double(-12, -12, 24, 24));
        }

        // Draw car body (rounded rectangle vehicle)
        double carLength = 16.0;
        double carWidth = 9.0;
        RoundRectangle2D body = new RoundRectangle2D.Double(-carLength / 2.0, -carWidth / 2.0, carLength, carWidth, 4, 4);

        // Status border highlight
        if (car.getStatus() == Car.Status.STUCK) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new RoundRectangle2D.Double(-carLength / 2.0 - 2, -carWidth / 2.0 - 2, carLength + 4, carWidth + 4, 6, 6));
        }

        g2.setColor(car.getColor());
        g2.fill(body);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(body);

        // Headlights (directional indicators)
        g2.setColor(Color.YELLOW);
        g2.fillRect((int) (carLength / 2.0 - 2), (int) (-carWidth / 2.0 + 1), 2, 2);
        g2.fillRect((int) (carLength / 2.0 - 2), (int) (carWidth / 2.0 - 3), 2, 2);

        // Car ID label
        g2.rotate(-heading); // keep text readable upright
        g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(car.getId());
        g2.setColor(new Color(20, 25, 30, 200));
        g2.fillRect(-tw / 2 - 2, -16, tw + 4, fm.getAscent() + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(car.getId(), -tw / 2, -16 + fm.getAscent());

        g2.setTransform(old);
    }

    private void drawOverlayHints(Graphics2D g2) {
        // Mini legend in top-left
        int boxX = 14;
        int boxY = 14;
        int boxW = 230;
        int boxH = 92;

        g2.setColor(new Color(20, 25, 32, 210));
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 8, 8);
        g2.setColor(new Color(60, 75, 90));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 8, 8);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(new Color(230, 240, 250));
        g2.drawString("TrafficSense Interactive Canvas", boxX + 10, boxY + 18);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        drawLegendItem(g2, boxX + 10, boxY + 34, COLOR_LOW, "< 50% (Fluid)");
        drawLegendItem(g2, boxX + 100, boxY + 34, COLOR_MED, "50-80% (Moderate)");
        drawLegendItem(g2, boxX + 10, boxY + 52, COLOR_HIGH, "80-100% (Dense)");
        drawLegendItem(g2, boxX + 100, boxY + 52, COLOR_CRIT, "> 100% (Congested)");

        g2.setColor(new Color(170, 190, 210));
        g2.drawString("• Click road to toggle closure   • Drag car to reroute", boxX + 10, boxY + 78);
    }

    private void drawLegendItem(Graphics2D g2, int x, int y, Color color, String text) {
        g2.setColor(color);
        g2.fillRect(x, y - 8, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y - 8, 10, 10);
        g2.drawString(text, x + 15, y);
    }

    private class MouseHandler extends MouseAdapter {
        private boolean isDragging = false;

        @Override
        public void mousePressed(MouseEvent e) {
            Point2D pt = e.getPoint();

            // 1. Check if clicked a Car
            Car hitCar = findCarAtPoint(pt, 16.0);
            if (hitCar != null) {
                selectedCar = hitCar;
                draggedCar = hitCar;
                isDragging = true;
                simulation.handleCarDragged(draggedCar, pt);
                if (onCarSelectedListener != null) {
                    onCarSelectedListener.accept(selectedCar);
                }
                repaint();
                return;
            }

            // 2. Check if clicked a Road to toggle blockage
            Road hitRoad = simulation.getNetwork().findRoadAtPoint(pt, 12.0);
            if (hitRoad != null) {
                simulation.toggleRoadBlock(hitRoad);
                repaint();
                return;
            }

            // 3. Deselect if clicked empty space
            selectedCar = null;
            if (onCarSelectedListener != null) {
                onCarSelectedListener.accept(null);
            }
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging && draggedCar != null) {
                simulation.handleCarDragged(draggedCar, e.getPoint());
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isDragging && draggedCar != null) {
                simulation.handleCarDropped(draggedCar, e.getPoint());
                draggedCar = null;
                isDragging = false;
                repaint();
            }
        }

        private Car findCarAtPoint(Point2D pt, double tolerance) {
            for (Car car : simulation.getCars()) {
                Point2D.Double carPos = car.getVisualPosition();
                if (carPos.distance(pt) <= tolerance) {
                    return car;
                }
            }
            return null;
        }
    }
}
