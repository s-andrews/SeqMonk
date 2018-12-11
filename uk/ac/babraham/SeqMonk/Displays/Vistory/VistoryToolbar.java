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
package uk.ac.babraham.SeqMonk.Displays.Vistory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JToolBar;

import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryText;
import uk.ac.babraham.SeqMonk.Vistory.VistoryTitle;

public class VistoryToolbar extends JToolBar implements ActionListener {

	public VistoryToolbar () {
		JButton addTextButton = new JButton("Add text");
		addTextButton.setActionCommand("add_text");
		addTextButton.addActionListener(this);
		add(addTextButton);

		JButton addTitleButton = new JButton("Add Title");
		addTitleButton.setActionCommand("add_title");
		addTitleButton.addActionListener(this);
		add(addTitleButton);

	
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		
		if (command.equals("add_text")) {
			Vistory.getInstance().addBlock(new VistoryText());
		}
		else if (command.equals("add_title")) {
			Vistory.getInstance().addBlock(new VistoryTitle());
		}
		else {
			throw new IllegalArgumentException("Unknown command "+command);
		}
	}
	
		
	
}
