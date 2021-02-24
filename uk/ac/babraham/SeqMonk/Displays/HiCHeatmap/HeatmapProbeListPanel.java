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
import java.util.Hashtable;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrixListener;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.InteractionProbePair;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

public class HeatmapProbeListPanel extends JPanel implements MouseListener, MouseMotionListener, HeatmapMatrixListener {

	private ProbeList [] probeLists;
	private HiCDataStore dataSet;
	private int totalProbeCount;

	private HeatmapMatrix matrix;

	private int maxNameWidth;
	private int nameHeight;
	private Hashtable<ProbeList, Long> probeListIndexOffsets = new Hashtable<ProbeList, Long>();

	// Variables used to limit which parts of the genome we show
	private int currentXStartIndex;
	private int currentXEndIndex;
	private int currentYStartIndex;
	private int currentYEndIndex;

	// Variables used when making a selection
	private boolean makingSelection = false;
	private int selectionStartX;
	private int selectionStartY;
	private int selectionEndX;
	private int selectionEndY;

	private ClusterPair currentCluster = null;
	private int [] probeSortingOrder = null;

	// These structures let us quickly skip over bits we can't see when
	// we draw the heatmap
	private boolean [][] drawnPixels = new boolean [0][0];

	// This list says how many clusters are contained in each grouped
	// set of probes according to the current r-value cutoff
	private int [] clusterIntervals = new int [0];

	// Formatter used for the top label
	private DecimalFormat df = new DecimalFormat("#.##");

	public HeatmapProbeListPanel (HiCDataStore dataSet, ProbeList [] probeLists, HeatmapMatrix matrix, Genome genome) {
		this.dataSet = dataSet;
		this.matrix = matrix;
		matrix.addOptionListener(this);
		this.probeLists = probeLists;

		for (int i=0;i<probeLists.length;i++){
			totalProbeCount += probeLists[i].getAllProbes().length;
		}

		// We start off looking at the whole genome on both axes
		currentXStartIndex = 0;
		currentYStartIndex = 0;
		currentXEndIndex = totalProbeCount-1;
		currentYEndIndex = totalProbeCount-1;

		addMouseListener(this);
		addMouseMotionListener(this);
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


			int xStart = getXForPosition(interactions[i].probe1Index());				
			if (xStart < maxNameWidth) continue;

			int xEnd = getXForPosition(interactions[i].probe1Index()+1);
			if (xEnd > getWidth()-10) continue;

			int yStart = getYForPosition(interactions[i].probe2Index());
			if (yStart>getHeight()-nameHeight) continue;

			int yEnd = getYForPosition(interactions[i].probe2Index()+1);
			if (yEnd < 30) continue;

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

	protected int currentYStartIndex () {
		return currentYStartIndex;
	}

	protected int currentYEndIndex () {
		return currentYEndIndex;
	}

	private int getXForPosition (int indexPosition) {

		int plotWidth = getWidth()-(10+maxNameWidth);
		double proportion = (((double)(indexPosition-currentXStartIndex)))/(double)(currentXEndIndex-currentXStartIndex);

		return maxNameWidth+(int)(plotWidth*proportion);
	}

	private int getPositionForX (int x) {

		int plotWidth = getWidth()-(10+maxNameWidth);

		double proportion = ((double)x-maxNameWidth)/plotWidth;

		return currentXStartIndex+(int)((currentXEndIndex-currentXStartIndex)*proportion);
	}


	private int getYForPosition (long indexPosition) {

		int plotHeight = getHeight()-(30+nameHeight);
		double proportion = (((double)(indexPosition-currentYStartIndex)))/(double)(currentYEndIndex-currentYStartIndex);

		return getHeight()-(int)(nameHeight+(plotHeight*proportion));
	}

	private int getPositionForY (int y) {

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

		// Find the max height and width of the probe list names in this genome
		if (maxNameWidth == 0) {
			nameHeight = g.getFontMetrics().getHeight();
			maxNameWidth = 0;

			long runningBaseOffset = 0;
			for (int l=0;l<probeLists.length;l++) {
				probeListIndexOffsets.put(probeLists[l], runningBaseOffset);
				int thisWidth = g.getFontMetrics().stringWidth(probeLists[l].name());
				if (thisWidth > maxNameWidth) maxNameWidth = thisWidth;
				runningBaseOffset += probeLists[l].getAllProbes().length;
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

			for (int forRev = 0; forRev <= 1; forRev++) {

				int probe1Index;
				int probe2Index;

				if (forRev == 0) {
					probe1Index = interactions[i].probe1Index();
					probe2Index = interactions[i].probe2Index();
				}
				else {
					probe2Index = interactions[i].probe1Index();
					probe1Index = interactions[i].probe2Index();
				}

				int xIndex = probe1Index;
				if (probeSortingOrder != null) {
					xIndex = probeSortingOrder[xIndex];
				}

				if (xIndex < currentXStartIndex) continue;
				if (xIndex > currentXEndIndex) continue;

				int xStart = getXForPosition(xIndex);				
				if (xStart < maxNameWidth) continue;

				int xEnd = getXForPosition(xIndex+1);
				if (xEnd > getWidth()-10) continue;

				int yIndex = probe2Index;
				if (probeSortingOrder != null) {
					yIndex = probeSortingOrder[yIndex];
				}

				if (yIndex < currentYStartIndex) continue;
				if (yIndex > currentYEndIndex) continue;

				int yStart = getYForPosition(yIndex);
				if (yStart>getHeight()-nameHeight) continue;

				int yEnd = getYForPosition(yIndex+1);
				if (yEnd < 30) continue;


				if (xEnd - xStart < 3) {
					xEnd+=1;
					xStart -= 1;
				}
				if (yEnd - yStart < 3) {
					yEnd-=1;
					yStart+=1;
				}

				// See if we can skip drawing this because something else is already there
				if (drawnPixels[xStart][yEnd]  && drawnPixels[xEnd][yStart]) {
					continue;
				}

				// Put the datasets on a non-linear gradient to try to get the
				// best contrast between the wide range of values we'll inevitably
				// have.
				
				switch (matrix.currentColourSetting()) {
				
				case HeatmapMatrix.COLOUR_BY_OBS_EXP: 
					g.setColor(matrix.colourGradient().getColor(Math.log10(interactions[i].strength()-matrix.initialMinStrength()), Math.log10(matrix.initialMinStrength()), Math.log10(matrix.maxValue()-matrix.initialMinStrength())));
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
				
				// Fill in the drawn pixels in the matrix so we don't draw on
				// them again
				for (int x=Math.min(xStart,xEnd);x<=Math.min(xStart, xEnd)+Math.abs(xStart-xEnd);x++) {
					for (int y=Math.min(yStart,yEnd);y<=Math.min(yStart, yEnd)+Math.abs(yStart-yEnd);y++) {
						drawnPixels[x][y] = true;
					}
				}
			}

		}

//		System.err.println("Skipped "+skipped+" interactions");

		// Draw the probe list lines
		g.setColor(Color.GRAY);

		// If we have clustered the probes then we don't want to draw probe list
		// lines but we will bracket around related groups.
		if (currentCluster == null) {
			// Draw Probe List Lines on X axis
			int runningGenomeLength = 0;
			for (int l=0;l<probeLists.length;l++) {
				int startPos = getXForPosition(runningGenomeLength);
				int endPos = getXForPosition(runningGenomeLength+probeLists[l].getAllProbes().length);

				if (l>0) {
					if (startPos >= maxNameWidth && startPos <= getWidth()-10) {
						g.drawLine(startPos, 30, startPos, getHeight()-nameHeight);
					}
				}
				if (l+1 == probeLists.length) {
					if (endPos >= maxNameWidth && endPos <= getWidth()-10) {
						g.drawLine(endPos, 30, endPos, getHeight()-nameHeight);
					}
				}

				int nameWidth = g.getFontMetrics().stringWidth(probeLists[l].name());

				g.drawString(probeLists[l].name(), (startPos+((endPos-startPos)/2))-(nameWidth/2), getHeight()-3);

				runningGenomeLength += probeLists[l].getAllProbes().length;

			}

			// Draw Chr Lines on Y axis
			runningGenomeLength = 0;
			for (int l=0;l<probeLists.length;l++) {
				int startPos = getYForPosition(runningGenomeLength);
				int endPos = getYForPosition(runningGenomeLength+probeLists[l].getAllProbes().length);

				if (l>0) {
					if (startPos<=getHeight()-nameHeight && startPos >= 30) {
						g.drawLine(maxNameWidth, startPos, getWidth()-10, startPos);
					}
				}
				if (l+1 == probeLists.length) {
					if (endPos<=getHeight()-nameHeight && endPos >= 30) {
						g.drawLine(maxNameWidth, endPos, getWidth()-10, endPos);
					}
				}

				int nameWidth = g.getFontMetrics().stringWidth(probeLists[l].name());

				g.drawString(probeLists[l].name(), (maxNameWidth/2)-(nameWidth/2), (endPos+((startPos-endPos)/2))+(g.getFontMetrics().getAscent()/2));

				runningGenomeLength += probeLists[l].getAllProbes().length;
			}
		}

		// If we are clustered then we draw bracketed sets around the current R value cutoff
		else {

			// Draw Cluster Lines on X axis
			int runningListPosition = 0;
			for (int l=0;l<clusterIntervals.length;l++) {
				runningListPosition += clusterIntervals[l];

				if (runningListPosition < currentXStartIndex) continue;
				if (runningListPosition > currentXEndIndex) break;
				int pos = getXForPosition(runningListPosition);

				g.drawLine(pos, 30, pos, getHeight()-nameHeight);

			}

			// Draw Cluster Lines on Y axis
			runningListPosition = 0;
			for (int l=0;l<clusterIntervals.length;l++) {

				runningListPosition += clusterIntervals[l];

				if (runningListPosition < currentYStartIndex) continue;
				if (runningListPosition > currentYEndIndex) break;

				int pos = getYForPosition(runningListPosition);

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

	}

	public void mouseClicked(MouseEvent arg0) {}

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
				int xMid = currentXStartIndex+((currentXEndIndex-currentXStartIndex)/2);
				currentXStartIndex = xMid-(currentXEndIndex-currentXStartIndex);
				currentXEndIndex = xMid+(currentXEndIndex-currentXStartIndex);
				if (currentXStartIndex < 0) currentXStartIndex = 0;
				if (currentXEndIndex > totalProbeCount-1) currentXEndIndex = totalProbeCount-1;
			}

			// If they control click we ignore expansion on the y-axis
			if (! me.isControlDown()) {
				int yMid = currentYStartIndex+((currentYEndIndex-currentYStartIndex)/2);
				currentYStartIndex = yMid-(currentYEndIndex-currentYStartIndex);
				currentYEndIndex = yMid+(currentYEndIndex-currentYStartIndex);
				if (currentYStartIndex < 0) currentYStartIndex = 0;
				if (currentYEndIndex > totalProbeCount-1) currentYEndIndex = totalProbeCount-1;
			}

		}
		else {

			// Don't allow really small selections
			if (Math.abs(selectionEndX-selectionStartX) < 3 || Math.abs(selectionEndY-selectionStartY)<3) {
				makingSelection = false;
				repaint();				
				return;
			}

			int newXStart = getPositionForX(Math.min(selectionEndX, selectionStartX));
			int newXEnd = getPositionForX(Math.max(selectionEndX, selectionStartX));
			int newYStart = getPositionForY(Math.max(selectionEndY, selectionStartY));
			int newYEnd = getPositionForY(Math.min(selectionEndY, selectionStartY));

			// Don't allow a view smaller than 1 index
			if (newXEnd != newXStart) {
				currentXStartIndex = newXStart;
				currentXEndIndex = newXEnd;
			}
			if (newYEnd != newYStart) {
				currentYStartIndex = newYStart;
				currentYEndIndex = newYEnd;
			}

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

	public void newCluster(ClusterPair cluster) {
		currentCluster = cluster;

		if (currentCluster == null) {
			probeSortingOrder = null;
		}
		else {
			Integer [] reverseLookup = currentCluster.getAllIndices();
			probeSortingOrder = new int [reverseLookup.length];
			for (int i=0;i<reverseLookup.length;i++) {
				probeSortingOrder[reverseLookup[i]] = i;
			}

			if (probeSortingOrder.length != totalProbeCount) {
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
