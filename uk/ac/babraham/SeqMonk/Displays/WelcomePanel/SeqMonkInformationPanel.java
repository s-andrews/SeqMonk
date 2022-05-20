/**
 * Copyright 2010- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.WelcomePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Calendar;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressRecordDialog;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.Help.HelpDialog;
import uk.ac.babraham.SeqMonk.Network.GenomeUpgrader;
import uk.ac.babraham.SeqMonk.Network.UpdateChecker;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.DownloadableGenomeSet;
import uk.ac.babraham.SeqMonk.Network.DownloadableGenomes.GenomeAssembly;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.R.RProgressListener;
import uk.ac.babraham.SeqMonk.R.RScriptRunner;
import uk.ac.babraham.SeqMonk.R.RVersionTest;
import uk.ac.babraham.SeqMonk.Utilities.TempDirectory;
import uk.ac.babraham.SeqMonk.Utilities.Templates.Template;

/**
 * This panel is displayed when the program first starts.
 * It shows information about the current SeqMonk install
 */
public class SeqMonkInformationPanel extends JPanel implements Runnable, ActionListener {

	/*
	 * We tried to use the standard OptionPanel icons here, but some systems didn't
	 * have sensible icons and used horrible defaults so now we specify our own.
	 * The error icon is a public domain icon from http://www.clker.com/clipart-12247.html
	 * The others are modifications of that icon done by Simon Andrews as part of this
	 * project.  The SVG files for these icons are in the same folder as the loaded
	 * png files.
	 */

	private ImageIcon infoIcon = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/StatusIcons/information.png"));

	/** The error icon. */
	private Icon errorIcon = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/StatusIcons/error.png"));

	/** The warning icon. */
	private Icon warningIcon = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/StatusIcons/warning.png"));

	/** The tick icon. */
	private Icon tickIcon = new ImageIcon(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/Resources/StatusIcons/tick.png"));


	/** The update label. */
	private JLabel programUpdateLabel;

	/** The update label text. */
	private JLabel programUpdateLabelText;

	private JLabel genomeUpdateLabel;
	private JLabel genomeUpdateLabelText;
	private JButton updateGenomesButton;
	private JLabel osxDiskAccessLabel;
	private JLabel osxDiskAccessLabelText;
	private JButton osxDiskAccessButton;
	private JLabel rLabel;
	private JLabel rLabelText;
	private JButton setRLocationButton;
	private JButton installRDependenciesButton;


	private GenomeAssembly [] updates = null;

	private SeqMonkApplication application;

	private boolean invalidCacheDirectory = false;

	/** The two dp. */
	private DecimalFormat twoDP = new DecimalFormat("#.##");

	/**
	 * Instantiates a new seq monk information panel.
	 */
	public SeqMonkInformationPanel (SeqMonkApplication application) {
		this.application = application;
		populatePanel();
		repaint();
	}

	protected void populatePanel () {

		removeAll();
		validate();
		invalidCacheDirectory = false;

		// We prepare a couple of buttons for optional later use
		JButton setTempDirButton = new JButton("Set Cache Directory");
		setTempDirButton.setActionCommand("set_temp_dir");
		setTempDirButton.addActionListener(this);

		JButton removeStaleFilesButton = new JButton("Delete Old Cache Files");
		removeStaleFilesButton.setActionCommand("clean_cache");
		removeStaleFilesButton.addActionListener(this);

		JButton setGenomesFolderButton = new JButton("Set Custom Genomes Folder");
		setGenomesFolderButton.setActionCommand("set_genomes_dir");
		setGenomesFolderButton.addActionListener(this);

		setRLocationButton = new JButton("Detect R");
		setRLocationButton.setActionCommand("set_r");
		setRLocationButton.addActionListener(this);

		installRDependenciesButton = new JButton("Install R Dependencies");
		installRDependenciesButton.setActionCommand("install_r");
		installRDependenciesButton.addActionListener(this);


		updateGenomesButton = new JButton("Update Genomes");
		updateGenomesButton.setActionCommand("update_genomes");
		updateGenomesButton.addActionListener(this);


		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.weightx=0.001;
		gbc.weighty=0.5;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5,5,5,5);

		//First the Memory available
		JLabel memoryLabel = new JLabel(infoIcon);
		add(memoryLabel,gbc);
		gbc.gridx=1;
		gbc.weightx=0.999;
		double memory = ((double)Runtime.getRuntime().maxMemory()) / (1024*1024*1024);
		add(new JLabel(twoDP.format(memory)+" GB of memory is available",JLabel.LEFT),gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx = 0.001;

		//Whether we're running the latest version
		programUpdateLabel = new JLabel(infoIcon);
		add(programUpdateLabel,gbc);
		gbc.gridx=1;
		gbc.weightx=0.999;
		programUpdateLabelText = new JLabel("Checking if a program update is available...",JLabel.LEFT);
		add(programUpdateLabelText,gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx = 0.001;

		// Whether we're using the latest genomes
		genomeUpdateLabel = new JLabel(infoIcon);
		add(genomeUpdateLabel,gbc);
		gbc.gridx=1;
		gbc.weightx=0.999;
		genomeUpdateLabelText = new JLabel("Checking if genome updates are available...",JLabel.LEFT);
		add(genomeUpdateLabelText,gbc);

		gbc.gridx=2;
		gbc.weightx=0.001;
		add(updateGenomesButton,gbc);
		updateGenomesButton.setVisible(false);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx = 0.001;

		// On OSX, whether we have full disk access
		
		// If we're on a mac check for full disk access
		if (System.getProperty("os.name").contains("Mac OS X")) {

			boolean limitedAccess = false;

			// We try to read this file which will fail if we don't have full
			// access.  We might in future expand this to include more fine
			// grained checks for external drives, the desktop etc. but this is
			// infinitely better than before for now.
			try {
				FileReader fr = new FileReader("/Library/Preferences/com.apple.TimeMachine.plist");
				fr.close();
			}
			catch (Exception e) {
				limitedAccess = true;
			}
			
			if (limitedAccess) {
				osxDiskAccessLabel = new JLabel(warningIcon);
				add(osxDiskAccessLabel,gbc);
				gbc.gridx=1;
				gbc.weightx=0.999;
				osxDiskAccessLabelText = new JLabel("Limited disk access. Can't read from some folders / drives",JLabel.LEFT);
				add(osxDiskAccessLabelText,gbc);
	
				osxDiskAccessButton = new JButton("Learn more");
				osxDiskAccessButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						HelpDialog help = new HelpDialog();
						help.DisplayPage("OSX Full Disk Access");
					}
				});
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(osxDiskAccessButton,gbc);
			}
			else {
				osxDiskAccessLabel = new JLabel(tickIcon);
				add(osxDiskAccessLabel,gbc);
				gbc.gridx=1;
				gbc.weightx=0.999;
				osxDiskAccessLabelText = new JLabel("Full disk access enabled",JLabel.LEFT);
				add(osxDiskAccessLabelText,gbc);				
			}
	
			gbc.gridx=0;
			gbc.gridy++;
			gbc.weightx = 0.001;
		}

		
		//Whether we've set a temp directory

		// This will record whether our temp dir is invalid

		File tempDir = SeqMonkPreferences.getInstance().tempDirectory();

		// As a separate step we'll test if the directory is defined but missing, and if
		// so we'll have a go at trying to make it.
		
		if (tempDir != null && !tempDir.exists()) {
			tempDir.mkdirs();
		}
		
		
		if (tempDir == null) {
			JLabel tempLabel = new JLabel(errorIcon);
			add(tempLabel,gbc);
			gbc.gridx=1;
			gbc.weightx=0.999;
			add(new JLabel("Please configure a cache directory to allow SeqMonk to run.",JLabel.LEFT),gbc);
			gbc.gridx=2;
			gbc.weightx=0.001;
			add(setTempDirButton,gbc);
			invalidCacheDirectory = true;
		}
		else if (! (tempDir.exists() && tempDir.isDirectory() && tempDir.canRead() && tempDir.canWrite() && tempDir.listFiles() != null)) {
			JLabel tempLabel = new JLabel(errorIcon);
			add(tempLabel,gbc);
			gbc.gridx=1;
			gbc.weightx=0.999;
			add(new JLabel("Your cache directory is invalid. Please configure a cache directory to allow SeqMonk to run.",JLabel.LEFT),gbc);			
			gbc.gridx=2;
			gbc.weightx=0.001;
			add(setTempDirButton,gbc);
			invalidCacheDirectory = true;
		}
		else {
			// Check if we can actually write something into the cache directory
			// (don't just trust that we can)

			try {
				File tempFile = File.createTempFile("seqmonk_test_data", ".temp", SeqMonkPreferences.getInstance().tempDirectory());
				FileOutputStream fis = new FileOutputStream(tempFile);
				fis.write(123456789);
				fis.close();
				tempFile.delete();
			}
			catch (IOException ioe) {
				// Something failed when trying to use the cache directory
				JLabel tempLabel = new JLabel(errorIcon);
				add(tempLabel,gbc);
				gbc.gridx=1;
				gbc.weightx=0.999;
				add(new JLabel("A test write to your cache directory failed ("+ioe.getLocalizedMessage()+"). Please configure a cache directory to allow SeqMonk to run.",JLabel.LEFT),gbc);			
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setTempDirButton,gbc);
				invalidCacheDirectory = true;
			}

			if (!invalidCacheDirectory) {
				// Check to see if we have stale seqmonk temp files
				File [] tempFiles = tempDir.listFiles();

				int staleFiles = 0;
				for (int f=0;f<tempFiles.length;f++) {
					
					if (Calendar.getInstance().getTimeInMillis() - tempFiles[f].lastModified() < 1000L * 60 * 60 * 12) {
						// We only deal with things which are at least 12 hours old
						continue;
					}
					
					if (tempFiles[f].getName().startsWith("seqmonk") && tempFiles[f].getName().contains("temp")) {
						staleFiles++;
					}
				}
				if (staleFiles > 0) {
					JLabel tempLabel = new JLabel(errorIcon);
					add(tempLabel,gbc);
					gbc.gridx=1;
					gbc.weightx=0.999;
					add(new JLabel("Disk caching is available and enabled - but you have "+staleFiles+" stale temp files",JLabel.LEFT),gbc);
					gbc.gridx=2;
					gbc.weightx=0.001;
					add(removeStaleFilesButton,gbc);
				}
				else {
					JLabel tempLabel = new JLabel(tickIcon);
					add(tempLabel,gbc);
					gbc.gridx=1;
					gbc.weightx=0.999;
					add(new JLabel("Disk caching is available and enabled",JLabel.LEFT),gbc);
				}

				application.cacheFolderChecked();
			}
		}		

		gbc.gridy++;
		gbc.weightx = 0.001;

		//Check the genomes directory

		rLabel = new JLabel(infoIcon);
		rLabelText = new JLabel("Checking R...");

		gbc.gridx=0;
		add(rLabel,gbc);
		gbc.gridx=1;
		gbc.weightx=0.999;
		add(rLabelText,gbc);

		gbc.gridx=2;
		gbc.weightx = 0.001;
		add(installRDependenciesButton,gbc);
		installRDependenciesButton.setVisible(false);
		add(setRLocationButton,gbc);
		setRLocationButton.setVisible(false);
		gbc.gridy++;

		//Check the genomes directory

		JLabel genomesLabel = new JLabel(errorIcon);
		JLabel genomesLabelText = new JLabel("Couldn't check genomes folder");

		if (SeqMonkPreferences.getInstance().customGenomeBaseUsed()) {
			// They're using a custom genomes folder
			File gb = null;
			try {
				gb = SeqMonkPreferences.getInstance().getGenomeBase();
			} 
			catch (FileNotFoundException e) {
				// There is no default genomes folder
				genomesLabel.setIcon(errorIcon);
				genomesLabelText.setText("Can't find your custom genomes folder");
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setGenomesFolderButton,gbc);
			}

			if (!gb.exists()) {
				// There is no default genomes folder
				genomesLabel.setIcon(errorIcon);
				genomesLabelText.setText("Can't find your custom genomes folder");				
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setGenomesFolderButton,gbc);
			}

			else if (!gb.canRead()) {
				// The default genomes folder is present but useless
				genomesLabel.setIcon(errorIcon);
				genomesLabelText.setText("You don't have read permission on your custom genomes folder");
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setGenomesFolderButton,gbc);
			}
			else if (!gb.canWrite()) {
				// The default genomes folder is present, but we can't import new genomes
				genomesLabel.setIcon(warningIcon);
				genomesLabelText.setText("You don't have write permission on your custom genomes folder so new genomes can't be downloaded");
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setGenomesFolderButton,gbc);
			}
			else {
				// Everything is OK
				genomesLabel.setIcon(infoIcon);
				genomesLabelText.setText("Using custom genomes folder");
			}
		}
		else {
			// They're using the default
			File gb = null;
			try {
				gb = SeqMonkPreferences.getInstance().getGenomeBase();

				if (!gb.canRead()) {
					// The default genomes folder is present but useless
					genomesLabel.setIcon(errorIcon);
					genomesLabelText.setText("Using default genomes folder - but don't have read permission");
					gbc.gridx=2;
					gbc.weightx=0.001;
					add(setGenomesFolderButton,gbc);
				}
				else if (!gb.canWrite()) {
					// The default genomes folder is present, but we can't import new genomes
					genomesLabel.setIcon(warningIcon);
					genomesLabelText.setText("Using default genomes folder - no write permission so new genomes can't be downloaded");
					gbc.gridx=2;
					gbc.weightx=0.001;
					add(setGenomesFolderButton,gbc);
				}
				else {
					// Everything is OK
					genomesLabel.setIcon(infoIcon);
					genomesLabelText.setText("Using default genomes folder");
					gbc.gridx=2;
					gbc.weightx=0.001;
					add(setGenomesFolderButton,gbc);
				}

			} 
			catch (FileNotFoundException e) {
				// There is no default genomes folder
				genomesLabel.setIcon(errorIcon);
				genomesLabelText.setText("Using default genomes folder - but this does not exist");
				gbc.gridx=2;
				gbc.weightx=0.001;
				add(setGenomesFolderButton,gbc);
			}


		}

		gbc.gridx=0;
		add(genomesLabel,gbc);
		gbc.gridx=1;
		gbc.weightx=0.999;
		add(genomesLabelText,gbc);

		gbc.gridx=0;
		gbc.gridy++;
		gbc.weightx = 0.001;



		// We can start the update checker if they've allowed us to
		if (SeqMonkPreferences.getInstance().checkForUpdates()) {
			Thread t = new Thread(this);
			t.start();
		}
		else {
			programUpdateLabel.setIcon(warningIcon);
			programUpdateLabelText.setText("Program update checking has been disabled");
			genomeUpdateLabel.setIcon(warningIcon);
			genomeUpdateLabelText.setText("Genome update checking has been disabled");
		}

		validate();
		repaint();

	}

	public boolean cacheDirectoryValid () {
		return !invalidCacheDirectory;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run () {
		
		// Check for an available update to the SeqMonk Program
		try {

			if (UpdateChecker.isUpdateAvailable()) {

				String latestVersion = UpdateChecker.getLatestVersionNumber();

				programUpdateLabel.setIcon(warningIcon);
				programUpdateLabelText.setText("A newer version of SeqMonk (v"+latestVersion+") is available");
			}
			else {
				if (SeqMonkApplication.VERSION.contains("devel")) {
					programUpdateLabel.setIcon(warningIcon);
					programUpdateLabelText.setText("You are running a current development version of SeqMonk");					
				}
				else {
					programUpdateLabel.setIcon(tickIcon);
					programUpdateLabelText.setText("You are running the latest version of SeqMonk");
				}
			}
		}
		catch (SeqMonkException e) {
			programUpdateLabel.setIcon(errorIcon);
			programUpdateLabelText.setText("Failed to check for SeqMonk updates");	
			e.printStackTrace();
		}


		// Check for an available update to any of the installed genomes
		try {

			DownloadableGenomeSet availableGenomes = new DownloadableGenomeSet();

			updates = availableGenomes.findUpdateableGenomes();

			if (updates != null && updates.length > 0) {

				genomeUpdateLabel.setIcon(warningIcon);
				genomeUpdateLabelText.setText("There are updates available for "+updates.length+" of your installed genomes");
				updateGenomesButton.setVisible(true);
			}
			else {
				genomeUpdateLabel.setIcon(tickIcon);
				genomeUpdateLabelText.setText("All of your installed genomes are up to date");

			}
		}
		catch (Exception e) {
			genomeUpdateLabel.setIcon(errorIcon);
			genomeUpdateLabelText.setText("Failed to check for genome updates");
			e.printStackTrace();
		}
		
		// Check the R dependencies
		

		try {
			String rVersion = RVersionTest.testRVersion(SeqMonkPreferences.getInstance().RLocation());
			
			// Do a check to see if the version of R is too old to support a recent
			// bioconductor installation.  All versions before 4.1 are now not viable
			
			if (
					rVersion.startsWith("2") || 
					rVersion.startsWith("3") || 
					rVersion.startsWith("4.0") 				
				) {
				// They're going to have a problem, so let's not encourage this
				rLabel.setIcon(warningIcon);
				rLabelText.setText("Found R version "+rVersion+" but this is too old to use.  Please update to the latest R");
				setRLocationButton.setVisible(true);

				return;
			}
			

			if (!RVersionTest.hasRDependencies()) {
				rLabel.setIcon(warningIcon);
				rLabelText.setText("Found a valid R installation, but package dependencies were missing");
				installRDependenciesButton.setVisible(true);
			}
			else {
				rLabel.setIcon(tickIcon);
				rLabelText.setText("Found valid R version ("+rVersion+") at '"+SeqMonkPreferences.getInstance().RLocation()+"'");

			}
						
		}
		catch (IOException ioe) {
			if (SeqMonkPreferences.getInstance().RLocation().equals("R")) {
				rLabel.setIcon(infoIcon);
				rLabelText.setText("Couldn't find a valid default R installation "+ioe.getMessage());
				setRLocationButton.setVisible(true);
			}
			else {
				rLabel.setIcon(errorIcon);
				rLabelText.setText("Couldn't find a valid R installation at "+SeqMonkPreferences.getInstance().RLocation()+" "+ioe.getMessage());
				setRLocationButton.setVisible(true);
			}

		}



	}

	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("set_temp_dir")) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select a Cache Directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				SeqMonkPreferences.getInstance().setTempDirectory(chooser.getSelectedFile());
				try {
					SeqMonkPreferences.getInstance().savePreferences();
					populatePanel();
				} 
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
			}
		}
		else if (e.getActionCommand().equals("set_genomes_dir")) {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Select a Genomes Directory");
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				SeqMonkPreferences.getInstance().setGenomeBase(chooser.getSelectedFile());
				try {
					SeqMonkPreferences.getInstance().savePreferences();
					populatePanel();
				} 
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
			}
		}
		else if (e.getActionCommand().equals("set_r")) {

			// First try to autodetect the location
			String autoDetect = RVersionTest.autoDetectRLocation();

			if (autoDetect != null) {
				int answer = JOptionPane.showConfirmDialog(this, "Found an R installation at '"+autoDetect+"' use this?","R detected",JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					SeqMonkPreferences.getInstance().setRLocation(autoDetect);
					try {
						SeqMonkPreferences.getInstance().savePreferences();
						populatePanel();
						return;
					} 
					catch (IOException ioe) {
						throw new IllegalStateException(ioe);
					}
				}
			}

			// If we can't auto-detect, or if they don't want to use that, then let them
			// choose where R is.

			JOptionPane.showMessageDialog(this, "Couldn't auto-detect an R installation.  Please manually select the location of the R executable");

			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("Find R executable");
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				SeqMonkPreferences.getInstance().setRLocation(chooser.getSelectedFile().getAbsolutePath());
				try {
					SeqMonkPreferences.getInstance().savePreferences();
					populatePanel();
				} 
				catch (IOException ioe) {
					throw new IllegalStateException(ioe);
				}
			}
		}

		else if (e.getActionCommand().equals("install_r")) {

			// R sucks
			//
			// If the user has R installed as an admin user, but they're running as a normal
			// user then a non-interactive session (such as the ones we use here), won't 
			// put up a prompt to create a local library.
			//
			// To get around this we can create the local library folder within the R script,
			// which is great, except that R checks for the existence of this library at
			// the start of the session, so making it during the session still doesn't allow
			// us to install anything.  We therefore have to run a completely separate script
			// just to make the directory

			// Do the local library script first
			
			
			// Now do the actual package install
			
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {

						File tempDir = TempDirectory.createTempDirectory();

						// Get the template script
						Template template = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/create_local_library.r"));

						// Write the script file
						File scriptFile = new File(tempDir.getAbsoluteFile()+"/script.r");
						PrintWriter pr = new PrintWriter(scriptFile);
						pr.print(template.toString());
						pr.close();			

						RScriptRunner runner = new RScriptRunner(tempDir);
						RProgressListener listener = new RProgressListener(runner);
						runner.addProgressListener(new ProgressRecordDialog("R Session",runner));
						runner.runScript();

						while (true) {
							if (listener.cancelled()) {
								return;
							}
							if (listener.exceptionReceived()) {
								return;
							}
							if (listener.complete()) break;

							Thread.sleep(500);
						}
						runner.cleanUp();

						
						File tempDir2 = TempDirectory.createTempDirectory();

						// Get the template script
						Template template2 = new Template(ClassLoader.getSystemResource("uk/ac/babraham/SeqMonk/load_required_modules.r"));

						// Write the script file
						File scriptFile2 = new File(tempDir2.getAbsoluteFile()+"/script.r");
						PrintWriter pr2 = new PrintWriter(scriptFile2);
						pr2.print(template2.toString());
						pr2.close();			

						RScriptRunner runner2 = new RScriptRunner(tempDir2);
						RProgressListener listener2 = new RProgressListener(runner2);
						runner2.addProgressListener(new ProgressRecordDialog("R Session",runner2));
						runner2.runScript();

						while (true) {
							if (listener2.cancelled()) {
								return;
							}
							if (listener2.exceptionReceived()) {
								return;
							}
							if (listener2.complete()) break;

							Thread.sleep(500);
						}
						runner2.cleanUp();
						populatePanel();
					}
					catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
					
				}
			});
			t.start();
			

		}

		else if (e.getActionCommand().equals("clean_cache")) {
			
			Thread t = new Thread(new Runnable() {
				
				public void run() {
					ProgressDialog pd = new ProgressDialog("Cleaning the cache");
				
					File [] tempFiles = SeqMonkPreferences.getInstance().tempDirectory().listFiles();

					for (int f=0;f<tempFiles.length;f++) {
						
						pd.progressUpdated("Deleting file "+(f+1)+" out of "+tempFiles.length,f,tempFiles.length);
						
						if (Calendar.getInstance().getTimeInMillis() - tempFiles[f].lastModified() < 1000L * 60 * 60 * 12) {
							// We only deal with things which are at least 12 hours old
							continue;
						}
						
						// This might be a simple temp data file
						if (tempFiles[f].isFile() && tempFiles[f].getName().startsWith("seqmonk") && tempFiles[f].getName().endsWith(".temp")) {
							tempFiles[f].delete();
						}
						
						// This might be a temp directory
						else if (tempFiles[f].isDirectory() && tempFiles[f].getName().startsWith("seqmonk") && tempFiles[f].getName().contains("temp")) {
							File [] files = tempFiles[f].listFiles();
							
							for (int g=0;g<files.length;g++) {
								if (!files[g].delete()) {
									throw new IllegalStateException(new IOException("Failed to delete "+files[g].getAbsolutePath()));
								}
							}
							
							// Now remove the directory
							tempFiles[f].delete();

						}
					}
					
					pd.progressComplete("cache_clean", null);
					
					populatePanel();
				}
			});
			
			t.start();
			
			
		}


		else if (e.getActionCommand().equals("update_genomes")) {
			updateGenomesButton.setEnabled(false);
			GenomeUpgrader upgrader = new GenomeUpgrader();
			upgrader.addProgressListener(new ProgressDialog("Updating installed genomes"));
			upgrader.addProgressListener(new ProgressListener() {

				public void progressWarningReceived(Exception e) {}

				public void progressUpdated(String message, int current, int max) {}

				public void progressExceptionReceived(Exception e) {}

				public void progressComplete(String command, Object result) {
					updateGenomesButton.setVisible(false);
					updates = null;
					genomeUpdateLabel.setIcon(tickIcon);
					genomeUpdateLabelText.setText("All of your installed genomes are up to date");
				}

				public void progressCancelled() {}
			});

			upgrader.upgradeGenomes(updates);

		}
		else {
			throw new IllegalArgumentException("Didn't understand action '"+e.getActionCommand()+"'");
		}
	}
}
