/**
 * Copyright 2019 Simon Andrews
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

package uk.ac.babraham.SeqMonk.Displays.DesignEditor;

import java.util.HashMap;
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

	
	public String [] getSimpleCofactorValues(int cofactorNumber) {
		
		// The first element is the test factor.  The others are the cofactors
		String [] originalNames = factors.elementAt(cofactorNumber+1);
		
		String [] cofactorNames = new String[originalNames.length];
		
		// We need to translate names.  R is limited as to the characters it can use
		// so we're just going to use A/B/C etc.
		
		// We'll keep a store of the mappings we've created
		
		HashMap<String, String> nameTranslations = new HashMap<String, String>();
		
		char currentName = 'A';
		
		for (int s=0;s<originalNames.length;s++) {
			if (!nameTranslations.containsKey(originalNames[s])) {
				nameTranslations.put(originalNames[s], ""+currentName);
				currentName++;
			}
			
			cofactorNames[s] = nameTranslations.get(originalNames[s]);
		}
		
		
		return cofactorNames;
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
	
	public int getCofactorCount () {
		return getColumnCount()-1;
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
