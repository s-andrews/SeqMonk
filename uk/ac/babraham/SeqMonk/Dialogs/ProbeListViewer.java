/**
 * Copyright Copyright 2010-17 Simon Andrews
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
import java.awt.Font;
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
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.sun.java.TableSorter;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;
import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;

/**
 * The Class ProbeListViewer shows a simple view of a probe list and
 * its description
 */
public class ProbeListViewer extends JDialog implements MouseListener, ActionListener {
		
	/** The table. */
	private JTable table;
	
	/**
	 * Instantiates a new probe list viewer.
	 * 
	 * @param list the list
	 * @param application the application
	 */
	@SuppressWarnings("rawtypes")
	public ProbeListViewer (ProbeList list, SeqMonkApplication application) {
		super(application,list.name()+" ("+list.getAllProbes().length+" probes)");
				
		Probe [] probes = list.getAllProbes();
		
		getContentPane().setLayout(new BorderLayout());

		JTextArea description = new JTextArea("Description:\n\n"+list.description()+"\n\nComments:\n\n"+list.comments(),5,0);
		description.setEditable(false);
		description.setFont(Font.getFont("default"));
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		getContentPane().add(new JScrollPane(description,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),BorderLayout.NORTH);
		
		String [] headers = new String [] {"Probe","Chr","Start","End",list.getValueName()};
		Class [] classes = new Class [] {String.class,String.class,Integer.class,Integer.class,Double.class};
		
		Object [][] rowData = new Object [probes.length][headers.length];
		
		for (int i=0;i<probes.length;i++) {
			rowData[i][0] = probes[i];
			if (probes[i].chromosome() != null) {
				rowData[i][1] = probes[i].chromosome().name();
			}
			else {
				rowData[i][1] = "No chr";				
			}
			rowData[i][2] = new Integer(probes[i].start());
			rowData[i][3] = new Integer(probes[i].end());
			rowData[i][4] = list.getValueForProbe(probes[i]);
		}

		TableSorter sorter = new TableSorter(new ProbeTableModel(rowData,headers,classes));
		table = new JTable(sorter);
//		table.setDefaultRenderer(Double.class, new SmallDoubleCellRenderer());
		table.addMouseListener(this);
		sorter.setTableHeader(table.getTableHeader());

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
		
		setSize(500,350);
		setLocationRelativeTo(application);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setVisible(true);
		
	}

	/**
	 * The Class ProbeTableModel.
	 */
	private class ProbeTableModel extends AbstractTableModel {

		/** The data. */
		private Object [][] data;
		
		/** The headers. */
		private String [] headers;
		
		/** The classes. */
		@SuppressWarnings("rawtypes")
		private Class [] classes;
		
		/**
		 * Instantiates a new probe table model.
		 * 
		 * @param data the data
		 * @param headers the headers
		 * @param classes the classes
		 */
		@SuppressWarnings("rawtypes")
		public ProbeTableModel (Object [][] data, String [] headers, Class [] classes) {
			super();
			this.data = data;
			this.headers = headers;
			this.classes = classes;
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return data.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			if (data.length > 0) {
				return data[0].length;
			}
			return 0;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			return data[r][c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			return headers[c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass (int c) {
			return classes[c];
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
		 */
		public boolean isCellEditable (int r, int c) {
			return false;
		}
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
		Probe p = (Probe)t.getValueAt(r,0);
		DisplayPreferences.getInstance().setLocation(p.chromosome(), p.packedPosition());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent arg0) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent arg0) {
	}

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
			if (! file.getPath().toLowerCase().endsWith(".txt")) {
				file = new File(file.getPath()+".txt");
			}
			
			SeqMonkPreferences.getInstance().setLastUsedSaveLocation(file);

			// Check if we're stepping on anyone's toes...
			if (file.exists()) {
				int answer = JOptionPane.showOptionDialog(this,file.getName()+" exists.  Do you want to overwrite the existing file?","Overwrite file?",0,JOptionPane.QUESTION_MESSAGE,null,new String [] {"Overwrite and Save","Cancel"},"Overwrite and Save");

				if (answer > 0) {
					return;
				}
			}

			
			try {
				PrintWriter p = new PrintWriter(new FileWriter(file));
				
				TableModel model = table.getModel();
				
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
