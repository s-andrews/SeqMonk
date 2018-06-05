package uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.HeatmapPanel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.QuantitationTrendPlot.QuantitationTrendData;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;

public class QuantitationHeatmapPanelGroup extends JPanel {

	private QuantitationHeatmapPanel [] heatmaps;
	
	
	public QuantitationHeatmapPanelGroup(QuantitationTrendData data, ColourGradient gradient, double min, double max) {
		
		// We simply need to set out the panels for the different parts of the plot.
		
		// We put probe lists in columns and data stores in rows
		// Names go over the top and down the sides.
		
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		
		gbc.gridx=0;
		gbc.gridy = 0;
		gbc.weightx = 0.999;
		gbc.weighty = 0.0001;
		gbc.insets = new Insets(3, 3, 3, 3);
		gbc.fill = GridBagConstraints.BOTH;
		
		// Do the probe list names first
		ProbeList [] lists = data.lists();
		for (int l=0;l<lists.length;l++) {
			gbc.gridx=l;
			add(new JLabel(lists[l].name(), JLabel.CENTER),gbc);
		}
		
		DataStore [] stores = data.stores();
		gbc.weighty=0.999;
		
		heatmaps = new QuantitationHeatmapPanel [stores.length * lists.length]; 
		
		// Now go through the datasets.
		for (int s=0;s<stores.length;s++) {
			for (int l=0;l<lists.length;l++) {
				gbc.gridx=l;
				gbc.gridy = s+1;
				
				heatmaps[(lists.length*s)+l] = new QuantitationHeatmapPanel(data, stores[s], lists[l], gradient, min, max);
				add(heatmaps[(lists.length*s)+l],gbc);
			}
			
			gbc.weightx=0.001;
			gbc.gridx=lists.length;
			add(new JLabel(stores[s].name()),gbc);
			
		}
	}

	public void setGradient (ColourGradient gradient) {
		for (int i=0;i<heatmaps.length;i++) {
			heatmaps[i].setGradient(gradient);
		}
	}
	
	public void setLimits (double min, double max) {
		for (int i=0;i<heatmaps.length;i++) {
			heatmaps[i].setLimits(min, max);
		}
		
	}
	
}
