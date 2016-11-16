package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	MatrixOperator provides operations that can be performed on two or more matrices.
 *
 *	<p>
 *	Given two matrices A,B -yields- C, where C is another matrix.
 * 	</p>
 */

public class MatrixOperator
{
	/**	Applies element-by-element operation combining elements of two matrices.
	 *
	 *	@param	a					First matrix.
	 *	@param	b					Second Matrix.
	 *	@param	matrixOperation		Operation to be performed on
	 *								corresponding elements of a and b.
	 *
	 *	@return						Matrix resulting from operation.
	 *
	 *	@throws						MatrixMismatchedSizeException
	 *									when a and b are not the same size.
	 */

	public static Matrix applyEBEOperation
	(
		Matrix a ,
		Matrix b ,
		MatrixEBEOperation matrixOperation
	)
	{
		int rows_a		= a.rows();
		int columns_a	= a.columns();

		int rows_b		= b.rows();
		int columns_b	= b.columns();

		Matrix result;

		if ( ( rows_a != rows_b ) && ( columns_a != columns_b ) )
		{
			throw new MatrixMismatchedSizeException(
				"Dimensions of a and b do not conform." );
		}
		else
		{
			result	= MatrixFactory.createMatrix( rows_a , columns_a );

			for ( int row = 1 ; row <= rows_a ; row++ )
			{
				for ( int column = 1 ; column <= columns_a ; column++ )
				{
					result.set
					(
						row ,
						column ,
						matrixOperation.apply
						(
							a.get( row , column ) , b.get( row , column )
						)
					);
				}
			}
		}

		return result;
	}

	/**	Solves system of equations.
	 *
	 *	@param	x	The coefficient matrix.  Need not be square.
	 *	@param	b	The constant vector.
	 *
	 *	@return		The solution matrix or c = x / b .
	 *
	 *	<p>
	 *	When the matrix is square, uses an LU decomposition.
	 *	Otherwise uses a QR decomposition.
	 *	</p>
	 */

	public static Matrix solve( Matrix x , Matrix b )
	{
		return
			MatrixProperty.isSquare( x ) ?
				new LUDecomposition( x ).solve( b ) :
				new QRDecomposition( x ).solve( b );
	}

	/**	Matrix addition.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing elementwise sums of each element
	 *				in a and b.
	 */

	public static Matrix add( Matrix a , Matrix b )
	{
		return applyEBEOperation
		(
			a ,
			b ,
			new MatrixEBEOperation()
			{
				public double apply( double a , double b )
				{
					return a + b;
				}
			}
		);
	}

	/**	Matrix subtraction.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing elementwise differences of each element
	 *				in a and b.
	 */

	public static Matrix subtract( Matrix a , Matrix b )
	{
		return applyEBEOperation
		(
			a ,
			b ,
			new MatrixEBEOperation()
			{
				public double apply( double a , double b )
				{
					return a - b;
				}
			}
		);
	}

	/**	Matrix multiplication.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix multiplication of a * b .
	 *
	 *	@throws		MatrixMismatchedSizeException
	 *					when a and b do not conform for multiplication.
	 */

	public static Matrix multiply( Matrix a , Matrix b )
	{
		int rows_a		= a.rows();
		int columns_a	= a.columns();

		int rows_b		= b.rows();
		int columns_b	= b.columns();

		Matrix result;

		if ( columns_a != rows_b )
		{
			throw new MatrixMismatchedSizeException(
				"Dimensions of matrices do not conform for multiplication" );
		}
		else
		{
			result	= MatrixFactory.createMatrix( rows_a , columns_b );

								// Pick row in a.

			for ( int row_a = 1 ; row_a <= rows_a ; row_a++ )
			{
								// Pick a column in b.

				for ( int column_b = 1 ; column_b <= columns_b ; column_b++ )
				{
								// Iterate over all cols in the selected
								// row for a .

					for ( int column_a = 1 ; column_a <= columns_a ; column_a++ )
					{
						double sum	= 0.0D;

								// Iterate over all rows in the selected
								// column for b.

						for ( int row_b = 1 ; row_b <= rows_b ; row_b++ )
						{
							sum	+=
								a.get( row_a , row_b ) *
								b.get( row_b , column_b );
						}

						result.set( row_a , column_b , sum );
					}
				}
			}
		}

		return result;
	}

	/**	Element by element matrix multiplication.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing elementwise products of each element
	 *				in a and b.
	 */

	public static Matrix multiplyEBE( Matrix a , Matrix b )
	{
		return applyEBEOperation
		(
			a ,
			b ,
			new MatrixEBEOperation()
			{
				public double apply( double a , double b )
				{
					return a * b;
				}
			}
		);
	}

	/**	Element by element matrix division.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing elementwise quotients of each element
	 *				in a and b.
	 *
	 *	<p>
	 *	If a division is attempted, the result is set to zero.
	 *	</p>
	 */

	public static Matrix divideEBE( Matrix a , Matrix b )
	{
		return applyEBEOperation
		(
			a ,
			b ,
			new MatrixEBEOperation()
			{
				public double apply( double a , double b )
				{
					double result	= 0.0D;

					if ( b != 0.0D ) result	= a / b;

					return result;
				}
			}
		);
	}

	/**
	 * Concatenates <code>a</code> and <code>b</code> horizontally with
	 * <code>b</code>'s columns attached to the end of <code>a</code>
	 * <p/>
	 * rows of <code>a</code> must be equal to rows of <code>b</code>
	 * <br/>
	 * <strong>Note:</strong> Matrix <code>a</code>'s underlying implementation is propogated in the resulting matrix
	 * <br/>
	 *
	 * @param a Matrix
	 * @param b Matrix
	 * @return c = a~b //gauss syntax
	 */

	public static Matrix horizontalConcatenation(Matrix a, Matrix b)
	{
		int rows_a = a.rows(), cols_a = a.columns(), rows_b = b.rows(), cols_b = b.columns();
		Matrix c;

		if (rows_a != rows_b)
		{
			throw new IllegalArgumentException("Dimensions of a and b don't conform");
		}
		else
		{
			c = MatrixFactory.createMatrix(rows_a, cols_a + cols_b);
			for (int row = 1; row <= rows_a; row++)
			{
				for (int col_a = 1; col_a <= cols_a; col_a++)
				{
					c.set(row, col_a, a.get(row, col_a));
				}
				for (int col_b = 1; col_b <= cols_b; col_b++)
				{
					c.set(row, cols_a + col_b, b.get(row, col_b));
				}
			}
		}
		return c;
	}

	/**
	 * Concatenates <code>a</code> and <code>b</code> vertically with
	 * <code>b</code>'s rows following the <code>a</code>'s rows
	 * <p/>
	 * cols of <code>a</code> must be equal to colss of <code>b</code>
	 * <br/>
	 * <strong>Note:</strong> Matrix <code>a</code>'s underlying implementation is propogated in the resulting matrix
	 * <br/>
	 *
	 * @param a Matrix
	 * @param b Matrix
	 * @return c = a|b //gauss syntax
	 */

	public static Matrix verticalConcatenation(Matrix a, Matrix b)
	{
		int rows_a = a.rows(), cols_a = a.columns(), rows_b = b.rows(), cols_b = b.columns();
		Matrix c;

		if (cols_a != cols_b)
		{
			throw new IllegalArgumentException("Dimensions of a and b don't conform");
		}
		else
		{
			c = MatrixFactory.createMatrix(rows_a + rows_b, cols_a);
			for (int col = 1; col <= cols_a; col++)
			{
				for (int row_a = 1; row_a <= rows_a; row_a++)
				{
					c.set(row_a, col, a.get(row_a, col));
				}
				for (int row_b = 1; row_b <= rows_b; row_b++)
				{
					c.set(rows_a + row_b, col, b.get(row_b, col));
				}
			}
		}
		return c;
	}

	/**	Gets the Kronecker (tensor) product of the two matrices.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing tensor product of a and b.
	 */

	public static Matrix kroneckerProduct( Matrix a , Matrix b )
	{
		/**
		 * 1. pick first row
		 * 2.      pick first column
		 * 3.          scalar multiplication of a(1,1) with b.
		 * 4.              assign to temp horizresult
		 * 5. repeat for second element in the row
		 * 6. do horizontal cocatenation with temp horizresult assign back to temp horizresult
		 * 7. so on till all columns are exhausted.
		 * 8. assign the resulting temp horizresult to  temp vertresult
		 * 9. repeat creating temp horizresult for next row
		 * 10. do vertical cocatenation with temp vertresult and assign back to temp vertresult
		 * 11 exhaust all rows.
		 */
		Matrix tmpVert = null;

		for (int row = 1; row <= a.rows(); row++)
		{
			Matrix horizVert = null;

			// columns in a row
			for (int col = 1; col <= a.columns(); col++)
			{
				final double scalar = a.get(row, col);

				if (horizVert == null)
					horizVert = MatrixEBETransformer.ebeTransform(b, new MatrixEBETransformation()
								{
									public double transform(double element)
									{
										return scalar * element;
									}
								}
							);
				else
					horizVert = MatrixOperator.horizontalConcatenation(horizVert, MatrixEBETransformer.ebeTransform(b, new MatrixEBETransformation()
									{
										public double transform(double element)
										{
											return scalar * element;
										}
									}
								));
			}
			if (tmpVert == null)
				tmpVert = horizVert;
			else
				tmpVert = MatrixOperator.verticalConcatenation(tmpVert, horizVert);
		}
		return tmpVert;
	}

	/**	Gets the Horizontal Direct Product of two matrices.
	 *
	 *	@param	a	First matrix.
	 *	@param	b	Second matrix.
	 *
	 *	@return		Matrix containing horizontal direct product of a and b.
	 */

	public static Matrix horizontalDirectProduct(Matrix a, Matrix b)
	{
		int rows_a = a.rows(), rows_b = b.rows();

		if (rows_a != rows_b)
			throw new IllegalArgumentException("Rows of a and b must be equal");
		else
		{
			Matrix tmpVert = null;

			for (int row = 1; row <= rows_a; row++)
			{
				Matrix rowA = a.getRow(row), rowB = b.getRow(row);

				if (tmpVert == null)
					tmpVert = MatrixOperator.kroneckerProduct(rowA, rowB);
				else
					tmpVert = MatrixOperator.verticalConcatenation(tmpVert, MatrixOperator.kroneckerProduct(rowA, rowB));
			}
			return tmpVert;
		}
	}

	/** Don't allow instantiation but do allow overrides. */

	protected MatrixOperator()
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

