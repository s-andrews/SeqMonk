/**
 * Copyright 2011-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.HiCHeatmap;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.HiCDataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Genome.Genome;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;

public class HeatmapGenomePanelCollection extends JPanel {

	private HeatmapGenomePanel genomePanel;
	private HiCGradientPanel gradient;
	
	public HeatmapGenomePanelCollection (HiCDataStore store, ProbeList probes, HeatmapMatrix matrix, Genome genome) {
		
		setLayout(new BorderLayout());
		genomePanel = new HeatmapGenomePanel(store, probes, matrix, genome);
		matrix.addOptionListener(genomePanel);
		add(genomePanel,BorderLayout.CENTER);
		
		gradient = new HiCGradientPanel(matrix);
		matrix.addOptionListener(gradient);
		add(gradient,BorderLayout.EAST);
		
	}
		
	public HeatmapGenomePanel genomePanel () {
		return genomePanel;
	}

	
}
