/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.PCAPlot;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * This displays the variance graph for a PCA and will allow the extraction of 
 * important genes for a given PC
 */
public class PCAVarianceDialog extends JDialog implements ActionListener, ChangeListener {

	/** The scatter plot panel. */
	private JPanel variancePlotPanel;

	private VarianceLineGraphPanel lineGraphPanel;
	private PCAHistogramPanel histogramPanel;
	private JSlider rotationCutoffSlider;


	/** The x stores. */
	private JComboBox xStores;

	/** The collection. */
	private PCADataCalculator data;

	/**
	 * Instantiates a new scatter plot dialog.
	 * 
	 * @param collection the collection
	 */
	public PCAVarianceDialog (PCADataCalculator data) {
		super(SeqMonkApplication.getInstance(),"PCA Variances");
		this.data = data;

		setTitle("PCA Variances ["+data.probeListName()+"]");

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout());

		JPanel plotAndControlsPanel = new JPanel();
		plotAndControlsPanel.setLayout(new BorderLayout());

		variancePlotPanel = new JPanel();
		variancePlotPanel.setLayout(new GridLayout(2, 1));

		lineGraphPanel = new VarianceLineGraphPanel(data.variances());

		variancePlotPanel.add(lineGraphPanel);

		histogramPanel = new PCAHistogramPanel(data.getPCARotations(0));

		variancePlotPanel.add(histogramPanel);

		plotAndControlsPanel.add(variancePlotPanel,BorderLayout.CENTER);

		rotationCutoffSlider = new JSlider(0,500,250);
		rotationCutoffSlider.addChangeListener(this);

		plotAndControlsPanel.add(rotationCutoffSlider, BorderLayout.SOUTH);

		getContentPane().add(plotAndControlsPanel,BorderLayout.CENTER);

		JPanel optionsPanel = new JPanel();
		String [] PCList = new String[data.getPCCount()];

		for (int i=0;i<data.getPCCount();i++) {
			PCList[i] = "PC"+(i+1);
		}


		xStores = new JComboBox(PCList);
		xStores.setPrototypeDisplayValue("No longer than this");
		xStores.addActionListener(this);
		xStores.setActionCommand("plot");

		optionsPanel.add(new JLabel("Highlight"));
		optionsPanel.add(xStores);

		getContentPane().add(optionsPanel,BorderLayout.NORTH);

		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);

		JButton saveProbesButton = new JButton("Save Probes");
		saveProbesButton.setActionCommand("save_probes");
		saveProbesButton.addActionListener(this);
		buttonPanel.add(saveProbesButton);

		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(700,520);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("plot")) {
			int xIndex = xStores.getSelectedIndex();

			lineGraphPanel.setSelectedIndex(xIndex);
			histogramPanel.setData(data.getPCARotations(xIndex));

			rotationCutoffSlider.setValue(250);

		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(variancePlotPanel);
		}
		else if (ae.getActionCommand().equals("save_probes")) {

			double proportion = rotationCutoffSlider.getValue()/500d;
			int xIndex = xStores.getSelectedIndex();

			double max = data.getMaxPCARotation(xIndex);
			double min = data.getMinPCARotation(xIndex);

			double selectedValue = min+ ((max-min)*proportion);

			ProbeList list = data.getProbeList(xIndex,selectedValue);
			
			if (list.getAllProbes().length == 0) {
				JOptionPane.showMessageDialog(this, "No probes were selected", "No probes", JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			// Ask for a name for the list
			String groupName=null;
			while (true) {
				groupName = (String)JOptionPane.showInputDialog(this,"Enter list name","Found "+list.getAllProbes().length+" probes",JOptionPane.QUESTION_MESSAGE,null,null,list.name());
				if (groupName == null) {
					list.delete();  // Remove the list which will have been created by this stage
					return;  // They cancelled
				}

				if (groupName.length() == 0)
					continue; // Try again

				break;
			}
			list.setName(groupName);
		}


		else {
			throw new IllegalArgumentException("Unknown command "+ae.getActionCommand());
		}
	}

	public void stateChanged(ChangeEvent e) {
		// The rotation slider was moved...
		// Find the new selected value

		double proportion = rotationCutoffSlider.getValue()/500d;

		int xIndex = xStores.getSelectedIndex();

		double max = data.getMaxPCARotation(xIndex);
		double min = data.getMinPCARotation(xIndex);

		double selectedValue = min+ ((max-min)*proportion);

		histogramPanel.setCutoff(selectedValue);

	}



}
