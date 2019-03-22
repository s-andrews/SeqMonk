/**
 * Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;

public class NumberKeyListener implements KeyListener {

	private boolean allowsFractions;
	private boolean allowsNegatives;
	
	// We looked at allowing min value filtering, but this can't work. It ends
	// up corrupting the values whilst you're trying to type a larger number,
	// eg, min value is 20 and you try to type 100 and end up with 200 because
	// 1 and 10 would both get turned to 20.  The current scheme can still break
	// if the max number is negative, but such is life.
	
	private Double maxDoubleValue = null;
	private Integer maxIntValue = null;
	
	public NumberKeyListener (boolean allowsFractions, boolean allowsNegatives) {
		this.allowsFractions = allowsFractions;
		this.allowsNegatives = allowsNegatives;
	}

	public NumberKeyListener (boolean allowsFractions, boolean allowsNegatives, double maxValue) {
		this.allowsFractions = allowsFractions;
		this.allowsNegatives = allowsNegatives;
		if (allowsFractions) {
			maxDoubleValue = maxValue;
		}
		else {
			maxIntValue = (int)maxValue;
		}
	}


	public void keyReleased(KeyEvent ke) {

		JTextField source;
		if (ke.getSource() instanceof JTextField) {
			source = (JTextField)ke.getSource();
		}
		else {
			throw new IllegalArgumentException("The NumberKeyListener expects to be attached to a JTextField");
		}
				
		String text = source.getText();
		
		// Blank entries are OK
		if (text.length() == 0) return;		
		
		// Check for the special case of a sole minus
		if (text.equals("-") && allowsNegatives) {
			return;
		}
		
		// Remove any commas if people have copy/pasted numbers formatted that way
		if (text.indexOf(",")>=0) {
			source.setText(text.replaceAll(",", ""));
		}

		// Remove any spaces
		if (text.indexOf(" ")>=0) {
			source.setText(text.replaceAll(" ", ""));
		}

		
		try {
			if (allowsFractions) {
				double dbl = Double.parseDouble(text);
				if (! allowsNegatives && dbl < 0) {
					throw new NumberFormatException();
				}
				
				// Check against limits			
				if (maxDoubleValue != null && dbl > maxDoubleValue) {
					source.setText(maxDoubleValue.toString());
				}
				
			}
			else {
				int integer = Integer.parseInt(text);
				if (! allowsNegatives && integer < 0) {
					throw new NumberFormatException();
				}
				
				// Check against limits
				if (maxIntValue != null && integer > maxIntValue) {
					source.setText(maxIntValue.toString());
				}

			}
		}
		catch (NumberFormatException nfe) {
			// We want to delete the last character and try again
			source.setText(text.substring(0, text.length()-1));
			keyReleased(ke);
		}
		
		
	}
	
	public void keyPressed(KeyEvent arg0) {}
	public void keyTyped(KeyEvent arg0) {}

}
