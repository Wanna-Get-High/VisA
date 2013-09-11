

import ij.*;
import ij.plugin.filter.*;
import ij.process.*;

public class sample_cfa implements PlugInFilter {

	ImagePlus imp;	// Fen�tre contenant l'image de r�f�rence
	int width;		// Largeur de la fen�tre
	int height;		// Hauteur de la fen�tre

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		// Lecture des dimensions de la fen�tre
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		
		return PlugInFilter.DOES_8G;
	}

	public void run(ImageProcessor ip) {

		// Calcul des �chantillons de chaque composante de l'image CFA
		ImageStack samples_stack = imp.createEmptyStack();
		samples_stack.addSlice("rouge", cfa_samples(ip,0));	// Composante R
		samples_stack.addSlice("vert", cfa_samples(ip,1));// Composante G
		samples_stack.addSlice("bleu", cfa_samples(ip,2));	// Composante B

		// Cr�ation de l'image r�sultat
		ImagePlus cfa_samples_imp = imp.createImagePlus();
		cfa_samples_imp.setStack("�chantillons couleur CFA", samples_stack);
		
		cfa_samples_imp.show();
	}
	
	private ByteProcessor cfa_samples(ImageProcessor ip, int composante) {
		
		ByteProcessor cfa_sample = new ByteProcessor(this.width, this.height);
		
		if (composante == 0) { // rouge
			for (int y=0; y<this.height; y+=2) {
				for (int x=1; x<this.width; x+=2) {
					cfa_sample.set(x, y,ip.getPixel(x,y));
				}
			}
		} else if (composante == 1) { // vert
			for (int y=0; y<this.height; y+=2) {
				for (int x=0; x<this.width; x+=2) {
					cfa_sample.set(x, y,ip.getPixel(x,y));
				}
			}

			for (int y=1; y<this.height; y+=2) {
				for (int x=1; x<this.width; x+=2) {
					cfa_sample.set(x, y,ip.getPixel(x,y));
				}
			}			
		} else { // bleu
			for (int y=1; y<this.height; y+=2) {
				for (int x=0; x<this.width; x+=2) {
					cfa_sample.set(x, y,ip.getPixel(x,y));
				}
			}
		}

		return cfa_sample;
	}
}