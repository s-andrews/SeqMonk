/**
 * Copyright 2009-19 Simon Andrews
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
package uk.ac.babraham.SeqMonk.Displays.ProbeTrendPlot;

import uk.ac.babraham.SeqMonk.DataTypes.Sequence.QuantitationStrandType;

/**
 * The Class TrendOverProbePreferences provides a summary of the
 * preferences which will be used to draw a trend plot
 */
public class TrendOverProbePreferences {
	
	public static final int CUMULATIVE_COUNT_PLOT = 10;
	public static final int RELATIVE_DISTRIBUTION_PLOT = 11;

	/** What type of plot we're drawing */
	public int plotType = RELATIVE_DISTRIBUTION_PLOT;
	
	/** Which reads to use in the plot */
	public QuantitationStrandType quantitationType = QuantitationStrandType.getTypeOptions()[0];
	
	/** Whether to remove duplicates. */
	public boolean removeDuplicates = false;
	
	/** Whether to correct for total count. */
	public boolean correctForTotalCount = false;
	
	/** Whether to scale within each store. */
	public boolean correctWithinEachStore = false;
	
	/** Whether to force a relative plot even for similarly sized probes */
	public boolean forceRelative = false;
	
}
