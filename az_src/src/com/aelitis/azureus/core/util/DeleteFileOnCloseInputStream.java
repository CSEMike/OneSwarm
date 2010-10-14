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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.gudy.azureus2.core3.util.Debug;

public class 
DeleteFileOnCloseInputStream
	extends FilterInputStream
{
	private InputStream			in;
	private File				file;
	private boolean				closed;
	
	public
	DeleteFileOnCloseInputStream(
		File		file )
	
		throws IOException
	{
		this( new FileInputStream( file ), file );
	}
	
	protected
	DeleteFileOnCloseInputStream(
		InputStream		_in,
		File			_file )
	{
		super( _in );
				
		in		= _in;
		file	= _file;
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
}
