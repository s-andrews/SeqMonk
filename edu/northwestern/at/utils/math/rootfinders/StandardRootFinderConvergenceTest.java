package edu.northwestern.at.utils.math.rootfinders;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;

/**	Standard test for convergence in root finders.
 */

public class StandardRootFinderConvergenceTest
	implements RootFinderConvergenceTest
{
	/**	Create StandardRootFinderConvergenceTest.
	 */

	public StandardRootFinderConvergenceTest()
	{
	}

 	/*	Test for convergence in root finder.
 	 *
 	 *	@param	xNow			Current root estimate.
 	 *	@param	xPrev			Previous root estimate.
 	 *	@param	dfxNow			Function value at xNow.
 	 *	@param	xTolerance		Convergence tolerance for estimates.
 	 *	@param	fxTolerance		Convergence tolerance for function values.
 	 *
 	 *	@return					true if convergence achieved.
 	 *
 	 *	<p>
 	 *	This standard convergence test indicates convergence when
 	 *	|xNow-xPrev| <= xTolerance, or |fxNow| <= fxTolerance.
 	 *	</p>
 	 */

	public boolean converged
	(
		double xNow ,
		double xPrev ,
		double fxNow ,
		double xTolerance ,
		double fxTolerance
	)
	{
		return
			( Math.abs( xNow - xPrev ) <= xTolerance ) ||
			( Math.abs( fxNow ) <= fxTolerance );
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

