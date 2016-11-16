package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.util.ArrayList;
import java.util.Iterator;

import edu.northwestern.at.utils.math.*;
import edu.northwestern.at.utils.math.statistics.*;

import edu.northwestern.at.utils.FileUtils;

/**	Matrices: Perform operations on matrices.
 */

public class Matrices
{
	public static int[] bandwidth( Matrix matrix )
	{
		return MatrixProperty.bandwidth( matrix );
	}

	/**
	 * Creation
	 */

	public static Matrix create( String src )
	{
		return MatrixFromString.fromMatlabString( src );
	}

	public static Matrix rand( int rows , int cols )
	{
		return MatrixFactory.createRandomMatrix( rows , cols );
	}

	public static Matrix zeros( int rows , int cols )
	{
		return MatrixFactory.createMatrix( rows , cols );
	}

	public static Matrix zeros( int size )
	{
		return zeros( size , size );
	}

	public static Matrix ones( int rows , int cols )
	{
		return MatrixFactory.createMatrix( rows , cols , 1.0D );
	}

	public static Matrix ones( int size )
	{
		return ones( size , size );
	}

	public static Matrix eye( int rows , int cols )
	{
		if ( rows == cols )
			return MatrixFactory.createIdentityMatrix(rows);

		boolean rowsAreBigger = (rows > cols);     // vertical concatenation

		if (rowsAreBigger)
			return MatrixOperator.verticalConcatenation(
				MatrixFactory.createIdentityMatrix( cols ),
				MatrixFactory.createMatrix( rows - cols , cols ) );
		else
			return MatrixOperator.horizontalConcatenation(
				MatrixFactory.createIdentityMatrix( rows ),
				MatrixFactory.createMatrix( rows, cols - rows ) );
	}

	/**
	 * Operators
	 */

	public static Matrix neg(Matrix a)
	{
		return MatrixTransformer.negate(a);
	}

	public static Matrix sin( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.sin( element );
				}
			}
		);
	}

	public static Matrix cos( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.cos( element );
				}
			}
		);
	}

	public static Matrix exp( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.exp( element );
				}
			}
		);
	}

	public static Matrix log( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.log( element );
				}
			}
		);
	}

	public static Matrix sqrt( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.sqrt( element );
				}
			}
		);
	}

	public static Matrix abs( Matrix a )
	{
		return MatrixEBETransformer.ebeTransform
		(
			a,
			new MatrixEBETransformation()
			{
				public double transform( double element )
				{
					return Math.abs( element );
				}
			}
		);
	}

	public static Matrix powElem(Matrix a, final double s)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return Math.pow(element, s);
					}
				}
			);
	}

	public static Matrix powElem(Matrix a, Matrix b)
	{
		return MatrixOperator.applyEBEOperation(a, b, new MatrixEBEOperation()
				{
					public double apply(double a, double b)
					{
						return Math.pow(a, b);
					}
				}
			);
	}

	public static Matrix pow(Matrix a, int s)
	{
		return MatrixTransformer.pow(a, s);
	}

	public static Matrix add(Matrix a, Matrix b)
	{
		return MatrixOperator.add(a, b);
	}

	public static Matrix add(Matrix a, final double scalar)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return element + scalar;
					}
				}
			);
	}

	public static Matrix subtract(Matrix a, Matrix b)
	{
		return MatrixOperator.subtract(a, b);
	}

	public static Matrix subtract(Matrix a, final double scalar)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return element - scalar;
					}
				}
			);
	}

	public static Matrix subtract(final double scalar, Matrix a)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return scalar - element;
					}
				}
			);
	}

	public static double dotprod(Matrix a, Matrix b)
	{
		return MatricesMeasure.dotProduct(a, b);
	}

	public static Matrix multiply(Matrix a, Matrix b)
	{
		return MatrixOperator.multiply(a, b);
	}

	public static Matrix multiplyEBE(Matrix a, Matrix b)
	{
		return MatrixOperator.multiplyEBE(a, b);
	}

	public static Matrix multiply(Matrix a, final double scalar)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return scalar * element;
					}
				}
			);
	}

	public static Matrix kron(Matrix a, Matrix b)
	{
		return MatrixOperator.kroneckerProduct(a, b);
	}

	public static Matrix hdp(Matrix a, Matrix b)
	{
		return MatrixOperator.horizontalDirectProduct(a, b);
	}

	public static Matrix solve(Matrix a, Matrix b)
	{
		return MatrixOperator.solve(a, b);
	}

	public static Matrix divide(Matrix a, Matrix b)
	{
		return MatrixOperator.divideEBE(a, b);
	}

	public static Matrix divide(Matrix a, final double scalar)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return element / scalar;
					}
				}
			);
	}

	public static Matrix divide(final double scalar, Matrix a)
	{
		return MatrixEBETransformer.ebeTransform(a, new MatrixEBETransformation()
				{
					public double transform(double element)
					{
						return scalar / element;
					}
				}
			);
	}

	public static Matrix t(Matrix m)
	{
		return MatrixTransformer.transpose(m);
	}

	public static Matrix transpose(Matrix m)
	{
		return MatrixTransformer.transpose(m);
	}

	public static Matrix inv(Matrix m)
	{
		return MatrixTransformer.inverse(m);
	}

	public static Matrix horzcat(Matrix a, Matrix b)
	{
		return MatrixOperator.horizontalConcatenation(a, b);
	}

	public static Matrix vertcat(Matrix a, Matrix b)
	{
		return MatrixOperator.verticalConcatenation(a, b);
	}

	public static Matrix blkdiag(ArrayList matrices)
	{
		Iterator iter = matrices.iterator();
		Matrix tmpMatrix = null;

		while (iter.hasNext())
		{
			Matrix matrix = (Matrix) iter.next();
			int rows_m = matrix.rows(), cols_m = matrix.columns(), rows_tmp = 0, cols_tmp = 0;

			if (tmpMatrix != null)
			{
				rows_tmp = tmpMatrix.rows();
				cols_tmp = tmpMatrix.columns();

				Matrix rightMatrix	=
					MatrixFactory.createMatrix(
						rows_tmp, cols_m,  0 );

				Matrix leftMatrix	=
					MatrixFactory.createMatrix(
						rows_m, cols_tmp,  0 );

				Matrix upperMatrix	= horzcat(tmpMatrix, rightMatrix);
				Matrix lowerMatrix	= horzcat(leftMatrix, matrix);

				tmpMatrix = vertcat(upperMatrix, lowerMatrix);
			}
			else
			{
				tmpMatrix = matrix;
			}
		}
		return tmpMatrix;
	}

	/**
	 * Methods
	 */

	public static int rank(Matrix m)
	{
		return MatrixMeasure.rank(m);
	}

	public static double det(Matrix m)
	{
		return MatrixMeasure.determinant(m);
	}

	public static double trace(Matrix m)
	{
		return MatrixMeasure.trace(m);
	}

	public static double norm(Matrix m)
	{
		return MatrixMeasure.norm2(m);
	}

	public static double norm_1(Matrix m)
	{
		return MatrixMeasure.norm1(m);
	}

	public static double normInf(Matrix m)
	{
		return MatrixMeasure.normInfinity(m);
	}

	public static double normFro(Matrix m)
	{
		return MatrixMeasure.normFrobenius(m);
	}

	public static double cond(Matrix m)
	{
		return MatrixMeasure.condition(m);
	}

	public static Matrix diag(Matrix m)
	{
		return MatrixTransformer.diagonal(m);
	}

	public static Matrix diagEx(Matrix m, int offset)
	{
		return MatrixTransformer.extractDiagonal(m, offset);
	}

	public static Matrix diagEm(Matrix m, int offset)
	{
		return MatrixTransformer.embedDiagonal(m, offset);
	}

	public static int length(Matrix m)
	{
		return MatrixMeasure.length(m);
	}

	public static double sumElem(Matrix m)
	{
		return MatrixMeasure.sum(m);
	}

	public static Matrix sumCol(Matrix m)
	{
		return ColumnTransformer.sum(m);
	}

	public static double prodElem(Matrix m)
	{
		return MatrixMeasure.product(m);
	}

	public static Matrix prodCol(Matrix m)
	{
		return ColumnTransformer.product(m);
	}

	public static double maxElem(Matrix m)
	{
		return MatrixMeasure.max(m);
	}

	public static Matrix maxCol(Matrix m)
	{
		return ColumnTransformer.max(m);
	}

	public static double minElem(Matrix m)
	{
		return MatrixMeasure.min(m);
	}

	public static Matrix minCol(Matrix m)
	{
		return ColumnTransformer.min(m);
	}

	public static double meanElem(Matrix m)
	{
		return MatrixMeasure.mean(m, false);
	}

	public static Matrix meanCol(Matrix m)
	{
		return ColumnTransformer.mean(m, false);
	}

	public static Matrix cumprod(Matrix m)
	{
		return MatrixTransformer.cumulativeColumnProduct(m);
	}

	public static Matrix cumsum(Matrix m)
	{
		return MatrixTransformer.cumulativeColumnSum(m);
	}

	public static Matrix triu(Matrix m, int offset)
	{
		return MatrixTransformer.extractUpperTriangle(m, offset);
	}

	public static Matrix tril(Matrix m, int offset)
	{
		return MatrixTransformer.extractLowerTriangle(m, offset);
	}

	public static Matrix zeroize(Matrix m)
	{
		return MatrixTransformer.zeroize( m , Constants.MACHEPS );
	}

	public static Matrix zeroise(Matrix m)
	{
		return MatrixTransformer.zeroise( m , Constants.MACHEPS );
	}

	/**
	 * Decompositions
	 */

	public static Matrix[] lu(Matrix m)
	{
		Matrix[] result = new Matrix[3];
		LUDecomposition lu = new LUDecomposition(m);

		result[0] = lu.getL();
		result[1] = lu.getU();
		result[2] = lu.getPivotMatrix();
		return result;
	}

	public static Matrix[] qr(Matrix m)
	{
		Matrix[] result = new Matrix[3];
		QRDecomposition qr = new QRDecomposition(m);

		result[0] = qr.getQ();
		result[1] = qr.getR();
		result[2] = qr.getH();
		return result;
	}

	public static Matrix[] eig(Matrix m)
	{
		Matrix[] result = new Matrix[2];
		EigenvalueDecomposition eig = new EigenvalueDecomposition(m);

		result[0] = eig.getV();
		result[1] = eig.getD();
		return result;
	}

	public static Matrix[] chol(Matrix m)
	{
		Matrix[] result = new Matrix[2];
		CholeskyDecomposition chol = new CholeskyDecomposition(m);

		result[0] = chol.getL();
		Matrix p = MatrixFactory.createMatrix(1, 1);

		if (chol.isSPD())
			p.set(1, 1, 0);
		else
			p.set(1, 1, 1);
		result[1] = p;
		return result;
	}

	public static Matrix[] svd( Matrix m )
	{
		Matrix[] result	= new Matrix[ 3 ];

		int mRows		= m.rows();
		int mCols		= m.columns();

		SingularValueDecomposition svd;

		if ( mRows > mCols )
		{
			svd	= new SingularValueDecomposition( m );

			result[ 0 ]	= svd.getU();
			result[ 1 ]	= svd.getS();
			result[ 2 ]	= svd.getV();
		}
		else
		{
			svd	= new SingularValueDecomposition( transpose( m ) );

			result[ 0 ]	= svd.getV();
			result[ 1 ]	= svd.getS();
			result[ 2 ]	= svd.getU();
		}

		return result;
	}

	/** Save matrix in an ASCII file.
	 *
   	 *	@param	fileName	The filename to save to.
	 *	@param	matrix    	The matrix to save.
	 */

	public static void save( String fileName , Matrix matrix )
	{
		try
		{
			FileUtils.writeTextFile
			(
				fileName ,
				false ,
				MatrixToString.toString( matrix )
			);
		}
		catch ( Exception e )
		{
		}
	}

	/** Load matrix from an ASCII file.
	 *
	 *	@param	fileName	The filename of the file containing the matrix.
	 *
	 *	@return				Matrix read from file.
	 */

	public static Matrix load( String fileName )
	{
		String	matrixString	= "";

		try
		{
			matrixString	= FileUtils.readTextFile( fileName );
		}
		catch ( Exception e )
		{
		}

		return MatrixFromString.fromPlainTextString( matrixString );
	}

	/** Get correlation matrix.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			Correlation matrix of columns in matrix.
	 */

	public static Matrix corr( Matrix matrix )
	{
		return Covar.correlation( matrix );
	}

	/** Get covariance matrix.
	 *
	 *	@param	matrix	The matrix.
	 *
	 *	@return			Covariance matrix of columns in matrix.
	 */

	public static Matrix cov( Matrix matrix )
	{
		return Covar.covariance( matrix );
	}

	/**	Standardize columns of a matrix.
	 *
	 *	@param	matrix	The matrix whose columns should be
	 *					standardized to mean=0 and standard deviation=1.
	 *
	 *	@return			Input matrix is standardized.
	 *					Two-element array returned with first entry
	 *					the row vector of column means and the
	 *					second entry the row vector of column
	 *					standard deviations.
	 */

	 public static Matrix[] standardize( Matrix matrix )
	 {
	 	return Standardize.standardize( matrix );
	 }

	/**	Rank order columns of a matrix.
	 *
	 *	@param	matrix	The matrix whose columns should be converted
	 *					to rank order.
	 */

	 public static void rankOrderColumns( Matrix matrix )
	 {
	 	RankOrder.rankOrder( matrix );
	 }

	/**	Found basis of the null space for a matrix.
	 *
	 *	@param	a	Matrix whose null space is to be found.
	 *
	 *	@return		Basis of the null space for a.
	 */

	public static Matrix nullSpace( Matrix a )
	{
		int rows	= a.rows();
		int cols	= a.columns();

		if ( ( rows == 0 ) && ( cols == 0 ) )
		{
			return MatrixFactory.createMatrix( 0 , 0 );
        }

		Matrix[] svda	= svd( a );

		int srows	= svda[ 1 ].rows();
		int scols	= svda[ 1 ].columns();

		Matrix s;

		if ( ( srows == 1 ) || ( scols == 1 ) )
		{
			s	= MatrixFactory.createMatrix( 1 , 1 );
			s.set( 1 , 1 , svda[ 1 ].get( 1 , 1 ) );
		}
		else
		{
			s	= diag( svda[ 1 ] );
		}

		double tol	= s.get( 1 , 1 ) * length( a ) * Constants.MACHEPS;

		int rank	= 0;

		for ( int i = 1 ; i < s.rows() ; i++ )
		{
			if ( s.get( i , 1 ) >= tol ) rank++;
		}

		Matrix result;

		if ( rank < cols )
		{
			result	= svda[ 2 ].getColumns( rank + 1 , cols );
		}
		else
		{
			result	= zeros( cols , 0 );
		}

		return result;
	}

	/**	Found orthonormal basis for a matrix.
	 *
	 *	@param	a	Matrix whose orthonmal basis is to be found.
	 *
	 *	@return		Orthonormal basis for a.
	 */

	public static Matrix orthBasis( Matrix a )
	{
		int rows	= a.rows();
		int cols	= a.columns();

		if ( ( rows == 0 ) && ( cols == 0 ) )
		{
			return MatrixFactory.createMatrix( 0 , 0 );
        }

		Matrix[] svda	= svd( a );

		int srows	= svda[ 1 ].rows();
		int scols	= svda[ 1 ].columns();

		Matrix s;

		if ( ( srows == 1 ) || ( scols == 1 ) )
		{
			s	= MatrixFactory.createMatrix( 1 , 1 );
			s.set( 1 , 1 , svda[ 1 ].get( 1 , 1 ) );
		}
		else
		{
			s	= diag( svda[ 1 ] );
		}

		double tol	= s.get( 1 , 1 ) * length( a ) * Constants.MACHEPS;

		int rank	= 0;

		for ( int i = 1 ; i < s.rows() ; i++ )
		{
			if ( s.get( i , 1 ) >= tol ) rank++;
		}

		Matrix result;

		if ( rank > 0 )
		{
			result	= neg( svda[ 2 ].getColumns( 1 , rank ) );
		}
		else
		{
			result	= zeros( rows , 0 );
		}

		return result;
	}

	/* Disallow instantiation, but allow overrides. */

	protected Matrices()
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

