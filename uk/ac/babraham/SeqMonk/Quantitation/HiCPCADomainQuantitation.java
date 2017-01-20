/**
 * Copyright Copyright 2010-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Quantitation;

import java.util.Vector;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.PCA;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.InteractionClusterMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.HiCHeatmap.HeatmapOptionsPanel;

/**
 * A quantitation to calculate PCA eigenvalues for the principal
 * component of a per-chromosome correlation matrix.  This is a
 * method adapted from the Kalhor et al Nature paper doi:10.1038/nbt.2057
 */
public class HiCPCADomainQuantitation extends Quantitation implements ProgressListener {

	private HeatmapOptionsPanel optionsPanel = new HeatmapOptionsPanel(0, 0, 1f, 0.05f, 3, true);

	/** The stores we're going to quantitate. */
	private HiCDataStore [] data;
	
	/** Some variables we use to get updates reported correctly **/
	HiCDataStore currentStore;
	Chromosome currentChromosome;
	int current;
	int total;

	/** Some flags for the progress updates **/
	private boolean wait = false;
//	private Exception error = null;
	

	public HiCPCADomainQuantitation(SeqMonkApplication application) {
		super(application);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {

		Vector<HiCDataStore> hiCDataStores = new Vector<HiCDataStore>();
		for (int d=0;d<data.length;d++) {
			if (data[d] instanceof HiCDataStore && ((HiCDataStore)data[d]).isValidHiC()) {
				hiCDataStores.add((HiCDataStore)data[d]);
			}
		}

		this.data = hiCDataStores.toArray(new HiCDataStore[0]);


		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	public boolean requiresHiC () {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {

		return optionsPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}

	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("HiC PCA Domain Quantiation ");

		sb.append(" min strength ");
		sb.append(optionsPanel.minStrength());

		sb.append(" min distance ");
		sb.append(optionsPanel.minDistance());

		sb.append(" max significance ");
		sb.append(optionsPanel.maxSignificance());

		if (optionsPanel.correctLinkage()) {
			sb.append(" correcting for physical linkage");
		}
		
		return sb.toString();
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		// We're going to go through the probes one chromosome at a time so we
		// can reduce the complexity we have to deal with

		Chromosome [] chromosomes = application.dataCollection().genome().getAllChromosomes();
		
		for (int c=0;c<chromosomes.length;c++) {
						
			if (cancel) {
				progressCancelled();
				return;
			}
			
			
			currentChromosome = chromosomes[c];
			
			Probe [] probes = application.dataCollection().probeSet().getProbesForChromosome(chromosomes[c]);
			
			if (probes.length < 5) {
				// It's not worth trying to find domains
				for (int d=0;d<data.length;d++) {
					for (int p=0;p<probes.length;p++) {
						((DataStore)data[d]).setValueForProbe(probes[p], 0f);
					}
				}
				
				continue;
			}

			ProbeList thisChrProbes = new ProbeList(application.dataCollection().probeSet(), chromosomes[c].name(), "", null);

			for (int p=0;p<probes.length;p++) {
				thisChrProbes.addProbe(probes[p], 0f);
			}

			
			for (int d=0;d<data.length;d++) {
				
				if (cancel) {
					progressCancelled();
					return;
				}
			
				currentStore = data[d];
				current = (d*c)+d;
				total = chromosomes.length*data.length;
				
				progressUpdated("Processing chromosome "+chromosomes[c].name()+" for "+data[d].name(), (d*c)+d, chromosomes.length*data.length);
				
				HeatmapMatrix matrix = new HeatmapMatrix(data[d], new ProbeList[]{thisChrProbes}, application.dataCollection().genome(), optionsPanel.minDistance(), optionsPanel.maxDistance(), optionsPanel.minStrength(), optionsPanel.maxSignificance(), optionsPanel.minAbsolute(), optionsPanel.correctLinkage());

				matrix.addProgressListener(this);
				
				wait = true;
				
				matrix.startCalculating();
				
				while (wait) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				
				// Now the matrix is calculated we can extract the correlation matrix from it.
				
				if (cancel) {
					progressCancelled();
					return;
				}

				InteractionClusterMatrix clusterMatrix = new InteractionClusterMatrix(matrix.filteredInteractions(), probes.length);
				
				clusterMatrix.addListener(this);
				
				wait = true;
				clusterMatrix.startCorrelating();
				
				while (wait) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				
				float [][] correlationMatrix = clusterMatrix.correlationMatix();
				
				// Annoyingly the PCA needs a double [][]
				
				double [][] correlationMatrixDouble = new double [correlationMatrix.length][];
				
				for (int i=0;i<correlationMatrix.length;i++) {
					double [] db = new double[correlationMatrix[i].length];
					for (int j=0;j<db.length;j++) {
						db[j] = correlationMatrix[i][j];
					}
					correlationMatrixDouble[i] = db;
				}
				
				// Now we can calculate the PCA values from the correlation matrix
				
				PCA pca = new PCA(correlationMatrixDouble);
				
				pca.addProgressListener(this);
				
				wait = true;
				
				pca.startCalculating();
				
				while (wait) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {}
				}
				
				double [] extractedEigenValues = pca.extractedEigenValues();
				
				// We can now assign these eigenvalues to the probe quantitations
				// for these probes
				for (int p=0;p<probes.length;p++) {
					((DataStore)data[d]).setValueForProbe(probes[p], (float)extractedEigenValues[p]);
				}
				
			}
			
			thisChrProbes.delete();
			
		}

		
		

		quantitatonComplete();

	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "HiC PCA Domain Quantitation";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}

	public void progressExceptionReceived(Exception e) {
		super.progressExceptionReceived(e);
		e.printStackTrace();
	}
	
	public void progressUpdated (String message, int current, int total) {
		if (wait) {
			// This is a message from a waited process, so we'll ammend it
			message = "Chr "+currentChromosome.name()+" "+currentStore.name()+" "+message;
			current = this.current;
			total = this.total;
		}
		super.progressUpdated(message, current, total);
	}
	
	public void progressCancelled () {
		super.progressCancelled();
	}
	
	
	public void progressWarningReceived(Exception e) {
		// TODO Auto-generated method stub
		
	}

	public void progressComplete(String command, Object result) {
		wait = false;		
	}

}
