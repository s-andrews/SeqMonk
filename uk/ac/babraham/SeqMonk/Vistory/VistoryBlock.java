/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Displays.Vistory.VistoryBlockButtons;;

public abstract class VistoryBlock extends JPanel implements MouseListener {
	
	private Date date;
	
	public VistoryBlock () {
		date = new Date();
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		add(new VistoryBlockButtons(this),BorderLayout.EAST);
		setBorder(BorderFactory.createLineBorder(Color.WHITE,3));
		addMouseListener(this);
	}
	
	public VistoryBlock (Date date) {
		this();
		date = this.date;
	}
	
	public Date date () {
		return date;
	}
	
	public void requestVistoryFocus () {
		Vistory.getInstance().requestVistoryFocus(this);
	}
	
	public void setVistoryFocus (boolean hasFocus) {
		if (hasFocus) {
			setBorder(BorderFactory.createLineBorder(Color.GRAY,3));
		}
		else {
			setBorder(BorderFactory.createLineBorder(Color.WHITE,3));
		}
	}
		
	public abstract String getHTML();
	
	public abstract String getType();
	
	public abstract String getData();
	
	public abstract boolean allowsRelativePosition();
	
	@Override
	public void mouseClicked(MouseEvent e) {
		requestVistoryFocus();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

}
