
import ij.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.gui.*;

public class compute_cfa implements PlugInFilter {

	ImagePlus imp;	// Fen�tre contenant l'image de r�f�rence
	int width;		// Largeur de la fen�tre
	int height;		// Hauteur de la fen�tre

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		// Lecture des dimensions de la fen�tre
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		
		return PlugInFilter.DOES_RGB;
	}

	public void run(ImageProcessor ip) {

		// Dispositions possibles pour le CFA
		String[] orders = {"R-G-R", "B-G-B", "G-R-G", "G-B-G"};

		// D�finition de l'interface
		GenericDialog dia = new GenericDialog("G�n�ration de l'image CFA...", IJ.getInstance());
		dia.addChoice("D�but de premi�re ligne :", orders, orders[2]);
		dia.showDialog();

		// Lecture de la r�ponse de l'utilisateur
		if (dia.wasCanceled()) return;
		int order = dia.getNextChoiceIndex();

		// G�n�ration de l'image CFA
		ImageProcessor cfaImage = cfa(order);
		imp = new ImagePlus("image cfa: "+ orders[order], cfaImage);
		imp.show();
	}

	private ImageProcessor cfa(int row_order) {	//G�n�re l'image CFA

		// Image couleur de r�f�rence et ses dimensions
		ImageProcessor ip = imp.getProcessor();

		int pixel_value = 0;	// Valeur du pixel source
		ImageProcessor cfa_ip = new ByteProcessor(this.width,this.height);	// Image CFA g�n�r�e
		
		// d�finition des indices permettant de choisir le type de filtre
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
		
		// �chantillons R
		for (int y=red_y_index; y<this.height; y+=2) {
			for (int x=red_x_index; x<this.width; x+=2) {
				pixel_value = ip.getPixel(x,y);
				int red = (int)(pixel_value & 0xff0000)>>16;
				cfa_ip.putPixel(x,y,red);
			}
		}
		
		// �chantillons B
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