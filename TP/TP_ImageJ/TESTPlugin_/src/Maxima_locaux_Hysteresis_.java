
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.util.ArrayList;
import java.util.List;

public class Maxima_locaux_Hysteresis_ implements PlugInFilter {

    private int seuilBas=30;
    private int seuilHaut=100;

    public int setup(String arg, ImagePlus imp) {
        return PlugInFilter.DOES_8G;
    }

    public void run(ImageProcessor ip) {
    	System.out.println("plap");
        ByteProcessor newbp = hystIter(ip, this.seuilBas, this.seuilHaut);
        System.out.println("plop");
        ImagePlus newImg = new ImagePlus("R�sultat du seuillage par hyst�r�sis", newbp);
        System.out.println("plip");
        newImg.show();
    }
    
    
    public ByteProcessor hystIter(ImageProcessor imNormeG, int seuilBas, int seuilHaut) {
        
    	// On r�cup�re la taille de l'image
    	int width = imNormeG.getWidth();
        int height = imNormeG.getHeight();


        // On va cr�er une liste de pixels contour pour lesquels la valeur 
        // est sup�rieur au seuil haut.
        // cette liste servira � savoir si il y a encore des pixels voisin
        // aux pixels contour compris entre les deux seuil
        ByteProcessor maxLoc = new ByteProcessor(width,height);
        List<int[]> highpixels = new ArrayList<int[]>();

        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {

            	// On r�cup�re le pixel courant de l'image originale
                int g = imNormeG.getPixel(x, y)&0xFF;
                
                // Si le pixel est inf�rieur aux pixels bas on laisse le pixel a 0.
                // Cela permet d'�liminer les pixels non contour (non candidat).
                if (g<seuilBas) continue;

                // Si le pixel est sup�rieur au seuil haut on le garde comme pixel contour.
                if (g>seuilHaut) {
                    maxLoc.set(x,y,255);
                    highpixels.add(new int[]{x,y});
                    continue;
                }

                // On met le pixel dont la valeur est comprise entre le seuil bas
                // et le seuil haut dans la nouvelle image. Se seront les pixels candidats.
                maxLoc.set(x,y,128);
            }
        }

        
        // on sauvegarde les pas pour acc�der aux pixels voisins 
        int[] dx8 = new int[] {-1, 0, 1,-1, 1,-1, 0, 1};
        int[] dy8 = new int[] {-1,-1,-1, 0, 0, 1, 1, 1};
        
        // La nouvelle liste de contour
        List<int[]> newhighpixels = new ArrayList<int[]>();

        // tant qu'il reste des candidats potentiels
        while(!highpixels.isEmpty()) {
        	
        	// on vide la liste de nouveaux pixels contour.
            newhighpixels.clear();
            
            // Pour chaque pixel coutour
            for(int[] pixel : highpixels) {
                int x=pixel[0], y=pixel[1];
                
                // On cherche a savoir lesquels de ses voisins 
                // sont des pixels candidat.
                // Si ce pixel est un pixel candidat alors on l'ajoute 
                // dans les nouveaux pixels contour.
                for(int k=0;k<8;k++) {
                	// On r�cup�re
                    int xk=x+dx8[k], yk=y+dy8[k];
                    
                    // on �vite les effet de bord
                    if (xk<0 || xk>=width) continue;
                    if (yk<0 || yk>=height) continue;
                    
                    // si le pixel est un candidat on le sauvegarde en tant que 
                    // nouveau pixel contour et on met la valeur a l'indice de ce 
                    // pixel � 255
                    if (maxLoc.get(xk, yk)==128) {
                        maxLoc.set(xk, yk, 255);
                        newhighpixels.add(new int[]{xk, yk});
                    }
                }
            }

            // On met dans la nouvelle liste de pixel contour les nouveau pixel pr�c�dement
            // calcul�
            List<int[]> swap = highpixels; 
            highpixels = newhighpixels; 
            newhighpixels = swap;
        }

        // On �limine les pixels isol�s
        for (int y=0; y<height; y++) {
            for (int x=0; x<width; x++) {
                if (maxLoc.get(x, y)!=255) maxLoc.set(x,y,0);
            }
        }
        
        // On retourne l'image
        return maxLoc;
    }
    
}


