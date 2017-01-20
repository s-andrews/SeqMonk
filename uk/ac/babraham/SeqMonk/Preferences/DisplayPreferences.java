/**
 * Copyright 2012-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Preferences;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.GotoDialog.GotoDialog;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.GreyscaleColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.HotColdColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.InvertedGradient;
import uk.ac.babraham.SeqMonk.Gradients.MagentaGreenColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.RedGreenColourGradient;
import uk.ac.babraham.SeqMonk.Gradients.RedWhiteColourGradient;

/**
 * This class is intended to be a single point at which all of the major display
 * preferences can be stored and from which changes can be passed to any 
 * views which care.
 * 
 * This will cover any visual changes to the display, so colours, types of 
 * graph, scales would all fall into this object.
 * 
 *
 */
public class DisplayPreferences {

	/** The single instance of this class **/
	private static DisplayPreferences instance = new DisplayPreferences();
	
	private Vector<DisplayPreferencesListener> listeners = new Vector<DisplayPreferencesListener>();
	
	/*
	 * DO NOT CHANGE THESE CONSTANTS.
	 * 
	 * Although they are arbitrary numbers they are used in the SeqMonk
	 * file format and if they are changed the settings will not be
	 * restored correctly and we'll probably get errors.
	 */	
	
	/** The set of constants for the colour type being used **/
	/* Some of these values are carried over from an older implementation hence the somewhat odd numbering */
	public static final int COLOUR_TYPE_FIXED = 1001;
	public static final int COLOUR_TYPE_INDEXED = 12;
	public static final int COLOUR_TYPE_GRADIENT = 11;	
	
	private int currentColourType = COLOUR_TYPE_GRADIENT;

	/** The set of constants for the read density **/
	/* Some of these values are carried over from an older implementation hence the somewhat odd numbering */
	public static final int READ_DENSITY_LOW = 6;
	public static final int READ_DENSITY_MEDIUM = 7;
	public static final int READ_DENSITY_HIGH = 8;	
	
	private int currentReadDensity = READ_DENSITY_LOW;


	/** The set of constants for the display mode **/
	/* Some of these values are carried over from an older implementation hence the somewhat odd numbering */
	public static final int DISPLAY_MODE_READS_ONLY = 2;
	public static final int DISPLAY_MODE_READS_AND_QUANTITATION = 1;
	public static final int DISPLAY_MODE_QUANTITATION_ONLY = 3;	
	
	private int currentDisplayMode = DISPLAY_MODE_READS_ONLY;

	/** The options for which gradient to use **/
	public static final int GRADIENT_HOT_COLD = 2001;
	public static final int GRADIENT_RED_GREEN = 2002;
	public static final int GRADIENT_GREYSCALE = 2003;
	public static final int GRADIENT_MAGENTA_GREEN = 2004;
	public static final int GRADIENT_RED_WHITE = 2005;

	private ColourGradient currentGradient = new HotColdColourGradient();
	private int currentGradientValue = GRADIENT_HOT_COLD;
	
	private boolean invertGradient = false;
	
	/** The options for the type of graph drawn **/
	public static final int GRAPH_TYPE_BAR = 3001;
	public static final int GRAPH_TYPE_LINE = 3002;
	public static final int GRAPH_TYPE_POINT = 3003;
	public static final int GRAPH_TYPE_BLOCK = 3004;

	private int currentGraphType = GRAPH_TYPE_BAR;
	
	/** The options for the scale used **/
	/* Some of these values are carried over from an older implementation hence the somewhat odd numbering */
	public static final int SCALE_TYPE_POSITIVE = 4;
	public static final int SCALE_TYPE_POSITIVE_AND_NEGATIVE = 5;
	
	private int currentScaleType = SCALE_TYPE_POSITIVE_AND_NEGATIVE;
	
	
	/** The options for the read display **/
	/* Some of these values are carried over from an older implementation hence the somewhat odd numbering */
	public static final int READ_DISPLAY_COMBINED = 9;
	public static final int READ_DISPLAY_SEPARATED = 10;
	
	private int currentReadDisplay = SCALE_TYPE_POSITIVE_AND_NEGATIVE;
	
	/** Whether we want to expand replicate sets into their component parts **/
	public static final int REPLICATE_SETS_EXPANDED = 4001;
	public static final int REPLICATE_SETS_COMPRESSED = 4002;
	
	private int currentReplicateSetsExpanded = REPLICATE_SETS_COMPRESSED;
	
	/** How we want to show variation in replicate sets **/
	public static final int VARIATION_NONE = 5001;
	public static final int VARIATION_STDEV = 5002;
	public static final int VARIATION_SEM = 5003;
	public static final int VARIATION_MAX_MIN = 5004;
	public static final int VARIATION_POINTS = 5005;

	public int currentVariation = VARIATION_NONE;
	
	/** The Data zoom level **/
	private double maxDataValue = 1;
	
	/** The currently visible location **/
	private long currentLocation = 0;
	
	/** The currently visible chromosome **/
	private Chromosome currentChromosome = null;
		
	
	/** We make this a singleton so it's only accessible by a static method **/
	private DisplayPreferences () {}
	
	public static DisplayPreferences getInstance () {
		return instance;
	}
	
	public void reset () {
		listeners.removeAllElements();
		currentColourType = COLOUR_TYPE_GRADIENT;
		currentDisplayMode = DISPLAY_MODE_READS_ONLY;
		currentGradient = new HotColdColourGradient();
		currentGradientValue = GRADIENT_HOT_COLD;
		currentGraphType = GRAPH_TYPE_BAR;
		currentReadDensity  = READ_DENSITY_LOW;
		currentReadDisplay = READ_DISPLAY_COMBINED;
		currentScaleType = SCALE_TYPE_POSITIVE;
	}
	
	/* We allow views to listen for changes */
	public void addListener (DisplayPreferencesListener l) {
		if (l != null && !listeners.contains(l)) listeners.add(l);
	}
	
	public void removeListener (DisplayPreferencesListener l) {
		if (l != null && listeners.contains(l)) listeners.remove(l);
	}
	
	private void optionsChanged () {
		Enumeration<DisplayPreferencesListener> en = listeners.elements();
		
		while (en.hasMoreElements()) {
			en.nextElement().displayPreferencesUpdated(this);
		}
	}
	
	/* The max data value */
	public double getMaxDataValue () {
		return maxDataValue;
	}
	
	public void setMaxDataValue (double value) {
		if (value<1) value=1;
		if (value>Math.pow(2, 20)) value=Math.pow(2, 20);
		maxDataValue = value;
		optionsChanged();
	}
	
	
	/* The  display mode */
	public int getDisplayMode () {
		return currentDisplayMode;
	}
	
	public void setDisplayMode (int displayMode) {
		if (equalsAny(new int [] {DISPLAY_MODE_QUANTITATION_ONLY,DISPLAY_MODE_READS_AND_QUANTITATION,DISPLAY_MODE_READS_ONLY}, displayMode)) {
			currentDisplayMode = displayMode;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+displayMode+" is not a valid display mode");
		}
	}
	
	/* The colour type */
	public int getColourType () {
		return currentColourType;
	}
	
	public void setColourType (int colourType) {
		if (equalsAny(new int [] {COLOUR_TYPE_FIXED,COLOUR_TYPE_GRADIENT,COLOUR_TYPE_INDEXED}, colourType)) {
			currentColourType = colourType;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+colourType+" is not a valid colour type");
		}
	}
	
	/* The read density */
	public int getReadDensity () {
		return currentReadDensity;
	}
	
	public void setReadDensity (int readDensity) {
		if (equalsAny(new int [] {READ_DENSITY_LOW,READ_DENSITY_MEDIUM,READ_DENSITY_HIGH}, readDensity)) {
			currentReadDensity = readDensity;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+readDensity+" is not a valid read density");
		}
	}
	
	/* The read display */
	public int getReadDisplay () {
		return currentReadDisplay;
	}
	
	public void setReadDisplay (int readDisplay) {
		if (equalsAny(new int [] {READ_DISPLAY_COMBINED,READ_DISPLAY_SEPARATED}, readDisplay)) {
			currentReadDisplay = readDisplay;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+readDisplay+" is not a valid read display");
		}
	}
	
	/* The location */
	public long getCurrentLocation () {
		return currentLocation;
	}
	
	public void setLocation (Chromosome c, long location) {
		currentChromosome = c;
				
		// We need to sanity check this.  If the region we're being sent to 
		// is smaller than 101bp then we'll make it bigger so we can see it
		
		if (SequenceRead.length(location) < 101) {
			// We'll make this up to at least 5bp
			if (c.length() < 101) return; // We just can't show a chromosome which is this small
			
			int centre = SequenceRead.midPoint(location);
			int left = Math.max(centre-50, 1);
			int right = left + 100;
			
			if (right > c.length()) {
				right = c.length();
				left = right - 100;
			}
			
			location = SequenceRead.packPosition(left, right, SequenceRead.strand(location));
		}
		
		currentLocation = location;
		GotoDialog.addRecentLocation(c, SequenceRead.start(location), SequenceRead.end(location));
		optionsChanged();
	}
	
	public void setLocation (long location) {
		currentLocation = location;
		optionsChanged();
	}
	
	/* The chromosome */
	public Chromosome getCurrentChromosome () {
		return currentChromosome;
	}
	
	public void setChromosome (Chromosome c) {
		currentChromosome = c;
		// Set the location to be a 1Mbp chunk in the middle if we can
		int mid = c.length()/2;
		int start = Math.max(mid-500000,1);
		int end = Math.min(mid+500000,c.length());
		currentLocation = SequenceRead.packPosition(start, end, Location.UNKNOWN);
		optionsChanged();
	}
	

	/* The gradient  */
	public ColourGradient getGradient () {
		return currentGradient;
	}
	
	public int getGradientValue () {
		return currentGradientValue;
	}
	
	public void setGradient (int gradientType) {
		if (equalsAny(new int [] {GRADIENT_GREYSCALE,GRADIENT_HOT_COLD,GRADIENT_RED_GREEN,GRADIENT_MAGENTA_GREEN,GRADIENT_RED_WHITE}, gradientType)) {
			
			currentGradientValue = gradientType;
			
			switch (gradientType) {
			
				case GRADIENT_GREYSCALE: currentGradient = new GreyscaleColourGradient();break;
				case GRADIENT_HOT_COLD: currentGradient = new HotColdColourGradient();break;
				case GRADIENT_RED_GREEN: currentGradient = new RedGreenColourGradient();break;
				case GRADIENT_MAGENTA_GREEN: currentGradient = new MagentaGreenColourGradient();break;
				case GRADIENT_RED_WHITE: currentGradient = new RedWhiteColourGradient();break;
			}
			
			if (invertGradient) {
				currentGradient = new InvertedGradient(currentGradient);
			}
			
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+gradientType+" is not a valid gradient type");
		}
	}
	
	public boolean getInvertGradient () {
		return invertGradient;
	}
	
	public void setInvertGradient (boolean invert) {
		invertGradient = invert;
		setGradient(currentGradientValue);
	}

	
	/* The graph type */
	public int getGraphType () {
		return currentGraphType;
	}
	
	public void setGraphType (int graphType) {
		if (equalsAny(new int [] {GRAPH_TYPE_BAR,GRAPH_TYPE_LINE,GRAPH_TYPE_POINT,GRAPH_TYPE_BLOCK}, graphType)) {
			currentGraphType = graphType;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+graphType+" is not a valid graph type");
		}
	}
	
	
	/* The scale type */
	public int getScaleType () {
		return currentScaleType;
	}
	
	public void setScaleType (int scaleType) {
		if (equalsAny(new int [] {SCALE_TYPE_POSITIVE,SCALE_TYPE_POSITIVE_AND_NEGATIVE}, scaleType)) {
			currentScaleType = scaleType;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+scaleType+" is not a valid scale type");
		}
	}

	
	/* The replicate set expansion */
	public int getReplicateSetExpansion () {
		return currentReplicateSetsExpanded;
	}
	
	public void setReplicateSetExpansion (int expansion) {
		if (equalsAny(new int [] {REPLICATE_SETS_COMPRESSED,REPLICATE_SETS_EXPANDED}, expansion)) {
			currentReplicateSetsExpanded = expansion;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+expansion+" is not a valid replicate set expansion");
		}
	}

	/* The way we show variation in replicate sets */
	public int getVariation () {
		return currentVariation;
	}
	
	public void setVariation (int variation) {
		if (equalsAny(new int [] {VARIATION_NONE,VARIATION_STDEV,VARIATION_SEM,VARIATION_MAX_MIN,VARIATION_POINTS}, variation)) {
			currentVariation = variation;
			optionsChanged();
		}
		else {
			throw new IllegalArgumentException("Value "+variation+" is not a valid variation value");
		}
	}

	
	
	
	public void writeConfiguration (PrintStream p) {
		
		/**
		 * NB!! If you change this then you need to increment the data version value
		 * in the SeqMonk data writer otherwise you'll generate invalid files which
		 * will cause errors when re-loaded.  You'll also need to fix the SeqMonk
		 * data parser and update the version there too.
		 */
		
		p.println("Display Preferences\t12"); //Make sure this number at the end equates to the number of configuration lines to be written
		
		p.println("DataZoom\t"+getMaxDataValue());
		
		p.println("ScaleMode\t"+getScaleType());
		
		p.println("DisplayMode\t"+getDisplayMode());

		p.println("ReplicateExpansion\t"+getReplicateSetExpansion());

		p.println("Variation\t"+getVariation());

		p.println("ReadDensity\t"+getReadDensity());
	
		p.println("SplitMode\t"+getReadDisplay());
		
		p.println("QuantitationColour\t"+getColourType());
		
		p.println("Gradient\t"+getGradientValue());
		
		String invertString = "0";
		if (invertGradient) invertString = "1";
		
		p.println("InvertGradient\t"+invertString);
		
		p.println("GraphType\t"+getGraphType());
		
		p.println("CurrentView\t"+currentChromosome.name()+"\t"+SequenceRead.start(currentLocation)+"\t"+SequenceRead.end(currentLocation));

	
	}
		

	private boolean equalsAny (int [] valid, int test) {
		for (int i=0;i<valid.length;i++) {
			if (test == valid[i]) return true;
		}
		
		return false;
	}
	
}
