package edu.northwestern.at.utils.math;

/*	Please see the license information at the end of this file. */

/** Interface for a dyadic two argument function.
 */

public interface DyadicFunction
{
	/** Evaluates a dyadic scalar function.
	 *
	 *	@param	x1	Values at which function is to be evaluated.
	 *	@param	x2
	 *
	 *	@return		Result of evaluating function using x1, x2.
	 */

	public double f( double x1 , double x2 );
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

