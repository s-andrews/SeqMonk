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
package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;

import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferencesListener;

public class ChromosomePositionScrollBar extends JScrollBar implements DisplayPreferencesListener, AdjustmentListener {

	private boolean ignoreInternal = false;
	
	public ChromosomePositionScrollBar () {
		super(JScrollBar.HORIZONTAL,0,250,0,10000);
		DisplayPreferences.getInstance().addListener(this);
		addAdjustmentListener(this);
	}
	
	public void displayPreferencesUpdated(DisplayPreferences prefs) {
		
		// The value of a scroll bar is always its start position.  You can get the
		// end position by adding the extent.
		
		int startPoint = SequenceRead.start(prefs.getCurrentLocation());
		double proportion = startPoint/(double)prefs.getCurrentChromosome().length();
		double extentProportion = SequenceRead.length(prefs.getCurrentLocation())/(double)prefs.getCurrentChromosome().length();
		int extent = (int)(10000*extentProportion);
		int value  = (int)(10000*proportion);
		ignoreInternal = true;
		getModel().setRangeProperties(value, extent, 0, 10000, false);
	}


	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (ignoreInternal) {
			ignoreInternal = false;
		}
		else {
			DisplayPreferences dp = DisplayPreferences.getInstance();
			double proportion = getValue()/10000d;
			int newStart = (int)(dp.getCurrentChromosome().length()*proportion);
			int distance = SequenceRead.length(dp.getCurrentLocation());
			int newEnd = newStart+(distance-1);
			dp.setLocation(SequenceRead.packPosition(newStart, newEnd, Location.UNKNOWN));
		}
	}
	

}
