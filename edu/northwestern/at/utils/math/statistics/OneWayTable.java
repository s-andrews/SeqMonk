package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;

import edu.northwestern.at.utils.math.*;
import edu.northwestern.at.utils.math.distributions.*;

/**	Statistical analysis of a one-way contingency table.
 */

public class OneWayTable
{
	public static double[] pearsonChiSquare( double[] counts )
	{
		int numCounts			= counts.length;

		double sumCounts		= 0.0D;
		double chiSquare		= 0.0D;
		double probChiSquare	= 1.0D;

		for ( int i = 0 ; i < numCounts ; i++ )
		{
			sumCounts	+= counts[ i ];
		}

		double expected		= 0.0D;

		if ( numCounts > 0 )
		{
			expected	= sumCounts / (double)numCounts;

			if ( expected > 0.0D )
			{
				for ( int i = 0 ; i < numCounts ; i++ )
				{
					double diff	= counts[ i ] - expected;
					chiSquare	+= ( diff * diff ) / expected;
				}

				probChiSquare	=
					Sig.chisquare( chiSquare , numCounts - 1 );
			}
		}

		return new double[]{ chiSquare , probChiSquare };
	}

	public static double[] logLikelihoodChiSquare( double[] counts )
	{
		int numCounts			= counts.length;

		double sumCounts		= 0.0D;
		double chiSquare		= 0.0D;
		double probChiSquare	= 1.0D;

		for ( int i = 0 ; i < numCounts ; i++ )
		{
			sumCounts	+= counts[ i ];
		}

		double expected		= 0.0D;

		if ( numCounts > 0 )
		{
			expected	= sumCounts / (double)numCounts;

			if ( expected > 0.0D )
			{
				for ( int i = 0 ; i < numCounts ; i++ )
				{
					chiSquare	+=
						counts[ i ] * ArithUtils.safeLog(
							counts[ i ] / expected );
				}

				chiSquare		= 2.0D * chiSquare;

				probChiSquare	=
					Sig.chisquare( chiSquare , numCounts - 1 );
			}
		}

		return new double[]{ chiSquare , probChiSquare };
	}

	/**	Don't allow instantiation but do allow overrides.
	 */

	protected OneWayTable()
	{
	}
}

/*
 * <p>
 * Copyright &copy; 2004-2011 Northwestern University.
 * </p>
 * <p>
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more
 * details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 * </p>
 */

