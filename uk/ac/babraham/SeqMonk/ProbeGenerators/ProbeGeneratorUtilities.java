/**
 * Copyright Copyright 2010-18 Simon Andrews
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

import uk.ac.babraham.SeqMonk.DataTypes.DataCollection;
import uk.ac.babraham.SeqMonk.DataTypes.DataStore;

/**
 * Static methods which may be of use to probe generators
 */
public class ProbeGeneratorUtilities {

	/**
	 * Suggest a suitable running window size based on the coverage in a data collection
	 * 
	 * @param collection The dataCollection to use to suggest the size
	 * @return The suggested window size (in bp)
	 */
	public static int suggestWindowSize (DataCollection collection) {
		/*
		 * The default average probe count is 5
		 */
		
		return suggestWindowSize(collection,5);
	}
	
	/**
	 * Suggest a suitable running window size based on the coverage in a data collection
	 * and a specified average number of reads per window
	 * 
	 * @param collection The dataCollection to use to suggest the size
	 * @param averageProbeCount How many reads should be contained in the average probe
	 * @return The suggested window size (in bp)
	 */
	public static int suggestWindowSize (DataCollection collection, int averageProbeCount) {
		
		/*
		 * Work out what data we're going to use.
		 * 
		 * If there are any groups defined then we use those by default.
		 * If no then we use the data sets.
		 */
		
		DataStore [] data = collection.getAllDataGroups();
		if (data == null || data.length == 0) {
			data = collection.getAllDataSets();
		}
		
		
		long minCount = 0;
		
		long genomeSize = collection.genome().getTotalGenomeLength();
			
		for (int i=0;i<data.length;i++) {
			long count = data[i].getTotalReadCount();
			
			// Don't include stores with no data at all
			if (count == 0) continue;
			
			if (minCount==0) {
				minCount = count;
			}
			
			if (count < minCount) {
				minCount = count;
			}
		}
		
		// Check that there was some data somewhere
		if (minCount == 0 || averageProbeCount == 0 || minCount/averageProbeCount == 0) {
			// We can't give a sensible value, so just make it 1kb
			return 1000;
		}
		
		int suggestedSize = (int)(genomeSize/(minCount/averageProbeCount));
		
		/*
		 * We want to round this to 1 significant figure.
		 * 
		 * Surely there must be a better way to reduce to 1 significant
		 * figure than this??  Had a quick google but didn't find one
		 * so this will do for now.
		 */ 
		
		int power = (int)(Math.pow(10,(""+suggestedSize).length()-1));
		
		suggestedSize = (1+(suggestedSize/power))*power;
		
		return suggestedSize;
		
	}
	
}
