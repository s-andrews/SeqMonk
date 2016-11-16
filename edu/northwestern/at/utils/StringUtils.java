package edu.northwestern.at.utils;

/*	Please see the license information at the end of this file. */

import java.lang.reflect.*;
import java.util.*;
import java.text.*;

/**	String utilities.
 *
 *	<p>
 *	This static class provides various utility methods for manipulating
 *	strings.
 *	</p>
 */

public class StringUtils
{
	/** Find end position of balanced string.
	 *
	 *	@param		s		The string.
	 *	@param		start	Starting offset in string.
	 *	@param		lChar	Left delimiter.
	 *	@param		rChar	Right delimiter to match left delimiter.
	 *
	 *	@return		The ending offset of the matching balanced string.
	 *				-1 if not found.
	 */

	public static int balPos
	(
		StringBuffer	s,
		int				start,
		char			lChar,
		char			rChar )
	{
		int count		= 0;
		int	ls			= s.length();
		int saveStart	= start;

		while ( true )
		{
			if ( s.charAt( start ) == lChar )
			{
				if ( lChar != rChar )
				{
					count++;
				}
				else if ( start == saveStart )
				{
					count++;
				}
				else
				{
					count--;
				}
			}
			else if ( s.charAt( start ) == rChar )
			{
				count--;
			}

			if ( ( start >= ls ) || ( count == 0 ) ) break;

			start++;
		}

		if ( count == 0 )
		{
			return start;
		}
		else
		{
			return -1;
		}
	}

	/** Count occurrences of character in string buffer.
	 *
	 *	@param	sb	The string buffer.
	 *	@param	ch	The character.
	 *
	 *	@return		Number of occurrences of ch in sb.
	 */

	public static int countChar( StringBuffer sb , char ch )
	{
    	int result = 0;

		for ( int i = 0; i < sb.length(); i++ )
		{
			if ( sb.charAt( i ) == ch ) result++;
		}

		return result;
	}

	/** Count occurrences of string in another string.
	 *
	 *	@param	source			The source string.
	 *	@param	searchString	The string for which to search.
	 *
	 *	@return					Number of occurrences of searchString in source.
	 */

	public static int countOccurrences( String source , String searchString )
	{
    	int result			= 0;
		int searchLength	= searchString.length();

		for ( int i = 0 ; i < ( source.length() - searchLength ) ; i++ )
		{
			if ( source.regionMatches( i , searchString , 0 , searchLength ) ) result++;
		}

		return result;
	}

	/** Delete all occurrences of a specified character from a string.
	 *
	 *	@param	str		The string.
	 *	@param	ch		The character to delete.
	 *
	 *	@return			The string with any occurrences of "ch" removed.
	 */

	public static String deleteChar( String str , char ch )
	{
		if ( safeString( str ).length() == 0 ) return str;

		StringBuffer sb = new StringBuffer( str.length() );

		for ( int i = 0; i < str.length(); i++ )
		{
			if ( str.charAt( i ) != ch )
			{
				sb.append( str.charAt( i ) );
			}
		}

		return sb.toString();
	}

	/**	Displays a boolean field.
	 *
	 *	@param	name	The field name.
	 *
	 *	@param	value	The field value.
	 *
	 *	@return			"name = true" or "name = false".
	 */

	public static String displayBooleanField( String name , boolean value )
	{
		return name + " = " + value;
	}

	/**	Displays an integer field.
	 *
	 *	@param	name	The field name.
	 *
	 *	@param	value	The field value.
	 *
	 *	@return			"name = value".
	 */

	public static String displayIntegerField( String name , int value )
	{
		return name + " = " + value;
	}

	/**	Displays a double field.
	 *
	 *	@param	name	The field name.
	 *
	 *	@param	value	The field value.
	 *
	 *	@return			"name = value".
	 */

	public static String displayDoubleField( String name , double value )
	{
		return name + " = " + value;
	}

	/**	Displays a string field.
	 *
	 *	@param	name	The field name.
	 *
	 *	@param	value	The field value.
	 *
	 *	@return			"name = value". If the value has multiple
	 *					lines the extra lines are displayed indented
	 *					so that they line up with the fist line.
	 */

	public static String displayStringField( String name , String value )
	{
		StringBuffer buf	= new StringBuffer();

		buf.append( name + " = " );

		if ( value == null )
		{
			buf.append( "null" );
		}
		else
		{
			int indent			= name.length() + 3;
			boolean firstLine	= true;

			StringTokenizer tokenizer	= new StringTokenizer( value , "\n" );

			while ( tokenizer.hasMoreTokens() )
			{
				if ( !firstLine )
				{
					buf.append( "\n" );

					for ( int i = 0 ; i < indent ; i++ ) buf.append( " " );
				}

				firstLine	= false;

				buf.append( tokenizer.nextToken() );
			}
		}

		return buf.toString();
	}

	/** Duplicate character into string.
	 *
	 *	@param	ch		The character to be duplicated.
	 *	@param	n		The number of duplicates desired.
	 *
	 *	@return			String containing "n" copies of "ch".
	 *
	 *	<p>
	 *	if n <= 0, the empty string "" is returned.
	 *	</p>
	 */

	 public static String dupl( char ch , int n )
	 {
	 	if ( n > 0 )
	 	{
			StringBuffer result = new StringBuffer( n );

			for ( int i = 0 ; i < n ; i++ )
			{
				result.append( ch );
			}

			return result.toString();
		}
		else
		{
    		return "";
		}
	 }

	/** Duplicate string into string.
	 *
	 *	@param	s		The string to be duplicated.
	 *	@param	n		The number of duplicates desired.
	 *
	 *	@return			String containing "n" copies of "s".
	 *
	 *	<p>
	 *	if n <= 0, the empty string "" is returned.
	 *	</p>
	 */

	 public static String dupl( String s , int n )
	 {
	 	if ( n > 0 )
	 	{
			StringBuffer result = new StringBuffer( n );

			for ( int i = 0 ; i < n ; i++ )
			{
				result.append( s );
			}

			return result.toString();
		}
		else
		{
    		return "";
		}
	 }

	/**	Returns true if two case-sensitive strings are equal.
	 *
	 *	<p>Nulls are permitted and are equal only to themselves.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			True if string 1 = string 2.
	 */

	public static boolean equals( String s1 , String s2 )
	{
		if ( s1 == null )
		{
			return s2 == null;
		}
		else
		{
			return s2 == null ? false : s1.equals( s2 );
		}
	}

	/**	Returns true if two case-insensitive strings are equal.
	 *
	 *	<p>Nulls are permitted and are equal only to themselves.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			True if string 1 = string 2.
	 */

	public static boolean equalsIgnoreCase( String s1 , String s2 )
	{
		if ( s1 == null )
		{
			return s2 == null;
		}
		else
		{
			return s2 == null ? false : s1.equalsIgnoreCase( s2 );
		}
	}

	/**	Compares two case-sensitive strings.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			< 0 if string 1 < string 2, 0 if
	 *					string 1 = string 2, > 0 if string 1 > string 2.
	 */

	public static int compare( String s1 , String s2 )
	{
		if ( s1 == null )
		{
			return s2 == null ? 0 : -1;
		}
		else
		{
			return s1 == null ? +1 : s1.compareTo( s2 );
		}
	}

	/**	Compares two case-insensitive strings.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	s1		String 1.
	 *
	 *	@param	s2		String 2.
	 *
	 *	@return			< 0 if string 1 < string 2, 0 if
	 *					string 1 = string 2, > 0 if string 1 > string 2.
	 */

	public static int compareIgnoreCase( String s1 , String s2 )
	{
		if ( s1 == null )
		{
			return s2 == null ? 0 : -1;
		}
		else
		{
			return s1 == null ? +1 : s1.compareToIgnoreCase( s2 );
		}
	}

	/**	Compares two dates.
	 *
	 *	<p>Nulls are permitted and are less than non-nulls.
	 *
	 *	@param	d1		Date 1.
	 *
	 *	@param	d2		Date 2.
	 *
	 *	@return			< 0 if date 1 < date 2, 0 if
	 *					date 1 = date 2, > 0 if date 1 > date 2.
	 */

	public static int compare( Date d1 , Date d2 )
	{
		if ( d1 == null )
		{
			return d2 == null ? 0 : -1;
		}
		else
		{
			return d1 == null ? +1 : d1.compareTo( d2 );
		}
	}

	/**	Compares two ints.
	 *
	 *	@param	n1		Int 1.
	 *
	 *	@param	n2		Int 2.
	 *
	 *	@return			-1 if n1 < n2, 0 if n1 = n2, +1 if n1 > n2.
	 */

	public static int compare( int n1 , int n2 )
	{
		if ( n1 < n2 )
		{
			return -1;
		}
		else if ( n1 > n2 )
		{
			return +1;
		}
		else
		{
			return 0;
		}
	}

	/**	Compares two longs.
	 *
	 *	@param	n1		Long 1.
	 *
	 *	@param	n2		Long 2.
	 *
	 *	@return			-1 if n1 < n2, 0 if n1 = n2, +1 if n1 > n2.
	 */

	public static int compare( long n1 , long n2 )
	{
		if ( n1 < n2 )
		{
			return -1;
		}
		else if ( n1 > n2 )
		{
			return +1;
		}
		else
		{
			return 0;
		}
	}

	/**	Searches a string for a case-insensitive whole word substring match.
	 *
	 *	@param	str1		The string to be searched.
	 *
	 *	@param	str2		The substring to search for, converted to lower case.
	 *
	 *	@param	fromIndex	The index to start the search from.
	 *
	 *	@return				The index of the match, or -1 if none.
	 */

	public static int indexOfIgnoreCaseWholeWord
	(
		String str1 ,
		String str2 ,
		int fromIndex
	)
	{
		if ( str1 == null ) return -1;

		str1		= str1.toLowerCase();
		int str1len	= str1.length();
		int str2len	= str2.length();

		while ( fromIndex >= 0 )
		{
			fromIndex = str1.indexOf( str2 , fromIndex );

			if ( fromIndex < 0 ) return -1;

			if ( ( fromIndex > 0 ) &&
				Character.isLetterOrDigit( str1.charAt( fromIndex - 1 ) ) )
			{
				fromIndex++;
				continue;
			}

			int k = fromIndex + str2len;

			if ( ( k < str1len ) &&
				Character.isLetterOrDigit( str1.charAt( k ) ) )
			{
				fromIndex++;
				continue;
			}

			return fromIndex;
		}

		return -1;
	}

	/**	Returns true if a string contains another case-insensitive string as
	 *	a whole word.
	 *
	 *	@param	str1	The string to be searched.
	 *
	 *	@param	str2	The substring to search for.
	 *
	 *	@return			True if str1 contains str2 as a case-insensitive whole
	 *					word.
	 */

	public static boolean containsIgnoreCaseWholeWord
	(
		String str1,
		String str2
	)
	{
		return indexOfIgnoreCaseWholeWord( str1 , str2.toLowerCase() , 0 ) >= 0;
	}

	/**	Extracts a number from a string.
	 *
	 *	@param	str		The string.
	 *
	 *	@param	pos		The position within the string.
	 *
	 *	@return			The number.
	 *
	 *	@throws	ParseException	If invalid number.
	 */

	public static int extractNumber( String str , int pos )
		throws ParseException
	{
		int len	= str.length();
		char c	= str.charAt( pos );

		if ( !Character.isLetterOrDigit( c ) && ( pos > 0 ) ) pos--;

		int begin	= pos;

		while ( begin >= 0 )
		{
			c	= str.charAt( begin );

			if ( Character.isDigit( c ) || ( c == ',' ) )
			{
				begin--;
			}
			else
			{
				break;
			}
		}

		begin++;

		int end	= pos;

		while ( end < len )
		{
			c	= str.charAt( end );

			if ( Character.isDigit( c ) || ( c == ',' ) )
			{
				end++;
			}
			else
			{
				break;
			}
		}

		if ( begin >= end )
			throw new ParseException( "Invalid number" , 0 );

		return parseNumberWithCommas( str.substring( begin , end ) );
	}

	/** Gets the first token from an array of bytes.
	 *
	 *	@param	bytes	The bytes.
	 *	@param	nBytes	The number of bytes to scan.
	 *
	 *	@return			The first token as a string.
	 *
	 *	<p>
	 *	The first token is defined to end at a space, a tab,
	 *	a carriage return, or a linefeed.
	 *	</p>
	 */

	public static String getFirstTokenFromBytes( byte[] bytes , int nBytes )
	{
								// Get the first token from the byte data.

		StringBuffer sb = new StringBuffer();

		for ( int i = 0; i < nBytes; i++ )
		{
			int value = ( bytes[ i ] & 0xFF );

			if	(	( value == '\r' ) ||
					( value == '\n' ) ||
					( value == '\t' ) ||
					( value == ' ' ) ) break;

			sb.append( (char)value );
		}
		                        // We only case about the following
		                        // RTSP commands and responses.
		                        // The rest we let pass through unchanged.

		return sb.toString();
	}

	/**	Returns the first line of a string.
	 *
	 *	@param	str		The string.
	 *
	 *	@return			Null if str is null, else the first line of str.
	 */

	public static String firstLine( String str )
	{
		if ( str == null ) return null;

		int i	= str.indexOf( '\n' );

		return ( i < 0 ) ? str : str.substring( 0 , i );
	}

	/**	Number formatter for numbers with commas. */

	static private final NumberFormat commaFormatter;

	static
	{
		commaFormatter	= NumberFormat.getInstance();
		commaFormatter.setGroupingUsed( true );
	}

	/**	Formats a number with commas.
	 *
	 *	@param	n		The number (int).
	 *
	 *	@return			The formatted number with commas.
	 */

	public static String formatNumberWithCommas( int n )
	{
		return commaFormatter.format(n);
	}

	/**	Formats a number with commas.
	 *
	 *	@param	n		The number (long).
	 *
	 *	@return			The formatted number with commas.
	 */

	public static String formatNumberWithCommas( long n )
	{
		return commaFormatter.format( n );
	}

	/**	Parses a number with commas.
	 *
	 *	@param	str		The string.
	 *
	 *	@return			The parsed number.
	 *
	 *	@throws	ParseException	Invalid number.
	 */

	public static int parseNumberWithCommas( String str )
		throws ParseException
	{
		return commaFormatter.parse( str ).intValue();
	}

	/**	Makes possibly null string safe for comparisons.
	 *
	 *	@param		s	Input string which may be null.
	 *
	 *	@return			The input string if not null,
	 *					other an empty string.
	 */

	public static String safeString( String s )
	{
		String string = s;

		if ( string == null )
		{
			string = "";
		}

		return string;
	}

	/**	Replaces all substrings of a string.
	 *
	 *	<p>Note: Not needed in Java 1.4 - can use replaceAll method of
	 *	String class instead.
	 *
	 *	@param	str		The string.
	 *
	 *	@param	s		Substring to be replaced.
	 *
	 *	@param	r		Replacement string.
	 *
	 *	@return			The string with all substrings replaced.
	 */

	public static String replaceAll( String str , String s , String r )
	{
		StringBuffer result	= new StringBuffer();

		int sLen			= s.length();
		int strLen			= str.length();
		char strCharArray[]	= str.toCharArray();
		int pos				= 0;

		while ( true )
		{
			int match	= str.indexOf( s , pos );

			if ( match >= 0 )
			{
				result.append( strCharArray , pos , match - pos );
				result.append( r );
				pos	 = match + sLen;
			}
			else
			{
				result.append( strCharArray , pos , strLen - pos );
				break;
			}
		}

		return result.toString();
	}

	/**	Checks if a string is null or empty.
	 *
	 *	@param	s	String to be checked.
	 *
	 *	@return		True if string is null or empty.
	 */

	public static boolean checkEmpty( String s )
	{
		return ( ( s == null ) || ( s == "" ) );
	}

	/**	Returns "yes" or "no" for boolean value.
	 *
	 *	@param	yesno	Boolean value to be checked.
	 *
	 *	@return			"yes" if yesno is true, else "no"
	 */

	public static String yesNo( boolean yesno )
	{
		String result;

		if ( yesno )
		{
			result = "yes";
		}
		else
		{
			result = "no";
		}

		return result;
	}

	/** Return "are", "are not" based upon boolean value.
	 *
	 *	@param	bool	Boolean value.
	 *
	 *	@return			"are" if bool is true, "are not" if false.
	 */

	public static String areOrAreNot( boolean bool )
	{
		return bool ? "are" : "are not";
	}

	/** Return "is", "is not" based upon boolean value.
	 *
	 *	@param	bool	Boolean value.
	 *
	 *	@return			"is" if bool is true, "is not" if false.
	 */

	public static String isOrIsNot( boolean bool )
	{
		return bool ? "is" : "is not";
	}

	/** Return "have", "have not" based upon boolean value.
	 *
	 *	@param	bool	Boolean value.
	 *
	 *	@return			"have" if bool is true, "have not" if false.
	 */

	public static String haveOrHaveNot( boolean bool )
	{
		return bool ? "have" : "have not";
	}

	/** Convert string to integer.
	 *
	 *	@param	strValue			The string to convert.
	 *	@param	defaultValue		Default value if conversion error occurs.
	 *
	 *	@return						The string converted to an integer.
	 */

	public static int stringToInt( String strValue , int defaultValue )
	{
		String str = safeString( trim( strValue ) );

		if ( str.length() == 0 )
		{
			return 0;
		}
		else
		{
			int result = defaultValue;

			try
			{
				result = Integer.parseInt( strValue );
			}
			catch ( NumberFormatException e )
			{
			}

			return result;
		}
	}

	/** Convert characters in a string to hex format.
	 *
	 *	@param	s	The string to convert to hex.
	 *
	 *	@return		The hex version of the string.
	 */

	public static String stringToHexString( String s )
	{
		String result = "";

		String safeS = safeString( s );

		if ( s.length() > 0 )
		{
			for ( int i = 0; i < safeS.length(); i++ )
			{
				if ( i > 0 )
				{
					result += ",";
				}

				result += ( "0x" + Integer.toString( safeS.charAt( i ) , 16 ) );
			}
		}

		return result;
	}

	/** Break up a string into an array of string tokens.
	 *
	 * 	@param		source	The source string.
	 * 	@param		delim	The delimiter.
	 *
	 *	@return		The array of tokens extracted from the source.
	 */

	public static String[] makeTokenArray( String source , String delim )
	{
		String[] result;

		if ( delim.equals( " " ) )
		{
			result	= makeTokenArray( source );
		}
		else
		{
			result = source.split( delim );

			for ( int i = 0; i < result.length; i++ )
			{
				result[ i ] = trim( result[ i ] );
			}
		}

		return result;
	}

	/** Break up a string into an array of string tokens.
	 *
	 * 	@param		source	The source string.
	 *
	 *	@return				The array of tokens extracted from the source.
	 */

	public static String [] makeTokenArray( String source )
	{
		ArrayList tokenArrayList = new ArrayList();

		StringTokenizer st = new StringTokenizer( source );

		while ( st.hasMoreTokens() )
		{
			tokenArrayList.add( st.nextToken() );
		}

		int numTokens = tokenArrayList.size();

		String[] result = new String[ numTokens ];

		for ( int i = 0; i < numTokens; i++ )
		{
			result[ i ] = (String)tokenArrayList.get( i );
		}

		return result;
	}

	/** Convert object to string representation.
	 *
	 *	@param	object	The object to be converted to a string representation.
	 *
	 *	@return			The string representation.  This looks like [a, b, c ... ]
	 *					for an array.  If the object is not an array, its
	 *					toString() method is used instead.  Null objects are
	 *					returned as "<null>".
	 *
	 *	<p>
	 *	The regular java toString() routine for arrays is brain-dead:  it returns
	 *	the hashcode/address of the array rather than a useful representation of
	 *	the array contents.
	 *	</p>
	 *
	 */

	 public static String objectToString( Object object )
	 {
	 	String result = "";

	 	try
	 	{
		 	if ( object == null )
		 	{
				result = "<null>";
		 	}

			else if ( object.getClass().isArray() )
			{
				ArrayList list = new ArrayList();

				int arrayLength = Array.getLength( object );

				for ( int j = 0; j < arrayLength; j++ )
				{
					try
					{
						Object arrayValue = Array.get( object , j );
						list.add( arrayValue );
					}
					catch ( Exception e )
					{
						break;
					}
				}

				result = list.toString();
			}

			else
			{
				result = object.toString();
			}
		}
		catch ( Exception e )
		{
		}

    	return result;
	}

	/** Select singular or plural string based upon count.
	 *
	 *	@param	count	The number of items.
	 *	@param	singular	The output string iif count==1 .
	 *	@param	plural		The output string iif count!=1 .
	 *
	 *	@return		Either singular or plural, based upon value
	 *				of count.
	 *
	 */

	public static String pluralize( int count, String singular, String plural )
	{
		return ( count == 1 ) ? singular : plural;
	}

	/** Convert integer to string.
	 *
	 *	@param	intValue	The integer to convert.
	 *
	 *	@return				The integer converted to a string.
	 *
	 */

	public static String intToString( int intValue )
	{
		return new Integer( intValue ).toString();
	}

	/** Convert integer to string with left zero fill.
	 *
	 *	@param	intValue	The integer to convert.
	 *	@param	width		Width of result field.
	 *
	 *	@return				The integer converted to a string
	 *						with enough leading zeros to fill
	 *						the specified field width.
	 */

	public static String intToStringWithZeroFill( int intValue , int width )
	{
		String s = new Integer( intValue ).toString();

		if ( s.length() < width )
			s = dupl( '0' , width - s.length() ) + s;

		return s;
	}

	/** Convert long to string.
	 *
	 *	@param	longValue	The long to convert.
	 *
	 *	@return				The long converted to a string.
	 *
	 */

	public static String longToString( long longValue )
	{
		return new Long( longValue ).toString();
	}

	/** Convert long to string with left zero fill.
	 *
	 *	@param	longValue	The long to convert.
	 *	@param	width		Width of result field.
	 *
	 *	@return				The long converted to a string
	 *						with enough leading zeros to fill
	 *						the specified field width.
	 */

	public static String longToStringWithZeroFill( long longValue , int width )
	{
		String s = new Long( longValue ).toString();

		if ( s.length() < width )
			s = dupl( '0' , width - s.length() ) + s;

		return s;
	}

	/**	Left pad string with blanks to specified width.
	 *
	 *	@param	s		The string to pad.
	 *	@param	width	The width to pad to.
	 *
	 *	@return			"s" padded with enough blanks on left
	 *					to be "width" columns wide.
	 */

	public static String lpad( String s , int width )
	{
		if ( s.length() < width )
		{
			return dupl( ' ' , width - s.length() ) + s;
		}
		else
		{
			return s;
		}
	}

	/**	Right pad string with blanks to specified width.
	 *
	 *	@param	s		The string to pad.
	 *	@param	width	The width to pad to.
	 *
	 *	@return			"s" padded with enough blanks on right
	 *					to be "width" columns wide.
	 */

	public static String rpad( String s , int width )
	{
		if ( s.length() < width )
		{
			return s + dupl( ' ' , width - s.length() );
		}
		else
		{
			return s;
		}
	}

	/** Convert string to integer.
	 *
	 *	@param	strValue	The string to convert.
	 *
	 *	@return				The string converted to an integer.
	 *
	 *	@throws				NumberFormatException
	 */

	public static int stringToInt( String strValue )
		throws NumberFormatException
	{
		String str = safeString( trim( strValue ) );

		if ( str.length() == 0 )
		{
			return 0;
		}
		else
		{
			return Integer.parseInt( strValue );
		}
	}

	/** Convert string to long.
	 *
	 *	@param	strValue	The string to convert.
	 *
	 *	@return				The string converted to a long.
	 *
	 *	@throws				NumberFormatException
	 */

	public static long stringToLong( String strValue )
		throws NumberFormatException
	{
		String str = trim( safeString( strValue ) );

		if ( str.length() == 0 )
		{
			return 0;
		}
		else
		{
			return Long.parseLong( strValue );
		}
	}

	/** Convert string to long.
	 *
	 *	@param	strValue	The string to convert.
	 *
	 *	@param	defValue	Default value to return if string cannot be converted.
	 *
	 *	@return				The string converted to a long.
	 */

	public static long stringToLong( String strValue , long defValue )
	{
		long result;

		try
		{
			result = Long.parseLong( strValue );
		}
		catch ( NumberFormatException e )
		{
			result = defValue;
		}

		return result;
	}

    /** Wrap one line of text.
     *
     * @param line       A line which is in need of word-wrapping.
     *
     * @param newline    The characters that define a newline.
     *
     * @param wrapColumn The column to wrap the words at.
     *
     * @return           A line with newlines inserted.
     */

    public static String wrapLine
	(	String line,
		String newline,
		int wrapColumn
	)
	{
        StringBuffer wrappedLine = new StringBuffer();

		while ( line.length() > wrapColumn )
		{
			int spaceToWrapAt = line.lastIndexOf( ' ' , wrapColumn );

            if ( spaceToWrapAt >= 0 )
			{
				wrappedLine.append( line.substring( 0 , spaceToWrapAt ) );
				wrappedLine.append( newline );

				line = line.substring( spaceToWrapAt + 1 );
			}

            // This must be a really long word or URL. Pass it
            // through unchanged even though it's longer than the
            // wrapColumn would allow. This behavior could be
            // dependent on a parameter for those situations when
            // someone wants long words broken at line length.

			else
			{
				spaceToWrapAt = line.indexOf( ' ' , wrapColumn );

				if ( spaceToWrapAt >= 0 )
				{
					wrappedLine.append( line.substring( 0 , spaceToWrapAt ) );
					wrappedLine.append( newline );

					line = line.substring(spaceToWrapAt + 1 );
				}
				else
				{
					wrappedLine.append( line );
					line = "";
				}
			}
		}
									// Remaining text in line is shorter than wrap column.

		wrappedLine.append( line );

		return ( wrappedLine.toString() );
	}

    /** Lines wraps a block of text.
     *
     * @param inString   Text which is in need of word-wrapping.
     *
     * @param newline    The characters that define a newline.
     *
     * @param wrapColumn The column to wrap the words at.
     *
     * @return           The text with all the long lines word-wrapped.
     *
     * <p>
     * This method wraps long lines based on the supplied wrapColumn parameter.
     * Note:  Remove or expand tabs before calling this method.
     * </p>
     */

	public static String wrapText
	(
		String inString,
		String newline,
		int wrapColumn
	)
    {
		StringTokenizer lineTokenizer =
			new StringTokenizer( inString, newline, true );

		StringBuffer stringBuffer = new StringBuffer();

		while ( lineTokenizer.hasMoreTokens() )
		{
			try
			{
				String nextLine = lineTokenizer.nextToken();

				if ( nextLine.length() > wrapColumn )
				{
									// Line is long enough to be wrapped.

					nextLine = wrapLine( nextLine, newline, wrapColumn );
				}

				stringBuffer.append( nextLine );
			}
			catch ( NoSuchElementException e )
			{
				break;
			}
		}

		return ( stringBuffer.toString() );
	}

	/**	Trims a string.
	 *
	 *	@param	s		The string.
	 *
	 *	@return			The trimmed string. Leading and trailing white
	 *					space characters are removed.  If s is null,
	 *					null is returned.
	 */

	public static String trim( String s )
	{
		if ( s == null ) return null;

		s = s.trim();

		return s;
	}

	/** Pad string with leading zeros.
	 *
	 *	@param	s		String to pad.
	 *	@param	length	Length to pad to.
	 *
	 *	@return			Input string left-padded with '0' characters
	 *					to specified length.
	 */

	public static String zeroPad( String s , int length )
	{
		for ( int i = s.length(); i < length; i++ )
		{
			s = "0" + s;
		}

		return s;
	}

	/**	Intersperse text lines from two files.
	 *
	 *	@param	s1				First string containing text lines.
	 *	@param	s2				Second string containing text lines.
	 *	@param	s1Prefix		Prefix for first string lines.
	 *	@param	s1Suffix		Suffix for first string lines.
	 *	@param	s2Prefix		Prefix for second string lines.
	 *	@param	s2Suffix		Suffix for second string lines.
	 *
	 *	@return		String with lines from s1 and s2
	 *				interspersed.
	 */

	public static String intersperseTextLines
	(
		String s1 ,
		String s2 ,
		String s1Prefix ,
		String s1Suffix ,
		String s2Prefix ,
		String s2Suffix
	)
	{
		String[] s1Lines	= s1.split( "\n" );
		String[] s2Lines	= s2.split( "\n" );

		StringBuffer sb		= new StringBuffer();

		int l1				= s1Lines.length;
		int l2				= s2Lines.length;

		int lMax			= Math.max( l1 , l2 );

		for ( int i = 0 ; i < lMax ; i++ )
		{
			if ( i < l1 )
			{
				if ( s1Prefix != null ) sb	= sb.append( s1Prefix );
				sb	= sb.append( s1Lines[ i ] );
				if ( s1Suffix != null ) sb	= sb.append( s1Suffix );
			}

			sb	= sb.append( "\n" );

			if ( i < l2 )
			{
				if ( s2Prefix != null ) sb	= sb.append( s2Prefix );
				sb	= sb.append( s2Lines[ i ] );
				if ( s2Suffix != null ) sb	= sb.append( s2Suffix );
			}

			sb	= sb.append( "\n" );
		}

		return sb.toString();
	}

	/**	Deletes parenthesized text from a string.
	 *
	 *	@param	s	The string from which to remove parenthesized text.
	 *
	 *	@return		String with parenthesized text removed.
	 *
	 *	<p>
	 *	Example:
	 *	<p>
	 *
	 *	<p>
	 *	deleteParenthesizedText( "aaaa (bb) ccc (ddd) e" )
	 *	</p>
	 *
	 *	<p>
	 *	yields
	 *	</p>
	 *
	 *	<p>
	 *	"aaaa  ccc  e"
	 *	</p>
	 */

	public static String deleteParenthesizedText( String s )
	{
		StringBuffer sb			= new StringBuffer( s );
		StringBuffer sbResult	= new StringBuffer();

		int parenDepth			= 0;

		for ( int i = 0 ; i < sb.length() ; i++ )
		{
			if ( sb.charAt( i ) == '(' )
			{
				parenDepth++;
			}
			else if ( sb.charAt( i ) == ')' )
			{
				parenDepth--;
				if ( parenDepth < 0 ) parenDepth = 0;
			}
			else if ( parenDepth == 0 )
			{
				sbResult	= sbResult.append( sb.charAt( i ) );
			}
		}

		return sbResult.toString();
	}

	/**	Deletes unparenthesized text from a string.
	 *
	 *	@param	s	The string from which to remove unparenthesized text.
	 *
	 *	@return		String with unparenthesized text removed.
	 *				Left parentheses are also removed, and right
	 *				parentheses replaced by a blank.
	 *
	 *	<p>
	 *	Example:
	 *	<p>
	 *
	 *	<p>
	 *	deleteUnparenthesizedText( "aaaa (bb) ccc (ddd) e" )
	 *	</p>
	 *
	 *	<p>
	 *	yields
	 *	</p>
	 *
	 *	<p>
	 *	"bb ddd"
	 *	</p>
	 */

	public static String deleteUnparenthesizedText( String s )
	{
		StringBuffer sb			= new StringBuffer( s );
		StringBuffer sbResult	= new StringBuffer();

		int parenDepth			= 0;

		for ( int i = 0 ; i < sb.length() ; i++ )
		{
			if ( sb.charAt( i ) == '(' )
			{
				parenDepth++;
			}
			else if ( sb.charAt( i ) == ')' )
			{
				parenDepth--;
				if ( parenDepth < 0 ) parenDepth = 0;
				sbResult.append( ' ' );
			}
			else if ( parenDepth > 0 )
			{
				sbResult	= sbResult.append( sb.charAt( i ) );
			}
		}

		return sbResult.toString().trim();
	}

	/**	Compress multiple instances of a character in a string.
	 *
	 *	@param	s	The string in which to compress multiple occurrences.
	 *	@param	c	Character whose multiple occurences should be compressed.
	 *
	 *	@return		String with multiple occurrences compressed.
	 *
	 *	<p>
	 *	Example:
	 *	<p>
	 *
	 *	<p>
	 *	compressMultipleOccurrences( "a     b   c" )
	 *	</p>
	 *
	 *	<p>
	 *	yields
	 *	</p>
	 *
	 *	<p>
	 *	"a b c"
	 *	</p>
	 */

	public static String compressMultipleOccurrences( String s , char c )
	{
		String cs		= c + "";
		String cscs		= cs + cs;
		String result	= replaceAll( s , cscs , cs );

        while ( result.indexOf( cscs ) > 0 )
		{
			result	= replaceAll( result , cscs , cs );
		}

		return result;
	}

	/**	See if string is a regular expression.
	 *
	 *	@param	s	The string.
	 *
	 *	@return		true if the string appears to be a regular expression.
	 *
	 *	<p>
	 *	A string is assumed to be a regular expression if it contains
	 *	any of the followint characters:
	 *	</p>
	 *
	 *	<pre>
	 *	* + - [ ] . ^ & \ $ ? { } ? =
	 *	</pre>
	 */

	public static boolean isRegularExpression( String s )
	{
		final char[] regExpChars	=
			new char[]
			{
				'*', '+', '-', '[', ']', '.', '^', '&', '\\', '$',
				'?', '{', '}', '='
			};

		final String regExpCharsString	= "*+-[].^&\\$?{}=";

		boolean result	= false;

		if ( ( s != null ) && ( s.length() > 0 ) )
		{
/*
			for ( int i = 0 ; i < regExpChars.length ; i++ )
			{
				result	= ( s.indexOf( regExpChars[ i ] ) >= 0 );

				if ( result && ( i > 0 ) )
				{
					result	= ( s.charAt( i - 1 ) != '\' );
				}

				if ( result ) break;
			}
*/
			for ( int i = 0 ; i < s.length() ; i++ )
			{
				result	=
					( regExpCharsString.indexOf( s.charAt( i ) ) >= 0 );

				if ( result && ( i > 0 ) )
				{
					result	= ( s.charAt( i - 1 ) != '\\' );
				}

				if ( result ) break;
			}
		}

		return result;
	}

	/**	Truncates a string and appends an ellipsis.
	 *
	 *	@param	str		String.
	 *
	 *	@param	n		Number of characters to retain.
	 *
	 *	@return			String truncated to n characters if necessary, with an
	 *					ellipsis replacing the truncated characters.
	 */

	 public static String truncate (String str, int n) {
		return str.length() < n ? str : (str.substring(0,n) + "...");
	}

	/**	Converts the first character of a string to upper case.
	 *
	 *	@param	str		String.
	 *
	 *	@return			String with first character mapped to upper case.
	 */

	public static String upperCaseFirstChar (String str) {
		if (str == null || str.length() == 0) return str;
		char c = str.charAt(0);
		return Character.toUpperCase(c) + str.substring(1);
	}

	/**	Remove enclosing brackets from a string.
	 *
	 *	@param	str	String from which to remove enclosing brackets [].
	 *
	 *	@return		str with enclosing brackets removed.
	 *
	 *	<p>
	 *	Only one level of brackets is remove.  Brackets must be paired
	 *	to be removed.
	 *	</p>
	 *
	 *	<p>
	 *	Examples:
	 *	</p>
	 *
	 *	<ul>
	 *		<li>[s] -> s</li>
	 *		<li>[s  -> [s</li>
	 *		<li>s]	-> s]</li>
	 *		<li>[[s]]	-> [s]</li>
	 *	</ul>
	 */

	public static String removeEnclosingBrackets( String str )
	{
		if ( ( str == null ) || ( str.length() < 2 ) ) return str;

		if ( str.startsWith( "[" ) && str.endsWith( "]" ) )
		{
			return str.substring( 1 , str.length() - 1 );
		}
		else
		{
			return  str;
		}
	}

	/**	Escape special characters.
	 *
	 *	@param	s	String with possible special chracters to escape.
	 *
	 *	@return		Return string with special characters escaped.
	 *				This allows the string to be displayed, for example.
	 *				If the input string is null, so is the output string.
	 *
	 *	<p>
	 *	The following characters are replaced by their "\"-escaped
	 *	versions: double quote ", single quote ', new line \n,
	 *	carriage return \r, tab \t, and backslash \.
	 *	</p>
	 */

	public static String escapeSpecialCharacters( String s )
	{
		String result	= s;

		if ( s != null )
		{
			StringBuffer sb	= new StringBuffer();

			for ( int i = 0 ; i < s.length() ; i++ )
			{
				char ch	= s.charAt( i );

				switch ( ch )
				{
					case '\"':
					{
						sb.append( "\\\"" );
					}
					break;

					case '\'':
					{
						sb.append( "\\\'" );
					}
					break;

					case '\\':
					{
						sb.append( "\\\\" );
					}
					break;

					case '\n':
					{
						sb.append( "\\n" );
					}
					break;

					case '\r':
					{
						sb.append( "\\r" );
					}
					break;

					case '\t':
					{
						sb.append( "\\t" );
					}
					break;

					default:
					{
						sb.append( ch );
					}
				}
			}

			result	= sb.toString();
		}

		return result;
	}

	/** Don't allow instantiation, do allow overrides. */

	protected StringUtils()
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

