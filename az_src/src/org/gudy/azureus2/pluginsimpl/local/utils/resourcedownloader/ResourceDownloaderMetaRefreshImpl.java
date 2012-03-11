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
import java.net.URL;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.html.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderMetaRefreshImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	public static final int	MAX_FOLLOWS = 1;
	
	protected ResourceDownloaderBaseImpl		delegate;
	protected ResourceDownloaderBaseImpl		current_delegate;
	
	protected long						size	= -2;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;
	protected Object					result;
	protected int						done_count;
	protected AESemaphore				done_sem	= new AESemaphore("RDMetaRefresh");
			
	public
	ResourceDownloaderMetaRefreshImpl(
		ResourceDownloaderBaseImpl	_parent,
		ResourceDownloader			_delegate )
	{
		super( _parent );
		
		delegate		= (ResourceDownloaderBaseImpl)_delegate;
		
		delegate.setParent( this );
		
		current_delegate	= delegate;
	}
	
	public String
	getName()
	{
		return( delegate.getName() + ": meta-refresh" );
	}
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{	
		if ( size == -2 ){
			
			try{
				size = getSizeSupport();
				
			}finally{
				
				if ( size == -2 ){
					
					size = -1;
				}
				
				setSize( size );
			}
		}
	
		return( size );
	}
	
	protected void
	setSize(
		long	l )
	{
		size	= l;
		
		if ( size >= 0 ){
			
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
		
		delegate.setProperty( name, value );
	}
	
	protected long
	getSizeSupport()
	
		throws ResourceDownloaderException
	{
		try{
			ResourceDownloader	x = delegate.getClone( this );
			
			addReportListener( x );
			
			HTMLPage	page = HTMLPageFactory.loadPage( x.download());
			
			URL base_url = (URL)x.getProperty( "URL_URL" );

			URL	redirect = page.getMetaRefreshURL( base_url );
	
			if ( redirect == null ){
				
				ResourceDownloaderBaseImpl c = delegate.getClone( this );
				
				addReportListener( c );
				
				long res = c.getSize();
				
				setProperties( c );
				
				return( res );
			}else{
				
				ResourceDownloaderURLImpl c =  new ResourceDownloaderURLImpl( getParent(), redirect );
				
				addReportListener( c );
				
				long res = c.getSize();
				
				setProperties( c );
				
				return( res );
			}
		}catch( HTMLException e ){
			
			throw( new ResourceDownloaderException( this, "getSize failed", e ));
		}
	}	
	

	public ResourceDownloaderBaseImpl
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		ResourceDownloaderMetaRefreshImpl c = new ResourceDownloaderMetaRefreshImpl( parent, delegate.getClone( this ));
		
		c.setSize( size );
		
		c.setProperties( this );

		return( c );
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		asyncDownload();
		
		done_sem.reserve();
		
		if ( result instanceof InputStream ){
			
			return((InputStream)result);
		}
		
		throw((ResourceDownloaderException)result);
	}
	
	public void
	asyncDownload()
	{
		try{
			this_mon.enter();
		
			if ( cancelled ){
				
				done_sem.release();
				
				informFailed((ResourceDownloaderException)result);
				
			}else{
			
				done_count++;
							
				current_downloader = current_delegate.getClone( this );
				
				informActivity( getLogIndent() + "Downloading: " + getName());
	
				current_downloader.addListener( this );
				
				current_downloader.asyncDownload();
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	cancel()
	{
		setCancelled();
		
		try{
			this_mon.enter();
		
			result	= new ResourceDownloaderCancelledException(  this  );
			
			cancelled	= true;
			
			informFailed((ResourceDownloaderException)result );
			
			done_sem.release();
			
			if ( current_downloader != null ){
				
				current_downloader.cancel();
			}
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public boolean
	completed(
		ResourceDownloader	downloader,
		InputStream			data )
	{
		boolean	complete = false;
		
		try{
			if ( done_count == 1 ){
				
					// assumption is that there is a refresh tag
				
				boolean	marked = false;
				
				if ( data.markSupported()){
				
					data.mark(data.available());
					
					marked	= true;
				}
				
					// leave file open if marked so we can recover
				
				HTMLPage	page = HTMLPageFactory.loadPage( data, !marked );
				
				URL base_url = (URL)downloader.getProperty( "URL_URL" );
				
				URL	redirect = page.getMetaRefreshURL( base_url );
				
				if ( redirect == null ){
					
					if ( !marked ){
						
						failed( downloader, new ResourceDownloaderException( this, "meta refresh tag not found and input stream not recoverable"));
						
					}else{
					
						data.reset();
					
						complete	= true;
					}
					
				}else{
				
					current_delegate = new ResourceDownloaderURLImpl( this, redirect );
					
					// informActivity( "meta-refresh -> " + current_delegate.getName());
					
					asyncDownload();
				}
				
				if ( marked && !complete){
					
					data.close();
				}
			}else{
				
				complete = true;
			}
			
			if ( complete ){
			
				if ( informComplete( data )){
					
					result	= data;
					
					done_sem.release();
				}
			}
		}catch( Throwable e ){
			
			failed( downloader, new ResourceDownloaderException( this, "meta-refresh processing fails", e ));
		}
		
		return( true );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		done_sem.release();

		informFailed(e);
	}
}
