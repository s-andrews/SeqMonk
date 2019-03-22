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

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;

public class GradientPanel extends JPanel {

	public static final int HORIZONTAL_GRADIENT = 5001;
	public static final int VERTICAL_GRADIENT = 5002;
	
	private ColourGradient gradient;
	private int direction;
	
	public GradientPanel (ColourGradient gradient, int direction) {
		this.gradient = gradient;
		
		if (direction == HORIZONTAL_GRADIENT || direction == VERTICAL_GRADIENT) {
			this.direction = direction;
		}
		else {
			throw new IllegalArgumentException("Gradient must be HORIZONTAL_GRADIENT or VERTICAL_GRADIENT");
		}
	}
	
	public void setGradient (ColourGradient gradient) {
		this.gradient = gradient;
		repaint();
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		
		if (direction == HORIZONTAL_GRADIENT) paintHorizontal(g);
		else paintVertical(g);
	}
	
	private void paintHorizontal (Graphics g) {
		
		int lastX = 0;
		for (int i=1;i<=100;i++) {
			g.setColor(gradient.getColor(i, 1, 100));
			
			int thisX = (getWidth()*i)/100;
			g.fillRect(lastX, 0, thisX-lastX, getHeight());
			
		}
	}
	
	public Dimension getPreferredSize () {
		return new Dimension(50,100);
	}
		
	
	private void paintVertical(Graphics g) {
		int lastY = 0;
		for (int i=0;i<=100;i++) {
			g.setColor(gradient.getColor(i, 1, 100));
			
			int thisY = getHeight() - (getHeight()*i)/100;
			g.fillRect(0, thisY, getWidth(), (lastY-thisY)+1);
			lastY = thisY;
			
		}
		
	}
	
	
	
}
