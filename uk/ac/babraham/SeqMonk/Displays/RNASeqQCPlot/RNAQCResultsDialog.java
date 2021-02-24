/**
 * Copyright 2014- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.RNASeqQCPlot;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class RNAQCResultsDialog extends JDialog implements SampleSelectionListener {

	private JPanel resultPanel;
	private RNAQCResult result;
	private PercentileStripChart [] charts;
	private DataStore [] selectedStores = new DataStore[0];

	public RNAQCResultsDialog (RNAQCResult result) {

		super(SeqMonkApplication.getInstance(),"RNA-Seq QC Report");

		this.result = result;

		resultPanel = new JPanel();

		resultPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		gbc.fill=GridBagConstraints.BOTH;

		String [] titles = result.getTitles();
		double [][] data = result.getPercentageSets();
		charts = new PercentileStripChart [titles.length];

		for (int i=0;i<titles.length;i++) {
			charts[i] = new PercentileStripChart(titles[i], data[i], result.stores());
			charts[i].addSampleSelectionListener(this);
			resultPanel.add(charts[i],gbc);
			gbc.gridx++;
		}

		gbc.weightx=0.01;
		resultPanel.add(new DataStoreNamePanel(result.getStoreNames()),gbc);

		getContentPane().setLayout(new BorderLayout());

		getContentPane().add(resultPanel,BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				setVisible(false);
				dispose();
			}
		});

		buttonPanel.add(closeButton);

		JButton saveButton = new JButton("Save Image");
		saveButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				ImageSaver.saveImage(resultPanel);
			}
		});


		buttonPanel.add(saveButton);


		JButton saveDataButton = new JButton("Save Data");
		saveDataButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileFilter(new FileFilter() {

					public String getDescription() {
						return "Text Files";
					}

					public boolean accept(File f) {
						if (f.isDirectory() || f.getName().toLowerCase().endsWith(".txt")) {
							return true;
						}
						return false;
					}
				});

				int result = chooser.showSaveDialog(RNAQCResultsDialog.this);
				if (result == JFileChooser.CANCEL_OPTION) return;

				File file = chooser.getSelectedFile();
				SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

				if (file.isDirectory()) return;

				if (! file.getPath().toLowerCase().endsWith(".txt")) {
					file = new File(file.getPath()+".txt");
				}

				// Check if we're stepping on anyone's toes...
				if (file.exists()) {
					int answer = JOptionPane.showOptionDialog(RNAQCResultsDialog.this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

					if (answer > 0) {
						return;
					}
				}

				// Now write out the results

				String [] titles = RNAQCResultsDialog.this.result.getTitles();
				double [][] data = RNAQCResultsDialog.this.result.getPercentageSets();
				DataStore [] stores = RNAQCResultsDialog.this.result.stores();

				try {
					PrintWriter pr = new PrintWriter(file);

					// Write the header
					StringBuffer br = new StringBuffer();

					br.append("DataStore");
					for (int i=0;i<titles.length;i++) {
						br.append("\t");
						br.append(titles[i]);
					}

					pr.println(br.toString());

					// Now do all of the data stores
					for (int s=0;s<stores.length;s++) {
						br = new StringBuffer();
						br.append(stores[s].name());
						for (int i=0;i<titles.length;i++) {
							br.append("\t");
							br.append(data[i][s]);
						}

						pr.println(br.toString());

					}

					pr.close();

				}
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}


			}
		});


		buttonPanel.add(saveDataButton);

		JButton removeButton = new JButton("Tag Selected");
		removeButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				String prefix = JOptionPane.showInputDialog(RNAQCResultsDialog.this, "Prefix to add", "BADQC_");

				if (prefix == null || prefix.length()==0) return;

				for (int d=0;d<selectedStores.length;d++) {
					
					// Don't add the prefix if it's already there
					if (!selectedStores[d].name().startsWith(prefix)) {
						selectedStores[d].setName(prefix+selectedStores[d].name());
					}
				}


				int answer = JOptionPane.showConfirmDialog(RNAQCResultsDialog.this,"Remove these stores from the chromosome view?", "Hide selected stores", JOptionPane.YES_NO_OPTION);

				if (answer == JOptionPane.YES_OPTION) {

					DataStore [] visibleStores = SeqMonkApplication.getInstance().drawnDataStores();
					ArrayList<DataStore> storesToKeep = new ArrayList<DataStore>();

					for (int i=0;i<visibleStores.length;i++) {
						boolean keepIt = true;
						for (int d=0;d<selectedStores.length;d++) {
							if (visibleStores[i].equals(selectedStores[d])) {
								keepIt = false;
								break;
							}
						}

						if (keepIt) {
							storesToKeep.add(visibleStores[i]);
						}
					}

					if (storesToKeep.size() < visibleStores.length) {
						// We need to make a change
						SeqMonkApplication.getInstance().setDrawnDataStores(storesToKeep.toArray(new DataStore[0]));
					}
				}

			}
		});

		buttonPanel.add(removeButton);


		getContentPane().add(buttonPanel,BorderLayout.SOUTH);


		setSize(800,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);

	}

	public void dataStoresSelected(DataStore[] stores) {

		this.selectedStores = stores;
		for (int i=0;i<charts.length;i++) {
			charts[i].setSelectedStores(stores);
		}
	}

}
