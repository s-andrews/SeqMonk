package uk.ac.babraham.SeqMonk.Displays.DesignEditor;

import java.util.Iterator;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;

public class StatisticalDesign implements TableModel {

	Vector<TableModelListener> listeners = new Vector<TableModelListener>();

	DataStore [] testStores;
	
	Vector<String []> factors = new Vector<String[]>();
	
	
	public StatisticalDesign (ReplicateSet [] testingSets) {
		int totalCount = 0;
		
		for (int i=0;i<testingSets.length;i++) {
			totalCount += testingSets[i].dataStores().length;
		}
		
		testStores = new DataStore[totalCount];
		
		totalCount = 0;
		
		for (int i=0;i<testingSets.length;i++) {
			DataStore [] theseStores = testingSets[i].dataStores();
			
			for (int j=0;j<theseStores.length;j++) {
				testStores[totalCount] = theseStores[j];
				totalCount++;
			}
		}

		
		
		// This can't fail so if it does something really horrible has gone wrong.
		try {
			addFactor(testingSets);
		} 
		catch (InvalidFactorException e) {
			throw new IllegalStateException("Unfound data when adding testing factor. That can't be right!");
		}
	
	}
	
	
	public void addFactor (ReplicateSet [] sets) throws InvalidFactorException {
		
		String [] factorNames = new String[testStores.length];
		
		STORE: for (int i=0;i<testStores.length;i++) {
			for (int s=0;s<sets.length;s++) {
				if (sets[s].containsDataStore(testStores[i])) {
					factorNames[i] = sets[s].name();
					continue STORE;
				}				
			}
			throw new InvalidFactorException("No match found to "+testStores[i]);
		}
		
		factors.add(factorNames);
		
		Iterator<TableModelListener> it = listeners.iterator();
		
		while (it.hasNext()) {
			it.next().tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
		}
		
	}
	
	public void removeFactor () {
		
		if (factors.size() == 1) return;
		
		factors.remove(factors.size()-1);

		Iterator<TableModelListener> it = listeners.iterator();
		
		while (it.hasNext()) {
			it.next().tableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
		}

	}
	
	
	@Override
	public void addTableModelListener(TableModelListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return factors.size();
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex == 0) {
			return "Test factor";
		}
		else {
			return "Cofactor "+columnIndex;
		}
		
	}

	@Override
	public int getRowCount() {
		return testStores.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return factors.get(columnIndex)[rowIndex];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		if (listeners.contains(l)) {
			listeners.remove(l);
		}
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}

}
