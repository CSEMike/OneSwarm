/*
 * File    : TrackerImpl.java
 * Created : 08-Dec-2003
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.pluginsimpl.local.tracker;

/**
 * @author parg
 *
 */

import java.net.InetAddress;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.torrent.*;
import org.gudy.azureus2.pluginsimpl.local.torrent.*;
import org.gudy.azureus2.core3.tracker.host.*;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;

public class 
TrackerImpl
	extends		TrackerWCHelper
	implements 	Tracker, TRHostListener2, TRHostAuthenticationListener
{
	private static TrackerImpl	singleton;
	private static AEMonitor 		class_mon 	= new AEMonitor( "Tracker" );

	private List	listeners	= new ArrayList();
	
	private TRHost		host;
	
	private List<TrackerAuthenticationListener>	auth_listeners	= new ArrayList<TrackerAuthenticationListener>();
	
	
	public static Tracker
	getSingleton()
	{
		try{
			class_mon.enter();
		
			if ( singleton == null ){
							
				singleton	= new TrackerImpl( TRHostFactory.getSingleton());
			}		
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	protected
	TrackerImpl(
		TRHost		_host )
	{
		setTracker( this );
		
		host		= _host;
				
		host.addListener2( this );
	}
	
	public String
	getName()
	{
		return( host.getName());
	}
	
	public void
	setEnableKeepAlive(
		boolean		enable )
	{
		Debug.out( "Keep alive setting ignored for tracker" );
	}
	
	public URL[]
	getURLs()
	{
		URL[][]	url_sets = TRTrackerUtils.getAnnounceURLs();
		
		URL[]	res = new URL[url_sets.length];
		     	          
		for (int i=0;i<res.length;i++){
			
			res[i] = url_sets[i][0];
		}
		
		return( res );
	}

	public InetAddress
	getBindIP()
	{
		return( host.getBindIP());
	}
	
	public TrackerTorrent
	host(
		Torrent		_torrent,
		boolean		_persistent )
	
		throws TrackerException
	{
		return( host( _torrent, _persistent, false ));
	}
	
	public TrackerTorrent
	host(
		Torrent		_torrent,
		boolean		_persistent,
		boolean		_passive )
	
		throws TrackerException
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		try{
			return( new TrackerTorrentImpl( host.hostTorrent( torrent.getTorrent(), _persistent, _passive )));
			
		}catch( Throwable e ){
			
			throw( new TrackerException( "Tracker: host operation fails", e ));
		}
	}
	
	public TrackerTorrent
	publish(
			Torrent _torrent)
	
		throws TrackerException
	{
		TorrentImpl	torrent = (TorrentImpl)_torrent;
		
		try{
			return( new TrackerTorrentImpl( host.publishTorrent( torrent.getTorrent() )));
			
		}catch( Throwable e ){
			
			throw( new TrackerException( "Tracker: publish operation fails", e ));
		}
	}
	
	public TrackerTorrent[]
	getTorrents()
	{
		TRHostTorrent[]	hts = host.getTorrents();
		
		TrackerTorrent[]	res = new TrackerTorrent[hts.length];
		
		for (int i=0;i<hts.length;i++){
			
			res[i] = new TrackerTorrentImpl(hts[i]);
		}
		
		return( res );
	}
	
	public TrackerTorrent
	getTorrent(
		Torrent		torrent )
	{
		TRHostTorrent	ht = host.getHostTorrent(((TorrentImpl)torrent).getTorrent());
		
		if ( ht == null ){
			
			return( null );
		}
		
		return( new TrackerTorrentImpl( ht ));
	}
	
	public TrackerWebContext
	createWebContext(
		int		port,
		int		protocol )
	
		throws TrackerException
	{
		return( new TrackerWebContextImpl( this, null, port, protocol, null ));
	}
	
	public TrackerWebContext
	createWebContext(
		String	name,
		int		port,
		int		protocol )
	
		throws TrackerException
	{
		return( new TrackerWebContextImpl( this, name, port, protocol, null ));
	}
	
	public TrackerWebContext
    createWebContext(
    	String		name,
    	int			port,
		int			protocol,
		InetAddress	bind_ip )
    
    	throws TrackerException
    {
		return( new TrackerWebContextImpl( this, name, port, protocol, bind_ip ));
    }
	
	public void
	torrentAdded(
		TRHostTorrent		t )
	{
		try{
			this_mon.enter();
		
			for (int i=0;i<listeners.size();i++){
				
				((TrackerListener)listeners.get(i)).torrentAdded(new TrackerTorrentImpl(t));
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	torrentChanged(
		TRHostTorrent		t )
	{
		for (int i=0;i<listeners.size();i++){
			
			((TrackerListener)listeners.get(i)).torrentChanged(new TrackerTorrentImpl(t));
		}
	}
	

	public void
	torrentRemoved(
		TRHostTorrent		t )	
	{	
		try{
			this_mon.enter();
		
			for (int i=0;i<listeners.size();i++){
			
				((TrackerListener)listeners.get(i)).torrentRemoved(new TrackerTorrentImpl(t));
			}
		}finally{
			
			this_mon.exit();
		}
	}
	

	public void
	addListener(
		TrackerListener		listener )
	{
		try{
			this_mon.enter();
		
			listeners.add( listener );
		
			TrackerTorrent[] torrents = getTorrents();
		
			for (int i=0;i<torrents.length;i++){
			
				listener.torrentAdded( torrents[i]);
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		TrackerListener		listener )
	{
		try{
			this_mon.enter();
		
			listeners.remove( listener );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public boolean
	authenticate(
		String		headers,
		URL			resource,
		String		user,
		String		password )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				TrackerAuthenticationListener listener = auth_listeners.get(i);
				
				boolean res;
				
				if ( listener instanceof TrackerAuthenticationAdapter ){
					
					res = ((TrackerAuthenticationAdapter)listener).authenticate( headers, resource, user, password );
					
				}else{
					
					res = listener.authenticate( resource, user, password );
				}
				
				if ( res ){
					
					return(true );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		return( false );
	}
	
	public byte[]
	authenticate(
		URL			resource,
		String		user )
	{
		for (int i=0;i<auth_listeners.size();i++){
			
			try{
				byte[] res = ((TrackerAuthenticationListener)auth_listeners.get(i)).authenticate( resource, user );
				
				if ( res != null ){
					
					return( res );
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
		}
		
		return( null );
	}
	
	public void
	addAuthenticationListener(
		TrackerAuthenticationListener	l )
	{	
		try{
			this_mon.enter();
				
			auth_listeners.add(l);
			
			if ( auth_listeners.size() == 1 ){
				
				host.addAuthenticationListener( this );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	removeAuthenticationListener(
		TrackerAuthenticationListener	l )
	{	
		try{
			this_mon.enter();
		
			auth_listeners.remove(l);
			
			if ( auth_listeners.size() == 0 ){
					
				host.removeAuthenticationListener( this );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	destroy()
	{
		super.destroy();
		
		auth_listeners.clear();

		host.removeAuthenticationListener( this );
		
		listeners.clear();
		
		host.close();
	}
}
