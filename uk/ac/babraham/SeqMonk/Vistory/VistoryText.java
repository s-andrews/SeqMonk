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
package uk.ac.babraham.SeqMonk.Vistory;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Date;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryText extends VistoryBlock implements FocusListener {

	protected JTextArea text;
	
	public VistoryText (Date date, String data) {
		super(date);
		startSetup();
		text.setText(data.replaceAll("<br>", "\n"));
	}
	
	public VistoryText () {
		startSetup();
	}
	
	private void startSetup() {
		text = new JTextArea("[Enter text here]");
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.setFont(getFont());
		text.addMouseListener(this);
		text.addFocusListener(this);
		add(text,BorderLayout.CENTER);
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	public void mouseClicked (MouseEvent me) {
		if (text.getText().equals("[Enter text here]")) {
			text.setText("");
		}
		super.mouseClicked(me);
	}
	
	
	@Override
	public String getHTML() {

		StringBuffer sb = new StringBuffer();
		
		String [] sections = text.getText().split("\n");
		
		for (int s=0;s<sections.length;s++) {
			sb.append("<p>"+EscapeHTML.escapeHTML(sections[s])+"</p>");
		}
	
		return sb.toString();
	}

	@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		if (text.getText().equals("")) {
			text.setText("[Enter text here]");
		}
	}

	@Override
	public String getType() {
		return "TEXT";
	}

	@Override
	public String getData() {
		return text.getText().replaceAll("\n", "<br>");
	}

	@Override
	public boolean allowsRelativePosition() {
		return true;
	}

}
