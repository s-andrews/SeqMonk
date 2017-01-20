/**
 * Copyright Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.GenomeViewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The Class ChromosomeDisplay shows a single chromosome within the genome view.
 */
public class ChromosomeScale extends JPanel {

	/** The max len. */
	private int maxLen;
			
	/** The scale used on the axis **/
	private AxisScale axisScale;
	
	/** The suffix used on the scales **/
	private String scaleSuffix;
	
	/** The division on the scales **/
	private int division;
		
	// Values cached from the last update and used when
	// relating pixels to positions
	private int xOffset = 0;
			
	public ChromosomeScale (Genome genome) {
		this(genome.getLongestChromosomeLength());
	}
	
	/**
	 * Instantiates a new chromosome scale.
	 * 
	 * @param genome the genome
	 * @param viewer the viewer
	 */
	public ChromosomeScale (int longestChr) {
		maxLen = longestChr;
		axisScale = new AxisScale(0, maxLen);
		
		if (maxLen >= 1000000) {
			scaleSuffix = "Mb";
			division = 1000000;
		}
		else if (maxLen >= 10000) {
			scaleSuffix = "kb";
			division = 1000;
		}
		else {
			scaleSuffix = "bp";
			division = 1;
		}
		
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent (Graphics g) {
		super.paintComponent(g);
		xOffset = getWidth()/80;
		if (xOffset > 10) xOffset=10;
		if (xOffset < 1) xOffset = 1;
		
		int yOffset = getHeight()/10;
		if (yOffset > 10) yOffset = 10;
		if (yOffset < 2) yOffset = 2;
		
		int width = getWidth() - (2*xOffset);
						
		g.setColor(Color.WHITE);
		g.fillRect(0,0,getWidth(),getHeight());
		
		g.setColor(Color.DARK_GRAY);
		g.drawLine(xOffset,2,xOffset+width,2);
		
		double currentY = axisScale.getStartingValue();
		while (currentY < maxLen) {
			
			int thisX = xOffset+scaleX(width, currentY, maxLen);
			String label = ""+(currentY/division);
			label = label.replaceAll("\\.0+$", "");
			
			label = label+" "+scaleSuffix;
			
			g.drawLine(thisX, 2, thisX, 4);
			
			thisX -= (g.getFontMetrics().stringWidth(label)/2);
			
			if (thisX < 2) {
				thisX = 2;
			}
			
			g.drawString(label, thisX , getHeight()-2);
			
			currentY += axisScale.getInterval();
		}
					
	}

	public Dimension getPreferredSize () {
		return new Dimension(200,20);
	}

	public Dimension getMinimumSize () {
		return new Dimension(100,20);
	}


	/**
	 * Scale x.
	 * 
	 * @param width the width
	 * @param measure the measure
	 * @param max the max
	 * @return the int
	 */
	private int scaleX (int width, double measure, double max) {
		return (int)(width*(measure/max));
	}
	
}
