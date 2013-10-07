package core;

import java.awt.Point;
import java.util.Vector;

import util.Couple;
import util.Matrix;

public class DTW {

	protected Vector<Couple> list;

	protected Vector<Point> referenceStroke;
	protected Vector<Point> testStroke;
	
	protected int referenceStrokeSize_n;
	protected int testStrokeSize_m;
	
	public DTW(Vector<Point> rStroke, Vector<Point> lStroke) {
		this.testStroke = lStroke;
		this.referenceStroke = rStroke;
		
		this.referenceStrokeSize_n = rStroke.size();
		this.testStrokeSize_m = lStroke.size();
		
		list = new Vector<Couple>();
	}
	
	
	/**
	 * compute the D Matrix 
	 * @return the D Matrix
	 */
	public void ComputeDMatrix() {
		Matrix dtw = new Matrix(this.referenceStrokeSize_n, this.testStrokeSize_m);
		
		dtw.items[0][0] = 0;
		dtw.couple[0][0] = new Couple();
		
		// initialization
		for (int i = 1; i < this.referenceStrokeSize_n; i++) {
			dtw.items[i][0] = dtw.items[i-1][0] + this.computeCost(i, 0);
			dtw.couple[i][0] = new Couple(i-1, 0);
		}
		
		for (int i = 1; i < this.testStrokeSize_m; i++) {
			dtw.items[0][i] = dtw.items[0][i-1] + this.computeCost(0, i);
			dtw.couple[0][i] = new Couple(0, i-1);
		}
		
		
		// searching for the minimum neighbor
		for (int i = 1; i < this.referenceStrokeSize_n; i++) {
			for (int j = 1; j < this.testStrokeSize_m; j++) {
				dtw.items[i][j] = this.computeCost(i, j) + this.getMinimumOfNeighbor(dtw, i,j);
			}
		}
		
		this.computeListOfPoints(dtw.couple);
	}
	
	
	/**
	 * Compute the corresponding points depending on the matrix of couples
	 * 
	 * @param couples the matrix that will be scanned.
	 */
	private void computeListOfPoints(Couple[][] couples) {
		
		int x = couples.length-1;
		int y = couples[0].length-1;
		
		int predX = -1;
		int predY = -1;
		
		list.add(new Couple(x,y));
		
		while (!(x == 0 && y == 0)) {

			x = couples[x][y].x;
			y = couples[x][y].y;
			
			// the condition to remove the point that are useless
			// to recognize the shape
			if ( predX != x && predY != y) {
				list.add(couples[x][y]);
			}
			
			predX = x;
			predY = y;
			
		}
		
	}
	
	/**
	 * Retrieve the minimum value of the neighbor ([i,j-1] [i-1,j] [i-1,j-1]) 
	 * and store in Couple[][] of Matrix at [i,j] the index of the minimum value.
	 * 
	 * @param dtw the Matrix that is build
	 * @param i the current reference index 
	 * @param j the current test index
	 * 
	 * @return the minimum value of the neighbor
	 */
	private double getMinimumOfNeighbor(Matrix dtw, int i, int j) {
		
		if (dtw.items[i-1][j] < dtw.items[i][j-1]) {
			if (dtw.items[i-1][j] < dtw.items[i-1][j-1]) {
				return couple(dtw, i, j, i-1, j);
			}
		} else {
			if (dtw.items[i][j-1] < dtw.items[i-1][j-1]) {
				return couple(dtw, i, j, i, j-1);
			}
		}
		
		return couple(dtw, i, j, i-1, j-1);
	}
	
	/**
	 * Retrieve the minimum founded value (see getMinimumOfNeighbor) 
	 * and store the indexes of that value in the Couple[][] matrix. 
	 * 
	 * @param dtw the Matrix that is builded -> needed to acces to Couple
	 * @param x1 the current reference index 
	 * @param y1 the current test index
	 * @param x2 the minimum neighbor reference index 
	 * @param y2 the minimum neighbor test index 
	 * @return
	 */
	private double couple(Matrix dtw, int x1, int y1, int x2, int y2) {
		dtw.couple[x1][y1] = new Couple(x2, y2);
		return dtw.items[x1][y1];
		
	}

	/**
	 * compute the cost between 2 points of the Vectors
	 * 
	 * @param i the index of the referenceStroke Vector
	 * @param j the index of the testStroke Vector
	 * 
	 * @return a double value representing the cost
	 */
	private double computeCost(int i, int j) {
		return this.distance(this.referenceStroke.get(i), this.testStroke.get(j));
	}
	
	/**
	 * Compute euclidian distance between the two points p1 and p2. 
	 * 
	 * @param p1 the first point.
	 * @param p2 the second point.
	 * 
	 * @return a double value representing the euclidian distance.
	 */
	private double distance(Point p1, Point p2) {
		return p1.distance(p2);
	}
	
	public Vector<Couple> getList() { return this.list; }
}
