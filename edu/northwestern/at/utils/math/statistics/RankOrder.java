package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;
import edu.northwestern.at.utils.Sorting;

/** RankOrder -- Convert columns of an (n x m) matrix to rank order values.
 */

public class RankOrder
{
	/**	Rank order columns of a matrix.
	 *
	 *	@param		matrix		The n x m data matrix whose columns are
	 *							to be rank ordered.
	 *
	 *	<p>
	 *	On return, the columns of the input matrix are
	 *	independently converted to rank order.
	 *	</p>
	 */

	public static void rankOrder( Matrix matrix )
	{
								// Get number of rows and columns
								// in data matrix.

		int	nCols		= matrix.columns();
		int	nRows		= matrix.rows();

                                // Loop over columns.

		for ( int j = 1 ; j <= nCols ; j++ )
		{
								//	Replace original column values with
								//	rank order values.

			matrix.setColumnData(
				j ,
				getRankOrders( matrix.getColumnData( j ) ) );
		}
	}

	/**	Compute rank order for array of doubles.
	 *
	 *	@param	values		Values to be rank ordered.
	 *
	 *	@return				The input values are converted to
	 *						rank order values, and the return value
	 *						is set the the values array as well.
	 *
	 *	<p>
	 *	Tied ranks are converted to mid-rank values.  This allow for
	 *	proper computation of various rank-based statistics, e.g.,
	 *	Spearman's correlation coefficient.
	 *	</p>
	 */

	public static double[] getRankOrders( double[] values )
	{
								//	Create index vector for values.

		int[] indices	= new int[ values.length ];

		for ( int i = 0 ; i < values.length ; i++ )
		{
			indices[ i ]	= i;
		}
								//	Sort data carrying along the indices.

		Sorting.quickSort( values , indices , 0 , values.length - 1 );

								//	Create vector to hold computed ranks.

		double[] rankOrders	= new double[ values.length ];

								//	Compute rank order, correcting
								//	for ties.
		int i	= 0;
		int k;
		int j;
		int l;

		while ( i < values.length )
		{
			j	= 0;
			l	= 0;
								//	Look for tied values and adjust for them
								//	by computing mid-rank.

			for ( k = i ; k < values.length ; k++ )
			{
				if ( values[ k ] == values[ i ] )
				{
					j	= j + 1;
					l	= l + k + 1;
				}
			}
								// 	Compute the rank order values.

			for ( k = 0 ; k < j ; k++ )
			{
				rankOrders[ i + k ] = (double)(((double)(l))/(double)j);
			}

			i	= i + j;
		}
								//	Move rank order values to original
								//	positions.

		for ( i = 0 ; i < values.length ; i++ )
		{
			values[ indices[ i ] ]	= rankOrders[ i ];
		}

		return values;
	}

	/** Don't allow instantiation, but allow subclassing. */

	protected RankOrder()
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

