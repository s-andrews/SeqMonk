package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;

/** Standardize -- Standardize columns of an (n x m) matrix.
 */

public class Standardize
{
	/**	Standardize columns of a matrix.
	 *
	 *	@param		matrix		The n x m data matrix whose columns are
	 *							to be standardized to mean=0 and
	 *							standard deviation=1.
	 *
	 *	@return					The input matrix is standardized.
	 *							The result is a two-element array of
	 *							type matrix.  The first element is
	 *							a matrix with one row and m columns
	 *							containing the column means.
	 *							The second element is a matrix with
	 *							one row and m columns containing
	 *							the column standard deviations.
	 */

	public static Matrix[] standardize( Matrix matrix )
	{
								// Get number of rows and columns
								// in data matrix.

		int	nCols		= matrix.columns();
		int	nRows		= matrix.rows();

		double nRowsm1	= nRows - 1.0D;

								// Will hold means for each column.

        Matrix columnMeans			= MatrixFactory.createMatrix( 1 , nCols );

								// Will hold standard deviations for
								// each column.

        Matrix columnStdDeviations	= MatrixFactory.createMatrix( 1 , nCols );

                                // Loop over columns.

		for ( int j = 1 ; j <= nCols ; j++ )
		{
			double mean			= 0.0D;

			for ( int i = 1 ; i <= nRows ; i++ )
			{
				mean	+=	matrix.get( i , j );
			}

			if ( nRows > 0 )
			{
				mean	= mean / nRows;
			}

			columnMeans.set( 1 , j , mean );

			double sumSquares	= 0.0D;
			double centered		= 0.0D;

			for ( int i = 1 ; i <= nRows ; i++ )
			{
				centered	=	matrix.get( i , j ) - mean;
				sumSquares	+=	centered * centered;
			}

			double stdDeviation	= 0.0D;

			if ( nRows > 1 )
			{
				stdDeviation	= Math.sqrt( sumSquares  / nRows );
			}

			columnStdDeviations.set( 1 , j , stdDeviation );

			if ( stdDeviation != 0.0D )
			{
				for ( int i = 1 ; i <= nRows ; i++ )
				{
					matrix.set
					(
						i , j ,
						( matrix.get( i , j ) - mean ) /
						( Math.sqrt( (double)nRows ) * stdDeviation )
					);
				}
			}
		}

		Matrix[] result	= new Matrix[ 2 ];

		result[ 0 ]		= columnMeans;
		result[ 1 ]		= columnStdDeviations;

		return result;
	}

	/** Don't allow instantiation, but allow subclassing. */

	protected Standardize()
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

