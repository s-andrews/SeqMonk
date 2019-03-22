/**
 * Copyright Copyright 2018-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Scrollable;

public class ScrollablePanel extends JPanel implements Scrollable {

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		// TODO Auto-generated method stub
		return getPreferredSize();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle arg0, int arg1, int arg2) {
		return 20;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle arg0, int arg1, int arg2) {
		return 5;
	}

}
