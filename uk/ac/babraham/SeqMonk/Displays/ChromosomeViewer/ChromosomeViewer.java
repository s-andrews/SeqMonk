/**
 * Copyright Copyright 2010-15 Simon Andrews
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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferencesListener;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

/**
 * The ChromosomeViewer represents all of the tracks contained in
 * the chromosome view.  It is responsible for organising and laying
 * out these tracks and passing information back from them to other
 * parts of the application.
 * 
 * In general the rest of the program shouldn't deal with anything
 * below the chromosome viewer.
 */
public class ChromosomeViewer extends JPanel implements DataChangeListener, DisplayPreferencesListener, MouseWheelListener {

	private SeqMonkApplication application;

	private Chromosome chromosome;

	private Vector<ChromosomeFeatureTrack> featureTracks = new Vector<ChromosomeFeatureTrack>();

	private Vector<ChromosomeDataTrack> dataTracks = new Vector<ChromosomeDataTrack>();

	private JPanel featurePanel;
	
	private int lastReplicateSetExpansion = 0;

	private JLabel titleLabel; // Tried using a TextField to get Copy/Paste, but this broke SVG export

	private int selectionStart = 0;
	private int selectionEnd = 0;	
	private boolean makingSelection = false;

	private static boolean showAllLables = false;

	private int currentStart = 1;
	private int currentEnd = 1;

	private int currentSelectionStart = 1;
	private int currentSelectionEnd = 1;

	/**
	 * Instantiates a new chromosome viewer.
	 * 
	 * @param application
	 * @param chromosome
	 */
	public ChromosomeViewer (SeqMonkApplication application, Chromosome chromosome) {
		this.application = application;
		this.chromosome = chromosome;

		DisplayPreferences displayPrefs = DisplayPreferences.getInstance();

		displayPreferencesUpdated(displayPrefs);

		//		System.err.println("New viewer for chr "+chromosome);

		setLayout(new BorderLayout());
		// Although this is a TextField we alter it to look like a normal label
		// just one you can copy from.
		titleLabel = new JLabel(application.dataCollection().genome().species()+" "+application.dataCollection().genome().assembly()+" chr"+chromosome.name()+":",JLabel.CENTER); 
		add(titleLabel,BorderLayout.NORTH);

		featurePanel = new JPanel();
		featurePanel.setLayout(new GridBagLayout());

		add(featurePanel,BorderLayout.CENTER);
		addMouseWheelListener(this);

		tracksUpdated();
	}

	/**
	 * This allows you to change the chromsome the viewer is looking at without
	 * altering the list of tracks which are displayed.
	 * 
	 * @param chromosome The new chromosome to display.
	 */
	private void setChromosome (Chromosome chromosome) {
		if (chromosome == null) throw new IllegalArgumentException("Chromosome can't be null");
		if (chromosome != this.chromosome) {
			this.chromosome = chromosome;
			
			Enumeration<ChromosomeDataTrack> en = dataTracks.elements();
			while (en.hasMoreElements()) {
				en.nextElement().updateReads();
			}

			Enumeration<ChromosomeFeatureTrack> enf = featureTracks.elements();
			while (enf.hasMoreElements()) {
				ChromosomeFeatureTrack ft = enf.nextElement();
				Feature [] features = application.dataCollection().genome().annotationCollection().getFeaturesForType(chromosome,ft.type());
				ft.updateFeatures(features);
			}

		}
	}		

	/**
	 * Automatically adjusts the Data Zoom level and scale type to
	 * match the data stores which are currently visible.
	 */
	public void autoScale () {

		// If there are no data tracks visible then don't do anything
		if (dataTracks.size() == 0) return;

		float maxValue = 0;
		float minValue = 0;

		boolean firstTrack = true;

		Enumeration<ChromosomeDataTrack> e = dataTracks.elements();

		while (e.hasMoreElements()) {
			float [] minMax = e.nextElement().getMinMaxProbeValues();
//			System.err.println("Looking at track with min="+minMax[0]+" max="+minMax[1]);
			if (firstTrack) {
				minValue = minMax[0];
				maxValue = minMax[1];
				firstTrack = false;
			}
			else {
				if (minMax[0] < minValue) minValue = minMax[0];
				if (minMax[1] > maxValue) maxValue = minMax[1];
			}
		}

		if (minValue < 0) {
			if (0-minValue > maxValue) maxValue = 0-minValue;
			DisplayPreferences.getInstance().setScaleType(DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE);
		}
		else {
			DisplayPreferences.getInstance().setScaleType(DisplayPreferences.SCALE_TYPE_POSITIVE);
		}
		
		// After auto scaling we should also check that the quantitated data
		// is visible
		if (DisplayPreferences.getInstance().getDisplayMode() == DisplayPreferences.DISPLAY_MODE_READS_ONLY) {
			DisplayPreferences.getInstance().setDisplayMode(DisplayPreferences.DISPLAY_MODE_READS_AND_QUANTITATION);
		}


		// Our data zoom limit (imposed by the slider!) is 2**20 so
		// don't exceed that.
		if (maxValue > Math.pow(2, 20)) {
			maxValue = (float)Math.pow(2, 20);
		}

		// Because our data zoom slider operates on a log scale we don't
		// want to set this value to 0 under any circumstances.
		if (maxValue <= 0) {
			maxValue = 1;
		}
		
		DisplayPreferences.getInstance().setMaxDataValue(maxValue);

	}


	/**
	 * Repaint quantitated data. Repaints just the portion of the screen
	 * containing quantitated data.
	 */
	public void repaintQuantitatedData () {
		/**
		 * This is a lightweight call which will repaint only the 
		 * quantitated portion of the screen if it is present
		 */

		Enumeration<ChromosomeDataTrack>e = dataTracks.elements();
		while (e.hasMoreElements()) {
			e.nextElement().repaintQuantitation();
		}
	}


	/**
	 * This is quite a heavyweight call to make.  It forces the recalculation
	 * of the layout of all tracks.  In many cases it is sufficient to call
	 * repaint on the chromosome viewer which will update existing information
	 * (name changes, selection changes etc).  Only use this when the actual
	 * data has changed.
	 */
	public void tracksUpdated() {

		if (featurePanel == null) return;

		String [] featureTypes = application.drawnFeatureTypes();
		featureTracks.removeAllElements();
		for (int i=0;i<featureTypes.length;i++) {
			ChromosomeFeatureTrack t = new ChromosomeFeatureTrack(this,featureTypes[i],application.dataCollection().genome().annotationCollection().getFeaturesForType(chromosome, featureTypes[i]));
			featureTracks.add(t);
		}

		DataStore [] dataStores = application.drawnDataStores();
		dataTracks.removeAllElements();
		for (int i=0;i<dataStores.length;i++) {
			
			if (dataStores[i] instanceof ReplicateSet && DisplayPreferences.getInstance().getReplicateSetExpansion() == DisplayPreferences.REPLICATE_SETS_EXPANDED) {
				DataStore [] localStores = ((ReplicateSet)dataStores[i]).dataStores();
				for (int j=0;j<localStores.length;j++) {
					ChromosomeDataTrack t = new ChromosomeDataTrack(this,application.dataCollection(),localStores[j]);
					t.setEnclosingReplicateSet((ReplicateSet)dataStores[i]);
					dataTracks.add(t);
				}
			}

			else {
				if (dataStores[i] == application.dataCollection().getActiveDataStore()) {
					ChromosomeDataTrack t = new MinSizeDataTrack(this,application.dataCollection(),dataStores[i]);
					dataTracks.add(t);					
				}
				else {
					ChromosomeDataTrack t = new ChromosomeDataTrack(this,application.dataCollection(),dataStores[i]);
					dataTracks.add(t);
				}
			}
		}

		featurePanel.removeAll();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0.001;

		Enumeration<ChromosomeFeatureTrack> e = featureTracks.elements();
		while (e.hasMoreElements()) {
			featurePanel.add(e.nextElement(),c);
			c.gridy++;
		}

		// We weight the data tracks way more heavily than the feature tracks since the feature tracks
		// have a preferred size which we don't need to exceed but the data tracks should take up whatever
		// is left.
		c.weighty = 0.6;
		Enumeration<ChromosomeDataTrack> e2 = dataTracks.elements();
		while (e2.hasMoreElements()) {
			featurePanel.add(e2.nextElement(),c);
			c.gridy++;
		}

		// Finally add a scale track, which we weigh very lightly
		c.weighty=0.00001;
		featurePanel.add(new ChromosomeScaleTrack(this),c);
		c.gridy++;

		featurePanel.validate();
		featurePanel.repaint();
	}

	/**
	 * Sets the view to a particular location
	 * 
	 * @param start The start position
	 * @param end The end position
	 */
	private void setView (int start, int end) {

		// If the view is a reversed section we need to swap
		// start and end
		if (start > end) {
			int temp = start;
			start = end;
			end = temp;
		}

		currentStart = start;
		currentEnd = end;
		repaint();
	}


	/**
	 * Toggles whether all feature labels are currently shown.
	 */
	public void toggleLabels () {
		showAllLables = !showAllLables;
		repaint();
	}


	/**
	 * Says whether we're currently showing all feature labels.
	 * 
	 * @return true, if we're showing all feature labels.
	 */
	public boolean showAllLables () {
		return showAllLables;
	}

	/**
	 * Doubles the area of the current view keeping the same
	 * midpoint if possible.
	 */
	public synchronized void zoomOut () {
		int midBase = currentStart+((currentEnd-currentStart)/2);
		int currentWidth = (currentEnd-currentStart)+1;

		int newStart = midBase-currentWidth;
		if (newStart < 1) newStart = 1;

		int newEnd = newStart+(currentWidth*2);
		if (newEnd > chromosome.length()) newEnd = chromosome.length();
		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(newStart,newEnd,Location.UNKNOWN));
	}

	/**
	 * Halves the area of the current view keeping the same mid point.
	 */
	public synchronized void zoomIn () {
		int midBase = currentStart+((currentEnd-currentStart)/2);
		int currentWidth = (currentEnd-currentStart)+1;

		int newStart = midBase-(currentWidth/4);

		int newEnd = newStart+(currentWidth/2);
		
		if (newEnd-newStart < 5) return;
					
		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(newStart,newEnd,Location.UNKNOWN));


		//TODO: Set limits on this.
		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(newStart,newEnd,Location.UNKNOWN));
	}

	/**
	 * Moves the view a small amount left
	 */
	public void moveLeft() {
		int currentWidth = (currentEnd-currentStart)+1;
		int interval = currentWidth/50;
		if (currentStart < interval+1) interval = currentStart-1;
		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(currentStart-interval,currentEnd-interval,Location.UNKNOWN));
	}

	/**
	 * Moves the view a small amount right.
	 */
	public void moveRight() {
		int currentWidth = (currentEnd-currentStart)+1;
		int interval = currentWidth/50;
		if (currentEnd+interval > chromosome.length()) interval = chromosome.length()-currentEnd;
		DisplayPreferences.getInstance().setLocation(SequenceRead.packPosition(currentStart+interval,currentEnd+interval,Location.UNKNOWN));
	}

	/**
	 * Says that we're starting to make a selction
	 * 
	 * @param b
	 */
	public void setMakingSelection (boolean b) {
		makingSelection = b;
		repaint();
	}

	/**
	 * Sets the selection start.
	 * 
	 * @param x The new selection start
	 */
	public void setSelectionStart (int x) {
		int min = findMin(new int [] {selectionStart,selectionEnd,x});
		int max = findMax(new int [] {selectionStart,selectionEnd,x});
		Rectangle r = new Rectangle(min,0,(max-min)+1,getHeight());
		selectionStart = x;
		Enumeration<ChromosomeDataTrack>e = dataTracks.elements();
		while (e.hasMoreElements()) {
			e.nextElement().repaint(r);
		}
		Enumeration<ChromosomeFeatureTrack>e2 = featureTracks.elements();
		while (e2.hasMoreElements()) {
			e2.nextElement().repaint(r);
		}
	}

	/**
	 * Sets the selection end.
	 * 
	 * @param x The new selection end
	 */
	public void setSelectionEnd (int x) {
		int min = findMin(new int [] {selectionStart,selectionEnd,x});
		int max = findMax(new int [] {selectionStart,selectionEnd,x});
		Rectangle r = new Rectangle(min,0,(max-min)+1,getHeight());
		selectionEnd = x;
		Enumeration<ChromosomeDataTrack>e = dataTracks.elements();
		while (e.hasMoreElements()) {
			e.nextElement().repaint(r);
		}
		Enumeration<ChromosomeFeatureTrack>e2 = featureTracks.elements();
		while (e2.hasMoreElements()) {
			e2.nextElement().repaint(r);
		}
	}

	/**
	 * Says if we're currently making a selection.
	 * 
	 * @return true, if we're making a selection
	 */
	public boolean makingSelection () {
		return makingSelection;
	}

	/**
	 * Selection start.
	 * 
	 * @return The point where the last complete selection was started.
	 */
	public int selectionStart () {
		return selectionStart;
	}

	/**
	 * Selection end.
	 * 
	 * @return The point where the last complete selection was ended
	 */
	public int selectionEnd () {
		return selectionEnd;
	}

	/**
	 * Current start.
	 * 
	 * @return The point where the current in-progress selection started
	 */
	public int currentSelectionStart () {
		return currentSelectionStart;
	}

	/**
	 * The point where the current in-progress selection ended
	 * 
	 * @return the int
	 */
	public int currentSelectionEnd () {
		return currentSelectionEnd;
	}

	public int currentStart () {
		return currentStart;
	}

	public int currentEnd () {
		return currentEnd;
	}


	/**
	 * Application.
	 * 
	 * @return The seq monk application
	 */
	public SeqMonkApplication application () {
		//TODO: Remove this dependency so messages pass in a nicer way
		return application;
	}

	/**
	 * Gets the positional index of a feature track
	 * 
	 * @param t The track to query
	 * @return The position of this track in the current set of displayed feature tracks.
	 */
	public int getIndex (ChromosomeFeatureTrack t) {
		return featureTracks.indexOf(t);
	}

	/**
	 * Gets the positional index of a data track
	 * 
	 * @param t The track to query
	 * @return The position of this track in the current set of displayed data tracks.
	 */
	public int getIndex (ChromosomeDataTrack t) {
		return dataTracks.indexOf(t);
	}


	/**
	 * Chromosome.
	 * 
	 * @return The chromosome
	 */
	public Chromosome chromosome () {
		return chromosome;
	}

	public void activeDataStoreChanged(DataStore s) {
		tracksUpdated();
	}

	public void activeProbeListChanged(ProbeList l) {
		Probe [] newProbes;

		if (application.dataCollection().probeSet() != null) {
			newProbes = application.dataCollection().probeSet().getActiveList().getProbesForChromosome(chromosome);
		}
		else {
			newProbes = new Probe[0];
		}


		Enumeration<ChromosomeDataTrack>e = dataTracks.elements();
		while (e.hasMoreElements()) {
			e.nextElement().setProbes(newProbes);
		}
	}

	public void dataGroupAdded(DataGroup g) {}

	public void dataGroupsRemoved(DataGroup [] g) {}

	public void dataGroupRenamed(DataGroup g) {
		repaint();
	}

	public void dataGroupSamplesChanged(DataGroup g) {}

	public void dataSetAdded(DataSet d) {}

	public void dataSetsRemoved(DataSet [] d) {}

	public void dataSetRenamed(DataSet d) {
		repaint();
	}

	public void probeSetReplaced(ProbeSet p) {
		// TODO: Do we need to do anything here?  Probably not as active probe list replaced will be called
	}

	public void replicateSetAdded(ReplicateSet r) {}

	public void replicateSetsRemoved(ReplicateSet [] r) {}

	public void replicateSetRenamed(ReplicateSet r) {
		repaint();
	}

	public void replicateSetStoresChanged(ReplicateSet r) {}


	/**
	 * Finds the max value from a set of ints.
	 * 
	 * @param ints the ints
	 * @return the max value
	 */
	private int findMax (int [] ints) {
		int max = ints[0];
		for (int i=1;i<ints.length;i++) {
			if(ints[i]>max) max=ints[i];
		}
		return max;
	}

	/**
	 * Finds the min value from a set of ints
	 * 
	 * @param ints the ints
	 * @return the int
	 */
	private int findMin (int [] ints) {
		int min = ints[0];
		for (int i=1;i<ints.length;i++) {
			if(ints[i]<min) min=ints[i];
		}
		return min;
	}

	public void displayPreferencesUpdated(DisplayPreferences displayPrefs) {
		

		if (displayPrefs.getCurrentChromosome() != null && !chromosome.equals(displayPrefs.getCurrentChromosome())) {
			setChromosome(displayPrefs.getCurrentChromosome());
		}
		
		if (displayPrefs.getReplicateSetExpansion() != lastReplicateSetExpansion) {
			lastReplicateSetExpansion = displayPrefs.getReplicateSetExpansion();
			tracksUpdated();
		}
		
		if (featurePanel != null) {
			setView(SequenceRead.start(displayPrefs.getCurrentLocation()),SequenceRead.end(displayPrefs.getCurrentLocation()));
			int currentLength = (currentEnd-currentStart)+1;

			String currentLengthString = PositionFormat.formatLength(currentLength);			

			titleLabel.setText(application.dataCollection().genome().species()+" "+application.dataCollection().genome().assembly()+" chr"+chromosome.name()+":"+currentStart+"-"+currentEnd+" ("+currentLengthString+")");
		}
	}

	public void mouseWheelMoved(MouseWheelEvent mwe) {
		
		if (mwe.getWheelRotation() > 0) {
			moveRight();
		}
		else if (mwe.getWheelRotation() < 0) {
			moveLeft();
		}
		
	}
}
