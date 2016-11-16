/**
 * Copyright Copyright 2010-15 Simon Andrews
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
import java.util.HashSet;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The ProbeNameFilter selects a set of probes based on a list of
 * queries against their names.
 */
public class ProbeNameFilter extends ProbeFilter {

	private final ProbeNameFilterOptionsPanel optionsPanel;
	private String [] queries;
	private boolean stripSuffixes;
	private boolean stripTranscript;
	private boolean caseInsensitive;

	/**
	 * Instantiates a new feature filter with default options
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public ProbeNameFilter (DataCollection collection)  throws SeqMonkException {
		this(collection,new String[0],true,true);
	}

	/**
	 * Instantiates a new feature filter with all options set so the filter
	 * is immediately ready to run.
	 * 
	 * @param collection The dataCollection to filter
	 * @param annotationType The type of annotation to use for the filter
	 * @param allowOvelapping Whether to add probes which overlap the feature
	 * @param allowUpstream Whether to add probes which are upstream of the filter
	 * @param allowDownstream Whether to add probes which are downstream of the filter
	 * @param cutoffDistance How far from the feature counts as upstream / downstream
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public ProbeNameFilter (DataCollection collection,String [] queries, boolean stripSuffixes, boolean stripTranscript) throws SeqMonkException {
		super(collection);
		this.queries = queries;
		this.stripSuffixes = stripSuffixes;
		this.stripTranscript = stripTranscript;

		optionsPanel = new ProbeNameFilterOptionsPanel();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters probes based on a name match to a set of names";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		queries = optionsPanel.queriesArea.getText().split("\n");
		stripSuffixes = optionsPanel.stripSuffixesBox.isSelected();
		stripTranscript = optionsPanel.stripTranscriptBox.isSelected();
		caseInsensitive = optionsPanel.caseInsensitiveBox.isSelected();

		ProbeList passedProbes = new ProbeList(startingList,"","",startingList.getValueName());

		// We start by building a list of query strings we're going to
		// check against.

		HashSet<String>queryStrings = new HashSet<String>();

		for (int q=0;q<queries.length;q++) {
			String query = queries[q].trim();
			if (caseInsensitive) {
				query = query.toLowerCase();
			}
			
			if (stripSuffixes) {
				query = query.replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
			}
			if (stripTranscript) {
				query = query.replaceAll("-\\d\\d\\d$", "");
			}
//			System.err.println("Adding query term "+query);
			queryStrings.add(query);
		}

		Probe [] probes = startingList.getAllProbes();

		// We can now step through the probes looking for a match to the stored feature names
		for (int p=0;p<probes.length;p++) {

			if (p % 100 == 0) {
				progressUpdated("Filtering probes", p, probes.length);
			}
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

			String name = probes[p].name();
			
			if (caseInsensitive) {
				name = name.toLowerCase();
			}

			if (stripSuffixes) {
				name = name.replaceFirst("_upstream$", "").replaceAll("_downstream$", "").replaceAll("_gene$", "");
			}
			if (stripTranscript) {
				name = name.replaceAll("-\\d\\d\\d$", "");
			}

			if (queryStrings.contains(name)) {
				passedProbes.addProbe(probes[p], startingList.getValueForProbe(probes[p]));						
			}
			else {
//				System.err.println("No match for "+name);
			}
		}

		filterFinished(passedProbes);
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
		return "Feature Name Filter";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		b.append("Probes from ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" whose name matches any of ");

		for (int i=0;i<queries.length;i++) {
			b.append(queries[i]);
			if (i<queries.length-1) {
				b.append(",");
			}
		}

		if (stripSuffixes) {
			b.append(" after stripping probe generator suffixes");
		}

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {		
		return "Name matched probes";
	}


	/**
	 * The FeatureFilterOptionsPanel.
	 */
	private class ProbeNameFilterOptionsPanel extends JPanel {

		private JTextArea queriesArea;
		private JCheckBox stripSuffixesBox;
		private JCheckBox stripTranscriptBox;
		private JCheckBox caseInsensitiveBox;

		/**
		 * Instantiates a new feature filter options panel.
		 */
		public ProbeNameFilterOptionsPanel () {

			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

			JPanel choicePanel1 = new JPanel();
			choicePanel1.setLayout(new BorderLayout());
			choicePanel1.add(new JLabel("Query terms",JLabel.CENTER),BorderLayout.NORTH);
			queriesArea = new JTextArea();
			choicePanel1.add(new JScrollPane(queriesArea),BorderLayout.CENTER);
			add(choicePanel1);


			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Remove probe generator suffixes "));
			stripSuffixesBox = new JCheckBox();
			stripSuffixesBox.setSelected(true);
			choicePanel2.add(stripSuffixesBox);
			add(choicePanel2);

			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel("Remove transcript suffixes "));
			stripTranscriptBox = new JCheckBox();
			stripTranscriptBox.setSelected(true);
			choicePanel3.add(stripTranscriptBox);
			add(choicePanel3);

			JPanel choicePanel4 = new JPanel();
			choicePanel4.add(new JLabel("Case insensitive "));
			caseInsensitiveBox = new JCheckBox();
			caseInsensitiveBox.setSelected(true);
			choicePanel4.add(caseInsensitiveBox);
			add(choicePanel4);

		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(400,500);
		}

	}
}
