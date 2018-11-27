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
package uk.ac.babraham.SeqMonk.Filters.EdgeRFilter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.SimpleStats;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

/**
 * Runs EdgeR in for/rev ratio mode on a set of replicated for/rev datasets.
 */
public class EdgeRForRevFilter extends ProbeFilter {

	private ReplicateSet [] replicateSets = new ReplicateSet[0];
	private static Double pValueCutoff = 0.05;
	private static Double absDiffCutoff = 0d;

	private static boolean multiTest = true;
	private static boolean resample = false;

	private final EdgeRForRevOptionsPanel optionsPanel;

	/**
	 * Instantiates a new EdgeR filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public EdgeRForRevFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new EdgeRForRevOptionsPanel();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filter to look for significant changes between sets of replicate count data";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		// We need to make a temporary directory, save the data into it, write out the R script
		// and then run it an collect the list of results, then clean up.

		// Make up the list of DataStores in each replicate set		
		DataStore [] fromStores = replicateSets[0].dataStores();
		DataStore [] toStores = replicateSets[1].dataStores();

		File tempDir;
		try {

			Probe [] probes = startingList.getAllProbes();

			if (resample) {
				// We need to check that the data stores are quantitated
				for (int i=0;i<fromStores.length;i++) {
					if (!fromStores[i].isQuantitated()) {
						progressExceptionReceived(new SeqMonkException("Data Store "+fromStores[i].name()+" wasn't quantitated"));
						return;
					}
					for (int p=0;p<probes.length;p++) {
						float value = fromStores[i].getValueForProbe(probes[p]);
						if ((!Float.isNaN(value)) && (value < 0 || value > 100)) {
							progressExceptionReceived(new SeqMonkException("Data Store "+fromStores[i].name()+" had a value outside the range 0-100 ("+value+")"));
							return;							
						}

					}
				}
				for (int i=0;i<toStores.length;i++) {
					if (!toStores[i].isQuantitated()) {
						progressExceptionReceived(new SeqMonkException("Data Store "+toStores[i].name()+" wasn't quantitated"));
						return;
					}
					for (int p=0;p<probes.length;p++) {
						float value = toStores[i].getValueForProbe(probes[p]);
						if ((!Float.isNaN(value)) && (value < 0 || value > 100)) {
							progressExceptionReceived(new SeqMonkException("Data Store "+toStores[i].name()+" had a value outside the range 0-100 ("+value+")"));
							return;							
						}

					}
				}
			}

			progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();

			System.err.println("Temp dir is "+tempDir.getAbsolutePath());

			progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Filters/EdgeRFilter/edger_for_rev_template.r"));

			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));

			template.setValue("DIFFERENCE", ""+absDiffCutoff);

			template.setValue("PVALUE", ""+pValueCutoff);

			if (multiTest) {
				template.setValue("MULTITEST", "TRUE");
				template.setValue("CORRECTED", "FDR");
			}
			else {
				template.setValue("MULTITEST", "FALSE");
				template.setValue("CORRECTED", "PValue");
			}

			// For the conditions we just repeat "from" and "to" the number of times they occur in the 
			// samples (twice for each sample since we have both meth and unmeth)
			
			StringBuffer conditions = new StringBuffer();
			for (int i=0;i<fromStores.length;i++) {
				if (i>0) conditions.append(",");
				conditions.append("\"from\",\"from\"");
			}
			for (int i=0;i<toStores.length;i++) {
				conditions.append(",\"to\",\"to\"");
			}
			
			template.setValue("CONDITIONS", conditions.toString());
			
			

			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			PrintWriter pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
			pr.close();

			// Write the count data

			File outFile = new File(tempDir.getAbsoluteFile()+"/counts.txt");
			pr = new PrintWriter(outFile);
			pr.print("id");
			for (int i=0;i<fromStores.length;i++) {
				pr.print("\tfrom_"+i+"_meth\tfrom_"+i+"_unmeth");
			}
			for (int i=0;i<toStores.length;i++) {
				pr.print("\tto_"+i+"_meth\tto_"+i+"_unmeth");
			}

			pr.print("\n");
			
			PROBE: for (int p=0;p<probes.length;p++) {

				if (p%1000 == 0) {
					progressUpdated("Writing data for chr"+probes[p].chromosome().name(),p,probes.length);
				}

				int [] fromMethCounts = new int[fromStores.length];
				int [] fromUnmethCounts = new int[fromStores.length];
				int [] toMethCounts = new int[toStores.length];
				int [] toUnmethCounts = new int[toStores.length];

				for (int i=0;i<fromStores.length;i++) {

					long [] reads = fromStores[i].getReadsForProbe(probes[p]);
					int totalCount = 0;
					int methCount = 0;


					if (resample) {
						float value = fromStores[i].getValueForProbe(probes[p]);
						if (Float.isNaN(value)) {
							continue PROBE;
						}
						totalCount = reads.length;
						methCount = Math.round((totalCount*value)/100f);
					}

					else {
						for (int r=0;r<reads.length;r++) {
							totalCount++;
							if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
								++methCount;
							}
						}
					}

					fromMethCounts[i] = methCount;
					fromUnmethCounts[i] = totalCount-methCount;

				}

				for (int i=0;i<toStores.length;i++) {

					long [] reads = toStores[i].getReadsForProbe(probes[p]);
					int totalCount = 0;
					int methCount = 0;

					if (resample) {
						float value = toStores[i].getValueForProbe(probes[p]);
						if (Float.isNaN(value)) {
							continue PROBE;
						}
						totalCount = reads.length;
						methCount = Math.round((totalCount*value)/100f);
					}

					else {
						for (int r=0;r<reads.length;r++) {
							totalCount++;
							if (SequenceRead.strand(reads[r]) == Location.FORWARD) {
								++methCount;
							}
						}
					}

					toMethCounts[i] = methCount;
					toUnmethCounts[i] = totalCount-methCount;

				}


				// Check to see we meet the requirements for the min amount of information
				// and the min diff.

				int totalFromMeth = 0;
				int totalFrom = 0;
				int totalToMeth = 0;
				int totalTo = 0;

				int validFrom = 0;
				for (int i=0;i<fromStores.length;i++) {
					totalFromMeth += fromMethCounts[i];
					totalFrom += fromMethCounts[i];
					totalFrom += fromUnmethCounts[i];
					if (fromMethCounts[i] >0 || fromUnmethCounts[i]>0) {
						++validFrom;
					}
				}

				int validTo = 0;
				for (int i=0;i<toStores.length;i++) {
					totalToMeth += toMethCounts[i];
					totalTo += toMethCounts[i];
					totalTo += toUnmethCounts[i];
					if (toMethCounts[i] >0 || toUnmethCounts[i]>0) {
						++validTo;
					}
				}

				// EdgeR only requires 2 valid observations
				if (validFrom < 2 || validTo < 2) {
					// We don't have enough data to measure this one
					continue;
				}

				// Now check the differences.  We do this two ways - by adding the 
				// total counts together, and by averaging the individual percentages
				// Both of these have to pass if we're going to do the testing.

				if (Math.abs((totalFromMeth*100f/totalFrom) - (totalToMeth*100f/totalTo)) < absDiffCutoff) {
					continue;
				}

				float [] fromPercentages = new float[validFrom];
				float [] toPercentages = new float[validTo];

				int lastFromIndex = 0;
				int lastToIndex = 0;

				for (int i=0;i<fromMethCounts.length;i++) {
					if (fromMethCounts[i]+fromUnmethCounts[i] == 0) continue;
					fromPercentages[lastFromIndex] = fromMethCounts[i]*100f/(fromMethCounts[i]+fromUnmethCounts[i]);
					++lastFromIndex;
				}

				for (int i=0;i<toMethCounts.length;i++) {
					if (toMethCounts[i]+toUnmethCounts[i] == 0) continue;
					toPercentages[lastToIndex] = toMethCounts[i]*100f/(toMethCounts[i]+toUnmethCounts[i]);
					++lastToIndex;
				}

				if (Math.abs(SimpleStats.mean(fromPercentages)-SimpleStats.mean(toPercentages)) < absDiffCutoff) {
					continue;
				}

				// If we get here then we're OK to use this probe so we print out its data.  We put all of the
				// data for one probe onto a single line.  The first value is the index of the probe.  The 
				// rest are pairs of meth/unmeth values for the from samples then the to samples.

				pr.print(p);
	
				for (int i=0;i<fromMethCounts.length;i++) {
					pr.print("\t"+fromMethCounts[i]+"\t"+fromUnmethCounts[i]);
				}
				for (int i=0;i<toMethCounts.length;i++) {
					pr.print("\t"+toMethCounts[i]+"\t"+toUnmethCounts[i]);
				}
				pr.print("\n");
				
				
			}				

			pr.close();

			progressUpdated("Running R Script",0,1);

			RScriptRunner runner = new RScriptRunner(tempDir);
			RProgressListener listener = new RProgressListener(runner);
			runner.addProgressListener(new ProgressRecordDialog("R Session",runner));
			runner.runScript();

			while (true) {
				if (listener.cancelled()) {
					progressCancelled();
					pr.close();
					return;
				}
				if (listener.exceptionReceived()) {
					pr.close();
					return;
				}
				if (listener.complete()) break;

				Thread.sleep(500);

			}

			// We can now parse the results and put the hits into a new probe list

			ProbeList newList;

			if (multiTest) {
				newList = new ProbeList(startingList,"","",new String[] {"FDR"});
			}
			else {
				newList = new ProbeList(startingList,"","",new String [] {"p-value"});
			}

			File hitsFile = new File(tempDir.getAbsolutePath()+"/hits.txt");

			BufferedReader br = new BufferedReader(new FileReader(hitsFile));

			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");

				String [] indexSections = sections[0].split("\\.");

				int probeIndex = Integer.parseInt(indexSections[indexSections.length-1]);
				float pValue = Float.parseFloat(sections[sections.length-1]);

				newList.addProbe(probes[probeIndex],new float [] {pValue});
			}

			br.close();

			runner.cleanUp();

			filterFinished(newList);



		}
		catch (Exception ioe) {
			progressExceptionReceived(ioe);
			return;
		}



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
		if (replicateSets.length != 2) return false;

		if (pValueCutoff == null || pValueCutoff > 1 || pValueCutoff < 0) return false;

		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "EdgeR for/rev Stats Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("EdgeR for/rev filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");

		for (int s=0;s<replicateSets.length;s++) {
			b.append(replicateSets[s].name());
			if (s < replicateSets.length-1) {
				b.append(" vs ");
			}
		}

		b.append(" had a significance below ");

		b.append(pValueCutoff);

		if (multiTest) {
			b.append(" after Benjamimi and Hochberg correction");
		}
		
		if (resample) {
			b.append(" with ratios recalculated from normalised quantitation");
		}

		b.append(" with a minimum difference of ");
		b.append(absDiffCutoff);

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("EdgeR for/rev p<");

		b.append(pValueCutoff);

		if (multiTest) {
			b.append(" after correction");
		}

		b.append(". Min diff ");
		b.append(absDiffCutoff);

		return b.toString();	
	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class EdgeRForRevOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {

		private JList dataList;
		private JTextField pValueCutoffField;
		private JTextField absDiffCutoffField;
		private JCheckBox multiTestBox;
		private JCheckBox resampleBox;

		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public EdgeRForRevOptionsPanel () {

			setLayout(new BorderLayout());

			JPanel dataPanel = new JPanel();
			dataPanel.setLayout(new BorderLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));

			dataPanel.add(new JLabel("Replicate Sets to Test",JLabel.CENTER),BorderLayout.NORTH);

			DefaultListModel dataModel = new DefaultListModel();

			ReplicateSet [] sets = collection.getAllReplicateSets();
			for (int i=0;i<sets.length;i++) {
				if (sets[i].isQuantitated() && sets[i].dataStores().length >=2) {
					dataModel.addElement(sets[i]);
				}
			}


			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			valueChanged(null); // Set the initial lists
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);

			add(dataPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();

			choicePanel.setLayout(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx=0;
			gbc.gridy=0;
			gbc.weightx=0.2;
			gbc.weighty=0.5;
			gbc.fill=GridBagConstraints.HORIZONTAL;


			gbc.gridwidth=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("P-value cutoff",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			pValueCutoffField = new JTextField(pValueCutoff.toString(),5);
			pValueCutoffField.addKeyListener(this);
			choicePanel.add(pValueCutoffField,gbc);

			gbc.gridx=0;
			gbc.gridwidth=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Absolute diff cutoff",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			absDiffCutoffField = new JTextField(absDiffCutoff.toString(),5);
			absDiffCutoffField.addKeyListener(this);
			choicePanel.add(absDiffCutoffField,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Apply multiple testing correction",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;


			multiTestBox = new JCheckBox();
			multiTestBox.setSelected(multiTest);
			multiTestBox.addChangeListener(this);
			choicePanel.add(multiTestBox,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Resample counts from current quantitation"),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			resampleBox = new JCheckBox();
			resampleBox.setSelected(resample);
			resampleBox.addChangeListener(this);
			choicePanel.add(resampleBox,gbc);

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

			if (f.equals(pValueCutoffField)) {
				if (f.getText().length() == 0) pValueCutoff = 0.05d;
				else {
					try {
						pValueCutoff = Double.parseDouble(pValueCutoffField.getText());
					}
					catch (NumberFormatException e) {
						pValueCutoffField.setText(pValueCutoffField.getText().substring(0,pValueCutoffField.getText().length()-1));
					}
				}
			}

			else if (f.equals(absDiffCutoffField)) {
				if (f.getText().length() == 0) absDiffCutoff = 0d;
				else {
					try {
						absDiffCutoff = Double.parseDouble(absDiffCutoffField.getText());
					}
					catch (NumberFormatException e) {
						absDiffCutoffField.setText(absDiffCutoffField.getText().substring(0,absDiffCutoffField.getText().length()-1));
					}
				}
			}

			else {
				throw new IllegalStateException("Unknown text field "+f);
			}	
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			Object [] o = dataList.getSelectedValues();
			ReplicateSet [] newSets = new ReplicateSet[o.length];
			for (int i=0;i<o.length;i++) {
				newSets[i] = (ReplicateSet)o[i];
			}
			replicateSets = newSets;
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged(ChangeEvent ce) {
			multiTest = multiTestBox.isSelected();
			resample = resampleBox.isSelected();
			optionsChanged();
		}

	}


}
