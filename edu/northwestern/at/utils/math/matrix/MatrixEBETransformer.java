package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	Apply a transformation to each element of a matrix.
 */

public class MatrixEBETransformer
{
	/**	Performs an element by element transformation on a matrix.
	 *
	 *	@param	matrix	Matrix to transform.
	 *	@param	mebet	MatrixEBETransformation object handling transformation.
	 *
	 *	@return			The transformed matrix.
	 */

	public static Matrix ebeTransform
	(
		Matrix matrix ,
		MatrixEBETransformation mebet
	)
	{
		Matrix transformed	= null;

		if ( matrix != null )
		{
			int rows	= matrix.rows();
			int cols	= matrix.columns();

			transformed	= MatrixFactory.createMatrix( rows , cols );

			for ( int row = 1 ; row <= rows ; row++ )
			{
				for ( int col = 1 ; col <= cols ; col++ )
				{
					transformed.set
					(
						row,
						col,
						mebet.transform( matrix.get( row , col ) )
					);
				}
			}
		}

		return transformed;
	}

	/**	Performs ebeTransform on a matrix.
	 *
	 *	@param	matrix	Matrix to be transformed.
	 *	@param	mcebet	MatrixConditionalEBETransformation object
	 *					handling transformation.
	 *
	 *	@return			Transformed matrix
	 */

	public static Matrix ebeTransform
	(
		Matrix matrix ,
		MatrixConditionalEBETransformation mcebet
	)
	{
		Matrix transformed	= null;

		if ( matrix != null )
		{
			int rows	= matrix.rows();
			int cols	= matrix.columns();

			transformed	= MatrixFactory.createMatrix( rows , cols );

			for ( int row = 1 ; row <= rows ; row++ )
			{
				for ( int col = 1 ; col <= cols ; col++ )
				{
					transformed.set
					(
						row,
						col,
						mcebet.transform( row , col , matrix.get( row , col ) )
					);
				}
			}
		}

		return transformed;
	}

	/** Don't allow instantiation but do allow overrides. */

	protected MatrixEBETransformer()
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

