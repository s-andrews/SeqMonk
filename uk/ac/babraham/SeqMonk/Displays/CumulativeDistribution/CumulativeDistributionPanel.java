/**
 * Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.CumulativeDistribution;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class CumulativeDistributionPanel extends JPanel {

	private String title;
	private String [] names;
	private float [][] distributions;
		
	// These set the limits, either globally, or if we're zoomed in
	// along with a flag to say when they've been calculated
	
	private double absoluteMin;
	private double absoluteMax;
	private double usedMin;
	private double usedMax;	
	
	
	// Spacing for the drawn panel
	private static final int X_AXIS_SPACE = 50;
	private static final int Y_AXIS_SPACE = 30;

	
	public CumulativeDistributionPanel (DataStore store, ProbeList [] lists) throws SeqMonkException {

		if (!store.isQuantitated()) {
			throw new SeqMonkException("Data store wasn't quantitated");
		}
		
		title = store.name();
		
		names = new String [lists.length];
		distributions = new float[names.length][];
		boolean setSomething = false;
		for (int i=0;i<names.length;i++) {
			names[i] = lists[i].name();
			Probe [] probes = lists[i].getAllProbes();
			float [] thisDistribution = new float[probes.length];
			for (int p=0;p<probes.length;p++) {
								
				thisDistribution[p] = store.getValueForProbe(probes[p]);

				if (Float.isInfinite(thisDistribution[p]) || Float.isNaN(thisDistribution[p])) continue;
				
				if (!setSomething) {
					absoluteMin = thisDistribution[p];
					absoluteMax = absoluteMin;
					setSomething = true;
				}
				if (thisDistribution[p]<absoluteMin) absoluteMin = thisDistribution[p];
				if (thisDistribution[p]>absoluteMax) absoluteMax = thisDistribution[p];

			}
			distributions[i] = shortenDistribution(thisDistribution);
		}
		
		usedMin = absoluteMin;
		usedMax = absoluteMax;
		
		if (usedMax == usedMin) usedMax = usedMin+1;
	
	}

	public CumulativeDistributionPanel (DataStore [] stores, ProbeList list) throws SeqMonkException {
		
		title = list.name();
		
		// Find the subset of quantiated stores from the original set
		Vector<DataStore> validStores = new Vector<DataStore>();
		
		for (int s=0;s<stores.length;s++) {
			if (stores[s].isQuantitated()) {
				validStores.add(stores[s]);
			}
		}
		
		DataStore [] quantiatedStores = validStores.toArray(new DataStore[0]);
		
		names = new String [quantiatedStores.length];
		Probe [] probes = list.getAllProbes();
		distributions = new float[names.length][];
		
		boolean setSomething = false;
		for (int i=0;i<names.length;i++) {
			names[i] = quantiatedStores[i].name();
			float [] thisDistribution = new float[probes.length];
			for (int p=0;p<probes.length;p++) {
				
				thisDistribution[p] = quantiatedStores[i].getValueForProbe(probes[p]);
				
				if (Float.isInfinite(thisDistribution[p]) || Float.isNaN(thisDistribution[p])) continue;
				
				if (!setSomething) {
					absoluteMin = thisDistribution[p];
					absoluteMax = absoluteMin;
					setSomething = true;
				}
				if (thisDistribution[p]<absoluteMin) absoluteMin = thisDistribution[p];
				if (thisDistribution[p]>absoluteMax) absoluteMax = thisDistribution[p];
				
			}
			
			Arrays.sort(thisDistribution);
			
			distributions[i] = shortenDistribution(thisDistribution);
		}
		
		usedMin = absoluteMin;
		usedMax = absoluteMax;
		
		if (usedMax == usedMin) usedMax = usedMin+1;
	
	}
	
	public void setScale (int percentage) {
		
		double range = absoluteMax - absoluteMin;
		
		range /= 100;
		range *= percentage;

		usedMax = usedMin + range;
		repaint();
	}
	

	
	private float [] shortenDistribution (float [] distribution) {
	
		// Firstly we need to remove any point which aren't valid.
		int nanCount = 0;
		for (int i=0;i<distribution.length;i++) {
			if (Float.isNaN(distribution[i]) || Float.isInfinite(distribution[i])) ++nanCount ;
		}

		if (nanCount > 0) {
			float [] validDistribution = new float[distribution.length-nanCount];
			
			int index = 0;
			
			for (int i=0;i<distribution.length;i++) {
				if (Float.isNaN(distribution[i]) || Float.isInfinite(distribution[i])) continue;
				validDistribution[index] = distribution[i];
				index++;
			}
			
			distribution = validDistribution;
		}
		
		Arrays.sort(distribution);
	
		// We need to return a 1000 element array
		float [] shortDistribution = new float[1000];
		
		if (distribution.length == 0) {
			// There's no valid data here so make everything zero
			for (int i=0;i<shortDistribution.length;i++) {
				shortDistribution[i] = 0;
			}
			
		}
		
		else {
			for (int i=0;i<shortDistribution.length;i++) {
				int index = (int)((distribution.length-1)*(i/1000d));
				shortDistribution[i] = distribution[index];
			}
		}
		
		return shortDistribution;
		
	}

	private int getYPixels (double value) {
		return (getHeight()-Y_AXIS_SPACE) - (int)((getHeight()-(10d+Y_AXIS_SPACE))*((value-usedMin)/(usedMax-usedMin)));
	}
	
	private int getXPixels (double value) {
		double proportion = value/100;
		
		
		return X_AXIS_SPACE+ (int)((getWidth()-(10+X_AXIS_SPACE))*proportion);
	}

	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
				
		g.setColor(Color.BLACK);
		
		// Draw the axes
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, getWidth()-10, getHeight()-Y_AXIS_SPACE); // X-axis
		g.drawLine(X_AXIS_SPACE, getHeight()-Y_AXIS_SPACE, X_AXIS_SPACE, 10); // Y-axis
		
		// Draw the Y scale
		AxisScale yAxisScale = new AxisScale(usedMin, usedMax);
		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < usedMax) {
			g.drawString(yAxisScale.format(currentYValue), 5, getYPixels(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(X_AXIS_SPACE-3, getYPixels(currentYValue), getWidth()-10, getYPixels(currentYValue));
			g.drawLine(X_AXIS_SPACE,getYPixels(currentYValue),X_AXIS_SPACE-3,getYPixels(currentYValue));
			g.setColor(Color.LIGHT_GRAY);
			g.drawLine(X_AXIS_SPACE+1, getYPixels(currentYValue), getWidth()-10, getYPixels(currentYValue));
			g.setColor(Color.BLACK);
			currentYValue += yAxisScale.getInterval();
		}
		
		// Draw x scale
		AxisScale xAxisScale = new AxisScale(0, 100);
		double currentXValue = xAxisScale.getStartingValue();
		while (currentXValue < 100) {
			g.drawString(xAxisScale.format(currentXValue), getXPixels(currentXValue), getHeight()-(Y_AXIS_SPACE-(g.getFontMetrics().getHeight()+3)));
			g.drawLine(getXPixels(currentXValue),getHeight()-Y_AXIS_SPACE,getXPixels(currentXValue),getHeight()-(Y_AXIS_SPACE-3));
			currentXValue += xAxisScale.getInterval();
		}
		// Mark the 50th percentile
		g.setColor(Color.LIGHT_GRAY);
		g.drawLine(getXPixels(50), getHeight()-Y_AXIS_SPACE, getXPixels(50), 10);
		g.setColor(Color.BLACK);
		
				
		// Put the probe list name at the top
		g.drawString(title, (getWidth()-10)-(g.getFontMetrics().stringWidth(title)), g.getFontMetrics().getHeight());
		
		// Put the names on the right
		for (int n=0;n<names.length;n++) {
			g.setColor(ColourIndexSet.getColour(n));
			g.drawString(names[n], X_AXIS_SPACE+5, g.getFontMetrics().getHeight()*(n+1));
			
		}
		
		// Now draw the lines
		for (int d=0;d<distributions.length;d++) {
		
			g.setColor(ColourIndexSet.getColour(d));
			int lastY = 0;
			int lastX = 0;
		
			double width = getWidth()-(X_AXIS_SPACE+10);
		
			for (int x=0;x<distributions[d].length;x++) {
			
				double proportion = x/(double)distributions[d].length;
				int thisY = getYPixels(distributions[d][x]);
				int thisX = X_AXIS_SPACE;
				thisX += (int)(width*proportion);
				if (x>0) {
					g.drawLine(lastX, lastY, thisX, thisY);
				}
				lastY = thisY;
				lastX = thisX;
			
			}
		}
		
	}
	

}
