/**
 * Copyright 2014-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

public class SmallRNAQCCalcualtor implements Cancellable, Runnable {

	private DataCollection collection;
	private String [] features;
	private DataStore [] stores;
	private int minLength;
	private int maxLength;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;


	public SmallRNAQCCalcualtor (DataCollection collection, String [] features, int minLength, int maxLength, DataStore [] stores) {
		this.collection = collection;
		this.features = features;
		this.stores = stores;
		this.minLength = minLength;
		this.maxLength = maxLength;
	}

	public void addListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	public void startCalculating () {
		Thread t = new Thread(this);
		t.start();
	}

	public void cancel() {
		cancel = true;
	}

	private void updateProgress (String message, int current, int total) {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressUpdated(message, current, total);
		}
	}

	private void progressComplete (SmallRNAQCResult [] results) {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressComplete("rna_qc", results);
		}
	}

	private void progressCancelled () {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressCancelled();
		}
	}


	public void run() {

		SmallRNAQCResult [] results = new SmallRNAQCResult [stores.length]; 
		for (int s=0;s<stores.length;s++) {
			results[s] = new SmallRNAQCResult(stores[s], minLength, maxLength, features);
		}
		
		for (int f=0;f<features.length;f++) {
			for (int d=0;d<stores.length;d++) {
				updateProgress("Quantitating "+features[f]+" for "+stores[d], (stores.length*f)+d, stores.length*features.length);

				int [] lengthCounts = new int [(maxLength-minLength)+1];

				String lastChr = "";
				Chromosome lastChrObject = null;

				Feature [] theseFeatures = collection.genome().annotationCollection().getFeaturesForType(features[f]);

				Arrays.sort(theseFeatures);

				for (int i=0;i<theseFeatures.length;i++) {

					if (theseFeatures[i].chromosomeName() != lastChr) {
						lastChr = theseFeatures[i].chromosomeName();
						lastChrObject = collection.genome().getExactChromsomeNameMatch(lastChr);
					}

					long [] reads = stores[d].getReadsForProbe(new Probe(lastChrObject, theseFeatures[i].location().packedPosition()));

					for (int r=0;r<reads.length;r++) {

						int length = SequenceRead.length(reads[r]);
						if (length >=minLength && length <= maxLength) {
							lengthCounts[length-minLength]++;

							if (cancel) {
								progressCancelled();
								return;
							}

						}

					} // End each read
					results[d].addCountsForFeatureIndex(f,lengthCounts);
				} // End each feature instance
			} // End each data store
		} // End each feature




		// Finally we make up the data sets we're going to pass back.



		progressComplete(results);
	}

}
