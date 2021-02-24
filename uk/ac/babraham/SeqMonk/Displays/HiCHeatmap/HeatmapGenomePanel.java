/**
 * Copyright 2011- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HiCHeatmap;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedWriter;
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

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrixListener;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.InteractionProbePair;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

public class HeatmapGenomePanel extends JPanel implements MouseListener, MouseMotionListener, HeatmapMatrixListener {

	private HiCDataStore dataSet;
	//	private ProbeList probeList;
	private Probe [] probes;
	private Genome genome;
	private long totalGenomeLength;

	private HeatmapMatrix matrix;

	private int maxNameWidth;
	private int nameHeight;
	private Hashtable<Chromosome, Long> chromosomeBaseOffsets = new Hashtable<Chromosome, Long>();

	// Variables used to limit which parts of the genome we show
	private long currentXStartBp;
	private long currentXEndBp;
	private long currentYStartBp;
	private long currentYEndBp;
	private int currentYStartIndex;
	private int currentYEndIndex;

	// Variables used when making a selection
	private boolean makingSelection = false;
	private int selectionStartX;
	private int selectionStartY;
	private int selectionEndX;
	private int selectionEndY;
	
	// Variables used when selecting a single point.
	private boolean displayXProbe = false;
	private boolean displayYProbe = false;
	private int displayX = 0;
	private int displayY = 0;

	// Used for restructuring the display if we have clustered the y-axis
	private ClusterPair currentCluster = null;
	private int [] probeSortingOrder = null;

	// This list says how many clusters are contained in each grouped
	// set of probes according to the current r-value cutoff
	private int [] clusterIntervals = new int [0];

	// Formatter used for the top label
	private DecimalFormat df = new DecimalFormat("#.##");

	// A set of listeners which will respond whenever the region we're
	// displaying changes.
	private Vector<HeatmapPositionListener> positionListeners = new Vector<HeatmapPositionListener>();

	// A bit array that we can use to skip drawing features which won't be seen
	private boolean [][] drawnPixels = new boolean[0][0];

	public HeatmapGenomePanel (HiCDataStore dataSet, ProbeList probes, HeatmapMatrix matrix, Genome genome) {
		this.dataSet = dataSet;
		//		this.probeList = probes;
		this.probes = probes.getAllProbes();
		this.matrix = matrix;
		matrix.addOptionListener(this);
		this.genome = genome;

		// Calculating the total genome length through the genome
		// object is slow so we cache the value here since we use it
		// so much
		totalGenomeLength = genome.getTotalGenomeLength();

		// We start off looking at the whole genome on both axes
		currentXStartBp = 0;
		currentYStartBp = 0;
		currentXEndBp = totalGenomeLength;
		currentYEndBp = totalGenomeLength;
		currentYStartIndex = 0;
		currentYEndIndex = this.probes.length-1;

		Chromosome [] chrs = genome.getAllChromosomes();
		Arrays.sort(chrs);

		if (maxNameWidth == 0) {
			long runningBaseOffset = 0;
			for (int c=0;c<chrs.length;c++) {
				chromosomeBaseOffsets.put(chrs[c], runningBaseOffset);
				runningBaseOffset += chrs[c].length();
			}
		}

		addMouseListener(this);
		addMouseMotionListener(this);
	}


	public void addPositionListener (HeatmapPositionListener l) {
		if (l != null && ! positionListeners.contains(l)) {
			positionListeners.add(l);
		}
	}

	public void removePositionListener (HeatmapPositionListener l) {
		if (l != null && positionListeners.contains(l)) {
			positionListeners.remove(l);
		}
	}


	public void setCurrentPosition (Chromosome chr, int start, int end) {
		long baseStart = chromosomeBaseOffsets.get(chr)+start;
		long baseEnd = chromosomeBaseOffsets.get(chr)+end;
		currentXStartBp = baseStart;
		currentXEndBp = baseEnd;
		currentYStartBp = baseStart;
		currentYEndBp = baseEnd;

		Enumeration<HeatmapPositionListener> en = positionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().updatePosition(chr, start, chr, end, chr, start, chr, end);
		}

		repaint();
	}

	public void saveData (File file) throws IOException {

		PrintWriter pr = new PrintWriter(new BufferedWriter(new FileWriter(file)));

		StringBuffer sb = new StringBuffer();
		sb.append("Probe 1");
		sb.append("\t");
		sb.append("Chr1");
		sb.append("\t");
		sb.append("Start1");
		sb.append("\t");
		sb.append("End1");
		sb.append("\t");
		sb.append("Strand1");
		sb.append("\t");
		sb.append("Probe 2");
		sb.append("\t");
		sb.append("Chr2");
		sb.append("\t");
		sb.append("Start2");
		sb.append("\t");
		sb.append("End2");
		sb.append("\t");
		sb.append("Strand2");
		sb.append("\t");
		sb.append(dataSet);
		sb.append("\n");

		pr.print(sb.toString());

		InteractionProbePair [] interactions = matrix.filteredInteractions();

		for (int i=0;i<interactions.length;i++) {

			// Check that this interaction is within the currently visible region
			long xStart = chromosomeBaseOffsets.get(interactions[i].probe1().chromosome())+interactions[i].probe1().start();
			if (xStart < currentXStartBp || xStart > currentXEndBp) continue;

			long xEnd = chromosomeBaseOffsets.get(interactions[i].probe1().chromosome())+interactions[i].probe1().end();
			if (xEnd < currentXStartBp || xEnd > currentXEndBp) continue;

			long yStart = chromosomeBaseOffsets.get(interactions[i].probe2().chromosome())+interactions[i].probe2().start();
			if (yStart < currentYStartBp || yStart > currentYEndBp) continue;

			long yEnd = chromosomeBaseOffsets.get(interactions[i].probe2().chromosome())+interactions[i].probe2().end();
			if (yEnd < currentYStartBp || yEnd > currentYEndBp) continue;

			sb = new StringBuffer();

			sb.append(interactions[i].probe1());
			sb.append("\t");
			sb.append(interactions[i].probe1().chromosome());
			sb.append("\t");
			sb.append(interactions[i].probe1().start());
			sb.append("\t");
			sb.append(interactions[i].probe1().end());
			sb.append("\t");
			if (interactions[i].probe1().strand() == Probe.FORWARD) {
				sb.append("+");
			}
			else if (interactions[i].probe1().strand() == Probe.REVERSE) {
				sb.append("");
			}
			sb.append("\t");
			sb.append(interactions[i].probe2());
			sb.append("\t");
			sb.append(interactions[i].probe2().chromosome());
			sb.append("\t");
			sb.append(interactions[i].probe2().start());
			sb.append("\t");
			sb.append(interactions[i].probe2().end());
			sb.append("\t");
			if (interactions[i].probe2().strand() == Probe.FORWARD) {
				sb.append("+");
			}
			else if (interactions[i].probe2().strand() == Probe.REVERSE) {
				sb.append("");
			}
			sb.append("\t");
			sb.append(interactions[i].strength());
			sb.append("\n");

			pr.print(sb.toString());
		}

		pr.close();

	}


	private Chromosome getChrForPosition (long basePosition) {
		// We find the maximum offset which is lower than this basePosition
		Enumeration<Chromosome>en = chromosomeBaseOffsets.keys();
		long highest = -1;
		Chromosome chr = null;

		while (en.hasMoreElements()) {
			Chromosome thisChr = en.nextElement();
			long offset = basePosition - chromosomeBaseOffsets.get(thisChr);
			if (offset >= 0 && chromosomeBaseOffsets.get(thisChr) > highest) {
				highest = chromosomeBaseOffsets.get(thisChr);
				chr = thisChr;
			}
		}

		return chr;
	}

	private int getXForPosition (long basePosition) {

		int plotWidth = getWidth()-(10+maxNameWidth);
		double proportion = (((double)(basePosition-currentXStartBp)))/(double)(currentXEndBp-currentXStartBp);

		return maxNameWidth+(int)(plotWidth*proportion);
	}

	private long getPositionForX (int x) {

		int plotWidth = getWidth()-(10+maxNameWidth);

		double proportion = ((double)x-maxNameWidth)/plotWidth;

		return currentXStartBp+(long)((currentXEndBp-currentXStartBp)*proportion);
	}


	private int getYForPosition (long basePosition) {

		int plotHeight = getHeight()-(30+nameHeight);
		double proportion = (((double)(basePosition-currentYStartBp)))/(double)(currentYEndBp-currentYStartBp);

		return getHeight()-(int)(nameHeight+(plotHeight*proportion));
	}

	private long getPositionForY (int y) {

		int plotHeight = getHeight()-(30+nameHeight);

		double proportion = 1-(((double)(y-30))/plotHeight);

		return currentYStartBp+(long)((currentYEndBp-currentYStartBp)*proportion);
	}

	private int getYForIndex (int indexPosition) {

		int plotHeight = getHeight()-(30+nameHeight);
		double proportion = (((double)(indexPosition-currentYStartIndex)))/(double)(currentYEndIndex-currentYStartIndex);

		return getHeight()-(int)(nameHeight+(plotHeight*proportion));
	}

	private int getIndexForY (int y) {

		int plotHeight = getHeight()-(30+nameHeight);

		double proportion = 1-(((double)(y-30))/plotHeight);

		return currentYStartIndex+(int)((currentYEndIndex-currentYStartIndex)*proportion);
	}


	public void paint (Graphics g) {

		super.paint(g);

		if (drawnPixels.length != getWidth() || drawnPixels[0].length != getHeight()) {
			drawnPixels = new boolean[getWidth()][getHeight()];
		}
		else {
			for (int i=0;i<getWidth();i++) {
				for (int j=0;j<getHeight();j++) {
					drawnPixels[i][j] = false;
				}
			}
		}
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);

		// Add a label at the top to signify current filters
		StringBuffer topLabel = new StringBuffer();

		// First include the position
		Chromosome xStartChr = getChrForPosition(currentXStartBp);

		if (xStartChr != null) {
			topLabel.append("X=Chr");
			topLabel.append(xStartChr.name());
			topLabel.append(":");
			topLabel.append(PositionFormat.formatLength(currentXStartBp-chromosomeBaseOffsets.get(xStartChr)));
			topLabel.append("-");

			Chromosome xEndChr = getChrForPosition(currentXEndBp);
			if (xStartChr != xEndChr) {
				topLabel.append("Chr");
				topLabel.append(xEndChr.name());
				topLabel.append(":");
			}
			topLabel.append(PositionFormat.formatLength(currentXEndBp-chromosomeBaseOffsets.get(xEndChr)));

			if (probeSortingOrder == null) {
				topLabel.append(" Y=Chr");
				Chromosome yStartChr = getChrForPosition(currentYStartBp);
				topLabel.append(yStartChr.name());
				topLabel.append(":");
				topLabel.append(PositionFormat.formatLength(currentYStartBp-chromosomeBaseOffsets.get(yStartChr)));
				topLabel.append("-");

				Chromosome yEndChr = getChrForPosition(currentYEndBp);
				if (yStartChr != yEndChr) {
					topLabel.append("Chr");
					topLabel.append(yEndChr.name());
					topLabel.append(":");
				}
				topLabel.append(PositionFormat.formatLength(currentYEndBp-chromosomeBaseOffsets.get(yEndChr)));
			}

			topLabel.append(" ");
		}

		// Now append any current limits
		if (matrix.currentMinStrength() > 0) {
			topLabel.append("Strength > ");
			topLabel.append(df.format(matrix.currentMinStrength()));
			topLabel.append(" ");
		}

		if (matrix.minDifference() > 0) {
			topLabel.append("Difference > ");
			topLabel.append(df.format(matrix.minDifference()));
			topLabel.append(" ");
		}

		if (matrix.currentMaxSignficance() < 1) {
			topLabel.append("P-value < ");
			topLabel.append(df.format(matrix.currentMaxSignficance()));
			topLabel.append(" ");
		}

		if (topLabel.length() == 0) {
			topLabel.append("No filters");
		}

		g.drawString(topLabel.toString(), getWidth()/2-(g.getFontMetrics().stringWidth(topLabel.toString())/2), 15+(g.getFontMetrics().getAscent()/2));

		Chromosome [] chrs = genome.getAllChromosomes();
		Arrays.sort(chrs);

		// Find the max height and width of the chromosome names in this genome
		if (maxNameWidth == 0) {
			nameHeight = g.getFontMetrics().getHeight();
			maxNameWidth = 0;

			for (int c=0;c<chrs.length;c++) {
				int thisWidth = g.getFontMetrics().stringWidth(chrs[c].name());
				if (thisWidth > maxNameWidth) maxNameWidth = thisWidth;
			}

			// Give both the width and height a bit of breathing space
			nameHeight += 6;
			maxNameWidth += 6;
		}



		// Make the background of the plot black
		g.setColor(Color.WHITE);
		g.fillRect(maxNameWidth, 30, getWidth()-(maxNameWidth+10), getHeight()-(nameHeight+30));

		// Draw the actual data

		InteractionProbePair [] interactions = matrix.filteredInteractions();
		
		// Cache some values for use with the quantitation colouring
		double minQuantitatedValue;
		double maxQuantitatedValue;
		if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE) {
			minQuantitatedValue = 0;
			maxQuantitatedValue = DisplayPreferences.getInstance().getMaxDataValue();
		}
		else {
			maxQuantitatedValue = DisplayPreferences.getInstance().getMaxDataValue();
			minQuantitatedValue = 0-maxQuantitatedValue;
		}
		
		for (int i=0;i<interactions.length;i++) {

			// Make a mini loop which allows us to draw each interaction twice to make
			// the plot symmetrical
			for (int forRev = 0; forRev <= 1; forRev++) {

				int yIndex;

				Probe probe1;
				Probe probe2;

				if (forRev == 0) {
					yIndex = interactions[i].probe2Index();
					probe1 = interactions[i].probe1();
					probe2 = interactions[i].probe2();
				}
				else {
					yIndex = interactions[i].probe1Index();
					probe2 = interactions[i].probe1();
					probe1 = interactions[i].probe2();
				}

				if (probeSortingOrder != null) {
					yIndex = probeSortingOrder[yIndex];
					if (yIndex < currentYStartIndex || yIndex > currentYEndIndex) {
//						System.err.println("Skipping for y zoom");
						continue;
					}
				}

				int xStart = getXForPosition(chromosomeBaseOffsets.get(probe1.chromosome())+probe1.start());				
				if (xStart < maxNameWidth) continue;
				if (chromosomeBaseOffsets.get(probe1.chromosome())+probe1.start() > currentXEndBp) {
//					System.err.println("Skipping for x end");
					continue;
				}

				int xEnd = getXForPosition(chromosomeBaseOffsets.get(probe1.chromosome())+probe1.end());
				if (xEnd > getWidth()-10) continue;
				if (chromosomeBaseOffsets.get(probe1.chromosome())+probe1.end() < currentXStartBp) {
//					System.err.println("Skipping for x start");
					continue;
				}

				int yStart;
				int yEnd;

				if (probeSortingOrder == null) {
					if (chromosomeBaseOffsets.get(probe2.chromosome())+probe2.start() > currentYEndBp) continue;
					if (chromosomeBaseOffsets.get(probe2.chromosome())+probe2.end() < currentYStartBp) continue;
					yStart = getYForPosition(chromosomeBaseOffsets.get(probe2.chromosome())+probe2.start());
					yEnd = getYForPosition(chromosomeBaseOffsets.get(probe2.chromosome())+probe2.end());
				}
				else {
					yStart = getYForIndex(probeSortingOrder[yIndex]);
					yEnd = getYForIndex(probeSortingOrder[yIndex]+1);
				}

				if (yStart>getHeight()-nameHeight) {
					continue;
				}

				if (yEnd < 30) continue;

				if (xEnd - xStart < 3) {
					xEnd+=1;
					xStart -= 1;
				}
				if (yStart - yEnd < 3) {
//					System.err.println("Expanding y selection");
					yEnd-=1;
					yStart+=1;
				}

				// See if we can skip drawing this because something else is already there
				// To be skipped there has to be colour at two corners and the middle.
				if (drawnPixels[xStart][yEnd]  && drawnPixels[xEnd][yStart] && drawnPixels[xStart+((xEnd-xStart)/2)][yEnd+((yStart-yEnd)/2)]) {
//					System.err.println("Skipping for overlap with existing");
					continue;
				}
				
				// Put the datasets on a non-linear gradient to try to get the
				// best contrast between the wide range of values we'll inevitably
				// have.
				
				switch (matrix.currentColourSetting()) {
				
				case HeatmapMatrix.COLOUR_BY_OBS_EXP:
					if (matrix.initialMinStrength() < 1) {
						// They're interested in depletion as well as enrichment.
						// Make a symmetrical gradient around 0 and the max strength
						g.setColor(matrix.colourGradient().getColor(Math.log10(interactions[i].strength()), Math.log10(1/matrix.maxValue()), Math.log10(matrix.maxValue())));						
					}
					else {
						g.setColor(matrix.colourGradient().getColor(Math.log10(interactions[i].strength()-matrix.initialMinStrength()), Math.log10(matrix.initialMinStrength()), Math.log10(matrix.maxValue()-matrix.initialMinStrength())));
					}
					break;

				case HeatmapMatrix.COLOUR_BY_INTERACTIONS: 
					g.setColor(matrix.colourGradient().getColor(interactions[i].absolute(),matrix.initialMinAbsolute(),50));
					break;

				case HeatmapMatrix.COLOUR_BY_P_VALUE:
					g.setColor(matrix.colourGradient().getColor(Math.log10(interactions[i].signficance())*-10,Math.log10(matrix.initialMaxSignificance())*-10,50));
					break;

				case HeatmapMatrix.COLOUR_BY_QUANTITATION:
					Probe probeForQuantitation;
					if (forRev == 0) {
						probeForQuantitation = interactions[i].lowestProbe();
					}
					else {
						probeForQuantitation = interactions[i].highestProbe();
					}

					try {
						g.setColor(matrix.colourGradient().getColor(((DataStore)dataSet).getValueForProbe(probeForQuantitation),minQuantitatedValue,maxQuantitatedValue));
					} catch (SeqMonkException e) {}
					break;

				}

				g.fillRect(xStart,yEnd, xEnd-xStart, yStart-yEnd);
				
				// If we're looking for selected probes check this now.
				if (displayXProbe || displayYProbe) {
					if (xStart<=displayX && xEnd>=displayX && yEnd<=displayY && yStart>=displayY) {
						if (displayXProbe) {
							DisplayPreferences.getInstance().setLocation(interactions[i].probe1().chromosome(), interactions[i].probe1().packedPosition());
							displayXProbe = false;
						}
						if (displayYProbe) {
							DisplayPreferences.getInstance().setLocation(interactions[i].probe2().chromosome(), interactions[i].probe2().packedPosition());
							displayYProbe = false;
						}
					}
				}
				
				
				// Fill in the drawn pixels in the matrix so we don't draw on
				// them again
				for (int x=Math.min(xStart,xEnd);x<=Math.min(xStart, xEnd)+Math.abs(xStart-xEnd);x++) {
					for (int y=Math.min(yStart,yEnd);y<=Math.min(yStart, yEnd)+Math.abs(yStart-yEnd);y++) {
						drawnPixels[x][y] = true;
					}
				}
			}
		}
		
//		System.err.println("Skipped "+skipped+" and drew "+drawn+" out of "+(interactions.length*2)+" interactions");

		// Draw the chromosome lines
		g.setColor(Color.GRAY);

		// Draw Chr Lines on X axis
		long runningGenomeLength = 0;
		for (int c=0;c<chrs.length;c++) {
			int startPos = getXForPosition(runningGenomeLength);
			int endPos = getXForPosition(runningGenomeLength+chrs[c].length());

			if (c>0) {
				if (startPos >= maxNameWidth && startPos <= getWidth()-10) {
					g.drawLine(startPos, 30, startPos, getHeight()-nameHeight);
				}
			}
			if (c+1 == chrs.length) {
				if (endPos >= maxNameWidth && endPos <= getWidth()-10) {
					g.drawLine(endPos, 30, endPos, getHeight()-nameHeight);
				}
			}

			int nameWidth = g.getFontMetrics().stringWidth(chrs[c].name());

			g.drawString(chrs[c].name(), (startPos+((endPos-startPos)/2))-(nameWidth/2), getHeight()-3);

			runningGenomeLength += chrs[c].length();

		}

		// Draw Chr Lines on Y axis
		if (probeSortingOrder == null) {
			runningGenomeLength = 0;
			for (int c=0;c<chrs.length;c++) {
				int startPos = getYForPosition(runningGenomeLength);
				int endPos = getYForPosition(runningGenomeLength+chrs[c].length());

				if (c>0) {
					if (startPos<=getHeight()-nameHeight && startPos >= 30) {
						g.drawLine(maxNameWidth, startPos, getWidth()-10, startPos);
					}
				}
				if (c+1 == chrs.length) {
					if (endPos<=getHeight()-nameHeight && endPos >= 30) {
						g.drawLine(maxNameWidth, endPos, getWidth()-10, endPos);
					}
				}

				int nameWidth = g.getFontMetrics().stringWidth(chrs[c].name());

				g.drawString(chrs[c].name(), (maxNameWidth/2)-(nameWidth/2), (endPos+((startPos-endPos)/2))+(g.getFontMetrics().getAscent()/2));

				runningGenomeLength += chrs[c].length();
			}
		}

		else {

			int runningListPosition = 0;
			for (int l=0;l<clusterIntervals.length;l++) {
				runningListPosition += clusterIntervals[l];

				if (runningListPosition < currentYStartIndex) continue;
				if (runningListPosition > currentYEndIndex) break;

				int pos = getYForIndex(runningListPosition);

				g.drawLine(maxNameWidth, pos, getWidth()-10, pos);
			}

		}



		// Draw the axes
		g.drawLine(maxNameWidth, getHeight()-nameHeight, getWidth()-10, getHeight()-nameHeight);
		g.drawLine(maxNameWidth, getHeight()-nameHeight, maxNameWidth, 30);


		// Draw a selection if we're making one
		if (makingSelection) {
			g.setColor(ColourScheme.DRAGGED_SELECTION);

			g.drawRect(Math.min(selectionEndX,selectionStartX), Math.min(selectionEndY, selectionStartY), Math.abs(selectionEndX-selectionStartX), Math.abs(selectionEndY-selectionStartY));
		}
		
		displayXProbe = false;
		displayYProbe = false;

	}

	public void mouseClicked(MouseEvent me) {
		
		if (me.getClickCount() == 2) {
			if (me.isShiftDown()) {
				displayYProbe = true;
			}
			else {
				displayXProbe = true;
			}
			displayX = me.getX();
			displayY = me.getY();
			repaint();
		}
		
	}

	public void mouseEntered(MouseEvent arg0) {}

	public void mouseExited(MouseEvent arg0) {}

	public void mousePressed(MouseEvent me) {
		// Don't do anything if they pressed the right mouse button
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {
			return;
		}

		// If they're inside the plot area then start a selection
		makingSelection = true;
		selectionStartX = me.getX();
		if (selectionStartX < maxNameWidth) selectionStartX = maxNameWidth;
		if (selectionStartX > getWidth()-10) selectionStartX = getWidth()-10;

		selectionStartY = me.getY();
		if (selectionStartY < 30) selectionStartY = 30;
		if (selectionStartY > getHeight()-nameHeight) selectionStartY = getHeight()-nameHeight;

		selectionEndX = selectionStartX;
		selectionEndY = selectionStartY;

	}

	public void mouseReleased(MouseEvent me) {

		// If they're right-clicking then we zoom out
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {

			// If they shift click we ignore the expansion on the x-axis
			if (! me.isShiftDown()) {
				long xMid = currentXStartBp+((currentXEndBp-currentXStartBp)/2);
				currentXStartBp = xMid-(currentXEndBp-currentXStartBp);
				currentXEndBp = xMid+(currentXEndBp-currentXStartBp);
				if (currentXStartBp < 0) currentXStartBp = 0;
				if (currentXEndBp > totalGenomeLength) currentXEndBp = totalGenomeLength;
			}

			// If they control click we ignore expansion on the y-axis
			if (! me.isControlDown()) {
				if (probeSortingOrder == null) {
					long yMid = currentYStartBp+((currentYEndBp-currentYStartBp)/2);
					currentYStartBp = yMid-(currentYEndBp-currentYStartBp);
					currentYEndBp = yMid+(currentYEndBp-currentYStartBp);
					if (currentYStartBp < 0) currentYStartBp = 0;
					if (currentYEndBp > totalGenomeLength) currentYEndBp = totalGenomeLength;
				}
				else {
					int yMid = currentYStartIndex+((currentYEndIndex-currentYStartIndex)/2);
					currentYStartIndex = yMid-(currentYEndIndex-currentYStartIndex);
					currentYEndIndex = yMid+(currentYEndIndex-currentYStartIndex);
					if (currentYStartIndex < 0) currentYStartIndex = 0;
					if (currentYEndIndex > probes.length-1) currentYEndIndex = probes.length-1;

				}
			}

		}
		else {

			// Don't allow really small selections
			if (Math.abs(selectionEndX-selectionStartX) < 3 || Math.abs(selectionEndY-selectionStartY)<3) {
				makingSelection = false;
				repaint();
				return;
			}

			long newXStart = getPositionForX(Math.min(selectionEndX, selectionStartX));
			long newXEnd = getPositionForX(Math.max(selectionEndX, selectionStartX));
			// Don't allow a view smaller than 50bp
			if (newXEnd - newXStart > 50) {
				currentXStartBp = newXStart;
				currentXEndBp = newXEnd;
			}

			if (probeSortingOrder == null) {
				long newYStart = getPositionForY(Math.max(selectionEndY, selectionStartY));
				long newYEnd = getPositionForY(Math.min(selectionEndY, selectionStartY));

				// Don't allow a view smaller than 50bp
				if (newYEnd - newYStart > 50) {
					currentYStartBp = newYStart;
					currentYEndBp = newYEnd;
				}

			}
			else {
				int newYStart = getIndexForY(Math.max(selectionEndY, selectionStartY));
				int newYEnd = getIndexForY(Math.min(selectionEndY, selectionStartY));

				System.err.println("New Y start = "+newYStart+" new Y end = "+newYEnd);

				if (newYEnd != newYStart) {
					currentYStartIndex = newYStart;
					currentYEndIndex = newYEnd;
					if (currentYStartIndex < 0) currentYStartIndex = 0;
					if (currentYEndIndex > probes.length-1) currentYEndIndex = probes.length-1;
				}
			}

		}

		Enumeration<HeatmapPositionListener> en = positionListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().updatePosition(
					getChrForPosition(currentXStartBp), 
					(int)(currentXStartBp-chromosomeBaseOffsets.get(getChrForPosition(currentXStartBp))), 
					getChrForPosition(currentXEndBp), 
					(int)(currentXEndBp-chromosomeBaseOffsets.get(getChrForPosition(currentXEndBp))), 
					getChrForPosition(currentYStartBp), 
					(int)(currentYStartBp-chromosomeBaseOffsets.get(getChrForPosition(currentYStartBp))), 
					getChrForPosition(currentYEndBp), 
					(int)(currentYEndBp-chromosomeBaseOffsets.get(getChrForPosition(currentYEndBp))) 
					);
		}

		makingSelection = false;
		repaint();

	}

	public void mouseDragged(MouseEvent me) {

		if (makingSelection) {
			selectionEndX = me.getX();
			if (selectionEndX < maxNameWidth) selectionEndX = maxNameWidth;
			if (selectionEndX > getWidth()-10) selectionEndX = getWidth()-10;

			selectionEndY = me.getY();
			if (selectionEndY < 30) selectionEndY = 30;
			if (selectionEndY > getHeight()-nameHeight) selectionEndY = getHeight()-nameHeight;

			repaint();
		}		
	}

	public void mouseMoved(MouseEvent arg0) {}

	public void newMaxSignificanceValue(double asymmetry) {
		repaint();
	}

	public void newMinDifferenceValue(double difference) {
		repaint();
	}

	public void newMinDistanceValue(int distance) {
		repaint();
	}

	public void newMaxDistanceValue(int distance) {
		repaint();
	}

	public void newMinAbsoluteValue (int absolute) {
		repaint();
	}

	public void newMinStrengthValue(double strength) {
		repaint();
	}

	protected int currentYStartIndex () {
		return currentYStartIndex;
	}

	protected int currentYEndIndex () {
		return currentYEndIndex;
	}

	public void newCluster(ClusterPair cluster) {
		currentCluster = cluster;

		currentYStartBp = 0;
		currentYEndBp = totalGenomeLength;
		currentYStartIndex = 0;
		currentYEndIndex = probes.length-1;

		if (currentCluster == null) {
			probeSortingOrder = null;
		}
		else {
			Integer [] reverseLookup = currentCluster.getAllIndices();
			probeSortingOrder = new int [reverseLookup.length];
			for (int i=0;i<reverseLookup.length;i++) {
				probeSortingOrder[reverseLookup[i]] = i;
			}

			if (probeSortingOrder.length != probes.length) {
				probeSortingOrder = null;
				throw new IllegalArgumentException("The sorted indices list must be the same size as the starting probe list");
			}
		}

		// Recalculate the cluster groupings
		newClusterRValue(matrix.currentClusterRValue());
	}

	public void newClusterRValue(float rValue) {

		// This could be being set before the cluster result is done.
		if (matrix.cluster() == null) {
			repaint();
			return;
		}

		ClusterPair [] connectedClusters = matrix.cluster().getConnectedClusters(rValue);

		int [] newClusterIntervals = new int [connectedClusters.length];

		for (int i=0;i<connectedClusters.length;i++) {
			newClusterIntervals[i] = connectedClusters[i].getAllIndices().length;
		}

		clusterIntervals = newClusterIntervals;

		repaint();
	}


	public void newColourSetting(int colour) {
		repaint();
	}

	public void newColourGradient(ColourGradient gradient) {
		repaint();
	}

	
	public void newProbeFilterList(ProbeList list) {
		repaint();
	}


}
