package edu.northwestern.at.utils;

/*	Please see the license information at the end of this file. */

import java.io.*;

/**	File utilities.
 *
 *	<p>
 *	This static class provides various utility methods for manipulating
 *	files.
 *	</p>
 */

public class FileUtils
{
	/**	Read text file to a string.
	 *
	 *	@param	file		Text file to read from.
	 *	@param	encoding	Text file encoding, e.g., "utf-8".
	 *
	 *	@return				Contents of file as a string.
	 *
	 *	@throws	IOException
	 *						if file cannot be read.
	 */

	public static String readTextFile
	(
		java.io.File file ,
		String encoding
	)
		throws IOException
	{
		StringBuffer contents	=
			new StringBuffer( (int)file.length() );

		BufferedReader bufferedReader;

		String safeEncoding	= ( encoding == null ) ? "" : encoding;

		if ( safeEncoding.length() > 0 )
		{
			bufferedReader	=
				new BufferedReader(
					new InputStreamReader(
						new FileInputStream( file ) , safeEncoding ) );
		}
		else
		{
			bufferedReader	=
				new BufferedReader(
					new InputStreamReader( new FileInputStream( file ) ) );
		}

		boolean eof						= false;

		String contentLine				= "";
		String eolChar					= "\n";

		while ( !eof )
		{
			contentLine	= bufferedReader.readLine();

			if ( contentLine == null )
			{
				eof = true;
			}
			else
			{
				contents.append( contentLine );
				contents.append( eolChar );
			}
		}

		bufferedReader.close();

		return contents.toString();
	}

	/**	Read text file to a string.
	 *
	 *	@param	file		Text file to read from.
	 *
	 *	@return				Contents of file as a string.
	 *
	 *	@throws	IOException
	 *						if file cannot be read.
	 */

	public static String readTextFile( java.io.File file )
		throws IOException
	{
		return readTextFile( file , null );
	}

	/**	Read text file to a string.
	 *
	 *	@param	fileName	Text file name to read from.
	 *	@param	encoding	Text file encoding, e.g., "utf-8".
	 *
	 *	@return				Contents of file as a string.
	 *
	 *	@throws	IOException
	 *						if file cannot be read.
	 */

	public static String readTextFile( String fileName , String encoding )
		throws IOException
	{
		return readTextFile( new java.io.File( fileName ) , encoding );
	}

	/**	Read text file to a string.
	 *
	 *	@param	fileName	Text file name to read from.
	 *
	 *	@return				Contents of file as a string.
	 *
	 *	@throws	IOException
	 *						if file cannot be read.
	 */

	public static String readTextFile( String fileName )
		throws IOException
	{
		return readTextFile( new java.io.File( fileName ) );
	}

	/**	Write text file from a string.
	 *
	 *	@param	file		Text file to write to.
	 *	@param	contents	String to write to file.
	 *	@param	append		True to append contents to existing file.
	 *	@param	encoding	Text file encoding, e.g., "utf-8".
	 *
	 *	@throws	IOException
	 *		in case of I/O exception.
	 */

	public static void writeTextFile
	(
		File file ,
		boolean append ,
		String contents ,
		String encoding
	)
		throws IOException
	{
		String safeEncoding	= ( encoding == null ) ? "" : encoding;
/*
		FileWriter fileWriter			= new FileWriter( file , append );
		BufferedWriter bufferedWriter	= new BufferedWriter( fileWriter );

		bufferedWriter.write( contents );
		bufferedWriter.flush();
		bufferedWriter.close();
*/
		FileOutputStream outputStream			=
			new FileOutputStream( file , append );

		BufferedOutputStream bufferedStream	=
			new BufferedOutputStream( outputStream );

		OutputStreamWriter writer	= null;

		if ( safeEncoding.length() > 0 )
		{
			writer	= new OutputStreamWriter( bufferedStream , safeEncoding );
		}
		else
		{
			writer	= new OutputStreamWriter( bufferedStream );
		}

		writer.write( contents );
		writer.flush();
		writer.close();
	}

	/**	Write text file from a string.
	 *
	 *	@param	file		Text file to write to.
	 *	@param	contents	String to write to file.
	 *	@param	append		True to append contents to existing file.
	 *
	 *	@throws	IOException
	 *		in case of I/O exception.
	 */

	public static void writeTextFile
	(
		File file ,
		boolean append ,
		String contents
	)
		throws IOException
	{
		writeTextFile( file , append , contents , "" );
	}

	/**	Write text file from a string.
	 *
	 *	@param	fileName	Text file name to write to.
	 *	@param	contents	String to write to file.
	 *	@param	append		True to append contents to existing file.
	 *
	 *	@throws	IOException
	 *		in case of I/O exception.
	 */

	public static void writeTextFile
	(
		String fileName ,
		boolean append ,
		String contents ,
		String encoding
	)
		throws IOException
	{
		writeTextFile(
			new java.io.File( fileName ) , append , contents , encoding );
	}

	/**	Write text file from a string.
	 *
	 *	@param	fileName	Text file name to write to.
	 *	@param	contents	String to write to file.
	 *	@param	append		True to append contents to existing file.
	 *
	 *	@throws	IOException
	 *		in case of I/O exception.
	 */

	public static void writeTextFile
	(
		String fileName ,
		boolean append ,
		String contents
	)
		throws IOException
	{
		writeTextFile( new java.io.File( fileName ) , append , contents , "" );
	}

	/**	Copy a file.
	 *
	 *	@param	srcFileName		Name of source file to copy.
	 *	@param	destFileName	Name of destination file to which to copy.
	 *
	 *	@return					true if copy successful.
	 */

	public static boolean copyFile
	(
		String srcFileName ,
		String destFileName
	)
	{
								//	Assume copy fails.

		boolean result	= false;

								//	Copy buffer.

		byte[] buffer	= new byte[ 128000 ];

		try
		{
								//	See if the source file exists.

			File srcFile		= new File( srcFileName );

			if ( !srcFile.exists() )
			{
				throw new IOException(
					"File " + srcFileName + " not found." );
			}
								//	See if the source is a directory.
								//	Error if so, as we only copy files.

			if ( srcFile.isDirectory() )
			{
				throw new IOException(
					"File " + srcFileName + " is a directory." );
			}
								//	Get source file modification time.

			long srcFileTime	= srcFile.lastModified();

								//	Make sure we can write to the
								//	destination file.  Create any
								//	needed subdirectories along the way.

			if ( !createPathForFile( destFileName ) )
			{
				throw new IOException(
					"Cannot create path for " + destFileName );
			}
								//	Open input stream for source.

			InputStream inputStream		=
				new FileInputStream( srcFile );

								//	Open output stream for destination.

			OutputStream outputStream	=
				new FileOutputStream( destFileName );

								//	Copy source characters to
								//	destination in buffer size chunks.

			int nread	= 0;

			do
			{
				nread	=
					inputStream.read
					(
						buffer ,
						0 ,
						buffer.length
					);

				if ( nread > 0 )
				{
					outputStream.write( buffer , 0 , nread );
				}
			}
			while ( nread >= 0 );

								//	Close source and destination files.

			inputStream.close();

			outputStream.flush();
			outputStream.close();

								//	Set modification time of destination
								//	file to match that of source file.

			File destFile		= new File( destFileName );

	    	result				=
	    		destFile.setLastModified( srcFileTime );

			result				=
				result && ( destFile.lastModified() == srcFileTime );
		}
		catch ( IOException e )
		{
//			System.out.println(
//				"*** error in copying file: " +e.getMessage() );
		}

		return result;
	}

	/**	Create intermediate directories for a directory path.
	 *
	 *	@param	directory	The subdirectory for which to create any
	 *						missing intermediate directories.
	 *
	 *	@return				true if all the intermediate directories
	 *						were created successfully, or the directory
	 *						already exists.
	 */

	public static boolean createPath( File directory )
	{
							//	Assume the directory does not exist.

		boolean result	= false;

							//	Do nothing id directory is null.

		if ( directory != null )
		{
							//	See if directory is a directory.
							//	If it is not, it could be either
							//	because it is a file, or it does
							//	not exist.

			result	= directory.isDirectory();

			if ( !result )
			{
							//	If the directory is a file,
							//	delete the file first.

				if ( directory.exists() ) directory.delete();

							//	Create all the intermediate
							//	directories.

				result	= directory.mkdirs();
			}
		}

		return result;
	}

	/**	Create directory for specified file name.
	 *
	 *	@param	fileName	File name for which to create
	 *						parent directory, if necessary.
	 *
	 *	@return				True if directory created successfully.
	 */

	public static boolean createPathForFile( String fileName )
	{
								//	Assume directory creation fails.

		boolean result	= false;

								//	Get the file name.

		File file		= new File( fileName );

								//	Get the parent path for the
								//	file name, and try to create it
								//	if necessary.
		if ( file != null )
		{
			result	=
				createPath(
					new File( file.getAbsoluteFile().getParent() ) );
		}

		return result;
	}

	/**	Get the current directory.
	 *
	 *	@return		The current directory.
	 */

	public static String getCurrentDirectory()
	{
		return System.getProperty( "user.dir" );
	}

	/**	Change the current directory.
	 *
	 *	@param	directory	New directory to which to move.
	 *						Ignored if null.
	 *
	 *	@return				Previous directory.
	 */

	public static String chdir( String directory )
	{
		String currentDirectory	= getCurrentDirectory();

		if ( directory != null )
		{
			System.setProperty( "user.dir" , directory );
		}

		return currentDirectory;
	}

	/** Don't allow instantiation but do allow overrides. */

	protected FileUtils()
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

