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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.SplitLocation;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The FeatureFilter filters probes based on their relationship to members
 * of a particular class of features.
 */
public class OldFeatureFilter extends ProbeFilter {

	private static final int ANY_STRAND = 1;
	private static final int FORWARD_ONLY = 2;
	private static final int REVERSE_ONLY = 3;
	private static final int SAME_STRAND = 4;
	private static final int OPPOSING_STRAND = 5;
	
	private String annotationType = null;
	private int annotationLimit = 0;
	private boolean overlapping = false;
	private boolean upstream = false;
	private boolean downstream = false;
	private boolean matchExactly = false;
	private boolean ignoreStrand = false;
	private int strand = ANY_STRAND;
	

	private final FeatureFilterOptionsPanel optionsPanel;
	
	
	/**
	 * Instantiates a new feature filter with default options
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated
	 */
	public OldFeatureFilter (DataCollection collection)  throws SeqMonkException {
		this(collection,collection.genome().annotationCollection().listAvailableFeatureTypes()[0],false,true,false,false,2000);
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
	public OldFeatureFilter (DataCollection collection,String annotationType, boolean matchExactly, boolean allowOvelapping, boolean allowUpstream, boolean allowDownstream, int cutoffDistance) throws SeqMonkException {
		super(collection);
		this.annotationType = annotationType;
		overlapping = allowOvelapping;
		upstream = allowUpstream;
		downstream = allowDownstream;
		annotationLimit = cutoffDistance;
		this.matchExactly = matchExactly;
		
		optionsPanel = new FeatureFilterOptionsPanel();
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters probes based on their relationship to a class of features";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
				
		ProbeList passedProbes = new ProbeList(startingList,"","",null);
		
		// Since we're going to be making the annotations on the
		// basis of position we should go through all probes one
		// chromosome at a time.
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();
		
		for (int c=0;c<chrs.length;c++) {
			
			progressUpdated("Processing features on Chr "+chrs[c].name(),c, chrs.length);
			
			Probe [] probes = startingList.getProbesForChromosome(chrs[c]);
			Feature [] features = collection.genome().annotationCollection().getFeaturesForType(chrs[c],annotationType);
			
			Arrays.sort(probes);
			Arrays.sort(features);
			
			int lastFoundIndex = 0;
			
			// We can now step through the probes looking for the best feature match
			for (int p=0;p<probes.length;p++) {
				
				for (int f=lastFoundIndex;f<features.length;f++) {
					
					if (cancel) {
						cancel = false;
						progressCancelled();
						return;
					}
					
					if (features[f].location().end()+annotationLimit < probes[p].start()) {
						lastFoundIndex = f;
						continue;
					}
					
					// See if we're skipping this feature for this probe based on its strand
					if (strand != ANY_STRAND) {
						switch (strand) {
						
							case FORWARD_ONLY: {
								if (features[f].location().strand() != Location.FORWARD) continue;
								break;
							}
							case REVERSE_ONLY: {
								if (features[f].location().strand() != Location.REVERSE) continue;
								break;
							}
							case SAME_STRAND: {
								if (features[f].location().strand() != probes[p].strand()) continue;
								break;
							}
							case OPPOSING_STRAND: {
								if (!
										(features[f].location().strand() == Location.FORWARD  && probes[p].strand() == Location.REVERSE) ||
										(features[f].location().strand() == Location.REVERSE  && probes[p].strand() == Location.FORWARD)
										)
										continue;
								break;
							}
												
						}
					}

					if (matchExactly) {
						
						// We can make a simple check to see if we're matching this exactly, either
						// overall or with one of our subfeatures.
						
						if (probes[p].start() == features[f].location().start() && probes[p].end() == features[f].location().end()) {
							if (ignoreStrand || probes[p].strand() == Probe.UNKNOWN || probes[p].strand() == features[f].location().strand()) {
								passedProbes.addProbe(probes[p], null);
							}
						}
						
						if (features[f].location() instanceof SplitLocation) {
							Location [] subLocs = ((SplitLocation)features[f].location()).subLocations();
							for (int l=0;l<subLocs.length;l++) {
								if (probes[p].start() == subLocs[l].start() && probes[p].end() == subLocs[l].end()) {
									if (ignoreStrand || probes[p].strand() == Probe.UNKNOWN || probes[p].strand() == subLocs[l].strand()) {
										passedProbes.addProbe(probes[p], null);
									}
								}
								
							}
						}
						
					}
					
					if (overlapping) {
						// Quickest check is whether a probe overlaps a feature
					
						if (probes[p].start() < features[f].location().end() && probes[p].end() > features[f].location().start()) {
							passedProbes.addProbe(probes[p],null);
							break;
						}
					}
					
					if (upstream) {
						// Check if we're upstream
						// Get the distance to the start
						int d=0;
						if (features[f].location().strand() == Location.FORWARD) {
							d = features[f].location().start() - probes[p].end();
						}
						else {
							d = probes[p].start() - features[f].location().end();
						}
						
						if (d >= 0) {
							// We're the right side of the feature

							if (d < annotationLimit) {
								passedProbes.addProbe(probes[p],null);
								break;
							}
						}
					}

					if (downstream) {
						// Check if we're downstream
						// Get the distance to the start
						int d=0;
						if (features[f].location().strand() == Location.FORWARD) {
							d = probes[p].start() - features[f].location().end();
						}
						else {
							d = features[f].location().start() - probes[p].end();
						}
						
						if (d >= 0) {
							// We're the right side of the feature

							if (d < annotationLimit) {
								passedProbes.addProbe(probes[p],null);
								break;
							}
						}
					}
				}
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
		if (annotationType != null && (matchExactly || overlapping || upstream || downstream)) {
			return true;
		}
		return false;
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Features Filter";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		b.append("Probes from ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" which were ");
		
		if (matchExactly) {
			b.append(" and exact match to features of type ");
		}
		else {
		
			boolean useOr = false;
			
			if (overlapping) {
				b.append("overlapping");
				useOr = true;
			}
			if (upstream) {
				if (useOr) {
					b.append(" or ");
				}
				b.append("upstream");
				useOr = true;
			}
			if (downstream) {
				if (useOr) {
					b.append(" or ");
				}
				b.append("downstream");
				useOr = true;
			}
			
			b.append(" of features of type ");
		
		}
		
		b.append(annotationType);
		
		b.append(" using features on ");
		
		switch (strand) {
			case ANY_STRAND: b.append("any strand"); break;
			case FORWARD_ONLY: b.append("the forward strand only"); break;
			case REVERSE_ONLY: b.append("the reverse strand only"); break;
			case SAME_STRAND: b.append("the same strand as the probe"); break;
			case OPPOSING_STRAND: b.append("the opposite strand to the probe"); break;
		}
		
		if (upstream || downstream) {
			b.append(" with distance cutoff ");
			b.append(annotationLimit);
		}
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		if (matchExactly) {
			b.append("exact match to ");
		}
		else {
			boolean useOr = false;
			
			if (overlapping) {
				b.append("overlapping");
				useOr = true;
			}
			if (upstream) {
				if (useOr) {
					b.append(" or ");
				}
				b.append("upstream");
				useOr = true;
			}
			if (downstream) {
				if (useOr) {
					b.append(" or ");
				}
				b.append("downstream");
				useOr = true;
			}
			
			b.append(" of ");
		}
		
		b.append(annotationType);

		return b.toString();
	}


	/**
	 * The FeatureFilterOptionsPanel.
	 */
	private class FeatureFilterOptionsPanel extends JPanel implements KeyListener, ItemListener {

		private JComboBox annotationPositionBox;
		private JComboBox annotationTypeBox;
		private JTextField annotationLimitField;
		private JComboBox strandBox;

		/**
		 * Instantiates a new feature filter options panel.
		 */
		public FeatureFilterOptionsPanel () {

			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			
			JPanel choicePanel1 = new JPanel();

			choicePanel1.add(new JLabel("Select probes which are "));
			annotationPositionBox = new JComboBox(new String [] {"overlapped by", "exactly matching", "exactly matching (ignore strand)","close to", "upstream of", "downstream of", "surrounded by or upstream of", "surrounded by or downstream of"});
			annotationPositionBox.addItemListener(this);
			choicePanel1.add(annotationPositionBox);
			annotationTypeBox = new JComboBox(collection.genome().annotationCollection().listAvailableFeatureTypes());
			annotationTypeBox.setPrototypeDisplayValue("No longer than this please");
			annotationTypeBox.addItemListener(this);
			choicePanel1.add(annotationTypeBox);
			add(choicePanel1);

			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Use features on strand "));
			strandBox = new JComboBox(new String [] {"Any","Forward only","Reverse only","Same as probe","Opposite to probe"});
			strandBox.addItemListener(this);
			choicePanel2.add(strandBox);
			add(choicePanel2);
			
			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel("Feature distance cutoff "));
			annotationLimitField = new JTextField(""+annotationLimit,7);
			annotationLimitField.addKeyListener(this);
			annotationLimitField.setEnabled(false);
			choicePanel3.add(annotationLimitField);
			choicePanel3.add(new JLabel("bp"));
			add(choicePanel3);

		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,200);
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

			if (f.getText().length() > 0) {
				try {
					annotationLimit = Integer.parseInt(f.getText());
					optionsChanged();
				}
				catch (NumberFormatException e) {
					f.setText(f.getText().substring(0,f.getText().length()-1));
				}
			}			
		}
	
	
		/* (non-Javadoc)
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		public void itemStateChanged(ItemEvent ie) {
			if (ie.getSource() == annotationPositionBox) {
				String annotationPositionValue = (String)annotationPositionBox.getSelectedItem();
				
				if (annotationPositionValue.equals("surrounded by") || annotationPositionValue.equals("overlapped by") || annotationPositionValue.equals("exactly matching") || annotationPositionValue.equals("exactly matching (ignore strand)")) {
					annotationLimitField.setEnabled(false);
				}
				else {
					annotationLimitField.setEnabled(true);
				}
	
				if (annotationPositionValue.equals("overlapped by")) {
					upstream = false;
					downstream = false;
					overlapping = true;
					matchExactly = false;
				}
				else if (annotationPositionValue.equals("exactly matching")) {
					upstream = false;
					downstream = false;
					overlapping = false;
					matchExactly = true;
				}
				else if (annotationPositionValue.equals("exactly matching (ignore strand)")) {
					upstream = false;
					downstream = false;
					overlapping = false;
					matchExactly = true;
					ignoreStrand = true;
				}
				else if (annotationPositionValue.equals("close to")) {
					upstream = true;
					downstream = true;
					overlapping = true;
					matchExactly = false;
				}
				else if (annotationPositionValue.equals("surrounded by or upstream of")) {
					downstream = false;
					upstream = true;
					overlapping = true;
					matchExactly = false;	
				}
				else if (annotationPositionValue.equals("surrounded by or downstream of")) {
					upstream = false;
					downstream = true;
					overlapping = true;
	
				}
				else if (annotationPositionValue.equals("upstream of")) {
					overlapping = false;
					downstream = false;
					upstream = true;
				}
				else if (annotationPositionValue.equals("downstream of")) {
					overlapping = false;
					upstream = false;
					downstream = true;
				}
				else {
					System.err.println("Didn't recognise position value '"+annotationPositionValue+"'");
					return;
				}
			}
			
			else if (ie.getSource() == annotationTypeBox) {
				annotationType = (String)annotationTypeBox.getSelectedItem();
			}

			else if (ie.getSource() == strandBox) {
				String strandString = strandBox.getSelectedItem().toString();
				
				if (strandString.equals("Any")) {
					strand = ANY_STRAND;
				}
				else if (strandString.equals("Forward only")) {
					strand = FORWARD_ONLY;
				}
				else if (strandString.equals("Reverse only")) {
					strand = REVERSE_ONLY;
				}
				else if (strandString.equals("Same as probe")) {
					strand = SAME_STRAND;
				}
				else if (strandString.equals("Opposite to probe")) {
					strand = OPPOSING_STRAND;
				}
				else {
					throw new IllegalStateException("Unknown strand '"+strandString+"'");
				}

			}

			else {
				System.err.println("Unknown source for ItemListener "+ie.getSource());
			}
			
			optionsChanged();
		}
	}
}
