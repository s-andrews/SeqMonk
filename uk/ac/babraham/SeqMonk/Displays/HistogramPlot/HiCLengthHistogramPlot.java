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
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The HiCLengthHistogramPlot allows you to construct a histogram from
 * the range of inter-probe HiC lengths in a ProbeList
 */
public class HiCLengthHistogramPlot extends JDialog implements ActionListener, Runnable {

	private HistogramPanel plotPanel;
	private final JLabel calculatingLabel = new JLabel("Calculating Plot...",JLabel.CENTER);
	private HiCDataStore store;
	private Probe [] probes;
	
	/**
	 * Instantiates a new probe length histogram plot.
	 * 
	 * @param p The probe list to plot
	 */
	public HiCLengthHistogramPlot (HiCDataStore store, ProbeList probes)  {
		
		super(SeqMonkApplication.getInstance(),"HiC Length Plot ["+store.name()+"]");
		setSize(800,600);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				
		this.store = store;
		if (probes != null) {
			this.probes = probes.getAllProbes();
			setTitle("HiC Length Plot ["+store.name()+"] for "+probes.name());
		}
		else {
			this.probes = null;
		}
		 
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(calculatingLabel,BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
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
		
		Thread t = new Thread(this);
		t.start();

	}
	
	/**
	 * Gets the data.
	 * 
	 * @param pl the pl
	 * @return the data
	 */
	private float [] getData () {
		
		Vector<Integer>data = new Vector<Integer>();
		
		// If there is a probeset then we get pairs for the current probe list
		if (probes != null) {
			
			for (int p=0;p<probes.length;p++) {
				HiCHitCollection hiCHits = store.getHiCReadsForProbe(probes[p]);
				
				long [] sourcePositions = hiCHits.getSourcePositionsForChromosome(hiCHits.getSourceChromosomeName());
				long [] hitPositions = hiCHits.getHitPositionsForChromosome(hiCHits.getSourceChromosomeName());
					
				for (int r=0;r<sourcePositions.length;r++) {
						
					// Don't enter every pair twice
					if (SequenceRead.compare(sourcePositions[r], hitPositions[r]) < 0) continue;

					data.add(SequenceRead.fragmentLength(sourcePositions[r], hitPositions[r]));
						
				}
					
			}
		}
		
		else {		
			Chromosome [] chrs = SeqMonkApplication.getInstance().dataCollection().genome().getAllChromosomes();
			
			for (int c1=0;c1<chrs.length;c1++) {

				HiCHitCollection hiCHits = store.getHiCReadsForChromosome(chrs[c1]);
				
				long [] sourcePositions = hiCHits.getSourcePositionsForChromosome(hiCHits.getSourceChromosomeName());
				long [] hitPositions = hiCHits.getHitPositionsForChromosome(hiCHits.getSourceChromosomeName());
					
				for (int r=0;r<sourcePositions.length;r++) {
						
					// Don't enter every pair twice
					if (SequenceRead.compare(sourcePositions[r], hitPositions[r]) < 0) continue;

					data.add(SequenceRead.fragmentLength(sourcePositions[r], hitPositions[r]));
						
				}
				
			}
		}
		
		// Convert to float [] for the return array
		float [] returnData = new float[data.size()];
		
		int index = 0;
		
		Enumeration<Integer>en = data.elements();
		while (en.hasMoreElements()) {
			returnData[index] = en.nextElement().floatValue();
			index++;
		}
		
		data.clear();
		
		return returnData;
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
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		plotPanel = new HistogramPanel(getData());
		getContentPane().remove(calculatingLabel);
		getContentPane().add(plotPanel,BorderLayout.CENTER);
		getContentPane().validate();
	}
}
