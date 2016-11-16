package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import edu.northwestern.at.utils.math.matrix.*;

/**	Perform element-by-element operation on a matrix.
 *
 *	<p>
 *	Encapsulates an operation that can be performed on elements at the
 *	same position in two matrices.
 *	</p>
 */

public interface MatrixEBEOperation
{
	/**	Apply an operation to matrix elements.
	 *
	 *	@param	a 	Matrix element.
	 *	@param	b	Matrix element.
	 *
	 *	@return		Result of applying operation to a and b.
	 */

	double apply( double a , double b );
}

/*
 * <p>
 * JMatrices is copyright &copy; 2001-2004 by Piyush Purang.
 * </p>
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * </p>
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * </p>
 * <p>
 * Modifications 2004-2006 by Northwestern University.
 * </p>
 */

