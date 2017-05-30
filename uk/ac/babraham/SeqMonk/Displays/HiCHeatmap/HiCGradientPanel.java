package uk.ac.babraham.SeqMonk.Displays.HiCHeatmap;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrix;
import uk.ac.babraham.SeqMonk.DataTypes.Interaction.HeatmapMatrixListener;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.GradientScaleBar.GradientScaleBar;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;
import uk.ac.babraham.SeqMonk.Preferences.DisplayPreferences;

public class HiCGradientPanel extends JPanel implements HeatmapMatrixListener {
	
	private HeatmapMatrix matrix;
	private GradientScaleBar scaleBar;
	
	
	public HiCGradientPanel (HeatmapMatrix matrix) {
		this.matrix = matrix;
		
		setLayout(new BorderLayout());
		
		scaleBar = new GradientScaleBar(null, 0, 1);
		configureBar();
		
		add(scaleBar,BorderLayout.CENTER);
		
	}
	
	private void configureBar() {
		
		// Setting the correct colour is the easy bit
		scaleBar.setGradient(matrix.colourGradient());
		
		// We now need to know what we're colouring by
		switch (matrix.currentColourSetting()) {
		
		case HeatmapMatrix.COLOUR_BY_OBS_EXP:
			if (matrix.initialMinStrength() < 1) {
				// They're interested in depletion as well as enrichment.
				// Make a symmetrical gradient around 0 and the max strength
				scaleBar.setLimits(Math.log10(1/matrix.maxValue()), Math.log10(matrix.maxValue()));						
			}
			else {
				scaleBar.setLimits(Math.log10(matrix.initialMinStrength()), Math.log10(matrix.maxValue()-matrix.initialMinStrength()));
			}
			break;

		case HeatmapMatrix.COLOUR_BY_INTERACTIONS: 
			scaleBar.setLimits(matrix.initialMinAbsolute(),50);
			break;

		case HeatmapMatrix.COLOUR_BY_P_VALUE:
			scaleBar.setLimits(Math.log10(matrix.initialMaxSignificance())*-10,50);
			break;

		case HeatmapMatrix.COLOUR_BY_QUANTITATION:
			double minQuantitatedValue;
			double maxQuantitatedValue;
			if (DisplayPreferences.getInstance().getScaleType() == DisplayPreferences.SCALE_TYPE_POSITIVE) {
				minQuantitatedValue = 0;
				maxQuantitatedValue = DisplayPreferences.getInstance().getMaxDataValue();
			}
			else {
				maxQuantitatedValue = DisplayPreferences.getInstance().getMaxDataValue();
				minQuantitatedValue = 0-maxQuantitatedValue;
			}

			scaleBar.setLimits(minQuantitatedValue,maxQuantitatedValue);
			break;

		}
		
	}
	
	
	public void newMinDistanceValue(int distance) {}

	public void newMaxDistanceValue(int distance) {}

	public void newMinStrengthValue(double strength) {
		configureBar();
	}

	public void newMaxSignificanceValue(double significance) {
		configureBar();
	}

	public void newMinDifferenceValue(double difference) {}

	public void newMinAbsoluteValue(int absolute) {
		configureBar();
	}

	public void newClusterRValue(float rValue) {}

	public void newCluster(ClusterPair cluster) {}

	public void newColourSetting(int colour) {
		configureBar();
	}

	public void newColourGradient(ColourGradient gradient) {
		configureBar();
	}

	public void newProbeFilterList(ProbeList list) {}

}
