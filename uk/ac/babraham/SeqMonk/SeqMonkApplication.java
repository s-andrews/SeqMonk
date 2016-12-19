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

package uk.ac.babraham.SeqMonk;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import uk.ac.babraham.SeqMonk.AnnotationParsers.GenomeParser;
import uk.ac.babraham.SeqMonk.DataParsers.DataParser;
import uk.ac.babraham.SeqMonk.DataParsers.SeqMonkParser;
import uk.ac.babraham.SeqMonk.DataTypes.CacheListener;
import uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataGroup;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.ReplicateSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener;
import uk.ac.babraham.SeqMonk.DataWriters.SeqMonkDataWriter;
import uk.ac.babraham.SeqMonk.Dialogs.GenomeSelector;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Dialogs.SeqMonkPreviewPanel;
import uk.ac.babraham.SeqMonk.Dialogs.DataParser.DataParserOptionsDialog;
import uk.ac.babraham.SeqMonk.Dialogs.GotoDialog.GotoDialog;
import uk.ac.babraham.SeqMonk.Displays.StatusPanel;
import uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer.ChromosomePositionScrollBar;
import uk.ac.babraham.SeqMonk.Displays.ChromosomeViewer.ChromosomeViewer;
import uk.ac.babraham.SeqMonk.Displays.DataViewer.DataViewer;
import uk.ac.babraham.SeqMonk.Displays.GenomeViewer.GenomeViewer;
import uk.ac.babraham.SeqMonk.Displays.WelcomePanel.InitialSetupPanel;
import uk.ac.babraham.SeqMonk.Displays.WelcomePanel.WelcomePanel;
import uk.ac.babraham.SeqMonk.Menu.SeqMonkMenu;
import uk.ac.babraham.SeqMonk.Network.GenomeDownloader;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * SeqMonkApplication represents the main SeqMonk GUI window and handles
 * functions which allow different parts of the UI to interact.
 */
public class SeqMonkApplication extends JFrame implements ProgressListener, DataChangeListener, ProbeSetChangeListener, AnnotationCollectionListener {

	private static SeqMonkApplication application;
	
	/** The version of SeqMonk */
	public static final String VERSION = "1.36.1.devel";
	
	private SeqMonkMenu menu;
	
	/** The genome viewer is the panel on the top right **/
	private GenomeViewer genomeViewer = null;
	
	/** The DataViewer is the set of folders shown on the top left **/
	private DataViewer dataViewer = null;
	
	/** The chromosome viewer is the interactive view at the bottom **/
	private ChromosomeViewer chromosomeViewer;

	/** The welcome panel is the status panel shown when the program is first launched **/
	// This needs to be able to access the application so we can't initialise it here.
	private WelcomePanel welcomePanel;
	
	/** This the split pane which separates the chromosome panel from the top panels **/
	private JSplitPane mainPane;
	
	/** This is the split pane which separates the genome and data views **/
	private JSplitPane topPane;
	
	/** This is the small strip at the bottom of the main display **/
	private StatusPanel statusPanel;
	
	/** The data collection is the main data model */
	private DataCollection dataCollection = null;
	
	/** A list of feature names which are currently displayed in the chromosome view */
	private Vector<String> drawnFeatureTypes = new Vector<String>();
	
	/** A list of data stores which are currently displayed in the chromosome view */
	private Vector<DataStore> drawnDataStores = new Vector<DataStore>();
	
	/** The last opened / saved file. */
	private File currentFile = null;
	
	/** Flag to check if anything substantial has changed since the file was last loaded/saved. **/
	private boolean changesWereMade = false;

	/** Flag used only when saving before shutting down (save on exit) **/
	private boolean shuttingDown = false;

	/** Flag used when saving before loading a new file **/
	private File fileToLoad = null;
	
	/** The cache listeners */
	private Vector<CacheListener> cacheListeners = new Vector<CacheListener>();
	
	/**
	 * The main method.
	 * 
	 * @param args [1] The name of a file to load
	 */
	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}

		try {
			
			// This allows us to catch all throwable errors reported
			// in our application.
			
			Thread.setDefaultUncaughtExceptionHandler(new ErrorCatcher());
			
			application = new SeqMonkApplication();
			
			application.setVisible(true);
			

			if (!application.welcomePanel.cacheDirectoryValid()) {			
				new InitialSetupPanel();
				application.welcomePanel.refreshPanel();
			}

			
			if (args.length > 0) {
				File f = new File(args[0]);
				application.loadProject(f);
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	/**
	 * Provides a static way to access the main instance of the SeqMonk
	 * Application so we don't need to keep passing references around
	 * through obscure paths.
	 * 
	 * @return The currently running application instance.
	 */
	
	public static SeqMonkApplication getInstance () {
		return application;
	}
	

	/**
	 * Instantiates a new SeqMonk application.
	 */
	private SeqMonkApplication (){
		
		setTitle("SeqMonk");
		setSize(1280, 720);
//		setSize(800, 600);
		setLocationRelativeTo(null);
		// We maximise the display by default
		setExtendedState(getExtendedState()|MAXIMIZED_BOTH);
		menu = new SeqMonkMenu(this);
		setJMenuBar(menu);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/monk_logo.png")).getImage());
		
		mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		topPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		
		mainPane.setTopComponent(topPane);

		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(menu.toolbarPanel(),BorderLayout.NORTH);
			
		welcomePanel = new WelcomePanel(this);		
		getContentPane().add(welcomePanel,BorderLayout.CENTER);
		
		statusPanel = new StatusPanel();
		
		getContentPane().add(statusPanel,BorderLayout.SOUTH);
		
		mainPane.setDividerLocation((double)0.25);
		topPane.setDividerLocation((double)0.25);
		

		
	}
		
	/**
	 * Sets a flag which causes the UI to prompt the user to save when closing
	 * the program.
	 */
	private void changesWereMade () {
		changesWereMade = true;
		if (!getTitle().endsWith("*")) {
			setTitle(getTitle()+"*");
		}
	}
	
	/**
	 * Unsets the changesWereMade flag so that the user will not be prompted
	 * to save even if the data has changed.
	 */
	public void resetChangesWereMade () {
		changesWereMade = false;
		if (getTitle().endsWith("*")) {
			setTitle(getTitle().replaceAll("\\*$", ""));
		}
	}
	
	public void cacheFolderChecked () {
		menu.cacheFolderChecked();
	}
	
	
	/* (non-Javadoc)
	 * @see java.awt.Window#dispose()
	 */
	public void dispose () {
		// We're overriding this so we can catch the application being
		// closed by the X in the corner.  We need to offer the opportunity
		// to save if they've changed anything.

		// We'll already have been made invisible by this stage, so make
		// us visible again in case we're hanging around.
		setVisible(true);
		
		// Check to see if the user has made any changes they might
		// want to save
		if (changesWereMade) {
			int answer = JOptionPane.showOptionDialog(this,"You have made changes which were not saved.  Do you want to save before exiting?","Save before exit?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Save and Exit","Exit without Saving","Cancel"},"Save");

			switch (answer){
			case 0: 
				shuttingDown = true;
				saveProject();
				return;
			case 1:
				break;
			case 2:
				return;
			}
		}

		setVisible(false);
		super.dispose();
		System.exit(0);
		
	}
		
	/**
	 * Launches the genome selector to begin a new project.
	 */
	public void startNewProject () {
		new GenomeSelector(this);
	}
	
	/**
	 * Clears all stored data and blanks the UI.
	 */
	public void wipeAllData () {
		
		currentFile = null;
		setTitle("SeqMonk");
		topPane.setRightComponent(null);
		topPane.setLeftComponent(null);
		mainPane.setBottomComponent(null);
		genomeViewer = null;
		dataViewer = null;
		chromosomeViewer = null;
		dataCollection = null;
		drawnFeatureTypes = new Vector<String>();
		drawnDataStores = new Vector<DataStore>();
		menu.resetMenus();
		DisplayPreferences.getInstance().reset();
	}
	
	/**
	 * Begins the import of new SequenceRead data
	 * 
	 * @param parser A DataParser which will actually do the importing.
	 */
	public void importData (DataParser parser) {
		parser.addProgressListener(this);
		
		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getDataLocation());
		chooser.setMultiSelectionEnabled(true);
		FileFilter filter = parser.getFileFilter();
		
		if (filter != null) {
			chooser.setFileFilter(parser.getFileFilter());
		
			int result = chooser.showOpenDialog(this);
		
			/*
			 * There seems to be a bug in the file chooser which allows the user to
			 * select no files, but not cancel if the control+double click on a file
			 */
			if (result == JFileChooser.CANCEL_OPTION || chooser.getSelectedFile() == null) {
				return;
			}

			SeqMonkPreferences.getInstance().setLastUsedDataLocation(chooser.getSelectedFile());
		
			parser.setFiles(chooser.getSelectedFiles());
		}
		
		// See if we need to display any options
		if (parser.hasOptionsPanel()) {
			DataParserOptionsDialog optionsDialog = new DataParserOptionsDialog(parser);
			optionsDialog.setLocationRelativeTo(this);
			boolean goAhead = optionsDialog.view();
			
			if (! goAhead) {
				return;
			}
		}

		ProgressDialog pd = new ProgressDialog(this,"Loading data...",parser);
		parser.addProgressListener(pd);
		
		try {
			parser.parseData();
		}
		catch (SeqMonkException ex) {
			throw new IllegalStateException(ex);
		}
	}
		
	/**
	 * Launches a FileChooser to allow the user to select a new file name under which to save
	 */
	public void saveProjectAs() {
		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setMultiSelectionEnabled(false);
		chooser.setFileFilter(new FileFilter() {
		
			public String getDescription() {
				return "SeqMonk files";
			}
		
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".smk")) {
					return true;
				}
				else {
					return false;
				}
			}
		
		});
		
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) return;

		File file = chooser.getSelectedFile();
		if (! file.getPath().toLowerCase().endsWith(".smk")) {
			file = new File(file.getPath()+".smk");
		}

		// Check if we're stepping on anyone's toes...
		if (file.exists()) {
			int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

			if (answer > 0) {
				saveProjectAs(); // Let them try again
				return;
			}
		}
		
		currentFile = file;
		saveProject(file);
	}
	
	
	
	
	/**
	 * Saves the current project under the same name as it was loaded.  If
	 * no file is associated with the project will call saveProjectAs
	 */
	public void saveProject () {
		if (currentFile == null) {
			saveProjectAs();
		}
		else {
			saveProject(currentFile);
		}
	}
	
	/**
	 * Saves the current project into the specified file.
	 * 
	 * @param file The file into which the project will be saved
	 */
	public void saveProject (File file) {
		
		SeqMonkDataWriter writer = new SeqMonkDataWriter();
		
		writer.addProgressListener(new ProgressDialog(this,"Saving Project...",writer));
		writer.addProgressListener(this);
		
		writer.writeData(this,file);
		
		setTitle("SeqMonk ["+file.getName()+"]");
		SeqMonkPreferences.getInstance().addRecentlyOpenedFile(file.getAbsolutePath());
	}
	
	/**
	 * Launches a FileChooser to select a project file to open
	 */
	public void loadProject () {
		JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
		chooser.setMultiSelectionEnabled(false);
		SeqMonkPreviewPanel previewPanel = new SeqMonkPreviewPanel();
		chooser.setAccessory(previewPanel);
		chooser.addPropertyChangeListener(previewPanel);
		chooser.setFileFilter(new FileFilter() {
		
			public String getDescription() {
				return "SeqMonk files";
			}
		
			public boolean accept(File f) {
				if (f.isDirectory() || f.getName().toLowerCase().endsWith(".smk")) {
					return true;
				}
				else {
					return false;
				}
			}
		
		});
		
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.CANCEL_OPTION) return;

		File file = chooser.getSelectedFile();
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
	
		loadProject(file);
	}
	
	public String projectName () {
		if (currentFile != null) {
			return currentFile.getName();
		}
		else {
			return "[No project name yet]";
		}
	}
	

	/**
	 * Loads an existing project from a file.  Will wipe all existing data and prompt to
	 * save if the currently loaded project has changed.
	 * 
	 * @param file The file to load
	 */
	public void loadProject (File file) {

		if (file == null) return;
		
		/*
		 * Before we wipe all of the data we need to check to see if
		 * we need to save the existing project.
		 */
		
		if (changesWereMade) {
			int answer = JOptionPane.showOptionDialog(this,"You have made changes which were not saved.  Do you want to save before exiting?","Save before loading new data?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Save before Loading","Load without Saving","Cancel"},"Save");

			switch (answer){
			case 0:
				fileToLoad = file;
				saveProject();
				return;
			case 1:
				break;
			case 2:
				return;
			}
		}
		
		SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
		
		wipeAllData();
		
		currentFile = file;
		
		SeqMonkParser parser = new SeqMonkParser(this);
		
		parser.addProgressListener(this);
		
		ProgressDialog dppd = new ProgressDialog(this,"Loading data...");
		parser.addProgressListener(dppd);
		parser.parseFile(file);
		dppd.requestFocus();
		setTitle("SeqMonk ["+file.getName()+"]");
		
		SeqMonkPreferences.getInstance().addRecentlyOpenedFile(file.getAbsolutePath());

	}

	
	/**
	 * This method is usually called from data gathered by the genome selector
	 * which will provide the required values for the assembly name.  This does
	 * not actually load the specified genome, but just downloads it from the
	 * online genome repository.
	 * 
	 * @param species Species name
	 * @param assembly Assembly name
	 * @param size The size of the compressed genome file in bytes
	 */
	public void downloadGenome (String species, String assembly, int size) {
		GenomeDownloader d = new GenomeDownloader();
		d.addProgressListener(this);
		ProgressDialog pd = new ProgressDialog(this,"Downloading genome...");
		d.addProgressListener(pd);
		d.downloadGenome(species,assembly,size,true);
		pd.requestFocus();
	}
	
	/**
	 * Loads a genome assembly.  This will fail if the genome isn't currently
	 * in the local cache and downloadGenome should be set first in this case.
	 * 
	 * @param baseLocation The folder containing the requested genome.
	 */
	public void loadGenome (File [] baseLocations) {
		GotoDialog.clearRecentLocations();
		GenomeParser parser = new GenomeParser();
		ProgressDialog pd = new ProgressDialog(this,"Loading genome...");
		parser.addProgressListener(pd);
		parser.addProgressListener(this);
		parser.parseGenome(baseLocations);
		pd.requestFocus();
	}
	


	/**
	 * Adds a loaded genome to the main display
	 * 
	 * @param g The Genome which has just been loaded.
	 */
	private void addNewLoadedGenome(Genome g) {
		
		// We've had a trace where the imported genome contained no
		// chromosomes.  No idea how that happened but we can check that
		// here.
		if (g.getAllChromosomes() == null || g.getAllChromosomes().length == 0) {
			JOptionPane.showMessageDialog(this, "No data was present in the imported genome", "Genome import error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		dataCollection = new DataCollection(g);
		dataCollection.addDataChangeListener(this);
		dataCollection.genome().annotationCollection().addAnnotationCollectionListener(this);
		drawnFeatureTypes.clear();
		
		// By default we'll show gene, mRNA and CDS if they're available.  If none
		// of them is then we'll show everything.
		String [] t = g.annotationCollection().listAvailableFeatureTypes();
		
		String geneString = null;
		String mRNAString = null;
		String cdsString = null;
		
		
		for (int i=0;i<t.length;i++) {			
			if (t[i].toLowerCase().equals("gene")) {
				geneString = t[i];
			}
			else if (t[i].toLowerCase().equals("cds")) {
				cdsString = t[i];
			}
			else if (t[i].toLowerCase().equals("mrna")) {
				mRNAString = t[i];
			}
		}
		
		// If we can't find any of the common feature types then
		// show everything by default.
		if (geneString == null && mRNAString == null && cdsString == null) {
			for (int i=0;i<t.length;i++) {			
				drawnFeatureTypes.add(t[i]);
			}
		}
		else {
			if (geneString != null) {
				drawnFeatureTypes.add(geneString);
			}
			if (mRNAString != null) {
				drawnFeatureTypes.add(mRNAString);
			}
			if (cdsString != null) {
				drawnFeatureTypes.add(cdsString);
			}
		}
		
		// We need to get rid of the welcome panel if that's still showing
		// and replace it with the proper SeqMonk display.
		remove(welcomePanel);
		remove(mainPane);
		add(mainPane,BorderLayout.CENTER);
		
		genomeViewer = new GenomeViewer(dataCollection.genome(),this);
		DisplayPreferences.getInstance().addListener(genomeViewer);
		dataCollection.addDataChangeListener(genomeViewer);
		topPane.setRightComponent(genomeViewer);
		dataViewer = new DataViewer(this);
		topPane.setLeftComponent(new JScrollPane(dataViewer));
		
		validate();
		
		chromosomeViewer = new ChromosomeViewer(this, dataCollection.genome().getAllChromosomes()[0]);
		DisplayPreferences.getInstance().addListener(chromosomeViewer);
		dataCollection.addDataChangeListener(chromosomeViewer);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(chromosomeViewer,BorderLayout.CENTER);
		bottomPanel.add(new ChromosomePositionScrollBar(),BorderLayout.SOUTH);
		
		mainPane.setBottomComponent(bottomPanel);
		
		validate();

		DisplayPreferences.getInstance().setChromosome(dataCollection.genome().getAllChromosomes()[0]);
		menu.genomeLoaded();
		
	}
	// End of the GenomeProgressListener methods.
	
	/**
	 * Chromosome viewer.
	 * 
	 * @return the Chromosome viewer
	 */
	public ChromosomeViewer chromosomeViewer () {
		return chromosomeViewer;
	}
	
	public GenomeViewer genomeViewer () {
		return genomeViewer;
	}
	
			
	/**
	 * Checks to see if a specified dataStore is currently being displayed
	 * 
	 * @param d The dataStore to check
	 * @return Is this dataStore currently visible?
	 */
	public boolean dataStoreIsDrawn (DataStore d) {
		if (drawnDataStores.contains(d)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Adds a set of dataStores to the set of currently visible data stores in the
	 * chromosome view.  If any data store is already visible it won't be
	 * added again.
	 * 
	 * @param d An array of dataStores to add
	 */
	public void addToDrawnDataStores (DataStore [] d) {
		// Remember that we changed something
		changesWereMade();
		for (int i=0;i<d.length;i++) {
			if (d[i] != null && ! drawnDataStores.contains(d[i])) {
				drawnDataStores.add(d[i]);
			}			
		}
		chromosomeViewer.tracksUpdated();
	}
		
	/**
	 * Removes a dataStore from the chromosome view
	 * 
	 * @param d The dataStore to remove
	 */
	public void removeFromDrawnDataStores (DataStore d) {
		removeFromDrawnDataStores(new DataStore [] {d});
	}

	/**
	 * Removes several dataStores from the chromosome view
	 * 
	 * @param d The dataStores to remove
	 */
	public void removeFromDrawnDataStores (DataStore [] stores) {
		// Remember that we changed something
		changesWereMade();
		for (int d=0;d<stores.length;d++) {
			if (drawnDataStores.contains(stores[d])) {
				drawnDataStores.remove(stores[d]);
			}
		}
		chromosomeViewer.tracksUpdated();
	}

	
	/**
	 * Replaces the current set of drawn datastores with a new list
	 * 
	 * @param d The set of dataStores to display
	 */
	public void setDrawnDataStores (DataStore [] d) {
		// Remember that we changed something
		changesWereMade();
		
		drawnDataStores.removeAllElements();
		for (int i=0;i<d.length;i++) {
			drawnDataStores.add(d[i]);
		}
		chromosomeViewer.tracksUpdated();
	}
	
	/**
	 * Adds a cache listener.
	 * 
	 * @param l the l
	 */
	public void addCacheListener (CacheListener l) {
		if (l != null && ! cacheListeners.contains(l)) {
			cacheListeners.add(l);
		}
	}
	
	/**
	 * Removes a cache listener.
	 * 
	 * @param l the l
	 */
	public void removeCacheListener (CacheListener l) {
		if (l != null && cacheListeners.contains(l)) {
			cacheListeners.remove(l);
		}
	}
	
	/**
	 * Notifies all listeners that the disk cache was used.
	 */
	public void cacheUsed () {
		Enumeration<CacheListener>en = cacheListeners.elements();
		while (en.hasMoreElements()) {
			en.nextElement().cacheUsed();
		}
	}
	
	
	/**
	 * Sets the text in the status bar at the bottom of the main window
	 * 
	 * @param text The text to display in the status bar
	 */
	public void setStatusText (String text) {
		statusPanel.setText(text);
	}
		
	/**
	 * Drawn feature types.
	 * 
	 * @return An array of feature names for the tracks currently shown in the chromosome viewer.
	 */
	public String [] drawnFeatureTypes() {
		return drawnFeatureTypes.toArray(new String[0]);
	}
	
	/**
	 * Sets the annotation tracks shown in the chromosome view
	 * 
	 * @param features A list of feature type names to display.
	 */
	public void setDrawnFeatureTypes (String [] features) {
		drawnFeatureTypes.removeAllElements();
		for (int i=0;i<features.length;i++) {
			drawnFeatureTypes.add(features[i]);
		}
		chromosomeViewer.tracksUpdated();
	}
	
	/**
	 * Provides a list of the dataStores currently being displayed in the chromosome view
	 * 
	 * @return An array containg the dataStores currently being displayed.
	 */
	public DataStore [] drawnDataStores () {
		return drawnDataStores.toArray(new DataStore[0]);
	}
	
	/**
	 * This method is similar to drawnDataStores in that it returns the currently visible
	 * data stores, but the difference is that if the user has opted to expand replicate
	 * sets in their display preferences then the individual component data stores will be
	 * returned instead of the replicate sets themselves.
	 * @return
	 */
	public DataStore [] drawnDataSets () {
		
		if (DisplayPreferences.getInstance().getReplicateSetExpansion() == DisplayPreferences.REPLICATE_SETS_COMPRESSED) {
			return drawnDataStores();
		}
		
		else {
			Vector<DataStore> actuallyVisibleStores = new Vector<DataStore>();
			
			for (int i=0;i<drawnDataStores.size();i++) {
				if (drawnDataStores.elementAt(i) instanceof ReplicateSet) {
					DataStore [] subStores = ((ReplicateSet)drawnDataStores.elementAt(i)).dataStores();
					for (int s=0;s<subStores.length;s++) {
						actuallyVisibleStores.add(subStores[s]);
					}
				}
				else {
					actuallyVisibleStores.add(drawnDataStores.elementAt(i));
				}
			}
			
			return actuallyVisibleStores.toArray(new DataStore[0]);
		}
		
		
	}
				
	/**
	 * Data collection.
	 * 
	 * @return The currently used data collection.
	 */
	public DataCollection dataCollection () {
		return dataCollection;
	}


	/**
	 * Adds new dataSets to the existing dataCollection and adds them
	 * to the main chromsome view
	 * 
	 * @param newData The new dataSets to add
	 */
	private void addNewDataSets (DataSet [] newData) {
		// We need to add the data to the data collection
		
		ArrayList<DataStore> storesToAdd = new ArrayList<DataStore>();
		
		for (int i=0;i<newData.length;i++) {
			if (newData[i].getTotalReadCount() > 0) {
				//TODO: Can we leave this out as this should be handled by the data collection listener?
				dataCollection.addDataSet(newData[i]);
				storesToAdd.add(newData[i]);
			}
		}
		
		if (dataCollection.getAllDataSets().length>0)
			menu.dataLoaded();
		
		addToDrawnDataStores(storesToAdd.toArray(new DataStore[0]));
		
	}
	// End of DataProgressListener
				

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupAdded(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupAdded(DataGroup g) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupsRemoved(DataGroup [] g) {
		removeFromDrawnDataStores(g);
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupRenamed(DataGroup g) {
		chromosomeViewer.repaint();
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataGroupSamplesChanged(uk.ac.babraham.SeqMonk.DataTypes.DataGroup)
	 */
	public void dataGroupSamplesChanged(DataGroup g) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRenamed(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetRenamed(DataSet d) {
		chromosomeViewer.repaint();
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetAdded(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetAdded(DataSet d) {
		changesWereMade();
	}	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#dataSetRemoved(uk.ac.babraham.SeqMonk.DataTypes.DataSet)
	 */
	public void dataSetsRemoved(DataSet [] d) {
		removeFromDrawnDataStores(d);
		changesWereMade();
	}

	public void replicateSetAdded(ReplicateSet r) {
		changesWereMade();
	}

	public void replicateSetsRemoved(ReplicateSet [] r) {
		removeFromDrawnDataStores(r);
		changesWereMade();
	}

	public void replicateSetRenamed(ReplicateSet r) {
		chromosomeViewer.repaint();
		changesWereMade();
	}

	public void replicateSetStoresChanged(ReplicateSet r) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListAdded(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListAdded(ProbeList l) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListRemoved(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListRemoved(ProbeList l) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.DataChangeListener#probeSetReplaced(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet)
	 */
	public void probeSetReplaced(ProbeSet probes) {
		probes.addProbeSetChangeListener(this);
		changesWereMade();
	}



	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSetChangeListener#probeListRenamed(uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList)
	 */
	public void probeListRenamed(ProbeList l) {
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressComplete(java.lang.String, java.lang.Object)
	 */
	public void progressComplete(String command, Object result) {

		// Many different operations can call this method and our actions
		// depend on who called us and what they sent.
		
		if (command == null) return;
		
		if (command.equals("load_genome")) {
			addNewLoadedGenome((Genome)result);
		}
		else if (command.equals("genome_downloaded")) {
			// No result is returned
			startNewProject();
		}
		else if (command.equals("datasets_loaded")) {
			addNewDataSets((DataSet [])result);
			changesWereMade();
		}
		else if (command.equals("data_written")) {
			// Since we've just saved we can reset the changes flag
			resetChangesWereMade();
			
			// We might have been called by a previous shutdown
			// operation, in which case we need to send them
			// back to shut down.
			if (shuttingDown) {
				shuttingDown = false;
				dispose();
			}
			
			// We might have been called by a previous load operation
			// in which case we need to resume this load
			if (fileToLoad != null) {
				loadProject(fileToLoad);
				fileToLoad = null;
			}
		}
		else if (command.equals("data_quantitation")) {
			// At this point a repaint isn't sufficient - we need to do
			// a tracks updated so the new probeset is recognised.
			chromosomeViewer.tracksUpdated();
			chromosomeViewer.autoScale();
			genomeViewer.repaint();
			changesWereMade();
		}
		
		else if (command.equals("pipeline_quantitation")) {
			// At this point a repaint isn't sufficient - we need to do
			// a tracks updated so the new probeset is recognised.
			chromosomeViewer.tracksUpdated();
			chromosomeViewer.autoScale();
			genomeViewer.repaint();
			changesWereMade();
		}
		
		else {
			throw new IllegalArgumentException("Don't know how to handle progress command '"+command+"'");
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressExceptionReceived(java.lang.Exception)
	 */
	public void progressExceptionReceived(Exception e) {
		// Should be handled by specialised widgets
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressUpdated(java.lang.String, int, int)
	 */
	public void progressUpdated(String message, int current, int max) {
		// Should be handled by specialised widgets		
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressCancelled()
	 */
	public void progressCancelled () {
		// Should be handled by specialised widgets
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.ProgressListener#progressWarningReceived(java.lang.Exception)
	 */
	public void progressWarningReceived(Exception e) {
		// Should be handled by specialised widgets
	}



	// These methods come from the annotation collection listener
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetAdded(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetsAdded(AnnotationSet [] annotationSets) {		

		// If these annotation sets contains 3 or fewer feature types then add them immediately
		// to the annotation tracks
		
		HashSet<String> newFeatureList = new HashSet<String>();
		
		for (int i=0;i<annotationSets.length;i++) {
			String [] newFeatures = annotationSets[i].getAvailableFeatureTypes();
			for (int j=0;j<newFeatures.length;j++) {
				newFeatureList.add(newFeatures[j]);
			}
			
		}
		
		if (newFeatureList.size() <=3) {
			Vector<String>newDrawnFeatures = new Vector<String>();
			Enumeration<String> e = drawnFeatureTypes.elements();
			while (e.hasMoreElements()) {
				newDrawnFeatures.add(e.nextElement());
			}

			Iterator<String> newFeatureIterator = newFeatureList.iterator();
			while (newFeatureIterator.hasNext()) {
				String name = newFeatureIterator.next();
				if (!newDrawnFeatures.contains(name)) {
					newDrawnFeatures.add(name);
				}
			}
			
			setDrawnFeatureTypes(newDrawnFeatures.toArray(new String[0]));
			
		}
	
		// We refresh the view in any case, since we may have added new
		// features to an existing track.
		
		chromosomeViewer.tracksUpdated();
		changesWereMade();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetRemoved(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetRemoved(AnnotationSet annotationSet) {
		// Check the list of drawn feature types to see if they're all still valid
		
		// This is tricky as the way this works the annotation set hasn't yet been deleted
		// from the annotation collection when we get this signal.
		
		AnnotationSet [] sets = dataCollection.genome().annotationCollection().anotationSets();
		
		Vector<String>newFeatureTypes = new Vector<String>();
		Enumeration<String>e = drawnFeatureTypes.elements();
		while (e.hasMoreElements()) {
			String type = e.nextElement();
			for (int i=0;i<sets.length;i++) {
				if (sets[i]==annotationSet) continue;
				if (sets[i].hasDataForType(type)) {
					newFeatureTypes.add(type);
					break;
				}
			}
		}
		
		setDrawnFeatureTypes(newFeatureTypes.toArray(new String[0]));
		changesWereMade();
	}
	
	public void annotationFeaturesRenamed (AnnotationSet annotationSet, String newName) {
		// We have to treat this the same as if a set had been removed in that any
		// of the existing feature tracks could be affected.  We assume that they want to 
		// put the name

		AnnotationSet [] sets = dataCollection.genome().annotationCollection().anotationSets();
		
		Vector<String>newFeatureTypes = new Vector<String>();
		Enumeration<String>e = drawnFeatureTypes.elements();
		while (e.hasMoreElements()) {
			String type = e.nextElement();
			for (int i=0;i<sets.length;i++) {
				if (sets[i].hasDataForType(type)) {
					newFeatureTypes.add(type);
					break;
				}
			}
		}
		
		if (!newFeatureTypes.contains(newName)) {
			newFeatureTypes.add(newName);
		}
		
		setDrawnFeatureTypes(newFeatureTypes.toArray(new String[0]));
		changesWereMade();

		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollectionListener#annotationSetRenamed(uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet)
	 */
	public void annotationSetRenamed(AnnotationSet annotationSet) {
		chromosomeViewer.repaint();
		changesWereMade();
	}

	public void activeDataStoreChanged(DataStore s) {
		// TODO Auto-generated method stub
		
	}

	public void activeProbeListChanged(ProbeList l) {
		// TODO Auto-generated method stub
		
	}

}
