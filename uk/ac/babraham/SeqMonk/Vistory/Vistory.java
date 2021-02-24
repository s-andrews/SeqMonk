/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Vistory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import uk.ac.babraham.SeqMonk.Utilities.EscapeHTML;

public class Vistory {

	/**
	 * This is the main class to hold a vistory.  Each project is only going to have a
	 * single vistory so we're going to construct this as a singleton.
	 */

	private static final int VISTORY_FILE_VERSION = 1;
	
	private static final Vistory vistory = new Vistory();
	private Vector<VistoryListener> listeners = new Vector<VistoryListener>();
	private VistoryBlock selectedBlock = null;
	private boolean needsSaving = false;
	
	private File saveFile = null;

	
	private Vector<VistoryBlock> blocks = new Vector<VistoryBlock>();
	
	private Vistory () {}

	public File saveFile () {
		return saveFile;
	}
		
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
		
		needsSaving = true;
		
		boolean relativeToSelected = block.allowsRelativePosition();
		
		VistoryBlock relativeBlock = null;
		if (relativeToSelected) {
			relativeBlock = selectedBlock;
		}
		else {
			if (blocks.size() > 0) {
				relativeBlock = blocks.elementAt(blocks.size()-1);
			}
		}
		
		if (relativeBlock == null) {
			blocks.add(block);
		}
		else {
			for (int i=0;i<blocks.size();i++) {
				if (blocks.elementAt(i).equals(relativeBlock)) {
					blocks.add(i+1,block);
				}
			}
		}
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blockAdded(block);
		}
	}
	
	public void requestVistoryFocus (VistoryBlock b) {
		// Go through all of the blocks setting the focus to the
		// one we're looking at now.
		
		Iterator<VistoryBlock> it = blocks.iterator();
		
		while (it.hasNext()) {
			VistoryBlock block = it.next();
			
			if (block.equals(b)) {
				block.setVistoryFocus(true);
				selectedBlock = b;
			}
			else {
				block.setVistoryFocus(false);
			}
		}

	}
	
	public void clear () {
		VistoryBlock [] blocksToClear = blocks();
		
		for (int b=0;b<blocksToClear.length;b++) {
			removeBlock(blocksToClear[b]);
		}
	}
	
	public void removeBlock (VistoryBlock block) {
		
		needsSaving = true;
		
		if (! blocks.contains(block))return;
		blocks.remove(block);
		if (block.equals(selectedBlock)) {
			selectedBlock = null;
		}
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blockRemoved(block);
		}
	}

	public boolean needsSaving () {
		return needsSaving;
	}
	
	
	public void raiseBlock (VistoryBlock block) {
		
		needsSaving = true;
		
		if (! blocks.contains(block))return;
		
		int index = blocks.indexOf(block);
		if (index == 0) return;
		
		blocks.set(index, blocks.elementAt(index-1));
		blocks.set(index-1, block);
		
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blocksReordered();
		}
	}

	public void lowerBlock (VistoryBlock block) {
		
		needsSaving = true;
		
		if (! blocks.contains(block))return;
		
		int index = blocks.indexOf(block);
		if (index == blocks.size()-1) return;
		
		blocks.set(index, blocks.elementAt(index+1));
		blocks.set(index+1, block);
		
		Enumeration<VistoryListener> en = listeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().blocksReordered();
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
		
		pr.println("<li class=\"title2\"><a href=\"#top\">Top</a></li>");
		int currentIndex = 1;
		for (int b=0;b<blocks.size();b++) {
			if (blocks.elementAt(b) instanceof VistoryTitle) {
				VistoryTitle block = (VistoryTitle)blocks.elementAt(b);
				block.setIndex(currentIndex);
				
				pr.println("<li class=\"title"+block.level()+"\"><a href=\"#"+currentIndex+"\">"+EscapeHTML.escapeHTML(block.getText())+"</a></li>");
				
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

	
	public void saveToFile (File file) throws IOException {

		PrintWriter pr = new PrintWriter(file);
		
		pr.println("SEQMONK_VISTORY\t"+VISTORY_FILE_VERSION);
		
		for (int b=0;b<blocks.size();b++) {
			// Our format is tab delimited.  The last section is always a specific block which the vistory block
			// needs to know how to deal with and probably contains the text it's going to use.
			// 
			// Section 1 is the type of block
			// Section 2 is the date/time associated with the block

			StringBuffer sb = new StringBuffer();

			sb.append(blocks.elementAt(b).getType());
			sb.append("\t");
			
			sb.append(blocks.elementAt(b).date().getTime());
			sb.append("\t");
			
			sb.append(blocks.elementAt(b).getData());
			
			pr.println(sb.toString());
			
		}
		
		pr.close();
		
		saveFile = file;
		
		needsSaving = false;
	}
	
	public void loadFile (File file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		String line = br.readLine();
		
		// First line is the data version
		if (line == null) throw new IOException("File was empty");

		String [] headers = line.split("\t");
		if (!headers[0].equals("SEQMONK_VISTORY")) {
			throw new IOException("Header doesn't look like a vistory file");
		}
		
		if (Integer.parseInt(headers[1]) > VISTORY_FILE_VERSION) {
			throw new IOException("You need a newer version of SeqMonk to read this file");
		}
				
		while ((line = br.readLine()) != null) {
			String [] sections = line.split("\t");
			String type = sections[0];
			Date date = new Date(Long.parseLong(sections[1]));
			
			if (type.equals("TEXT")) {
				addBlock(new VistoryText(date,sections[sections.length-1]));
			}
			else if (type.startsWith("TITLE")) {
				int level;
				
				if (type.equals("TITLE")) {
					level = 2;
				}
				else {
					level = Integer.parseInt(type.substring(5));
				}
								
				addBlock(new VistoryTitle(level,date,sections[sections.length-1]));
			}
			else if (type.equals("TABLE")) {
				addBlock(new VistoryTable(date,sections[sections.length-1]));
			}
			else if (type.equals("EVENT")) {
				addBlock(new VistoryEvent(date,sections[sections.length-1]));
			}
			else if (type.equals("IMAGE")) {
				addBlock(new VistoryImage(date,sections[sections.length-1]));
			}
			else {
				throw new IllegalStateException("Unknown block type "+type);
			}

		}
		
		br.close();
		
		saveFile = file;
	}

	
//	public static void main (String [] args) {
//		File f = new File("C:/Users/andrewss/Desktop/vistory.html");
//		try {
//			Vistory.getInstance().writeReport(f);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	
}
