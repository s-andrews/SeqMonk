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
package uk.ac.babraham.SeqMonk.Displays.LineGraph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class LineGraphPanel extends JPanel implements Runnable, MouseListener {

	private DataStore [] stores;
	private Probe [] probes;
	private ProbeList list;
	
	private Probe selectedProbe = null;
	private int selectedX = 0;
	private int selectedY = 0;
	
	private Color BLUE = new Color(0,0,200);
	
	// These set the limits, either globally, or if we're zoomed in
	// along with a flag to say when they've been calculated
	private double usedMin;
	private double usedMax;
	private boolean calculated = false;
	
	// These are cached values for the overall trends (mean and confidence interval)
	private double [] means;
	private double [] confInts;
	
	private boolean normalise = false;
	private boolean summarise = false;
	
	/** A reusable data store for temporary calculation of probe values **/
	private float [] values;
	private float max;
	private float min;
	
	// Spacing for the drawn panel
	private static final int X_AXIS_SPACE = 50;
	private static final int Y_AXIS_SPACE = 30;

	
	
	public LineGraphPanel (DataStore [] stores, ProbeList probes) throws SeqMonkException {
		this.probes = probes.getAllProbes();
		this.list = probes;
		
		addMouseListener(this);

		Vector<DataStore> quantitatedStores = new Vector<DataStore>();
		
		for (int s=0;s<stores.length;s++) {
			if (stores[s].isQuantitated()) {
				quantitatedStores.add(stores[s]);
			}
		}
		
		this.stores = quantitatedStores.toArray(new DataStore[0]);
		
		means = new double[stores.length];
		confInts = new double[stores.length];
		
		if (this.stores.length < 2) {
			throw new SeqMonkException("At least 2 quantitated data stores are required to draw a line graph");
		}
		
		values = new float[this.stores.length];
		
		Thread t = new Thread(this);
		t.start();
		
	}
	
	public void setNormalise (boolean normalise) {
		if (this.normalise != normalise) {
			this.normalise = normalise;
			calculated = false;
			Thread t = new Thread(this);
			t.start();
		}
	}
	
	public void setSummarise (boolean summarise) {
		if (this.summarise != summarise) {
			this.summarise = summarise;
			repaint();
		}
	}
	
	public void run() {
		
		// Filter the starting list of probes to exclude any which have NaN values in them
		boolean [] keepMe = new boolean[probes.length];
		int keepCount = 0;
		for (int p=0;p<probes.length;p++) {
			keepMe[p] = true;
			for (int s=0;s<stores.length;s++) {
				try {
					float value = stores[s].getValueForProbe(probes[p]);
					if (Float.isNaN(value) || Float.isInfinite(value)) {
						keepMe[p] = false;
					}
				}
				catch (SeqMonkException sme) {}
			}
			if (keepMe[p]) ++keepCount;
		}
		
		if (keepCount != probes.length) {
			// We need to truncate probes
			Probe [] keepers = new Probe[keepCount];
			
			keepCount--; // Convert length to index
			for (int p=0;p<probes.length;p++) {
				if (keepMe[p]) {
					keepers[keepCount] = probes[p];
					keepCount--;
				}
			}
			
			probes = keepers;
			
		}
		
		// Sort the probes based on their expression in the first store
		Arrays.sort(probes,new ProbeSorter());

		// We go through all of the values in all of the samples
		// to work out the global max and min
		
		boolean somethingSet = false;
		
		double globalMin = 0;
		double globalMax = 0;
		
		
		for (int p=0;p<probes.length;p++) {
			getValues(probes[p]);
			for (int s=0;s<values.length;s++) {
				if (! somethingSet) {
					globalMax = values[s];
					globalMin = globalMax;
					somethingSet = true;
				}
					
				if (values[s]<globalMin) globalMin = values[s];
				if (values[s]>globalMax) globalMax = values[s];
			}			
		}
		
		// We now do a second pass through the data to calculate the trend data
		// We do it this way so we don't have to hold all of the values for all
		// positions in memory at once.  This is slower but not quite as memory
		// intensive.
		
		double [] conditionValues = new double[probes.length];
		for (int s=0;s<stores.length;s++) {
			for (int p=0;p<probes.length;p++) {
				getValues(probes[p]);
				conditionValues[p] = values[s];
			}
			means[s] = SimpleStats.mean(conditionValues);
			confInts[s] = 1.96d*(SimpleStats.stdev(conditionValues, means[s])/Math.sqrt(probes.length));
			
			if (means[s]-confInts[s] < globalMin) globalMin = means[s]-confInts[s];
			if (means[s]+confInts[s] > globalMax) globalMax = means[s]+confInts[s];
			
		}
		
		
		usedMax = globalMax;
		usedMin = globalMin;
		calculated = true;
		repaint();
		
	}
	
	private void getValues (Probe p) {
		
		for (int s=0;s<stores.length;s++) {
			try {
				values[s] = stores[s].getValueForProbe(p);
				if (! normalise) {
					if (s == 0 || values[s]>max) max = values[s];
					if (s == 0 || values[s]<min) min = values[s];
				}
			}
			catch (SeqMonkException e) {
				values[s] = 0;
			}
		}
		
		if (normalise) {
			
			float median = SimpleStats.mean(values);
			for (int d=0;d<values.length;d++) {
				values[d] -= median;
				if (values[d]>max) max=values[d];
				if (values[d]<min) min=values[d];
			}
		}
	}
	
	private int getYPixels (double value) {
		return (getHeight()-Y_AXIS_SPACE) - (int)((getHeight()-(10d+Y_AXIS_SPACE))*((value-usedMin)/(usedMax-usedMin)));
	}

	private int getXPixels(int pos) {
		return X_AXIS_SPACE + (pos*((getWidth()-(10+X_AXIS_SPACE))/(stores.length-1)));
	}

	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		if (!calculated) return;
		
		g.setColor(Color.BLACK);
		
		// Draw the axes
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-10, getHeight()-Y_AXIS_SPACE); // X-axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, X_AXIS_SPACE, 10); // Y-axis
		
		// Draw the Y scale
		AxisScale yAxisScale = new AxisScale(usedMin, usedMax);
		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < usedMax) {
			g.drawString(yAxisScale.format(currentYValue), 5, getYPixels(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(X_AXIS_SPACE,getYPixels(currentYValue),X_AXIS_SPACE-3,getYPixels(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}
		
		// Draw x ticks
		for (int s=0;s<stores.length;s++) {
			int xTick = X_AXIS_SPACE + (s*((getWidth()-(10+X_AXIS_SPACE))/(stores.length-1)));
			g.drawLine(xTick, getHeight()-Y_AXIS_SPACE, xTick, getHeight()-(Y_AXIS_SPACE-3));
		}
		
		for (int s=0;s<stores.length;s++) {
			int xTick = X_AXIS_SPACE + (s*((getWidth()-(10+X_AXIS_SPACE))/(stores.length-1)));
			
			int stringWidth = g.getFontMetrics().stringWidth(stores[s].name());
			int xStart = xTick - (stringWidth/2);
			
			if (xStart < 1) xStart = 1;
			if (xStart + stringWidth > (getWidth()-1)) xStart = (getWidth()-1)-stringWidth;
			
			g.drawString(stores[s].name(), xStart, getHeight()-(Y_AXIS_SPACE-(g.getFontMetrics().getHeight()+3)));
		}
		
		// Put the probe list name at the top
		String listName = list.name()+" ("+probes.length+" probes)";
		g.drawString(listName, (getWidth()-10)-(g.getFontMetrics().stringWidth(listName)), 10);
		
		// Now draw the probes

		int lastX = 0;
		int lastY = 0;
		int thisX = 0;
		int thisY = 0;
		double value = 0;

		if (summarise) {
			
			int upperY;
			int lowerY;
			
			g.setColor(BLUE);
			for (int s=0;s<means.length;s++) {
				thisX = getXPixels(s);
				value = means[s]; 

				if (value < usedMin) value = usedMin;
				if (value > usedMax) value = usedMax;
				thisY = getYPixels(value);

				if (s>0) {
					g.drawLine(lastX, lastY, thisX, thisY);
				}
				
				upperY = getYPixels(means[s]+confInts[s]);
				lowerY = getYPixels(means[s]-confInts[s]);
				
				g.drawLine(thisX, lowerY, thisX, upperY); // Line between upper and lower CIs
				g.drawLine(thisX-2,lowerY,thisX+2,lowerY); // Bar across lower CI
				g.drawLine(thisX-2,upperY,thisX+2,upperY); // Bar across upper CI
				
				lastX = thisX;
				lastY = thisY;
			}
		}
		else {
			for (int p=0;p<probes.length;p++) {
				getValues(probes[p]);
				g.setColor(DisplayPreferences.getInstance().getGradient().getColor(values[0], usedMin, usedMax));
				for (int s=0;s<values.length;s++) {
					thisX = X_AXIS_SPACE + (s*((getWidth()-(10+X_AXIS_SPACE))/(stores.length-1)));
					value = values[s]; 
	
					if (value < usedMin) value = usedMin;
					if (value > usedMax) value = usedMax;
					thisY = getYPixels(value);
					
					if (s>0) {
						g.drawLine(lastX, lastY, thisX, thisY);
					}
					lastX = thisX;
					lastY = thisY;
				}
			}
			
			if (selectedProbe != null) {
				getValues(selectedProbe);
				g.setColor(Color.BLACK);
				for (int s=0;s<values.length;s++) {
					thisX = X_AXIS_SPACE + (s*((getWidth()-(10+X_AXIS_SPACE))/(stores.length-1)));
					value = values[s]; 
	
					if (value < usedMin) value = usedMin;
					if (value > usedMax) value = usedMax;
					thisY = getYPixels(value);
					
					if (s>0) {
						g.drawLine(lastX, lastY, thisX, thisY);
					}
					lastX = thisX;
					lastY = thisY;
				}
				g.drawString(selectedProbe.name(), selectedX, selectedY);
			}
		}
		
	}
	

	private class ProbeSorter implements Comparator<Probe> {

		public int compare(Probe p1, Probe p2) {
			try {
				
				float mean1 = 0;
				for (int s=0;s<stores.length;s++) {
					mean1 += stores[s].getValueForProbe(p1);
				}
				mean1 /=stores.length;
				float mean2=0;
				for (int s=0;s<stores.length;s++) {
					mean2 += stores[s].getValueForProbe(p2);
				}
				mean2 /= stores.length;
								
				return Float.compare(mean1,mean2);
			}
			catch (SeqMonkException e) {
				return 0;
			}
		}
		
	}

	public void mouseClicked(MouseEvent me) {

		// We need to find the line we're closest to.
		int x = me.getX();
		int y = me.getY();
		
		// Don't do anything if we're before the first store
		// or after the last one
		if (x < X_AXIS_SPACE || x > getWidth() -10) return;
		
		// We first need to find the points we're between and
		// the proportion we are between the points
		
		int previousStoreIndex = 0;
		int nextStoreIndex = 0;
		float storeProportion = 0;
		
		for (int s=1;s<stores.length;s++) {
			int thisX = getXPixels(s);
			if (thisX >= x) {
				previousStoreIndex = s-1;
				nextStoreIndex = s;
				storeProportion = (x-getXPixels(s-1))/(float)(getXPixels(s)-getXPixels(s-1));
				break;
			}
		}
		
		// Now we need to go through the probes finding the point which is closest to the 
		// y value we have
		
		int closestDistance = getHeight();
		Probe closestProbe = null;
		
		for (int p=0;p<probes.length;p++) {
			// Get the values for the previous and next stores
			
			getValues(probes[p]);
			float previousValue = values[previousStoreIndex];
			float nextValue = values[nextStoreIndex];
			
			// Get the proportionally adjusted value
			float adjustedValue = previousValue + ((nextValue-previousValue)*storeProportion);
			int adjustedY = getYPixels(adjustedValue);
			
			int yDiff = Math.abs(adjustedY-y);
			
			if (yDiff < closestDistance && yDiff < 5) {
				closestDistance = yDiff;
				closestProbe = probes[p];
				if (yDiff == 0) break; // We're not getting any closer than that.
			}
		}
		
		selectedProbe = closestProbe;
		selectedX = x;
		selectedY = y;
		repaint();
		
		if (me.getClickCount() == 2 && selectedProbe != null) {
			// We take them to the location of that probe
			DisplayPreferences.getInstance().setLocation(selectedProbe.chromosome(),SequenceRead.packPosition(selectedProbe.start(),selectedProbe.end(),Location.UNKNOWN));
		}
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		if (selectedProbe != null) {
			selectedProbe = null;	
			repaint();
		}
	}
}
