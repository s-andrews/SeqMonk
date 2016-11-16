package edu.northwestern.at.utils.math.rootfinders;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;

/** Find roots of equations using Newton/Raphson iteration.
 *
 *	<p>
 *	The Method of NewtonRaphson is a root-finding method which requires an
 *	initial estimate x0 for a root and that the function be
 *	continuous and everywhere differentiable.
 *	</p>
 *
 *	<p>
 *	If the derivative of the function whose root is being sought
 *	is difficult or expensive to compute, the Method of Secants or
 *	Brent's Method is a better choice.  If the function is not
 *	everywhere differentiable, Bisection is the method to use.
 *	</p>
 */

public class NewtonRaphson implements MonadicFunctionRootFinder
{
	/**	Find root using the Method of Newton/Raphson.
	 *
	 *	@param	x0						First approximation to root value.
	 *	@param	tol						Desired accuracy for root value.
	 *	@param	maxIter					Maximum number of iterations.
	 *	@param	function				Class implementing MonadicFunction
	 *									interface to provide function values.
	 *	@param	derivativeFunction		Class implementing MonadicFunction
	 *									interface to provide function
	 *									derivative values.
	 *	@param	convergenceTest			RootFinderConvergenceTest which
 	 *									tests for convergence of the root-finding
 	 *									process.
	 *	@param	iterationInformation	Class implementing
	 *									RootFinderIterationInformation
	 *									for retrieving information about
	 *									each iteration of root finding
	 *									process.  Set to null if you don't
	 *									want this information.
	 *
	 *	@return							Approximation to root.
	 *
	 *	@throws							IllegalArgumentException
	 *										if function or
	 *										derivativeFunction is null.
	 */

	public static double newtonRaphson
	(
		double x0 ,
		double tol ,
		int maxIter ,
		MonadicFunction function ,
		MonadicFunction derivativeFunction ,
		RootFinderConvergenceTest convergenceTest ,
		RootFinderIterationInformation iterationInformation
	)
		throws IllegalArgumentException
	{
		/* Calculated value of x at each iteration. */

		double x;

		/* Function value at calculated value of x . */

		double fx;

		/* Function derivative value at calculated value of x . */

		double dfx;

		/* Previous function value. */

		double xPrevious;
								// Make sure function and derivativeFunction are not null.

		if ( function == null )
		{
			throw new IllegalArgumentException(
				"Function cannot be null" );
		}

		if ( derivativeFunction == null )
		{
			throw new IllegalArgumentException(
				"Derivative function cannot be null" );
		}
								// Begin Newton/Raphson iteration loop.
		x	= x0;

		for ( int iter = 0 ; iter < maxIter ; iter++ )
		{
								// Compute new approximant from first order
								// Taylor series.
        	xPrevious	= x;

			fx			= function.f( xPrevious );
			dfx			= derivativeFunction.f( xPrevious );

			x			= xPrevious - ( fx / dfx );

								// Post updated iteration information.

			if ( iterationInformation != null )
			{
				iterationInformation.iterationInformation( x , fx , dfx , iter );
			}
								// See if updated function value is close
								// enough to root to stop iterations.

			if	( convergenceTest.converged
			(
				x , xPrevious , fx , tol , tol )
			)
			{
				break;
			}
		}

		return x;
	}

	/**	Find root using the Method of Newton/Raphson.
	 *
	 *	@param	x0						First approximation to root value.
	 *	@param	tol						Desired accuracy for root value.
	 *	@param	maxIter					Maximum number of iterations.
	 *	@param	function				Class implementing MonadicFunction
	 *									interface to provide function values.
	 *	@param	derivativeFunction		Class implementing MonadicFunction
	 *									interface to provide function
	 *									derivative values.
	 *
	 *	@return							Approximation to root.
	 *
	 *	@throws							IllegalArgumentException
	 *										if function or
	 *										derivativeFunction is null.
	 */

	public static double newtonRaphson
	(
		double x0 ,
		double tol ,
		int maxIter ,
		MonadicFunction function ,
		MonadicFunction derivativeFunction
	)
		throws IllegalArgumentException
	{
		return newtonRaphson(
			x0 , tol , maxIter , function , derivativeFunction ,
			new StandardRootFinderConvergenceTest() , null );
	}

	/**	Find root using the Method of Newton/Raphson.
	 *
	 *	@param	x0						First approximation to root value.
	 *	@param	function				Class implementing MonadicFunction
	 *									interface to provide function values.
	 *	@param	derivativeFunction		Class implementing MonadicFunction
	 *									interface to provide function
	 *									derivative values.
	 *
	 *	@return							Approximation to root.
	 *
	 *	@throws							IllegalArgumentException
	 *										if function or
	 *										derivativeFunction is null.
	 *
	 *	<p>
	 *	Up to 100 iterations are attempted with the convergence tolerance
	 *	set to Constants.MACHEPS .
	 *	</p>
	 */

	public static double newtonRaphson
	(
		double x0 ,
		MonadicFunction function ,
		MonadicFunction derivativeFunction
	)
		throws IllegalArgumentException
	{
		return newtonRaphson(
			x0 , Constants.MACHEPS , 100 , function , derivativeFunction ,
			new StandardRootFinderConvergenceTest() , null );
	}

	/** Implementation for {@link MonadicFunctionRootFinder} interface.
	 */

	public double findRoot
	(
		double x0 ,
		double x1 ,
		double tol ,
		int maxIter ,
		MonadicFunction function ,
		MonadicFunction derivativeFunction ,
		RootFinderConvergenceTest convergenceTest ,
		RootFinderIterationInformation iterationInformation
	)
		throws IllegalArgumentException
	{
		return newtonRaphson(
			x0 , tol , maxIter , function , derivativeFunction ,
			convergenceTest , iterationInformation );
	}

	/** Constructor if RootFinder interface used.
	 */

	public NewtonRaphson()
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

