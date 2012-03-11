/*
 * Created on 25-Apr-2004
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
import java.util.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.utils.resourcedownloader.*;

public class 
ResourceDownloaderAlternateImpl 	
	extends 	ResourceDownloaderBaseImpl
	implements	ResourceDownloaderListener
{
	protected ResourceDownloader[]		delegates;
	protected int						max_to_try;
	protected boolean					random;
	
	protected boolean					cancelled;
	protected ResourceDownloader		current_downloader;
	protected int						current_index;
	
	protected Object					result;
	protected AESemaphore				done_sem	= new AESemaphore("RDAlternate");
		
	protected long						size	= -2;
	
	public
	ResourceDownloaderAlternateImpl(
		ResourceDownloaderBaseImpl	_parent,
		ResourceDownloader[]		_delegates,
		int							_max_to_try,
		boolean						_random )
	{
		super( _parent );
		
		delegates		= _delegates;
		max_to_try		= _max_to_try;
		random			= _random;
		
		for (int i=0;i<delegates.length;i++){
			
			((ResourceDownloaderBaseImpl)delegates[i]).setParent( this );
		}

		if ( max_to_try < 0 ){
			
			max_to_try = delegates.length;
			
		}else{
			
			max_to_try = Math.min( max_to_try, delegates.length );
		}
		
		if ( random ){
			
			List	l = new ArrayList(Arrays.asList( delegates ));
			
			delegates = new ResourceDownloader[delegates.length];
			
			for (int i=0;i<delegates.length;i++){
								
				delegates[i] = (ResourceDownloader)l.remove((int)(Math.random()*l.size()));
			}
		}
	}
	
	public String
	getName()
	{
		String	res = "[";
		
		for (int i=0;i<delegates.length;i++){
			
			res += (i==0?"":",") + delegates[i].getName();
		}
		
		return( res + "]");
	}	
	
	
	public long
	getSize()
	
		throws ResourceDownloaderException
	{		
		if( delegates.length == 0 ){
			
			ResourceDownloaderException error = new ResourceDownloaderException( this, "Alternate download fails - 0 alteratives");
			
			informFailed( error );
			
			throw( error );
		}
		
		if ( size != -2 ){
			
			return( size );
		}
		
		try{
			for (int i=0;i<max_to_try;i++){
				
				try{
					ResourceDownloaderBaseImpl c = ((ResourceDownloaderBaseImpl)delegates[i]).getClone( this );
					
					addReportListener( c );
					
					size = c.getSize();
					
					setProperties( c );

					break;
					
				}catch( ResourceDownloaderException e ){
					
					if ( i == delegates.length-1 ){
						
						throw( e );
					}
				}
			}
		}finally{
			
			if ( size == -2 ){
				
				size = -1;
			}
			
			setSize( size );
		}

		return( size );
	}
	
	protected void
	setSize(
		long	l )
	{
		size	= l;
		
		if ( size >= 0 ){
			
			for (int i=0;i<delegates.length;i++){
				
				((ResourceDownloaderBaseImpl)delegates[i]).setSize( size );
			}
		}
	}
	
	public void
	setProperty(
		String	name,
		Object	value )
	
		throws ResourceDownloaderException
	{
		setPropertySupport( name, value );
		
		for (int i=0;i<delegates.length;i++){
			
			((ResourceDownloaderBaseImpl)delegates[i]).setProperty( name, value );
		}
	}
	
	public ResourceDownloaderBaseImpl
	getClone(
		ResourceDownloaderBaseImpl	parent )
	{
		ResourceDownloader[]	clones = new ResourceDownloader[delegates.length];
		
		for (int i=0;i<delegates.length;i++){
			
			clones[i] = ((ResourceDownloaderBaseImpl)delegates[i]).getClone( this );
		}
		
		ResourceDownloaderAlternateImpl c = 
			new ResourceDownloaderAlternateImpl( parent, clones, max_to_try, random );
		
		c.setSize(size);
		
		c.setProperties( this );

		return( c );
	}
	
	public InputStream
	download()
	
		throws ResourceDownloaderException
	{
		if( delegates.length == 0 ){
			
			ResourceDownloaderException error = new ResourceDownloaderException( this, "Alternate download fails - 0 alteratives");
			
			informFailed( error );
			
			throw( error );
		}
		
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
		
			if ( current_index == max_to_try || cancelled ){
				
				done_sem.release();
				
				informFailed((ResourceDownloaderException)result);
				
			}else{
			
				current_downloader = ((ResourceDownloaderBaseImpl)delegates[current_index]).getClone( this );
								
				informActivity( getLogIndent() + "Downloading: " + getName());
				
				current_index++;			
	
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
		
			result	= new ResourceDownloaderCancelledException( this );
			
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
		if ( informComplete( data )){
			
			result	= data;
			
			done_sem.release();
			
			return( true );
		}
		
		return( false );
	}
	
	public void
	failed(
		ResourceDownloader			downloader,
		ResourceDownloaderException e )
	{
		result		= e;
		
		asyncDownload();
	}
}
