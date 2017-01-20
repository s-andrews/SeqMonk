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
package uk.ac.babraham.SeqMonk.Reports;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.HTMLFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

/**
 * The Chromsome view report saves a chromosome view screenshot
 * for the region around every probe.
 */
public class ChromosomeViewReport extends Report {

	/**
	 * Instantiates a new chromsome view report
	 * 
	 * @param collection The dataCollection to use
	 * @param storesToAnnotate The set of dataStores whose data can be added to the report
	 */
	public ChromosomeViewReport(DataCollection collection,DataStore[] storesToAnnotate) {
		super(collection, storesToAnnotate);
	}

	/** The options panel. */
	private JPanel optionsPanel = null;

	/** The position of features to annotate with */
	private JTextField upstreamContextField;

	/** The annotation type to use */
	private JTextField downstreamContextField;

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#getOptionsPanel()
	 */
	public JPanel getOptionsPanel () {

		if (optionsPanel != null) return optionsPanel;

		optionsPanel = new JPanel();

		optionsPanel.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


		JPanel choicePanel1 = new JPanel();

		choicePanel1.add(new JLabel("Upstream context "));
		upstreamContextField = new JTextField("",10);
		upstreamContextField.addKeyListener(new NumberKeyListener(false, true));
		choicePanel1.add(upstreamContextField);
		choicePanel.add(choicePanel1);


		JPanel choicePanel2 = new JPanel();
		choicePanel2.add(new JLabel("Downstream context "));
		downstreamContextField = new JTextField("",10);
		downstreamContextField.addKeyListener(new NumberKeyListener(false, true));
		choicePanel2.add(downstreamContextField);
		choicePanel.add(choicePanel2);

		optionsPanel.add(choicePanel,BorderLayout.CENTER);

		return optionsPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#generateReport()
	 */
	public void generateReport () {

		Thread t = new Thread(this);
		t.start();

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Reports.Report#name()
	 */
	public String name () {
		return "Chromsome View Report";
	}


	@Override
	public boolean isReady() {
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		int upstreamContext = 0;
		if (upstreamContextField.getText().length() > 0) {
			upstreamContext = Integer.parseInt(upstreamContextField.getText());
		}

		int downstreamContext = 0;
		if (downstreamContextField.getText().length() > 0) {
			downstreamContext = Integer.parseInt(downstreamContextField.getText());
		}

		// We need an HTML file we're going to make into an index
		
		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new HTMLFileFilter());

		int result = chooser.showSaveDialog(SeqMonkApplication.getInstance());
		if (result == JFileChooser.CANCEL_OPTION) return;

		File file = chooser.getSelectedFile();
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

		if (file.isDirectory()) return;

		if (! (file.getPath().toLowerCase().endsWith(".html") || file.getPath().toLowerCase().endsWith(".html")) ) {
			file = new File(file.getPath()+".html");
		}

		// Check if we're stepping on anyone's toes...
		if (file.exists()) {
			int answer = JOptionPane.showOptionDialog(SeqMonkApplication.getInstance(),file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

			if (answer > 0) {
				return;
			}
		}

		// We need to make a folder from the file
		File outputFolder = new File(file.getAbsolutePath().replace(".html", "_files"));
		
		outputFolder.mkdir();
		
		System.err.println("Output file is "+file.getAbsolutePath()+" output folder is "+outputFolder.getAbsolutePath());
		
		Chromosome [] chrs = collection.genome().getAllChromosomes();

		// Save the starting location so we can go back there after we're done
		Chromosome currentChr = DisplayPreferences.getInstance().getCurrentChromosome();
		long currentLocation = DisplayPreferences.getInstance().getCurrentLocation();
		
		
		for (int c=0;c<chrs.length;c++) {

			progressUpdated("Processing Chr"+chrs[c].name(), c, chrs.length);

			Probe [] probes = collection.probeSet().getActiveList().getProbesForChromosome(chrs[c]);
			

			// We can now step through the probes making the image for each one
			for (int p=0;p<probes.length;p++) {

				if (cancel) {
					progressCancelled();
					return;
				}
				
				int start = probes[p].start();
				int end = probes[p].end();
				
				start -= upstreamContext;
				end += downstreamContext;
				
				if (start < 1) start = 1;
				if (end < 1) end = 1;
				if (start > chrs[c].length()) start = chrs[c].length();
				if (end > chrs[c].length()) end = chrs[c].length();
				
				if (end - start < 100) {
					progressWarningReceived(new SeqMonkException("View for "+probes[p].name()+" was too small to export"));
					continue;
				}

				DisplayPreferences.getInstance().setLocation(chrs[c], SequenceRead.packPosition(start, end, Location.FORWARD));
				
				// TODO: Is this immediate, or do we need to wait?
				// It's not immediate - we need to figure out how to tell when it's finished.
				
				try {
					Thread.sleep(1000);
				} 
				catch (InterruptedException e1) {}
				
				File saveFile = new File(outputFolder.getAbsolutePath()+"/"+probes[p].name()+".png");
	
				try {
					ImageSaver.savePNG(SeqMonkApplication.getInstance().chromosomeViewer(), saveFile);
				} 
				catch (IOException e) {
					progressExceptionReceived(e);
					return;
				}
				
			}
		}

		DisplayPreferences.getInstance().setLocation(currentChr, currentLocation);
		
		reportComplete(null);
	}

}
