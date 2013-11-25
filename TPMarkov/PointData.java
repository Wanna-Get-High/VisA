import java.awt.Point;

/**
 * PointData.java
 *
 * @author <a href="mailto:gery.casiez@lifl.fr">Gery Casiez</a>
 * @version
 */

public class PointData {
	private Point p;
	private long timeStamp;
	private double x;
	private double y;
	
	public PointData(Point p, long timeStamp) {
		this.p = p;
		this.timeStamp = timeStamp;
		x = p.x;
		y = p.y;
	}
	
	public PointData(PointData pt) {
		p = pt.getPoint();
		timeStamp = pt.getTimeStamp();
		x = pt.getX();
		y = pt.getY();
	}
	
	public PointData(double x, double y, long timeStamp) {
		this.x = x;
		this.y = y;
		this.timeStamp = timeStamp;
		p = new Point((int)x, (int)y);
	}
	
	public Point getPoint() {
		return p;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
}
