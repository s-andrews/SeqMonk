/**
 * Copyright 2012- 21 Simon Andrews
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

public abstract class ColourGradient {
	
	// To save generating ridiculous numbers of colours and wasting
	// memory we're going to make up a pool of 100 colours and pick
	// the closest one from that set to return.
	
	private Color [] colors = null;
	
	protected abstract Color []  makeColors ();
	
	public abstract String name ();
	
	public String toString () {
		return name();
	}
	
	/**
	 * Gets a colour from the gradient
	 * 
	 * @param value The value for which you want a colour
	 * @param min The minimum value in the gradient
	 * @param max The maximum value in the gradient
	 * @return A colour from the appropriate part of the gradient
	 */
	public Color getColor (double value,double min, double max) {
		if (colors == null) colors = makeColors();

		int percentage = (int)((colors.length * (value-min)) / (max-min));
	
		if (percentage > colors.length) percentage = colors.length;
		if (percentage < 1) percentage = 1;
		
		return colors[percentage-1];
	}

	/**
	 * Gets an RGB object for separate colour values
	 * 
	 * @param value The value stored
	 * @param min The minimum value in the gradient
	 * @param max The maximum value in the gradient
	 * @return An RGB object representing the colour
	 */
	protected RGB getRGB (double value, double min, double max) {
		
		int red;
		int green;
		int blue;
		
		double diff = max - min;
		
		// Red
		// Red is 0 for the first 50%, scales from 0-200 over 50-75%
		// and stays at 200 from 75-100%

		// Green
		// Green scales from 0-200 over the first 25%, stays at
		// 200 from 25-75% and then scales from 200-0 from 75-100%

		// Blue
		// Blue starts at 200 until 25%, then scales from 200-0
		// from 25-50%, then stays at 0 until 100%

		// Since all transitions happen in quarters of the spectrum
		// range it's easiest to deal with colour values in those
		// ranges
		
		
		if (value < (min+(diff*0.25))) {
			red = 0;
			blue = 200;
			green = (int)(200 * ((value-min) / (diff*0.25)));
		
		}
		else if (value < (min+(diff*0.5))) {
			red = 0;
			green = 200;
			blue = (int)(200 - (200 * ((value-(min+(diff*0.25))) / (diff*0.25))));
		}
		else if (value < (min+(diff*0.75))) {
			green = 200;
			blue = 0;
			red = (int)(200 * ((value-(min+(diff*0.5))) / (diff*0.25)));
		}
		else {
			red = 200;
			blue = 0;
			green = (int)(200 - (200 * ((value-(min+(diff*0.75))) / (diff*0.25))));
		}

		return new RGB(red,green,blue);
	}
	
}
