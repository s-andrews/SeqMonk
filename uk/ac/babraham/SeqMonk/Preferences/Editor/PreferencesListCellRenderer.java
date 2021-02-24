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

package uk.ac.babraham.SeqMonk.Preferences.Editor;

import java.awt.Component;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

public class PreferencesListCellRenderer implements ListCellRenderer {

	private Component [] icons;
	private NamedValueObject [] objects;
	
	
	public PreferencesListCellRenderer (NamedValueObject [] objects) {
		this.objects = objects;
		// For each object we try to find an equivalent image icon
		
		icons = new Component [objects.length];
		
		for (int i=0;i<objects.length;i++) {
			// Find the icon if there is one
			URL url = ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/DisplayPrefsIcons/"+objects[i].toString()+".png");

			if (url == null) {
				throw new IllegalStateException("Can't find: 'uk/ac/babraham/SeqMonk/Resources/DisplayPrefsIcons/"+objects[i].toString()+".png");
			}
			
			icons[i] = new JLabel(objects[i].toString(), new ImageIcon(url), SwingConstants.LEFT);
		}
		
		
	}
	
	public Component getListCellRendererComponent(JList list, Object obj,int index, boolean selected, boolean focussed) {
		
		for (int i=0;i<objects.length;i++) {
			if (obj == objects[i]) {
				return icons[i];
			}
		}

		return null;
	}

}
