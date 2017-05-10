package uk.ac.babraham.SeqMonk.Displays.TsneDataStorePlot;

import javax.swing.JDialog;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * Sets the options for a t-sne plot of multiple data stores
 */

public class TsneOptionsDialog extends JDialog {

	
	public TsneOptionsDialog (ProbeList probes, DataStore [] stores) {
		
		super(SeqMonkApplication.getInstance(),"Tsne Options");
		
		// For now we'll just launch the calculator.
		
		new TsneDataStoreResult(probes, stores, 3, 10);
		
		
		
	}
	
}
