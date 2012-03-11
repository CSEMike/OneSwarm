/*
 * File    : PluginPEPeerWrapper.java
 * Created : 01-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.peers;

/**
 * @author parg
 *
 */

import java.util.*;

import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.messaging.Message;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;

import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;


public class 
PeerImpl 
	extends LogRelation
	implements Peer
{
	protected PeerManagerImpl	manager;
	protected PEPeer			delegate;
	protected AEMonitor			this_mon	= new AEMonitor( "Peer" );
  	
	private HashMap peer_listeners;
  	

		/**
		 * don't use me, use PeerManagerImpl.getPeerForPEPeer
		 * @param _delegate
		 */
	
	protected
	PeerImpl(
		PEPeer	_delegate )
	{
		delegate	= _delegate;
		
		manager = PeerManagerImpl.getPeerManager( delegate.getManager());
	}

	public PeerManager
	getManager()
	{
		return( manager );
	}
	
	public PEPeer
	getDelegate()
	{
		return( delegate );
	}
  
  public Connection getConnection() {
    return delegate.getPluginConnection();
  }
  
  
  public boolean supportsMessaging() {
    return delegate.supportsMessaging();
  }
  
  
  public Message[] getSupportedMessages() {
    com.aelitis.azureus.core.peermanager.messaging.Message[] core_msgs = delegate.getSupportedMessages();
    
    Message[] plug_msgs = new Message[ core_msgs.length ];
    
    for( int i=0; i < core_msgs.length; i++ ) {
      plug_msgs[i] = new MessageAdapter( core_msgs[i] );
    }
    
    return plug_msgs;
  }
  
  
  
	public int 
	getState()
	{
		int	state = delegate.getPeerState();
		
		switch( state ){
			
			case PEPeer.CONNECTING:
			{
				return( Peer.CONNECTING );
			}
			case PEPeer.DISCONNECTED:
			{
				return( Peer.DISCONNECTED );
			}
			case PEPeer.HANDSHAKING:
			{
				return( Peer.HANDSHAKING );
			}
			case PEPeer.TRANSFERING:
			{
				return( Peer.TRANSFERING );
			}
		}
		
		return( -1 );
	}

	public byte[] getId()
	{
			// we *really* don't want a plugin to accidentally change our peerid (e.g. the Stuffer plugin did this)
			// as this screws stuff up bigtime
		
		byte[]	id = delegate.getId();
		
		if ( id == null ){
			return( null );
		}
		
		byte[]	copy = new byte[id.length];
		
		System.arraycopy( id, 0, copy, 0, copy.length );
		
		return( copy );
	}

	public String getIp()
	{
		return( delegate.getIp());
	}
 
	public int getPort()
	{
		return( delegate.getPort());
	}
  
  public int getTCPListenPort() {  return delegate.getTCPListenPort();  }
  public int getUDPListenPort() {  return delegate.getUDPListenPort();  }
  public int getUDPNonDataListenPort() { return delegate.getUDPNonDataListenPort(); }
	
	public final boolean[] getAvailable()
	{
		return( delegate.getAvailable().flags );
	}
   
	public boolean isPieceAvailable(int pieceNumber)
	{
		return delegate.isPieceAvailable(pieceNumber);
	}
   
	public boolean
	isTransferAvailable()
	{
		return( delegate.transferAvailable());
	}
	
	public boolean isDownloadPossible()
	{
		return delegate.isDownloadPossible();
	}
	
	public boolean isChoked()
	{
		return( delegate.isChokingMe());
	}

	public boolean isChoking()
	{
		return( delegate.isChokedByMe());
	}

	public boolean isInterested()
	{
		return( delegate.isInteresting());
	}

	public boolean isInteresting()
	{
		return( delegate.isInterested());
	}

	public boolean isSeed()
	{
		return( delegate.isSeed());
	}
 
	public boolean isSnubbed()
	{
		return( delegate.isSnubbed());
	}
 
	public long getSnubbedTime()
	{
		return delegate.getSnubbedTime();
	}
	
	public void
	setSnubbed(
		boolean	b )
	{
		delegate.setSnubbed(b);
	}
	
	public PeerStats getStats()
	{
		return( new PeerStatsImpl(manager, this, delegate.getStats()));
	}
 	

	public boolean isIncoming()
	{
		return( delegate.isIncoming());
	}

	public int getPercentDone()
	{
		return( delegate.getPercentDoneInThousandNotation());
	}

	public int getPercentDoneInThousandNotation()
	{
		return( delegate.getPercentDoneInThousandNotation());
	}
	
	public String getClient()
	{
		return( delegate.getClient());
	}

	public boolean isOptimisticUnchoke()
	{
		return( delegate.isOptimisticUnchoke());
	}
	
  public void setOptimisticUnchoke( boolean is_optimistic ) {
    delegate.setOptimisticUnchoke( is_optimistic );
  }
	
	public void
	initialize()
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public List
	getExpiredRequests()
	{
		throw( new RuntimeException( "not supported"));
	}	
  		
	public List
	getRequests()
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public int
	getNumberOfRequests()
	{
		throw( new RuntimeException( "not supported"));
	}

	public int
	getMaximumNumberOfRequests()
	{
		throw( new RuntimeException( "not supported"));
	}

	public int[]
	getPriorityOffsets()
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public boolean
	requestAllocationStarts(
		int[]	base_priorities )
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public void
	requestAllocationComplete()
	{
		throw( new RuntimeException( "not supported"));		
	}
	
	public void
	cancelRequest(
		PeerReadRequest	request )
	{
		throw( new RuntimeException( "not supported"));
	}

 
	public boolean 
	addRequest(
		PeerReadRequest	request )
	{
		throw( new RuntimeException( "not supported"));
	}


	public void
	close(
		String 		reason,
		boolean 	closedOnError,
		boolean 	attemptReconnect )
	{
		manager.removePeer( this, reason );
	}
	
	public int
	readBytes(
		int	max )
	{
		throw( new RuntimeException( "not supported"));
	}
	
	public int
	writeBytes(
		int	max )
	{
		throw( new RuntimeException( "not supported"));
	}
	
	protected void
	closed()
	{
		if ( delegate instanceof PeerForeignDelegate ){
			
			((PeerForeignDelegate)delegate).stop();
		}
	}
	
	public int
	getPercentDoneOfCurrentIncomingRequest()
	{
		return( delegate.getPercentDoneOfCurrentIncomingRequest());
	}
		  
	public int
	getPercentDoneOfCurrentOutgoingRequest()
	{
		return( delegate.getPercentDoneOfCurrentOutgoingRequest());
	}

	public void 
	addListener( 
		final PeerListener	l ) 
	{
		PEPeerListener core_listener = 
			new PEPeerListener() 
			{
				public void 
				stateChanged(
					final PEPeer peer,	// seems don't need this here
					int new_state ) 
				{
					try{
						l.stateChanged( new_state );
						
					}catch( Throwable e ){
						Debug.printStackTrace(e);
					}
				}
      
				public void 
				sentBadChunk( 
					final PEPeer peer,	// seems don't need this here
					int piece_num, 
					int total_bad_chunks )
				{
					try{
						l.sentBadChunk( piece_num, total_bad_chunks );
					
					}catch( Throwable e ){
						Debug.printStackTrace(e);
					}
				}
				
				public void addAvailability(final PEPeer peer, BitFlags peerHavePieces)
				{
				}

				public void removeAvailability(final PEPeer peer, BitFlags peerHavePieces)
				{
				}
			};
    
		delegate.addListener( core_listener );
    
		if( peer_listeners == null ){
			
			peer_listeners = new HashMap();
		}
		
		peer_listeners.put( l, core_listener );
	}
	

	public void	
	removeListener( 
		PeerListener	l ) 
	{
		if ( peer_listeners != null ){
			
			PEPeerListener core_listener = (PEPeerListener)peer_listeners.remove( l );
    
			if( core_listener != null ) {
      
				delegate.removeListener( core_listener );
			}
		}
	}
	
	public void 
	addListener( 
		final PeerListener2	l ) 
	{
		PEPeerListener core_listener = 
			new PEPeerListener() 
			{
				public void 
				stateChanged(
					final PEPeer peer,	// seems don't need this here
					int new_state ) 
				{
					fireEvent( PeerEvent.ET_STATE_CHANGED, new Integer( new_state ));
				}
      
				public void 
				sentBadChunk( 
					final PEPeer peer,	// seems don't need this here
					int piece_num, 
					int total_bad_chunks )
				{
					fireEvent( PeerEvent.ET_BAD_CHUNK, new Integer[]{ new Integer(piece_num), new Integer(total_bad_chunks)});
				}
				
				public void addAvailability(final PEPeer peer, BitFlags peerHavePieces)
				{
					fireEvent( PeerEvent.ET_ADD_AVAILABILITY,peerHavePieces.flags );
				}

				public void removeAvailability(final PEPeer peer, BitFlags peerHavePieces)
				{
					fireEvent( PeerEvent.ET_REMOVE_AVAILABILITY,peerHavePieces.flags );
				}
				protected void
				fireEvent(
					final int		type,
					final Object	data )
				{
					try{
						l.eventOccurred(
							new PeerEvent()
							{
								public int getType(){ return( type );}
								public Object getData(){ return( data );}
							});
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			};
    
		delegate.addListener( core_listener );
    
		if( peer_listeners == null ){
			
			peer_listeners = new HashMap();
		}
		
		peer_listeners.put( l, core_listener );
	}
	

	public void	
	removeListener( 
		PeerListener2	l ) 
	{
		if ( peer_listeners != null ){
			
			PEPeerListener core_listener = (PEPeerListener)peer_listeners.remove( l );
    
			if( core_listener != null ) {
      
				delegate.removeListener( core_listener );
			}
		}
	}
	
	public boolean 
	isPriorityConnection() 
	{
		return( delegate.isPriorityConnection());
	}
	
	public void 
	setPriorityConnection(
		boolean is_priority ) 
	{
		delegate.setPriorityConnection ( is_priority );
	}
	
	public void
	setUserData(
		Object		key,
		Object		value )
	{
		delegate.setUserData( key, value );
	}
	
	public Object
	getUserData(
		Object	key )
	{
		return( delegate.getUserData( key ));
	}
	
		// as we don't maintain a 1-1 mapping between these and delegates make sure
		// that "equals" etc works sensibly
	
	public boolean
	equals(
		Object	other )
	{
		if ( other instanceof PeerImpl ){
			
			return( delegate == ((PeerImpl)other).delegate );
		}
		
		return( false );
	}
	
	public int
	hashCode()
	{
		return( delegate.hashCode());
	}
	
	/** Core use only.  This is not propogated to the plugin interface
	 * 
	 * @return PEPeer object associated with the plugin Peer object
	 */
	public PEPeer getPEPeer() {
		return delegate;
	}

  // Pass LogRelation off to core objects

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getLogRelationText()
	 */
	public String getRelationText() {
		return propogatedRelationText(delegate);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.logging.LogRelation#getQueryableInterfaces()
	 */
	public Object[] getQueryableInterfaces() {
		return new Object[] { delegate };
	}
	
	public byte[] getHandshakeReservedBytes() {
		return delegate.getHandshakeReservedBytes();
	}
}
