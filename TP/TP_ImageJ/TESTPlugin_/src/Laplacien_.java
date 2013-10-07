

import ij.*;	// pour classes ImagePlus et IJ
import ij.gui.*;	// pour classes GenericDialog et DialogListener
import ij.plugin.filter.*;	// pour interface PlugInFilter et Convolver
import ij.process.*;	// pour classe ImageProcessor et sous-classes
import java.awt.*;		//pour classes AWTEvent, CheckBox, TextField
import java.util.Vector;	// pour classe Vector


public class Laplacien_ implements PlugInFilter, DialogListener {
 
	private static int filtre=0;
	private final static String[] FILTRES_LAPLACIENS3x3 = {"Laplacien1", "Laplacien2", "Laplacien3","Laplacien4","Laplacien5"};
	private final static float[][] MASQUES_LAPLACIENS3x3 = {
		{0,1,0, 1,-4,1, 0,1,0},
		{1,0,1, 0,-4,0, 1,0,1},
		{1,1,1, 1,-8,1, 1,1,1},
		{1,2,1, 2,-12,2, 1,2,1},
		{1,4,1, 4,-20,4, 1,4,1}
	};
	private static float sigma=0.0f;
	private static boolean seuillageZeroCross=false;
	private static boolean seuilZeroCrossAuto=true;
	private static float seuilZeroCross=10f;
	
	private ImagePlus imp;

	// ---------------------------------------------------------------------------------
	// Méthodes de l'interface PlugInFilter
	
	// Initialisation du plugin
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return PlugInFilter.DOES_8G;
	}

	// Méthode principale du plugin
	public void run(ImageProcessor ip) {
		
		// Affichage de la fenêtre de configuration
		if (!showDialog()) return;

		// Titre et extension de l'image source
		String titre = imp.getTitle();
		//String extension="";
		int index = titre.lastIndexOf('.');
		
		if (index<=0) index = titre.length();
		
		titre = titre.substring(0,index);
		
		// Calcul et représentation du Laplacien (exercice 1, puis à modifier dans le 3) 
		FloatProcessor fpLaplacian = (FloatProcessor)(ip.duplicate().convertToFloat());
		
		String imageName = "seuillage Laplacien";
		
		Convolver conv = new Convolver();
		
		// Génération d'un masque LoG de taille impaire (exercice 3)
		if (Laplacien_.sigma > 0.0) {
			
			int tailleMasque = (int)Math.ceil(Laplacien_.sigma*6);
			System.out.println(tailleMasque);
			if (tailleMasque%2 ==0) tailleMasque++;
					
			imageName = "seuillage LoG : sigma = "+Laplacien_.sigma+" - taille du masque = "+tailleMasque;
			float[] masqueLog = Laplacien_.masqueLoG(tailleMasque, Laplacien_.sigma);
			
			conv.setNormalize(false);
			conv.convolve(fpLaplacian,masqueLog,tailleMasque,tailleMasque);
			
		} else {
			fpLaplacian.convolve(MASQUES_LAPLACIENS3x3[filtre],3,3);
		}
		
		// Création de la nouvelle image contenant la convolution du Laplacien
		imp = new ImagePlus("convolve", fpLaplacian);
		imp.show();
		
		// Détection et affichage des passages par 0 du Laplacien par seuillage du Laplacien (exercice 2)
		if (Laplacien_.seuillageZeroCross) {
			System.out.println(Laplacien_.seuilZeroCross);
			ByteProcessor laplacienZeros = this.laplacienZero(fpLaplacian, Laplacien_.seuilZeroCross);
			ImagePlus imagePlus = new ImagePlus(imageName, laplacienZeros);
			imagePlus.show();
		}
	}
	
	// ---------------------------------------------------------------------------------
	/**
	 * Méthode d'affichage de la fenêtre de config et de lecture des valeurs saisies, appelée dans run()
	 * 
	 * @return false si la fenêtre a été fermée en cliquant sur Cancel, true sinon (boolean)
	 */ 
    
    public boolean showDialog() {
    	
		// Description de la fenêtre de config
		GenericDialog gd = new GenericDialog("Laplacian parameters");
		gd.addChoice("Laplacian filter type:", FILTRES_LAPLACIENS3x3, FILTRES_LAPLACIENS3x3[filtre]);
		gd.addNumericField("Gaussian filtering scale (0 for none)", sigma, 1);
		gd.addCheckbox("Threshold Laplacian zero-crossings", seuillageZeroCross);
		gd.addNumericField("Threshold value", seuilZeroCross, 0);
		gd.getComponent(gd.getComponentCount()-1).setEnabled(seuillageZeroCross && !seuilZeroCrossAuto); // Désactiver champ ?
		gd.addCheckbox ("Auto threshold", seuilZeroCrossAuto);
		gd.getComponent(gd.getComponentCount()-1).setEnabled(seuillageZeroCross && seuilZeroCrossAuto); // Désactiver case ?
		gd.addDialogListener(this);     		// the DialogItemChanged method will be called on user input
		gd.showDialog();                		// display the dialog; preview runs in the background now
		if (gd.wasCanceled()) return false;

       	// Lecture des valeurs saisies
		filtre = gd.getNextChoiceIndex();
		sigma = (float) gd.getNextNumber();
		seuillageZeroCross = gd.getNextBoolean();
		seuilZeroCross = (float) gd.getNextNumber();
		seuilZeroCrossAuto = gd.getNextBoolean();

        return true;
    }	

    // ---------------------------------------------------------------------------------
	/**
	 * Méthode appelée sur modification (par un événement) de la fenêtre de config
	 * 
	 * @param gd Fenêtre de dialogue (GenericDialog)
	 * @param e Événement à traiter (AWTEvent) 
	 * @return true si la saisie est correcte, false sinon (boolean)
	 */
    
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		
		// Accès aux champs de la fenêtre
		Checkbox zcCheckbox = (Checkbox)gd.getCheckboxes().get(0);
		Checkbox zcAutoCheckbox = (Checkbox)gd.getCheckboxes().get(1);
		Vector numFields = gd.getNumericFields();
		TextField sigmaField = (TextField)numFields.get(1);
		TextField zcThresholdField = (TextField)numFields.get(1);
 		if (e!=null) {	// e==null si clic sur OK
			// Désactivation/activation du champ numérique de seuil, selon seuillage ou pas
	        if (e.getSource() == zcCheckbox) { 
	        	zcThresholdField.setEnabled(zcCheckbox.getState()&&!zcAutoCheckbox.getState());
	        	zcAutoCheckbox.setEnabled(zcCheckbox.getState());
	        }
	        else if (e.getSource() == zcAutoCheckbox) {
	        	zcThresholdField.setEnabled(!zcAutoCheckbox.getState());
	        }
 		}
 		sigma = Float.valueOf(sigmaField.getText());
 		seuilZeroCross = Integer.valueOf(zcThresholdField.getText());
        return (!gd.invalidNumber() && sigma>=0 && seuilZeroCross>=0);
    }
	
	// ---------------------------------------------------------------------------------

	/**
	 * Détection des passages par 0 du Laplacien (algo min<-seuil && max>seuil)
	 * 
	 * @param imLaplacien Image du Laplacien (ImageProcessor 32 bits)
	 * @param seuil Seuil sur les valeurs du Laplacien
	 * @return imZeros Carte des passages par 0 (image binaire)
	 */
	public ByteProcessor laplacienZero(ImageProcessor imLaplacien, Float seuil) {
		
		// Le voisinage du point courant analisé
		int nbVoisin = 3;
		
		// La largeur et la hauteur du Laplacien
		int width = imLaplacien.getWidth();
		int height = imLaplacien.getHeight();
		
		// Les pas du voisinage
		int depart = (nbVoisin/2);
		int arrive = -(nbVoisin/2);
		
		// Image binaire résultat des points contours après seuillage
		ByteProcessor imZeros = new ByteProcessor(width,height);
		
		// On parcourt toute l'image du Laplacien 
		for (int i = depart; i < width+arrive; i++) {
			for (int j = depart; j < height+arrive; j++) {
				
				// on récupère la valeur courante
				// et on affecte le min et le max avec cette valeur 
				// afin de toujours trouver un min et un max qui correspond 
				float curent = imLaplacien.getf(i, j);
				float min = curent;
				float max = curent;
				
				// On regarde le voisinage du point courant afin de selectionner le min et le max
				// qui serviront a savoir si le point courant est un point contour.
				for (int k = depart; k >= arrive; k--) {
					for (int l = depart; l >= arrive; l--) {
						
						float currentNeighbours = imLaplacien.getf(i+k, j+l);
						
						if (currentNeighbours > max) {
							max = currentNeighbours;
						}
						
						if (currentNeighbours < min) {
							min = currentNeighbours;
						}
					}
				}
				
				// le point est il considéré comme point contour ?
				// si oui on le met en blanc 
				// sinon en noir
				if (max > seuil && min < -seuil) {
					imZeros.set(i, j, 255);
				} else {
					imZeros.set(i, j, 0);
				}
			}
		}
		
		return imZeros;
	}

	// ---------------------------------------------------------------------------------
	
    /**
     * Génération d'un masque LoG dans un tableau float[]
     * 
     * @param tailleMasque nombre de lignes et de colonnes du masque LoG (int, impair).
     * @param sigma écart-type de la gaussienne (float, au moins 5*sigma)
     * @return masque de l'opérateur LoG
     */
    public static float[] masqueLoG(int tailleMasque, float sigma)
    {     
		short aperture = (short)(tailleMasque/2);
		double[][] LoG = new double[2*aperture+1][2*aperture+1];
		float[] out=new float[(2*aperture+1)*(2*aperture+1)];
		double sum=0, s2=Math.pow(sigma,2);
		int k=0;

		 // Calcul du masque LoG
		 for(int dy=-aperture;dy<=aperture;dy++)
		 {
			 for(int dx=-aperture;dx<=aperture;dx++)
			 {
				 double r2=-(dx*dx+dy*dy)/2.0/s2;
				 LoG[dy+aperture][dx+aperture]=-1/(Math.PI*s2*s2)*(1+r2)*Math.exp(r2);
				 sum+=LoG[dy+aperture][dx+aperture];
			 }
		 }
		 // Soustraction de la moyenne pour obtenir une somme nulle des coefs
		 sum=sum/(tailleMasque*tailleMasque);
		 for(int dy=-aperture;dy<=aperture;dy++)
			 for(int dx=-aperture;dx<=aperture;dx++)
				 out[k++]= (float)(LoG[dy+aperture][dx+aperture]-sum);
		 
		 return out;
    }

}