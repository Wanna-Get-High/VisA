import java.util.ArrayList;
import java.util.List;

import DCT.DCT2D;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class Dct_ implements PlugInFilter {

	// Matrice de quantification pour la luminance (colonne par colonne)
	public final static int[][] QY = {
		{16, 12, 14, 14,  18,  24,  49,  72},
		{11, 12, 13, 17,  22,  35,  64,  92},
		{10, 14, 16, 22,  37,  55,  78,  95},
		{16, 19, 24, 29,  56,  64,  87,  98},
		{24, 26, 40, 51,  68,  81, 103, 112},
		{40, 58, 57, 87, 109, 104, 121, 100},
		{51, 60, 69, 80, 103, 113, 120, 103},
		{61, 55, 56, 62,  77,  92, 101,  99}
	};

	final static int BLOCK_SIZE = 8;
	ImagePlus imp;
	private static ByteProcessor bpQuantification = new ByteProcessor(BLOCK_SIZE, BLOCK_SIZE);
	private int qualite;



	@Override
	public int setup(String arg, ImagePlus imp) {
		// TODO Auto-generated method stub
		this.imp = imp;
		return PlugInFilter.DOES_32;
	}

	@Override
	public void run(ImageProcessor ip) {
		if(!showDialog())
			return;

		FloatProcessor fp=(FloatProcessor) ip.duplicate();
		bpQuantification.setIntArray(QY);
		if(qualite<97)
			if(qualite<=50) {
				bpQuantification.multiply(50/qualite);
			} else {
				bpQuantification.multiply(2.0-(2.0*qualite/100.0));
			}

		ArrayList<List<Float>> listOfList = new ArrayList<List<Float>>();
		for(int x=0;x<ip.getWidth();x+=BLOCK_SIZE){
			for(int y=0;y<ip.getHeight();y+=BLOCK_SIZE){
				fp.setRoi(x, y, BLOCK_SIZE, BLOCK_SIZE);
				DCT2D.forwardDCT(fp);
				
				fp.copyBits(bpQuantification, x, y, Blitter.DIVIDE);

				for(int i=0;i<BLOCK_SIZE;++i){
					for(int j=0;j<BLOCK_SIZE;++j){
						fp.putPixelValue(x+i, y+j, Math.round(fp.getPixelValue(x+i, y+j)));
					}
				}
				
				ImageProcessor imgP = fp.resize(BLOCK_SIZE);
				listOfList.add(coefsBeforeEOB(imgP));
			}
		}

		int poids=0;
		for(List l : listOfList)
			poids+=l.size();
		
		System.out.println(poids+"/"+(fp.getWidth()*fp.getHeight()));
		new ImagePlus("DCT2D", fp).show();
	}

	private boolean showDialog() {
		// TODO Auto-generated method stub
		GenericDialog g=new GenericDialog("choix qualite");
		g.addSlider("qualite", 0, 100, 80);
		g.showDialog();
		if(g.wasCanceled())
			return false;
		qualite=(int)g.getNextNumber();
		return true;
	}

	/**
	 * GŽnre la sŽquence des valeurs rŽsultant de la lecture zigzag (parcours des coefficients DCT pour JPEG)
	 * @param fpBlock bloc (carrŽ) de coefficients DCT (donnŽs dans un ImageProcessor)
	 * @return out SŽquence des coefficients DCT suite ˆ lecture zigzag
	 */
	private static float[] zigzagSeq(ImageProcessor fpBlock) {

		int size = fpBlock.getWidth();	// taille du bloc
		int moitie = size*(size+1)/2;	// nombre de coefs sur-diagonaux
		float[] out = new float[size*size];

		int nCoefs = 0;	// nb de coefs dŽjˆ parcourus
		// Parcours de la d-ime diagonale (numŽrotŽe ˆ partir de 0)
		for (int d = 0; d < 2*size-1; d++) {
			if (d%2 == 0) {	//Diagonale paire
				for (int x = (nCoefs<moitie?0:d-size+1); x<=(nCoefs<moitie?d:size-1); x++) {
					out[nCoefs++]=fpBlock.getPixelValue(x,d-x);
				}
			} else { // Diagonale impaire
				for (int x = (nCoefs<moitie?d:size-1); x>=(nCoefs<moitie?0:d-size+1); x--) {
					out[nCoefs++]=fpBlock.getPixelValue(x,d-x);
				}
			}
		}
		return out;
	}


	private static List<Float> coefsBeforeEOB(ImageProcessor f){
		List<Float> retour=new ArrayList<Float>();
		float tab[]=zigzagSeq(f);
		int index=tab.length-1;
		while(index>=0&&tab[index]==0)
			index--;
		while(index>=0)
			retour.add(0, tab[index--]);
		return retour;
	}

}