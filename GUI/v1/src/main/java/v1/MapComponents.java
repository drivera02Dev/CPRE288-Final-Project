package v1;

import java.awt.Color;
import java.util.ArrayList;

import org.jfree.data.xy.XYSeries;

public class MapComponents {
	
	enum ShapeType {
		BORDER, OBJECT, ARROW, CYBOT,
	}
	
	public class Shape {
		private ShapeType shapeType; 
		private ArrayList<Vector> local_vectors;
		private Vector global_center; //where local (0,0) falls on global graph
		private Color color;
		

		//helper function for predefined shape constructor 
		private void initializeShape(Shape shape, ShapeType shapeType) {
			switch(shapeType) {
				case BORDER:
					for(int y = -5; y < 6; y++) {
						shape.local_vectors.add(new Vector(0, y));
					}
					shape.color = Color.RED;
					break;
				case CYBOT:
					for(int x = -6; x < 7; x++) {
						for(int y = -6; y < 7; y++) {
							if(x*x + y*y <= 6) {
								shape.local_vectors.add(new Vector(x, y));
							}
						}
					}
					shape.color = Color.GREEN;
					break;
			}	
		}

		
		public Shape(ShapeType shapeType, Vector global_center) {
			this.local_vectors = new ArrayList<Vector>(10);
			this.global_center = global_center;
			initializeShape(this, shapeType);
		}
		
		

		/**
		 * @param newGlobalCenter where the center of the shape ends up globally
		 */
		public void moveShape(XYSeries series, Vector newGlobalCenter) {
			undrawShape(series);
			this.global_center = newGlobalCenter;
			drawShape(series);
		}

		private void undrawShape(XYSeries series) {
			for(Vector v : local_vectors) {
				series.remove(v.x + this.global_center.x);
			}
		}

		public void drawShape(XYSeries series) {
			for(Vector v : local_vectors) {
				series.add(v.x + global_center.x, v.y + global_center.y);
			}
		}

	}

}

class Vector {

	public double x;
	public double y;
	
	public Vector(double x, double y) {
		this.x = x;
		this.y = y;
	}

}