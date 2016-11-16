package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/**	LUDecomposition of a matrix using Crout/Dolittle algorithm.
 *
 *	<p>
 *	For an m-by-n matrix A with m >= n, the LU decomposition is an m-by-n
 *	unit lower triangular matrix L, an n-by-n upper triangular matrix U,
 *	and a permutation vector piv of length m so that A(piv,:) = L*U.
 *	If m < n, then L is m-by-m and U is m-by-n.
 *	</p>
 *
 *	<p>
 *	The LU decompostion with pivoting always exists, even if the matrix is
 *	singular, so the constructor will never fail.  The primary use of the
 *	LU decomposition is in the solution of square systems of simultaneous
 *	linear equations.  This will fail if isNonsingular() returns false.
 *	</p>
 *
 *	<p>
 *	This is the JAMA implementation modified for use with our
 *	Matrix class.
 *	</p>
 */

public class LUDecomposition
{
	/**
	 * Array for internal storage of decomposition.
	 */

	private double[][] LU;

	/**
	 * Row and column dimensions, and pivot sign.
	 */

	private int m, n, pivsign;

	/**
	 * Internal storage of pivot vector.
	 */

	private int[] piv;

	/* ------------------------
	 Constructor
	 * ------------------------ */

	/**	LU Decomposition using Crout/Dolittle algorithm.
	 *
	 * @param a Rectangular matrix
	 */

	public LUDecomposition( Matrix a )
	{
								// Use a "left-looking", dot-product,
								// Crout/Doolittle algorithm.

		LU		= a.get();
		m		= a.rows();
		n		= a.columns();

		piv		= new int[ m ];

		for ( int i = 0 ; i < m ; i++ )
		{
			piv[ i ]	= i + 1;
		}

		pivsign	= 1;

		double[] LUrowi;
		double[] LUcolj	= new double[ m ];

								// Outer loop.

		for ( int j = 0 ; j < n ; j++ )
		{
								// Make a copy of the j-th column
								// to localize references.

			for ( int i = 0 ; i < m ; i++ )
			{
				LUcolj[ i ]	= LU[ i ][ j ];
			}

								// Apply previous transformations.

			for ( int i = 0 ; i < m ; i++ )
			{
				LUrowi	= LU[ i ];

								// Most of the time is spent in the
								// following dot product.

				int kmax	= Math.min( i , j );

				double s	= 0.0D;

				for ( int k = 0 ; k < kmax ; k++ )
				{
					s	+= LUrowi[ k ] * LUcolj[ k];
				}

				LUrowi[ j ]	= LUcolj[ i ] -= s;
			}

								// Find pivot and exchange if necessary.

			int p = j;

			for ( int i = j + 1 ; i < m ; i++ )
			{
				if ( Math.abs( LUcolj[ i ] ) > Math.abs( LUcolj[ p ] ) )
				{
					p = i;
				}
			}

			if ( p != j )
			{
				for ( int k = 0 ; k < n ; k++ )
				{
					double t		= LU[ p ][ k ];

					LU[ p ][ k ]	= LU[ j ][ k ];
					LU[ j ][ k ]	= t;
				}

				int k	= piv[ p ];

				piv[ p ]	= piv[ j ];
				piv[ j ]	= k;
				pivsign		= -pivsign;
			}
								// Compute multipliers.

			if ( ( j < m ) && ( LU[ j ][ j ] != 0.0D ) )
			{
				for ( int i = j + 1 ; i < m ; i++ )
				{
					LU[ i ][ j ]	/= LU[ j ][ j ];
				}
			}
		}
	}

	/* ------------------------
	 Public Methods
	 * ------------------------ */

	/**	Is matrix nonsingular?
	 *
	 *	@return		true if U, and hence A, is nonsingular.
	 */

	public boolean isNonsingular()
	{
		for ( int j = 0 ; j < n ; j++ )
		{
			if ( LU[ j ][ j ] == 0.0D )
			{
				return false;
			}
		}

		return true;
	}

	/**	Is matrix singular?
	 *
	 *	@return		true if U, and hence A, is singular.
	 */

	public boolean isSingular()
	{
		return !isNonsingular();
	}

	/**	Return lower triangular factor.
	 *
	 *	@return 	The lower triangular factor L.
	 */

	public Matrix getL()
	{
		double[][] L	= new double[ m ][ n ];

		for ( int i = 0 ; i < m ; i++ )
		{
			for ( int j = 0 ; j < n ; j++ )
			{
				if ( i > j )
				{
					L[ i ][ j ]	= LU[ i ][ j ];
				}
				else if ( i == j )
				{
					L[ i ][ j ]	= 1.0D;
				}
				else
				{
					L[ i ][ j ]	= 0.0D;
				}
			}
		}

		return MatrixFactory.createMatrix( m , n , L );
	}

	/**	Return upper triangular factor.
	 *
	 *	@return		The upper triangular factor U .
	 */

	public Matrix getU()
	{
		double[][] U	= new double[ m ][ n ];

		for ( int i = 0 ; i < n ; i++ )
		{
			for ( int j = 0 ; j < n ; j++ )
			{
				if ( i <= j )
				{
					U[ i ][ j ]	= LU[ i ][ j ];
				}
				else
				{
					U[ i ][ j ]	= 0.0D;
				}
			}
		}

		return MatrixFactory.createMatrix( m , n , U );
	}

	/**	Return pivot permutation vector.
	 *
	 *	@return		The pivor permutation vector.
	 */

	public int[] getPivot()
	{
		return ((int[])piv.clone());
	}

	/**	Return pivot permutation vector as a one-dimensional array of doubles.
	 *
	 *	@return		Pivot array as doubles.
	 */

	public double[][] getDoublePivot()
	{
		double[][] vals	= new double[ m ][ 1 ];

		for ( int i = 0 ; i < m ; i++ )
		{
			vals[ i ][ 0 ]	= (double)piv[ i ];
		}

		return vals;
	}

	/**	Return pivot permutation vector .
	 *
	 *	@return 	Pivot permutation vector as a matrix with 1 column.
	 */

	public Matrix getPivotMatrix()
	{
		double[][] dblPivot	= getDoublePivot();

		return MatrixFactory.createMatrix( dblPivot.length , 1 , dblPivot );
	}

	/**	Return determinant of matrix.
	 *
	 *	@return		Determinant of matrix which was decomposed.
	 *
	 *	@throws		IllegalArgumentException
	 *					If matrix is not square.
	 */

	public double det()
	{
		if ( m != n )
		{
			throw new IllegalArgumentException(
				"Matrix must be square." );
		}

		double d = (double)pivsign;

		for ( int j = 0 ; j < n ; j++ )
		{
			d	*= LU[ j ][ j ];
		}

		return d;
	}

	/**	Solve A*X = B .
	 *
	 *	@param	B	Matrix with as many rows as A and any number of columns.
	 *
	 *	@return 	X so that L*U*X = B(piv,:) .
	 *
	 *	@throws		IllegalArgumentException
	 *					Matrix row dimensions must agree.
	 *
	 *	@throws RuntimeException
	 *					Matrix is singular.
	 */

	public Matrix solve( Matrix B )
	{
		if ( B.rows() != m )
		{
			throw new IllegalArgumentException(
				"Matrix row dimensions must agree." );
		}

		if ( this.isSingular() )
		{
			throw new RuntimeException(
				"Matrix is singular." );
		}

								// Copy right hand side with pivoting

		int nx			= B.columns();

		Matrix Xmat		= B.getSubMatrix( piv , 1 , nx );

		double[][] X	= Xmat.get();

								// Solve L*Y = B(piv,:)

		for ( int k = 0 ; k < n ; k++ )
		{
			for ( int i = k + 1 ; i < n ; i++ )
			{
				for ( int j = 0 ; j < nx ; j++ )
				{
					X[ i ][ j ]	-= X[ k ][ j ] * LU[ i ][ k ];
				}
			}
		}
								// Solve U*X = Y;

		for ( int k = n - 1 ; k >= 0 ; k-- )
		{
			for ( int j = 0 ; j < nx ; j++ )
			{
				X[ k ][ j ]	/= LU[ k ][ k ];
			}

			for ( int i = 0 ; i < k ; i++ )
			{
				for ( int j = 0 ; j < nx ; j++ )
				{
					X[ i ][ j ]	-= X[ k ][ j ] * LU[ i ][ k ];
				}
			}
		}

		return MatrixFactory.createMatrix( Xmat.rows() , Xmat.columns() , X );
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

