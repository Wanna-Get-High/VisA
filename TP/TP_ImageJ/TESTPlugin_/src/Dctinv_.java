import DCT.DCT2D;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;


public class Dctinv_ implements PlugInFilter {

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
		FloatProcessor fp=(FloatProcessor) ip.duplicate();
		if(!showDialog())
			return;

		bpQuantification.setIntArray(QY);
		bpQuantification.setIntArray(QY);
		if(qualite<97)
			if(qualite<=50){
				bpQuantification.multiply(50/qualite);
			}else{
				bpQuantification.multiply(2.0-(2.0*qualite/100.0));
			}


		for(int x=0;x<ip.getWidth();x+=BLOCK_SIZE){
			for(int y=0;y<ip.getHeight();y+=BLOCK_SIZE){
				fp.setRoi(x, y, BLOCK_SIZE, BLOCK_SIZE);
				fp.copyBits(bpQuantification, x, y, Blitter.MULTIPLY);
				DCT2D.inverseDCT(fp);
			}
		}
		new ImagePlus("DCT2D inv", fp).show();

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
}