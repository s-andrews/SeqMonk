package edu.northwestern.at.utils;

/*	Please see the license information at the end of this file. */

import java.util.*;

/**	Sorting.
 *
 *	<p>Sorting provides methods for sorting values.</p>
 */

public class Sorting
{
	/**	Recursive quicksort that carries along an index array.
	 *
	 *	@param	data	The data to sort (input and output).
	 *	@param	index	The index (permutation) vector (output).
	 *	@param	lo0		The first element to sort.
	 *	@param	hi0		The last element to sort.
	 *
	 *	<p>
	 *	On output, data is sorted in ascending order.
	 *	The index array contains the permuted indices of the sorted
	 *	elements.
	 *	</p>
	 */

	public static void quickSort
	(
		double data[] ,
		int index[] ,
		int lo0 ,
		int hi0
	)
	{
		int lo	= lo0;
		int hi	= hi0;

		double mid;
		double dtemp;
		int itemp;

		if ( hi0 > lo0 )
		{
			mid	= data[ ( lo0 + hi0 ) / 2 ];

			while( lo <= hi )
			{
				while( ( lo < hi0 ) && ( data[ lo ] < mid ) ) lo++;

				while( ( hi > lo0 ) && ( data[ hi ] > mid ) ) hi--;

				if ( lo <= hi )
				{
					dtemp		= data[ lo ];
					data[ lo ]	= data[ hi ];
					data[ hi ]	= dtemp;

					itemp		= index[ lo ];
					index[ lo ]	= index[ hi ];
					index[ hi ]	= itemp;

					lo++;
					hi--;
				}
			}

			if ( lo0 < hi )
				quickSort( data , index , lo0 , hi );

			if( lo < hi0 )
            	quickSort( data , index , lo , hi0 );
		}
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

