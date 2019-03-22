/**
 * Copyright 2010-19 Simon Andrews
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

import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class VarianceLineGraphPanel extends JPanel {

	private float [] values;
	
	private int selectedIndex = 0;

	private Color BLUE = new Color(0,0,200);

	// These set the limits, either globally, or if we're zoomed in
	// along with a flag to say when they've been calculated
	private double usedMin;
	private double usedMax;

	// Spacing for the drawn panel
	private static final int Y_AXIS_SPACE = 50;
	private static final int X_AXIS_SPACE = 30;



	public VarianceLineGraphPanel (float [] values) {
		this.values = values;
		this.usedMax = values[0];
		this.usedMin = 0;
		
		// In case there is no variance between the samples set an artificial 
		// range so we don't trigger a crash
		if (usedMax == 0) usedMax = 1;		
	}

	public void setSelectedIndex (int index) {
		this.selectedIndex = index;
		repaint();
	}

	private int getYPixels (double value) {
		return (getHeight()-X_AXIS_SPACE) - (int)((getHeight()-(10d+X_AXIS_SPACE))*((value-usedMin)/(usedMax-usedMin)));
	}

	private int getXPixels(int pos) {
		return Y_AXIS_SPACE + (pos*((getWidth()-(10+Y_AXIS_SPACE))/(values.length-1)));
	}

	public void paint (Graphics g) {
		super.paint(g);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.BLACK);

		// In contrast to other plots we fix the Y axis space here because
		// it needs to line up with the histogram below.
		AxisScale yAxisScale = new AxisScale(usedMin, usedMax);
		
		// Draw the axes
		g.drawLine(Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE, getWidth()-10, getHeight()-X_AXIS_SPACE); // X-axis
		g.drawLine(Y_AXIS_SPACE, getHeight()-X_AXIS_SPACE, Y_AXIS_SPACE, 10); // Y-axis

		// Draw the Y scale
		double currentYValue = yAxisScale.getStartingValue();
		while (currentYValue < usedMax) {
			String yLabel = yAxisScale.format(currentYValue);
			g.drawString(yLabel, Y_AXIS_SPACE-(5+g.getFontMetrics().stringWidth(yLabel)), getYPixels(currentYValue)+(g.getFontMetrics().getAscent()/2));
			g.drawLine(Y_AXIS_SPACE,getYPixels(currentYValue),Y_AXIS_SPACE-3,getYPixels(currentYValue));
			currentYValue += yAxisScale.getInterval();
		}

		// Draw x ticks
		for (int s=0;s<values.length;s++) {
			int xTick = Y_AXIS_SPACE + (s*((getWidth()-(10+Y_AXIS_SPACE))/(values.length-1)));
			g.drawLine(xTick, getHeight()-X_AXIS_SPACE, xTick, getHeight()-(X_AXIS_SPACE-3));
		}

		for (int s=0;s<values.length;s++) {
			int xTick = Y_AXIS_SPACE + (s*((getWidth()-(10+Y_AXIS_SPACE))/(values.length-1)));

			int stringWidth = g.getFontMetrics().stringWidth("PC"+(s+1));
			int xStart = xTick - (stringWidth/2);

			if (xStart < 1) xStart = 1;
			if (xStart + stringWidth > (getWidth()-1)) xStart = (getWidth()-1)-stringWidth;

			g.drawString("PC"+(s+1), xStart, getHeight()-(X_AXIS_SPACE-(g.getFontMetrics().getHeight()+3)));
		}

		// Put the probe list name at the top
		String listName = "Variance";
		g.drawString(listName, Y_AXIS_SPACE, 10);

		// Now draw the probes

		int lastX = 0;
		int lastY = 0;
		int thisX = 0;
		int thisY = 0;
		double value = 0;

		g.setColor(BLUE);
		for (int s=0;s<values.length;s++) {
			thisX = getXPixels(s);
			value = values[s]; 

			if (value < usedMin) value = usedMin;
			if (value > usedMax) value = usedMax;
			thisY = getYPixels(value);

			if (s>0) {
				g.drawLine(lastX, lastY, thisX, thisY);
			}

			g.fillOval(thisX-4, thisY-4, 8, 8);
			
			lastX = thisX;
			lastY = thisY;
		}
		
		// Now highlight the selected index
		thisX = getXPixels(selectedIndex);
		
		g.setColor(Color.BLACK);
		g.drawLine(thisX, getHeight()-X_AXIS_SPACE, thisX, 10);
		
		
	}

}
