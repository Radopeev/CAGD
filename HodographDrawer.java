import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

class HodographDrawer extends JPanel {
    private static final int FONT_SIZE = 14;
    private static final double RESIZE_WEIGHT = 0.5;
    private static final int DIVIDER_SIZE = 1;
    private static final int POINT_SIZE_NOT_CLICKED = 3;
    private static final int POINT_SIZE_CLICKED = 4;
    private static final int POINT_WIDTH = 8;
    private static final int POINT_HEIGHT = 8;
    private static final int MAX_DISTANCE = 10;
    private static final float STROKE_WIDTH = 3.0F;
    private static final int ADJUSTING_HODOGRAPH_VECTORS = 297;
    private static final int ADJUSTING_HODOGRAPH_CURVE = 300;
    private static final int FRAME_WIDTH = 800;
    private static final int FRAME_HEIGHT = 400;
    private final List<Point> points;
    private final JPanel derivativePanel;
    private final JPanel curvePanel;

    private void addLabels() {
        curvePanel.setLayout(new BorderLayout());
        derivativePanel.setLayout(new BorderLayout());

        JLabel bezierLabel = new JLabel("Bezier curve", SwingConstants.CENTER);
        JLabel hodographLabel = new JLabel("Hodograph", SwingConstants.CENTER);

        Font labelFont = new Font("Arial", Font.BOLD, FONT_SIZE);
        bezierLabel.setFont(labelFont);
        hodographLabel.setFont(labelFont);

        bezierLabel.setBackground(Color.LIGHT_GRAY);
        bezierLabel.setOpaque(true);
        hodographLabel.setBackground(Color.LIGHT_GRAY);
        hodographLabel.setOpaque(true);

        curvePanel.add(BorderLayout.NORTH, bezierLabel);
        curvePanel.add(new PointsInputPanel());
        derivativePanel.add(BorderLayout.NORTH, hodographLabel);
        derivativePanel.add(new DerivativePanel());
    }

    private void addSplitLine() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, curvePanel, derivativePanel);
        splitPane.setResizeWeight(RESIZE_WEIGHT);
        splitPane.setEnabled(false);
        splitPane.setDividerSize(DIVIDER_SIZE);

        add(BorderLayout.CENTER, splitPane);
    }

    public HodographDrawer() {
        points = new ArrayList<>();
        setLayout(new BorderLayout());

        curvePanel = new JPanel();
        derivativePanel = new JPanel();

        addLabels();
        addSplitLine();
    }

    private void enableAntiAliasing(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        Stroke stroke = new BasicStroke(STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        g2d.setStroke(stroke);
    }

    private class PointsInputPanel extends JPanel {
        private boolean isExistingPointClicked = false;
        private int selectedPointIndex = -1;

        public PointsInputPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        int closestIndex = getClosestPointIndex(e.getPoint());
                        if (closestIndex != -1) {
                            points.remove(closestIndex);
                            repaint();
                            derivativePanel.repaint();
                        }
                    } else if (!isExistingPointClicked) {
                        points.add(e.getPoint());
                        repaint();
                        derivativePanel.repaint();
                    }
                    isExistingPointClicked = false;
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    selectedPointIndex = getClosestPointIndex(e.getPoint());
                    if (selectedPointIndex != -1) {
                        isExistingPointClicked = true;
                    }
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    selectedPointIndex = -1;
                    repaint();
                }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isExistingPointClicked && selectedPointIndex != -1) {
                        points.set(selectedPointIndex, e.getPoint());
                        repaint();
                        derivativePanel.repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            enableAntiAliasing(g2d);

            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                g.setColor(Color.BLACK);
                g.fillOval(p.x - POINT_SIZE_NOT_CLICKED, p.y - POINT_SIZE_NOT_CLICKED, POINT_WIDTH, POINT_HEIGHT);

                if (i == selectedPointIndex) {
                    g.setColor(Color.RED);
                    g.fillOval(p.x - POINT_SIZE_CLICKED, p.y - POINT_SIZE_CLICKED, POINT_WIDTH, POINT_HEIGHT);
                }
            }

            drawBezierCurveVectors(g);
            drawBezierCurve(g);
        }

        private int getClosestPointIndex(Point clickPoint) {
            if (!points.isEmpty()) {
                double minDistance = Double.MAX_VALUE;
                int closestIndex = -1;

                for (int i = 0; i < points.size(); i++) {
                    double distance = points.get(i).distance(clickPoint);
                    if (distance < minDistance && distance < MAX_DISTANCE) {
                        minDistance = distance;
                        closestIndex = i;
                    }
                }

                return closestIndex;
            }
            return -1;
        }
    }

    private class DerivativePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            enableAntiAliasing(g2d);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            List<Point> hodoPoints = calculateDerivative(points);

            drawHodographVectors(g, hodoPoints);
            drawHodographCurve(g, hodoPoints);
        }
    }

    private void drawLine(Graphics2D g2d, List<Point> points) {
        if (points.size() > 1) {

            Point prevPoint = points.getFirst();
            for (int i = 1; i < points.size(); i++) {
                Point currentPoint = points.get(i);
                g2d.drawLine(prevPoint.x, prevPoint.y, currentPoint.x, currentPoint.y);
                prevPoint = currentPoint;
            }
        }
    }

    private void drawHodographLines(Graphics2D g2d, List<Point> hodographPoints) {
        Point prevPoint = hodographPoints.getFirst();
        for (int i = 1; i < hodographPoints.size(); i++) {
            Point currentPoint = hodographPoints.get(i);
            g2d.drawLine(prevPoint.x + ADJUSTING_HODOGRAPH_CURVE, prevPoint.y + ADJUSTING_HODOGRAPH_CURVE,
                currentPoint.x + ADJUSTING_HODOGRAPH_CURVE, currentPoint.y + ADJUSTING_HODOGRAPH_CURVE);
            prevPoint = currentPoint;
        }
    }

    private void drawBezierCurveVectors(Graphics g) {
        if (points.size() < 2) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.PINK);

        drawLine(g2d, points);
    }

    private void drawHodographVectors(Graphics g, List<Point> derivativePoints) {
        if (points.size() < 2) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.WHITE);

        for (Point p : derivativePoints) {
            g2d.fillOval(p.x + ADJUSTING_HODOGRAPH_VECTORS, p.y + ADJUSTING_HODOGRAPH_VECTORS,
                POINT_WIDTH, POINT_HEIGHT);
        }

        g2d.setColor(Color.GREEN);

        if (derivativePoints.size() > 1) {
            drawHodographLines(g2d, derivativePoints);
        }
    }

    private void drawHodographCurve(Graphics g, List<Point> derivativePoints) {
        if (points.size() < 2) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.ORANGE);

        if (derivativePoints.size() > 1) {
            List<Point> hodographPoints = calculateBezierCurve(derivativePoints);

            drawHodographLines(g2d, hodographPoints);
        }
    }

    private void drawBezierCurve(Graphics g) {
        if (points.size() < 2) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);

        List<Point> curvePoints = calculateBezierCurve(points);
        drawLine(g2d, curvePoints);
    }

    private List<Point> calculateDerivative(List<Point> controlPoints) {
        List<Point> derivative = new ArrayList<>();

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            int derivativeX = controlPoints.get(i + 1).x - controlPoints.get(i).x;
            int derivativeY = controlPoints.get(i + 1).y - controlPoints.get(i).y;
            Point derivativePoint = new Point(derivativeX, derivativeY);
            derivative.add(derivativePoint);
        }

        return derivative;
    }

    private List<Point> calculateBezierCurve(List<Point> controlPoints) {
        List<Point> curvePoints = new ArrayList<>();
        final int steps = 10000;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point curvePoint = deCasteljauAlgorithm(controlPoints, t);
            curvePoints.add(curvePoint);
        }

        return curvePoints;
    }

    private Point deCasteljauAlgorithm(List<Point> controlPoints, double t) {
        List<Point> pointsCopy = new ArrayList<>(controlPoints);

        while (pointsCopy.size() > 1) {
            List<Point> newPoints = new ArrayList<>();
            for (int i = 0; i < pointsCopy.size() - 1; i++) {
                int x = (int) ((1 - t) * pointsCopy.get(i).x + t * pointsCopy.get(i + 1).x);
                int y = (int) ((1 - t) * pointsCopy.get(i).y + t * pointsCopy.get(i + 1).y);
                newPoints.add(new Point(x, y));
            }
            pointsCopy = new ArrayList<>(newPoints);
        }

        return pointsCopy.getFirst();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HodographDrawer curveDrawer = new HodographDrawer();
            JFrame frame = new JFrame("Bezier Curve");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(curveDrawer);
            frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}



