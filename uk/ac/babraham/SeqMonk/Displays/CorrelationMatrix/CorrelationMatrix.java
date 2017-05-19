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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
		
		tablePanel.add(new CorrelationPanel(),BorderLayout.CENTER);
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
			if (row==0 && col==0) {
				return "";
			}
			else if (row == 0) {
				return matrix.stores()[col-1].name();
			}
			else if (col == 0) {
				return matrix.stores()[row-1].name();
			}
			
			try {
				return matrix.stores()[row-1]+" vs "+matrix.stores()[col-1]+" r="+matrix.getCorrelationForStoreIndices(row-1, col-1);
			}
			catch (SeqMonkException e) {
				throw new IllegalStateException(e);
			}
		}
		
		
	}
	
	private class CorrelationPanel extends JPanel implements MouseMotionListener {
		
		private ColourGradient gradient = DisplayPreferences.getInstance().getGradient();
		private float minCorrelation = model.getMinCorrelation();
		private float maxCorrelation = model.getMaxCorrelation();
				
		public CorrelationPanel () {
			addMouseMotionListener(this);
		}
		
		public void paintComponent (Graphics g) {
			
			int lastEndX = 0;
			int lastEndY = 0;
			
			// We need to work out whether it's worth trying to draw text and if so
			// at what font size we'd be working.
			
			boolean drawText = true;
			
			// We'll base the font size on being half the height of the boxes
			int boxHeight = (getY(1) - getY(0))/2;
			int boxWidth = getX(1) - getX(0);
			
			int fontSize = 1;
			
			while (true) {
				
				g.setFont(new Font("sans",Font.PLAIN,fontSize));
				if (g.getFontMetrics().getAscent() > boxHeight) break;
				
				++fontSize;
			}
			
			// If we don't have at least a 5 point font then give up
			if (fontSize < 2) {
				drawText = false;
			}
			else {
			
				// Now we need to see if we have enough width to draw anything
				// sensible
				if (g.getFontMetrics().stringWidth("0.99") > boxWidth) {
					drawText = false;
				}
				
			}
			
			
			for (int r=0;r<model.getRowCount();r++) {
				
				// Reset the X pointer
				lastEndX = 0;
				
				// See if we need to draw this row at all
				int thisEndY = getY(r+1);
				if (thisEndY <= lastEndY) continue; // We can't even see this row
				
				for (int c=0;c<model.getColumnCount();c++) {
					
					// See if we need to draw this column
					int thisEndX = getX(c+1);
					if (thisEndX <= lastEndX) continue; // Can't see it
					
					
					try {
						float f = Float.parseFloat(""+model.getValueAt(r, c));
												
						if (f >= minCorrelation && f <= maxCorrelation) {
							g.setColor(gradient.getColor(f, minCorrelation, maxCorrelation));
						}
						else {
							g.setColor(Color.WHITE);
						}
					}
					catch (NumberFormatException nfe) {
						g.setColor(Color.WHITE);
					}
				
					g.fillRect(lastEndX, lastEndY, thisEndX-lastEndX, thisEndY-lastEndY);
					
					if (drawText) {
						g.setColor(Color.BLACK);
						
						String string = ""+model.getValueAt(r, c);
						
						while (g.getFontMetrics().stringWidth(string) > boxWidth) {
							string = string.substring(0, string.length()-1);
						}
						
						int stringX = (lastEndX+((thisEndX-lastEndX)/2))-(g.getFontMetrics().stringWidth(string)/2);
						
						g.drawString(string, stringX, lastEndY+g.getFontMetrics().getHeight());
					}
					
					lastEndX = thisEndX;
					
				}
				
				lastEndY = thisEndY;
			}
		
		}
		
		public int getX(int index) {
			return((int)((getWidth()/(double)model.getColumnCount())*index));
		}
		
		public int getY(int index) {
			return((int)((getHeight()/(double)model.getColumnCount())*index));
		}

		public void mouseDragged(MouseEvent arg0) {}

		public void mouseMoved(MouseEvent me) {
			// We set the tooltip to show what's under the current position
			
			int c = (int)(me.getX() / (getWidth()/(double)model.getColumnCount()));
			int r = (int)(me.getY() / (getHeight()/(double)model.getRowCount()));
			
			setToolTipText(model.getLabel(r, c));
			
		}

		
				
	}
	
	
	
}
