/**
 * Copyright 2009-17 Simon Andrews
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
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Analysis.Statistics.BoxWhisker;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;

/**
 * A display panel which shows a single boxwhisker plot.  Can be combined 
 * into a MultiBoxWhiskerPlot to allow multiple boxwhisker plots to be put
 * on the same scale
 */
public class BoxWhiskerPanel extends JPanel {

	private final BoxWhisker bw;
	
	/** The min value. */
	private double minValue;
	
	/** The max value. */
	private double maxValue;
	
	/**
	 * Instantiates a new box whisker panel.
	 * 
	 * @param bw The pre-calculated BoxWhisker result to display
	 */
	public BoxWhiskerPanel (BoxWhisker bw) {
		this(bw,bw.minValue(),bw.maxValue());
		
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
	public BoxWhiskerPanel (BoxWhisker bw, double minValue, double maxValue) {
		this.bw = bw;
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
		
		// Work out the positions we're going to use
		int upperWhisker = getY(bw.upperWhisker());
		int upperQuartile = getY(bw.upperQuartile());
		int lowerQuartile = getY(bw.lowerQuartile());
		int lowerWhisker = getY(bw.lowerWhisker());
		int median = getY (bw.median());
		
		// Draw the main quartile box
		g.setColor(ColourScheme.BOXWHISKER_FILL);
		g.fillRoundRect(4, upperQuartile, getWidth()-8, lowerQuartile-upperQuartile,5,5);
		g.setColor(Color.BLACK);
		g.drawRoundRect(4, upperQuartile, getWidth()-8, lowerQuartile-upperQuartile,5,5);
		
		// Add the median
		g.drawLine(4, median, getWidth()-4, median);
		
		// Draw the upper whisker
		g.drawLine(getWidth()/4,upperWhisker,(getWidth()*3)/4,upperWhisker);
		g.drawLine(getWidth()/2,upperWhisker,getWidth()/2,upperQuartile);
		
		// Draw the lower whisker
		g.drawLine(getWidth()/4,lowerWhisker,(getWidth()*3)/4,lowerWhisker);
		g.drawLine(getWidth()/2,lowerWhisker,getWidth()/2,lowerQuartile);
		
		// Draw any outliers
		float [] outliers = bw.upperOutliers();
		
		int lastY = -1;
		
		for (int o=0;o<outliers.length;o++) {

			int y = getY(outliers[o]);
			if (y == lastY) {
				continue;
			}
			lastY = y;

			g.setColor(ColourScheme.BOXWHISKER_OURLIERS_ABOVE);
			g.fillOval(getWidth()/2-2, y-2, 4, 4);
			g.setColor(Color.BLACK);
			g.drawOval(getWidth()/2-2, y-2, 4, 4);
		}

		lastY = -1;
		outliers = bw.lowerOutliers();
		
		for (int o=0;o<outliers.length;o++) {

			int y = getY(outliers[o]);
			if (y == lastY) {
				continue;
			}
			lastY = y;

			g.setColor(ColourScheme.BOXWHISKER_OUTLIERS_BELOW);
			g.fillOval(getWidth()/2-2, getY(outliers[o])-2, 4, 4);
			g.setColor(Color.BLACK);
			g.drawOval(getWidth()/2-2, getY(outliers[o])-2, 4, 4);
		}
		
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
