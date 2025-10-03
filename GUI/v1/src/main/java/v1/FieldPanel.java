package v1;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * FIELD SIZE: 427 x 244 cm => /5 = 86 x 49 grid squares
 * CyBot dimensions: 34cm 
 */
class FieldPanel extends JPanel {
    List<Point> obstacles = new ArrayList<>();
    Point cyBotPosition = new Point(50, 30);
    double positionRemainder_x = 0;
    double positionRemainder_y = 0;
    ArrayList<Point> previousPositions = new ArrayList<Point>();
    double cyBotDirection = 0;	//0 = right
    public static final int GRID_WIDTH = 8; 
    public static final int CYBOT_WIDTH = 34;
    public static BufferedImage arrow;
    
    public FieldPanel() {
    	this.setSize(86*GRID_WIDTH, 49*GRID_WIDTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw grid (optional)
        for (int i = 0; i < getWidth(); i += GRID_WIDTH)
            for (int j = 0; j < getHeight(); j += GRID_WIDTH)
                g.drawRect(i, j, GRID_WIDTH, GRID_WIDTH);

        // Draw previous positions
        g.setColor(Color.gray);
        int counter = 0;
        for(Point p : previousPositions) {
        	g.fillRect(p.x * GRID_WIDTH, p.y * GRID_WIDTH, GRID_WIDTH, GRID_WIDTH);
        	g.setColor(Color.orange);
        	g.drawString(String.valueOf(counter), GRID_WIDTH *p.x + GRID_WIDTH /4, GRID_WIDTH *p.y + GRID_WIDTH);
        	g.setColor(Color.GRAY);
        	counter++;
        }

        // Draw cybot
        g.setColor(Color.BLUE);
        g.fillOval(cyBotPosition.x * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10, cyBotPosition.y * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10, CYBOT_WIDTH * GRID_WIDTH/5, CYBOT_WIDTH * GRID_WIDTH /5);


        // Draw obstacles
        g.setColor(Color.RED);
        for (Point p : obstacles)
            g.fillRect(p.x*GRID_WIDTH, p.y*GRID_WIDTH, GRID_WIDTH, GRID_WIDTH );
        
        // Draw Arrow
        g.setColor(Color.BLUE);
        drawArrow(g);
//        g.setColor(Color.green);
//        g.fillRect(cyBotPosition.x * GRID_WIDTH,cyBotPosition.y * GRID_WIDTH , 20, 20);
    }
    
    private void drawArrow(Graphics g) {
	   Graphics2D g2d = (Graphics2D) g;

		// Rotation center (e.g. image center)
		int centerX = arrow.getWidth()/2 +cyBotPosition.x * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10 + 10;
		int centerY = arrow.getHeight()/2 + cyBotPosition.y * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10 + 10;

		// Save original transform
		AffineTransform old = g2d.getTransform();

		// Rotate around the center of the image
		g2d.rotate(Math.toRadians(cyBotDirection), centerX, centerY);

		// Draw image at robotX, robotY (top-left corner)
		g2d.drawImage(arrow, cyBotPosition.x * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10 + 10, cyBotPosition.y * GRID_WIDTH - CYBOT_WIDTH * GRID_WIDTH/10 + 10, null);

		// Restore original transform
		g2d.setTransform(old);	
    }

    
    public void hitBorder() {
    	Point pointHit = new Point(0,0);
    	pointHit.x = (int) (cyBotPosition.x + CYBOT_WIDTH/10 * Math.cos(cyBotDirection));
    	pointHit.y = (int) (cyBotPosition.y + CYBOT_WIDTH/10 * Math.sin(cyBotDirection));
    	obstacles.add(pointHit);
    }
    
    /**
     * 
     * @param distance_centimeters
     * @param width centimeters
     * @param medial_angle angle of center of object from current cybot angle
     * 		  medial angle [0,180] -> [-90,90]  degrees
     *
     */
    public void addObstacle(double distance_centimeters, double width, double start_angle, double end_angle) {
    	int numGridSquaresSelected = (int) width/5;
    	
    }

    public void rotate_cyBot(double degrees) {
    	this.cyBotDirection = degrees;
    }
    
    /**
     * Checks if remainder has accumulated to <= -1 or >= 1 and moves the cyBOT there to avoid drift
     */
    public void move_remainder() {
    	if(positionRemainder_x >= 1 || positionRemainder_x <= -1) {
    		cyBotPosition.x += (int) positionRemainder_x;
    		positionRemainder_x -= (double) ((int) positionRemainder_x);
    	}
    	if(positionRemainder_y >= 1 || positionRemainder_y <= -1) {
    		cyBotPosition.y += (int) positionRemainder_y;
    		positionRemainder_y -= (double) ((int) positionRemainder_y);
    	}
    }

    /**
     * 1 grid square = 5 centimeters. Remainder get saved and move_remainder is called so no long term drift
     * @param centimeters positive or negative (for backwards) relative to arrow
     */
    public void move_cyBot(double centimeters) {
    	previousPositions.add((Point) cyBotPosition.clone());
    	double delta_x = ((centimeters / 5.0)) * Math.cos(Math.toRadians(cyBotDirection));
    	double delta_y = ((centimeters / 5.0)) * Math.sin(Math.toRadians(cyBotDirection));
    	cyBotPosition.x += (int) delta_x;
    	cyBotPosition.y += (int) delta_y;
    	positionRemainder_x += delta_x - ((int) delta_x); 
    	positionRemainder_y += delta_y - ((int) delta_y);
    	System.out.println(positionRemainder_x + " " + positionRemainder_y);
    	move_remainder();
    	repaint();
    }

    public static void main(String args[]) throws InterruptedException {
    	try {
			arrow = ImageIO.read(new File("arrow.png"));
    	} catch(Exception e) {}

    	JFrame frame = new JFrame("Test");
    	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	frame.setSize(86*2*GRID_WIDTH, 49*2*GRID_WIDTH);
    	frame.setResizable(false);
    	FieldPanel field = new FieldPanel();
    	field.move_cyBot(60);
    	field.rotate_cyBot(180);
    	field.hitBorder();
//    	field.move_cyBot(-30);
//    	field.rotate_cyBot(45);
//    	field.move_cyBot(50);
//    	field.rotate_cyBot(120);
    	//field.move_cyBot(-10);
    	//field.rotate_cyBot(70);
    	//field.move_cyBot(60);
    	field.repaint();
    	frame.add(field);
    	frame.setVisible(true);
    }
}

