/**
 * Copyright 2009-17 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.AlignedProbePlot;

import uk.ac.babraham.SeqMonk.DataTypes.DataStore;
import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;

/**
 * The Class AlignedSummaryPreferences provides a summary of the
 * preferences which will be used to draw an aligned probes plot
 */
public class AlignedSummaryPreferences {

	/** Which reads to use in the plot */
	public QuantitationStrandType quantitationType = QuantitationStrandType.getTypeOptions()[0];
	
	/** Whether to correct for total library size **/
	public boolean globallyCorrect = true;
	
	/** Whether to remove duplicates. */
	public boolean removeDuplicates = false;
			
	/** Whether to force a relative plot even for similarly sized probes */
	public boolean forceRelative = false;
	
	/** Whether to put the intensities on a log scale **/
	public boolean useLogScale = false;
	
	public DataStore orderBy = null;
	
	
}
