package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

/** Two-dimensional matrix interface class for doubles.
 *
 *	<p>
 *	Matrix elements are assumed to be doubles, but this class
 *	imposes no specific structure as to how the doubles
 *	are stored.  Only operations to get and set
 *	elements in the matrix are defined here.  All other
 *	matrix operations are handled by methods in other classes
 *	which take Matrix types as arguments.
 *	</p>
 */

public interface Matrix extends java.io.Serializable
{
	/**	Get number of rows in the matrix.
	 *
	 *	@return		Number of rows in the matrix.
	 */

	public int rows();

	/**	Get number of columns in the matrix.
	 *
	 *	@return		Number of columns in the matrix.
	 */

	public int columns();

	/**	Gets value of element at given row and column.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@return			The value at the given position.
	 */

	public double get( int row , int column );

	/**	Set an element at the given position to a new value.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@param value	The new value to be set.
	 */

	public void set( int row , int column , double value );

	/**	Get all elements as a new matrix.
	 *
	 *	@return		Copy of all elements as another matrix.
	 *
	 *	<p>
	 *	Essentially creates a deep clone of the current matrix.
	 *	</p>
	 *
	 */

	public Matrix getCopy();

	/**	Set all elements from another matrix.
	 *
	 *	@param matrix	Matrix whose value to copy to this matrix.
	 *
	 *	@throws			MatrixMismatchedSizeException
	 *						If the source matrix and this one don't have
	 *						the same number of rows and columns.
	 */

	public void set( Matrix matrix );

	/**	Get all elements as a two-dimensional double array.
	 *
	 *	@return		Copy of all elements as a two-dimensional double array.
	 */

	public double[][] get();

	/**	Set all elements of a matrix from a double array.
	 *
	 *	@param	values	The double[][] from which values should be set.
	 */

	public void set( double[][] values );

	/**	Get entire row as a matrix.
	 *
	 *	@param	row		Row to retrieve.
	 *
	 *	@return 		Matrix containing row.
	 */

	public Matrix getRow( int row );

	/**	Get entire row as an array of doubles .
	 *
	 *	@param	row		Row to retrieve.
	 *
	 *	@return 		Array of doubles containing row data.
	 */

	public double[] getRowData( int row );

	/**	Set entire row of values from a row matrix.
	 *
	 *	@param	row			Row to set.
	 *
	 *	@param	rowMatrix	Matrix containing row data.
	 */

	public void setRow( int row , Matrix rowMatrix );

	/**	Set entire row from an array of doubles .
	 *
	 *	@param	row			Row to set.
	 *
	 *	@param	rowData		Array of doubles containing row data.
	 */

	public void setRowData( int row , double rowData[] );

	/**	Get entire column as a matrix.
	 *
	 *	@param	column	Column to retrieve.
	 *
	 *	@return 		Matrix containing column.
	 */

	public Matrix getColumn( int column );

	/**	Get entire column as an array of doubles.
	 *
	 *	@param	column	Column to retrieve.
	 *
	 *	@return 		Array of doubles containing column data.
	 */

	public double[] getColumnData( int column );

	/**	Set entire column of values from a column matrix.
	 *
	 *	@param	column			Column to set.
	 *
	 *	@param	columnMatrix	Matrix containing column data.
	 */

	public void setColumn( int column , Matrix columnMatrix );

	/**	Set entire column from an array of doubles .
	 *
	 *	@param	column		Column to set.
	 *
	 *	@param	columnData	Array of doubles containing column data.
	 */

	public void setColumnData( int column , double columnData[] );

	/**	Set entire column from an array of ints .
	 *
	 *	@param	column		Column to set.
	 *
	 *	@param	columnData	Array of ints containing column data.
	 */

	public void setColumnData( int column , int columnData[] );

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
	);

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

	public Matrix getSubMatrix( int[] rows , int[] columns );

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

	public Matrix getSubMatrix( int firstRow , int lastRow , int[] columns );

	/**	Extract a submatrix.
	 *
	 *	@param	rows			Array of row indices.
	 *	@param	firstColumn		First column index.
	 *	@param	lastColumn		Last column index.
	 *
	 *	@return					Matrix defined by specified row and column
	 *							indices.
	 *
	 *	@throws MismatchedSizeException
	 *		If requested matrix indices are bad.
	 */

	public Matrix getSubMatrix( int[] rows , int firstColumn , int lastColumn );

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

	public Matrix getColumns( int firstColumn , int lastColumn );

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

	public Matrix getColumns( int[] columns );

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

	public Matrix getRows( int firstRow , int lastRow );

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

	public Matrix getRows( int[] rows );

	/** Return the matrix as a displayable string.
	 *
	 *	@return		The matrix as a string.
	 */

	public String toString();
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

