/**
 * Copyright 2012-15 Simon Andrews
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

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class ListOverlapsDialog extends JDialog implements Runnable {
	

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
	
}
