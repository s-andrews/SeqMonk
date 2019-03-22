/**
 * Copyright Copyright 2010-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.AnnotationParsers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.AnnotationSet;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Feature;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Location;
import uk.ac.babraham.SeqMonk.Utilities.ChromosomeWithOffset;

/**
 * Provides a flexible import mechanism for delimited text files which
 * contain all of the required data for a read but aren't in an
 * otherwise recognised format.
 */
public class GenericAnnotationParser extends AnnotationParser {

	private String delimitersValue = "\t";
	private static int startRowValue = 0;
	private static int chrColValue = -1;
	private static int startColValue = -1;
	private static int endColValue = -1;
	private static int strandColValue = -1;
	private static int typeColValue = -1;
	private static int nameColValue = -1;
	private static int descriptionColValue = -1;
	private String featureType = null;
	private boolean useStrand = false;
	private JDialog options = null;
	private File file;

	/**
	 * Instantiates a new generic seq read parser.
	 * 
	 * @param data The dataCollection to which new data will be added.
	 */
	public GenericAnnotationParser (Genome genome) {
		super(genome);
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#fileFilter()
	 */
	public FileFilter fileFilter () {
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
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#parseAnnotation(java.io.File, uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome)
	 */
	public AnnotationSet [] parseAnnotation (File file, Genome genome) throws Exception {
		this.file = file;
		
		if (options == null) {
			options = new JDialog(SeqMonkApplication.getInstance());
			options.setModal(true);
			options.setContentPane(new GenericAnnotationParserOptions(options));
			options.setSize(700,400);
			options.setLocationRelativeTo(null);
		}

		// We have to set cancel to true as a default so we don't try to 
		// proceed with processing if the user closes the options using
		// the X on the window.

		options.setTitle("Format for "+file.getName()+"...");
		
		cancel = true;
		options.setVisible(true);
				
		if (cancel) {
			progressCancelled();
			return null;
		}
		
		// We keep track of how many types have been added
		// to catch if someone sets the wrong field and makes
		// loads of different feature types.
		HashSet<String>addedTypes = new HashSet<String>();
		
		BufferedReader br;

		if (file.getName().toLowerCase().endsWith(".gz")) {
			br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));	
		}
		else {
			br = new BufferedReader(new FileReader(file));
		}

		
		String line;
		// First skip the header lines
		for (int i=0;i<startRowValue;i++) {
			line = br.readLine();
			if (line == null) {
				throw new Exception ("Ran out of file before skipping all of the header lines");
			}
		}

		int maxIndexValue = 0;
		if (chrColValue > maxIndexValue) maxIndexValue = chrColValue;
		if (startColValue > maxIndexValue) maxIndexValue = startColValue;
		if (endColValue > maxIndexValue) maxIndexValue = endColValue;
		if (strandColValue > maxIndexValue) maxIndexValue = strandColValue;
		if (typeColValue > maxIndexValue) maxIndexValue = typeColValue;
		if (nameColValue > maxIndexValue) maxIndexValue = nameColValue;
		if (descriptionColValue > maxIndexValue) maxIndexValue = descriptionColValue;


		Vector<AnnotationSet> annotationSets = new Vector<AnnotationSet>();

		AnnotationSet currentAnnotation = new AnnotationSet(genome, file.getName());
		annotationSets.add(currentAnnotation);

		int lineCount = 0;
		// Now process the rest of the file
		while ((line = br.readLine())!= null) {
			++lineCount;

			if (cancel) {
				progressCancelled();
				return null;
			}

			if (lineCount%1000 == 0) {
				progressUpdated("Read "+lineCount+" lines from "+file.getName(),0,1);
			}
			
			if (lineCount>1000000 && lineCount%1000000 == 0) {
				progressUpdated("Caching...",0,1);
				currentAnnotation.finalise();
				currentAnnotation = new AnnotationSet(genome, file.getName()+"["+annotationSets.size()+"]");
				annotationSets.add(currentAnnotation);
				
			}

			String [] sections = line.split(delimitersValue,-1);

			// Check to see if we've got enough data to work with
			if (maxIndexValue >= sections.length) {
				progressWarningReceived(new SeqMonkException("Not enough data ("+sections.length+") to get a probe name on line '"+line+"'"));
				continue; // Skip this line...						
			}

			int strand;
			int start;
			int end;

			try {

				start = Integer.parseInt(sections[startColValue]);
				end = Integer.parseInt(sections[endColValue]);

				// End must always be later than start
				if (end < start) {
					int temp = start;
					start = end;
					end = temp;
				}

				if (useStrand) {
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
			}
			catch (NumberFormatException e) {
				progressWarningReceived(new SeqMonkException("Location "+sections[startColValue]+"-"+sections[endColValue]+" was not an integer"));
				continue;
			}

			ChromosomeWithOffset c;

			try {
				c = genome.getChromosome(sections[chrColValue]);
			}
			catch (IllegalArgumentException sme) {
				progressWarningReceived(sme);
				continue;
			}

			end = c.position(end);
			start = c.position(start);
			
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
			
			// Now we can add the type, name and description
			String type;
			
			// If there's a column containing the type then use that
			if (typeColValue >=0 ){
				type = sections[typeColValue];
			}
			
			// If not then use the manually specified type if they set it
			else if (featureType != null && featureType.length() > 0){
				type = featureType;
			}
			
			// If all else fails use the file name as the type
			else {
				type = file.getName();
			}
			
			if (!addedTypes.contains(type)) {
				addedTypes.add(type);
				
				if (addedTypes.size() > 100) {
					throw new SeqMonkException("More than 100 different types of feature added - you don't want to do this!");
				}
			}
			
			String name = null;
			
			if (nameColValue >=0) {
				name = sections[nameColValue];
			}
			
			String description = null;
			if (descriptionColValue >=0) {
				description = sections[descriptionColValue];
			}
			
			// We can now make the new annotation
			Feature feature = new Feature(type,c.chromosome().name());
			if (name != null) {
				feature.addAttribute("name", name);
			}
			if (description != null) feature.addAttribute("description", description);
			feature.setLocation(new Location(start,end,strand));
			currentAnnotation.addFeature(feature);					
			//				System.out.println("Added probe "+newProbe.name()+" on "+newProbe.chromosome()+" at pos "+newProbe.position());
		}

		// We're finished with the file.
		br.close();
		
		options.dispose();
		options = null;

		return annotationSets.toArray(new AnnotationSet[0]);

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.DataParsers.DataParser#description()
	 */
	public String description() {
		return "Generic parser for any delimited text file";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#name()
	 */
	public String name() {
		return "Generic annotation parser";
	}


	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.AnnotationParsers.AnnotationParser#requiresFile()
	 */
	public boolean requiresFile() {
		return true;
	}


	/**
	 * The GenericSeqReadParserOptions.
	 */
	private class GenericAnnotationParserOptions extends JPanel implements ActionListener, KeyListener {

		private JComboBox delimiters;
		private JComboBox startRow;
		private JComboBox chrCol;
		private JComboBox startCol;
		private JComboBox endCol;
		private JComboBox strandCol;
		private JComboBox typeCol;
		private JTextField forcedTypeField;
		private JComboBox nameCol;
		private JComboBox descriptionCol;
		private JButton continueButton;
		private String [] previewData = new String [50];
		private ProbeTableModel model;
		private JTable table;
		private JScrollPane tablePane = null;
		private JDialog dialog;

		/**
		 * Instantiates a new generic seq read parser options.
		 */
		public GenericAnnotationParserOptions (JDialog dialog) {
			this.dialog = dialog;

			// We can now read the first 50 lines from the first file
			try {
				
				BufferedReader br;

				if (file.getName().toLowerCase().endsWith(".gz")) {
					br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));	
				}
				else {
					br = new BufferedReader(new FileReader(file));
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
			delimiters = new JComboBox(new String [] {"Tab","Space","Comma","Whitespace"});
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
			optionPanel.add(new JLabel("Type Col"),c);
			typeCol = new JComboBox();
			typeCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(typeCol,c);
			
			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Type"),c);
			forcedTypeField = new JTextField(5);
			forcedTypeField.addKeyListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(forcedTypeField,c);

			
			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Name Col"),c);
			nameCol = new JComboBox();
			nameCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(nameCol,c);
			
			c.gridx=0;
			c.weightx=0.5;
			c.gridy++;
			optionPanel.add(new JLabel("Description Col"),c);
			descriptionCol = new JComboBox();
			descriptionCol.addActionListener(this);
			c.gridx=1;
			c.weightx=0.1;
			optionPanel.add(descriptionCol,c);
			
			add(optionPanel,BorderLayout.EAST);
			model = new ProbeTableModel();

			updateTable();
			
			JPanel buttonPanel = new JPanel();
			
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setActionCommand("cancel");
			cancelButton.addActionListener(this);
			buttonPanel.add(cancelButton);
			
			continueButton = new JButton("Continue");
			continueButton.setActionCommand("continue");
			continueButton.addActionListener(this);
			continueButton.setEnabled(false);
			buttonPanel.add(continueButton);
			
			setDefault(startRow, startRowValue);
			setDefault(chrCol, chrColValue+1);
			setDefault(descriptionCol, descriptionColValue+1);
			setDefault(nameCol, nameColValue+1);
			setDefault(typeCol, typeColValue+1);
			setDefault(strandCol, strandColValue+1);
			setDefault(startCol, startColValue+1);
			setDefault(endCol, endColValue+1);
			
			add(buttonPanel,BorderLayout.SOUTH);

		}
		
		private void setDefault (JComboBox box, int value) {
			String text = ""+value;
			for (int i=0;i<box.getItemCount();i++) {
				if (value == 0 && box.getItemAt(i) == null) {
					box.setSelectedIndex(i);
					return;
				}
				if (box.getItemAt(i) != null && box.getItemAt(i).equals(text)) {
					box.setSelectedIndex(i);
					return;
				}
			}
			
			// If we get here then there was no match so we explicitly set the
			// first value to trigger an update to the appropriate value
			box.setSelectedIndex(0);
			
			
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(800,600);
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
			typeCol.removeAllItems();
			nameCol.removeAllItems();
			descriptionCol.removeAllItems();

			for (int i=0;i<columnList.length;i++) {
				chrCol.addItem(columnList[i]);
				startCol.addItem(columnList[i]);
				endCol.addItem(columnList[i]);
				strandCol.addItem(columnList[i]);
				typeCol.addItem(columnList[i]);
				nameCol.addItem(columnList[i]);
				descriptionCol.addItem(columnList[i]);
			}
			chrCol.validate();
			startCol.validate();
			endCol.validate();
			strandCol.validate();
			typeCol.validate();
			nameCol.validate();
			descriptionCol.validate();

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
			if (((String)(delimiters.getSelectedItem())).equals("Whitespace")) {
				return "\\s+";
			}

			throw new IllegalArgumentException("Unknown delimiter option selected '"+delimiters.getSelectedItem()+"'");
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent ae) {

			if (ae.getActionCommand().equals("continue")) {
				System.out.println("Setting user cancelled to false");
				cancel = false;
				dialog.setVisible(false);

				// Don't dispose() here as it messes up the signal we get at the
				// calling end.  We need to extract the data from the preferences
				// window before we dispose of it.
				return;
			}

			if (ae.getActionCommand().equals("cancel")) {
				cancel = true;
				dialog.setVisible(false);
				// Don't dispose() here as it messes up the signal we get at the
				// calling end.  We need to extract the data from the preferences
				// window before we dispose of it.
				return;
			}

			
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
			else if (ae.getSource() == typeCol) {
				if (typeCol.getSelectedItem() == null) {
					typeColValue = -1;
					forcedTypeField.setEnabled(true);
				}
				else {
					typeColValue = Integer.parseInt((String)typeCol.getSelectedItem())-1;
					forcedTypeField.setEnabled(false);
				}
			}
			else if (ae.getSource() == nameCol) {
				if (nameCol.getSelectedItem() == null) {
					nameColValue = -1;
				}
				else {
					nameColValue = Integer.parseInt((String)nameCol.getSelectedItem())-1;
				}
			}
			else if (ae.getSource() == descriptionCol) {
				if (descriptionCol.getSelectedItem() == null) {
					descriptionColValue = -1;
				}
				else {
					descriptionColValue = Integer.parseInt((String)descriptionCol.getSelectedItem())-1;
				}
			}
			else if (ae.getSource() == strandCol) {
				if (strandCol.getSelectedItem() == null) {
					strandColValue = -1;
					useStrand = false;
				}
				else {
					strandColValue = Integer.parseInt((String)strandCol.getSelectedItem())-1;
					useStrand = true;
				}
			}
			
			if (chrColValue >=0 && startColValue >=0 && endColValue >=0) {
				continueButton.setEnabled(true);
			}
			else {
				continueButton.setEnabled(false);
			}
			
		}

		public void keyPressed(KeyEvent e) {}

		public void keyReleased(KeyEvent e) {
			featureType = forcedTypeField.getText().trim();
		}

		public void keyTyped(KeyEvent e) {}


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


				String [] sections = previewData[r+(startRowValue-1)].split(getDelimiter());
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