package edu.northwestern.at.utils;

/*	Please see the license information at the end of this file. */

import java.io.*;
import java.util.*;
import java.text.*;

/**	Formatting utilties. */

public class Formatters {
	
	/**	Number formatter for numbers with commas. */
	
	static private final NumberFormat COMMA_FORMATTER = 
		NumberFormat.getInstance();
		
	static {
		COMMA_FORMATTER.setGroupingUsed(true);
	}

	/**	Formats an integer with commas.
	 *
	 *	@param	n		The number.
	 *
	 *	@return			The formatted number with commas.
	 */

	public static String formatIntegerWithCommas (int n) {
		return COMMA_FORMATTER.format(n);
	}

	/**	Formats a long with commas.
	 *
	 *	@param	n		The number.
	 *
	 *	@return			The formatted number with commas.
	 */

	public static String formatLongWithCommas (long n) {
		return COMMA_FORMATTER.format(n);
	}
	
	/**	Number formatter for floating point numbers. */
	
	static private final NumberFormat FLOAT_FORMATTER = 
		NumberFormat.getInstance();
		
	static {
		FLOAT_FORMATTER.setMinimumIntegerDigits(1);
	}
	
	/**	Formats a float.
	 *
	 *	<p>The formatted number always has a minimum of one digit
	 *	before the decimal point, and a fixed specified number
	 *	of digits after the decimal point.
	 *
	 *	@param	x		The number.
	 *
	 *	@param	d		Number of digits after the decimal point.
	 *
	 *	@return			The formatted number.
	 */
	 
	public static String formatFloat (float x, int d) {
		FLOAT_FORMATTER.setMinimumFractionDigits(d);
		FLOAT_FORMATTER.setMaximumFractionDigits(d);
		return FLOAT_FORMATTER.format(x);
	}
	
	/**	Formats a double.
	 *
	 *	<p>The formatted number always has a minimum of one digit
	 *	before the decimal point, and a fixed specified number
	 *	of digits after the decimal point.
	 *
	 *	@param	x		The number.
	 *
	 *	@param	d		Number of digits after the decimal point.
	 *
	 *	@return			The formatted number.
	 */
	 
	public static String formatDouble (double x, int d) {
		FLOAT_FORMATTER.setMinimumFractionDigits(d);
		FLOAT_FORMATTER.setMaximumFractionDigits(d);
		return FLOAT_FORMATTER.format(x);
	}

	/** Hides the default no-arg constructor. */

	private Formatters () {
		throw new UnsupportedOperationException();
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

