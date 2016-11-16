package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/** Cholesky Decomposition.
 *
 *	<p>
 *	For a symmetric, positive definite matrix A, the Cholesky decomposition
 *	is an lower triangular matrix L so that A = L*L'.
 *	</p>
 *
 *	<p>
 *	If the matrix is not symmetric or positive definite, the constructor
 *	returns a partial decomposition and sets an internal flag that may
 *	be queried by the isSPD() method.
 *	</p>
 *
 *	<p>
 *	This is a slight modification of the JAMA implementation.
 *	</p>
 */

public class CholeskyDecomposition
{
	/** Array for internal storage of decomposition. */

	private double[][] L;

	/** Row and column dimension (square matrix). */

	private int n;

	/** Symmetric and positive definite flag. */

	private boolean isspd;

	/** Perform Cholesky decomposition of symmetric positive definite matrix.
	 *
	 *	@param	matrix	Square, symmetric matrix.
	 */

	public CholeskyDecomposition( Matrix matrix )
	{
								// Initialize.

		double[][] A	= matrix.get();

								// Number of rows in matrix.

		n				= matrix.rows();

								// Holds computed lower triangular factorization.

		L				= new double[ n ][ n ];

								// Must have same number of rows as columns!

		isspd			= ( matrix.columns() == n );

								// Main loop.

		for  ( int j = 0 ; j < n ; j++ )
		{
			double[] Lrowj	= L[ j ];
			double d		= 0.0D;

			for ( int k = 0 ; k < j ; k++ )
			{
				double[] Lrowk	= L[ k ];
				double s		= 0.0D;

				for ( int i = 0 ; i < k ; i++ )
				{
					s	+= Lrowk[ i ] * Lrowj[ i ];
				}

				Lrowj[ k ]	= s	= ( A[ j ][ k ] - s ) / L[ k ][ k ];

				d			= d + s * s;

				isspd		= isspd && ( A[ k ][ j ] == A[ j ][ k ] );
			}

			d			= A[ j ][ j ] - d;

			isspd		= isspd && ( d > 0.0D );

			L[ j ][ j ]	= Math.sqrt( Math.max( d , 0.0D ) );

			for ( int k = j + 1 ; k < n ; k++ )
			{
				L[ j ][ k ] = 0.0D;
			}
		}
	}

	/**	Is the matrix symmetric and positive definite?
	 *
	 *	@return		True if A is symmetric and positive definite.
	 */

	public boolean isSPD()
	{
		return isspd;
	}

	/** Return triangular factor.
	 *
	 *	@return		Triangular factor.
	 */

	public Matrix getL()
	{
		return MatrixFactory.createMatrix( n , n , L );
	}

	/** Solve A*X = B .
	 *
	 *	@param  B   A Matrix with as many rows as A and any number of columns.
	 *
	 *	@return     X so that L*L'*X = B
	 *
	 *	@exception  IllegalArgumentException  Matrix row dimensions must agree.
	 *	@exception  RuntimeException  Matrix is not symmetric positive definite.
	 */

	public Matrix solve( Matrix B )
	{
								// Check that input matrix dimensions
								// are compatible with Cholesky decomposition.

		if ( B.rows() != n )
		{
			throw new IllegalArgumentException(
				"Matrix row dimensions must agree." );
		}
								// Make sure we have a valid decomposition.
		if ( !isspd )
		{
			throw new RuntimeException(
				"Matrix is not symmetric positive definite." );
		}

								// Copy right hand side.

		double[][] X	= B.get();
		int nx			= B.columns();

								// Solve L*Y = B .

		for ( int k = 0 ; k < n ; k++ )
		{
			for ( int i = k + 1 ; i < n ; i++ )
			{
				for ( int j = 0 ; j < nx ; j++ )
				{
					X[ i ][ j ]	-= X[ k ][ j ] * L[ i ][ k ];
				}
			}

			for ( int j = 0 ; j < nx ; j++ )
			{
				X[ k ][ j ]	/= L[ k ][ k ];
			}
		}
								// Solve L'*X = Y .

		for ( int k = n - 1 ; k >= 0 ; k-- )
		{
			for ( int j = 0 ; j < nx ; j++ )
			{
				X[ k ][ j ]	/= L[ k ][ k ];
			}

			for ( int i = 0 ; i < k ; i++ )
			{
				for ( int j = 0 ; j < nx ; j++ )
				{
					X[ i ][ j ]	-= X[ k ][ j ] * L[ k ][ i ];
				}
			}
		}

		return MatrixFactory.createMatrix( n , nx , X );
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

