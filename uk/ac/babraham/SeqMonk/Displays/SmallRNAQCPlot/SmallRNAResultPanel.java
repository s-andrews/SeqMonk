/**
 * Copyright 2014-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;

public class SmallRNAResultPanel extends JPanel {

	private SmallRNAQCResult result;
	private int maxCountForAnyLength = 0;
	private int xUnitCounts;
	
	private static int Y_TOP_SPACE = 20;
	private static int Y_BOTTOM_SPACE = 20;
	private static int X_LEFT_SPACE = 10;
	private static int X_RIGHT_SPACE = 10;
	
	
	
	public SmallRNAResultPanel (SmallRNAQCResult result) {
		this.result = result;
		
		for (int l=result.minLength();l<=result.maxLength();l++) {
			int [] counts = result.getCountsForLength(l);
			
			int sum = 0;
			
			for (int i=0;i<counts.length;i++) sum+=counts[i];
			
			if (sum > maxCountForAnyLength) maxCountForAnyLength = sum;
		}
		
		xUnitCounts = result.maxLength()-result.minLength();
		
		
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		// Draw some axes
		g.setColor(Color.BLACK);
		g.drawLine(X_LEFT_SPACE, Y_TOP_SPACE, X_LEFT_SPACE, getHeight()-Y_BOTTOM_SPACE);
		
		g.drawLine(X_LEFT_SPACE, getHeight()-Y_BOTTOM_SPACE, getWidth()-X_RIGHT_SPACE, getHeight()-Y_BOTTOM_SPACE);
		
		for (int l=result.minLength();l<=result.maxLength();l++) {
			g.drawLine(getX(l),getHeight()-Y_BOTTOM_SPACE,getX(l),getHeight()-(Y_BOTTOM_SPACE-3));
			int stringCentre = getX(l)+(getXUnitWidth()/2);
			g.drawString(""+l+"bp", stringCentre-(g.getFontMetrics().stringWidth(""+l+"bp")/2), getHeight()-3);
		}
		
		// Draw the stacked charts
		
		for (int l=result.minLength();l<=result.maxLength();l++) {
			
			int lastYTop = getY(0);
			int runningCount = 0;
			
			int [] counts = result.getCountsForLength(l);
			
			for (int c=0;c<counts.length;c++) {
				runningCount += counts[c];
				int thisYTop = getY(runningCount);
				
				g.setColor(ColourIndexSet.getColour(c));
				g.fillRect(getX(l)+2, thisYTop, getXUnitWidth()-4, lastYTop-thisYTop);
				
				lastYTop = thisYTop;
			}
		
		}
		
		// Draw the sample name
		g.setColor(Color.BLACK);
		int nameWidth = g.getFontMetrics().stringWidth(result.store().name());
		g.drawString(result.store().name(), getWidth()-(X_RIGHT_SPACE+nameWidth), Y_TOP_SPACE);
		
	}
	
	
	private int getY (int value) {
		int height = getHeight() - (Y_TOP_SPACE+Y_BOTTOM_SPACE);
		
		double proportion = value/(double)maxCountForAnyLength;
		
		return Y_TOP_SPACE+(int)(height*(1-proportion));
		
	}
	
	private int getXUnitWidth () {
		
		int width = getWidth() - (X_LEFT_SPACE+X_RIGHT_SPACE);
		
		double unitWidth = width / (double)(xUnitCounts+1);
		
		return (int)unitWidth;
	}
	
	private int getX (int length) {
		int width = getWidth() - (X_LEFT_SPACE+X_RIGHT_SPACE);
		
		double unitWidth = width / (double)(xUnitCounts+1);
		
		return (X_LEFT_SPACE+(int)(unitWidth*(length-result.minLength())));
	}

	
}
