package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.util.*;

/**	Implements a sparse matrix using a hash map.
 */

public class SparseMatrix extends AbstractMatrix implements Matrix
{
	/** The matrix contents as a two-dimensional array of doubles. */

	protected HashMap matrixData;

	/** Don't allow instantiation without size specification.
	 */

	protected SparseMatrix()
	{
	}

	/** Create a matrix of the specified size.
	 *
	 *	@param	rows		Number of rows.
	 *	@param	columns		Number of columns.
	 *
	 *	<p>
	 *	Each element of the result matrix will be set to zero.
	 *	</p>
	 */

	public SparseMatrix( int rows , int columns )
	{
		super( rows , columns );

		matrixData		= new HashMap( rows * columns );
	}

	/**	Set an element at the given position to a new value.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@param value	The new value to be set.
	 *
	 *	<p>
	 *	Note:  	only non-zero values are stored.
	 *	</p>
	 */

	public void set( int row, int column, double value )
	{
		String key		= row + ":" + column;
		Object oValue	= matrixData.get( key );

								//	Remove existing value, if any.

		if ( oValue != null )
		{
			matrixData.remove( key ) ;
		}
								//	Add new value if non-zero.

		if ( value != 0.0D )
		{
			matrixData.put( key , new Double( value ) ) ;
		}
	}

	/**	Gets value of element at given row and column.
	 *
	 *	@param row		Row in which the element occurs.
	 *	@param column	Column in which the element occurs.
	 *	@return			The value at the given position.
	 */

	public double get( int row , int column )
	{
								//	Assume value of zero as default.

		double	result	= 0.0D;

		if	(	( row > 0 ) && ( row <= rows() ) &&
		        ( columns > 0 ) && ( columns <= columns() ) )
		{
			Object oValue	=
				matrixData.get( row + ":" + column );

			if ( oValue != null )
			{
				result	= ((Double)oValue).doubleValue();
			}
		}

		return result;
	}

	public int getUsed()
	{
		return matrixData.size();
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

