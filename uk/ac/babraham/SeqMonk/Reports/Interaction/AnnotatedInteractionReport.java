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
package uk.ac.babraham.SeqMonk.Reports.Interaction;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.InteractionProbePair;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;

/**
 * The AnnotatedListReport adds feature annotation to a probe list.  It
 * doesn't combine the probes but keeps the report at one line per probe
 */
public class AnnotatedInteractionReport extends InteractionReport implements KeyListener, ItemListener {

	/**
	 * Instantiates a new annotated list report.
	 * 
	 * @param collection The dataCollection to use
	 * @param storesToAnnotate The set of dataStores whose data can be added to the report
	 */
	public AnnotatedInteractionReport(DataCollection collection,  HeatmapMatrix matrix) {
		super(collection, matrix);
		interactions = matrix.filteredInteractions();
		
		dataStore = matrix.dataStore();

		HashSet<Probe> uniqueProbes = new HashSet<Probe>();
		for (int i=0;i<interactions.length;i++) {
			uniqueProbes.add(interactions[i].highestProbe());
			uniqueProbes.add(interactions[i].lowestProbe());
		}
		probes = uniqueProbes.toArray(new Probe[0]);
		Arrays.sort(probes);
	}

	private InteractionProbePair [] interactions;

	private Hashtable<Probe, Feature>probeAnnotations = new Hashtable<Probe, Feature>();

	/** The options panel. */
	private JPanel optionsPanel = null;

	private Probe [] probes;

	/** The position of features to annotate with */
	private JComboBox annotationPosition;

	/** The annotation type to use */
	private JComboBox annotationType;

	/** Whether to exclude unannotated probes */
	private JComboBox excludes;

	/** How far away a feature can be to be attached to a probe */
	private JTextField annotationLimit;
	
	/** We report on the current quantitation so we need the data store for this **/
	private HiCDataStore dataStore;

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
		annotationPosition = new JComboBox(new String [] {"[Don't annotate]","closest", "overlapping", "upstream", "downstream", "surrounding or upstream", "surrounding or downstream","name matched"});
		annotationPosition.addItemListener(this);
		choicePanel1.add(annotationPosition);
		annotationType = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
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

		optionsPanel.add(choicePanel,BorderLayout.CENTER);

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
		return "Annotated Interaction Report";
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

		if (a.equals("overlapping")  || a.equals("name matched") || a.equals("[Don't annotate]")) {
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


		if (annotationPositionValue.equals("[Don't annotate]")) {
			upstream = false;
			downstream = false;
			surrounding = false;
		}		
		else if (annotationPositionValue.equals("overlapping")) {
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

		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.


		Feature [] features = null;
		Chromosome lastChr = null;

		// We can now step through the probes looking for the best feature match
		for (int p=0;p<probes.length;p++) {

			if (cancel) {
				progressCancelled();
				return;
			}
			
			if (p % 100 == 0) {
				progressUpdated("Processed "+p+" probes", p, probes.length);
			}

			if (!probes[p].chromosome().equals(lastChr)) {
				features = collection.genome().annotationCollection().getFeaturesForType(probes[p].chromosome(),annotationTypeValue);
				lastChr = probes[p].chromosome();
			}

			String nameWithoutExtensions = "";
			String nameWithoutTranscript = "";

			if (matchname) {
				nameWithoutExtensions = probes[p].name().replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
				nameWithoutTranscript = nameWithoutExtensions.replaceAll("-\\d\\d\\d$", "");
			}

			Feature bestFeature = null;
			int closestDistance = 0;

			for (int f=0;f<features.length;f++) {

				if (matchname) {
					// Simplest check is if the name matches exactly
					if (features[f].name().equals(probes[p].name())  || features[f].name().equals(nameWithoutExtensions) || features[f].name().equals(nameWithoutTranscript)) {
						bestFeature = features[f];
						closestDistance = 0;
						break;
					}


				}

				if (surrounding) {
					// Quickest check is whether a probe overlaps a feature

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

					if (d >=0) {

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
			if (bestFeature == null) {
				continue;
			}

			probeAnnotations.put(probes[p],bestFeature);
		}

		if (!includeAll) {
			// We need to filter the interaction list to include only those which 
			// have annotations on both probes
			Vector<InteractionProbePair> filteredInteractions = new Vector<InteractionProbePair>();
			
			for (int i=0;i<interactions.length;i++) {
				if (probeAnnotations.containsKey(interactions[i].probe1()) && probeAnnotations.containsKey(interactions[i].probe2())) {
					filteredInteractions.add(interactions[i]);
				}
			}
			
			interactions = filteredInteractions.toArray(new InteractionProbePair[0]);
		}

		TableModel model = new AnnotationTableModel();
		reportComplete(model);
	}


	/**
	 * A TableModel representing the results of the AnnotatedListReport..
	 */
	private class AnnotationTableModel extends AbstractTableModel  {


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return interactions.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			return 17;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "Probe1";
			case 1: return "Chromosome";
			case 2: return "Start";
			case 3: return "End";
			case 4: return "Probe2";
			case 5: return "Chromosome";
			case 6: return "Start";
			case 7: return "End";
			case 8: return "Feature1";
			case 9: return "Description1";
			case 10: return "Feature2";
			case 11: return "Description2";
			case 12: return "Obs/Exp";
			case 13: return "P-value";
			case 14: return "Interactions";
			case 15: return "Probe1Quantitation";
			case 16: return "Probe2Quantitation";
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass (int c) {
			switch (c) {
			case 0: return Probe.class;
			case 1: return String.class;
			case 2: return Integer.class;
			case 3: return Integer.class;
			case 4: return Probe.class;
			case 5: return String.class;
			case 6: return Integer.class;
			case 7: return Integer.class;
			case 8: return Feature.class;
			case 9: return String.class;
			case 10: return Feature.class;
			case 11: return String.class;
			case 12: return Double.class;
			case 13: return Double.class;
			case 14: return Integer.class;
			case 15: return Float.class;
			case 16: return Float.class;
			}
			return null;
		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			case 0 :
				return interactions[r].lowestProbe();

			case 1:
				return interactions[r].lowestProbe().chromosome().name();

			case 2:
				return interactions[r].lowestProbe().start();

			case 3:
				return interactions[r].lowestProbe().end();

			case 4 :
				return interactions[r].highestProbe();

			case 5:
				return interactions[r].highestProbe().chromosome().name();

			case 6:
				return interactions[r].highestProbe().start();

			case 7:
				return interactions[r].highestProbe().end();
			case 8: 
				if (probeAnnotations.containsKey(interactions[r].lowestProbe()))
					return probeAnnotations.get(interactions[r].lowestProbe());
				return "";
			case 9: 
				if (probeAnnotations.containsKey(interactions[r].lowestProbe()))
					return probeAnnotations.get(interactions[r].lowestProbe()).description();
				return "";
			case 10: 
				if (probeAnnotations.containsKey(interactions[r].highestProbe()))
					return probeAnnotations.get(interactions[r].highestProbe());
				return "";
			case 11: 
				if (probeAnnotations.containsKey(interactions[r].highestProbe()))
					return probeAnnotations.get(interactions[r].highestProbe()).description();
				return "";
			case 12: return interactions[r].strength();
			case 13: return interactions[r].signficance();
			case 14: return interactions[r].absolute();
			case 15: try {
					return ((DataStore)dataStore).getValueForProbe(interactions[r].lowestProbe());
				} catch (SeqMonkException e) {
					return Float.NaN;
				}
			case 16: try {
					return ((DataStore)dataStore).getValueForProbe(interactions[r].highestProbe());
				} catch (SeqMonkException e) {
					return Float.NaN;
				}


			}

			return null;
		}

	}
}
