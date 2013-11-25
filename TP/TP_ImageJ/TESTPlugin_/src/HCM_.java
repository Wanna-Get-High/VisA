import ij.*;
import ij.process.*;
import ij.gui.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.plugin.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import javax.swing.*;

public class HCM_ implements PlugIn {


	public Color pickColor;

	public Semaphore sema;

	
	/**  The main method called when plugin is used */
	public void run(String arg) {

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////
		////                    ASKING DATA TO THE USER                 ////
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		// the semaphore and canvas for the manual selection (by clicking on the colors)
		this.sema = new Semaphore(0);
		ImageCanvas win = WindowManager.getCurrentWindow().getCanvas();


		// Currently opened image
		ImagePlus currentImage = WindowManager.getCurrentImage();
		final ImageProcessor currentImage_imgProc = currentImage.getProcessor();

		// Size of the images : witdth, height, nbPixels
		int currentImage_width = currentImage_imgProc.getWidth();
		int currentImage_height = currentImage_imgProc.getHeight();
		int currentImage_nbPixels = currentImage_width * currentImage_height;

		// The segmented image -> after the clustering
		ImagePlus imageSegmenteFCM = NewImage.createImage("Image segmentée par HCM",currentImage_width,currentImage_height,1,24,0);
		imageSegmenteFCM.show();

		// the mouse listener used to be able to retrieve the position of the click of the user  
		win.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent arg0) {
			}
			public void mousePressed(MouseEvent arg0) {
			}
			public void mouseExited(MouseEvent arg0) {
			}
			public void mouseEntered(MouseEvent arg0) {
			}
			public void mouseClicked(MouseEvent arg0) {
				int[] colorarray2 = new int[3];
				currentImage_imgProc.getPixel(arg0.getX(), arg0.getY(), colorarray2);
				pickColor=new Color(colorarray2[0], colorarray2[1], colorarray2[2]);
				sema.release(1);
			}
		});
		
		
		String demande = JOptionPane.showInputDialog("Pour les cas 1 et 2 les cliques doivent s'effectuer avec l'outil \"Main\"\n" +
				"\n0 = Tout auto                (rien à rentrer)," +
				"\n1 = supervise                (cliquer pour choisir les centroids de départ + tout à rentrer)," +
				"\n2 = supervise auto        (cliquer pour choisir les centroids de départ + rien à rentrer)," +
				"\n3 = non supervise         (centroids choisie aleatoirement + tout à rentrer)," +
				"\n4 = non supervise auto (centroids choisie aleatoirement + rien à rentrer)");
		
		if (demande == null) return;
		
		int demandeInt  = Integer.parseInt(demande);

		int nbClasses;
		double indiceDeFlou_m;
		int nbIterationMaximum;
		double seuilDeStabilite;
		int bool;

		// if the user want the data to be filled automatically
		if (demandeInt == 0 || demandeInt == 2 || demandeInt == 4) {
			nbClasses = 6;
			indiceDeFlou_m = 1;
			nbIterationMaximum = 30;
			seuilDeStabilite = 0.2;
			bool = 1;

		} 
		// else we ask for each field used for the PCM method
		else {
			// Ask for the number of classes (cluster) wanted
			demande = JOptionPane.showInputDialog("Nombre de classes : ");
			nbClasses = Integer.parseInt(demande);

			// Ask for the fuzzy index (2 is a good choice)
			demande = JOptionPane.showInputDialog("Valeur de m (indice de flou) : ");
			indiceDeFlou_m = Double.parseDouble(demande);

			// Ask for the number of maximum iteration
			demande = JOptionPane.showInputDialog("Nombre d'iteration max : ");
			nbIterationMaximum =Integer.parseInt(demande);

			// Ask for the stability threshold
			demande = JOptionPane.showInputDialog("Valeur du seuil de stabilite : ");
			seuilDeStabilite = Double.parseDouble(demande);

			// Ask if the user want a better randomization 
			demande = JOptionPane.showInputDialog("Randomisation amelioree ? (0 = non ou 1 = oui)");
			bool = Integer.parseInt(demande);		
		}

		// the boolean that tells if the user ask for a better randomization  
		boolean randomisationAmelioree = bool==1;
		// the centroids
		double centroids[][] = new double[nbClasses][3];
		// the distances between the data and the centroids
		double distancesDataCentroids[][] = new double[nbClasses][currentImage_nbPixels];
		// the repartition matrix
		double Umat[][] = new double[nbClasses][currentImage_nbPixels];
		// the color component
		double red[] = new double[currentImage_nbPixels];
		double green[] = new double[currentImage_nbPixels];
		double blue[] = new double[currentImage_nbPixels];

		// initialize the performance indexes
		double performanceIndexes[] = new double[nbIterationMaximum];

		// Récupération des données images 
		int l = 0;
		for(int i = 0; i < currentImage_width; i++)
		{
			for(int j = 0; j < currentImage_height; j++)
			{
				int[] colorarray = new int[3];
				currentImage_imgProc.getPixel(i,j,colorarray);
				red[l] = (double)colorarray[0];
				green[l] = (double) colorarray[1];
				blue[l] = (double)colorarray[2];
				l++;
			}
		}

////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
////                          HCM                               ////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////

		// manual initialization of the centroids (by clicking on the colors)
		if (demandeInt == 1 || demandeInt == 2) {
			centroids = this.computeManualCentroids(nbClasses);
		} 
		// Initialization of the centroids (randomly)		
		else {
			centroids = this.computeRandomCentroids(currentImage_imgProc, currentImage_width, currentImage_height, nbClasses, randomisationAmelioree);
		}
		
		// computing the distances between the data and the centroids
		distancesDataCentroids = this.computeDistancesDataCentroids(red, green, blue, centroids, currentImage_nbPixels, nbClasses);

		// Initialization of the membership degree
		Umat = this.computeMembershipDegree(distancesDataCentroids, nbClasses, currentImage_nbPixels, indiceDeFlou_m);

		/////////////////////////////////////////////////////////////
		//////                 MAIN LOOP                    /////////
		/////////////////////////////////////////////////////////////
		int iter = 0;
		double stab = Double.MAX_VALUE;

		while ((iter < nbIterationMaximum) && (stab > seuilDeStabilite)) 
		{
			// Update the matrix of centroids
			centroids = this.updateCentroids(red, green, blue, Umat, indiceDeFlou_m, currentImage_nbPixels, nbClasses);

			// Compute distancesDataCentroids, the matrix of distances (euclidian) with the centroids
			distancesDataCentroids = this.computeDistancesDataCentroids(red, green, blue, centroids, currentImage_nbPixels, nbClasses);
			
			// recompute the membership degree
			Umat = this.computeMembershipDegree(distancesDataCentroids, nbClasses, currentImage_nbPixels, indiceDeFlou_m);
						
			// Calculate difference between the previous partition and the new partition (performance index)
			performanceIndexes[iter] = this.computePerformanceIndex(currentImage_nbPixels, nbClasses, indiceDeFlou_m, Umat, distancesDataCentroids);

			// just verify if there is at least 2 value for the comparison
			if (iter > 0) 
				stab = Math.abs(performanceIndexes[iter] - performanceIndexes[iter-1]);
			
			iter++;

			// draw the segmented image
			this.drawSegmentedImage(imageSegmenteFCM, Umat, centroids, nbClasses, currentImage_width, currentImage_height);
		}

		// draw the performance plot
		this.drawPlot(performanceIndexes, nbIterationMaximum);
	}
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
////						  HCM                               ////
////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////
	
	/**
	 * Compute the centroids by asking the user to click on the image.
	 * 
	 * @param nbClasses the number of asked class
	 * 
	 * @return the centroids matrix.
	 */
	private double[][] computeManualCentroids(int nbClasses) {

		double[][] centroids = new double[nbClasses][3];

		for(int pick=0;pick<nbClasses;pick++){

			Color color = null;

			while (color == null) {
				
				try {
					sema.acquire();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				ColorChooser cc = new ColorChooser("Classe n°"+pick, pickColor, false);
				color = cc.getColor();
			}

			centroids[pick][0]=color.getRed();
			centroids[pick][1]=color.getGreen();
			centroids[pick][2]=color.getBlue();
		}

		return centroids;
	}

	/**
	 * Compute random centroids.
	 * 
	 * @param currentImage_imgProc the image data
	 * @param currentImage_width the width of this image
	 * @param currentImage_height the height of this image
	 * @param nbClasses the number of class (cluster)
	 * @param randomisationAmelioree tell if the user wanted an improved randomization
	 * 
	 * @return the computed centroids 
	 */
	private double[][] computeRandomCentroids( ImageProcessor currentImage_imgProc, int currentImage_width,
			int currentImage_height, int nbClasses, boolean randomisationAmelioree) {

		int epsilonx,epsilony;
		int[] init = new int[3];
		double[][] centroids = new double[nbClasses][3];

		for(int i=0; i<nbClasses; i++)
		{
			if(randomisationAmelioree) 
			{  
				epsilonx=rand((int)(currentImage_width/(i+2)),(int)(currentImage_width/2));
				epsilony=rand((int)(currentImage_height/(4)),(int)(currentImage_height/2));
			}
			else
			{
				epsilonx=0;
				epsilony=0;
			}

			int rx = rand(0+epsilonx, currentImage_width-epsilonx);
			int ry = rand(0+epsilony, currentImage_height-epsilony);

			currentImage_imgProc.getPixel(rx,ry,init);

			centroids[i][0] = init[0]; 
			centroids[i][1] =init[1];
			centroids[i][2] = init[2];
		}

		return centroids;
	}


	/**
	 * Update the position of the centroids
	 * 
	 * @param red the red component
	 * @param green the green component
	 * @param blue the blue component
	 * @param Umat the membership degree matrix
	 * @param indiceDeFlou_m the fuzzy index
	 * @param currentImage_nbPixels the number of pixels of this image
	 * @param nbClasses the number of class (cluster)
	 * 
	 * @return the new centroids places matrix
	 */
	private double[][] updateCentroids(double[] red, double[] green, double[] blue, double[][] Umat, 
										double indiceDeFlou_m, int currentImage_nbPixels, int nbClasses) {

		double [][] centroids = new double[nbClasses][3];

		for(int i=0; i<nbClasses; i++) {

			double composanteRouge = 0;
			double composanteVert = 0;
			double composanteBleu = 0;
			double somme = 0;

			for (int j = 0; j < currentImage_nbPixels; j++) {
				somme += Math.pow(Umat[i][j],indiceDeFlou_m);
				composanteRouge += Math.pow(Umat[i][j],indiceDeFlou_m)*red[j];
				composanteVert += Math.pow(Umat[i][j],indiceDeFlou_m)*green[j];
				composanteBleu += Math.pow(Umat[i][j],indiceDeFlou_m)*blue[j];
			}

			if (somme > 0 ) {
				centroids[i][0] = composanteRouge / somme;
				centroids[i][1] = composanteVert / somme;
				centroids[i][2] = composanteBleu / somme;
			}
		}

		return centroids;
	}

	/**
	 * Compute the distances matrix. 
	 * 
	 * @param red the red component
	 * @param green the green component
	 * @param blue the blue component
	 * @param centroids the matrix containing the position of each centroids 
	 * @param currentImage_nbPixels the number of pixels in this image
	 * @param nbClasses the number of classes
	 * 
	 * @return the distances matrix.
	 */
	private double[][] computeDistancesDataCentroids(double[] red, double[] green, double[] blue, double[][] centroids, 
														int currentImage_nbPixels, int nbClasses) {

		double[][] distancesDataCentroids = new double[nbClasses][currentImage_nbPixels];

		for(int l = 0; l < currentImage_nbPixels; l++)
		{
			for(int k = 0; k < nbClasses; k++)
			{
				double r2 = Math.pow(red[l] - centroids[k][0], 2);
				double g2 = Math.pow(green[l] - centroids[k][1], 2);
				double b2 = Math.pow(blue[l] - centroids[k][2], 2);
				distancesDataCentroids[k][l] = r2 + g2 + b2;
			}
		}

		return distancesDataCentroids;
	}

	/**
	 * Return an array containing the membership degree for each pixels
	 * 
	 * @param distancesDataCentroids the matrix of distance between the centroids and each pixels
	 * @param nbClasses the number of classes
	 * @param currentImage_nbPixels the number of pixels
	 * @param indiceDeFlou_m the fuzzy index
	 * 
	 * @return Umat
	 */
	private double[][] computeMembershipDegree( double[][] distancesDataCentroids, int nbClasses,
												int currentImage_nbPixels, double indiceDeFlou_m) {

		double[][] Umat = new double[nbClasses][currentImage_nbPixels];
		
		for (int j = 0; j < currentImage_nbPixels; j++) {

			double appartenanceMin = Double.MAX_VALUE;
			int indiceAppartenanceMin = 0; 
			
			// on recherche le minimum
			for (int i = 0; i < nbClasses; i++) {

				// on met 0 partout
				Umat[i][j] = 0;
				
				
				if (distancesDataCentroids[i][j] < appartenanceMin) {
					appartenanceMin = distancesDataCentroids[i][j];
					indiceAppartenanceMin = i;
				}
			}
			
			// on met 1 au minimum
			Umat[indiceAppartenanceMin][j] = 1;

		}

		return Umat;
	}

	/**
	 * Compute the performance index at each iteration of the PCM method.
	 * 
	 * @param currentImage_nbPixels the number of pixels of the curent loaded  image
	 * @param nbClasses the number of cluster
	 * @param indiceDeFlou_m the fuzzy index for the calculation
	 * @param n the n matrix
	 * @param Umat the U matrix
	 * @param distancesDataCentroids the matrix of distances
	 * 
	 * @return the performance index
	 */
	private double computePerformanceIndex(	int currentImage_nbPixels, int nbClasses, double indiceDeFlou_m, 
											double[][] Umat, double[][] distancesDataCentroids) {
		
		double performance = 0;
		
		for (int i = 0; i < nbClasses; i++) {

			for (int j = 0; j < currentImage_nbPixels; j++) {

				performance += Math.pow(Umat[i][j],indiceDeFlou_m) * distancesDataCentroids[i][j] ;

			}
		}

		return performance;
	}
	
	/**
	 * Returns the index of the maximum value in the array
	 * 
	 * @param array the array that will be visited
	 * 
	 * @return the index of the maximum value
	 */
	public int indexOfMaxValueIn(double[] array) 
	{
		double max = 0;
		int indice = 0;

		for (int i=0; i<array.length; i++) {
			if (array[i]>max) {
				max=array[i];
				indice=i;
			}
		}

		return indice;
	}


	/**
	 * Return a random number between min and max
	 * 
	 * @param min the minimum range value 
	 * @param max the maximum range value
	 * 
	 * @return a random integer between min and max
	 */
	public int rand(int min, int max) { return min + (int)(new Random().nextDouble()*(max-min)); }
	
	
	/**
	 * Draw a plot of the performance index.
	 * 
	 * @param performanceIndexes the performance recorded within the method
	 * @param nbIterationMaximum the number of maximum iteration
	 */	
	private void drawPlot(double[] performanceIndexes, int nbIterationMaximum) {
		
		double[] xplot = new double[nbIterationMaximum];
		double[] yplot = new double[nbIterationMaximum];
		for(int i = 0; i < nbIterationMaximum; i++)
		{
			xplot[i]=(double)i;
			yplot[i]=(double) performanceIndexes[i];
		}
		Plot plot = new Plot("Performance Index (HCM)","iterations","J(P) value",xplot,yplot);
		plot.setLineWidth(2);
		plot.setColor(Color.blue);
		plot.show();	
	}

	/**
	 * Draw the segmented image.
	 * 
	 * @param imageSegmenteFCM	the image 
	 * @param Umat the U matrix
	 * @param centroids the position of the centroids
	 * @param nbClasses the number of classes
	 * @param currentImage_width the width of the image
	 * @param currentImage_height the height of the image
	 */
	private void drawSegmentedImage(ImagePlus imageSegmenteFCM, double[][] Umat, 
									double[][] centroids, int nbClasses, int currentImage_width, int currentImage_height) {
		
		
		ImageProcessor imageSegmenteFCM_imgProc = imageSegmenteFCM.getProcessor();
		
		double[] mat_array=new double[nbClasses];
		int index = 0;
		
		for(int i=0; i<currentImage_width; i++)
		{
			for(int j = 0; j<currentImage_height; j++)
			{
				for(int k = 0; k<nbClasses; k++)
				{ 
					mat_array[k]=Umat[k][index];
				}

				int maxIndex = this.indexOfMaxValueIn(mat_array);

				int array[] = new int[3];
				array[0] = (int)centroids[maxIndex][0];
				array[1] = (int)centroids[maxIndex][1];
				array[2] = (int)centroids[maxIndex][2];
				
				imageSegmenteFCM_imgProc.putPixel(i, j, array);
				index++;
			}
		}
		
		imageSegmenteFCM.updateAndDraw();
	}
	
}