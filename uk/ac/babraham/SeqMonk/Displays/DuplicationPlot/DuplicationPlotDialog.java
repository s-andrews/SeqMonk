/**
 * Copyright 2016-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.DuplicationPlot;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class DuplicationPlotDialog extends JDialog implements Runnable, Cancellable, ActionListener, ChangeListener {

	private DataStore [] data;
	private Probe [] probes;
	private float [][] densities;
	private float [][] duplications;
	private boolean cancel = false;
	private DuplicationPlotPanel [] plotPanels;
	private JPanel multiPlotPanel;

	/** The dot size slider. */
	private JSlider dotSizeSlider;


	private ProgressDialog pd;

	public DuplicationPlotDialog (DataStore [] data, ProbeList probeList) {
		super(SeqMonkApplication.getInstance(),"Duplication ["+probeList.name()+"]");
			
		if (data.length > 12) {
			RestrictedDataStoreSelector rds = new RestrictedDataStoreSelector(data, 12);
			this.data = rds.selectStores();
		}
		else {
			this.data = data;
		}
		
		if (this.data == null || this.data.length == 0) return;
		
		this.probes = probeList.getAllProbes();
		densities = new float[data.length][probes.length];
		duplications = new float[data.length][probes.length];

		pd = new ProgressDialog("Calculating...",this);

		Thread t = new Thread(this);
		t.start();

		getContentPane().setLayout(new BorderLayout());

		dotSizeSlider = new JSlider(JSlider.VERTICAL,1,100,3);

		// This call is left in to work around a bug in the Windows 7 LAF
		// which makes the slider stupidly thin in ticks are not drawn.
		dotSizeSlider.setPaintTicks(true);
		dotSizeSlider.addChangeListener(this);
		getContentPane().add(dotSizeSlider,BorderLayout.EAST);


		JPanel buttonPanel = new JPanel();
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);


		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);

		getContentPane().add(buttonPanel,BorderLayout.SOUTH);


		setSize(900,600);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

	}

	public void cancel() {
		cancel = true;
	}

	public void run() {

		plotPanels = new DuplicationPlotPanel[data.length];
		
		for (int d=0;d<data.length;d++) {

			for (int p=0;p<probes.length;p++) {
				if (cancel) {
					pd.progressCancelled();
					dispose();
					return;
				}

				if (p % 1000 == 0) {
					pd.progressUpdated("Calculating Duplication", (probes.length*d)+p, probes.length*data.length);
				}

				long [] reads = data[d].getReadsForProbe(probes[p]);
				densities[d][p] = (float)(Math.log(reads.length / (probes[p].length()/1000f))/Math.log(2));

				int dupReads = 0;
				for (int r=1;r<reads.length;r++) {
					if (reads[r] == reads[r-1]) ++dupReads;
				}

				duplications[d][p] = (dupReads*100f)/reads.length;
				//			System.err.println("Dup="+dupReads+" total="+reads.length+" Percent="+duplications[p]);
			}
			plotPanels[d] = new DuplicationPlotPanel(densities[d], duplications[d], probes, 2, data[d].name());
		}


		// Make a multiple panel to hold the plot panels
		multiPlotPanel = new JPanel();
		
		int cols = (int)Math.ceil((data.length*2)/3);
		if (cols < 1) cols = 1;
		
		int rows = data.length/cols;
		if (data.length % cols != 0) {
			rows++;
		}
		
		
		multiPlotPanel.setLayout(new GridLayout(rows, cols));

		for (int p=0;p<plotPanels.length;p++) {
			multiPlotPanel.add(plotPanels[p]);
		}
		
		getContentPane().add(multiPlotPanel,BorderLayout.CENTER);

		pd.progressComplete("duplication", null);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);

	}

	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(multiPlotPanel);
		}

	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The dot size slider has been moved
		if (multiPlotPanel != null) {
			for (int p=0;p<plotPanels.length;p++) {
				plotPanels[p].setDotSize(dotSizeSlider.getValue());
			}
		}
	}

}
