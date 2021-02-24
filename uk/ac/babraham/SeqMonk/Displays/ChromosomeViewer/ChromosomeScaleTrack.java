/**
 * Copyright Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Utilities.AxisScale;

/**
 * The ChromosomeScaleTrack shows the current genome position on a sensible scale.  
 * It is usually only created and managed by a surrounding instance of ChromsomeViewer.
 */
public class ChromosomeScaleTrack extends JPanel {

	/** The chromosome viewer which contains this track **/
	private ChromosomeViewer viewer;			
	
	/** The full virtual width of this track */
	private long lastLocation;
	
	/** The height of this track */
	private int height;
	/** A cached value of the width of the visible portion of this track inside the surrounding JScrollPane */
	private int width;

	private AxisScale scale = null;
	
	
	/**
	 * Instantiates a new scale track.
	 * 
	 * @param viewer The chromosome viewer which holds this track
	 */
	public ChromosomeScaleTrack (ChromosomeViewer viewer) {
		this.viewer = viewer;
		
	}
		
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	public void paintComponent (Graphics g) {

		super.paintComponent(g);
		
		DisplayPreferences dp = DisplayPreferences.getInstance();
		
		if (dp.getCurrentLocation() != lastLocation) {
			// We need to rescale the frequency with which we're drawing points
			scale = new AxisScale(0, SequenceRead.length(dp.getCurrentLocation()));
			lastLocation = dp.getCurrentLocation();
		}
		
		height = getHeight();
		width = getWidth();
	
		g.setColor(Color.WHITE);
		g.fillRect(0,0,width,height);
		
		g.setColor(Color.DARK_GRAY);
						
		
		// Draw a line along the top
		g.drawLine(0, 3, width, 3);
		
		// Now go through all the scale positions figuring out whether they
		// need to be displayed
				
		int startBp = SequenceRead.start(lastLocation);
		int endBp = SequenceRead.end(lastLocation);

		int currentBase = 0;
		
		while (currentBase < endBp) {
		
			if (currentBase < startBp) {
				currentBase += scale.getInterval();
				continue;
			}
			
			String name = commify(currentBase);
			
			int nameWidth = g.getFontMetrics().stringWidth(name);
			
			int thisX = bpToPixel(currentBase);
			
			g.drawString(name,thisX-(nameWidth/2), getHeight()-2);
			
			g.drawLine(thisX, 3, thisX, height-(g.getFontMetrics().getAscent()+3));
			
			currentBase += scale.getInterval();
		}			

	}
	
	private static String commify (int number) {
		char [] numbers = (""+number).toCharArray();
		
		char [] commaNumbers = new char[numbers.length+((numbers.length-1)/3)];
				
		int commaPos=commaNumbers.length-1;
		for (int numberPos = 0;numberPos<numbers.length;numberPos++) {
			if (numberPos%3 == 0 && numberPos>0) {
				commaNumbers[commaPos] = ',';
				commaPos--;
			}
			commaNumbers[commaPos] = numbers[numbers.length-(numberPos+1)];
			commaPos--;
		}
				
		return new String(commaNumbers);
	}
	
	
	
	/**
	 * Bp to pixel.
	 * 
	 * @param bp the bp
	 * @return the int
	 */
	private int bpToPixel (int bp) {
		return (int)(((double)(bp-viewer.currentStart())/((viewer.currentEnd()-viewer.currentStart())))*width);		
	}
	
	// There's no sense in letting the annotation tracks get too tall.  We're better
	// off using that space for data tracks.
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#getMinimumSize()
	 */
	public Dimension getMinimumSize () {
		return new Dimension(30,25);
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(30,25);
	}
	


}
