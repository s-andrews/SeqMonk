/**
 * Copyright 2009- 21 Simon Andrews
 *
 *    This file is part of SeqMonk.
 *
 *    SeqMonk is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    SeqMonk is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with SeqMonk; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package uk.ac.babraham.SeqMonk.Displays.VariancePlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SmoothedVarianceDataset;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Gradients.HotColdColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The actual variance plot panel which implements the selection of
 * regions.
 */
public class VariancePlotPanel extends JPanel implements Runnable, MouseMotionListener, MouseListener {

	private final static Color VERY_LIGHT_GREY = new Color(225, 225, 225);
	
	public static final int VARIANCE_STDEV = 52301;
	public static final int VARIANCE_SEM = 52302;
	public static final int VARIANCE_COEF = 52303;
	public static final int VARIANCE_QUARTILE_DISP = 52304;
	public static final int VARIANCE_NUMBER_UNMEASURED = 52305;
	
	
	/** The replicate set */
	private ReplicateSet repSet;

	private int varianceMeasure = VARIANCE_STDEV;
	
	/** The probe list. */
	private ProbeList probeList;

	/** The set of sublists to highlight */
	private ProbeList [] subLists;
	
	private SmoothedVarianceDataset smoothedTrend;
	
	/** The difference value at which we started the current selection */
	private double diffStart = 0;

	/** The difference value at which we ended the current selection */
	private double diffEnd = 0;

	/** Whether we're making a selection. */
	private boolean makingSelection = false;

	/** Whether there is a selection made which we need to render (as opposed to an active one) */
	private boolean madeSelection = false;

	/** The min value x. */
	private double minValueX = 0;

	/** The max value x. */
	private double maxValueX = 1;

	/** The min value y. */
	private double minValueY = 0;

	/** The max value y. */
	private double maxValueY = 1;

	/** The ready to draw. */
	protected boolean readyToDraw = false;

	private boolean noData = false;
	
	
	/** The dot size. */
	private int dotSize = 1;

	/** The df. */
	private final DecimalFormat df = new DecimalFormat("#.###");

	/** The pearsons r-value to display on the graph */
//	private String rValue;

	/**  Some points which say where the cursor is currently located */
	private int cursorX = 0;
	private int cursorY = 0;

	/** The last calculated non-redundant set of probe values **/
	private ProbePairValue [] nonRedundantValues = null;

	/** The last image size for which we calculated a nonredundant set **/
	private int lastNonredWidth = 0;
	private int lastNonredHeight = 0;

	/**
	 * The set of points to label
	 */
	private HashSet<ProbePairValue> labelledPoints = new HashSet<ProbePairValue>();  

	
	/**
	 * The last ProbePairValue we were near
	 */
	private ProbePairValue closestPoint = null;


	private int Y_AXIS_SPACE = 50;
	private static final int X_AXIS_SPACE = 30;


	/**
	 * Instantiates a new scatter plot panel.
	 * 
	 * @param xStore the x store
	 * @param yStore the y store
	 * @param probeList the probe list
	 * @param commonScale the common scale
	 * @param dotSize the dot size
	 */
	public VariancePlotPanel (ReplicateSet repSet, int varianceMeasure, ProbeList probeList, ProbeList [] subLists, int dotSize) {

		this.repSet = repSet;
		this.varianceMeasure = varianceMeasure;
		this.probeList = probeList;
		this.subLists = subLists;
		this.dotSize = dotSize;

		addMouseListener(this);
		addMouseMotionListener(this);

		Thread t = new Thread(this);
		t.start();
	}

	/**
	 * Sets the dot size.
	 * 
	 * @param dotSize the new dot size
	 */
	public void setDotSize (int dotSize) {
		this.dotSize = dotSize;
		repaint();
	}

	/**
	 * This collapses individual points which are over the same
	 * pixel when redrawing the plot at a different scale
	 */
	private synchronized void calculateNonredundantSet () {

		closestPoint = null;

		ProbePairValue [][] grid = new ProbePairValue [getWidth()][getHeight()];

		Probe [] probes = probeList.getAllProbes();

		try {
			for (int p=0;p<probes.length;p++) {
				float xValue = repSet.getValueForProbeExcludingUnmeasured(probes[p]);
				
				float yValue = getYValue(probes[p]);
				
				if (Float.isNaN(xValue)  || Float.isInfinite(xValue)  || Float.isNaN(yValue) || Float.isInfinite(yValue)) {
					continue;
				}
				
				int x = getX(xValue);
				int y = getY(yValue);
				if (grid[x][y] == null) {
					grid[x][y] = new ProbePairValue(xValue, yValue,x,y);
					grid[x][y].setProbe(probes[p]);
				}
				else {
					// We don't increment the count if we're plotting sublists
					// as we use the count field to indicate which sublist we
					// belong to
					if (subLists == null) grid[x][y].count++;
					
					// As we have multiple probes at this point we remove the 
					// specific probe annotation.
					grid[x][y].setProbe(null);
				}
			}


			// If we have subLists then we need to re-use the count values
			// in the grid to assign a value to indicate which sublist it
			// came from.

			//  We could improve this by doing something clever where values
			// from two lists share the same pixel, but for now we just let
			// the last one we see win.

			if (subLists != null) {

				for (int s=0;s<subLists.length;s++) {
					Probe [] subListProbes = subLists[s].getAllProbes();
					for (int p=0;p<subListProbes.length;p++) {
						float xValue = repSet.getValueForProbeExcludingUnmeasured(subListProbes[p]);
												
						float yValue = getYValue(subListProbes[p]);

						int x = getX(xValue);
						int y = getY(yValue);

						if (grid[x][y] == null) {
							// This messes up where we catch it in the middle of a redraw
							continue;
//							throw new IllegalArgumentException("Found subList position not in main list");
						}

						grid[x][y].count = s+2; // 1 = no list so 2 is the lowest sublist index

					}
				}

			}
		}

		catch (SeqMonkException e) {
			throw new IllegalStateException(e);
		}

		// Now we need to put all of the ProbePairValues into
		// a single array;

		int count = 0;

		for (int x=0;x<grid.length;x++) {
			for (int y=0;y<grid[x].length;y++) {
				if (grid[x][y] != null) count++;
			}
		}

		ProbePairValue [] nonred = new ProbePairValue[count];
		count--;

		for (int x=0;x<grid.length;x++) {
			for (int y=0;y<grid[x].length;y++) {
				if (grid[x][y] != null) {
					nonred[count] = grid[x][y];
					count--;
				}
			}
		}

		Arrays.sort(nonred);

		// Work out the 95% percentile count
		int minCount = 1;
		int maxCount = 2;
		if (nonred.length > 0) {
			minCount = nonred[0].count;
			maxCount = nonred[((nonred.length-1) *95) / 100].count;
		}


		// Go through every nonred assigning a suitable colour
		ColourGradient gradient = new HotColdColourGradient();
		for (int i=0;i<nonred.length;i++) {
//			nonred[i].color = ColourGradient.getColor(nonred[i].count, minCount, maxCount);
			
			if (subLists == null) {
				nonred[i].color = gradient.getColor(nonred[i].count, minCount, maxCount);
			}
			else {
				if (nonred[i].count > subLists.length+1) {
					throw new IllegalArgumentException("Count above threshold when showing sublists");
				}
				if (nonred[i].count == 1) {
					nonred[i].color = VERY_LIGHT_GREY;
				}
				else {
					nonred[i].color = ColourIndexSet.getColour(nonred[i].count-2);
				}
			}

		}

		nonRedundantValues = nonred;
		lastNonredWidth = getWidth();
		lastNonredHeight = getHeight();

		//		System.out.println("Nonred was "+nonRedundantValues.length+" from "+probes.length);

	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.WHITE);

		g.fillRect(0, 0, getWidth(), getHeight());

		FontMetrics metrics = getFontMetrics(g.getFont());

		if (! readyToDraw) {
			g.setColor(Color.GRAY);
			String message = "Calculating Plot";
			if (noData) {
				message = "No data to show";
			}
			g.drawString(message, (getWidth()/2)-(metrics.stringWidth(message)/2), (getHeight()/2-2));
			return;
		}

		// Check to see if we need to work out the counts for this size
		if (nonRedundantValues == null || lastNonredWidth != getWidth() || lastNonredHeight != getHeight()) {
			calculateNonredundantSet();
		}


		// If we're here then we can actually draw the graphs		
		g.setColor(Color.BLACK);

		// X axis
		g.drawLine(Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE, getWidth()-10, getHeight()-X_AXIS_SPACE);

		AxisScale xAxisScale = new AxisScale(minValueX, maxValueX);
		double currentXValue = xAxisScale.getStartingValue();
		while (currentXValue < maxValueX) {
			String xLabel = xAxisScale.format(currentXValue);
			g.drawString(xLabel, getX(currentXValue)-(g.getFontMetrics().stringWidth(xLabel)/2), getHeight()-(X_AXIS_SPACE-(3+g.getFontMetrics().getHeight())));
			g.drawLine(getX(currentXValue),getHeight()-X_AXIS_SPACE,getX(currentXValue),getHeight()-(X_AXIS_SPACE-3));
			currentXValue += xAxisScale.getInterval();
		}

		// Y axis
		g.drawLine(Y_AXIS_SPACE, 10, Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE);

		AxisScale yAxisScale = new AxisScale(minValueY, maxValueY);

		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < maxValueY) {
			String yLabel = yAxisScale.format(currentYValue); 
			g.drawString(yLabel, Y_AXIS_SPACE-(5+g.getFontMetrics().stringWidth(yLabel)), getY(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(Y_AXIS_SPACE,getY(currentYValue),Y_AXIS_SPACE-3,getY(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}

		// X label
		g.drawString(repSet.name(),(getWidth()/2)-(metrics.stringWidth(repSet.name())/2),getHeight()-3);		

		// Y label
		if (varianceMeasure == VARIANCE_SEM) {
			g.drawString("SEM",Y_AXIS_SPACE+3,15);
		}
		else if (varianceMeasure == VARIANCE_STDEV) {
			g.drawString("StDev",Y_AXIS_SPACE+3,15);
		}
		else if (varianceMeasure == VARIANCE_COEF) {
			g.drawString("Coef Var",Y_AXIS_SPACE+3,15);			
		}
			
		
		// If we have sublists draw them below this in the right colours
		if (subLists != null) {
			for (int s=0;s<subLists.length;s++) {
				g.setColor(ColourIndexSet.getColour(s));
				g.drawString(subLists[s].name(),Y_AXIS_SPACE+3,15+(g.getFontMetrics().getHeight()*(s+1)));
			}
			g.setColor(Color.BLACK);
		}


		// ProbeList
		g.drawString(probeList.name(), getWidth()-10-metrics.stringWidth(probeList.name()), 15);
		// Replicate Set
		g.drawString(repSet.name(), getWidth()-10-metrics.stringWidth(repSet.name()), 15+g.getFontMetrics().getHeight());


		g.setColor(Color.BLUE);

		for (int p=0;p<nonRedundantValues.length;p++) {
			
			if ((madeSelection||makingSelection) && nonRedundantValues[p].difference() >= Math.min(diffStart, diffEnd) && nonRedundantValues[p].difference() <= Math.max(diffStart, diffEnd)) {
				g.setColor(Color.BLACK);
			}
			else {
				g.setColor(nonRedundantValues[p].color);
			}

			g.fillRect(nonRedundantValues[p].x - (dotSize/2), nonRedundantValues[p].y-(dotSize/2), dotSize, dotSize);
		}

		
		// Now we draw the regression for the smoothed values
		g.setColor(Color.BLACK);
		
		float [] intensityValues = smoothedTrend.orderedValues();
		float [] smoothedVariances = smoothedTrend.orderedSmoothedVariances();
		for (int i=1;i<intensityValues.length;i++) {
			g.drawLine(getX(intensityValues[i-1]),getY(smoothedVariances[i-1]),getX(intensityValues[i]),getY(smoothedVariances[i]));
		}
		
		
		// We annotate with the overall difference to the mean
		String overallDiffLabel = "Overall variance difference = "+df.format(smoothedTrend.averageDeviationFromSmoothed());
		g.setColor(Color.BLACK);
		g.drawString(overallDiffLabel, getWidth()-(10+g.getFontMetrics().stringWidth(overallDiffLabel)), getHeight()-(X_AXIS_SPACE+3));
		
		
		// Draw the labels on the labelled points
		Iterator<ProbePairValue> it = labelledPoints.iterator();
		
		while (it.hasNext()) {
			ProbePairValue thisPoint = it.next();

			g.setColor(Color.BLACK);
			if (thisPoint != null && thisPoint.probe() != null) {
				g.drawString(thisPoint.probe().name(),thisPoint.x+1,thisPoint.y-1);
			}

		}
		
		
		// Finally we draw the current measures if the mouse is inside the plot
		if (cursorX > 0) {
			g.setColor(Color.BLACK);
			//			System.out.println("Drawing label at x="+cursorX+" y="+cursorY+" x*="+getValueFromX(cursorX)+" y*="+getValueFromY(cursorY));

			String label = "x="+df.format(getValueFromX(cursorX))+" y="+df.format(getValueFromY(cursorY))+" diff="+df.format(getValueFromY(cursorY)-smoothedTrend.getSmoothedValueForX((float)getValueFromX(cursorX)));
			int labelXPos = Y_AXIS_SPACE+((getWidth()-(Y_AXIS_SPACE+10))/2)-(g.getFontMetrics().stringWidth(label)/2);
			g.drawString(label, labelXPos, getHeight()-(X_AXIS_SPACE+3));

			// We also draw the names on the closest point if there is one
			if (closestPoint != null && closestPoint.probe() != null) {
				g.drawString(closestPoint.probe().name(),closestPoint.x+1,closestPoint.y-1);
			}
		}

	}

	/**
	 * Gets the value from y.
	 * 
	 * @param y the y
	 * @return the value from y
	 */
	public double getValueFromY (int y) {
		double value = minValueY;
		value += (maxValueY-minValueY) * (((getHeight()-(10+X_AXIS_SPACE)-(y-10d)) / (getHeight()-(10+X_AXIS_SPACE))));
		return value;
	}

	/**
	 * Gets the value from x.
	 * 
	 * @param x the x
	 * @return the value from x
	 */
	public double getValueFromX (int x) {
		double value = minValueX;
		value += (maxValueX-minValueX) * ((x-Y_AXIS_SPACE) / (double)(getWidth()-(10+Y_AXIS_SPACE)));
		return value;
	}


	/**
	 * Gets the y.
	 * 
	 * @param value the value
	 * @return the y
	 */
	public int getY (double value) {
		double proportion = (value-minValueY)/(maxValueY-minValueY);

		int y = getHeight()-X_AXIS_SPACE;

		y -= (int)((getHeight()-(10+X_AXIS_SPACE))*proportion);

		// Sanity check
		if (y < 10) {
			y = 10;
		}
		
		if (y >= getHeight()) {
			y = getHeight()-1;
		}
		
		return y;
	}

	/**
	 * Gets the x.
	 * 
	 * @param value the value
	 * @return the x
	 */
	public int getX (double value) {
		double proportion = (value-minValueX)/(maxValueX-minValueX);

		int x = Y_AXIS_SPACE;

		x += (int)((getWidth()-(10+Y_AXIS_SPACE))*proportion);

		// Sanity check
		if (x < Y_AXIS_SPACE) {
			x = Y_AXIS_SPACE;
		}
		
		if (x >= getWidth()) {
			x = getWidth()-1;
		}

		return x;
	}

	/**
	 * Gets the filtered probes.
	 * 
	 * @param probeset the probeset
	 * @return the filtered probes
	 */
	public ProbeList getFilteredProbes (ProbeSet probeset) {

		double minDiff = Math.min(diffStart, diffEnd);
		double maxDiff = Math.max(diffStart, diffEnd);

		String varianceName = "Standard Deviation";
		switch(varianceMeasure) {
		
			case VARIANCE_COEF: {
				varianceName = "Coefficient of variation";
				break;
			}
			case VARIANCE_SEM: {
				varianceName = "Standard error of the mean";
				break;
			}
			case VARIANCE_QUARTILE_DISP: {
				varianceName = "Quartile dispersion";
				break;
			}
			case VARIANCE_NUMBER_UNMEASURED: {
				varianceName = "Number of unmeasured data stores";
				break;
			}
		
		}
		
		ProbeList list = new ProbeList(probeList,varianceName+" difference between "+df.format(minDiff)+" and "+df.format(maxDiff),varianceName+" difference in "+repSet.name()+" was between "+df.format(minDiff)+" and "+df.format(maxDiff),varianceName+" diff");

		if (madeSelection) {

			Probe [] probes = probeList.getAllProbes();


			for (int p=0;p<probes.length;p++) {
				try {
					
					float varianceMeasure = getYValue(probes[p]);
					
					double diff = varianceMeasure - smoothedTrend.getSmoothedValueForX(repSet.getValueForProbeExcludingUnmeasured(probes[p]));
					if (diff < minDiff) continue;
					if (diff > maxDiff) continue;

					list.addProbe(probes[p], new Float(diff));
				}
				catch (SeqMonkException e) {
					e.printStackTrace();
				}
			}
		}

		return list;

	}
	
	private float getYValue (Probe p) throws SeqMonkException {
				
		if (varianceMeasure == VARIANCE_STDEV) {
			return repSet.getStDevForProbe(p);
		}
		else if (varianceMeasure == VARIANCE_SEM) {
			return repSet.getSEMForProbe(p);
		}
		else if (varianceMeasure == VARIANCE_COEF) {
			return repSet.getCoefVarForProbe(p);
		}
		else if (varianceMeasure == VARIANCE_QUARTILE_DISP) {
			return repSet.getQuartileCoefDispForProbe(p);
		}
		else if (varianceMeasure == VARIANCE_NUMBER_UNMEASURED) {
			return repSet.getUnmeasuredCountForProbe(p);
		}
		
		throw new IllegalArgumentException("Unknown variance measure "+varianceMeasure);
	
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {


		// We need to find the mix/max counts, and get a non-overlapping
		// set of probes
		Probe [] probes = probeList.getAllProbes();
		
		// We need to get rid of any probes which don't have a value associated with them (ie NaN values)
		
		Vector<Probe> validProbes = new Vector<Probe>();
		
		try {
			for (int p=0;p<probes.length;p++) {
				if (!Float.isNaN(repSet.getValueForProbeExcludingUnmeasured(probes[p]))) {
					validProbes.add(probes[p]);
				}
			}
		}
		catch (SeqMonkException sme) {
			return;
		}

		probes = validProbes.toArray(new Probe[0]);
		
		
		// If there aren't any probes there's no point going any further
		if (probes.length == 0) {
			noData = true;
			repaint();
			return;
		}

		boolean someXValueSet = false;
		boolean someYValueSet = false;
		
		try {

			// We extract the data to allow the calculation of the smoothed values
			float [] xData = new float[probes.length];
			float [] yData = new float[probes.length];


			for (int p=0;p<probes.length;p++) {
				xData[p]=repSet.getValueForProbeExcludingUnmeasured(probes[p]);
				yData[p]=getYValue(probes[p]);
				if (!someXValueSet && !(Float.isNaN(xData[p]) || Float.isInfinite(xData[p]))) {
					minValueX = xData[p];
					maxValueX = xData[p];
					someXValueSet = true;
				}
				if (!someYValueSet && !(Float.isNaN(yData[p]) || Float.isInfinite(yData[p]))) {
					minValueY = yData[p];
					maxValueY = yData[p];
					someYValueSet = true;
				}
				
				
				if (!(Float.isNaN(xData[p]) || Float.isInfinite(xData[p]))) {
					if (xData[p]<minValueX) minValueX = xData[p];
					if (xData[p]>maxValueX) maxValueX = xData[p];
				}
				
				if (!(Float.isNaN(yData[p]) || Float.isInfinite(yData[p]))) {
					if (yData[p]<minValueY) minValueY = yData[p];
					if (yData[p]>maxValueY) maxValueY = yData[p];
				}
			}

			// Do a sanity check that something has a non-NaN value in it
			boolean someRealData = false;
			for (int i=0;i<xData.length;i++) {
				if (!Float.isNaN(xData[i])) {
					someRealData = true;
					break;
				}
			}
			
			if (!someRealData) {
				// There's no point doing anything else
				return;
			}

			// Calculate the smoothed values
			smoothedTrend = new SmoothedVarianceDataset(repSet, probes, varianceMeasure,xData.length/100);

		}
		catch (SeqMonkException e) {
			e.printStackTrace();
		}

		AxisScale yAxisScale = new AxisScale(minValueY, maxValueY);
		Y_AXIS_SPACE = yAxisScale.getXSpaceNeeded()+10;

		
		readyToDraw = true;
		repaint();

	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {

		if (smoothedTrend == null) return;
		
		if (makingSelection) {
			// Set the end points
			int x = e.getX();
			int y = e.getY();

			if (x<10) x = 10;
			if (x>getWidth()-10) x = getWidth()-10;

			if (y<10) {
				diffEnd = Float.POSITIVE_INFINITY;
			}
			else if (y>getHeight()-20) {
				diffEnd = Float.NEGATIVE_INFINITY;
			}
			else {
				diffEnd = getValueFromY(y) - smoothedTrend.getSmoothedValueForX((float)getValueFromX(x));
			}
		}
		else {
			// Set the start points
			makingSelection = true;
			madeSelection = false;

			// Set the end points
			int x = e.getX();
			int y = e.getY();

			if (x<10) x = 10;
			if (x>getWidth()-10) x = getWidth()-10;

			if (y<10) y=10;
			if (y>getHeight()-20) y = getHeight()-20;

			makingSelection = true;
			diffStart = getValueFromY(y) - smoothedTrend.getSmoothedValueForX((float)getValueFromX(x));
			diffEnd = diffStart;
		}

//		System.err.println("Diff range = "+diffStart+" - "+diffEnd);
		
		repaint();

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {

		int x = e.getX();
		int y = e.getY();

		// We only care if we're inside the plot area
		if (x<10 || x>getWidth()-10 || y<10 || y>getHeight()-20) {
			cursorX = 0;
			closestPoint = null;
		}
		else {

			// Find out if we're near a ProbePairValue
			double closestDistance = 5;
			if (nonRedundantValues != null) {
				for (int i=0;i<nonRedundantValues.length;i++) {
					double distance = nonRedundantValues[i].getDistance(x, y);
					if (distance < closestDistance) {
						closestPoint = nonRedundantValues[i];
						closestDistance = distance;
					}
				}
				if (closestDistance == 5) {
					closestPoint = null;
				}
			}

			cursorX = x;
			cursorY = y;
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			if (closestPoint != null && closestPoint.probe() != null) {
				DisplayPreferences.getInstance().setLocation(closestPoint.probe().chromosome(),SequenceRead.packPosition(closestPoint.probe().start(),closestPoint.probe().end(),Location.UNKNOWN));
			}
		}
		else if (e.getClickCount() == 1){
			// Add or remove this point from the drawn set
			if (closestPoint != null && closestPoint.probe != null) {
				if (labelledPoints.contains(closestPoint)) {
					labelledPoints.remove(closestPoint);
				}
				else {
					labelledPoints.add(closestPoint);
				}
			}
		}
		else if (e.getClickCount() == 3) {
			labelledPoints.clear();
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
		if (madeSelection) {
			makingSelection = false;
			repaint();
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		if (madeSelection) {
			madeSelection = false;
		}

		if (makingSelection) {
			makingSelection = false;

			if (diffStart != diffEnd) {
				madeSelection = true;
				//				System.out.println("New selection is "+selectionStartX+"-"+selectionEndX+" "+selectionStartY+"-"+selectionEndY);
			}
		}

		repaint();
	}

	/**
	 * The Class ProbePairValue.
	 */
	private class ProbePairValue implements Comparable<ProbePairValue> {
		
		public double diffToSmoothed;

		/** The count. */
		public int count;

		/** The color. */
		public Color color = null;

		public Probe probe = null;

		/** The x,y positions */
		public int x = 0;
		public int y = 0;

		/**
		 * Instantiates a new probe pair value.
		 * 
		 * @param xValue the x value
		 * @param yValue the y value
		 * @param x the X position for this spot
		 * @param y the Y position for this spot
		 */
		public ProbePairValue (double xValue, double yValue, int x, int y) {
			this.x = x;
			this.y = y;
			this.count = 1;
			diffToSmoothed = yValue - smoothedTrend.getSmoothedValueForX((float)xValue);
		}

		public void setProbe (Probe probe) {
			this.probe = probe;
		}

		public Probe probe () {
			return probe;
		}

		public double difference () {
			return diffToSmoothed;
		}

		public double getDistance (int x, int y) {
			return Math.sqrt(Math.pow(x-this.x, 2)+Math.pow(y-this.y,2));
		}

		/* (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(ProbePairValue o) {
			return count - o.count;
		}

	}


}
