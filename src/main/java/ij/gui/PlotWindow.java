//EU_HOU
package ij.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.util.*;
import ij.*;
import ij.process.*;
import ij.util.*;
import ij.text.TextWindow;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.PlugInFilterRunner;
import ij.measure.*;
import ij.io.SaveDialog;

/** This class implements the Analyze/Plot Profile command.
* @author Michael Schmid
* @author Wayne Rasband
*/
public class PlotWindow extends ImageWindow implements ActionListener, ItemListener,
	ClipboardOwner, ImageListener, RoiListener, Runnable {

	/** @deprecated */
	public static final int CIRCLE = Plot.CIRCLE;
	/** @deprecated */
	public static final int X = Plot.X;
	/** @deprecated */
	public static final int BOX = Plot.BOX;
	/** @deprecated */
	public static final int TRIANGLE = Plot.TRIANGLE;
	/** @deprecated */
	public static final int CROSS = Plot.CROSS;
	/** @deprecated */
	public static final int LINE = Plot.LINE;
	/** Write first X column when listing or saving. */
	public static boolean saveXValues = true;
	/** Autom	atically close window after saving values. To set, use Edit/Options/Plots. */
	public static boolean autoClose;
	/** Display the XY coordinates in a separate window. To set, use Edit/Options/Plots. */
	public static boolean listValues;
	/** Interpolate line profiles. To set, use Edit/Options/Plots or setOption("InterpolateLines",boolean). */
	public static boolean interpolate = true;
	// default values for new installations; values will be then saved in prefs
	private static final int WIDTH = 600;
	private static final int HEIGHT = 340;
	private static int defaultFontSize = 14; 
	/** The width of the plot (without frame) in pixels. */
	public static int plotWidth = WIDTH;
	/** The height of the plot in pixels. */
	public static int plotHeight = HEIGHT;
	/** The plot text size, can be overridden by Plot.setFont, Plot.setFontSize, Plot.setXLabelFont etc. */
	public static int fontSize = defaultFontSize;
	/** Have axes with no grid lines. If both noGridLines and noTicks are true,
	 *	only min&max value of the axes are given */
	public static boolean noGridLines;
	/** Have axes with no ticks. If both noGridLines and noTicks are true,
	 *	only min&max value of the axes are given */
	public static boolean noTicks;

	private static final int LEFT_MARGIN = 50;
    private static final int RIGHT_MARGIN = 20;
    private static final int TOP_MARGIN = 20;
    private static final int BOTTOM_MARGIN = 30;
	private static final String PREFS_WIDTH = "pp.width";
	private static final String PREFS_HEIGHT = "pp.height";
	private static final String PREFS_FONT_SIZE = "pp.fontsize";
	private static final String OPTIONS = "pp.options";
	private static final int SAVE_X_VALUES = 1;
	private static final int AUTO_CLOSE = 2;
	private static final int LIST_VALUES = 4;
	private static final int INTERPOLATE = 8;
	private static final int NO_GRID_LINES = 16;
	private static final int NO_TICKS = 32;
	private static String moreButtonLabel = "More "+'\u00bb';
	private static String dataButtonLabel = "Data "+'\u00bb';

	boolean wasActivated;			// true after window has been activated once, needed by PlotCanvas

	private Button data, more, live; //EU_HOU CHANGES : removed list
	private PopupMenu dataPopupMenu, morePopupMenu;
	private static final int NUM_MENU_ITEMS = 20; //how many menu items we have in total
	private MenuItem[] menuItems = new MenuItem[NUM_MENU_ITEMS];
	private Label statusLabel;
	private String userStatusText;
	private static String defaultDirectory = null;
	private static int options;
	private int defaultDigits = -1;
	private int markSize = 5;
	private static Plot staticPlot;
	private Plot plot;

	private PlotMaker plotMaker;
	private ImagePlus srcImp;		// the source image for live plotting
	private Thread bgThread;		// thread for plotting (in the background)
	private boolean doUpdate;		// tells the background thread to update

	private Roi[] rangeArrowRois;	// the overlays (arrows etc) for changing the range. Note: #10-15 must correspond to PlotDialog.dialogType!
	private boolean rangeArrowsVisible;
	private int activeRangeArrow = -1;
	private static Color inactiveRangeArrowColor = Color.GRAY;
	private static Color inactiveRangeRectColor = new Color(0x20404040, true); //transparent gray
	private static Color activeRangeArrowColor = Color.RED;
	private static Color activeRangeRectColor = new Color(0x18ff0000, true); //transparent red

	/*
	 * EU_HOU CHANGES
	 */
    private Font font = new Font("Helvetica", Font.PLAIN, 12);
    private int nPoints;
    private String xLabel;
    private String yLabel;
	public float[] xValues, yValues, xInitValues, yInitValues, yValuesBaseLine, yValuesMergeGaussFit;
	public double[] gaussFitResult;
	public int nbgauss;
	public double xMin, xMax, xInitMin, xInitMax, yMin, yMax;
	public Button list, save, copy, setting, setScale, option, setScaleRadio, baseline, subtract, gauss;
	public Label Xcoord, Ycoord;
	public boolean modifListener = false;
	public int oldX = -1, oldY = -1, oldindex = -1;
	public int Xpoints[], Ypoints[];
	protected ImagePlus imp;
	public ImagePlus origin;
	public Roi region;
	/**
	 *  Begin the plot profile with the number pixel selected and not zero. the
	 *  selection should be horizontal
	 */
	 private static boolean horizontal;//EU_HOU_rb
	 /**
	  * Specific menu for the spectrum analysis
	  */
	    private double xScale, yScale;
	 public static boolean RadioSpectra = false;//EU_HOU_jl
	 public static boolean Base_Line = false;
	 public static boolean Base_Line_subtracted = false;
	 public static boolean ZERO_LINE = false;
	 public final static int X_NUMBERS = 0x1;
	    /**
	     * flag for numeric labels of x-axis ticks
	     */
	    public final static int Y_NUMBERS = 0x2;
	    /**
	     * flag for drawing x-axis ticks
	     */
	    public final static int X_TICKS = 0x4;
	    /**
	     * flag for drawing x-axis ticks
	     */
	    public final static int Y_TICKS = 0x8;
	    /**
	     * flag for drawing vertical grid lines for x-axis ticks
	     */
	    public final static int X_GRID = 0x10;
	    /**
	     * flag for drawing horizontal grid lines for y-axis ticks
	     */
	    public final static int Y_GRID = 0x20;
	    /**
	     * flag for forcing frame to coincide with the grid/ticks in x direction
	     * (results in unused space)
	     */
	    public final static int X_FORCE2GRID = 0x40;
	    /**
	     * flag for forcing frame to coincide with the grid/ticks in y direction
	     * (results in unused space)
	     */
	    public final static int Y_FORCE2GRID = 0x80;
	    /**
	     * the default flags
	     */
	    public final static int DEFAULT_FLAGS = X_NUMBERS + Y_NUMBERS
	            + /*
	             * X_TICKS + Y_TICKS +
	             */ X_GRID + Y_GRID;
	    public final static int MAX_INTERVALS = 12;//maximum number of intervals between ticks or grid lines
	    public final static int MIN_X_GRIDWIDTH = 60;//minimum distance between grid lines or ticks along x
	    public final static int MIN_Y_GRIDWIDTH = 40;//minimum distance between grid lines or ticks along y
	    public final static int TICK_LENGTH = 3;
	    private int flags;
	    public final Color gridColor = new Color(0xc0c0c0);

	private Vector labels = new Vector(), xlabs = new Vector(), ylabs = new Vector();
	
    private ImageProcessor ip;
    private Rectangle frame = null;
	
	// static initializer
	static {
		options = Prefs.getInt(OPTIONS, SAVE_X_VALUES);
		autoClose = (options&AUTO_CLOSE)!=0;
		plotWidth = Prefs.getInt(PREFS_WIDTH, WIDTH);
		plotHeight = Prefs.getInt(PREFS_HEIGHT, HEIGHT);
		defaultFontSize = fontSize = Prefs.getInt(PREFS_FONT_SIZE, defaultFontSize);
		Dimension screen = IJ.getScreenSize();
		if (plotWidth>screen.width && plotHeight>screen.height) {
			plotWidth = WIDTH;
			plotHeight = HEIGHT;
		}
	}

	/**
	* @deprecated
	* replaced by the Plot class.
	*/
	public PlotWindow(String title, String xLabel, String yLabel, float[] xValues, float[] yValues) {
		super(createImage(title, xLabel, yLabel, xValues, yValues));
		plot = staticPlot;
		((PlotCanvas)getCanvas()).setPlot(plot);
	}

	/**
	* @deprecated
	* replaced by the Plot class.
	*/
	public PlotWindow(String title, String xLabel, String yLabel, double[] xValues, double[] yValues) {
		this(title, xLabel, yLabel, Tools.toFloat(xValues), Tools.toFloat(yValues));
	}

	/** Creates a PlotWindow from a given ImagePlus with a Plot object.
	 *  (called when reading an ImagePlus with an associated plot from a file) */
	public PlotWindow(ImagePlus imp, Plot plot) {
		super(imp);
		((PlotCanvas)getCanvas()).setPlot(plot);
		this.plot = plot;
		draw();
	}

	/** Creates a PlotWindow from a Plot object. */
	PlotWindow(Plot plot) {
		super(plot.getImagePlus());
		((PlotCanvas)getCanvas()).setPlot(plot);
		this.plot = plot;
		draw();
	}
	
	/*
	 * EU_HOU CHANGES
	 */
    public PlotWindow(String title, String xLabel, String yLabel, double[] xValues, double[] yValues, final ImagePlus origin) {
        this(title, xLabel, yLabel, Tools.toFloat(xValues), Tools.toFloat(yValues));
        this.horizontal = false;
        this.origin = origin;
    }
	/*
	 * EU_HOU CHANGES END
	 */

	/** Called by the constructor to generate the image the plot will be drawn on.
		This is a static method because constructors cannot call instance methods. */
	static ImagePlus createImage(String title, String xLabel, String yLabel, float[] xValues, float[] yValues) {
		staticPlot = new Plot(title, xLabel, yLabel, xValues, yValues);
		return new ImagePlus(title, staticPlot.getBlankProcessor());
	}

	/** Sets the x-axis and y-axis range.
	 *  @deprecated use the corresponding method of the Plot class */
	public void setLimits(double xMin, double xMax, double yMin, double yMax) {
		plot.setLimits(xMin, xMax, yMin, yMax);
	}

	/** Adds a set of points to the plot or adds a curve if shape is set to LINE.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @param x			the x-coodinates
	 *  @param y			the y-coodinates
	 *  @param shape		Plot.CIRCLE, X, BOX, TRIANGLE, CROSS, LINE etc.
	 *  @deprecated use the corresponding method of the Plot class */
	public void addPoints(float[] x, float[] y, int shape) {
		plot.addPoints(x, y, shape);
	}
	
	/*
	 * EU_HOU ADD
	 */
	public void addPoints(float[] x, float[] y, int shape, Color color) {
		plot.addPoints(x, y, shape, color);
	}
	/*
	 * EU_HOU ADD END
	 */

	/** Adds a set of points to the plot using double arrays.
	 *	Must be called before the plot is displayed.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void addPoints(double[] x, double[] y, int shape) {
		addPoints(Tools.toFloat(x), Tools.toFloat(y), shape);
	}

	/** Adds vertical error bars to the plot.
	 *	Must be called before the plot is displayed.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void addErrorBars(float[] errorBars) {
		plot.addErrorBars(errorBars);
	}

	/** Draws a label.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void addLabel(double x, double y, String label) {
		plot.addLabel(x, y, label);
	}

	/** Changes the drawing color. The frame and labels are
	 *	always drawn in black.
	 *	Must be called before the plot is displayed.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void setColor(Color c) {
		plot.setColor(c);
	}

	/** Changes the line width.
	 *	Must be called before the plot is displayed.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void setLineWidth(int lineWidth) {
		plot.setLineWidth(lineWidth);
	}

	/** Changes the font.
	 *	Must be called before the plot is displayed.
	 *	Note that there are more options available by using the methods of the Plot class instead.
	 *  @deprecated use the corresponding method of the Plot class */
	public void changeFont(Font font) {
		plot.changeFont(font);
	}

	/** Displays the plot. */
	public void draw() {
		Panel bottomPanel = new Panel();
		int hgap = IJ.isMacOSX()?1:5;

		list = new Button(" List ");
		list.addActionListener(this);
		bottomPanel.add(list);
		bottomPanel.setLayout(new FlowLayout(FlowLayout.RIGHT,hgap,0));
		data = new Button(dataButtonLabel);
		data.addActionListener(this);
		bottomPanel.add(data);
		more = new Button(moreButtonLabel);
		more.addActionListener(this);
		bottomPanel.add(more);
		if (plot!=null && plot.getPlotMaker()!=null) {
			live = new Button("Live");
			live.addActionListener(this);
			bottomPanel.add(live);
		}
		statusLabel = new Label();
		statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
		statusLabel.setBackground(new Color(220, 220, 220));
		bottomPanel.add(statusLabel);
		add(bottomPanel);
		data.add(getDataPopupMenu());
		more.add(getMorePopupMenu());
		plot.draw();
		LayoutManager lm = getLayout();
		if (lm instanceof ImageLayout)
			((ImageLayout)lm).ignoreNonImageWidths(true);  //don't expand size to make the panel fit
		GUI.scale(bottomPanel);
		maximizeCoordinatesLabelWidth();
		pack();

		ImageProcessor ip = plot.getProcessor();
		boolean ipIsColor = ip instanceof ColorProcessor;
		boolean impIsColor = imp.getProcessor() instanceof ColorProcessor;
		if (ipIsColor != impIsColor)
			imp.setProcessor(null, ip);
		else
			imp.updateAndDraw();
		if (listValues)
			showList(/*useLabels=*/false);
		else
			ic.requestFocus();	//have focus on the canvas, not the button, so that pressing the space bar allows panning
	}

	/** Sets the Plot object shown in this PlotWindow. Does not update the window. */
	public void setPlot(Plot plot) {
		this.plot = plot;
		((PlotCanvas)getCanvas()).setPlot(plot);
	}

	/** Releases the resources used by this PlotWindow */
	public void dispose() {
		if (plot!=null)
			plot.dispose();
		disableLivePlot();
		plot = null;
		plotMaker = null;
		srcImp = null;
		super.dispose();
	}

	/** Called when the window is activated (WindowListener)
	 *  Window layout is finished at latest a few millisec after windowActivated, then the
	 *  'wasActivated' boolean is set to tell the ImageCanvas that resize events should
	 *  lead to resizing the canvas (before, creating the layout can lead to resize events)*/
	public void windowActivated(WindowEvent e) {
		super.windowActivated(e);
		if (!wasActivated) {
			new Thread(new Runnable() {
				public void run() {
					IJ.wait(50);  //sometimes, window layout is done only a few millisec after windowActivated
					wasActivated = true;
				}
			}).start();
		}
	}

	/** Called when the canvas is resized */
	void canvasResized() {
		if (plot == null) return;
		/*Dimension d1 = getExtraSize();
		Dimension d2 = plot.getMinimumSize();
		setMinimumSize(new Dimension(d1.width + d2.width, d1.height + d2.height));*/
		maximizeCoordinatesLabelWidth();
	}

	/** Maximizes the width for the coordinate&status readout field and its parent bottomPanel */
	void maximizeCoordinatesLabelWidth() {
		Insets insets = getInsets();                    //by default, left & right insets are 0 anyhow
		Component parent = statusLabel.getParent();     //the bottomPanel, has insets of 0
		if (!parent.isValid()) parent.validate();
		int cWidth = getWidth() - 2*HGAP - statusLabel.getX() - insets.left - insets.right;
		int cHeight = statusLabel.getPreferredSize().height;
		statusLabel.setPreferredSize(new Dimension(cWidth, cHeight));
		parent.setSize(getWidth() - 2*HGAP, parent.getHeight());
	}

	/** Shows the text in the coordinate&status readout field at the bottom.
	 *  This text may get temporarily replaced for 'tooltips' (mouse over range arrows etc.).
	 *  Call with a null argument to enable coordinate readout again. */
	public void showStatus(String text) {
		userStatusText = text;
		if (statusLabel != null)
			statusLabel.setText(text == null ? "" : text);
	}

	/** Names for popupMenu items. Update NUM_MENU_ITEMS at the top when adding new ones! */
	private static int SAVE=0, COPY=1, COPY_ALL=2, LIST_SIMPLE=3, ADD_FROM_TABLE=4, ADD_FROM_PLOT=5, ADD_FIT=6, //data menu
			SET_RANGE=7, PREV_RANGE=8, RESET_RANGE=9, FIT_RANGE=10,  //the rest is in the more menu
			ZOOM_SELECTION=11, AXIS_OPTIONS=12, LEGEND=13, STYLE=14, TEMPLATE=15, RESET_PLOT=16,
			FREEZE=17, HI_RESOLUTION=18, PROFILE_PLOT_OPTIONS=19;
	//the following commands are disabled when the plot is frozen
	private static int[] DISABLED_WHEN_FROZEN = new int[]{ADD_FROM_TABLE, ADD_FROM_PLOT, ADD_FIT,
			SET_RANGE, PREV_RANGE, RESET_RANGE, FIT_RANGE, ZOOM_SELECTION, AXIS_OPTIONS, LEGEND, STYLE, RESET_PLOT};

	/** Prepares and returns the popupMenu of the Data>> button */
	PopupMenu getDataPopupMenu() {
		dataPopupMenu = new PopupMenu();
		GUI.scalePopupMenu(dataPopupMenu);
		menuItems[SAVE] = addPopupItem(dataPopupMenu, "Save Data...");
		menuItems[COPY] = addPopupItem(dataPopupMenu, "Copy 1st Data Set");
		menuItems[COPY_ALL] = addPopupItem(dataPopupMenu, "Copy All Data");
		menuItems[LIST_SIMPLE] = addPopupItem(dataPopupMenu, "List (Simple Headings)");
		dataPopupMenu.addSeparator();
		menuItems[ADD_FROM_TABLE] = addPopupItem(dataPopupMenu, "Add from Table...");
		menuItems[ADD_FROM_PLOT] = addPopupItem(dataPopupMenu, "Add from Plot...");
		menuItems[ADD_FIT] = addPopupItem(dataPopupMenu, "Add Fit...");
		return dataPopupMenu;
	}

	/** Prepares and returns the popupMenu of the More>> button */
	PopupMenu getMorePopupMenu() {
		morePopupMenu = new PopupMenu();
		GUI.scalePopupMenu(morePopupMenu);
		menuItems[SET_RANGE] = addPopupItem(morePopupMenu, "Set Range...");
		menuItems[PREV_RANGE] = addPopupItem(morePopupMenu, "Previous Range");
		menuItems[RESET_RANGE] = addPopupItem(morePopupMenu, "Reset Range");
		menuItems[FIT_RANGE] = addPopupItem(morePopupMenu, "Set Range to Fit All");
		menuItems[ZOOM_SELECTION] = addPopupItem(morePopupMenu, "Zoom to Selection");
		morePopupMenu.addSeparator();
		menuItems[AXIS_OPTIONS] = addPopupItem(morePopupMenu, "Axis Options...");
		menuItems[LEGEND] = addPopupItem(morePopupMenu, "Legend...");
		menuItems[STYLE] = addPopupItem(morePopupMenu, "Contents Style...");
		menuItems[TEMPLATE] = addPopupItem(morePopupMenu, "Use Template...");
		menuItems[RESET_PLOT] = addPopupItem(morePopupMenu, "Reset Format");
		menuItems[FREEZE] = addPopupItem(morePopupMenu, "Freeze Plot", true);
		menuItems[HI_RESOLUTION] = addPopupItem(morePopupMenu, "High-Resolution Plot...");
		morePopupMenu.addSeparator();
		menuItems[PROFILE_PLOT_OPTIONS] = addPopupItem(morePopupMenu, "Plot Defaults...");
		return morePopupMenu;
	}

	MenuItem addPopupItem(PopupMenu popupMenu, String s) {
		return addPopupItem(popupMenu, s, false);
	}

	MenuItem addPopupItem(PopupMenu popupMenu, String s, boolean isCheckboxItem) {
		MenuItem mi = null;
		if (isCheckboxItem) {
			mi = new CheckboxMenuItem(s);
			((CheckboxMenuItem)mi).addItemListener(this);
		} else {
			mi = new MenuItem(s);
			mi.addActionListener(this);
		}
		popupMenu.add(mi);
		return mi;
	}

	/** Called if user has activated a button or popup menu item */
	public void actionPerformed(ActionEvent e) {
		try {
		Object b = e.getSource();
		if (b==live)
			toggleLiveProfiling();
		else if (b==list)
			showList(/*useLabels=*/true);
		else if (b==data) {
			enableDisableMenuItems();
			dataPopupMenu.show((Component)b, 1, 1);
		} else if (b==more) {
			enableDisableMenuItems();
			morePopupMenu.show((Component)b, 1, 1);
		} else if (b==menuItems[SAVE])
			saveAsText();
		else if (b==menuItems[COPY])
			copyToClipboard(false);
		else if (b==menuItems[COPY_ALL])
			copyToClipboard(true);
		else if (b==menuItems[LIST_SIMPLE])
			showList(/*useLabels=*/false);
		else if (b==menuItems[ADD_FROM_TABLE])
			new PlotContentsDialog(plot, PlotContentsDialog.ADD_FROM_TABLE).showDialog(this);
		else if (b==menuItems[ADD_FROM_PLOT])
			new PlotContentsDialog(plot, PlotContentsDialog.ADD_FROM_PLOT).showDialog(this);
		else if (b==menuItems[ADD_FIT])
			new PlotContentsDialog(plot, PlotContentsDialog.ADD_FIT).showDialog(this);
		else if (b==menuItems[ZOOM_SELECTION]) {
			if (imp!=null && imp.getRoi()!=null && imp.getRoi().isArea())
				plot.zoomToRect(imp.getRoi().getBounds());
		} else if (b==menuItems[SET_RANGE])
			new PlotDialog(plot, PlotDialog.SET_RANGE).showDialog(this);
		else if (b==menuItems[PREV_RANGE])
			plot.setPreviousMinMax();
		else if (b==menuItems[RESET_RANGE])
			plot.setLimitsToDefaults(true);
		else if (b==menuItems[FIT_RANGE])
			plot.setLimitsToFit(true);
		else if (b==menuItems[AXIS_OPTIONS])
			new PlotDialog(plot, PlotDialog.AXIS_OPTIONS).showDialog(this);
		else if (b==menuItems[LEGEND])
			new PlotDialog(plot, PlotDialog.LEGEND).showDialog(this);
		else if (b==menuItems[STYLE])
			new PlotContentsDialog(plot, PlotContentsDialog.STYLE).showDialog(this);
		else if (b==menuItems[TEMPLATE])
			new PlotDialog(plot, PlotDialog.TEMPLATE).showDialog(this);
		else if (b==menuItems[RESET_PLOT]) {
			plot.setFont(Font.PLAIN, fontSize);
			plot.setAxisLabelFont(Font.PLAIN, fontSize);
			plot.setFormatFlags(Plot.getDefaultFlags());
			plot.setFrameSize(plotWidth, plotHeight); //updates the image only when size changed
			plot.updateImage();
		} else if (b==menuItems[HI_RESOLUTION])
			new PlotDialog(plot, PlotDialog.HI_RESOLUTION).showDialog(this);
		else if (b==menuItems[PROFILE_PLOT_OPTIONS])
			IJ.doCommand("Plots...");
		ic.requestFocus();	//have focus on the canvas, not the button, so that pressing the space bar allows panning
		} catch (Exception ex) { IJ.handleException(ex); }
	}

	private void enableDisableMenuItems() {
		boolean frozen = plot.isFrozen();	//prepare menu according to 'frozen' state of plot
		((CheckboxMenuItem)menuItems[FREEZE]).setState(frozen);
		for (int i : DISABLED_WHEN_FROZEN)
			menuItems[i].setEnabled(!frozen);
		if (!PlotContentsDialog.tableWindowExists())
			menuItems[ADD_FROM_TABLE].setEnabled(false);
		if (plot.getDataObjectDesignations().length == 0)
			menuItems[ADD_FIT].setEnabled(false);
	}

	/** Called if the user activates/deactivates a CheckboxMenuItem */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource()==menuItems[FREEZE]) {
			boolean frozen = ((CheckboxMenuItem)menuItems[FREEZE]).getState();
			plot.setFrozen(frozen);
		}
	}

	/**
	 * Updates the X and Y values when the mouse is moved and, if appropriate,
	 * shows/hides the overlay with the triangular buttons for changing the axis
	 * range limits.
	 * Overrides mouseMoved() in ImageWindow.
	 *
	 * @see ij.gui.ImageWindow#mouseMoved
	 */
	public void mouseMoved(int x, int y) {
		super.mouseMoved(x, y);
		if (plot == null)
			return;
		String statusText = null; //coordinate readout, status or tooltip, will be shown in coordinate&status line

		//arrows and other symbols for modifying the plot range
		if (x < plot.leftMargin || y > plot.topMargin + plot.frameHeight) {
			if (!rangeArrowsVisible && !plot.isFrozen())
				showRangeArrows();
			if (activeRangeArrow < 0)       //mouse is not on one of the symbols, ignore (nothing to display)
				{}
			else if (activeRangeArrow < 8)  //mouse over an arrow: 0,3,4,7 for increase, 1,2,5,6 for decrease
				statusText = ((activeRangeArrow+1)&0x02) != 0 ? "Decrease Range" : "Increase Range";
			else if (activeRangeArrow == 8) //it's the 'R' icon
				statusText = "Reset Range";
			else if (activeRangeArrow == 9) //it's the 'F' icon
				statusText = "Full Range (Fit All)";
			else if (activeRangeArrow >= 10 &&
					activeRangeArrow < 14)  //space between arrow-pairs for single number
				statusText = "Set limit...";
			else if (activeRangeArrow >= 14)
				statusText = "Axis Range & Options...";
			boolean repaint = false;
			if (activeRangeArrow >= 0 && !rangeArrowRois[activeRangeArrow].contains(x, y)) {
				rangeArrowRois[activeRangeArrow].setFillColor(
						activeRangeArrow < 10 ? inactiveRangeArrowColor : inactiveRangeRectColor);
				repaint = true;             //de-highlight arrow where cursor has moved out
				activeRangeArrow = -1;
			}
			if (activeRangeArrow < 0) {     //no currently highlighted arrow, do we have a new one?
				int i = getRangeArrowIndex(x, y);
				if (i >= 0) {               //we have an arrow or symbol at cursor position
					rangeArrowRois[i].setFillColor(
							i < 14 ? activeRangeArrowColor : activeRangeRectColor);
					activeRangeArrow = i;
					repaint = true;
				}
			}
			if (repaint) ic.repaint();
		} else if (rangeArrowsVisible)
			hideRangeArrows();

		if (statusText == null)
			statusText = userStatusText != null ? userStatusText : plot.getCoordinates(x, y);
		if (statusLabel != null)
			statusLabel.setText(statusText);
	}

	/** Called by PlotCanvas */
	void mouseExited(MouseEvent e) {
		if (rangeArrowsVisible)
			hideRangeArrows();
	}

	/** Mouse wheel: zooms when shift or ctrl is pressed, scrolls in x if space bar down, in y otherwise. */
	public synchronized void mouseWheelMoved(MouseWheelEvent e) {
		if (plot.isFrozen() || !(ic instanceof PlotCanvas)) {	   //frozen plots are like normal images
			super.mouseWheelMoved(e);
			return;
		}
		int rotation = e.getWheelRotation();
		int amount = e.getScrollAmount();
		if (e.getX() < plot.leftMargin || e.getX() > plot.leftMargin + plot.frameWidth)//n__
			return;
		if (e.getY() < plot.topMargin || e.getY() > plot.topMargin + plot.frameHeight)
			return;
		boolean ctrl = (e.getModifiers()&Event.CTRL_MASK)!=0;
		if (amount<1) amount=1;
		if (rotation==0)
			return;
		if (ctrl||IJ.shiftKeyDown()) {
			double zoomFactor = rotation<0 ? Math.pow(2, 0.2) : Math.pow(0.5, 0.2);
			Point loc = ic.getCursorLoc();
			int x = ic.screenX(loc.x);
			int y = ic.screenY(loc.y);
			((PlotCanvas)ic).zoom(x, y, zoomFactor);
		} else if (IJ.spaceBarDown())
			plot.scroll(rotation*amount*Math.max(ic.imageWidth/50, 1), 0);
		else
			plot.scroll(0, rotation*amount*Math.max(ic.imageHeight/50, 1));
	}

    /**
     * Creates an overlay with triangular buttons and othr symbols for changing the axis range
     * limits and shows it
     */
    void showRangeArrows() {
        if (imp == null)
            return;
        hideRangeArrows(); //in case we have old arrows from a different plot size or so
        rangeArrowRois = new Roi[4 * 2 + 2 + 4 + 2]; //4 arrows per axis, + 'Reset' and 'Fit All' icons, + 4 numerical input boxes + 2 axes
        int i = 0;
        int height = imp.getHeight();
        int arrowH = plot.topMargin < 14 ? 6 : 8; //height of arrows and distance between them; base is twice that value
        float[] yP = new float[]{height - arrowH / 2, height - 3 * arrowH / 2, height - 5 * arrowH / 2 - 0.1f};

        for (float x : new float[]{plot.leftMargin, plot.leftMargin + plot.frameWidth}) { //create arrows for x axis
            float[] x0 = new float[]{x - arrowH / 2, x - 3 * arrowH / 2 - 0.1f, x - arrowH / 2};
            rangeArrowRois[i++] = new PolygonRoi(x0, yP, 3, Roi.POLYGON);
            float[] x1 = new float[]{x + arrowH / 2, x + 3 * arrowH / 2 + 0.1f, x + arrowH / 2};
            rangeArrowRois[i++] = new PolygonRoi(x1, yP, 3, Roi.POLYGON);
        }
        float[] xP = new float[]{arrowH / 2 - 0.1f, 3 * arrowH / 2, 5 * arrowH / 2 + 0.1f};
        for (float y : new float[]{plot.topMargin + plot.frameHeight, plot.topMargin}) { //create arrows for y axis
            float[] y0 = new float[]{y + arrowH / 2, y + 3 * arrowH / 2 + 0.1f, y + arrowH / 2};
            rangeArrowRois[i++] = new PolygonRoi(xP, y0, 3, Roi.POLYGON);
          float[] y1 = new float[]{y - arrowH / 2, y - 3 * arrowH / 2 - 0.1f, y - arrowH / 2};
            rangeArrowRois[i++] = new PolygonRoi(xP, y1, 3, Roi.POLYGON);
        }
        Font theFont = new Font("SansSerif", Font.BOLD, 13);

        TextRoi txtRoi = new TextRoi(1, height - 19, "\u2009R\u2009", theFont);  //thin spaces to make roi slightly wider
        rangeArrowRois[8] = txtRoi;
        TextRoi txtRoi2 = new TextRoi(20, height - 19, "\u2009F\u2009", theFont);
        rangeArrowRois[9] = txtRoi2;

		rangeArrowRois[10] = new Roi(plot.leftMargin - arrowH/2 + 1, height - 5 * arrowH / 2, arrowH - 2, arrowH * 2);//numerical box left
		rangeArrowRois[11] = new Roi(plot.leftMargin + plot.frameWidth - arrowH/2 + 1, height - 5 * arrowH / 2, arrowH - 2, arrowH * 2);//numerical box right
        rangeArrowRois[12] = new Roi(arrowH / 2, plot.topMargin + plot.frameHeight - arrowH/2 + 1, arrowH * 2, arrowH -2);//numerical box bottom
        rangeArrowRois[13] = new Roi(arrowH / 2, plot.topMargin - arrowH/2 + 1,  arrowH * 2, arrowH - 2   );//numerical box top

        int topMargin = plot.topMargin;
        int bottomMargin = topMargin + plot.frameHeight;
        int leftMargin = plot.leftMargin;
        int rightMargin = plot.leftMargin + plot.frameWidth;
        rangeArrowRois[14] = new Roi(leftMargin, bottomMargin+2,        // area to click for x axis options
				rightMargin - leftMargin + 1, 2*arrowH);
        rangeArrowRois[15] = new Roi(leftMargin-2*arrowH-2, topMargin,  // area to click for y axis options
				2*arrowH, bottomMargin - topMargin + 1);

        Overlay ovly = imp.getOverlay();
        if (ovly == null)
            ovly = new Overlay();
        for (Roi roi : rangeArrowRois) {
            if (roi instanceof PolygonRoi)
                   roi.setFillColor(inactiveRangeArrowColor);
			else if (roi instanceof TextRoi) {
                roi.setStrokeColor(Color.WHITE);
                roi.setFillColor(inactiveRangeArrowColor);
            } else
                roi.setFillColor(inactiveRangeRectColor); //transparent gray for single number boxes and axis range
            ovly.add(roi);
        }
        imp.setOverlay(ovly);
        ic.repaint();
        rangeArrowsVisible = true;
    }

	void hideRangeArrows() {
		if (imp == null || rangeArrowRois==null) return;
		Overlay ovly = imp.getOverlay();
		if (ovly == null) return;
		for (Roi roi : rangeArrowRois)
			ovly.remove(roi);
		ic.repaint();
		rangeArrowsVisible = false;
		activeRangeArrow = -1;
	}

	/** Returns the index of the range-modifying symbol or axis at the
	 *  cursor position x,y, or -1 of none.
	 *  Index numbers for arrows start with 0 at the 'down' arrow of the
	 *  lower side of the x axis and end with 7 the up arrow at the upper
	 *  side of the y axis. Numbers 8 & 9 are for "Reset Range" and "Fit All";
	 *  numbers 10-13 for a dialog to set a single limit, and 14-15 for the axis options. */

	int getRangeArrowIndex(int x, int y) {
		if (!rangeArrowsVisible) return -1;
		for (int i=0; i<rangeArrowRois.length; i++)
			if (rangeArrowRois[i].getBounds().contains(x,y))
				return i;
		return -1;
	}


	/** Shows the data of the backing plot in a Textwindow with columns */
	void showList(boolean useLabels){
		ResultsTable rt = plot.getResultsTable(saveXValues, useLabels);
		if (rt==null) return;
		rt.show("Plot Values");
		if (autoClose) {
			imp.changes=false;
			close();
		}
	}

	/** Returns the plot values with simple headings (X, Y, Y1 etc, not the labels) as a ResultsTable.
	 *  Use plot.getResultsTableWithLabels for a table with data set labels as column headings */
	public ResultsTable getResultsTable() {
		return plot.getResultsTable(saveXValues);
	}

	/** creates the data that fills the showList() function values */
	private String getValuesAsString(){
		ResultsTable rt = getResultsTable();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<rt.size(); i++) {
			sb.append(rt.getRowAsString(i));
			sb.append("\n");
		}
		return sb.toString();
	}

	/** Saves the data of the plot in a text file */
	void saveAsText() {
		if (plot.getXValues() == null) {
			IJ.error("Plot has no data");
			return;
		}
		SaveDialog sd = new SaveDialog("Save as Text", "Values", Prefs.defaultResultsExtension());
		String name = sd.getFileName();
		if (name==null) return;
		String directory = sd.getDirectory();
		IJ.wait(250);  // give system time to redraw ImageJ window
		IJ.showStatus("Saving plot values...");
		ResultsTable rt = plot.getResultsTable(/*writeFirstXColumn=*/saveXValues, /*useLabels=*/true);
		try {
			rt.saveAs(directory+name);
		} catch (IOException e) {
			IJ.error("" + e);
			return;
		}
		if (autoClose)
			{imp.changes=false; close();}
	}

	/** Copy the first dataset or all values to the clipboard */
	void copyToClipboard(boolean writeAllColumns) {
		float[] xValues = plot.getXValues();
		float[] yValues = plot.getYValues();
		if (xValues == null) return;
		Clipboard systemClipboard = null;
		try {systemClipboard = getToolkit().getSystemClipboard();}
		catch (Exception e) {systemClipboard = null; }
		if (systemClipboard==null)
			{IJ.error("Unable to copy to Clipboard."); return;}
		IJ.showStatus("Copying plot values...");
		CharArrayWriter aw = new CharArrayWriter(10*xValues.length);
		PrintWriter pw = new PrintWriter(aw); //uses platform's line termination characters

		if (writeAllColumns) {
			ResultsTable rt = plot.getResultsTableWithLabels();
			if (!Prefs.dontSaveHeaders) {
				String headings = rt.getColumnHeadings();
				pw.println(headings);
			}
			for (int i=0; i<rt.size(); i++)
				pw.println(rt.getRowAsString(i));
		} else {
			int xdigits = 0;
			if (saveXValues)
				xdigits = plot.getPrecision(xValues);
			int ydigits = plot.getPrecision(yValues);
			for (int i=0; i<Math.min(xValues.length, yValues.length); i++) {
				if (saveXValues)
					pw.println(IJ.d2s(xValues[i],xdigits)+"\t"+IJ.d2s(yValues[i],ydigits));
				else
					pw.println(IJ.d2s(yValues[i],ydigits));
			}
		}
		String text = aw.toString();
		pw.close();
		StringSelection contents = new StringSelection(text);
		systemClipboard.setContents(contents, this);
		IJ.showStatus(text.length() + " characters copied to Clipboard");
		if (autoClose)
			{imp.changes=false; close();}
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents) {}

	public float[] getXValues() {
		return plot.getXValues();
	}

	public float[] getYValues() {
		return plot.getYValues();
	}

	/** Draws a new plot in this window. */
	public void drawPlot(Plot plot) {
		this.plot = plot;
		if (imp!=null) {
			if (ic instanceof PlotCanvas)
				((PlotCanvas)ic).setPlot(plot);
			imp.setProcessor(null, plot.getProcessor());
			plot.setImagePlus(imp); //also adjusts the calibration of imp
		}
	}

	/** Called once when ImageJ quits. */
	public static void savePreferences(Properties prefs) {
		double min = ProfilePlot.getFixedMin();
		double max = ProfilePlot.getFixedMax();
		prefs.put(PREFS_WIDTH, Integer.toString(plotWidth));
		prefs.put(PREFS_HEIGHT, Integer.toString(plotHeight));
		prefs.put(PREFS_FONT_SIZE, Integer.toString(defaultFontSize));
		int options = 0;
		if (!interpolate) options |= INTERPOLATE; // true=0, false=1
		prefs.put(OPTIONS, Integer.toString(options));
	}

	private void toggleLiveProfiling() {
		boolean liveMode = bgThread != null;
		if (liveMode)
			disableLivePlot();
		else
			enableLivePlot();
	}

	/* Enable live plotting.
	 * This requires that the PlotWindow has been initialized with a Plot having a PlotMaker */
	private void enableLivePlot() {
		if (plotMaker==null)
			plotMaker = plot!=null?plot.getPlotMaker():null;
		if (plotMaker==null) return;
		srcImp = plotMaker.getSourceImage();
		if (srcImp==null)
			return;
		if (bgThread==null) {
			bgThread = new Thread(this, "Live Plot");
			bgThread.setPriority(Math.max(bgThread.getPriority()-3, Thread.MIN_PRIORITY));
			doUpdate = true;
			bgThread.start();
		}
		if (IJ.debugMode) IJ.log("PlotWindow.createListeners");
		ImagePlus.addImageListener(this);
		Roi.addRoiListener(this);
		Font font = live.getFont();
		live.setFont(new Font(font.getName(), Font.BOLD, font.getSize()));
		live.setForeground(Color.red);
	}

	private void disableLivePlot() {
		if (IJ.debugMode) IJ.log("PlotWindow.disableLivePlot: "+srcImp);
		if (srcImp==null)
			return;
		if (bgThread!=null)
			bgThread.interrupt();
		bgThread = null;
		ImagePlus.removeImageListener(this);
		Roi.removeRoiListener(this);
		if (live != null) {
			Font font = live.getFont();
			live.setFont(new Font(font.getName(), Font.PLAIN, font.getSize()));
			live.setForeground(Color.black);
		}
	}


	/** For live plots, update the plot if the ROI of the source image changes */
	public synchronized void roiModified(ImagePlus img, int id) {
		if (IJ.debugMode) IJ.log("PlotWindow.roiModified: "+img+"  "+id);
		if (img==srcImp) {
			doUpdate=true;
			notify();
		}
	}

	// Unused
	public void imageOpened(ImagePlus imp) {
	}

	/** For live plots, this method is called if the source image content is changed. */
	public synchronized void imageUpdated(ImagePlus imp) {
		if (imp==srcImp) {
			doUpdate = true;
			notify();
		}
	}

	/** For live plots, if either the source image or this image are closed, exit live mode */
	public void imageClosed(ImagePlus imp) {
		if (imp==srcImp || imp==this.imp) {
			disableLivePlot();
			srcImp = null;
			plotMaker = null;
		}
	}

	// the background thread for live plotting.
	public void run() {
		while (true) {
			IJ.wait(50);	//delay to make sure the roi has been updated
			Plot plot = plotMaker!=null?plotMaker.getPlot():null;
			if (doUpdate && plot!=null && plot.getNumPlotObjects()>0) {
				plot.useTemplate(this.plot, this.plot.templateFlags | Plot.COPY_SIZE | Plot.COPY_LABELS | Plot.COPY_AXIS_STYLE |
						Plot.COPY_CONTENTS_STYLE | Plot.COPY_LEGEND | Plot.COPY_EXTRA_OBJECTS);
				plot.setPlotMaker(plotMaker);
				this.plot = plot;
				((PlotCanvas)ic).setPlot(plot);
				ImageProcessor ip = plot.getProcessor();
				if (ip!=null && imp!=null) {
					imp.setProcessor(null, ip);
					plot.setImagePlus(imp);
				}
			}
			synchronized(this) {
				if (doUpdate) {
					doUpdate = false;		//and loop again
				} else {
					try {wait();}	//notify wakes up the thread
					catch(InterruptedException e) { //interrupted tells the thread to exit
						return;
					}
				}
			}
		}
	}
	
	/*
	 * EU_HOU ADD
	 */
	// EU_HOU
		/**
		 *  Description of the Method
		 *
		 * @param  x  Description of the Parameter
		 * @param  y  Description of the Parameter
		 */
		public void updatePlot(int x, int y) {
			// EU_HOU
			//System.out.println("PlotWindow mousemoved 0");
			if (frame == null) {
				return;
			}

			int xOrigine = 0;

			//if ((region != null) && (horizontal)) {
			//	xOrigine = ((Line) region).getX();
			//}

			if (frame.contains(oldX - 1, oldY)) {
				//System.out.println("PlotWindow mousemoved 1");
				ip.setColor(Color.white);
				// EU_HOU
				if (imp != null) {
					Roi region = imp.getRoi();
					imp.setRoi(frame);
					ip.fill();
					imp.setRoi(region);
				}

				/*
				    ip.drawLine(oldX,frame.y ,oldX,frame.y+frame.height);
				    ip.drawLine(frame.x,Ypoints[oldindex],frame.x+frame.width,Ypoints[oldindex]);
				  */
				ip.setColor(Color.black);
				drawPolyline(ip, Xpoints, Ypoints, nPoints);
				ip.setColor(Color.black);
				ip.drawRect(frame.x, frame.y, frame.width + 1, frame.height + 1);
				ip.setAntialiasedText(true);
				Enumeration e = labels.elements();
				int i = 0;
				while (e.hasMoreElements()) {
					int xxx = ((Integer) xlabs.elementAt(i)).intValue();
					int yyy = ((Integer) ylabs.elementAt(i++)).intValue();
					ip.drawString((String) e.nextElement(), xxx, yyy);
				}
				// EU_HOU
				if (imp != null) {
					imp.updateAndDraw();
				}
			}
			if (frame.contains(x, y)) {
				//System.out.println("PlotWindow mousemoved 2");
				int index = (int) ((x - frame.x) / ((double) frame.width / xValues.length));
				ip.setColor(Color.gray.brighter());
				ip.drawLine(x, frame.y, x, frame.y + frame.height);
				ip.drawLine(frame.x, Ypoints[index], frame.x + frame.width, Ypoints[index]);
				ip.setColor(Color.black);

				ip.drawRect(frame.x, frame.y, frame.width + 1, frame.height + 1);
				// EU_HOU
				if (imp != null) {
					imp.updateAndDraw();
				}
				if (index > 0 && index < xValues.length) {
					double xv;
					xv = xValues[index];
					//if(horizontal) xv-= xOrigine;
					double yv = yValues[index];
					//ip.drawPixel( LEFT_MARGIN + (int)((xv-xMin)*xScale),TOP_MARGIN + frame.height - (int)((yv-yMin)*yScale));


					if ((origin != null) && (region != null) && (region.getType() == Roi.LINE)) {
						origin.getWindow().getCanvas().setRoi(region);
						Line lr = (Line) region;
						if (horizontal) {
							xv -= xOrigine;
						}
						// dessin du petit point dans l'image originale
						double c = xv / (origin.getRoi().getLength());
						int xi = lr.x1 + (int) (c * (lr.x2 - lr.x1) + 0.5);
						int yi = lr.y1 + (int) (c * (lr.y2 - lr.y1) + 0.5);
						origin.getWindow().getCanvas().setPoint(xi, yi, region);
					}
					oldindex = index;
				}
				//coordinates.setText("X=" + d2s(x/xScale+xMin)+", Y=" + d2s((frameHeight-y)/yScale+yMin));
			}

			oldX = x;
			oldY = y;

			if (yValuesBaseLine != null) {
				addPoints(xValues, yValuesBaseLine, LINE, Color.red);
			}
			if (yValuesMergeGaussFit != null) {
				addPoints(xValues, yValuesMergeGaussFit, LINE, Color.red);
			}
			//displayGaussianFit();
			if (ZERO_LINE) {
				addPoints(xValues, new float[xValues.length], LINE, Color.blue);
			}
			drawTicksEtc();
		}
		// EU_HOU

		//EU_HOU
		/**
		 *  Description of the Method
		 */
		public void killOverlayOrigin() {
			if ((origin != null) && (region != null) && (region.getType() == Roi.LINE)) {

				origin.getWindow().getCanvas().setPoint(-1, -1, null);
			}
		}
		//EU_HOU
	

void drawTick(ImageProcessor ip) {
    double taille = xMax - xMin;
    taille = java.lang.Math.abs(taille);
    double k = 1;
    double kk = 0;
    double dtmp;
    int itmp;
    while (kk != k) {
        kk = k;
        dtmp = taille / k;
        if (dtmp <= 5) {
            k = k / 10;
        }
        if (dtmp >= 100) {
            k = k * 10;
        }
    }
    itmp = (int) java.lang.Math.round(xMin / k);
    double j = itmp * k;
    while (j < xMax) {
        if ((j > xMin) && (j < xMax)) {
            int n = 2;
            dtmp = j / k;
            itmp = (int) dtmp;
            itmp = itmp % 10;
            if (itmp == 0) {
                n = 6;
            }
            ip.moveTo(LEFT_MARGIN + (int) ((j - xMin) * xScale), TOP_MARGIN + frame.height - n);
            ip.lineTo(LEFT_MARGIN + (int) ((j - xMin) * xScale), TOP_MARGIN + frame.height);
        }
        j += k;
    }
    taille = java.lang.Math.abs(yMax - yMin);
    k = 1;
    kk = 0;
    while (kk != k) {
        kk = k;
        dtmp = taille / k;
        if (dtmp <= 5) {
            k = k / 10;
        }
        if (dtmp >= 100) {
            k = k * 10;
        }
    }
    itmp = (int) java.lang.Math.round(yMin / k);
    j = itmp * k;
    while (j < yMax) {
        if ((j > yMin) && (j < yMax)) {
            int n = 2;
            dtmp = j / k;
            itmp = (int) dtmp;
            itmp = itmp % 10;
            if (itmp == 0) {
                n = 6;
            }
            ip.moveTo(LEFT_MARGIN, TOP_MARGIN + frame.height - (int) ((j - yMin) * yScale));
            ip.lineTo(LEFT_MARGIN + n, TOP_MARGIN + frame.height - (int) ((j - yMin) * yScale));
        }
        j += k;
    }
}

/**
 * dialog allow to Change the values or labels of the two axis.
 *
 */
double niceNumber(double v) {//the smallest "nice" number >= v. "Nice" numbers are .. 0.5, 1, 2, 5, 10, 20 ...
    double base = Math.pow(10, Math.floor(Math.log(v) / Math.log(10) - 1.e-6));
    if (v > 5.0000001 * base) {
        return 10 * base;
    } else if (v > 2.0000001 * base) {
        return 5 * base;
    } else {
        return 2 * base;
    }
}

void drawTicksEtc() {
    int fontAscent = ip.getFontMetrics().getAscent();
    int fontMaxAscent = ip.getFontMetrics().getMaxAscent();
    if ((flags & (X_NUMBERS + X_TICKS + X_GRID)) != 0) {
        double step = Math.abs(Math.max(frame.width / MAX_INTERVALS, MIN_X_GRIDWIDTH) / xScale);//the smallest allowable increment
        step = niceNumber(step);
        int i1;
        int i2;
        if ((flags & X_FORCE2GRID) != 0) {
            i1 = (int) Math.floor(Math.min(xMin, xMax) / step + 1.e-10);//this also allows for inverted xMin, xMax
            i2 = (int) Math.ceil(Math.max(xMin, xMax) / step - 1.e-10);
            xMin = i1 * step;
            xMax = i2 * step;
            xScale = frame.width / (xMax - xMin);//rescale to make it fit
        } else {
            i1 = (int) Math.ceil(Math.min(xMin, xMax) / step - 1.e-10);
            i2 = (int) Math.floor(Math.max(xMin, xMax) / step + 1.e-10);
        }
        int digits = -(int) Math.floor(Math.log(step) / Math.log(10) + 1e-6);
        if (digits < 0) {
            digits = 0;
        }
        int y1 = TOP_MARGIN;
        int y2 = TOP_MARGIN + frame.height;
        int yNumbers = y2 + fontAscent + 7;
        for (int i = 0; i <= (i2 - i1); i++) {
            double v = (i + i1) * step;
            int x = (int) Math.round((v - xMin) * xScale) + LEFT_MARGIN;
            if ((flags & X_GRID) != 0) {
                ip.setColor(gridColor);
                ip.drawLine(x, y1, x, y2);
                ip.setColor(Color.black);
            }
            if ((flags & X_TICKS) != 0) {
                ip.drawLine(x, y1, x, y1 + TICK_LENGTH);
                ip.drawLine(x, y2, x, y2 - TICK_LENGTH);
            }
            if ((flags & X_NUMBERS) != 0) {
                String s = IJ.d2s(v, digits);
                ip.drawString(s, x - ip.getStringWidth(s) / 2, yNumbers);
            }
        }
    }
    int maxNumWidth = 0;
    if ((flags & (Y_NUMBERS + Y_TICKS + Y_GRID)) != 0) {
        double step = Math.abs(Math.max(frame.width / MAX_INTERVALS, MIN_Y_GRIDWIDTH) / yScale);//the smallest allowable increment
        step = niceNumber(step);
        int i1;
        int i2;
        if ((flags & X_FORCE2GRID) != 0) {
            i1 = (int) Math.floor(Math.min(yMin, yMax) / step + 1.e-10);//this also allows for inverted xMin, xMax
            i2 = (int) Math.ceil(Math.max(yMin, yMax) / step - 1.e-10);
            yMin = i1 * step;
            yMax = i2 * step;
            yScale = frame.height / (yMax - yMin);//rescale to make it fit
        } else {
            i1 = (int) Math.ceil(Math.min(yMin, yMax) / step - 1.e-10);
            i2 = (int) Math.floor(Math.max(yMin, yMax) / step + 1.e-10);
        }
        int digits = -(int) Math.floor(Math.log(step) / Math.log(10) + 1e-6);
        if (digits < 0) {
            digits = 0;
        }
        int x1 = LEFT_MARGIN;
        int x2 = LEFT_MARGIN + frame.width;
        for (int i = 0; i <= (i2 - i1); i++) {
            double v = (i + i1) * step;
            int y = TOP_MARGIN + frame.height - (int) Math.round((v - yMin) * yScale);
            if ((flags & Y_GRID) != 0) {
                ip.setColor(gridColor);
                ip.drawLine(x1, y, x2, y);
                ip.setColor(Color.black);
            }
            if ((flags & Y_TICKS) != 0) {
                ip.drawLine(x1, y, x1 + TICK_LENGTH, y);
                ip.drawLine(x2, y, x2 - TICK_LENGTH, y);
            }
            if ((flags & Y_NUMBERS) != 0) {
                String s = IJ.d2s(v, digits);
                int w = ip.getStringWidth(s);
                if (w > maxNumWidth) {
                    maxNumWidth = w;
                }
                ip.drawString(s, LEFT_MARGIN - w - 4, y + fontMaxAscent / 2 + 1);
            }
        }
    }
    if ((flags & Y_NUMBERS) == 0) {//simply note y-axis min&max
        int digits = getDigits(yMin, yMax);
        String s = IJ.d2s(yMax, digits);
        int sw = ip.getStringWidth(s);
        if ((sw + 4) > LEFT_MARGIN) {
            ip.drawString(s, 4, TOP_MARGIN - 4);
        } else {
            ip.drawString(s, LEFT_MARGIN - ip.getStringWidth(s) - 4, TOP_MARGIN + 10);
        }
        s = IJ.d2s(yMin, digits);
        sw = ip.getStringWidth(s);
        if ((sw + 4) > LEFT_MARGIN) {
            ip.drawString(s, 4, TOP_MARGIN + frame.height);
        } else {
            ip.drawString(s, LEFT_MARGIN - ip.getStringWidth(s) - 4, TOP_MARGIN + frame.height);
        }
    }
    FontMetrics fm = ip.getFontMetrics();
    int x = LEFT_MARGIN;
    int y = TOP_MARGIN + frame.height + fm.getAscent() + 6;
    if ((flags & X_NUMBERS) == 0) {//simply note x-axis min&max
        int digits = getDigits(xMin, xMax);
        ip.drawString(IJ.d2s(xMin, digits), x, y);
        String s = IJ.d2s(xMax, digits);
        ip.drawString(s, x + frame.width - ip.getStringWidth(s) + 6, y);
    } else {
        y += fm.getAscent();
    }//space needed for x numbers
    drawXLabel(xLabel, 270, 235, 0, fm);
    //ip.drawString(xLabel, LEFT_MARGIN + (frame.width - ip.getStringWidth(xLabel)) / 2, y + 6);
    drawYLabel(yLabel, LEFT_MARGIN - maxNumWidth - 4, TOP_MARGIN, frame.height, fm);
}

int getDigits(double n1, double n2) {
    if (Math.round(n1) == n1 && Math.round(n2) == n2) {
        return 0;
    } else {
        n1 = Math.abs(n1);
        n2 = Math.abs(n2);
        double n = n1 < n2 && n1 > 0.0 ? n1 : n2;
        double diff = Math.abs(n2 - n1);
        if (diff > 0.0 && diff < n) {
            n = diff;
        }
        int digits = 1;
        if (n < 10.0) {
            digits = 2;
        }
        if (n < 0.01) {
            digits = 3;
        }
        if (n < 0.001) {
            digits = 4;
        }
        if (n < 0.0001) {
            digits = 5;
        }
        return digits;
    }
}

void drawXLabel(String yLabel, int x, int y, int height, FontMetrics fm) {
    if (yLabel.equals("")) {
        return;
    }
    int w = fm.stringWidth(yLabel) + 5;
    int h = fm.getHeight() + 5;
    ImageProcessor label = new ByteProcessor(w, h);
    label.setColor(Color.white);
    label.fill();
    label.setColor(Color.black);
    label.setFont(font);
    label.setAntialiasedText(true);
    int descent = fm.getDescent();
    label.drawString(yLabel, 0, h - descent);
    int y2 = y + (height - ip.getStringWidth(yLabel)) / 2;
    if (y2 < y) {
        y2 = y;
    }
    int x2 = Math.max(x - h, 0);
    ip.insert(label, x2, y2);
}

void drawPolyline(ImageProcessor ip, int[] x, int[] y, int n) {
    ip.moveTo(x[0], y[0]);
    for (int i = 0; i < n; i++) {
        ip.lineTo(x[i], y[i]);
    }
}

void drawYLabel(String yLabel, int x, int y, int height, FontMetrics fm) {
    if (yLabel.equals("")) {
        return;
    }
    int w = fm.stringWidth(yLabel) + 5;
    int h = fm.getHeight() + 5;
    ImageProcessor label = new ByteProcessor(w, h);
    label.setColor(Color.white);
    label.fill();
    label.setColor(Color.black);
    label.setFont(font);
    label.setAntialiasedText(true);
    int descent = fm.getDescent();
    label.drawString(yLabel, 0, h - descent);
    label = label.rotateLeft();
    int y2 = y + (height - ip.getStringWidth(yLabel)) / 2;
    if (y2 < y) {
        y2 = y;
    }
    int x2 = x - h - 2;
    //new ImagePlus("after", label).show();
    ip.insert(label, x2, y2);
}

	/*
	 * EU_HOU ADD END
	 */

	/** Returns the Plot associated with this PlotWindow. */
	public Plot getPlot() {
		return plot;
	}

	/** Freezes the active plot window, so the image does not get redrawn for zooming,
	 *  setting the range, etc. */
	public static void freeze() {
		Window win = WindowManager.getActiveWindow();
		if (win!=null && (win instanceof PlotWindow))
			((PlotWindow)win).getPlot().setFrozen(true);
	}
	
	public static void setDefaultFontSize(int size) {
		if (size < 9) size = 9;
		if (size > 36) size = 36;
		defaultFontSize = size;
	}

	public static int getDefaultFontSize() {
		return defaultFontSize;
	}


}
