package core;
/**
 * @author <a href="mailto:gery.casiez@lifl.fr">Gery Casiez</a>
 */

import javax.swing.*;

import view.Canvas2D;


public class TestGUI {

	public TestGUI() {

		JFrame fen = new JFrame("Université de Lille 1 - M2 IVI - VisA - Dynamic Time Warping - G. Casiez");
		fen.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		fen.setLocationByPlatform(true);
		
		Canvas2D c = new Canvas2D();
		c.setSize(600, 700);
		fen.add(c);
		fen.pack();
		
		fen.setVisible(true);	
	}

	public static void main(String[] args) {
	    //Schedule a job for the event-dispatching thread:
	    //creating and showing this application's GUI.
	    javax.swing.SwingUtilities.invokeLater(new Runnable() {
		    public void run() {
			new TestGUI();
		    }
		});
	}

}
