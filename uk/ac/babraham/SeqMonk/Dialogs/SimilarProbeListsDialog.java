/**
 * Copyright 2012-18 Simon Andrews
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.sun.java.TableSorter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.ProgressDialog.ProgressDialog;

public class SimilarProbeListsDialog extends JDialog implements Runnable, MouseListener {
	
	private ProbeListHit [] hits = new ProbeListHit[0];
	private DataCollection collection;
	private ProbeList startingList;
	private ProgressDialog progressDialog;
	private JTable table = null;
	
	public SimilarProbeListsDialog (DataCollection collection) {
		
		super(SeqMonkApplication.getInstance(),"Similar Lists to "+collection.probeSet().getActiveList().name());
		
		setSize(600,400);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		getContentPane().setLayout(new BorderLayout());

		this.startingList = collection.probeSet().getActiveList();
		this.collection = collection;
		
		this.progressDialog = new ProgressDialog("Calculating list overlaps");
		Thread t = new Thread(this);
		t.start();
		
	}
	
	
	public void run() {
		
		Vector<ProbeListHit> tempHits = new Vector<SimilarProbeListsDialog.ProbeListHit>();
		
		ProbeList topList = startingList;
		while (topList.parent() != null) {
			topList = topList.parent();
		}
		
		ProbeList [] allLists = topList.getAllProbeLists();
		
		for (int l=0;l<allLists.length;l++) {
			
			progressDialog.progressUpdated("Processed "+l+" out of "+allLists.length+" lists", l, allLists.length);
			
			if (allLists[l] == startingList) continue; // We know we're similar to ourself!
			
			int overlap = getOverlap(startingList,allLists[l]);
			if (overlap > 0) {
				tempHits.add(new ProbeListHit(allLists[l], overlap));
			}
		}

		
		hits = tempHits.toArray(new ProbeListHit[0]);
		progressDialog.progressComplete("", null);
		
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		
		TableSorter model = new TableSorter(new HitListModel());
		
		table = new JTable(model);
		
		model.setTableHeader(table.getTableHeader());
		
		table.addMouseListener(this);
		table.setColumnSelectionAllowed(false);
		table.setCellSelectionEnabled(true);
		
		getContentPane().add(new JScrollPane(table),BorderLayout.CENTER);
		validate();
		setVisible(true);

		
	}
	
	public void mouseClicked(MouseEvent me) {
		//We're only interested in double clicks
		if (me.getClickCount() !=2 ) return;
		
		// If the table hasn't been drawn yet then bail out
		if (table == null) return;
		
		// Get the selected row
		int selectedRow = table.getSelectedRow();
		if (selectedRow < 0) return;
		
		ProbeList newActiveList = (ProbeList)table.getModel().getValueAt(selectedRow, 0);
		try {
			collection.probeSet().setActiveList(newActiveList);
		}
		catch (SeqMonkException e) {
			throw new IllegalStateException(e);
		}
	}


	public void mouseEntered(MouseEvent me) {}
	public void mouseExited(MouseEvent arg0) {}
	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}


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


	private class ProbeListHit {
		
		private ProbeList list;
		private int hitCount;
		
		public ProbeListHit (ProbeList list, int hitCount) {
			this.list = list;
			this.hitCount = hitCount;
		}
	}
	
	private class HitListModel extends DefaultTableModel {
		
		/*
		 * Columns are:
		 * 
		 * 0) List name
		 * 1) Overlap count
		 * 2) Percentage of starting list
		 * 3) Percentage of hit list
		 */
		
		public int getColumnCount() {
			return 4;
		}
		
		public boolean isCellEditable (int row, int col) {
			return false;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass(int col) {
			switch (col) {
			
			case 0:
				return String.class;
			case 1:
				return Integer.class;
			case 2:
				return Double.class;
			case 3:
				return Double.class;
			}
			return null;
		}
		
		public String getColumnName(int col) {
			switch (col) {
			
			case 0:
				return "List name";
			case 1:
				return "Overlap";
			case 2:
				return "% Query List";
			case 3:
				return "% Hit List";
			}
			return null;
		}

		public int getRowCount() {
			return hits.length;
		}

		public Object getValueAt(int row, int col) {

			switch (col) {
			
			case 0:
				return hits[row].list;
			case 1:
				return hits[row].hitCount;
			case 2:
				return (hits[row].hitCount*100d)/startingList.getAllProbes().length;
			case 3:
				return (hits[row].hitCount*100d)/hits[row].list.getAllProbes().length;
			
			}
			return null;
		}
		
	}
	
}
