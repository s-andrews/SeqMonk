/**
 * Copyright 2012-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Gradients;

import java.awt.Color;

public class RedWhiteBlueColourGradient extends ColourGradient {

	
	public String name () {
		return "Red White Blue Colour Gradient";
	}
		
	protected Color [] makeColors() {
		/*
		 * We pre-generate a list of 100 colours we're going to
		 * use for this display.
		 * 
		 * Because a linear gradient ends up leaving too much
		 * green in the spectrum we put this on a log scale
		 * to emphasise low and high values so the display
		 * is clearer.
		 */
		
		Color [] colors = new Color[100];
		
		
		for (int i=0;i<=49;i++) {
			colors[i] = new Color(255,(255*(i+1))/50,(255*(i+1))/50);
		}
		for (int i=50;i<100;i++) {
			colors[i] = new Color(255-((255*(i-49))/50),255-((255*(i-49))/50),255);
		}
						
		return colors;
	}
}
