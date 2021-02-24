/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.JPanel;


import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

public class AxisScalePanel extends JPanel {

	private AxisScale scale;
	private int xSpace = 20;
	private DecimalFormat df = new DecimalFormat("#.###");
	private int maxWidth = 0;

	
	public AxisScalePanel (double min, double max) {
		scale = new AxisScale(min, max);
		
		FontMetrics fm = this.getFontMetrics(this.getFont());
		
		for (double yValue = scale.getStartingValue();yValue <= scale.getMax();yValue+=scale.getInterval()) {
			String yText = "";
			if ((int)yValue == yValue) {
				yText = ""+(int)yValue;
			}
			else {
				yText = df.format(yValue);
			}
			
			int yWidth = fm.stringWidth(yText)+6;
			if (yWidth > maxWidth) {
				maxWidth = yWidth;
			}

		}
		
	}
	
	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		g.setColor(Color.BLACK);
		
		g.drawLine(getWidth(), getY(scale.getMin()), getWidth(),getY(scale.getMax()));
		
		for (double yValue = scale.getStartingValue();yValue <= scale.getMax();yValue+=scale.getInterval()) {
			int y = getY(yValue);
			g.drawLine(getWidth()-3, y, getWidth(), y);	
			
			String yText = "";
			if ((int)yValue == yValue) {
				yText = ""+(int)yValue;
			}
			else {
				yText = df.format(yValue);
			}
			
			int yWidth = g.getFontMetrics().stringWidth(yText);
			
			g.drawString(yText, getWidth()-(5+yWidth), getY(yValue)+(g.getFontMetrics().getAscent()/2));
			
		}
		
	}
	
	
	public Dimension getPreferredSize () {
		return new Dimension(maxWidth+3, 1000);
	}
	
	
	public Dimension getMinimumSize() {
		return new Dimension(maxWidth+3, 1);
		
	}
		
	
	private int getY(double value) {
		double proportion = (value-scale.getMin())/(scale.getMax()-scale.getMin());

		int y = getHeight()-xSpace;

		y -= (int)((getHeight()-xSpace)*proportion);

		return y;

	}
	
	
	
}
