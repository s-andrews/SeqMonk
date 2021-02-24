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
package uk.ac.babraham.SeqMonk.Filters.SegmentationFilter;

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
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Filters.ProbeFilter;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

/**
 * Runs fastseg on a set of values to divide them into groups based on
 * positional consistency 
 */
public class SegmentationFilter extends ProbeFilter {

	private DataStore dataStore = null;
	private static Double alpha = 0.1;

	private static boolean global = false;

	private final SegmentationOptionsPanel optionsPanel;
	
	/**
	 * Instantiates a new limma stats filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public SegmentationFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
		optionsPanel = new SegmentationOptionsPanel();
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filter to look for significant changes between sets of replicate normalised data";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		// We need to make a temporary directory, save the data into it, write out the R script
		// and then run it an collect the list of results, then clean up.

		File tempDir;
		try {

			Probe [] probes = startingList.getAllProbes();

			// fastseg won't use probes which have an NA in them, so we need to remove
			// any probes which aren't complete across all samples
			
			Vector<Probe> validProbes = new Vector<Probe>();
			
			for (int p=0;p<probes.length;p++) {
				
				if (Float.isNaN(dataStore.getValueForProbe(probes[p]))) {
					continue;
				}
				
				validProbes.add(probes[p]);
			}
			
			if (validProbes.size() == 0) {
				JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "Found no probes without NA values - can't run segmentation", "Can't run segmentation", JOptionPane.ERROR_MESSAGE);
				progressCancelled();
				return;
			}
			
			if (validProbes.size() < probes.length) {
				progressWarningReceived(new SeqMonkException("Removed "+(probes.length-validProbes.size())+" probes since they contained NA values"));
			}
			
			probes = validProbes.toArray(new Probe[0]);

			
			progressUpdated("Creating temp directory",0,1);

			tempDir = TempDirectory.createTempDirectory();

//			System.err.println("Temp dir is "+tempDir.getAbsolutePath());

			progressUpdated("Writing R script",0,1);
			// Get the template script
			Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Filters/SegmentationFilter/segmentation_template.r"));

			// Substitute in the variables we need to change
			template.setValue("WORKING", tempDir.getAbsolutePath().replace("\\", "/"));

			if (global) {
				template.setValue("GLOBAL", "2");
			}
			else {
				template.setValue("GLOBAL", "1");
			}
			
			template.setValue("ALPHA", alpha.toString());


			// Write the script file
			File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
			PrintWriter pr = new PrintWriter(scriptFile);
			pr.print(template.toString());
			pr.close();

			// Write the count data
			File countFile = new File(tempDir.getAbsoluteFile()+"/quantitation.txt");

			pr = new PrintWriter(countFile);

			pr.println("value");

			progressUpdated("Writing quantitation data",0,1);

			
			for (int p=0;p<probes.length;p++) {

				if (p%1000 == 0) {
					progressUpdated("Writing count data",p,probes.length);					
				}

				pr.println(dataStore.getValueForProbe(probes[p]));
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
					progressExceptionReceived(listener.exception());
					return;
				}
				if (listener.complete()) break;

				Thread.sleep(500);
			}
			
			// We can now parse the results and put the hits into a new probe list

			ProbeList newList;

			newList = new ProbeList(startingList,"","","Segment");

			File segmentFile = new File(tempDir.getAbsolutePath()+"/segments.txt");

			BufferedReader br = new BufferedReader(new FileReader(segmentFile));

			String line = br.readLine();

			
			Vector<ClusteredSegment> segments = new Vector<ClusteredSegment>();
			
			while ((line = br.readLine()) != null) {
				String [] sections = line.split("\t");

				int startSegment = Integer.parseInt(sections[1]) - 1;
				int endSegment = Integer.parseInt(sections[2])-1;
				
				float segmentMean = Float.parseFloat(sections[7]);
				
				segments.add(new ClusteredSegment(startSegment, endSegment, segmentMean));
								
			}

			br.close();

			runner.cleanUp();

			
			// Now we need to pass the segments on to the clustering dialog so we 
			// know how to split them into groups
			SegmentClusteringDialog clusteringDialog = new SegmentClusteringDialog(segments.toArray(new ClusteredSegment[0]));
			
			ClusteredSegment [][] splitSegments = clusteringDialog.getClusteredSegments();
			float [] boundaryPoints = clusteringDialog.getBoundaryValues();
			
			for (int s=0;s<splitSegments.length;s++) {
				
				String description;
				if (s==0) {
					description = "Segments with a mean below "+boundaryPoints[0];
				}
				else if (s==splitSegments.length-1) {
					description = "Segments with a mean above "+boundaryPoints[boundaryPoints.length-1];
				}
				else {
					description = "Segments with a mean between "+boundaryPoints[s-1]+" and "+boundaryPoints[s];
				}
				
				ProbeList theseSegmentsList = new ProbeList(newList, "Segment Group "+(s+1), description, "Mean");
				
				for (int i=0;i<splitSegments[s].length;i++) {
					for (int j=splitSegments[s][i].startIndex;j<=splitSegments[s][i].endIndex;j++) {
						newList.addProbe(probes[j], (float)(i+1));
						theseSegmentsList.addProbe(probes[j], splitSegments[s][i].mean);
					}
				}
				
			}
			
			
			
			// We should also make an annotation track out of them.
			
			
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
		if (dataStore == null) return false;

		if (alpha == null || alpha > 1 || alpha < 0) return false;

		return true;	
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Segmentation Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Segmentation clustering on probes in ");
		b.append(startingList.name());
		b.append(" using data from ");
		b.append(dataStore.name());

		if (global) {
			b.append(" using a global segmentation model");
		}
		
		b.append(" with an alpha value of "+alpha);

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

		b.append("Segmentation clustering");

		if (global) {
			b.append(" (global)");
		}

		return b.toString();	
	}

	/**
	 * The WindowedReplicateOptionsPanel.
	 */
	private class SegmentationOptionsPanel extends JPanel implements ListSelectionListener, KeyListener, ChangeListener {

		private JList dataList;
		private JTextField alphaField;
		private JCheckBox globalBox;

		/**
		 * Instantiates a new windowed replicate options panel.
		 */
		public SegmentationOptionsPanel () {

			setLayout(new BorderLayout());

			JPanel dataPanel = new JPanel();
			dataPanel.setLayout(new BorderLayout());
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));

			dataPanel.add(new JLabel("Data Store to Test",JLabel.CENTER),BorderLayout.NORTH);

			DefaultListModel dataModel = new DefaultListModel();

			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					dataModel.addElement(stores[i]);
				}
			}


			dataList = new JList(dataModel);
//			ListDefaultSelector.selectDefaultStores(dataList);
//			valueChanged(null); // Set the initial lists
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

			choicePanel.add(new JLabel("Alpha Value",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;

			alphaField = new JTextField(alpha.toString(),5);
			alphaField.addKeyListener(this);
			alphaField.addKeyListener(new NumberKeyListener(true, false, 1));
			choicePanel.add(alphaField,gbc);

			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx=0.2;

			choicePanel.add(new JLabel("Compare to global mean",JLabel.RIGHT),gbc);

			gbc.gridx++;
			gbc.weightx=0.6;


			globalBox = new JCheckBox();
			if (global) {
				globalBox.setSelected(true);
			}
			else {
				globalBox.setSelected(false);
			}
			globalBox.addChangeListener(this);
			choicePanel.add(globalBox,gbc);

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

			if (f.equals(alphaField)) {
				if (f.getText().length() == 0) alpha = null;
				else {
					try {
						alpha = Double.parseDouble(alphaField.getText());
					}
					catch (NumberFormatException e) {
						alphaField.setText(alphaField.getText().substring(0,alphaField.getText().length()-1));
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
			dataStore = (DataStore)dataList.getSelectedValue();
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		public void stateChanged(ChangeEvent ce) {
			global = globalBox.isSelected();
			optionsChanged();
		}

	}


}
