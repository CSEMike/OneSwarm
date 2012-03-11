/*
 * Created on 03-Aug-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.diskmanager.cache;

/**
 * @author parg
 *
 */
public class 
CacheFileManagerException 
	extends Exception
{
	private CacheFile	file;
	private int			fail_index;
	
	public
	CacheFileManagerException(
		CacheFile	_file,
		String		_str )
	{
		super(_str);
		
		file		= _file;
	}
	
	public
	CacheFileManagerException(
		CacheFile	_file,
		String		_str,
		Throwable	_cause )
	{
		super( _str, _cause );
		
		file	= _file;
	}
	
	public
	CacheFileManagerException(
		CacheFile	_file,
		String		_str,
		Throwable	_cause,
		int			_fail_index )
	{
		super( _str, _cause );
		
		file		= _file;
		fail_index	= _fail_index;
	}
	
	public CacheFile
	getFile()
	{
		return( file );
	}
	
	public int
	getFailIndex()
	{
		return( fail_index );
	}
}
