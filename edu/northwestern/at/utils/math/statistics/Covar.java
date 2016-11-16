package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;

/** Covar -- get covariance or correlation matrix for an (n x m) matrix.
 *
 *	<p>
 *	Philip R. Burns.  2004/06/25.
 *	</p>
 */

public class Covar
{
	/** Generate a covariance matrix.
	 *
	 *	@param		dataMatrix	The n x m data matrix for which to compute
	 *							a covariance matrix.
	 *
	 *	@return					The n-by-n covariance matrix.
	 */

	public static Matrix covariance( Matrix dataMatrix )
	{
								// Get number of rows and columns
								// in data matrix.

		int	nCols	= dataMatrix.columns();
		int	nRows	= dataMatrix.rows();

								// Get degrees of freedom for
								// variances and covariances.  This
								// is the number of data rows - 1.

		int df		= ( nRows  - 1 );

								// Allocate a square matrix to hold
								// the variances and covariances.

		Matrix covarianceMatrix	= MatrixFactory.createMatrix( nCols , nCols );

                                // Begin accumulating the sums of squares
                                // and cross-products.

		for ( int i = 1 ; i <= nCols ; i++ )
		{
			for ( int j = i ; j <= nCols ; j++ )
			{
				double crossProduct	= 0.0D;
				double ssi			= 0.0D;
				double ssj			= 0.0D;

                                // Get sum of squares.

				for ( int k = 1 ; k <= nRows ; k++ )
				{
					ssi += dataMatrix.get( k , i );
					ssj += dataMatrix.get( k , j );
				}

				ssi = ssi / nRows;
				ssj = ssj / nRows;

								// Get sum of cross products.

				for ( int k = 1 ; k <= nRows ; k++ )
				{
					crossProduct +=
						( dataMatrix.get( k , i ) - ssi ) *
						( dataMatrix.get( k , j ) - ssj );
				}
								// Divide cross product term by
								// degrees of freedom to
								// obtain variance or covariance.

				covarianceMatrix.set( i , j , crossProduct / df );
				covarianceMatrix.set( j , i , crossProduct / df );
			}
		}

		return covarianceMatrix;
	}

	/** Generate a correlation matrix.
	 *
	 *	@param		dataMatrix	The n x m data matrix for which to compute
	 *							a correlation matrix.
	 *
	 *	@return					The n-by-n correlation matrix.
	 */

	public static Matrix correlation( Matrix dataMatrix )
	{
								// Get the variance-covariance matrix.

		Matrix covarianceMatrix		= covariance( dataMatrix );
		Matrix correlationMatrix	= covarianceMatrix.getCopy();

								// Convert to correlation matrix by
								// dividing each element by
								// appropriate variances.

		double corr;

		for ( int i = 1 ; i <= covarianceMatrix.columns() ; i++ )
		{
			for ( int j = i ; j <= covarianceMatrix.columns() ; j++ )
			{
				double divisor	=
					Math.sqrt(	covarianceMatrix.get( i , i ) *
								covarianceMatrix.get( j , j ) );

				if ( divisor != 0.0D )
				{
					corr	= covarianceMatrix.get( i , j ) / divisor;
				}
				else
				{
					corr	= 0.0D;
				}

				correlationMatrix.set( i , j , corr );
				correlationMatrix.set( j , i , corr );
			}
		}

		return correlationMatrix;
	}

	/** Don't allow instantiation, but allow subclassing. */

	protected Covar()
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

