package uk.ac.babraham.SeqMonk.Vistory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
	
	
	public void writeReport (File file) throws IOException {

		PrintWriter pr = new PrintWriter(file);
		
		// Grab the header template
		BufferedReader header = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("Templates/vistory_header.html")));
		
		String line;
		while ((line = header.readLine()) != null) {
			pr.print(line);
		}
		
		// Write the title index
		
		// Write out the HTML from the blocks
		for (int b=0;b<blocks.size();b++) {
			pr.println(blocks.elementAt(b).getHTML());
		}
		
		// Grab the footer template
		BufferedReader footer = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("Templates/vistory_footer.html")));
		
		while ((line = footer.readLine()) != null) {
			pr.print(line);
		}
		
		pr.close();
	}
	
	
	public static void main (String [] args) {
		File f = new File("C:/Users/andrewss/Desktop/vistory.html");
		try {
			Vistory.getInstance().writeReport(f);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
