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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * Generates random lists of probes of a fixed size.
 */
public class RandomFilter extends ProbeFilter {
	
	private int numberOfListsToGenerate = 1;
	private int numberOfProbesPerList = 0;
	private int probesInStartingList = 0;

	private RandomFilterOptionsPanel optionsPanel;
	
	/**
	 * Instantiates a new position filter with all options set to allow the
	 * filter to be run immediately.  Any probes overlapping the region of
	 * interest will be selected.
	 * 
	 * @param collection The dataCollection to filter
	 * @param selectedChromosome The chromosome to use
	 * @param start The start of the region of interest
	 * @param end The end of the region of interest.
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public RandomFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		if (collection.probeSet() != null) {
			probesInStartingList = collection.probeSet().getActiveList().getAllProbes().length;
			numberOfProbesPerList = Math.min(probesInStartingList/2,100);
		}
		
		optionsPanel = new RandomFilterOptionsPanel();
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Generates randomly selected subsets of probes";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		ProbeList newList = new ProbeList(startingList,"","",new String [0]);
		
		Probe [] probes = startingList.getAllProbes();
		
		// This list is a sublist and is only used if we're generating more than one list
		ProbeList currentList = null;
		HashSet<Probe> seenProbes = new HashSet<Probe>();

		for (int i=1;i<=numberOfListsToGenerate;i++) {
			
			if (numberOfListsToGenerate > 1) {
				currentList = new ProbeList(newList, "Random list "+i,listDescription(), new String[0]);
			}
			// Make a random set
			
			
			Collections.shuffle(Arrays.asList(probes));
			for (int j=0;j<numberOfProbesPerList;j++) {

				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}
				
				if (numberOfListsToGenerate == 1) {
					newList.addProbe(probes[j], null);
				}
				else {
					currentList.addProbe(probes[j],null);
					if (!seenProbes.contains(probes[j])) {
						newList.addProbe(probes[j], null);
						seenProbes.add(probes[j]);
					}
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
		
		if (numberOfListsToGenerate <=0) return false;
		if (numberOfProbesPerList <=0) return false;
		if (numberOfProbesPerList > probesInStartingList) return false;
		
		return true;
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Position Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		b.append("A random subset of ");
		b.append(numberOfProbesPerList);
		
		b.append(" probes from ");
		b.append(collection.probeSet().getActiveList().name());
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		return "Random Probes";
	}

	/**
	 * The RandomFilterOptionsPanel.
	 */
	private class RandomFilterOptionsPanel extends JPanel implements KeyListener {
	
			private JTextField numberOfListsField;
			private JTextField numberOfProbesField;
			
			/**
			 * Instantiates a new position filter options panel.
			 */
			public RandomFilterOptionsPanel () {
				setLayout(new GridBagLayout());
			
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx=0;
				gbc.gridy=0;
				gbc.weightx=0.2;
				gbc.weighty=0.5;
				gbc.fill=GridBagConstraints.HORIZONTAL;
				gbc.insets = new Insets(5,5,5,5);
						
				add(new JLabel("Number of lists to generate ",JLabel.RIGHT),gbc);
			
				gbc.gridx++;
				gbc.weightx=0.6;
			
				numberOfListsField = new JTextField(""+numberOfListsToGenerate,5);
				numberOfListsField.addKeyListener(this);
				add(numberOfListsField,gbc);
	
				gbc.gridx=0;
				gbc.gridy++;
				gbc.weightx=0.2;
			
				add(new JLabel("Number of probes per list ",JLabel.RIGHT),gbc);
			
				gbc.gridx++;
				gbc.weightx=0.6;
			
				numberOfProbesField = new JTextField(""+numberOfProbesPerList,5);
				numberOfProbesField.addKeyListener(this);
				add(numberOfProbesField,gbc);		
	
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		public void keyTyped(KeyEvent arg0) {
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(350,200);
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
				if (f == numberOfListsField) {
					if (f.getText().length() == 0) numberOfListsToGenerate = 0;
					else {
						numberOfListsToGenerate = Integer.parseInt(f.getText());
					}
				}
				else if (f == numberOfProbesField) {
					if (f.getText().length() == 0) numberOfProbesPerList = 0;
					else {
						numberOfProbesPerList = Integer.parseInt(numberOfProbesField.getText());
					}
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}
			
			optionsChanged();
		}
	
	}
}
