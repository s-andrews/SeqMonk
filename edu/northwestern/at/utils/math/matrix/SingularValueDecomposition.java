package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.ArithUtils;

/**	Singular Value Decomposition of a matrix.
 *
 *	<p>
 *	For an m-by-n matrix A with m >= n, the singular value decomposition is
 *	an m-by-n orthogonal matrix U, an n-by-n diagonal matrix S, and
 *	an n-by-n orthogonal matrix V so that A = U*S*V'.
 *	</p>
 *
 *	<p>
 *	The singular values, sigma[k] = S[k][k], are ordered so that
 *	sigma[0] >= sigma[1] >= ... >= sigma[n-1].
 *	</p>
 *
 *	<p>
 *	The singular value decompostion always exists, so the constructor will
 *	never fail.  The matrix condition number and the effective numerical
 *	rank can be computed from this decomposition.
 *	</p>
 *
 *	<p>
 *	This code is a slight modification of the JAMA code, which in
 *	turn is based on the Linpack code.
 *	</p>
 */

public class SingularValueDecomposition
{
	/**	Maximum number of iterations. */

	private static final int MAX_ITER	= 75;

	/**	Arrays for internal storage of U and V.	*/

	private double[][] U;
	private double[][] V;

	/**	Array for internal storage of singular values. */

	private double[] s;

	/**	Row and column dimensions. */

	private int m;
	private int n;

	/**	Construct the singular value decomposition.
	 *
	 *	@param	matrix	Rectangular matrix to decompose.
	 */

	public SingularValueDecomposition( Matrix matrix )
	{
		double[][] A	= matrix.get();

		m		= matrix.rows();
		n		= matrix.columns();
//		int nu	= Math.min( m , n );
		int nu	= m;

		s		= new double[ Math.min( m + 1 , n ) ];
		U		= new double[ m ][ nu ];
		V		= new double[ n ][ n ];

		double[] e		= new double[ n ];
		double[] work	= new double[ m ];

		boolean wantu	= true;
		boolean wantv	= true;

								//	Reduce A to bidiagonal form,
								//	storing the diagonal elements
								//	in s and the super-diagonal
								//	elements in e.

		int nct	= Math.min( m - 1 , n );
		int nrt	= Math.max( 0 , Math.min( n - 2 , m ) );

		for ( int k = 0 ; k < Math.max( nct , nrt ) ; k++ )
		{
			if ( k < nct )
			{
								//	Compute transformer for the k-th column
								//	and place the k-th diagonal in s[k].
								//	Compute 2-norm of k-th column without
								// 	under/overflow.
				s[ k ]	= 0;

				for ( int i = k ; i < m ; i++ )
				{
					s[ k ]	= ArithUtils.hypot( s[ k ] , A[ i ][ k ] );
				}

				if ( s[ k ] != 0.0D )
				{
					if ( A[ k ][ k ] < 0.0D )
					{
						s[ k ]	= -s[ k ];
					}

					for ( int i = k ; i < m ; i++ )
					{
						A[ i ][ k ]	/= s[ k ];
					}

					A[ k ][ k ]	+= 1.0D;
				}

				s[ k ]	= -s[ k ];
			}

			for ( int j = k + 1 ; j < n ; j++ )
			{
				if ( ( k < nct ) & ( s[ k ] != 0.0D ) )
				{
								//	Apply the transformer.

					double t	= 0.0D;

					for ( int i = k ; i < m ; i++ )
					{
						t	+= A[ i ][ k ] * A[ i ][ j ];
					}

					t	= -t / A[ k ][ k ];

					for ( int i = k ; i < m ; i++ )
					{
						A[ i ][ j ]	+= t * A[ i ][ k ];
					}
				}
								//	Place the k-th row of A into e for the
								//	subsequent calculation of the
								//	row transformer.

				e[ j ]	= A[ k ][ j ];
			}

			if ( wantu & ( k < nct ) )
			{
								//	Place the transformer in U for subsequent
								//	back multiplication.

				for ( int i = k ; i < m ; i++ )
				{
					U[ i ][ k ]	= A[ i ][ k ];
				}
			}

			if ( k < nrt )
			{
								//	Compute the k-th row transformer
								//	and place the k-th super-diagonal in e[k].
								//	Compute 2-norm without under/overflow.
				e[ k ]	= 0;

				for ( int i = k + 1 ; i < n ; i++ )
				{
					e[ k ]	= ArithUtils.hypot( e[ k ] , e[ i ] );
				}

				if ( e [ k ] != 0.0D )
				{
					if ( e[ k + 1 ] < 0.0D )
					{
						e[ k ]	= -e[ k ];
					}

					for ( int i = k + 1 ; i < n ; i++ )
					{
						e[ i ]	/= e[ k ];
					}

					e[ k + 1 ]	+= 1.0D;
				}

				e[ k ]	= -e[ k ];

				if ( ( ( k + 1 ) < m ) & ( e[ k ] != 0.0D ) )
				{
								//	Apply the transformer.

					for ( int i = k + 1 ; i < m ; i++ )
					{
						work[ i ]	= 0.0D;
					}

					for ( int j = k + 1 ; j < n; j++ )
					{
						for ( int i = k + 1 ; i < m ; i++ )
						{
							work[ i ]	+= e[ j ] * A[ i ][ j ];
						}
					}

					for ( int j = k + 1 ; j < n ; j++ )
					{
						double t	= -e[ j ] / e[ k + 1 ];

						for ( int i = k + 1 ; i < m ; i++ )
						{
							A[ i ][ j ]	+= t * work[ i ];
						}
					}
				}

				if ( wantv )
				{
								//	Place the transformer in V for
								//	subsequent back multiplication.

					for ( int i = k + 1 ; i < n ; i++ )
					{
						V[ i ][ k ]	= e[ i ];
					}
				}
			}
		}
								//	Set up the final bidiagonal matrix
								//	of order p.

		int p	= Math.min( n , m + 1 );

		if ( nct < n )
		{
			s[ nct ]	= A[ nct ][ nct ];
		}

		if ( m < p )
		{
			s[ p - 1 ]	= 0.0D;
		}

		if ( ( nrt + 1 ) < p )
		{
			e[ nrt ]	= A[ nrt ][ p - 1 ];
		}

		e[ p - 1 ]	= 0.0D;

								//	If required, generate U.
		if ( wantu )
		{
			for ( int j = nct ; j < nu ; j++ )
			{
				for ( int i = 0 ; i < m ; i++ )
				{
					U[ i ][ j ]	= 0.0D;
				}

				U[ j ][ j ]	= 1.0D;
			}

			for ( int k = nct - 1 ; k >= 0 ; k-- )
			{
				if ( s[ k ] != 0.0D )
				{
					for ( int j = k + 1 ; j < nu ; j++ )
					{
						double t	= 0.0D;

						for ( int i = k ; i < m ; i++ )
						{
							t	+= U[ i ][ k ] * U[ i ][ j ];
						}

						t	= -t / U[ k ][ k ];

						for ( int i = k ; i < m ; i++ )
						{
							U[ i ][ j ]	+= t * U[ i ][ k ];
						}
					}

					for ( int i = k ; i < m ; i++ )
					{
						U[ i ][ k ]	= -U[ i ][ k ];
					}

					U[ k ][ k ]	= 1.0D + U[ k ][ k ];

					for ( int i = 0 ; i < k - 1 ; i++ )
					{
						U[ i ][ k ]	= 0.0D;
					}
				}
				else
				{
					for ( int i = 0 ; i < m ; i++ )
					{
						U[ i ][ k ]	= 0.0D;
					}

					U[ k ][ k ]	= 1.0D;
				}
			}
		}
								//	If required, generate V.
		if ( wantv )
		{
			for ( int k = n - 1 ; k >= 0 ; k-- )
			{
				if ( ( k < nrt ) & ( e[ k ] != 0.0D ) )
				{
					for ( int j = k + 1 ; j < nu ; j++ )
					{
						double t	= 0.0D;

						for ( int i = k + 1 ; i < n ; i++ )
						{
							t	+= V[ i ][ k ] * V[ i ][ j ];
						}

						t	= -t / V[ k + 1 ][ k ];

						for ( int i = k + 1 ; i < n ; i++ )
						{
							V[ i ][ j ]	+= t * V[ i ][ k ];
						}
					}
				}

				for ( int i = 0 ; i < n ; i++ )
				{
					V[ i ][ k ]	= 0.0D;
				}

				V[ k ][ k ]	= 1.0D;
			}
		}
								//	Main iteration loop for the singular values.

		int pp		= p - 1;
		int iter	= 0;

		double eps	= Math.pow( 2.0D , -52.0D );
		double tiny	= Math.pow( 2.0D , -966.0D );

		while ( p > 0 )
		{
			int k;
			int kase;

								//	Here is where a test for too many
								//	iterations would go.  JAMA removed
								//	the Linpack iteration test.  We leave
								//	it out too for consistency.

//			if ( iter >= MAX_ITER ) break;

								//	This section of the program inspects for
								//	negligible elements in the s and e arrays.
								//	On completion the variables kase and k
								//	are set as follows.

								//	kase = 1	if s(p) and e[k-1] are
								//				negligible and k<p
								//	kase = 2	if s(k) is negligible and k<p
								//	kase = 3	if e[k-1] is negligible,
								//				k<p, and
								//				s(k), ..., s(p) are not
								//				negligible ( qr step ).
								//	kase = 4	if e(p-1) is negligible
								//				(convergence).

			for ( k = p - 2 ; k >= -1 ; k-- )
			{
				if ( k == -1 )
				{
					break;
				}

				if	(	Math.abs( e[ k ] ) <=
						tiny + eps * ( Math.abs( s [ k ] ) +
						Math.abs( s[ k + 1 ] ) ) )
				{
					e[ k ]	= 0.0D;
					break;
				}
			}

			if ( k == ( p - 2 ) )
			{
				kase	= 4;
			}
			else
			{
				int ks;

				for ( ks = p - 1 ; ks >= k ; ks-- )
				{
					if ( ks == k )
					{
						break;
					}

					double t	=
						( ks != p ? Math.abs( e[ ks ] ) : 0.0D ) +
						( ks != ( k + 1 ) ? Math.abs( e[ ks - 1 ] ) : 0.0D );

					if ( Math.abs( s[ ks ] ) <= ( tiny + ( eps * t ) ) )
					{
						s[ ks ]	= 0.0D;
						break;
					}
				}

				if ( ks == k )
				{
					kase	= 3;
				}

				else if ( ks == ( p - 1 ) )
				{
					kase	= 1;
				}
				else
				{
					kase	= 2;
					k		= ks;
				}
			}

			k++;

								// Perform the task indicated by kase.

			switch ( kase )
			{
								// Deflate negligible s(p).
				case 1:
				{
					double f	= e[ p - 2 ];

					e[ p - 2 ]	= 0.0D;

					for ( int j = p - 2 ; j >= k ; j-- )
					{
						double t	= ArithUtils.hypot( s[ j ] , f );
						double cs	= s[ j ] / t;
						double sn	= f / t;

						s[ j ]		= t;

						if ( j != k )
						{
							f			= -sn * e[ j - 1 ];
							e[ j - 1 ]	= cs * e[ j - 1 ];
						}

							if ( wantv )
							{
								for ( int i = 0 ; i < n ; i++ )
								{
									t			=
										 cs * V[ i ][ j ] + sn *
										 V[ i ][ p - 1 ];

									V[ i ][ p - 1 ]	=
										-sn * V[ i ][ j ] + cs *
										V[ i ][ p - 1 ];

									V[ i ][ j ]	= t;
								}
							}
						}
				}
				break;
								// Split at negligible s(k).
				case 2:
				{
					double f	= e[ k - 1 ];

					e[ k - 1 ]	= 0.0D;

					for ( int j = k ; j < p ; j++ )
					{
						double t	= ArithUtils.hypot( s[ j ] , f );
						double cs	= s[ j ] / t;
						double sn	= f / t;

						s[ j ]	= t;
						f		= -sn * e[ j ];
						e[ j ]	= cs * e[ j ];

						if ( wantu )
						{
							for ( int i = 0 ; i < m ; i++ )
							{
								t				=
									cs * U[ i ][ j ] + sn * U[ i ][ k - 1 ];

								U[ i ][ k - 1 ]	=
									-sn * U[ i ][ j ] + cs * U[ i ][ k - 1 ];

								U[ i ][ j ]	= t;
							}
						}
					}
				}
				break;
								// Perform one qr step.
				case 3:
				{
								//	Calculate the shift.

					double scale	=
						Math.max(
							Math.max(
								Math.max(
									Math.max(
										Math.abs( s[ p - 1 ] ) ,
										Math.abs( s[ p - 2 ] ) ) ,
										Math.abs( e[ p - 2 ] ) ) ,
										Math.abs( s[ k ] ) ) ,
										Math.abs( e[ k ] ) );

					double sp		= s[ p - 1 ] / scale;
					double spm1		= s[ p - 2 ] / scale;
					double epm1		= e[ p - 2 ] / scale;
					double sk		= s[ k ] / scale;
					double ek		= e[ k ] / scale;
					double b		=
						( ( spm1 + sp ) * ( spm1 - sp ) + epm1 * epm1 ) / 2.0D;
					double c		= ( sp * epm1 ) * ( sp * epm1 );
					double shift	= 0.0D;

					if ( ( b != 0.0D ) | ( c != 0.0D ) )
					{
						shift	= Math.sqrt( b * b + c );

						if ( b < 0.0D )
						{
							shift	= -shift;
						}

						shift	= c / ( b + shift );
					}

					double f	= ( sk + sp ) * ( sk - sp ) + shift;
					double g	= sk * ek;

								//	Chase zeros.

					for ( int j = k ; j < p - 1 ; j++ )
					{
						double t	= ArithUtils.hypot( f , g );
						double cs	= f / t;
						double sn	= g / t;

						if ( j != k )
						{
							e[ j - 1 ]	= t;
						}

						f			= cs * s[ j ] + sn * e[ j ];
						e[ j ]		= cs * e[ j ] - sn * s[ j ];
						g			= sn * s[ j + 1 ];
						s[ j + 1 ]	= cs * s[ j + 1 ];

						if ( wantv )
						{
							for ( int i = 0 ; i < n ; i++ )
							{
								t	=
									cs * V[ i ][ j ] + sn * V[ i ][ j + 1 ];

								V[ i ][ j + 1 ]	=
									-sn * V[ i ][ j ] + cs * V[ i ][ j + 1 ];

								V[ i ][ j ]	= t;
							}
						}

						t	= ArithUtils.hypot( f , g );
						cs	= f / t;
						sn	= g / t;

						s[ j ]	= t;

						f			= cs * e[ j ] + sn * s[ j + 1 ];
						s[ j + 1 ]	= -sn * e[ j ] + cs * s[ j + 1 ];
						g			= sn * e[ j + 1 ];
						e[ j + 1 ]	= cs * e[ j + 1 ];

						if ( wantu && ( j < ( m - 1 ) ) )
						{
							for ( int i = 0 ; i < m ; i++ )
							{
								t	=
									cs * U[ i ][ j ] + sn * U[ i ][ j + 1 ];

								U[ i ][ j + 1 ]	=
									-sn * U[ i ][ j ] + cs * U[ i ][ j + 1 ];

								U[ i ][ j ]	= t;
							}
						}
					}

					e[ p - 2 ]	= f;
					iter++;
				}
				break;
								//	Convergence.
				case 4:
				{
								//	Make the singular values positive.

					if ( s[ k ] <= 0.0D )
					{
						s[ k ]	= (s[ k ] < 0.0D ? -s[ k ] : 0.0D );

						if ( wantv )
						{
							for ( int i = 0 ; i <= pp ; i++ )
							{
								V[ i ][ k ]	= -V[ i ][ k ];
							}
						}
					}

								//	Order the singular values.

					while ( k < pp )
					{
						if ( s[ k ] >= s[ k + 1 ] )
						{
							break;
						}

						double t	= s[ k ];
						s[ k ]		= s[ k + 1 ];
						s[ k + 1 ]	= t;

						if ( wantv && ( k < ( n - 1 ) ) )
						{
							for ( int i = 0 ; i < n ; i++ )
							{
								t				= V[ i ][ k + 1 ];
								V[ i ][ k + 1 ]	= V[ i ][ k ];
								V[ i ][ k ]		= t;
							}
						}

						if ( wantu && ( k < ( m - 1 ) ) )
						{
							for ( int i = 0 ; i < m ; i++ )
							{
								t				= U[ i ][ k + 1 ];
								U[ i ][ k + 1 ]	= U[ i ][ k ];
								U[ i ][ k ]		= t;
							}
						}

						k++;
					}

					iter	= 0;
					p--;
				}
				break;
			}
		}
	}

	/**	Return the left singular vectors
	 *
	 *	@return		U (left singular vectors) as a matrix.
	 */

	public Matrix getU()
	{
		return MatrixFactory.createMatrix( m , Math.min( m + 1 , n ) , U );
	}

	/**	Return the right singular vectors
	 *
	 *	@return		V (right singular vectors) as a matrix.
	 */

	public Matrix getV()
	{
		return MatrixFactory.createMatrix( n , n , V );
	}

	/**	Return singular values.
	 *
	 *	@return		double[] vector of singular values.
	 */

	public double[] getSingularValues()
	{
		return s;
	}

	/**	Return diagonal matrix of singular values.
	 *
	 *	@return		S as a diagonal matrix.
	 */

	public Matrix getS()
	{
		int minRC	= Math.min( m , n );

		Matrix S	= MatrixFactory.createMatrix( minRC , minRC );

		for ( int i = 1 ; i <= minRC ; i++ )
		{
			S.set( i , i , this.s[ i - 1 ] );
		}

		return S;
	}

	/**	Return two norm.
	 *
	 *	@return		Maximum singular value.
	 */

	public double norm2()
	{
		return s[ 0 ];
	}

	/**	Return two norm condition number.
	 *
	 *	@return		maximum singular value / minimum singular value.
	 */

	public double cond()
	{
		return s[ 0 ] / s[ Math.min( m , n ) - 1 ];
	}

	/**	Return effective numerical matrix rank.
	 *
	 *	@return		Number of nonnegligible singular values.
	 */

	public int rank()
	{
		double eps	= Math.pow( 2.0D , -52.0D );
		double tol	= Math.max( m , n ) * s[ 0 ] * eps;

		int r	= 0;

		for ( int i = 0 ; i < s.length ; i++ )
		{
			if ( s[ i ] > tol )
			{
				r++;
			}
		}

		return r;
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

