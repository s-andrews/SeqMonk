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
package uk.ac.babraham.SeqMonk.ProbeGenerators;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.Probe;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeSet;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

/**
 * Generates an evenly spaced set of probes over the whole genome
 */
public class CurrentRegionProbeGenerator extends ProbeGenerator {

	private JPanel optionPanel = null;

	/**
	 * Instantiates a new running window probe generator.
	 * 
	 * @param collection
	 */
	public CurrentRegionProbeGenerator(DataCollection collection) {
		super(collection);
		
		optionPanel = new JPanel();
		optionPanel.setLayout(new BorderLayout());
		optionPanel.add(new JLabel("[No Options]",JLabel.CENTER),BorderLayout.CENTER);
	}
	
	public boolean requiresExistingProbeSet () {
		return false;
	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#generateProbes(boolean)
	 */
	public void generateProbes() {
		Probe p =  new Probe(DisplayPreferences.getInstance().getCurrentChromosome(),DisplayPreferences.getInstance().getCurrentLocation());
		ProbeSet finalSet = new ProbeSet("Currently visible region", new Probe [] {p});
		
		generationComplete(finalSet);

	}

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#getOptionsPanel(uk.ac.babraham.SeqMonk.SeqMonkApplication)
	 */
	public JPanel getOptionsPanel() {
		
		return optionPanel;
	}
	
	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.ProbeGenerators.ProbeGenerator#isReady()
	 */
	public boolean isReady() {
		return true;
	}
	

	/* (non-Javadoc)
	 * @see uk.ac.babraham.SeqMonk.Dialogs.Cancellable#cancel()
	 */
	public void cancel () {
		cancel = true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString () {
		return "Current Region Generator";
	}
	
}
