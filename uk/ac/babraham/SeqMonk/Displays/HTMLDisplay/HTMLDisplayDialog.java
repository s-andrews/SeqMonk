/**
 * Copyright 2012-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HTMLDisplay;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

public class HTMLDisplayDialog extends JDialog {

	public HTMLDisplayDialog (String html) {
		
		super(SeqMonkApplication.getInstance(),"Crash Report Help");
		
		System.err.println("Making help dialog");
		
		JEditorPane jep = new JEditorPane("text/html", html);
		
		setContentPane(new JScrollPane(jep,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		setSize(700,500);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	
	
}
