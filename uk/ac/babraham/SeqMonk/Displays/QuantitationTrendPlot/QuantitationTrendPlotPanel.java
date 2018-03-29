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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class TrendOverProbePanel draws a probe trend plot.
 */
public class QuantitationTrendPlotPanel extends JPanel implements Runnable, MouseMotionListener, MouseListener, Cancellable {


	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;

	/** The probes to plot over */
	private Probe [] probes;

	/** The quantitated probes */
	private Probe [] quantitatedProbes;

	/** The stores. */
	private DataStore [] stores;

	/** The raw counts **/
	private double [][] rawCounts = null;

	/** The smoothed counts. */
	private double [][] counts = null;

	/** The draw cross hair. */
	private boolean drawCrossHair = false;

	/** The mouse x. */
	private int mouseX = 0;

	/* These are the counts for this section of the plot only */
	private double localMinCount;
	private double localMaxCount;


	/* These are the counts for all of the plots together and are used for drawing */
	private double minCount;
	private double maxCount;

	/** The probes calculated. */
	private int percentCalculated = 0;

	/** A flag to say if we're doing a fixed length plot */
	private boolean sameLength = true;

	/** If we're doing a fixed length plot this says what length we're using */
	private int fixedLength;

	/** This says over how many points we should smooth the data */
	private int smoothingLevel = 0;

	/** This just helps with formatting numbers displayed on the plot */
	private DecimalFormat df = new DecimalFormat("#.###");

	private boolean leftMost = false;
	private boolean rightMost = false;

	/** This is just so we can annotate the feature type */
	private String featureName;


	private int Y_AXIS_SPACE = 0;

	/**
	 * Instantiates a new trend over probe panel.
	 * 
	 * @param probes the probes
	 * @param stores the DataStores to plot
	 * @param prefs the preferences file for this plot
	 */
	public QuantitationTrendPlotPanel (Probe [] probes, DataStore [] stores, Probe [] quantitatedProbes, String featureName) {

		this.probes = probes;
		this.stores = stores;
		this.featureName = featureName;
		this.quantitatedProbes = quantitatedProbes;
				
		Arrays.sort(this.probes);
		Arrays.sort(this.quantitatedProbes);
		addMouseListener(this);
		addMouseMotionListener(this);

	}

	public void setLeftmost (boolean leftMost) {
		// If we're the leftmost plot in the set we'll draw an axis, otherwise
		// we won't.

		this.leftMost = leftMost;
	}

	public void setRightmost (boolean rightMost) {
		// If we're the rightmost plot then we draw the legend.
		this.rightMost = rightMost;
	}


	public void setMinMax (double min, double max) {
		minCount = min;
		maxCount = max;
		repaint();
	}

	public double localMin () {
		return localMinCount;
	}

	public double localMax () {
		return localMaxCount;
	}


	public void addProgressListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void removeProgressListener (ProgressListener l) {
		if (l != null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	public void startCalculating () {
		Thread t = new Thread(this);
		t.start();
	}


	/**
	 * Save data.
	 * 
	 * @param file the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void saveData (PrintWriter pr, boolean addHeader) throws IOException {

		// The top line is a header listing the different positions
		if (addHeader) {
			StringBuffer headers = new StringBuffer();
			headers.append("Position");
			for (int i=0;i<stores.length;i++) {
				headers.append("\t");
				headers.append(stores[i].name());
			}

			pr.println(headers.toString());
		}
		StringBuffer line;

		for (int p=0;p<counts[0].length;p++) {
			line = new StringBuffer();
			line.append(p+1);

			for (int i=0;i<stores.length;i++) {
				line.append("\t");
				line.append(counts[i][p]);
			}

			pr.println(line.toString());

		}

	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.WHITE);

		g.fillRect(0, 0, getWidth(), getHeight());

		FontMetrics metrics = getFontMetrics(g.getFont());

		// Don't draw anything if the calculation isn't done.
		if (counts == null) {
			return;
		}

		// First work out the amount of space we need to leave for the X axis
		int X_AXIS_SPACE = 0;

		AxisScale yAxisScale = new AxisScale(minCount, maxCount);

		double currentYValue = yAxisScale.getStartingValue();

		while (currentYValue < maxCount) {
			int stringWidth = g.getFontMetrics().stringWidth(yAxisScale.format(currentYValue))+10;
			if (stringWidth > X_AXIS_SPACE) {
				X_AXIS_SPACE = stringWidth;
			}
			currentYValue += yAxisScale.getInterval();
		}

		if (!leftMost) {
			X_AXIS_SPACE = 0;
		}

		if (Y_AXIS_SPACE == 0) {
			Y_AXIS_SPACE = g.getFontMetrics().getAscent()*3;
		}


		// If we're here then we can actually draw the graphs

		g.setColor(Color.BLACK);

		// X axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth(), getHeight()-Y_AXIS_SPACE);

		// Y axis
		g.drawLine(X_AXIS_SPACE, 10, X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE);

		// X labels
		String xLabel;
		if (sameLength) {
			xLabel = "Bases 1 - "+fixedLength;
		}
		else {
			xLabel = "Relative distance across "+featureName;
		}
		g.drawString(xLabel,(getWidth()/2)-(metrics.stringWidth(xLabel)/2),getHeight()-5);

		if (sameLength) {
			AxisScale xAxisScale = new AxisScale(1, fixedLength);

			double currentXValue = xAxisScale.getStartingValue();
			double lastXLabelEnd = -1;

			while (currentXValue+xAxisScale.getInterval() <= fixedLength) {
				int thisX = X_AXIS_SPACE;
				double xProportion = (double)currentXValue/fixedLength;
				thisX += (int)(xProportion * (getWidth()-X_AXIS_SPACE));

				int thisHalfLabelWidth = (g.getFontMetrics().stringWidth(xAxisScale.format(currentXValue))/2);

				if (thisX - thisHalfLabelWidth <= lastXLabelEnd) {
					currentXValue += xAxisScale.getInterval();
					continue;
				}

				lastXLabelEnd = thisX + thisHalfLabelWidth;

				g.drawString(xAxisScale.format(currentXValue), thisX-thisHalfLabelWidth, (int)((getHeight()-Y_AXIS_SPACE)+15));
				g.drawLine(thisX, getHeight()-Y_AXIS_SPACE, thisX, getHeight()-(Y_AXIS_SPACE-3));

			}
		}

		// Labels on the y-axis

		if (leftMost) {
			currentYValue = yAxisScale.getStartingValue();

			while (currentYValue < maxCount) {
				g.drawString(yAxisScale.format(currentYValue), 2, getY(currentYValue)+(g.getFontMetrics().getAscent()/2));
				g.drawLine(X_AXIS_SPACE, getY(currentYValue),X_AXIS_SPACE-3,getY(currentYValue));
				currentYValue += yAxisScale.getInterval();
			}
		}



		// Now go through the various datastores
		for (int d=0;d<counts.length;d++) {
			g.setColor(ColourIndexSet.getColour(d));

			if (rightMost) {
				g.drawString(stores[d].name(), getWidth()-10-metrics.stringWidth(stores[d].name()), 15+(15*d));
			}

			int lastX = X_AXIS_SPACE;
			int lastY = getY(counts[d][0]);

			boolean drawnCrossHair = false;

			for (int x=1;x<counts[d].length;x++) {
				int thisX = X_AXIS_SPACE;
				double xProportion = (double)x/(counts[d].length-1);
				thisX += (int)(xProportion * (getWidth()-X_AXIS_SPACE));

				if (thisX == lastX) continue; // Only draw something when we've moving somewhere.

				int thisY = getY(counts[d][x]);

				if (drawCrossHair && ! drawnCrossHair && thisX >=mouseX) {
					String label = df.format(counts[d][x]);

					// Clean up the ends if we can
					label = label.replaceAll("0+$", "");
					label = label.replaceAll("\\.$", "");

					g.drawString(label, thisX+2, thisY);
					drawnCrossHair = true;

					if (d==0) {
						Color oldColor = g.getColor();
						g.setColor(Color.GRAY);
						g.drawLine(thisX, 10, thisX, getHeight()-20);
						g.drawString(""+x, thisX+2, getHeight()-27);
						g.setColor(oldColor);
					}
				}


				g.drawLine(lastX, lastY, thisX, thisY);
				lastX = thisX;
				lastY = thisY;
			}

		}

	}

	/**
	 * Gets the y.
	 * 
	 * @param count the count
	 * @return the y
	 */
	public int getY (double count) {
		double proportion = ((double)(count-minCount))/(maxCount-minCount);

		int y = getHeight()-Y_AXIS_SPACE;

		y -= (int)((getHeight()-(10+Y_AXIS_SPACE))*proportion);

		return y;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Find out if all probes are the same length
		determineFixedLength();

		double [][]theseCounts;
		try {
			if (sameLength) {
				theseCounts = getFixedWidthProfile();
			}
			else {
				theseCounts = getVariableWidthProfile();
			}
		}
		catch (SeqMonkException sme) {
			Enumeration<ProgressListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressExceptionReceived(sme);
			}
			return;
		}

		// Check to see if they cancelled
		if (theseCounts == null) {
			Enumeration<ProgressListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressCancelled();
			}
			return;
		}

		// Now we need to find the mix/max counts
		localMinCount = theseCounts[0][0];
		localMaxCount = theseCounts[0][0];

		for (int d=0;d<theseCounts.length;d++) {
			for (int p=0;p<theseCounts[d].length;p++) {
				if (theseCounts[d][p]<localMinCount)localMinCount = theseCounts[d][p];
				if (theseCounts[d][p]>localMaxCount)localMaxCount = theseCounts[d][p];
			}
		}

		rawCounts = theseCounts;

		smoothCounts();

		Enumeration<ProgressListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressComplete("calculate_quant_trend_plot", this);
		}

	}

	/**
	 * Sets a new smoothing level and redraws the plot
	 * @param smoothingLevel
	 */
	public void setSmoothing (int smoothingLevel) {

		// Don't let them set a stupidly high smoothing level.
		if (smoothingLevel > rawCounts[0].length/2) {
			smoothingLevel = rawCounts[0].length/2;
		}

		// Don't do anything if we're already using this smoothing level
		if (smoothingLevel == this.smoothingLevel) return;

		this.smoothingLevel = smoothingLevel;		
		smoothCounts();
	}

	/**
	 * This smoothes the raw counts according to the currently
	 * set smoothing level.
	 */
	private void smoothCounts () {

		//We may not have finished calculating yet
		if (rawCounts == null) return;

		double [][] smoothedCounts = new double[rawCounts.length][];

		for (int line=0;line<rawCounts.length;line++) {

			double [] raw = rawCounts[line];
			double [] smoothed = new double [raw.length];

			for (int pos=0;pos<raw.length;pos++) {
				double total = 0;

				int smoothingOffset = smoothingLevel;
				//				if (smoothingOffset>pos) smoothingOffset = pos;
				//				if (smoothingOffset+pos>raw.length-1) smoothingOffset = (raw.length-1)-pos;

				int usedCount = 0;
				for (int offset=0-smoothingOffset;offset<=smoothingOffset;offset++) {
					if (pos+offset < 0) continue;
					if (pos+offset >= raw.length) continue;
					total+=raw[pos+offset];
					usedCount++;
				}				

				smoothed[pos] = total/usedCount;

			}
			smoothedCounts[line] = smoothed;

		}

		counts = smoothedCounts;
		repaint();
	}


	/**
	 * Gets the fixed width counts.
	 * 
	 * @return the fixed width counts
	 */
	private double [][] getFixedWidthProfile () throws SeqMonkException {

		/** This variable is going to hold the sums of the quantitation
		 *  we can map to this data
		 */
		double [][] overallProfiles = new double [stores.length][fixedLength];

		/** This variable is going to hold the counts of the number of observations
		 * we've made to get the total in the other profile.
		 * 
		 */
		int [][] overallCounts = new int[stores.length][fixedLength];

		// We're going to use this variable to cache the position of the start of 
		// the chromosome we're currently processing.
		int probeStartIndex = 0;

		for (int p=0;p<probes.length;p++) {

			if (cancel) {
				return null;
			}

			int percent = (p*100)/probes.length;

			if (percent > percentCalculated) {
				percentCalculated = percent;
				Enumeration<ProgressListener>e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressUpdated("Processed "+p+" probes", percentCalculated, 100);
				}
			}


			//			System.out.println("Looking at probe "+probes[p].toString());

			// Now we need to find the set of quantitated probes which apply to this
			// probe
			if (p==0 || probes[p].chromosome() != probes[p-1].chromosome()) {

				boolean foundHit = false;
				// Find the first quantitated probe which matches this new chromsome
				for (int qp=0;qp<quantitatedProbes.length;qp++) {
					if (quantitatedProbes[qp].chromosome() == probes[p].chromosome()) {
						probeStartIndex = qp;
						foundHit = true;
						break;
					}
				}
				if (!foundHit) {
					// If we get here then there aren't any quantitated probes for this chromosome
					probeStartIndex = quantitatedProbes.length;
				}

			}

			if (probes[p].length() != fixedLength) {
				// Skip calculating this one
				continue;
			}


			for (int qp=probeStartIndex;qp<quantitatedProbes.length;qp++) {

				if (quantitatedProbes[qp].start() > probes[p].end()) break;

				if (!SequenceRead.overlaps(quantitatedProbes[qp].packedPosition(),probes[p].packedPosition())) continue;

				// TODO: We can probably optimise the value of probeStartIndex here

				int startPos = Math.max(quantitatedProbes[qp].start()-probes[p].start(),0);
				int endPos = Math.min(quantitatedProbes[qp].end()-probes[p].start(),fixedLength-1);

				if (probes[p].strand() == Location.REVERSE) {
					// We add the positions from the back
					startPos = (fixedLength-1)-startPos;
					endPos = (fixedLength-1)-endPos;
					int temp = startPos;
					startPos = endPos;
					endPos = temp;
				}

				for (int d=0;d<stores.length;d++) {
					float value = stores[d].getValueForProbe(quantitatedProbes[qp]);

					if (Float.isNaN(value)) continue;

					for (int pos = startPos;pos<=endPos;pos++) {
						overallProfiles[d][pos]+= value;
						overallCounts[d][pos]++;
					}

				}


			}

		}

		// Now we need to go through the profiles adjusting the values for anything
		// which is missing.

		// First we find the first position for which there is a measured
		// value and we fill in all prior positions with that value

		for (int d=0;d<stores.length;d++) {
			TOP: for (int i=0;i<overallCounts[d].length;i++) {			
				if (overallCounts[d][i]>0) {

					for (int j=0;j<i;j++) {
						overallCounts[d][j] = 1;
						overallProfiles[d][j] = overallProfiles[d][i]/overallCounts[d][i];
					}
					break TOP;
				}
			}
		}

		// Now we go through the datasets filling in any holes with the last true
		// value we measured, and averaging out the observations we have.

		for (int d=0;d<stores.length;d++) {
			int lastValidMeasure = 0;
			for (int i=0;i<overallCounts[d].length;i++) {
				if (overallCounts[d][i] == 0) {
					overallProfiles[d][i] = overallProfiles[d][lastValidMeasure];
				}
				else {
					overallProfiles[d][i] /= overallCounts[d][i];
					lastValidMeasure = i;
				}
			}
		}



		return overallProfiles;
	}


	/**
	 * Gets the fixed width counts.
	 * 
	 * @return the fixed width counts
	 */
	private double [][] getVariableWidthProfile () throws SeqMonkException {

		/** This variable is going to hold the sums of the quantitation
		 *  we can map to this data
		 */

		int numberOfPointsInVariableProfile = 200;

		double [][] overallProfiles = new double [stores.length][numberOfPointsInVariableProfile];

		/** This variable is going to hold the counts of the number of observations
		 * we've made to get the total in the other profile.
		 * 
		 */
		int [][] overallCounts = new int[stores.length][numberOfPointsInVariableProfile];

		// We're going to use this variable to cache the position of the start of 
		// the chromosome we're currently processing.
		int probeStartIndex = 0;

		for (int p=0;p<probes.length;p++) {

			if (cancel) {
				return null;
			}

			int percent = (p*100)/probes.length;

			if (percent > percentCalculated) {
				percentCalculated = percent;
				Enumeration<ProgressListener>e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressUpdated("Processed "+p+" probes", percentCalculated, 100);
				}
			}

			//			System.out.println("Looking at probe "+probes[p].toString());

			// Now we need to find the set of quantitated probes which apply to this
			// probe
			if (p==0 || probes[p].chromosome() != probes[p-1].chromosome()) {

				boolean foundHit = false;
				// Find the first quantitated probe which matches this new chromsome
				for (int qp=0;qp<quantitatedProbes.length;qp++) {
					if (quantitatedProbes[qp].chromosome() == probes[p].chromosome()) {
						probeStartIndex = qp;
						foundHit = true;
						break;
					}
				}
				if (!foundHit) {
					// If we get here then there aren't any quantitated probes for this chromosome
					probeStartIndex = quantitatedProbes.length;
				}

			}

			for (int qp=probeStartIndex;qp<quantitatedProbes.length;qp++) {

				if (quantitatedProbes[qp].start() > probes[p].end()) break;

				if (!SequenceRead.overlaps(quantitatedProbes[qp].packedPosition(),probes[p].packedPosition())) continue;


				// TODO: We can probably optimise the value of probeStartIndex here


				int minPosInProbe = Math.max(quantitatedProbes[qp].start()-probes[p].start(),0);
				int maxPosInProbe = Math.min(quantitatedProbes[qp].end()-probes[p].start(),probes[p].length()-1);

				int percentMinPosInProbe = (int)Math.round(minPosInProbe*(double)(numberOfPointsInVariableProfile-1)/(probes[p].length()-1));
				int percentMaxPosInProbe = (int)Math.round(maxPosInProbe*(double)(numberOfPointsInVariableProfile-1)/(probes[p].length()-1));


				if (probes[p].strand() == Location.REVERSE) {
					// We add counts from the reverse end.
					percentMinPosInProbe = (numberOfPointsInVariableProfile-1)-percentMinPosInProbe;
					percentMaxPosInProbe = (numberOfPointsInVariableProfile-1)-percentMaxPosInProbe;
					int temp = percentMinPosInProbe;
					percentMinPosInProbe = percentMaxPosInProbe;
					percentMaxPosInProbe = temp;
				}


				for (int d=0;d<stores.length;d++) {
					float value = stores[d].getValueForProbe(quantitatedProbes[qp]);

					if (Float.isNaN(value)) continue;

					for (int pos = percentMinPosInProbe;pos<=percentMaxPosInProbe;pos++) {
						overallProfiles[d][pos]+= stores[d].getValueForProbe(quantitatedProbes[qp]);
						overallCounts[d][pos]++;
					}

				}


			}

		}

		// Now we need to go through the profiles adjusting the values for anything
		// which is missing.

		// First we find the first position for which there is a measured
		// value and we fill in all prior positions with that value

		for (int d=0;d<stores.length;d++) {
			TOP: for (int i=0;i<overallCounts[d].length;i++) {			
				if (overallCounts[d][i]>0) {

					for (int j=0;j<i;j++) {
						overallCounts[d][j] = 1;
						overallProfiles[d][j] = overallProfiles[d][i]/overallCounts[d][i];
					}
					break TOP;
				}
			}
		}

		// Now we go through the datasets filling in any holes with the last true
		// value we measured, and averaging out the observations we have.

		for (int d=0;d<stores.length;d++) {
			int lastValidMeasure = 0;
			for (int i=0;i<overallCounts[d].length;i++) {
				if (overallCounts[d][i] == 0) {
					overallProfiles[d][i] = overallProfiles[d][lastValidMeasure];
				}
				else {
					overallProfiles[d][i] /= overallCounts[d][i];
					lastValidMeasure = i;
				}
			}
		}



		return overallProfiles;
	}

	/**
	 * Determine fixed length.
	 */
	private void determineFixedLength () {

		// Check to see if we should use a fixed or variable
		// width display.  Since supposedly fixed width probe
		// sets (windows or feature based) can vary in length
		// if they hit the end of a chromosome we just look for
		// the most common length being the vast majority of
		// probes in the set.

		Hashtable<Integer, Integer>lengthCount = new Hashtable<Integer, Integer>();

		for (int p=0;p<probes.length;p++) {
			if (lengthCount.containsKey(probes[p].length())) {
				lengthCount.put(probes[p].length(), lengthCount.get(probes[p].length())+1);
			}
			else {
				lengthCount.put(probes[p].length(),1);
			}
		}

		// Find the biggest count
		int biggestCount = 0;
		Enumeration<Integer>lengths = lengthCount.keys();
		while (lengths.hasMoreElements()) {
			Integer length = lengths.nextElement();
			if (lengthCount.get(length)>biggestCount) {
				biggestCount = lengthCount.get(length);
				fixedLength = length;
			}
		}

		//		System.out.println("Biggest count was "+biggestCount+" for length "+fixedLength+" out of "+probes.length+" probes");

		// We make a fixed width plot if >95% of the probes are the same length

		if ((biggestCount*100)/probes.length > 95) {
			sameLength = true;
		}
		else {
			sameLength = false;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
		if (e.getX()>10 && e.getX()<getWidth()) {
			drawCrossHair = true;
			mouseX = e.getX();
			repaint();
		}
		else {
			drawCrossHair = false;
			repaint();
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
		drawCrossHair = false;
		repaint();
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {}

	public void cancel() {
		cancel = true;
	}

}
