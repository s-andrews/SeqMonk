package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.ArithUtils;

/**	MatrixProperty returns a boolean value for a property of a matrix.
 */

public class MatrixProperty
{
	/**	Return bandwidth for matrix.
	 *
	 *	@param	matrix		The matrix.
	 *	@param	tolerance	Tolerance for checking for zero.
	 *
	 *	@return			Integer array with three entries.
	 *					[0]	= lower bandwidth
	 *					[1] = upper bandwidth
	 *					[2]	= total bandwidth
	 */

	public static int[] bandwidth( Matrix matrix , double tolerance )
	{
		int lowerBandWidth	= 0;
		int upperBandWidth	= 0;
		int diagNotZero		= 0;
		int rows			= matrix.rows();
		int columns			= matrix.columns();
		int minDimension	= Math.min( rows , columns );

		for ( int i = 1 ; i <= rows ; i++ )
		{
			for ( int j = ( i + 1 ) ; j <= columns ; j++ )
			{
				if ( !ArithUtils.areEqual(
					matrix.get( i , j ) , 0.0D , tolerance ) )
				{
					upperBandWidth	=
						Math.max( Math.abs( i - j ) , upperBandWidth );
				}
			}

			if	(	( i <= minDimension ) &&
					( !ArithUtils.areEqual(
						matrix.get( i , i ) , 0.0D , tolerance ) ) )
			{
				diagNotZero++;
			}

			for ( int j = 1 ; j < i ; j++ )
			{
				if ( !ArithUtils.areEqual(
					matrix.get( i , j ) , 0.0D , tolerance ) )
				{
					lowerBandWidth	=
						Math.max( Math.abs( i - j ) , lowerBandWidth );
				}
			}
		}

		int bandWidth	=
			lowerBandWidth + upperBandWidth + ( ( diagNotZero > 0 ) ? 1 : 0 );

		int[] result	= new int[ 3 ];

		result[ 0 ]		= lowerBandWidth;
		result[ 1 ]		= upperBandWidth;
		result[ 2 ]		= bandWidth;

		return result;
	}

	/**	Return bandwidth for matrix.
	 *
	 *	@param	matrix		The matrix.
	 *
	 *	@return			Integer array with three entries.
	 *					[0]	= lower bandwidth
	 *					[1] = upper bandwidth
	 *					[2]	= total bandwidth
	 */

	public static int[] bandwidth( Matrix matrix )
	{
		return bandwidth( matrix , 0.0D );
    }

	/**
	 * Determines whether or not a matrix is a Column Vector
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			true if m has only one column.
	 */

	public static boolean isColumnVector( Matrix matrix )
	{
		return ( matrix.columns() == 1 );
	}

	/**	Is square matrix diagonal.
	 *
	 *	@param	matrix		Matrix.
	 *	@param	tolerance	Tolerance for checking for zero.
	 *
	 *	@return				True if all elements other than those on the
	 *						main diagonal are zero to within the specified
	 *						tolerance, e.g., an element is considered zero
	 *						if |matrix(i,j)| <= tolerance.
	 */

	public static boolean isDiagonal( Matrix matrix , double tolerance )
	{
		if ( isSquare( matrix ) )
		{
			for ( int row = 1 ; row <= matrix.rows() ; row++ )
			{
				for ( int col = 1 ; col <= matrix.columns() ; col++ )
				{
					if	(	( row != col ) &&
							( !ArithUtils.areEqual(
								matrix.get( row , col ) , 0.0D, tolerance ) ) )
					{
						return false;
					}
				}
			}

			return true;
		}
		else
		{
			return false;
		}
	}

	/**	Is square matrix diagonal.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			True if all elements other than those on the
	 *					main diagonal are zero.
	 */

	public static boolean isDiagonal( Matrix matrix )
	{
		return isDiagonal( matrix , 0.0D );
	}

	/**	Is matrix high.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if the matrix has more rows than
	 *					columns.
	 */

	public static boolean isHigh( Matrix matrix )
	{
		return ( matrix.rows() > matrix.columns() );
	}

	/**	Is matrix idempotent.
	 *
	 *	@param	matrix		Matrix.
	 *	@param	tolerance	Tolerance for checking for zero.
	 *
	 *	@return				True if matrix is equal to its element-wise
	 *						product within the specified tolerance.
	 */

	public static boolean isIdempotent( Matrix matrix , double tolerance )
	{
		return MatricesMeasure.areEqual
		(
			matrix ,
			MatrixTransformer.pow( matrix , 2 ) ,
			tolerance
		);
	}

	/**	Is matrix idempotent.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@return				True if matrix is equal to its element-wise
	 *						product.
	 */

	public static boolean isIdempotent( Matrix matrix )
	{
		return MatricesMeasure.areEqual
		(
			matrix ,
			MatrixTransformer.pow( matrix , 2 ) ,
			0.0D
		);
	}

	/**	Is matrix an identity matrix.
	 *
	 *	@param	matrix		Matrix.  Must be square.
	 *	@param	tolerance	Tolerance for checking for zero.
	 *
	 *	@return				True if matrix is square, all off-diagonal
	 *						elements are zero to within tolerance,
	 *						and all the diagonal elements are 1 within
	 *						tolerance.
	 */

	public static boolean isIdentity( Matrix matrix , double tolerance )
	{
		return
			isSquare( matrix ) &&
				MatricesMeasure.areEqual
				(
					matrix ,
					MatrixFactory.createIdentityMatrix( matrix.rows() ) ,
					tolerance
				);
	}

	/**	Is matrix an identity matrix.
	 *
	 *	@param	matrix		Matrix.  Must be square.
	 *
	 *	@return				True if matrix is square, all off-diagonal
	 *						elements are zero, and all the diagonal elements
	 *						are 1.
	 */

	public static boolean isIdentity( Matrix matrix )
	{
		return
			isSquare( matrix ) &&
				MatricesMeasure.areEqual
				(
					matrix ,
					MatrixFactory.createIdentityMatrix( matrix.rows() )
				);
	}

	/**	Is matrix lower-triangular.
	 *
	 *	@param	matrix		Matrix.
	 *	@param	tolerance	Tolerance for checking for zero.
	 *
	 *	@return				True if matrix is lower triangular.
	 */

	public static boolean isLowerTriangular( Matrix matrix , double tolerance )
	{
		return MatricesMeasure.areEqual
		(
			MatrixTransformer.extractLowerTriangle( matrix , 0 ) ,
			matrix ,
			tolerance
		);
	}

	/**	Is matrix lower-triangular.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@return				True if matrix is lower triangular.
	 */

	public static boolean isLowerTriangular( Matrix matrix )
	{
		return MatricesMeasure.areEqual
		(
			MatrixTransformer.extractLowerTriangle( matrix , 0 ) ,
			matrix
		);
	}

	/**	Is matrix positive-definite.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@return				True if matrix is positive definite.
	 *
	 *	<p>
	 *	A matrix is positive definite if all its eigenvalues
	 *	are real and greater than zero.
	 *	</p>
	 */

	public static boolean isPositiveDefinite( Matrix matrix )
	{
		boolean result	= true;

		if ( isSquare( matrix ) )
		{
			EigenvalueDecomposition eigDecomp	=
				new EigenvalueDecomposition( matrix );

			double[] realEigenValues		= eigDecomp.getRealEigenvalues();
			double[] imaginaryEigenValues	= eigDecomp.getImagEigenvalues();

			for ( int i = 0 ; i < realEigenValues.length ; i++ )
			{
				result	=
					result && ( realEigenValues[ i ] > 0.0D ) &&
						( imaginaryEigenValues[ i ] == 0.0D );
			}
		}
		else
		{
			result	= false;
		}

		return result;
	}

	/**	Determines if matrix is a row vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if matrix has only one row.
	 */

	public static boolean isRowVector( Matrix matrix )
	{
		return ( matrix.rows() == 1 );
	}

	/**	Determines if square matrix is a scalar matrix.
	 *
	 *	@param	matrix		The matrix.
	 *	@param	tolerance	Tolerance for equality checking.
	 *
	 *	@return 			True if matrix is square and diagonal with
	 *						all diagonal elements equal.
	 */

	public static boolean isScalar( Matrix matrix , double tolerance )
	{
		double diagValue	= matrix.get( 1 , 1 );

		if ( isSquare( matrix ) )
		{
			for ( int row = 1 ; row <= matrix.rows() ; row++ )
			{
				for ( int column = 1 ; column <= matrix.columns() ; column++ )
				{
					if ( row == column )
					{
						if ( !ArithUtils.areEqual(
							diagValue , matrix.get( row , column ) , tolerance ) )
						{
							return false;
						}
					}
					else if ( matrix.get( row , column ) != 0 )
					{
						return false;
					}
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/**	Is matrix semipositive definite.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return			True if matrix is semipositive definite.
	 *
	 *	<p>
	 *	A matrix is semi-positive definition if all of its eigenvalues
	 *	are >= 0.
	 *	</p>
	 */

	public static boolean isSemiPositiveDefinite( Matrix matrix )
	{
		boolean result	= true;

		if ( isSquare( matrix ) )
		{
			EigenvalueDecomposition eigDecomp	=
				new EigenvalueDecomposition( matrix );

			double[] realEigenValues		= eigDecomp.getRealEigenvalues();
			double[] imaginaryEigenValues	= eigDecomp.getImagEigenvalues();

			for ( int i = 0 ; i < realEigenValues.length ; i++ )
			{
				result	=
					result && ( realEigenValues[ i ] >= 0.0D ) &&
						( imaginaryEigenValues[ i ] == 0.0D );
			}
		}
		else
		{
			result	= false;
		}

		return result;
	}

	/**	Determines if a square matrix is singular.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True is matrix is singular.
	 *
	 *	<p>
	 *	We use an LU decomposition to determine if the matrix is singular.
	 *	</p>
	 */

	public static boolean isSingular( Matrix matrix )
	{
		return new LUDecomposition( matrix ).isSingular();
	}

	/**	Is matrix skew symnmetric.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@param	tolerance	Tolerance for checking equality of matrix values.
	 *
	 *	@return				True iff A' = -A .
	 */

	public static boolean isSkewSymmetric( Matrix matrix , double tolerance )
	{
		return MatricesMeasure.areEqual
		(
			MatrixTransformer.transpose( matrix ) ,
			MatrixTransformer.negate( matrix ) ,
			tolerance
		);
	}

	/**	Is matrix skew symnmetric.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@return				True iff A' = -A .
	 */

	public static boolean isSkewSymmetric( Matrix matrix )
	{
		return MatricesMeasure.areEqual
		(
			MatrixTransformer.transpose( matrix ) ,
			MatrixTransformer.negate( matrix )
		);
	}

	/**	Determine if matrix is square.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if the matrix has the same number of rows as
	 *					columns.
	 */

	public static boolean isSquare( Matrix matrix )
	{
		return ( matrix.rows() == matrix.columns() );
	}

	/**	Is matrix symmetric.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@param	tolerance	Tolerance for checking equality of matrix values.
	 *
	 *	@return				True iff A' = A .
	 */

	public static boolean isSymmetric( Matrix matrix , double tolerance )
	{
		return MatricesMeasure.areEqual
		(
			matrix ,
			MatrixTransformer.transpose( matrix ) ,
			tolerance
		);
	}

	/**	Is matrix symmetric.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@return				True iff A' = A .
	 */

	public static boolean isSymmetric( Matrix matrix )
	{
		return MatricesMeasure.areEqual
		(
			matrix,
			MatrixTransformer.transpose( matrix )
		);
	}

	/**	Determine if matrix is symmetric positive definite.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if matrix is symmetric positive definite.
	 *
	 *	<p>
	 *	A Cholesky decomposition is used to determine if the matrix
	 *	is positive semidefinite.
	 *	</p>
	 */

	public static boolean isSymmetricPositiveDefinite( Matrix matrix )
	{
		return new CholeskyDecomposition( matrix ).isSPD();
	}

	/**	Determine if a diagonal matrix is an identity matrix.
	 *
	 *	@param	matrix		The matrix.
	 *	@param	tolerance	Tolerance for zero check.
	 *
	 *	@return				True if matrix is unit (identity) matrix.
	 */

	public static boolean isUnit( Matrix matrix , double tolerance )
	{
		return isIdentity( matrix , tolerance );
	}

	/**	Determine if a diagonal matrix is an identity matrix.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if matrix is identity matrix.
	 */

	public static boolean isUnit( Matrix matrix )
	{
		return isIdentity( matrix );
	}

	/**	Determines if matrix is upper triangular.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if matrix is upper triangular.
	 */

	public static boolean isUpperTriangular( Matrix matrix )
	{
		return MatricesMeasure.areEqual(
			MatrixTransformer.extractUpperTriangle( matrix , 0 ) , matrix );
	}

	/**	Determine if matrix is a vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if matrix had only one row or column.
	 */

	public static boolean isVector( Matrix matrix )
	{
		return ( ( matrix.columns() == 1 ) || ( matrix.rows() == 1 ) );
	}

	/**	Determine if matrix is wide.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			True if the matrix has more coluns than
	 *					rows.
	 */

	public static boolean isWide( Matrix matrix )
	{
		return ( matrix.rows() < matrix.columns() );
	}

	/** Don't allow instantiation but do allow overrides. */

	protected MatrixProperty()
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

