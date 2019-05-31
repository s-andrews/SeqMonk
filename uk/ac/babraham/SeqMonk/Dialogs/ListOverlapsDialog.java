/**
 * Copyright 2012-19 Simon Andrews
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.FileFilters.TxtFileFilter;
import uk.ac.babraham.SeqMonk.Vistory.Vistory;
import uk.ac.babraham.SeqMonk.Vistory.VistoryTable;

public class ListOverlapsDialog extends JDialog implements Runnable, ActionListener {
	

	private int [][] overlapCounts;
	private ProbeList [] lists;
	private ProgressDialog progressDialog;
	private JTable table = null;
	
	public ListOverlapsDialog (ProbeList [] lists) {
		
		super(SeqMonkApplication.getInstance(),"List Overlaps");
		this.lists = lists;
		overlapCounts = new int [lists.length][lists.length];
		
		setSize(600,600);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout());
		
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

		add(buttonPanel,BorderLayout.SOUTH);
		
		
		
		this.progressDialog = new ProgressDialog("Calculating list overlaps");
		Thread t = new Thread(this);
		t.start();
		
	}
	
	
	public void run() {
		

		for (int i=0;i<lists.length;i++) {

			progressDialog.progressUpdated("Processed "+i+" out of "+lists.length+" lists", i, lists.length);

			for (int j=i;j<lists.length;j++) {
				int overlap = getOverlap(lists[i], lists[j]);
				overlapCounts[i][j] = overlap;
				overlapCounts[j][i] = overlap;
			}
		}
		
		progressDialog.progressComplete("", null);
		
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		TableModel model = new HitListModel();
		
		table = new JTable(model);
		table.setTableHeader(null);
				
		table.setColumnSelectionAllowed(false);
		table.setCellSelectionEnabled(true);
		
		getContentPane().add(new JScrollPane(table),BorderLayout.CENTER);
		validate();
		setVisible(true);

		
	}
	


	private int getOverlap (ProbeList list1, ProbeList list2) {
		
		Probe [] firstListProbes = list1.getAllProbes();
		Probe [] secondListProbes = list2.getAllProbes();
		
		int l1 = 0;
		int l2 = 0;
		
		int overlapCount = 0;
		
		while (true) {
			
			if (l1 >= firstListProbes.length) {
				break;
			}

			if (l2 >= secondListProbes.length) {
				break;
			}
						
			// Compare the two current probes to see what state we're in
			
			if (firstListProbes[l1] == secondListProbes[l2]) {
				//This probe is common to both lists
				++overlapCount;
				++l1;
				++l2;
			}
			else if (firstListProbes[l1].compareTo(secondListProbes[l2]) > 0) {
				// We can make a decision about the lower value (l2)
				l2++;
			}
			else {
				// We can make a decision about the lower value (l1)
				l1++;
			}
			
			
		}
		
		return overlapCount;
	}

	
	private class HitListModel extends DefaultTableModel {		
		
		public int getColumnCount() {
			return lists.length+1;
		}
		
		public boolean isCellEditable (int row, int col) {
			return false;
		}

		public int getRowCount() {
			return lists.length+1;
		}

		public Object getValueAt(int row, int col) {

			if (row == 0 && col == 0) {
				return "";
			}
			if (row == 0) {
				return lists[col-1].name();
			}
			if (col == 0) {
				return lists[row-1].name();
			}
			
			return overlapCounts[row-1][col-1];
		}
	}
	
	
	public void actionPerformed(ActionEvent ae) {
		
		if (ae.getActionCommand().equals("close")){
			setVisible(false);
			dispose();
		}
		else if (ae.getActionCommand().equals("save_vistory")){
			Vistory.getInstance().addBlock(new VistoryTable(table.getModel()));
		}
		else if (ae.getActionCommand().equals("save")){
			
			
			JFileChooser chooser = new JFileChooser(SeqMonkPreferences.getInstance().getSaveLocation());
			chooser.setMultiSelectionEnabled(false);
						
			TxtFileFilter txtff = new TxtFileFilter();
			chooser.addChoosableFileFilter(txtff);
			chooser.setFileFilter(txtff);
			
			int result = chooser.showSaveDialog(this);
			if (result == JFileChooser.CANCEL_OPTION) return;

			File file = chooser.getSelectedFile();
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);
			
			if (file.isDirectory()) return;

			file = new File(file.getPath()+".txt");
			
			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			try {
				saveTextReport(file);
			}

			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		
		
	}

	private void saveTextReport (File file) throws IOException {
		
		PrintWriter p = new PrintWriter(new FileWriter(file));
		
		TableModel model = table.getModel();
		
		int rowCount = model.getRowCount();
		int colCount = model.getColumnCount();
			
		StringBuffer b;
		
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

}
