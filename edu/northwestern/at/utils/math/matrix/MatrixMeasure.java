package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	MatrixMeasure computes important measures associated with a matrix.
 */

public class MatrixMeasure
{
	/**	Breadth of matrix, e.g., the minimum dimension.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			Smaller of rows and columns in the matrix.
	 */

	public static int breadth( Matrix matrix )
	{
		return Math.min( matrix.rows() , matrix.columns() );
	}

	/**	Condition number of matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Ratio of largest to smallest singular value.
	 */

	public static double condition( Matrix matrix )
	{
		return new SingularValueDecomposition( matrix ).cond();
	}

	/**	Determinant of matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Determinant.
	 *
	 *	<p>
	 *	We compute the determinant from the LU decomposition of the matrix.
	 *	</p>
	 */

	public static double determinant( Matrix matrix )
	{
		return ( new LUDecomposition( matrix ) ).det();
	}

	/**	Length of the matrix, e.g., the maximum dimension.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return 		Larger of rows and columns of matrix.
	 */

	public static int length( Matrix matrix )
	{
		return Math.max( matrix.rows() , matrix.columns() );
	}


	/**	Maximum value in matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Maximum value in matrix.
	 */

	public static double max( Matrix matrix )
	{
		return
			( RowTransformer.max(
				ColumnTransformer.max( matrix ) ) ).get( 1 , 1 );
	}

	/**	Mean of all matrix values.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@param	adjustment	True to divide by (n-1) instead of n, where
	 *						n is the number of values in the matrix.
	 *
	 *	@return 			Mean of all matrix entries.
	 */

	public static double mean( Matrix matrix , boolean adjustment )
	{
		double prod = matrix.rows() * matrix.columns();

		if ( adjustment )
			return ( ( prod / ( prod - 1.0D ) ) * mean( matrix , false ) );
		else
			return RowTransformer.mean(
				ColumnTransformer.mean(
					matrix , false ) , false ).get( 1 , 1 );
	}

	/**	Minimum value in matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Minimum value in matrix.
	 */

	public static double min( Matrix matrix )
	{
		return
			( RowTransformer.min(
				ColumnTransformer.min( matrix ) ) ).get( 1 , 1 );
	}

	/**	1-norm -- maximum column sum.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Manhanttan norm.
	 */

	public static double norm1( Matrix matrix )
	{
		return RowTransformer.max
		(
			ColumnTransformer.sum
			(
				MatrixEBETransformer.ebeTransform
				(
					matrix ,
					new MatrixEBETransformation()
					{
						public double transform( double element )
						{
							return Math.abs( element );
						}
					}
				)
			)
		).get( 1 , 1 );
	}

	/**	2-norm -- maximum singular value.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Maximum singular value.
	 */

	public static double norm2( Matrix matrix )
	{
		return new SingularValueDecomposition( matrix ).norm2();
	}

	/**	Infinity norm	-- maximum row sum.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Maximum row sum.
	 */

	public static double normInfinity( Matrix matrix )
	{
		return ColumnTransformer.max
		(
			RowTransformer.sum
			(
				MatrixEBETransformer.ebeTransform
				(	matrix ,
					new MatrixEBETransformation()
					{
						public double transform( double element )
						{
							return Math.abs( element );
						}
					}
				)
			)
		).get( 1 , 1 );
	}

	/**	Frobenius norm	-- square root of sum of squares of all entries.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Frobenius norm.
	 */

	public static double normFrobenius( Matrix matrix )
	{
		return Math.sqrt
		(
			MatrixMeasure.sum
			(
				MatrixEBETransformer.ebeTransform
				(	matrix ,
					new MatrixEBETransformation()
					{
						public double transform( double element )
						{
							return element * element;
						}
					}
				)
			)
		);
	}

	/**	Product of all matrix values.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return 		Product of all matrix values.
	 */

	public static double product( Matrix matrix )
	{
		return
			( RowTransformer.product(
				ColumnTransformer.product( matrix ) ) ).get( 1 , 1 );
	}

	/**	Rank of matrix.
	 *
	 *	@param	matrix	Matrix.
     *
	 *	@return			Rank.
	 *
	 *	<p>
	 *	The rank is computed from the Singular Value Decomposition.
	 *	</p>
	 */

	public static int rank( Matrix matrix )
	{
		return ( new SingularValueDecomposition( matrix ) ).rank();
	}

	/**	Sum of all matrix values.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Sum of all matrix values.
	 */

	public static double sum( Matrix matrix )
	{
		return
			( RowTransformer.sum(
				ColumnTransformer.sum( matrix ) ) ).get( 1 , 1 );
	}

	/**	Trace of matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			Trace of matrix.
	 */

	public static double trace( Matrix matrix )
	{
		return ColumnTransformer.sum(
			MatrixTransformer.diagonal( matrix ) ).get( 1 , 1 );
	}

	/** Don't allow instantiation but do allow overrides. */

	protected MatrixMeasure()
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

