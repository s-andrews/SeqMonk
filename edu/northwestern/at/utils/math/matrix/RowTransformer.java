package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	RowTransformer performs operations that can be carried out on rows of a matrix.
 *
 *	<p>
 *	Given a matrix of dimension mxn M -yields- C, where C is a column vector
 *	of dimension mx1.
 *	</p>
 */

public class RowTransformer
{
	/**	Sums all rows and returns sums as a column vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			c(mx1) column vector with row sums.
	 */

	public static Matrix sum( Matrix matrix )
	{
		return MatrixTransformer.transpose
		(
			ColumnTransformer.sum( MatrixTransformer.transpose( matrix ) )
		);
	}

	/**	Multiplies all elements in a row and returns them as a column vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			c(mx1) column vector with row products.
	 */

	public static Matrix product( Matrix matrix )
	{
		return MatrixTransformer.transpose
		(
			ColumnTransformer.product( MatrixTransformer.transpose( matrix ) )
		);
	}

	/**	Get means of all the elements in a row.
	 *
	 *	@param	matrix		The matrix.
	 *	@param	adjustment	True to divide sums by (#rows - 1) instead of #rows.
	 *
	 *	@return				c(mx1) column vector with row means.
	 */

	public static Matrix mean( Matrix matrix , boolean adjustment )
	{
		return MatrixTransformer.transpose
		(
			ColumnTransformer.mean(
				MatrixTransformer.transpose( matrix ) , adjustment )
		);
	}

	/**	Gets maximum element in each row and returns them as a column vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			c(mx1) column vector with each row's maximum value.
	 */

	public static Matrix max( Matrix matrix )
	{
		return MatrixTransformer.transpose
		(
			ColumnTransformer.max( MatrixTransformer.transpose( matrix ) )
		);
	}

	/**	Gets minimum element in each column and returns them in a row vector.
	 *
	 *	@param	matrix		The matrix.
	 *
	 *	@return				c(mx1) column vector with each row's minimum value.
	 */

	public static Matrix min( Matrix matrix )
	{
		return MatrixTransformer.transpose
		(
			ColumnTransformer.min( MatrixTransformer.transpose( matrix ) )
		);
	}

	/**	Don't allow instantiation but do allow overrides. */

	protected RowTransformer()
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

