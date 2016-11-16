package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.ArithUtils;
import edu.northwestern.at.utils.math.Constants;

/**	MatricesMeasure provides methods applicable to two (or more) matrices.
 *
 *	<p>
 *	Given two matrices A and B, returns a single value which is a function of A and B.
 * 	</p>
 */

public class MatricesMeasure
{
	/**	Dot product of two matrices.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		The dot product of the two matrices.
	 *
	 *	<p>
	 *	The two matrices must be the same size.  The dot product is the sum
	 *	of the element-wise products of the two matrices.
	 *	</p>
	 */

	public static double dotProduct( Matrix a , Matrix b )
	{
		if ( areSameSize( a , b ) )
		{
			return MatrixMeasure.sum( MatrixOperator.multiplyEBE( a , b ) );
		}
		else
		{
			throw new MatrixMismatchedSizeException(
				"Matrices must have same size to compute dot product." );
		}
	}

	/**	Compares all elements of two matrices for equality to specified tolerance.
	 *
	 *	@param	a			First matrix.
	 *	@param	b			Second matrix.
	 *	@param	tolerance	Tolerance.
	 *
	 *	@return 	true if all corresponding elements in a and b are equal.
	 *
	 *	<p>
	 *	Two matrix elements are deemed equal if
	 *		|a(i,j) - b(i,j)| <= tolerance.
	 *	</p>
	 */

	public static boolean areEqual( Matrix a , Matrix b , double tolerance )
	{
		boolean	result	= false;

		if ( areSameSize( a , b ) )
		{
			for ( int row = 1 ; row <= a.rows() ; row++ )
			{
				for ( int col = 1 ; col <= a.columns() ; col++ )
				{
					if	(	!ArithUtils.areEqual(
								a.get( row , col ) ,
								b.get( row , col ) ,
								tolerance ) )
						return false;
				}
			}

			result	= true;
		}

		return result;
	}

	/**	Compares all elements of two matrices for equality.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return 	true if all corresponding elements in a and b are
	 *				exactly equal.
	 */

	public static boolean areEqual( Matrix a , Matrix b )
	{
		return areEqual( a , b , Constants.MACHEPS );
	}

	/**	Compares size of two matrices.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		True if a and b have the same number of rows and columns.
	 */

	public static boolean areSameSize( Matrix a , Matrix b )
	{
		return ( ( a.rows() == b.rows() ) && ( a.columns() == b.columns() ) );
	}
}

/*
 * <p>
 * JMatrices is copyright &copy; 2001-2004 by Piyush Purang.
 * </p>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * </p>
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </p>
 * <p>
 * Modifications 2004-2006 by Northwestern University.
 * </p>
 */

