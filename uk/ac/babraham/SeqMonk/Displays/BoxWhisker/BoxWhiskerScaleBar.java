/**
 * Copyright 2011-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.BoxWhisker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class BoxWhiskerScaleBar extends JPanel {

	private AxisScale axisScale;
	
	public BoxWhiskerScaleBar (double min, double max) {
		setLimits(min,max);
	}
	
	public void setLimits (double min, double max) {
		axisScale = new AxisScale(min, max);
		repaint();
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);
		
		// We need a border of 5 at each end
		
		double yScale = (getHeight()-10d) / (axisScale.getMax()-axisScale.getMin());
		
		g.drawLine(getWidth()-5, 5, getWidth()-5, getHeight()-5);
		
		double currentY = axisScale.getStartingValue();
		
		while (currentY < axisScale.getMax()) {
			
			int y = (getHeight()-5)-(int)((currentY-axisScale.getMin())*yScale);
			
			g.drawLine(getWidth()-5, y, getWidth()-8,y);
			g.drawString(axisScale.format(currentY), 5, y+(g.getFontMetrics().getAscent()/2));
			
			currentY += axisScale.getInterval();
		}
		
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(50,100);
	}
	
	public Dimension getMinimumSize () {
		return new Dimension(50,50);
	}
}
