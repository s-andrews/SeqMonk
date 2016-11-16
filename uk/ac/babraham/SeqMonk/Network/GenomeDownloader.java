/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The GenomeDownloader actually performs the network interaction required
 * to download a new genome from the main genome database and install it
 * in the local genome cache.
 */
public class GenomeDownloader implements Runnable {

	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();
	private SeqMonkPreferences prefs = SeqMonkPreferences.getInstance();
	private String species;
	private String assembly;
	private boolean allowCaching;
	private int size;
	
	
	/**
	 * Download genome.  The values for this should be obtained from the genome
	 * index file or the header of an existing SeqMonk file.  The size is used
	 * merely to provide better feedback during the download of the data and isn't
	 * expected to be set correctly from a SeqMonk file where the compressed size
	 * isn't recorded.
	 * 
	 * @param species The latin name of the species
	 * @param assembly The official assembly name
	 * @param size The size of the download in bytes
	 * @param allowCaching sets the cache headers to say if a cached copy is OK
	 */
	public void downloadGenome (String species, String assembly, int size, boolean allowCaching) {
		
		this.species = species;
		this.assembly = assembly;
		this.allowCaching = allowCaching;
		this.size = size;
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Adds a progress listener.
	 * 
	 * @param pl The progress listener to add
	 */
	public void addProgressListener (ProgressListener pl) {
		if (pl != null && ! listeners.contains(pl))
			listeners.add(pl);
	}
	
	/**
	 * Removes a progress listener.
	 * 
	 * @param pl The progress listener to remove
	 */
	public void removeProgressListener (ProgressListener pl) {
		if (pl != null && listeners.contains(pl))
			listeners.remove(pl);
	}
		
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		
		// First we need to download the file from the repository
		try {
			
//			System.out.println("Downloading "+prefs.getGenomeDownloadLocation()+species+"/"+assembly+".zip");
			URL url = new URL((new String(prefs.getGenomeDownloadLocation()+species+"/"+assembly+".zip")).replaceAll(" ","%20"));
			
			URLConnection connection = url.openConnection();
			connection.setUseCaches(allowCaching);
			
			InputStream is = connection.getInputStream();
			DataInputStream d = new DataInputStream(new BufferedInputStream(is));
//			System.out.println("Output file is "+prefs.getGenomeBase()+"/"+assembly+".zip");
			File f = new File(prefs.getGenomeBase()+"/"+assembly+".zip");
			DataOutputStream o;
			try {	
				o = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			}
			catch (FileNotFoundException fnfe) {
				throw new SeqMonkException("Could't write into your genomes directory.  Please check your file preferences.");
			}
			byte [] b = new byte [1024];
			int totalBytes = 0;
			int i;
			while ((i=d.read(b))>0){
//				System.out.println("Read "+totalBytes+" bytes");
				o.write(b,0,i);
				totalBytes += i;
				Enumeration<ProgressListener> en = listeners.elements();
				
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("Downloaded "+totalBytes/1048576+"Mb",totalBytes,size);
				}
			}

			d.close();
			o.close();
			
			File outFile = new File(prefs.getGenomeBase()+"/"+species+"/"+assembly);
			outFile.mkdirs();
			// Now we can uncompress the downloaded file to create the genome
			ZipFile zipFile = new ZipFile(f);
			Enumeration<? extends ZipEntry> e = zipFile.entries();
			int count = 0;
			while (e.hasMoreElements()) {
				count++;
				Enumeration<ProgressListener> en = listeners.elements();
				
				while (en.hasMoreElements()) {
					en.nextElement().progressUpdated("Unzipped "+count+" files",count,zipFile.size());
				}
				
				ZipEntry ze = e.nextElement();

				if (ze.isDirectory()) continue;
//				System.out.println("Found entry "+ze.getName()+" with size "+ze.getSize());
				BufferedOutputStream out;
				try {	
					out  = new BufferedOutputStream(new FileOutputStream(prefs.getGenomeBase()+"/"+ze.getName()));
				}
				catch (FileNotFoundException fnfe) {
					throw new SeqMonkException("Couldn't write into the genomes directory.  Please check your file permissions or change your genomes folder.");
				}
				BufferedInputStream in = new BufferedInputStream(zipFile.getInputStream(ze));
				while ((i=in.read(b))>0) {
					out.write(b,0,i);
				}
				in.close();
				out.close();
			}
			// Get rid of the zip file so we don't waste disk space.
			zipFile.close();
			f.delete();
			
			// Finally we need to clear out any cache files which exist from a previous
			// installation of this genome so the new one will be cached the next time
			// it's loaded.
			
			File cacheDir = new File(outFile.getAbsoluteFile()+"/cache");
			
			if (cacheDir.exists()) {
				File [] cacheFiles = cacheDir.listFiles();
				for (int cf=0;cf<cacheFiles.length;cf++) {
					if (cacheFiles[cf].isFile() && cacheFiles[cf].getName().indexOf("cache")>=0) {
						if (!cacheFiles[cf].delete()) {
							System.out.println("Failed to delete cache file "+cacheFiles[cf].getAbsolutePath());
						}
					}
				}
			}
			
			
		} 
		catch (Exception ex) {
			ProgressListener [] en = listeners.toArray(new ProgressListener[0]);
			
			for (int i=en.length-1;i>=0;i--) {
				en[i].progressExceptionReceived(ex);
			}
			return;
		}

		// Tell everyone we're finished
		
		/*
		 * Something odd happens here on my linux system.  If I notify the listeners
		 * in the usual order then I'm told that there are two listeners, but the loop
		 * through these listeners (either via Enumeration or array) only notifies one
		 * (SeqMonkApplication) and the progress dialog is never told.
		 * 
		 * If I notify them in reverse order then it works as expected, but I can't see
		 * why telling the application first should stop further processing.
		 * 
		 * On my windows system I don't get this problem.
		 * 
		 */
		ProgressListener [] en = listeners.toArray(new ProgressListener[0]);
				
		for (int i=en.length-1;i>=0;i--) {
			en[i].progressComplete("genome_downloaded", null);
		}
		
	}
	
}
