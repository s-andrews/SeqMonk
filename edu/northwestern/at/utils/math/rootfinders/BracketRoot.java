package edu.northwestern.at.utils.math.rootfinders;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;

/** Find interval bracketing a root.
 */

public class BracketRoot
{
	/** Find interval bracketing a root.
	 *
	 *	@param	bracket				Array containing initial estimate for
	 *								of bracketing interval.
	 *
	 *								bracket[0] is left hand estimate.
	 *								bracket[1] is right hand estimate.
	 *
	 *	@param	function			Function whose root is to be bracketed.
	 *
	 *	@param	maxIter				Maximum number of iterations to try
	 *								expanding bracket.
	 *
	 *	@param	expansionFactor		Factor by which to expand bracket interval
	 *								on each iteration.
	 *
	 *	@return						True if bracket successfully found
	 *								The new bracket will be placed in
	 *								"bracket".
	 *
	 *								False if bracket could not be found within
	 *								maxIter attempts.
	 */

	public static boolean bracketRoot
	(
		double bracket[],
		MonadicFunction function,
		int maxIter,
		double expansionFactor
	)
	{
								// Check for valid initial bracket.

		if	(	( bracket == null ) || ( bracket.length < 2 ) ||
				( bracket[ 0 ] == bracket[ 1 ] ) )
		{
			throw new IllegalArgumentException( "initial bracket bad" );
		}
								// Compute function at initial
								// left and right bracket points.

		double fLeft	= function.f( bracket[ 0 ] );
		double fRight	= function.f( bracket[ 1 ] );

		for ( int iter = 1; iter <= maxIter; iter++ )
		{
								// If the left and right function
								// values are of different sign,
								// a zero is bracketed between them.

			if ( ( fLeft * fRight ) < 0.0 ) return true;

								// Determine which end of the bracket
								// to expand.

			if ( Math.abs( fLeft ) < Math.abs( fRight ) )
			{
				bracket[ 0 ]	=
					expansionFactor * ( bracket[ 0 ] - bracket[ 1 ] );

				fLeft			= function.f( bracket[ 0 ] );
			}
			else
			{
				bracket[ 1 ]	=
					expansionFactor * ( bracket[ 1 ] - bracket[ 0 ] );

				fRight			= function.f( bracket[ 1 ] );
			}
		}

		return false;
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

