/**
 * Copyright 2009-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.BeanPlot;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.JPanel;

/**
 * A display panel which shows a single boxwhisker plot.  Can be combined 
 * into a MultiBoxWhiskerPlot to allow multiple boxwhisker plots to be put
 * on the same scale
 */
public class BeanPlotPanel extends JPanel {

	private float [] centres;
	private float [] counts;
	
	/** The min value. */
	private double minValue;
	
	/** The max value. */
	private double maxValue;
	
	/** The colour to fill the bean with **/
	private Color fillColour;
	
	/**
	 * Instantiates a new box whisker panel.
	 * 
	 * @param bw The pre-calculated BoxWhisker result to display
	 */
	public BeanPlotPanel (float [] data, Color fillColor) {
		this(data,minArray(data),maxArray(data), fillColor);
	}
	
	private static float minArray (float [] data) {
		if (data.length == 0) return 0;
		float minValue = data[0];
		for (int i=1;i<data.length;i++) {
			if (data[i]<minValue) minValue = data[i];
		}
		return minValue;
	}

	private static float maxArray (float [] data) {
		if (data.length == 0) return 0;
		float maxValue = data[0];
		for (int i=1;i<data.length;i++) {
			if (data[i]>maxValue) maxValue = data[i];
		}
		return maxValue;
	}

	
	
	/**
	 * Instantiates a new box whisker panel with a min/max value
	 * which is different to the intrinsic min/max of the 
	 * result file being shown.
	 * 
	 * @param bw The pre-calculated BoxWhisker result to display.
	 * @param minValue the min value
	 * @param maxValue the max value
	 */
	public BeanPlotPanel (float [] data, float minValue, float maxValue, Color fillColour) {
		
		this.fillColour = fillColour;
		
		// We'll have to figure out a better way of optimising the bandwidth we're
		// going to use, but for now we'll take 1/20th of the range
		
		Arrays.sort(data);
		
		float bandwidth = (maxValue-minValue)/20;

//		System.err.println("Bandwidth is "+bandwidth+" from min="+minValue+" max="+maxValue);
		
		// We'll divide the range into 200 bins and calculate which bins each
		// data point falls into
		
		int numberOfBins = 200;
		
		float [] binMins = new float[numberOfBins];
		float [] binMaxs = new float[numberOfBins];
		
		centres = new float[numberOfBins];
		counts = new float[numberOfBins];
		
		float binWidth = (maxValue-minValue)/(numberOfBins-1);
		
//		System.err.println("Bin Width is "+binWidth);
		
		for (int i=0;i<centres.length;i++) {
			centres[i] = minValue+(binWidth*i);
			binMins[i] = centres[i]-(bandwidth/2);
			binMaxs[i] = centres[i]+(bandwidth/2);
			
//			System.err.println("Bin "+i+" goes from "+binMins[i]+" to "+binMaxs[i]);
			
		}
		
		// Now we can go through the data adding the counts to the appropriate bins
		
		int firstBinIndex = 0;
		
		DATA: for (int d=0;d<data.length;d++) {
			
			if (Float.isNaN(data[d])) continue;
			
			for (int bin=firstBinIndex;bin<centres.length;bin++) {
				if (data[d] > binMaxs[bin]) {
					++firstBinIndex; // We're never going to find anything in this bin again
					continue;
				}
				
				// We've hit our first matching bin so now we keep iterating until we
				// run out of matching bins
				for (int bin2=bin;bin2<centres.length;bin2++) {
					if (binMins[bin2] > data[d]) continue DATA;
					++counts[bin2];
				}
				continue DATA;
			}
		}
		
//		for (int i=0;i<counts.length;i++) {
//			System.err.println("Bin "+i+" had a count of "+counts[i]);
//		}
		
		
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint (Graphics g) {
		super.paint(g);
				
		// Make the background white
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// We need to make up the polygon for the beanplot.  To do this
		// we'll need to go up and down the data
		
		int [] xPoints = new int [centres.length*2];
		int [] yPoints = new int [centres.length*2];
		
		float maxCount = maxArray(counts);
		
		for (int i=0;i<centres.length;i++) {
			yPoints[i] = getY(centres[i]);
			xPoints[i] = getLowerX(counts[i], maxCount);
//			System.err.println("i="+i+" y="+yPoints[i]+" count="+counts[i]+" x="+xPoints[i]);
		}
		
		for (int i=centres.length-1;i>=0;i--) {
			yPoints[yPoints.length-(i+1)] = getY(centres[i]);
			xPoints[yPoints.length-(i+1)] = getUpperX(counts[i], maxCount);
		}
		
		g.setColor(fillColour);
		g.fillPolygon(xPoints, yPoints, xPoints.length);		

		g.setColor(Color.BLACK);
		g.drawPolygon(xPoints, yPoints, xPoints.length);		

		
	}
		
	/**
	 * Gets the y.
	 * 
	 * @param value the value
	 * @return the y
	 */
	private int getY (double value) {
		return 5 + ((getHeight()-10) - ((int)((getHeight()-10)*((value-minValue)/(maxValue-minValue)))));
	}

	private int getLowerX (double value, double max) {
		int totalWidth = getWidth()-6;
		int halfWidth = totalWidth/2;
		
		double proportion = value/max;
		return (getWidth()/2)-(int)(halfWidth*proportion);
	}

	private int getUpperX (double value, double max) {
		int totalWidth = getWidth()-6;
		int halfWidth = totalWidth/2;
		
		double proportion = value/max;
		return getWidth()/2+(int)(halfWidth*proportion);
	}


}
