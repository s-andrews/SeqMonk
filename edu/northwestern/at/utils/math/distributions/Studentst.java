package edu.northwestern.at.utils.math.distributions;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;
import edu.northwestern.at.utils.math.rootfinders.*;

/**	Student's t distribution functions.
 */

public class Studentst
{
	/** Compute probability for Student t distribution.
	 *
	 *	@param	t	Percentage point of Student t distribution
	 *
	 *	@param	df	Degrees of freedom
	 *
	 *	@return		The corresponding probability for the
	 *				Student t distribution.
	 *
	 *	@throws		IllegalArgumentException
	 *                  if df <= 0
	 *
	 *	<p>
	 *	The probability is computed using the following
	 *	relationship between the incomplete beta distribution
	 *	and Student's t:
	 *	</p>
	 *
	 *	<p>
	 *	tprob(t) = incompleteBeta( df/(df*t*t), df/2, 0.5 )
	 *	</p>
	 *
	 *	<p>
	 *	The result is accurate to about 14 decimal digits.
	 *	</p>
	 */

	public static double t( double t , double df )
		throws IllegalArgumentException
	{
		double result	= 0.0D;

		if ( df > 0.0D )
		{
			result	=
				Beta.incompleteBeta
				(
					df / ( df + t * t ) ,
					df / 2.0D ,
					0.5D ,
					Constants.MAXPREC
				);
		}
		else
		{
			throw new IllegalArgumentException( "df <= 0" );
		}

		return result;
	}

	/** Compute percentage point for Student t distribution.
	 *
	 *	@param	p	Probability level for percentage point
	 *	@param	df	Degrees of freedom
	 *
	 *	@return		The corresponding percentage point of
	 *				the Student t distribution.
	 *
	 *	@throws		IllegalArgumentException
	 *					p < 0 or p > 1 or df <= 0
     *
	 *	@throws		ArithmeticException
	 *					if incomplete beta evaluation fails
	 *
	 *	<p>
	 *	The percentage point is computed using the inverse
	 *	incomplete beta distribution.  This allows for fractional
	 *	degrees of freedom.
	 *	</p>
	 */

	public static double tInverse( double p , double df )
		throws ArithmeticException, IllegalArgumentException
	{
		double result	= 0.0D;

		if ( df > 0.0D )
		{
			if ( ( p >= 0.0D ) && ( p <= 1.0D ) )
			{
				result	=
					Beta.incompleteBetaInverse( 1.0D - p , 0.5D , df / 2.0D );

            	if ( ( result >= 0.0D ) && ( result < 1.0D ) )
            	{
            		result  = Math.sqrt( result * df / ( 1.0D - result ) );
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
			throw new IllegalArgumentException( "df <= 0" );
		}

		return result;
	}

	/**	Make class non-instantiable but inheritable.
	 */

	protected Studentst()
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

