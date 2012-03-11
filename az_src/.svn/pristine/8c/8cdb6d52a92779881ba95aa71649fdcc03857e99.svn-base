/*
 * Created on 21-May-2004
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

package org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader;

/**
 * @author parg
 *
 */

import java.io.*;

import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderErrorImpl
	extends ResourceDownloaderBaseImpl
{
	protected ResourceDownloaderException		error;
	
	protected
	ResourceDownloaderErrorImpl(
		ResourceDownloaderBaseImpl	_parent,
		ResourceDownloaderException	_error )
	{
		super( _parent );
		
		error	= _error;
	}
			
	public String
	getName()
	{
		return( "<error>:" + error.getMessage());
	}
	
	public ResourceDownloaderBaseImpl
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		return( this );
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		throw( error );
	}
	
	
	public void
	asyncDownload()
	{
	}
	
	protected void
	setSize(
		long	size )
	{
	}
	
	public void
	setProperty(
		String	name,
		Object	value )
	{
		setPropertySupport( name, value );
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		throw( error );
	}
	
	public void
	cancel()
	{
		setCancelled();
	}
	
	public void
	reportActivity(
		String				activity )
	{
		informActivity( activity );
	}
	
	public void
	addListener(
		ResourceDownloaderListener	l )
	{
		super.addListener(l);
		
		informFailed( error );
	}
}
