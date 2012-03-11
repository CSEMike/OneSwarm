/*
 * File    : PeerForeignDelegate.java
 * Created : 22-Mar-2004
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
 * @author MjrTom
 *			2005/Oct/08: Add _lastPiece
 *
 */

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.peer.impl.PEPeerControl;
import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.network.Connection;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.pluginsimpl.local.messaging.MessageAdapter;

import com.aelitis.azureus.core.networkmanager.LimitedRateGroup;
import com.aelitis.azureus.core.networkmanager.NetworkConnectionBase;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.messaging.Message;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItemFactory;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;

public class 
PeerForeignDelegate
	implements 	PEPeerTransport
{
		// this implementation supports read-only peers (i.e. download only)

	protected volatile int		_lastPiece =-1;

	private PeerManagerImpl		manager;
	private Peer				foreign;
	
	private NetworkConnectionBase	network_connection;
	
	private long	create_time		= SystemTime.getCurrentTime();
	private long	last_data_received_time =-1;
	private long	last_data_message_received_time =-1;
	private int[]	reserved_pieces	= null;
	private int		consecutive_no_requests;
	
	private BitFlags	bit_flags;
	
	private boolean		priority_connection;
	
	private Map			data;

	private HashMap		peer_listeners;

	protected AEMonitor	this_mon	= new AEMonitor( "PeerForeignDelegate" );

	protected
	PeerForeignDelegate(
		PeerManagerImpl		_manager,
		Peer				_foreign )
	{
		manager		= _manager;
		foreign		= _foreign;
		
		PEPeerManager pm = manager.getDelegate();
		
		network_connection = new PeerForeignNetworkConnection( foreign );
				
		network_connection.addRateLimiter( pm.getUploadLimitedRateGroup(), true );
		network_connection.addRateLimiter( pm.getDownloadLimitedRateGroup(), false );
	}
	
	public void
	start()
	{
		NetworkManager.getSingleton().startTransferProcessing( network_connection );
		
		NetworkManager.getSingleton().upgradeTransferProcessing( network_connection, manager.getPartitionID());
	}
	
	protected void
	stop()
	{
		NetworkManager.getSingleton().stopTransferProcessing( network_connection );
	}
	
    /**
     * Should never be called
     */
    public void sendChoke() {}

    /**
     * Nothing to do if called
     */
    public void sendHave(int piece) {}

    /**
     * Should never be called
     */
    public void sendUnChoke() {}
    
    public InetAddress getAlternativeIPv6() {  return null; }

    
 
    public boolean
    transferAvailable() 
    {
    	return( foreign.isTransferAvailable());
    }
    
    public boolean isDownloadPossible()
    {
    	return foreign.isDownloadPossible();
    }
    
	public void
	sendCancel(
		DiskManagerReadRequest	request )
	{
		foreign.cancelRequest( request );
	}
	
  /**
   * 
   * @param pieceNumber
   * @param pieceOffset
   * @param pieceLength
   * @return true is the piece is really requested
   */
	public DiskManagerReadRequest 
	request(
		int pieceNumber, 
		int pieceOffset, 
		int pieceLength )
	{
		DiskManagerReadRequest	request = manager.getDelegate().getDiskManager().createReadRequest( pieceNumber, pieceOffset, pieceLength );
		
		if ( foreign.addRequest( request )){
			
			return( request );
			
		}else{
			
			return( null );
		}
	}
	
	public int
	getRequestIndex(
		DiskManagerReadRequest request )
	{
		return( foreign.getRequests().indexOf( request ));
	}
	
	protected  void
	dataReceived()
	{
		last_data_received_time	= SystemTime.getCurrentTime();
	}
  
	public void 
	closeConnection( 
		String reason ) 
	{
		try{
			foreign.close( reason, false, false );
			
		}finally{
			
			stop();
		}
	}
		
	public List
	getExpiredRequests()
	{
		return( foreign.getExpiredRequests());
	}
  		
	public int
	getMaxNbRequests()
	{
		return( foreign.getMaximumNumberOfRequests());
	}
	public int
	getNbRequests()
	{
		return( foreign.getNumberOfRequests());
	}
		
	public int[]
	getPriorityOffsets()
	{
		return( foreign.getPriorityOffsets());
	}
	
	public boolean
	requestAllocationStarts(
		int[]	base_priorities )
	{
		return( foreign.requestAllocationStarts( base_priorities ));
	}
	
	public void
	requestAllocationComplete()
	{
		foreign.requestAllocationComplete();
	}
	
	public PEPeerControl
	getControl()
	{
		return((PEPeerControl)manager.getDelegate());
	}
  
	
	public void 
	updatePeerExchange()
	{
	}
  
  
	public PeerItem 
	getPeerItemIdentity() 
	{
		return PeerItemFactory.createPeerItem( 
				foreign.getIp(), 
				foreign.getTCPListenPort(), 
				PeerItemFactory.PEER_SOURCE_PLUGIN, 
				PeerItemFactory.HANDSHAKE_TYPE_PLAIN,
				foreign.getUDPListenPort(),
				PeerItemFactory.CRYPTO_LEVEL_1,
				0 );
	}
  
  
	public int 
	getConnectionState() 
	{
		int	peer_state = getPeerState();
	  
		if ( peer_state == Peer.CONNECTING ){
		  
			return( CONNECTION_CONNECTING );
		  
		}else if ( peer_state == Peer.HANDSHAKING ){
		  
			return( CONNECTION_WAITING_FOR_HANDSHAKE );
			  
		}else if ( peer_state == Peer.TRANSFERING ){
		  
			return( CONNECTION_FULLY_ESTABLISHED );
		  
		}else{
		
			return( CONNECTION_FULLY_ESTABLISHED );
		}
	}  
  
  	public void 
  	doKeepAliveCheck() 
  	{
  	}
  
  	public boolean 
  	doTimeoutChecks() 
  	{
  		return false;
  	}
  
  	public void 
  	doPerformanceTuningCheck() 
  	{
  	}
  
    public void
    setSuspendedLazyBitFieldEnabled(
  		boolean	enable )
    {
    }
    
  	public long 
  	getTimeSinceConnectionEstablished() 
  	{
  		long	now = SystemTime.getCurrentTime();
  		
  		if ( now > create_time ){
  			
  			return( now - create_time );
  		}
  		
  		return( 0 );
  	}
  
    public long getTimeSinceLastDataMessageReceived() {
        if (last_data_message_received_time ==-1)
          return -1;	//never received
        
        final long now =SystemTime.getCurrentTime();
        if (last_data_message_received_time <now)
            last_data_message_received_time =now;   //time went backwards
        return now -last_data_message_received_time;
      }
  
	public long getTimeSinceGoodDataReceived()
	{
		if (last_data_received_time ==-1)
			return -1;	// never received
		long now =SystemTime.getCurrentTime();
		long time_since =now -last_data_received_time;

		if (time_since <0)
		{	// time went backwards
			last_data_received_time =now;
			time_since =0;
		}

		return time_since;
	}
  
  	public long 
  	getTimeSinceLastDataMessageSent() 
  	{
  		return 0;
  	}

  	public long
  	getUnchokedForMillis()
  	{
  		return( 0 );
  	}
  	 public int 
  	 getConsecutiveNoRequestCount()
  	 {
  		 return( consecutive_no_requests );
  	 }
  	  
  	 public void 
  	 setConsecutiveNoRequestCount( 
  		int num )
  	 {
  		 consecutive_no_requests = num;
  	 }

		// PEPeer stuff
	
	public PEPeerManager
	getManager()
	{
		return( manager.getDelegate());
	}
	
	public String
	getPeerSource()
	{
		return( PEPeerSource.PS_PLUGIN );
	}
	
	public int 
	getPeerState()
	{
		int	peer_state = foreign.getState();
		
		return( peer_state );
	}

  

  
	public byte[] 
	getId()
	{
		return( foreign.getId());
	}


	public String 
	getIp()
	{
		return( foreign.getIp());
	}
	
	public String 
	getIPHostName()
	{
		return( foreign.getIp());
	}
 
	public int 
	getPort()
	{
		return( foreign.getPort());
	}

	
	public int getTCPListenPort() {  return foreign.getTCPListenPort();  }
	public int getUDPListenPort() {  return foreign.getUDPListenPort();  }
	public int getUDPNonDataListenPort() { return( foreign.getUDPNonDataListenPort()); }
  
  
  
  
	public BitFlags 
	getAvailable()
	{
		boolean[]	flags = foreign.getAvailable();
		
		if ( bit_flags == null || bit_flags.flags != flags ){
			
			bit_flags = new BitFlags( flags );
		}
		
		return( bit_flags );
	}

	public boolean
	hasReceivedBitField()
	{
		return( true );
	}
	
	public boolean isPieceAvailable(int pieceNumber)
	{
		return foreign.isPieceAvailable(pieceNumber);
	}

	public void 
	setSnubbed(boolean b)
	{
		foreign.setSnubbed( b );
	}

  
	public boolean 
	isChokingMe()
	{
		return( foreign.isChoked());
	}


	public boolean 
	isChokedByMe()
	{
		return( foreign.isChoking());
	}

	public boolean 
	isUnchokeOverride() 
	{
		return( false );
	}

	public boolean 
	isInteresting()
	{
		return( foreign.isInteresting());
	}


	public boolean 
	isInterested()
	{
		return( foreign.isInterested());
	}


	public boolean 
	isSeed()
	{
		return( foreign.isSeed());
	}
	
	public boolean 
	isRelativeSeed() 
	{
		return( false );
	}

 
	public boolean 
	isSnubbed()
	{
		return( foreign.isSnubbed());
	}
	
	public long getSnubbedTime()
	{
		return foreign.getSnubbedTime();
	}

 
	public boolean isLANLocal() {
		return( AddressUtils.isLANLocalAddress( foreign.getIp()) == AddressUtils.LAN_LOCAL_YES );
	}
	
	public boolean
	sendRequestHint(
		int		piece_number,
		int		offset,
		int		length,
		int		life )
	{	
		return( false );
	}
	
	public int[] 
	getRequestHint()
	{
		return null;
	}
	
	public void
	clearRequestHint()
	{
	}
	
	public void 
	sendRejectRequest(
		DiskManagerReadRequest request) 
	{
	}
	
	public void 
	sendBadPiece(
		int piece_number) 
	{
	}
	
	public void
	sendStatsRequest(
		Map		request )
	{
	}
	
	public void
	sendStatsReply(
		Map		reply )
	{
	}		

	public boolean
	isTCP()
	{
		return( true );
	}
	
	public PEPeerStats 
	getStats()
	{
		return( ((PeerStatsImpl)foreign.getStats()).getDelegate());
	}

 	
	public boolean 
	isIncoming()
	{
		return( foreign.isIncoming());
	}

	public int 
	getPercentDoneInThousandNotation()
	{
		return foreign.getPercentDoneInThousandNotation();
	}

	public long
	getBytesRemaining()
	{
		int	rem_pm = 1000 - getPercentDoneInThousandNotation();
		
		if ( rem_pm == 0 ){
			
			return( 0 );
		}
		
		try{
			Torrent t = manager.getDownload().getTorrent();
			
			if ( t == null ){
				
				return( Long.MAX_VALUE );
			}
			
			return(( t.getSize() * rem_pm ) / 1000 );
			
		}catch( Throwable e ){
			
			return( Long.MAX_VALUE );
		}
	}
	
	public String 
	getClient()
	{
		return( foreign.getClient());
	}

	public byte[] 
	getHandshakeReservedBytes() 
	{
		return foreign.getHandshakeReservedBytes();
	}

	public boolean 
	isOptimisticUnchoke()
	{
		return( foreign.isOptimisticUnchoke());
	}
  
  public void setOptimisticUnchoke( boolean is_optimistic ) {
    foreign.setOptimisticUnchoke( is_optimistic );
  }
	
	public int getUniqueAnnounce() 
	{
	    return -1;
	}

	public int getUploadHint() 
	{
	    return 0;
	}


	public void setUniqueAnnounce(int uniquePieceNumber) {}

	public void setUploadHint(int timeToSpread) {}  
	
	public boolean isStalledPendingLoad(){return( false );}

	public void addListener(final PEPeerListener l )
	{
		final PEPeer self =this;
		// add a listener to the foreign, then call our listeners when it calls us
		PeerListener2 core_listener = 
			new PeerListener2() 
			{
				public void
				eventOccurred(
					PeerEvent	event )
				{
					Object	data = event.getData();
					
					switch( event.getType() ){
						case PeerEvent.ET_STATE_CHANGED:{
							l.stateChanged(self, ((Integer)data).intValue());
							break;
						}
						case PeerEvent.ET_BAD_CHUNK:{
							Integer[] d = (Integer[])data;
							l.sentBadChunk(self, d[0].intValue(), d[1].intValue() );
							break;
						}
						case PeerEvent.ET_ADD_AVAILABILITY:{
							l.addAvailability(self, new BitFlags((boolean[])data));
							break;
						}
						case PeerEvent.ET_REMOVE_AVAILABILITY:{
							l.removeAvailability(self, new BitFlags((boolean[])data));
							break;
						}
					}
				}	
			};
    
			foreign.addListener( core_listener );
    
		if( peer_listeners == null ){
			
			peer_listeners = new HashMap();
		}
		
		peer_listeners.put( l, core_listener );
		
	}

	public void removeListener( PEPeerListener l )
	{
		if ( peer_listeners != null ){
			
			Object core_listener = peer_listeners.remove( l );
    
			if ( core_listener != null ){
				
				if( core_listener instanceof PeerListener ) {
	      
					foreign.removeListener((PeerListener)core_listener );
					
				}else{
					
					foreign.removeListener((PeerListener2)core_listener );

				}
			}
		}
	}
  
	public Connection
	getPluginConnection()
	{
		return( foreign.getConnection());
	}
  
	public int
	getPercentDoneOfCurrentIncomingRequest()
	{
		return( foreign.getPercentDoneOfCurrentIncomingRequest());
	}
	  
	public int
	getPercentDoneOfCurrentOutgoingRequest()
	{
		return( foreign.getPercentDoneOfCurrentOutgoingRequest());	
	}
  
	public boolean 
	supportsMessaging() 
	{
		return foreign.supportsMessaging();
	}
	
	public int getMessagingMode()
	{
		return PEPeer.MESSAGING_EXTERN;
	}

	public String
	getEncryption()
	{
		return( "" );
	}
	
	public String
	getProtocol()
	{
		String res = (String)foreign.getUserData( Peer.PR_PROTOCOL );
		
		if ( res != null ){
			
			return( res );
		}
		
		return( "Plugin" );
	}
	
	public Message[] 
	getSupportedMessages() 
	{
		org.gudy.azureus2.plugins.messaging.Message[] plug_msgs = foreign.getSupportedMessages();
    
		Message[] core_msgs = new Message[ plug_msgs.length ];
    
		for( int i=0; i < plug_msgs.length; i++ ) {
			core_msgs[i] = new MessageAdapter( plug_msgs[i] );
		}
    
		return core_msgs;
	}
    
	public Object getData(String key) {
		
		return( getUserData( key ));
	}
	
	public void 
	setData(String key, Object value)
	{
		setUserData( key, value );
	}
	
	/** To retreive arbitrary objects against a peer. */
	public Object getUserData (Object key) {
		try{
			this_mon.enter();
			if (data == null) return null;
			return data.get(key);
		}finally{

			this_mon.exit();
		}
	}

	/** To store arbitrary objects against a peer. */
	public void setUserData (Object key, Object value) {
		try{
			this_mon.enter();

			if (data == null) {
				data = new LightHashMap();
			}
			if (value == null) {
				if (data.containsKey(key)){
					data.remove(key);
					if ( data.size() == 0 ){
						data = null;
					}
				}
			} else {
				data.put(key, value);
			}
		}finally{

			this_mon.exit();
		}
	}
	  
	public boolean 
	equals(
		Object 	other )
	{
		if ( other instanceof PeerForeignDelegate ){
				
			return( foreign.equals(((PeerForeignDelegate)other).foreign ));
		}
			
		return( false );
	}
		
	public int
	hashCode()
	{
		return( foreign.hashCode());
	}
  
  
  
	public int[]
	getReservedPieceNumbers() 
	{
		return( reserved_pieces );
	}
 
  	public void 
  	addReservedPieceNumber(int piece_number) 
  	{
  		int[]	existing = reserved_pieces;
  		
  		if ( existing == null ){
  			
  			reserved_pieces = new int[]{ piece_number };
  			
  		}else{
  			
  			int[] updated = new int[existing.length+1];
  			
  			System.arraycopy( existing, 0, updated, 0, existing.length );
  					
  			updated[existing.length] = piece_number;
  			
  			reserved_pieces = updated;
  		}
  	}

  	public void 
  	removeReservedPieceNumber(int piece_number) 
  	{
  		int[]	existing = reserved_pieces;
  		
  		if ( existing != null ){
  			
  			if ( existing.length == 1 ){
  				
  				if ( existing[0] == piece_number ){
  				
  					reserved_pieces = null;
  				}
  			}else{
  				
  				int[] updated = new int[existing.length-1];
  				
  				int		pos 	= 0;
  				boolean	found 	= false;
  				
  				for (int i=0;i<existing.length;i++){
  				
  					int	pn = existing[i];
  					
  					if ( found || pn != piece_number ){
  						
  						if ( pos == updated.length ){
  							
  							return;
  						}
  						
  						updated[pos++] = pn;
  						
  					}else{
  						
  						found = true;
  					}
  				}
  				
  				reserved_pieces = updated;
  			}
  		}
  	}
  	
	public int[] 
	getIncomingRequestedPieceNumbers() 
	{
		return( new int[0] );
	}

	public int 
	getIncomingRequestCount()
	{
		return( 0 );
	}
	
	public int getOutgoingRequestCount()
	{
		return( foreign.getRequests().size());
	}
	 
	public int[] 
	getOutgoingRequestedPieceNumbers() 
	{
		List	l = foreign.getRequests();
		
		int[]	res = new int[l.size()];
		
		for (int i=0;i<l.size();i++){
			
			res[i] = ((PeerReadRequest)l.get(i)).getPieceNumber();
		}
		
		return( res );
	}

	public int getOutboundDataQueueSize()
	{
			// don't know, assume all requests are queued and block size
		
		return( getOutgoingRequestCount() * DiskManager.BLOCK_SIZE );
	}
	
	public int getLastPiece()
	{
		return _lastPiece;
	}

	public void setLastPiece(int pieceNumber)
	{
		_lastPiece =pieceNumber;
	}

    /**
     * Nothing to do if called
     */
    public void checkInterested() {}
    
    /**
     * Apaprently nothing significant to do if called
     */
	public boolean isAvailabilityAdded() {return false;}

    /**
     * Nothing to do if called
     */
	public void clearAvailabilityAdded() {};
	
	public PEPeerTransport reconnect(boolean tryUDP, boolean tryIPv6){ return null; }
	public boolean isSafeForReconnect() { return false; }
	
	public void setUploadRateLimitBytesPerSecond( int bytes ){ network_connection.setUploadLimit( bytes ); }
	public void setDownloadRateLimitBytesPerSecond( int bytes ){ network_connection.setDownloadLimit( bytes ); }
	public int getUploadRateLimitBytesPerSecond(){ return network_connection.getUploadLimit(); }
	public int getDownloadRateLimitBytesPerSecond(){ return network_connection.getDownloadLimit(); }
	
	public void
	addRateLimiter(
		LimitedRateGroup	limiter,
		boolean				upload )
	{
		network_connection.addRateLimiter( limiter, upload );
	}
	
	public void
	removeRateLimiter(
		LimitedRateGroup	limiter,
		boolean				upload )
	{
		network_connection.removeRateLimiter( limiter, upload );
	}
	
	public void
	setHaveAggregationEnabled(
		boolean		enabled )
	{
	}
	
	public void
	setPriorityConnection(
		boolean		is_priority )
	{
		priority_connection = is_priority;
	}
	
	public boolean 
	isPriorityConnection() 
	{
		return( priority_connection );
	}
	
	public void
	generateEvidence(
		IndentWriter	writer )
	{
		writer.println( "delegate: ip=" + getIp() + ",tcp=" + getTCPListenPort()+",udp="+getUDPListenPort()+",state=" + foreign.getState()+",foreign=" + foreign );
	}
	
	public String getClientNameFromExtensionHandshake() {return null;}
	public String getClientNameFromPeerID() {return null;}
	
}
