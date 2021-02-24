/**
 * Copyright Copyright 2018- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Dialogs.DataSetEditor;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;

public class MultiDataSetRenameDialog extends JDialog {

	private DataSet [] sets;
	private String [] names;
	
	private JTable nameTable;
	
	public MultiDataSetRenameDialog (DataSet [] sets) {
		super(SeqMonkApplication.getInstance(),"Edit DataSet names");
		this.sets = sets;
		names = new String[sets.length];
		for (int i=0;i<sets.length;i++) {
			names[i] = sets[i].name();
		}
		
		getContentPane().setLayout(new BorderLayout());
		
		nameTable = new JTable(new renameTableModel());
		nameTable.setColumnSelectionAllowed(true);
		nameTable.setRowSelectionAllowed(false);
		nameTable.setAutoCreateRowSorter(true);
		nameTable.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {}
			
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.isControlDown() && e.getKeyCode()==KeyEvent.VK_V) {
					pasteFromClipboard();
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {}
		});
		
		getContentPane().add(new JScrollPane(nameTable), BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		
		JButton editButton = new JButton("Edit names");
		editButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int i=0;i<MultiDataSetRenameDialog.this.sets.length;i++) {
					if (!MultiDataSetRenameDialog.this.sets[i].name().equals(names[i])) {
						MultiDataSetRenameDialog.this.sets[i].setName(names[i]);
					}
				}
				setVisible(false);
				dispose();
			}
		});
		

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});

		buttonPanel.add(editButton);
		buttonPanel.add(cancelButton);
		
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		setModal(true);
		setSize(400,500);
		setLocationRelativeTo(SeqMonkApplication.getInstance());
		setVisible(true);
	}
	
	
	private void pasteFromClipboard() { 
        int startRow=nameTable.getSelectedRows()[0]; 
        int startCol=nameTable.getSelectedColumns()[0];
        
        if (startCol != 1) return;

        String pasteString = ""; 
        try { 
                pasteString = (String)(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor)); 
        } catch (Exception e) { 
                JOptionPane.showMessageDialog(null, "Invalid Paste Type", "Invalid Paste Type", JOptionPane.ERROR_MESSAGE);
                return; 
        } 
        
        String[] lines = pasteString.split("\n"); 
        for (int i=0 ; i<lines.length; i++) { 
        	if (nameTable.getRowCount()>startRow+i && nameTable.getColumnCount()>startCol) { 
        		nameTable.setValueAt(lines[i], startRow+i, startCol);
        	} 
        }
	} 

	
	private class renameTableModel extends AbstractTableModel {

		@Override
		public int getColumnCount() {
			return 2;
		}

		public int getRowCount() {
			return sets.length;
		}
		
		public String getColumnName (int c) {
			if (c==0) return "Original name";
			return "New name";
		}
		
		public Class getColumnClass (int c) {
			return String.class;
		}

		@Override
		public Object getValueAt(int r, int c) {
			if (c==0) return sets[r].name();
			return names[r];
		}
		
		public boolean isCellEditable (int r, int c) {
			return (c==1);
		}

		public void setValueAt(Object o,int r, int c) {
			if (c==1) {
				names[r] = (String)o;
				fireTableCellUpdated(r, c);
			}

		}

		
	}
	
	
}
