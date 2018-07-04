/**
 * Copyright Copyright 2010-18 Simon Andrews and 2009 Ieuan Clay
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;

/**
 * A quantitation which corrects all dataStores relative to a reference store.
 */
public class ManualCorrectionQuantitation extends Quantitation {
	private static Hashtable<String, Float> storedCorrections = new Hashtable<String, Float>();
	
	private static final int ADD = 1;
	private static final int SUBTRACT = 2;
	private static final int MULTIPLY = 3;
	private static final int DIVIDE = 4;
		
	private JPanel optionPanel = null;
	private JComboBox correctionActions;
	private DataStore [] data = null;
	private JTextField [] correctionFactors = null;
	private int correctionAction;
	

	public ManualCorrectionQuantitation(SeqMonkApplication application) {
		super(application);
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Probe [] probes = application.dataCollection().probeSet().getAllProbes();

		// Clear the cache so we can store new values
		storedCorrections.clear();
		
		for (int d=0;d<data.length;d++) {

			// See if we need to quit
			if (cancel) {
				progressCancelled();
				return;
			}

			progressUpdated(d, data.length);

			// If they haven't specified a correction factor then we don't do anything
			if (correctionFactors[d].getText().trim().length()==0) {
				continue;
			}

			// Get the correction value
			float correctionFactor = Float.parseFloat(correctionFactors[d].getText());
			storedCorrections.put(data[d].name(), correctionFactor);

			// Apply the correction to all probes

			for (int p=0;p<probes.length;p++) {

				// See if we need to quit
				if (cancel) {
					progressCancelled();
					return;
				}

				// get reference value, storing, so we don't need to fetch it more times than necessary
				try {
					float existingValue = data[d].getValueForProbe(probes[p]);

					switch (correctionAction) {
					
					case (ADD): existingValue += correctionFactor;break;
					case (SUBTRACT): existingValue -= correctionFactor;break;
					case (MULTIPLY): existingValue *= correctionFactor;break;
					case (DIVIDE): existingValue /= correctionFactor;break;
					default: throw new IllegalArgumentException("Didn't understand correction action "+correctionAction);
					
					}

					data[d].setValueForProbe(probes[p], existingValue);

				} 

				catch (SeqMonkException e) {
					throw new IllegalStateException(e);
				}	

			}	
		}
		quantitatonComplete();

	}
	
	public String description () {
		String existingDescription = "Unknown quantitation";
		if (application.dataCollection().probeSet().currentQuantitation() != null) {
			existingDescription = application.dataCollection().probeSet().currentQuantitation();
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append(existingDescription);
		sb.append(" transformed into by manual correction using ");
		sb.append(correctionActions.getSelectedItem().toString());
		sb.append(" with values");
		for (int i=0;i<correctionFactors.length;i++) {
			if (correctionFactors[i].getText().length() > 0) {
				sb.append(" ");
				sb.append(data[i].name());
				sb.append("=");
				sb.append(correctionFactors[i].getText());
			}
		}
		
		return sb.toString();
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
		optionPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.weightx=0.5;
		gbc.weighty=0.1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		
		optionPanel.add(new JLabel("Method of correction"),gbc);
		gbc.gridx = 2;
		correctionActions = new JComboBox(new String [] {"Add","Subtract","Multiply","Divide"});
		optionPanel.add(correctionActions,gbc);

		Vector<DataStore>quantitatedStores = new Vector<DataStore>();

		DataSet [] sets = application.dataCollection().getAllDataSets();
		for (int s=0;s<sets.length;s++) {
			if (sets[s].isQuantitated()) {
				quantitatedStores.add(sets[s]);
			}
		}
		DataGroup [] groups = application.dataCollection().getAllDataGroups();
		for (int g=0;g<groups.length;g++) {
			if (groups[g].isQuantitated()) {
				quantitatedStores.add(groups[g]);
			}
		}
		
		data = quantitatedStores.toArray(new DataStore[0]);
		correctionFactors = new JTextField[data.length];
		
		// Now we work our way through the sets
		
		JPanel correctionPanel = new JPanel();
		correctionPanel.setLayout(new GridBagLayout());
		GridBagConstraints cbc = new GridBagConstraints();
		cbc.gridx=1;
		cbc.gridy=1;
		cbc.weightx=0.5;
		cbc.weighty=0.5;
		cbc.insets = new Insets(1, 3, 1, 3);
		cbc.fill=GridBagConstraints.HORIZONTAL;
		
		for (int d=0;d<data.length;d++) {
			cbc.gridx=1;
			cbc.gridy++;
			correctionPanel.add(new JLabel(data[d].name()),cbc);
			cbc.gridx=2;
			correctionFactors[d] = new JTextField();
			if (storedCorrections.containsKey(data[d].name())) {
				correctionFactors[d].setText(""+storedCorrections.get(data[d].name()));
			}
			correctionFactors[d].addKeyListener(new NumberKeyListener(true, true));
			
			correctionPanel.add(correctionFactors[d],cbc);
		}
		
		gbc.gridx=1;
		gbc.gridy++;
		gbc.gridwidth = 2;
		
		optionPanel.add(new JLabel("Correction factors",JLabel.CENTER),gbc);
		
		gbc.gridy++;
		gbc.weighty=0.9;
		gbc.fill =  GridBagConstraints.BOTH;
		
		optionPanel.add(new JScrollPane(correctionPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),gbc);
				
		gbc.gridx=1;
		gbc.gridy++;
		gbc.gridwidth = 2;
		gbc.weighty = 0.001;
		gbc.fill = GridBagConstraints.NONE;
		
		JButton loadFromFileButton = new JButton("Load from file");
		loadFromFileButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getDataLocation());
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileFilter(new TxtFileFilter());
				
				int result = chooser.showOpenDialog(optionPanel);
				if (result == JFileChooser.CANCEL_OPTION) return;

				File file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedDataLocation(file);
			
				try {
					BufferedReader br = new BufferedReader(new FileReader(file));

					String line;
					while ((line = br.readLine()) != null) {
						line.trim();
						if (line.startsWith("#")) continue;
						
						String [] sections = line.split("\t");
						if (sections.length < 2) continue;
						if (sections[1].length() == 0) continue;
						
						// See if we can find a dataset with the name of this section
						for (int d=0;d<data.length;d++) {
							if (data[d].name().equals(sections[0])) {
								// We try setting the value to whatever is stored for this
								try {
									Double.parseDouble(sections[1]);
									correctionFactors[d].setText(sections[1]);
								}
								catch (NumberFormatException nfe) {
									JOptionPane.showMessageDialog(optionPanel, "Skipping "+sections[0]+" as "+sections[1]+" isn't a number", "Correction import error", JOptionPane.WARNING_MESSAGE);									
								}
							}
						}
					}
					
					br.close();
				}
				catch (IOException ioe) {
					JOptionPane.showMessageDialog(optionPanel, "Failed to read input file:"+ioe.getMessage(), "Correction import error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
		});
		
		
		
		optionPanel.add(loadFromFileButton,gbc);
		
		return optionPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#isReady()
	 */
	public boolean isReady() {

		//TODO: Should we check that there is at least one correction?
		
		return true;
	
	}	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#quantitate(uk.ac.babraham.SeqMonk.DataTypes.DataStore[])
	 */
	public void quantitate(DataStore[] data) {

		// We're actually going to ignore the list of stores passed to
		// us and just use the ones which have some data in the corrections
		// instead;
		
		// We need to set the correction action
		if (correctionActions.getSelectedItem().equals("Add")) {
			correctionAction = ADD;
		}
		else if (correctionActions.getSelectedItem().equals("Subtract")) {
			correctionAction = SUBTRACT;
		}
		else if (correctionActions.getSelectedItem().equals("Divide")) {
			correctionAction = DIVIDE;
		}
		else if (correctionActions.getSelectedItem().equals("Multiply")) {
			correctionAction = MULTIPLY;
		}
		else {
			throw new IllegalArgumentException("Didn't undestand correction action "+correctionActions.getSelectedItem());
		}
		
		Thread t = new Thread(this);
		cancel = false;
		t.start();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Quantitation.Quantitation#requiresExistingQuantitation()
	 */
	public boolean requiresExistingQuantitation() {
		return true;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Manual Correction Quantitation";
	}
	
}