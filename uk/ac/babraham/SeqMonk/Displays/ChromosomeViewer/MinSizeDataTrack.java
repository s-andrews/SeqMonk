package uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer;

import java.awt.Dimension;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

public class MinSizeDataTrack extends ChromosomeDataTrack {

	public MinSizeDataTrack(ChromosomeViewer viewer, DataCollection collection, DataStore data) {
		super(viewer, collection, data);
	}
	
	public Dimension getMinimumSize () {
		return new Dimension(1, 100);
	}

}
