/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Reports;

import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * A report which provides basic statistics for a set of dataStores
 */
public class DataStoreSummaryReport extends Report {
	
	/** The summaries. */
	private StoreSummary [] summaries;
	
	/**
	 * Instantiates a new data store summary report.
	 * 
	 * @param collection The current dataCollection
	 * @param storesToAnnotate The stores to annotate
	 */
	public DataStoreSummaryReport(DataCollection collection,DataStore[] storesToAnnotate) {
		super(collection, storesToAnnotate);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#getOptionsPanel()
	 */
	public JPanel getOptionsPanel () {
		return null;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#generateReport()
	 */
	public void generateReport () {
				
		Thread t = new Thread(this);
		t.start();
				
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#name()
	 */
	public String name () {
		return "DataStore Summary Report";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#isReady()
	 */
	@Override
	public boolean isReady() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		summaries = new StoreSummary[storesToAnnotate.length];
		
		Probe [] probes;
		if (dataCollection().probeSet() != null) {
			probes = dataCollection().probeSet().getActiveList().getAllProbes();
		}
		else {
			probes = new Probe[0];
		}

		for (int s=0;s<storesToAnnotate.length;s++) {

			summaries[s] = new StoreSummary(storesToAnnotate[s]);
			summaries[s].forwardReads = storesToAnnotate[s].getReadCountForStrand(Location.FORWARD);
			summaries[s].reverseReads = storesToAnnotate[s].getReadCountForStrand(Location.REVERSE);
			summaries[s].unknownReads = storesToAnnotate[s].getReadCountForStrand(Location.UNKNOWN);
			summaries[s].totalReads = storesToAnnotate[s].getTotalReadCount();
			summaries[s].totalReadLength = storesToAnnotate[s].getTotalReadLength();
			
			progressUpdated("Processing "+storesToAnnotate[s].name(), s, storesToAnnotate.length);
			
			
			// Now work out the mean read length
			summaries[s].medianReadLength = (int)(summaries[s].totalReadLength/summaries[s].totalReads);
			
			// ..and the fold coverage
			summaries[s].foldCoverage = ((double)summaries[s].totalReadLength)/dataCollection().genome().getTotalGenomeLength();
			
			// Now go through all of the quantitations to work out the stats for those
			
			// If we don't have any probes then we don't need to do this			
			if (probes.length > 0) {
				float [] values = new float[probes.length];
				for (int p=0;p<probes.length;p++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}

					
					try {
						values[p] = storesToAnnotate[s].getValueForProbe(probes[p]);
						if (!(Float.isNaN(values[p]) || Float.isInfinite(values[p]))) {
							summaries[s].totalQuantitation+=values[p];
							summaries[s].validQuantiationCount++;
						}
					} 
					catch (SeqMonkException e) {
						values[p] = Float.NaN;
					}
				}
				
				// Now work out the stats
				summaries[s].meanQuantitation = SimpleStats.mean(values);
				summaries[s].medianQuantitation = SimpleStats.median(values);
			}
			else {
				summaries[s].meanQuantitation = 0;
				summaries[s].medianQuantitation = 0;				
			}
		}
		
		TableModel model = new AnnotationTableModel(summaries);
		reportComplete(model);
	}

	/**
	 * A set of statistics for a single dataStore
	 */
	private class StoreSummary {

		/** The store. */
		public DataStore store;
		
		/** The total read count. */
		public int totalReads;
		
		/** The forward read count. */
		public int forwardReads;
		
		/** The reverse read count. */
		public int reverseReads;
		
		/** The unknown read count. */
		public int unknownReads;
		
		/** The total length of all reads in bases */
		public long totalReadLength;
		
		/** The median read length. */
		public int medianReadLength;
		
		/** The fold coverage relative to the overall genome length */
		public double foldCoverage;
		
		/** The sum of all current quantitation values */
		public double totalQuantitation;
		
		/** The median quantitation value. */
		public float medianQuantitation;
		
		/** The mean quantitation value */
		public float meanQuantitation;
		
		public int validQuantiationCount = 0;
		
		/**
		 * Instantiates a new store summary.
		 * 
		 * @param store The DataStore to summarise
		 * 
		 */
		public StoreSummary (DataStore store) {
			this.store = store;
		}
	}

	/**
	 * A TableModel representing the results of the DataStore summary
	 */
	private class AnnotationTableModel extends AbstractTableModel  {
		
		private StoreSummary [] data;
		
		/**
		 * Instantiates a new annotation table model.
		 * 
		 * @param data The set of pre-calculated summaries
		 */
		public AnnotationTableModel (StoreSummary [] data) {
			this.data = data;
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return data.length;
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 12;
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "DataStore";
			case 1: return "Total Read Count";
			case 2: return "Forward Read Count";
			case 3: return "Reverse Read Count";
			case 4: return "Unknown Read Count";
			case 5: return "Mean Read Length";
			case 6: return "Total Read Length";
			case 7: return "Fold Coverage";
			case 8: return "Total Quantitation";
			case 9: return "Median Quantitation";
			case 10: return "Mean Quantitation";
			case 11: return "Valid Quantitations";
			default: return null;
			}
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Class getColumnClass (int c) {
			switch (c) {
			case 0: return String.class;
			case 1: return Integer.class;
			case 2: return Integer.class;
			case 3: return Integer.class;
			case 4: return Integer.class;
			case 5: return Double.class;
			case 6: return Long.class;
			case 7: return Float.class;
			case 8: return Float.class;
			case 9: return Float.class;
			case 10: return Float.class;
			case 11: return Integer.class;
			default: return null;
			}
		}
	
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0 :
				return data[r].store.name();
			
			case 1:
				return data[r].totalReads;
				
			case 2:
				return data[r].forwardReads;
			
			case 3:
				return data[r].reverseReads;
				
			case 4:
				return data[r].unknownReads;
				
			case 5:
				return data[r].medianReadLength;
	
			case 6:
				return data[r].totalReadLength;

			case 7:
				return data[r].foldCoverage;
				
			case 8:
				return data[r].totalQuantitation;
			
			case 9:
				return data[r].medianQuantitation;
	
			case 10:
				return data[r].meanQuantitation;

			case 11:
				return data[r].validQuantiationCount;

			default:
				return null;
			}
		}
		
	}
}
