/**
 * Copyright 2014-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

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
import uk.ac.babraham.SeqMonk.Utilities.Features.FeatureMerging;

public class RNAQCCalcualtor implements Cancellable, Runnable {

	private DataCollection collection;

	private String geneFeatureName;
	private String transcriptFeatureName;
	private String rRNAFeatureName;
	private Chromosome [] chromosomes;

	private Feature [] geneFeatures;
	private Feature [] transcriptFeatures;
	private Feature [] rRNAFeatures;
	private DataStore [] stores;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;


	public RNAQCCalcualtor (DataCollection collection, String geneFeatures, String transcriptFeatures, String rRNAFeatures, Chromosome [] chromosomes, DataStore [] stores) {
		this.collection = collection;
		this.geneFeatureName = geneFeatures;
		this.transcriptFeatureName = transcriptFeatures;
		this.rRNAFeatureName = rRNAFeatures;
		this.chromosomes = chromosomes;
		this.stores = stores;
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

	private void progressWarning (String message) {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			Exception e = new Exception(message);
			en.nextElement().progressWarningReceived(e);
		}
	}

	private void progressComplete (RNAQCResult result) {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressComplete("rna_qc", result);
		}
	}

	private void progressCancelled () {
		Enumeration<ProgressListener>en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().progressCancelled();
		}
	}


	public void run() {

		updateProgress("Getting features", 0, 1);
		this.geneFeatures = collection.genome().annotationCollection().getFeaturesForType(geneFeatureName);
		this.transcriptFeatures = collection.genome().annotationCollection().getFeaturesForType(transcriptFeatureName);
		this.rRNAFeatures = collection.genome().annotationCollection().getFeaturesForType(rRNAFeatureName);

		updateProgress("Getting merged genes", 0, 1);
		// Get the merged set of gene locations
		Feature [] mergedGenes = FeatureMerging.getNonOverlappingLocationsForFeatures(geneFeatures, false);


		if (cancel) {
			progressCancelled();
			return;
		}

		updateProgress("Getting merged transcripts", 0, 1);
		// Get the merged set of transcript features
		Feature [] mergedTranscripts = FeatureMerging.getNonOverlappingLocationsForFeatures(transcriptFeatures, true);

		if (cancel) {
			progressCancelled();
			return;
		}


		// Quantitate the genes.
		long [] geneBaseCounts = new long[stores.length];
		
		int [] measuredGenesCounts = new int [stores.length];

		for (int s=0;s<stores.length;s++) {
			updateProgress("Quantitating "+stores[s].name()+" over genes", s, stores.length*4);

			String lastChr = "";
			Chromosome lastChrObject = null;

			for (int f=0;f<mergedGenes.length;f++) {

				//				if (f%1000 == 0) {
				//					updateProgress("Quantitating "+stores[s].name()+" over "+mergedGenes.length+" genes", f, mergedGenes.length);
				//				}

				if (mergedGenes[f].chromosomeName() != lastChr) {
					lastChr = mergedGenes[f].chromosomeName();
					lastChrObject = collection.genome().getExactChromsomeNameMatch(lastChr);
				}

				long [] reads = stores[s].getReadsForProbe(new Probe(lastChrObject, mergedGenes[f].location().packedPosition()));

				// See if we measured anything for this gene
				if (reads.length > 0) {
					++measuredGenesCounts[s];
				}
				
				for (int r=0;r<reads.length;r++) {

					if (cancel) {
						progressCancelled();
						return;
					}


					// Get the length of the overlap
					int overlap = 1 + (Math.min(SequenceRead.end(reads[r]),mergedGenes[f].location().end()) - Math.max(SequenceRead.start(reads[r]),mergedGenes[f].location().start()));
					geneBaseCounts[s] += overlap;
				}

			}
		}

		// Quantitate the transcripts
		long [] transcriptBaseCounts = new long[stores.length];
		long [] transcriptSameStrandBaseCounts = new long[stores.length];
		long [] transcriptOpposingStrandBaseCounts = new long[stores.length];

		for (int s=0;s<stores.length;s++) {
			updateProgress("Quantitating "+stores[s].name()+" over transcripts", s+stores.length, stores.length*4);

			String lastChr = "";
			Chromosome lastChrObject = null;


			for (int f=0;f<mergedTranscripts.length;f++) {

				//				if (f%1000 == 0) {
				//					updateProgress("Quantitating "+stores[s].name()+" over "+mergedTranscripts.length+" genes", f, mergedTranscripts.length);
				//				}

				if (mergedTranscripts[f].chromosomeName() != lastChr) {
					lastChr = mergedTranscripts[f].chromosomeName();
					lastChrObject = collection.genome().getExactChromsomeNameMatch(lastChr);
				}


				long [] reads = stores[s].getReadsForProbe(new Probe(lastChrObject, mergedTranscripts[f].location().packedPosition()));

				for (int r=0;r<reads.length;r++) {

					if (cancel) {
						progressCancelled();
						return;
					}

					// Get the length of the overlap
					int overlap = 1 + (Math.min(SequenceRead.end(reads[r]),mergedTranscripts[f].location().end()) - Math.max(SequenceRead.start(reads[r]),mergedTranscripts[f].location().start()));
					transcriptBaseCounts[s] += overlap;
					if (SequenceRead.strand(reads[r]) == mergedTranscripts[f].location().strand()) {
						transcriptSameStrandBaseCounts[s] += overlap;
					}
					else {
						transcriptOpposingStrandBaseCounts[s] += overlap;
					}
				}

			}
		}

		// Quantitate the rRNA
		long [] rRNABaseCounts = new long[stores.length];

		for (int s=0;s<stores.length;s++) {
			updateProgress("Quantitating "+stores[s].name()+" over rRNAs", s+(stores.length*2), stores.length*4);

			String lastChr = "";
			Chromosome lastChrObject = null;


			for (int f=0;f<rRNAFeatures.length;f++) {

				//				if (f%1000 == 0) {
				//					updateProgress("Quantitating "+stores[s].name()+" over "+mergedrRNAs.length+" genes", f, mergedTranscripts.length);
				//				}

				if (rRNAFeatures[f].chromosomeName() != lastChr) {
					lastChr = rRNAFeatures[f].chromosomeName();
					lastChrObject = collection.genome().getExactChromsomeNameMatch(lastChr);
				}


				long [] reads = stores[s].getReadsForProbe(new Probe(lastChrObject, rRNAFeatures[f].location().packedPosition()));

				for (int r=0;r<reads.length;r++) {

					if (cancel) {
						progressCancelled();
						return;
					}

					// Get the length of the overlap
					int overlap = 1 + (Math.min(SequenceRead.end(reads[r]),rRNAFeatures[f].location().end()) - Math.max(SequenceRead.start(reads[r]),rRNAFeatures[f].location().start()));
					rRNABaseCounts[s] += overlap;
				} 
			}
		}


		// Quantitate the chromosomes
		long [][] chromosomeBaseCounts = new long[chromosomes.length][stores.length];

		for (int s=0;s<stores.length;s++) {
			for (int c=0;c<chromosomes.length;c++) {
				updateProgress("Quantitating "+stores[s].name()+" over "+chromosomes[c].name(), s+(stores.length*3), stores.length*4);

				long [] reads = stores[s].getReadsForChromosome(chromosomes[c]);
				for (int r=0;r<reads.length;r++) {
					chromosomeBaseCounts[c][s] += SequenceRead.length(reads[r]);
				}
			}

		}


		// Finally we make up the data sets we're going to pass back.

		RNAQCResult result = new RNAQCResult(stores);

		double [] percentInGene = new double[stores.length];
		for (int i=0;i<geneBaseCounts.length;i++) {
			percentInGene[i] = (geneBaseCounts[i]/(double)stores[i].getTotalReadLength())*100;
			if (percentInGene[i] > 100) {
				progressWarning("Percent in gene was >100 for "+stores[i]);
				percentInGene[i] = 100;
			}
		}

		result.addPercentageSet("Percent in Gene", percentInGene);

		double [] percentInTranscript = new double[stores.length];
		for (int i=0;i<geneBaseCounts.length;i++) {
			percentInTranscript[i] = (transcriptBaseCounts[i]/(double)geneBaseCounts[i])*100;
			if (percentInTranscript[i] > 100) {
				progressWarning("Percent in exons was >100 for "+stores[i]);
				percentInTranscript[i] = 100;
			}
		}

		result.addPercentageSet("Percent in exons", percentInTranscript);

		double [] percentInrRNA = new double[stores.length];
		for (int i=0;i<rRNABaseCounts.length;i++) {
			percentInrRNA[i] = (rRNABaseCounts[i]/(double)stores[i].getTotalReadLength())*100;
			if (percentInrRNA[i] > 100) {
				progressWarning("Percent in rRNA was >100 for "+stores[i]);
				percentInrRNA[i] = 100;
			}
		}
		
		result.addPercentageSet("Percent in rRNA", percentInrRNA);
		
		
		double [] percentageMeasuredGenes = new double[stores.length];
		for (int i=0;i<measuredGenesCounts.length;i++) {
			percentageMeasuredGenes[i] = measuredGenesCounts[i] /(double)mergedGenes.length*100;
		}

		result.addPercentageSet("Percent Genes Measured", percentageMeasuredGenes);

		// Work out the relative coverage
		double [] percentageOfMaxCoverage = new double[stores.length];

		long maxLength = 0;
		for (int i=0;i<stores.length;i++) {
			if (stores[i].getTotalReadLength() > maxLength) maxLength = stores[i].getTotalReadLength();
		}
		
		for (int i=0;i<stores.length;i++) {
			percentageOfMaxCoverage[i] = (stores[i].getTotalReadLength()*100d)/maxLength;
		}
		
		result.addPercentageSet("Percentage of max data size", percentageOfMaxCoverage);

		

		double [][] percentInChromosomes = new double[chromosomes.length][stores.length];
		for (int c=0;c<percentInChromosomes.length;c++) {
			for (int i=0;i<chromosomeBaseCounts[c].length;i++) {
				percentInChromosomes[c][i] = (chromosomeBaseCounts[c][i]/(double)stores[i].getTotalReadLength())*100;
				if (percentInChromosomes[c][i] > 100) {
					progressWarning("Percent in "+chromosomes[c]+" was >100 for "+stores[i]);
					percentInChromosomes[c][i] = 100;
				}
			}
		}


		for (int c=0;c<percentInChromosomes.length;c++) {
			result.addPercentageSet("Percent in "+chromosomes[c].name(), percentInChromosomes[c]);
		}



		double [] percentOnSenseStrand = new double[stores.length];
		for (int i=0;i<transcriptBaseCounts.length;i++) {
			percentOnSenseStrand[i] = (transcriptSameStrandBaseCounts[i]/(double)transcriptBaseCounts[i])*100;
			if (percentOnSenseStrand[i] > 100) {
				progressWarning("Percent on sense strand was >100 for "+stores[i]);
				percentOnSenseStrand[i] = 100;
			}
		}


		result.addPercentageSet("Percent on sense strand", percentOnSenseStrand);

		progressComplete(result);
	}

}
