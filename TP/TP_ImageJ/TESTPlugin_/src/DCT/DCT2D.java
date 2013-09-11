package DCT;


import java.awt.Rectangle;

import ij.process.*;	// Pour classe Float Processor

abstract public class DCT2D {

	// ---------------------------------------------------------------------------------
	/**
	 * Transformation DCT 2D directe (methode de classe) utilisant la separabilite
	 * @param fp Signal 2D d'entree et de sortie (MxN) (FloatProcessor)
	 */ 
	public static void forwardDCT(FloatProcessor fp) {


		Rectangle r=fp.getRoi();
		if(r==null)
			r=new Rectangle(0, 0, fp.getWidth(), fp.getHeight());

		int width=r.width;
		int height=r.height;

		//	float data[][]=fp.getFloatArray();
		double tmp[];


		// Traiter les lignes
		for(int i=0;i<height;++i){
			tmp=new double[width];

			for(int d=0;d<width;++d)
				tmp[d]=fp.getPixelValue(d+r.x, i+r.y)-128;

			tmp=DCT1D.forwardDCT(tmp);

			for(int y=0;y<width;++y)
				fp.putPixelValue(y+r.x, i+r.y, tmp[y]);

		}


		// Traiter les colonnes de l'image resultant du traitement des lignes
		for(int i=0;i<width;++i){
			tmp=new double[height];

			for(int d=0;d<height;++d)
				tmp[d]=fp.getPixelValue(i+r.x, d+r.y);

			tmp=DCT1D.forwardDCT(tmp);

			for(int y=0;y<height;++y){
				fp.putPixelValue(i+r.x, y+r.y, tmp[y]);
			}

		}
	}

	// ---------------------------------------------------------------------------------
	/**
	 * Transformation DCT 2D inverse (methode de classe)
	 * @param fp Signal 2D d'entree et de sortie (FloatProcessor)
	 */
	public static void inverseDCT(FloatProcessor fp) {

		Rectangle r=fp.getRoi();
		if(r==null)
			r=new Rectangle(0, 0, fp.getWidth(), fp.getHeight());

		int width=r.width;
		int height=r.height;


		//	float data[][]=fp.getFloatArray();
		double tmp[];

		// Traiter les lignes
		for(int i=0;i<height;++i){
			tmp=new double[width];

			for(int d=0;d<width;++d)
				tmp[d]=fp.getPixelValue(d+r.x, i+r.y);

			tmp=DCT1D.inverseDCT(tmp);

			for(int y=0;y<width;++y)
				fp.putPixelValue(y+r.x, i+r.y, tmp[y]);

		}


		// Traiter les colonnes de l'image resultant du traitement des lignes
		for(int i=0;i<width;++i){
			tmp=new double[height];

			for(int d=0;d<height;++d)
				tmp[d]=fp.getPixelValue(i+r.x, d+r.y);

			tmp=DCT1D.inverseDCT(tmp);

			for(int y=0;y<height;++y){
				fp.putPixelValue(i+r.x, y+r.y, tmp[y]+128);
			}

		}
	}
}