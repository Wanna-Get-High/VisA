import ij.*;
import ij.plugin.filter.*;
import ij.process.*;

public class demat_ha implements PlugInFilter {

	ImagePlus imp;	// Fenêtre contenant l'image de référence
	int width;		// Largeur de la fenêtre
	int height;		// Hauteur de la fenêtre

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		
		// Lecture des dimensions de la fenêtre
		this.width = imp.getWidth();
		this.height = imp.getHeight();
		
		return PlugInFilter.DOES_8G;
	}

	public void run(ImageProcessor ip) {
		// Déclaration d'un noyau et d'un objet Convolver pour la convolution
		float[] kernel = {1,2,1 , 2,4,2 , 1,2,1};
		for (int i=0;i<kernel.length;i++) {
		    kernel[i]=kernel[i]/4;
		}
		
		Convolver con = new Convolver();
		con.setNormalize(false);	// SANS normalisation (par défaut, la méthode convolve() normalise)
		// Composante R estimée par interpolation bilinéaire grâce à la convolution
		
		ImageProcessor red = cfa_samples(ip,0);
		con.convolve(red,kernel,3,3);
		
		ImageProcessor blue = cfa_samples(ip,2);
		con.convolve(blue,kernel,3,3);
		
		ImageProcessor green = est_G_hamilton(ip);
		
		ImageStack samples_stack = imp.createEmptyStack();
		samples_stack.addSlice("rouge", red);	// Composante R
		samples_stack.addSlice("vert", green);// Composante G
		samples_stack.addSlice("bleu", blue);	// Composante B

		// Création de l'image résultat
		ImagePlus cfa_samples_imp = imp.createImagePlus();
		cfa_samples_imp.setStack("Échantillons couleur CFA", samples_stack);
		cfa_samples_imp.show();
	}
	
	private ImageProcessor est_G_hamilton(ImageProcessor cfa_ip) {
		ImageProcessor est_ip = cfa_ip.duplicate();
		
		// interpolation pour G-R-G
		
		// interpolation pour le rouge
		for (int y=2; y<height-1; y+=2) {
			for (int x=3; x<width-1; x+=2) {
				
				int G_Min1_0 = 	cfa_ip.getPixel(x-1, y);
				int G_1_0 = 	cfa_ip.getPixel(x+1, y);
				int G_0_Min1 = 	cfa_ip.getPixel(x, y-1);
				int G_0_1 = 	cfa_ip.getPixel(x, y+1);
				
				int R = 		cfa_ip.getPixel(x, y);
				
				int R_Min2_0 = 	cfa_ip.getPixel(x-2, y);
				int R_2_0 = 	cfa_ip.getPixel(x+2, y);
				int R_0_Min2 = 	cfa_ip.getPixel(x, y-2);
				int R_0_2 = 	cfa_ip.getPixel(x, y+2);
				
				int deltaX = Math.abs(G_Min1_0 - G_1_0) + Math.abs(2*R - R_Min2_0 - R_2_0);
				int deltaY = Math.abs(G_0_Min1 - G_0_1) + Math.abs(2*R - R_0_Min2 - R_0_2);
				
				if 		(deltaX < deltaY)	est_ip.putPixel(x,y, (G_Min1_0 + G_1_0)/2 + (2*R - R_Min2_0 - R_2_0)/4 );
				else if (deltaX > deltaY)	est_ip.putPixel(x,y, (G_0_Min1 + G_0_1)/2 + (2*R - R_0_Min2 - R_0_2)/4 );
				else   /*deltaX = deltaY*/	est_ip.putPixel(x,y, (G_Min1_0 + G_1_0 + G_0_Min1 + G_0_1)/4 + 
																	(4*R - R_Min2_0 - R_2_0 - R_0_Min2 - R_0_2)/8 );
			}
		}
		
		// interpolation pour le bleu
		for (int y=3; y<height-1; y+=2) {
			for (int x=2; x<width-1; x+=2) {
				
				int G_Min1_0 = 	cfa_ip.getPixel(x-1, y);
				int G_1_0 = 	cfa_ip.getPixel(x+1, y);
				int G_0_Min1 = 	cfa_ip.getPixel(x, y-1);
				int G_0_1 = 	cfa_ip.getPixel(x, y+1);
				
				int R = 		cfa_ip.getPixel(x, y);
				
				int R_Min2_0 = 	cfa_ip.getPixel(x-2, y);
				int R_2_0 = 	cfa_ip.getPixel(x+2, y);
				int R_0_Min2 = 	cfa_ip.getPixel(x, y-2);
				int R_0_2 = 	cfa_ip.getPixel(x, y+2);
				
				int deltaX = Math.abs(G_Min1_0 - G_1_0) + Math.abs(2*R - R_Min2_0 - R_2_0);
				int deltaY = Math.abs(G_0_Min1 - G_0_1) + Math.abs(2*R - R_0_Min2 - R_0_2);
				
				if 		(deltaX < deltaY)	est_ip.putPixel(x,y, (G_Min1_0 + G_1_0)/2 + (2*R - R_Min2_0 - R_2_0)/4 );
				else if (deltaX > deltaY)	est_ip.putPixel(x,y, (G_0_Min1 + G_0_1)/2 + (2*R - R_0_Min2 - R_0_2)/4 );
				else   /*deltaX = deltaY*/	est_ip.putPixel(x,y, (G_Min1_0 + G_1_0 + G_0_Min1 + G_0_1)/4 + 
																	(4*R - R_Min2_0 - R_2_0 - R_0_Min2 - R_0_2)/8 );
			}
		}
		
	    return (est_ip);
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