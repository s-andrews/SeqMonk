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
package uk.ac.babraham.SeqMonk.Displays.Report;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
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
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Chromosome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Reports.Report;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.GFFFileFilter;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryTable;

import com.sun.java.TableSorter;

/**
 * The Class ReportTableDialog is the generic container for all of the
 * different types of report.
 */
public class ReportTableDialog extends JDialog implements MouseListener, ActionListener {

	/** The application. */
	private SeqMonkApplication application;
	
	/** The model. */
	private TableSorter model;
	
	/** The report */
	private Report report;
	
	/**
	 * Instantiates a new report table dialog.
	 * 
	 * @param application the application
	 * @param report the report
	 * @param originalModel the original model
	 */
	public ReportTableDialog (SeqMonkApplication application, Report report, TableModel originalModel) {
		super(application,report.name());
		
		this.report = report;
		
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
		
		JButton saveButton = new JButton("Save to File");
		saveButton.setActionCommand("save");
		saveButton.addActionListener(this);
		buttonPanel.add(saveButton);

		JButton saveVistoryButton = new JButton("Save to Vistory");
		saveVistoryButton.setActionCommand("save_vistory");
		saveVistoryButton.addActionListener(this);
		buttonPanel.add(saveVistoryButton);

		
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
		
		Chromosome chr = null;
		int start = -1;
		int end = -1;
		
		for (int i=0;i<model.getColumnCount();i++) {
			if (model.getColumnName(i).equals("Chr") || model.getColumnName(i).equals("Chromosome")) {
				
				// The chromosome field can be a Chromosome or String
				// object depending on the report.
				
				if (model.getValueAt(r, i) instanceof Chromosome) {
					chr = (Chromosome)model.getValueAt(r, i);
				}
				else if (model.getValueAt(r, i) instanceof String) {
					chr = application.dataCollection().genome().getChromosome((String)model.getValueAt(r, i)).chromosome();
				}
			}
			else if (model.getColumnName(i).equals("Start")) {
				start = ((Integer)model.getValueAt(r, i)).intValue();
			}
			else if (model.getColumnName(i).equals("End")) {
				end = ((Integer)model.getValueAt(r, i)).intValue();
			}
		}
		
		if (chr != null && start > 0 && end > 0) {
			DisplayPreferences.getInstance().setLocation(chr, SequenceRead.packPosition(start, end, Location.UNKNOWN));
		}
		else {
			System.err.println("Couldn't find a position to jump to.  Closest thing was "+chr+" "+start+"-"+end);
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
		else if (ae.getActionCommand().equals("save_vistory")){
			Vistory.getInstance().addBlock(new VistoryTable(model));
		}
		else if (ae.getActionCommand().equals("save")){
			
			
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
			
			if (report.canExportGFF()) {
				chooser.addChoosableFileFilter(new GFFFileFilter());
			}
			
			TxtFileFilter txtff = new TxtFileFilter();
			chooser.addChoosableFileFilter(txtff);
			chooser.setFileFilter(txtff);
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			FileFilter filter = chooser.getFileFilter();
			
			if (filter instanceof TxtFileFilter) {		
				if (! file.getPath().toLowerCase().endsWith(".txt")) {
					file = new File(file.getPath()+".txt");
				}
			}
			else if (filter instanceof GFFFileFilter) {
				if (! file.getPath().toLowerCase().endsWith(".gff")) {
					file = new File(file.getPath()+".gff");
				}			
			}
			else {
				System.err.println("Unknown file filter type "+filter+" when saving image");
				return;
			}
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			try {					
				if (filter instanceof TxtFileFilter) {
					saveTextReport(file);
				}
				else if (filter instanceof GFFFileFilter) {
					saveGFFReport(file);
				}
				else {
					System.err.println("Unknown file filter type "+filter+" when saving image");
					return;
				}
			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		
		
	}

	private void saveTextReport (File file) throws IOException {
		
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
	
	private void saveGFFReport (File file) throws IOException {
		
		PrintWriter p = new PrintWriter(new FileWriter(file));
		
		int chrColumn = report.chromosomeColumn();
		int startColumn = report.startColumn();
		int endColumn = report.endColumn();
		int strandColumn = report.strandColumn();
		
		
		if (startColumn < 0 || endColumn < 0 || chrColumn < 0) {
			p.close();
			throw new IllegalStateException("Couldn't find chr start or end in report");
		}
		
		
		int rowCount = model.getRowCount();
		int colCount = model.getColumnCount();
		
		StringBuffer b;
		
		for (int r=0;r<rowCount;r++) {
			
			b = new StringBuffer();
			
			b.append(model.getValueAt(r, chrColumn));
			b.append("\t");
			b.append("seqmonk");
			b.append("\t");
			b.append("probe");
			b.append("\t");
			b.append(model.getValueAt(r, startColumn));
			b.append("\t");
			b.append(model.getValueAt(r, endColumn));
			b.append("\t");
			b.append(".");
			b.append("\t");

			if (strandColumn >=0) {
				String strand = model.getValueAt(r, strandColumn).toString();
				if (strand.equals("+") || strand.equals("-")) {
					b.append(strand);
				}
				else {
					b.append(".");
				}
			}
			else {
				b.append(".");
			}
			b.append("\t");
			b.append(".");
			b.append("\t");
			b.append(".");
			b.append("\t");
			
			b.append("ID=");
			b.append(r+1);
			
			for (int c=0;c<colCount;c++) {
				
				if (model.getColumnName(c).equals("No value")) continue;
				
				b.append(";");
				b.append(model.getColumnName(c));
				b.append("=");
				b.append(model.getValueAt(r,c));
			}
			p.println(b);
			
		}
		p.close();
	}
	

	
}
