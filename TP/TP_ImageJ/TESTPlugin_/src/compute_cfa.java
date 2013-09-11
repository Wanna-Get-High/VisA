
import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;

public class compute_cfa implements PlugInFilter {

	ImagePlus imp;	// Fenêtre contenant l'image de référence
	int width;		// Largeur de la fenêtre
	int height;		// Hauteur de la fenêtre

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		// Lecture des dimensions de la fenêtre
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		
		return PlugInFilter.DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		// Dispositions possibles pour le CFA
		String[] orders = {"R-G-R", "B-G-B", "G-R-G", "G-B-G"};

		// Définition de l'interface
		GenericDialog dia = new GenericDialog("Génération de l'image CFA...", IJ.getInstance());
		dia.addChoice("Début de première ligne :", orders, orders[2]);
		dia.showDialog();

		// Lecture de la réponse de l'utilisateur
		if (dia.wasCanceled()) return;
		int order = dia.getNextChoiceIndex();

		// Génération de l'image CFA
		ImageProcessor cfaImage = cfa(order);
		imp = new ImagePlus("image cfa: "+ orders[order], cfaImage);
		imp.show();
	}

	private ImageProcessor cfa(int row_order) {	//Génère l'image CFA

		// Image couleur de référence et ses dimensions
		ImageProcessor ip = imp.getProcessor();

		int pixel_value = 0;	// Valeur du pixel source
		ImageProcessor cfa_ip = new ByteProcessor(this.width,this.height);	// Image CFA générée
		
		// définition des indices permettant de choisir le type de filtre
		int green_1st_x_index = 0;
		int green_1st_y_index = 0;
		
		int green_2nd_x_index = 0;
		int green_2nd_y_index = 0;
		
		int blue_x_index = 0;
		int blue_y_index = 0;
		
		int red_x_index = 0;
		int red_y_index = 0;
		
		// on affecte aux indices les valeurs correspondant
		if (row_order == 0) { 			// "R-G-R"
			green_1st_y_index = 0;
			green_1st_x_index = 1;

			green_2nd_y_index = 1;
			green_2nd_x_index = 0;
			
			red_y_index = 0;
			red_x_index = 0;
			
			blue_y_index = 1;
			blue_x_index = 1;
		} else if (row_order == 1) { 	// "B-G-B"
			green_1st_y_index = 0;
			green_1st_x_index = 1;

			green_2nd_y_index = 1;
			green_2nd_x_index = 0;
			
			red_y_index = 1;
			red_x_index = 1;
			
			blue_y_index = 0;
			blue_x_index = 0;
		} else if (row_order == 2) { 	// "G-R-G"
			green_1st_y_index = 0;
			green_1st_x_index = 0;

			green_2nd_y_index = 1;
			green_2nd_x_index = 1;
			
			red_y_index = 0;
			red_x_index = 1;
			
			blue_y_index = 1;
			blue_x_index = 0;
		} else { 						// "G-B-G"
			green_1st_y_index = 0;
			green_1st_x_index = 0;

			green_2nd_y_index = 1;
			green_2nd_x_index = 1;
			
			red_y_index = 1;
			red_x_index = 0;
			
			blue_y_index = 0;
			blue_x_index = 1;
		}
		
		
		for (int y=green_1st_y_index; y<this.height; y+=2) {
			for (int x=green_1st_x_index; x<this.width; x+=2) {
				pixel_value = ip.getPixel(x,y);
				int green = (int)(pixel_value & 0x00ff00)>>8;
				cfa_ip.putPixel(x,y,green);
			}
		}

		for (int y=green_2nd_y_index; y<this.height; y+=2) {
			for (int x=green_2nd_x_index; x<this.width; x+=2) {
				pixel_value = ip.getPixel(x,y);
				int green = (int)(pixel_value & 0x00ff00)>>8;
				cfa_ip.putPixel(x,y,green);
			}
		}
		
		// Échantillons R
		for (int y=red_y_index; y<this.height; y+=2) {
			for (int x=red_x_index; x<this.width; x+=2) {
				pixel_value = ip.getPixel(x,y);
				int red = (int)(pixel_value & 0xff0000)>>16;
				cfa_ip.putPixel(x,y,red);
			}
		}
		
		// Échantillons B
		for (int y=blue_y_index; y<this.height; y+=2) {
			for (int x=blue_x_index; x<this.width; x+=2) {
				pixel_value = ip.getPixel(x,y);
				int blue = (int)(pixel_value & 0x0000ff);
				cfa_ip.putPixel(x,y,blue);
			}
		}

		return cfa_ip;
	}



}