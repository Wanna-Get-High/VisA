import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

/**
 * Canvas2D.java
 *
 * @author <a href="mailto:gery.casiez@lifl.fr">Gery Casiez</a>
 * @version
 */


public class Canvas2D extends Canvas implements MouseMotionListener, MouseListener {
	// Stroke to test
	private Vector<PointData> TStroke;
	
	// The points of TStroke resampled
	private Vector<Point> TresampledPoints;
	
	// Hidden Markov Model
	private HMM hmm;
	
	// Holds results to display on the interface
	private Vector<String> console;
	
	Canvas2D () {
		TStroke = new Vector<PointData>();
		TresampledPoints = new Vector<Point>();
		
		addMouseListener(this);
		addMouseMotionListener(this);
		
		setBackground(new Color(255,255,255));
		hmm = new HMM();

		console = new Vector<String>();
	}
	
	
	public void paint (Graphics g){ 
		g.drawString("Drag avec le bouton gauche ou droit de la souris : création d'une courbe de test",10,15); 
		int r = 5;  
		
		// Display the stroke
		if (!TStroke.isEmpty()) {
			g.setColor(Color.black);
			for (int i = 1; i < TStroke.size(); i++) {
				g.drawLine(TStroke.get(i-1).getPoint().x, TStroke.get(i-1).getPoint().y,
						TStroke.get(i).getPoint().x, TStroke.get(i).getPoint().y);
				g.drawArc(TStroke.get(i-1).getPoint().x - r,
						TStroke.get(i-1).getPoint().y - r, 2*r, 2*r, 0, 360);
			}
			g.drawArc(TStroke.get(TStroke.size()-1).getPoint().x - r,
					TStroke.get(TStroke.size()-1).getPoint().y - r, 2*r, 2*r, 0, 360);
		}
		
		// Display the console
		g.setColor(Color.LIGHT_GRAY);
		for (int i = 0; i < console.size(); i++) {
			g.drawString(console.get(i),10,15*(i+2));			
		}
	
		// Display the resampled stroke
		if (!TresampledPoints.isEmpty()) {
			g.setColor(Color.red);
			for (int i = 0; i < TresampledPoints.size(); i++) {
				g.drawArc(TresampledPoints.get(i).x - r, TresampledPoints.get(i).y - r, 2*r, 2*r, 0, 360);
			}
		}		
		
	}

	public void mouseMoved(MouseEvent e) {

	}

	public void mouseDragged(MouseEvent e) {
		TStroke.add(new PointData(e.getPoint(), e.getWhen()));
		repaint();
	}	
	
	public void mouseClicked(MouseEvent e) {
		
	}

	public void mouseEntered(MouseEvent e) {
		
	}

	public void mouseExited(MouseEvent e) {
		
	}

	public void mousePressed(MouseEvent e) {
		TStroke.clear();
		TresampledPoints.clear();
	}

	public void mouseReleased(MouseEvent e) {
		if (!TStroke.isEmpty()) {
			hmm.setRawSourcePoints(TStroke);
			TresampledPoints = hmm.getResampledPoints();
			hmm.recognize();
			console = hmm.getRecognitionInfo();
			//hmm.TestAllExamples();
			repaint();
		}
	}  
}
