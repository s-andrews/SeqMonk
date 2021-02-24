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
package uk.ac.babraham.SeqMonk.Reports;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;


import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.ProbeListAnnotationSelector.ProbeListAnnotation;
import uk.ac.babraham.SeqMonk.Displays.ProbeListAnnotationSelector.ProbeListAnnotationSelectorDialog;

/**
 * The AnnotatedListReport adds feature annotation to a probe list.  It
 * doesn't combine the probes but keeps the report at one line per probe
 */
public class AnnotatedProbeReport extends Report implements KeyListener, ItemListener {

	private ProbeList listToAnnotate;
	private boolean probesHaveNames = false;
	
	
	/**
	 * Instantiates a new annotated list report.
	 * 
	 * @param collection The dataCollection to use
	 * @param storesToAnnotate The set of dataStores whose data can be added to the report
	 */
	public AnnotatedProbeReport(DataCollection collection,DataStore[] storesToAnnotate) {
		super(collection, storesToAnnotate);
		listToAnnotate = collection.probeSet().getActiveList();
		
		probesHaveNames = listToAnnotate.getAllProbes()[0].hasDefinedName();
	}

	/** The options panel. */
	private JPanel optionsPanel = null;

	/** The position of features to annotate with */
	private JComboBox annotationPosition;

	/** The annotation type to use */
	private JComboBox annotationType;

	/** Whether to exclude unannotated probes */
	private JComboBox excludes;

	/** Whether to add data from visible stores to the report */
	private JComboBox data;

	/** How far away a feature can be to be attached to a probe */
	private JTextField annotationLimit;
	
	/** The set of annotations to put on the report **/
	private ProbeListAnnotation [] annotations = new ProbeListAnnotation[0];
	private JLabel annotationLabel;
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#getOptionsPanel()
	 */
	public JPanel getOptionsPanel () {

		if (optionsPanel != null) return optionsPanel;

		optionsPanel = new JPanel();

		optionsPanel.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


		JPanel choicePanel1 = new JPanel();

		choicePanel1.add(new JLabel("Annotate with "));
		annotationPosition = new JComboBox(new String [] {"[Don't annotate]","closest", "overlapping", "exactly overlapping", "upstream", "downstream", "surrounding or upstream", "surrounding or downstream","name matched"});
		annotationPosition.addItemListener(this);		
		choicePanel1.add(annotationPosition);
		String [] featureTypes = collection.genome().annotationCollection().listAvailableFeatureTypes();
		annotationType = new JComboBox(featureTypes);
		annotationType.setPrototypeDisplayValue("No longer than this please");
		choicePanel1.add(annotationType);
		choicePanel.add(choicePanel1);

		JPanel choicePanel2 = new JPanel();
		choicePanel2.add(new JLabel("Annotation distance cutoff "));
		annotationLimit = new JTextField("2000",7);
		annotationLimit.addKeyListener(this);
		annotationLimit.setEnabled(false);
		choicePanel2.add(annotationLimit);
		choicePanel2.add(new JLabel("bp"));
		choicePanel.add(choicePanel2);

		JPanel choicePanel3 = new JPanel();
		excludes = new JComboBox(new String [] {"Include","Exclude"});
		choicePanel3.add(excludes);
		choicePanel3.add(new JLabel(" unannotated probes"));
		choicePanel.add(choicePanel3);

		JPanel choicePanel4 = new JPanel();
		data = new JComboBox(new String [] {"Include","Don't include"});
		choicePanel4.add(data);
		choicePanel4.add(new JLabel(" data for currently visible stores"));
		choicePanel.add(choicePanel4);

		
		JPanel choicePanel5 = new JPanel();
		JButton annotationButton = new JButton("Select List Annotation");
		
		// The initial annotations will be the values on the list we're annotating
		ProbeList listToAnnotate = collection.probeSet().getActiveList();
		String [] valueNames = listToAnnotate.getValueNames();
		
		annotations = new ProbeListAnnotation[valueNames.length];
		
		for (int i=0;i<valueNames.length;i++) {
			annotations[i] = new ProbeListAnnotation(listToAnnotate,valueNames[i]);
		}
		
		annotationButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ProbeListAnnotationSelectorDialog d = new ProbeListAnnotationSelectorDialog(annotations);
				
				annotations = d.getAnnotations();
				annotationLabel.setText(" "+annotations.length+" annotations selected");
			}
		});
		choicePanel5.add(annotationButton);
		annotationLabel = new JLabel(" "+annotations.length+" annotations selected");
		choicePanel5.add(annotationLabel);
		choicePanel.add(choicePanel5);

		optionsPanel.add(choicePanel,BorderLayout.CENTER);
		
		
		// If we have named probes then we'll default to using 
		// name matched genes to annotate to since that's usually
		// the right thing.
		if (probesHaveNames) {
			int geneIndex = -1;
			for (int f=0;f<featureTypes.length;f++) {
				if (featureTypes[f].toLowerCase().equals("gene")) {
					geneIndex = f;
					break;
				}
			}
			
			// We only do this if we can find a gene track
			if (geneIndex >= 0) {
				annotationPosition.setSelectedIndex(annotationPosition.getModel().getSize()-1);
				annotationType.setSelectedIndex(geneIndex);
			}
		}


		return optionsPanel;
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
		return "Annotated Probe Report for "+listToAnnotate.name();
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

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent arg0) {
		String a = (String)annotationPosition.getSelectedItem();

		if (a.equals("overlapping")  || a.equals("exactly overlapping") || a.equals("name matched") || a.equals("[Don't annotate]")) {
			annotationLimit.setEnabled(false);
		}
		else {
			annotationLimit.setEnabled(true);
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#isReady()
	 */
	@Override
	public boolean isReady() {
		return annotationLimit.getText().length() > 0;
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
		boolean surrounding = true;
		boolean upstream = true;
		boolean downstream = true;
		boolean matchname = false;
		boolean exactmatch = false;
		boolean skipAnnotation = false;


		if (annotationPositionValue.equals("[Don't annotate]")) {
			upstream = false;
			downstream = false;
			surrounding = false;
			skipAnnotation = true;
		}		
		else if (annotationPositionValue.equals("overlapping")) {
			upstream = false;
			downstream = false;
		}
		else if (annotationPositionValue.equals("exactly overlapping")) {
			upstream = false;
			downstream = false;
			exactmatch = true;
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
		else if (annotationPositionValue.equals("name matched")) {
			matchname = true;
			upstream = false;
			surrounding = false;
			downstream = false;
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

		Vector<ProbeAnnotation> annotations = new Vector<ProbeAnnotation>();

		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.

		Chromosome [] chrs = collection.genome().getAllChromosomes();

		for (int c=0;c<chrs.length;c++) {

			progressUpdated("Processing Chr"+chrs[c].name(), c, chrs.length);

			Probe [] probes = collection.probeSet().getActiveList().getProbesForChromosome(chrs[c]);
			Feature [] features = new Feature [0];
			
			if (!skipAnnotation) {
				features = collection.genome().annotationCollection().getFeaturesForType(chrs[c],annotationTypeValue);
			}

			// We can now step through the probes looking for the best feature match
			for (int p=0;p<probes.length;p++) {

				if (cancel) {
					progressCancelled();
					return;
				}

				String nameWithoutExtensions = "";
				String nameWithoutTranscript = "";

				if (matchname) {
					nameWithoutExtensions = probes[p].name().replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
					nameWithoutTranscript = nameWithoutExtensions.replaceAll("-\\d\\d\\d$", "");
				}

				Feature bestFeature = null;
				int closestDistance = 0;
				String relationshipType = "Not found";

				for (int f=0;f<features.length;f++) {

					if (matchname) {
						// Simplest check is if the name matches exactly
						if (features[f].name().equals(probes[p].name())  || features[f].name().equals(nameWithoutExtensions) || features[f].name().equals(nameWithoutTranscript)) {
							bestFeature = features[f];
							closestDistance = 0;
							relationshipType = "Name match";
							break;
						}


					}

					if (surrounding) {
						// Quickest check is whether a probe overlaps a feature

						if (probes[p].start() <= features[f].location().end() && probes[p].end() >= features[f].location().start()) {
							
							// If this is an exact overlap then we check to see that the positions exactly align.
							if ((!exactmatch) || (probes[p].start() == features[f].location().start() && probes[p].end() == features[f].location().end())) {
								bestFeature = features[f];
								closestDistance = 0;
								relationshipType = "overlapping";

								// Once we've found an overlapping feature we quit.
								break;
							}
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

						if (d >=0) {

							if (d > distanceLimit || (bestFeature != null && d > closestDistance)) {
								continue;
							}

							// See if this is the closest feature we have so far...
							if (bestFeature == null || d < closestDistance) {
								bestFeature = features[f];
								relationshipType = "downstream";
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
								relationshipType = "upstream";
								closestDistance = d;
							}
							continue;
						}
					}

				}
				if (bestFeature == null && (! includeAll)) {
					continue;
				}

				annotations.add(new ProbeAnnotation(probes[p],bestFeature,closestDistance,relationshipType));
			}

		}

		if (includeAll) {
			Probe [] probes = collection.probeSet().getActiveList().getProbesForChromosome(null);

			for (int p=0;p<probes.length;p++) {
				annotations.add(new ProbeAnnotation(probes[p],null,0,"Not found"));				
			}

		}

		DataStore [] stores = new DataStore[0];

		if (((String)data.getSelectedItem()).equals("Include")) {
			stores = storesToAnnotate;
		}

		TableModel model = new AnnotationTableModel(annotations.toArray(new ProbeAnnotation[0]),collection.probeSet().getActiveList(),stores);
		reportComplete(model);
	}

	public boolean canExportGFF () {
		return true;
	}

	public int chromosomeColumn () {
		return 1;
	}

	public int startColumn () {
		return 2;
	}

	public int endColumn () {
		return 3;
	}

	public int strandColumn () {
		return 4;
	}


	/**
	 * A class representing the annotation for a single probe
	 */
	private class ProbeAnnotation {

		/** The probe. */
		public Probe probe;

		/** The feature. */
		public Feature feature;

		/** How far the feature is from the probe */
		public int distance;

		/** What spatial relationship the feature has to the probe */
		public String orientation;

		/**
		 * Instantiates a new probe annotation.
		 * 
		 * @param p The probe to annotate
		 * @param f The feature to annotate with
		 * @param d How far the feature was from the probe
		 * @param o A description of the orientation (upstream, downstream, surrounding etc)
		 */
		public ProbeAnnotation (Probe p, Feature f, int d, String o) {
			probe = p;
			feature = f;
			distance = d;
			orientation = o;
		}
	}

	/**
	 * A TableModel representing the results of the AnnotatedListReport..
	 */
	private class AnnotationTableModel extends AbstractTableModel  {

		private ProbeAnnotation [] data;

		/** The dataStores whose data will be added to the report */
		private DataStore [] stores;

		/**
		 * Instantiates a new annotation table model.
		 * 
		 * @param data The set of annotated probes in the report
		 * @param list The starting probe list
		 * @param stores The data stores whose data will be added to the report.
		 */
		public AnnotationTableModel (ProbeAnnotation [] data, ProbeList list, DataStore [] stores) {
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
			return 12+annotations.length+stores.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "Probe";
			case 1: return "Chromosome";
			case 2: return "Start";
			case 3: return "End";
			case 4: return "Probe Strand";
			case 5: return "Feature";
			case 6: return "ID";
			case 7: return "Description";
			case 8: return "Feature Strand";
			case 9: return "Type";
			case 10: return "Feature Orientation";
			case 11: return "Distance";
			default: 
				if (c < 12 + annotations.length) {
					return annotations[c-12].toString();
				}
				
				return stores[c-(12+annotations.length)].name();
			}
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass (int c) {
			switch (c) {
			case 0: return String.class;
			case 1: return String.class;
			case 2: return Integer.class;
			case 3: return Integer.class;
			case 4: return String.class;
			case 5: return Object.class;
			case 6: return String.class;
			case 7: return String.class;
			case 8: return String.class;
			case 9: return String.class;
			case 10: return String.class;
			case 11: return Integer.class;
			default: return Double.class;
			}
		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0 :
				return data[r].probe.name();

			case 1:
				return data[r].probe.chromosome();

			case 2:
				return Integer.valueOf(data[r].probe.start());

			case 3:
				return Integer.valueOf(data[r].probe.end());

			case 4:
				if (data[r].probe.strand()== Probe.FORWARD) return "+";
				if (data[r].probe.strand()== Probe.REVERSE) return "-";
				return "";

			case 5:
				return data[r].feature;

			case 6:
				if (data[r].feature != null)
					return data[r].feature.id();
				else 
					return "";

			case 7:
				if (data[r].feature != null)
					return data[r].feature.description();
				else 
					return "";
				
			case 8:
				if (data[r].feature == null) return "";
				if (data[r].feature.location().strand() == Location.FORWARD) return "+";
				if (data[r].feature.location().strand() == Location.REVERSE) return "-";
				return "";


			case 9:
				if (data[r].feature != null) 
					return data[r].feature.type();
				else 
					return "";

			case 10:
				return data[r].orientation;

			case 11:
				return Integer.valueOf(data[r].distance);

			default:
				if (c < 12 + annotations.length) {
					if (annotations[c-12].list().getValuesForProbe(data[r].probe) == null) {
						return Double.NaN;
					}
					else {
						return Double.valueOf(annotations[c-12].list().getValuesForProbe(data[r].probe)[annotations[c-12].index()]);
					}
				}
				
				try {
					return Double.valueOf(stores[c-(12+annotations.length)].getValueForProbe(data[r].probe));
				} 
				catch (SeqMonkException e) {
					return null;
				}
			}
		}

	}
}
