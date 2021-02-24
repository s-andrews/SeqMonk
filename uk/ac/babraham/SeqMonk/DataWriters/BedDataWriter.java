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
package uk.ac.babraham.SeqMonk.DataWriters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.ReadsWithCounts;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ValidFileNameGenerator;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.BEDFileFilter;

public class BedDataWriter implements Runnable, Cancellable {

	private DataStore data;
	private File file;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;

	public BedDataWriter (DataStore store) {
		this.data = store;
	}


	/**
	 * Adds the progress listener.
	 * 
	 * @param l the l
	 */
	public void addProgressListener (ProgressListener l) {
		if (l != null && ! listeners.contains(l))
			listeners.add(l);
	}

	/**
	 * Removes the progress listener.
	 * 
	 * @param l the l
	 */
	public void removeProgressListener (ProgressListener l) {
		if (l != null && listeners.contains(l))
			listeners.remove(l);
	}


	public void startProcessing () {
		
		// Get the filename to export under.
		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setSelectedFile(new File(ValidFileNameGenerator.makeValidFileName(data.name())));
		chooser.setMultiSelectionEnabled(false);
				
		BEDFileFilter bedff = new BEDFileFilter();
		chooser.addChoosableFileFilter(bedff);
		chooser.setFileFilter(bedff);
		
		int result = chooser.showSaveDialog(SeqMonkApplication.getInstance());
		if (result == JFileChooser.CANCEL_OPTION) return;

		file = chooser.getSelectedFile();
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
		
		if (file.isDirectory()) return;

		FileFilter filter = chooser.getFileFilter();
		
		if (filter instanceof BEDFileFilter) {		
			if (! file.getPath().toLowerCase().endsWith(".bed")) {
				file = new File(file.getPath()+".bed");
			}
		}


		// Check if we're stepping on anyone's toes...
		if (file.exists()) {

			int answer = JOptionPane.showOptionDialog(SeqMonkApplication.getInstance(),file.getName()+" exists.  Do you want to overwrite the existing file(s)?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

			if (answer > 0) {
				return;
			}
		}

		addProgressListener(new ProgressDialog("Saving BED File",this));

		Thread t = new Thread(this);
		t.start();
	}

	public void run() {

		PrintWriter pr;
		try {
			pr = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		} 
		catch (IOException ioe) {
			throw new IllegalStateException(ioe);
		}

		Chromosome [] chrs = SeqMonkApplication.getInstance().dataCollection().genome().getAllChromosomes();

		Arrays.sort(chrs, new Comparator<Chromosome>() {	
			public int compare(Chromosome o1, Chromosome o2) {
				return(o1.name().compareTo(o2.name()));
			}
		});

		for (int c=0;c<chrs.length;c++) {

			// Tell the listeners how far we've got
			Enumeration<ProgressListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressUpdated("Saving Bed File", c, chrs.length);
			}

			ReadsWithCounts reads = data.getReadsForChromosome(chrs[c]);
			
			for (int r=0;r<reads.reads.length;r++) {

				// See if we need to bail out early
				if (cancel) {
					pr.close();
					file.delete();
					Enumeration<ProgressListener>el = listeners.elements();
					while (el.hasMoreElements()) {
						el.nextElement().progressCancelled();
					}
					return;
				}

				for (int j=0;j<reads.counts[r];j++) {
					pr.println("chr"+chrs[c].name()+"\t"+SequenceRead.start(reads.reads[r])+"\t"+SequenceRead.end(reads.reads[r])+"\t.\t.\t"+SequenceRead.strandSymbol(reads.reads[r]));
				}
			}

			pr.close();
		}
		
		Enumeration<ProgressListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressComplete("save_bed", null);
		}
	}

	public void cancel() {
		cancel = true;
	}

}
