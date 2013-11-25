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
		double min;
		int iMin = 0, jMin = 0;
		
		Matrix dtw = new Matrix(this.referenceStrokeSize_n, this.testStrokeSize_m);
		int[][] neighbours = { {-1,0} , {0,-1} , {-1,-1} };
		
		dtw.items[0][0] = 0;
		dtw.couple[0][0] =  new Couple(0, 0);
		
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
		for(int i=1; i<this.referenceStrokeSize_n; i++)
		{
			for(int j=1; j<this.testStrokeSize_m; j++)
			{
				min = Double.MAX_VALUE;
				
				// get the minimum value
				for(int k=0; k<neighbours.length; k++)
				{
					if(dtw.items[i + neighbours[k][0]][j + neighbours[k][1]] < min)
					{
						iMin = i + neighbours[k][0];
						jMin = j + neighbours[k][1];
						min = dtw.items[i + neighbours[k][0]][j + neighbours[k][1]];
					}
				}
				
				dtw.items[i][j] = min +computeCost(i, j);
				dtw.couple[i][j] = new Couple(iMin, jMin);
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
		
		int x = this.referenceStrokeSize_n-1;
		int y = this.testStrokeSize_m-1;
		
		list.clear();
		
		if ( x != 0 && x != couples.length-1) {
			list.add(new Couple(x,y));
		}
		
		while (!(x == 0 && y == 0)) {

			x = couples[x][y].x;
			y = couples[x][y].y;
				
			// the condition to remove the point that are useless
			// to recognize the shape
			if ( x != 0 && x != couples.length-1) {
				list.add(couples[x][y]);
			}
		}
		
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
