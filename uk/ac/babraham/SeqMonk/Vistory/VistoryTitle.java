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
import java.awt.Font;
import java.util.Date;

import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class VistoryTitle extends VistoryBlock {

	protected JTextArea text;
	private int indexPosition = 0;
	private int level;
	
	private boolean allowsRelative = true;
	
	public VistoryTitle(int level, Date date, String data) {
		super(date);
		startSetup();
		text.setText(data.replaceAll("<br>", "\n"));
	}
	
	public VistoryTitle (int level) {
		this.level = level;
		startSetup();
	}

	public int level () {
		return level;
	}
	
	public VistoryTitle (String message, int level) {
		this.level = level;
		// If a message is being supplied then this isn't a user
		// instigated title so we put it at the end.
		allowsRelative = false;
		startSetup();
		text.setText(message);
	}

	
	private void startSetup() {
		text = new JTextArea("New Title");
		text.setLineWrap(true);
		text.setWrapStyleWord(true);
		text.addMouseListener(this);
		Font font = getFont();

		if (level == 2) {
			font = new Font(font.getName(), font.getStyle(), font.getSize()*2);
		}
		else if (level == 3) {
			font = new Font(font.getName(), font.getStyle(), (int)(font.getSize()*1.5));
		}
		
		// Anything below 3 stays at default size.
		
		text.setFont(font);
		
		add(text,BorderLayout.CENTER);
	}
	
	public void setIndex (int index) {
		this.indexPosition = index;
	}
	
	public void setText (String text) {
		this.text.setText(text);
	}
	
	
	public String getText () {
		return text.getText();
	}
	@Override
	public String getHTML() {
		return("<h"+level+" id=\""+indexPosition+"\">"+EscapeHTML.escapeHTML(text.getText())+"</h2>");
	
	}

	@Override
	public String getType() {
		return "TITLE"+level;
	}

	@Override
	public String getData() {
		return text.getText().replaceAll("\n", "<br>");
	}

	@Override
	public boolean allowsRelativePosition() {
		return allowsRelative;
	}
	

}
