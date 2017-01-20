/**
 * Copyright 2014-17 Laura Biggins
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
package uk.ac.babraham.SeqMonk.Filters.GeneSetFilter;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.MappedGeneSetTTestValue;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.ScatterPlot.ScatterPlotPanel;
import uk.ac.babraham.SeqMonk.Filters.GeneSetFilter.GeneSetScatterPlotPanel;


import uk.ac.babraham.SeqMonk.Preferences.SeqMonkPreferences;
import uk.ac.babraham.SeqMonk.Utilities.ImageSaver.ImageSaver;

import com.sun.java.TableSorter;


	
	/**
	 * This is the intermediate display that holds the table of results and the scatterplot.
	 * 
	 * @author bigginsl
	 *
	 */
public class GeneSetDisplay extends JDialog implements ListSelectionListener, MouseListener,  ActionListener, ChangeListener, WindowListener {
	
	/** The collection. */
	protected DataCollection collection;
	
	/** The table model */
	GeneSetTableModel tableModel;

	/** The main pane that holds the scatter plot and results table */
	JSplitPane mainPane;
	
	/** The table of results */
	private JTable table;
	
	/** The results generated from the geneSetIntensityFilter */
	private MappedGeneSetTTestValue [] filterResultsPVals;
	
	/** The highlighted probe list to display on the scatterplot, needs to be an array to be compatible with the original scatterPanel */
	protected ProbeList [] currentSelectedProbeList = null; 
	
	/** the plotting area which holds the scatterplot panel and slider */
	JPanel plotPanel;
	
	/** The scatter plot panel. */
	private JPanel scatterPlotPanel;
	
	/** The dot size slider. */
	private JSlider dotSizeSlider;	
	
	private JButton saveImageButton;
	
	private JToggleButton swapPlotButton; 

	/** selectAllProbeListsButton */
	private JToggleButton selectAllButton;
	
	/** saveSelectedProbeListsButton */
	private JButton saveSelectedProbeListsButton;	
	
	/** save table button */
	private JButton saveTableButton;
	
	/** close button */
	private JButton closeButton;
	
	/** The starting probelist that we need to use as the parent for saving new probelists*/
	private ProbeList startingProbeList;
	
	/** description of filter run */
	private String description;
	
	/** from store */
	private DataStore fromStore;
	
	/** to store */
	private DataStore toStore;
	
	/** all the valid starting probes */ 
	private Probe[] probes;
	
	/** hashtable containing probes and their z-scores */
	private Hashtable zScoreLookupTable;
	
	private float [][] customRegressionValues;
	
	private SimpleRegression simpleRegression;
	
	
	public GeneSetDisplay (DataCollection dataCollection, String description, DataStore fromStore, DataStore toStore, Probe[] probes, Hashtable zScoreLookupTable, 
			MappedGeneSetTTestValue [] filterResults,  ProbeList startingProbeList, float[][] customRegressionValues, SimpleRegression simpleRegression){

		super(SeqMonkApplication.getInstance(),"Gene Set Results");
		this.collection = dataCollection;	
		this.filterResultsPVals = filterResults;
		this.startingProbeList = startingProbeList;
		this.description = description;
		this.fromStore = fromStore;
		this.toStore = toStore;
		this.probes = probes;
		this.zScoreLookupTable = zScoreLookupTable;
		this.customRegressionValues = customRegressionValues;
		this.simpleRegression = simpleRegression;
		this.addWindowListener(this);
		
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		
		// This is to hold the scatterPanel and its associated bits.
		plotPanel = new JPanel();		
		plotPanel.setLayout(new BorderLayout());

		scatterPlotPanel = new JPanel();
		plotPanel.add(scatterPlotPanel,BorderLayout.CENTER);			
		
		dotSizeSlider = new JSlider(JSlider.VERTICAL,1,100,3);

		// This call is left in to work around a bug in the Windows 7 LAF
		// which makes the slider stupidly thin in ticks are not drawn.
		dotSizeSlider.setPaintTicks(true);
		dotSizeSlider.addChangeListener(this);
		plotPanel.add(dotSizeSlider,BorderLayout.EAST);
		
		JPanel plotButtonPanel = new JPanel();
		plotButtonPanel.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.insets = new Insets(2,2,2,2);
		c1.gridx=0;
		c1.gridy=0;
		
		saveImageButton = new JButton("Save Image");		
		saveImageButton.addActionListener(this);
		saveImageButton.setActionCommand("save_image");
		
		plotButtonPanel.add(saveImageButton, c1);
		
		c1.gridx++;
		swapPlotButton = new JToggleButton("Display standard scatterplot");		
		swapPlotButton.addActionListener(this);
		swapPlotButton.setActionCommand("swap_plot");
		
		plotButtonPanel.add(swapPlotButton, c1);		
		
		plotPanel.add(plotButtonPanel,BorderLayout.SOUTH);
		
		/** The table where the probe lists are displayed */
		tableModel = new GeneSetTableModel(filterResultsPVals);	
		table = new JTable(new TableSorter(tableModel));
		table.setRowSelectionAllowed(true);	
		table.addMouseListener(this);					
		table.setAutoCreateRowSorter(true);
		table.getSelectionModel().addListSelectionListener(this);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// sort by z-score
		table.getRowSorter().toggleSortOrder(3);
					
		// Set our initial column widths
		TableColumn column = null;
		for (int i = 0; i < tableModel.getColumnCount(); i++) {
		    column = table.getColumnModel().getColumn(i);
		    if (i == 0) {
		        column.setPreferredWidth(40);
		    } 
		    else if(i ==1){
		    	column.setPreferredWidth(200);
		    }       			    	
	    	else {
		       column.setPreferredWidth(80);
		    }
		}
		
		JScrollPane scrollPane = new JScrollPane(table);
		JPanel tablePanel = new JPanel();
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridBagLayout());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.insets = new Insets(2,2,2,2);
		c2.gridx=0;
		c2.gridy=0;
		
		selectAllButton = new JToggleButton("Select all");		
		selectAllButton.addActionListener(this);
		selectAllButton.setActionCommand("select_all");
		
		buttonPanel.add(selectAllButton, c2);
		
		saveSelectedProbeListsButton = new JButton("Save selected probe lists");
		saveSelectedProbeListsButton.addActionListener(this);
		saveSelectedProbeListsButton.setActionCommand("save_selected_probelists");
		c2.gridx++;
		c2.gridx++;
		buttonPanel.add(saveSelectedProbeListsButton, c2);
		
		saveTableButton = new JButton("Save table");
		saveTableButton.addActionListener(this);
		saveTableButton.setActionCommand("save_table");
		c2.gridx++;
		c2.gridx++;
		buttonPanel.add(saveTableButton, c2);
		
		
		closeButton = new JButton("Close");
		closeButton.addActionListener(this);
		closeButton.setActionCommand("close");
		c2.gridx++;
		buttonPanel.add(closeButton, c2);
					
		tablePanel.setLayout(new BorderLayout());
		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(buttonPanel, BorderLayout.SOUTH);
		
		// sort this out 
		mainPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);			
		mainPane.setLeftComponent(plotPanel);
		mainPane.setRightComponent(tablePanel);
		
		mainPane.setResizeWeight(0.6);
		mainPane.setDividerLocation(500);
		
		getContentPane().add(BorderLayout.CENTER, mainPane);
				
		if (storesQuantitated()){
			
			plotPanel.remove(scatterPlotPanel);
			scatterPlotPanel = new ZScoreScatterPlotPanel(fromStore,toStore,probes,currentSelectedProbeList,dotSizeSlider.getValue(),zScoreLookupTable);
			plotPanel.add(scatterPlotPanel,BorderLayout.CENTER); 
		}

		setSize(1000, 500);
		setVisible(true);
		
	}	

	public void actionPerformed(ActionEvent ae) {
		
	/*	if (ae.getActionCommand().equals("plot")) {

			drawScatterPlot();			
		}
	*/	
		if (ae.getActionCommand().equals("save_image")){
			ImageSaver.saveImage(scatterPlotPanel);
		}
		
		else if (ae.getActionCommand().equals("swap_plot")){
			
			if (storesQuantitated()){
			
				plotPanel.remove(scatterPlotPanel);
				
				if (scatterPlotPanel instanceof GeneSetScatterPlotPanel) {
					scatterPlotPanel = new ZScoreScatterPlotPanel(fromStore,toStore,probes,currentSelectedProbeList,dotSizeSlider.getValue(),zScoreLookupTable);
					plotPanel.add(scatterPlotPanel,BorderLayout.CENTER); 
					swapPlotButton.setText("Display standard scatterplot");
				}
				else if (scatterPlotPanel instanceof ZScoreScatterPlotPanel) {
					scatterPlotPanel = new GeneSetScatterPlotPanel(fromStore,toStore,startingProbeList,currentSelectedProbeList,true,dotSizeSlider.getValue(),customRegressionValues, simpleRegression);
					plotPanel.add(scatterPlotPanel,BorderLayout.CENTER); 					
					swapPlotButton.setText("Display z-score plot");
				}
			}	
		}
		
		else if(ae.getActionCommand().equals("close")) {
			
		/*	if(currentSelectedProbeList != null){
				currentSelectedProbeList[0].delete();
				//currentSelectedProbeList = null;
				
			}
		*/	this.dispose();
			
		}
		else if(ae.getActionCommand().equals("select_all")){
			
			if(selectAllButton.isSelected()){
				
				for(int i=0; i<tableModel.selected.length; i++) {
					
					tableModel.selected[i] = true;	
					
					tableModel.fireTableCellUpdated(i, 0);
				}					
				selectAllButton.setText("deselect all");
			}
			else{
				
				for(int i=0; i<tableModel.selected.length; i++) {
					
					tableModel.selected[i] = false;	
					tableModel.fireTableCellUpdated(i, 0);
				}					
				selectAllButton.setText("select all");
			}
			
		}
		
		else if (ae.getActionCommand().equals("save_selected_probelists")){
			
			boolean [] selectedListsBoolean = tableModel.selected;
			
			if (selectedListsBoolean.length != filterResultsPVals.length){
				System.err.println("not adding up here");
			}
			
			else{										
				
				ArrayList <MappedGeneSetTTestValue> selectedListsArrayList = new ArrayList<MappedGeneSetTTestValue>();
				
				for (int i = 0; i < selectedListsBoolean.length; i++){
					
					if(selectedListsBoolean[i] == true){
						selectedListsArrayList.add(filterResultsPVals[i]);
					}	
				}
				
				MappedGeneSetTTestValue [] selectedLists = selectedListsArrayList.toArray(new MappedGeneSetTTestValue[0]);				

				if (selectedLists.length == 0){
					
					JOptionPane.showMessageDialog(SeqMonkApplication.getInstance(), "No probe lists were selected", "No probe lists selected", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
									
				saveProbeLists(selectedLists);	
				
				if(currentSelectedProbeList != null){
					currentSelectedProbeList[0].delete();
					currentSelectedProbeList = null;
				}					
			}				
		}
		
		else if (ae.getActionCommand().equals("save_table")){
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
				for (int c=1;c<colCount;c++) {
					b.append(model.getColumnName(c));
					if (c+1 != colCount) {
						b.append("\t");
					}
				}
				
				p.println(b);
				
				for (int r=0;r<rowCount;r++) {
					b = new StringBuffer();
					for (int c=1;c<colCount;c++) {
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
		
		else {
			throw new IllegalArgumentException("Unknown command "+ae.getActionCommand());
		}
	}

	private String getDescription(MappedGeneSetTTestValue geneSetTTestValue){
		
		StringBuffer b = new StringBuffer();

		b.append(geneSetTTestValue.mappedGeneSet.name());
		b.append(" contains ");
		b.append(geneSetTTestValue.mappedGeneSet.getProbes().length);
		b.append(" matched probes, mean z-score = ");
		b.append(geneSetTTestValue.mappedGeneSet.meanZScore);
		b.append(", p value = ");
		b.append((double)Math.round(geneSetTTestValue.p * 100000)/100000);
		
		if(geneSetTTestValue.q != null){
			b.append(", adjusted p value = ");
			b.append((double)Math.round(geneSetTTestValue.q * 100000)/100000);			
		}
		return b.toString();
	}
	
	private void saveProbeLists(MappedGeneSetTTestValue [] selectedLists){
		
		ProbeList parentProbeList = new ProbeList(startingProbeList, "gene set filter results", description,"z score");
		
		for (int i = 0; i < selectedLists.length; i++){
			
			String childDescription = getDescription(selectedLists[i]);
		
			ProbeList newProbeList = new ProbeList(parentProbeList, selectedLists[i].mappedGeneSet.name(), childDescription,"z score");
			Probe [] tempProbes = selectedLists[i].mappedGeneSet.getProbes();
			
			for (int j=0; j<tempProbes.length; j++){	
				
				parentProbeList.addProbe(tempProbes[j], (float)(selectedLists[i].mappedGeneSet.zScores[j]));
				newProbeList.addProbe(tempProbes[j], (float)(selectedLists[i].mappedGeneSet.zScores[j]));														
			}
		}
		
		String groupName=null;
		while (true) {
			groupName = (String)JOptionPane.showInputDialog(this,"Enter list name","Found "+parentProbeList.getAllProbes().length+" probes",JOptionPane.QUESTION_MESSAGE,null,null,parentProbeList.name());
			if (groupName == null){
				// Since the list will automatically have been added to
				// the ProbeList tree we actively need to delete it if
				// they choose to cancel at this point.
				parentProbeList.delete();
				return;  // They cancelled
			}			
				
			if (groupName.length() == 0)
				continue; // Try again
			
			break;
		}
		parentProbeList.setName(groupName);		
		
	}
	
/*	private void drawScatterPlot(){
					
		DataStore xStore = fromStore;
		DataStore yStore = toStore;
		plotPanel.remove(scatterPlotPanel);

		// Check if these stores are quantitated
		if (!xStore.isQuantitated()) {
			JOptionPane.showMessageDialog(this, xStore.name()+" is not quantitated", "Can't make plot", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (! yStore.isQuantitated()) {
			JOptionPane.showMessageDialog(this, yStore.name()+" is not quantitated", "Can't make plot", JOptionPane.INFORMATION_MESSAGE);				
		}
		else {
			scatterPlotPanel = new ZScoreScatterPlotPanel(xStore,yStore,probes,currentSelectedProbeList,dotSizeSlider.getValue(),zScoreLookupTable);
			//scatterPlotPanel = new ZScoreScatterPlotPanel(xStore,yStore,startingProbeList,currentSelectedProbeList,false,dotSizeSlider.getValue(),geneSetFilter.probeZScoreLookupTable);
			//scatterPlotPanel = new GeneSetScatterPlotPanel(xStore,yStore,startingProbeList,currentSelectedProbeList,true,dotSizeSlider.getValue(),geneSetFilter.customRegressionValues, geneSetFilter.simpleRegression);
			plotPanel.add(scatterPlotPanel,BorderLayout.CENTER);
		}
		validate();				
	}
*/	
	
	private boolean storesQuantitated(){
		
		if (!fromStore.isQuantitated()) {
			JOptionPane.showMessageDialog(this, fromStore.name()+" is not quantitated", "Can't make plot", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		else if (! toStore.isQuantitated()) {
			JOptionPane.showMessageDialog(this, toStore.name()+" is not quantitated", "Can't make plot", JOptionPane.INFORMATION_MESSAGE);
			return false;
		}
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent ce) {

		// The dot size slider has been moved
		if (scatterPlotPanel instanceof ScatterPlotPanel) {
			((ScatterPlotPanel) scatterPlotPanel).setDotSize(dotSizeSlider.getValue());
		}
		else if (scatterPlotPanel instanceof ZScoreScatterPlotPanel) {
			((ZScoreScatterPlotPanel) scatterPlotPanel).setDotSize(dotSizeSlider.getValue());
		}
	}		
	
		
	public void valueChanged(ListSelectionEvent ae) {
					
		int viewRow = table.getSelectedRow();
		
		if(viewRow >= 0){
		
			int modelRow = table.convertRowIndexToModel(viewRow);
			
			ProbeList tempProbeList = new ProbeList(startingProbeList, filterResultsPVals[modelRow].mappedGeneSet.name(), "temp list","z score");
			Probe [] tempProbes = filterResultsPVals[modelRow].mappedGeneSet.getProbes();
			
			for (int j=0; j<tempProbes.length; j++){	
				tempProbeList.addProbe(tempProbes[j], (float) 0);			
			}
			
			if(currentSelectedProbeList != null){
				currentSelectedProbeList[0].delete();
			}

			currentSelectedProbeList = new ProbeList[1]; 
			currentSelectedProbeList[0] = tempProbeList;
		}
		else {
			currentSelectedProbeList = null;
		}
		
		if (scatterPlotPanel instanceof GeneSetScatterPlotPanel) {
			System.err.println("trying to update the scatter plot");
			plotPanel.remove((GeneSetScatterPlotPanel)scatterPlotPanel);
			
			//plotPanel.repaint();
			scatterPlotPanel = new GeneSetScatterPlotPanel(fromStore,toStore,startingProbeList,currentSelectedProbeList,true,dotSizeSlider.getValue(),customRegressionValues, simpleRegression);
			//here 
			plotPanel.add(scatterPlotPanel,BorderLayout.CENTER); 
			plotPanel.revalidate();
			plotPanel.repaint();
			
		}
		else if (scatterPlotPanel instanceof ZScoreScatterPlotPanel) {
			((ZScoreScatterPlotPanel) scatterPlotPanel).setSubLists(currentSelectedProbeList);
		}
		
		// Check to see if we can add anything...
		//if (availableList.getSelectedIndices().length>0) {
		//	System.out.println("Selected index = " + availableList.getSelectedIndices().toString());
		//}
	}
	
	public void mouseClicked(MouseEvent me) {
				
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
	
	protected class GeneSetTableModel extends AbstractTableModel {

		private MappedGeneSetTTestValue [] filterResults;
		
		private boolean [] selected; 
		
		private boolean multipleTestingCorrection;
		
		/**
		 * 
		 * @param list The starting probe list
		 */
		public GeneSetTableModel (MappedGeneSetTTestValue [] filterResults) {			
			
			System.out.println("trying to create new GeneSetTableModel with " + filterResults.length + " pseudo probeLists");
			
			this.filterResults = filterResults;
							
			this.selected = new boolean[filterResults.length];
			
			if(filterResults.length > 0){
				if(filterResults[0].q != null){
					multipleTestingCorrection = true;
				}
				else{
					multipleTestingCorrection = false;
				}
			}	
		}
		
		public boolean isCellEditable(int r, int c){
			
			if(c == 0){
				return true;
			}
			return false;			
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount() {
			return filterResults.length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount() {
			
			if(multipleTestingCorrection){
				return 7;
			}
			return 6;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		public String getColumnName (int c) {
			switch (c) {
			case 0: return "select";
			case 1: return "Term";
			case 2: return "No of probes";
			case 3: return "mean z-score";
			case 4: return "mean abs z-score";
			//case 3: return "median z-score";
			//case 4: return "median abs z-score";
			case 5: return "p-value";
			case 6: return "adj p-value";
			
			default: return "couldn't get the column name";
			}
		}
		
		// sort the initial view by mean absolute zScore 
	//	public (MappedGeneSetTTestValue [] sortByZscore(MappedGeneSetTTestValue [] unsorted){
											
	//	}
		
		 public void setValueAt(Object value, int row, int col) {
	         
			 selected[row] = (Boolean) value;

	         fireTableCellUpdated(row, col);
	     }
		
		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
		 */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class getColumnClass (int c) {
			switch (c) {
			case 0: return Boolean.class;
			case 1: return String.class;
			case 2: return Integer.class;
			case 3: return Float.class;
			case 4: return Float.class;	
			case 5: return Float.class;	
			case 6: return Float.class;	
			default: return Boolean.class;
			}
		}


		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int r, int c) {
			switch (c) {
			
			case 0:
				return new Boolean(selected[r]);					
			case 1 :
				return filterResults[r].mappedGeneSet.name();
			case 2:
				return new Integer(filterResults[r].mappedGeneSet.getProbes().length);
			case 3:
				return new Float(filterResults[r].mappedGeneSet.meanZScore);
			case 4:
				return new Float(Math.abs(filterResults[r].mappedGeneSet.meanZScore));	
			case 5:
				return new Float(filterResults[r].p);
			case 6:
				return new Float(filterResults[r].q);	

			}
			throw new IllegalStateException("shouldn't have a row that doesn't ...");
		}
	}

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
		
		if(currentSelectedProbeList != null){
			if(currentSelectedProbeList[0] != null){
		
				currentSelectedProbeList[0].delete();
				currentSelectedProbeList = null;
			}	
		}	
		// TODO Auto-generated method stub
	}

	public void windowClosing(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
