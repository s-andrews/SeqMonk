/**
 * Copyright 2010-18 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.QQDistributionPlot;

import java.util.Arrays;

import uk.ac.babraham.SeqMonk.SeqMonkException;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Probes.ProbeList;
import uk.ac.babraham.SeqMonk.Displays.CumulativeDistribution.CumulativeDistributionPanel;

public class QQDistributionPanel extends CumulativeDistributionPanel {


	public QQDistributionPanel (DataStore store, ProbeList [] lists) throws SeqMonkException {

		super(store,lists);
		absoluteMin = 0;
		absoluteMax = 100;
		usedMin = absoluteMin;
		usedMax = absoluteMax;
	}

	public QQDistributionPanel (DataStore [] stores, ProbeList list) throws SeqMonkException {
		
		super(stores,list);

		absoluteMin = 0;
		absoluteMax = 100;
		usedMin = absoluteMin;
		usedMax = absoluteMax;	
	}
		
	protected float [] shortenDistribution (float [] distribution) {
	
		// Firstly we need to remove any point which aren't valid.
		int nanCount = 0;
		for (int i=0;i<distribution.length;i++) {
			if (Float.isNaN(distribution[i]) || Float.isInfinite(distribution[i])) ++nanCount ;
		}

		double totalCount = 0;
		float [] validDistribution = new float[distribution.length-nanCount];
			
		int index1 = 0;
			
		for (int i=0;i<distribution.length;i++) {
			if (Float.isNaN(distribution[i]) || Float.isInfinite(distribution[i])) continue;
			totalCount += Math.abs(distribution[i]);
			validDistribution[index1] = distribution[i];
			index1++;
		}
			
		distribution = validDistribution;
		
		Arrays.sort(distribution);
		
		// Now we need to replace the values with percentiles
		float [] absDistribution = new float[distribution.length];
		
		double runningTotal = 0;
		for (int i=0;i<distribution.length;i++) {
			runningTotal += Math.abs(distribution[i]);
			absDistribution[i] = (float)(runningTotal/totalCount)*100;
		}
		
		distribution = absDistribution;
		
		// We need to return a 1000 element array
		float [] shortDistribution = new float[1000];
		
		if (distribution.length == 0) {
			// There's no valid data here so make everything zero
			for (int i=0;i<shortDistribution.length;i++) {
				shortDistribution[i] = 0;
			}
		}
		
		else {
			for (int i=0;i<shortDistribution.length;i++) {
				int index = (int)((distribution.length-1)*(i/1000d));
				shortDistribution[i] = distribution[index];
			}
		}
		
		return shortDistribution;
		
	}

}
