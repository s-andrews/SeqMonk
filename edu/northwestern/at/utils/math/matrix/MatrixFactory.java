package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.lang.reflect.*;
import java.util.Iterator;
import java.util.List;

import edu.northwestern.at.utils.math.randomnumbers.RandomVariable;

/**	MatrixFactory creates matrices with different types of entries.
 */

public class MatrixFactory
{
	/** Default matrix implementation class. */

	protected static Class defaultMatrixClass = DenseMatrix.class;

	/**	Create matrix of specified dimensions and implementation class.
	 *
	 *	@param	rows			Number of rows in the matrix (> 1).
	 *	@param	columns			Number of columns in the matrix (> 1).
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return 		Matrix of the given dimensions and matrix class
	 *					with all entries set to zero.
	 */

	public static Matrix createMatrix
	(
		int rows ,
		int columns ,
		Class matrixClass
	)
	{
		Matrix result	= null;

								// Set parameter types.

		Class[] paramTypes =
			new Class[]
			{
				Integer.TYPE,
				Integer.TYPE
			};
								// Set parameter values.

		Object[] params =
			new Object[]
			{
				new Integer( rows ),
				new Integer( columns )
			};
								// Allocate constructor and create matrix.
		try
		{
			Constructor constructor =
				matrixClass.getDeclaredConstructor( paramTypes );

			result	= (Matrix)constructor.newInstance( params );
		}
		catch ( Exception e )
		{
		}

		return result;
	}

	/**	Create matrix with specified dimensions.
	 *
	 *	@param	rows		Number of rows in the matrix (> 1).
	 *	@param	columns 	Number of columns in the matrix (> 1).
	 *
	 *	@return 			Matrix of the given dimensions with all entries
	 *						set to zero.
	 */

	public static Matrix createMatrix( int rows , int columns )
	{
		return createMatrix( rows , columns , defaultMatrixClass );
	}

	/**	Create matrix with specified dimensions and a specified value.
	 *
	 *	@param	rows			Number of rows in the matrix (> 1).
	 *	@param	columns			Number of columns in the matrix (> 1).
	 *	@param	scalar			Initial value for each matrix element.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return					Matrix of the given dimensions and value.
	 */

	public static Matrix createMatrix
	(
		int rows ,
		int columns ,
		double scalar ,
		Class matrixClass
	)
	{
		final double finalScalar	= scalar;

		return MatrixEBETransformer.ebeTransform
		(
			createMatrix( rows , columns , matrixClass ) ,
				new MatrixEBETransformation()
				{
					public double transform( double element )
					{
						return element + finalScalar;
					}
				}
		);
	}

	/**	Create matrix with specified dimensions and a specified value.
	 *
	 *	@param	rows		Number of rows in the matrix (> 1).
	 *	@param	columns		Number of columns in the matrix (> 1).
	 *	@param	scalar		Initial value for each matrix element.
	 *
	 *	@return				Matrix of the given dimensions and value.
	 */

	public static Matrix createMatrix
	(
		int rows ,
		int columns ,
		double scalar
	)
	{
		return createMatrix( rows , columns , scalar , defaultMatrixClass );
	}

	/**	Create matrix with specified dimensions and values.
	 *
	 *	<p>
	 *	All elements are set to values in the passed array.
	 *	</p>
	 *
	 *	@param	rows   			Number of rows in the matrix (> 1).
	 *	@param	columns			Number of columns in the matrix (> 1).
	 *	@param	values 			Initial values of the matrix elements.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return					Matrix of given dimensions and values.
	 */

	public static Matrix createMatrix
	(
		int rows ,
		int columns ,
		double values[][] ,
		Class matrixClass
	)
	{
		Matrix result	= createMatrix( rows , columns , matrixClass );

		if ( result != null )
		{
			result.set( values );
		}

		return result;
	}

	/**	Create matrix with specified dimensions and values.
	 *
	 *	<p>
	 *	All elements are set to values in the passed array.
	 *	</p>
	 *
	 *	@param rows   		Number of rows in the matrix (> 1).
	 *	@param columns   	Number of columns in the matrix (> 1).
	 *	@param values 		Initial values of the matrix elements.
	 *
	 *	@return				Matrix of given dimensions and values.
	 */

	public static Matrix createMatrix
	(
		int rows ,
		int columns ,
		double values[][]
	)
	{
		return createMatrix( rows , columns , values , defaultMatrixClass );
	}

	/**	Create matrix with specified values.
	 *
	 *	@param	values			Initial values of the matrix elements.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return					Matrix of given values.
	 *
	 *	<p>
	 *	All elements are set to values in the passed array.
	 *	</p>
	 */

	public static Matrix createMatrix
	(
		double values[][] ,
		Class matrixClass
	)
	{
		int	nRows		= values.length;
		int nColumns	= values[ 0 ].length;

		return createMatrix( nRows , nColumns , values , matrixClass );
	}

	/**	Create matrix with specified values.
	 *
	 *	<p>
	 *	All elements are set to values in the passed array.
	 *	</p>
	 *
	 *	@param values 		Initial values of the matrix elements.
	 *
	 *	@return				Matrix of given values.
	 */

	public static Matrix createMatrix
	(
		double values[][]
	)
	{
		return createMatrix( values , defaultMatrixClass );
	}

	/**	Create a column vector matrix from a list of double values.
	 *
	 *	@param	list			List containing the Double values for the matrix.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return					Matrix as a column vector with the specified values.
	 */

	public static Matrix createMatrix( List list , Class matrixClass )
	{
		if ( list.size() <= 0 )
			throw new IllegalArgumentException(
				"Array list size should atleast be 1" );

		Matrix	result	= createMatrix( list.size() , 1 , matrixClass );

		if ( result != null )
		{
			Iterator iter	= list.iterator();
			int row			= 1;

			while ( iter.hasNext() )
			{
				Double value	= (Double)iter.next();

				result.set( row , 1 , value.doubleValue() );
				row++;
			}
		}

		return result;
	}

	/**	Create a column vector matrix from a list of double values.
	 *
	 *	@param	list	List containing the Double values for the matrix.
	 *
	 *	@return			Matrix as a column vector with the specified values.
	 */

	public static Matrix createMatrix( List list )
	{
		return createMatrix( list , defaultMatrixClass );
	}

	/**	Create matrix from another matrix.
	 *
	 *	@param	matrix	Matrix to copy.
	 *
	 *	@return 		Deep copy of source matrix.  The implementation
	 *					class of the copy is always the same as the
	 *					source matrix.
	 */

	public static Matrix createMatrix( Matrix matrix )
	{
		Matrix matrixCopy	= null;

		if ( matrix != null )
		{
			matrixCopy	=
				createMatrix(
					matrix.rows() , matrix.columns() , matrix.getClass() );

			if ( matrixCopy != null )
			{
				matrixCopy.set( matrix );
			}
		}

		return matrixCopy;
	}

	/**	Create an identity matrix.
	 *
	 *	@param 	size			Row and column size of the square matrix.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return					Square matrix with diagonal elements set to 1.
	 */

	public static Matrix createIdentityMatrix( int size , Class matrixClass )
	{
		return createScalarMatrix( size , 1.0D , matrixClass );
	}

	/**	Create an identity matrix.
	 *
	 *	@param 	size	Row and column size of the square matrix.
	 *
	 *	@return			Square matrix with diagonal elements set to 1.
	 */

	public static Matrix createIdentityMatrix( int size )
	{
		return createScalarMatrix( size , 1.0D );
	}

	/**	Creates a scalar matrix.
	 *
	 *	@param	size			Number of rows and columns in the square matrix.
	 *	@param	diagonalValue	The value for each main diagonal element.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return 				Square matrix with all diagonal elements
	 *							set to the specified diagonal value.
	 */

	public static Matrix createScalarMatrix
	(
		int size ,
		final double diagonalValue ,
		Class matrixClass
	)
	{
		Matrix matrix	=
			MatrixFactory.createMatrix( size , size , matrixClass );

		return MatrixEBETransformer.ebeTransform
		(
			matrix,
			new MatrixConditionalEBETransformation()
			{
				public double transform( int row , int column , double element )
				{
					if ( row == column )
					{
						return diagonalValue;
					}

					return element;
				}
			}
		);
	}

	/**	Creates a scalar matrix.
	 *
	 *	@param	size			Number of rows and columns in the square matrix.
	 *	@param	diagonalValue	The value for each main diagonal element.
	 *
	 *	@return 				Square matrix with all diagonal elements
	 *							set to the specified diagonal value.
	 */

	public static Matrix createScalarMatrix
	(
		int size ,
		final double diagonalValue
	)
	{
		return createScalarMatrix( size ,  diagonalValue , defaultMatrixClass );
	}

	/**	Creates a scalar matrix.
	 *
	 *	@param	size			Number of rows and columns in the square matrix.
	 *	@param	diagonalValues	double[] vector of values for each main diagonal
	 *							element.
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return 				Square matrix with all diagonal elements
	 *							set to the specified diagonal values.
	 */

	public static Matrix createScalarMatrix
	(
		int size ,
		final double[] diagonalValues ,
		Class matrixClass
	)
	{
		Matrix matrix	=
			MatrixFactory.createMatrix( size , size , matrixClass );

		return MatrixEBETransformer.ebeTransform
		(
			matrix,
			new MatrixConditionalEBETransformation()
			{
				public double transform( int row , int column , double element )
				{
					if ( ( row == column ) && ( row <= diagonalValues.length ) )
					{
						return diagonalValues[ row - 1 ];
					}
					else
					{
						return element;
					}
				}
			}
		);
	}

	/**	Creates a scalar matrix.
	 *
	 *	@param	size			Number of rows and columns in the square matrix.
	 *	@param	diagonalValues	double[] vector of values for each main diagonal
	 *							element.
	 *
	 *	@return 				Square matrix with all diagonal elements
	 *							set to the specified diagonal values.
	 */

	public static Matrix createScalarMatrix
	(
		int size ,
		final double[] diagonalValues
	)
	{
		return createScalarMatrix( size ,  diagonalValues , defaultMatrixClass );
	}

	/**	Create matrix with specified dimensions filled with random values.
	 *
	 *	@param	rows			Number of rows in the matrix (> 1).
	 *	@param	columns 		Number of columns in the matrix (> 1).
	 *	@param	matrixClass		Implementation class for the matrix.
	 *
	 *	@return 				Matrix of the given dimensions with all entries
	 *							set to uniform random numbers from [0,1].
	 */

	public static Matrix createRandomMatrix
	(
		int rows ,
		int columns ,
		Class matrixClass
	)
	{
		return MatrixEBETransformer.ebeTransform
		(
			createMatrix( rows , columns , matrixClass ),
				new MatrixEBETransformation()
				{
					public double transform( double element )
					{
						return RandomVariable.rand();
					}
				}
		);
	}

	/**	Create matrix with specified dimensions filled with random values.
	 *
	 *	@param	rows		Number of rows in the matrix (> 1).
	 *	@param	columns 	Number of columns in the matrix (> 1).
	 *
	 *	@return 			Matrix of the given dimensions with all entries
	 *						set to uniform random numbers from [0,1].
	 */

	public static Matrix createRandomMatrix( int rows , int columns )
	{
		return createRandomMatrix( rows , columns , defaultMatrixClass );
	}

	/**	Determine if a class implements the Matrix interface.
	 *
	 *	@param	possibleMatrixClass		The class to check.
	 *
	 *	@return							true if the class implements Matrix,
	 *									else false.
	 */

	public static boolean isMatrixClass( Class possibleMatrixClass )
	{
		boolean	result			= false;

		Class	currentClass	= possibleMatrixClass;

								//	Walk back up class hierarchy
								//	to check interfaces for "Matrix".

		while ( currentClass != null )
		{
								//	Get interfaces for this class.

			Class[] interfaces	= currentClass.getInterfaces();

								//	Is Matrix one of them?

			for ( int i = 0 ; i < interfaces.length ; i++ )
			{
				if ( interfaces[ i ] == Matrix.class )
				{
					result	= true;
					break;
				}
			}

			currentClass	= currentClass.getSuperclass();
		}

		return result;
	}

	/** Get default matrix class.
	 *
	 *	@return		The default matrix class for creating matrices.
	 */

	public static Class getDefaultMatrixClass()
	{
		return defaultMatrixClass;
	}

	/** Set default matrix class.
	 *
	 *	@param	matrixClass		The default matrix class for creating
	 *							matrices.
	 */

	public static void setDefaultMatrixClass( Class matrixClass )
	{
		if ( isMatrixClass( matrixClass ) )
		{
			defaultMatrixClass	= matrixClass;
		}
	}

	/**	Don't allow instantiation but do allow overrides.
	 */

	protected MatrixFactory()
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

