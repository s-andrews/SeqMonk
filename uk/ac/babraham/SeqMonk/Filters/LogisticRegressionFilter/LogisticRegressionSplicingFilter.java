/**
 * Copyright Copyright 2010-17 Simon Andrews
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
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
public class LogisticRegressionSplicingFilter extends ProbeFilter {

	private ReplicateSet [] replicateSets = new ReplicateSet[0];
	private static Double pValueCutoff = 0.05;
	private static int absCountCutoff = 10;

	private static boolean multiTest = true;

	private final LogisticRegressionOptionsPanel optionsPanel;

	/**
	 * Instantiates a new replicate set stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public LogisticRegressionSplicingFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new LogisticRegressionOptionsPanel();
		
		// Do a quick check that the data we're being given are integers.  We don't 
		// need to check everything - a sample will do in most cases
		ReplicateSet [] repSets = collection.getAllReplicateSets();
		int validRepSetCount = 0;

		Probe [] probes = startingList.getAllProbes();

		for (int r=0;r<repSets.length;r++) {
			if (repSets[r].isQuantitated() && repSets[r].dataStores().length>=2) {
				++validRepSetCount;
			}
			else {
				continue;
			}

			DataStore [] stores = repSets[r].dataStores();

			for (int i=0;i<probes.length;i++) {
				if (i==1000) break;

				for (int s=0;s<stores.length;s++) {
					float value = stores[s].getValueForProbe(probes[i]);
					if (value != (int)value) {
						JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>This filter requires raw counts as input<br>You must re-quantitate your data as raw,uncorrected counts to use this filter<br>It won't work with the quantitation you're currently using.</html>", "Non-integer data", JOptionPane.WARNING_MESSAGE);
						return;
					}
				}
			}
		}

		if (validRepSetCount < 2) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>We didn't find enough data to run this filter.<br>You need at least 2 replicate sets with at least 2 data stores in each to run this.</html>", "Not enough data", JOptionPane.WARNING_MESSAGE);
		}

		
		
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filter to look for significant changes between sets of replicate count data";
	}
	
	private Probe [] filterProbesByCount (Probe [] probes, DataStore [] fromStores, DataStore [] toStores) {
	
		Vector<Probe> passedProbes = new Vector<Probe>();
		
		for (int p=0;p<probes.length;p++) {
			try {
				boolean passedFrom = true;
				for (int i=0;i<fromStores.length;i++) {
					if (fromStores[i].getValueForProbe(probes[p]) < absCountCutoff) {
						passedFrom = false;
					}
				}
				boolean passedTo = true;
				for (int i=0;i<toStores.length;i++) {
					if (toStores[i].getValueForProbe(probes[p]) < absCountCutoff) {
						passedTo = false;
					}
				}
				
				if (passedFrom || passedTo)
					passedProbes.add(probes[p]);
			}
			catch (SeqMonkException sme) {}
		}
		
		return passedProbes.toArray(new Probe[0]);
		
	}
	
	private ProbePair [] makeProbePairs (Probe [] probes) {

		// Make pairs of probes based on whether they have matching start
		// or end positions.
		
		Vector<ProbePair> pairs = new Vector<ProbePair>();
				
		for (int p=0;p<probes.length;p++) {
			for (int i=p+1;i<probes.length;i++) {
				
				if (probes[p].chromosome() != probes[i].chromosome()) {
					break;
				}
				if (probes[i].start() > probes[p].end()) {
					break;
				}
				
				if (probes[i].start() == probes[p].start() || probes[i].end() == probes[p].end()) {
					// Make a pair
					pairs.add(new ProbePair(probes[p], probes[i]));
				}
			}
		}
		
		
		return pairs.toArray(new ProbePair[0]);
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
			
			probes = filterProbesByCount(probes, fromStores, toStores);

			progressUpdated("Pairing Probes",0,1);
			// Make pairs of probes to test
			
			ProbePair [] pairs = makeProbePairs(probes);
			
			System.err.println("Found "+pairs.length+" pairs to test");
			
			progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();

			System.err.println("Temp dir is "+tempDir.getAbsolutePath());

			progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Filters/LogisticRegressionFilter/logistic_regression_template.r"));

			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));

			template.setValue("PVALUE", ""+pValueCutoff);

			if (multiTest) {
				template.setValue("MULTITEST", "TRUE");
			}
			else {
				template.setValue("MULTITEST", "FALSE");
			}


			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			PrintWriter pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
			pr.close();

			// Write the count data

			// Sort these so we can get probes from the same chromosome together
			Arrays.sort(probes);
			pr = null;
			String lastChr = "";

			for (int p=0;p<pairs.length;p++) {

				if (!pairs[p].probe1.chromosome().name().equals(lastChr)) {
					if (pr != null) pr.close();
					File outFile = new File(tempDir.getAbsoluteFile()+"/data_chr"+pairs[p].probe1.chromosome().name()+".txt");
					pr = new PrintWriter(outFile);
					lastChr = pairs[p].probe1.chromosome().name();
					pr.println("id\tgroup\treplicate\tstate\tcount");
				}


				if (p%1000 == 0) {
					progressUpdated("Writing data for chr"+lastChr,p,probes.length);
				}

				int [] fromProbe1Counts = new int[fromStores.length];
				int [] fromProbe2Counts = new int[fromStores.length];
				int [] toProbe1Counts = new int[toStores.length];
				int [] toProbe2Counts = new int[toStores.length];

				for (int i=0;i<fromStores.length;i++) {

					// TODO: For the moment we'll expect they've quantitated this themselves
					fromProbe1Counts[i] = (int)fromStores[i].getValueForProbe(pairs[p].probe1);
					fromProbe2Counts[i] = (int)fromStores[i].getValueForProbe(pairs[p].probe2);

				}

				for (int i=0;i<toStores.length;i++) {

					// TODO: For the moment we'll expect they've quantitated this themselves
					toProbe1Counts[i] = (int)toStores[i].getValueForProbe(pairs[p].probe1);
					toProbe2Counts[i] = (int)toStores[i].getValueForProbe(pairs[p].probe2);

				}

				// If we get here then we're OK to use this probe pair

				for (int i=0;i<fromProbe1Counts.length;i++) {
					pr.println(p+"\tfrom\t"+i+"\tmeth\t"+fromProbe1Counts[i]);
					pr.println(p+"\tfrom\t"+i+"\tunmeth\t"+fromProbe2Counts[i]);
				}
				for (int i=0;i<toProbe1Counts.length;i++) {
					pr.println(p+"\tto\t"+i+"\tmeth\t"+toProbe1Counts[i]);
					pr.println(p+"\tto\t"+i+"\tunmeth\t"+toProbe2Counts[i]);
				}

			}				

			if (pr != null)
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

			ProbeList newList;

			if (multiTest) {
				newList = new ProbeList(startingList,"","","FDR");
			}
			else {
				newList = new ProbeList(startingList,"","","p-value");
			}

			File hitsFile = new File(tempDir.getAbsolutePath()+"/hits.txt");

			BufferedReader br = new BufferedReader(new FileReader(hitsFile));

			String line = br.readLine();

			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");

				String [] indexSections = sections[0].split("\\.");

				int probeIndex = Integer.parseInt(indexSections[indexSections.length-1]);
				float pValue = Float.parseFloat(sections[sections.length-1]);

				// TODO: Work out what to do if the same probe is found multiple times
				newList.addProbe(pairs[probeIndex].probe1,pValue);
				newList.addProbe(pairs[probeIndex].probe2,pValue);
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

		b.append("Logistic regression splicing filter on probes in ");
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
		

		b.append(" with a minimum count of ");
		b.append(absCountCutoff);

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		b.append("Splicing Logistic regression p<");

		b.append(pValueCutoff);

		if (multiTest) {
			b.append(" after correction");
		}

		b.append(". Min count ");
		b.append(absCountCutoff);

		return b.toString();	
	}

	
	private class ProbePair {
		
		public Probe probe1;
		public Probe probe2;
		
		public ProbePair (Probe probe1, Probe probe2) {
			this.probe1 = probe1;
			this.probe2 = probe2;
		}
		
		
	}
	
	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class LogisticRegressionOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {

		private JList dataList;
		private JTextField pValueCutoffField;
		private JTextField absCountCutoffField;
		private JCheckBox multiTestBox;

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

			choicePanel.add(new JLabel("Absolute count cutoff",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			absCountCutoffField = new JTextField(""+absCountCutoff,5);
			absCountCutoffField.addKeyListener(new NumberKeyListener(false, false));
			absCountCutoffField.addKeyListener(this);
			choicePanel.add(absCountCutoffField,gbc);

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

			else if (f.equals(absCountCutoffField)) {
				if (f.getText().length() == 0) absCountCutoff = 1;
				else {
					try {
						absCountCutoff = Integer.parseInt(absCountCutoffField.getText());
					}
					catch (NumberFormatException e) {
						absCountCutoffField.setText(absCountCutoffField.getText().substring(0,absCountCutoffField.getText().length()-1));
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
			optionsChanged();
		}

	}


}
