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
package uk.ac.babraham.SeqMonk.Displays;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import uk.ac.babraham.SeqMonk.Displays.MemoryMonitor.MemoryMonitor;

/**
 * The Class StatusPanel shows the interactive bar at the bottom
 * of the main application screen.
 */
public class StatusPanel extends JPanel {
	
	/** The label. */
	private JLabel label = new JLabel("Seq Monk",JLabel.LEFT);
	
	/**
	 * Instantiates a new status panel.
	 */
	public StatusPanel () {
		setLayout(new BorderLayout());
		add(label,BorderLayout.WEST);
		add(new MemoryMonitor(),BorderLayout.EAST);
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	}
	
	/**
	 * Sets the text.
	 * 
	 * @param text the new text
	 */
	public void setText (String text) {
		label.setText(text);
	}
}
