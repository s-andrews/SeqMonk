/**
 * Copyright Copyright 2006-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HistogramPlot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Class ReadLengthHistogramPlot shows the distribution of read lengths
 * in a data store.
 */
public class ReadLengthHistogramPlot extends JDialog implements ActionListener {

	/** The plot panel. */
	private HistogramPanel plotPanel;
	
	/**
	 * Instantiates a new read length histogram plot.
	 * 
	 * @param d the data
	 */
	public ReadLengthHistogramPlot (DataStore d) {
		super(SeqMonkApplication.getInstance(),"Read Length Plot ["+d.name()+"]");
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		plotPanel = new HistogramPanel(getReadLengths(d));
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(plotPanel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton scaleButton = new JButton("Auto Scale");
		scaleButton.setActionCommand("scale");
		scaleButton.addActionListener(this);
		buttonPanel.add(scaleButton);
		
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);
		
		JButton exportButton = new JButton("Export Data");
		exportButton.setActionCommand("export");
		exportButton.addActionListener(this);
		buttonPanel.add(exportButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		setVisible(true);

	}
	
	/**
	 * Gets the read lengths.
	 * 
	 * @param d the d
	 * @return the read lengths
	 */
	private float [] getReadLengths (DataStore d) {
		
		// TODO: This is horribly inefficient, and kludged we should do better.
		
		float [] data;
		if (d.getTotalReadCount() < 2000000000l) {
			data = new float[(int)d.getTotalReadCount()];
		}
		else {
			data = new float[2000000000];
		}
		int index = 0;
		
		Chromosome [] chrs = d.collection().genome().getAllChromosomes();

		CHR: for (int c=0;c<chrs.length;c++) {
			ReadsWithCounts reads = d.getReadsForChromosome(chrs[c]);

			for (int r=0;r<reads.reads.length;r++) {
				for (int ct=0;ct<reads.counts[r];ct++) {
					data[index] = SequenceRead.length(reads.reads[r]);
					++index;
					if (index == data.length) break CHR;
				}
			}
		}
		
		return data;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")){
			ImageSaver.saveImage(plotPanel.mainHistogramPanel());
		}
		else if (ae.getActionCommand().equals("export")) {
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
				plotPanel.exportData(file);
			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}

		}
		else if (ae.getActionCommand().equals("scale")) {
			int min = (int)plotPanel.currentMinValue();
			int max = (int)plotPanel.currentMaxValue();
			int interval = max-min;
			
			if (interval > 200) {
				// There are too many bins to draw 1 bin per base
				int multiplier = interval/200;
				
//				System.err.println("Initial multiplier is "+multiplier);
				if (multiplier != interval/200d) {
					multiplier++;
//					System.err.println("Not an exact interval, multiplier is now "+multiplier);
				}
				
				// We now know we need to put [multiplier] bins per
				// base to get it to fit

				// We now need to see if we need to change the range to
				// have it finish on a boundary
				if (interval%multiplier != 0) {
//					System.err.println("Interval "+interval+" is not divisible by "+multiplier+" adjusting min="+min+" max="+max);
					max = min+interval+(multiplier-(interval%multiplier));
//					System.err.println("New max="+max);
				}

				plotPanel.setScale(min, max, ((max-min)/multiplier));
				
			}
			
			else {
				plotPanel.setScale(min, max, (max-min));
			}

		}
	}
}
