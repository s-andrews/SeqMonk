/**
 * Copyright 2009-15-13 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ProbeTrendPlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class TrendOverProbePanel draws a probe trend plot.
 */
public class TrendOverProbePanel extends JPanel implements Runnable, MouseMotionListener, MouseListener, Cancellable {


	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;

	/** The probes. */
	private ProbeList [] probeLists;

	private Probe [][] probes;

	/** The stores. */
	private DataStore [] stores;

	/** The raw counts **/
	private double [][] rawCounts = null;

	/** The smoothed counts. */
	private double [][] counts = null;

	/** The prefs. */
	private TrendOverProbePreferences prefs;

	/** The draw cross hair. */
	private boolean drawCrossHair = false;

	/** The mouse x. */
	private int mouseX = 0;

	// TODO: Should the mix / max counts be per datastore?
	/** The min count. */
	private double minCount;

	/** The max count. */
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

	private int Y_AXIS_SPACE = 0;

	/**
	 * Instantiates a new trend over probe panel.
	 * 
	 * @param probes the probes
	 * @param stores the DataStores to plot
	 * @param prefs the preferences file for this plot
	 */
	public TrendOverProbePanel (ProbeList [] probeLists, DataStore [] stores, TrendOverProbePreferences prefs) {

		this.probeLists = probeLists;
		this.stores = stores;
		this.prefs = prefs;

		probes = new Probe [probeLists.length][];
		for (int i=0;i<probeLists.length;i++) {
			probes[i] = probeLists[i].getAllProbes();
			Arrays.sort(probes[i]);
		}

		addMouseListener(this);
		addMouseMotionListener(this);

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
	public void saveData (File file) throws IOException {
		PrintWriter pr = new PrintWriter(new FileWriter(file));

		// The top line is a header listing the different positions
		StringBuffer headers = new StringBuffer();
		headers.append("Position");
		for (int p=0;p<probeLists.length;p++) {
			for (int i=0;i<stores.length;i++) {
				headers.append("\t");
				headers.append(probeLists[p].name()+":"+stores[i].name());
			}
		}

		pr.println(headers.toString());

		StringBuffer line;

		for (int p=0;p<counts[0].length;p++) {
			line = new StringBuffer();
			line.append(p+1);

			for (int i=0;i<counts.length;i++) {
				line.append("\t");
				line.append(counts[i][p]);
			}
			

			pr.println(line.toString());
		
		}

		pr.close();
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

		if (Y_AXIS_SPACE == 0) {
			Y_AXIS_SPACE = g.getFontMetrics().getAscent()*3;
		}


		// If we're here then we can actually draw the graphs

		g.setColor(Color.BLACK);

		// X axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-10, getHeight()-Y_AXIS_SPACE);

		// Y axis
		g.drawLine(X_AXIS_SPACE, 10, X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE);

		// X labels
		String xLabel;
		if (sameLength) {
			xLabel = "Bases 1 - "+fixedLength;
		}
		else {
			xLabel = "Relative distance across probe";
		}
		g.drawString(xLabel,(getWidth()/2)-(metrics.stringWidth(xLabel)/2),getHeight()-5);

		if (sameLength) {
			AxisScale xAxisScale = new AxisScale(1, fixedLength);

			double currentXValue = xAxisScale.getStartingValue();

			while (currentXValue+xAxisScale.getInterval() < fixedLength) {
				int thisX = X_AXIS_SPACE;
				double xProportion = (double)currentXValue/fixedLength;
				thisX += (int)(xProportion * (getWidth()-(X_AXIS_SPACE+10)));


				g.drawString(xAxisScale.format(currentXValue), thisX-(g.getFontMetrics().stringWidth(xAxisScale.format(currentXValue))/2), (int)((getHeight()-Y_AXIS_SPACE)+15));
				g.drawLine(thisX, getHeight()-Y_AXIS_SPACE, thisX, getHeight()-(Y_AXIS_SPACE-3));

				currentXValue += xAxisScale.getInterval();
			}
		}

		// Labels on the y-axis

		currentYValue = yAxisScale.getStartingValue();

		while (currentYValue < maxCount) {
			g.drawString(yAxisScale.format(currentYValue), 2, getY(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(X_AXIS_SPACE, getY(currentYValue),X_AXIS_SPACE-3,getY(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}



		// Now go through the various datastores
		for (int p=0;p<probeLists.length;p++) {
			for (int s=0;s<stores.length;s++) {

				int d = (p * stores.length)+s;
				g.setColor(ColourIndexSet.getColour(d));

				String name = probeLists[p].name()+" - "+stores[s].name();

				g.drawString(name, getWidth()-10-metrics.stringWidth(name), 15+(15*d));

				int lastX = X_AXIS_SPACE;
				int lastY = getY(counts[d][0]);

				boolean drawnCrossHair = false;

				for (int x=1;x<counts[d].length;x++) {
					int thisX = X_AXIS_SPACE;
					double xProportion = (double)x/(counts[d].length-1);
					thisX += (int)(xProportion * (getWidth()-(X_AXIS_SPACE+10)));

					if (thisX == lastX) continue;
					
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
		if (sameLength) {
			if (prefs.plotType == TrendOverProbePreferences.CUMULATIVE_COUNT_PLOT) {
				theseCounts = getFixedWidthCounts();
			}
			else {
				theseCounts = getFixedWidthProfile();				
			}
		}
		else {
			if (prefs.plotType == TrendOverProbePreferences.CUMULATIVE_COUNT_PLOT) {
				theseCounts = getVariableWidthCounts();
			}
			else {
				theseCounts = getVariableWidthProfile();
			}
		}

		// Check to see if they cancelled
		if (theseCounts == null) {
			Enumeration<ProgressListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressCancelled();
			}
			return;
		}

		// Correct within each set if needed
		if (prefs.correctWithinEachStore) {

			// We put everything on a range of 1 - 100
			for (int i=0;i<stores.length;i++) {
				double min = theseCounts[i][0];
				double max = min;

				for (int p=1;p<theseCounts[i].length;p++) {
					if (theseCounts[i][p]<min) min = theseCounts[i][p];
					if (theseCounts[i][p]>max) max = theseCounts[i][p];
				}

				// Now go back and correct

				// Don't try to correct if we don't have any counts
				if (max > 0) {
					for (int p=0;p<theseCounts[i].length;p++) {
						theseCounts[i][p] = ((theseCounts[i][p]-min) * 100)/(max-min); 
					}
				}
			}

		}


		// Now we need to find the mix/max counts
		minCount = theseCounts[0][0];
		maxCount = theseCounts[0][0];

		for (int d=0;d<theseCounts.length;d++) {
			for (int p=0;p<theseCounts[d].length;p++) {
				if (theseCounts[d][p]<minCount)minCount = theseCounts[d][p];
				if (theseCounts[d][p]>maxCount)maxCount = theseCounts[d][p];
			}
		}

		rawCounts = theseCounts;

		smoothCounts();

		Enumeration<ProgressListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressComplete("calculate_trend_plot", this);
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
	private double [][] getFixedWidthProfile () {

		double [][] overallProfiles = new double [probes.length*stores.length][fixedLength];

		for (int p1=0;p1<probes.length;p1++) {
			for (int p2=0;p2<probes[p1].length;p2++) {

				if (cancel) {
					return null;
				}

				int percent = ((probes[p1].length*p1) + p2)*100/(probes[p1].length*probes.length);

				if (percent > percentCalculated) {
					percentCalculated = percent;
					Enumeration<ProgressListener>e = listeners.elements();
					while (e.hasMoreElements()) {
						e.nextElement().progressUpdated("Processed "+percentCalculated+"%", percentCalculated, 100);
					}
				}

				//			System.out.println("Looking at probe "+probes[p].toString());

				for (int d=0;d<stores.length;d++) {

					long [] theseCounts = new long[fixedLength];
					double totalBaseCount = 0;

					long [] reads = stores[d].getReadsForProbe(probes[p1][p2]);
					//				System.out.println("Got "+reads.length+" reads from "+stores[d].name());
					for (int r=0;r<reads.length;r++) {

						// Check if we can skip this read as a duplicate
						if (prefs.removeDuplicates && r > 0) {
							if (reads[r] == reads[r-1]) {
								continue;
							}
						}

						// Check if we're using reads in this direction
						if (! prefs.quantitationType.useRead(probes[p1][p2], reads[r])) {
							continue;
						}

						int startPos = Math.max(SequenceRead.start(reads[r])-probes[p1][p2].start(),0);
						int endPos = Math.min(SequenceRead.end(reads[r])-probes[p1][p2].start(),theseCounts.length-1);

						if (probes[p1][p2].strand() == Location.REVERSE) {
							// We add the positions from the back
							startPos = (theseCounts.length-1)-startPos;
							endPos = (theseCounts.length-1)-endPos;
							int temp = startPos;
							startPos = endPos;
							endPos = temp;
						}

						for (int pos = startPos;pos<=endPos;pos++) {
							theseCounts[pos]++;
						}

						/*
						 * This caused no end of trouble. All of these relative plots used to
						 * 'smile' at the ends and we couldn't figure out why.  The reason is
						 * that partial overlaps allow for much higher percentage increases in
						 * enrichment than full overlaps (100% increase for 1bp overlap vs 1%
						 * increase for 100bp overlap). The fix is to always count the full length
						 * of the probe in the total count even if we're actually only counting the
						 * parts which overlap this probe.
						 */
						totalBaseCount += SequenceRead.length(reads[r]);

					}

					totalBaseCount /= fixedLength;

					// We can now convert the counts to proportions and add it
					// to the total
					if (totalBaseCount > 0) {
						for (int i=0;i<theseCounts.length;i++) {
							overallProfiles[(p1*stores.length)+d][i] += (theseCounts[i]/totalBaseCount)/probes[p1].length;
						}
					}
				}
			}
		}

		//		for (int i=0;i<overallProfiles[0].length;i++) {
		//			System.out.println("Position "+i+" value "+overallProfiles[0][i]);
		//		}

		return overallProfiles;
	}



	/**
	 * Gets the fixed width counts.
	 * 
	 * @return the fixed width counts
	 */
	private double [][] getFixedWidthCounts () {

		long [][] theseCounts = new long [stores.length*probes.length][fixedLength];

		for (int p1=0;p1<probes.length;p1++) {
			for (int p2=0;p2<probes[p1].length;p2++) {

				if (cancel) {
					return null;
				}

				int percent = ((probes[p1].length*p1) + p2)*100/(probes[p1].length*probes.length);

				if (percent > percentCalculated) {
					percentCalculated = percent;
					Enumeration<ProgressListener>e = listeners.elements();
					while (e.hasMoreElements()) {
						e.nextElement().progressUpdated("Processed "+percentCalculated+"%", percentCalculated, 100);
					}
				}


				//			System.out.println("Looking at probe "+probes[p].toString());

				for (int d=0;d<stores.length;d++) {
					long [] reads = stores[d].getReadsForProbe(probes[p1][p2]);
					//				System.out.println("Got "+reads.length+" reads from "+stores[d].name());
					for (int r=0;r<reads.length;r++) {

						// Check if we can skip this read as a duplicate
						if (prefs.removeDuplicates && r > 0) {
							if (reads[r] == reads[r-1]) {
								continue;
							}
						}

						// Check if we're using reads in this direction
						if (! prefs.quantitationType.useRead(probes[p1][p2], reads[r])) {
							continue;
						}


						int startPos = Math.max(SequenceRead.start(reads[r])-probes[p1][p2].start(),0);
						int endPos = Math.min(SequenceRead.end(reads[r])-probes[p1][p2].start(),theseCounts[d].length-1);

						if (probes[p1][p2].strand() == Location.REVERSE) {
							// We add the positions from the back
							startPos = (theseCounts[d].length-1)-startPos;
							endPos = (theseCounts[d].length-1)-endPos;
							int temp = startPos;
							startPos = endPos;
							endPos = temp;
						}

						for (int pos = startPos;pos<=endPos;pos++) {
							theseCounts[(p1*stores.length)+d][pos]++;
						}
					}
				}
			}
		}

		double [][] returnedCounts = new double [stores.length*probes.length][fixedLength];

		// Correct for total read count if needed
		if (prefs.correctForTotalCount) {

			// We scale up smaller sets to match the largest set
			// so we need to find the largest set
			double largestSet = 0;
			for (int i=0;i<stores.length;i++) {
				//TODO: Should this correction be just for the strand of reads used?
				if (stores[i].getTotalReadCount()>largestSet) largestSet = stores[i].getTotalReadCount();
			}

			// Now we can correct for
			for (int i=0;i<stores.length;i++) {
				double correction = largestSet/stores[i].getTotalReadCount();

				for (int d=0;d<theseCounts.length;d++) {
					if (d%stores.length == i) {
						for (int p=0;p<theseCounts[d].length;p++) {
							returnedCounts[d][p] = (theseCounts[d][p] * correction) / probes[d/stores.length].length ;
						}
					}
				}
			}
		}


		else {

			// Just transfer the long data over into the double structure
			for (int i=0;i<theseCounts.length;i++) {

				for (int p=0;p<theseCounts[i].length;p++) {
					returnedCounts[i][p] = theseCounts[i][p] / (double)probes[i/stores.length].length;
				}

			}

		}

		return returnedCounts;
	}
	
	


	/**
	 * Gets the variable width profile.
	 * 
	 * @return the variable width counts
	 */
	private double [][] getVariableWidthProfile () {

		// We divide the counts up into 100 bins rather than 
		// using the actual bp across the probe

		double [][] returnedCounts = new double [stores.length*probes.length][100];

		for (int p1=0;p1<probes.length;p1++) {
			for (int p2=0;p2<probes[p1].length;p2++) {

				if (cancel) {
					return null;
				}

				int percent = ((probes[p1].length*p1) + p2)*100/(probes[p1].length*probes.length);

				if (percent > percentCalculated) {
					percentCalculated = percent;
					Enumeration<ProgressListener>e = listeners.elements();
					while (e.hasMoreElements()) {
						e.nextElement().progressUpdated("Processed "+percentCalculated+"%", percentCalculated, 100);
					}
				}

				//			System.out.println("Looking at probe "+probes[p].toString());

				for (int d=0;d<stores.length;d++) {

					long [] theseCounts = new long [100];
					double totalCount = 0;

					long [] reads = stores[d].getReadsForProbe(probes[p1][p2]);
					//				System.out.println("Got "+reads.length+" reads from "+stores[d].name());

					for (int r=0;r<reads.length;r++) {

						// Check if we can skip this read as a duplicate
						if (prefs.removeDuplicates && r > 0) {
							if (reads[r] == reads[r-1]) {
								continue;
							}
						}

						// Check if we're using reads in this direction
						if (! prefs.quantitationType.useRead(probes[p1][p2], reads[r])) {
							continue;
						}

						int minPosInProbe = Math.max(SequenceRead.start(reads[r])-probes[p1][p2].start(),0);
						int maxPosInProbe = Math.min(SequenceRead.end(reads[r])-probes[p1][p2].start(),probes[p1][p2].length()-1);

						int percentMinPosInProbe = (int)Math.round(minPosInProbe*99d/(probes[p1][p2].length()-1));
						int percentMaxPosInProbe = (int)Math.round(maxPosInProbe*99d/(probes[p1][p2].length()-1));


						if (probes[p1][p2].strand() == Location.REVERSE) {
							// We add counts from the reverse end.
							percentMinPosInProbe = 99-percentMinPosInProbe;
							percentMaxPosInProbe = 99-percentMaxPosInProbe;
							int temp = percentMinPosInProbe;
							percentMinPosInProbe = percentMaxPosInProbe;
							percentMaxPosInProbe = temp;
						}

						for (int pos = percentMinPosInProbe;pos<=percentMaxPosInProbe;pos++) {
							theseCounts[pos]++;
						}

						/*
						 * This caused no end of trouble. All of these relative plots used to
						 * 'smile' at the ends and we couldn't figure out why.  The reason is
						 * that partial overlaps allow for much higher percentage increases in
						 * enrichment than full overlaps (100% increase for 1bp overlap vs 1%
						 * increase for 100bp overlap). The fix is to always count the full length
						 * of the probe in the total count even if we're actually only counting the
						 * parts which overlap this probe.
						 */
						int percentOverallMinInProbe = (int)Math.round((SequenceRead.start(reads[r])-probes[p1][p2].start())*99d/(probes[p1][p2].length()-1));
						int percentOverallMaxInProbe = (int)Math.round((SequenceRead.end(reads[r])-probes[p1][p2].start())*99d/(probes[p1][p2].length()-1));
						totalCount += (percentOverallMaxInProbe-percentOverallMinInProbe)+1;


					}

					totalCount /= 100;

					// We can now convert the counts to proportions and add it
					// to the total
					if (totalCount > 0) {
						for (int i=0;i<theseCounts.length;i++) {
							returnedCounts[(p1*stores.length)+d][i] += (theseCounts[i]/totalCount)/probes[p1].length;
						}
					}
				}
			}
		}


		return returnedCounts;
	}


	/**
	 * Gets the variable width counts.
	 * 
	 * @return the variable width counts
	 */
	private double [][] getVariableWidthCounts () {

		// We divide the counts up into 100 bins rather than 
		// using the actual bp across the probe

		long [][] theseCounts = new long [stores.length*probes.length][100];

		for (int p1=0;p1<probes.length;p1++) {
			for (int p2=0;p2<probes[p1].length;p2++) {

				if (cancel) {
					return null;
				}

				int percent = ((probes[p1].length*p1) + p2)*100/(probes[p1].length*probes.length);

				if (percent > percentCalculated) {
					percentCalculated = percent;
					Enumeration<ProgressListener>e = listeners.elements();
					while (e.hasMoreElements()) {
						e.nextElement().progressUpdated("Processed "+percentCalculated+"%", percentCalculated, 100);
					}
				}

				//			System.out.println("Looking at probe "+probes[p].toString());

				for (int d=0;d<stores.length;d++) {
					long [] reads = stores[d].getReadsForProbe(probes[p1][p2]);
					//				System.out.println("Got "+reads.length+" reads from "+stores[d].name());

					for (int r=0;r<reads.length;r++) {

						// Check if we can skip this read as a duplicate
						if (prefs.removeDuplicates && r > 0) {
							if (reads[r] == reads[r-1]) {
								continue;
							}
						}

						// Check if we're using reads in this direction
						if (! prefs.quantitationType.useRead(probes[p1][p2], reads[r])) {
							continue;
						}

						int minPosInProbe = Math.max(SequenceRead.start(reads[r])-probes[p1][p2].start(),0);
						int maxPosInProbe = Math.min(SequenceRead.end(reads[r])-probes[p1][p2].start(),probes[p1][p2].length()-1);

						int percentMinPosInProbe = (int)Math.round(minPosInProbe*99d/(probes[p1][p2].length()-1));
						int percentMaxPosInProbe = (int)Math.round(maxPosInProbe*99d/(probes[p1][p2].length()-1));

						if (probes[p1][p2].strand() == Location.REVERSE) {
							// We add counts from the reverse end.
							percentMinPosInProbe = 99-percentMinPosInProbe;
							percentMaxPosInProbe = 99-percentMaxPosInProbe;
							int temp = percentMinPosInProbe;
							percentMinPosInProbe = percentMaxPosInProbe;
							percentMaxPosInProbe = temp;
						}

						for (int pos = percentMinPosInProbe;pos<=percentMaxPosInProbe;pos++) {
							theseCounts[(stores.length*p1)+d][pos]++;
						}
					}
				}
			}
		}

		double [][] returnedCounts = new double [stores.length*probes.length][100];

		// Correct for total read count if needed
		if (prefs.correctForTotalCount) {

			// We scale up smaller sets to match the largest set
			// so we need to find the largest set
			double largestSet = 0;
			for (int i=0;i<stores.length;i++) {
				if (stores[i].getTotalReadCount()>largestSet) largestSet = stores[i].getTotalReadCount();
			}

			// Now we can correct for our total counts
			for (int i=0;i<stores.length;i++) {
				System.out.println("Looking at store "+i);
				double correction = largestSet/stores[i].getTotalReadCount();

				for (int d=0;d<theseCounts.length;d++) {
										
					if (d%stores.length == i) {
						for (int p=0;p<theseCounts[d].length;p++) {
							returnedCounts[d][p] = (theseCounts[d][p] * correction)/probes[d/stores.length].length;
						}
					}
				}
			}
		}

		else {

			// Just transfer the long data over into the double structure
			for (int i=0;i<theseCounts.length;i++) {
				for (int p=0;p<theseCounts[i].length;p++) {
					returnedCounts[i][p] = theseCounts[i][p] / (double)probes[i/stores.length].length;
				}

			}

		}

		return returnedCounts;
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
		int totalProbes = 0;

		for (int p1=0;p1<probes.length;p1++) {
			for (int p2=0;p2<probes[p1].length;p2++) {
				totalProbes++;
				if (lengthCount.containsKey(probes[p1][p2].length())) {
					lengthCount.put(probes[p1][p2].length(), lengthCount.get(probes[p1][p2].length())+1);
				}
				else {
					lengthCount.put(probes[p1][p2].length(),1);
				}
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

		if (prefs.forceRelative) {
			sameLength = false;
		}
		else {
			if ((biggestCount*100)/totalProbes > 95) {
//				System.out.println("Biggest count is for size "+fixedLength+" with "+biggestCount+" out of "+totalProbes);
				sameLength = true;
			}
			else {
				sameLength = false;
			}
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
		if (e.getX()>10 && e.getX()<getWidth()-10) {
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
