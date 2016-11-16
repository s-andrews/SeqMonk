package edu.northwestern.at.utils.math.distributions;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;
import edu.northwestern.at.utils.math.rootfinders.*;

/**	Fisher's F distribution functions.
 */

public class FishersF
{
	/** Compute probability for Fisher's F distribution.
	 *
	 *	@param	f		Percentage point of Fisher's F distribution
	 *	@param	dfn		Numerator degrees of freedom
	 *	@param	dfd		Denominator degrees of freedom
	 *
	 *	@return			The corresponding probabiity for
	 *					Fisher's F distribution.
	 *
	 *	@throws		    IllegalArgumentException
	 *                  	if dfn <= 0 or dfd <= 0 .
	 *
	 *	<p>
	 *	fprob(f) = incompleteBeta( dfd/(dfd*f*dfn), df/2, dfn/2 )
	 *	</p>
	 *
	 *	<p>
	 *	The result is accurate to about 14 decimal digits.
	 *	</p>
	 */

	public static double f( double f , double dfn , double dfd )
		throws IllegalArgumentException
	{
		double result = 0.0D;

		if ( ( dfn > 0.0D ) && ( dfd > 0.0D ) )
		{
			result	=
				Beta.incompleteBeta
				(
					dfd / ( dfd + f * dfn ) ,
					dfd / 2.0D ,
					dfn / 2.0D ,
					Constants.MAXPREC
				);
		}
		else
		{
			throw new IllegalArgumentException( "dfn or dfd <= 0" );
		}

		return result;
	}

	/** Compute percentage point for Fisher's F distribution.
	 *
	 *	@param	p		Probability level for percentage point.
	 *	@param	dfn		Numerator degrees of freedom.
	 *	@param	dfd		Denominator degrees of freedom.
	 *
	 *	@return			The corresponding probabiity for Fisher's F
	 *					distribution.
	 *
	 *	@throws			IllegalArgumentException
	 *                  	if p < 0 or p > 1 or dfn <= 0 or dfd <= 0 .
	 *
	 *	@throws			ArithmeticException
	 *						if incomplete beta evaluation fails
	 */

	public static double fInverse( double p, double dfn, double dfd )
		throws IllegalArgumentException
	{
		double result	= 0.0D;

		if ( ( dfn > 0.0D ) && ( dfd > 0.0D ) )
		{
			if ( ( p >= 0.0D ) && ( p <= 1.0D ) )
			{
	            result	=
	            	Beta.incompleteBetaInverse( 1.0D - p , dfn / 2.0D , dfd / 2.0D );

				if ( ( result >= 0.0D ) && ( result < 1.0D ) )
				{
					result  = result * dfd / ( dfn * ( 1.0D - result ) );
				}
				else
				{
					throw new ArithmeticException(
						"inverse incomplete beta evaluation failed" );
				}
			}
			else
			{
				throw new IllegalArgumentException( "p < 0 or p > 1" );
			}
		}
		else
		{
			throw new IllegalArgumentException( "dfn or dfd <= 0" );
		}

		return result;
	}

	/**	Make class non-instantiable but inheritable.
	 */

	protected FishersF()
	{
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

