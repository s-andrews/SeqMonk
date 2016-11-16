/**
 * Copyright 2010-15 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.WelcomePanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.SeqMonkApplication;

/**
 * This is an information panel which takes up all of the main display when
 * SeqMonk is first launched.  It is intended to be a more friendly
 * introduction than the current blank screen.  As well as putting up an
 * advert it will also show some status information to show people what
 * is and isn't set up and working.
 */

public class WelcomePanel extends JPanel {
	
	private SeqMonkInformationPanel infoPanel;
	
	public WelcomePanel (SeqMonkApplication application) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.5;
		gbc.weighty = 0.8;
		gbc.fill = GridBagConstraints.NONE;
		
		add(new JPanel(),gbc);
		gbc.weighty = 0.2;
		gbc.gridy++;
		
		add(new SeqMonkTitlePanel(),gbc);
		gbc.gridy++;
		infoPanel = new SeqMonkInformationPanel(application);
		add(infoPanel,gbc);

		gbc.weighty = 0.8;
		gbc.gridy++;
		add(new JPanel(),gbc);

		
	}
	
	public boolean cacheDirectoryValid () {
		return infoPanel.cacheDirectoryValid();
	}

	public void refreshPanel () {
		infoPanel.populatePanel();
	}
	

}
