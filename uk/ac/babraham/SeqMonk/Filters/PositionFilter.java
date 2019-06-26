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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeStrandType;

/**
 * Filters probes based on their position in the genome.
 */
public class PositionFilter extends ProbeFilter {
	
	private Chromosome chromosome = null;
	private Integer start = null;
	private Integer end = null;
	private ProbeStrandType strandType = null;

	private PositionFilterOptionsPanel optionsPanel;
	
	/**
	 * Instantiates a new position filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection is not quantitated.
	 */
	public PositionFilter (DataCollection collection) throws SeqMonkException {
		this(collection,null,0,0);
	}
	
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
	public PositionFilter (DataCollection collection, Chromosome selectedChromosome, int start, int end) throws SeqMonkException {
		super(collection);
		this.chromosome = selectedChromosome;
		this.start = start;
		this.end = end;

		optionsPanel = new PositionFilterOptionsPanel();
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters probes based on their genomic position";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		ProbeList newList = new ProbeList(startingList,"","",new String[0]);
		
		Probe [] probes = startingList.getAllProbes();
		
		// We only take probes which are completely enclosed in the selected region
		for (int p=0;p<probes.length;p++) {
			if (p % 10000 == 0) 
				progressUpdated(p, probes.length);
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

			if (chromosome != null) {
				if (probes[p].chromosome() != chromosome) continue;
			}
			
			if (! strandType.useProbe(probes[p])) continue;
			if (start != null && probes[p].start() < start) continue;
			if (end != null && probes[p].end() > end) continue;
			
			newList.addProbe(probes[p],null);

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
		
		if (start != null && start<0) return false;

		if (end != null && end<0) return false;
		
		if (start != null && end != null && end < start) return false;
		
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
		b.append("Probes from ");
		b.append(startingList.name());
		if (chromosome == null) {
			b.append(" on any chromosome ");			
		}
		else {
			b.append(" which are on chromosome ");
			b.append(chromosome.name());
		}
		b.append(" on strand ");
		b.append(strandType.toString());
		if (start != null && end != null) {
			b.append(" between ");
			b.append(start);
			b.append(" and ");
			b.append(end);
		}
		else if (start != null) {
			b.append(" after ");
			b.append(start);
		}
		else if (end != null) {
			b.append(" before ");
			b.append(end);
		}
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		
		String startString = "start";
		String endString = "end";
		
		if (start != null) {
			startString = start.toString();
		}
		if (end != null) {
			endString = end.toString();
		}
		
		if (chromosome != null) {
			if (startString.equals("start") && endString.equals("end")) {
				return "Chr "+chromosome.name();				
			}
			else {
				return "Chr "+chromosome.name()+" "+startString+"-"+endString;
			}
		}
		else {
			if (startString.equals("start") && endString.equals("end")) {
				return "Any chromosome";
			}
			else {
				return "Any chromosome "+startString+"-"+endString;
			}
		}
	}

	/**
	 * The PositionFilterOptionsPanel.
	 */
	private class PositionFilterOptionsPanel extends JPanel implements KeyListener, ItemListener {
	
			private JComboBox chromosomeBox;
			private JComboBox strandBox;
			private JTextField startField;
			private JTextField endField;
			
			/**
			 * Instantiates a new position filter options panel.
			 */
			public PositionFilterOptionsPanel () {
				setLayout(new GridBagLayout());
			
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.gridx=0;
				gbc.gridy=0;
				gbc.weightx=0.2;
				gbc.weighty=0.5;
				gbc.fill=GridBagConstraints.HORIZONTAL;
				gbc.insets = new Insets(5,5,5,5);
			
			
				add(new JLabel("Chromosome",JLabel.RIGHT),gbc);
		
				gbc.gridx++;
				gbc.weightx=0.6;
			
				Chromosome [] chrs = collection.genome().getAllChromosomes();
				Object [] chrOptions = new Object [chrs.length+1];
				chrOptions[0] = "[Any]";
				for (int i=0;i<chrs.length;i++) {
					chrOptions[i+1] = chrs[i];
				}
			
				chromosomeBox = new JComboBox(chrOptions);
				chromosomeBox.addItemListener(this);
				if (chromosome != null) {
					chromosomeBox.setSelectedItem(chromosome);
				}
			
				add(chromosomeBox,gbc);

				gbc.gridx=0;
				gbc.gridy++;
				gbc.weightx=0.2;
				
				add(new JLabel("Strand",JLabel.RIGHT),gbc);
				
				gbc.gridx++;
				gbc.weightx=0.6;
			
				ProbeStrandType [] strandTypes = ProbeStrandType.getTypeOptions();
				strandBox = new JComboBox(strandTypes);
				strandBox.addItemListener(this);
				strandType = strandTypes[0];
			
				add(strandBox,gbc);
				
				gbc.gridx=0;
				gbc.gridy++;
				gbc.weightx=0.2;
			
				add(new JLabel("From ",JLabel.RIGHT),gbc);
			
				gbc.gridx++;
				gbc.weightx=0.6;
			
				startField = new JTextField(""+start,5);
				startField.addKeyListener(this);
				add(startField,gbc);
	
				gbc.gridx=0;
				gbc.gridy++;
				gbc.weightx=0.2;
			
				add(new JLabel("To ",JLabel.RIGHT),gbc);
			
				gbc.gridx++;
				gbc.weightx=0.6;
			
				endField = new JTextField(""+end,5);
				endField.addKeyListener(this);
				add(endField,gbc);		
	
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
			return new Dimension(350,250);
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
				if (f == startField) {
					if (f.getText().length() == 0) start = null;
					else {
						start = Integer.parseInt(f.getText());
					}
				}
				else if (f == endField) {
					if (f.getText().length() == 0) end = null;
					else {
						end = Integer.parseInt(f.getText());
					}
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}
			
			optionsChanged();
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent e) {
			if (e.getSource() == chromosomeBox) {
				if (chromosomeBox.getSelectedItem() instanceof Chromosome) {
					chromosome = (Chromosome)chromosomeBox.getSelectedItem();
				}
				else {
					// This is the any option
					chromosome = null;
				}
			}
			else if (e.getSource() == strandBox) {
				strandType = (ProbeStrandType)strandBox.getSelectedItem();
			}
			optionsChanged();
		}
		}
}
