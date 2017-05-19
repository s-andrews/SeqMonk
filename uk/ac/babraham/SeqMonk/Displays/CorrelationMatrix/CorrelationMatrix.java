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
package uk.ac.babraham.SeqMonk.Displays.CorrelationMatrix;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Correlation.DistanceMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.ProgressListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

public class CorrelationMatrix extends JDialog implements ProgressListener, ActionListener {

	private DistanceMatrix matrix;
	private DistanceTableModel model;
	private JPanel tablePanel = null;
	
	public CorrelationMatrix (DataStore [] stores, ProbeList probes) {
		super(SeqMonkApplication.getInstance(),"Correlation table ["+probes.name()+"]");
		matrix = new DistanceMatrix(stores,probes.getAllProbes());
		matrix.addProgressListener(this);
		matrix.addProgressListener(new ProgressDialog(SeqMonkApplication.getInstance(), "Correlation Calculation",matrix));
				
		getContentPane().setLayout(new BorderLayout());
		
		JPanel buttonPanel = new JPanel();
		
		JButton closeButton = new JButton("Close");
		closeButton.setActionCommand("close");
		closeButton.addActionListener(this);
		buttonPanel.add(closeButton);
		
		JButton saveButton = new JButton("Save Data");
		saveButton.setActionCommand("save_data");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);

		JButton saveImageButton = new JButton("Save Image");
		saveImageButton.setActionCommand("save_image");
		saveImageButton.addActionListener(this);
		buttonPanel.add(saveImageButton);

		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);
		
		setSize(700, 500);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		Thread t = new Thread(matrix);
		t.start();		
		
	}

	public void progressCancelled() {
		dispose();
	}

	public void progressComplete(String command, Object result) {
		model = new DistanceTableModel();
		
		tablePanel = new JPanel();
		tablePanel.setLayout(new BorderLayout());
		
		tablePanel.add(new CorrelationPanel(model, new CorrelationCellRenderer(model)),BorderLayout.CENTER);
		tablePanel.add(new GradientScaleBar(DisplayPreferences.getInstance().getGradient(), model.getMinCorrelation(), model.getMaxCorrelation()), BorderLayout.EAST);
		
		getContentPane().add(tablePanel, BorderLayout.CENTER);
		
		setVisible(true);
				
	}

	public void progressExceptionReceived(Exception e) {}

	public void progressUpdated(String message, int current, int max) {}

	public void progressWarningReceived(Exception e) {}	
	
	public void actionPerformed(ActionEvent e) {

		if (e.getActionCommand().equals("close")) {
			setVisible(false);
			dispose();
		}
		
		else if (e.getActionCommand().equals("save_image")) {
			if (tablePanel == null) return; // There's nothing to see...
			
			ImageSaver.saveImage(tablePanel);
			
		}
		else if (e.getActionCommand().equals("save_data")) {
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			chooser.setFileFilter(new FileFilter() {
				
				public String getDescription() {
					return "Text files";
				}
				
				public boolean accept(File f) {
					if (f.isDirectory() || f.getName().toLowerCase().endsWith(".txt")) {
						return true;
					}
					return false;
				}
			});
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			if (! file.getPath().toLowerCase().endsWith(".txt")) {
				file = new File(file.getPath()+".txt");
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}
	
			try {
				saveFile(file);
			}
			catch (IOException e1) {
				throw new IllegalStateException(e1);
			}
			
			

		}
		else {
			throw new IllegalStateException("Unknown command '"+e.getActionCommand()+"'");
		}
	}
	
	private void saveFile (File file) throws IOException {
	
		PrintWriter pr = new PrintWriter(file);
		
		for (int line=0;line<model.getRowCount();line++) {
			for (int col=0;col<model.getRowCount();col++) {
				if (col>0) pr.print("\t");
				pr.print(model.getValueAt(line, col));
			}
			pr.print("\n");
		}
		pr.print("\n");
		
		pr.close();
		
	}

	private class DistanceTableModel extends AbstractTableModel {
	
		public int getColumnCount() {
			return matrix.stores().length+1;
		}
	
		public int getRowCount() {
			return matrix.stores().length+1;
		}
		
		public float getMinCorrelation() {
			try {
				return(matrix.getMinCorrelation());
			}
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}
		}

		public float getMaxCorrelation() {
			try {
				return(matrix.getMaxCorrelation());
			}
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}
		}
		
		public Object getValueAt(int r, int c) {
			
			if (r==0 && c==0) {
				return "";
			}
			if (r==0) {
				return matrix.stores()[c-1].name();
			}
			if (c==0) {
				return matrix.stores()[r-1].name();
			}
			try {
				return matrix.getCorrelationForStoreIndices(r-1, c-1);
			}
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}			
		}
		
		public Class getColumnClass (int c) {
			return String.class;
		}
		
		public String getLabel(int row, int col) {
			if (row==0 || col==0) {
				return "";
			}
			
			try {
				return matrix.stores()[row-1]+" vs "+matrix.stores()[col-1]+" r="+matrix.getCorrelationForStoreIndices(row-1, col-1);
			}
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}
		}
		
		
	}
	
	private class CorrelationCellRenderer {
		
		private DistanceTableModel model;
		private float minCorrelation;
		private float maxCorrelation;
		
		private ColourGradient gradient;
		
		
		public CorrelationCellRenderer (DistanceTableModel model) {
			minCorrelation = model.getMinCorrelation();
			maxCorrelation = model.getMaxCorrelation();
			this.model = model;			
			
			gradient = DisplayPreferences.getInstance().getGradient();
			
		}
		
		public JLabel getLabel (int r, int c) {
			JLabel l = new JLabel(""+model.getValueAt(r, c),JLabel.CENTER);
			l.setVerticalAlignment(JLabel.CENTER);
			l.setToolTipText(model.getLabel(r, c));
			try {
				float f = Float.parseFloat(l.getText());
				
//				System.err.println("Parsed number is "+f+" min="+minCorrelation+" max="+maxCorrelation);
				
				if (f >= minCorrelation && f <= maxCorrelation) {						
					l.setOpaque(true);
					l.setBackground(gradient.getColor(f, minCorrelation, maxCorrelation));
				}
				else {
					l.setOpaque(false);
				}
			}
			catch (NumberFormatException nfe) {
				l.setOpaque(false);
			}
			
			return(l);
			
		}
		
	}
	
	private class CorrelationPanel extends JPanel {
		
		public CorrelationPanel (DistanceTableModel model, CorrelationCellRenderer renderer) {
			
			System.err.println("Rows="+model.getRowCount()+" cols="+model.getColumnCount());
			
			setLayout(new GridLayout(model.getRowCount(), model.getColumnCount()));
			for (int r=0;r<model.getRowCount();r++) {
				for (int c=0;c<model.getColumnCount();c++) {
					add(renderer.getLabel(r, c));
				}
			}
			
			
		}
				
	}
	
	
	
}
