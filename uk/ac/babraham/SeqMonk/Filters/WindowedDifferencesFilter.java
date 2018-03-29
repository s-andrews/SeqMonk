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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ConsecutiveProbeGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.FeatureProbeGroupGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ProbeGroupGenerator;
import uk.ac.babraham.SeqMonk.Filters.ProbeGroupGenerator.ProbeWindowGenerator;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * The WindowedDifferencesFilter filters probes based on the absolute difference
 * in probe values between probes in different dataStores.  Rather than filtering
 * probes independently they are combined in sets covering a window of a defined 
 * size and all quantitated values are averaged over all of the probes in the
 * set.  A set of probes passes or fails the filter as a whole.
 */
public class WindowedDifferencesFilter extends ProbeFilter {

	private static final int DISTANCE_WINDOW = 111;
	private static final int CONSECUTIVE_WINDOW = 112;
	private static final int FEATURE_WINDOW = 113;

	private Double lowerLimit = null;
	private Double upperLimit = null;
	private Integer windowSize = null;
	private int windowType = DISTANCE_WINDOW;

	private DataStore [] fromStores = new DataStore[0];
	private DataStore [] toStores = new DataStore[0];

	private int combineType = -1;

	private final WindowedDifferencesFilterOptionsPanel optionPanel = new WindowedDifferencesFilterOptionsPanel();

	/**
	 * Instantiates a new windowed differences filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated.
	 */
	public WindowedDifferencesFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		// Set some defaults
		windowSize = 1000;
		combineType = DifferencesFilter.AVERAGE;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the average difference across a set of probes in a sliding window";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		// We need to check that we don't add any probes more than once
		// so we need to keep a hash of the probes we've added to the
		// filtered list.

		Hashtable<Probe,Float> goingToAdd = new Hashtable<Probe,Float>();

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","","Difference");

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		for (int c=0;c<chromosomes.length;c++) {

			progressUpdated("Processing windows on Chr"+chromosomes[c].name(), c, chromosomes.length);

			Probe [] probes = startingList.getProbesForChromosome(chromosomes[c]);

			ProbeGroupGenerator gen = null;
			if (windowType == DISTANCE_WINDOW) {
				gen = new ProbeWindowGenerator(probes,windowSize);
			} 
			else if (windowType == CONSECUTIVE_WINDOW) {
				gen = new ConsecutiveProbeGenerator(probes, windowSize);
			}
			else if (windowType == FEATURE_WINDOW) {
				gen = new FeatureProbeGroupGenerator(probes, collection.genome().annotationCollection().getFeaturesForType(optionPanel.featureTypeBox.getSelectedItem().toString()));
			}


			while (true) {

				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}

				Probe [] theseProbes = gen.nextSet();

				if (theseProbes == null) {
					break;
				}

				int count = 0;
				float d = 0;
				for (int s1=0;s1<fromStores.length;s1++) {
					for (int s2=0;s2<toStores.length;s2++) {
						switch (combineType) {
						case DifferencesFilter.AVERAGE:
							d+= getDifferenceValue(toStores[s2],fromStores[s1],theseProbes);
							count++;
							break;
						case DifferencesFilter.MAXIMUM:
							float dt1 = getDifferenceValue(toStores[s2],fromStores[s1],theseProbes);
							if (count == 0 || dt1 > d)
								d = dt1;
							count++;
							break;
						case DifferencesFilter.MINIMUM:
							float dt2 = getDifferenceValue(toStores[s2],fromStores[s1],theseProbes);
							if (count == 0 || dt2 < d)
								d = dt2;
							count++;
							break;
						}
					}	
				}

				if (combineType == DifferencesFilter.AVERAGE) {
					d/=count;
				}

				// Now we have the value we need to know if it passes the test
				if (upperLimit != null)
					if (d > upperLimit.doubleValue())
						continue;

				if (lowerLimit != null)
					if (d < lowerLimit.doubleValue())
						continue;

				for (int i=0;i<theseProbes.length;i++) {

					if (goingToAdd.containsKey(theseProbes[i])) {
						// Don't do anything if this probe is already there with a bigger difference
						continue;
						//						if (Math.abs(goingToAdd.get(theseProbes[i])) > Math.abs(d)) continue;
					}

					goingToAdd.put(theseProbes[i],d);
				}

			}
		}

		// Finally add all of the cached probes to the actual probe list
		Enumeration<Probe> en = goingToAdd.keys();
		while (en.hasMoreElements()) {
			Probe p = en.nextElement();
			newList.addProbe(p, goingToAdd.get(p));
		}


		filterFinished(newList);
	}

	/**
	 * Gets the difference value.
	 * 
	 * @param s1 the s1
	 * @param s2 the s2
	 * @param p the p
	 * @return the difference value
	 */
	private float getDifferenceValue (DataStore s1, DataStore s2, Probe [] p) {
		float d1=0;
		float d2=0;
		for (int i=0;i<p.length;i++) {
			try {
				d1 += s1.getValueForProbe(p[i]);
				d2 += s2.getValueForProbe(p[i]);
			}
			catch (SeqMonkException e) {
			}
		}
		d1 /= p.length;
		d2 /= p.length;

		float d = d1-d2;

		return d;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#hasOptionsPanel()
	 */
	@Override
	public boolean hasOptionsPanel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#isReady()
	 */
	@Override
	public boolean isReady() {
		// Check for the special case of comparing just to ourselves
		if (fromStores.length==1 && toStores.length==1 && fromStores[0]==toStores[0]) {
			return false;
		}

		if (fromStores.length ==0 || toStores.length == 0) return false;

		if (upperLimit == null && lowerLimit == null) return false;

		if (upperLimit != null && lowerLimit != null && lowerLimit > upperLimit) return false;

		if (windowSize == null || windowSize < 0) return false;

		if (! (combineType == DifferencesFilter.AVERAGE || combineType == DifferencesFilter.MAXIMUM || combineType == DifferencesFilter.MINIMUM)) return false;

		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Windowed Differences Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");
		if (combineType == DifferencesFilter.AVERAGE) {
			b.append("average ");
		}
		else if (combineType == DifferencesFilter.MINIMUM) {
			b.append("minimum ");
		}
		else if (combineType == DifferencesFilter.MAXIMUM) {
			b.append("maximum ");
		}

		b.append("difference when comparing ");

		for (int s=0;s<fromStores.length;s++) {
			b.append(fromStores[s].name());
			if (s < fromStores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" to ");

		for (int s=0;s<toStores.length;s++) {
			b.append(toStores[s].name());
			if (s < toStores.length-1) {
				b.append(" , ");
			}
		}

		b.append(" had a value ");

		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit);
			b.append(" and ");
			b.append(upperLimit);
		}

		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit);
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit);
		}

		b.append(" over a window of ");

		if (windowType == FEATURE_WINDOW) {
			b.append(optionPanel.featureTypeBox.getSelectedItem().toString());
			b.append(" features");
		}
		else {
			b.append(windowSize);
			if (windowType == DISTANCE_WINDOW) {
				b.append("bp");
			}
			else {
				b.append(" probes");
			}
		}

		b.append(". Quantitation was ");
		if (collection.probeSet().currentQuantitation() == null) {
			b.append("not known.");
		}
		else {
			b.append(collection.probeSet().currentQuantitation());
		}


		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("Difference ");

		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit);
			b.append(" and ");
			b.append(upperLimit);
		}

		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit);
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit);
		}

		b.append(" over ");
		if (windowType == FEATURE_WINDOW) {
			b.append(optionPanel.featureTypeBox.getSelectedItem());
			b.append(" features");
		}
		else {
			b.append(windowSize);
			if (windowType == DISTANCE_WINDOW) {
				b.append("bp");
			}
			else {
				b.append(" probes");
			}
		}

		return b.toString();
	}

	/**
	 * The WindowedDifferencesFilterOptionsPanel.
	 */
	public class WindowedDifferencesFilterOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {

		private JList fromDataList;
		private JList toDataList;
		private JComboBox combineTypeBox;
		private JTextField lowerLimitField;
		private JTextField upperLimitField;
		private JTextField windowSizeField;
		private JComboBox windowTypeBox;
		private JComboBox featureTypeBox;


		/**
		 * Instantiates a new windowed differences filter options panel.
		 */
		public WindowedDifferencesFilterOptionsPanel () {

			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new GridBagLayout());
			GridBagConstraints dpgbc = new GridBagConstraints();
			dpgbc.gridx=0;
			dpgbc.gridy=0;
			dpgbc.weightx=0.5;
			dpgbc.weighty=0.01;
			dpgbc.fill=GridBagConstraints.BOTH;

			dataPanel.add(new JLabel("From Data Store / Group",JLabel.CENTER),dpgbc);

			DefaultListModel fromDataModel = new DefaultListModel();
			DefaultListModel toDataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					fromDataModel.addElement(stores[i]);
					toDataModel.addElement(stores[i]);
				}
			}

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			fromDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(fromDataList);
			fromDataList.setCellRenderer(new TypeColourRenderer());
			fromDataList.addListSelectionListener(this);
			JScrollPane fromScrollPane = new JScrollPane(fromDataList);
			fromScrollPane.setPreferredSize(new Dimension(200,fromDataList.getPreferredSize().height));

			dataPanel.add(fromScrollPane,dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.01;

			dataPanel.add(new JLabel("To Data Store / Group",JLabel.CENTER),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			toDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(toDataList);
			toDataList.setCellRenderer(new TypeColourRenderer());
			toDataList.addListSelectionListener(this);
			JScrollPane toScrollPane = new JScrollPane(toDataList);
			toScrollPane.setPreferredSize(new Dimension(200,toDataList.getPreferredSize().height));
			
			dataPanel.add(toScrollPane,dpgbc);


			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


			JPanel choicePanel1 = new JPanel();

			combineTypeBox = new JComboBox(new String [] {"Average","Maximum","Minimum"});
			combineTypeBox.setEnabled(false);
			combineTypeBox.addItemListener(this);
			choicePanel1.add(combineTypeBox);

			valueChanged(null);  // Set the initial selections

			choicePanel1.add(new JLabel("difference"));
			choicePanel.add(choicePanel1);

			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel(" must be between "));
			lowerLimitField = new JTextField(3);
			lowerLimitField.addKeyListener(this);
			choicePanel2.add(lowerLimitField);

			choicePanel2.add(new JLabel(" and "));

			upperLimitField = new JTextField(3);
			upperLimitField.addKeyListener(this);
			choicePanel2.add(upperLimitField);
			choicePanel.add(choicePanel2);


			JPanel choicePanel4 = new JPanel();

			windowTypeBox = new JComboBox(new String [] {"Window size (bp)","Number of probes","Probes overlapping features"});
			windowTypeBox.addItemListener(this);
			choicePanel4.add(windowTypeBox);

			windowSizeField = new JTextField("1000",5);
			windowSizeField.addKeyListener(this);
			choicePanel4.add(windowSizeField);
			choicePanel.add(choicePanel4);

			JPanel choicePanel5 = new JPanel();
			choicePanel5.add(new JLabel("Select Feature Type"));
			featureTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			featureTypeBox.setEnabled(false);
			choicePanel5.add(featureTypeBox);

			choicePanel.add(choicePanel5);

			add(new JScrollPane(choicePanel),BorderLayout.CENTER);


		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,300);
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

			if (f.equals(lowerLimitField)) {
				if (f.getText().length() == 0) lowerLimit = null;
				else if (f.getText().equals("-")) {
					lowerLimit = 0d;
				}
				else {
					try {
						lowerLimit = Double.parseDouble(lowerLimitField.getText());
					}
					catch (NumberFormatException e) {
						lowerLimitField.setText(lowerLimitField.getText().substring(0,lowerLimitField.getText().length()-1));
					}
				}
			}

			else if (f.equals(upperLimitField)) {
				if (f.getText().length() == 0) upperLimit = null;
				else if (f.getText().equals("-")) {
					upperLimit = 0d;
				}
				else {
					try {
						upperLimit = Double.parseDouble(upperLimitField.getText());
					}
					catch (NumberFormatException e) {
						upperLimitField.setText(upperLimitField.getText().substring(0,upperLimitField.getText().length()-1));
					}
				}
			}

			else if (f.equals(windowSizeField)) {
				if (f.getText().length() == 0) windowSize = null;
				else {
					try {
						windowSize = Integer.parseInt(windowSizeField.getText());
					}
					catch (NumberFormatException e) {
						windowSizeField.setText(windowSizeField.getText().substring(0,windowSizeField.getText().length()-1));
					}
				}
			}

			else {
				System.err.println("Unknown text field "+f);
			}

			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			// If we have 2 or less items selected then we can disable the
			// combobox which says whether we're looking at min, max or average
			// difference (since they're all the same with only 1 comparison)

			Object [] fromSelectedObjects = fromDataList.getSelectedValues();
			Object [] toSelectedObjects = toDataList.getSelectedValues();

			if (fromSelectedObjects.length == 0 || toSelectedObjects.length == 0) {
				combineTypeBox.setEnabled(false);
			}
			else if (fromSelectedObjects.length <2 && toSelectedObjects.length < 2) {
				combineTypeBox.setEnabled(false);
			}
			else {
				combineTypeBox.setEnabled(true);
			}


			DataStore [] newFromStores = new DataStore[fromSelectedObjects.length];
			for (int i=0;i<fromSelectedObjects.length;i++){
				newFromStores[i] = (DataStore)fromSelectedObjects[i];
			}
			fromStores = newFromStores;

			DataStore [] newToStores = new DataStore[toSelectedObjects.length];
			for (int i=0;i<toSelectedObjects.length;i++){
				newToStores[i] = (DataStore)toSelectedObjects[i];
			}
			toStores = newToStores;

			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {

			if (ie.getSource().equals(combineTypeBox)) {
				// Look at the limit type and convert to a char
				String l = (String)combineTypeBox.getSelectedItem();
				if (l.equals("Average"))
					combineType = DifferencesFilter.AVERAGE;
				else if (l.equals("Maximum"))
					combineType = DifferencesFilter.MAXIMUM;
				else if (l.equals("Minimum"))
					combineType = DifferencesFilter.MINIMUM;
				else {
					System.err.println("Didn't recognise limit type "+l);
				}
			}
			else if (ie.getSource() == windowTypeBox) {
				String type = (String)windowTypeBox.getSelectedItem();
				if (type.equals("Window size (bp)")) {
					windowType = DISTANCE_WINDOW;
					featureTypeBox.setEnabled(false);
					windowSizeField.setEnabled(true);
				}
				else if (type.equals("Number of probes")) {
					windowType = CONSECUTIVE_WINDOW;
					featureTypeBox.setEnabled(false);
					windowSizeField.setEnabled(true);
				}
				else if (type.equals("Probes overlapping features")) {
					windowType = FEATURE_WINDOW;
					featureTypeBox.setEnabled(true);
					windowSizeField.setEnabled(false);
				}
				else {
					System.err.println("Unknown window type '"+type+"'");
					windowType = -1;
				}

			}
			optionsChanged();

		}

	}

}
