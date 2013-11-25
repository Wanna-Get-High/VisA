import DCT.DCT2D;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class Test_dctInv implements PlugInFilter {

	final static int BLOCK_SIZE = 8;

	@Override
	public int setup(String arg, ImagePlus imp) {
		return PlugInFilter.DOES_ALL;
	}

	@Override
	public void run(ImageProcessor ip) {
		FloatProcessor fp = (FloatProcessor) ip.convertToFloat();

		int width = ip.getWidth();
		int height = ip.getHeight();

		for (int i = 0; i < width; i += BLOCK_SIZE) {
			for (int j = 0; j < height; j += BLOCK_SIZE) {
				fp.setRoi(i, j, BLOCK_SIZE, BLOCK_SIZE);
				DCT2D.inverseDCT(fp);
			}
		}

		ImagePlus img = new ImagePlus("image apres DCTInv", fp);
		img.show();

	}

}
