package edu.northwestern.at.utils.math.rootfinders;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;

/**	Interface implemented by monadic function root finders.
 */

public interface MonadicFunctionRootFinder
{
	/** MonadicFunctionRootFinder interface.
	 *
	 *	@param	x0						Left bracket value for root.
	 *	@param	x1						Right bracket value for root.
	 *									Not used by some root-finder
	 *									(e.g., Newton/Raphson),
	 *									set to same value as x0 in those cases.
	 *	@param	tol						Convergence tolerance.
	 *	@param	maxIter					Maximum number of iterations.
	 *	@param	function				MonadicFunction computes value for
	 *									function whose root is being sought.
	 *	@param	derivativeFunction		MonadicFunction computes derivative
	 *									value for function whose root is
	 *									being sought.  Currently used only
	 *									by Newton/Raphson, set to null for
	 *									other methods.
	 *	@param	convergenceTest			RootFinderConvergenceTest which
	 *									tests for convergence of the root-finding
	 *									process.
	 *	@param	iterationInformation	Method implementing the
	 *									RootFinderIterationInformation
	 *									interace.  Allows retrieval of
	 *									function, function derivative, and
	 *									iteration number for each iteration
	 *									in the root-finding process.
	 *									Can be set to null if you don't want
	 *									to get that information.
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
	);
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

