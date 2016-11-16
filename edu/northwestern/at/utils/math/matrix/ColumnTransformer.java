package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	ColumnTransformer -- operations that can be carried out on columns of a matrix.
 *
 * <p>
 * Given a matrix of dimension mxn M -yields-> R,
 *	where R is a row vector of dimension 1xn.
 * </p>
 */

public class ColumnTransformer
{
	/**	Apply a column operation.
	 *
	 *	@param	matrix					The matrix.
	 *	@param	columnTransformation	The column transformation.
	 *
	 *	@return							The transformed matrix.
	 */

	public static Matrix applyColumnOperation
	(
		Matrix matrix ,
		ColumnTransformation columnTransformation
	)
	{
		throw new UnsupportedOperationException( "to be implemented" );
	}

	/**	Sums all the columns and returns them as a row vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			r(1xn) row vector with column sums.
	 */

	public static Matrix sum( Matrix matrix )
	{
		int rows	= matrix.rows();
		int columns	= matrix.columns();

		Matrix result;

		if ( rows == 1 )
		{
			return matrix;
		}
		else
		{
			result	= MatrixFactory.createMatrix( 1 , columns );

			for ( int column = 1 ; column <= columns ; column++ )
			{
				double columnSum	= 0.0D;

				for ( int row = 1 ; row <= rows ; row++ )
				{
					columnSum	+= matrix.get( row , column );
				}

				result.set( 1 , column , columnSum );
			}
		}

		return result;
	}

	/**	Multiplies all elements in a column and returns them as a row vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			r(1xn) row vector with column products.
	 */

	public static Matrix product( Matrix matrix )
	{
		int rows	= matrix.rows();
		int columns	= matrix.columns();

		Matrix result;

		if ( rows == 1 )
		{
			return matrix;
		}
		else
		{
			result	= MatrixFactory.createMatrix( 1 , columns );

			for ( int column = 1 ; column <= columns ; column++ )
			{
				double columnProduct	= 1.0D;

				for ( int row = 1 ; row <= rows ; row++ )
				{
					columnProduct	=
						columnProduct * matrix.get( row , column );

					if ( columnProduct == 0.0D ) break;
				}

				result.set( 1 , column , columnProduct );
			}
		}

		return result;
	}

	/**	Get means of all the elements in each column as a row vector.
	 *
	 *	@param	matrix		The matrix.
	 *	@param	adjustment	true to divide sum of values by (rows - 1) instead
	 *						of rows.
	 *
	 *	@return				r(1xn) row vector with column means.
	 */

	public static Matrix mean( Matrix matrix , boolean adjustment )
	{
		int rows		= matrix.rows();
		int columns		= matrix.columns();
		int denominator	= rows;

		if ( adjustment ) denominator--;

		Matrix result;

		if ( rows == 1 )
		{
			return matrix;
		}
		else
		{
			result	= MatrixFactory.createMatrix( 1 , columns );

			for ( int column = 1 ; column <= columns ; column++ )
			{
				double columnSum	= 0.0D;

				for ( int row = 1 ; row <= rows ; row++ )
				{
					columnSum	+= matrix.get( row , column );
				}

				result.set( 1 , column , columnSum / denominator );
			}
		}

		return result;
	}

	/**	Get maximum elements for each column as a row vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			r(1xn) row vector with each column's maximum value.
	 */

	public static Matrix max( Matrix matrix )
	{
		int rows		= matrix.rows();
		int columns		= matrix.columns();

		Matrix result;

		if ( rows == 1 )
		{
			return matrix;
		}
		else
		{
			result	= MatrixFactory.createMatrix( 1 , columns );

			for ( int column = 1 ; column <= columns ; column++ )
			{
				double maxValue	= matrix.get( 1 , column );

				for ( int row = 2 ; row <= rows ; row++ )
				{
					maxValue	=
						Math.max( maxValue , matrix.get( row , column ) );
				}

				result.set( 1 , column , maxValue );
			}
		}

		return result;
	}

	/**	Get minimum elements for each column as a row vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			r(1xn) row vector with each column's minimum value.
	 */

	public static Matrix min( Matrix matrix )
	{
		int rows		= matrix.rows();
		int columns		= matrix.columns();

		Matrix result;

		if ( rows == 1 )
		{
			return matrix;
		}
		else
		{
			result	= MatrixFactory.createMatrix( 1 , columns );

			for ( int column = 1 ; column <= columns ; column++ )
			{
				double minValue	= matrix.get( 1 , column );

				for ( int row = 2 ; row <= rows ; row++ )
				{
					minValue	=
						Math.min( minValue , matrix.get( row , column ) );
				}

				result.set( 1 , column , minValue );
			}
		}

		return result;
	}

	/** Don't allow instantiations but do allow overrides. */

	protected ColumnTransformer()
	{
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

