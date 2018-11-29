/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.VolcanoPlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.swing.JPanel;

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
 * The actual scatterplot panel which implements the selection of
 * regions.
 */
public class VolcanoPlotPanel extends JPanel implements Runnable, MouseMotionListener, MouseListener {

	private final static Color VERY_LIGHT_GREY = new Color(225, 225, 225);

	/** The x store. */
	private String diffName;
	private int diffIndex;

	/** The y store. */
	private String sigName;
	private int sigIndex;

	float pCutoff = 0.05f;
	
	/** The probe list. */
	private ProbeList probeList;

	/** The set of sublists to highlight */
	private ProbeList [] subLists;

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
	//private boolean readyToDraw = false;
	protected boolean readyToDraw = false;

	/** The dot size. */
	private int dotSize = 1;

	/** The df. */
	private final DecimalFormat df = new DecimalFormat("#.###");

	/**  Some points which say where the cursor is currently located */
	private int cursorX = 0;
	private int cursorY = 0;

	/** The last calculated non-redundant set of probe values **/
	private ProbePairValue [] nonRedundantValues = null;

	/** The last image size for which we calculated a nonredundant set **/
	private int lastNonredWidth = 0;
	private int lastNonredHeight = 0;

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
	public VolcanoPlotPanel (String diffName, String sigName, ProbeList probeList, ProbeList [] subLists, int dotSize) {

		this.diffName = diffName;
		this.sigName = sigName;
		this.probeList = probeList;
		this.subLists = subLists;
		this.dotSize = dotSize;

		// Find the index positions for the two attributes
		for (int i=0;i<probeList.getValueNames().length;i++) {
			if (probeList.getValueNames()[i].equals(diffName)) 
				diffIndex = i;

			if (probeList.getValueNames()[i].equals(sigName)) 
				sigIndex = i;
		}

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
		
		for (int p=0;p<probes.length;p++) {
			float xValue = probeList.getValuesForProbe(probes[p])[diffIndex];
			float yValue = probeList.getValuesForProbe(probes[p])[sigIndex];

			if (Float.isNaN(xValue)  || Float.isInfinite(xValue)  || Float.isNaN(yValue) || Float.isInfinite(yValue)) {
				continue;
			}
			
			// We want to do a Phred score for the y data, which means limiting
			// how low the value can be
			if (yValue < pCutoff)
				yValue = pCutoff;

			yValue = (float)(0-(Math.log10(yValue)));


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
					float xValue = probeList.getValuesForProbe(subListProbes[p])[diffIndex];
					float yValue = probeList.getValuesForProbe(subListProbes[p])[sigIndex];
					if (yValue < pCutoff)
						yValue = pCutoff;

					yValue = (float)(0-(Math.log10(yValue)));

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
			g.drawString(message, (getWidth()/2)-(metrics.stringWidth(message)/2), (getHeight()/2-2));
			return;
		}

		// Check to see if we need to work out the counts for this size
		if (nonRedundantValues == null || lastNonredWidth != getWidth() || lastNonredHeight != getHeight()) {
			calculateNonredundantSet();
		}


		// If we're here then we can actually draw the graphs

		// We need to work out the spacing for the y axis
		AxisScale yAxisScale = new AxisScale(minValueY, maxValueY);

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

		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < maxValueY) {
			String yLabel = yAxisScale.format(currentYValue); 
			g.drawString(yLabel, Y_AXIS_SPACE-(5+g.getFontMetrics().stringWidth(yLabel)), getY(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(Y_AXIS_SPACE,getY(currentYValue),Y_AXIS_SPACE-3,getY(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}

		// X label
		g.drawString(diffName,(getWidth()/2)-(metrics.stringWidth(diffName)/2),getHeight()-3);		

		// Y label
		g.drawString("Abs log10 "+sigName,Y_AXIS_SPACE+3,15);

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


		// Centre line
		g.setColor(Color.GRAY);
		g.drawLine(getX(0),getHeight()-X_AXIS_SPACE,getX(0),10);

		g.setColor(Color.BLUE);

		for (int p=0;p<nonRedundantValues.length;p++) {

			if ((madeSelection||makingSelection) && nonRedundantValues[p].yValue >= Math.min(diffStart, diffEnd) && nonRedundantValues[p].yValue <= Math.max(diffStart, diffEnd)) {
				g.setColor(Color.BLACK);
			}
			else {
				g.setColor(nonRedundantValues[p].color);
			}

			g.fillRect(nonRedundantValues[p].x - (dotSize/2), nonRedundantValues[p].y-(dotSize/2), dotSize, dotSize);
		}

		// Finally we draw the current measures if the mouse is inside the plot
		if (cursorX > 0) {
			g.setColor(Color.BLACK);
			//			System.out.println("Drawing label at x="+cursorX+" y="+cursorY+" x*="+getValueFromX(cursorX)+" y*="+getValueFromY(cursorY));

			String label = "x="+df.format(getValueFromX(cursorX))+" y="+df.format(getValueFromY(cursorY))+" diff="+df.format(getValueFromX(cursorX)-getValueFromY(cursorY));
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

		ProbeList list = new ProbeList(probeList,"Abs log10 "+sigName+" between "+df.format(minDiff)+" and "+df.format(maxDiff),"Difference between "+diffName+" and "+sigName+" was between "+df.format(minDiff)+" and "+df.format(maxDiff),new String[]{"Abs log10 "+sigName});

		if (madeSelection) {

			Probe [] probes = probeList.getAllProbes();


			for (int p=0;p<probes.length;p++) {
				float yValue = probeList.getValuesForProbe(probes[p])[sigIndex];
				if (yValue < pCutoff)
					yValue = pCutoff;

				yValue = (float)(0-(Math.log10(yValue)));
				if (Float.isNaN(yValue)) continue;
				if (yValue < minDiff) continue;
				if (yValue > maxDiff) continue;

				list.addProbe(probes[p], yValue);
			}
		}

		return list;

	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {


		// We need to find the mix/max counts, and get a non-overlapping
		// set of probes
		Probe [] probes = probeList.getAllProbes();

		// If there aren't any probes there's no point going any further
		if (probes.length == 0) return;

		boolean someXValueSet = false;
		boolean someYValueSet = false;


		// We extract the data to allow the calculation of an r-value
		float [] xData = new float[probes.length];
		float [] yData = new float[probes.length];


		// We need to find the lowest p-value above 0 to use as a cutoff for the 
		// log transformed data
		pCutoff = 0.05f;
		for (int p=0;p<probes.length;p++) {
			float value =probeList.getValuesForProbe(probes[p])[sigIndex];
			
			if (value > 0 && value < pCutoff) pCutoff = value;
		}
		
		for (int p=0;p<probes.length;p++) {
			xData[p]=probeList.getValuesForProbe(probes[p])[diffIndex];
			yData[p]=probeList.getValuesForProbe(probes[p])[sigIndex];

			// We want to do a Phred score for the y data, which means limiting
			// how low the value can be
			if (yData[p] < pCutoff)
				yData[p] = pCutoff;

			yData[p] = (float)(0-(Math.log10(yData[p])));

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
				if (yData[p]>maxValueY) {
					System.err.println("Increased maxy from "+maxValueY+" to "+yData[p]);
					maxValueY = yData[p];
				}
			}
		}

		// Make the difference values symmetrical
		float maxDiff = 0.5f+(float)Math.max(Math.abs(minValueX), Math.abs(maxValueX));
		minValueX = 0-maxDiff;
		maxValueX = maxDiff;

		// Clear these arrays since we don't need them any more.
		xData = null;
		yData = null;

		// Work out how much space we need for the y axis
		AxisScale yAxisScale = new AxisScale(minValueY, maxValueY);
		Y_AXIS_SPACE = yAxisScale.getXSpaceNeeded()+10;

		readyToDraw = true;
		repaint();

	}


	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {

		if (makingSelection) {
			// Set the end points
			int y = e.getY();

			if (y<10) y=10;
			if (y>getHeight()-20) y = getHeight()-20;

			diffEnd = getValueFromY(y);
		}
		else {
			// Set the start points
			makingSelection = true;
			madeSelection = false;

			// Set the end points
			int y = e.getY();

			if (y<10) y=10;
			if (y>getHeight()-20) y = getHeight()-20;

			makingSelection = true;
			diffStart = getValueFromY(y);
			diffEnd = diffStart;
		}

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

		/** The x value. */
		public double xValue;

		/** The y value */
		public double yValue;

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
			this.xValue = xValue;
			this.yValue = yValue;
			this.x = x;
			this.y = y;
			this.count = 1;
		}

		public void setProbe (Probe probe) {
			this.probe = probe;
		}

		public Probe probe () {
			return probe;
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
