import ij.*;
import ij.plugin.frame.PlugInFrame;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.gui.*;
import ij.util.Tools;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.geom.*;

/**
 * This plugin continuously displays the pixel values of the cursor and
 * its surroundings. The position can be fixed by a keystroke; in this
 * case the last cursor position is used further on and marked in the
 * image. Use this mode to examine how a filter changes the data (also
 * during preview). By default, the key switching between fixed mode
 * and following the cursor is the "!" key.
 *
 * If the Pixel Inspector Window is in the foreground, "c" with any modifier
 * keys (CTRL-C etc) copies the current data into the clipboard (tab-delimited).
 * The arrow keys nudge the position (in fixed mode).
 *
 * Preferences (Press the Prefs button at top left):
 *
 * Radius determines the size of the window, 3x3 for radius=1, etc.
 * The Pixel Inspector window must be closed and opened to get the new
 * size.
 * Readout for grayscale 8&16 bit images can be raw, calibrated or
 * hexadecimal.
 * Readout for RGB images can ge R,G,B triples, gray value or hexadecimal.
 * The "fix position" key should be a key not used by ImageJ as a shortcut,
 * e.g. "!". It can be a normal character key or F1 ... F12.
 * The color of the square showing the position in fixed mode can be selected.
 * For copying the data to clipboard, it can be selected whether the position
 * (x,y) is not not written, written in the first line or in the same way
 * as the header lines of the Pixel Inspector panel.
 *
 * Limitations and known problems:
 *
 * x and y coordinates are always uncalibrated pixel numbers.
 *
 * Some image operations do not update the display.
 *
 * If the image is in the foreground, sometimes the "fix position"
 * key may not work (On OS X, this can also happen with the ImageInspector
 * panel in the foreground). As a workaround, either click on the image
 * with any area selection tool or put the Pixel Inspector image in
 * the foreground.
 *
 *
 * Michael Schmid
 * Version 2007-Dec-06 - bugs fixed:
 *      did not always follow cursor
 *      nudge could make the display hang
 *      pixel value calibration was sometimes ignored
 * Version 2007-Dec-14 - supports exponential format for large/small data values
 */
public class Pixel_Inspector extends PlugInFrame
        implements MouseMotionListener, ImageListener, ActionListener, KeyListener, Runnable {
    //MouseMotionListener detects mouse movement
    //ImageListener: listens to closing of the image (and changes of image data)
    //ActionListener: for Button
    //KeyListener: for fix/unfix key
    //Runnable: for background thread

    /* Preferences and related */
    int radius = 3;                 //determines size of the surrounding (min. 1)
    final static int MAX_RADIUS = 10;//the largest radius possible (ImageJ can hang if too large)
    int saveRadius = radius;        //what to write into the prefs
    int grayDisplayType = 0;        //how to display 8-bit&16-bit grayscale pixels
    final static String[] GRAY_DISPLAY_TYPES = {"Raw","Calibrated","Hex"};
    final static int GRAY_RAW = 0, GRAY_CAL = 1, GRAY_HEX = 2;
    int rgbDisplayType = 0;         //how to display rgb pixels
    final static String[] RGB_DISPLAY_TYPES = {"R,G,B","Gray Value","Hex"};
    final static int RGB_RGB = 0, RGB_GRAY = 1, RGB_HEX = 2;
    int copyType = 0;               //what to copy to the clipboard
    final static String[] COPY_TYPES = {"Data Only","x y and Data","Header and Data"};
    final static int COPY_DATA = 0, COPY_XY = 1, COPY_HEADER = 2;
    int colorNumber = 0;            //color of the position marker in fixed mode
    final static String[] COLOR_STRINGS = {"red","orange","yellow","green","cyan","blue","magenta",};
    final static Color[] COLORS = {Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA};
    int fixKey = '!';               //the key (keycode+0x10000 or char) for fixing/unfixing the position
    final static int KEYCODE_OFFSET = 0x10000;  //we add this to keycodes to separate them from key characters
    /* current status */
    private boolean isFixed = false; //whether the position is fixed and does not move with the cursor
    private int x0,y0;              //the current position
    int nextUpdate;                 //type of next update
    final static int POSITION_UPDATE = 1, FULL_UPDATE = 2;

    ImageJ ij;
    ImagePlus imp;                  //the ImagePlus that we listen to
    int impType;                    //the type of the ImagePlus
    int digits;                     //decimal fraction digits to display
    boolean expMode;                //whether to display the data in exp format
    ImageCanvas canvas;             //the canvas of imp
    Thread bgThread;                //thread for output (in the background)
    Button prefsButton;             //gets the prefs dialog
    Label[] labels;                 //the display fields
    final String PREFS_KEY="pixelinspector."; //key in IJ_Prefs.txt

    /* Initialization, preparing the window (panel) **/
    public Pixel_Inspector() {
        super("Pixel Inspector");
        ij = IJ.getInstance();
        if (ij == null) return;     //it won't work with the ImageJ applet
        imp = WindowManager.getCurrentImage();
        if (imp==null) {
            IJ.noImage(); return;
        }
        impType = imp.getType();
        setTitle("Pixels of "+imp.getTitle());
        WindowManager.addWindow(this);
        readPreferences();
        int trim = IJ.isMacOSX()?11:0;
        prefsButton = new TrimmedButton("Prefs", trim);
        prefsButton.addActionListener(this);
        prefsButton.addKeyListener(this);   //this is enough as a listener for the panel
        int size = 2*radius+2;              //number of columns and rows
        labels = new Label[size*size];
        for (int i=1; i<labels.length; i++) //make the labels (display fields)
            labels[i] = new Label();
        initializeLabels();                 //fill the labels with spaceholders
        GridBagLayout gridbag = new GridBagLayout();//set up the layout
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);
        c.insets = new Insets(1,0,1,0);     //top, left, bottom, right
        int y = 0;
        c.gridwidth = 1; c.anchor = GridBagConstraints.EAST;
        for (int row=0,p=0; row<size; row++,y++) {
            for (int col=0; col<size; col++,p++) {
                if (row == 0 && col == 0) {
                    c.anchor = GridBagConstraints.WEST;
                    add(prefsButton, c);
                } else {
                    c.gridx = col; c.gridy = y;
                    c.anchor = GridBagConstraints.EAST;
                    add(labels[p], c);
                }
            }
        }
        pack();                             //prepare for display
        Point loc = Prefs.getLocation(PREFS_KEY+"loc");
        if (loc!=null)
            setLocation(loc);
        else
            GUI.center(this);
        setResizable(false);
        show();
        toFront();
        addImageListeners();
                                            //thread for output in the background
        bgThread = new Thread(this, "Pixel Inspector");
        bgThread.start();
        bgThread.setPriority(Math.max(bgThread.getPriority()-3, Thread.MIN_PRIORITY));
        update(FULL_UPDATE);                //the first data display
    }

    public void close() {
        Prefs.saveLocation(PREFS_KEY+"loc", getLocation());
        removeImageListeners();
        isFixed = false;
        drawOverlay();                      //remove overlay
        synchronized(this) {                //terminate the background thread
            bgThread.interrupt();
        }
        super.close();                      //also does WindowManager.removeWindow(this);
    }

    private void addImageListeners() {
        imp.addImageListener(this);
        ImageWindow win = imp.getWindow();
        if (win == null) close();
        canvas = win.getCanvas();
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
    }

    private void removeImageListeners() {
        imp.removeImageListener(this);
        canvas.removeMouseMotionListener(this);
        canvas.removeKeyListener(this);
    }

    //MouseMotionListner
    public void mouseDragged(MouseEvent e) { update(POSITION_UPDATE); }
    public void mouseMoved(MouseEvent e) { update(POSITION_UPDATE); }
    //ImageListener
    public void imageUpdated(ImagePlus imp) { update(FULL_UPDATE); }
    public void imageOpened(ImagePlus imp) {}
    public void imageClosed(ImagePlus imp) {
        if (imp == this.imp) close();
    }
    //ActionListener (Prefs Button)
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof Button) { showDialog(); }
    }
    //KeyListener
    public void keyPressed(KeyEvent e) {
        if (e.getKeyChar() == fixKey || e.getKeyCode()+KEYCODE_OFFSET == fixKey) {
            isFixed = !isFixed;
            update(FULL_UPDATE);
        } else if (e.getSource() instanceof Button) { //keys typed into this panel
            if (e.getKeyCode() == KeyEvent.VK_C) {//all keys "c" (independent of modifier): copy
                copyToClipboard();
            } else if (isFixed && e.getKeyCode() == KeyEvent.VK_UP && y0 > 0) {
                y0--; update(FULL_UPDATE);;     //mouse keys nudge the position in fixed mode
            } else if (isFixed && e.getKeyCode() == KeyEvent.VK_DOWN && y0 < imp.getHeight()-1) {
                y0++; update(FULL_UPDATE);
            } else if (isFixed && e.getKeyCode() == KeyEvent.VK_LEFT && x0 > 0) {
                x0--; update(FULL_UPDATE);
            } else if (isFixed && e.getKeyCode() == KeyEvent.VK_RIGHT && x0 < imp.getWidth()-1) {
                x0++; update(FULL_UPDATE);
            } else
                ij.keyPressed(e);               //forward other keys from the panel to ImageJ
        }
    }
    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    synchronized void update(int whichUpdate) {
        if (isFixed && whichUpdate == POSITION_UPDATE) return;
        if (nextUpdate < whichUpdate) nextUpdate = whichUpdate;
        notify();                               //wake up the background thread
    }

    // the background thread for updating the table
    public void run() {
        boolean doFullUpdate = false;
        while (true) {
            if (doFullUpdate) {
                drawOverlay();
                setCalibration();
            }
            writeNumbers();
            IJ.wait(50);

            synchronized(this) {
                if (nextUpdate == 0) {
                    try {wait();}               //notify wakes up the thread
                    catch(InterruptedException e) { //interrupted tells the thread to exit
                        return;
                    }
                } else {
                    doFullUpdate = nextUpdate == FULL_UPDATE;
                    nextUpdate = 0;
                }
            }
        } //while (true)
    }

    /** get the surrounding pixels and display them */
    void writeNumbers() {
        if (imp.getType() != impType || imp.getWindow().getCanvas() != canvas) {    //image type or canvas changed?
            removeImageListeners();
            addImageListeners();
            initializeLabels();
            this.pack();
            impType = imp.getType();
            nextUpdate = FULL_UPDATE;
            return;
        }
        ImageProcessor ip = imp.getProcessor();
        if (ip == null) return;
        int width = ip.getWidth();
        int height = ip.getHeight();
        if (!isFixed) {
            Point cursorLoc = canvas.getCursorLoc();
            if (cursorLoc == null) return;
            this.x0 = cursorLoc.x;
            this.y0 = cursorLoc.y;
        }
        int x0 = this.x0;       //class variables may change asynchronously, fixed values needed here
        int y0 = this.y0;
        int p = 1;    //pointer in labels array
        for (int x = x0-radius; x <= x0+radius; x++,p++)
            labels[p].setText(x>=0&&x<width ? Integer.toString(x) : " ");
        for (int y = y0-radius; y <= y0+radius; y++) {
            boolean yInside = y>=0&&y<height;
            int yDisplay =  (Analyzer.getMeasurements() & Measurements.INVERT_Y)!=0 ? height-y-1 : y;
            labels[p].setText(yInside ? Integer.toString(yDisplay) : " ");
            p++;
            for (int x = x0-radius; x <= x0+radius; x++,p++) {
                if (x>=0&&x<width&&yInside) {
                    if (ip instanceof ColorProcessor && rgbDisplayType == RGB_RGB) {
                        int c = ip.getPixel(x,y);
                        int r = (c&0xff0000)>>16;
                        int g = (c&0xff00)>>8;
                        int b = c&0xff;
                        labels[p].setText(r+","+g+","+b);
                    } else if (ip instanceof ColorProcessor && rgbDisplayType == RGB_HEX)
                        labels[p].setText(int2hex(ip.getPixel(x,y),6));
                    else if ((ip instanceof ByteProcessor || ip instanceof ShortProcessor) && grayDisplayType == GRAY_RAW)
                        labels[p].setText(Integer.toString(ip.getPixel(x,y)));
                    else if ((ip instanceof ByteProcessor || ip instanceof ShortProcessor) && grayDisplayType == GRAY_HEX)
                        labels[p].setText(int2hex(ip.getPixel(x,y), ip instanceof ByteProcessor ? 2 : 4));
                    else
                        labels[p].setText(stringOf(ip.getPixelValue(x,y), digits, expMode));
                } else
                    labels[p].setText(" ");
            }
        } //for y
    }

    /** initialize content of the labels to make sure we have enough space */
    void initializeLabels() {
        Color bgColor = new Color(0xcccccc);    //background for row/column header
        String placeHolder = "000000.00";       //how much space to reserve (enough for float, calibrated, rgb)
        ImageProcessor ip = imp.getProcessor();
        if (ip instanceof ByteProcessor || ip instanceof ShortProcessor) {
            if (grayDisplayType == GRAY_RAW || grayDisplayType == GRAY_HEX)
                placeHolder = "00000";          //minimum space, needed for header (max 99k pixels)
        } else if (ip instanceof ColorProcessor) {
            if (rgbDisplayType == RGB_RGB)
                placeHolder = "000,000,000";
            if (rgbDisplayType == RGB_GRAY)
                placeHolder = "000.00";
            else if (rgbDisplayType == RGB_HEX)
                placeHolder = "CCCCCC";
        } 
        int p = 0;                              //pointer in labels array
        int size = 2*radius+1;
        for (int y = 0; y<size+1; y++) {        //header line and data lines
            if (y > 0)                          //no label in top-left corner
                labels[p].setText("00000");
            p++;
            for (int x = 0; x<size; x++,p++)
                labels[p].setText(placeHolder);
        }
        labels[radius+1].setForeground(Color.RED); //write current position in red
        labels[(2*radius+2)*(radius+1)].setForeground(Color.RED);
        labels[(2*radius+2)*(radius+1)+radius+1].setForeground(Color.RED);
        for (int i=0; i<size; i++) {            //header lines have a darker background
            labels[i+1].setBackground(bgColor);
            labels[(2*radius+2)*(i+1)].setBackground(bgColor);      
        }
        for (int i=1; i<labels.length; i++)
            labels[i].setAlignment(Label.RIGHT);
    }

    /* set the pixel value calibration of the ImageProcessor and the output format */
    void setCalibration() {
        Calibration cal = imp.getCalibration();
        float[] cTable = cal.getFunction()==Calibration.NONE ? null : cal.getCTable();
        ImageProcessor ip = imp.getProcessor();
        if (ip != null) ip.setCalibrationTable(cTable);
        if (ip instanceof FloatProcessor || cTable != null) {
            float[] data = (ip instanceof FloatProcessor) ? (float[])ip.getPixels() : cTable;
            double[] minmax = Tools.getMinMax(data);
            double maxDataValue = Math.max(Math.abs(minmax[0]), Math.abs(minmax[1]));
            digits = (int)(6-Math.log(maxDataValue)/Math.log(10));
            expMode = digits<-1 || digits>7;
            if (Math.min(minmax[0], minmax[1]) < 0)
                digits--; //more space needed for minus sign
        } else {
            digits = 2;
            expMode = false;
        }
    }

    /** Converts a number to a string in decimal or exp format.
     *  The number of digits is chosen to make the value fit into
     *  a cell the size of "000000.00"
     */
    String stringOf(float v, int digits, boolean expMode) {
        if (expMode) {
            int exp = (int)Math.floor(Math.log(Math.abs(v))/Math.log(10));
            double mant = v/Math.pow(10,exp);
            digits = (exp > 0 && exp < 10) ? 5 : 4;
            if (v<0) digits--;      //space needed for minus
            return IJ.d2s(mant,digits)+"e"+exp;
        } else
            return IJ.d2s(v, digits);
    }

    void copyToClipboard() {
        final char delim = '\t';
        int size = 2*radius +1;
        int p = 1;
        StringBuffer sb = new StringBuffer();
        if (copyType == COPY_XY) {
            sb.append(labels[radius+1].getText()); sb.append(delim);
            sb.append(labels[(2*radius+2)*(radius+1)].getText()); sb.append('\n');
        } else if (copyType == COPY_HEADER) {
            for (int x=0; x<size; x++,p++) {
                sb.append(delim);
                sb.append(labels[p].getText());
            }
            sb.append('\n');
        }
        p = size + 1;
        for (int y=0; y<size; y++) {
            if (copyType == COPY_HEADER) {
                sb.append(labels[p].getText()); sb.append(delim);
            }
            p++;
            for (int x=0; x<size; x++,p++) {
                if (x > 0)
                    sb.append(delim);
                sb.append(labels[p].getText());
            }
            sb.append('\n');
        }
        String s = new String(sb);
        Clipboard clip = getToolkit().getSystemClipboard();
        if (clip==null) return;
        StringSelection contents = new StringSelection(s);
        clip.setContents(contents, contents);
    }

    /** draw the fixed position as overlay (outside the pixels we look at)*/
    void drawOverlay() {
        if (canvas == null) return;
        if (isFixed) {
            float moveOutside = 1f/(float)Math.ceil(canvas.getMagnification());
            GeneralPath path = new GeneralPath();
            path.moveTo(x0-radius-moveOutside, y0-radius-moveOutside);
            path.lineTo(x0+radius+1, y0-radius-moveOutside);
            path.lineTo(x0+radius+1, y0+radius+1);
            path.lineTo(x0-radius-moveOutside, y0+radius+1);
            path.lineTo(x0-radius-moveOutside, y0-radius-moveOutside);
            canvas.setDisplayList(path, COLORS[colorNumber], null);
        } else
            canvas.setDisplayList(null);
    }

    /** Preferences dialog */
    void showDialog() {
        String fixKeyString = "!";
        if (fixKey < KEYCODE_OFFSET)
            fixKeyString = String.valueOf((char) fixKey);
        else
            fixKeyString = "F" + (fixKey - KEYCODE_OFFSET - KeyEvent.VK_F1 +1);
        GenericDialog gd = new GenericDialog("Pixel Inspector Prefs...");
        gd.addNumericField("Radius *", saveRadius, 0, 6, "(1-"+MAX_RADIUS+")");
        gd.addChoice("Grayscale 8&16-Bit Readout",GRAY_DISPLAY_TYPES,GRAY_DISPLAY_TYPES[grayDisplayType]);
        gd.addChoice("Color (RGB) Readout",RGB_DISPLAY_TYPES,RGB_DISPLAY_TYPES[rgbDisplayType]);
        gd.addStringField("Fix Position Key ", fixKeyString,3);
        gd.addChoice("Fixed Position Marker", COLOR_STRINGS, COLOR_STRINGS[colorNumber]);
        gd.addChoice("Copy to Clipboard", COPY_TYPES, COPY_TYPES[copyType]);
        gd.addMessage("* close and reopen to apply");
        gd.showDialog();
        if (!gd.wasCanceled()){
            int temp = (int)gd.getNextNumber(); //radius
            if (temp >= 1 && temp <= MAX_RADIUS) saveRadius = temp;
            grayDisplayType = gd.getNextChoiceIndex();
            rgbDisplayType = gd.getNextChoiceIndex();
            String keyString = gd.getNextString();
            colorNumber = gd.getNextChoiceIndex();
            copyType = gd.getNextChoiceIndex();
            boolean keyOK = false;
            if (keyString.length() == 1) {      //decode "fix position" key
                fixKey = keyString.charAt(0);
                keyOK = true;
            } else if (keyString.toUpperCase().charAt(0) == 'F') {
                int keynum = Integer.parseInt(keyString.substring(1));
                if (keynum >=1 && keynum <=12) {
                    fixKey = KEYCODE_OFFSET + KeyEvent.VK_F1 - 1 + keynum;
                    keyOK = true;
                }
            }
            if (!keyOK)
                IJ.error("Invalid Fix Position Key","Key must be a single character or F1...F12");
            savePreferences();
            initializeLabels();                 //format (cell size) may have changed
            this.pack();
            update(FULL_UPDATE);                //format and overlay color may have changed
        }
    }

    void readPreferences() {
        radius = (int)Prefs.get(PREFS_KEY+"radius", radius);
        grayDisplayType = (int)Prefs.get(PREFS_KEY+"display.gray", grayDisplayType);
        rgbDisplayType = (int)Prefs.get(PREFS_KEY+"display.rgb", rgbDisplayType);
        fixKey = (int)Prefs.get(PREFS_KEY+"fixkey", fixKey);
        colorNumber = (int)Prefs.get(PREFS_KEY+"color", colorNumber);
        copyType = (int)Prefs.get(PREFS_KEY+"copy", copyType);
        saveRadius = radius;
    }

    void savePreferences() {
        Prefs.set(PREFS_KEY+"radius", saveRadius);
        Prefs.set(PREFS_KEY+"display.gray", grayDisplayType);
        Prefs.set(PREFS_KEY+"display.rgb", rgbDisplayType);
        Prefs.set(PREFS_KEY+"fixkey", (int)fixKey);
        Prefs.set(PREFS_KEY+"color", colorNumber);
        Prefs.set(PREFS_KEY+"copy", copyType);
    }

    static String int2hex(int i, int digits) {
        boolean addHexSign = digits<6;
        char[] buf = new char[addHexSign ? digits+1 : digits];
        for (int pos=buf.length-1; pos>=buf.length-digits; pos--) {
            buf[pos] = Tools.hexDigits[i&0xf];
            i >>>= 4;
            if (addHexSign) buf[0] = 'x';
        }
        return new String(buf);
    }
}
