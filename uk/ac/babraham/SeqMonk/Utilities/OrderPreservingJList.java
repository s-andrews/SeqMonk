package uk.ac.babraham.SeqMonk.Utilities;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class OrderPreservingJList extends JList implements ListSelectionListener {

	
	private Vector selectedItems = new Vector();
	
	public OrderPreservingJList(ListModel model) {
		super(model);
		addListSelectionListener(this);
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		
		Object [] selected = super.getSelectedValues();
		
		// Remove any items we're holding onto which aren't in the currently 
		// selected set.
		
		Iterator it = selectedItems.iterator();
		
		Vector itemsToRemove = new Vector();
		
		while (it.hasNext()) {
			Object value = it.next();
			
			boolean found = false;
			
			for (int i=0;i<selected.length;i++) {
				if (selected[i] == value) {
					found = true;
					break;
				}
			}
			
			if (!found) {
				itemsToRemove.add(value);
			}
			
		}
		
		it = itemsToRemove.iterator();
		
		while (it.hasNext()) {
			selectedItems.remove(it.next());
		}
		
		// Add any items which aren't in the selected set
		
		for (int i=0;i<selected.length;i++) {
			if (!selectedItems.contains(selected[i])) {
				selectedItems.add(selected[i]);
			}
		}

				
	}
	
	@Override
	public Object [] getSelectedValues () {
		return selectedItems.toArray(new Object[0]);
	}


	
	
}
