package uk.ac.babraham.SeqMonk.Pipelines.Transcription;

import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class RNASeqParametersModel extends AbstractTableModel {

	private DataStore [] stores;
	private float [] dna;
	private int [] duplication;
	
	public RNASeqParametersModel(DataStore [] stores, float [] dna, int [] duplication) {
		this.stores = stores;
		this.dna = dna;
		this.duplication = duplication;
	}
	
	public int getColumnCount() {
		int columns = 3;
		if (dna == null) columns--;
		if (duplication == null) columns--;
		
		return columns;
	}
	
	

	@Override
	public int getRowCount() {
		return stores.length;
	}

	public Class<?> getColumnClass (int c) {
		if (dna == null) {
			return new Class[] {String.class,Integer.class}[c];
		}
		else if (duplication == null) {
			return new Class[] {String.class,Float.class}[c];
		}
		else if (dna == null) {
			return new Class[] {String.class,Integer.class}[c];
		}
		else {
			return new Class[] {String.class,Float.class,Integer.class}[c];
		}
	}

	
	public String getColumnName (int c) {
		if (dna == null) {
			return new String[] {"Data Store","Duplication"}[c];
		}
		else if (duplication == null) {
			return new String[] {"Data Store","DNA Contamination"}[c];
		}
		else if (dna == null) {
			return new String[] {"Data Store","Duplication"}[c];
		}

		else {
			return new String[] {"Data Store","DNA Contamination","Duplication"}[c];
		}
	}

	
	@Override
	public Object getValueAt(int r, int c) {
		
		if (c==0) {
			return stores[r];
		}
		if (c==1) {
			if (dna==null) {
				return (duplication[r]);
			}
			else {
				return (dna[r]);
			}
		}
		if (c==2) {
			return duplication[r];
		}
		throw new IllegalStateException("Shouldn't have this many columns");
	}

}
