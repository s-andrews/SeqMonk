/**
 * Copyright Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.AnnotationSetEditor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.PatternSyntaxException;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.CoreAnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.BEDFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;

/**
 * The Class DataSetEditor allows the editing and deleting data sets
 */
public class AnnotationSetEditor extends JDialog implements ActionListener, ListSelectionListener {
	
	/** The data collection. */
	private DataCollection collection;
	
	/** The data set model. */
	private DefaultListModel annotationSetModel = new DefaultListModel();
	
	/** The data set list. */
	private JList annotationSetList;
	
	/** The delete button. */
	private JButton deleteButton;
	
	/** The rename button. */
	private JButton renameButton;

	/** The replace button. */
	private JButton replaceButton;
	
	/** The export button. */
	private JButton exportButton;
	
	
	/**
	 * Instantiates a new data set editor.
	 * 
	 * @param collection the application
	 */
	public AnnotationSetEditor (DataCollection collection) {
		super(SeqMonkApplication.getInstance(),"Edit Annotation Sets...");
		setSize(300,400);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		this.collection = collection;
		
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.weightx=0.9;
		c.weighty=0.9;
		c.fill = GridBagConstraints.BOTH;
		
		JPanel samplePanel = new JPanel();
		samplePanel.setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx=0;
		gc.gridy=0;
		gc.weightx=0.5;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;
		samplePanel.add(new JLabel("Annotation Sets",JLabel.CENTER),gc);
		
		gc.gridy++;
		gc.weighty=1;
		gc.fill = GridBagConstraints.BOTH;
		
		annotationSetList = new JList(annotationSetModel);
		annotationSetList.addListSelectionListener(this);
		samplePanel.add(new JScrollPane(annotationSetList),gc);

		gc.gridy++;
		gc.weighty=0;
		gc.fill = GridBagConstraints.HORIZONTAL;

		deleteButton = new JButton("Delete Annotation Sets");
		deleteButton.setActionCommand("delete");
		deleteButton.addActionListener(this);
		deleteButton.setEnabled(false);
		samplePanel.add(deleteButton,gc);
		
		gc.gridy++;
		renameButton = new JButton("Rename Annotation Set");
		renameButton.setActionCommand("rename");
		renameButton.addActionListener(this);
		renameButton.setEnabled(false);
		samplePanel.add(renameButton,gc);

		gc.gridy++;
		replaceButton = new JButton("Replace in name");
		replaceButton.setActionCommand("replace");
		replaceButton.addActionListener(this);
		replaceButton.setEnabled(false);
		samplePanel.add(replaceButton,gc);

		gc.gridy++;
		exportButton = new JButton("Export as BED");
		exportButton.setActionCommand("export");
		exportButton.addActionListener(this);
		exportButton.setEnabled(false);
		samplePanel.add(exportButton,gc);

		
		
		getContentPane().add(samplePanel,c);

		c.gridx++;
		
		c.gridx=0;
		c.gridy++;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.NONE;

		JPanel bottomPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		bottomPanel.add(closeButton);
		
		getContentPane().add(bottomPanel,c);
		
		// Fill the lists with the data we know about
		AnnotationSet [] annotationSets = collection.genome().annotationCollection().anotationSets();
		for (int i=0;i<annotationSets.length;i++) {
			if (annotationSets[i] instanceof CoreAnnotationSet) continue;
			annotationSetModel.addElement(annotationSets[i]);
		}
				
		setVisible(true);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		String c = ae.getActionCommand();

		if (c.equals("rename")) {
			
			AnnotationSet s = (AnnotationSet)annotationSetList.getSelectedValue();
			String annotationSetName=null;
			while (true) {
				annotationSetName = (String)JOptionPane.showInputDialog(this,"Enter Annotation Set name","Annotation Set Name",JOptionPane.QUESTION_MESSAGE,null,null,s.name());
				if (annotationSetName == null) return; // They cancelled
				
				if (annotationSetName.length()>0) break;
			}
			s.setName(annotationSetName);
			annotationSetModel.setElementAt(s,annotationSetList.getSelectedIndex());
		}
		
		else if (c.equals("replace")) {
			
			Object [] o = annotationSetList.getSelectedValues();
			AnnotationSet [] as = new AnnotationSet[o.length];
			for (int i=0;i<o.length;i++) {
				as[i] = (AnnotationSet)o[i];
			}
			
			String replaceWhat=null;
			while (true) {
				replaceWhat = (String)JOptionPane.showInputDialog(this,"Replace what","Replace text",JOptionPane.QUESTION_MESSAGE,null,null,"");
				if (replaceWhat == null) return; // They cancelled
				
				if (replaceWhat.length()>0) break;
			}

			String replaceWith = (String)JOptionPane.showInputDialog(this,"Replace with","Replace text",JOptionPane.QUESTION_MESSAGE,null,null,"");
			if (replaceWith == null) return; // They cancelled
			
			// If they used a regex in their search term then they could have
			// introduced a syntax error which will trigger an exception.  We'll
			// catch this so as to produce a nicer error message
			try {
				for (int s=0;s<as.length;s++) {
					String oldName = as[s].name();
					String newName = oldName.replaceAll(replaceWhat, replaceWith);
					as[s].setName(newName);
				}
				ListDataListener [] l = annotationSetModel.getListDataListeners();
				for (int i=0;i<l.length;i++) {
					l[i].contentsChanged(new ListDataEvent(annotationSetModel, ListDataEvent.CONTENTS_CHANGED, 0, as.length));
				}
			}
			catch (PatternSyntaxException pse) {
				JOptionPane.showMessageDialog(this, "<html>You used a regex in your search, but it contained a syntax error<br><br>"+pse.getLocalizedMessage(), "Pattern error", JOptionPane.ERROR_MESSAGE);
			}
			
		}

		
		else if (c.equals("close")) {
			setVisible(false);
			dispose();
		}			
		
		else if (c.equals("delete")) {
			Object [] o = annotationSetList.getSelectedValues();
			
			if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete "+o.length+" annotation sets?", "Really delete?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) return;
			
			AnnotationSet [] as = new AnnotationSet[o.length];
			for (int i=0;i<o.length;i++) {
				as[i] = (AnnotationSet)o[i];
				annotationSetModel.removeElement(o[i]);
			}
			
			// TODO: Can we find a way to do this in a single operation?
			for (int i=0;i<as.length;i++) {
				as[i].delete();
			}
		}

		else if (c.equals("export")) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new BEDFileFilter());
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			if (! (file.getPath().toLowerCase().endsWith(".txt") || file.getPath().toLowerCase().endsWith(".bed"))) {
				file = new File(file.getPath()+".bed");
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			try {					
				PrintWriter pr = new PrintWriter(file);

				Object [] o = annotationSetList.getSelectedValues();

				for (int a=0;a<o.length;a++) {
					AnnotationSet as = (AnnotationSet)o[a];
					
					Feature [] features = as.getAllFeatures();
					
					// Add a track title
					pr.println("track name="+file.getName().replaceAll(" ", "_").replaceAll(".bed$", "") + " description="+file.getName().replaceAll(" ", "_").replaceAll(".bed$", ""));
					
					for (int f=0;f<features.length;f++) {
						StringBuffer sb = new StringBuffer();
						
						
						// UCSC browser is thick and can't figure out that 1 and chr1 are the
						// same thing so we need to help it out.
						if (!features[f].chromosomeName().startsWith("chr")) {
							sb.append("chr");
						}
						sb.append(features[f].chromosomeName());
						sb.append("\t");
						sb.append(features[f].location().start());
						sb.append("\t");
						sb.append(features[f].location().end());
						sb.append("\t");
						
						// UCSC browser is thick and won't accept (perfectly valid) strings if they
						// have spaces in them, even if they are quoted so we have to fix this
						sb.append(features[f].type().replaceAll(" ", "_"));
						sb.append("\t");
						sb.append("100");
						sb.append("\t");
						if (features[f].location().strand() == Location.FORWARD) sb.append("+");
						else if (features[f].location().strand() == Location.REVERSE) sb.append("-");
					
						// UCSC browser is thick and won't accept the (perfectly valid) unknown strand
						// so we're going to turn this into a plus
						else sb.append("+");

						pr.println(sb.toString());
						
					}
					
				}
				
				
				pr.close();

			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}

			
		}
		else if (c.equals("close")) {
			setVisible(false);
			dispose();
		}		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	public void valueChanged(ListSelectionEvent ae) {
		if (annotationSetList.getSelectedValues().length > 0) {
			deleteButton.setEnabled(true);
			replaceButton.setEnabled(true);
			exportButton.setEnabled(true);
		}
		else {
			deleteButton.setEnabled(false);
			replaceButton.setEnabled(false);
			exportButton.setEnabled(false);
		}
		
		if (annotationSetList.getSelectedValues().length == 1) {
			renameButton.setEnabled(true);
		}
		else {
			renameButton.setEnabled(false);
		}
	}
}
