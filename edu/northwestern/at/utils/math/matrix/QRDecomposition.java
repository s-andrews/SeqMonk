package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.ArithUtils;

/**	QR Decomposition of a matrix using Householder reflections.
 *
 * <p>
 * For an m-by-n matrix A with m >= n, the QR decomposition is an m-by-n
 * orthogonal matrix Q and an n-by-n upper triangular matrix R so that
 * A = Q*R.
 * </p>
 *
 * <p>
 * The QR decompostion always exists, even if the matrix does not have
 * full rank, so the constructor will never fail.  The primary use of the
 * QR decomposition is in the least squares solution of nonsquare systems
 * of simultaneous linear equations.  This will fail if isFullRank()
 * returns false.
 * </p>
 *
 *	<p>
 *	This is the JAMA implementation modified for use with our
 *	Matrix class.
 *	</p>
 */

public class QRDecomposition
{
	/**	Holds QR decomposition.
	 */

	private double[][] QR;

	/**	Row and column dimensions.
	 */

	private int m, n;

	/**	Holds diagonal of R.
	 */

	private double[] Rdiag;

	/**	QR Decomposition computed by Householder reflections.
	 *
	 *	@param	matrix	Rectangular matrix to decompose.
	 */

	public QRDecomposition( Matrix matrix )
	{
								// Initialize.

		QR		= matrix.get();
		m		= matrix.rows();
		n		= matrix.columns();

		Rdiag	= new double[ n ];

								// Main loop.

		for ( int k = 0 ; k < n ; k++ )
		{
								// Compute 2-norm of k-th column
								// without under/overflow.

			double nrm = 0.0D;

			for ( int i = k ; i < m ; i++ )
			{
				nrm = ArithUtils.hypot( nrm , QR[ i ][ k ] );
			}

			if ( nrm != 0.0D )
			{
								// Form k-th Householder vector.

				if ( QR[ k ][ k ] < 0.0D )
				{
					nrm = -nrm;
				}

				for ( int i = k ; i < m ; i++ )
				{
					QR[ i ][ k ]	/= nrm;
				}

				QR[ k ][ k ]	+= 1.0D;

								// Apply transformer to remaining columns.

				for ( int j = k + 1 ; j < n ; j++ )
				{
					double s = 0.0D;

					for ( int i = k ; i < m ; i++ )
					{
						s	+= QR[ i ][ k ] * QR[ i ][ j ];
					}

					s	= -s / QR[ k ][ k ];

					for ( int i = k ; i < m ; i++ )
					{
						QR[ i ][ j ]	+= s * QR[ i ][ k ];
					}
				}
			}

			Rdiag[ k ] = -nrm;
		}
	}

	/**	Is the matrix full rank?
	 *
	 *	@return		true if R, and hence A, has full rank.
	 */

	public boolean isFullRank()
	{
		for ( int j = 0 ; j < n ; j++ )
		{
			if ( Rdiag[ j ] == 0.0D )
			{
				return false;
			}
		}

		return true;
	}

	/**	Return the Householder vectors.
	 *
	 *	@return		Lower trapezoidal matrix whose columns define the
	 *				Householder reflections.
	 */

	public Matrix getH()
	{
		double[][] H	= new double[ m ][ n ];

		for ( int i = 0 ; i < m ; i++ )
		{
			for ( int j = 0 ; j < n ; j++ )
			{
				if ( i >= j )
				{
					H[ i ][ j ]	= QR[ i ][ j ];
				}
				else
				{
					H[ i ][ j ]	= 0.0D;
				}
			}
		}

		return MatrixFactory.createMatrix( m , n , H );
	}

	/**	Return the upper triangular factor.
	 *
	 *	@return 	The upper triangular factor R.
	 */

	public Matrix getR()
	{
		double[][] R = new double[ n ][ n ];

		for ( int i = 0 ; i < n ; i++ )
		{
			for ( int j = 0 ; j < n ; j++ )
			{
				if ( i < j )
				{
					R[ i ][ j ]	= QR[ i ][ j ];
				}
				else if ( i == j )
				{
					R[ i ][ j ] = Rdiag[ i ];
				}
				else
				{
					R[ i ][ j ] = 0.0D;
				}
			}
		}

		return MatrixFactory.createMatrix( n , n , R );
	}

	/**	Generate and return the (economy-sized) orthogonal factor.
	 *
	 *	@return		The orthogonal factor Q .
	 */

	public Matrix getQ()
	{
		double[][] Q	= new double[ m ][ n ];

		for ( int k = n - 1 ; k >= 0 ; k-- )
		{
			for ( int i = 0 ; i < m ; i++ )
			{
				Q[ i ][ k ]	= 0.0D;
			}

			Q[ k ][ k ]	= 1.0D;

			for ( int j = k ; j < n ; j++ )
			{
				if ( QR[ k ][ k ] != 0.0D )
				{
					double s = 0.0D;

					for ( int i = k ; i < m ; i++ )
					{
						s	+= QR[ i ][ k ] * Q[ i ][ j ];
					}

					s	= -s / QR[ k ][ k ];

					for ( int i = k ; i < m ; i++ )
					{
						Q[ i ][ j ]	+= s * QR[ i ][ k ];
					}
				}
			}
		}

		return MatrixFactory.createMatrix( m , n , Q );
	}

	/**	Least squares solution of A*X = B .
	 *
	 *	@param	B	A Matrix with as many rows as A and any number of columns.
	 *
	 *	@return		X that minimizes the two norm of Q*R*X-B.
	 *
	 *	@throws		IllegalArgumentException
	 *					Matrix row dimensions must agree.
	 *
	 *	@throws		RuntimeException
	 *					Matrix is rank deficient.
	 */

	public Matrix solve( Matrix B )
	{
		if ( B.rows() != m )
		{
			throw new IllegalArgumentException(
				"Matrix row dimensions must agree." );
		}

		if ( !this.isFullRank() )
		{
			throw new RuntimeException(
				"Matrix is rank deficient." );
		}

								// Copy right hand side.

		int nx			= B.columns();
		double[][] X	= B.get();

								// Compute Y = transpose(Q)*B .

		for ( int k = 0 ; k < n ; k++ )
		{
			for ( int j = 0 ; j < nx ; j++ )
			{
				double s	= 0.0D;

				for ( int i = k ; i < m ; i++ )
				{
					s	+= QR[ i ][ k ] * X[ i ][ j ];
				}

				s	= -s / QR[ k ][ k ];

				for ( int i = k ; i < m ; i++ )
				{
					X[ i ][ j ]	+= s * QR[ i ][ k ];
				}
			}
		}
								// Solve R*X = Y .

		for ( int k = n - 1 ; k >= 0 ; k-- )
		{
			for ( int j = 0 ; j < nx ; j++ )
			{
				X[ k ][ j ]	/= Rdiag[ k ];
			}

			for ( int i = 0 ; i < k ; i++ )
			{
				for ( int j = 0 ; j < nx ; j++ )
				{
					X[ i ][ j ]	-= X[ k ][ j ] * QR[ i ][ k ];
				}
			}
		}

		return MatrixFactory.createMatrix
		(
			B.rows() ,
			B.columns() ,
			X
		).getSubMatrix( 1 , 1 , n , nx );
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

