package edu.northwestern.at.utils.math.distributions;

/*	Please see the license information at the end of this file. */

/**	Point probabilities and percentage points for statistical distributions.
 *
 *	<p>
 *	This class provides "one stop shopping" for point probabilities
 *	percentage points for commonly used statistical distributions.
 *	These include the Beta, F, t, chi-square, and normal distributions.
 *	Both the forward (probability) and inverse (percentage point) functions
 *	are provided.  This class is actually just provides a shim to the
 *	methods in the individual classes for each distribution.
 *	</p>
 *
 *	<p>
 *	Point probability routines
 *	</p>
 *
 *	<ul>
 *	<li>chisquare		-- significance of chi-square</li>
 *	<li>f				-- significance of F</li>
 *	<li>normal			-- significance of normal value</li>
 *	<li>t				-- significance of Student's t</li>
 *	</ul>
 *
 *	<p>
 *	Inverse distributions (percentage points)
 *	</p>
 *
 *	<ul>
 *	<li>chisquareInverse			-- inverse chi-square</li>
 *	<li>fInverse					-- inverse F</li>
 *	<li>normalInverse				-- inverse normal</li>
 *	<li>tInverse					-- inverse t</li>
 *	</ul>
 */

public class Sig
{
	/*	--- Chisquare --- */

	public static double chisquare( double chiSquare , double df )
	{
		return ChiSquare.chisquare( chiSquare , df );
	}

	public static double chisquare( double chiSquare , int df )
	{
		return ChiSquare.chisquare( chiSquare , (double)df );
	}

	public static double chisquareInverse( double p , double df )
	{
		return ChiSquare.chisquareInverse( p , df );
	}

	public static double chisquareInverse( double p , int df )
	{
		return ChiSquare.chisquareInverse( p , (double)df );
	}

	/*	--- Fisher's F --- */

	public static double f( double f , double dfn , double dfd )
	{
		return FishersF.f( f , dfn , dfd  );
	}

	public static double f( double f , int dfn , int dfd )
	{
		return FishersF.f( f , (double)dfn , (double)dfd  );
	}

	public static double fInverse( double p , double dfn , double dfd )
	{
		return FishersF.fInverse( p , dfn , dfd );
	}

	public static double fInverse( double p , int dfn , int dfd )
	{
		return FishersF.fInverse( p , (double)dfn , (double)dfd );
	}

	/*	--- Normal distribution --- */

	public static double normal( double z )
	{
		return Normal.normal( z );
	}

	public static double normalInverse( double p )
	{
		return Normal.normalInverse( p );
	}

	/*	--- Student's t --- */

	public static double t( double t , double df )
	{
		return Studentst.t( t , df );
	}

	public static double t( double t , int df )
	{
		return Studentst.t( t , (double)df );
	}

	public static double tInverse( double p , double df )
	{
		return Studentst.tInverse( p , df );
	}

	public static double tInverse( double p , int df )
	{
		return Studentst.tInverse( p , (double)df );
	}

    public static double sidak( double p , int n )
    {
		double result	= p;

        if ( n > 1 ) result	= 1.0D - Math.pow( 1.0D - p , ( 1.0D / n ) );

		return result;
    }

    public static double bonferroni( double p , int n )
    {
		double result	= p;

        if ( n > 1 ) result	= result / (double)n;

		return result;
    }

	/**	Make class non-instantiable but inheritable.
	 */

	protected Sig()
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

