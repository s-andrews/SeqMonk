/**
 * Copyright 2016- 21 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.BoxWhisker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.Analysis.Statistics.BoxWhisker;
import uk.ac.babraham.SeqMonk.Displays.BeanPlot.BeanPlotScaleBar;

public class MultiBoxWhiskerPanel extends JPanel {
		
	public MultiBoxWhiskerPanel (BoxWhisker [] whiskers, String [] names, String panelName, float min, float max, Color [] colours) {
				
		setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx=0.5;
		gbc.insets = new Insets(2,0,2,0);
		
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.gridwidth=whiskers.length+1;
		add(new NoWidthLabel(panelName, JLabel.CENTER),gbc);
		gbc.gridwidth=1;
		
		gbc.fill=GridBagConstraints.BOTH;
		gbc.gridx=0;
		gbc.gridy=2;
		gbc.weighty=0.999;
		gbc.weightx=0.001;
		add(new BeanPlotScaleBar(min, max),gbc);
		gbc.weightx=0.5;
		
		for (int i=0;i<whiskers.length;i++) {
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridx=i+1;
			gbc.gridy=1;
			gbc.weighty=0.01;
			gbc.anchor = GridBagConstraints.NORTH;
			add(new NoWidthLabel(names[i],JLabel.CENTER),gbc);
			gbc.fill=GridBagConstraints.BOTH;
			gbc.weighty=0.999;
			gbc.gridy=2;
			add(new BoxWhiskerPanel(whiskers[i], min, max,colours[i]),gbc);
		}
	}
	

	/**
	 * A JLabel with no minimum width so out plot isn't forced to be
	 * ridiculously wide if the DataStores have long names.  We have
	 * to live with the fact that names will be truncated if using this.
	 */
	private class NoWidthLabel extends JLabel {
		
		/**
		 * Constructor which passes directly to JLabel
		 * 
		 * @param text the text
		 * @param orientation the orientation
		 */
		public NoWidthLabel (String text, int orientation) {
			super(text,orientation);
		}
		
		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getPreferredSize()
		 */
		public Dimension getPreferredSize () {
			Dimension orig = super.getPreferredSize();
			return new Dimension (1,orig.height);
		}

		/* (non-Javadoc)
		 * @see javax.swing.JComponent#getMinimumSize()
		 */
		public Dimension getMinimumSize () {
			Dimension orig = super.getMinimumSize();
			return new Dimension (1,orig.height);
		}

	}

}


