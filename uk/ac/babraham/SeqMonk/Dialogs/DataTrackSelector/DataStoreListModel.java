/**
 * Copyright Copyright 2017-19 Simon Andrews
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


package uk.ac.babraham.SeqMonk.Dialogs.DataTrackSelector;

import java.util.Vector;

import javax.swing.AbstractListModel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class DataStoreListModel extends AbstractListModel {

	Vector<DataStore> stores = new Vector<DataStore>();
	
	public void setElementAt (DataStore o, int index) {
		stores.setElementAt(o, index);
		fireContentsChanged(this, 0, stores.size()-1);
	}

	public DataStore elementAt (int index) {
		return stores.elementAt(index);
	}
	
	public void removeAllElements () {
		int length = stores.size();
		stores.removeAllElements();
		fireContentsChanged(this, 0, length-1);
	}
	
	public void setElementAt (Object o, int index) {
		stores.setElementAt((DataStore)o, index);
		fireContentsChanged(this, index, index);
	}
	
	public DataStore [] getStores () {
		return stores.toArray(new DataStore[0]);
	}
	
	public void addElements (DataStore [] newStores) {
		for (int i=0;i<newStores.length;i++) {
			stores.add(newStores[i]);
		}
		fireContentsChanged(this, 0, stores.size()-1);
	}
	
	
	public void removeElements (DataStore [] newStores) {
		int length = stores.size();
		for (int i=0;i<newStores.length;i++) {
			stores.removeElement(newStores[i]);
		}
		fireContentsChanged(this, 0, length-1);
	}

	@Override
	public Object getElementAt(int index) {
		return stores.elementAt(index);
	}

	@Override
	public int getSize() {
		return stores.size();
	}

}
