/*
 * Created on Feb 8, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.core3.util.Debug;

public class 
DeleteFileOnCloseInputStream
	extends InputStream
{
	private InputStream			in;
	private File				file;
	private boolean				closed;
	
	private long				pos;
	private long				mark;
	
	public
	DeleteFileOnCloseInputStream(
		File		_file )
	
		throws IOException
	{
		file		= _file;
		in			= new BufferedInputStream( new FileInputStream( file ), 128*1024 );
	}
	
	public void 
	close() 
	
		throws IOException 
	{
		if ( closed ){
			
			return;
		}
		
		closed = true;
		
		try{
			in.close();
		
		}finally{
			
			if ( !file.delete()){
				
				Debug.out( "Failed to delete file '" + file + "'" );
			}
		}
	}
	
	public int 
	read() 
	
		throws IOException 
	{
		int	result = in.read();
		
		pos++;
		
		return( result );
	}


	public int 
	read(
		byte 	b[] ) 
	
		throws IOException 
	{
		int	res = read( b, 0, b.length );
		
		if ( res > 0 ){
		
			pos += res;
		}
		
		return( res );
	}


	public int 
	read(
		byte	b[], 
		int 	off, 
		int 	len )
	
		throws IOException 
	{
		int res = in.read( b, off, len );
		
		if ( res > 0 ){
			
			pos += res;
		}
		
		return( res );
	}


	public long 
	skip(
		long 	n ) 
	
		throws IOException 
	{
		long res = in.skip( n );
		
		pos += res;
		
		return( res );
	}


	public int 
	available() 
	
		throws IOException 
	{
		return( in.available());
	}
	
	public synchronized void 
	mark(
		int readlimit ) 
	{
		mark	= pos;
	}
	
	public synchronized void 
	reset() 
	
		throws IOException
	{
		in.close();
		
		in = new FileInputStream( file );
		
		in.skip( mark );
		
		pos = mark;
	}
	
	public boolean 
	markSupported() 
	{
		return( true );
	}

	/**
	 * @return
	 *
	 * @since 3.1.1.1
	 */
	public File getFile() {
		return file;
	}
}