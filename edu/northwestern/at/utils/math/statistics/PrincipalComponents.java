package edu.northwestern.at.utils.math.statistics;

/*	Please see the license information at the end of this file. */

import java.util.*;
import edu.northwestern.at.utils.math.matrix.*;

/** PrincipalComponents -- Calculate principal components of a data matrix.
 */

public class PrincipalComponents
{
	/** Data matrix whose principal components are desired. */

	protected Matrix dataMatrix;

	/** Standardized data matrix. */

	protected Matrix standardizedDataMatrix;

	/** Correlation matrix derived from data matrix. */

	protected Matrix correlation;

	/** Eigenvectors of correlation matrix. */

	protected Matrix eigenVectors;

	/** Eigenvalues of correlation matrix. */

	protected Matrix eigenValues;

	/** Square root of eigenvalues. */

	protected Matrix sqrtEigenValues;

	/** Holds means and standard deviations of variables. */

	protected Matrix[] meansAndSDs;

	/** Holds principal component scores for rows. */

	protected Matrix rowScores;

	/** Holds principal component scores for columns. */

	protected Matrix columnScores;

	/** Compute principal components of an (n x m) data matrix.
	 *
	 *	@param	dataMatrix	The data matrix.
	 */

	public PrincipalComponents( Matrix dataMatrix )
	{
		this.dataMatrix	= dataMatrix.getCopy();

								// Get standardized data matrix.

		standardizedDataMatrix	= this.dataMatrix.getCopy();

		meansAndSDs	=
			Matrices.standardize( standardizedDataMatrix );

								// Create correlation matrix for data matrix.

		correlation		= Matrices.corr( dataMatrix );

								// Compute eigenvalue/eigenvector
								// decomposition.

		Matrix e[]		= Matrices.eig( correlation.getCopy() );

								// Retrieve eigenvalues.

		eigenVectors	= e[ 0 ];

								// Retrieve eigenvectors.

		eigenValues		= Matrices.diag( e[ 1 ] );

								// Resort eigenvalues and associated
								// eigenvectors in decending value.

		int eRows	= eigenValues.rows();

		for ( int i = 1 ; i <= ( eRows / 2 ) ; i++ )
		{
			double eig		= eigenValues.get( i , 1 );

			int j			= eRows - i + 1;

			eigenValues.set( i , 1 , eigenValues.get( j , 1 ) );
			eigenValues.set( j , 1 , eig );

			double[] col_i	= eigenVectors.getColumnData( i );

			eigenVectors.setColumnData( i , eigenVectors.getColumnData( j ) );
			eigenVectors.setColumnData( j , col_i );
		}
								// Compute square root of eigenvalues.

		sqrtEigenValues	= Matrices.sqrt( eigenValues );

								// Scale eigenvectors to eigenvalues.
/*
		MatrixConditionalEBETransformation scaleByEigenvalues	=
			new MatrixConditionalEBETransformation()
			{
				public double transform( int row , int column , double element )
				{
					return element / sqrtEigenValues.get( row , 1 );
				}
			};

		eigenVectors	=
			MatrixEBETransformer.ebeTransform(
				eigenVectors , scaleByEigenvalues );
*/
		rowScores	=
			Matrices.multiply( standardizedDataMatrix , eigenVectors );

		columnScores	=
			Matrices.multiply( correlation , eigenVectors );

		for ( int j = 1 ; j <= columnScores.columns() ; j++ )
		{
			double	eigenValue	= sqrtEigenValues.get( j , 1 );

			if ( eigenValue	!= 0.0D ) eigenValue	= 1.0D / eigenValue;

			for ( int i = 1 ; i <= columnScores.rows() ; i++ )
			{
				columnScores.set(
					i , j , columnScores.get( i , j ) * eigenValue );
			}
		}
	}

	public Matrix getDataMatrix()
	{
		return dataMatrix;
	}

	public Matrix getVectors()
	{
		return eigenVectors;
	}

	public Matrix getValues()
	{
		return eigenValues;
	}

	public Matrix getMeans()
	{
		return meansAndSDs[ 0 ];
	}

	public Matrix getStandardDeviations()
	{
		return meansAndSDs[ 1 ];
	}

	public Matrix getCorrelations()
	{
		return correlation;
	}

	public Matrix getStandardizedDataMatrix()
	{
		return standardizedDataMatrix;
	}

	public Matrix getColumnScores()
	{
		return columnScores;
	}

	public Matrix getRowScores()
	{
		return rowScores;
	}
}

/*
 * <p>
 * Copyright &copy; 2004-2011 Northwestern University.
 * </p>
 * <p>
 * This program is free software; you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * </p>
 * <p>
 * This program is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more
 * details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA.
 * </p>
 */

