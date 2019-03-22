/**
 * Copyright 2009-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.Report.InteractionReport;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Reports.Interaction.InteractionReport;

import com.sun.java.TableSorter;

/**
 * The Class ReportTableDialog is the generic container for all of the
 * different types of report.
 */
public class InteractionReportTableDialog extends JDialog implements MouseListener, ActionListener {

	/** The application. */
	private SeqMonkApplication application;
	
	/** The model. */
	private TableSorter model;
	
	/**
	 * Instantiates a new report table dialog.
	 * 
	 * @param application the application
	 * @param report the report
	 * @param originalModel the original model
	 */
	public InteractionReportTableDialog (SeqMonkApplication application, InteractionReport report, TableModel originalModel) {
		super(application,report.name());
		
		this.application = application;

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		model = new TableSorter(originalModel);
		
		JTable table = new JTable(model);
		table.setColumnSelectionAllowed(true);
		table.setCellSelectionEnabled(true);
		table.addMouseListener(this);

		model.setTableHeader(table.getTableHeader());
		
		getContentPane().setLayout(new BorderLayout());
		
		getContentPane().add(new JScrollPane(table),BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		JButton cancelButton = new JButton("Close");
		cancelButton.setActionCommand("close");
		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);
		
		JButton saveButton = new JButton("Save");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);
		
		getContentPane().add(buttonPanel,BorderLayout.SOUTH);

		setSize(800,600);
		setLocationRelativeTo(application);
		setVisible(true);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent me) {
		//We're only interested in double clicks
		if (me.getClickCount() !=2 ) return;
		// This is only linked from the report JTable
		JTable t = (JTable)me.getSource();
		int r = t.getSelectedRow();
		int c = t.getSelectedColumn();

		Object clickedOn = t.getModel().getValueAt(r, c);

		if (clickedOn == null) return;
		
		if (clickedOn instanceof Probe) {
			Probe p = (Probe)clickedOn;
			DisplayPreferences.getInstance().setLocation(p.chromosome(),p.packedPosition());
		}
		if (clickedOn instanceof Feature) {
			Feature f = (Feature)clickedOn;
			DisplayPreferences.getInstance().setLocation(application.dataCollection().genome().getChromosome(f.chromosomeName()).chromosome(),f.location().packedPosition());
		}
				
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save")){
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
					else {
						return false;
					}
				}
			
			});
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
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
				PrintWriter p = new PrintWriter(new FileWriter(file));
				
				int rowCount = model.getRowCount();
				int colCount = model.getColumnCount();
				
				// Do the headers first
				StringBuffer b = new StringBuffer();
				for (int c=0;c<colCount;c++) {
					b.append(model.getColumnName(c));
					if (c+1 != colCount) {
						b.append("\t");
					}
				}
				
				p.println(b);
				
				for (int r=0;r<rowCount;r++) {
					b = new StringBuffer();
					for (int c=0;c<colCount;c++) {
						b.append(model.getValueAt(r,c));
						if (c+1 != colCount) {
							b.append("\t");
						}
					}
					p.println(b);
					
				}
				p.close();
			
			}
				
					
			catch (FileNotFoundException e) {
				throw new IllegalStateException(e);
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
			
		}
		
		
	}
	
}
