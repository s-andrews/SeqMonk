package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.HeatmapPanel;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendData;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Utilities.DoubleVector;
import uk.ac.babraham.SeqMonk.Utilities.FloatVector;

public class QuantitationHeatmapPanel extends JPanel {

	private double [] data;
	private ColourGradient gradient;
	private double min;
	private double max;
	
	public QuantitationHeatmapPanel(QuantitationTrendData data, DataStore store, ProbeList list, ColourGradient gradient, double min, double max) {
		this.gradient = gradient;
		this.min = min;
		this.max = max;
		
		DoubleVector v = new DoubleVector();
		
		if (data.hasUpstream()) {
			double [] upstream = data.getUpstreamData(store, list);
			for (int d=0;d<upstream.length;d++) {
				v.add(upstream[d]);
			}
		}
		
		if (true) {
			double [] central = data.getCentralData(store, list);
			for (int d=0;d<central.length;d++) {
				v.add(central[d]);
			}
		}
		
		if (data.hasDownstream()) {
			double [] downstream = data.getDownstreamData(store, list);
			for (int d=0;d<downstream.length;d++) {
				v.add(downstream[d]);
			}
		}

		this.data = v.toArray();
		
	}

	public void setGradient (ColourGradient gradient) {
		this.gradient = gradient;
		repaint();
	}
	
	public void setLimits(double min, double max) {
		this.min = min;
		this.max = max;
		repaint();
	}
	
	public void paint (Graphics g) {
		super.paint(g);
		g.setColor(Color.RED);
		g.fillRect(0, 0, getWidth(), getHeight());
		
		int lastX = -1;
		
		for (int d=0;d<data.length;d++) {
			int thisX = getX(d);
			if (thisX > lastX) {
				g.setColor(gradient.getColor(data[d], min, max));
				g.fillRect(lastX, 0, thisX-lastX, getHeight());
				lastX = thisX;
			}
		}
	}

	
	private int getX (int index) {
		double proportion = index/(double)data.length;
		return (int)(getWidth()*proportion);
	}
	
	
}
