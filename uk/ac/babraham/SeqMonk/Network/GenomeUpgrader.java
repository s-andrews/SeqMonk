/**
 * Copyright 2010-17 Simon Andrews
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

import java.util.Enumeration;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.GenomeAssembly;

public class GenomeUpgrader implements Runnable, ProgressListener {

	private GenomeAssembly [] genomes;
	private boolean wait = false;
	private boolean cancel = false;
	private Vector<ProgressListener> listeners = new Vector<ProgressListener>();
	private int currentProgress = 0;
	
	
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
	
	public void upgradeGenomes (GenomeAssembly [] genomes) {
		this.genomes = genomes;
		Thread t = new Thread(this);
		t.start();
	}
	
	
	public void run () {
		for (int i=0;i<genomes.length;i++) {
			currentProgress = i;
			GenomeDownloader downloader = new GenomeDownloader();
			downloader.addProgressListener(this);
			wait = true;
			downloader.downloadGenome(genomes[i].set().species().name(), genomes[i].assembly(), genomes[i].fileSize(), false);
			while (wait) {
				try {
					Thread.sleep(500);
				} 
				catch (InterruptedException e) {}
			}
			
			if (cancel) return;
		}
		
		Enumeration<ProgressListener> e = listeners.elements();
		
		while (e.hasMoreElements()) {
			e.nextElement().progressComplete("upgrade_genomes", null);
		}

	}


	public void progressCancelled() {
		Enumeration<ProgressListener> e = listeners.elements();
		
		while (e.hasMoreElements()) {
			e.nextElement().progressCancelled();
		}
		
	}

	public void progressComplete(String command, Object result) {
		wait = false;
	}

	public void progressExceptionReceived(Exception ex) {
		cancel = true;
		Enumeration<ProgressListener> e = listeners.elements();
		
		while (e.hasMoreElements()) {
			e.nextElement().progressExceptionReceived(ex);
		}
	}

	public void progressUpdated(String message, int current, int max) {
		Enumeration<ProgressListener> e = listeners.elements();
		
		while (e.hasMoreElements()) {
			e.nextElement().progressUpdated(message, currentProgress, genomes.length);
		}

	}

	public void progressWarningReceived(Exception ex) {
		Enumeration<ProgressListener> e = listeners.elements();
		
		while (e.hasMoreElements()) {
			e.nextElement().progressWarningReceived(ex);
		}

	}
	
}
