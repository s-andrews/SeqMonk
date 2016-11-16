package edu.northwestern.at.utils.math.matrix;

/*	Please see the license information at the end of this file. */

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;
import java.io.BufferedReader;

/**	MatrixFromString provides methods for parsing a string containing a matrix.
 *
 *	<p>
 *	MatrixFromString can parse the following types of matrix input.
 *	</p>
 *
 *	<ul>
 *	<li>
 *	<p>A matrix specified in Matlab/Scilab format.
 *	</p>
 *	<p>
 *	Example: [ -2,3.5,6;   7,8, 9.0; 10,-11,12   ]
 *	</p>
 *	</li>
 *	<li>
 *	<p>
 *	A matrix specified in Gauss format.
 *	</p>
 *	<p>
 *	Example: { 2 -3.5 6 , 7 -8 9.0, 10 11 12}
 *	</p>
 *	</li>
 *	<li>
 *	<p>A matrix specified in plain text format, with linefeeds separating
 *	rows of the matrix.
 *	</p>
 *	<p>
 *	Example:
 *	</p>
 *	<p>
 *	2 -3.5 6<br />
 *	7 -8 9.0<br />
 *	10 11 12<br />
 *	</p>
 *	</li>
 *	<li>
 *	<p>
 *	A matrix specified in MathML format.
 *	</p>
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
 *	</li>
 *	</ul>
 *
 *	<p>
 *	You can use the method MatrixFromStringParser to parse other types of
 *	matrix definition strings as long as these can be specified in terms
 *	of regular expressions.  MatrixFromStringParser underlies all the parsing
 *	methods here.
 *	</p>
 *
 *	<p>
 *	While it is possible to parse simple MathML using regular expressions,
 *	as we do here, it may be better to use an XML parser.
 *	To do this, uncomment the imports above for org.jdom.* and
 *	see the comments below preceding the method fromMathMLString .
 *	</p>
 */

public class MatrixFromString
{
	public static final MatrixFromStringParser matlabMatrixFromString		=
		new MatrixFromStringParser(
			"\\[" , "\\]" , "", ";" , "", "," );

	public static final MatrixFromStringParser gaussMatrixFromString		=
		new MatrixFromStringParser(
			"\\{" , "\\}" , "", "," , "", "\\s" );

	public static final MatrixFromStringParser plainTextMatrixFromString	=
		new MatrixFromStringParser(
			"" , "" , "", "\\n" , "", "\\s" );

	public static final MatrixFromStringParser mathMLMatrixFromString		=
		new MatrixFromStringParser(
			"<matrix>" , "</matrix>" , "<matrixrow>", "</matrixrow>" ,
			"<cn>", "</cn>" );

	/** Parse a matrix definition string with a specified Matrix parser.
	 *
	 *	@param	src		String containing matrix definition.
	 *	@param	parser	The matrix parser to use.
	 *
	 *	@return			The matrix corresponding to the definition string.
	 */

	public static Matrix parseMatrix( String src , MatrixFromStringParser parser )
	{
		return parser.parse( src );
	}

	/** Parse a matrix definition string in Matlab format.
	 *
	 *	@param	src		String containing matrix definition.
	 *
	 *	@return			The matrix corresponding to the definition string.
	 *
	 *	<p>
	 *	Sample matrix definition:	[ -2,3.5,6;   7,8, 9.0; 10,-11,12   ]
	 *	</p>
	 */

	public static Matrix fromMatlabString( String src )
	{
		return parseMatrix( src , matlabMatrixFromString );
	}

	/** Parse a matrix definition string in Gauss format.
	 *
	 *	@param	src		String containing matrix definition.
	 *
	 *	@return			The matrix corresponding to the definition string.
	 *
	 *	<p>
	 *	Sample matrix definition:	{ 2 -3.5 6 , 7 -8 9.0, 10 11 12}
	 *	</p>
	 */

	public static Matrix fromGaussString( String src )
	{
		return parseMatrix( src , gaussMatrixFromString );
	}

	/** Parse a matrix definition string in plain text format.
	 *
	 *	@param	src		String containing matrix definition.
	 *
	 *	@return			The matrix corresponding to the definition string.
	 *
	 *	<p>
	 *	Sample matrix definition:
	 *	</p>
	 *	<p>
	 *	2 -3.5 6<br />
	 *	7 -8 9.0<br />
	 *	10 11 12
	 *	</p>
	 */

	public static Matrix fromPlainTextString( String src )
	{
		return parseMatrix( src , plainTextMatrixFromString );
	}

	/** Parse a matrix definition string in MathML format.
	 *
	 *	@param	src		String containing matrix definition.
	 *
	 *	@return			The matrix corresponding to the definition string.
	 *
	 *	<p>
	 *	Sample matrix definition:
	 *	</p>
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

	// To use JDOM to parse MathML, comment out the following method.

	public static Matrix fromMathMLString( String src )
	{
		return parseMatrix( src , mathMLMatrixFromString );
	}

	// To use JDOM to parse MathML, uncomment the following method.

/*
	public static Matrix fromMathMLString( String src )
	{
		Matrix matrix	= null;

								// Create DOM tree of MathML elements.

		SAXBuilder builder		= new SAXBuilder();
		Document matrixDocument	= null;

		try
		{
			matrixDocument	=
				builder.build( new BufferedReader( new StringReader( src ) ) );
		}
		catch ( Exception e )
		{
			return matrix;
		}
								// Get the root element of the document tree.
								// This is the "<matrix>" element.

		Element rootElement	= matrixDocument.getRootElement();

								// The immediate children of the root
								// element are <matrixrow> elements,
								// each of which defines one row of data
								// for the matrix.

		List allRows	= rootElement.getChildren();

								// Assume the number of columns for
								// the matrix is the same as the
								// length of the first row.  We get
								// the length by counting the number
								// of "<cn></cn>" elements in the
								// first row.

		List firstRow	= ( (Element)allRows.get( 0 ) ).getChildren();

								// Allocate a matrix to hold the
								// parsed input data.
		matrix	=
			MatrixFactory.createMatrix( allRows.size() , firstRow.size() );

								// Parse each matrix data element and store
								// in the result matrix.  Start by
								// looping over the number of rows in the
								// result matrix.

		for ( int i = 1 ; i <= matrix.rows(); i++ )
		{
								// Get a list of the column elements
								// for the current row.

			List currentRow = ( (Element)allRows.get( i - 1 ) ).getChildren();

								// Loop over all columns in current row.

			for ( int j = 1 ; j <= matrix.columns() ; j++ )
			{
								// Get the element at the current
								// row and column position.

				Element current	= ( (Element)currentRow.get( j - 1  ) );

								// Convert the text of the current element
								// to a double and store in the result matrix.

				matrix.set( i , j , Double.parseDouble( current.getText() ) );
			}
		}
                                // Return the matrix data to the caller.
		return matrix;
	}
*/

	/** General recursive descent parser class for matrices whose forma
	 *	can be parsed with regular expressions.
	 */

	public static class MatrixFromStringParser
	{
		/** String which starts matrix definition. */

		protected String startMatrixString;

		/** String which ends matrix definition. */

		protected String endMatrixString;

		/** String which starts each row of matrix. */

		protected String startRowString;

		/** String which ends each row of matrix. */

		protected String endRowString;

		/** String which starts a matrix entry. */

		protected String startEntryString;

		/** String which ends a matrix entry. */

		protected String endEntryString;

		/** True to ignore linefeed characters when parsing matrix string. */

		protected boolean skipLF;

		/** Define a matrix parser.
		 *
		 *	@param	startMatrixString	String which starts matrix definition.
		 *	@param	endMatrixString		String which ends matrix definition.
		 *	@param	startRowString		String which starts each row of matrix.
		 *	@param	endRowString		String which ends each row of matrix.
		 *	@param	startEntryString	String which starts each matrix element.
		 *	@param	endEntryString		String which ends each matrix element.
		 */

		public MatrixFromStringParser
		(
			String startMatrixString ,
			String endMatrixString ,
			String startRowString ,
			String endRowString ,
			String startEntryString ,
			String endEntryString
		)
		{
			this.startMatrixString		= startMatrixString;
			this.endMatrixString		= endMatrixString;
			this.startRowString			= startRowString;
			this.endRowString			= endRowString;
			this.startEntryString		= startEntryString;
			this.endEntryString			= endEntryString;

								//	Ignore linefeeds unless a linefeed
								//	appears in the start or end row strings.

			this.skipLF					=
				( startRowString.indexOf( '\n' ) == 0 ) &&
				( endRowString.indexOf( '\n' ) == 0 );
		}

		/** Parse matrix string.
		 *
		 *	@param	matrixDefinition	The matrix definition string to parse.
		 *
		 *	@return						Matrix holding values extracted
		 *								from matrix definition string.
		 */

		protected Matrix parse( String matrixDefinition )
		{
								//	Remove unneeded whitespace from
								//	matrix definition string.

			matrixDefinition	=
				deleteRedundantWhitespace( matrixDefinition , skipLF );

								//	Get matrix entries into double array
								//	using recursive descent parser.

			double[][] values	=
				processRows
				(
					divideIntoRows
					(
						discardStartEnd
						(
							matrixDefinition ,
							startMatrixString ,
							endMatrixString
						)
					)
				);
                                //	Create matrix from retrieved values.

			return MatrixFactory.createMatrix(
				values.length , values[ 0 ].length , values );
		}

		/**	Discard matching start and end element brackets.
		 *
		 *	@param	sourceString	String from which start/end brackets
		 *							should be removed.
		 *
		 *	@param	startString		Starting string to remove.
		 *
		 *	@param	endString		Ending string to remove.
		 *
		 *	@return					The source string with starting/ending
		 *							strings removed.
		 *
		 *	<p>
		 *	Either or both of the starting/ending strings may be empty.
		 *	</p>
		 */

		protected String discardStartEnd
		(
			String sourceString ,
			String startString ,
			String endString
		)
		{
								//	Remove starting string, if any.

			if ( startString.length() > 0 )
			{
				try
				{
					sourceString	= sourceString.split( startString )[ 1 ];
				}
				catch ( Exception e )
				{
				}
			}
								//	Remove ending string, if any.

			if ( endString.length() > 0 )
			{
				try
				{
					sourceString	= sourceString.split( endString )[ 0 ];
				}
				catch ( Exception e )
				{
				}
			}
								//	Remove updated string.

			return sourceString;
		}

		/** Split string into rows.
		 *
		 *	@param	sourceString	The string to split into rows.
		 *
		 *	@return					Array of row strings.
		 *
		 *	<p>
		 *	The source string is split into rows using the startRowString
		 *	and/or endRowString values.  Both the startRowString and
		 *	endRowString are removed from each row string.
		 *	</p>
		 */

		protected String[] divideIntoRows( String sourceString )
		{
			String[] rows	= null;

								//	Split source string into rows
								//	using either the start of row string
								//	or the end of row string, whichever
								// 	is defined.

			if ( startRowString.length() > 0 )
			{
				rows	= sourceString.split( startRowString );
			}

			else if ( endRowString.length() > 0 )
			{
				rows	= sourceString.split( endRowString );
			}
								//	Remove the start/end row strings
								// 	and extraneous whitespace from each row.
								//	Also throw away rows that are empty.

			ArrayList newRows	= new ArrayList( rows.length );

			for ( int irow = 0 ; irow < rows.length ; irow++ )
			{
				rows[ irow ]	=
					discardStartEnd(
						rows[ irow ] , startRowString , endRowString );

				rows[ irow ]	=
					deleteRedundantWhitespace( rows[ irow ] , false ).trim();

				if ( rows[ irow ].length() > 0 )
				{
					newRows.add( new String( rows[ irow ] ) );
				}
			}
								//	We now have a list of non-empty rows.
								//	Move these to a string array.

			rows	= new String[ newRows.size() ];

			for ( int irow = 0 ; irow < newRows.size() ; irow++ )
			{
				rows[ irow ]	= (String)newRows.get( irow );
			}

			return rows;
		}

		/**	Process row strings.
		 *
		 *	@param	rows	String array of row strings.
		 *
		 *	@return			Array of entry values as doubles for each row.
		 */

		protected double[][] processRows( String[] rows )
		{
			double[][] values	= new double[ rows.length ][];

			for ( int row = 0 ; row < rows.length ; row++ )
			{
				values	=
					parseIndividualEntries
					(
						deleteRedundantWhitespace( rows[ row ] , false ) ,
						row ,
						values
					);
			}

			return values;
		}

		/**	Parse individual double entries from string.
		 *
		 *	@param	rowString	The string containing one row of values.
		 *	@param	row			The row number of the row string (0 based).
		 *	@param	values		The resulting double values.
		 *
		 *	@return				Double array with parsed double values.
		 */

		protected double[][] parseIndividualEntries
		(
			String rowString ,
			int row ,
			double[][] values
		)
		{
			String[] stringEntries	= null;
			String localRowStr		= rowString.trim();

								//	Split row into individual entries
								//	using startEntryString or
								//	endEntryString, whichever is defined.

			if ( startEntryString.length() > 0 )
			{
				stringEntries	= localRowStr.split( startEntryString );
            }
			else if ( endEntryString.length() > 0 )
			{
				stringEntries	= localRowStr.split( endEntryString );
			}
								//	Remove the start/end entry strings
								// 	and extraneous whitespace from each entry.
								//	Also throw away entries that are empty.

			ArrayList newEntries	= new ArrayList( stringEntries.length );

			for ( int i = 0 ; i < stringEntries.length ; i++ )
			{
				stringEntries[ i ]	=
					discardStartEnd(
						stringEntries[ i ] , startEntryString , endEntryString );

				stringEntries[ i ]	=
					deleteRedundantWhitespace(
						stringEntries[ i ] , false ).trim();

				if ( stringEntries[ i ].length() > 0 )
				{
					newEntries.add( new String( stringEntries[ i ] ) );
				}
			}
								//	We now have a list of non-empty entries.
								//	Move these to a string array.

			stringEntries	= new String[ newEntries.size() ];

			for ( int i = 0 ; i < newEntries.size() ; i++ )
			{
				stringEntries[ i ]	= (String)newEntries.get( i );
			}
								//	Allocate double array to hold parsed entries.

			values[ row ]	= new double[ stringEntries.length ];

								//	Parse individual matrix entry strings
								//	into doubles.

			for ( int element = 0 ; element < stringEntries.length ; element++ )
			{
				values[ row ][ element ]	=
					Double.parseDouble( stringEntries[ element ] );
			}

			return values;
		}

		/**	Delete redundant whitespace from a matrix definition string.
		 *
		 *	@param	inputString		The string from which to remove whitespace.
		 *	@param	skipLF			true to consider linefeed as whitespace.
		 *
		 *	@return					The string with redundant whitespace removed.
		 */

		protected static String deleteRedundantWhitespace
		(
			String inputString ,
			boolean skipLF
		)
		{
			String patternStr	= "\\s+";

			if ( !skipLF )
			{
				patternStr	= "[ \\t\\x0B\\f\\r]";
			}

			String replaceStr	= " ";

			Pattern pattern		= Pattern.compile( patternStr );
			Matcher matcher		= pattern.matcher( inputString );

			return matcher.replaceAll( replaceStr );
		}
	}

	/** Don't allow instantiation, but do allow overrides. */

	protected MatrixFromString()
	{
	}
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

