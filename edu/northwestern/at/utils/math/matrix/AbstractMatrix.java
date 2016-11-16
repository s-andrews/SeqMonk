package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**	Abstract class which implements Matrix interface.
 *
 *	<p>
 *	Provides concrete implementations of all the Matrix
 *	interface methods except two: get and set.
 *	All the Matrix interface methods can be written
 *	in terms of get anc set.  To implement a new concrete
 *	matrix subclass all that is needed is to
 *	override the get and set methods with concrete versions.
 *	</p>
 *
 *	<p>
 *	See the {@link DenseMatrix} and {@link SparseMatrix}
 *	classes of two concrete subclasses derived by extending
 *	AbstractMatrix.
 *	</p>
 */

public abstract class AbstractMatrix implements Matrix
{
	/** Number of rows in matrix. */

	protected int rows;

	/** Number of columns in matrix. */

	protected int columns;

	/** Don't allow instantiation without size specification.
	 */

	protected AbstractMatrix()
	{
	}

	/** Create a matrix of the specified size.
	 *
	 *	@param	rows		Number of rows.
	 *	@param	columns		Number of columns.
	 *
	 *	<p>
	 *	A subclass constructor should call this constructor
	 *	and then create the matrix storage for the specified
	 *	number of rows and columns.
	 *	</p>
	 */

	public AbstractMatrix( int rows , int columns )
	{
		if ( ( rows < 1 ) || ( columns < 1 ) )
		{
			throw new IllegalArgumentException(
				"Matrix must have positive number of rows and columns" );
		}
		else
		{
			this.rows		= rows;
			this.columns	= columns;
		}
	}

	/**	Get number of rows in the matrix.
	 *
	 *	@return		Number of rows in the matrix.
	 */

	public int rows()
	{
		return rows;
	}

	/**	Get number of columns in the matrix.
	 *
	 *	@return		Number of columns in the matrix.
	 */

	public int columns()
	{
		return columns;
	}

	/**	Set an element at the given position to a new value.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@param value	The new value to be set.
	 *
	 *	<p>
	 *	Must be overridden by a subclass.
	 *	</p>
	 */

	public abstract void set( int row, int column, double value );

	/**	Set all elements from another matrix.
	 *
	 *	@param matrix	Matrix whose value to copy to this matrix.
	 *
	 *	@throws			MatrixMismatchedSizeException
	 *						If the source matrix and this one don't have
	 *						the same number of rows and columns.
	 */

	public void set( Matrix matrix )
		throws MatrixMismatchedSizeException
	{
		if ( matrix != null )
		{
			if (	( this.rows() != matrix.rows() ) ||
				    ( this.columns() != matrix.columns() ) )
			{
				throw new MatrixMismatchedSizeException(
					"Source matrix not same size" );
			}
			else
			{
				for ( int j = 1 ; j <= columns() ; j++ )
				{
					for ( int i = 1 ; i <= rows() ; i++ )
					{
						set( i , j , matrix.get( i , j ) );
					}
				}
			}
		}
	}

	/**	Get all elements as a new matrix.
	 *
	 *	@return		Copy of all elements as another matrix.
	 *
	 *	<p>
	 *	Essentially creates a deep clone of the current matrix
	 *	as another AbstractMatrix .
	 *	</p>
	 */

	public Matrix getCopy()
	{
		Matrix result	=
			MatrixFactory.createMatrix(
				rows() , columns() , getClass() );

		for ( int i = 1 ; i <= rows() ; i++ )
		{
			for ( int j = 1 ; j <= columns() ; j++ )
			{
				result.set( i , j ,  get( i , j ) );
			}
		}

		return result;
    }

	/**	Set all elements of a matrix from a double array.
	 *
	 *	@param	values	The double[][] from which values should be set.
	 *
	 *	<p>
	 *	If there are fewer values entries than the size of the matrix,
	 *	the other elements are set to zero.  If there are more values
	 *	entries than the size of the matrix, the extra values are ignored.
	 *	</p>
	 */

	public void set( double[][] values )
	{
		if ( values != null )
		{
			for ( int j = 1 ; j <= columns() ; j++ )
			{
				for ( int i = 1 ; i <= rows() ; i++ )
				{
					try
					{
						set( i , j , values[ i - 1 ][ j - 1 ] );
					}
					catch ( ArrayIndexOutOfBoundsException e )
					{
						set( i , j , 0.0D );
					}
				}
			}
		}
	}

	/**	Gets value of element at given row and column.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@return			The value at the given position.
	 *
	 *	<p>
	 *	Must be overridden by a subclass.
	 *	</p>
	 */

	public abstract double get( int row , int column );

	/**	Get entire row as a matrix.
	 *
	 *	@param	row		Row to retrieve.
	 *
	 *	@return 		Matrix containing row.
	 */

	public Matrix getRow( int row )
	{
		Matrix result	=
			MatrixFactory.createMatrix( 1 , columns() , getClass() );

		for ( int j = 1 ; j <= columns() ; j++ )
		{
			result.set( 1 , j , get( row , j ) );
		}

		return result;
	}

	/**	Get entire row as an array of doubles .
	 *
	 *	@param	row		Row to retrieve.
	 *
	 *	@return 		Array of doubles containing row data.
	 */

	public double[] getRowData( int row )
	{
		double[] result	= new double[ columns() ];

		for ( int j = 1 ; j <= columns() ; j++ )
		{
			result[ j - 1 ]	= get( row , j );
		}

		return result;
	}

	/**	Set entire row of values from a row matrix.
	 *
	 *	@param	row			Row to set.
	 *
	 *	@param	rowMatrix	Matrix containing row data.
	 *
	 *	<p>
	 *	If the length of the first row in rowMatrix
	 *	exceeds the number of columns in this matrix, the
	 *	extra values are ignored.  If rowMatrix's first row
	 *	is shorter than the number of columns in this matrix,
	 *	the remaining row elements are set to zero.
	 *	</p>
	 */

	public void setRow( int row , Matrix rowMatrix )
	{
		if ( ( row > 0 ) && ( row <= rows() ) && ( rowMatrix != null ) )
		{
			for ( int i = 1 ; i <= rowMatrix.columns() ; i++ )
			{
				if ( i <= columns() )
				{
					set( row , i , rowMatrix.get( 1 , i ) );
				}
			}

			for ( int i = ( rowMatrix.columns() + 1 ); i <= columns() ; i++ )
			{
				set( row , i , 0.0D );
			}
		}
	}

	/**	Set entire row from an array of doubles .
	 *
	 *	@param	row			Row to set.
	 *
	 *	@param	rowData		Array of doubles containing row data.
	 *
	 *	<p>
	 *	If the length of rowData exceeds the number of columns
	 *	in the matrix, the extra values are ignored.  If rowData
	 *	is shorter than the number of columns in the matrix,
	 *	the remaining row elements are set to zero.
	 *	</p>
	 */

	public void setRowData( int row , double rowData[] )
	{
		if ( ( row > 0 ) && ( row <= rows() ) && ( rowData != null ) )
		{
			for ( int i = 0 ; i < rowData.length ; i++ )
			{
				if ( i < columns() ) set( row , i + 1 , rowData[ i ] );
			}

			for ( int i = ( rowData.length + 1 ) ; i <= columns() ; i++ )
			{
				set( row , i , 0.0D );
			}
		}
	}

	/**	Get entire column as a matrix.
	 *
	 *	@param	column	Column to retrieve.
	 *
	 *	@return 		Matrix containing column.
	 */

	public Matrix getColumn( int column )
	{
		Matrix result	=
			MatrixFactory.createMatrix( rows() , 1 , getClass() );

		for ( int i = 1 ; i <= rows() ; i++ )
		{
			result.set( i , 1 , get( i , column ) );
		}

		return result;
	}

	/**	Get entire column as an array of doubles.
	 *
	 *	@param	column	Column to retrieve.
	 *
	 *	@return 		Array of doubles containing column data.
	 */

	public double[] getColumnData( int column )
	{
		double[] result	= new double[ rows() ];

		for ( int i = 1 ; i <= rows() ; i++ )
		{
			result[ i - 1 ]	= get( i , column );
		}

		return result;
	}

	/**	Set entire column of values from a column matrix.
	 *
	 *	@param	column			Column to set.
	 *
	 *	@param	columnMatrix	Matrix containing column data.
	 *
	 *	<p>
	 *	If the length of the first column in columnMatrix
	 *	exceeds the number of rows in this matrix, the
	 *	extra values are ignored.  If columnMatrix's first column
	 *	is shorter than the number of rows in this matrix,
	 *	the remaining column elements are set to zero.
	 *	</p>
	 */

	public void setColumn( int column , Matrix columnMatrix )
	{
		if ( ( column > 0 ) && ( column <= columns() ) && ( columnMatrix != null ) )
		{
			for ( int i = 1 ; i <= columnMatrix.rows() ; i++ )
			{
				if ( i <= rows() )
				{
					set( i , column , columnMatrix.get( i , 1 ) );
				}
			}

			for ( int i = ( columnMatrix.rows() + 1 ) ; i <= rows() ; i++ )
			{
				set( i , column , 0.0D );
			}
		}
	}

	/**	Set entire column from an array of doubles .
	 *
	 *	@param	column		Column to set.
	 *
	 *	@param	columnData	Array of doubles containing column data.
	 *
	 *	<p>
	 *	If the length of columnData exceeds the number of rows
	 *	in the matrix, the extra values are ignored.  If columnData
	 *	is shorter than the number of rows in this matrix,
	 *	the remaining column elements are set to zero.
	 *	</p>
	 */

	public void setColumnData( int column , double columnData[] )
	{
		if ( ( column > 0 ) && ( column <= columns() ) && ( columnData != null ) )
		{
			for ( int i = 0 ; i < columnData.length ; i++ )
			{
				if ( i < rows() )
				{
					set( i + 1 , column , columnData[ i ] );
				}
			}

			for ( int i = ( columnData.length + 1 ) ; i <= rows() ; i++ )
			{
				set( i , column , 0.0D );
			}
		}
	}

	/**	Set entire column from an array of ints .
	 *
	 *	@param	column		Column to set.
	 *
	 *	@param	columnData	Array of ints containing column data.
	 *
	 *	<p>
	 *	If the length of columnData exceeds the number of rows
	 *	in the matrix, the extra values are ignored.  If columnData
	 *	is shorter than the number of rows in this matrix,
	 *	the remaining column elements are set to zero.
	 *	</p>
	 */

	public void setColumnData( int column , int columnData[] )
	{
		if ( ( column > 0 ) && ( column <= columns() ) && ( columnData != null ) )
		{
			for ( int i = 0 ; i < columnData.length ; i++ )
			{
				if ( i < rows() )
				{
					set( i + 1 , column , (double)columnData[ i ] );
				}
			}

			for ( int i = ( columnData.length + 1 ) ; i <= rows() ; i++ )
			{
				set( i , column , 0.0D );
			}
		}
	}

	/**	Get all matrix elements as a two-dimensional double array.
	 *
	 *	@return		Copy of all elements as a two-dimensional double array.
	 */

	public double[][] get()
	{
		double[][] result	= new double[ rows() ][ columns() ];

		for ( int j = 1 ; j <= columns() ; j++ )
		{
			for ( int i = 1 ; i <= rows() ; i++ )
			{
				result[ i - 1 ][ j - 1 ]	= get( i , j );
			}
		}

		return result;
	}

	/**	Extract a submatrix.
	 *
	 *	@param	firstRow		First row index.
	 *	@param	firstColumn		First column index.
	 *	@param	lastRow			Last row index.
	 *	@param	lastColumn		Last column index.
	 *
	 *	@return		Matrix containing rows firstRow through lastRow and
	 *				columns firstColumn through lastColumn .
	 *
	 *	@throws MatrixMismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getSubMatrix
	(
		int firstRow ,
		int firstColumn ,
		int lastRow ,
		int lastColumn
	)
	{
		Matrix result	=
			MatrixFactory.createMatrix
			(
				lastRow - firstRow + 1 ,
				lastColumn - firstColumn + 1 ,
				getClass()
			);

		if ( ( result.rows() > rows() ) || ( result.columns() > columns() ) )
		{
			result	= null;

			throw new MatrixMismatchedSizeException(
				"Submatrix violates dimension constraints" );
		}
		else
		{
			for ( int row = firstRow ; row <= lastRow ; row++ )
			{
				for ( int col = firstColumn ; col <= lastColumn ; col++ )
				{
					result.set
					(
						row - firstRow + 1 ,
						col - firstColumn + 1 ,
						get( row , col )
					);
				}
			}
		}

		return result;
	}

	/**	Extract a submatrix.
	 *
	 *	@param	rows		Array of row indices.
	 *	@param	columns		Array of column indices.
	 *
	 *	@return				Matrix containing rows and columns
	 *							defined by indices.
	 *
	 *	@throws MatrixMismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getSubMatrix( int[] rows , int[] columns )
	{
		Matrix result	=
			MatrixFactory.createMatrix
			(
				rows.length ,
				columns.length ,
				getClass()
			);

		for ( int row = 1 ; row <= rows.length ; row++ )
		{
			for ( int col = 1 ; col <= columns.length; col++ )
			{
				result.set(
					row , col , get( rows[ row - 1 ] , columns[ col - 1 ] ) );
			}
		}

		return result;
	}

	/**	Extract a submatrix.
	 *
	 *	@param firstRow		First row index.
	 *	@param lastRow		Last row index
	 *	@param columns		Array of column indices.
	 *
	 *	@return				Matrix defined by specified row and column indices.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 *
	 */

	public Matrix getSubMatrix( int firstRow , int lastRow , int[] columns )
	{
		Matrix result	=
			MatrixFactory.createMatrix
			(
				lastRow - firstRow + 1 ,
				columns.length ,
				getClass()
			);

		if ( result.rows() > rows() )
		{
			result	= null;

			throw new MatrixMismatchedSizeException(
				"Submatrix violates dimension constraints" );
		}
		else
		{
			for ( int row = firstRow ; row <= lastRow ; row++ )
			{
				for ( int col = 1 ; col <= columns.length ; col++ )
				{
					result.set(
						row - firstRow + 1 ,
						col,
						get( row , columns[ col - 1 ] ) );
				}
			}
		}

		return result;
	}

	/**	Extract a submatrix.
	 *
	 *	@param	rows			Array of row indices.
	 *	@param	firstColumn		First column index.
	 *	@param	lastColumn		Last column index.
	 *
	 *	@return					Submatrix defined by row and column indices.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getSubMatrix( int[] rows , int firstColumn , int lastColumn )
	{
		Matrix result	=
			MatrixFactory.createMatrix
			(
				rows.length ,
				lastColumn - firstColumn + 1 ,
				getClass()
			);

		if ( result.columns() > columns() )
		{
			result	= null;

			throw new MatrixMismatchedSizeException(
				"Submatrix violates dimension constraints" );
		}
		else
		{
			for ( int row = 1 ; row <= rows.length ; row++ )
			{
				for ( int col = firstColumn ; col <= lastColumn ; col++ )
				{
					result.set(
						row ,
						col - firstColumn + 1 ,
						get( rows[ row - 1 ] , col ) );
				}
			}
		}

		return result;
	}

	/**	Extract columns.
	 *
	 *	@param	firstColumn		First column index.
	 *	@param	lastColumn		Last column index.
	 *
	 *	@return					Matrix containing all rows for specified
	 *							column range.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getColumns( int firstColumn , int lastColumn )
	{
		return getSubMatrix( 1 , firstColumn , rows() , lastColumn );
	}

	/**	Extract columns.
	 *
	 *	@param	columns			Indices of columns to extract.
	 *
	 *	@return					Matrix containing all rows for specified
	 *							column indices.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getColumns( int[] columns )
	{
		return getSubMatrix( 1 , rows() , columns );
	}

	/**	Extract rows.
	 *
	 *	@param	firstRow		First row index.
	 *	@param	lastRow			Last row index.
	 *
	 *	@return					Matrix containing all columns for specified
	 *							row range.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getRows( int firstRow , int lastRow )
	{
		return getSubMatrix( firstRow , 1 , lastRow , columns() );
	}

	/**	Extract rows.
	 *
	 *	@param	rows			Indices of rows to extract.
	 *
	 *	@return					Matrix containing all columns for specified
	 *							row indices.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getRows( int[] rows )
	{
		return getSubMatrix( rows , 1 , columns() );
	}

	/** Return matrix contents as a string.
	 *
	 *	@return		The matrix contents as a string.
	 */

	public String toString()
	{
		return MatrixToString.toString( this );
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

