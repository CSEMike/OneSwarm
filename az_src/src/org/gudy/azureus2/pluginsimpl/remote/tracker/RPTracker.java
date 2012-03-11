/*
 * Created on 21-Jun-2004
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

package org.gudy.azureus2.pluginsimpl.remote.tracker;

/**
 * @author parg
 *
 */


import java.net.InetAddress;
import java.net.URL;

import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.remote.torrent.RPTorrent;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

import org.gudy.azureus2.pluginsimpl.remote.*;


public class 
RPTracker
	extends		RPObject
	implements 	Tracker
{
	protected transient Tracker		delegate;

	public static RPTracker
	create(
		Tracker		_delegate )
	{
		RPTracker	res =(RPTracker)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPTracker( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPTracker(
		Tracker		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (Tracker)_delegate;
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String		method 	= request.getMethod();
		Object[]	params	= request.getParams();
		
		if ( method.equals( "host[Torrent,boolean]")){
			
			try{
				Torrent	torrent = params[0]==null?null:(Torrent)((RPTorrent)params[0])._setLocal();
				
				if ( torrent == null ){
					
					throw( new RPException( "Invalid torrent" ));
				}
				
				TrackerTorrent tt = delegate.host(torrent,((Boolean)params[1]).booleanValue());
				
				RPTrackerTorrent res = RPTrackerTorrent.create( tt );
			
				return( new RPReply( res ));		
				
			}catch( TrackerException e ){
				
				return( new RPReply( e ));
			}
		}else if ( method.equals( "getTorrents")){
			
			TrackerTorrent[]	torrents = delegate.getTorrents();
						
			RPTrackerTorrent[]	res = new RPTrackerTorrent[torrents.length];
			
			for (int i=0;i<res.length;i++){
				
				res[i] = RPTrackerTorrent.create( torrents[i]);
			}
			
			return( new RPReply( res ));	
		}
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	// ************************************************************************
	
	public TrackerTorrent
	host(
		Torrent		torrent,
		boolean		persistent )
		
		throws TrackerException
	{
		try{
			RPTrackerTorrent resp = (RPTrackerTorrent)_dispatcher.dispatch( new RPRequest( this, "host[Torrent,boolean]", new Object[]{torrent, new Boolean(persistent)})).getResponse();
			
			resp._setRemote( _dispatcher );
			
			return( resp );
			
		}catch( RPException e ){
			
			if ( e.getCause() instanceof TrackerException ){
				
				throw((TrackerException)e.getCause());
			}
			
			throw( e );
		}		
	}
	
	public TrackerTorrent
	host(
		Torrent		torrent,
		boolean		persistent,
		boolean		passive )
		
		throws TrackerException
	{
		notSupported();
		
		return( null );
	}
	
	public TrackerTorrent
	publish(
		Torrent		torrent )
	
		throws TrackerException
	{
		notSupported();
		
		return( null );
	}
	
    public TrackerTorrent[]
    getTorrents()
    {
		RPTrackerTorrent[]	res = (RPTrackerTorrent[])_dispatcher.dispatch( new RPRequest( this, "getTorrents", null )).getResponse();
		
		for (int i=0;i<res.length;i++){
			
			res[i]._setRemote( _dispatcher );
		}
		
		return( res ); 	
    }
        
    public TrackerTorrent
    getTorrent(
    	Torrent	t )
    {
    	notSupported();
    	
    	return( null );
    }
    
    public TrackerWebContext
    createWebContext(
    	int		port,
		int		protocol )
    
    	throws TrackerException
	{
       	notSupported();
		
		return( null );
	}
    
    public TrackerWebContext
    createWebContext(
    	String	name,
    	int		port,
		int		protocol )
    
    	throws TrackerException
	{	
    	notSupported();
		
		return( null );
	}
    
    public TrackerWebContext
    createWebContext(
    	String		name,
    	int			port,
		int			protocol,
		InetAddress	bind_ip )
    
    	throws TrackerException
	{	
    	notSupported();
		
		return( null );
	}
    
    public void
    addListener(
   		TrackerListener		listener )
    {
    	
    }
    
    public void
    removeListener(
   		TrackerListener		listener )
    {
    	
    }
    
	public String
	getName()
	{	
	   	notSupported();
		
		return( null );
	}
	
	public void
	setEnableKeepAlive(
		boolean		enable )
	{
	   	notSupported();
	}
	
	public URL[]
	getURLs()
	{
	   	notSupported();
		
		return( null );				
	}
	
	public InetAddress 
	getBindIP()
	{
	   	notSupported();
		
		return( null );		
	}
	
	public void
	addPageGenerator(
		TrackerWebPageGenerator	generator )
	{
		
	}
	
	public void
	removePageGenerator(
		TrackerWebPageGenerator	generator )
	{
	}
			
	public TrackerWebPageGenerator[]
	getPageGenerators()
	{
	   	notSupported();
		
		return( null );		
	}
	
	public void
	addAuthenticationListener(
		TrackerAuthenticationListener l )
	{
		
	}
	
	public void
	removeAuthenticationListener(
		TrackerAuthenticationListener l )
	{
		
	}
	
	public void
	destroy()
	{
		notSupported();
	}
}
