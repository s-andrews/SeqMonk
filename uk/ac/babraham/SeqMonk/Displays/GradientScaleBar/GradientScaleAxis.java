/**
 * Copyright 2012-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.GradientScaleBar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class GradientScaleAxis extends JPanel {

	private AxisScale axisScale;
	
	private int maxX = 0;

	
	public GradientScaleAxis (double min, double max) {
		setLimits(min, max);
	}
	
	public void setLimits (double min,double max) {
		if (max < min) throw new IllegalArgumentException("Max cannot be <= min max="+max+" min="+min);
		axisScale = new AxisScale(min, max);
		maxX = getMaxWidth();
		repaint();
	}
	
	private int getMaxWidth () {
		if (this.getGraphics() == null) return 50;
		int maxWidth = 0;
		
		double current = axisScale.getStartingValue();
		if (current == axisScale.getMin()) current += axisScale.getInterval();
		for (;current<axisScale.getMax();current+=axisScale.getInterval()) {
			
			String number = axisScale.format(current);
			
			int stringWidth = 10 + this.getGraphics().getFontMetrics().stringWidth(number) + 5;
			if (stringWidth > maxWidth) maxWidth = stringWidth;
					
		}
		
		return maxWidth;
	}
	
	public Dimension getPreferredSize () {
		int x = 50;
		if (maxX > 0) x = maxX;
		return new Dimension(x,100);
	}

	public void paint (Graphics g) {
		super.paint(g);
		
		// Draw an axis line
		
		g.setColor(Color.BLACK);
		
		g.drawLine(2, 0, 2, getHeight());
		
		// Draw the points on the line
		double current = axisScale.getStartingValue();
		if (current == axisScale.getMin()) current += axisScale.getInterval();
		for (;current<axisScale.getMax();current+=axisScale.getInterval()) {
			
			int y = getY(current);
			
			g.drawLine(2, y, 5, y);
			
			String number = axisScale.format(current);
						
			g.drawString(number, 10, y+(g.getFontMetrics().getAscent()/2));
		
		}
		
	}
	
	
	private int getY (double value) {
		double proportion = (value-axisScale.getMin())/(axisScale.getMax()-axisScale.getMin());
		return getHeight()- (int)(getHeight()*proportion);
	}
	
}
