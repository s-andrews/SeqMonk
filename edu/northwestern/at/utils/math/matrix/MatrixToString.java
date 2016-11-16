package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.text.*;
import java.util.*;
import java.io.*;

import edu.northwestern.at.utils.math.ArithUtils;
import edu.northwestern.at.utils.PrintfFormat;
import edu.northwestern.at.utils.StringUtils;

/**	Converts matrix entries to string for output.
 */

public class MatrixToString
{
	/** Default start of matrix string. */

	protected static String defaultStartMatrixString	= "";

	/** Default end of matrix string. */

	protected static String defaultEndMatrixString		= "";

	/** Default start of matrix row string. */

	protected static String	defaultStartRowString		= "";

	/** Default end of matrix row string. */

	protected static String	defaultEndRowString			= "\n";

	/** Default end of last matrix row string. */

	protected static String	defaultEndLastRowString		= "\n";

	/** Default start of matrix entry string. */

	protected static String	defaultStartEntryString		= " ";

	/** Default end of matrix entry string. */

	protected static String	defaultEndEntryString		= " ";

	/** Default end of last matrix entry in row string. */

	protected static String	defaultEndLastEntryString	= " ";

	/** Default PrintfFormat string for converting matrix entries to strings. */

	protected static String defaultFormatString		= "%26.18g";

	/** Matrix entry output width options.
	 *
	 *	<p>
	 *	These size options may not be effective if column titles are
	 *	specified.
	 *	</p>
	 */

	/** Each entry placed in field just large enough to hold it. */

	public static final int INDIVIDUALWIDTH		= 0;

	/** Each entry placed in constant width field just large enough to
	 *	hold maximum width element.
	 */

	public static final int MAXIMUMELEMENTWIDTH	= 1;

	/** Each entry uses the width specified by the format width. */

	public static final int FORMATWIDTH			= 2;

	/** Default column alignment:  Align columns to minimum entry width. */

	protected static int defaultWidthOption	= MAXIMUMELEMENTWIDTH;

	/** Convert matrix entries to a string.
	 *
	 *	@param	matrix			The matrix to convert to XHTML string.
	 *	@param	title			Title for matrix.
	 *	@param	columnNames		Column names.
	 *	@param	formatString	PrintFformat format string for all entries.
	 *
	 *	<p>
	 *	This produces a matrix with columns of constant width in which
	 *	the matrix entries are separated by a blank and the rows of the
	 *	matrix are separated by a linefeed.  The specified format is used
	 *	to format each matrix entry.
	 *	</p>
	 */

	public static String toString
	(
		Matrix matrix ,
		String title ,
		String[] columnNames ,
		String formatString
	)
	{
		return toString
		(
			matrix ,
			title ,
			false ,
			columnNames ,
			false ,
			"",
			"\n",
			"",
			"\n",
			" ",
			" ",
			defaultStartMatrixString ,
			defaultEndMatrixString ,
			defaultStartRowString ,
			defaultEndRowString ,
			defaultEndLastRowString ,
			defaultStartEntryString ,
			defaultEndEntryString ,
			defaultEndLastEntryString ,
			formatString ,
			defaultWidthOption
		);
	}

	/** Convert matrix entries to a string.
	 *
	 *	@param	matrix		The matrix to convert to XHTML string.
	 *	@param	title		Title for matrix.
	 *	@param	columnNames	Column names.
	 *
	 *	<p>
	 *	This produces a matrix with columns of constant width in which
	 *	the matrix entries are separated by a blank and the rows of the
	 *	matrix are separated by a linefeed.  A Fortran style "G" format
	 *	is used so that entries which have no fractional portion print
	 *	as integers and numbers between 10^^-4 and 10^4 appear in
	 *	ordinary decimal format.  Numbers outside this range appear in
	 *	scientific notation with an "E" exponent.
	 *	</p>
	 */

	public static String toString
	(
		Matrix matrix ,
		String title ,
		String[] columnNames
	)
	{
		return toString
		(
			matrix ,
			title ,
			false ,
			columnNames ,
			false ,
			"",
			"\n",
			"",
			"\n",
			" ",
			" ",
			defaultStartMatrixString ,
			defaultEndMatrixString ,
			defaultStartRowString ,
			defaultEndRowString ,
			defaultEndLastRowString ,
			defaultStartEntryString ,
			defaultEndEntryString ,
			defaultEndLastEntryString ,
			defaultFormatString ,
			defaultWidthOption
		);
	}

	/** Convert matrix entries to a string.
	 *
	 *	@param	matrix		The matrix to convert to a string.
	 *
	 *	<p>
	 *	This produces a matrix with columns of constant width in which
	 *	the matrix entries are separated by a blank and the rows of the
	 *	matrix are separated by a linefeed.  A Fortran style "G" format
	 *	is used so that entries which have no fractional portion print
	 *	as integers and numbers between 10^^-4 and 10^4 appear in
	 *	ordinary decimal format.  Numbers outside this range appear in
	 *	scientific notation with an "E" exponent.
	 *	</p>
	 */

	public static String toString( Matrix matrix )
	{
		return toString
		(
			matrix ,
			null ,
			false ,
			null ,
			false ,
			"",
			"",
			"",
			"",
			"",
			"",
			defaultStartMatrixString ,
			defaultEndMatrixString ,
			defaultStartRowString ,
			defaultEndRowString ,
			defaultEndLastRowString ,
			defaultStartEntryString ,
			defaultEndEntryString ,
			defaultEndLastEntryString ,
			defaultFormatString ,
			defaultWidthOption
		);
	}

	/** Convert matrix entries to a string.
	 *
	 *	@param	matrix		The matrix to convert to a string.
	 *	@param	decimals	Decimal places for each entry.
	 *
	 *	<p>
	 *	This produces a matrix with columns of constant width in which
	 *	the matrix entries are separated by a blank and the rows of the
	 *	matrix are separated by a linefeed.  A fixed format with
	 *	"decimals" decimal places is used.
	 *	</p>
	 */

	public static String toString( Matrix matrix , int decimals )
	{
		String decimalsString	=
			StringUtils.intToString(
				Math.min( 20 , Math.max( decimals , 0 ) ) );

		return toString
		(
			matrix ,
			null ,
			false ,
			null ,
			false ,
			"",
			"",
			"",
			"",
			"",
			"",
			defaultStartMatrixString ,
			defaultEndMatrixString ,
			defaultStartRowString ,
			defaultEndRowString ,
			defaultEndLastRowString ,
			defaultStartEntryString ,
			defaultEndEntryString ,
			defaultEndLastEntryString ,
			"%26." + decimalsString + "f" ,
			defaultWidthOption
		);
	}

	/** Convert matrix entries to a string in Matlab/Scilab format.
	 *
	 *	@param	matrix		The matrix to convert to a string.
	 *
	 *	<p>
	 *	This produces a matrix with columns of variable width in which
	 *	the matrix entries are separated by a comma and the rows by a
	 *	semicolon.  Left and right brackets surround the the entire
	 *	matrix output.
	 *	</p>
	 *
	 *	<p>
	 *	Example:	"[-2,3.5,6; 7,8, 9.0; 10,-11,12]"
	 *	</p>
	 */

	public static String toMatlabString( Matrix matrix )
	{
		return toString
		(
			matrix ,
			null ,
			false ,
			null ,
			false ,
			"",
			"",
			"" ,
			"" ,
			"" ,
			"" ,
			"[" ,
			"]" ,
			"" ,
			"; " ,
			"" ,
			" " ,
			"," ,
			"" ,
			"%26.18g" ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string in Gauss format.
	 *
	 *	@param	matrix		The matrix to convert to a string.
	 *
	 *	<p>
	 *	This produces a matrix with columns of variable width in which
	 *	the matrix entries are separated by a blank and the rows by a
	 *	comma.  Left and right braces surround the the entire
	 *	matrix output.
	 *	</p>
	 *
	 *	<p>
	 *	Example:	"{-2 3.5 6, 7 8  9.0, 10,-11,12}"
	 *	</p>
	 */

	public static String toGaussString( Matrix matrix )
	{
		return toString
		(
			matrix ,
			null ,
			false ,
			null ,
			false ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"{" ,
			"}" ,
			"" ,
			", " ,
			"" ,
			" " ,
			"" ,
			"" ,
			"%26.18g" ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string in MathML format.
	 *
	 *	@param	matrix		The matrix to convert to a string.
	 *
	 *	<p>
	 *	This produces a MathML version of the matrix.
	 *	See the <a href="http://www.w3.org/Math/">w3C Math Home</a> page
	 *	for information about MathML.
	 *	</p>
	 *
	 *	<p>
	 *	Example:
	 *	</p>
	 *
	 *	<p>
	 *	&lt;matrix&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;-2&lt;/cn&gt; &lt;cn&gt;3.5&lt;/cn&gt; &lt;cn&gt;6&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;7&lt;/cn&gt; &lt;cn&gt;8&lt;/cn&gt; &lt;cn&gt;9.0&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;19&lt;/cn&gt; &lt;cn&gt;-11&lt;/cn&gt; &lt;cn&gt;12&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;/matrix&gt;
	 *	</p>
	 */

	public static String toMathMLString( Matrix matrix )
	{
		return toString
		(
			matrix ,
			null ,
			false ,
			null ,
			false ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"<matrix>\n" ,
			"</matrix>\n" ,
			"<matrixrow>\n" ,
			"\n</matrixrow>\n" ,
			"\n</matrixrow>\n" ,
			"<cn>" ,
			"</cn> " ,
			"</cn> " ,
			"%26.18g" ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string in XHTML table format.
	 *
	 *	@param	matrix		The matrix to convert to XHTML string.
	 *
	 *	<p>
	 *	This produces an XHTML version of the matrix.
	 *	</p>
	 *
	 *	<p>
	 *	Example:
	 *	</p>
	 *
	 *	<p>
	 *	&lt;matrix&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;-2&lt;/cn&gt; &lt;cn&gt;3.5&lt;/cn&gt; &lt;cn&gt;6&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;7&lt;/cn&gt; &lt;cn&gt;8&lt;/cn&gt; &lt;cn&gt;9.0&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;19&lt;/cn&gt; &lt;cn&gt;-11&lt;/cn&gt; &lt;cn&gt;12&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;/matrix&gt;
	 *	</p>
	 */

	public static String toXHTMLString( Matrix matrix )
	{
		return toString
		(
			matrix,
			null ,
			false ,
			null ,
			false ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"" ,
			"<table border=\"1\">\n" ,
			"</table>\n" ,
			"<tr>\n" ,
			"\n</tr>\n" ,
			"\n</tr>\n" ,
			"<td>" ,
			"</td> " ,
			"</td> " ,
			"%26.18g" ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string in XHTML table format.
	 *
	 *	@param	matrix		The matrix to convert to XHTML string.
	 *	@param	title		Title for matrix.
	 *	@param	columnNames	Column names.
	 *
	 *	<p>
	 *	This produces an XHTML version of the matrix with a title and
	 *	column names.
	 *	</p>
	 *
	 *	<p>
	 *	Example:
	 *	</p>
	 *
	 *	<p>
	 *	&lt;matrix&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;-2&lt;/cn&gt; &lt;cn&gt;3.5&lt;/cn&gt; &lt;cn&gt;6&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;7&lt;/cn&gt; &lt;cn&gt;8&lt;/cn&gt; &lt;cn&gt;9.0&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;19&lt;/cn&gt; &lt;cn&gt;-11&lt;/cn&gt; &lt;cn&gt;12&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;/matrix&gt;
	 *	</p>
	 */

	public static String toXHTMLString
	(
		Matrix matrix ,
		String title ,
		String[] columnNames
	)
	{
		return toString
		(
			matrix ,
			title ,
			true ,
			columnNames ,
			true ,
			"<caption><big>" ,
			"</big></caption>\n" ,
			"<tr>\n" ,
			"\n</tr>\n" ,
			"<th>" ,
			"</th>" ,
			"<table border=\"1\">\n" ,
			"</table>\n" ,
			"<tr>\n" ,
			"\n</tr>\n" ,
			"\n</tr>\n" ,
			"<td>" ,
			"</td> " ,
			"</td> " ,
			"%26.18g" ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string in XHTML table format.
	 *
	 *	@param	matrix			The matrix to convert to XHTML string.
	 *	@param	title			Title for matrix.
	 *	@param	columnNames		Column names.
	 *	@param	border			Border value for table (0=no border).
	 *	@param	formatString	Format string for matrix entries.
	 *
	 *	<p>
	 *	This produces an XHTML version of the matrix with a title and
	 *	column names and with entries in a specified format.
	 *	</p>
	 *
	 *	<p>
	 *	Example:
	 *	</p>
	 *
	 *	<p>
	 *	&lt;matrix&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;-2&lt;/cn&gt; &lt;cn&gt;3.5&lt;/cn&gt; &lt;cn&gt;6&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;7&lt;/cn&gt; &lt;cn&gt;8&lt;/cn&gt; &lt;cn&gt;9.0&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;matrixrow&gt;
	 *	&lt;cn&gt;19&lt;/cn&gt; &lt;cn&gt;-11&lt;/cn&gt; &lt;cn&gt;12&lt;/cn&gt;
	 *	&lt;/matrixrow&gt;
	 *	&lt;/matrix&gt;
	 *	</p>
	 */

	public static String toXHTMLString
	(
		Matrix matrix ,
		String title ,
		String[] columnNames ,
		int border ,
		String formatString
	)
	{
		return toString
		(
			matrix ,
			title ,
			true ,
			columnNames ,
			true ,
			"<caption><big>" ,
			"</big></caption>\n" ,
			"<tr>\n" ,
			"\n</tr>\n" ,
			"<th>" ,
			"</th>" ,
			"<table border=\"" + border + "\">\n" ,
			"</table>\n" ,
			"<tr>\n" ,
			"\n</tr>\n" ,
			"\n</tr>\n" ,
			"<td>" ,
			"</td> " ,
			"</td> " ,
			formatString ,
			MatrixToString.INDIVIDUALWIDTH
		);
	}

	/** Convert matrix entries to a string.
	 *
	 *	@param	matrix						The matrix to convert to a string.
	 *	@param	title						Title for matrix.
	 *										If null, no title output.
	 *	@param	titleEmbedded				True to output title after
	 *										startMatrixString, false to
	 *										output before StartMatrixString.
	 *	@param	columnNames					Names for each column in matrix.
	 *										If null, no column names output.
	 *	@param	columnNamesEmbedded			True to output column names
	 *										embedded, false otherwise.
	 *	@param	startTitleString			String to output before title.
	 *	@param	endTitleString				String to output after title.
	 *	@param	startColumnNamesString		String to output at start of
	 *										column names row.
	 *	@param	endColumnNamesString		String to output at end of
	 *										column names row.
	 *	Wparam	startColumnNameString		String to output before column name.
	 *	@param	endColumnNameString			String to output after column name.
	 *	@param	startMatrixString			String to output before the first
	 *										matrix entry.
	 *	@param	endMatrixString				String to output after the last
	 *										matrix entry.
	 *	@param	startMatrixRowString		String to output at start of each
	 *										matrix row.
	 *	@param	endMatrixRowString			String to output at end of each
	 *										matrix row except last.
	 *	@param	endLastMatrixRowString		String to output at end of last
	 *										matrix row.
	 *	@param	startMatrixEntryString		String to output before each matrix
	 *										entry.
	 *	@param	endMatrixEntryString		String to output after each matrix
	 *										entry except last in row.
	 *	@param	endLastMatrixEntryString	String to output after last
	 *										entry in row.
	 *	@param	formatString				PrintfFormat format specification to
	 *										convert matrix entries.
	 *	@param	widthOption					Output width option.
	 *										"Matrix entry output width options"
	 *										above for possible values.
	 *
	 *	<p>
	 *	If the number of column names specified is less than the number
	 *	of columns in the matrix, blank column names are used for the remainder.
	 *	If you do not want any column names, specify null as the value
	 *	of the columnNames parameter.
	 *	</p>
	 */

	public static String toString
	(
		Matrix matrix ,
		String title ,
		boolean titleEmbedded ,
		String[] columnNames ,
		boolean columnNamesEmbedded ,
		String startTitleString ,
		String endTitleString ,
		String startColumnNamesString ,
		String endColumnNamesString ,
		String startColumnNameString ,
		String endColumnNameString ,
		String startMatrixString ,
		String endMatrixString ,
		String startMatrixRowString ,
		String endMatrixRowString ,
		String endLastMatrixRowString ,
		String startMatrixEntryString ,
		String endMatrixEntryString ,
		String endLastMatrixEntryString ,
		String formatString ,
		int widthOption
	)
	{
								// Create format element.

		PrintfFormat format		= new PrintfFormat( formatString );

		double x;
		String s;
                                // maxColWidth will hold the maximum width
                                // of a converted element.

		int maxColWidth		= 0;

								// If column names specified, set
								// maxColWidth to the widest column name.

		if ( columnNames != null )
		{
			for ( int i = 0 ; i < columnNames.length ; i++ )
			{
				maxColWidth	=
					Math.max( maxColWidth , columnNames[ i ].length() );
			}
			              		// Reset width option to maximum element width
			              		// when column names specified and
			              		// individual element width option requested.

			if ( widthOption == INDIVIDUALWIDTH )
			{
				widthOption	= MAXIMUMELEMENTWIDTH;
			}
		}
								// sValues holds the converted matrix elements.

		ArrayList sValues	= new ArrayList();

								// Convert each matrix entry to string
								// using the specfied format.

		for ( int i = 1; i <= matrix.rows(); i++ )
		{
			for ( int j = 1; j <= matrix.columns(); j++ )
			{
								// Get next matrix elment.

				x	= matrix.get( i , j );

								// Convert to string.

				s	= format.sprintf( x );

								// Trim converted entry to minimum width
								// by removing leading and trailing blanks
								// unless we are to use the width specified
								// by the format entry.

				if ( widthOption != FORMATWIDTH )
				{
					s	= StringUtils.trim( s );
				}

								// Add converted element to sValue array list.

				sValues.add( new String( s ) );

								// If this converted entry is the widest,
								// remember that.

				if ( s.length() > maxColWidth )
					maxColWidth	= s.length();
			}
		}
								// Index of matrix entry being output.

		int k	= 0;
								// Create string buffer to accumulate the
								// matrix string.

		StringBuffer sb		=
			new StringBuffer( maxColWidth * matrix.rows() * matrix.columns() );

								// Add title if it comes before
								// the start of the matrix string.

		if ( ( title != null ) && !titleEmbedded )
		{
			sb	= sb.append( startTitleString );
			sb	= sb.append( title );
			sb	= sb.append( endTitleString );
		}
								// The matrix start string begins the
								// output.

		sb	= sb.append( startMatrixString );

								// Add title if it comes after
								// the start of the matrix string.

		if ( ( title != null ) && titleEmbedded )
		{
			sb	= sb.append( startTitleString );
			sb	= sb.append( title );
			sb	= sb.append( endTitleString );
		}
								// Output column names.

		if ( columnNames != null )
		{
			sb	= sb.append( startColumnNamesString );

			for (	int j = 0 ;
					j < Math.min( columnNames.length , matrix.columns() ) ;
					j++ )
			{
								// Output the string which
								// starts a column name.

				sb	= sb.append( startColumnNameString );

								// Output column name.

//				sb	= sb.append( columnNames[ j ] );
				sb	=
					sb.append(
						StringUtils.lpad( columnNames[ j ] , maxColWidth ) );

								// Output the string which
								// ends a column name.

				sb	= sb.append( endColumnNameString );
			}

			sb	= sb.append( endColumnNamesString );
		}
								// Loop over matrix rows.

		for ( int i = 1 ; i <= matrix.rows() ; i++ )
		{
								// Output string which starts a matrix row.

			sb	= sb.append( startMatrixRowString );

								// Loop over matrix columns.

			for ( int j = 1 ; j <= matrix.columns() ; j++ )
			{
								// Output the string which
								// starts a matrix entry.

				sb	= sb.append( startMatrixEntryString );

								// Pick up next converted matrix value.

				s	= (String)sValues.get( k++ );

								// If we're outputting all matrix elements
								// in a field of size equal to the width
								// of the largest element, left pad the
								// current converted element with enough
								// blanks to match the maximum width element.

				if ( widthOption == MAXIMUMELEMENTWIDTH )
				{
//					s	=
//						StringUtils.dupl(
//						' ' , maxColWidth - s.length() ) + s;
					s	= StringUtils.lpad( s , maxColWidth );
				}

								// Append current converted matrix element
								// to output string buffer.

				sb	= sb.append( s );

								// Output the string which
								// ends a matrix entry.

				if ( j < matrix.columns() )
				{
					sb	= sb.append( endMatrixEntryString );
				}
				else
				{
					sb	= sb.append( endLastMatrixEntryString );
				}
			}
								// Append end of row string.

			if ( i < matrix.rows() )
			{
				sb	= sb.append( endMatrixRowString );
			}
			else
			{
				sb	= sb.append( endLastMatrixRowString );
			}
		}
								// Append end of matrix string.

		sb.append( endMatrixString );

								// Convert string buffer to a string and
								// return the string to the caller.

		return sb.toString();
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

