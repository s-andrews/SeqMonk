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
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

/**
 * Runs DESeq2 on two replicate sets to look for count differences.  Assumes that
 * the data is already quantitated as raw counts.
 */
public class EdgeRFilter extends ProbeFilter {

	private ReplicateSet [] replicateSets = new ReplicateSet[0];
	private static Double cutoff = 0.05;

	private static boolean multiTest = true;
	
	private final EdgeROptionsPanel optionsPanel;

	/**
	 * Instantiates a new replicate set stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public EdgeRFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new EdgeROptionsPanel();
		
		// Put out a warning if we see that we're not using all possible probes
		// for the test.
		if (!(startingList instanceof ProbeSet)) {
			JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "<html>This test requires a representative set of all probes to be valid.<br>Be careful running it on a biased subset of probes</html>", "Filtered list used", JOptionPane.WARNING_MESSAGE);
		}

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
			
			progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();
			
			System.err.println("Temp dir is "+tempDir.getAbsolutePath());
		
			progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Filters/EdgeRFilter/edger_template.r"));
			
			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));
			
			// Say which p value column we're filtering on
			if (multiTest) {
				template.setValue("CORRECTED", "FDR");
			}
			else {
				template.setValue("CORRECTED", "PValue");
			}
			
			StringBuffer sb = new StringBuffer();
			for (int i=0;i<fromStores.length;i++) {
				if (i>0) sb.append(",");
				sb.append("1");
			}
			for (int i=0;i<toStores.length;i++) {
				sb.append(",");
				sb.append("2");
			}
			template.setValue("CONDITIONS", sb.toString());
			template.setValue("PVALUE", ""+cutoff);
			
			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			PrintWriter pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
			pr.close();
			
			// Write the count data
			File countFile = new File(tempDir.getAbsoluteFile()+"/counts.txt");
			
			pr = new PrintWriter(countFile);
			
			sb = new StringBuffer();
			sb.append("probe");
			for (int i=0;i<fromStores.length;i++) {
				sb.append("\t");
				sb.append("from");
				sb.append(i);
			}
			for (int i=0;i<toStores.length;i++) {
				sb.append("\t");
				sb.append("to");
				sb.append(i);
			}
			
			pr.println(sb.toString());

			progressUpdated("Writing count data",0,1);

			Probe [] probes = startingList.getAllProbes();

			float value;

			for (int p=0;p<probes.length;p++) {
				
				if (p%1000 == 0) {
					progressUpdated("Writing count data",p,probes.length);					
				}
				sb = new StringBuffer();
				sb.append(p);
				for (int i=0;i<fromStores.length;i++) {
					sb.append("\t");
					value = fromStores[i].getValueForProbe(probes[p]);
					if (value != (int)value) {
						progressExceptionReceived(new IllegalArgumentException("Inputs to the EdgeR filter MUST be raw, incorrected counts, not things like "+value));
						pr.close();
						return;
					}
					sb.append(value);
				}
				for (int i=0;i<toStores.length;i++) {
					sb.append("\t");
					value = toStores[i].getValueForProbe(probes[p]);
					if (value != (int)value) {
						progressExceptionReceived(new IllegalArgumentException("Inputs to the EdgeR filter MUST be raw, incorrected counts, not things like "+value));
						pr.close();
						return;
					}
					sb.append(value);
				}
				
				pr.println(sb.toString());
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
					return;
				}
				if (listener.exceptionReceived()) {
					progressExceptionReceived(new SeqMonkException("R Script failed"));
					return;
				}
				if (listener.complete()) break;
				
				Thread.sleep(500);
				
			}
			
			// We can now parse the results and put the hits into a new probe list
			
			ProbeList newList;
			
			newList = new ProbeList(startingList,"","","FDR");

			File hitsFile = new File(tempDir.getAbsolutePath()+"/hits.txt");
			
			BufferedReader br = new BufferedReader(new FileReader(hitsFile));
			
			String line = br.readLine();
			
			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");
		
				int probeIndex = Integer.parseInt(sections[0]);
				float pValue = Float.parseFloat(sections[sections.length-1]);
				
				newList.addProbe(probes[probeIndex],pValue);
			}
			
			br.close();
			
			runner.cleanUp();
					
			filterFinished(newList);

			
			
		}
		catch (Exception ioe) {
			progressExceptionReceived(ioe);
			return;
		}
		

		

	
//		filterFinished(newList);
		
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

		if (cutoff == null || cutoff > 1 || cutoff < 0) return false;
						
		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "EdgeR Stats Filter";
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("EdgeR stats filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");

		for (int s=0;s<replicateSets.length;s++) {
			b.append(replicateSets[s].name());
			if (s < replicateSets.length-1) {
				b.append(" vs ");
			}
		}
		
		b.append(" had a significance below ");
		
		b.append(cutoff);
		
		if (multiTest) {
			b.append(" after Benjamimi and Hochberg correction");
		}
		
		b.append(". Quantitation was ");
		if (collection.probeSet().currentQuantitation() == null) {
			b.append("not known.");
		}
		else {
			b.append(collection.probeSet().currentQuantitation());
		}

		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();
		
		b.append("EdgeR stats p<");
		
		b.append(cutoff);
		
		if (multiTest) {
			b.append(" after correction");
		}
		
		return b.toString();	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class EdgeROptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {
	
		private JList dataList;
		private JTextField cutoffField;
		private JCheckBox multiTestBox;
	
		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public EdgeROptionsPanel () {
				
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
	
			if (cutoff == null) cutoff = 0.05;
			cutoffField = new JTextField(cutoff.toString(),5);
			cutoffField.addKeyListener(this);
			choicePanel.add(cutoffField,gbc);
		
			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;
	
			choicePanel.add(new JLabel("Apply multiple testing correction",JLabel.RIGHT),gbc);
	
			gbc.gridx++;
			gbc.weightx=0.6;
	
	
			multiTestBox = new JCheckBox();
			if (multiTest) {
				multiTestBox.setSelected(true);
			}
			else {
				multiTestBox.setSelected(false);
			}
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
			
			if (f.equals(cutoffField)) {
				if (f.getText().length() == 0) cutoff = null;
				else {
					try {
						cutoff = Double.parseDouble(cutoffField.getText());
					}
					catch (NumberFormatException e) {
						cutoffField.setText(cutoffField.getText().substring(0,cutoffField.getText().length()-1));
					}
				}
			}
			
			else {
				System.err.println("Unknown text field "+f);
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
