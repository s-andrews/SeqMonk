/**
 * Copyright Copyright 2010-18 Simon Andrews
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

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * A report which is based around a class of features where probes associated
 * with each feature are combined in each line of the report.
 */
public class FeatureReport extends Report {

	/** The feature type to use */
	private JComboBox annotationType;
	
	/** Whether we require an exact overlap or just any overlap */
	private JComboBox overlapType;
	
	/** Whether to exclude features with no probes */
	private JComboBox excludes;
	
	/** The options panel. */
	private JPanel optionsPanel = null;
	
	/** The starting list of probes */
	private ProbeList list;

	
	/**
	 * Instantiates a new feature report.
	 * 
	 * @param collection The current data collection
	 * @param storesToAnnotate The list of stores whose data can be added to the report
	 */
	public FeatureReport(DataCollection collection, DataStore[] storesToAnnotate) {
		super(collection, storesToAnnotate);
		list = collection.probeSet().getActiveList();
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#getOptionsPanel()
	 */
	public JPanel getOptionsPanel () {

		if (optionsPanel != null) return optionsPanel;
		
		
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel,BoxLayout.Y_AXIS));
		
		JPanel choicePanel1 = new JPanel();
		
		choicePanel1.add(new JLabel("Annotate "));
		annotationType = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		annotationType.setPrototypeDisplayValue("No longer than this please");
		choicePanel1.add(annotationType);		
		optionsPanel.add(choicePanel1);

		
		JPanel choicePanel2 = new JPanel();
		
		choicePanel2.add(new JLabel("With "));
		overlapType = new JComboBox(new String [] {"any overlapping","exactly overlapping"});
		choicePanel2.add(overlapType);
		choicePanel2.add(new JLabel(" probes"));
		optionsPanel.add(choicePanel2);
				
		JPanel choicePanel3 = new JPanel();
		excludes = new JComboBox(new String [] {"Exclude","Include"});
		choicePanel3.add(excludes);
		choicePanel3.add(new JLabel(" features with no probes"));
		optionsPanel.add(choicePanel3);

		return optionsPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#name()
	 */
	public String name () {
		return "Feature Report";
	}
	
	/**
	 * A class representing a feature and the probes associated with it.
	 */
	private class FeatureAnnotation {
		
		/** The probes. */
		public Vector<Probe> probes;
		
		/** The feature. */
		private Feature feature;
		
		/** The feature subLocations which can be matched. */
		private Location [] subLocations;
		
		/**
		 * Instantiates a new feature annotation.
		 * 
		 * @param feature
		 */
		public FeatureAnnotation (Feature feature) {
			probes = new Vector<Probe>();
			this.feature = feature;
			if (feature.location() instanceof SplitLocation) {
				subLocations = ((SplitLocation)feature.location()).subLocations();
			}
			else {
				subLocations = new Location [] {feature.location()};
			}
		}
		
		/**
		 * Tries to associate a new probe with this feature.  Can be
		 * called safely with all probes and only valid matches will
		 * be kept.
		 * 
		 * @param p The probe to add
		 * @param onlyExactOverlaps Whether the probes position must match exactly with that of the feature (or its sublocation) or can just overlap it.
		 */
		public void addProbe (Probe p, boolean onlyExactOverlaps) {
			
			if (onlyExactOverlaps) {
				// Check if it is enclosed
				if (p.start()<feature.location().start() || p.end()>feature.location().end()) {
					// It's not contained in this feature
					return;
				}
				
				// Now we check for an exact overlap either with the entire
				// feature or the individual sublocations
				if (p.start() == feature.location().start() && p.end() == feature.location().end()) {
					probes.add(p);
					return;
				}
				
				for (int s=0;s<subLocations.length;s++) {
					if (subLocations[s].start() == p.start() && subLocations[s].end() == p.end()) {
						probes.add(p);
						return;
					}					
				}
				
			}
			else {
				if (p.end()>= feature.location().start() && p.start() <= feature.location().end()) {
					probes.add(p);
				}
			}
			
		}
				
		/**
		 * Probes.
		 * 
		 * @return The set of probes associated with this feature.
		 */
		public Probe [] probes () {
			return probes.toArray(new Probe[0]);
		}
		
		/**
		 * Feature.
		 * 
		 * @return The feature this object represents
		 */
		public Feature feature () {
			return feature;
		}
				
		/**
		 * Gets the annotation value.
		 * 
		 * @return The average probe list value for all probes associated with this feature.
		 */
		public double [] getAnnotationValues () {
			double [] value = new double[list.getValueNames().length];
			int count = 0;
			
			Enumeration<Probe> e = probes.elements();
			while (e.hasMoreElements()) {
				Probe p = e.nextElement();
				float [] d = list.getValuesForProbe(p);
				if (d != null) {
					for (int i=0;i<d.length;i++) {
						value[i] += d[i];
					}
					++count;
				}
			}
			if (count > 0) {
				for (int i=0;i<value.length;i++) {
					value[i] /= count;
				}
				return value;
			}
			else {
				return null;
			}
		}
				
		/**
		 * Number of probes.
		 * 
		 * @return How many probes are associated with this feature.
		 */
		public int numberOfProbes () {
			return probes.size();
		}
				
	}
	
	/**
	 * A table model representing the results of this report.
	 */
	private class AnnotationTableModel extends AbstractTableModel  {
		
		private FeatureAnnotation [] data;
		
		/** The data stores whose data will be added to the report */
		private DataStore [] stores;
		
		/**
		 * Instantiates a new annotation table model.
		 * 
		 * @param data The data collection to use
		 * @param stores The dataStores whose data will be added to the report.
		 */
		public AnnotationTableModel (FeatureAnnotation [] data, DataStore [] stores) {
//			System.err.println("Created table with "+data.length+" rows");
			this.data = data;
			this.stores = stores;
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
			return 10+(stores.length*2);
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "Chr";
			case 1: return "Start";
			case 2: return "End";
			case 3: return "Strand";
			case 4: return "Feature";
			case 5: return "ID";
			case 6: return "Description";
			case 7: return "No. Sublocations";
			case 8: return "No. Probes";
			default:
				System.err.println("Index is "+c+" values is "+list.getValueNames().length+" stores is "+stores.length);
				if (c < 9+list.getValueNames().length) {
					return(list.getValueNames()[c-9]);
				}
				if ((c-(9+list.getValueNames().length)) % 2 == 0) {
					return "Mean "+stores[(c-(8+list.getValueNames().length))/2].name();
				}
				else {
					return "StDev "+stores[(c-(9+list.getValueNames().length))/2].name();					
				}
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
			case 3: return String.class;
			case 4: return String.class;
			case 5: return String.class;
			case 6: return String.class;
			case 7: return Integer.class;
			case 8: return Integer.class;
			default: return Float.class;
			}
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0 :
				return data[r].feature().chromosomeName();
			
			case 1:
				return new Integer(data[r].feature().location().start());
				
			case 2:
				return new Integer(data[r].feature().location().end());
				
			case 3:
				if (data[r].feature().location().strand() == Location.FORWARD) return "+";
				if (data[r].feature().location().strand() == Location.REVERSE) return "-";
				if (data[r].feature().location().strand() == Location.UNKNOWN) return "";
			
			case 4:
				return data[r].feature().name();

			case 5:
				return data[r].feature().id();
	
			case 6:
				return data[r].feature().description();

			case 7:
				if (data[r].feature().location() instanceof SplitLocation) {
					return ((SplitLocation)data[r].feature().location()).subLocations().length;					
				}
				return 1;

				
			case 8:
				return new Integer(data[r].numberOfProbes());
			
				
			default:
				Probe [] theseProbes = data[r].probes();
				
				if (c < 9+list.getValueNames().length) {
					return(data[r].getAnnotationValues()[c-(8+list.getValueNames().length)]);
				}

				
				double [] values = new double [theseProbes.length];
				for (int i=0;i<theseProbes.length;i++) {
					try {
						values[i] = stores[(c-(9+list.getValueNames().length))/2].getValueForProbe(theseProbes[i]);
					} 
					catch (SeqMonkException e) {}
				}
				
				if ((c-(9+list.getValueNames().length)) % 2 == 0) {
					return new Float(SimpleStats.mean(values));
				}
				else {
					return new Float(SimpleStats.stdev(values));					
				}
			}
		}
		
	}
	
	public boolean canExportGFF () {
		return true;
	}
	
	public int chromosomeColumn () {
		return 0;
	}

	public int startColumn () {
		return 11;
	}
	
	public int endColumn () {
		return 2;
	}

	public int strandColumn () {
		return -1;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#generateReport()
	 */
	@Override
	public void generateReport() {
		Thread t = new Thread(this);
		t.start();
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
		String annotationTypeValue = (String)annotationType.getSelectedItem();
		
		// Check what to do with unannotated features
		boolean includeAll = true;
		if (((String)excludes.getSelectedItem()).equals("Exclude")) {
			includeAll = false;
		}
		
		// Check if we're only interested in exact overlaps
		boolean onlyExactOverlaps = false;
		if (((String)(overlapType.getSelectedItem())).equals("exactly overlapping")) {
			onlyExactOverlaps = true;
		}
									
		Vector<FeatureAnnotation> annotations = new Vector<FeatureAnnotation>();
		
		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.
		
		Chromosome [] chrs = dataCollection().genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			progressUpdated("Processing Chr"+chrs[c].name(), c, chrs.length);
			
			Probe [] probes = collection.probeSet().getActiveList().getProbesForChromosome(chrs[c]);
			Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chrs[c],annotationTypeValue);
			
			FeatureAnnotation [] thisChrFeatureAnnotations = new FeatureAnnotation [features.length];
			for (int f=0;f<features.length;f++) {
				
				if (cancel) {
					progressCancelled();
					return;
				}
				
				thisChrFeatureAnnotations[f] = new FeatureAnnotation(features[f]);
			}
						
			// We can now step through the probes looking for a match
			for (int p=0;p<probes.length;p++) {
				
//				if (p%1000 == 0) {
//					System.err.println("Processed "+p+" probes");
//				}

				for (int f=0;f<thisChrFeatureAnnotations.length;f++) {
					
					if (cancel) {
						progressCancelled();
						return;
					}
					
					// This method will silently reject any probes it doesn't like
					thisChrFeatureAnnotations[f].addProbe(probes[p],onlyExactOverlaps);
				}
			}
			
			// Now we add the features we want to keep to the overall collection
			for (int f=0;f<thisChrFeatureAnnotations.length;f++) {
				if (includeAll || thisChrFeatureAnnotations[f].numberOfProbes() > 0) {
					annotations.add(thisChrFeatureAnnotations[f]);
				}
			}
			
		}
						
		TableModel model = new AnnotationTableModel(annotations.toArray(new FeatureAnnotation[0]),storesToAnnotate);

		reportComplete(model);
		
	}


	
}
