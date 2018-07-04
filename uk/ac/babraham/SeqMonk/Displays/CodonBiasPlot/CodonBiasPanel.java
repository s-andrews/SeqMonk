/**
 * Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.CodonBiasPlot;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class CodonBiasPanel extends JPanel implements Runnable {

	private DataStore store;
	private Feature feature;
	
	private Color RED = new Color(200,0,0);
	private Color GREEN = new Color(0,200,0);
	private Color BLUE = new Color(0,0,200);
	private Color GREY = new Color(230,230,230);
	
	// These set the limits, either globally, or if we're zoomed in
	// along with a flag to say when they've been calculated
	private boolean calculated = false;
	
	// These are cached values for the overall abundances at each position
	private int [] abundance;
	
	// These are the positions of the exon starts within the overall feature length.
	private int [] exonBoundaries;
	
	private boolean reverse = false;
	
	
	// Spacing for the drawn panel
	private static final int X_AXIS_SPACE = 50;
	private static final int Y_AXIS_SPACE = 50;

	
	
	public CodonBiasPanel () {
	}
	
	public void setDisplay (Feature feature, DataStore store, boolean reverse) {
		if (feature == null) return;
		this.feature = feature;
		this.store = store;
		this.reverse = reverse;
		calculated = false;
		Thread t = new Thread(this);
		t.start();
	}
	
	
	public void run() {

		// First we need to know the length of the feature we'll be analysing.  This isn't the
		// full length in the genome but the sum length of the exons.  We'll also make up an 
		// array of offsets so that we can convert genomic positions into positions within the 
		// feature easily.
		
		Location [] subLocations;
		
		if (feature.location() instanceof SplitLocation) {
			subLocations = ((SplitLocation)(feature.location())).subLocations();
		}
		else {
			subLocations = new Location [] {feature.location()};
		}
		
		System.err.println("Working with "+feature.name());
		
		System.err.println("There are "+subLocations.length+" sublocations");
		
		// First work out the total transcript length so we can make an appropriate data structure
		int totalLength = 0;
		for (int e=0;e<subLocations.length;e++) {
			totalLength += subLocations[e].length();
		}
		
		System.err.println("Total exon length is "+totalLength);
		
		int [] abundance = new int[totalLength];
		
		
		// Now work out the exon boundary positions within the feature
		// We can also work out a mapping between relative genomic position and
		// feature position.
		int [] exonBoundaries = new int [subLocations.length];
		int [] genomeToFeatureMap = new int[1+feature.location().end()-feature.location().start()];
		for (int j=0;j<genomeToFeatureMap.length;j++) {
			genomeToFeatureMap[j] = -1;
		}
		
		System.err.println("Genome to feature map length is "+genomeToFeatureMap.length);
		
		if (feature.location().strand() == Location.FORWARD) {
			
			System.err.println("Feature is forward strand");
			
			int length = 0;
			int positionInFeature = 0;
			for (int i=0;i<subLocations.length;i++) {
				
				System.err.println("Looking at sublocation "+i+" from "+subLocations[i].start()+" to "+subLocations[i].end());
				exonBoundaries[i] = length;
				System.err.println("Added exon boundary at "+exonBoundaries[i]);
				length += subLocations[i].length();
				
				for (int x=0;x<subLocations[i].length();x++) {
					
					int genomePostion = subLocations[i].start()+x;
					int relativeGenomePosition = genomePostion - feature.location().start();
					
					System.err.println("Sublocation Pos="+x+" Genome Pos="+genomePostion+" Rel Genome Pos="+relativeGenomePosition+" Feature pos="+positionInFeature);
					
					genomeToFeatureMap[relativeGenomePosition] = positionInFeature;
					positionInFeature++;
				}
				
			}
		}
		
		else if (feature.location().strand() == Location.REVERSE) {
			int length = 0;
			int positionInFeature = 0;
			for (int i=subLocations.length-1;i>=0;i--) {
				exonBoundaries[i] = length;
				length += subLocations[i].length();
				for (int x=0;x<subLocations[i].length();x++) {
					genomeToFeatureMap[subLocations[i].end()-x] = positionInFeature;
					positionInFeature++;
				}

			}
		}
		
		// Now we can get all of the reads and position them within the read.		
		
		long [] reads = store.getReadsForProbe(new Probe(SeqMonkApplication.getInstance().dataCollection().genome().getExactChromsomeNameMatch(feature.chromosomeName()), feature.location().packedPosition()));
		
		for (int r=0;r<reads.length;r++) {
			// We need to work out the position of this read in the feature.  This will depend 
			// on whether the feature is forward or reverse strand, and whether we're reversing
			// the direction of reads.
			
			System.err.println("Looking at read "+SequenceRead.toString(reads[r]));
			
			int genomicPosition = 0;

			if (feature.location().strand() == Location.FORWARD) {
				System.err.println("It's a forward feature");
				if (reverse) {
					System.err.println("We're a same strand library");
					if (SequenceRead.strand(reads[r]) != Location.REVERSE) continue;
					genomicPosition = SequenceRead.end(reads[r]);
				}
				else {
					System.err.println("We're an opposing strand library");
					if (SequenceRead.strand(reads[r]) != Location.FORWARD) continue;
					genomicPosition = SequenceRead.start(reads[r]);
				}
				
				System.err.println("Raw genomic position is "+genomicPosition);
				genomicPosition = genomicPosition-feature.location().start();
				System.err.println("Corrected genomic position is "+genomicPosition);
			}
			else if (feature.location().strand() == Location.REVERSE) {
				if (reverse) {
					if (SequenceRead.strand(reads[r]) != Location.REVERSE) continue;
					genomicPosition = SequenceRead.start(reads[r]);
				}
				else {
					if (SequenceRead.strand(reads[r]) != Location.FORWARD) continue;
					genomicPosition = SequenceRead.end(reads[r]);
				}
				
				genomicPosition = feature.location().end()-genomicPosition;
				
			}
			
			System.err.println("Final genomic position is "+genomicPosition);
			
			if (genomicPosition < 0 || genomicPosition>=genomeToFeatureMap.length) continue;

			System.err.println("Position in feature is "+genomeToFeatureMap[genomicPosition]);

			// Now we need to translate the genomic position into a position within the read, and bail
			// out if it doesn't fall into an exon.
			
			if (genomeToFeatureMap[genomicPosition] != -1) {
				abundance[genomeToFeatureMap[genomicPosition]]++;
			}
					
		}
		
		this.abundance = abundance;
		this.exonBoundaries = exonBoundaries;
		calculated = true;
		repaint();
		
	}
	
	
	private int getYPixels (double value) {
		return (getHeight()-Y_AXIS_SPACE) - (int)((getHeight()-(10d+Y_AXIS_SPACE))*(value/100));
	}

	private int getXPixels (double value) {
		return X_AXIS_SPACE + (int)((getWidth()-(X_AXIS_SPACE*2))*(value/abundance.length));
	}

	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		if (!calculated) return;
		
		g.setColor(Color.BLACK);
		
		// Draw the axes
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE); // X-axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, X_AXIS_SPACE, 10); // Y-axis 1
		g.drawLine(getWidth()-X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-X_AXIS_SPACE, 10); // Y-axis 2
		
		// Draw the Y1 scale
		AxisScale yAxisScale = new AxisScale(0, 100);
		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < yAxisScale.getMax()) {
			g.drawString(yAxisScale.format(currentYValue), 5, getYPixels(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(X_AXIS_SPACE,getYPixels(currentYValue),X_AXIS_SPACE-3,getYPixels(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}
		
		// Draw the Y2 scale (abundance)
		yAxisScale = new AxisScale(0, 100);
		currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < yAxisScale.getMax()) {
			g.drawString(yAxisScale.format(currentYValue), getWidth()-(X_AXIS_SPACE-5), getYPixels(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(getWidth()-X_AXIS_SPACE,getYPixels(currentYValue),getWidth()-(X_AXIS_SPACE-3),getYPixels(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}
		
		// Draw x axis
		AxisScale xAxisScale = new AxisScale(1, abundance.length);
		double currentXValue = xAxisScale.getStartingValue();
		while (currentXValue < xAxisScale.getMax()) {
			g.drawString(xAxisScale.format(currentXValue), getXPixels(currentXValue), getHeight() - (g.getFontMetrics().getAscent()/2));
			g.drawLine(X_AXIS_SPACE,getYPixels(currentYValue),X_AXIS_SPACE-3,getYPixels(currentYValue));
			currentXValue += xAxisScale.getInterval();
		}

		// Highlight the exons
		int lastExonEndX = getXPixels(0);
		for (int i=1;i<exonBoundaries.length;i++) {
			
			int thisExonX = getXPixels(exonBoundaries[i]);
			if (i%2 != 0) {
				g.setColor(GREY);
				g.fillRect(lastExonEndX, getYPixels(100), thisExonX-lastExonEndX, getYPixels(0)-getYPixels(100));
			}
			
			lastExonEndX = thisExonX;
			
		}
		
		if (exonBoundaries.length%2 != 0) {
			int thisExonX = getXPixels(abundance.length);
			g.setColor(GREY);
			g.fillRect(lastExonEndX, getYPixels(100), thisExonX-lastExonEndX, getYPixels(0)-getYPixels(100));
		}
		
		
		// Draw the plots per codon
		
		// Work out the max count per codon
		int maxCountPerCodon = 0;
		for (int p=0;p<abundance.length;p+=3) {
			// Get the total abundance for the codon
			int codon = abundance[p]+abundance[p+1]+abundance[p+2];
			if (codon > maxCountPerCodon) maxCountPerCodon = codon;
		}
		
		float lastY1 = -1;
		float lastY2 = -1;
		float lastY3 = -1;
		float lastYabundance = -1;
		
		
		for (int p=0;p<abundance.length-2;p+=3) {
			// Get the total abundance for the codon
			int codon = abundance[p]+abundance[p+1]+abundance[p+2];
			
			float codonPercent = (codon*100f)/maxCountPerCodon;
			
			if (p>0) {
				g.setColor(Color.GRAY);
				g.drawLine(getXPixels(p-2), getYPixels(lastYabundance), getXPixels(p+1), getYPixels(codonPercent));
			}
			lastYabundance = codonPercent;
			
			// Calculate percentages for each sub-position
			// Don't allow infinite values
			
			float c1percent = lastY1;
			float c2percent = lastY2;
			float c3percent = lastY3;
			
			
			if (maxCountPerCodon > 0)  {
				c1percent = (abundance[p]*100f)/maxCountPerCodon;
				c2percent = (abundance[p+1]*100f)/maxCountPerCodon;
				c3percent = (abundance[p+2]*100f)/maxCountPerCodon;
			}
			
			System.err.println("Codon at "+p+" 1="+abundance[p]+" 2="+abundance[p+1]+" 3="+abundance[p+2]+" total="+codon);
			
			System.err.println("Codon percentages "+p+" 1="+c1percent+" 2="+c2percent+" 3="+c3percent);
			
			if (p>0) {
				g.setColor(RED);
				g.drawLine(getXPixels(p-3), getYPixels(lastY1), getXPixels(p), getYPixels(c1percent));
				
				g.setColor(GREEN);
				g.drawLine(getXPixels(p-2), getYPixels(lastY2), getXPixels(p+1), getYPixels(c2percent));

				g.setColor(BLUE);
				g.drawLine(getXPixels(p-1), getYPixels(lastY3), getXPixels(p+2), getYPixels(c3percent));

			}
			
			lastY1 = c1percent;
			lastY2 = c2percent;
			lastY3 = c3percent;
			
		}
		
		
	}
	
}
