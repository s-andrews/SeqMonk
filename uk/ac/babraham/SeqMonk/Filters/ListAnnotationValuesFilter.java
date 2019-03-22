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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

/**
 * The ListAnnotationValuesFilter filters probes based on the annotation
 * values contained within an existing probe list.
 */
public class ListAnnotationValuesFilter extends ProbeFilter {

	private String annotationToUse = null;
	private Float lowerLimit = null;
	private Float upperLimit = null;
	private boolean absoluteTransform = false;

	private ValuesFilterOptionPanel optionsPanel = new ValuesFilterOptionPanel();


	/**
	 * Instantiates a new values filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the dataCollection isn't quantitated.
	 */
	public ListAnnotationValuesFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the annotation values from an existing probe list";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {

		//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);

		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",new String[]{annotationToUse});

		absoluteTransform = optionsPanel.absoluteBox.getSelectedItem().equals("Absolute value");
		
		int indexForAnnotation = -1;

		for (int i=0;i<startingList.getValueNames().length;i++) {
			if (annotationToUse.equals(startingList.getValueNames()[i])) {
				indexForAnnotation = i;
				break;
			}
		}

		if (indexForAnnotation == -1) {
			throw new IllegalStateException("Can't find annotation name "+annotationToUse+" in "+startingList.name());
		}

		for (int p=0;p<probes.length;p++) {

			progressUpdated(p, probes.length);

			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}

			float [] values = startingList.getValuesForProbe(probes[p]);

			if (values == null) continue;

			float value = values[indexForAnnotation];

			if (Float.isNaN(value)) continue; // NaN values always fail the filter.

			float originalValue = value;
			if (absoluteTransform) {
				value = Math.abs(value);
			}
			
			// Now we have the value we need to know if it passes the test
			if (upperLimit != null)
				if (value > upperLimit)
					continue;

			if (lowerLimit != null)
				if (value < lowerLimit)
					continue;

			newList.addProbe(probes[p],originalValue);
		}


		newList.setName("Value between "+lowerLimit+"-"+upperLimit);
		filterFinished(newList);

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#getOptionsPanel()
	 */
	@Override
	public JPanel getOptionsPanel() {
		return optionsPanel;
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
		if (annotationToUse == null) return false;

		if (lowerLimit == null && upperLimit == null) return false;

		if (lowerLimit != null && upperLimit != null && lowerLimit > upperLimit) return false;

		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "List Annotation Values Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();

		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where annotation ");

		b.append(annotationToUse);

		if (absoluteTransform)
			b.append(" had an absolute value ");
		else 
			b.append(" had a value ");

		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit);
			b.append(" and ");
			b.append(upperLimit);
		}

		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit);
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit);
		}

		return b.toString();
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listName()
	 */
	@Override
	protected String listName() {
		StringBuffer b = new StringBuffer();

		if (absoluteTransform)
			b.append("Absolute ");
		
		b.append(annotationToUse);
		b.append(" ");

		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(lowerLimit.toString().replaceAll("\\.0+$", ""));
			b.append(" and ");
			b.append(upperLimit.toString().replaceAll("\\.0+$", ""));
		}

		else if (lowerLimit != null) {
			b.append("above ");
			b.append(lowerLimit.toString().replaceAll("\\.0+$", ""));
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(upperLimit.toString().replaceAll("\\.0+$", ""));
		}

		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class ValuesFilterOptionPanel extends JPanel implements ListSelectionListener, KeyListener {

		private JList annotationList;
		private JTextField lowerLimitField;
		private JTextField upperLimitField;
		private JComboBox<String> absoluteBox;

		/**
		 * Instantiates a new values filter option panel.
		 */
		public ValuesFilterOptionPanel () {
			setLayout(new BorderLayout());
			JPanel annotationPanel = new JPanel();
			annotationPanel.setBorder(BorderFactory.createEmptyBorder(4,4,0,4));
			annotationPanel.setLayout(new BorderLayout());
			annotationPanel.add(new JLabel("Annotation values",JLabel.CENTER),BorderLayout.NORTH);

			DefaultListModel dataModel = new DefaultListModel();

			String [] annotations = startingList.getValueNames();

			for (int i=0;i<annotations.length;i++) {
				dataModel.addElement(annotations[i]);
			}

			annotationList = new JList(dataModel);
			if (annotations.length==1) {
				annotationList.setSelectedIndex(0);
			}
			annotationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			annotationList.addListSelectionListener(this);

			JScrollPane scrollPane = new JScrollPane(annotationList);
			scrollPane.setPreferredSize(new Dimension(200,annotationList.getPreferredSize().height));
			annotationPanel.add(scrollPane,BorderLayout.CENTER);

			add(annotationPanel,BorderLayout.WEST);

			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));

			JPanel choicePanel1 = new JPanel();
			absoluteBox = new JComboBox<String>(new String[]{"Value","Absolute value"});
			choicePanel1.add(absoluteBox);
			choicePanel.add(choicePanel1);

			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("must be between "));
			lowerLimitField = new JTextField(3);
			lowerLimitField.addKeyListener(this);
			choicePanel2.add(lowerLimitField);

			choicePanel2.add(new JLabel(" and "));

			upperLimitField = new JTextField(3);
			upperLimitField.addKeyListener(this);
			choicePanel2.add(upperLimitField);
			choicePanel.add(choicePanel2);

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

			try {
				if (f == lowerLimitField) {
					if (f.getText().length() == 0) {
						lowerLimit = null;
					}
					else if (f.getText().equals("-")) {
						lowerLimit = 0f;
					}
					else {
						lowerLimit = Float.parseFloat(f.getText());
					}
				}
				else if (f == upperLimitField) {
					if (f.getText().length() == 0) {
						upperLimit = null;
					}
					else if (f.getText().equals("-")) {
						upperLimit = 0f;
					}
					else {
						upperLimit = Float.parseFloat(f.getText());
					}
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}

			optionsChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		public void valueChanged(ListSelectionEvent lse) {

			annotationToUse = (String)annotationList.getSelectedValue();
			optionsChanged();
		}

	}
}
