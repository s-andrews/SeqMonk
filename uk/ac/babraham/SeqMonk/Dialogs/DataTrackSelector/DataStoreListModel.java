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
		stores.removeAllElements();
		fireContentsChanged(this, 0, 0);
	}
	
	public void setElementAt (Object o, int index) {
		stores.setElementAt((DataStore)o, index);
		fireContentsChanged(this, 0, stores.size()-1);
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
		for (int i=0;i<newStores.length;i++) {
			stores.removeElement(newStores[i]);
		}
		fireContentsChanged(this, 0, stores.size()-1);
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
