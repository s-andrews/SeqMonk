/**
 * Copyright 2009-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Correlation.PearsonCorrelation;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

/**
 * The Class AlignedSummaryPanel draws an aligned density plot from a set of probes
 */
public class AlignedSummaryPanel extends JPanel implements Runnable, Cancellable {


	/** The probes. */
	private Probe [] probes;
	private ProbeList list;

	/** The store. */
	private DataStore store;

	/** The order to sort the probes for display **/
	private Integer [] sortOrder;

	/** The raw counts **/
	private int [][] rawCounts = null;

	/** The smoothed counts **/
	private float [][] smoothedCounts = null;

	/** The prefs. */
	private AlignedSummaryPreferences prefs;

	/** The max count. */
	private float maxCount = 0;

	/** The probes calculated. */
	private int percentCalculated = 0;

	/** A flag to say if we're doing a fixed length plot */
	private boolean sameLength = true;

	/** If we're doing a fixed length plot this says what length we're using */
	private int fixedLength;

	/** This says over how many points we should smooth the data */
	private int smoothingLevel = 1;

	/** This says what actual cutoff we're using for the max intensity **/
	private float maxIntensity = 1;

	/** An instance of the smoother class to asynchronously smooth our data */
	Smoother smoother = new Smoother();

	/** A set of colors to use for the plots */
	private Color [] colors = new Color[256];

	/** A flag to say if we can abandon calculating the plot */
	private boolean cancel = false;

	/** Objects listening to our progress **/
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();


	/**
	 * Instantiates a new trend over probe panel.
	 * 
	 * @param probes the probes
	 * @param stores the DataStores to plot
	 * @param prefs the preferences file for this plot
	 */

	//	public AlignedSummaryPanel (Probe [] probes, DataStore store, AlignedSummaryPreferences prefs) {
	//		this(probes,store,prefs,0);
	//	}

	public AlignedSummaryPanel (ProbeList list, DataStore store, AlignedSummaryPreferences prefs, int index, Integer [] sortOrder) {

		this.list = list;
		this.probes = list.getAllProbes();
		Arrays.sort(this.probes);
		
		this.store = store;
		this.prefs = prefs;
		this.sortOrder = sortOrder;

		Color baseColour = ColourIndexSet.getColour(index);
		int baseR = baseColour.getRed();
		int baseG = baseColour.getGreen();
		int baseB = baseColour.getBlue();

//		System.err.println("Base colour is "+baseR+","+baseG+","+baseB);

		for (int i=0;i<256;i++) {

			// We scale from the starting position to 255 in 256
			// steps
			int thisR = (int)((255-baseR)*((255-i)/256d))+baseR;
			int thisG = (int)((255-baseG)*((255-i)/256d))+baseG;
			int thisB = (int)((255-baseB)*((255-i)/256d))+baseB;

			colors[i] = new Color(thisR,thisG,thisB);
		}

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



	public void cancel() {
		cancel = true;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.WHITE);

		g.fillRect(0, 0, getWidth(), getHeight());

		FontMetrics metrics = getFontMetrics(g.getFont());

		if (smoothedCounts == null) {
			g.setColor(Color.GRAY);
			String message;
			if (rawCounts == null) {
				message = "Processed "+percentCalculated+" percent of probes";
			}
			else {
				message = "Clustered "+percentCalculated+" percent of probes";
			}
			g.drawString(message, (getWidth()/2)-(metrics.stringWidth(message)/2), (getHeight()/2-2));
			return;
		}

		// If we're here then we can actually draw the plot

		g.setColor(Color.BLACK);

		g.drawString(store.name(), (getWidth()/2)-(g.getFontMetrics().stringWidth(store.name())/2), g.getFontMetrics().getHeight());
		g.drawString(list.name(), (getWidth()/2)-(g.getFontMetrics().stringWidth(list.name())/2), g.getFontMetrics().getHeight()*2);

		// X axis
		//		g.drawLine(10, getHeight()-20, getWidth()-10, getHeight()-20);

		// Y axis
		//		g.drawLine(9, 10, 9, getHeight()-20);

		// X labels
		String xLabel;
		if (sameLength) {
			xLabel = "Bases 1 - "+fixedLength;
		}
		else {
			xLabel = "Relative distance across probe";
		}
		g.drawString(xLabel,(getWidth()/2)-(metrics.stringWidth(xLabel)/2),getHeight()-5);

		int lastY = 30;

		// If there are multiple lines with the same y value then we average across
		// them.  This variable keeps the total of the counts and the running count
		// keeps the number of samples merged so we can average out from that.
		float [] runningFloats = new float[smoothedCounts[0].length];
		Vector<float []> possibleLineData = new Vector<float[]>();

		// Now go through the various probes
		for (int p=0;p<smoothedCounts.length;p++) {

			int thisY = getY(p);

			//			System.out.println("Looking at probe "+p+" out of "+smoothedCounts.length+" with y-value of "+thisY+" and last of "+lastY);				

			if (thisY != lastY && possibleLineData.size() > 0) {
				//				System.out.println("Drawing cache from "+lastY+" to "+thisY);

				// We need to work out which data we're going to plot.  To keep the scaling looking right
				// the way we do this is to correlate each of the possible datasets to the average trace we
				// produced and take the most representative one.
				
				float [] dataToPlot;
				
				// Take the easy case first
				if (possibleLineData.size() == 1) {
					dataToPlot = possibleLineData.elementAt(0);
				}
				
				else {
					// We have to work out the best one to take.
					dataToPlot = possibleLineData.elementAt(0);
					float bestR = -2; // Start with a value we'll have to be better than!
					for (int i=0;i<possibleLineData.size();i++) {
						try {
							float thisR = PearsonCorrelation.calculateCorrelation(runningFloats, possibleLineData.elementAt(i));
							if (thisR > bestR) {
								bestR = thisR;
								dataToPlot = possibleLineData.elementAt(i);
							}
						}
						catch (SeqMonkException sme) {}
					}
				
				}
				
				
				int lastX = 10;

				for (int c=0;c<dataToPlot.length;c++) {

					Color color;
					if ((dataToPlot[c]) > maxIntensity) {
						color = colors[255];
					}
					else if (prefs.useLogScale) {
						int index = (int)((255 * Math.log((dataToPlot[c])+1))/Math.log(maxIntensity+1));
						//						System.out.println("Log index of "+(dataToPlot[c]/runningCount)+" vs "+maxCount+" is "+index);
						color = colors[index];					
					}
					else {
						int index = (int)(255 * (dataToPlot[c])/maxIntensity);
						//						System.out.println("Index of "+(dataToPlot[c]/runningCount)+" vs "+maxCount+" is "+index);					
						color = colors[index];
					}
					g.setColor(color);


					int thisX = 10;
					double xProportion = (double)c/(smoothedCounts[p].length-1);
					thisX += (int)(xProportion * (getWidth()-20));

					if (thisX == lastX) {
						//						System.out.println("Skipping position with no space");
						continue;
					}

					g.fillRect(lastX, lastY, thisX-lastX, thisY-lastY);

					lastX = thisX;
				}

				lastY = thisY;
				for (int i=0;i<runningFloats.length;i++) {
					runningFloats[i] = 0;
				}
				possibleLineData.clear();
			}

			else {
				for (int i=0;i<runningFloats.length;i++) {					
					runningFloats[i] = smoothedCounts[p][i];
				}
				possibleLineData.add(smoothedCounts[p]);
			}

		}

		//TODO: Print the left over probe?

	}

	/**
	 * Gets the y.
	 * 
	 * @param value the count
	 * @return the y
	 */
	public int getY (int probe) {
		double proportion = ((double)probe)/(smoothedCounts.length-1);

		int y = 30;

		y += (int)((getHeight()-50)*proportion);

		return y;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// Find out if all probes are the same length
		determineFixedLength();

		int [][]theseCounts;
		if (sameLength) {
			theseCounts = getFixedWidthCounts();
		}
		else {
			theseCounts = getVariableWidthCounts();
		}		

		if (theseCounts == null) {
			Enumeration<ProgressListener>en = listeners.elements();
			while (en.hasMoreElements()) {
				en.nextElement().progressCancelled();
			}
			return;
		}

		rawCounts = theseCounts;
		percentCalculated = 0;

		try {
			clusterCounts();
		} 
		catch (SeqMonkException e) {
			throw new IllegalStateException(e);
		}

		smoother.smooth();

		Enumeration<ProgressListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressComplete("aligned_probes", this);
		}

	}


	private void clusterCounts () throws SeqMonkException {


		if (sortOrder == null) {

			// If there's no defined sort order we simply order the counts from the highest to the lowest

			Arrays.sort(rawCounts,new Comparator<int []>() {

				public int compare(int[] o1, int[] o2) {
					int sumo1 = 0;
					for (int i=0;i<o1.length;i++) {
						sumo1 += o1[i];
					}

					int sumo2 = 0;
					for (int i=0;i<o2.length;i++) {
						sumo2 += o2[i];
					}

					return sumo2 - sumo1;
				}

			});
		}
		else {
			// We take the defined order we've been given
			int [][] orderedCounts = new int[rawCounts.length][];
			for (int i=0;i<orderedCounts.length;i++) {
				orderedCounts[i] = rawCounts[sortOrder[i]];
			}

			rawCounts = orderedCounts;
		}



	}

	/**
	 * Sets a new smoothing level and redraws the plot
	 * @param smoothingLevel
	 */
	public void setMaxIntensity (float maxIntensity) {
		this.maxIntensity = maxIntensity;
		repaint();
	}
	
	public float maxCount () {
		return maxCount;
	}


	/**
	 * Gets the fixed width counts.
	 * 
	 * @return the fixed width counts
	 */
	private int [][] getFixedWidthCounts () {
		int [][] theseCounts = new int [probes.length][fixedLength];

		for (int p=0;p<probes.length;p++) {

			int percent = (p*100)/probes.length;

			if (percent > percentCalculated) {
				if (cancel) return null;
				percentCalculated = percent;
				Enumeration<ProgressListener>en = listeners.elements();
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("", percentCalculated, 100);
				}
			}

			//			System.out.println("Looking at probe "+probes[p].toString());

			long [] reads = store.getReadsForProbe(probes[p]);
			//			System.out.println("Got "+reads.length+" reads from "+stores[d].name());
			for (int r=0;r<reads.length;r++) {

				// Check if we can skip this read as a duplicate
				if (prefs.removeDuplicates && r > 0) {
					if (SequenceRead.start(reads[r]) == SequenceRead.start(reads[r-1]) && SequenceRead.end(reads[r]) == SequenceRead.end(reads[r-1])) {
						continue;
					}
					System.err.println("Removed duplicate");
				}

				// Check if we're using reads in this direction
				if (! prefs.quantitationType.useRead(probes[p], reads[r])) {
					System.err.println("Remove rejected read type");
					continue;
				}			
				
				int startPos = Math.max(SequenceRead.start(reads[r])-probes[p].start(),0);
				int endPos = Math.min(SequenceRead.end(reads[r])-probes[p].start(),fixedLength-1);

				if (probes[p].strand() == Location.REVERSE) {
					// We add the positions from the back
					startPos = (theseCounts[p].length-1)-startPos;
					endPos = (theseCounts[p].length-1)-endPos;
					int temp = startPos;
					startPos = endPos;
					endPos = temp;
				}

				for (int pos = startPos;pos<=endPos;pos++) {
					theseCounts[p][pos]++;
				}
			}
		}

		return theseCounts;
	}

	/**
	 * Gets the variable width counts.
	 * 
	 * @return the variable width counts
	 */
	private int [][] getVariableWidthCounts () {

		// We divide the counts up into 200 bins rather than 
		// using the actual bp across the probe

		int [][] theseCounts = new int [probes.length][200];

		for (int p=0;p<probes.length;p++) {
			int percent = (p*100)/probes.length;

			if (percent > percentCalculated) {
				percentCalculated = percent;
				Enumeration<ProgressListener>en = listeners.elements();
				if (cancel) {
					return null;
				}
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("", percentCalculated, 100);
				}
			}

			//			System.out.println("Looking at probe "+probes[p].toString());

			long [] reads = store.getReadsForProbe(probes[p]);
			//			System.out.println("Got "+reads.length+" reads from "+stores[d].name());

			for (int r=0;r<reads.length;r++) {

				// Check if we can skip this read as a duplicate
				if (prefs.removeDuplicates && r > 0) {
					if (SequenceRead.start(reads[r]) == SequenceRead.start(reads[r-1]) && SequenceRead.end(reads[r]) == SequenceRead.end(reads[r-1])) {
						continue;
					}
				}

				// Check if we're using reads in this direction
				if (! prefs.quantitationType.useRead(probes[p], reads[r])) {
					continue;
				}

				int minPosInProbe = Math.max(SequenceRead.start(reads[r])-probes[p].start(),0);
				int maxPosInProbe = Math.min(SequenceRead.end(reads[r])-probes[p].start(),probes[p].length()-1);

				int percentMinPosInProbe = (int)Math.round(minPosInProbe*199d/(probes[p].length()-1));
				int percentMaxPosInProbe = (int)Math.round(maxPosInProbe*199d/(probes[p].length()-1));

				if (probes[p].strand() == Location.REVERSE) {
					// We add counts from the reverse end.
					percentMinPosInProbe = 199-percentMinPosInProbe;
					percentMaxPosInProbe = 199-percentMaxPosInProbe;
					int temp = percentMinPosInProbe;
					percentMinPosInProbe = percentMaxPosInProbe;
					percentMaxPosInProbe = temp;
				}

				for (int pos = percentMinPosInProbe;pos<=percentMaxPosInProbe;pos++) {
					theseCounts[p][pos]++;
				}
			}
		}

		return theseCounts;
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
		if (prefs.forceRelative) {
			sameLength = false;
		}

		// If the region they're using is more than 2000px wide we
		// always force a relative view
		else if (fixedLength > 5000) {
			sameLength = false;
		}
		else {
			if ((biggestCount*100)/probes.length > 95) {
				sameLength = true;
			}
			else {
				sameLength = false;
			}
		}
	}

	private class Smoother implements Runnable {

		private boolean running = false;
		private boolean stop = false;

		public synchronized void smooth () {

			if (running) {
				stop = true;
				while (running) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {}
				}
			}

			running = true;
			stop = false;
			Thread t = new Thread(this);
			t.start();


		}

		/**
		 * This smoothes the raw counts according to the currently
		 * set smoothing level.
		 */
		public void run() {

			//We may not have finished calculating yet

			if (rawCounts == null) return;

			float [][] newCounts = new float[rawCounts.length][];

			for (int line=0;line<rawCounts.length;line++) {

				int [] raw = rawCounts[line];
				float [] smoothed = new float [raw.length];

				
				//TODO: Why did we do this??  Can't see the point but I'm guessing
				// it's here for a reason.
				if (line % 1000 == 0) {
					try {
						Thread.sleep(2);
					} catch (InterruptedException e) {}
				}


				for (int pos=0;pos<raw.length;pos++) {
					int total = 0;
					int count = 0;

					for (int offset=0-smoothingLevel;offset<=smoothingLevel;offset++) {
						if (stop) {
							running = false;
							return;
						}
						if (pos+offset<0) continue;
						if (pos+offset>=raw.length) continue;
						total+=raw[pos+offset];
						count++;
					}
					smoothed[pos] = (float)total/count;
				}
				newCounts[line] = smoothed;

			}

			// Now we need to find the mix/max counts
			float newMaxCount = 0;

			for (int p=0;p<newCounts.length;p++) {
				for (int i=0;i<newCounts[p].length;i++) {
					if (newCounts[p][i]>newMaxCount) newMaxCount = newCounts[p][i];
				}
			}

			maxCount = newMaxCount;
			// We start with the max intensity being the max count.
			maxIntensity = maxCount;

			smoothedCounts = newCounts;
			running = false;

			repaint();
		}			

	}


}
