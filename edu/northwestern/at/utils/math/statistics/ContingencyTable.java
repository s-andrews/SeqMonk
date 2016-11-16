package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;

/**	Statistical analysis of a two-way contingency table.
 */

public class ContingencyTable
{
	/**	Compute chisquare for a table of frequencies.
	 *
	 *	@param	table	A matrix containing the table.
	 *
	 *	@return			Pearson's chisquare for the table.
	 */

	public static double chisquare( Matrix table )
	{
		int	rows	= table.rows();
		int columns	= table.columns();

		Matrix rowsums	= RowTransformer.sum( table );
		Matrix colsums	= ColumnTransformer.sum( table );
		double total	= MatrixMeasure.sum( table );
	    double result	= 0.0D;

		if ( total > 0.0D )
		{
		    double expected;
		    double ominuse;

		    for ( int row = 1 ; row <= rows ; row++ )
		    {
				for ( int column = 1 ; column <= columns ; column++ )
				{
	    			expected	=
	    				( colsums.get( 1 , column ) *
	    					rowsums.get( row , 1 ) ) / total;

					ominuse		= table.get( row , column ) - expected;

					result		+= ( ominuse * ominuse ) / expected;
		    	}
			}
		}

		return result;
	}

	protected static double logLikelihood
	(
		Matrix p ,
		double total ,
		Matrix k ,
		int columns
	)
	{
		double sum		= 0.0D;
		double logtotal	= Math.log( total );

		for ( int column = 1 ; column <= columns ; column++ )
		{
			if ( p.get( 1 , column ) != 0.0D )
			{
	    		sum	+=
	    			k.get( 1 , column ) *
	    			Math.log( p.get( 1 , column ) / total );
			}
	    }

    	return sum;
	}

	/**	Compute likelihood ratio for a table of frequencies.
	 *
	 *	@param	table	A matrix containing the table.
	 *
	 *	@return			Likelihood ratio for the table.
	 */

	public static double likelihoodRatio( Matrix table )
	{
		int	rows	= table.rows();
		int columns	= table.columns();

		Matrix rowsums	= RowTransformer.sum( table );
		Matrix colsums	= ColumnTransformer.sum( table );
		double total	= MatrixMeasure.sum( table );
		double sum		= 0.0D;

		if ( total > 0.0D )
		{
		    for ( int row = 1 ; row <= rows ; row++ )
		    {
				sum +=
					logLikelihood(
						table.getRow( row ) ,
						rowsums.get( row ,  1 ) ,
						table.getRow( row ) ,
						columns ) -
					logLikelihood(
						colsums ,
						total ,
						table.getRow( row ) ,
						columns );
			}
	    }

	    return 2.0D * sum;
	}

	/**	Compute Fisher's exact test for a 2x2 table.
	 *
	 *	@param	table	The table (must be 2x2).
	 *
	 *	@return			double array with three entries.
	 *
	 *	@throws			RuntimeException
	 *						If the
	 */

	public static double[] fishersExactTest( Matrix table )
	{
		if ( ( table.rows() != 2 ) || ( table.columns() != 2 ) )
		{
			throw new RuntimeException( "Table is not 2x2" );
		}
		else
		{
			int n11	= (int)table.get( 1 , 1 );
			int n12	= (int)table.get( 1 , 2 );
			int n21	= (int)table.get( 2 , 1 );
			int n22	= (int)table.get( 2 , 2 );

			return FishersExactTest.fishersExactTest( n11 , n12 , n21 , n22 );
		}
	}

	/**	Don't allow instantiation but do allow overrides.
	 */

	protected ContingencyTable()
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

