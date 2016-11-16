package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;
import edu.northwestern.at.utils.math.distributions.*;

/** SpearmanCorr -- get Spearman rank order correlations for an m x n matrix.
 */

public class SpearmanCorr
{
	/** Generate a Spearman rank-order correlation matrix.
	 *
	 *	@param		dataMatrix	The n x m data matrix for which to compute
	 *							a Spearman rank-order correlation matrix.
	 *
	 *	@return					The n-by-n correlation matrix.
	 *
 	 *	<p>
 	 *  We calculate Spearman's rank-order correlation coefficient
 	 *  by computing the rank-order of each variable stored as a column
 	 *	in a matrix.  Ties are handled by mid-ranking.  Computing the
 	 *	usual Pearson correlation on the rank ordered data yields
 	 *	Spearman's correlation coefficient.
	 */

	public static Matrix spearmanCorr( Matrix dataMatrix )
	{
								//	Get number of rows and columns
								//	in data matrix.

		int	nCols	= dataMatrix.columns();
		int	nRows	= dataMatrix.rows();

								//	Get a copy of the data matrix to hold
								//	the rank order values.

		Matrix rankOrderDataMatrix	= dataMatrix.getCopy();

								//	Get rank order values for each
								//	column in the copy of the data matrix.

		RankOrder.rankOrder( rankOrderDataMatrix );

								//	Compute Pearson correlation on the
								//	rank-ordered data.  This yields
								//	the Spearman rank-order correlations.

		Matrix correlationMatrix	=
			Covar.correlation( rankOrderDataMatrix );

								//	Return the Spearman correlations.

		return correlationMatrix;
	}

	/**	Calculate approximate significance of Spearman correlation coefficient.
	 *
	 *	@param	r		The Spearman correlation coefficient.
	 *	@param	n		The sample size.
	 *
	 *	@return			The approximate significance level
	 *					of the Spearman coefficient.
	 *
	 *	<p>
	 *	The approximate significance of Spearman's correlation r is computing
	 *	using Student's t distribution with ( n - 2 ) degrees of freedom,
	 *	as follows:
	 *	<p>
	 *
	 *	<p>
	 *	df = n - 2<br />
	 *	t = r * sqrt( df / ( 1 - r^2 ) )<br />
	 *	sig(r) = sigt( t , df )<br />
	 *	</p>
	 *
	 *	<p>
	 *	If r is exactly one, the significance is returned as zero.
	 *	</p>
	 */

	public static double sigSpearmanCorr( double r , int n )
	{
		double result	= 0.0D;
		double f		= ( 1.0D - r ) * ( 1.0D + r );

		if ( f > 0.0D )
		{
			double df	= n - 2.0D;
			double t	= r * Math.sqrt( df / f );
			result		= Sig.t( t , df );
		}

		return result;
	}

	/** Don't allow instantiation, but do allow subclassing. */

	protected SpearmanCorr()
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

