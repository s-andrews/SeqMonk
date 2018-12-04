/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.LongVector;

/**
 * The Class ChromosomeDataTrack represents a single track in the
 * chromosome view containing the data from a single data store.
 * Depending on the display preferences it can show either just
 * raw data, or quantitated data, or both.
 */
public class ChromosomeDataTrack extends JPanel implements MouseListener, MouseMotionListener{

	/** The viewer. */
	private ChromosomeViewer viewer;

	/** The data collection */
	private DataCollection collection;

	/** The data. */
	private DataStore data;

	/** The name prefix (used for distinguishing datasets which are part of a replicate set) **/
	private ReplicateSet enclosingSet = null;

	/** The reads. */
	private long [] reads = new long[0];

	/** The probes. */
	private Probe [] probes;

	/** The width. */
	private int width;

	/** The last cached height. */
	private int height;

	/** The last cached read density. */
	private int lastReadDensity;

	/** The last cached split mode */
	private int lastSplitMode;

	/** The last cached read density. */
	private int thisReadDensity;

	/** The last cached read colours setting */
	private int dataColours;

	/** The drawn reads. */
	private Vector<DrawnRead> drawnReads = new Vector<DrawnRead>();

	/** The set of drawn probe positions. */
	private Vector<DrawnProbe> drawnProbes = new Vector<DrawnProbe>();

	/** The active read. */
	private long activeRead = 0;

	/** The active read index. */
	private int activeReadIndex;

	/** The active probe. */
	private Probe activeProbe = null;

	/** The max quantitated value. */
	private float maxValue;

	/** The min quantitated value. */
	private float minValue;

	/** Stores the packed slot index for each read in this display */
	private int [] slotValues = null;

	/** Stores the Y axis base position for each lane of sequence reads */
	private int [] slotYValues = null;

	/** The df. */
	private static DecimalFormat df = new DecimalFormat("#.###");

	/** Whether we're drawing probes. */
	private boolean drawProbes;

	/** Whether we're drawing reads. */
	private boolean drawReads;

	/** Whether this is a hic dataset */
	private boolean isHiC;

	/** Whether we're splitting the quanitated view into positive and negative scales. */
	private boolean showNegative;

	/** The height of each read */
	private int readHeight = 5;

	/** The amount of space between reads */
	private int readSpace = 4;

	// This flag is a kludge to work around an antialiasing bug in openJDK which
	// causes refreshes to take forever.  Until they fix this we're going to
	// disable AA on openJDK installations.
	/** A flag to say if we're enabling anti-aliasing */
	private boolean useAntiAliasing = true;

	/* This is a cached value.  We set it to false so it
	 * will be wrong the first time it is checked and will
	 * be changed.
	 */ 
	/** The last draw probes. */
	private boolean lastDrawProbes = false;

	// These are used to optimise the drawing routines
	/** The last probe x end. */
	private int lastProbeXEnd = 0;
	private int lastProbeXMid = 0;
	private int lastProbeY = 0;

	private int lastReadIndexStart = 0;

	/** The last probe value. */
	private double lastProbeValue = 0;

	/** The last read x ends. */
	private int [] lastReadXEnds = new int[0];

	private int [][] cachedHiCPixelCounts = null;
	private int lastStart = 0;
	private int lastEnd = 0;
	private int lastWidth = 0;

	private long [] lastSourceHiC = null;
	private long [] lastHitHic = null;
	private Chromosome lastChromosome = null;
	private int lastInteractionIndexStart = 0;


	/**
	 * Instantiates a new chromosome data track.
	 * 
	 * @param viewer the viewer
	 * @param collection the collection
	 * @param data the data
	 */
	public ChromosomeDataTrack (ChromosomeViewer viewer, DataCollection collection, DataStore data) {
		this.viewer = viewer;
		this.data = data;
		this.collection = collection;
		isHiC = data instanceof HiCDataStore && ((HiCDataStore)data).isValidHiC();
		updateReads();
		//		System.out.println("Chr"+viewer.chromosome().name()+" has "+probes.length+" probes on it");
		addMouseMotionListener(this);
		addMouseListener(this);

		//TODO: Disable this as soon as the openJDK AA bug is fixed
		//This disables anti-aliasing on openJDK installations since
		//they currently have a bug which makes refreshes *really* slow.
		//
		// Since openJDK looks exactly the same as Sun's JDK from System.property
		// then we have to use a big hammer and disable AA under linux, which sucks.
		if (System.getProperty("os.name").toLowerCase().contains("linux")) {
			useAntiAliasing = false;
		}

	}
	
	public DataStore getStore () {
		return data;
	}


	public Dimension getMinimumSize () {
		return new Dimension(1, 1);
	}


	/**
	 * This call can be made whenever the reads need to be updated - particularly when
	 * the viewer is being switched to show a different chromosome.
	 */
	public void updateReads() {
		if (collection.probeSet() != null) {
			probes = collection.probeSet().getActiveList().getProbesForChromosome(DisplayPreferences.getInstance().getCurrentChromosome());			
		}
		else {
			probes = new Probe[0];
		}
		lastReadIndexStart = 0;
		lastInteractionIndexStart = 0;

		Arrays.sort(probes);

		//Force the slots to be reassigned
		height = 0;

		repaint();
	}

	public void setEnclosingReplicateSet (ReplicateSet repSet) {
		this.enclosingSet = repSet;
		repaint();
	}


	/**
	 * Assign slots.
	 */
	private void assignSlots () {

		// We need to stack all of the reads in a set of available slots.
		// Each read is [readHeight] high and slots have a [readSpace] gap between them.

		// The quick thing to do is to check if the height has changed since
		// the last time we looked.  We also need to check whether we're
		// drawing probes as that will change how we assign slots.

		if (getHeight() == height && DisplayPreferences.getInstance().getReadDisplay() == lastSplitMode && thisReadDensity == lastReadDensity && drawProbes == lastDrawProbes) {
			// Nothing to do.
			return;
		}
		
		// We can also skip most of this if the height isn't the same, but the number of slots 
		// doesn't change.
		boolean checkSlotCount = false;
		if (DisplayPreferences.getInstance().getReadDisplay() == lastSplitMode && thisReadDensity == lastReadDensity && drawProbes == lastDrawProbes) {
			checkSlotCount = true;
		}
		
		// Cache the values so we might be able to skip this next time.
		height = getHeight();
		lastReadDensity = thisReadDensity;
		lastDrawProbes = drawProbes;
		lastSplitMode = DisplayPreferences.getInstance().getReadDisplay();

		// Lets recalculate the slot values

		/* 
		 * Each slot is a shaded area of [readHeight]px separated by a
		 * blank [readSpace]px area.  This means there are [readHeight+readSpace]px between
		 * adjacent slots.  Because height might not be even
		 * we need to calculate for the smallest half (hence
		 * the divide by 2 and later multiply by 2.
		 *
		 * Finally we leave the top and bottom slots empty so
		 * we can distinguish between tracks (hence the -2 at
		 * the end.
		 * 
		 * I've changed the -2 to -1 since there should always be an odd
		 * number of slots (a central one and then pairs around it)
		 * 
		 * If we don't have much space for each lane then we can get
		 * negative slot counts, and we can't let that happen!
		 * 
		 * We also calculate differently depending on whether we have to
		 * draw probes as well.  If we're drawing probes we only
		 * have half of the lane to work with.  If we're just
		 * drawing reads we've got the whole space.
		 */

		// We'll only use half of the height if we're either drawing probes, or if
		// we're a HiC dataset where the bottom half will show interactions.



		//		int halfHeightCorrection = (drawProbes ? 2 : 1);
		int halfHeightCorrection = 1;

		if (drawProbes || isHiC) {
			halfHeightCorrection = 2;
		}

		/*
		 * This gets a value of 2 if we're drawing probes as well and 1
		 * if we're not.
		 */


		int slotCount = (((height/(2*halfHeightCorrection))/(readHeight+readSpace))*2)-1;
		if (slotCount < 1) slotCount = 1;
		
		slotYValues = new int [slotCount];

		//		System.err.println("There will be "+slotYValues.length+" slots");

		int mid = height/(2*halfHeightCorrection);
		for (int i=0;i<slotYValues.length;i++) {
			if (i==0) {
				slotYValues[i] = mid;
			}
			else if (i%2==0) {
				// We're going down
				slotYValues[i] = mid + ((readHeight+readSpace)*(i/2));
			}
			else {
				// We're going up
				slotYValues[i] = mid - ((readHeight+readSpace)*((i+1)/2));
			}
		}

		// See if we can bail out at this point with the same reads in the same
		// slots (even though the slots might have moved slightly)
		
		if (checkSlotCount && slotCount == lastReadXEnds.length) {
			return;
		}
		
		// We now need to assign each probe to a slot

		// We're going to go back to the original source for the reads.  That way we only need to keep 
		// hold of the ones which are assignable in this height of view which could save us a lot of
		// memory

		ReadsWithCounts rwc = data.getReadsForChromosome(DisplayPreferences.getInstance().getCurrentChromosome());

		// We'll start a temporary list of the reads which we can draw, and this will be what we put together.
		LongVector drawableReads = new LongVector();
		IntVector drawableSlotValues = new IntVector();

		// We can also make the array of cached positions to optimise drawing
		lastReadXEnds = new int [slotCount];		

		// The lastBase array keeps track of the last
		// base to be drawn in each slot.
		int [] lastBase = new int [slotCount];
		for (int i=0;i<lastBase.length;i++) {
			lastBase[i] = 0;
		}

		// Now we go through the reads assigning them to a slot

		// If we're using combined mode we put reads wherever we can
		// fit them
		if (lastSplitMode == DisplayPreferences.READ_DISPLAY_COMBINED) {

			// To save doing a lot of processing we're going to cache the
			// next available position if we're off the end of the display
			// so we can quickly skip over reads which are never going to
			// fit
			int nextPossibleSlot = 0;

			POSITION: for (int r=0;r<rwc.reads.length;r++) {

				long read = rwc.reads[r];

				// There's never any point in checking more reads for the same
				// position than the number of slots, so we can optimise this a
				// bit.
				READ: for (int c=0;c<Math.min(rwc.counts[r],slotCount);c++) {


					if (nextPossibleSlot != 0) {
						// See if we can quickly skip this read
						if (nextPossibleSlot > SequenceRead.start(reads[r])) {
							continue POSITION;
						}
						else {
							// Reset this as we're adding reads again.
							nextPossibleSlot = 0;
						}
					}

					for (int s=0;s<slotCount;s++) {
						if (lastBase[s] < SequenceRead.start(read)) {
							drawableReads.add(read);
							drawableSlotValues.add(s);
							lastBase[s] = SequenceRead.end(read);
							continue READ;
						}	
					}

					// If we get here then we don't have enough
					// slots to draw the reads in this chromosome.
					// In this case we just don't draw them in this
					// display.  That means we don't add them to 
					// the drawable slots or drawable reads

					// Now set the nextPossibleSlot value so we can 
					// skip stuff quickly in future
					for (int s=0;s<slotCount;s++) {
						if (lastBase[s] < nextPossibleSlot) nextPossibleSlot = lastBase[s];
					}
				}
			}
		}

		else if (lastSplitMode == DisplayPreferences.READ_DISPLAY_SEPARATED) {
			// If we're using split mode then reads of unknown strand go
			// in the middle line.  Forward reads go above that and reverse
			// reads go below.

			int nextPossibleSlot = 0;

			POSITION: for (int r=0;r<rwc.reads.length;r++) {

				long read = rwc.reads[r];


				// Only check up to half the slot count reads, since we
				// can't possibly place the same read more times than that
				READ: for (int c=0;c<Math.min(rwc.counts[r],(slotCount/2)+1);c++) {

					if (nextPossibleSlot != 0) {
						// See if we can quickly skip this read
						if (nextPossibleSlot > SequenceRead.start(reads[r])) {
							continue POSITION;
						}
						else {
							// Reset this as we're adding reads again.
							nextPossibleSlot = 0;
						}
					}


					int startSlot = 0;
					int interval = slotCount;
					if (SequenceRead.strand(read) == Location.FORWARD) {
						startSlot = 1;
						interval = 2;
					}
					else if (SequenceRead.strand(read) == Location.REVERSE) {
						startSlot = 2;
						interval = 2;
					}
					for (int s=startSlot;s<slotCount;s+=interval) {
						if (lastBase[s] < SequenceRead.start(read)) {
							drawableSlotValues.add(s);
							drawableReads.add(read);
							lastBase[s] = SequenceRead.end(read);
							continue READ;
						}
					}

					// If we get here then we don't have enough
					// slots to draw the reads in this chromosome.
					// In this case we just don't draw them in this
					// display.  That just means we don't add them
					// to anything.

					// Now set the nextPossibleSlot value so we can 
					// skip stuff quickly in future
					for (int s=0;s<slotCount;s++) {
						if (lastBase[s] < nextPossibleSlot) nextPossibleSlot = lastBase[s];
					}


				}
			}
		}

		reads = drawableReads.toArray();
		slotValues = drawableSlotValues.toArray();

	}

	private int [][] getHiCPixelCounts () {

		// Check if we're able to use the last cached data.
		if (lastWidth == width && viewer.currentStart() == lastStart && viewer.currentEnd() == lastEnd && cachedHiCPixelCounts != null) {
			return cachedHiCPixelCounts;
		}

		lastWidth = width;
		lastStart = viewer.currentStart();
		lastEnd = viewer.currentEnd();


		cachedHiCPixelCounts = new int [width+1][width+1];

		if (lastChromosome != viewer.chromosome()) {
			HiCHitCollection col = ((HiCDataStore)data).getHiCReadsForChromosome(viewer.chromosome());
			lastSourceHiC = col.getSourcePositionsForChromosome(viewer.chromosome().name());
			lastHitHic = col.getHitPositionsForChromosome(viewer.chromosome().name());
			lastChromosome = viewer.chromosome();

			// Check if the HiC collection is sorted.
			//			for (int i=1;i<lastSourceHiC.length;i++) {
			//				if (SequenceRead.start(lastSourceHiC[i-1]) > SequenceRead.start(lastSourceHiC[i])) {
			//					throw new IllegalStateException("HiC set isn't sorted");
			//				}
			//			}

		}

		// No need to do anything more if there's no data
		if (lastSourceHiC.length == 0) return cachedHiCPixelCounts;

		// We can use the last cached start position to more quickly find where we
		// need to start looking



		// Go back until we're sure we're not going to see any more reads
		for (int i=lastInteractionIndexStart;i>=0;i--) {
			if (i >= lastSourceHiC.length) continue;

			if (SequenceRead.start(lastSourceHiC[i]) < viewer.currentStart()-data.getMaxReadLength()) {
				lastInteractionIndexStart = i;
				break;
			}
		}

		// Go forward until we hit our first read
		for (int i=lastInteractionIndexStart;i<reads.length;i++) {
			if (SequenceRead.start(lastSourceHiC[i]) >= viewer.currentStart()-data.getMaxReadLength()) {
				lastInteractionIndexStart = i;
				break;
			}
		}



		for (int i=lastInteractionIndexStart;i<lastSourceHiC.length;i++) {
			// Check if both of the ends are in the current window
			int sourceMidPoint = SequenceRead.midPoint(lastSourceHiC[i]);
			if (SequenceRead.start(lastSourceHiC[i]) > viewer.currentEnd()) {
				break;
			}
			if (sourceMidPoint < viewer.currentStart() || sourceMidPoint > viewer.currentEnd()) {
				continue;
			}

			int hitMidPoint = SequenceRead.midPoint(lastHitHic[i]);
			if (hitMidPoint < viewer.currentStart() || hitMidPoint > viewer.currentEnd()) {
				continue;
			}

			int sourcePixel = bpToPixel(Math.min(sourceMidPoint,hitMidPoint));
			int hitPixel = bpToPixel(Math.max(sourceMidPoint,hitMidPoint));

			//			System.err.println("Found valid interaction between "+sourceMidPoint+" and "+hitMidPoint+" which is "+sourcePixel+" to "+hitPixel);


			// Now we can add the interaction to the matrix
			cachedHiCPixelCounts[sourcePixel][hitPixel]++;
		}

		return cachedHiCPixelCounts;
	}



	/**
	 * Gets the min max probe values.
	 * 
	 * @return the min max probe values
	 */
	protected float [] getMinMaxProbeValues () {
		/**
		 * This method is used to auto scale the data zoom level.  What
		 * we actually return is an array of two values (min and max) which
		 * are the 10th and 90th percentile of all of the data values on this
		 * chromosome.
		 * 
		 * We need to account for the fact that some of the values might be NaN
		 * if no value could be assigned to them.
		 */

		// If this data store isn't quantitated then we don't need to bother
		// going any further.  We return the smallest range it's possible to
		// use anyway.
		if (! data.isQuantitated()) return new float[] {0,1};

		// It's also possible for this data to be quantitated but not to have
		// any probes on this chromosome.
		if (probes.length == 0) return new float[] {0,1};

		float [] values = new float [probes.length];

		for (int i=0;i<probes.length;i++) {
			try {
				values[i] = data.getValueForProbe(probes[i]);
				// We don't want to return stupid numbers or stuff will break.  Convert
				// anything problematic to NaN and we'll deal with this later.
				if (Float.isNaN(values[i])  || Float.isInfinite(values[i])) {
					values[i] = Float.NaN;
				}
			} 
			catch (SeqMonkException e) {
				values[i] = 0;
			}
		}

		Arrays.sort(values);

		float [] minMax = new float[2];

		int lastValidIndex = values.length-1;
		for (;lastValidIndex>=0;lastValidIndex--) {
			if (!Float.isNaN(values[lastValidIndex])) {
				break;
			}
		}

		if (lastValidIndex <= 0) {
			// There are no valid values
			return new float[]{0,1};
		}

		minMax[0] = values[(int)(lastValidIndex * 0.1)];
		minMax[1] = values[(int)(lastValidIndex * 0.9)];

		return minMax;
	}

	/***
	 * This call is made by the ChromosomeViewer when the active
	 * probe list is changed.  It allows us to update the set of
	 * probes without having to do a completely new layout (which
	 * is slow).
	 * 
	 * @param newProbes The new probes for the currently displayed chromosome
	 */
	protected void setProbes (Probe [] newProbes) {
		probes = newProbes;
		Arrays.sort(probes);
		repaintQuantitation();
	}

	/**
	 * Repaint quantitation.
	 */
	protected void repaintQuantitation () {
		// See if the quantitation is actually showing
		if (DisplayPreferences.getInstance().getDisplayMode() == DisplayPreferences.DISPLAY_MODE_READS_ONLY) return;
		if (DisplayPreferences.getInstance().getDisplayMode() == DisplayPreferences.DISPLAY_MODE_QUANTITATION_ONLY) {
			repaint();
		}
		else if (DisplayPreferences.getInstance().getDisplayMode() == DisplayPreferences.DISPLAY_MODE_READS_AND_QUANTITATION) {
			repaint(0,getHeight()/2,getWidth(),getHeight()/2);
		}
		else {
			throw new IllegalStateException("Unknown display mode "+DisplayPreferences.getInstance().getDisplayMode());
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
		
		if (getHeight() == 0) return;
		
		if (useAntiAliasing && g instanceof Graphics2D) {
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}

		int displayMode = DisplayPreferences.getInstance().getDisplayMode();
		int scaleMode = DisplayPreferences.getInstance().getScaleType();
		dataColours = DisplayPreferences.getInstance().getColourType();
		thisReadDensity = DisplayPreferences.getInstance().getReadDensity();

		drawProbes = true;
		drawReads = true;
		showNegative = true;

		if (displayMode == DisplayPreferences.DISPLAY_MODE_QUANTITATION_ONLY) {
			drawReads = false;
		}
		if (displayMode == DisplayPreferences.DISPLAY_MODE_READS_ONLY) {
			drawProbes = false;
		}
		if (scaleMode == DisplayPreferences.SCALE_TYPE_POSITIVE) {
			showNegative = false;
		}

		if (thisReadDensity == DisplayPreferences.READ_DENSITY_LOW) {
			readHeight = 3;
			readSpace = 2;
		}
		else if (thisReadDensity == DisplayPreferences.READ_DENSITY_MEDIUM) {
			readHeight = 2;
			readSpace = 1;
		}
		else if (thisReadDensity == DisplayPreferences.READ_DENSITY_HIGH) {
			readHeight = 1;
			readSpace = 1;
		}


		if (drawReads) {
			assignSlots();
		}

		// Cache a copy of the current reads array so we don't get it changed from under us
		long [] reads = this.reads;

		drawnReads.removeAllElements();
		drawnProbes.removeAllElements();
		height = getHeight();
		maxValue = (float)DisplayPreferences.getInstance().getMaxDataValue();
		if (showNegative) {
			minValue = 0-maxValue;
		}
		else {
			minValue = 0;
		}

		width = getWidth();

		// Otherwise we alternate colours so we can see the difference
		// between tracks.
		if (viewer.getIndex(this)%2 == 0) {
			g.setColor(ColourScheme.DATA_BACKGROUND_EVEN);
		}
		else {
			g.setColor(ColourScheme.DATA_BACKGROUND_ODD);
		}

		g.fillRect(0,0,width,height);

		// If we're in the middle of making a selection then highlight the
		// selected part of the display in green.

		if (viewer.makingSelection()) {
			int selStart = viewer.selectionStart();
			int selEnd = viewer.selectionEnd();
			int useStart = (selEnd > selStart) ? selStart : selEnd;
			int selWidth = selEnd-selStart;
			if (selWidth < 0) selWidth = 0-selWidth;
			g.setColor(ColourScheme.DRAGGED_SELECTION);
			g.fillRect(useStart,0,selWidth,height);
		}

		int startBp = viewer.currentStart();
		int endBp = viewer.currentEnd();

		if (drawProbes) {

			if (data.isQuantitated()) {

				// If we're showing negative values put a line in the middle to show
				// the origin
				if (showNegative) {
					g.setColor(Color.LIGHT_GRAY);
					if (drawReads) {
						g.drawLine(0, (height*3)/4, width, (height*3)/4);
					}
					else {
						g.drawLine(0, height/2, width, height/2);					
					}
				}


				// Now go through all the probes figuring out whether they
				// need to be displayed

				// Reset the values used to optimise drawing
				lastProbeXEnd = 0;
				lastProbeXMid = 0;
				lastProbeY = 0;
				lastProbeValue = 0;

				for (int i=0;i<probes.length;i++) {
					//				System.out.println("Looking at read "+i+" Start: "+reads[i].start()+" End: "+reads[i].end()+" Global Start:"+startBp+" End:"+endBp);
					if (probes[i].end() > startBp && probes[i].start() < endBp) {
						drawProbe(probes[i],g,false);
					}
				}

				// Always draw the active probe last so it stays on top, unless we're doing a line graph
				if (activeProbe != null && DisplayPreferences.getInstance().getGraphType() != DisplayPreferences.GRAPH_TYPE_LINE) {
					drawProbe(activeProbe, g, true);
				}
			}
			else {
				// This DataStore isn't quantitated
				g.setColor(Color.GRAY);
				if (displayMode == DisplayPreferences.DISPLAY_MODE_QUANTITATION_ONLY) {
					g.drawString("DataStore Not Quantitated", ((width/2)-20), (getHeight()/2)-2);
				}
				else {
					g.drawString("DataStore Not Quantitated", ((width/2)-20), ((getHeight()*3)/4)-2);					
				}
			}
		}

		if (drawReads && reads.length > 0) {

			// Wipe the cached end values
			for (int i=0;i<lastReadXEnds.length;i++) {
				lastReadXEnds[i] = 0;
			}

			// Now go through all the reads figuring out whether they
			// need to be displayed

			// Find where we need to start 

			//			System.err.println("Starting looking at "+lastReadIndexStart+" at pos "+SequenceRead.start(reads[lastReadIndexStart])+" startBp is "+startBp);

			// Go back until we're sure we're not going to see any more reads
			boolean hitLimit = false;
			if (lastReadIndexStart > reads.length-1) {
				lastReadIndexStart = reads.length-1;
			}
			for (int i=lastReadIndexStart;i>=0;i--) {
				if (SequenceRead.start(reads[i]) < startBp-data.getMaxReadLength()) {
					lastReadIndexStart = i;
					hitLimit=true;
					break;
				}
			}

			if (! hitLimit) {
				//				System.err.println("Didn't hit the limit so bailing out");
				lastReadIndexStart = 0;
			}

			//			System.err.println("Went back to "+lastReadIndexStart+" at pos "+SequenceRead.start(reads[lastReadIndexStart]));


			// Go forward until we hit our first read
			for (int i=lastReadIndexStart;i<reads.length;i++) {
				if (SequenceRead.start(reads[i]) >= startBp-data.getMaxReadLength()) {
					lastReadIndexStart = i;
					break;
				}
			}
			//			System.err.println("Went forward to "+lastReadIndexStart+" at pos "+SequenceRead.start(reads[lastReadIndexStart]));

			//			System.err.println("Starting at index "+lastReadIndexStart+" with start position "+SequenceRead.start(reads[lastReadIndexStart])+"\n");

			for (int i=lastReadIndexStart;i<reads.length;i++) {
				//				System.out.println("Looking at read "+i+" Start: "+reads[i].start()+" End: "+reads[i].end()+" Global Start:"+startBp+" End:"+endBp);
				if (SequenceRead.end(reads[i]) > startBp && SequenceRead.start(reads[i]) < endBp) {
					drawRead(reads[i],i,g);
				}
				if (SequenceRead.start(reads[i])>endBp) break;
			}

			// Always draw the active read last
			if (activeRead != 0) {
				drawRead(activeRead, activeReadIndex, g);
			}
		}

		if (drawReads && reads.length > 0 && ! drawProbes && isHiC) {
			int [][] hicPixelCounts = getHiCPixelCounts();

			boolean foundSomething = false;
			boolean drawnSomthing = false;

			int drawnCount = 0;

			INTERACTIONS: for (int length=width;length>=1;length--) {
				for (int x1=0;x1<width-length;x1++) {
					int x2=x1+length;

					if (hicPixelCounts[x1][x2] != 0) foundSomething = true;

					if (hicPixelCounts[x1][x2] > maxValue) {
						++drawnCount;

						// Once we've drawn 5k interactions there's no room for
						// anything else on screen and everything gets super slow.
						if (drawnCount >= 10000) break INTERACTIONS;

						// Draw this interaction.
						drawnSomthing = true;
						// Work out the height of the crossover.  This is proportional to the distance between the pairs
						// and the total width of the window.

						double proportion = (x2-x1)/(double)width;
						int interactionHeight = (int)((height/2)*proportion);

						// Scale the colours by distance so you can tell where things go
						g.setColor(DisplayPreferences.getInstance().getGradient().getColor((height/2)-interactionHeight,0,height/2));

						// Draw the interaction
						int midPoint = x1+((x2-x1)/2);
						g.drawLine(x1, height/2, midPoint, (height/2)+interactionHeight);
						g.drawLine(midPoint, (height/2)+interactionHeight,x2,height/2);

					}
				}
			}

			//			System.err.println("Drew "+drawnCount+" interactions");

			if (foundSomething && ! drawnSomthing) {
				g.setColor(Color.GRAY);
				g.drawString("Lower the data zoom level to see HiC interactions", ((width/2)-100), ((getHeight()*3)/4)-2);					

			}
			if (drawnCount >= 10000 && maxValue < 2) {
				g.setColor(Color.DARK_GRAY);
				g.drawString("Increase the data zoom level to filter HiC interactions", ((width/2)-100), ((getHeight()*3)/4)-2);									
			}

		}

		//		System.out.println("Drew "+drawnReads.size()+" reads");

		// Draw a line across the bottom of the display if there is space
		if (getHeight()>=10) {
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(0, height-1, width, height-1);


			// If we're the active data store then surround us in red

			// This can fail if the viewer is being destroyed (viewer returns null)
			// so catch this
			try {
				if (viewer.application().dataCollection().getActiveDataStore() == data || (enclosingSet != null && viewer.application().dataCollection().getActiveDataStore() == enclosingSet)) {
					g.setColor(Color.RED);
					g.drawLine(0, height-2, width, height-2);
					g.drawLine(0, height-1, width, height-1);
					g.drawLine(0, 0, width, 0);
					g.drawLine(0, 1, width, 1);
				}
			}
			catch (NullPointerException npe) {}
		}

		String name = data.name();

		if (data instanceof HiCDataStore && ((HiCDataStore)data).isValidHiC()) {
			name = "[HiC] "+name;
		}

		if (enclosingSet != null) {
			name = "["+enclosingSet.name()+"] "+name;
		}

		int fontSize = 12;
		int nameHeight = height;
		while (fontSize >1) {

			g.setFont(new Font("Default",Font.PLAIN,fontSize));
			nameHeight = g.getFontMetrics().getAscent()+g.getFontMetrics().getDescent();
			if (nameHeight<getHeight()) break;

			fontSize--;

		}

		// Draw a box into which we'll put the track name so it's not obscured
		// by the data
		int nameWidth = g.getFontMetrics().stringWidth(name);

		if (viewer.getIndex(this)%2 == 0) {
			g.setColor(ColourScheme.DATA_BACKGROUND_EVEN);
		}
		else {
			g.setColor(ColourScheme.DATA_BACKGROUND_ODD);
		}

		g.fillRect(0, (height/2)-(nameHeight/2), nameWidth+5, nameHeight);

		// Finally draw the name of the data track
		g.setColor(Color.GRAY);
		g.drawString(name,2,(height/2)+((nameHeight/2)-g.getFontMetrics().getDescent()));
	}

	/**
	 * Draw read.
	 * 
	 * @param r the r
	 * @param index the index
	 * @param g the g
	 */
	private void drawRead (long r, int index,  Graphics g) {

		// Occasionally we can get caught in the middle of an update to the
		// slots.  Just bail out if it looks like the slot data doesn't fit
		// any more.
		if (index >= slotValues.length) return;


		// Skip this read if it didn't fit into this display
		if (slotValues[index] == -1) return;

		int wholeXStart = bpToPixel(SequenceRead.start(r));
		int wholeXEnd = bpToPixel(SequenceRead.end(r));

		//		System.out.println("Drawing read from "+r.start()+"-"+r.end()+" "+wholeXEnd+"-"+wholeXEnd+" lastX slot"+slotValues[index]+" end "+lastReadXEnds[slotValues[index]]);

		// We make sure that this new read is at least 3px wide
		if ((wholeXEnd-wholeXStart)<3) {
			wholeXEnd = wholeXStart+3;
		}

		// We skip this read unless it makes it at least 2px beyond
		// the last read in this track
		if (lastReadXEnds[slotValues[index]] >= (wholeXEnd-2) && r != activeRead) {
			//			System.out.println("Skipping");
			return;
		}



		lastReadXEnds[slotValues[index]] = wholeXEnd;

		if (r == activeRead && index == activeReadIndex) {
			g.setColor(ColourScheme.ACTIVE_FEATURE);
		}
		else if (r == activeRead) {
			g.setColor(ColourScheme.ACTIVE_FEATURE_MATCH);
		}
		else {
			if (SequenceRead.strand(r) == Location.FORWARD) {
				g.setColor(ColourScheme.FORWARD_FEATURE);
			}
			else if (SequenceRead.strand(r) == Location.REVERSE) {
				g.setColor(ColourScheme.REVERSE_FEATURE);
			}
			else {
				g.setColor(ColourScheme.UNKNOWN_FEATURE);
			}
		}

		int yBoxStart = slotYValues[slotValues[index]]-1;
		int yBoxEnd = yBoxStart+readHeight;

		//		System.err.println("Drawing read from "+wholeXStart+","+yBoxStart+","+wholeXEnd+","+yBoxEnd);
		drawnReads.add(new DrawnRead(wholeXStart, wholeXEnd, yBoxStart, yBoxEnd, index, r));

		//		System.out.println("Drawing probe from x="+wholeXStart+" y="+yBoxStart+" width="+(wholeXEnd-wholeXStart)+" height="+(yBoxEnd-yBoxStart));
		g.fillRect(wholeXStart,yBoxStart,(wholeXEnd-wholeXStart),yBoxEnd-yBoxStart);

	}

	/**
	 * Draw probe.
	 * 
	 * @param p the p
	 * @param g the g
	 */
	private void drawProbe (Probe p, Graphics g, boolean forceDraw) {

		int wholeXStart = bpToPixel(p.start());
		int wholeXEnd = bpToPixel(p.end()+1);
		if ((wholeXEnd-wholeXStart)<2) {
			wholeXEnd = wholeXStart+2;
		}

		float value;
		if (! data.hasValueForProbe(p)) return;

		try {
			value = data.getValueForProbe(p);
		} 
		catch (SeqMonkException e) {
			// Should have been caught by now, but better safe than sorry
			return;
		}

		if (Float.isNaN(value)) return;

		boolean valueIsNegative = value<0;

		// Restrict the range we cover...

		double origValue = value;

		if (value > maxValue) value = maxValue;
		if (value < minValue) value = minValue;


		// Don't draw probes which overlap exactly with the last one
		// and are of lower height
		if (!forceDraw) {
			if (wholeXEnd <= lastProbeXEnd) {

				if (lastProbeValue > 0 && value > 0 && value <= lastProbeValue) {
					return;
				}

				if (lastProbeValue < 0 && value < 0 && value >= lastProbeValue) {
					return;
				}

			}
		}

		// We only update the last value if we have moved past the end of the last probe
		// This might makes lines look a little odd in heavily overlapped datasets with
		// different probe lengths but it prevents drawing artefacts in bar graphs where
		// probes can appear and disappear.

		if (wholeXEnd > lastProbeXEnd) {
			lastProbeXEnd = wholeXEnd;
			lastProbeValue = value;
		}


		if (p == activeProbe) {
			g.setColor(ColourScheme.ACTIVE_FEATURE);
		}
		else {
			if (dataColours == DisplayPreferences.COLOUR_TYPE_GRADIENT) {
				g.setColor(DisplayPreferences.getInstance().getGradient().getColor(value,minValue,maxValue));
			}
			else {
				g.setColor(ColourIndexSet.getColour(viewer.getIndex(this)));
			}

		}


		int yBoxStart;
		int yBoxEnd;

		/*
		 * If we're drawing reads as well we can only take up half of
		 * the track.  If it's just probes we can take the whole track.
		 * 
		 * We also need to consider if we're showing negative values.  If
		 * we are then we draw from the middle of the track up or down
		 */

		int yValue;

		if (showNegative) {
			if (drawReads) {
				if (value > 0) {
					yBoxEnd = (height*3)/4;
					yBoxStart =  ((height*3)/4) - ((int)(((double)height/4)*(value/maxValue)));
					yValue = yBoxStart;
				}
				else {
					yBoxStart = (height*3)/4;
					yBoxEnd =  height - ((int)(((double)height/4)*((minValue-value)/minValue)));
					yValue = yBoxEnd;
				}
			}
			else {
				if (value > 0) {
					yBoxEnd = height/2;
					yBoxStart =  (height/2) - ((int)(((double)height/2)*(value/maxValue)));
					yValue = yBoxStart;
				}
				else {
					yBoxStart = height/2;
					yBoxEnd =  height - ((int)(((double)height/2)*((minValue-value)/minValue)));
					yValue = yBoxEnd;
				}
			}

		}
		else {
			if (drawReads) {
				yBoxStart =  height - ((int)(((double)height/2)*(value/maxValue)));
				yValue = yBoxStart;
			}
			else {
				yBoxStart =  height - ((int)(((double)height)*(value/maxValue)));
				yValue = yBoxStart;
			}
			yBoxEnd = height;
		}

		drawnProbes.add(new DrawnProbe(wholeXStart, wholeXEnd, p, origValue));

		if ((!showNegative) && valueIsNegative) return; // Don't bother drawing probes which will actually have zero size

		switch (DisplayPreferences.getInstance().getGraphType()) {

		case DisplayPreferences.GRAPH_TYPE_BAR: 
			g.fillRect(wholeXStart,yBoxStart,(wholeXEnd-wholeXStart),yBoxEnd-yBoxStart);
			break;

		case DisplayPreferences.GRAPH_TYPE_BLOCK:
			yBoxEnd = height;
			if (drawReads) {
				yBoxStart = height/2;
			}
			else {
				yBoxStart = 0;
			}
			g.fillRect(wholeXStart,yBoxStart,(wholeXEnd-wholeXStart),yBoxEnd-yBoxStart);
			break;


		case DisplayPreferences.GRAPH_TYPE_POINT:
			g.fillOval((wholeXStart+((wholeXEnd-wholeXStart)/2))-2, yValue-2, 4, 4);
			break;

		case DisplayPreferences.GRAPH_TYPE_LINE:
			int xMid = wholeXStart+((wholeXEnd-wholeXStart)/2);
			if (xMid <= lastProbeXMid) return; // Can happen with probes of different length
			if (lastProbeXMid > 0) {
				g.drawLine(lastProbeXMid, lastProbeY, xMid, yValue);
			}
			lastProbeXMid = xMid;
			lastProbeY = yValue;
			break;

		}

		//		System.out.println("Drawing probe from x="+wholeXStart+" y="+yBoxStart+" width="+(wholeXEnd-wholeXStart)+" height="+(yBoxEnd-yBoxStart));
		//		g.fillRect(wholeXStart,yBoxStart,(wholeXEnd-wholeXStart),yBoxEnd-yBoxStart);


		// For replicate sets draw the variability in the way people would like.
		int variation = DisplayPreferences.getInstance().getVariation(); 

		if (data instanceof ReplicateSet  && variation != DisplayPreferences.VARIATION_NONE) {

			DataStore [] stores = ((ReplicateSet)data).dataStores();
			float [] values = new float [stores.length];

			for (int s=0;s<stores.length;s++) {

				try {
					values[s] = stores[s].getValueForProbe(p);
				} 
				catch (SeqMonkException e) {
					// Should have been caught by now, but better safe than sorry
					return;
				}
			}

			g.setColor(Color.DARK_GRAY);

			if (variation == DisplayPreferences.VARIATION_POINTS) {

				for (int v=0;v<values.length;v++) {
					// Don't draw points which fall off the screen...
					if (values[v] > maxValue) continue;
					if (values[v] < minValue) continue;

					int thisPointY = getYForProbeValue(values[v]);

					g.fillOval((wholeXStart+((wholeXEnd-wholeXStart)/2))-2, thisPointY-2, 4, 4);
				}

			}

			else  {
				float variabilityValuePlus;
				float variabilityValueMinus;

				if (variation == DisplayPreferences.VARIATION_STDEV) {
					variabilityValuePlus = value+SimpleStats.stdev(values);
					variabilityValueMinus = value-SimpleStats.stdev(values);
				}
				else if (variation == DisplayPreferences.VARIATION_SEM) {
					variabilityValuePlus = value+SimpleStats.stdev(values)/(float)Math.sqrt(values.length);
					variabilityValueMinus = value-SimpleStats.stdev(values)/(float)Math.sqrt(values.length);
				}
				else if (variation == DisplayPreferences.VARIATION_MAX_MIN) {
					variabilityValuePlus = values[0];
					variabilityValueMinus = values[0];
					for (int v=1;v<values.length;v++) {
						if (values[v]>variabilityValuePlus) variabilityValuePlus = values[v];
						if (values[v]<variabilityValueMinus) variabilityValueMinus = values[v];
					}
				}
				else {
					throw new IllegalStateException("Don't understand variability value "+variation);
				}

				int yPlus = getYForProbeValue(variabilityValuePlus);
				int yMinus = getYForProbeValue(variabilityValueMinus);

				// Move the ends in a bit so the error bars are narrower than the blocks
				int xOffset = (wholeXEnd-wholeXStart)/5;

				// Draw some whiskers around the probe.
				g.drawLine(wholeXStart+xOffset, yPlus, wholeXEnd-xOffset, yPlus);
				g.drawLine(wholeXStart+xOffset, yMinus, wholeXEnd-xOffset, yMinus);

				int middleX = wholeXStart+((wholeXEnd-wholeXStart)/2);

				g.drawLine(middleX, yPlus, middleX, yMinus);	

			}

		}



	}

	private int getYForProbeValue (float value) {
		int yValue;

		if (value > maxValue) value = maxValue;
		if (value < minValue) value = minValue;


		if (showNegative) {
			if (drawReads) {
				if (value > 0) {
					yValue = ((height*3)/4) - ((int)(((double)height/4)*(value/maxValue)));
				}
				else {
					yValue = height - ((int)(((double)height/4)*((minValue-value)/minValue)));
				}
			}
			else {
				if (value > 0) {
					yValue =  (height/2) - ((int)(((double)height/2)*(value/maxValue)));
				}
				else {
					yValue =  height - ((int)(((double)height/2)*((minValue-value)/minValue)));
				}
			}

		}
		else {
			if (drawReads) {
				yValue =  height - ((int)(((double)height/2)*(value/maxValue)));
			}
			else {
				yValue =  height - ((int)(((double)height)*(value/maxValue)));
			}
		}

		return yValue;

	}

	private float getValueForYPixel (int pixel) {

		int trueHeight = 0;
		int offset = 0;

		if (showNegative) {
			if (drawReads) {
				trueHeight = height/4;
				offset = (trueHeight*3)-pixel;
			}
			else {
				trueHeight = height/2;
				offset = trueHeight-pixel;
			}

		}
		else {
			if (drawReads) {
				trueHeight = height/2;
				offset = height-pixel;
			}
			else {
				trueHeight = height;
				offset = height-pixel;
			}
		}

		float returnValue = maxValue * (offset/(float)trueHeight);

		//		System.err.println("Value="+returnValue+" trueHeight="+trueHeight+" offset="+offset+" y="+pixel);

		return returnValue;
	}

	/**
	 * Pixel to bp.
	 * 
	 * @param x the x
	 * @return the int
	 */
	private int pixelToBp (int x) {
		int pos = viewer.currentStart()+(int)(((double)x/width)*(viewer.currentEnd()-viewer.currentStart()));
		if (pos < 1) pos = 1;
		if (pos > viewer.chromosome().length()) pos = viewer.chromosome().length();
		return pos;
	}

	/**
	 * Bp to pixel.
	 * 
	 * @param bp the bp
	 * @return the int
	 */
	private int bpToPixel (int bp) {
		return (int)(((double)(bp-viewer.currentStart())/((viewer.currentEnd()-viewer.currentStart())))*width);		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent me) {
		viewer.setSelectionEnd(me.getX());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent me) {
		int x = me.getX();
		int y = me.getY();

		/*
		 * In many cases we don't need to search through reads and
		 * probes, so we can quickly work out what we should be
		 * looking for from what we're drawing and where the mouse is.
		 * 
		 */

		if (drawReads) {
			if (drawProbes) {
				if (y<=height/2) {
					findRead(x, y);
				}
				else {
					findProbe(x, y);
				}
			}
			else {
				findRead(x, y);
			}
		}
		else {
			findProbe(x, y);
		}
	}

	/**
	 * Find read.
	 * 
	 * @param x the x
	 * @param y the y
	 */
	private void findRead (int x, int y) {
		Enumeration<DrawnRead> e = drawnReads.elements();
		while (e.hasMoreElements()) {
			DrawnRead r = e.nextElement();
			if (r.isInFeature(x,y)) {
				if (activeRead != r.read || r.index != activeReadIndex) {
					viewer.application().setStatusText(
							" "+
									data.name()+
									" "+
									SequenceRead.toString(r.read)
							);
					activeRead = r.read;
					activeReadIndex = r.index;
					activeProbe = null;
					repaint();
				}
				return;
			}
		}			
	}

	/**
	 * Find probe.
	 * 
	 * @param x the x
	 * @param y the y
	 */
	private void findProbe (int x, int y) {

		// The rule is that we find all probes where x falls within the probe
		// region.  If we have multiple probes to choose from then we pick
		// the one with the value closest to 0 which is still encompassed by the 
		// y point.

		float pointValue = getValueForYPixel(y);

		DrawnProbe bestProbe = null;
		float bestValue = 0;
		boolean wasWithinRange = false;

		Enumeration<DrawnProbe> e = drawnProbes.elements();
		while (e.hasMoreElements()) {
			DrawnProbe p = e.nextElement();
			if (p.isInFeature(x,y)) {

				float thisProbeValue = 0;
				try {
					thisProbeValue = data.getValueForProbe(p.probe);
				} catch (SeqMonkException e1) {}

				boolean isWithinRange = false;

				if (pointValue >= 0 && thisProbeValue >= 0 && thisProbeValue >= pointValue) {
					isWithinRange = true;
				}
				else if (pointValue < 0 && thisProbeValue < 0 && thisProbeValue <= pointValue) {
					isWithinRange = true;
				}
				else {
					isWithinRange = false;
				}


				if (bestProbe == null) {
					// This is the best option so far
					bestProbe = p;
					bestValue = thisProbeValue;
					wasWithinRange = isWithinRange;
					continue;
				}

				// See if this is better than the last one

				// Does this overlap where the last probe didn't?
				if (!wasWithinRange && isWithinRange) {
					// This is a new overlap, so it's better
					bestProbe = p;
					bestValue = thisProbeValue;
					wasWithinRange = isWithinRange;
					continue;					
				}

				if (isWithinRange) {
					// See if it's closer to the origin than the best we have
					if (bestValue >=0 && thisProbeValue < bestValue) {
						bestProbe = p;
						bestValue = thisProbeValue;
						wasWithinRange = isWithinRange;
						continue;											
					}
					if (bestValue <0 && thisProbeValue > bestValue) {
						bestProbe = p;
						bestValue = thisProbeValue;
						wasWithinRange = isWithinRange;
						continue;											
					}
				}
			}
		}			

		if (bestProbe != null) {
			if (activeProbe != bestProbe.probe) {
				viewer.application().setStatusText(" "+data.name()+" "+bestProbe.probe.toString()+" value="+bestValue);
				activeRead = 0;
				activeProbe = bestProbe.probe;
				repaint();
			}
		}
		return;


	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent me) {
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			viewer.zoomOut();
			return;
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent me) {
		// Don't start making a selection if they click the right mouse button
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			return;
		}
		viewer.setMakingSelection(true);
		viewer.setSelectionStart(me.getX());
		viewer.setSelectionEnd(me.getX());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent me) {
		// Don't process anything if they released the right mouse button
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			return;
		}
		viewer.setMakingSelection(false);

		int width = viewer.selectionEnd() - viewer.selectionStart();
		if (width < 0) {
			width = 0-width;
		}
		if (width < 5) return;

		int newStart = pixelToBp(viewer.selectionStart());
		int newEnd = pixelToBp(viewer.selectionEnd());

		if (newStart > newEnd) {
			int temp = newStart;
			newStart = newEnd;
			newEnd = temp;
		}

		if (newEnd-newStart < 101) return;

		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(newStart,newEnd,Location.UNKNOWN));
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent arg0) {
		viewer.application().setStatusText(" "+data.name());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent arg0) {
		activeRead = 0;
		activeProbe = null;
		repaint();
	}

	/**
	 * The Class DrawnRead.
	 */
	private class DrawnRead {

		/** The left. */
		public int left;

		/** The right. */
		public int right;

		/** The top. */
		public int top;

		/** The bottom. */
		public int bottom;

		/** The index */
		public int index;

		/** The read. */
		public long read;

		/**
		 * Instantiates a new drawn read.
		 * 
		 * @param left the left
		 * @param right the right
		 * @param bottom the bottom
		 * @param top the top
		 * @param read the read
		 */
		public DrawnRead (int left, int right, int bottom, int top, int index, long read) {
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
			this.read = read;
			this.index = index;
		}

		/**
		 * Checks if is in feature.
		 * 
		 * @param x the x
		 * @param y the y
		 * @return true, if is in feature
		 */
		public boolean isInFeature (int x, int y) {
			if (x >=left && x <= right && y >= bottom && y<=top) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	/**
	 * The Class DrawnProbe.
	 */
	private class DrawnProbe {

		/** The left. */
		public int left;

		/** The right. */
		public int right;

		/** The probe. */
		public Probe probe;

		/** The value. */
		public double value;

		/**
		 * Instantiates a new drawn probe.
		 * 
		 * @param left the left
		 * @param right the right
		 * @param bottom the bottom
		 * @param top the top
		 * @param probe the probe
		 * @param value the value
		 */
		public DrawnProbe (int left, int right, Probe probe, double value) {
			this.left = left;
			this.right = right;
			this.probe = probe;
			this.value = value;
		}

		/**
		 * Checks if is in feature.
		 * 
		 * @param x the x
		 * @param y the y
		 * @return true, if is in feature
		 */
		public boolean isInFeature (int x, int y) {
			if (x >=left && x <= right) {
				return true;
			}
			else {
				return false;
			}
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString () {
			return probe.toString()+" value="+df.format(value);
		}
	}

}
