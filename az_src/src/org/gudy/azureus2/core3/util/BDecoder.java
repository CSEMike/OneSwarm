/*
 * BeDecoder.java
 *
 * Created on May 30, 2003, 2:44 PM
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.core3.util;

import java.util.*;
import java.io.*;
import java.nio.*;

/**
 * A set of utility methods to decode a bencoded array of byte into a Map.
 * integer are represented as Long, String as byte[], dictionnaries as Map, and list as List.
 * 
 * @author TdC_VgA
 *
 */
public class BDecoder 
{
	private boolean recovery_mode;
	
	public static Map
	decode(
		byte[]	data )

		throws IOException
	{
		return( new BDecoder().decodeByteArray( data ));
	}

	public static Map
	decode(
		byte[]	data,
		int		offset,
		int		length )

		throws IOException
	{
		return( new BDecoder().decodeByteArray( data, offset, length ));
	}

	public static Map
	decode(
		BufferedInputStream	is  )

		throws IOException
	{
		return( new BDecoder().decodeStream( is ));
	}


	public 
	BDecoder() 
	{	
	}

	public Map 
	decodeByteArray(
		byte[] data) 

		throws IOException 
	{ 
		return( decode(new BDecoderInputStreamArray(data)));
	}

	public Map 
	decodeByteArray(
		byte[] 	data,
		int		offset,
		int		length )

		throws IOException 
	{ 
		return( decode(new BDecoderInputStreamArray(data, offset, length )));
	}
	
	public Map 
	decodeStream(
		BufferedInputStream data )  

		throws IOException 
	{
		Object	res = decodeInputStream(new BDecoderInputStreamStream(data), 0);

		if ( res == null ){

			throw( new BEncodingException( "BDecoder: zero length file" ));

		}else if ( !(res instanceof Map )){

			throw( new BEncodingException( "BDecoder: top level isn't a Map" ));
		}

		return((Map)res );
	}

	private Map 
	decode(
		BDecoderInputStream data ) 

		throws IOException 
	{
		Object res = decodeInputStream(data, 0);

		if ( res == null ){

			throw( new BEncodingException( "BDecoder: zero length file" ));

		}else if ( !(res instanceof Map )){

			throw( new BEncodingException( "BDecoder: top level isn't a Map" ));
		}

		return((Map)res );
	}

	private Object 
	decodeInputStream(
		BDecoderInputStream dbis,
		int			nesting ) 

		throws IOException 
	{
		if (nesting == 0 && !dbis.markSupported()) {

			throw new IOException("InputStream must support the mark() method");
		}

			//set a mark
		
		dbis.mark(Integer.MAX_VALUE);

			//read a byte
		
		int tempByte = dbis.read();

			//decide what to do
		
		switch (tempByte) {
		case 'd' :
				//create a new dictionary object
			
			Map tempMap = new LightHashMap();

			try{
					//get the key   
				
				byte[] tempByteArray = null;

				while ((tempByteArray = (byte[]) decodeInputStream(dbis, nesting+1)) != null) {

						//decode some more

					Object value = decodeInputStream(dbis,nesting+1);
					
						// value interning is too CPU-intensive, let's skip that for now
						//if(value instanceof byte[] && ((byte[])value).length < 17)
						//value = StringInterner.internBytes((byte[])value);


						// keys often repeat a lot - intern to save space
					
					String	key = null;//StringInterner.intern( tempByteArray );

					if ( key == null ){

						CharBuffer	cb = Constants.BYTE_CHARSET.decode(ByteBuffer.wrap(tempByteArray));

						key = new String(cb.array(),0,cb.limit());

						key = StringInterner.intern( key );
					}

					tempMap.put( key, value);
				}

				/*	
	        if ( tempMap.size() < 8 ){

	        	tempMap = new CompactMap( tempMap );
	        }*/

				dbis.mark(Integer.MAX_VALUE);
				tempByte = dbis.read();
				dbis.reset();
				if ( nesting > 0 && tempByte == -1 ){

					throw( new BEncodingException( "BDecoder: invalid input data, 'e' missing from end of dictionary"));
				}
			}catch( Throwable e ){

				if ( !recovery_mode ){

					if ( e instanceof IOException ){

						throw((IOException)e);
					}

					throw( new IOException( Debug.getNestedExceptionMessage(e)));
				}
			}

			if (tempMap instanceof LightHashMap)
				((LightHashMap) tempMap).compactify(0.9f);


				//return the map
			
			return tempMap;

		case 'l' :
				//create the list
			
			ArrayList tempList = new ArrayList();

			try{
					//create the key
				
				Object tempElement = null;
				while ((tempElement = decodeInputStream(dbis, nesting+1)) != null) {
						//add the element
					tempList.add(tempElement);
				}

				tempList.trimToSize();
				dbis.mark(Integer.MAX_VALUE);
				tempByte = dbis.read();
				dbis.reset();
				if ( nesting > 0 && tempByte == -1 ){

					throw( new BEncodingException( "BDecoder: invalid input data, 'e' missing from end of list"));
				}
			}catch( Throwable e ){

				if ( !recovery_mode ){

					if ( e instanceof IOException ){

						throw((IOException)e);
					}

					throw( new IOException( Debug.getNestedExceptionMessage(e)));
				}
			}
				//return the list
			return tempList;

		case 'e' :
		case -1 :
			return null;

		case 'i' :
			return new Long(getNumberFromStream(dbis, 'e'));

		case '0' :
		case '1' :
		case '2' :
		case '3' :
		case '4' :
		case '5' :
		case '6' :
		case '7' :
		case '8' :
		case '9' :
				//move back one
			dbis.reset();
				//get the string
			return getByteArrayFromStream(dbis);

		default :{

			int	rem_len = dbis.available();

			if ( rem_len > 256 ){

				rem_len	= 256;
			}

			byte[] rem_data = new byte[rem_len];

			dbis.read( rem_data );

			throw( new BEncodingException(
					"BDecoder: unknown command '" + tempByte + ", remainder = " + new String( rem_data )));
		}
		}
	}

	/*
  private long getNumberFromStream(InputStream dbis, char parseChar) throws IOException {
    StringBuffer sb = new StringBuffer(3);

    int tempByte = dbis.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
    	sb.append((char)tempByte);
      tempByte = dbis.read();
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    String str = sb.toString();

    	// support some borked impls that sometimes don't bother encoding anything

    if ( str.length() == 0 ){

    	return( 0 );
    }

    return Long.parseLong(str);
  }
	 */

	private long 
	getNumberFromStream(
		BDecoderInputStream 	dbis, 
		char 					parseChar) 

		throws IOException 
	{
		final char[]	chars = new char[32];

		int tempByte = dbis.read();

		int pos = 0;

		while ((tempByte != parseChar) && (tempByte >= 0)) {
			chars[pos++] = (char)tempByte;
			if ( pos == chars.length ){
				throw( new NumberFormatException( "Number too large: " + new String(chars,0,pos) + "..." ));
			}
			tempByte = dbis.read();
		}

		//are we at the end of the stream?

		if (tempByte < 0) {

			return -1;

		}else if ( pos == 0 ){
			// support some borked impls that sometimes don't bother encoding anything

			return(0);
		}

		return( parseLong( chars, 0, pos ));
	}

	public static long
	parseLong(
		char[]	chars,
		int		start,
		int		length )
	{
		long result = 0;

		boolean negative = false;

		int 	i 	= start;
		int	max = start + length;

		long limit;

		if ( length > 0 ){

			if ( chars[i] == '-' ){

				negative = true;

				limit = Long.MIN_VALUE;

				i++;

			}else{

				limit = -Long.MAX_VALUE;
			}

			if ( i < max ){

				int digit = chars[i++] - '0';

				if ( digit < 0 || digit > 9 ){

					throw new NumberFormatException(new String(chars,start,length));

				}else{

					result = -digit;
				}
			}

			long multmin = limit / 10;

			while ( i < max ){

				// Accumulating negatively avoids surprises near MAX_VALUE

				int digit = chars[i++] - '0';

				if ( digit < 0 || digit > 9 ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				if ( result < multmin ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				result *= 10;

				if ( result < limit + digit ){

					throw new NumberFormatException(new String(chars,start,length));
				}

				result -= digit;
			}
		}else{

			throw new NumberFormatException(new String(chars,start,length));
		}

		if ( negative ){

			if ( i > start+1 ){

				return result;

			}else{	/* Only got "-" */

				throw new NumberFormatException(new String(chars,start,length));
			}
		}else{

			return -result;
		}  
	}



	// This one causes lots of "Query Information" calls to the filesystem
	/*
  private long getNumberFromStreamOld(InputStream dbis, char parseChar) throws IOException {
    int length = 0;

    //place a mark
    dbis.mark(Integer.MAX_VALUE);

    int tempByte = dbis.read();
    while ((tempByte != parseChar) && (tempByte >= 0)) {
      tempByte = dbis.read();
      length++;
    }

    //are we at the end of the stream?
    if (tempByte < 0) {
      return -1;
    }

    //reset the mark
    dbis.reset();

    //get the length
    byte[] tempArray = new byte[length];
    int count = 0;
    int len = 0;

    //get the string
    while (count != length && (len = dbis.read(tempArray, count, length - count)) > 0) {
      count += len;
    }

    //jump ahead in the stream to compensate for the :
    dbis.skip(1);

    //return the value

    CharBuffer	cb = Constants.DEFAULT_CHARSET.decode(ByteBuffer.wrap(tempArray));

    String	str_value = new String(cb.array(),0,cb.limit());

    return Long.parseLong(str_value);
  }
	 */

	private byte[] 
	getByteArrayFromStream(
		BDecoderInputStream dbis )
		
		throws IOException 
	{
		int length = (int) getNumberFromStream(dbis, ':');

		if (length < 0) {
			return null;
		}

		// note that torrent hashes can be big (consider a 55GB file with 2MB pieces
		// this generates a pieces hash of 1/2 meg

		if ( length > 8*1024*1024 ){

			throw( new IOException( "Byte array length too large (" + length + ")"));
		}

		byte[] tempArray = new byte[length];
		int count = 0;
		int len = 0;
		//get the string
		while (count != length && (len = dbis.read(tempArray, count, length - count)) > 0) {
			count += len;
		}

		if ( count != tempArray.length ){
			throw( new IOException( "BDecoder::getByteArrayFromStream: truncated"));
		}

		return tempArray;
	}

	public void
	setRecoveryMode(
		boolean	r )
	{
		recovery_mode	= r;
	}

	public static void
	print(
		PrintWriter	writer,
		Object		obj )
	{
		print( writer, obj, "", false );
	}

	private static void
	print(
		PrintWriter	writer,
		Object		obj,
		String		indent,
		boolean		skip_indent )
	{
		String	use_indent = skip_indent?"":indent;

		if ( obj instanceof Long ){

			writer.println( use_indent + obj );

		}else if ( obj instanceof byte[]){

			byte[]	b = (byte[])obj;

			if ( b.length==20 ){
				writer.println( use_indent + " { "+ ByteFormatter.nicePrint( b )+ " }" );
			}else if ( b.length < 64 ){
				writer.println( new String(b) );
			}else{
				writer.println( "[byte array length " + b.length );
			}

		}else if ( obj instanceof String ){

			writer.println( use_indent + obj );

		}else if ( obj instanceof List ){

			List	l = (List)obj;

			writer.println( use_indent + "[" );

			for (int i=0;i<l.size();i++){

				writer.print( indent + "  (" + i + ") " );

				print( writer, l.get(i), indent + "    ", true );
			}

			writer.println( indent + "]" );

		}else{

			Map	m = (Map)obj;

			Iterator	it = m.keySet().iterator();

			while( it.hasNext()){

				String	key = (String)it.next();

				if ( key.length() > 256 ){
					writer.print( indent + key.substring(0,256) + "... = " );
				}else{
					writer.print( indent + key + " = " );
				}

				print( writer, m.get(key), indent + "  ", true );
			}
		}
	}

	/**
	 * Converts any byte[] entries into UTF-8 strings
	 * @param map
	 * @return
	 */

	public static Map
	decodeStrings(
		Map	map )
	{
		if (map == null ){

			return( null );
		}

		Iterator it = map.entrySet().iterator();

		while( it.hasNext()){

			Map.Entry	entry = (Map.Entry)it.next();

			Object	value = entry.getValue();

			if ( value instanceof byte[]){

				try{
					entry.setValue( new String((byte[])value,"UTF-8" ));

				}catch( Throwable e ){

					Debug.printStackTrace(e);
				}
			}else if ( value instanceof Map ){

				decodeStrings((Map)value );
			}else if ( value instanceof List ){

				decodeStrings((List)value );
			}
		}

		return( map );
	}

	public static List
	decodeStrings(
		List	list )
	{
		if ( list == null ){

			return( null );
		}

		for (int i=0;i<list.size();i++){

			Object value = list.get(i);

			if ( value instanceof byte[]){

				try{
					String str = new String((byte[])value, "UTF-8" );

					list.set( i, str );

				}catch( Throwable e ){

					Debug.printStackTrace(e);
				}
			}else if ( value instanceof Map ){

				decodeStrings((Map)value );

			}else if ( value instanceof List ){

				decodeStrings((List)value );		 
			}
		}

		return( list );
	}

	private static void
	print(
		File		f,
		File		output )
	{
		try{
			BDecoder	decoder = new BDecoder();

			decoder.setRecoveryMode( false );

			PrintWriter	pw = new PrintWriter( new FileWriter( output ));

			print( pw, decoder.decodeStream( new BufferedInputStream( new FileInputStream( f ))));

			pw.flush();

		}catch( Throwable e ){

			e.printStackTrace();
		}
	}

	private interface
	BDecoderInputStream
	{
		public int
		read()

			throws IOException;

		public int
		read(
			byte[] buffer )

			throws IOException;

		public int
		read(
			byte[] 	buffer,
			int		offset,
			int		length )

			throws IOException;

		public int
		available()

			throws IOException;

		public boolean
		markSupported();

		public void
		mark(
				int	limit );

		public void
		reset()

			throws IOException;
	}

	private class
	BDecoderInputStreamStream
	
		implements BDecoderInputStream
	{
		final private BufferedInputStream		is;

		private
		BDecoderInputStreamStream(
			BufferedInputStream	_is )
		{
			is	= _is;
		}

		public int
		read()

		throws IOException
		{
			return( is.read());
		}

		public int
		read(
			byte[] buffer )

		throws IOException
		{
			return( is.read( buffer ));
		}

		public int
		read(
			byte[] 	buffer,
			int		offset,
			int		length )

			throws IOException
		{
			return( is.read( buffer, offset, length ));  
		}

		public int
		available()

			throws IOException
		{
			return( is.available());
		}

		public boolean
		markSupported()
		{
			return( is.markSupported());
		}

		public void
		mark(
			int	limit )
		{
			is.mark( limit );
		}

		public void
		reset()

			throws IOException
		{
			is.reset();
		}
	}

	private class
	BDecoderInputStreamArray
	
		implements BDecoderInputStream
	{
		final private byte[]		buffer;
		final private int			count;

		private int	pos;
		private int	mark;

		private
		BDecoderInputStreamArray(
			byte[]		_buffer )
		{
			buffer	= _buffer;
			count	= buffer.length;
		}

		private
		BDecoderInputStreamArray(
			byte[]		_buffer,
			int			_offset,
			int			_length )
		{
			buffer		= _buffer;
			pos			= _offset;
			count 		= Math.min( _offset + _length, _buffer.length );
			mark		= _offset;
		}
		
		public int
		read()

			throws IOException
		{
			return (pos < count) ? (buffer[pos++] & 0xff) : -1;
		}

		public int
		read(
			byte[] buffer )

			throws IOException
		{
			return( read( buffer, 0, buffer.length ));
		}

		public int
		read(
			byte[] 	b,
			int		offset,
			int		length )

			throws IOException
		{
			if ( pos >= count ){

				return( -1 );
			}

			if ( pos + length > count ){
				
				length = count - pos;
			}

			if (length <= 0){

				return( 0 );
			}

			System.arraycopy(buffer, pos, b, offset, length);

			pos += length;

			return( length );
		}

		public int
		available()

			throws IOException
		{
			return( count - pos );
		}

		public boolean
		markSupported()
		{
			return( true );
		}

		public void
		mark(
			int	limit )
		{
			mark	= pos;
		}

		public void
		reset()

			throws IOException
		{
			pos	= mark;
		}
	}


	public static void
	main(
			String[]	args )
	{	  
		print( 	new File( "C:\\Temp\\xxx.torrent" ),
				new File( "C:\\Temp\\xxx.txt" ));
	}
}
