/**
 * Copyright 2012-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HierarchicalClusterPlot;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

public class HierarchicalClusterPanel extends JPanel implements MouseListener, MouseMotionListener {

	private ProbeList probeList;
	private DataStore [] stores;
	private ClusterPair clusters;
	private int [] clusterPostions;
	private int [] clusterIntervals;
	private Probe [] probes;
	private boolean normalise;

	private int lastHeight = 0;
	private boolean [] skippablePositions;

	// Variables used when we're zooming in on the y-axis
	private int currentYStartIndex;
	private int currentYEndIndex;

	// Data Zoom level
	private double maxValue = 2;
	private double rValue = 0.7;

	// Constants for borders around the plot
	private static final int TOP_NAME_HEIGHT = 30;
	private static final int BOTTOM_NAME_HEIGHT = 10;
	private static final int LEFT_BORDER = 10;
	private static final int RIGHT_BORDER = 10;
	private static final int NAME_SPACE = 50;

	// Some parameters used when we're making a selection
	private boolean makingSelection = false;
	private int selectionStartY = 0;
	private int selectionEndY = 0;
	
	// The gradient we're using
	private ColourGradient gradient;
	
	// This is a flag to say whether the gradient we're using is going to be negative
	// as well as positive
	private boolean negativeScale = false;


	public HierarchicalClusterPanel (ProbeList probes, DataStore [] stores, ClusterPair clusters, boolean normalise, ColourGradient gradient) {
		this.probeList = probes;
		this.stores = stores;
		this.clusters = clusters;
		this.probes = probeList.getAllProbes();
		this.normalise = normalise;
		this.gradient = gradient;
		
		if (normalise) {
			negativeScale = true;
		}
		else if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE_AND_NEGATIVE) {
			negativeScale = true;
		}
		
		clusterPostions = new int [this.probes.length];
		Integer [] clusterOrder = clusters.getAllIndices();
		for (int i=0;i<clusterOrder.length;i++) {
			clusterPostions[clusterOrder[i]] = i;
		}

		skippablePositions = new boolean[this.probes.length];

		currentYStartIndex = 0;
		currentYEndIndex = this.probes.length-1;

		addMouseListener(this);
		addMouseMotionListener(this);

		calculateSkippablePositions();
		setClusterRValue(rValue);

	}
	
	public void setGradient (ColourGradient gradient) {
		this.gradient = gradient;
		repaint();
	}

	private int getYForPosition (int indexPosition) {

		double plotHeight = getHeight()-(TOP_NAME_HEIGHT+BOTTOM_NAME_HEIGHT);
		double proportion = ((indexPosition-currentYStartIndex))/(double)((currentYEndIndex-currentYStartIndex)+1);

		return (int)(TOP_NAME_HEIGHT + (plotHeight-(plotHeight*proportion)));
	}

	public void setMaxValue (double value) {
		this.maxValue = value;
		repaint();
	}
	
	public ProbeList saveClusters (int minClusterSize) {
		
		ProbeList allClusterList = new ProbeList(probeList, "Hierarchical Clusters", "Hierarchical Clusters with R > "+rValue, null);
				
		// Now we need to work our way through the connected clusters
		// to make the appropriate sub-lists
		
		ClusterPair [] connectedClusters = clusters.getConnectedClusters((float)rValue);
		
		int runningCount = 0;
		for (int subListIndex = 0;subListIndex < connectedClusters.length;subListIndex++) {

			Integer [] indices = connectedClusters[subListIndex].getAllIndices();

//			System.err.println("Looking at index "+subListIndex+" which has "+indices.length+" probes");
			runningCount += indices.length;

			// If we're starting before the currently visible set then we can skip
			if (runningCount-indices.length < currentYStartIndex) {
//				System.err.println("Skipping index "+(runningCount-indices.length)+" which is less than "+currentYStartIndex);
				continue;
			}
			
			
			// If we're after the end of the current visible set we can stop all together
			if (runningCount > (currentYEndIndex+1)) {
//				System.err.println("Stopping because "+runningCount+" is bigger than "+currentYEndIndex);
				break;
			}
			

			// We can immediately discard any lists which start off smaller than our limit.
			// We may get rid of the list later if there are duplicates in it.
			if (indices.length < minClusterSize) continue;
			
			Probe [] theseProbes = new Probe [indices.length];
			
			for (int i=0;i<theseProbes.length;i++) {
				theseProbes[i] = probes[indices[i]];
			}
			
			// We used to sort and deduplicate these clusters so that we could be sure
			// the number of probes is correct.  We're now going to leave this alone
			// (probe lists deduplicate internally) and annotate on the index rather 
			// than the r-value of the specific cluster.
			
			ProbeList thisList = new ProbeList(allClusterList, "Cluster "+(subListIndex+1), "HiC cluster list number "+(subListIndex+1), "Index");
			
			thisList.addProbe(theseProbes[0], (float)0);
			allClusterList.addProbe(theseProbes[0], null);
			
			for (int i=1;i<theseProbes.length;i++) {
				if (theseProbes[i] == theseProbes[i-1]) continue;
				thisList.addProbe(theseProbes[i], (float)i);
				allClusterList.addProbe(theseProbes[i],null);
			}
		}
		
		return allClusterList;
				
	}

	public void setClusterRValue (double r) {
		
		this.rValue = r;
		
		ClusterPair [] connectedClusters = clusters.getConnectedClusters((float)r);

		int [] newClusterIntervals = new int [connectedClusters.length];

		for (int i=0;i<connectedClusters.length;i++) {
			newClusterIntervals[i] = connectedClusters[i].getAllIndices().length;
		}

		clusterIntervals = newClusterIntervals;

		repaint();
	}

	private int getPositionForY (int y) {

		if (y<=getYForPosition(currentYEndIndex)) return currentYEndIndex;
		if (y>=getYForPosition(currentYStartIndex)) return currentYStartIndex;

		double plotHeight = getHeight()-(TOP_NAME_HEIGHT+BOTTOM_NAME_HEIGHT);

		double proportion = 1-((y-TOP_NAME_HEIGHT)/plotHeight);

		return currentYStartIndex+(int)(((currentYEndIndex-currentYStartIndex)+1)*proportion);
	}

	private int getXForPosition (int indexPosition) {

		int plotWidth = getWidth()-(LEFT_BORDER+RIGHT_BORDER+NAME_SPACE);
		double proportion = (((double)indexPosition))/(double)(stores.length);

		return LEFT_BORDER+(int)(plotWidth*proportion);
	}

	private synchronized void calculateSkippablePositions () {
		if (lastHeight == getHeight()) return;

		for (int i=0;i<currentYStartIndex;i++) {
			skippablePositions[i] = true; // We can skip rows we can't see
		}
		int lastEnd = getYForPosition(-1);
		for (int i=currentYStartIndex;i<=currentYEndIndex;i++) {
			int thisEnd = getYForPosition(i);
			if (thisEnd >= lastEnd){
				skippablePositions[i] = true;
			}
			else {
				skippablePositions[i] = false;
				lastEnd = thisEnd;
			}
		}
		for (int i=currentYEndIndex+1;i<skippablePositions.length;i++) {
			skippablePositions[i] = true;
		}

		lastHeight = getHeight();		
	}

	public void paint (Graphics g) {
		super.paint(g);

		if (getHeight() != lastHeight) {
			calculateSkippablePositions();
		}

		
		// Work out how big a font we can use
		Font nameFont = new Font("sans",Font.PLAIN,10);
		Font originalFont = g.getFont();
					
		
		for (int p=0;p<probes.length;p++) {
			int position = clusterPostions[p];
			if (skippablePositions[position]) continue;

			// Retrieve and normalise the raw values
			float [] theseValues = new float[stores.length];
			for (int d=0;d<stores.length;d++) {
				try {
					theseValues[d] = stores[d].getValueForProbe(probes[p]);
				}
				catch (SeqMonkException e) {}
			}
			
			if (normalise) {
				float median = SimpleStats.mean(theseValues);
				for (int d=0;d<theseValues.length;d++) {
					theseValues[d] -= median;
				}
			}

			
			int startY = getYForPosition(position);
			int endY = getYForPosition(position+1);
			int yHeight = (startY-endY)+1;
			if (yHeight<1) yHeight = 1;
						
			// Draw the probe name
			String probeName = probes[p].name();
			
			g.setFont(nameFont);
			g.setColor(Color.BLACK);
			g.drawString(probeName, getXForPosition(stores.length)+1, startY - (yHeight/2));
			g.setFont(originalFont);			
			
			for (int d=0;d<stores.length;d++) {
				int startX = getXForPosition(d);
				int endX = getXForPosition(d+1);

				if (Float.isNaN(theseValues[d])) continue;
				
				if (negativeScale) {
					g.setColor(gradient.getColor(theseValues[d], 0-maxValue, maxValue));
				}
				else {
					g.setColor(gradient.getColor(theseValues[d], 0, maxValue));
				}

				g.fillRect(startX, endY, endX-startX, yHeight);

			}
			g.setColor(Color.DARK_GRAY);

		}

		// Draw the sample names at the top

		// Work out what width we have to work with
		int nameWidth = getXForPosition(1)-getXForPosition(0);
		g.setColor(Color.DARK_GRAY);

		for (int d=0;d<stores.length;d++) {

			// Find the longest version of the name which fits within the available width
			String thisName = stores[d].name();

			if (g.getFontMetrics().stringWidth(thisName) < nameWidth) {
				int startX = getXForPosition(d);
				startX += (nameWidth-g.getFontMetrics().stringWidth(thisName))/2;
				g.drawString(thisName, startX, TOP_NAME_HEIGHT-3);
			}
			else {
				
				// It's possible that we won't even have space to draw ".." for the
				// name so we need to at least check that there's some name left to
				// be able to shorten.
				while (g.getFontMetrics().stringWidth(thisName+"..") > nameWidth && thisName.length() > 0) {
					thisName = thisName.substring(0,thisName.length()-1);
				}	

				g.drawString(thisName+"..", getXForPosition(d), TOP_NAME_HEIGHT-3);
			}
		}

		// Draw lines on the cluster boundaries for the current R-value limit
		// Draw Cluster Lines on X axis
		int runningListPosition = 0;
		g.setColor(Color.BLACK);

		for (int l=0;l<clusterIntervals.length;l++) {

			runningListPosition += clusterIntervals[l];

			if (runningListPosition < currentYStartIndex) continue;
			if (runningListPosition > currentYEndIndex) break;

			g.drawLine(1, getYForPosition(runningListPosition), (getWidth()-NAME_SPACE), getYForPosition(runningListPosition));
		}



		// Draw a box if we're making a selection
		if (makingSelection) {
			g.setColor(Color.GREEN);
			g.drawRect(getXForPosition(0), Math.min(selectionEndY, selectionStartY), getXForPosition(stores.length)-getXForPosition(0), Math.abs(selectionStartY-selectionEndY));
		}


	}

	public void mouseDragged(MouseEvent me) {

		if (makingSelection) {

			selectionEndY = me.getY();
			if (selectionEndY < TOP_NAME_HEIGHT) selectionEndY = TOP_NAME_HEIGHT;
			if (selectionEndY > getHeight()-BOTTOM_NAME_HEIGHT) selectionEndY = getHeight()-BOTTOM_NAME_HEIGHT;

			repaint();
		}		
	}

	public void mouseMoved(MouseEvent me) {
		
		int index = getPositionForY(me.getY());

		int probeIndex = -1;
		
		for (int i=0;i<clusterPostions.length;i++) {
			if (clusterPostions[i] == index) {
				probeIndex = i;
				break;
			}
		}
		
		setToolTipText(probes[probeIndex].name());

		
	}

	public void mouseClicked(MouseEvent me) {
		if (me.getClickCount() == 2) {
			
			int index = getPositionForY(me.getY());

			int probeIndex = -1;
			
			for (int i=0;i<clusterPostions.length;i++) {
				if (clusterPostions[i] == index) {
					probeIndex = i;
					break;
				}
			}
			
			DisplayPreferences.getInstance().setLocation(probes[probeIndex].chromosome(), probes[probeIndex].packedPosition());
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

		selectionStartY = me.getY();
		if (selectionStartY < TOP_NAME_HEIGHT) selectionStartY = TOP_NAME_HEIGHT;
		if (selectionStartY > getHeight()-BOTTOM_NAME_HEIGHT) selectionStartY = getHeight()-BOTTOM_NAME_HEIGHT;

		selectionEndY = selectionStartY;

	}

	public void mouseReleased(MouseEvent me) {
		lastHeight = 0;

		// If they're right-clicking then we zoom out
		if ((me.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK) {

			int yMid = currentYStartIndex+((currentYEndIndex-currentYStartIndex)/2);
			currentYStartIndex = yMid-(currentYEndIndex-currentYStartIndex);
			currentYEndIndex = yMid+(currentYEndIndex-currentYStartIndex);
			if (currentYStartIndex < 0) currentYStartIndex = 0;
			if (currentYEndIndex > probes.length-1) currentYEndIndex = probes.length-1;

		}
		else {

			// Don't allow really small selections
			if (Math.abs(selectionEndY-selectionStartY)<3) {
				makingSelection = false;
				repaint();				
				return;
			}

			int newYStart = getPositionForY(Math.max(selectionEndY, selectionStartY));
			int newYEnd = getPositionForY(Math.min(selectionEndY, selectionStartY));
			if (newYStart<0) newYStart = 0;
			if (newYEnd > probes.length-1) newYEnd = probes.length-1;

			// Don't allow a view smaller than 1 index
			if (newYEnd != newYStart) {
				currentYStartIndex = newYStart;
				currentYEndIndex = newYEnd;
			}

		}

		makingSelection = false;

		// Reset the drawing optimisations
		lastHeight = 0;

		repaint();
	}

}
