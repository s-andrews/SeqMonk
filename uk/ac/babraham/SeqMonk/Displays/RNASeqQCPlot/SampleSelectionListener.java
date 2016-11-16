package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public interface SampleSelectionListener {

	public void dataStoresSelected (DataStore [] stores);
	
}
