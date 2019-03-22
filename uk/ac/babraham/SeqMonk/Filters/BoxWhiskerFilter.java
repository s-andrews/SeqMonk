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
package uk.ac.babraham.SeqMonk.Filters;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.Analysis.Statistics.BoxWhisker;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Dialogs.Renderers.TypeColourRenderer;
import uk.ac.babraham.SeqMonk.Utilities.ListDefaultSelector;

/**
 * Filters probes which are outliers from a BoxWhisker plot
 */
public class BoxWhiskerFilter extends ProbeFilter {

	private final BoxWhiskerOptionsPanel options;
	private DataStore [] stores;
	private boolean useUpper = true;
	private boolean useLower = true;
	private double stringency;;
	private int filterType;
	private int storeCutoff;
	public static final int ABOVE_ONLY = 10;
	public static final int BELOW_ONLY = 11;
	public static final int EITHER_ABOVE_OR_BELOW = 12;
	
	/**
	 * Instantiates a new box whisker filter.
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection is not quantiated
	 */
	public BoxWhiskerFilter (DataCollection collection) throws SeqMonkException {
		this(collection,new DataStore[0],EITHER_ABOVE_OR_BELOW,EXACTLY,1.5d,1);
	}

	/**
	 * Instantiates a new box whisker filter with all options set allowing
	 * it to be run immediately.
	 * 
	 * @param collection The dataCollection to filter
	 * @param stores The list of stores to use to create BoxWhisker plots
	 * @param outlierType Use constants ABOVE_ONLY, BELOW_ONLY or EITHER_ABOVE_OR_BELOW
	 * @param filterType Use constants from ProbeFilter EXACTLY, AT_LEAST, NO_MORE_THAN
	 * @param stringency The BoxWhisker stringency (default is 2)
	 * @param storeCutoff How many stores need to pass the filter
	 * @throws SeqMonkException If the collection is not quantitated.
	 */
	public BoxWhiskerFilter (DataCollection collection, DataStore [] stores, int outlierType, int filterType, double stringency, int storeCutoff) throws SeqMonkException {
		super(collection);
		
		if (stores == null) {
			throw new IllegalArgumentException("List of stores cannot be null");
		}

		if (!(outlierType == ABOVE_ONLY || outlierType == BELOW_ONLY || outlierType == EITHER_ABOVE_OR_BELOW)) {
			throw new IllegalArgumentException("Outlier type must be ABOVE_ONLY, BELOW_ONLY or EITHER_ABOVE_OR_BELOW");
		}
		if (!(filterType == AT_LEAST || filterType == NO_MORE_THAN || filterType == EXACTLY)) {
			throw new IllegalArgumentException("Filter type must be AT_LEAST, NO_MORE_THAN or EXACTLY");
		}
		if (storeCutoff < 1) {
			throw new IllegalArgumentException("Store cutoff "+storeCutoff+" was below 1");
		}
		
		this.stores = stores;
		this.filterType = filterType;
		this.stringency = stringency;
		this.storeCutoff = storeCutoff;
		
		if (outlierType == ABOVE_ONLY) {
			useUpper = true;
			useLower = false;
		}
		else if (outlierType == BELOW_ONLY) {
			useUpper = false;
			useLower = true;
		}
		else if (outlierType == EITHER_ABOVE_OR_BELOW) {
			useUpper = true;
			useLower = true;
		}
		
		options = new BoxWhiskerOptionsPanel();
		
	}

	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters outliers based on a BoxWhisker Plot";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return options;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#hasOptionsPanel()
	 */
	@Override
	public boolean hasOptionsPanel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#isReady()
	 */
	@Override
	public boolean isReady() {
		if (stores.length > 0 && storeCutoff > 0 && storeCutoff <= stores.length && stringency > 0) {
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "BoxWhisker Outlier Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("BoxWhisker filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where ");
		if (filterType == EXACTLY) {
			b.append("exactly ");
		}
		else if (filterType == AT_LEAST) {
			b.append("at least ");
		}
		else if (filterType == NO_MORE_THAN) {
			b.append("no more than ");
		}
		
		b.append(storeCutoff);
		
		b.append(" of ");
		
		for (int s=0;s<stores.length;s++) {
			b.append(stores[s].name());
			if (s < stores.length-1) {
				b.append(" , ");
			}
		}
		
		b.append(" was an outlier with stringency ");
		b.append(stringency);
		
		if (useUpper) {
			b.append(" above ");
			if (useLower) {
				b.append(" or ");
			}
		}
		if (useLower) {
			b.append(" below ");
		}
		
		b.append("the main body of the distribution.");
		
		b.append(" Quantitation was ");
		if (collection.probeSet().currentQuantitation() == null) {
			b.append("not known.");
		}
		else {
			b.append(collection.probeSet().currentQuantitation());
		}

		
		return b.toString();
		
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();
		b.append("Outliers ");
		b.append(stringency);
		
		if (useUpper) {
			b.append(" above ");
			if (useLower) {
				b.append(" or ");
			}
		}
		if (useLower) {
			b.append(" below ");
		}

		b.append("the median");
		
		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	protected void generateProbeList () {
		
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",new String[0]);
	
		Hashtable<Probe, Integer> hitCounts = new Hashtable<Probe, Integer>();
	
		for (int s=0;s<stores.length;s++) {
	
			progressUpdated("Processing "+stores[s].name(), s, stores.length);
	
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			
			BoxWhisker bw;
			try {
				bw = new BoxWhisker(stores[s],startingList,stringency);
			} 
			catch (SeqMonkException e) {
				System.err.println("Ignoring unquantitated dataset");
				e.printStackTrace();
				continue;
			}
	
			if (useUpper) {
				Probe [] p = bw.upperProbeOutliers();
				
				if (cancel) {
					cancel = false;
					progressCancelled();
					return;
				}

				for (int i=0;i<p.length;i++) {
					if (hitCounts.containsKey(p[i])) {
						hitCounts.put(p[i], hitCounts.get(p[i]).intValue()+1);
					}
					else {
						hitCounts.put(p[i], 1);
					}
				}	
			}
			if (useLower) {
				Probe [] p = bw.lowerProbeOutliers();
				for (int i=0;i<p.length;i++) {
					
					if (cancel) {
						cancel = false;
						progressCancelled();
						return;
					}
					
					if (hitCounts.containsKey(p[i])) {
						hitCounts.put(p[i], hitCounts.get(p[i]).intValue()+1);
					}
					else {
						hitCounts.put(p[i], 1);
					}
				}	
			}
		}
	
		// Now we can go through the probes which hit and see if
		// we had enough hits to put them into our final list.
	
	
		Enumeration<Probe> candidates = hitCounts.keys();
	
	
		while (candidates.hasMoreElements()) {
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

	
			Probe candidate = candidates.nextElement();
			int count = hitCounts.get(candidate).intValue();
	
			// We can now figure out if the count we've got lets us add this
			// probe to the probe set.
			switch (filterType) {
			case EXACTLY:
				if (count == storeCutoff)
					newList.addProbe(candidate,null);
				break;
	
			case AT_LEAST:
				if (count >= storeCutoff)
					newList.addProbe(candidate,null);
				break;
	
			case NO_MORE_THAN:
				if (count <= storeCutoff)
					newList.addProbe(candidate,null);
				break;
			}
		}
	
		filterFinished(newList);
	
	}
	

	/**
	 * The BoxWhiskerOptionsPanel.
	 */
	private class BoxWhiskerOptionsPanel extends JPanel implements ActionListener,ListSelectionListener, KeyListener {
		
		private JList dataList;
		private JTextField stringencyField;
		private JComboBox outlierType;
		private JComboBox limitType;
		private JTextField dataChosenNumber;
		private JLabel dataAvailableNumber;
	
		/**
		 * Instantiates a new box whisker options panel.
		 */
		public BoxWhiskerOptionsPanel () {
	
			setLayout(new BorderLayout());
			JPanel dataPanel = new JPanel();
			dataPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			dataPanel.setLayout(new BorderLayout());
			dataPanel.add(new JLabel("Data Sets/Groups",JLabel.CENTER),BorderLayout.NORTH);
	
			DefaultListModel dataModel = new DefaultListModel();
	
			DataStore [] stores = collection.getAllDataStores();
			for (int i=0;i<stores.length;i++) {
				if (stores[i].isQuantitated()) {
					dataModel.addElement(stores[i]);
				}
			}
	
			dataList = new JList(dataModel);
			ListDefaultSelector.selectDefaultStores(dataList);
			dataList.setCellRenderer(new TypeColourRenderer());
			dataList.addListSelectionListener(this);
			dataPanel.add(new JScrollPane(dataList),BorderLayout.CENTER);
	
			add(dataPanel,BorderLayout.WEST);
	
			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));
		
			JPanel choicePanel1 = new JPanel();
	
			choicePanel1.add(new JLabel("Find outliers "));
			choicePanel.add(choicePanel1);
	
			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel(" with stringency > "));
			stringencyField = new JTextField(""+stringency,3);
			stringencyField.addKeyListener(this);
			choicePanel2.add(stringencyField);
	
			choicePanel2.add(new JLabel(" which are "));
			outlierType = new JComboBox(new String [] {"Above or Below","Above","Below"});
			outlierType.addActionListener(this);
			choicePanel2.add(outlierType);
			choicePanel2.add(new JLabel(" the median "));
	
			choicePanel.add(choicePanel2);
	
			JPanel choicePanel3 = new JPanel();
			choicePanel3.add(new JLabel(" for "));
	
			limitType = new JComboBox(new String [] {"Exactly","At least","No more than"});
			limitType.addActionListener(this);
			choicePanel3.add(limitType);
	
			dataChosenNumber = new JTextField(""+storeCutoff,3);
			dataChosenNumber.addKeyListener(this);
			choicePanel3.add(dataChosenNumber);
	
			choicePanel3.add(new JLabel(" of the "));
	
			dataAvailableNumber = new JLabel("");
			choicePanel3.add(dataAvailableNumber);
			
			valueChanged(null); // Set the initial state
	
			choicePanel3.add(new JLabel(" selected Data Stores "));
	
			choicePanel.add(choicePanel3);
			add(new JScrollPane(choicePanel),BorderLayout.CENTER);
	
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			return new Dimension(600,300);
		}
		
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
		 */
		public void keyTyped(KeyEvent arg0) {
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent ke) {
	
		}
	
		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
		 */
		public void keyReleased(KeyEvent ke) {
			JTextField f = (JTextField)ke.getSource();
	
			char [] c = f.getText().toCharArray();
	
			StringBuffer b = new StringBuffer();
			for (int i=0;i<c.length;i++) {
	
				if (Character.isDigit(c[i])) {
					b.append(c[i]);
					continue;
				}
				if (f != dataChosenNumber) {
					if (c[i] == KeyEvent.VK_PERIOD) {
						b.append(c[i]);
						continue;
					}
				}
				f.setText(b.toString());
				break;
			}
	
			if (ke.getSource() == stringencyField) {
				if (f.getText().length()>0)
					stringency = Double.parseDouble(f.getText());
				else
					stringency = 1.5; //Default value
			}
			else if (ke.getSource() == dataChosenNumber) {
				if (f.getText().length()>0)
					storeCutoff = Integer.parseInt(f.getText());
				else
					storeCutoff = 1; //Default value
			}
	
			optionsChanged();
		}
	
		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {
			// Update the list of stores
			Object [] o = dataList.getSelectedValues();
			stores = new DataStore[o.length];
			for (int i=0;i<o.length;i++) {
				stores[i] = (DataStore)o[i];
			}
			dataAvailableNumber.setText(""+dataList.getSelectedIndices().length);
			
			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent ae) {
			// Check where this is coming from
			if (ae.getSource() == outlierType) {
				if (outlierType.getSelectedItem() == "Above or Below") {
					useUpper = true;
					useLower = true;
				}
				else if (outlierType.getSelectedItem() == "Above") {
					useUpper = true;
					useLower = false;
				}
				else if (outlierType.getSelectedItem() == "Below") {
					useUpper = false;
					useLower = true;
				}
			}
			else if (ae.getSource() == limitType) {
				if (limitType.getSelectedItem() == "Exactly") {
					filterType = EXACTLY;
				}
				else if (limitType.getSelectedItem() == "At least") {
					filterType = AT_LEAST;
				}
				else if (limitType.getSelectedItem() == "No more than") {
					filterType = NO_MORE_THAN;
				}
			}
			optionsChanged();
		}
	}
}
