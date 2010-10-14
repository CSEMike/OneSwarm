/*
 * Created on 21-Mar-2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
 *
 */

package com.aelitis.azureus.core.util;

import java.util.*;
import java.io.*;

public class 
CaseSensitiveFileMap 
{
	private Map	map = new HashMap();
	
	public File
	get(
		File		key )
	{
		return((File)map.get( new wrapper( key )));
	}
	
	public void
	put(
		File		key,
		File		value )
	{
		map.put( new wrapper( key ), value );
	}
	
	public void
	remove(
		File		key )
	{
		map.remove( new wrapper( key ));
	}
	
	public Iterator
	keySetIterator()
	{
		return(
			new Iterator()
			{
				private Iterator iterator = map.keySet().iterator();
				
				public boolean 
				hasNext()
				{
					return( iterator.hasNext());
				}
				
				public Object
				next()
				{
					wrapper	wrap = (wrapper)iterator.next();
					
					return( wrap.getFile());
				}
				
				public void
				remove()
				{
					iterator.remove();
				}
			});
	}
	
	public 
	static class
	wrapper
	{
		private File		file;
		private String		file_str;
		
		protected
		wrapper(
			File	_file )
		{
			file		= _file;
			file_str	= file.toString();
		}
		
		protected File
		getFile()
		{
			return( file );
		}
		
		public boolean
		equals(
			Object	other )
		{
			if ( other instanceof wrapper ){
				
				return( file_str.equals(((wrapper)other).file_str ));
			}
			
			return( false );
		}
		
		public int
		hashCode()
		{
			return( file_str.hashCode());
		}
	}
}
