/**
 * Copyright Copyright 2010- 21 Simon Andrews
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

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HiCInteractionStrengthCalculator;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;

/**
 * A quantitation based on the enrichment of 4C reads from a HiC dataset
 */
public class FourCEnrichmentQuantitation extends Quantitation {

	
//	/** Do we get our total read count for correction just from within the probes we're quantitating. */
//	private boolean correctOnlyInProbes;
		
	private JPanel optionPanel;
	
	private HiCDataStore [] hiCData;
	private DataSet [] data;
		

	public FourCEnrichmentQuantitation(SeqMonkApplication application) {
		super(application);
		
		// List all of the HiC stores so we can select the ones we need
		// for our analysis later
		
		DataStore [] allStores = application.dataCollection().getAllDataStores();
		
		Vector<HiCDataStore> hiCDataStores = new Vector<HiCDataStore>();
		
		for (int d=0;d<allStores.length;d++) {
			if (allStores[d] instanceof HiCDataStore && ((HiCDataStore)allStores[d]).isValidHiC()) {
				hiCDataStores.add((HiCDataStore)allStores[d]);
			}
		}
		
		hiCData = hiCDataStores.toArray(new HiCDataStore[0]);
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore [] data) {
		
		// Find the 4C looking data stores amongst the set we've been given 
		
		Vector<DataSet> fourCStores = new Vector<DataSet>();
		for (int d=0;d<data.length;d++) {
			if (data[d] instanceof HiCDataStore && ((HiCDataStore)data[d]).isValidHiC()) {
				progressWarningReceived(new SeqMonkException("Skipping "+data[d]+" which was HiC data"));
				continue;
			}
			else if (! (data[d] instanceof DataSet)) {
				// We can only analyse direct 4C data sets
				progressWarningReceived(new SeqMonkException("Skipping "+data[d]+" which wasn't a DataSet"));
				continue;
			}
			else if (! ((DataSet)data[d]).fileName().startsWith("HiC other end")) {
				progressWarningReceived(new SeqMonkException("Skipping "+data[d]+" which didn't look like a 4C data set"));
				continue;
			}
			
			fourCStores.add((DataSet)data[d]);
			
		}
		
		this.data = fourCStores.toArray(new DataSet[0]);
		
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
		
		if (optionPanel != null) {
			// We've done this already
			return optionPanel;
		}
		
		optionPanel = new JPanel();
		optionPanel.setLayout(new BorderLayout());
		optionPanel.add(new JLabel("No Options",JLabel.CENTER));
		
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	
	public String description () {
		StringBuffer sb = new StringBuffer();
		sb.append("4C Enrichment Quantitaiton");
		
		return sb.toString();
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		// Start off by finding the right HiC data for each 4C dataset
		HiCDataStore [] parentHiC = new HiCDataStore[data.length];
		Probe [] parentProbes = new Probe[data.length];
		ProbeList [] significantLists = new ProbeList[data.length];
		
		for (int d=0;d<data.length;d++) {
			String filename = data[d].fileName();
			filename = filename.replaceAll("HiC other end of ", "");
			filename = filename.replaceAll(" for region.*", "");
			
			System.err.println("Looking for HiC match to "+filename);
			
			for (int h=0;h<hiCData.length;h++) {
				if (((DataStore)hiCData[h]).name().equals(filename)) {
					parentHiC[d] = hiCData[h]; 
				}
			}
			
			if (parentHiC[d] == null) {
				progressWarningReceived(new SeqMonkException("Failed to find HiC dataset '"+filename+"' for 4C dataset "+data[d].name()));
				continue;
			}
			
			significantLists[d] = new ProbeList(application.dataCollection().probeSet(), "4C p<0.05 "+data[d].name(), "4C pipeline significance < 0.05", "P-value");
			
			// Also make up a probe to represent the region from which
			// the dataset was made
			filename = data[d].fileName();
			filename = filename.replaceAll("^.*for region ", "");
			
			String [] locationSections = filename.split("[-:]");
			if (locationSections.length != 3) {
				progressWarningReceived(new SeqMonkException("Couldn't extract location from "+filename));
				continue;
			}
			
			try {
				parentProbes[d] = new Probe(application.dataCollection().genome().getChromosome(locationSections[0]).chromosome(), Integer.parseInt(locationSections[1]),Integer.parseInt(locationSections[2]));
			} 
			catch (Exception e) {
				progressExceptionReceived(e);
				return;
			}
			
		}
		
		// Make strength calculators for each HiC set
		HiCInteractionStrengthCalculator [] strengthCalculators = new HiCInteractionStrengthCalculator[parentHiC.length];
		for (int i=0;i<parentHiC.length;i++) {
			strengthCalculators[i] = new HiCInteractionStrengthCalculator(parentHiC[i], true);
		}
		
		// Get the cis/trans counts for the parent probes
		int [] parentProbeCisCounts = new int[parentHiC.length];
		int [] parentProbeTransCounts = new int[parentHiC.length];
		
		for (int p=0;p<parentHiC.length;p++) {
			if (parentHiC[p] == null) continue;
			HiCHitCollection hits = parentHiC[p].getHiCReadsForProbe(parentProbes[p]);
			
			String [] chrNames = hits.getChromosomeNamesWithHits();
			for (int c=0;c<chrNames.length;c++) {
				if (chrNames[c].equals(hits.getSourceChromosomeName())) {
					parentProbeCisCounts[p] = hits.getSourcePositionsForChromosome(chrNames[c]).length;
				}
				else {
					parentProbeTransCounts[p] += hits.getSourcePositionsForChromosome(chrNames[c]).length;					
				}
			}
		}
		
		
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();
				
		for (int p=0;p<probes.length;p++) {
			
			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}
			

			progressUpdated(p, probes.length);
			
			for (int d=0;d<data.length;d++) {
				
				if (parentHiC[d] == null) continue;
				
				int thisProbeCisCount = 0;
				int thisProbeTransCount = 0;
				
				HiCHitCollection hiCHits = parentHiC[d].getHiCReadsForProbe(probes[p]);
				String [] chrNames = hiCHits.getChromosomeNamesWithHits();
				for (int c=0;c<chrNames.length;c++) {
					if (chrNames[c].equals(hiCHits.getSourceChromosomeName())) {
						thisProbeCisCount = hiCHits.getSourcePositionsForChromosome(chrNames[c]).length;
					}
					else {
						thisProbeTransCount += hiCHits.getSourcePositionsForChromosome(chrNames[c]).length;					
					}
				}
				strengthCalculators[d].calculateInteraction(data[d].getReadsForProbe(probes[p]).length, thisProbeCisCount, thisProbeTransCount, parentProbeCisCounts[d], parentProbeTransCounts[d], probes[p], parentProbes[d]);
				
				float obsExp = (float)strengthCalculators[d].obsExp();
				data[d].setValueForProbe(probes[p], obsExp);
				float pValue = (float)strengthCalculators[d].rawPValue() * probes.length;
				
				if (pValue < 0.05) {
					significantLists[d].addProbe(probes[p], pValue);
				}
				
			}
			
		}

		quantitatonComplete();
		
	}
	

	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "4C Enrichment Quantitation";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return false;
	}
	
	public boolean canUseExistingQuantitation () {
		// We set this so that the quantitation applies only to a subset of
		// the data stores which can use this.  This breaks our quantitaiton
		// model somewhat but it's a useful thing to be able to do, otherwise
		// we can't keep any quantitation for anything which isn't a 4C store
		return true;
	}

}
