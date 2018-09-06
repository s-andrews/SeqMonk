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
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.northwestern.at.utils.math.statistics.FishersExactTest;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BenjHochFDR;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.ProbeTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * Filters probes based on a Fisher's exact test determining that
 * they represent a different proportion of the library as a whole
 */
public class ProportionOfLibraryStatisticsFilter extends ProbeFilter {

	private Double pValueLimit = 0.05;
	private boolean applyMultipleTestingCorrection = true;
	private boolean testForIncreasesOnly = true;

	private DataStore [] fromStores = new DataStore[0];
	private DataStore [] toStores = new DataStore[0];

	private ProportionStatsOptionsPanel optionsPanel;

	/**
	 * Instantiates a new differences filter with default options.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public ProportionOfLibraryStatisticsFilter (DataCollection collection) throws SeqMonkException {
		super(collection);

		Probe [] probes = startingList.getAllProbes();

		// Put out a warning if we see that we're not using an integer dataset.  We can probably
		// just check two datastores.  It's unlikely that we wouldn't spot a problem with 2 stores

		DataStore [] stores = collection.getAllDataStores();
		int quantitatedCheckedCount = 0;

		STORES: for (int s=0;s<stores.length;s++) {
			if (!stores[s].isQuantitated()) continue;

			for (int p=0;p<probes.length;p++) {
				float value = stores[s].getValueForProbe(probes[p]);
				if (value != (int)value) {
					JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>This test is designed to work with raw counts, and your libraries appear to have been normalised.<br>P-values obtained from this method of analysis may not be valid.</html>", "Non-integer list used", JOptionPane.WARNING_MESSAGE);
					break STORES;
				}
			}

			++quantitatedCheckedCount;
			if (quantitatedCheckedCount > 2) break STORES;
		}

		optionsPanel = new ProportionStatsOptionsPanel();


	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList() {

		fromStores = optionsPanel.fromStores();
		toStores = optionsPanel.toStores();
		
//		System.err.println("Found "+fromStores.length+" from stores and "+toStores.length+" to stores");
		
		applyMultipleTestingCorrection = optionsPanel.multipleTestingBox.isSelected();
		testForIncreasesOnly = optionsPanel.increasesOnlyBox.isSelected();

		Probe [] probes = startingList.getAllProbes();

		// We'll pull the number of probes to sample from the preferences if they've changed it

		ProbeList newList = new ProbeList(startingList,"Filtered Probes","","Diff p-value");

		// We'll build up a set of p-values as we go along
		float [] lowestPValues = new float[probes.length];
		for (int p=0;p<lowestPValues.length;p++) {
			lowestPValues[p] = 1;
		}


		// Put something in the progress whilst we're ordering the probe values to make
		// the comparison.
		progressUpdated("Generating background model",0,1);

		try {
			for (int f=0;f<fromStores.length;f++) {
				for (int t=0;t<toStores.length;t++) {

					progressUpdated("Comparing "+fromStores[f]+" to "+toStores[t],0,1);

					// We need to work out the total counts in the probes we're using
					int fromTotalCount = 0;
					for (int p=0;p<probes.length;p++) {
						fromTotalCount += (int)fromStores[f].getValueForProbe(probes[p]);
						if (cancel) {
							cancel = false;
							progressCancelled();
							return;
						}
					}
					int toTotalCount = 0;
					for (int p=0;p<probes.length;p++) {
						toTotalCount += (int)toStores[t].getValueForProbe(probes[p]);
						if (cancel) {
							cancel = false;
							progressCancelled();
							return;
						}
					}
					
//					System.err.println("Total count for "+fromStores[f].name()+" is "+fromTotalCount);
//					System.err.println("Total count for "+toStores[t].name()+" is "+toTotalCount);


					for (int p=0;p<probes.length;p++) {

//						System.err.println("Checking probe "+probes[p].name());
						
						if (cancel) {
							cancel = false;
							progressCancelled();
							return;
						}

						int n11 = (int)fromStores[f].getValueForProbe(probes[p]);
						int n12 = fromTotalCount - n11;
						int n21 = (int)toStores[t].getValueForProbe(probes[p]);
						int n22 = toTotalCount - n21;
						
						double [] pValues = FishersExactTest.fishersExactTest(n11, n12, n21, n22);	

//						System.err.println("N11="+n11+" N12="+n12+" N21="+n21+" N22="+n22+" p="+pValues[0]);
						
						// The values in the array are 0=2-sided p-value, 1=left-sided p-value, 2=right-sided p-value
						if (testForIncreasesOnly) {
							if (pValues[1] < lowestPValues[p]) lowestPValues[p] = (float)pValues[1];
						}
						else {
							if (pValues[0] < lowestPValues[p]) lowestPValues[p] = (float)pValues[0];							
						}
					}

				}	

			}
		}
		catch (SeqMonkException sme) {
			progressExceptionReceived(sme);
		}


		// Now we can go through the lowest P-value set and see if any of them
		// pass the filter.
		
		if (applyMultipleTestingCorrection) {
			ProbeTTestValue [] statsValues = new ProbeTTestValue[probes.length];
			for (int i=0;i<probes.length;i++) {
				statsValues[i] = new ProbeTTestValue(probes[i], lowestPValues[i]);
			}
			
			BenjHochFDR.calculateQValues(statsValues);
			
			for (int i=0;i<statsValues.length;i++) {
				if (statsValues[i].q < pValueLimit) {
					newList.addProbe(statsValues[i].probe, (float)statsValues[i].q);
				}
			}
			
		}
		
		else {
			for (int i=0;i<lowestPValues.length;i++) {
				if (lowestPValues[i] < pValueLimit) {
					newList.addProbe(probes[i],lowestPValues[i]);
				}
			}
		}

		filterFinished(newList);		
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the intensity corrected statistical differnce between stores";
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
		// Check for the special case of comparing just to ourselves
		if (fromStores.length==1 && toStores.length==1 && fromStores[0]==toStores[0]) {
			return false;
		}
		if (fromStores.length > 0 && toStores.length > 0 && pValueLimit != null) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Proportion of Library Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where minimum ");

		b.append("p-value when comparing proportion of counts between ");

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


		b.append(" was below ");

		b.append(pValueLimit);

		if (applyMultipleTestingCorrection) {
			b.append(" multiple testing correction applied ");
		}
		
		if (testForIncreasesOnly) {
			b.append(" testing for increases only ");
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

		b.append("Intensity Difference p<");
		b.append(pValueLimit);
		return b.toString();
	}

	/**
	 * The DifferencesOptionsPanel.
	 */
	private class ProportionStatsOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ItemListener {

		private JList fromDataList;
		private JList toDataList;
		private JTextField pValueField;
		private JCheckBox multipleTestingBox;
		private JCheckBox increasesOnlyBox;

		/**
		 * Instantiates a new differences options panel.
		 */
		public ProportionStatsOptionsPanel () {

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
			fromStores=SeqMonkApplication.getInstance().drawnDataStores();
			fromDataList.setCellRenderer(new TypeColourRenderer());
			fromDataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(fromDataList),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.01;

			dataPanel.add(new JLabel("To Data Store / Group",JLabel.CENTER),dpgbc);

			dpgbc.gridy++;
			dpgbc.weighty=0.99;

			toDataList = new JList(fromDataModel);
			ListDefaultSelector.selectDefaultStores(toDataList);
			toStores=SeqMonkApplication.getInstance().drawnDataStores();
			toDataList.setCellRenderer(new TypeColourRenderer());
			toDataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(toDataList),dpgbc);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=1;
			gbc.gridy=1;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx=0.5;
			gbc.weighty=0.5;

			choicePanel.add(new JLabel("P-value must be below "),gbc);
			pValueField = new JTextField(""+pValueLimit,5);
			pValueField.addKeyListener(this);

			gbc.gridx = 2;
			choicePanel.add(pValueField,gbc);
			
			gbc.gridx = 1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Only test for coverage increase"),gbc);

			gbc.gridx=2;

			increasesOnlyBox = new JCheckBox();
			increasesOnlyBox.setSelected(true);
			choicePanel.add(increasesOnlyBox,gbc);

			gbc.gridx = 1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Apply Multiple Testing Correction"),gbc);

			gbc.gridx=2;

			multipleTestingBox = new JCheckBox();
			multipleTestingBox.setSelected(true);
			choicePanel.add(multipleTestingBox,gbc);

			add(new JScrollPane(choicePanel),BorderLayout.CENTER);

		}
		
		public DataStore [] fromStores () {
			Object [] fromObjects = fromDataList.getSelectedValues();
			DataStore [] fromStores = new DataStore[fromObjects.length];
			for (int i=0;i<fromObjects.length;i++) {
				fromStores[i] = (DataStore)fromObjects[i];
			}
			
			return fromStores;
		}

		public DataStore [] toStores () {
			Object [] toObjects = toDataList.getSelectedValues();
			DataStore [] toStores = new DataStore[toObjects.length];
			for (int i=0;i<toObjects.length;i++) {
				toStores[i] = (DataStore)toObjects[i];
			}
			
			return toStores;
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

			Double d = null;

			if (f.getText().length()>0) {

				if (f.getText().equals("-")) {
					d = 0d;
				}
				else {
					try {
						d = Double.parseDouble(f.getText());
					}
					catch (NumberFormatException e) {
						f.setText(f.getText().substring(0,f.getText().length()-1));
						return;
					}
				}
			}

			if (f == pValueField) {
				pValueLimit = d;
			}
			else {
				System.err.println("Unexpected text field "+f+" sending data to keylistener in differences filter");
			}
			optionsChanged();

		}

		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {
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
	}

}
