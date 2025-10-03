package v3;

import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class MapPanel extends JPanel{

	// === Constants ===
    private static final int GRID_WIDTH = 10;
    private static final int FIELD_WIDTH_CM = 427*2;
    private static final int FIELD_HEIGHT_CM = 427*2;
    private static final int GRID_SCALE = 8; // 1 grid square = 5 cm	
    private static final double CENTIMETERS_PER_PIXEL = (1.0*GRID_SCALE) / (1.0*GRID_WIDTH);
    private static final int FIELD_WIDTH_GRIDS = FIELD_WIDTH_CM / GRID_SCALE;
    private static final int FIELD_HEIGHT_GRIDS = FIELD_HEIGHT_CM / GRID_SCALE;
    private static final int CYBOT_DIAMETER_CM = 34;
    private static final int CYBOT_DIAMETER_PIXELS = (int) (CYBOT_DIAMETER_CM/CENTIMETERS_PER_PIXEL);
    private static final int CYBOT_RADIUS_GRIDS = (CYBOT_DIAMETER_CM / 2) / GRID_SCALE;
    		
    // === Field Variables ===
    private PointDouble cyBotPosition_centimeters;
    private ArrayList<PointDouble> previous_positions_centimeters;
    private int cyBotAngle_degrees;
    private BufferedImage arrow;
    private ArrayList<BumpedObject> bumpedObjects;
    private ArrayList<Calculated_ScannedObject> scannedObjs;
    private ArrayList<BorderPoint> BorderObjs;
   
    
    // === Constructors ===
	public MapPanel() {
		 setPreferredSize(new Dimension(FIELD_WIDTH_GRIDS * GRID_WIDTH, FIELD_HEIGHT_GRIDS * GRID_WIDTH));		
		 cyBotPosition_centimeters = new PointDouble(FIELD_WIDTH_CM / 2, FIELD_HEIGHT_CM / 2);
		 previous_positions_centimeters = new ArrayList<PointDouble>();
		 cyBotAngle_degrees = 0;
		 try {
			 arrow = ImageIO.read(new File("arrow.png"));
		 } catch(Exception ignore) {System.out.println("arrow png not found"); }
		 bumpedObjects = new ArrayList<BumpedObject>(3);
		 scannedObjs = new ArrayList<Calculated_ScannedObject>(30);
		 BorderObjs = new ArrayList<BorderPoint>();

	}
	
	private class PointDouble {
		public double x;
		public double y;
		public PointDouble(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}
	
	private class BumpedObject {
		PointDouble end2;
		double width;
		double angle_degrees;

		public BumpedObject(PointDouble end2, int angle_degrees, double width) {
			this.end2 = end2;
			this.width = width;
			this.angle_degrees = angle_degrees;
		}
	}
	
	private class BorderPoint {
		PointDouble faceOfCybot;
		boolean hitLeft;
		int angleOfCybot;
		
		public BorderPoint(PointDouble p, boolean hitLeft, int angleOfCybot) {
			this.faceOfCybot = p;
			this.hitLeft = hitLeft;
			this.angleOfCybot = angleOfCybot;
		}
	}

	private class Calculated_ScannedObject {
	
		PointDouble cyBotFace_pixels;
		int distanceFromCybot_pixels; //face to edge distance
		int rotation_angle_degrees;
		int width;
		public Calculated_ScannedObject(PointDouble cyBotFace_pixels, int distanceFromCybot_pixels, int rotationAngle_degrees, int width) {
			this.cyBotFace_pixels = cyBotFace_pixels;
			this.distanceFromCybot_pixels = distanceFromCybot_pixels;
			this.rotation_angle_degrees = rotationAngle_degrees;
			this.width = width;
		}
	}

	//called by repaint()
	@Override
	protected void paintComponent(Graphics g) {
		//Draw gridlines 
		for(int i = 0; i < FIELD_WIDTH_GRIDS; i++) {
			g.fillRect(i * GRID_WIDTH, 0, 1, FIELD_HEIGHT_GRIDS * GRID_WIDTH);
		}
		for(int j = 0; j < FIELD_HEIGHT_GRIDS; j++) {
			g.fillRect(0, j * GRID_WIDTH, FIELD_WIDTH_GRIDS*GRID_WIDTH, 1);
		}
		
		//Draw Cybot
		g.setColor(Color.blue);
		int x = centimetersToPixel((int) cyBotPosition_centimeters.x);
		int y = centimetersToPixel((int) cyBotPosition_centimeters.y);
		fillOvalFromCenter(g, x, y, CYBOT_DIAMETER_PIXELS, CYBOT_DIAMETER_PIXELS);
		drawArrow(g);

		//Draw Bumped Objects
		g.setColor(Color.red);
		for(BumpedObject b : bumpedObjects) {
			drawBumpedObject(g, b);
		}
		
		//Draw Scanned Objects 
		g.setColor(Color.yellow);
		for(Calculated_ScannedObject o : scannedObjs) {
			drawScannedObject(g, o);
		}
		
		//Draw Border objcts
		g.setColor(Color.green);
		for(BorderPoint p : BorderObjs) {
			drawBorderPoint(g, p);
		}
	}

	//which border side was hit
	public enum Locations {
		NORTH, SOUTH, EAST, WEST
	}

	private Locations findSideHit(BorderPoint p) {
		Locations location = Locations.NORTH;
		//checks each quadrant and side of cybot hit to determine which border was hit
		if(p.angleOfCybot > 0 && p.angleOfCybot < 90) {
			if(p.hitLeft) {
				location = Locations.EAST;
			}
			else {
				location = Locations.SOUTH;
			}
		}
		else if(p.angleOfCybot > 90 && p.angleOfCybot < 180) {
			if(p.hitLeft) {
				location = Locations.SOUTH;
			}
			else {
				location = Locations.WEST;
			}
		}
		else if(p.angleOfCybot > 180 && p.angleOfCybot < 270) {
			if(p.hitLeft) {
				location = Locations.WEST;
			}
			else {
				location = Locations.NORTH;
			}
		}
		else if(p.angleOfCybot > 270 && p.angleOfCybot < 360) {
			if(p.hitLeft) {
				location = Locations.NORTH;
			}
			else {
				location = Locations.EAST;
			}
		}
		//can p.angle be greater than 360? the world may never know
		return location;
	}

	private void drawBorderPoint(Graphics g, BorderPoint p) {
		Locations location = findSideHit(p);
		//System.out.println(location);
		switch (location) {
			case NORTH:
				g.fillRect((int) (p.faceOfCybot.x-GRID_WIDTH), (int) p.faceOfCybot.y-GRID_WIDTH/2, GRID_WIDTH*2, GRID_WIDTH);
				break;
			case EAST:
				g.fillRect((int) p.faceOfCybot.x, (int) p.faceOfCybot.y - GRID_WIDTH, GRID_WIDTH, GRID_WIDTH*2);
				break;
			case SOUTH:
				g.fillRect((int) (p.faceOfCybot.x-GRID_WIDTH), (int) p.faceOfCybot.y, GRID_WIDTH*2, GRID_WIDTH);
				break;
			case WEST:
				g.fillRect((int) (p.faceOfCybot.x-GRID_WIDTH/2), (int) p.faceOfCybot.y-GRID_WIDTH, GRID_WIDTH, GRID_WIDTH*2);
				break;
		}
		repaint();
	}

	private void drawScannedObject(Graphics g, Calculated_ScannedObject o) {
		Graphics2D g2d = (Graphics2D) g;
		
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(o.rotation_angle_degrees), o.cyBotFace_pixels.x, o.cyBotFace_pixels.y);
		//System.out.println(o.rotation_angle_degrees + " " + o.distanceFromCybot_pixels + " " + o.width + " " + o.cyBotFace_pixels.x + " " + o.cyBotFace_pixels.y);
		g2d.fillOval((int) ( o.cyBotFace_pixels.x  + o.distanceFromCybot_pixels) , (int) (o.cyBotFace_pixels.y - (o.width/2)), o.width, o.width);
		g2d.setTransform(old);

	}

	private void drawBumpedObject(Graphics g, BumpedObject b) {

		Graphics2D g2d = (Graphics2D) g;
		
		AffineTransform old = g2d.getTransform();
		g2d.rotate(Math.toRadians(b.angle_degrees), centimetersToPixel((int)b.end2.x),centimetersToPixel((int) b.end2.y));
		g2d.fillRect(centimetersToPixel((int)b.end2.x), centimetersToPixel((int) b.end2.y),
				centimetersToPixel((int) b.width), centimetersToPixel((int) b.width));
		g2d.setTransform(old);
	}


	//draws arrow ontop of cybot an n degrees (cybot angle)
    private void drawArrow(Graphics g) {
        if (arrow == null) {
            System.err.println("Arrow image is null");
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        int pixelX = (int) ((1.0*cyBotPosition_centimeters.x) / (CENTIMETERS_PER_PIXEL*1.0));
        int pixelY = (int) ((1.0*cyBotPosition_centimeters.y) / (CENTIMETERS_PER_PIXEL*1.0));

        int imgW = arrow.getWidth();
        int imgH = arrow.getHeight();

        int drawX = pixelX - imgW / 2;
        int drawY = pixelY - imgH / 2;

        AffineTransform old = g2d.getTransform();

        g2d.rotate(Math.toRadians(cyBotAngle_degrees), pixelX, pixelY);
        g2d.drawImage(arrow, drawX, drawY, null);
        g2d.setTransform(old);
    }

	private int centimetersToPixel(int centimeters) {
		return (int) (centimeters/CENTIMETERS_PER_PIXEL);
	}

	//theres going to be off by one errors here... whatever
	private void fillOvalFromCenter(Graphics g, int x, int y, int width, int height) {
		g.fillOval(x-width/2, y-height/2, width, height);
	}

	public void cyBot_move(int millimeters) {
		previous_positions_centimeters.add(new PointDouble(cyBotPosition_centimeters.x, cyBotPosition_centimeters.y));
		double centimeters = millimeters/10.0;
		cyBotPosition_centimeters.x += centimeters * Math.cos(Math.toRadians(cyBotAngle_degrees));
		cyBotPosition_centimeters.y += centimeters * Math.sin(Math.toRadians(cyBotAngle_degrees));
		repaint();
	}

	public void cyBot_turn(int degrees) {
		cyBotAngle_degrees += degrees;
		repaint();
	}

	public void cyBot_hitObject(boolean isLeft) {
		if(isLeft) {
			System.out.println("gate4");
			//object is 13 cm. First point is directly ontop the cybot where its facing
			PointDouble p1 = new PointDouble(
					cyBotPosition_centimeters.x + (CYBOT_DIAMETER_CM/2.0)*Math.cos(Math.toRadians(cyBotAngle_degrees)),
					cyBotPosition_centimeters.y + (CYBOT_DIAMETER_CM/2.0)*Math.sin(Math.toRadians(cyBotAngle_degrees)));
			//second point follows orthogonally of the cybot (-90 because left) up to radius + 13 cm
			PointDouble p2 = new PointDouble(
					p1.x + Math.cos(Math.toRadians(cyBotAngle_degrees -90)) * ((CYBOT_DIAMETER_CM/2.0)+13),
					p1.y + Math.sin(Math.toRadians(cyBotAngle_degrees -90)) * ((CYBOT_DIAMETER_CM/2.0)+13)
					);
			BumpedObject b = new BumpedObject(p2,cyBotAngle_degrees , ((CYBOT_DIAMETER_CM/2.0)+13));
			bumpedObjects.add(b);
		}
		else {
			System.out.println("gate4");
			//object is 13 cm. First point is directly ontop the cybot where its facing
			PointDouble p1 = new PointDouble(
					cyBotPosition_centimeters.x + (CYBOT_DIAMETER_CM/2.0)*Math.cos(Math.toRadians(cyBotAngle_degrees)),
					cyBotPosition_centimeters.y + (CYBOT_DIAMETER_CM/2.0)*Math.sin(Math.toRadians(cyBotAngle_degrees)));
			//second point follows orthogonally of the cybot (-90 because left) up to radius + 13 cm
			PointDouble p2 = new PointDouble(
					p1.x + Math.cos(Math.toRadians(cyBotAngle_degrees +90)) * ((CYBOT_DIAMETER_CM/2.0)+13),
					p1.y + Math.sin(Math.toRadians(cyBotAngle_degrees +90)) * ((CYBOT_DIAMETER_CM/2.0)+13)
					);
			BumpedObject b = new BumpedObject(p1,cyBotAngle_degrees , ((CYBOT_DIAMETER_CM/2.0)+13));
			bumpedObjects.add(b);
		}
		repaint();
	}

	public void cyBot_hitBoundary(boolean isLeft) {
		BorderObjs.add(new BorderPoint(new PointDouble(cyBotPosition_centimeters.x/CENTIMETERS_PER_PIXEL,
				cyBotPosition_centimeters.y/CENTIMETERS_PER_PIXEL),
				isLeft, cyBotAngle_degrees));
	}

	public void addObjects(ArrayList<ScannedObject> objects) {
		for(ScannedObject o : objects) {
			int start_angle = cyBotAngle_degrees + 90 - o.startAngle;
			int end_angle = cyBotAngle_degrees + 90 - o.endAngle;
			double mid_angle = ( start_angle + end_angle ) /2;
			double face_of_cybot_x = (cyBotPosition_centimeters.x)/CENTIMETERS_PER_PIXEL + (CYBOT_DIAMETER_PIXELS/2)* Math.cos(Math.toRadians(cyBotAngle_degrees));
			double face_of_cybot_y = (cyBotPosition_centimeters.y)/CENTIMETERS_PER_PIXEL + (CYBOT_DIAMETER_PIXELS/2) * Math.sin(Math.toRadians(cyBotAngle_degrees));
			double distance_to_center_of_object = o.distance + o.width/2;
			
			System.out.println("width:" + o.width + " distance:" + o.distance);
			int object_width_pixels = (int) (o.width /CENTIMETERS_PER_PIXEL);
//			System.out.println("start_angle: " + start_angle + " end angle: " + end_angle + " mid_angle: " + mid_angle + " face-x: " + face_of_cybot_x + 
//					"\n face y: " + face_of_cybot_y + "object center: " + object_center_x + " " + object_center_y );

			scannedObjs.add(new Calculated_ScannedObject(new PointDouble(face_of_cybot_x, face_of_cybot_y), (int) (o.distance/CENTIMETERS_PER_PIXEL), (int) mid_angle,
					object_width_pixels));
		}
		repaint();
	}

}
