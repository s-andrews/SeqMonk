package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.io.File;
import java.io.IOException;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public interface PCASource {

	public String probeListName();
	
	public int getPCCount();
	
	public void writeExportData(File file) throws IOException;
	
	public int getStoreCount();
	
	public float getPCAValue (int storeIndex, int pcindex);
	
	public DataStore getStore(int index);
	
	public float [] variances();	
	
}
