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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The ProbeGroup report allows closely positions probes to be grouped
 * together into longer candidate regions.  These regions are annotated
 * with one or more features and with quantiated data from a set of
 * DataStores.
 */
public class ProbeGroupReport extends Report implements KeyListener, ItemListener {

	/** The relative position of features to annotate with */
	private JComboBox annotationPosition;
	
	/** The annotation type. */
	private JComboBox annotationType;
	
	/** Whether to exclude unannotated probes. */
	private JComboBox excludes;
	
	/** Whether to include data alongside annotation. */
	private JComboBox data;
	
	/** How far away features can be and still be used */
	private JTextField annotationLimit;
	
	/** How close probes must be to merge them together */
	private JTextField probeDistanceLimit;
	
	/** The starting probe list */
	private ProbeList list;
	
	/** The options panel. */
	private JPanel optionsPanel = null;
	
	/** A cached set of features */
	Feature [] cachedFeatures = null;
	String lastChromsomeFeaturesLoaded = null;

	/**
	 * Instantiates a new probe group report.
	 * 
	 * @param collection The current dataCollection
	 * @param storesToAnnotate The list of dataStores whose data can be included
	 */
	public ProbeGroupReport(DataCollection collection,DataStore[] storesToAnnotate) {
		super(collection, storesToAnnotate);
		this.list = collection.probeSet().getActiveList();
	}	
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#getOptionsPanel()
	 */
	public JPanel getOptionsPanel () {
	
		if (optionsPanel != null) return optionsPanel;
			
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel,BoxLayout.Y_AXIS));
		
		JPanel choicePanel1 = new JPanel();
		
		choicePanel1.add(new JLabel("Annotate with "));
		annotationPosition = new JComboBox(new String [] {"[Don't annotate]","all overlapping","all enclosed","closest", "surrounding", "upstream", "downstream", "surrounding or upstream", "surrounding or downstream"});
		annotationPosition.addItemListener(this);
		choicePanel1.add(annotationPosition);
		annotationType = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
		annotationType.setPrototypeDisplayValue("No longer than this please");
		choicePanel1.add(annotationType);
		optionsPanel.add(choicePanel1);
		
		
		JPanel choicePanel2 = new JPanel();
		choicePanel2.add(new JLabel("Annotation distance cutoff "));
		annotationLimit = new JTextField("2000",7);
		annotationLimit.addKeyListener(this);
		annotationLimit.setEnabled(false);
		choicePanel2.add(annotationLimit);
		choicePanel2.add(new JLabel("bp"));
		optionsPanel.add(choicePanel2);
		
		JPanel choicePanel3 = new JPanel();
		excludes = new JComboBox(new String [] {"Include","Exclude"});
		choicePanel3.add(excludes);
		choicePanel3.add(new JLabel(" unannotated probes"));
		optionsPanel.add(choicePanel3);
		
		JPanel choicePanel4 = new JPanel();
		data = new JComboBox(new String [] {"Include","Don't include"});
		choicePanel4.add(data);
		choicePanel4.add(new JLabel(" data for currently visible stores"));
		optionsPanel.add(choicePanel4);
		
		JPanel choicePanel5 = new JPanel();
		choicePanel5.add(new JLabel("Group probes within "));
		probeDistanceLimit = new JTextField("5000",7);
		probeDistanceLimit.addKeyListener(this);
		choicePanel5.add(probeDistanceLimit);
		choicePanel5.add(new JLabel(" bp"));
		optionsPanel.add(choicePanel5);
		
		return optionsPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#name()
	 */
	public String name () {
		return "Probe Group Report";
	}
	
	/**
	 * Adds the annotation to a set of closely positioned probes
	 * 
	 * @param g The group of probes to annotate
	 * @param justEnclosed Flag to say whether to only use features contained within the region
	 * @param featureType The class of features to annotate with
	 */
	private void annotateGroup (ProbeGroupAnnotation g, boolean justEnclosed, String featureType) {

		if (g.chromosome() == null) return;
				
		if (!g.chromosome().name().equals(lastChromsomeFeaturesLoaded)) {
			cachedFeatures = collection.genome().annotationCollection().getFeaturesForType(g.chromosome(),featureType);
			lastChromsomeFeaturesLoaded = g.chromosome().name();
		}
				
		for (int f=0;f<cachedFeatures.length;f++) {
			
			if (cachedFeatures[f].location().start() > g.end()) {
				return;
			}

			if (justEnclosed) {
				if (cachedFeatures[f].location().start()>=g.start() && cachedFeatures[f].location().end()<=g.end()) {
					g.addFeature(cachedFeatures[f]);
				}
			}
			else {
				if (cachedFeatures[f].location().start()<g.end() && cachedFeatures[f].location().end()>g.start()) {
					g.addFeature(cachedFeatures[f]);
				}				
			}
		}

	}
	


	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent ke) {

	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent ke) {
		JTextField f = (JTextField)ke.getSource();
		
		char [] c = f.getText().toCharArray();
		
		StringBuffer b = new StringBuffer();
		for (int i=0;i<c.length;i++) {
			
			if (Character.isDigit(c[i])) {
				b.append(c[i]);
				continue;
			}

			f.setText(b.toString());
			break;
		}
		
		
		optionsChanged();
	}
	
	/**
	 * A class to represent a set of features annotating a group of probes.
	 */
	private class ProbeGroupAnnotation {
		
		/** The probes. */
		public Vector<Probe> probes;
		
		/** The features. */
		public Vector<Feature> features;
		
		/**
		 * Instantiates a new probe group annotation.
		 */
		public ProbeGroupAnnotation () {
			probes = new Vector<Probe>();
			features = new Vector<Feature>();
		}
		
		/**
		 * Adds a probe to the set.
		 * 
		 * @param p The probe to add
		 */
		public void addProbe (Probe p) {
			probes.add(p);
		}
		
		/**
		 * Adds an annotation feature
		 * 
		 * @param f The feature to add
		 */
		public void addFeature (Feature f) {
			if (! features.contains(f))
				features.add(f);
		}
		
		/**
		 * Start.
		 * 
		 * @return The start position of the first probe in the group.
		 */
		public int start () {
			Enumeration<Probe> e = probes.elements();
			int start = 0;
			while (e.hasMoreElements()) {
				Probe p = e.nextElement();
				if (start == 0) {
					start = p.start();
				}
				
				if (p.start() < start)
					start = p.start();
			}
			
			return start;
		}
		
		/**
		 * Probes.
		 * 
		 * @return The set of probes in the group
		 */
		public Probe [] probes () {
			return probes.toArray(new Probe[0]);
		}
		
		/**
		 * End.
		 * 
		 * @return The end position of the last probe in the group.
		 */
		public int end () {
			Enumeration<Probe> e = probes.elements();
			int end = 0;
			while (e.hasMoreElements()) {
				Probe p = e.nextElement();
				if (end == 0) {
					end = p.end();
				}
				
				if (p.end() > end)
					end = p.end();
			}
			
			return end;
		}
		
		/**
		 * Gets the annotation value.
		 * 
		 * @return The average probe list value for all probes in this set.
		 */
		public Double getAnnotationValue () {
			double value = 0;
			int count = 0;
			
			Enumeration<Probe> e = probes.elements();
			while (e.hasMoreElements()) {
				Probe p = e.nextElement();
				Float d = list.getValuesForProbe(p);
				if (d != null) {
					value += d.doubleValue();
					++count;
				}
			}
			if (count > 0) {
				return new Double(value/count);
			}
			else {
				return new Double(Double.NaN);
			}
		}
		
		/**
		 * Chromosome.
		 * 
		 * @return The chromosome of all probes in this set
		 */
		public Chromosome chromosome () {
			if (probes.size()>0) {
				return probes.elementAt(0).chromosome();
			}
			else {
				return null;
			}
		}
		
		/**
		 * Number of probes.
		 * 
		 * @return The number of probes
		 */
		public int numberOfProbes () {
			return probes.size();
		}
		
		/**
		 * Feature names.
		 * 
		 * @return A concatonated list of feature names separated by spaces.
		 */
		public String featureNames () {
			StringBuffer b = new StringBuffer();
			Enumeration<Feature> e = features.elements();
			while (e.hasMoreElements()) {
				b.append(e.nextElement().name());
				b.append(" ");
			}
			return b.toString();
		}
		
		/**
		 * Feature descriptions.
		 * 
		 * @return A concatonated list of descriptions of features separated by spaces
		 */
		public String featureDescriptions () {
			StringBuffer b = new StringBuffer();
			Enumeration<Feature> e = features.elements();
			while (e.hasMoreElements()) {
				b.append(e.nextElement().description());
				b.append(" ");
			}
			return b.toString();
			
		}
		
	}
	
	/**
	 * The A TableModel which represents the results of the ProbeGroupReport
	 */
	private class AnnotationTableModel extends AbstractTableModel  {
		
		private ProbeGroupAnnotation [] data;
		private DataStore [] stores;
		
		/**
		 * Instantiates a new annotation table model.
		 * 
		 * @param data A set of ProbeGroupAnnotations
		 * @param stores The dataStores whose data will be added to the report
		 */
		public AnnotationTableModel (ProbeGroupAnnotation [] data, DataStore [] stores) {
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
			return 7+(stores.length*2);
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "Chr";
			case 1: return "Start";
			case 2: return "End";
			case 3: if (list != null)return "Average "+list.getValueName(); else return "No value";
			case 4: return "No. Probes";
			case 5: return "Features";
			case 6: return "Descriptions";
			default: 
				if ((c-7) % 2 == 0) {
					return "Mean "+stores[(c-7)/2].name();
				}
				else {
					return "StDev "+stores[(c-7)/2].name();					
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
			case 3: return Float.class;
			case 4: return Integer.class;
			case 5: return String.class;
			case 6: return String.class;
			default: return Float.class;
			}
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0 :
				return data[r].chromosome();
			
			case 1:
				return new Integer(data[r].start());
				
			case 2:
				return new Integer(data[r].end());
				
			case 3:
				return data[r].getAnnotationValue();

			case 4:
				return new Integer(data[r].numberOfProbes());
				
			case 5:
				return data[r].featureNames();
				
			case 6:
				return data[r].featureDescriptions();
				
			default:
				Probe [] theseProbes = data[r].probes();
				double [] values = new double [theseProbes.length];
				for (int i=0;i<theseProbes.length;i++) {
					try {
						values[i] = stores[(c-7)/2].getValueForProbe(theseProbes[i]);
					} 
					catch (SeqMonkException e) {}
				}
				
				if ((c-7) % 2 == 0) {
					return new Float(SimpleStats.mean(values));
				}
				else {
					return new Float(SimpleStats.stdev(values));					
				}
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent arg0) {
		String a = (String)annotationPosition.getSelectedItem();
		
		if (a.equals("all overlapping") || a.equals("all enclosed") || a.equals("surrounding")) {
			annotationLimit.setEnabled(false);
		}
		else {
			annotationLimit.setEnabled(true);
		}
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
		// Check to see if enough information has been added to allow us to
		// enable the OK button.	
		return probeDistanceLimit.getText().length() > 0;	
	}
	
	public boolean canExportGFF () {
		return true;
	}
	
	public int chromosomeColumn () {
		return 0;
	}

	public int startColumn () {
		return 1;
	}
	
	public int endColumn () {
		return 2;
	}

	public int strandColumn () {
		return -1;
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		String annotationTypeValue = (String)annotationType.getSelectedItem();
		int distanceLimit = 0;
		
		// Check what to do with unannotated probes
		boolean includeAll = true;
		if (((String)excludes.getSelectedItem()).equals("Exclude")) {
			includeAll = false;
		}
		
		String annotationPositionValue = (String)annotationPosition.getSelectedItem();
		// We're going to set up a set of booleans which tell us which kinds
		// of relationships we're allowed to look for later.
		boolean annotateIndividualProbes = true;
		boolean justEnclosed = false;
		boolean surrounding = true;
		boolean upstream = true;
		boolean downstream = true;
		
		
		if (annotationPositionValue.equals("[Don't annotate]")) {
			annotateIndividualProbes = true; // It's easier to leave this true as we can bail out the annotation more quickly
			justEnclosed = false;
			surrounding = false;
			upstream = false;
			downstream = false;
		}
		else if (annotationPositionValue.equals("all overlapping")) {
			annotateIndividualProbes = false;
			justEnclosed = false;
		}
		else if (annotationPositionValue.equals("all enclosed")) {
			annotateIndividualProbes = false;
			justEnclosed = true;
		}
		else if (annotationPositionValue.equals("surrounding")) {
			upstream = false;
			downstream = false;
		}
		else if (annotationPositionValue.equals("surrounding or upstream")) {
			downstream = false;
		}
		else if (annotationPositionValue.equals("surrounding or downstream")) {
			upstream = false;
		}
		else if (annotationPositionValue.equals("upstream")) {
			surrounding = false;
			downstream = false;
		}
		else if (annotationPositionValue.equals("downstream")) {
			surrounding = false;
			upstream = false;
		}
		else if (annotationPositionValue.equals("closest")) {
			// Leave things as they are!
		}
		else {
			System.err.println("Didn't recognise position value '"+annotationPositionValue+"'");
		}
	
		// We only need to worry about a distance cutoff if we're not using
		// surrounding.
		if (!annotationPositionValue.equals("surrounding")) {
			if (annotationLimit.getText().length()> 0) {
				distanceLimit = Integer.parseInt(annotationLimit.getText());
			}
		}
		
		// Find out how closely to group probes
		int groupDistance = Integer.parseInt(probeDistanceLimit.getText());
				
		Vector<ProbeGroupAnnotation> annotations = new Vector<ProbeGroupAnnotation>();
		
		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			progressUpdated("Processing Chr"+chrs[c].name(), c, chrs.length);
			
			Probe [] probes = collection.probeSet().getActiveList().getProbesForChromosome(chrs[c]);
			Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chrs[c],annotationTypeValue);
			
			ProbeGroupAnnotation group = new ProbeGroupAnnotation();
			int lastPosition = -1;
			
			// We can now step through the probes looking for the best feature match
			for (int p=0;p<probes.length;p++) {
				
				if (cancel) {
					progressCancelled();
					return;
				}

				
//				System.err.println("Looking at probe at pos "+probes[p].position()+" last was "+lastPosition);
				
				// Check if we're still within the same probe group as before
				
				if (lastPosition == -1 || probes[p].start()-groupDistance <= lastPosition) {
					// We're in the same group
					group.addProbe(probes[p]);
					if (probes[p].end() > lastPosition)
						lastPosition = probes[p].end();
//					System.err.println("Adding to existing group");
				}
				else {
					// We're at the end of a group so create a new one
//					System.err.println("End of group");
					
					// Since we have a completed group we may need to do the
					// group annotations
					if (! annotateIndividualProbes) {
						annotateGroup(group,justEnclosed,annotationTypeValue);
					}
					if(includeAll || group.featureNames().length()>0) {
//						System.err.println("Adding group to keepers");
						annotations.add(group);
					}
					else {
//						System.err.println("Not adding this group");
					}
					
					// Create a new group and add the current probe to start it off.
					group = new ProbeGroupAnnotation();
					group.addProbe(probes[p]);
					lastPosition = probes[p].end();
				}

				if (! annotateIndividualProbes) continue;
				
				Feature bestFeature = null;
				int closestDistance = 0;
				
				for (int f=0;f<features.length;f++) {
					
					if (surrounding) {
						// Quickest check is whether a probe overlaps with a feature
					
						if (probes[p].start() <= features[f].location().end() && probes[p].end() >= features[f].location().start()) {
							bestFeature = features[f];
							closestDistance = 0;
						
							// Once we've found an overlapping feature we quit.
							break;
						}
					}
					
					if (downstream) {
						// Check if the feature is downstream
						// Get the distance to the start
						int d=0;
						if (features[f].location().strand() == Location.FORWARD) {
							d = features[f].location().start() - probes[p].end();
						}
						else {
							d = probes[p].start() - features[f].location().end();
						}
						
						if (d >= 0) {
							// We're the right side of the feature

							if (d > distanceLimit || (bestFeature != null && d > closestDistance)) {
								continue;
							}

							// See if this is the closest feature we have so far...
							if (bestFeature == null || d < closestDistance) {
								bestFeature = features[f];
								closestDistance = d;
							}
							continue;
						}
					}

					if (upstream) {
						// Check if the feature is upstream
						// Get the distance to the start
						int d=0;
						if (features[f].location().strand() == Location.FORWARD) {
							d = probes[p].start() - features[f].location().end();
						}
						else {
							d = features[f].location().start() - probes[p].end();
						}
						
						if (d >= 0) {
							// We're the right side of the feature

							if (d > distanceLimit || (bestFeature != null && d > closestDistance)) {
								continue;
							}

							// See if this is the closest feature we have so far...
							if (bestFeature == null || d < closestDistance) {
								bestFeature = features[f];
								closestDistance = d;
							}
							continue;
						}
					}
				
				}
				if (bestFeature != null) {
					group.addFeature(bestFeature);
				}
			}
			
			// We've reached the end of the chromosome
			
			// We still need to check whether to add the current group.
			// and do any necessary annotation.
			if (group.numberOfProbes()>0) {
				// We need to do the annotation for this last group (if we have to)
				if (! annotateIndividualProbes) {
					annotateGroup(group,justEnclosed,annotationTypeValue);
				}
				if(includeAll || group.featureNames().length()>0) {
//					System.err.println("Adding group to keepers");
					annotations.add(group);
				}
			}
		}
				
				
		DataStore [] stores = new DataStore[0];
		
		if (((String)data.getSelectedItem()).equals("Include")) {
			stores = storesToAnnotate;
		}

		TableModel model = new AnnotationTableModel(annotations.toArray(new ProbeGroupAnnotation[0]),stores);
		
		reportComplete(model);
				
	}
	
}
