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
ResourceDownloaderDelayedImpl
	extends ResourceDownloaderBaseImpl
{
	protected ResourceDownloaderDelayedFactory		factory;
	
	protected ResourceDownloaderBaseImpl		delegate;
		
	protected long		size = -2;
	
	protected
	ResourceDownloaderDelayedImpl(
		ResourceDownloaderBaseImpl				_parent,
		ResourceDownloaderDelayedFactory		_factory )
	{
		super( _parent );
		
		factory	= _factory;
	}
	
	protected void
	getDelegate()
	{
		try{
			this_mon.enter();
		
			if ( delegate == null ){
				
				try{
					delegate	= (ResourceDownloaderBaseImpl)factory.create();
					
					delegate.setParent( this );
	
					if ( size >= 0 ){
						
						delegate.setSize( size );
					}
					
				}catch(  ResourceDownloaderException e ){
					
					delegate = new ResourceDownloaderErrorImpl( this, e );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public String
	getName()
	{
		if ( delegate == null ){
			
			return( "<...>" );
		}
		
		return( delegate.getName());
	}
	
	public ResourceDownloaderBaseImpl
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{		
		ResourceDownloaderDelayedImpl	c = new ResourceDownloaderDelayedImpl( parent, factory );
		
		c.setSize( size );
		
		c.setProperties( this );

		return( c );
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		getDelegate();
		
		return( delegate.download());	
	}
	
	
	public void
	asyncDownload()
	{
		getDelegate();
		
		delegate.asyncDownload();	
	}
	
	protected void
	setSize(
		long	_size )
	{
		size	= _size;
		
		if ( delegate != null && size >= 0){
					
			delegate.setSize( size );
		}
	}
	
	public void
	setProperty(
		String	name,
		Object	value )
	
		throws ResourceDownloaderException
	{
		setPropertySupport( name, value );
		
		if ( delegate != null ){
			
			delegate.setProperty( name, value );
		}
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		getDelegate();
		
		return( delegate.getSize());	
	}
	
	public void
	cancel()
	{
		setCancelled();
		
		getDelegate();
		
		delegate.cancel();		
	}
	
	public void
	reportActivity(
		String				activity )
	{
		getDelegate();
		
		delegate.reportActivity( activity );		
	}
	
	public void
	addListener(
		ResourceDownloaderListener	l )
	{
		getDelegate();
		
		delegate.addListener(l);	
	}
	
	public void
	removeListener(
		ResourceDownloaderListener	l )
	{
		getDelegate();
		
		delegate.removeListener(l);		
	}
}
