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
package uk.ac.babraham.SeqMonk.Filters.LogisticRegressionFilter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;

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
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

/**
 * Runs logistic regression on a set of replicated for/rev datasets.
 */
public class LogisticRegressionFilter extends ProbeFilter {

	private ReplicateSet [] replicateSets = new ReplicateSet[0];
	private static Double pValueCutoff = 0.05;
	private static Integer minObservations = 10;
	private static Integer minValid = 10;
	private static boolean allValid = true;

	private static boolean multiTest = true;
	private static boolean resample = false;

	private final LogisticRegressionOptionsPanel optionsPanel;

	/**
	 * Instantiates a new replicate set stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public LogisticRegressionFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new LogisticRegressionOptionsPanel();
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
		
		// Check the value for the min observations.  We won't use a value lower than 3.		
		if (minObservations < 1) minObservations = 1;

		// Check the value for the min valid.  We won't use a value lower than 3.		
		if (minValid < 1) minValid = 3;

		
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

			progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Filters/LogisticRegressionFilter/logistic_regression_template.r"));


			// Write the count data

			// Sort these so we can get probes from the same chromosome together
			Arrays.sort(probes);
			PrintWriter pr = null;
			String lastChr = "";
			
			// Rather than not testing probes based on their absolute difference
			// we should just post-filter them.  The easiest way to do this will
			// be to not test (as we do now) but explicity pass in the number of
			// tests we should have performed to the multiple testing correction.
			
			int numberOfTestsToCorrectBy = 0;
			
			PROBE: for (int p=0;p<probes.length;p++) {

				if (!probes[p].chromosome().name().equals(lastChr)) {
					if (pr != null) pr.close();
					File outFile = new File(tempDir.getAbsoluteFile()+"/data_chr"+probes[p].chromosome().name()+".txt");
					pr = new PrintWriter(outFile);
					lastChr = probes[p].chromosome().name();
					pr.println("id\tgroup\treplicate\tstate\tcount");
				}


				if (p%1000 == 0) {
					progressUpdated("Writing data for chr"+lastChr,p,probes.length);
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
				

				int validFrom = 0;
				for (int i=0;i<fromStores.length;i++) {
					if (fromMethCounts[i] + fromUnmethCounts[i] >= minObservations) {
						++validFrom;
					}
				}

				int validTo = 0;
				for (int i=0;i<toStores.length;i++) {
					if (toMethCounts[i] + toUnmethCounts[i] >= minObservations) {
						++validTo;
					}
				}

				// Check we have enough valid observations to continue with this probe
				if (allValid) {
					// We're going to be quite strict in saying that we need to
					// have enough data in all stores to go ahead and do the test.
					if (validFrom < fromStores.length || validTo < toStores.length) {
						continue;
					}
				}
				else {
					// We use the cutoff for minValid which they set
					if (validFrom < minValid || validTo < minValid) {
						// We don't have enough data to measure this one
						continue;
					}
				}

				// At this point we have to count this probe as valid for the 
				// purposes of multiple testing correction
				++numberOfTestsToCorrectBy;
				

				float [] fromPercentages = new float[validFrom];
				float [] toPercentages = new float[validTo];

				int lastFromIndex = 0;
				int lastToIndex = 0;

				for (int i=0;i<fromMethCounts.length;i++) {
					if (fromMethCounts[i]+fromUnmethCounts[i] < minObservations) continue;
					fromPercentages[lastFromIndex] = fromMethCounts[i]*100f/(fromMethCounts[i]+fromUnmethCounts[i]);
					++lastFromIndex;
				}

				for (int i=0;i<toMethCounts.length;i++) {
					if (toMethCounts[i]+toUnmethCounts[i] < minObservations) continue;
					toPercentages[lastToIndex] = toMethCounts[i]*100f/(toMethCounts[i]+toUnmethCounts[i]);
					++lastToIndex;
				}


				// If we get here then we're OK to use this probe

				for (int i=0;i<fromMethCounts.length;i++) {
					pr.println(p+"\tfrom\t"+i+"\tmeth\t"+fromMethCounts[i]);
					pr.println(p+"\tfrom\t"+i+"\tunmeth\t"+fromUnmethCounts[i]);
				}
				for (int i=0;i<toMethCounts.length;i++) {
					pr.println(p+"\tto\t"+i+"\tmeth\t"+toMethCounts[i]);
					pr.println(p+"\tto\t"+i+"\tunmeth\t"+toUnmethCounts[i]);
				}

			}				

			pr.close();
			
			// Sanity check to make sure we have something to work with.
			if (numberOfTestsToCorrectBy == 0) {
				progressExceptionReceived(new IllegalStateException("No probes had enough data to test."));
			}
			
			
			// Now we can complete the template 
			
			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));
			
			template.setValue("CORRECTCOUNT",""+numberOfTestsToCorrectBy);

			template.setValue("PVALUE", ""+pValueCutoff);

			if (multiTest) {
				template.setValue("MULTITEST", "TRUE");
			}
			else {
				template.setValue("MULTITEST", "FALSE");
			}


			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
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
					progressExceptionReceived(new SeqMonkException("R Script failed"));
					pr.close();
					return;
				}
				if (listener.complete()) break;

				Thread.sleep(500);

			}

			// We can now parse the results and put the hits into a new probe list

			ProbeList newList = new ProbeList(startingList,"","",new String [] {"P-value","FDR","Difference"});

			File hitsFile = new File(tempDir.getAbsolutePath()+"/hits.txt");

			BufferedReader br = new BufferedReader(new FileReader(hitsFile));

			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
				
				// We can get NA coming in the difference field
				// and we'll just discard this if it happens
				if (sections[2].equals("NA")) {
					continue;
				}

				int probeIndex = Integer.parseInt(sections[0]);
				float pValue = Float.parseFloat(sections[1]);
				float qValue = Float.parseFloat(sections[3]);
				float diff = Float.parseFloat(sections[2]);

				newList.addProbe(probes[probeIndex],new float[]{pValue,qValue,diff});
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
		return "Logistic Regression Stats Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Logistic regression filter on probes in ");
		b.append(startingList.name());
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

		b.append(" with a minimum number of observations of ");
		b.append(minObservations);
		
		if (allValid) {
			b.append(" in all samples");
		}
		else {
			b.append(" in at least ");
			b.append(minValid);
			b.append(" samples");
		}

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("Logistic regression p<");

		b.append(pValueCutoff);

		if (multiTest) {
			b.append(" after correction");
		}

		b.append(". Min obs ");
		b.append(minObservations);

		return b.toString();	
	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class LogisticRegressionOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {

		private JList dataList;
		private JTextField pValueCutoffField;
		private JTextField minObsField;
		private JCheckBox multiTestBox;
		private JCheckBox resampleBox;
		private JCheckBox allValidSamples;
		private JTextField minValidSamplesField;

		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public LogisticRegressionOptionsPanel () {

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
			gbc.insets = new Insets(5, 5, 5, 5);


			gbc.gridwidth=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("P-value cutoff",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			if (pValueCutoff == null) pValueCutoff=0.05;
			pValueCutoffField = new JTextField(pValueCutoff.toString(),5);
			pValueCutoffField.addKeyListener(this);
			choicePanel.add(pValueCutoffField,gbc);

			gbc.gridx=0;
			gbc.gridwidth=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Minimum Observations",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			if (minObservations == null) minObservations = 10;
			minObsField = new JTextField(minObservations.toString(),5);
			minObsField.addKeyListener(new NumberKeyListener(false, false));
			minObsField.addKeyListener(this);
			choicePanel.add(minObsField,gbc);

			
			gbc.gridx=0;
			gbc.gridwidth=1;
			gbc.gridy++;

			choicePanel.add(new JLabel("Min Valid Replicates",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;
			
			if (minValid == null) minValid = 10; 
			minValidSamplesField = new JTextField(minValid.toString(),5);
			minValidSamplesField.addKeyListener(new NumberKeyListener(false, false));
			minValidSamplesField.addKeyListener(this);
			minValidSamplesField.setEnabled(!allValid);

			allValidSamples = new JCheckBox("All",allValid);
			allValidSamples.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					minValidSamplesField.setEnabled(!allValidSamples.isSelected());
					allValid = allValidSamples.isSelected();
				}
			});

			JPanel validPanel = new JPanel();
			validPanel.add(allValidSamples);
			validPanel.add(minValidSamplesField);
			
			choicePanel.add(validPanel,gbc);

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

			choicePanel.add(new JLabel("Resample counts from current quantitation",JLabel.RIGHT),gbc);

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
					pValueCutoff = Double.parseDouble(pValueCutoffField.getText());
				}
			}

			else if (f.equals(minObsField)) {
				if (f.getText().length() == 0) minObservations = 0;
				else {
					minObservations = Integer.parseInt(minObsField.getText());
				}
			}

			else if (f.equals(minValidSamplesField)) {
				if (f.getText().length() == 0) minValid = 3;
				else {
					minValid = Integer.parseInt(minValidSamplesField.getText());
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
