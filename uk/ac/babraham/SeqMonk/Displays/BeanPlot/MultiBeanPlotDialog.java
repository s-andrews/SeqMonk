/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.BeanPlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ReplicateSetSelector;
import uk.ac.babraham.SeqMonk.Gradients.ColourIndexSet;
import uk.ac.babraham.SeqMonk.Preferences.ColourScheme;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class MultiBeanPlotDialog extends JDialog implements ActionListener, Runnable {

	private JPanel graphPanel;
	private MultiBeanPlotPanel beanPanel;
	private JComboBox xAxisChoice;
	private String currentChoice = "";
	private DataStore[] stores;
	private ProbeList [] probes;
	private float [][][] dataValues;
	
	private ReplicateSet [] repSets = null;

	private float min = Float.NaN;
	private float max = Float.NaN;

	public MultiBeanPlotDialog (DataStore [] stores, ProbeList [] probes) {
		super(SeqMonkApplication.getInstance(),"Bean Plots");

		this.stores = stores;
		this.probes = probes;

		getContentPane().setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();

		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);

		if (stores.length > 1) {
			JButton highlightButton = new JButton("Highlight Rep Sets");
			highlightButton.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					repSets = ReplicateSetSelector.selectReplicateSets();
					updateGraphs();
				}
			});
		
			buttonPanel.add(highlightButton);
		}
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);

		JPanel optionsPanel = new JPanel();

		optionsPanel.add(new JLabel("X-Axis"));
		xAxisChoice = new JComboBox(new String [] {"Data Stores","Probe Lists"});
		xAxisChoice.addActionListener(this);
		xAxisChoice.setActionCommand("xaxis");

		optionsPanel.add(xAxisChoice);

		if (stores.length == 1) {
			currentChoice = "Probe Lists";
			xAxisChoice.setSelectedIndex(1);
		}
		else if (probes.length == 1) {
			currentChoice = "Data Stores";
			xAxisChoice.setSelectedIndex(0);
		}

		// Only show the options if there are multiple probe lists and stores
		if (stores.length>1 && probes.length > 1) {
			getContentPane().add(optionsPanel,BorderLayout.NORTH);
		}

		graphPanel = new JPanel();

		getContentPane().add(graphPanel,BorderLayout.CENTER);

		Thread t = new Thread(this);
		t.start();

		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
	
	private Color getStoreColor (DataStore s) {
		Color colourToReturn = ColourScheme.BOXWHISKER_FILL;
		
		int repSetIndex = -1;
		if (repSets != null) {			
			for (int r=0;r<repSets.length;r++) {
				if (repSets[r].containsDataStore(s)) {
					repSetIndex = r;
				}
			}
		}

		if (repSetIndex >= 0) {
			colourToReturn = ColourIndexSet.getColour(repSetIndex);
		}
		
		return(colourToReturn);

	}

	private void updateGraphs () {

		boolean useProbesOnXAxis = true;
		if (xAxisChoice.getSelectedItem().equals("Data Stores")) {
			useProbesOnXAxis = false;
		}

		int rows;
		int cols;

		if (useProbesOnXAxis) {
			rows = (int)Math.sqrt(stores.length);
			cols = stores.length/rows;
			if (stores.length % rows > 0) {
				cols++;
			}
		}
		else {
			rows = (int)Math.sqrt(probes.length);
			cols = probes.length/rows;
			if (probes.length % rows > 0) {
				cols++;
			}
		}

		graphPanel.removeAll();

		graphPanel.setLayout(new GridLayout(rows, cols));

		if (useProbesOnXAxis) {

			String [] panelNames = new String[probes.length];
			for (int p=0;p<probes.length;p++) {
				panelNames[p] = probes[p].name();
			}

			for (int g=0;g<stores.length;g++) {
				Color [] colours = new Color[stores.length];
				Color thisColour = getStoreColor(stores[g]);
				for (int i=0;i<colours.length;i++) {
					colours[i] = thisColour;
				}
				
				String groupName = stores[g].name();

				float [][] theseData = new float [probes.length][];

				for (int p=0;p<probes.length;p++) {
					theseData[p] = dataValues[p][g];
				}

				beanPanel = new MultiBeanPlotPanel(theseData, panelNames, groupName, min, max, colours);
				beanPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				graphPanel.add(beanPanel);
			}			
		}
		else {

			String [] panelNames = new String[stores.length];
			for (int s=0;s<stores.length;s++) {
				panelNames[s] = stores[s].name();
			}

			Color [] colours = new Color[stores.length];
			
			for (int s=0;s<stores.length;s++) {
				colours[s] = getStoreColor(stores[s]);
			}

			
			for (int p=0;p<probes.length;p++) {

				String groupName = probes[p].name();

				float [][] theseWhiskers = new float [stores.length][];

				for (int s=0;s<stores.length;s++) {
					theseWhiskers[s] = dataValues[p][s];
				}

				beanPanel = new MultiBeanPlotPanel(theseWhiskers, panelNames, groupName, min, max,colours);

				beanPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
				graphPanel.add(beanPanel);
			}
		}

		graphPanel.revalidate();
		graphPanel.repaint();
	}


	public void actionPerformed(ActionEvent ae) {

		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")) {
			ImageSaver.saveImage(graphPanel);
		}
//		else if (ae.getActionCommand().equals("save_data")) {
//			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
//			chooser.setMultiSelectionEnabled(false);
//			chooser.setFileFilter(new FileFilter() {
//
//				public String getDescription() {
//					return "Text Files";
//				}
//
//				public boolean accept(File f) {
//					if (f.isDirectory() || f.getName().toLowerCase().endsWith(".txt")) {
//						return true;
//					}
//					return false;
//				}
//			});
//
//			int result = chooser.showSaveDialog(MultiBeanPlotDialog.this);
//			if (result == JFileChooser.CANCEL_OPTION) return;
//
//			File file = chooser.getSelectedFile();
//			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
//
//			if (file.isDirectory()) return;
//
//			if (! file.getPath().toLowerCase().endsWith(".txt")) {
//				file = new File(file.getPath()+".txt");
//			}
//
//			// Check if we're stepping on anyone's toes...
//			if (file.exists()) {
//				int answer = JOptionPane.showOptionDialog(MultiBeanPlotDialog.this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");
//
//				if (answer > 0) {
//					return;
//				}
//			}
//
//			// Now write out the results
//			try {
//				PrintWriter pr = new PrintWriter(file);
//				pr.println("ProbeList\tDataStore\tMedian\tLowerQuartile\tUpperQuartile\tLowerWhisker\tUpperWhisker\tOutliers");
//
//				StringBuffer br;
//
//				// Now do all of the data stores
//				for (int s=0;s<stores.length;s++) {
//					for (int p=0;p<probes.length;p++) {
//						br = new StringBuffer();
//
//						BoxWhisker bw = whiskers[p][s];
//						br.append(probes[p].name());
//						br.append("\t");
//						br.append(stores[s].name());
//						br.append("\t");
//						br.append(bw.median());
//						br.append("\t");
//						br.append(bw.lowerQuartile());
//						br.append("\t");
//						br.append(bw.upperQuartile());
//						br.append("\t");
//						br.append(bw.lowerWhisker());
//						br.append("\t");
//						br.append(bw.upperWhisker());
//
//						float [] outliers = bw.lowerOutliers();
//
//						for (int f=0;f<outliers.length;f++) {
//							br.append("\t");
//							br.append(outliers[f]);
//						}
//
//						outliers = bw.upperOutliers();
//
//						for (int f=0;f<outliers.length;f++) {
//							br.append("\t");
//							br.append(outliers[f]);
//						}
//
//						pr.println(br.toString());
//					}
//				}
//
//				pr.close();
//
//			}
//			catch (IOException ioe) {
//				new CrashReporter(ioe);
//			}
//
//		}
		else if (ae.getActionCommand().equals("xaxis")) {
			if (!xAxisChoice.getSelectedItem().equals(currentChoice)) {
				currentChoice = (String)xAxisChoice.getSelectedItem();
				updateGraphs();
			}
		}
		else {
			throw new IllegalStateException("Unknown command "+ae.getActionCommand());
		}
	}

	public void run() {

		dataValues = new float[probes.length][stores.length][];

		try {
			for (int p=0;p<probes.length;p++) {
				
				Probe [] probesToUse = probes[p].getAllProbes();
				
				for (int s=0;s<stores.length;s++) {
					dataValues[p][s] = new float[probesToUse.length];

					for (int i=0;i<probesToUse.length;i++) {

						dataValues[p][s][i] = stores[s].getValueForProbe(probesToUse[i]);
												
						if (Float.isNaN(min) && !Float.isNaN(dataValues[p][s][i])) {
							min=dataValues[p][s][i];
							max=dataValues[p][s][i];
						}

						if (!Float.isNaN(dataValues[p][s][i])) {
							if (dataValues[p][s][i] < min) {
								min=dataValues[p][s][i];
							}

							if (dataValues[p][s][i] > max) {
								max=dataValues[p][s][i];
							}
						}
					}
					
				}
			}

			updateGraphs();
			setVisible(true);

		}
		catch (SeqMonkException sme) {
			throw new IllegalStateException(sme);
		}
	}	
}
