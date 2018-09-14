/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class HistogramPanel displays an interactive histogram from
 * any linear set of data.
 */
public class PCAHistogramPanel extends JPanel {

	/** The Constant df. */
//	private static final DecimalFormat df = new DecimalFormat("#.##");

	/** The min data value. */
	private double currentMinValue = 1;

	/** The max data value. */
	private double currentMaxValue = 1;
	
	private double cutoffValue = 0;

	private static final int Y_AXIS_SPACE = 50;
	private static final int X_AXIS_SPACE = 30;


	/** The categories. */
	private HistogramCategory [] categories = new HistogramCategory[0];

	private int maxCount;

	/**
	 * Instantiates a new histogram panel.
	 * 
	 * @param data the data
	 */
	public PCAHistogramPanel (float [] data) {
		setData(data);
	}
	
	public void setCutoff (double value) {
		this.cutoffValue = value;
		repaint();
	}
	
	public void setData (float [] data) {
		currentMinValue = data[0];
		currentMaxValue = data[0];
		
		for (int i=1;i<data.length;i++) {
//			System.err.println("Value is "+data[i]);
			if (data[i] > currentMaxValue) currentMaxValue = data[i];
			if (data[i] < currentMinValue) currentMinValue = data[i];
		}
		
		// Our categories go between -1 and 1 and we have 50 of them
		
		categories = new HistogramCategory[50];
		
		double interval = (currentMaxValue-currentMinValue)/50;
		
		for (int i=0;i<categories.length;i++) {
			categories[i] = new HistogramCategory((i*interval)+currentMinValue, ((i+1)*interval)+currentMinValue);
		
			// TODO: Check boundaries
			for (int d=0;d<data.length;d++) {
				if (categories[i].minValue <= data[d] && categories[i].maxValue >= data[d]) {
					categories[i].count++;
				}
			}
			
			if (categories[i].count > maxCount) maxCount = categories[i].count;
			
//			System.err.println("Category count for "+i+" is "+categories[i].count+" from "+categories[i].minValue+" to "+categories[i].maxValue);
		}
		
		repaint();
	}

	public void paintComponent (Graphics g) {
		super.paintComponent(g);

		// We want a white background
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		// Draw the graph axes first.  We leave a border on all sides
		g.setColor(Color.BLACK);

		g.drawLine(Y_AXIS_SPACE, 5, Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE);
		g.drawLine(Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE, getWidth()-5, getHeight()-X_AXIS_SPACE);

		// If we don't have any data we can stop here
		if (categories == null) return;

		// If we have a stupid scale we can also stop here
		if (Double.isInfinite(currentMaxValue) || Double.isNaN(currentMaxValue) || Double.isInfinite(currentMinValue) || Double.isNaN(currentMinValue)) {
			System.err.println("Had infinite or NaN ends to the scale - not going to try to draw that");
			return;
		}


		// We need the scaling factor for the y-axis
		double yScale = 0;

		yScale = (double)(getHeight()-(5+X_AXIS_SPACE))/maxCount;
		

		// Now draw the scale on the y axis
		AxisScale yAxisScale = new AxisScale(0, maxCount);

		double currentYValue = yAxisScale.getStartingValue();

		while (currentYValue < maxCount) {

			double yHeight = currentYValue*yScale;
			
			String yLabel = yAxisScale.format(currentYValue);

			g.drawString(yLabel, Y_AXIS_SPACE - (5+g.getFontMetrics().stringWidth(yLabel)), (int)((getHeight()-X_AXIS_SPACE)-yHeight)+(g.getFontMetrics().getAscent()/2));

			// Put a line across the plot
			if (currentYValue != 0) {
				g.setColor(Color.LIGHT_GRAY);
				g.drawLine(Y_AXIS_SPACE, (int)((getHeight()-X_AXIS_SPACE)-yHeight), getWidth()-5, (int)((getHeight()-X_AXIS_SPACE)-yHeight));
				g.setColor(Color.BLACK);
			}

			currentYValue += yAxisScale.getInterval();
		}

		// Now draw the scale on the x axis
		if (categories.length>0) {
			AxisScale xAxisScale = new AxisScale(currentMinValue, currentMaxValue);

			double currentXValue = xAxisScale.getStartingValue();
			while (currentXValue < currentMaxValue) {
				g.drawString(xAxisScale.format(currentXValue), getX(currentXValue), (int)((getHeight()-X_AXIS_SPACE)+15));

				currentXValue += xAxisScale.getInterval();

			}
		}



		// Now we can draw the different categories
		for (int c=0;c<categories.length;c++) {
			categories[c].xStart = getX(categories[c].minValue);;
			categories[c].xEnd = getX(categories[c].maxValue);

			g.setColor(ColourScheme.HISTOGRAM_BAR);

			double ySize = categories[c].count*yScale;
			
			g.fillRect(categories[c].xStart, (int)((getHeight()-X_AXIS_SPACE)-ySize), categories[c].xEnd-categories[c].xStart, (int)(ySize));


			// Draw a box around it
			g.setColor(Color.BLACK);
			g.drawRect(categories[c].xStart, (int)((getHeight()-X_AXIS_SPACE)-ySize), categories[c].xEnd-categories[c].xStart, (int)(ySize));

		}
		
		// Draw the line for the cutoff value
		g.setColor(Color.RED);
		g.drawLine(getX(cutoffValue), 5, getX(cutoffValue), getHeight()-X_AXIS_SPACE);

		

	}

	public int getX (double xValue) {
		return Y_AXIS_SPACE + (int)(((getWidth()-(Y_AXIS_SPACE+5))/(currentMaxValue-currentMinValue))*(xValue-currentMinValue));
	}

	public double getXValue (int xPosition) {			
		return ((((double)(xPosition-Y_AXIS_SPACE))/(getWidth()-(Y_AXIS_SPACE+5)))*(currentMaxValue-currentMinValue))+currentMinValue;			
	}






	/**
	 * The Class HistogramCategory.
	 */
	private class HistogramCategory {

		/** The min value. */
		public double minValue;

		/** The max value. */
		public double maxValue;

		/** The x start. */
		public int xStart = 0;

		/** The x end. */
		public int xEnd = 0;

		/** The count. */
		public int count;

		/**
		 * Instantiates a new histogram category.
		 * 
		 * @param minValue the min value
		 * @param maxValue the max value
		 */
		public HistogramCategory (double minValue, double maxValue) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			count = 0;
		}
	}


}
