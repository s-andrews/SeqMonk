/**
 * Copyright 2011- 21 Simon Andrews
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

public class ColourIndexSet {


	private static Color [] colours = null;


	public static synchronized Color getColour (int index) {

		if (colours == null) {
			makeColours();
		}

		index = index % colours.length;
		
		if (index < 0) index = 0;

		return colours[index];
	}

	private static void makeColours () {
		
		// Rather than try to dynamically make a colour set we've taken a
		// set of optimised colours from colorbrewer2.org
		
		colours = new Color[20];
		
		colours[0] = new Color(31,120,180);
		colours[1] = new Color(227,26,28);
		colours[2] = new Color(51,160,44);
		colours[3] = new Color(255,127,0);
		colours[4] = new Color(106,61,154);
		colours[5] = new Color(166,206,227);
		colours[6] = new Color(178,223,138);
		colours[7] = new Color(251,154,153);
		colours[8] = new Color(253,191,111);
		colours[9] = new Color(202,178,214);
		colours[10] = new Color(141,211,199);
		colours[11] = new Color(190,186,218);
		colours[12] = new Color(251,128,114);
		colours[13] = new Color(128,177,211);
		colours[14] = new Color(253,180,98);
		colours[15] = new Color(179,222,105);
		colours[16] = new Color(252,205,229);
		colours[17] = new Color(188,128,189);
		colours[18] = new Color(204,235,197);
		colours[19] = new Color(255,237,111);
		
	}
	
	
//	private static void makeColours () {
//
//		float [] proportions = new float [] {0,0.5f,0.25f,0.75f,0.125f,0.375f,0.625f,0.885f};
//
//		colours = new Color[proportions.length*4*4];
//
//		int index = 0;
//		for (int saturation=4;saturation>0;saturation--) {
//			for (int brightness=4;brightness>0;brightness--) {
//				for (int p=0;p<proportions.length;p++) {
//
//					colours[index] = new Color(Color.HSBtoRGB(proportions[p], saturation/5f, brightness/5f));
//					index++;
//
//				}
//			}		
//
//		}
//
//	}
}
