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

package com.aelitis.azureus.plugins.extseed.impl;

import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HostNameToIPResolver;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.peers.Peer;
import org.gudy.azureus2.plugins.peers.PeerManager;
import org.gudy.azureus2.plugins.peers.PeerManagerEvent;
import org.gudy.azureus2.plugins.peers.PeerManagerListener2;
import org.gudy.azureus2.plugins.peers.PeerReadRequest;
import org.gudy.azureus2.plugins.peers.PeerStats;
import org.gudy.azureus2.plugins.peers.Piece;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.Monitor;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.plugins.utils.Semaphore;

import com.aelitis.azureus.core.util.CopyOnWriteSet;
import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPeer;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReaderListener;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloaderListener;

public abstract class 
ExternalSeedReaderImpl 
	implements ExternalSeedReader, PeerManagerListener2
{
	public static final int	RECONNECT_DEFAULT 			= 30*1000;
	public static final int INITIAL_DELAY				= 30*1000;
	public static final int STALLED_DOWNLOAD_SPEED		= 20*1024;
	public static final int STALLED_PEER_SPEED			= 5*1024;
	
	public static final int TOP_PIECE_PRIORITY			= 100*1000;

	private ExternalSeedPlugin	plugin;
	private Torrent				torrent;
	
	private String			host;
	private String			ip_use_accessor;
	
	private String			status;
	
	private boolean			active;
	private boolean			permanent_fail;
	
	private long			last_failed_read;
	private int				consec_failures;
	
	private String			user_agent;
	
	private long			peer_manager_change_time;
	
	private volatile PeerManager		current_manager;
		
	private List<PeerReadRequest>			requests			= new LinkedList<PeerReadRequest>();
	private List<PeerReadRequest>			dangling_requests;

	private Thread			request_thread;
	private Semaphore		request_sem;
	private Monitor			requests_mon;
	
	private ExternalSeedReaderRequest	active_read_request;
	
	private int[]		priority_offsets;
	
	private boolean		fast_activate;
	private int			min_availability;
	private int			min_download_speed;
	private int			max_peer_speed;
	private long		valid_until;
	private boolean		transient_seed;
	
	private int			reconnect_delay	= RECONNECT_DEFAULT;
	
	private volatile ExternalSeedReaderRequest	current_request;

	private List	listeners	= new ArrayList();
	
	private AESemaphore			rate_sem = new AESemaphore( "ExternalSeedReaderRequest" );
	private int					rate_bytes_read;
	private int					rate_bytes_permitted;

	private volatile CopyOnWriteSet<MutableInteger>		bad_pieces = new CopyOnWriteSet<MutableInteger>();
	
	protected
	ExternalSeedReaderImpl(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,
		String					_host,
		Map						_params )
	{
		plugin	= _plugin;
		torrent	= _torrent;
		host	= _host;
		
		fast_activate 		= getBooleanParam( _params, "fast_start", false );
		min_availability 	= getIntParam( _params, "min_avail", 1 );	// default is avail based
		min_download_speed	= getIntParam( _params, "min_speed", 0 );
		max_peer_speed		= getIntParam( _params, "max_speed", 0 );
		valid_until			= getIntParam( _params, "valid_ms", 0 );
		
		if ( valid_until > 0 ){
			
			valid_until += getSystemTime();
		}
		
		transient_seed		= getBooleanParam( _params, "transient", false );

		requests_mon	= plugin.getPluginInterface().getUtilities().getMonitor();
		request_sem		= plugin.getPluginInterface().getUtilities().getSemaphore();
		
		PluginInterface	pi = plugin.getPluginInterface();
		
		user_agent = pi.getAzureusName();
		
		try{
			Properties	props = new Properties();
		
			pi.getClientIDManager().getGenerator().generateHTTPProperties( props );
			
			String ua = props.getProperty( ClientIDGenerator.PR_USER_AGENT );
			
			if ( ua != null ){
				
				user_agent	= ua;
			}
		}catch( Throwable e ){
		}
			
		setActive( null, false );
	}
	
	public String
	getIP()
	{
		synchronized( host ){
			
			if ( ip_use_accessor == null ){
				
				try{
					ip_use_accessor = HostNameToIPResolver.syncResolve( host ).getHostAddress();
					
				}catch( Throwable e ){
					
					ip_use_accessor = host;
					
					Debug.out( e );
				}
			}
			
			return( ip_use_accessor );
		}
	}
	
	public Torrent
	getTorrent()
	{
		return( torrent );
	}
	
	public String
	getStatus()
	{
		return( status );
	}
	
	public boolean
	isTransient()
	{
		return( transient_seed );
	}
	
	protected void
	log(
		String	str )
	{
		plugin.log( str );
	}
	
	protected String
	getUserAgent()
	{
		return( user_agent );
	}
	protected long
	getSystemTime()
	{
		return( plugin.getPluginInterface().getUtilities().getCurrentSystemTime());
	}
	
	protected int
	getFailureCount()
	{
		return( consec_failures );
	}
	
	protected long
	getLastFailTime()
	{
		return( last_failed_read );
	}
	
	public boolean
	isPermanentlyUnavailable()
	{
		return( permanent_fail );
	}
	
	protected void
	setReconnectDelay(
		int			delay,
		boolean		reset_failures )
	{
		reconnect_delay = delay;
		
		if ( reset_failures ){
			
			consec_failures = 0;
		}
	}
	
	public void
	eventOccurred(
		PeerManagerEvent	event )
	{
		if ( event.getType() == PeerManagerEvent.ET_PEER_SENT_BAD_DATA ){
			
			if ( event.getPeer().getIp().equals( getIP())){
									
				if ( bad_pieces.size() > 128 ){
						
					return;
				}
						
				bad_pieces.add(new MutableInteger((Integer)event.getData()));
			}
		}
	}
	
	protected boolean
	readyToActivate(
		PeerManager	peer_manager,
		Peer		peer,
		long		time_since_start )
	{
		boolean	early_days = time_since_start < INITIAL_DELAY;
		
		try{

				// first respect failure count 
			
			int	fail_count = getFailureCount();
			
			if ( fail_count > 0 ){
				
				int	delay	= reconnect_delay;
				
				for (int i=1;i<fail_count;i++){
					
					delay += delay;
					
					if ( delay > 30*60*1000 ){
						
						break;
					}
				}
				
				long	now = getSystemTime();
				
				long	last_fail = getLastFailTime();
				
				if ( last_fail < now && now - last_fail < delay ){
					
					return( false );
				}
			}
	
				// next obvious things like validity and the fact that we're complete
			
			if ( valid_until > 0 && getSystemTime() > valid_until ){
				
				return( false );
			}
			
			if ( peer_manager.getDownload().getState() != Download.ST_DOWNLOADING ){
				
				return( false );
			}
				
				// check dnd completeness too
			
			if ( peer_manager.getDownload().isComplete()){
				
				return( false );
			}
			
				// now the more interesting stuff
			
			if ( transient_seed ){
						
					// kick any existing peers that are running too slowly if the download appears
					// to be stalled		
				
				Peer[]	existing_peers = peer_manager.getPeers( getIP());
						
				int	existing_peer_count = existing_peers.length;
				
				int	global_limit	= TransferSpeedValidator.getGlobalDownloadRateLimitBytesPerSecond();
				
				if ( global_limit > 0 ){
				
						// if we have a global limit in force and we are near it then no point in
						// activating 
					
					int current_down = plugin.getGlobalDownloadRateBytesPerSec();
					
					if ( global_limit - current_down < 5*1024 ){
						
						return( false );
					}
				}
				
				int	download_limit  = peer_manager.getDownloadRateLimitBytesPerSecond();
						
				if ( global_limit > 0 && global_limit < download_limit ){
					
					download_limit = global_limit;
				}
				
				if ( 	( download_limit == 0 || download_limit > STALLED_DOWNLOAD_SPEED + 5*1024 ) &&
						peer_manager.getStats().getDownloadAverage() < STALLED_DOWNLOAD_SPEED ){
					
					for (int i=0;i<existing_peers.length;i++){
					
						Peer	existing_peer = existing_peers[i];
						
							// no point in booting ourselves!
						
						if ( existing_peer instanceof ExternalSeedPeer ){
							
							continue;
						}
						
						PeerStats stats = existing_peer.getStats();
						
						if ( stats.getTimeSinceConnectionEstablished() > INITIAL_DELAY ){
							
							if ( stats.getDownloadAverage() < STALLED_PEER_SPEED ){
								
								existing_peer.close( "Replacing slow peer with web-seed", false, false );
								
								existing_peer_count--;
							}
						}
					}
				}
				
				if ( existing_peer_count == 0 ){
					
					// check to see if we have pending connections to the same address 
								
					if ( peer_manager.getPendingPeers( getIP()).length == 0 ){
						
						log( getName() + ": activating as transient seed and nothing blocking it" );
						
						return( true );
					}
				}
			}
			
				// availability and speed based stuff needs a little time before being applied
			
			if ( fast_activate || !early_days ){
				
				if ( min_availability > 0 ){
										
					float availability = peer_manager.getDownload().getStats().getAvailability();
				
					if ( availability < min_availability){
					
						log( getName() + ": activating as availability is poor" );
						
						return( true );
					}
				}
					
				if ( min_download_speed > 0 ){
										
					if ( peer_manager.getStats().getDownloadAverage() < min_download_speed ){
						
						log( getName() + ": activating as speed is slow" );
						
						return( true );
					}
				}	
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( false );	
	}
	
	protected boolean
	readyToDeactivate(
		PeerManager	peer_manager,
		Peer		peer )
	{
		try{
				// obvious stuff first
			
			if ( valid_until > 0 && getSystemTime() > valid_until ){
				
				return( true );
			}
			
			if ( peer_manager.getDownload().getState() == Download.ST_SEEDING ){
				
				return( true );
			}
		
				// more interesting stuff
			
			if ( transient_seed ){
				
				return( false );
			}
			
			boolean	deactivate = false;
			String	reason		= "";
			
			if ( min_availability > 0 ){

				float availability = peer_manager.getDownload().getStats().getAvailability();
			
				if ( availability >= min_availability + 1 ){
				
					reason =  "availability is good";
				
					deactivate = true;
				}
			}
			
			if ( min_download_speed > 0 ){
				
				long	my_speed 		= peer.getStats().getDownloadAverage();
				
				long	overall_speed 	= peer_manager.getStats().getDownloadAverage();
				
				if ( overall_speed - my_speed > 2 * min_download_speed ){
					
					reason += (reason.length()==0?"":", ") + "speed is good";

					deactivate = true;
					
				}else{
					
					deactivate = false;
				}
			}
			
			if ( deactivate ){
				
				log( getName() + ": deactivating as " + reason );

				return( true );
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
		return( false );
	}
	
	public boolean
	checkActivation(
		PeerManager		peer_manager,
		Peer			peer )
	{
		long now = getSystemTime();
		
		if ( peer_manager == current_manager ){
			
			if ( peer_manager_change_time > now ){
				
				peer_manager_change_time	= now;
			}
			
			long	time_since_started = now - peer_manager_change_time;
			
			
			if ( peer_manager != null ){
				
				if ( active ){
					
					if ( now - peer_manager_change_time > INITIAL_DELAY && readyToDeactivate( peer_manager, peer )){
													
						setActive( peer_manager, false );
						
					}else{
						
						if ( max_peer_speed > 0 ){
							
							PeerStats ps = peer.getStats();
							
							if ( ps != null && ps.getDownloadRateLimit() != max_peer_speed ){
								
								ps.setDownloadRateLimit( max_peer_speed );
							}
						}
					}
				}else{
					
					if ( !isPermanentlyUnavailable()){
					
						if ( readyToActivate( peer_manager, peer, time_since_started )){
							
							if ( max_peer_speed > 0 ){
								
								PeerStats ps = peer.getStats();
								
								if ( ps != null ){
								
									ps.setDownloadRateLimit( max_peer_speed );
								}
							}
							
							setActive( peer_manager, true );				
						}
					}
				}
			}
		}else{
			
				// if the peer manager's changed then we always go inactive for a period to wait for 
				// download status to stabilise a bit
			
			peer_manager_change_time	= now;
			
			PeerManager existing_manager = current_manager;
			
			if ( current_manager != null ){
				
				current_manager.removeListener( this );
			}
			
			current_manager	= peer_manager;
			
			if ( current_manager != null ){

				current_manager.addListener( this );
			}
			
			setActive( existing_manager, false );
		}
		
		return( active );
	}
	
	public void
	deactivate(
		String	reason )
	{
		plugin.log( getName() + ": deactivating (" + reason  + ")" );
		
		checkActivation( null, null );
	}
	
	protected void
	setActive(
		PeerManager	_peer_manager,
		boolean		_active )
	{
		try{
			requests_mon.enter();
			
			active	= _active;
			
			status = active?"Active":"Idle";
			
			setActiveSupport( _peer_manager, _active );
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	protected void
	setActiveSupport(
		PeerManager	_peer_manager,
		boolean		_active )
	{
		// overridden if needed
	}
	
	public boolean
	isActive()
	{
		return( active );
	}
	
	protected void
	processRequests()
	{
		try{
			requests_mon.enter();

			if ( request_thread != null ){
				
				return;
			}

			request_thread = Thread.currentThread();
			
		}finally{
			
			requests_mon.exit();
		}
		
		while( true ){
			
			try{
				if ( !request_sem.reserve(30000)){
					
					try{
						requests_mon.enter();
						
						if ( requests.size() == 0 ){
							
							dangling_requests = null;
							
							request_thread	= null;
							
							break;
						}
					}finally{
						
						requests_mon.exit();
					}
				}else{
					
					List<PeerReadRequest>	selected_requests 	= new ArrayList<PeerReadRequest>();
					PeerReadRequest			cancelled_request	= null;
					
					try{
						requests_mon.enter();

							// get an advisory set to process together
						
						int	count = selectRequests( requests );
						
						if ( count <= 0 || count > requests.size()){
							
							Debug.out( "invalid count" );
							
							count	= 1;
						}
						
						for (int i=0;i<count;i++){
							
							PeerReadRequest	request = requests.remove(0);
							
							if ( request.isCancelled()){
								
									// if this is the first request then process it, otherwise leave
									// for the next-round
															
								if ( i == 0 ){
									
									cancelled_request = request;
									
								}else{
									
									requests.add( 0, request );
								}
								
								break;
								
							}else{
								
								selected_requests.add( request );
																
								if ( i > 0 ){
								
										// we've only got the sem for the first request, catch up for subsequent
									
									request_sem.reserve();
								}
							}
						}
						
						dangling_requests = new ArrayList<PeerReadRequest>( selected_requests );
						
					}finally{
						
						requests_mon.exit();
					}
					
					if ( cancelled_request != null ){
						
						informCancelled( cancelled_request );

					}else{
						
						processRequests( selected_requests );
					}
				}
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}

		/**
		 * Rate handling 
		 */
	
	public int
	readBytes(
		int		max )
	{
			// permission to read a bunch of bytes
		
			// we're out of step here due to multiple threads so we have to report what
			// has already happened and prepare for what will
		
		int	res = 0;
		
		synchronized( rate_sem ){
			
			if ( rate_bytes_read > 0 ){
				
				res = rate_bytes_read;
				
				if ( res > max ){
					
					res = max;
				}
				
				rate_bytes_read -= res;
			}
			
			int	rem = max - res;
			
			if ( rem > rate_bytes_permitted ){
				
				if ( rate_bytes_permitted == 0 ){
					
					rate_sem.release();
				}
				
				rate_bytes_permitted = rem;
			}
		}
		
		return( res );
	}
	
	public int
	getPermittedBytes()
	
		throws ExternalSeedException
	{
		synchronized( rate_sem ){
			
			if ( rate_bytes_permitted > 0 ){
				
				return( rate_bytes_permitted );
			}
		}
		
		if ( !rate_sem.reserve( 1000 )){
			
			return( 1 );	// one byte a sec to check for connection liveness
		}
		
		return( rate_bytes_permitted );
	}
	
	public void
	reportBytesRead(
		int		num )
	{
		synchronized( rate_sem ){
			
			rate_bytes_read += num;
			
			rate_bytes_permitted -= num;
			
			if ( rate_bytes_permitted < 0 ){
				
				rate_bytes_permitted = 0;
			}
		}
	}
	
	public int
	getPercentDoneOfCurrentIncomingRequest()
	{
		ExternalSeedReaderRequest	cr = current_request;
		
		if ( cr == null ){
			
			return( 0 );
		}
		
		return( cr.getPercentDoneOfCurrentIncomingRequest());
	}
	
	public int
	getMaximumNumberOfRequests()
	{
		if ( getRequestCount() == 0 ){
			
			return((int)(( getPieceGroupSize() * torrent.getPieceSize() ) / PeerReadRequest.NORMAL_REQUEST_SIZE ));
			
		}else{
			
			return( 0 );
		}
	}

	public void
	calculatePriorityOffsets(
		PeerManager		peer_manager,
		int[]			base_priorities )
	{
		try{
			Piece[]	pieces = peer_manager.getPieces();
			
			int	piece_group_size = getPieceGroupSize();
			
			int[]	contiguous_best_pieces = new int[piece_group_size];
			int[]	contiguous_highest_pri = new int[piece_group_size];
					
			Arrays.fill( contiguous_highest_pri, -1 );
			
			int	contiguous			= 0;
			int	contiguous_best_pri	= -1;
			
			int	max_contiguous	= 0;
			
			int	max_free_reqs		= 0;
			int max_free_reqs_piece	= -1;
			
			MutableInteger	mi = new MutableInteger(0);
			
			for (int i=0;i<pieces.length;i++){
				
				mi.setValue( i );
				
				if ( bad_pieces.contains(mi)){
					
					continue;
				}
				
				Piece	piece = pieces[i];
				
				if ( piece.isFullyAllocatable()){
			
					contiguous++;
					
					int	base_pri = base_priorities[i];
					
					if ( base_pri > contiguous_best_pri ){
						
						contiguous_best_pri	= base_pri;
					}
					
					for (int j=0;j<contiguous && j<contiguous_highest_pri.length;j++){
						
						if ( contiguous_best_pri > contiguous_highest_pri[j] ){
							
							contiguous_highest_pri[j]	= contiguous_best_pri;
							contiguous_best_pieces[j]	= i - j;
						}
						
						if ( j+1 > max_contiguous ){
								
							max_contiguous	= j+1;
						}
					}
		
				}else{
					
					contiguous			= 0;
					contiguous_best_pri	= -1;
					
					if ( max_contiguous == 0 ){
						
						int	free_reqs = piece.getAllocatableRequestCount();
						
						if ( free_reqs > max_free_reqs ){
							
							max_free_reqs 		= free_reqs;
							max_free_reqs_piece	= i;
						}
					}
				}
			}
					
			if ( max_contiguous == 0 ){
			
				if ( max_free_reqs_piece >= 0 ){
					
					priority_offsets	 = new int[ (int)getTorrent().getPieceCount()];

					priority_offsets[max_free_reqs_piece] = TOP_PIECE_PRIORITY;
					
				}else{
					
					priority_offsets	= null;
				}
			}else{
				
				priority_offsets	 = new int[ (int)getTorrent().getPieceCount()];
				
				int	start_piece = contiguous_best_pieces[max_contiguous-1];
				
				for (int i=start_piece;i<start_piece+max_contiguous;i++){
								
					priority_offsets[i] = TOP_PIECE_PRIORITY - (i-start_piece);
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
			
			priority_offsets	= null;
		}
	}
	
	protected abstract int
	getPieceGroupSize();
	
	protected abstract boolean
	getRequestCanSpanPieces();
	
	public int[]
	getPriorityOffsets()
	{
		return( priority_offsets );
	}
	
	protected int
	selectRequests(
		List<PeerReadRequest>	requests )
	{
		long	next_start = -1;
		
		int	last_piece_number = -1;
		
		for (int i=0;i<requests.size();i++){
			
			PeerReadRequest	request = (PeerReadRequest)requests.get(i);
			
			int	this_piece_number	= request.getPieceNumber();
			
			if ( last_piece_number != -1 && last_piece_number != this_piece_number ){
				
				if ( !getRequestCanSpanPieces()){
					
					return( i );
				}
			}
			
			long	this_start = this_piece_number * torrent.getPieceSize() + request.getOffset();
			
			if ( next_start != -1 && this_start != next_start ){
				
				return(i);
			}
			
			next_start	= this_start + request.getLength();
			
			last_piece_number	= this_piece_number;
		}
		
		return( requests.size());
	}
	    
	public byte[]
   	read(
   		int			piece_number,
   		int			piece_offset,
   		int			length,
   		final int	timeout )
   	
   		throws ExternalSeedException
   	{
   		final byte[] 	result = new byte[ length ];
   		
   		ExternalSeedHTTPDownloaderListener listener =
   			new ExternalSeedHTTPDownloaderListener()
   			{
   				private int		bp;
   				private long	start_time = SystemTime.getCurrentTime();
   				
   				public byte[]
   	        	getBuffer()
   	        	
   	        		throws ExternalSeedException
   	        	{
   					return( result );
   	        	}
   	        	
   	        	public void
   	        	setBufferPosition(
   	        		int	position )
   	        	{
   	        		bp = position;
   	        	}
   	        	
   	        	public int
   	        	getBufferPosition()
   	        	{
   	        		return( bp );
   	        	}
   	        	
   	        	public int
   	        	getBufferLength()
   	        	{
   	        		return( result.length );
   	        	}
   	        	
   	        	public int
   	        	getPermittedBytes()
   	        	
   	        		throws ExternalSeedException
   	        	{
   	        		return( result.length );
   	        	}
   	        	
   	        	public int
   	        	getPermittedTime()
   	        	{
   	        		if ( timeout == 0 ){
   	        			
   	        			return( 0 );
   	        		}
   	        		
   	        		int	rem = timeout - (int)( SystemTime.getCurrentTime() - start_time );
   	        		
   	        		if ( rem <= 0 ){
   	        			
   	        			return( -1 );
   	        		}
   	        		
   	        		return( rem );
   	        	}
   	        	
   	        	public void
   	        	reportBytesRead(
   	        		int		num )
   	        	{        		
   	        	}
   	        	
   	        	public boolean 
   	        	isCancelled() 
   	        	{
   	        		return false;
   	        	}
   	        	
   	        	public void
   	        	done()
   	        	{        		
   	        	}
   			};
   				
   		readData( piece_number, piece_offset, length, listener );
   		
   		return( result );
   	}
	
	protected void
	readData(
		ExternalSeedReaderRequest	request )
	
		throws ExternalSeedException
	{	
		readData( request.getStartPieceNumber(), request.getStartPieceOffset(), request.getLength(), request );
	}
	
	protected abstract void
	readData(
		int									piece_number,
		int									piece_offset,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException;
	
	protected void
	processRequests(
		List<PeerReadRequest>		requests )
	{	
		boolean	ok = false;
				
		ExternalSeedReaderRequest	request = new ExternalSeedReaderRequest( this, requests );
		
		active_read_request = request;
		
		try{
			current_request = request;
			
			readData( request );
													
			ok	= true;

		}catch( ExternalSeedException 	e ){
			
			if ( e.isPermanentFailure()){
				
				permanent_fail	= true;
			}
			
			status = "Failed: " + Debug.getNestedExceptionMessage(e);
			
			request.failed();
			
		}catch( Throwable e ){
			
			status = "Failed: " + Debug.getNestedExceptionMessage(e);
				
			request.failed();
			
		}finally{
			
			active_read_request = null;
			
			if ( ok ){
				
				last_failed_read	= 0;
				
				consec_failures		= 0;

			}else{
				last_failed_read	= getSystemTime();
				
				consec_failures++;
			}
		}
	}
	
	public void
	addRequests(
		List<PeerReadRequest>	new_requests )
	{
		try{
			requests_mon.enter();
			
			if ( !active ){
				
				Debug.out( "request added when not active!!!!" );
			}
				
			for (int i=0;i<new_requests.size();i++){
			
				requests.add( new_requests.get(i));

				request_sem.release();
			}
						
			if ( request_thread == null ){
				
				plugin.getPluginInterface().getUtilities().createThread(
						"RequestProcessor",
						new Runnable()
						{
							public void
							run()
							{
								processRequests();
							}
						});
			}

		}finally{
			
			requests_mon.exit();
		}
	}
	
	public void
	cancelRequest(
		PeerReadRequest	request )
	{
		try{
			requests_mon.enter();
			
			if ( requests.contains( request ) && !request.isCancelled()){
				
				request.cancel();
			}
			
			if ( dangling_requests != null && dangling_requests.contains( request ) && !request.isCancelled()){
				
				request.cancel();
			}
			
		}finally{
			
			requests_mon.exit();
		}
	}
	
	public void
	cancelAllRequests()
	{
		try{
			requests_mon.enter();
			
			for ( PeerReadRequest request: requests ){
							
				if ( !request.isCancelled()){
	
					request.cancel();
				}
			}	
			
			if ( dangling_requests != null ){
			
				for ( PeerReadRequest request: dangling_requests ){
							
					if ( !request.isCancelled()){
	
						request.cancel();
					}
				}
			}	
			
			if ( active_read_request != null ){
				
				active_read_request.cancel();
			}
		}finally{
			
			requests_mon.exit();
		}	
	}
	
	public int
	getRequestCount()
	{
		try{
			requests_mon.enter();

			return( requests.size());
			
		}finally{
			
			requests_mon.exit();
		}	
	}
	
	public List<PeerReadRequest>
	getExpiredRequests()
	{
		List<PeerReadRequest>	res = null;
		
		try{
			requests_mon.enter();
			
			for (int i=0;i<requests.size();i++){
				
				PeerReadRequest	request = (PeerReadRequest)requests.get(i);
				
				if ( request.isExpired()){
					
					if ( res == null ){
						
						res = new ArrayList<PeerReadRequest>();
					}
					
					res.add( request );
				}
			}			
		}finally{
			
			requests_mon.exit();
		}	
		
		return( res );
	}
	
	public List<PeerReadRequest>
	getRequests()
	{
		List<PeerReadRequest>	res = null;
		
		try{
			requests_mon.enter();
			
			res = new ArrayList<PeerReadRequest>( requests );
			
		}finally{
			
			requests_mon.exit();
		}	
		
		return( res );
	}
	
	protected void
	informComplete(
		PeerReadRequest		request,
		byte[]				buffer )
	{
		PooledByteBuffer pool_buffer = plugin.getPluginInterface().getUtilities().allocatePooledByteBuffer( buffer );
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestComplete( request, pool_buffer );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}		
	}
	
	protected void
	informCancelled(
		PeerReadRequest		request )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestCancelled( request );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}		
	}
	
	protected void
	informFailed(
		PeerReadRequest	request )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((ExternalSeedReaderListener)listeners.get(i)).requestFailed( request );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
	
	public void
	addListener(
		ExternalSeedReaderListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		ExternalSeedReaderListener	l )
	{
		listeners.remove( l );
	}
	
	protected int
	getIntParam(
		Map			map,
		String		name,
		int			def )
	{
		Object	obj = map.get(name);
		
		if ( obj instanceof Long ){
			
			return(((Long)obj).intValue());
		}
		
		return( def );
	}
	
	protected boolean
	getBooleanParam(
		Map			map,
		String		name,
		boolean		def )
	{
		return( getIntParam( map, name, def?1:0) != 0 );
	}
	
	protected static class
	MutableInteger
	{
		private int	value;
		
		protected
		MutableInteger(
			int		v )
		{
			value = v;
		}

		protected void
		setValue(
			int	v )
		{
			value = v;
		}
		
		protected int
		getValue()
		{
			return( value );
		}
		
		public int 
		hashCode() 
		{
			return value;
		}

		public boolean 
		equals(
			Object obj )
		{
			if (obj instanceof MutableInteger) {
				return value == ((MutableInteger)obj).value;
			}
			return false;
		}
	}
}
