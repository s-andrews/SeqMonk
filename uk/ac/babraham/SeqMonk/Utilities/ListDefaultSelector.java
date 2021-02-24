/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Utilities;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JList;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class ListDefaultSelector {

	
	public static void selectDefaultStores (JList list) {
		DataStore [] stores = SeqMonkApplication.getInstance().drawnDataStores();
		Vector<Integer>selected = new Vector<Integer>();
		
		for (int index=0;index<list.getModel().getSize();index++) {
			for (int store=0;store<stores.length;store++) {
				if (stores[store] == list.getModel().getElementAt(index)) {
					selected.add(index);
				}
			}
		}
		
		int [] selectedIndices = new int [selected.size()];
		
		Enumeration<Integer>e = selected.elements();
		int i=0;
		while (e.hasMoreElements()) {
			selectedIndices[i] = e.nextElement();
			i++;
		}
		
		list.setSelectedIndices(selectedIndices);
		
		
	}
	
}
