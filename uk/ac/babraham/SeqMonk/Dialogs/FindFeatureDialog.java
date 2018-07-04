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
package uk.ac.babraham.SeqMonk.Dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationTagValue;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.FeatureListViewer.FeatureListViewer;


/**
 * The Class FindFeatureDialog shows a dialog which the user can use to
 * search for any kind of annotation.
 */
public class FindFeatureDialog extends JDialog implements ActionListener, Runnable, Cancellable {

	/** The data collection containing the annotation we want to search **/
	private DataCollection dataCollection;
	
	/** The annotation collection */
	private AnnotationCollection collection;
	
	/** The search. */
	private JTextField search;
	
	/** The search in. */
	private JComboBox searchIn;
	
	/** The feature type. */
	private JComboBox featureType;
	
	/** The search button. */
	private JButton searchButton;
	
	/** The button to save the current hits **/
	private JButton saveAllHitsAsTrackButton;

	/** The button to save the current hits **/
	private JButton saveSelectedHitsAsTrackButton;
	
	/** The last list of hits found **/
	private Feature [] lastHits = new Feature[0];
	
	/** The viewer. */
	private FeatureListViewer viewer = null;
	
	/** The scroll. */
	private JScrollPane scroll = null;
	
	/** The spd. */
	private ProgressDialog spd;
	
	private boolean cancelSearch = false;
	
	
	// We want to open the dialog with the same feature type selected
	// as for the last search they did, so we record this in a static
	// field.
	// We default to searching gene names
	/** The last searched type. */
	private static String lastSearchedType = "gene";
	
	// We also want to remember if they searched just the name id or
	// the whole annotation
	/** The search all. */
	private static String lastSearchTarget = "name";

	/**
	 * Instantiates a new find feature dialog.
	 * 
	 * @param application the application
	 */
	public FindFeatureDialog (DataCollection dataCollection) {
		super(SeqMonkApplication.getInstance(),"Find Feature...");
		setSize(700,350);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		this.dataCollection = dataCollection;
		this.collection = dataCollection.genome().annotationCollection();
		
		getContentPane().setLayout(new BorderLayout());
		
		JPanel choicePanel = new JPanel();
		
		choicePanel.add(new JLabel("Search for "));
		search = new JTextField(15);
		choicePanel.add(search);
		choicePanel.add(new JLabel(" in "));
		searchIn = new JComboBox(new String [] {"name","id","all"});

		// Restore any saved preference from the last search
		if (lastSearchTarget == "all") {
			searchIn.setSelectedIndex(2);
		}
		else if (lastSearchTarget == "id") {
			searchIn.setSelectedIndex(1);
		}

		choicePanel.add(searchIn);
		
		choicePanel.add(new JLabel(" of "));
		
		String [] featureTypes = collection.listAvailableFeatureTypes();
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		model.addElement("all");
		for (int i=0;i<featureTypes.length;i++) {
			model.addElement(featureTypes[i]);
		}
		featureType = new JComboBox(model);
		featureType.setPrototypeDisplayValue("No longer than this please");
		
		// See if we can set a type from a previous search
		if (FindFeatureDialog.lastSearchedType.length() > 0) {
			for (int i=0;i<featureTypes.length;i++) {
				if (featureTypes[i].equals(FindFeatureDialog.lastSearchedType)) {
					featureType.setSelectedIndex(i+1); // We need to add 1 since 'all' comes before everything else
					break;
				}
			}
		}
		
		choicePanel.add(featureType);
		
		choicePanel.add(new JLabel(" features"));
		
		getContentPane().add(choicePanel,BorderLayout.NORTH);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		saveAllHitsAsTrackButton = new JButton("Save All As Annotation Track");
		saveAllHitsAsTrackButton.setActionCommand("save_annotation_all");
		saveAllHitsAsTrackButton.addActionListener(this);
		saveAllHitsAsTrackButton.setEnabled(false);
		buttonPanel.add(saveAllHitsAsTrackButton);

		saveSelectedHitsAsTrackButton = new JButton("Save Selected As Annotation Track");
		saveSelectedHitsAsTrackButton.setActionCommand("save_annotation_selected");
		saveSelectedHitsAsTrackButton.addActionListener(this);
		saveSelectedHitsAsTrackButton.setEnabled(false);
		buttonPanel.add(saveSelectedHitsAsTrackButton);

		
		searchButton = new JButton("Search");
		searchButton.setActionCommand("search");
		searchButton.addActionListener(this);
		getRootPane().setDefaultButton(searchButton);
		buttonPanel.add(searchButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setVisible(true);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		spd = new ProgressDialog("Searching...",this);
		spd.progressUpdated("Starting Search...", 0, 1);
		makeFeatureList();
	}

	public void cancel() {
		cancelSearch = true;
	}

	/**
	 * Make feature list.
	 */
	private void makeFeatureList () {
		
		if (scroll != null) {
			remove(scroll);
			validate();
		}
		Vector<Feature> hits = new Vector<Feature>();
		String query = search.getText().toLowerCase().trim();

		lastSearchTarget = (String)searchIn.getSelectedItem();
				
		String [] types;
		
		if (featureType.getSelectedItem().equals("all")) {
			types= collection.listAvailableFeatureTypes();
		}
		else {
			types = new String [] {(String)featureType.getSelectedItem()};
		}
		
		// Remember the type of feature so we use the same one next time
		FindFeatureDialog.lastSearchedType = (String)featureType.getSelectedItem();

		for (int j=0;j<types.length;j++) {
			Feature [] f = collection.getFeaturesForType(types[j]);
			for (int k=0;k<f.length;k++) {
				if (cancelSearch) {
					spd.progressCancelled();
					cancelSearch = false;
					return;
				}
				
				spd.progressUpdated("Searching...", (j*f.length)+k, types.length*f.length);
				
				if (lastSearchTarget == "name") {
					if (f[k].name().toLowerCase().indexOf(query)>=0) {
						hits.add(f[k]);
						continue;
					}
				}
				else if (lastSearchTarget == "id") {
					if (f[k].id().toLowerCase().indexOf(query)>=0) {
						hits.add(f[k]);
						continue;
					}
				}
				else if (lastSearchTarget == "all") {
					if (f[k].getAllAnnotation().toLowerCase().indexOf(query)>=0) {
						hits.add(f[k]);
					}						
				}
				else {
					throw new IllegalStateException("Unknown search target: "+lastSearchTarget);
				}
			}
		}
		
		lastHits = hits.toArray(new Feature[0]);
		setTitle("Find features ["+lastHits.length+" hits]");
		saveAllHitsAsTrackButton.setEnabled(lastHits.length>0);
		saveSelectedHitsAsTrackButton.setEnabled(lastHits.length>0);
		
		spd.progressComplete("search_features", lastHits);
		
		if (hits.size()>0) {
			viewer = new FeatureListViewer(lastHits);			
			scroll = new JScrollPane(viewer);
			add(scroll,BorderLayout.CENTER);
			validate();
		}
		else {
			repaint(); // So we aren't left with a corrupted table showing from a previous search
			JOptionPane.showMessageDialog(this,"No hits found","Search results",JOptionPane.INFORMATION_MESSAGE);
		}
		
	}
	
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		if (ae.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("search")) {
			Thread t = new Thread(this);
			t.start();
		}
		else if (ae.getActionCommand().equals("save_annotation_all")) {
			// Find a name for the type of feature they want to create
			String name = (String) JOptionPane.showInputDialog(this, "Feature type", "Make Annotation Track", JOptionPane.QUESTION_MESSAGE,null,null,search.getText()+" "+featureType.getSelectedItem()+" search");

			if (name == null) return;  // They cancelled
			
			// Now we can go ahead and make the new annotation set
			AnnotationSet searchAnnotations = new AnnotationSet(dataCollection.genome(), search.getText()+" "+featureType.getSelectedItem()+" search");
			for (int f=0;f<lastHits.length;f++) {

				Feature feature = new Feature(name, lastHits[f].chromosomeName());
				feature.setLocation(lastHits[f].location());
				
				AnnotationTagValue [] tags = lastHits[f].getAnnotationTagValues();
				for (int t=0;t<tags.length;t++) {
					feature.addAttribute(tags[t].tag(), tags[t].value());
				}
				searchAnnotations.addFeature(feature);
			}
			
			dataCollection.genome().annotationCollection().addAnnotationSets(new AnnotationSet [] {searchAnnotations});
			
		}

		else if (ae.getActionCommand().equals("save_annotation_selected")) {

			Feature [] selectedHits = viewer.getSelectedFeatures();
			
			if (selectedHits.length == 0) {
				JOptionPane.showMessageDialog(this,"There are no selected features from which to make a track","Can't make track",JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			
			// Find a name for the type of feature they want to create
			String name = (String) JOptionPane.showInputDialog(this, "Feature type", "Make Annotation Track", JOptionPane.QUESTION_MESSAGE,null,null,"selected "+search.getText());

			if (name == null) return;  // They cancelled
			
			// Now we can go ahead and make the new annotation set
			AnnotationSet searchAnnotations = new AnnotationSet(dataCollection.genome(), search.getText()+" search results");
			for (int f=0;f<selectedHits.length;f++) {

				Feature feature = new Feature(name, selectedHits[f].chromosomeName());
				feature.setLocation(selectedHits[f].location());
				
				AnnotationTagValue [] tags = selectedHits[f].getAnnotationTagValues();
				for (int t=0;t<tags.length;t++) {
					feature.addAttribute(tags[t].tag(), tags[t].value());
				}
				searchAnnotations.addFeature(feature);
			}
			
			dataCollection.genome().annotationCollection().addAnnotationSets(new AnnotationSet [] {searchAnnotations});
			
		}

		
	}
	
		
}
