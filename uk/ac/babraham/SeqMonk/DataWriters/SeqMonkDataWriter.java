/**
 * Copyright Copyright 2010-18 Simon Andrews
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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.PairedDataSet;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationTagValue;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.CoreAnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.HiCHitCollection;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The Class SeqMonkDataWriter serialises a SeqMonk project to a single file.
 */
public class SeqMonkDataWriter implements Runnable, Cancellable {
	
	// THIS VALUE IS IMPORTANT!!!
	/** The Constant DATA_VERSION. */
	public static final int DATA_VERSION = 17;
	
	// If you make ANY changes to the format written by this class
	// you MUST increment this value to stop older parsers from
	// trying to parse it.  Once you have updated the parser to 
	// read the new format you can then update the corresponding
	// value in the parser so that it will work.
	
	
	/*
	 * TODO: Some of these data sets take a *long* time to save due
	 * to the volume of data.  Often when people are saving they're
	 * just saving display preferences.  In these cases it would be
	 * nice to have a mode where the display preferences were just
	 * appended to the end of an existing file, rather than having
	 * to put out the whole thing again.  Since the size of the
	 * preferences section is pretty small it won't affect overall
	 * file size much.
	 * 
	 * If the data (probes, groups or quantitation) changes then
	 * we'll have to do a full rewrite.
	 */
	
	/** The listeners. */
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();
		
	/** The data. */
	private DataCollection data;
	
	/** The genome. */
	private Genome genome;
	
	/** The final file to save to file. */
	private File file;
	
	/** The temporary file to work with */
	private File tempFile;
	
	/** The default feature tracks. */
	private String [] defaultFeatureTracks;
	
	/** The visible stores. */
	private DataStore [] visibleStores;
	
	/** Whether to cancel */
	private boolean cancel = false;
	
	/**
	 * Instantiates a new seq monk data writer.
	 */
	public SeqMonkDataWriter () {
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

	
	/**
	 * Write data.
	 * 
	 * @param application the application
	 * @param file the file
	 */
	public void writeData (SeqMonkApplication application, File file) {
		data = application.dataCollection();
		genome = data.genome();
		this.file = file;
		defaultFeatureTracks = application.drawnFeatureTypes();
		visibleStores = application.drawnDataStores();
		
		Thread t = new Thread(this);
		t.start();
	}

	/**
	 * Write data.
	 * 
	 * @param application the application
	 * @param file the file
	 */
	public void writeData (DataCollection data, File file) {
		this.data = data;
		genome = data.genome();
		this.file = file;
		
		// We made all data sets visible by default
		visibleStores = data.getAllDataStores();
		
		Thread t = new Thread(this);
		t.start();
	}
	
	
	
	public void cancel() {
		cancel = true;
	}
	
	private void cancelled (PrintStream p) throws IOException {
		p.close();
		
		if (!tempFile.delete()) {
			throw new IOException("Couldn't delete temp file");
		}
		Enumeration<ProgressListener>e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressCancelled();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			
			// Generate a temp file in the same directory as the final
			// destination
			tempFile = File.createTempFile("seqmonk",".temp", file.getParentFile());
			
			BufferedOutputStream bos;
			
			if (SeqMonkPreferences.getInstance().compressOutput()) {
				bos = new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(tempFile),2048));
			}
			else {
				bos = new BufferedOutputStream(new FileOutputStream(tempFile));
			}			
			PrintStream p = new PrintStream(bos);
			
			printDataVersion(p);
			
			printAssembly(p);
						

			DataSet [] dataSets = data.getAllDataSets();
			DataGroup [] dataGroups = data.getAllDataGroups();
			ReplicateSet [] replicateSets = data.getAllReplicateSets();
			
			if (!printDataSets(dataSets,p)) {
				return;  // They cancelled
			}
			
			printDataGroups(dataSets,dataGroups,p);
			
			printReplicateSets(dataSets, dataGroups, replicateSets, p);
			
			AnnotationSet [] annotationSets = data.genome().annotationCollection().anotationSets();
			for (int a=0;a<annotationSets.length;a++) {
				if (annotationSets[a] instanceof CoreAnnotationSet) continue;
				
				if (!printAnnotationSet(annotationSets[a],p)) {
					// They cancelled
					return;
				}
			}
			
			Probe [] probes = null;
			
			if (data.probeSet() != null) {
				probes = data.probeSet().getAllProbes();
			}

			if (probes != null) {
				if (! printProbeSet(data.probeSet(), probes, dataSets, dataGroups, p)) {
					return; // They cancelled
				}
			}

			if (visibleStores != null) {
				printVisibleDataStores(dataSets, dataGroups, replicateSets, p);
			}

			if (probes != null) {
				if (!printProbeLists(probes,p)) {
					return; // They cancelled
				}
			}
			
			if (defaultFeatureTracks != null) {
				printDisplayPreferences(p);
			}
			
			p.close();

			// We can now overwrite the original file
			if (file.exists()) {
				if (!file.delete()) {
					throw new IOException("Couldn't delete old project file when making new one");
				}
			}
			
			if (! tempFile.renameTo(file)) {
				throw new IOException("Failed to rename temporary file");
			}
			
			Enumeration<ProgressListener> e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressComplete("data_written", null);
			}
		}
		
		catch (Exception ex) {
			Enumeration<ProgressListener> e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressExceptionReceived(ex);
			}
		}
		
	}

	/**
	 * Prints the data version.
	 * 
	 * @param p the p
	 */
	private void printDataVersion (PrintStream p) {
		// The first line of the file will be the version of the data
		// format we're using.  This will help us out should we need 
		// to update the format in the future.
		p.println("SeqMonk Data Version\t"+DATA_VERSION);
	}
	
	/**
	 * Prints the assembly.
	 * 
	 * @param p the p
	 */
	private void printAssembly (PrintStream p) {
		// The next thing we need to do is to output the details of the genome
		// we're using
						
		p.println("Genome\t"+genome.species()+"\t"+genome.assembly());
		
		// Now we print out the list of default feature tracks to show
		if (defaultFeatureTracks != null) {
			p.println("Features\t"+defaultFeatureTracks.length);
			for (int i=0;i<defaultFeatureTracks.length;i++) {
				p.println(defaultFeatureTracks[i]);
			}
		}
	}
	
	/**
	 * Prints the annotation set.
	 * 
	 * @param a the a
	 * @param p the p
	 * @return false if cancelled, else true;
	 */
	private boolean printAnnotationSet (AnnotationSet a, PrintStream p) throws IOException {		
		Feature [] features = a.getAllFeatures();
		p.println("Annotation\t"+a.name()+"\t"+features.length);
		
		Enumeration<ProgressListener> e = listeners.elements();
		while (e.hasMoreElements()) {
			e.nextElement().progressUpdated("Writing annotation set "+a.name(),0,1);
		}

		
		for (int f=0;f<features.length;f++) {
			
			if (cancel) {
				cancelled(p);
				return false;
			}
			
			StringBuffer b = new StringBuffer();
			b.append(features[f].type());
			b.append("\t");
			b.append(features[f].chromosomeName());
			b.append("\t");
			b.append(features[f].location().locationString());

			AnnotationTagValue [] tagValues = features[f].getAnnotationTagValues();
			
			for (int i=0;i<tagValues.length;i++) {
				b.append("\t");
				b.append(tagValues[i].tag());
				b.append("\t");
				b.append(tagValues[i].value());
			}

			p.println(b.toString());
			
		}
		
		return true;
		
	}
	
	
	
	/**
	 * Prints the data sets.
	 * 
	 * @param dataSets the data sets
	 * @param p the p
	 * @return false if cancelled, else true
	 */
	private boolean printDataSets (DataSet [] dataSets, PrintStream p) throws IOException {
		p.println("Samples\t"+dataSets.length);
		for (int i=0;i<dataSets.length;i++) {
			if (dataSets[i] instanceof PairedDataSet) {
				p.println(dataSets[i].name()+"\t"+dataSets[i].fileName()+"\tHiC");
			}
			else {
				p.println(dataSets[i].name()+"\t"+dataSets[i].fileName()+"\t");
			}
		}

		// We now need to print the data for each data set
		for (int i=0;i<dataSets.length;i++) {
			Enumeration<ProgressListener> e = listeners.elements();
			while (e.hasMoreElements()) {
				e.nextElement().progressUpdated("Writing data for "+dataSets[i].name(),i*10,dataSets.length*10);
			}
			

			if (dataSets[i] instanceof PairedDataSet) {
				boolean returnValue = printPairedDataSet((PairedDataSet)dataSets[i], p, i, dataSets.length);
				if (! returnValue) return false; // They cancelled				
			}
			else {
				boolean returnValue = printStandardDataSet(dataSets[i], p, i, dataSets.length);
				if (! returnValue) return false; // They cancelled
			}
		}
		
		return true;
	}
	
	private boolean printPairedDataSet (PairedDataSet set, PrintStream p, int index, int indexTotal) throws IOException {
		
		p.println(set.getTotalReadCount()*2+"\t"+set.name());

		// Go through one chromosome at a time.
		Chromosome [] chrs = data.genome().getAllChromosomes();
		for (int c=0;c<chrs.length;c++) {
			
			HiCHitCollection hiCHits = set.getHiCReadsForChromosome(chrs[c]);			
			
			// Work out how many of these reads we're actually going to output
			int validReadCount = 0;
			for (int c2=0;c2<chrs.length;c2++) {
				validReadCount += hiCHits.getSourcePositionsForChromosome(chrs[c2].name()).length;
			}
			
			p.println(chrs[c].name()+"\t"+validReadCount);
			
			
			for (int c2=0;c2<chrs.length;c2++) {
				
				long [] sourceReads = hiCHits.getSourcePositionsForChromosome(chrs[c2].name());
				long [] hitReads = hiCHits.getHitPositionsForChromosome(chrs[c2].name());
				
				for (int j=0;j<sourceReads.length;j++) {

					if (cancel) {
						cancelled(p);
						return false;
					}

					// TODO: Fix the progress bar
					if ((j % (1+(validReadCount/10))) == 0) {
						Enumeration<ProgressListener> e2 = listeners.elements();
						while (e2.hasMoreElements()) {
							e2.nextElement().progressUpdated("Writing data for "+set.name(),index*chrs.length+c,indexTotal*chrs.length);
						}

					}
				
					p.println(sourceReads[j]+"\t"+chrs[c2].name()+"\t"+hitReads[j]);
				}
			}
		}
		// Print a blank line after the last chromosome
		p.println("");


		return true;
	}
	
	private boolean printStandardDataSet (DataSet set, PrintStream p, int index, int indexTotal) throws IOException {
			
		p.println(set.getTotalReadCount()+"\t"+set.name());

		// Go through one chromosome at a time.
		Chromosome [] chrs = data.genome().getAllChromosomes();
		for (int c=0;c<chrs.length;c++) {
			long [] reads = set.getReadsForChromosome(chrs[c]);
			p.println(chrs[c].name()+"\t"+reads.length);

			long lastRead = 0;
			int lastReadCount = 0;
			
			for (int j=0;j<reads.length;j++) {

				if (cancel) {
					cancelled(p);
					return false;
				}

				if ((j % (1+(reads.length/10))) == 0) {
					Enumeration<ProgressListener> e2 = listeners.elements();
					while (e2.hasMoreElements()) {
						e2.nextElement().progressUpdated("Writing data for "+set.name(),index*chrs.length+c,indexTotal*chrs.length);
					}

				}

				if (lastReadCount == 0 || reads[j] == lastRead) {
					lastRead = reads[j];
					++lastReadCount;
				}
				
				else {
					if (lastReadCount > 1) {
						p.println(lastRead+"\t"+lastReadCount);
					}
					else if (lastReadCount == 1) {
						p.println(lastRead);
					}
					else {
						throw new IllegalStateException("Shouldn't have zero count ever, read is "+reads[j]+" last read is "+lastRead+" count is "+lastReadCount);
					}
					lastRead = reads[j];
					lastReadCount = 1;
				}
			}
			if (lastReadCount > 1) {
				p.println(lastRead+"\t"+lastReadCount);
			}
			
			// If there are no reads on a chromosome then this value could be zero
			else if (lastReadCount == 1){
				p.println(lastRead);
			}
			
		}
		// Print a blank line after the last chromosome
		p.println("");


		return true;
	}
	
	/**
	 * Prints the data groups.
	 * 
	 * @param dataSets the data sets
	 * @param dataGroups the data groups
	 * @param p the p
	 */
	private void printDataGroups (DataSet [] dataSets, DataGroup [] dataGroups, PrintStream p) {

		p.println("Data Groups\t"+dataGroups.length);
		for (int i=0;i<dataGroups.length;i++) {
			DataSet [] groupSets = dataGroups[i].dataSets();
			
			// We used to use the name of the dataset to populate the group
			// but this caused problems when we had duplicated dataset names
			// we therefore have to figure out the index of each dataset in
			// each group
			
			
			StringBuffer b = new StringBuffer();
			b.append(dataGroups[i].name());
			for (int j=0;j<groupSets.length;j++) {
				for (int d=0;d<dataSets.length;d++) {
					if (groupSets[j] == dataSets[d]) {
						b.append("\t");
						b.append(d);
						continue;
					}
				}
			}
			
			p.println(b);
		}
	}

	/**
	 * Prints the replicate sets.
	 * 
	 * @param dataSets the data sets
	 * @param dataGroups the data groups
	 * @param replicates the replicate sets
	 * @param p the printwriter
	 */
	private void printReplicateSets (DataSet [] dataSets, DataGroup [] dataGroups, ReplicateSet [] replicates, PrintStream p) {

		p.println("Replicate Sets\t"+replicates.length);
		for (int i=0;i<replicates.length;i++) {
			DataStore [] stores = replicates[i].dataStores();
						
			StringBuffer b = new StringBuffer();
			b.append(replicates[i].name());
			for (int j=0;j<stores.length;j++) {
				
				if (stores[j] instanceof DataSet) {
					for (int d=0;d<dataSets.length;d++) {
						if (stores[j] == dataSets[d]) {
							b.append("\ts");
							b.append(d);
							continue;
						}
					}					
				}
				else if (stores[j] instanceof DataGroup) {
					for (int d=0;d<dataGroups.length;d++) {
						if (stores[j] == dataGroups[d]) {
							b.append("\tg");
							b.append(d);
							continue;
						}
					}					
				}
				else {
					throw new IllegalArgumentException("Member of replicate set wasn't a dataset or a data group");
				}
				
			}
			
			p.println(b);
		}
	}

	
	/**
	 * Prints the probe set.
	 * 
	 * @param probeSet the probe set
	 * @param probes the probes
	 * @param dataSets the data sets
	 * @param dataGroups the data groups
	 * @param p the p
	 * @return false if cancelled, else true
	 */
	private boolean printProbeSet (ProbeSet probeSet, Probe [] probes, DataSet [] dataSets, DataGroup [] dataGroups, PrintStream p) throws IOException {
		//Put out the number of probes
		
		String probeSetQuantitation = "";
		if (probeSet.currentQuantitation() != null) {
			probeSetQuantitation = probeSet.currentQuantitation();
		}
		
		// We need the saved string to be linear so we replace the line breaks with ` (which we've replaced with ' in the
		// comment.  We put back the line breaks when we load the comments back.
		
		String comments = probeSet.comments().replaceAll("[\\r\\n]", "`");
		
		p.println("Probes\t"+probes.length+"\t"+probeSet.justDescription()+"\t"+probeSetQuantitation+"\t"+comments);

		// Next we print out the data

		for (int i=0;i<probes.length;i++) {
			
			if (cancel) {
				cancelled(p);
				return false;
			}
			
			if (i%1000 == 0) {
				Enumeration<ProgressListener> e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressUpdated("Written data for "+i+" probes out of "+probes.length,i,probes.length);
				}
			}
			
			StringBuffer b = new StringBuffer();
			if (probes[i].hasDefinedName()) {
				b.append(probes[i].name());
			}
			else {
				b.append("null");
			}
			b.append("\t");
			b.append(probes[i].chromosome().name());
			b.append("\t");
			b.append(probes[i].packedPosition());
							
			for (int j=0;j<dataSets.length;j++) {
				b.append("\t");
				if (!dataSets[j].hasValueForProbe(probes[i])) {
					// It's OK for some probes not to have any value - just skip these.
					continue;
				}
				try {
					b.append(dataSets[j].getValueForProbe(probes[i]));
				}
				catch (SeqMonkException e) {
					e.printStackTrace();
				}
				
			}
			for (int j=0;j<dataGroups.length;j++) {
				b.append("\t");
				try {
					b.append(dataGroups[j].getValueForProbe(probes[i]));
				}
				catch (SeqMonkException e) {
					// This can happen if a group is made but never quantiated.
				}
			}
			p.println(b.toString());
		}
		return true;
	}
	
	/**
	 * Prints the visible data stores.
	 * 
	 * @param dataSets the data sets
	 * @param dataGroups the data groups
	 * @param p the p
	 */
	private void printVisibleDataStores (DataSet [] dataSets, DataGroup [] dataGroups, ReplicateSet [] replicates, PrintStream p) {
		// Now we can put out the list of visible stores
		// We have to refer to these by position rather than name
		// since names are not guaranteed to be unique.
		p.println("Visible Stores\t"+visibleStores.length);
		for (int i=0;i<visibleStores.length;i++) {
			if (visibleStores[i] instanceof DataSet) {
				for (int s=0;s<dataSets.length;s++) {
					if (visibleStores[i] == dataSets[s]) {							
						p.println(s+"\t"+"set");
					}
				}
			}
			else if (visibleStores[i] instanceof DataGroup) {
				for (int g=0;g<dataGroups.length;g++) {
					if (visibleStores[i] == dataGroups[g]) {							
						p.println(g+"\t"+"group");
					}
				}
			}
			else {
				for (int s=0;s<replicates.length;s++) {
					if (visibleStores[i] == replicates[s]) {							
						p.println(s+"\t"+"replicate");
					}
				}
				
			}
		}
	}
	
	/**
	 * Prints the probe lists.
	 * 
	 * @param probes the probes
	 * @param p the p
	 */
	private boolean printProbeLists (Probe [] probes, PrintStream p) throws SeqMonkException,IOException {
		// Now we print out the list of probe lists
		
		/*
		 * We rely on this list coming in tree order, that is to say that
		 * when we see a node at depth n we assume that all subsequent nodes
		 * at depth n+1 are children of the first node, until we see another
		 * node at depth n.
		 * 
		 * This should be how the nodes are created anyway.
		 */
		ProbeList [] lists = data.probeSet().getAllProbeLists();
		
		// The way we determine which probes are in which list is to pull
		// out the ordered set of probes from the lists and then compare
		// each of the full set of probes to the position we've reached in
		// each list.  We therefore need the full set of lists, and an
		// array of ints to keep track of where we've got to in each of them.
		
		Probe [][] orderedProbes = new Probe [lists.length][];
		int [] orderedProbeIndices = new int [lists.length];
		
		for (int l=0;l<lists.length;l++) {
			orderedProbes[l] = lists[l].getAllProbes();
			orderedProbeIndices[l] = 0;
		}

		// We start at the second list since the first list will always
		// be "All probes" which we'll sort out some other way.
		
		p.println("Lists\t"+(lists.length-1));
				
		for (int i=1;i<lists.length;i++) {
			String listComments = lists[i].comments().replaceAll("[\\r\\n]", "`");
			p.println(getListDepth(lists[i])+"\t"+lists[i].name()+"\t"+lists[i].getValueName()+"\t"+lists[i].description()+"\t"+listComments);
		}
		
		//Put out the number of probes
		p.println("Probes\t"+probes.length);
		// Now we print out the data for the probe lists
		
		
		for (int i=0;i<probes.length;i++) {
			
			if (cancel) {
				cancelled(p);
				return false;
			}
			if (i%1000 == 0) {
				Enumeration<ProgressListener> e = listeners.elements();
				while (e.hasMoreElements()) {
					e.nextElement().progressUpdated("Written lists for "+i+" probes out of "+probes.length,i,probes.length);
				}
			}
			
			StringBuffer b = new StringBuffer();
			b.append(probes[i].name());
			
			for (int j=1;j<lists.length;j++) {
				b.append("\t");
				
				// If we've not reached the end of this list, and if this
				// probe is the next one in this list then we print out the
				// value it has associated with it.
				if (orderedProbeIndices[j] < orderedProbes[j].length && orderedProbes[j][orderedProbeIndices[j]] == probes[i]) {
					b.append(lists[j].getValueForProbe(probes[i]));
					orderedProbeIndices[j]++;
				}
			}
			p.println(b.toString());
		}
		
		// Check that we've written everything out for all of the probes we have
		for (int i=1;i<orderedProbes.length;i++) {
			if (orderedProbeIndices[i] != orderedProbes[i].length) {
				throw new SeqMonkException("Probe list "+i+" only reported "+orderedProbeIndices[i]+" out of "+orderedProbes[i].length+" probes");
			}
		}
		
		
		return true;
	}
	
	/**
	 * Prints the display preferences.
	 * 
	 * @param p the print stream to write the preferences to
	 */
	private void printDisplayPreferences(PrintStream p) {
		// Now write out some display preferences
		DisplayPreferences.getInstance().writeConfiguration(p);
		
	}
	

	/**
	 * Gets the list depth.
	 * 
	 * @param p the p
	 * @return the list depth
	 */
	private int getListDepth (ProbeList p) {
		int depth = 0;
		
		while (p.parent() != null) {
			depth++;
			p = p.parent();
		}
		return depth;
	}
}
