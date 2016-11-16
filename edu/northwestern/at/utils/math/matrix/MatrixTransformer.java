package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.util.ArrayList;
import java.util.Iterator;

/**	Transform all matrix entries element-by-element.
 */

public class MatrixTransformer
{
	/** Get cumulative column products for a matrix.
	 *
	 *	@param	matrix	The matrix.
	 *	@return			The cumulative column products as a matrix.
	 */

	public static Matrix cumulativeColumnProduct( Matrix matrix )
	{
		Matrix result	=
			MatrixFactory.createMatrix( matrix.rows() , matrix.columns() );

		for ( int row = 1; row <= matrix.rows(); row++ )
		{
			for ( int col = 1; col <= matrix.columns(); col++ )
			{
				if ( row == 1 )
				{
					result.set( row , col , matrix.get( row , col ) );
				}
				else
				{
					result.set(
						row ,
						col ,
						result.get( row - 1 , col ) * matrix.get( row , col ) );
				}
			}
		}

		return result;
	}

	/** Get cumulative column sums for a matrix.
	 *
	 *	@param	matrix	The matrix.
	 *	@return			The cumulative column sums as a matrix.
	 */

	public static Matrix cumulativeColumnSum( Matrix matrix )
	{
		Matrix result	=
			MatrixFactory.createMatrix(
				matrix.rows() , matrix.columns() );

		for ( int row = 1 ; row <= matrix.rows() ; row++ )
		{
			for ( int col = 1 ; col <= matrix.columns() ; col++ )
			{
				if ( row == 1 )
				{
					result.set( row , col , matrix.get( row , col ) );
				}
				else
				{
					result.set(
						row ,
						col ,
						result.get( row - 1 , col ) + matrix.get( row , col ) );
				}
			}
		}

		return result;
	}

	/** Get diagonal elements of a matrix.
	 *
	 *	@param	matrix	The matrix.
	 *	@return			The diagonal elements in a matrix with one column.
	 */

	public static Matrix diagonal( Matrix matrix )
	{
		int rows	= matrix.rows();
		int columns	= matrix.columns();

		Matrix diagonal	= null;

		if ( ( rows == 1 ) && ( columns == 1 ) )
		{
			return MatrixFactory.createMatrix( 1 , 1 , matrix.get( 1 , 1 ) );
		}
		else
		{
			diagonal	= MatrixFactory.createMatrix( rows , 1 );

			for ( int row = 1 ; row <= rows ; row++ )
			{
				for ( int column = 1 ; column <= columns ; column++ )
				{
					if ( row == column )
						diagonal.set( row , 1 , matrix.get( row , column ) );
				}
			}
		}

		return diagonal;
	}

	/**	Compose matrix with a diagonal specified by row or column vector.
	 *
	 *	@param	matrix		Row or column vector, e.g., (n x 1) or (1 x n)
	 *						matrix.
	 *
	 *	@param	offset		Diagonal to be embeded
	 *						( =0 will embed the elements as the main diagonal )
	 *
	 *	@return				Matrix with the specified diagonal elements
	 *						set to the vector contents, and zero elsewhere.
	 */

	public static Matrix embedDiagonal( Matrix matrix , int offset )
	{
		switch ( matrix.rows() )
		{
			case 1:
								//	If row matrix, convert into column matrix
								//	and let program execution fall through
								//	to the next case.

				matrix = MatrixTransformer.transpose( matrix );

			default:
								//	Mot a row matrix.

				switch ( matrix.columns() )
				{

					case 1:
						int d	=
							Math.abs( offset ) + MatrixMeasure.length( matrix );

							Matrix dm	= MatrixFactory.createMatrix( d , d );

							if ( offset > 0 )
							{
								for	(	int row = 1 ;
										row <= MatrixMeasure.length( matrix ) ;
										row++
									)
								{
									dm.set(
										row ,
										row + offset ,
										matrix.get( row , 1 ) );
								}
							}
							else
							{
								for (	int row = 1 ;
										row <= MatrixMeasure.length( matrix ) ;
										row++
									)
								{
									dm.set(
										row + Math.abs( offset ) ,
										row ,
										matrix.get( row , 1 ) );
								}
							}

							return dm;

					default:
						throw new MatrixMismatchedSizeException(
							"Matrix must be a row or column vector" );
				}
		}
	}

	/**	Get elements of indicated diagonal (offset) as a column vector.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@param	offset	The diagonal to be extracted
	 *					(=0 will extract the main diagonal)
	 *
	 *	@return			Column vector of elements extracted from the
	 *					indicated diagonal.
	 */

	public static Matrix extractDiagonal( Matrix matrix , int offset)
	{
		ArrayList list	= new ArrayList( 10 );

		if ( MatrixProperty.isVector( matrix ) )
		{
			throw new IllegalArgumentException(
				" Matrix cannot be a vector" );
		}
		else
		{
			int length	= MatrixMeasure.length( matrix );

			if ( offset > 0 )
			{
				for ( int row = 1 ; row <= length - offset ; row++ )
				{
					if	(	( row <= matrix.rows() ) &&
							( ( row + offset ) <= matrix.columns() ) )
					{
						list.add(
							new Double( matrix.get( row , row + offset ) ) );
					}
				}
			}
			else
			{
				if ( ( length + offset ) < 1 )
					throw new MatrixMismatchedSizeException(
						"Length of the matrix and offset combined yield " +
						"invalid matrix indices" );

				for ( int row = 1 ; row <= length + offset ; row++ )
				{
					if (	( ( row + Math.abs( offset ) ) <= matrix.rows() ) &&
							( row <= matrix.columns() ) )
					{
						list.add(
							new Double(
								matrix.get( row + Math.abs( offset ) , row ) ) );
					}
				}
			}
		}

		return MatrixFactory.createMatrix( list );
	}

	/**	Extracts lower triangular matrix given an offset that indicates
	 *	the relative diagonal.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@param	offset	The diagonal to be extracted
	 *					(=0 will extract the main diagonal)
	 *
	 *	@return			Matrix containing specified lower triangle.
	 */

	public static Matrix extractLowerTriangle( Matrix matrix , int offset )
	{
		return MatrixOperator.subtract
		(	matrix ,
			MatrixTransformer.extractUpperTriangle( matrix , offset + 1 )
		);
	}

	/**	Extracts upper triangular matrix given an offset that indicates
	 *	the relative diagonal.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@param	offset	The diagonal to be extracted
	 *					(=0 will extract the main diagonal)
	 *
	 *	@return			Matrix containing specified upper triangle.
	 */

	public static Matrix extractUpperTriangle( Matrix matrix , int offset)
	{
		int rows	= matrix.rows();
		int columns	= matrix.columns();

		Matrix result	= MatrixFactory.createMatrix( rows , columns );

		for	(	int row = 1 ;
				row <= MatrixMeasure.length( matrix ) - Math.abs( offset ) ;
				row++
			)
		{
			for	(	int column = row + Math.abs( offset ) ;
					column <= MatrixMeasure.length( matrix );
					column++
				)
			{
				if ( ( row <= rows ) && ( column <= columns ) )
					result.set( row , column , matrix.get( row , column ) );
			}
		}

		if ( offset < 0 )
		{
			for ( int row = 1 ; row <= matrix.rows() ; row++ )
			{
				for	(	int column = Math.max( 1 , row + offset ) ;
						column <= Math.min(
							MatrixMeasure.length( matrix ) , row - offset - 1 ) ;
						column++
					)
				{
					if ( ( row <= rows ) && ( column <= columns ) )
						result.set( row , column , matrix.get( row , column ) );
				}
			}
		}

		return result;
	}

	/**	Invert a square matrix.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return 		The inverse of matrix, if matrix is square and
	 *					of full rank.
	 */

	public static Matrix inverse( Matrix matrix )
	{
		return MatrixOperator.solve
		(
			matrix ,
			MatrixFactory.createIdentityMatrix( matrix.rows() )
		);
	}

	/**	Negate all elements of a matrix.
	 *
	 *	@param	matrix	Matrix.
	 *
	 *	@return 		Matrix containing all elements of matrix negated.
	 */

	public static Matrix negate( Matrix matrix )
	{
		return
			MatrixEBETransformer.ebeTransform
			(
				matrix ,
				new MatrixEBETransformation()
				{
					public double transform( double element )
					{
						return -element;
					}
				}
			);
	}

	/**	Multiply square matrix by itself one or more times.
	 *
	 *	@param	matrix	The matrix (must be square).
	 *
	 *	@param	power	If power > 1 performs the operation.
	 *					If power <= 0 returns a matrix composed of ones.
	 *
	 *	@return			The resulting matrix.
	 */

	public static Matrix pow( Matrix matrix , int power )
	{
		if ( MatrixProperty.isSquare( matrix ) )
		{
			if ( power <= 0 )
			{
				return MatrixFactory.createMatrix(
					matrix.rows() , matrix.columns() , 1 );
			}

			Matrix result	= matrix;

			for ( int counter = 2 ; counter <= power ; counter++ )
			{
				result	= MatrixOperator.multiply( matrix , result );
			}

			return result;
		}
		else
		{
			throw new MatrixMismatchedSizeException(
				"Matrix must be square" );
		}
	}

	/**	Get transpose of matrix.
	 *
	 *	@param	matrix	The matrix whose transpose we want.
	 *
	 *	@return 		The transpose of matrix.
	 */

	public static Matrix transpose( Matrix matrix )
	{
		int rows		= matrix.rows();
		int columns		= matrix.columns();

		Matrix result	= MatrixFactory.createMatrix( columns , rows );

		for ( int row = 1 ; row <= rows ; row++ )
		{
			for ( int column = 1 ; column <= columns ; column++ )
			{
				result.set( column , row , matrix.get( row , column ) );
			}
		}

		return result;
	}

	/**	Zero out small elements (within specified tolerance) of a matrix.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@param	tolerance	Tolerance for zero checking.
	 *
	 *	@return 			Matrix with elements whose absolute values is
	 *						<= tolerance set to zero.
	 */

	public static Matrix zeroize( Matrix matrix , double tolerance )
	{
		final double finalTolerance	= tolerance;

		return
			MatrixEBETransformer.ebeTransform
			(
				matrix ,
				new MatrixEBETransformation()
				{
					public double transform( double element )
					{
						return
							( Math.abs( element ) <= finalTolerance ) ?
								0.0D : element;
					}
				}
			);
	}

	/**	Zero out small elements (within specified tolerance) of a matrix.
	 *
	 *	@param	matrix		Matrix.
	 *
	 *	@param	tolerance	Tolerance for zero checking.
	 *
	 *	@return 			Matrix with elements whose absolute values is
	 *						<= tolerance set to zero.
	 *
	 *	<p>
	 *	Alternate spelling to keep Brits happy.
	 *	</p>
	 */

	public static Matrix zeroise( Matrix matrix , double tolerance )
	{
		return zeroize( matrix , tolerance );
	}

	/** Don't allow instantiation but do allow overrides. */

	protected MatrixTransformer()
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

