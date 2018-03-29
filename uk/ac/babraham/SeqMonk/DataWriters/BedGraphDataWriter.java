/**
 * Copyright 2010-18 Simon Andrews
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

public class BedGraphDataWriter implements Runnable, Cancellable {

	private DataStore [] data;
	private ProbeList list;
	private File [] files;
	private File chrSizeFile;
	private Vector<ProgressListener>listeners = new Vector<ProgressListener>();
	private boolean cancel = false;

	public BedGraphDataWriter (DataStore [] stores, ProbeList list) {

		for (int i=0;i<stores.length;i++) {
			if (!stores[i].isQuantitated()) {
				throw new IllegalArgumentException("All DataStores for BEDGraph export must be quantitated");
			}
		}

		this.data = stores;
		this.list = list;		

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

		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setMultiSelectionEnabled(false);

		chooser.setFileFilter(new FileFilter() {

			public String getDescription() {
				return "BedGraph";
			}

			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".bg")) {
					return true;
				}
				return false;
			}
		});

		int result = chooser.showSaveDialog(SeqMonkApplication.getInstance());
		if (result == JFileChooser.CANCEL_OPTION) return;

		File file = chooser.getSelectedFile();
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

		if (file.isDirectory()) return;

		if (! file.getPath().toLowerCase().endsWith(".bg")) {
			file = new File(file.getPath()+".bg");
		}

		// We're going to make custom files for each of the data stores we're processing
		// so let's check whether these are going to cause problems.
		
		// We might have duplicated store names, or we might have names which aren't 
		// compatible with being turned into filenames so we need to fix both of those.
		
		String [] fileFriendlyNames = new String [data.length];
		
		HashSet<String> seenNames = new HashSet<String>();
		
		for (int i=0;i<data.length;i++) {
			String name = data[i].name();
			name = name.replaceAll("[^\\w\\d\\._-]+", "_");
			
			if (seenNames.contains(name)) {
				// We need to add a suffix to make this work
				int suffixCount = 1;
				
				while (true) {
					if (seenNames.contains(""+name+"_"+suffixCount)) {
						++suffixCount;
					}
					else {
						break;
					}
				}
				
				name = name+"_"+suffixCount;
			}
			
			seenNames.add(name);
			fileFriendlyNames[i] = name;
			
			
		}
		
		
		
		files = new File[data.length];

		ArrayList<String> existingFiles = new ArrayList<String>();

		for (int i=0;i<data.length;i++) {
			String path = file.getAbsolutePath();
			
			path = path.replaceAll("\\.bg$", "");

			path += "_"+fileFriendlyNames[i]+".bg";
			
			files[i] = new File(path);
			if (files[i].exists()) {
				existingFiles.add(path);
			}
		}

		chrSizeFile = new File(file.getParentFile().getAbsolutePath()+"/chr_sizes.txt");
		if (chrSizeFile.exists()) {
			existingFiles.add(chrSizeFile.getAbsolutePath());
		}

		// Check if we're stepping on anyone's toes...
		if (!existingFiles.isEmpty()) {

			StringBuffer sb = new StringBuffer();
			for (String path : existingFiles) {
				sb.append(path);
				sb.append("\n");
			}

			int answer = JOptionPane.showOptionDialog(SeqMonkApplication.getInstance(),sb.toString()+" exists.  Do you want to overwrite the existing file(s)?","Overwrite file(s)?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

			if (answer > 0) {
				return;
			}
		}

		addProgressListener(new ProgressDialog("Saving BEDGraph Files",this));

		Thread t = new Thread(this);
		t.start();
	}

	public void run() {

		/*
		 * We need to sort the chromosomes alphabetically rather than by their
		 * natural order
		 */

		Chromosome [] chrs = SeqMonkApplication.getInstance().dataCollection().genome().getAllChromosomes();

		Arrays.sort(chrs, new Comparator<Chromosome>() {	
			public int compare(Chromosome o1, Chromosome o2) {
				return(o1.name().compareTo(o2.name()));
			}
		});


		PrintWriter pr = null;

		for (int i=0;i<data.length;i++) {
			try {
				pr = new PrintWriter(new BufferedWriter(new FileWriter(files[i])));
			} 
			catch (IOException ex) {
				Enumeration<ProgressListener>e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressExceptionReceived(ex);
				}
				pr.close();
				return;
			}

			try {

				for (int c=0;c<chrs.length;c++) {

					// Tell the listeners how far we've got
					Enumeration<ProgressListener>e = listeners.elements();
					while (e.hasMoreElements()) {
						e.nextElement().progressUpdated("Saving BedGraph Files", (i*chrs.length)+c, chrs.length*data.length);
					}

					Probe [] probes = list.getProbesForChromosome(chrs[c]);

					for (int p=0;p<probes.length;p++) {

						// See if we need to bail out early
						if (cancel) {
							pr.close();
							files[i].delete();
							Enumeration<ProgressListener>el = listeners.elements();
							while (el.hasMoreElements()) {
								el.nextElement().progressCancelled();
							}
							return;
						}

						pr.println("chr"+probes[p].chromosome().name()+"\t"+probes[p].start()+"\t"+probes[p].end()+"\t"+data[i].getValueForProbe(probes[p]));
					}
				}
			}
			catch (SeqMonkException ex) {
				pr.close();
				Enumeration<ProgressListener>e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressExceptionReceived(ex);
				}
				return;
			}

			pr.close();
		}

		// Write out the chromsome sizes file.
		try {
			pr = new PrintWriter(new BufferedWriter(new FileWriter(chrSizeFile)));
			for (int c=0;c<chrs.length;c++) {
				pr.println("chr"+chrs[c].name()+"\t"+chrs[c].length());
			}
			pr.close();
		}
		catch (IOException ex) {
			Enumeration<ProgressListener>e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressExceptionReceived(ex);
			}
			pr.close();
			return;
		}

		Enumeration<ProgressListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressComplete("save_bedgraph", null);
		}
	}

	public void cancel() {
		cancel = true;
	}

}
