package uk.ac.babraham.SeqMonk.Vistory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

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
	
	public void requestVistoryFocus (VistoryBlock b) {
		// Go through all of the blocks setting the focus to the
		// one we're looking at now.
		
		Iterator<VistoryBlock> it = blocks.iterator();
		
		System.err.println("Setting blocks to "+b);
		while (it.hasNext()) {
			VistoryBlock block = it.next();
			
			if (block.equals(b)) {
				System.err.println("Found block");
				block.setVistoryFocus(true);
			}
			else {
				System.err.println("Reset block");
				block.setVistoryFocus(false);
			}
		}
		System.err.println("Finished");

	}
	
	public void clear () {
		VistoryBlock [] blocksToClear = blocks();
		
		for (int b=0;b<blocksToClear.length;b++) {
			removeBlock(blocksToClear[b]);
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
		
		pr.println("<div class=\"toc\">");
		pr.println("<ul class=\"toc\">");
		
		pr.println("<li><a href=\"#top\">Top</a></li>");
		int currentIndex = 1;
		for (int b=0;b<blocks.size();b++) {
			if (blocks.elementAt(b) instanceof VistoryTitle) {
				VistoryTitle block = (VistoryTitle)blocks.elementAt(b);
				block.setIndex(currentIndex);
				
				pr.println("<li><a href=\"#"+currentIndex+"\">"+EscapeHTML.escapeHTML(block.getText())+"</a></li>");
				
				currentIndex++;
				
			}
		}
		pr.println("</ul></div>");
		
		
		
		// Write out the HTML from the blocks
		for (int b=0;b<blocks.size();b++) {
			pr.println("<div class=\"vistoryblock\">");
			pr.println(blocks.elementAt(b).getHTML());
			pr.println("</div>");
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
