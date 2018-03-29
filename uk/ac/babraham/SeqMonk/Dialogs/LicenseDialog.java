/**
 * Copyright Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.Font;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

/**
 * The Class LicenseDialog shows a text representation of the GPL
 */
public class LicenseDialog extends JDialog {
	
	/** The html pane. */
	public JEditorPane htmlPane;

	/**
	 * Instantiates a new license dialog.
	 * 
	 * @param a the a
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public LicenseDialog (SeqMonkApplication a) throws IOException {
		super(a);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setTitle("Seq Monk License...");
		htmlPane = new JEditorPane(ClassLoader.getSystemResource("LICENSE.txt"));
		htmlPane.setEditable(false);
		htmlPane.setFont(new Font("Monospaced",Font.PLAIN,12));
		setContentPane(new JScrollPane(htmlPane));
		setSize(560,500);
		setLocationRelativeTo(a);
		setVisible(true);
	}
}

