package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.HeatmapPanel;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendData;

public class QuantitationHeatmapPanel extends JPanel {

	public QuantitationHeatmapPanel(QuantitationTrendData data, DataStore store, ProbeList list) {
		// TODO Auto-generated constructor stub
	}

	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.RED);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
	
	
}
