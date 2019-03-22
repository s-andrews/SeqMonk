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

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Utilities.PositionFormat;

/**
 * Filters probes based on their length
 */
public class ProbeLengthFilter extends ProbeFilter {

	private Integer lowerLimit = null;
	private Integer upperLimit = null;

	private ValuesFilterOptionPanel optionsPanel = new ValuesFilterOptionPanel();
	

	/**
	 * Instantiates a new probe length filter with default values
	 * 
	 * @param collection The dataCollection to filter
	 * @throws SeqMonkException if the collection isn't quantitated.
	 */
	public ProbeLengthFilter (DataCollection collection) throws SeqMonkException {
		super(collection);
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#description()
	 */
	@Override
	public String description() {
		return "Filters on the length of each probe";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#generateProbeList()
	 */
	@Override
	protected void generateProbeList() {
		
//		System.out.println("Data store size="+stores.length+" lower="+lowerLimit+" upper="+upperLimit+" type="+limitType+" chosen="+chosenNumber);
		
		Probe [] probes = startingList.getAllProbes();
		ProbeList newList = new ProbeList(startingList,"Filtered Probes","",new String[0]);
		
		for (int p=0;p<probes.length;p++) {
			
			progressUpdated(p, probes.length);
			
			if (cancel) {
				cancel = false;
				progressCancelled();
				return;
			}
			
			if (upperLimit != null)
				if (probes[p].length() > upperLimit)
					continue;
					
			if (lowerLimit != null)
				if (probes[p].length() < lowerLimit)
					continue;
				
			newList.addProbe(probes[p],null);
		}

		
		newList.setName("Length between "+lowerLimit+"-"+upperLimit);
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
		
		if (lowerLimit == null && upperLimit == null) return false;
		
		if (lowerLimit != null && upperLimit != null && lowerLimit > upperLimit) return false;
		
		return true;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#name()
	 */
	@Override
	public String name() {
		return "Probe Length Filter";
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Filters.ProbeFilter#listDescription()
	 */
	@Override
	protected String listDescription() {
		StringBuffer b = new StringBuffer();
		
		b.append("Filter on probes in ");
		b.append(collection.probeSet().getActiveList().name());
		b.append(" where probe length was ");
		
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
			
		b.append("Probe Length ");
		
		if (lowerLimit != null && upperLimit != null) {
			b.append("between ");
			b.append(PositionFormat.formatLength(lowerLimit));
			b.append(" and ");
			b.append(PositionFormat.formatLength(upperLimit));
		}
		
		else if (lowerLimit != null) {
			b.append("above ");
			b.append(PositionFormat.formatLength(lowerLimit));
		}
		else if (upperLimit != null) {
			b.append("below ");
			b.append(PositionFormat.formatLength(upperLimit));
		}
		
		return b.toString();
	}

	/**
	 * The ValuesFilterOptionPanel.
	 */
	private class ValuesFilterOptionPanel extends JPanel implements KeyListener {
			
		private JTextField lowerLimitField;
		private JTextField upperLimitField;
			
		/**
		 * Instantiates a new values filter option panel.
		 */
		public ValuesFilterOptionPanel () {
			setLayout(new BorderLayout());
			
			JPanel choicePanel = new JPanel();
			choicePanel.setLayout(new BoxLayout(choicePanel,BoxLayout.Y_AXIS));


			JPanel choicePanel2 = new JPanel();
			choicePanel2.add(new JLabel("Probe length must be between "));
			lowerLimitField = new JTextField(6);
			lowerLimitField.addKeyListener(this);
			choicePanel2.add(lowerLimitField);

			choicePanel2.add(new JLabel(" and "));

			upperLimitField = new JTextField(6);
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
		public void keyTyped(KeyEvent arg0) {}

		/* (non-Javadoc)
		 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
		 */
		public void keyPressed(KeyEvent ke) {}

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
					else {
						lowerLimit = Integer.parseInt(f.getText());
					}
				}
				else if (f == upperLimitField) {
					if (f.getText().length() == 0) {
						upperLimit = null;
					}
					else {
						upperLimit = Integer.parseInt(f.getText());
					}
				}
			}
			catch (NumberFormatException e) {
				f.setText(f.getText().substring(0,f.getText().length()-1));
			}
				
			optionsChanged();
		}
	}
}
