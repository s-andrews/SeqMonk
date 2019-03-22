/**
 * Copyright 2011-19 Simon Andrews
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * The deduplication filter allows the removal of redundancy from a
 * probe list, either based on the probes having a common prefix
 * to their names, or by looking for overlapping probes.
 * 
 * @author andrewss
 *
 */
public class DeduplicationFilter extends ProbeFilter {

	private DeduplicationFilterOptionsPanel optionsPanel = new DeduplicationFilterOptionsPanel();
	
	public DeduplicationFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}

	public String description() {
		return "Removes duplicated probes based either on their names or positional overlaps";
	}

	protected void generateProbeList() {

		ProbeList newList = new ProbeList(startingList,"","",startingList.getValueNames());
		
		Probe [] probes = startingList.getAllProbes();

		boolean useNames = optionsPanel.filterOnNames();

		// Find out what we're using to select the best hit
		boolean discardDuplicates = optionsPanel.discardDuplicates();
		boolean useHighest = optionsPanel.selectHighest();
		boolean useLength = optionsPanel.selectLength();
		
		// For the name filtering we need to keep track of the longest probe
		// with each suffix.
		HashMap<String, Probe> probePrefixes = null;
		
		HashSet<String> duplicatedProbePrefixes = new HashSet<String>();
		
		// For overlap filtering we need to keep track of the last valid 
		// probe to test against when looking for overlaps.
		Probe lastValidProbe = null;
		// For the discard option we'll also need to keep track of whether
		// the last valid probe was overlapped so we can throw it away when
		// we get to it.
		boolean lastValidWasOverlapped = false;
		
		if (useNames) {
			probePrefixes = new HashMap<String, Probe>();
		}
		else {
			// If we're sorting by overlap then we need the probes to
			// come in position order.
			Arrays.sort(probes);
		}
		
		
		for (int p=0;p<probes.length;p++) {
			
			if (p % 10000 == 0) 
				progressUpdated(p, probes.length);
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			
			if (useNames) {
				String prefix = probes[p].name().replaceFirst(optionsPanel.regexPattern(), "");
				
//				if (prefix.equals(probes[p].name())) {
//					System.err.println("No change when stripping "+optionsPanel.regexPattern()+" from '"+probes[p].name()+"'");
//				}

				if (probePrefixes.containsKey(prefix)) {
					
					if (discardDuplicates) {
						// We don't need to do anything other than record that this happened since
						// we're going to be throwing all of the probes away anyhow
						duplicatedProbePrefixes.add(prefix);
					}
					
					else if (useLength) {
						if (useHighest) {
							if (probes[p].length() > probePrefixes.get(prefix).length()) {
								probePrefixes.put(prefix, probes[p]);						
							}
						}
						else {
							if (probes[p].length() < probePrefixes.get(prefix).length()) {
								probePrefixes.put(prefix, probes[p]);						
							}
						}
					}
					else {
						// TODO: Fix this so they either specify a value, and allow us to cope if there isn't one.
						if (useHighest) {
							if (startingList.getValuesForProbe(probes[p])[0] > startingList.getValuesForProbe(probePrefixes.get(prefix))[0]) {
								probePrefixes.put(prefix, probes[p]);
							}
						}
						else {
							if (startingList.getValuesForProbe(probes[p])[0] < startingList.getValuesForProbe(probePrefixes.get(prefix))[0]) {
							probePrefixes.put(prefix, probes[p]);
							}
						}
					}
				}
				else {
					probePrefixes.put(prefix, probes[p]);
				}
			}
			
			else {
				
				if (lastValidProbe == null) {
					lastValidProbe = probes[p];
					lastValidWasOverlapped = false;
				}
				else {
					// Look for an overlap between this probe and 
					// the last valid probe.  If we don't find one,
					// or if the overlap is too small we keep the
					// last one and make this probe the last valid
					// one.
					
					if (lastValidProbe.chromosome() == probes[p].chromosome()) {
						
						int overlap = (Math.min(lastValidProbe.end(),probes[p].end())-Math.max(lastValidProbe.start(), probes[p].start()))+1;

						if (overlap > 0) {
							double percentOverlap = (overlap*100d)/(Math.min(lastValidProbe.length(), probes[p].length()));
							if (percentOverlap > optionsPanel.percentOverlap()) {
								// This is a valid overlap
								lastValidWasOverlapped = true;
								
								if (useLength) {
									if (useHighest) {
										if (probes[p].length() > lastValidProbe.length()) {
											lastValidProbe = probes[p];						
										}
									}
									else {
										if (probes[p].length() < lastValidProbe.length()) {
											lastValidProbe = probes[p];						
										}
									}
								}
								else {
									if (useHighest) {
										if (startingList.getValuesForProbe(probes[p])[0] > startingList.getValuesForProbe(lastValidProbe)[0]) {
											lastValidProbe = probes[p];
										}
									}
									else {
										if (startingList.getValuesForProbe(probes[p])[0] < startingList.getValuesForProbe(lastValidProbe)[0]) {
											lastValidProbe = probes[p];
										}
									}
								}
																
								continue;
							}
						}
						
					}
					
					// We'd normally keep the last overlap, unless we're discarding duplicates and the
					// last probe was overlapped.
					if (!(discardDuplicates & lastValidWasOverlapped)) {
						newList.addProbe(lastValidProbe, startingList.getValuesForProbe(lastValidProbe));
					}

					lastValidProbe = probes[p];
					lastValidWasOverlapped = false;
						
				}
				
			}
			
		}
		
		// Add the last stored probe
		if (lastValidProbe != null) {
			newList.addProbe(lastValidProbe, startingList.getValuesForProbe(lastValidProbe));
		}

		
		if (useNames) {
			Iterator<String>it = probePrefixes.keySet().iterator();
			while (it.hasNext()) {
				String prefix = it.next();
				if (discardDuplicates && duplicatedProbePrefixes.contains(prefix)) {
					continue;
				}
				Probe p = probePrefixes.get(prefix);
				newList.addProbe(p, startingList.getValuesForProbe(p));
			}
		}

		
		filterFinished(newList);
		
	}

	public JPanel getOptionsPanel() {
		return optionsPanel;
	}

	public boolean hasOptionsPanel() {
		return true;
	}

	public boolean isReady() {
		if (optionsPanel.filterOnNames()) {
			return optionsPanel.regexPattern() != null;
		}
		else {
			return optionsPanel.percentOverlap() >=0 && optionsPanel.percentOverlap() <=100;
		}
	}

	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		b.append("Probes from ");
		b.append(startingList.name());

		if (optionsPanel.filterOnNames()) {
			b.append("with common name defined by pattern ");
			b.append(optionsPanel.regexPattern());
		}
		else {
			b.append("with overlap of at least ");
			b.append(optionsPanel.percentOverlap());
			b.append("%");
		}
		
		return b.toString();
	}

	protected String listName() {
		if (optionsPanel.filterOnNames()) {
			return "Deduplicated by name";
		}
		else {
			return "Deduplicated by "+optionsPanel.percentOverlap()+"% overlap";
		}
	}

	public String name() {
		return "Deduplication filter";
	}


	/**
	 * The PositionFilterOptionsPanel.
	 */
	private class DeduplicationFilterOptionsPanel extends JPanel implements KeyListener, ActionListener {

		private JComboBox filterTypeBox;
		private JTextField regexField;
		private JTextField percentOverlapField;
		private JComboBox highLowBox;
		private JComboBox selectWhatBox;

		private String regex = "-\\d\\d\\d$";
		private int percentOverlap = 50;

		public DeduplicationFilterOptionsPanel () {
			setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.2;
			gbc.weighty=0.5;
			gbc.fill=GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5,5,5,5);


			add(new JLabel("Filter type",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			filterTypeBox = new JComboBox(new String [] {"Name","Overlap"});
			filterTypeBox.addActionListener(this);

			add(filterTypeBox,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			add(new JLabel("Suffix Pattern",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			regexField = new JTextField("-\\d\\d\\d$");
			regexField.addKeyListener(this);

			add(regexField,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			add(new JLabel("Percentage Overlap Required",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			percentOverlapField = new JTextField("50");
			percentOverlapField.addKeyListener(new NumberKeyListener(false, false));
			percentOverlapField.addKeyListener(this);
			percentOverlapField.setEnabled(false);
			add(percentOverlapField,gbc);
			
			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			add(new JLabel("Select",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			JPanel selectPanel = new JPanel();
			selectPanel.setLayout(new BorderLayout());
			
			if (startingList.getValueNames() != null && startingList.getValueNames().length>0) {
				selectWhatBox = new JComboBox(new String [] {"Length",startingList.getValueNames()[0]});
			}
			else {
				selectWhatBox = new JComboBox(new String [] {"Length"});
			}

			highLowBox = new JComboBox(new String [] {"Highest","Lowest","Discard"});
			highLowBox.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					if (highLowBox.getSelectedItem().equals("Discard")) {
						selectWhatBox.setEnabled(false);
					}
					else {
						selectWhatBox.setEnabled(true);
					}
				}
			});
			selectPanel.add(highLowBox,BorderLayout.WEST);
			selectPanel.add(selectWhatBox,BorderLayout.CENTER);
			add(selectPanel,gbc);

		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(450,230);
		}
		
		public boolean discardDuplicates () {
			return highLowBox.getSelectedItem().equals("Discard");
		}
		
		public boolean selectHighest () {
			return highLowBox.getSelectedItem().equals("Highest");
		}
		
		public boolean selectLength () {
			return selectWhatBox.getSelectedItem().equals("Length");
		}
		
		public String regexPattern () {
			return regex;
		}
		
		public int percentOverlap () {
			return percentOverlap;
		}
		
		public boolean filterOnNames () {
			return filterTypeBox.getSelectedItem().equals("Name");
		}

		public void keyPressed(KeyEvent arg0) {}

		public void keyReleased(KeyEvent ke) {

			if (ke.getSource().equals(percentOverlapField)) {
				if (percentOverlapField.getText().length() > 0) {
					percentOverlap = Integer.parseInt(percentOverlapField.getText());
				}
				else {
					percentOverlap = -1;
				}
				optionsChanged();
			}
			else if (ke.getSource().equals(regexField)) {
				try {
					Pattern.compile(regexField.getText());
					regex = regexField.getText();
				}
				catch (PatternSyntaxException ex) {
					regex = null;
				}
				optionsChanged();
			}
		}

		public void keyTyped(KeyEvent arg0) {}

		public void actionPerformed(ActionEvent ae) {

			// Comes from the filterTypeBox

			if (filterTypeBox.getSelectedItem().equals("Name")) {
				regexField.setEnabled(true);
				percentOverlapField.setEnabled(false);
			}
			else {
				regexField.setEnabled(false);
				percentOverlapField.setEnabled(true);
			}
		}
	}

}
