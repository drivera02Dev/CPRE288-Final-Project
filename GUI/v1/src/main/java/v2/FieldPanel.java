package v2;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class FieldPanel extends JPanel {
    // === Constants ===
    private static final int GRID_WIDTH = 8;
    private static final int FIELD_WIDTH_CM = 427;
    private static final int FIELD_HEIGHT_CM = 244;
    private static final int GRID_SCALE = 5; // 1 grid square = 5 cm
    private static final int FIELD_WIDTH_GRIDS = FIELD_WIDTH_CM / GRID_SCALE;
    private static final int FIELD_HEIGHT_GRIDS = FIELD_HEIGHT_CM / GRID_SCALE;
    private static final int CYBOT_DIAMETER_CM = 34;
    private static final int CYBOT_RADIUS_GRIDS = (CYBOT_DIAMETER_CM / 2) / GRID_SCALE;

    private static BufferedImage arrowImage;

    // === State ===
    private final List<Point> obstacles = new ArrayList<>();
    private final List<Point> previousPositions = new ArrayList<>();
    private Point cyBotPosition = new Point(50, 30);
    private final List<Shape> impactMarkers = new ArrayList<>();
    private double remainderX = 0;
    private double remainderY = 0;
    private double cyBotDirectionDegrees = 0; // 0 = right

    public FieldPanel() {
        setPreferredSize(new Dimension(FIELD_WIDTH_GRIDS * GRID_WIDTH, FIELD_HEIGHT_GRIDS * GRID_WIDTH));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawGrid(g);
        drawPreviousPositions(g);
        drawCyBot(g);
        drawObstacles(g);
        drawArrow(g);
        drawImpactMarkers(g);
    }

    private void drawGrid(Graphics g) {
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < getWidth(); i += GRID_WIDTH)
            for (int j = 0; j < getHeight(); j += GRID_WIDTH)
                g.drawRect(i, j, GRID_WIDTH, GRID_WIDTH);
    }

    private void drawPreviousPositions(Graphics g) {
        g.setColor(Color.GRAY);
        int counter = 0;
        for (Point p : previousPositions) {
            int px = p.x * GRID_WIDTH;
            int py = p.y * GRID_WIDTH;
            g.fillRect(px, py, GRID_WIDTH, GRID_WIDTH);
            g.setColor(Color.ORANGE);
            g.drawString(String.valueOf(counter++), px + GRID_WIDTH / 4, py + GRID_WIDTH);
            g.setColor(Color.GRAY);
        }
    }

    private void drawCyBot(Graphics g) {
        g.setColor(Color.BLUE);
        int diameter = CYBOT_RADIUS_GRIDS * 2 * GRID_WIDTH;
        int px = cyBotPosition.x * GRID_WIDTH - diameter / 2;
        int py = cyBotPosition.y * GRID_WIDTH - diameter / 2;
        g.fillOval(px, py, diameter, diameter);
    }

    private void drawObstacles(Graphics g) {
        g.setColor(Color.RED);
        for (Point p : obstacles) {
            g.fillRect(p.x * GRID_WIDTH, p.y * GRID_WIDTH, GRID_WIDTH, GRID_WIDTH);
        }
    }

    private void drawArrow(Graphics g) {
        if (arrowImage == null) {
            System.err.println("Arrow image is null");
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        // Position on screen in pixels
        int pixelX = cyBotPosition.x * GRID_WIDTH;
        int pixelY = cyBotPosition.y * GRID_WIDTH;

        // Arrow dimensions
        int imgW = arrowImage.getWidth();
        int imgH = arrowImage.getHeight();

        // Center the arrow on the cyBot's position
        int drawX = pixelX - imgW / 2;
        int drawY = pixelY - imgH / 2;

        // Save transform
        AffineTransform old = g2d.getTransform();

        // Rotate around the center of the image
        g2d.rotate(Math.toRadians(cyBotDirectionDegrees), pixelX, pixelY);

        // Draw centered
        g2d.drawImage(arrowImage, drawX, drawY, null);

        // Restore transform
        g2d.setTransform(old);
    }

    
    private void drawImpactMarkers(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.red); // semi-transparent green

        for (Shape marker : impactMarkers) {
            g2d.fill(marker);
        }
    }

    public void hitBorder() {
        int dx = (int) Math.round(CYBOT_RADIUS_GRIDS * Math.cos(Math.toRadians(cyBotDirectionDegrees)));
        int dy = (int) Math.round(CYBOT_RADIUS_GRIDS * Math.sin(Math.toRadians(cyBotDirectionDegrees)));
        Point hitPoint = new Point(cyBotPosition.x + dx, cyBotPosition.y + dy);
        obstacles.add(hitPoint);

        // Width of rectangle in cm and grids
        double widthCm = 10.0;
        double widthGrids = widthCm / GRID_SCALE;

        // Orthogonal direction (normal vector)
        double angleRad = Math.toRadians(cyBotDirectionDegrees + 90); // +90° = perpendicular
        double nx = Math.cos(angleRad);
        double ny = Math.sin(angleRad);

        // Rectangle center in pixels
        double centerX = hitPoint.x * GRID_WIDTH;
        double centerY = hitPoint.y * GRID_WIDTH;

        // Size in pixels
        double rectWidth = widthGrids * GRID_WIDTH;
        double rectHeight = GRID_WIDTH; // 1 grid tall

        // Top-left corner of rectangle
        double drawX = centerX - (rectWidth / 2) * nx;
        double drawY = centerY - (rectWidth / 2) * ny;

        // Create rotated rectangle
        Rectangle2D rect = new Rectangle2D.Double(drawX, drawY, rectWidth, rectHeight);
        AffineTransform transform = AffineTransform.getRotateInstance(
            Math.toRadians(cyBotDirectionDegrees + 90), centerX, centerY
        );
        Shape rotatedRect = transform.createTransformedShape(rect);
        impactMarkers.add(rotatedRect); // no cast needed


        repaint();
    }


    public void rotateCyBot(double degrees) {
        cyBotDirectionDegrees = degrees;
    }

    public void moveCyBot(double cm) {
        previousPositions.add(new Point(cyBotPosition));

        double grids = cm / GRID_SCALE;
        double dx = grids * Math.cos(Math.toRadians(cyBotDirectionDegrees));
        double dy = grids * Math.sin(Math.toRadians(cyBotDirectionDegrees));

        cyBotPosition.translate((int) dx, (int) dy);
        remainderX += dx - (int) dx;
        remainderY += dy - (int) dy;
        applyRemainders();

        repaint();
    }

    private void applyRemainders() {
        if (Math.abs(remainderX) >= 1) {
            cyBotPosition.x += (int) remainderX;
            remainderX -= (int) remainderX;
        }
        if (Math.abs(remainderY) >= 1) {
            cyBotPosition.y += (int) remainderY;
            remainderY -= (int) remainderY;
        }
    }

    public static void main(String[] args) {
        try {
            arrowImage = ImageIO.read(new File("arrow.png"));
        } catch (Exception ignored) {System.out.println("couldnt find arrow");}

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Field Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FieldPanel field = new FieldPanel();

            field.moveCyBot(60);
            field.rotateCyBot(180);
            field.hitBorder();

            frame.add(field);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}

