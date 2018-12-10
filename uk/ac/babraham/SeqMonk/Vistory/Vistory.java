package uk.ac.babraham.SeqMonk.Vistory;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

public class Vistory {

	/**
	 * This is the main class to hold a vistory.  Each project is only going to have a
	 * single vistory so we're going to construct this as a singleton.
	 */

	private static final Vistory vistory = new Vistory();
	private Vector<VistoryListener> listeners = new Vector<VistoryListener>();
	
	private Vector<VistoryBlock> blocks = new Vector<VistoryBlock>();
	
	private Vistory () {}
	
	public void addListener (VistoryListener l) {
		if (!listeners.contains(l)) listeners.add(l);
	}
	
	public void removeListener (VistoryListener l) {
		if (listeners.contains(l)) listeners.removeElement(l);
	}
	
	public static Vistory getInstance() {
		return(vistory);
	}
	
	public void addBlock (VistoryBlock block) {
		blocks.add(block);
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blockAdded(block);
		}
	}
	
	public void removeBlock (VistoryBlock block) {
		if (! blocks.contains(block))return;
		blocks.remove(block);
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blockRemoved(block);
		}
	}
	
	public VistoryBlock [] blocks () {
		return blocks.toArray(new VistoryBlock[0]);
	}
	
	
	public void writeReport (File file) {
		// TODO: Write the report.
	}
	
	
	
	
	
}
