/**
 * Copyright 2009-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataParsers;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.Cancellable;

/**
 * Represents a generic data parser for read data.  Actual data parsers for
 * specific formats will be subclasses of this.
 */
public abstract class DataParser implements Runnable, Cancellable {

	private final ArrayList<ProgressListener> listeners;
	private File [] files;
	protected final DataCollection collection;
	protected boolean cancel = false;

	
	/**
	 * Instantiates a new data parser.
	 * 
	 * @param collection The dataCollection to which the new data will be added
	 */
	public DataParser (DataCollection collection) {
		this.collection = collection;
		listeners = new ArrayList<ProgressListener>();
	}
	
	/**
	 * Sets a flag which tells the data parser that the user wants to
	 * cancel this request.  It's up to the implementing class to notice
	 * that this flag has been set. 
	 */
	public void cancel () {
		cancel = true;
	}
	
	/**
	 * Gets an options panel.
	 * 
	 * @return The options panel
	 */
	abstract public JPanel getOptionsPanel ();
		
	/**
	 * Checks whether this parser has an options panel
	 * 
	 * @return true, if there is an options panel
	 */
	abstract public boolean hasOptionsPanel ();
	
	/**
	 * Checks if all options have been set to allow the parser to be run
	 * 
	 * @return true, if the parser is ready to go
	 */
	abstract public boolean readyToParse ();

	/**
	 * A short name for the parser
	 * 
	 * @return A name for the parser
	 */
	abstract public String name ();
	
	/**
	 * A longer description which details what data this parser can read
	 * 
	 * @return A description
	 */
	abstract public String description ();
	
	/**
	 * Sets the files which are to be parsed
	 * 
	 * @param files A list of files to parse
	 */
	public void setFiles (File [] files) {
		this.files = files;
	}
	
	/**
	 * Gets the list of files to be parsed
	 * 
	 * @return A list of files to parse
	 */
	protected File [] getFiles () {
		return files;
	}
		
	/**
	 * Gets a file filter which will identify all files which could be
	 * read by this parser.  This is judged solely on the filename so
	 * false positives are OK.  We should ensure that directories are
	 * always allowed when overriding this method.
	 * 
	 * @return A file filter for files which are parsable by this class
	 */
	public FileFilter getFileFilter () {
		return new FileFilter() {
			public boolean accept(File pathname) {
				return true;
			}

			public String getDescription() {
				return "All Files";
			}
		};
	}
	
	/**
	 * Data collection.
	 * 
	 * @return The data collection
	 */
	public DataCollection dataCollection () {
		return collection;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return name();
	}
	
	/**
	 * Parses the data.
	 * 
	 * @throws SeqMonkException
	 */
	public void parseData () throws SeqMonkException {

		if (! readyToParse()) {
			throw new SeqMonkException("Data Parser is not ready to parse (some options may not have been set)");
		}
				
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * Adds a progress listener.
	 * 
	 * @param l The listener to add
	 */
	public void addProgressListener (ProgressListener l) {
		if (l == null) {
			throw new NullPointerException("DataParserListener can't be null");
		}
		
		if (! listeners.contains(l)) {
			listeners.add(l);
		}
	}

	
	/**
	 * Removes a progress listener.
	 * 
	 * @param l The listener to remove
	 */
	public void removeProgressListener (ProgressListener l) {		
		if (l !=null && listeners.contains(l)) {
			listeners.remove(l);
		}
	}
	
	/**
	 * Alerts all listeners to a progress update
	 * 
	 * @param message The message to send
	 * @param current The current level of progress
	 * @param max The level of progress at completion
	 */
	protected void progressUpdated(String message, int current, int max) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressUpdated(message, current, max);
		}
	}
	
	/**
	 * Alerts all listeners that an exception was received. The
	 * parser is not expected to continue after issuing this call.
	 * 
	 * @param e The exception
	 */
	protected void progressExceptionReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressExceptionReceived(e);
		}
	}
	
	/**
	 * Alerts all listeners that a warning was received.  The parser
	 * is expected to continue after issuing this call.
	 * 
	 * @param e The warning exception received
	 */
	protected void progressWarningReceived (Exception e) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressWarningReceived(e);
		}
	}
	
	/**
	 * Alerts all listeners that the user cancelled this import.
	 * 
	 */
	protected void progressCancelled () {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressCancelled();
		}
	}
	
	/**
	 * Tells all listeners that the parser has finished parsing the data
	 * The list of dataSets should be the same length as the original file list.
	 * 
	 * @param newData An array of completed dataSets.  
	 */
	protected void processingFinished(DataSet [] newData) {
		Iterator<ProgressListener> i = listeners.iterator();
		while (i.hasNext()) {
			i.next().progressComplete("datasets_loaded", newData);
		}
	}
}
