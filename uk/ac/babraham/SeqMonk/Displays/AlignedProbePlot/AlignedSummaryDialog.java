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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class AlignedSummaryDialog is a container for the probe trend
 * plot.  It can display either a progress message or the actual
 * plot.
 */
public class AlignedSummaryDialog extends JDialog implements ActionListener, ChangeListener, ProgressListener, Cancellable {

	/** The trend panel. */
	private JPanel summaryPanelsPlusScaleBar;
	private AlignedSummaryPanel [] summaryPanels;

	/** The slider to adjust the contrast on the plot */
	private JSlider contrastSlider;

	private int progressIndex = 0;
	
	private GradientScaleBar scaleBar;

	private ProgressDialog pd = null;
	
	private boolean logTransform;


	/**
	 * Instantiates a new aligned summary dialog.
	 * 
	 * @param probes the probes
	 * @param store the store
	 * @param prefs the prefs
	 */
	public AlignedSummaryDialog (ProbeList [] lists, DataStore [] stores, AlignedSummaryPreferences prefs) {
		super(SeqMonkApplication.getInstance(),"Aligned probes plot");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		logTransform = prefs.useLogScale;

		pd = new ProgressDialog(SeqMonkApplication.getInstance(),"Aligning...", this);
		pd.progressUpdated("Setting up alignment...", 0, 1);

		// We need to come back and
		// sort out the monitoring of this code properly at some point as it's all
		// hacked at the moment.

		// We have two levels of sorting.  The first dimension is the number of probe lists
		// The second is the number of probes in each list.
		Integer [][] sortOrder = new Integer[lists.length][];

		if (prefs.orderBy != null) {

			for (int l=0;l<lists.length;l++) {
				sortOrder[l] = new Integer[lists[l].getAllProbes().length];
			}

			// We need to work out the order of the probes to use
			for (int l=0;l<lists.length;l++) {
				int [] counts = new int [lists[l].getAllProbes().length];
				Probe [] probes = lists[l].getAllProbes();
				Arrays.sort(probes);
				for (int p=0;p<probes.length;p++) {
					sortOrder[l][p] = p;
					long [] reads = prefs.orderBy.getReadsForProbe(probes[p]);
					for (int r=0;r<reads.length;r++) {

						// Check if we can skip this read as a duplicate
						if (prefs.removeDuplicates && r > 0) {
							if (SequenceRead.start(reads[r]) == SequenceRead.start(reads[r-1]) && SequenceRead.end(reads[r]) == SequenceRead.end(reads[r-1])) {
								continue;
							}
						}

						// Check if we're using reads in this direction
						if (! prefs.quantitationType.useRead(probes[p], reads[r])) {
							continue;
						}

						++counts[p];
					}

				}

				// Now we need to sort the indices by the counts
				Arrays.sort(sortOrder[l],new CountSorter(counts));
			}
		}


		summaryPanels = new AlignedSummaryPanel [stores.length*lists.length];

		for (int s=0;s<stores.length;s++) {
			for (int l=0;l<lists.length;l++) {
				// TODO: Activate these separately in case they share a data store and will fight for the cache.
				summaryPanels[(l*stores.length)+s] = new AlignedSummaryPanel(lists[l],stores[s],prefs,s,sortOrder[l]);
				summaryPanels[(l*stores.length)+s].addProgressListener(this);
			}
		}

		getContentPane().setLayout(new BorderLayout());

		summaryPanelsPlusScaleBar = new JPanel();
		summaryPanelsPlusScaleBar.setLayout(new BorderLayout());
		JPanel allSummaryPanels = new JPanel();
		allSummaryPanels.setLayout(new GridLayout(lists.length, stores.length));

		for (int i=0;i<summaryPanels.length;i++) {
			allSummaryPanels.add(summaryPanels[i]);
		}


		summaryPanelsPlusScaleBar.add(allSummaryPanels,BorderLayout.CENTER);	

		contrastSlider = new JSlider(JSlider.VERTICAL,1,100,100);

		// This call is left in to work around a bug in the Windows 7 LAF
		// which makes the slider stupidly thin in ticks are not drawn.
		contrastSlider.setPaintTicks(true);
		contrastSlider.addChangeListener(this);

		getContentPane().add(contrastSlider,BorderLayout.WEST);
		
		// We're going to add a scale bar to the left hand side
		scaleBar = new GradientScaleBar(new ColourGradient() {
			
			public String name() {
				return "";
			}
			
			protected Color[] makeColors() {
				return summaryPanels[0].colors;
			}
		}, 0,1);
		
		summaryPanelsPlusScaleBar.add(scaleBar, BorderLayout.EAST);

		getContentPane().add(summaryPanelsPlusScaleBar, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);


		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);

		
		JButton saveDataButton = new JButton("Export Data");
		saveDataButton.setActionCommand("save_data");
		saveDataButton.addActionListener(this);
		buttonPanel.add(saveDataButton);

		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setSize(Math.min(150+(200*stores.length), 800),700);
		setLocationRelativeTo(SeqMonkApplication.getInstance());

		pd.progressUpdated("Aligning...", 0, 1);

		// Start the processing
		Thread t = new Thread(summaryPanels[0]);
		t.start();

	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(summaryPanelsPlusScaleBar);
		}
		else if (ae.getActionCommand().equals("save_data")){
			
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new TxtFileFilter());
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			if (! file.getPath().toLowerCase().endsWith(".txt")) {
				file = new File(file.getPath()+".txt");
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			try {					
				PrintWriter pr = new PrintWriter(file);
				
				pr.println("Store\tList\tProbe\tCounts...");
				
				for (int i=0;i<summaryPanels.length;i++) {
					summaryPanels[i].writeData(pr);
				}
				
				pr.close();

			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}

		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The contrast slider was moved
		
		float percentile = (float)(Math.pow(contrastSlider.getValue(),4)/Math.pow(100,4))*100f;
		
		if (summaryPanels != null) {

			float maxValue = 1;

			for (int i=0;i<summaryPanels.length;i++) {
				
				float thisMax = summaryPanels[i].maxCount();
				if (thisMax != 0 && thisMax > maxValue) {
					maxValue = thisMax;
				}
			}
			
			
			maxValue /= 100;
			maxValue *= percentile;
			
			if (logTransform) {
				scaleBar.setLimits(0, Math.log(maxValue+1));
			}
			else {
				scaleBar.setLimits(0, maxValue);
			}
			
			for (int i=0;i<summaryPanels.length;i++) {
				if (summaryPanels[i] != null) {
					summaryPanels[i].setMaxIntensity(maxValue);
				}
			}
		}
	}

	public void progressExceptionReceived(Exception e) {
		pd.progressExceptionReceived(e);
		return;
	}

	public void progressWarningReceived(Exception e) {
		pd.progressWarningReceived(e);
	}

	public void progressUpdated(String message, int current, int max) {
		pd.progressUpdated("Aligning...", (progressIndex*max)+current, summaryPanels.length*max);
	}

	public void progressCancelled() {
		pd.progressCancelled();
	}

	public void progressComplete(String command, Object result) {
		++progressIndex;
		
		if (progressIndex<summaryPanels.length) {
			Thread t = new Thread(summaryPanels[progressIndex]);
			t.start();
		}
		else {
			pd.progressComplete("aligned_probes", this);
			stateChanged(null);
			setVisible(true);
		}
	}

	public void cancel() {
		if (summaryPanels != null) {
			for (int i=0;i<summaryPanels.length;i++) {
				if (summaryPanels[i] != null)
					summaryPanels[i].cancel();
			}
		}
	}

	private class CountSorter implements Comparator<Integer> {
		private int [] counts;
		public CountSorter (int [] counts) {
			this.counts = counts;
		}
		public int compare(Integer o1, Integer o2) {
			return counts[o2] - counts[o1];
		}
	}

}
