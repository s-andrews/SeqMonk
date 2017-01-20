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
package uk.ac.babraham.SeqMonk.DataTypes.Interaction;

import uk.ac.babraham.SeqMonk.DataTypes.Cluster.ClusterPair;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Gradients.ColourGradient;

public interface HeatmapMatrixListener {

	public void newMinDistanceValue (int distance);

	public void newMaxDistanceValue (int distance);

	public void newMinStrengthValue (double strength);
	
	public void newMaxSignificanceValue (double significance);
	
	public void newMinDifferenceValue (double difference);
	
	public void newMinAbsoluteValue (int absolute);
		
	public void newClusterRValue (float rValue);
	
	public void newCluster (ClusterPair cluster);
	
	public void newColourSetting (int colour);
	
	public void newColourGradient (ColourGradient gradient);

	public void newProbeFilterList (ProbeList list);
	
}
