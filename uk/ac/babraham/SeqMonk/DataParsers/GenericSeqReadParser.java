/**
 * Copyright Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.DataParsers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataSet;
import uk.ac.babraham.SeqMonk.DataTypes.PairedDataSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.SequenceRead;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;
import uk.ac.babraham.SeqMonk.Utilities.NumberKeyListener;

/**
 * Provides a flexible import mechanism for delimitd text files which
 * contain all of the required data for a read but aren't in an
 * otherwise recognised format.
 */
public class GenericSeqReadParser extends DataParser {
	
	private String delimitersValue = "\t";
	private int startRowValue = 0;
	private int chrColValue = -1;
	private int startColValue = -1;
	private int endColValue = -1;
	private int strandColValue = -1;
	private int countColValue = -1;
	private boolean useStrand = false;
	private GenericSeqReadParserOptions optionsPanel;

	/**
	 * Instantiates a new generic seq read parser.
	 * 
	 * @param data The dataCollection to which new data will be added.
	 */
	public GenericSeqReadParser (DataCollection data) {
		super(data);
	}

	/**
	 * Instantiates a new generic seq read parser with all options set.  All column
	 * positions count from 0.
	 * 
	 * @param data The dataCollection to which new data will be added.
	 * @param delimiter The delimiter between fields
	 * @param startAtRow The row to start reading at
	 * @param chrCol Which column contains the chromosome
	 * @param startCol Which column contains the start position
	 * @param endCol Which column contains the end position
	 * @param strandCol Which column contains the strand
	 */
	public GenericSeqReadParser (DataCollection data, String delimiter, int startAtRow, int chrCol, int startCol, int endCol, int strandCol, int countCol) {
		super(data);
		this.startRowValue = startAtRow;
		this.delimitersValue = delimiter;
		this.startColValue = startCol;
		this.endColValue = endCol;
		this.strandColValue = strandCol;
		this.strandColValue = strandCol;
		if (strandColValue >=0) {
			useStrand = true;
		}
		else {
			useStrand = false;
		}

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getFileFilter()
	 */
	public FileFilter getFileFilter () {
		return new FileFilter() {
			public boolean accept(File pathname) {
				return (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".txt") || pathname.getName().toLowerCase().endsWith(".txt.gz"));
			}

			public String getDescription() {
				return "Text Files";
			}
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		try {
			// This call just makes sure that the options panel exists if
			// it's never been called for before.
			getOptionsPanel();

			boolean removeDuplicates = optionsPanel.removeDuplicates();
			int extendBy = optionsPanel.extendBy();
			
			File [] probeFiles = getFiles();
			DataSet [] newData = new DataSet [probeFiles.length];

			for (int f=0;f<probeFiles.length;f++) {

				BufferedReader br;

				if (probeFiles[f].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(probeFiles[f]))));	
				}
				else {
					br = new BufferedReader(new FileReader(probeFiles[f]));
				}
				
				String line;
				// First skip the header lines
				for (int i=0;i<startRowValue;i++) {
					line = br.readLine();
					if (line == null) {
						br.close();
						throw new Exception ("Ran out of file before skipping all of the header lines");
					}
				}

				int maxIndexValue = 0;
				if (chrColValue > maxIndexValue) maxIndexValue = chrColValue;
				if (startColValue > maxIndexValue) maxIndexValue = startColValue;
				if (endColValue > maxIndexValue) maxIndexValue = endColValue;
				if (strandColValue > maxIndexValue) maxIndexValue = strandColValue;
				if (countColValue > maxIndexValue) maxIndexValue = countColValue;

				// Just use the filename for the dataset name - they can change it later.
				
				if (optionsPanel.isHiC()) {
					int distance = 0;
					if (optionsPanel.hiCDistance.getText().length() > 0) {
						distance = Integer.parseInt(optionsPanel.hiCDistance.getText());
					}
					
					// TODO: Add an option to remove trans hits when importing from the generic parser.
					newData[f] = new PairedDataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),removeDuplicates,distance,false);
				}
				else {
					newData[f] = new DataSet(probeFiles[f].getName(),probeFiles[f].getCanonicalPath(),removeDuplicates);					
				}

				int lineCount = 0;
				// Now process the rest of the file
				while ((line = br.readLine())!= null) {
					
					if (cancel) {
						br.close();
						progressCancelled();
						return;
					}
					
					++lineCount;
					if (lineCount%100000 == 0) {
						progressUpdated("Read "+lineCount+" lines from "+probeFiles[f].getName(),f,probeFiles.length);
					}

					String [] sections = line.split(delimitersValue);

					// Check to see if we've got enough data to work with
					if (maxIndexValue >= sections.length) {
						progressWarningReceived(new SeqMonkException("Not enough data ("+sections.length+") to get a probe name on line '"+line+"'"));
						continue; // Skip this line...						
					}

					int strand;
					int start;
					int end;
					int count = 1;

					try {

						start = Integer.parseInt(sections[startColValue].replaceAll(" ", ""));
						end = Integer.parseInt(sections[endColValue].replaceAll(" ", ""));

						// End must always be later than start
						if (end < start) {
							int temp = start;
							start = end;
							end = temp;
						}

						if (countColValue != -1 && sections[countColValue].length()>0) {
							try {
								count = Integer.parseInt(sections[countColValue].replaceAll(" ", ""));
							}
							catch (NumberFormatException e) {
								progressWarningReceived(new SeqMonkException("Count value "+sections[countColValue]+" was not an integer"));
								continue;
							}
						}
						
						if (useStrand) {
							sections[strandColValue] = sections[strandColValue].replaceAll(" ", "");
							if (sections[strandColValue].equals("+") || sections[strandColValue].equals("1") || sections[strandColValue].equals("FF") || sections[strandColValue].equals("F")) {
								strand = Location.FORWARD;
							}
							else if (sections[strandColValue].equals("-") || sections[strandColValue].equals("-1")|| sections[strandColValue].equals("RF") || sections[strandColValue].equals("R")) {
								strand = Location.REVERSE;
							}
							else {
								progressWarningReceived(new SeqMonkException("Unknown strand character '"+sections[strandColValue]+"' marked as unknown strand"));
								strand = Location.UNKNOWN;
							}
						}
						else {
							strand = Location.UNKNOWN;
						}
						
						if (extendBy > 0) {
							if (strand == Location.REVERSE) {
								start -= extendBy;
							}
							else {
								end += extendBy;
							}
						}
					}
					catch (NumberFormatException e) {
						progressWarningReceived(new SeqMonkException("Location '"+sections[startColValue]+"'-'"+sections[endColValue]+"' was not an integer"));
						continue;
					}

					ChromosomeWithOffset c;
					
					try {
						c = dataCollection().genome().getChromosome(sections[chrColValue]);
					}
					catch (IllegalArgumentException sme) {
						progressWarningReceived(sme);
						continue;
					}
					
					start = c.position(start);
					end = c.position(end);

					// We also don't allow readings which are beyond the end of the chromosome
					if (end > c.chromosome().length()) {
						int overrun = end - c.chromosome().length();
						progressWarningReceived(new SeqMonkException("Reading position "+end+" was "+overrun+"bp beyond the end of chr"+c.chromosome().name()+" ("+c.chromosome().length()+")"));
						continue;
					}

					if (start < 1) {
						progressWarningReceived(new SeqMonkException("Reading start position "+start+" was less than 1"));
						continue;						
					}

					// We can now make the new reading
					try {
						long read = SequenceRead.packPosition(start,end,strand);
						for (int i=0;i<count;i++) {
							newData[f].addData(c.chromosome(),read);
						}
					}
					catch (SeqMonkException e) {
						progressWarningReceived(e);
						continue;
					}
//					System.out.println("Added probe "+newProbe.name()+" on "+newProbe.chromosome()+" at pos "+newProbe.position());
				}

				// We're finished with the file.
				br.close();
				
				// Cache the data in the new dataset
				progressUpdated("Caching data from "+probeFiles[f].getName(), f, probeFiles.length);
				newData[f].finalise();
			}

			processingFinished(newData);
		}
		catch (Exception ex) {
			progressExceptionReceived(ex);
			return;
		}
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Generic parser for any delimited text file";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		if (optionsPanel == null) {
			optionsPanel = new GenericSeqReadParserOptions();
		}
		return optionsPanel;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#hasOptionsPanel()
	 */
	public boolean hasOptionsPanel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#name()
	 */
	public String name() {
		return "Generic Text File Reader";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#readyToParse()
	 */
	public boolean readyToParse() {
		if (startColValue >=0 && endColValue >=0 && chrColValue >=0) {
			return true;
		}
	
		return false;
	}

	/**
	 * The GenericSeqReadParserOptions.
	 */
	private class GenericSeqReadParserOptions extends JPanel implements ActionListener {

		private JComboBox delimiters;
		private JComboBox startRow;
		private JComboBox chrCol;
		private JComboBox startCol;
		private JComboBox endCol;
		private JComboBox strandCol;
		private JComboBox countCol;
		private JTextField extendBy;
		private JCheckBox removeDuplicates;
		private JCheckBox hiCData;
		private JTextField hiCDistance;
		private String [] previewData = new String [50];
		private ProbeTableModel model;
		private JTable table;
		private JScrollPane tablePane = null;

		/**
		 * Instantiates a new generic seq read parser options.
		 */
		public GenericSeqReadParserOptions () {

			// We can now read the first 50 lines from the first file
			try {
				
				BufferedReader br;

				if (getFiles()[0].getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(getFiles()[0]))));	
				}
				else {
					br = new BufferedReader(new FileReader(getFiles()[0]));
				}
				
				int x=0;
				String line;
				while (true) {
					line = br.readLine();
					// We could (theoretically) have less than 50 lines of text
					if (line == null) line = "";
					if (x>49) break;
					previewData[x] = new String(line);
					x++;
				}
				br.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}


			setLayout(new BorderLayout());

			JPanel optionPanel = new JPanel();

			optionPanel.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.gridx=0;
			c.gridy=0;
			c.weightx=0.5;
			c.weighty=0.5;
			c.fill = GridBagConstraints.NONE;

			optionPanel.add(new JLabel("Column Delimiter"),c);
			delimiters = new JComboBox(new String [] {"Tab","Space","Comma"});
			delimiters.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(delimiters,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Start at Row"),c);

			String [] rowList = new String[51];
			for (int i=0;i<51;i++) {
				rowList[i] = ""+i;
			}

			startRow = new JComboBox(rowList);
			startRow.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(startRow,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Chr Col"),c);
			chrCol = new JComboBox();
			chrCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(chrCol,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Start Col"),c);
			startCol = new JComboBox();
			startCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(startCol,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("End Col"),c);
			endCol = new JComboBox();
			endCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(endCol,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Strand Col"),c);
			strandCol = new JComboBox();
			strandCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(strandCol,c);
			

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Count Col"),c);
			countCol = new JComboBox();
			countCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(countCol,c);
			
			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Extend by"),c);
			extendBy = new JTextField(5);
			extendBy.addKeyListener(new NumberKeyListener(false, false));
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(extendBy,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Remove duplicates"),c);
			removeDuplicates = new JCheckBox();
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(removeDuplicates,c);

			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Treat as HiC"),c);
			hiCData = new JCheckBox();
			hiCData.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					hiCDistance.setEnabled(hiCData.isSelected());
				}
			});
			
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(hiCData,c);
			
			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Min HiC Distance"),c);
			hiCDistance = new JTextField("1000");
			hiCDistance.addKeyListener(new NumberKeyListener(false, false));
			hiCDistance.setEnabled(false);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(hiCDistance,c);
			
			add(optionPanel,BorderLayout.EAST);
			model = new ProbeTableModel();

			updateTable();

		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(800,600);
		}
	
		/**
		 * Says whether the options panel was set to remove duplicate reads
		 * @return true if we should remove duplicates
		 */
		public boolean removeDuplicates () {
			return removeDuplicates.isSelected();
		}
		
		public boolean isHiC () {
			return hiCData.isSelected();
		}

		/**
		 * Says how much we should extend each read by
		 * @return the distance (in bp) by which the read should be extended
		 */
		public int extendBy () {
			int extend = 0;
			if (extendBy.getText().trim().length() > 0) {
				extend = Integer.parseInt(extendBy.getText().trim());
			}
			return extend;
		}
		
		/**
		 * Updates the preview table when new options have been set.
		 */
		private void updateTable () {

			// We need to rebuild the list of column numbers.
			String [] columnList = new String [model.getColumnCount()];
			columnList[0] = null;
			for (int i=1;i<model.getColumnCount();i++) {
				columnList[i] = ""+i;
			}
			chrCol.removeAllItems();
			startCol.removeAllItems();
			endCol.removeAllItems();
			strandCol.removeAllItems();
			countCol.removeAllItems();

			for (int i=0;i<columnList.length;i++) {
				chrCol.addItem(columnList[i]);
				startCol.addItem(columnList[i]);
				endCol.addItem(columnList[i]);
				strandCol.addItem(columnList[i]);
				countCol.addItem(columnList[i]);
			}
			chrCol.validate();
			startCol.validate();
			endCol.validate();
			strandCol.validate();
			countCol.validate();

			if (tablePane != null) {
				model.fireTableStructureChanged();
				model.fireTableDataChanged();
			}
			else {
				table = new JTable(model);
				TableCellRenderer r = new MyTableCellRenderer();
				table.setDefaultRenderer(Object.class, r);
				tablePane = new JScrollPane(table);
				add(tablePane,BorderLayout.CENTER);
				validate();
			}
		}

		/**
		 * Gets the delimiter between fields.
		 * 
		 * @return The delimiter
		 */
		private String getDelimiter () {
			if (((String)(delimiters.getSelectedItem())).equals("Tab")) {
				return "\t";
			}
			if (((String)(delimiters.getSelectedItem())).equals("Space")) {
				return " ";
			}
			if (((String)(delimiters.getSelectedItem())).equals("Comma")) {
				return ",";
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent ae) {

			if (ae.getSource() == delimiters) {
				delimitersValue = getDelimiter();
				updateTable();
				return;
			}
			else if (ae.getSource() == startRow) {
				startRowValue = Integer.parseInt((String)startRow.getSelectedItem());
				model.fireTableDataChanged();
				return;
			}
			else if (ae.getSource() == chrCol) {
				if (chrCol.getSelectedItem() == null) {
					chrColValue = -1;
				}
				else {
					chrColValue = Integer.parseInt((String)chrCol.getSelectedItem())-1;
				}
			}
			else if (ae.getSource() == startCol) {
				if (startCol.getSelectedItem() == null) {
					startColValue = -1;
				}
				else {
					startColValue = Integer.parseInt((String)startCol.getSelectedItem())-1;
				}
			}
			else if (ae.getSource() == endCol) {
				if (endCol.getSelectedItem() == null) {
					endColValue = -1;
				}
				else {
					endColValue = Integer.parseInt((String)endCol.getSelectedItem())-1;
				}
			}
			else if (ae.getSource() == strandCol) {
				if (strandCol.getSelectedItem() == null) {
					strandColValue = -1;
				}
				else {
					strandColValue = Integer.parseInt((String)strandCol.getSelectedItem())-1;
				}
				if (strandColValue >=0) {
					useStrand = true;
				}
				else {
					useStrand = false;
				}
			}

			else if (ae.getSource() == countCol) {
				if (countCol.getSelectedItem() == null) {
					strandColValue = -1;
					hiCData.setEnabled(true);
				}
				else {
					countColValue = Integer.parseInt((String)countCol.getSelectedItem())-1;
					hiCData.setSelected(false);
					hiCData.setEnabled(false);
				}
			}

		
		}


		/**
		 * The ProbeTableModel.
		 */
		private class ProbeTableModel extends AbstractTableModel {

			/* (non-Javadoc)
			 * @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				int startRowValue = 1;
				if (startRow.getSelectedItem()!=null) {
					startRowValue = Integer.parseInt((String)startRow.getSelectedItem());
				}

				if (startRowValue < 1 || startRowValue > 50) {
					startRowValue = 1;
				}

				return 51-startRowValue;
			}

			/* (non-Javadoc)
			 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
			 */
			public String getColumnName (int column) {
				if (column==0) {
					return "Row number";
				}
				else {
					return "Col "+column;
				}
			}

			/* (non-Javadoc)
			 * @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				int max = 1;
				for (int i=0;i<previewData.length;i++) {
					String [] sections = previewData[i].split(delimitersValue);
					if (sections.length > max) {
						max = sections.length;
					}
				}
				return max+1;
			}



			/* (non-Javadoc)
			 * @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int r, int c) {

				if (c==0) {
					return ""+r;
				}

				c-=1;

				int startRowValue = 1;
				//			if (startRow.getText().length() > 0) {
				//				startRowValue = Integer.parseInt(startRow.getText());
				//			}
				//			
				//			if (startRowValue < 1 || startRowValue > 50) {
				//				startRowValue = 1;
				//			}


				String [] sections = previewData[r+(startRowValue-1)].split("["+getDelimiter()+"]");
				if (sections.length <=c) {
					return null;
				}
				else {
					return sections[c];
				}
			}
		}

		/**
		 * MyTableCellRenderer is used to shade parts of the data which will be skipped
		 * when importing.
		 */
		private class MyTableCellRenderer extends DefaultTableCellRenderer {

			// The only point of this class is so we can tell which rows we are ignoring.
			// We do this by selcting the ignored ones so they go grey.

			/* (non-Javadoc)
			 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
			 */
			public Component getTableCellRendererComponent (JTable table,Object value,boolean isSelected, boolean hasFocus, int r, int c) {

				if (r<Integer.parseInt((String)startRow.getSelectedItem())) {
					isSelected = true;
				}
				else {
					isSelected = false;
				}
				Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, r, c);

				return cell;
			}

		}

	}
}