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
package uk.ac.babraham.SeqMonk.Displays.StarWars;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Analysis.Statistics.StarWars;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;

/**
 * A display panel which shows a single starwars plot.  Can be combined 
 * into a MultiStarWarsPlot to allow multiple starwars plots to be put
 * on the same scale
 */
public class StarWarsPanel extends JPanel {

	private final StarWars sw;
	
	/** The min value. */
	private double minValue;
	
	/** The max value. */
	private double maxValue;
	
	/**
	 * Instantiates a new box whisker panel.
	 * 
	 * @param sw The pre-calculated StarWars result to display
	 */
	public StarWarsPanel (StarWars sw) {
		this(sw,sw.maxValue(),sw.minValue());
		
	}
	
	/**
	 * Instantiates a new box whisker panel with a min/max value
	 * which is different to the intrinsic min/max of the 
	 * result file being shown.
	 * 
	 * @param sw The pre-calculated StarWars result to display.
	 * @param minValue the min value
	 * @param maxValue the max value
	 */
	public StarWarsPanel (StarWars sw, double minValue, double maxValue) {
		this.sw = sw;
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
		g.setColor(Color.BLACK);
		
		// Work out the positions we're going to use
		int mean = getY(sw.mean());
		int standard_error_lower_whisker = getY(sw.mean() - sw.standard_error());
		int standard_error_upper_whisker = getY(sw.mean() + sw.standard_error());
		
		
		// Draw the upper whisker
		g.drawLine(getWidth()/4,standard_error_upper_whisker,(getWidth()*3)/4,standard_error_upper_whisker);
		g.drawLine(getWidth()/2,standard_error_upper_whisker,getWidth()/2,mean);
		
		// Draw the lower whisker
		g.drawLine(getWidth()/4,standard_error_lower_whisker,(getWidth()*3)/4,standard_error_lower_whisker);
		g.drawLine(getWidth()/2,standard_error_lower_whisker,getWidth()/2,mean);
		
		// Draw the mean
		g.setColor(ColourScheme.BOXWHISKER_OURLIERS_ABOVE);
		g.fillOval(getWidth()/2-8, mean-8, 16, 16);
		g.setColor(Color.BLACK);
		g.drawOval(getWidth()/2-8, mean-8, 16, 16);
		
		
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
}
