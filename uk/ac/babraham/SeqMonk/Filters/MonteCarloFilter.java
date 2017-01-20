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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Filters probes based on the absolute differences between two
 * or more dataStores.
 */
public class MonteCarloFilter extends ProbeFilter {

	public static final int MAXIMUM = 1;
	public static final int MEDIAN = 2;
	public static final int MEAN = 3;

	private int testType = MEAN;

	private ProbeList toList;
	private DataStore store;
	private int iterationCount = 10000;

	private final MoneteCarloOptionsPanel optionsPanel = new MoneteCarloOptionsPanel();

	/**
	 * Instantiates a new differences filter with default options.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public MonteCarloFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList() {

		Probe [] probes = toList.getAllProbes();

		ArrayList<Probe> allProbes = new ArrayList<Probe>();

		Probe [] fromProbes = startingList.getAllProbes();

		try {
			for (int i=0;i<fromProbes.length;i++) {
				allProbes.add(fromProbes[i]);
			}

			int passedCount = 0;

			float targetValue = getValue(probes);

			for (int iteration = 0;iteration < iterationCount; iteration++) {

				if (iteration % 100 == 0) {
					progressUpdated("Performed "+iteration+" iterations",iteration, iterationCount);
				}

				if (cancel) {
					progressCancelled();
					return;
				}

				Probe [] theseProbes = makeRandomProbes(allProbes,probes.length);
				float value = getValue(theseProbes);

				if (value >= targetValue) {
					++passedCount;
				}
			}


			float pValue = ((float)passedCount)/iterationCount;

			ProbeList newList = new ProbeList(toList, "", "", "P-value");

			for (int i=0;i<probes.length;i++) {
				newList.addProbe(probes[i], pValue);
			}

			filterFinished(newList);		

		}
		catch (SeqMonkException sme) {
			progressExceptionReceived(sme);
			return;
		}
	}

	public float getValue (Probe [] probes) throws SeqMonkException {

		switch (testType) {

		case MEAN:
			float value = 0;
			for (int i=0;i<probes.length;i++) {
				value += store.getValueForProbe(probes[i]);
			}
			return value/probes.length;

		case MEDIAN:
			float [] values = new float [probes.length];
			for (int i=0;i<probes.length;i++) {
				values[i] = store.getValueForProbe(probes[i]);
			}
			return SimpleStats.median(values);

		case MAXIMUM:
			float max = store.getValueForProbe(probes[0]);
			for (int i=1;i<probes.length;i++) {
				float thisValue = store.getValueForProbe(probes[i]);
				if (thisValue > max) max = thisValue;
			}
			return max;

		default:
			throw new SeqMonkException("Unknown test type value "+testType);
		}

	}

	public Probe [] makeRandomProbes (List<Probe> probes,int count) {

		Collections.shuffle(probes);

		Probe [] returnList = new Probe[count];

		for (int i=0;i<returnList.length;i++) {
			returnList[i] = probes.get(i);
		}
		return returnList;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Does a Monte-Carlo simulation of a sub-list selection";
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

		if (toList != null && store != null && iterationCount > 0) {
			return true;
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Monte Carlo Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Monte Carlo sumulation using data in ");
		b.append(store.name());
		b.append(" comparing ");
		if (testType == MEAN) {
			b.append("mean ");
		}
		else if (testType == MEDIAN) {
			b.append("median ");
		}
		else if (testType == MAXIMUM) {
			b.append("maximum ");
		}

		b.append("values when making ");

		b.append(iterationCount);

		b.append(" simulations of selecting '");

		b.append(toList.name());

		b.append("' from '");

		b.append(startingList.name());

		b.append("'. Quantitation was ");
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

		b.append(iterationCount);
		b.append(" iteration Monte Carlo");

		return b.toString();
	}

	/**
	 * The DifferencesOptionsPanel.
	 */
	private class MoneteCarloOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {

		private JList dataStoreList;
		private JList probeListList;
		private JComboBox combineTypeBox;
		private JTextField iterationsField;

		/**
		 * Instantiates a new monte carlo options panel.
		 */
		public MoneteCarloOptionsPanel () {

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

			dataPanel.add(new JLabel("Data Store to Test",JLabel.CENTER),dpgbc);

			DefaultListModel fromDataModel = new DefaultListModel();
			DefaultListModel toDataModel = new DefaultListModel();

			DataStore [] dataStores = collection.getAllDataStores();
			for (int i=0;i<dataStores.length;i++) {
				if (dataStores[i].isQuantitated()) {
					fromDataModel.addElement(dataStores[i]);
				}
			}

			ProbeList [] probeLists = collection.probeSet().getActiveList().getAllProbeLists();
			for (int i=0;i<probeLists.length;i++) {
				toDataModel.addElement(probeLists[i]);
			}

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			dataStoreList = new JList(fromDataModel);
			dataStoreList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			dataStoreList.setCellRenderer(new TypeColourRenderer());
			dataStoreList.addListSelectionListener(this);
			for (int i=0;i<dataStores.length;i++) {
				if (dataStores[i] == collection.getActiveDataStore()) {
					dataStoreList.setSelectedIndex(i);
					break;
				}
			}

			dataPanel.add(new JScrollPane(dataStoreList),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.01;

			dataPanel.add(new JLabel("Test Probe List",JLabel.CENTER),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			probeListList = new JList(toDataModel);
			probeListList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			probeListList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(probeListList),dpgbc);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));

			JPanel choicePanel1 = new JPanel();

			choicePanel1.add(new JLabel("Compare "));
			combineTypeBox = new JComboBox(new String [] {"Mean","Median","Maximum"});
			combineTypeBox.addItemListener(this);
			choicePanel1.add(combineTypeBox);

			choicePanel1.add(new JLabel(" values"));
			choicePanel.add(choicePanel1);

			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Over "));
			iterationsField = new JTextField(6);
			iterationsField.setText(""+iterationCount);
			iterationsField.addKeyListener(new NumberKeyListener(false, false));
			iterationsField.addKeyListener(this);
			choicePanel2.add(iterationsField);

			choicePanel2.add(new JLabel(" iterations."));

			choicePanel.add(choicePanel2);

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

			int iterations = 0;

			if (f.getText().length()>0) {

				if (f.getText().equals("-")) {
					iterations = 0;
				}
				else {
					try {
						iterations = Integer.parseInt(f.getText());
					}
					catch (NumberFormatException e) {
						return;
					}
				}
			}

			iterationCount = iterations;

			optionsChanged();

		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {

			// Look at the limit type and convert to a char
			String l = (String)combineTypeBox.getSelectedItem();
			if (l.equals("Mean"))
				testType = MonteCarloFilter.MEAN;
			else if (l.equals("Maximum"))
				testType = MonteCarloFilter.MAXIMUM;
			else if (l.equals("Median"))
				testType = MonteCarloFilter.MEDIAN;
			else {
				System.err.println("Didn't recognise limit type "+l);
			}
			optionsChanged();

		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {

			if (lse.getSource() == dataStoreList) {
				store = (DataStore)dataStoreList.getSelectedValue();				
			}

			else if (lse.getSource() == probeListList) {
				toList = (ProbeList)probeListList.getSelectedValue();
			}

			optionsChanged();
		}
	}

}
