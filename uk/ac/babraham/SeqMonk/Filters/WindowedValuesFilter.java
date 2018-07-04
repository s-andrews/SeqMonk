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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;

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
 * Filters probes based on their associated quantiation values. Rather than filtering
 * probes independently they are combined in sets covering a window of a defined 
 * size and all quantitated values are averaged over all of the probes in the
 * set.  A set of probes passes or fails the filter as a whole.
 */
public class WindowedValuesFilter extends ProbeFilter {

	private static final int DISTANCE_WINDOW = 111;
	private static final int CONSECUTIVE_WINDOW = 112;
	private static final int FEATURE_WINDOW = 113;

	private DataStore [] stores = new DataStore[0];
	private Double upperLimit = null;
	private Double lowerLimit = null;
	private int limitType = EXACTLY;
	private int windowType = DISTANCE_WINDOW;
	private int storesLimit = 0;
	private Integer windowSize = 1000;

	private final WindowedValuesFilterOptionsPanel optionsPanel = new WindowedValuesFilterOptionsPanel();


	/**
	 * Instantiates a new windowed values filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantiated.
	 */
	public WindowedValuesFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the average value of probes within a larger sliding window";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",null);

		Chromosome [] chromosomes = collection.genome().getAllChromosomes();

		for (int c=0;c<chromosomes.length;c++) {

			HashSet<Probe>passedProbes = new HashSet<Probe>();

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
				gen = new FeatureProbeGroupGenerator(probes, collection.genome().annotationCollection().getFeaturesForType(optionsPanel.featureTypeBox.getSelectedItem().toString()));
			}
			else {
				progressExceptionReceived(new SeqMonkException("No window type known with number "+windowType));
				return;
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
				for (int s=0;s<stores.length;s++) {
					double totalValue = 0;
					for (int i=0;i<theseProbes.length;i++) {
						// Get the values for the probes in this set
						try {
							totalValue += stores[s].getValueForProbe(theseProbes[i]);
						} 
						catch (SeqMonkException e) {
						}
					}
					totalValue /= theseProbes.length;

					// Now we have the value we need to know if it passes the test
					if (upperLimit != null)
						if (totalValue > upperLimit.doubleValue())
							continue;

					if (lowerLimit != null)
						if (totalValue < lowerLimit.doubleValue())
							continue;

					// This one passes, we can add it to the count
					++count;

				}


				// We can now figure out if the count we've got lets us add this
				// probe to the probe set.
				switch (limitType) {
				case EXACTLY:
					if (count == storesLimit)
						for (int i=0;i<theseProbes.length;i++) {
							if (passedProbes.contains(theseProbes[i])) continue;
							newList.addProbe(theseProbes[i],null);
							passedProbes.add(theseProbes[i]);
						}
					break;

				case AT_LEAST:
					if (count >= storesLimit)
						for (int i=0;i<theseProbes.length;i++) {
							if (passedProbes.contains(theseProbes[i])) continue;
							newList.addProbe(theseProbes[i],null);
							passedProbes.add(theseProbes[i]);
						}
					break;

				case NO_MORE_THAN:
					if (count <= storesLimit)
						for (int i=0;i<theseProbes.length;i++) {
							if (passedProbes.contains(theseProbes[i])) continue;
							newList.addProbe(theseProbes[i],null);
							passedProbes.add(theseProbes[i]);
						}
					break;
				}
			}
		}

		filterFinished(newList);

	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return optionsPanel;
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
		if (stores.length == 0) return false;

		if (upperLimit == null && lowerLimit == null) return false;

		if (upperLimit != null && lowerLimit != null && lowerLimit > upperLimit) return false;

		if (storesLimit == 0 || storesLimit > stores.length) return false;

		if (windowSize == null || windowSize < 0) return false;

		if (! (limitType == EXACTLY || limitType == NO_MORE_THAN || limitType == AT_LEAST)) return false;

		return true;

	}



	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Windowed Values Filter";
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
		if (limitType == EXACTLY) {
			b.append("exactly ");
		}
		else if (limitType == AT_LEAST) {
			b.append("at least ");
		}
		else if (limitType == NO_MORE_THAN) {
			b.append("no more than ");
		}

		b.append(storesLimit);

		b.append(" of ");

		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
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

		b.append(" in a window of ");

		if (windowType == FEATURE_WINDOW) {
			b.append(optionsPanel.featureTypeBox.getSelectedItem().toString());
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

		b.append("Value ");

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
			b.append(optionsPanel.featureTypeBox.getSelectedItem().toString());
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
	 * The WindowedValuesFilterOptionsPanel.
	 */
	private class WindowedValuesFilterOptionsPanel extends JPanel implements ListSelectionListener,KeyListener, ItemListener {

		private JList dataList;
		private JTextField lowerLimitField;
		private JTextField upperLimitField;
		private JComboBox limitTypeBox;
		private JComboBox featureTypeBox;
		private JTextField dataChosenNumberField;
		private JLabel dataAvailableNumber;
		private JTextField windowSizeField;
		private JComboBox windowTypeBox;


		/**
		 * Instantiates a new windowed values filter options panel.
		 */
		public WindowedValuesFilterOptionsPanel () {


			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new BorderLayout());
			dataPanel.add(new JLabel("Data Sets/Groups",JLabel.CENTER),BorderLayout.NORTH);

			DefaultListModel dataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					dataModel.addElement(stores[i]);
				}
			}

			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.addListSelectionListener(this);
			JScrollPane scrollPane = new JScrollPane(dataList);
			scrollPane.setPreferredSize(new Dimension(200,dataList.getPreferredSize().height));
			dataPanel.add(scrollPane,BorderLayout.CENTER);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));

			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Average value must be between "));
			lowerLimitField = new JTextField(3);
			lowerLimitField.addKeyListener(this);
			choicePanel2.add(lowerLimitField);

			choicePanel2.add(new JLabel(" and "));

			upperLimitField = new JTextField(3);
			upperLimitField.addKeyListener(this);
			choicePanel2.add(upperLimitField);
			choicePanel.add(choicePanel2);

			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel(" for "));

			limitTypeBox = new JComboBox(new String [] {"Exactly","At least","No more than"});
			limitTypeBox.addItemListener(this);
			choicePanel3.add(limitTypeBox);

			storesLimit = dataList.getSelectedIndices().length;
			dataChosenNumberField = new JTextField(""+storesLimit,3);
			dataChosenNumberField.addKeyListener(this);
			choicePanel3.add(dataChosenNumberField);

			choicePanel3.add(new JLabel(" of the "));

			dataAvailableNumber = new JLabel("");
			valueChanged(null); // Update number
			choicePanel3.add(dataAvailableNumber);

			choicePanel3.add(new JLabel(" selected Data Stores "));

			JPanel choicePanel4 = new JPanel();

			windowTypeBox = new JComboBox(new String [] {"Window size (bp)","Number of probes","Probes overlapping features"});
			windowTypeBox.addItemListener(this);
			choicePanel4.add(windowTypeBox);


			windowSizeField = new JTextField(windowSize.toString(),5);
			windowSizeField.addKeyListener(this);
			choicePanel4.add(windowSizeField);
			choicePanel.add(choicePanel4);

			JPanel choicePanel5 = new JPanel();
			choicePanel5.add(new JLabel("Select Feature Type"));
			featureTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			featureTypeBox.setEnabled(false);
			choicePanel5.add(featureTypeBox);

			choicePanel.add(choicePanel5);

			choicePanel.add(choicePanel3);
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

			try {
				if (f.equals(lowerLimitField)) {
					if (f.getText().length() == 0) lowerLimit = null;
					else if (f.getText().equals("-")) {
						lowerLimit = 0d;
					}
					else {
						lowerLimit = Double.parseDouble(lowerLimitField.getText());
					}
				}

				else if (f.equals(upperLimitField)) {
					if (f.getText().length() == 0) upperLimit = null;
					else if (f.getText().equals("-")) {
						upperLimit = 0d;
					}
					else {
						upperLimit = Double.parseDouble(upperLimitField.getText());
					}
				}

				else if (f.equals(dataChosenNumberField)) {
					if (f.getText().length() == 0) storesLimit = 0;
					else {
						storesLimit = Integer.parseInt(dataChosenNumberField.getText());
					}
				}

				else if (f.equals(windowSizeField)) {
					if (f.getText().length() == 0) windowSize = null;
					else {
						windowSize = Integer.parseInt(windowSizeField.getText());
					}
				}

				else {
					System.err.println("Unknown text field "+f);
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}

			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			Object [] o = dataList.getSelectedValues();

			DataStore [] newStores = new DataStore[o.length];

			for (int i=0;i<o.length;i++) {
				newStores[i] = (DataStore)o[i];
			}

			stores = newStores;

			dataAvailableNumber.setText(""+o.length);

			optionsChanged();
		}


		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {
			if (ie.getSource() == limitTypeBox) {
				// The limit type box has changed
				String type = (String)limitTypeBox.getSelectedItem();
				if (type.equals("Exactly")) {
					limitType = EXACTLY;
				}
				else if (type.equals("At least")) {
					limitType = AT_LEAST;
				}
				else if (type.equals("No more than")) {
					limitType = NO_MORE_THAN;
				}
				else {
					System.err.println("Unknown limit type '"+type+"'");
					limitType = -1;
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
