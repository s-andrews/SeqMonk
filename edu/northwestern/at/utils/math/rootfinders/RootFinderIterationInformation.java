package edu.northwestern.at.utils.math.rootfinders;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.*;

/**	Interface for returning iteration information from root finders.
 */

public interface RootFinderIterationInformation
{
	/**	Interface for returning iteration information from root finders.
 	 *
 	 *	@param	x						Current value of x.
 	 *	@param	fx						Current function value at x.
 	 *	@param	dfx						Current function derivative value at x.
 	 *	 								Set to NAN if not used by a specific
 	 *									root finder method.
 	 *	@param	currentIteration		Current iteration number.
 	 */

	public void iterationInformation
	(
		double x ,
		double fx ,
		double dfx ,
		int currentIteration
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

