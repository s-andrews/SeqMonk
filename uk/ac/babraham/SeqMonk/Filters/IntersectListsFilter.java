/**
 * Copyright Copyright 2010-19 Simon Andrews
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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * This filter collates probes from multiple existing probe lists.
 */
public class IntersectListsFilter extends ProbeFilter {
	
	private CollateListsOptionPanel optionsPanel = new CollateListsOptionPanel();
	

	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public IntersectListsFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Combines probes from multiple lists";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
						
		ProbeList newList = new ProbeList(collection.probeSet(),"Filtered Probes","", new String[0]);
		
		ProbeList [] includedLists = optionsPanel.includedLists();
		ProbeList [] excludedLists = optionsPanel.excludedLists();


		Probe [] probes = startingList.getAllProbes();
		
		boolean [] validProbes = new boolean[probes.length];
		
		for (int i=0;i<validProbes.length;i++)validProbes[i] = true;
		
		for (int i=0;i<includedLists.length;i++) {
			progressUpdated("Checking list "+(i+1)+" of "+(includedLists.length+excludedLists.length),i+1,(includedLists.length+excludedLists.length));
			HashSet<Probe> checkProbes = new HashSet<Probe>();
			
			Probe [] theseProbes = includedLists[i].getAllProbes();
			for (int p=0;p<theseProbes.length;p++) {
				checkProbes.add(theseProbes[p]);
			}
			
			for (int p=0;p<probes.length;p++) {
				if (!checkProbes.contains(probes[p])) {
					validProbes[p] = false;
				}
			}
		}
			
		for (int i=0;i<excludedLists.length;i++) {
			HashSet<Probe> checkProbes = new HashSet<Probe>();

			progressUpdated("Checking list "+(i+1+includedLists.length)+" of "+(includedLists.length+excludedLists.length),i+1+includedLists.length,(includedLists.length+excludedLists.length));

			Probe [] theseProbes = excludedLists[i].getAllProbes();
			for (int p=0;p<theseProbes.length;p++) {
				checkProbes.add(theseProbes[p]);
			}
			
			for (int p=0;p<probes.length;p++) {
				if (checkProbes.contains(probes[p])) {
					validProbes[p] = false;
				}
			}
		}

		
		for (int i=0;i<validProbes.length;i++) {
			if (validProbes[i])
				newList.addProbe(probes[i], null);
		}
			
		
		newList.setName("Intersection of "+(includedLists.length+excludedLists.length)+" lists");
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
				
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Intersect Probe Lists Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		ProbeList [] includedLists = optionsPanel.includedLists();
		ProbeList [] excludedLists = optionsPanel.excludedLists();
		
		b.append("Probes from ");
		b.append(startingList.name());
		b.append(" which were present in ");
		for (int i=0;i<includedLists.length;i++) {
			b.append(includedLists[i].name());
			if (i < includedLists.length-1) {
				b.append(",");
			}
		}

		b.append(" and excluded from ");
		for (int i=0;i<excludedLists.length;i++) {
			b.append(excludedLists[i].name());
			if (i < excludedLists.length-1) {
				b.append(",");
			}
		}
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {

		return "Intersection of "+(optionsPanel.includedLists().length+optionsPanel.excludedLists().length)+" lists";
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class CollateListsOptionPanel extends JPanel {
			
			private JCheckBox [] includeBoxes;
			private JCheckBox [] excludeBoxes;
			private ProbeList [] allLists;
			
			/**
			 * Instantiates a new values filter option panel.
			 */
			public CollateListsOptionPanel () {
				
				JPanel masterPanel = new JPanel();
				
				
				allLists = collection.probeSet().getAllProbeLists();
				setLayout(new BorderLayout());
				masterPanel.setLayout(new GridBagLayout());
				
				includeBoxes = new JCheckBox [allLists.length];
				excludeBoxes = new JCheckBox [allLists.length];
				
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx=0;
				gbc.gridy=0;
				gbc.weighty=0.01;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx=0.99;
				gbc.insets = new Insets(3, 3, 3, 3);
				
				
				gbc.gridx=0;
				JLabel listLabel = new JLabel("List");
				listLabel.setFont(new Font("default", Font.BOLD, 12));
				masterPanel.add(listLabel,gbc);
				
				gbc.weightx=0.01;
				gbc.gridx=1;
				JLabel includeLabel = new JLabel("Include");
				includeLabel.setFont(new Font("default", Font.BOLD, 12));
				masterPanel.add(includeLabel,gbc);
				
				gbc.gridx=2;
				JLabel excludeLabel = new JLabel("Exclude");
				excludeLabel.setFont(new Font("default", Font.BOLD, 12));
				masterPanel.add(excludeLabel,gbc);

				gbc.gridx=0;
				gbc.weightx=0.99;
				
				for (int i=0;i<allLists.length;i++) {
					gbc.gridy=i+1;
					masterPanel.add(new JLabel(allLists[i].toString()),gbc);
				}
				
				gbc.weightx=0.01;

				for (int i=0;i<allLists.length;i++) {
					gbc.gridy=i+1;
					gbc.gridx=1;
					includeBoxes[i] = new JCheckBox();
					masterPanel.add(includeBoxes[i],gbc);
					gbc.gridx=2;
					excludeBoxes[i] = new JCheckBox();
					masterPanel.add(excludeBoxes[i],gbc);
				}

				// Force the components to the top of the panel
				gbc.gridy++;
				gbc.gridx=1;
				gbc.gridwidth=3;
				gbc.fill = GridBagConstraints.BOTH;
				gbc.weighty=0.999;
				masterPanel.add(new JPanel(),gbc);
				
				
				add (new JScrollPane(masterPanel),BorderLayout.CENTER);
			}
			
			private ProbeList [] includedLists () {
				Vector<ProbeList> includedLists = new Vector<ProbeList>();
				
				for (int i=0;i<includeBoxes.length;i++) {
					if (includeBoxes[i].isSelected()) {
						includedLists.add(allLists[i]);
					}
				}
				
				return includedLists.toArray(new ProbeList[0]);
			}
			

			private ProbeList [] excludedLists () {
				Vector<ProbeList> excludedLists = new Vector<ProbeList>();
				
				for (int i=0;i<excludeBoxes.length;i++) {
					if (excludeBoxes[i].isSelected()) {
						excludedLists.add(allLists[i]);
					}
				}
				
				return excludedLists.toArray(new ProbeList[0]);
			}

			
			
			/* (non-Javadoc)
			 * @see javax.swing.JComponent#getPreferredSize()
			 */
			public Dimension getPreferredSize () {
				return new Dimension(400,500);
			}

	}
}
