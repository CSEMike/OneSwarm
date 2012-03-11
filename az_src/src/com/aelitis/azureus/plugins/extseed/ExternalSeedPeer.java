/*
 * Created on 15-Dec-2005
 * Created by Paul Gardner
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.plugins.extseed;

import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.*;

import com.aelitis.azureus.core.util.CopyOnWriteList;


public class 
ExternalSeedPeer
	implements Peer, ExternalSeedReaderListener
{
	private ExternalSeedPlugin		plugin;
	
	private Download				download;
	private PeerManager				manager;
	private PeerStats				stats;
	private Map						user_data;
	
	private	ExternalSeedReader		reader;
	
	private int						state;
	
	private byte[]					peer_id;
	private boolean[]				available;
	private boolean					availabilityAdded;
	private long					snubbed;
	private boolean					is_optimistic;
	
	private Monitor					connection_mon;
	private boolean					peer_added;
	
	private List<PeerReadRequest>					request_list = new ArrayList<PeerReadRequest>();
	
	private CopyOnWriteList			listeners;
	private Monitor					listeners_mon;
		
	private boolean					doing_allocations;
	
	protected
	ExternalSeedPeer(
		ExternalSeedPlugin		_plugin,
		Download				_download,
		ExternalSeedReader		_reader )
	{
		plugin		= _plugin;
		download	= _download;
		reader		= _reader;
				
		connection_mon	= plugin.getPluginInterface().getUtilities().getMonitor();
		
		Torrent	torrent = reader.getTorrent();
				
		available	= new boolean[(int)torrent.getPieceCount()];
		
		Arrays.fill( available, true );
		
		peer_id	= new byte[20];
		
		new Random().nextBytes( peer_id );
		
		peer_id[0]='E';
		peer_id[1]='x';
		peer_id[2]='t';
		peer_id[3]=' ';
		
		listeners = new CopyOnWriteList();
		listeners_mon	= plugin.getPluginInterface().getUtilities().getMonitor();
		
		_reader.addListener( this );
	}
	
	protected boolean
	sameAs(
		ExternalSeedPeer	other )
	{
		return( reader.sameAs( other.reader ));
	}
	
	protected void
	setManager(
		PeerManager	_manager )
	{
		setState(Peer.CONNECTING);
		
		try{
			connection_mon.enter();

			manager	= _manager;
			
			if ( manager == null ){
				
				stats = null;
				
			}else{
				
				stats = manager.createPeerStats( this );
			}
			
			checkConnection();
			
		}finally{
			
			connection_mon.exit();
		}
	}
	
	public PeerManager
	getManager()
	{
		return( manager );
	}
	
	protected Download
	getDownload()
	{
		return( download );
	}
	
	protected ExternalSeedReader
	getReader()
	{
		return( reader );	
	}
	
	protected void 
	setState(
		int newState )
	{
		state	= newState;
		
		fireEvent( PeerEvent.ET_STATE_CHANGED, new Integer( newState ));
	}
	
	protected boolean
	checkConnection()
	{
		boolean	state_changed = false;
		
		try{
			connection_mon.enter();
			
			boolean	active = reader.checkActivation( manager, this );
			
			if ( manager != null && active != peer_added ){
				
				state_changed	= true;
				
				boolean	peer_was_added	= peer_added;
				
				peer_added	= active;
				
				if ( active ){
					
					addPeer();
					
				}else{
										
					if ( peer_was_added ){
						
						removePeer();
					}
				}
			}
		}finally{
			
			connection_mon.exit();
		}
		
		return( state_changed );
	}
	
	protected void
	addPeer()
	{
		setState(Peer.HANDSHAKING);

		manager.addPeer( this );
		
			// we can get synchronously disconnected - e.g. IP filter rules
		
		if ( peer_added ){
			
			setState(Peer.TRANSFERING);
	
			try{
				listeners_mon.enter();
	
				if ( availabilityAdded ){
					
					Debug.out( "availabililty already added" );
					
				}else{
					
					availabilityAdded	= true;
	
					fireEvent( PeerEvent.ET_ADD_AVAILABILITY, getAvailable());				
				}
			
			}finally{
				
				listeners_mon.exit();
			}
		}
	}
	
	protected void
	removePeer()
	{	
		setState(Peer.CLOSING);
	
		try{
			listeners_mon.enter();
			
			if ( availabilityAdded ){
				
				availabilityAdded	= false;

				fireEvent( PeerEvent.ET_REMOVE_AVAILABILITY, getAvailable());				
			}
		}finally{
			
			listeners_mon.exit();
		}
				
		manager.removePeer( this );
		
		setState( Peer.DISCONNECTED );
	}
	
	public void
	requestComplete(
		PeerReadRequest		request,
		PooledByteBuffer	data )
	{
		PeerManager	man = manager;
		
		if ( request.isCancelled() || man == null ){
	
			data.returnToPool();
			
		}else{
			
			try{
				man.requestComplete( request, data, this );
					
				// moved to the rate-limiting code for more accurate stats
				// stats.received( request.getLength());
				
			}catch( Throwable e ){
				
				data.returnToPool();
				
				e.printStackTrace();
			}
		}	
	}
	
	public void
	requestCancelled(
		PeerReadRequest		request )
	{
		PeerManager	man = manager;

		if ( man != null ){
			
			man.requestCancelled( request, this );
		}
	}
	
	public void
	requestFailed(
		PeerReadRequest		request )
	{
		PeerManager	man = manager;

		if ( man != null ){
				
			man.requestCancelled( request, this );

			try{
				connection_mon.enter();
				
				if ( peer_added ){
					
					plugin.log( reader.getName() + " failed - " + reader.getStatus() + ", permanent = " + reader.isPermanentlyUnavailable());
						
					peer_added	= false;

					removePeer();
				}
			}finally{
				
				connection_mon.exit();
			}
			
			if ( reader.isTransient() && reader.isPermanentlyUnavailable()){
				
				plugin.removePeer( this );
			}
		}
	}
	
	public int 
	getState()
	{
		return state;
	}

	public byte[] 
	getId()
	{
		return( peer_id );
	}
  
	public URL
	getURL()
	{
		return( reader.getURL());
	}
	
	public String 
	getIp()
	{
		return( reader.getIP());
	}
  
	public int 
	getTCPListenPort()
	{
		return( 0 );
	}
  
	public int 
	getUDPListenPort()
	{
		return( 0 );
	}
	
	public int 
	getUDPNonDataListenPort()
	{
		return( 0 );
	}
  
	public int 
	getPort()
	{
		return( reader.getPort());
	}
	
	
	public final boolean[] 
	getAvailable()
	{
		return( available );
	}
   
	public final boolean 
	isPieceAvailable(
		int pieceNumber )
	{
		return( true );
	}
	              
	public boolean
	isTransferAvailable()
	{
		return( reader.isActive());
	}
	
	public boolean isDownloadPossible()
	{
		return peer_added &&reader.isActive();
	}
	
	public boolean 
	isChoked()
	{
		return( false );
	}

	public boolean 
	isChoking()
	{
		return( false );
	}

	public boolean 
	isInterested()
	{
		return( false );
	}

	public boolean 
	isInteresting()
	{
		return( true );
	}

	public boolean 
	isSeed()
	{
		return( true );
	}
 
	public boolean 
	isSnubbed()
	{
		if ( snubbed != 0 ){
			
				// mindless snubbing control - if we have no outstanding requests then we
				// drop the snubbed status :)
			
			if ( reader.getRequestCount() == 0 ){
				
				snubbed = 0;
			}
		}
		
		return( snubbed != 0 );
	}
 
	public long	
	getSnubbedTime()
	{
		if ( !isSnubbed()){
			
			return 0;
		}
		
		final long now = plugin.getPluginInterface().getUtilities().getCurrentSystemTime();
		
		if ( now < snubbed ){
			
			snubbed = now - 26;	// odds are ...
		}
		
		return now - snubbed;
	}
 
	public void 
	setSnubbed( 
		boolean b)
	{
		if (!b){
			
			snubbed = 0;
			
		}else if ( snubbed == 0 ){
			
			snubbed = plugin.getPluginInterface().getUtilities().getCurrentSystemTime();
		}
	}
	
	public boolean 
	isOptimisticUnchoke()
	{
		return( is_optimistic );
	}
	  
	public void 
	setOptimisticUnchoke( 
		boolean _is_optimistic )
	{
		is_optimistic	= _is_optimistic;
	}

	public PeerStats 
	getStats()
	{
		return( stats );
	}
 	
	public boolean 
	isIncoming()
	{
		return( false );
	}
	
	public int 
	getPercentDone()
	{
		return( 1000 );
	}

	public int 
	getPercentDoneInThousandNotation()
	{
		return( 1000 );
	}
	
	public String 
	getClient()
	{
		return( reader.getName());
	}
	
	public List
	getExpiredRequests()
	{
		return( reader.getExpiredRequests());
		
	}
  		
	public List<PeerReadRequest>
	getRequests()
	{
		List<PeerReadRequest> requests = reader.getRequests();
		
		if ( request_list.size() > 0 ){
			
			try{
				requests.addAll( request_list );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
		
		return( requests );
	}

	public int
	getMaximumNumberOfRequests()
	{
		return( reader.getMaximumNumberOfRequests());
	}
	
	public int
	getNumberOfRequests()
	{
		return( reader.getRequestCount() + request_list.size());
	}

	public int[]
	getPriorityOffsets()
	{

		return( reader.getPriorityOffsets());
	}
	
	public boolean
	requestAllocationStarts(
		int[]	base_priorities )
	{
		if ( doing_allocations ){
			
			Debug.out( "recursive allocations" );
		}
		
		doing_allocations	= true;
		
		if ( request_list.size() != 0 ){
			
			Debug.out( "req list must be empty" );
		}
		
		PeerManager	pm = manager;
		
		if ( pm != null ){
		
			reader.calculatePriorityOffsets( pm, base_priorities );
		}
		
		return( true );
	}
	
	public void
	requestAllocationComplete()
	{
		reader.addRequests( request_list );
		
		request_list.clear();
		
		doing_allocations	= false;
	}
	
	public boolean 
	addRequest(
		PeerReadRequest	request )
	{	
		if ( !doing_allocations ){
			
			Debug.out( "request added when not in allocation phase" );
		}
		
		if ( !request_list.contains( request )){
			
			request_list.add( request );
			
			snubbed = 0;
		}
		
		return( true );
	}

	public void
	cancelRequest(
		PeerReadRequest	request )
	{
		reader.cancelRequest( request );
	}

	public void
	close(
		String 		reason,
		boolean 	closedOnError,
		boolean 	attemptReconnect )
	{
		boolean	peer_was_added;
		
		try{
			connection_mon.enter();

			peer_was_added	= peer_added;

			reader.cancelAllRequests();
			
			reader.deactivate( reason );
			
			peer_added	= false;
			
			try{
				listeners_mon.enter();
				
				if ( availabilityAdded ){
								
					availabilityAdded	= false;
	
					fireEvent( PeerEvent.ET_REMOVE_AVAILABILITY, getAvailable());
				}
			}finally{
				
				listeners_mon.exit();
			}
		}finally{
			
			connection_mon.exit();
		}
		
		if ( peer_was_added ){
		
			manager.removePeer( this );
		}
		
		setState( Peer.DISCONNECTED );
		
		if ( reader.isTransient()){
							
			plugin.removePeer( this );
		}
	}
	
	public void
	remove()
	{
		plugin.removePeer( this );
	}

	public void	
	addListener( 
		PeerListener	listener )
	{
		try{
			listeners_mon.enter();
						
			listeners.add(listener);
			
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void 
	removeListener(	
		PeerListener listener )
	{	
		try{
			listeners_mon.enter();
		
			listeners.remove(listener);
		
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void	
	addListener( 
		PeerListener2	listener )
	{
		try{
			listeners_mon.enter();
						
			listeners.add(listener);
			
		}finally{
			
			listeners_mon.exit();
		}
	}
	
	public void 
	removeListener(	
		PeerListener2 listener )
	{	
		try{
			listeners_mon.enter();
		
			listeners.remove(listener);
		
		}finally{
			
			listeners_mon.exit();
		}
	}
  
	protected void
	fireEvent(
		final int		type,
		final Object	data )
	{
		try{
			listeners_mon.enter();

			List	ref = listeners.getList();
			
			for (int i =0; i <ref.size(); i++){
				
				try{
					Object	 _listener = ref.get(i);
						
					if ( _listener instanceof PeerListener ){
						
						PeerListener	listener = (PeerListener)_listener;
						
						if ( type == PeerEvent.ET_STATE_CHANGED ){
							
							listener.stateChanged(((Integer)data).intValue());
							
						}else if ( type == PeerEvent.ET_BAD_CHUNK ){
							
							Integer[]	d = (Integer[])data;
							
							listener.sentBadChunk(d[0].intValue(),d[1].intValue());
						}
					}else{
						
						PeerListener2	listener = (PeerListener2)_listener;
	
						listener.eventOccurred(
							new PeerEvent()
							{
								public int
								getType()
								{
									return( type );
								}
								
								public Object
								getData()
								{
									return( data );
								}
							});
					}
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}finally{
			
			listeners_mon.exit();
		}
	}
	public Connection 
	getConnection()
	{
		return( null );
	}
  
  
	public boolean 
	supportsMessaging()
	{
		return( false );
	}
  
	public Message[] 
	getSupportedMessages()
	{
		return( new Message[0] );
	}
	
	public int
	readBytes(
		int	max )
	{
		int	res = reader.readBytes( max );
		
		if ( res > 0 ){
			
			stats.received( res );
		}
				
		return( res );
	}
	
	public int
	writeBytes(
		int	max )
	{
		throw( new RuntimeException( "Not supported" ));
	}
	
	public int
	getPercentDoneOfCurrentIncomingRequest()
	{
		return( reader.getPercentDoneOfCurrentIncomingRequest());
	}
		  
	public int
	getPercentDoneOfCurrentOutgoingRequest()
	{
		return( 0 );
	}
	
	public Map
	getProperties()
	{
		return( new HashMap());
	}	
	
	public String
	getName()
	{
		return( reader.getName());
	}
	
	public void
	setUserData(
		Object		key,
		Object		value )
	{
		if ( user_data == null ){
			
			user_data	= new HashMap();
		}
		
		user_data.put( key, value );
	}
	
	public Object
	getUserData(
		Object	key )
	{
		if ( key == Peer.PR_PROTOCOL ){
			
			return( reader.getURL().getProtocol().toUpperCase());
		}
		
		if ( user_data == null ){
			
			return( null );
		}
		
		return( user_data.get( key ));
	}
	
	public byte[] getHandshakeReservedBytes() {
		return null;
	}
	
	public boolean isPriorityConnection() {
		return false;
	}
	
	public void setPriorityConnection(boolean is_priority) {
	}
}
