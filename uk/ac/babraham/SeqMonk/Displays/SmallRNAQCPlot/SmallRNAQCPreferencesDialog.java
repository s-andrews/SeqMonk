/**
 * Copyright 2014-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.SmallRNAQCPlot;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.IntVector;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

public class SmallRNAQCPreferencesDialog extends JDialog implements ActionListener, ProgressListener {

	private JList dataList;
	private JList featureList;
	private JTextField minLengthField;
	private JTextField maxLengthField;
	private DataCollection collection;
	
	private static int lowLength = 20;
	private static int highLength = 40;
	
	public SmallRNAQCPreferencesDialog(DataCollection collection)  {
		super(SeqMonkApplication.getInstance(),"Small RNA QC Plot");
		this.collection = collection;
		getContentPane().setLayout(new BorderLayout());
		JPanel dataPanel = new JPanel();
		dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
		dataPanel.setLayout(new BorderLayout());
		dataPanel.add(new JLabel("Data Sets/Groups",JLabel.CENTER),BorderLayout.NORTH);

		DefaultListModel dataModel = new DefaultListModel();

		DataStore [] stores = collection.getAllDataStores();
		
		for (int i=0;i<stores.length;i++) {
			dataModel.addElement(stores[i]);
		}

		dataList = new JList(dataModel);
		ListDefaultSelector.selectDefaultStores(dataList);
		dataList.setCellRenderer(new TypeColourRenderer());
		dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);

		getContentPane().add(dataPanel,BorderLayout.WEST);

		JPanel choicePanel = new JPanel();
		
		choicePanel.setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx=0.5;
		gbc.weighty=0.5;
		gbc.gridx=1;
		gbc.gridy=1;
		
		choicePanel.add(new JLabel("Gene features"),gbc);
		gbc.gridx++;
		
		featureList = new JList(collection.genome().annotationCollection().listAvailableFeatureTypes());
		IntVector smallRNAIndices = new IntVector();
		for (int i=0;i<featureList.getModel().getSize();i++) {
			if (featureList.getModel().getElementAt(i).toString().toLowerCase().contains("rna")) {
				if (featureList.getModel().getElementAt(i).toString().toLowerCase().equals("mrna")) continue; // We want non mRNA rnas
				smallRNAIndices.add(i);
			}
		}
		
		featureList.setSelectedIndices(smallRNAIndices.toArray());
		
		choicePanel.add(new JScrollPane(featureList),gbc);
		
		gbc.gridy++;
		gbc.gridx=1;

		choicePanel.add(new JLabel("Min length"),gbc);
		gbc.gridx++;

		minLengthField = new JTextField(""+lowLength);
		minLengthField.addKeyListener(new NumberKeyListener(false, false, 50));
		choicePanel.add(minLengthField,gbc);

		gbc.gridy++;
		gbc.gridx=1;

		choicePanel.add(new JLabel("Max length"),gbc);
		gbc.gridx++;

		maxLengthField = new JTextField(""+highLength);
		maxLengthField.addKeyListener(new NumberKeyListener(false, false, 50));
		choicePanel.add(maxLengthField,gbc);

		getContentPane().add(choicePanel,BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		
		JButton plotButton = new JButton("Create Plot");
		plotButton.setActionCommand("plot");
		plotButton.addActionListener(this);
		buttonPanel.add(plotButton);
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		
		setSize(400,300);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
		
	}
	
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("plot")) {
			
			Object [] featureObjects = featureList.getSelectedValues();
			String [] features = new String[featureObjects.length];
			for (int i=0;i<featureObjects.length;i++) {
				features[i] = (String)featureObjects[i];
			}
			
			
			Object [] selectedObjects = dataList.getSelectedValues();
			
			if (selectedObjects.length == 0) {
				JOptionPane.showMessageDialog(this, "No data stores were selected", "Oops", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			DataStore [] selectedStores = new DataStore[selectedObjects.length];
			for (int i=0;i<selectedObjects.length;i++) {
				selectedStores[i] = (DataStore)selectedObjects[i];
			}

			int minLength = 20;
			int maxLength = 30;
			
			if (minLengthField.getText().trim().length()>0) {
				minLength = Integer.parseInt(minLengthField.getText().trim());
			}
			
			if (maxLengthField.getText().trim().length()>0) {
				maxLength = Integer.parseInt(maxLengthField.getText().trim());
			}
			
			if (minLength > maxLength) {
				int temp = minLength;
				minLength = maxLength;
				maxLength = temp;
			}
			
			lowLength = minLength;
			highLength = maxLength;
			
			
			SmallRNAQCCalcualtor calc = new SmallRNAQCCalcualtor(collection, features, minLength, maxLength, selectedStores);
			calc.addListener(this);
			calc.addListener(new ProgressDialog("Small RNA QC Plot", calc));
			calc.startCalculating();

			setVisible(false);

		}
	}

	public void progressExceptionReceived(Exception e) {
		dispose();
	}

	public void progressWarningReceived(Exception e) {}

	public void progressUpdated(String message, int current, int max) {}

	public void progressCancelled() {
		setVisible(true);
	}

	public void progressComplete(String command, Object result) {
		new SmallRNAResultsDialog((SmallRNAQCResult [])result);
		dispose();
	}

	
	
	
}
