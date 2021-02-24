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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * Creates a new probeList by combining two existing lists
 */
public class CombineFilter extends ProbeFilter {

	private ProbeList firstList;
	private ProbeList secondList;
	private int combineType;
	private CombineFilterOptionsPanel optionsPanel;
	
	public static final int AND = 1;
	public static final int OR = 2;
	public static final int BUTNOT = 3;

	
	/**
	 * Instantiates a new combine filter with enough options to allow it
	 * to be run immediately
	 * 
	 * @param collection The dataCollection to filter
	 * @param firstList The first list to filter
	 * @param secondList The second list to filter
	 * @param combineType How to combine the filters, use constants AND, OR, BUTNOT
	 * @throws SeqMonkException if the dataCollection is not quantitated
	 */
	public CombineFilter (DataCollection collection, ProbeList firstList, ProbeList secondList, int combineType) throws SeqMonkException {
		super(collection);
		this.firstList = firstList;
		this.secondList = secondList;
		if (combineType == AND || combineType == OR || combineType == BUTNOT) {
			this.combineType = combineType;
		}
		else {
			throw new IllegalArgumentException("Combine type must be AND, OR or BUTNOT");
		}
		optionsPanel = new CombineFilterOptionsPanel();
	}

	/**
	 * Instantiates a new combine filter with default options
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException If the collection is not quantitated
	 */
	public CombineFilter (DataCollection collection) throws SeqMonkException {
		this(collection,collection.probeSet(),collection.probeSet(),AND);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Creates a new list by combining two existing lists";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		ProbeList newList;
		
		if (combineType == OR && firstList.parent() == secondList.parent()) {
			newList= new ProbeList(firstList.parent(),"","",new String[0]);
		}
		else {
			newList= new ProbeList(firstList,"","",new String[0]);			
		}
		
		Probe [] firstListProbes = firstList.getAllProbes();
		Probe [] secondListProbes = secondList.getAllProbes();
		
		int l1 = 0;
		int l2 = 0;
		
		while (true) {
			
			if (l1 >= firstListProbes.length) {
				// All of the remaining l2 probes are unique to l2
				if (combineType == OR) {
					for (int i=l2;i<secondListProbes.length;i++) {
						newList.addProbe(secondListProbes[i], null);
					}
				}
				break;
			}

			if (l2 >= secondListProbes.length) {
				// All of the remaining l1 probes are unique to l1
				if (combineType == OR || combineType == BUTNOT) {
					for (int i=l1;i<firstListProbes.length;i++) {
						newList.addProbe(firstListProbes[i], null);
					}
				}
				break;
			}
			
			progressUpdated(l1+l2, firstListProbes.length+secondListProbes.length);
			
			// Compare the two current probes to see what state we're in
			
			if (firstListProbes[l1] == secondListProbes[l2]) {
				//This probe is common to both lists
				if (combineType == AND || combineType == OR) {
					newList.addProbe(firstListProbes[l1], null);
				}
				++l1;
				++l2;
			}
			else if (firstListProbes[l1].compareTo(secondListProbes[l2]) > 0) {
				// We can make a decision about the lower value (l2)
				if (combineType == OR) {
					newList.addProbe(secondListProbes[l2], null);
				}
				l2++;
			}
			else {
				// We can make a decision about the lower value (l1)
				if (combineType == OR || combineType == BUTNOT) {
					newList.addProbe(firstListProbes[l1], null);
				}
				l1++;
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
		if (firstList == null || secondList == null || firstList == secondList) return false;
		if (! (combineType == AND || combineType == OR || combineType == BUTNOT)) return false;
		
		return true;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		return listName();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		if (! isReady()) return "Not ready";
		
		String combineString = null;
		
		switch (combineType) {
		case AND: 
			combineString = "AND";
			break;
		case OR: 
			combineString = "OR";
			break;
		case BUTNOT: 
			combineString = "BUTNOT";
			break;
		default:
			throw new IllegalStateException("Unknown combine type "+combineType+" in combine filter");
		}
		
		return firstList.name()+" "+combineString+" "+secondList.name();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Combine Filter";
	}


	/**
	 * The CombineFilterOptionsPanel.
	 */
	private class CombineFilterOptionsPanel extends JPanel implements ItemListener {
	
		private JComboBox firstListBox;
		private JComboBox typeBox;
		private JComboBox secondListBox;
		
		/**
		 * Instantiates a new combine filter options panel.
		 */
		public CombineFilterOptionsPanel() {
	
			setLayout(new BorderLayout());
	
			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx=0;
			c.gridy=0;
			c.weightx=0.5;
			c.weighty=0.5;
			c.fill=GridBagConstraints.NONE;
			c.anchor=GridBagConstraints.CENTER;
	
			ProbeList [] lists = collection.probeSet().getAllProbeLists();
	
			firstListBox = new JComboBox(lists);
			firstListBox.setSelectedItem(firstList);
			firstListBox.addItemListener(this);
			choicePanel.add(firstListBox,c);
	
			c.gridy++;
			typeBox = new JComboBox(new String [] {"AND","OR","BUTNOT"});
			typeBox.addItemListener(this);
			choicePanel.add(typeBox,c);
	
			c.gridy++;
			secondListBox = new JComboBox(lists);
			secondListBox.setSelectedItem(secondList);
			secondListBox.addItemListener(this);
			choicePanel.add(secondListBox,c);
	
			add(new JScrollPane(choicePanel),BorderLayout.CENTER);
	
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(500,200);
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() == firstListBox) {
				firstList = (ProbeList)firstListBox.getSelectedItem();
			}
			else if (e.getSource() == secondListBox) {
				secondList = (ProbeList)secondListBox.getSelectedItem();
			}
			else if (e.getSource() == typeBox) {
				String type = (String)typeBox.getSelectedItem();
				
				if (type.equals("AND")) {
					combineType = AND;
				}
				else if (type.equals("OR")) {
					combineType = OR;
				}
				else if (type.equals("BUTNOT")) {
					combineType = BUTNOT;
				}
				else {
					throw new IllegalStateException("Unknown combine type "+type+" when changing combine filter options");
				}
			}
			else {
				throw new IllegalStateException("Unknown source for selection event in combine filter");
			}
			optionsChanged();
		}
	}
}
