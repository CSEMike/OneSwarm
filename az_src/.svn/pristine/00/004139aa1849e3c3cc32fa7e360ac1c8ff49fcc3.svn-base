/*
 * File    : PeerManagerImpl.java
 * Created : 28-Dec-2003
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

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.gudy.azureus2.plugins.disk.DiskManager;
import org.gudy.azureus2.plugins.download.*;
import org.gudy.azureus2.plugins.peers.*;
import org.gudy.azureus2.plugins.torrent.Torrent;
import org.gudy.azureus2.plugins.utils.PooledByteBuffer;
import org.gudy.azureus2.pluginsimpl.local.PluginCoreUtils;
import org.gudy.azureus2.pluginsimpl.local.disk.DiskManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.PooledByteBufferImpl;

import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.peermanager.peerdb.PeerItem;

public class 
PeerManagerImpl
	implements PeerManager
{
	private static final String	PEPEER_DATA_KEY	= PeerManagerImpl.class.getName();
	
	protected PEPeerManager	manager;
	
	protected static AEMonitor	pm_map_mon	= new AEMonitor( "PeerManager:Map" );

	public static PeerManagerImpl
	getPeerManager(
		PEPeerManager	_manager )
	{
		try{
			pm_map_mon.enter();
			
			PeerManagerImpl	res = (PeerManagerImpl)_manager.getData( "PluginPeerManager" );
			
			if ( res == null ){
				
				res = new PeerManagerImpl( _manager );
				
				_manager.setData( "PluginPeerManager", res );
			}
			
			return( res );
		}finally{
			
			pm_map_mon.exit();
		}
	}
	
	private Map		foreign_map		= new HashMap();
	
	private Map<PeerManagerListener,PEPeerManagerListener>	listener_map1 	= new HashMap<PeerManagerListener,PEPeerManagerListener>();
	private Map<PeerManagerListener2,CoreListener>	listener_map2 	= new HashMap<PeerManagerListener2,CoreListener>();
	
	protected AEMonitor	this_mon	= new AEMonitor( "PeerManager" );

	private final DiskManagerPiece[]	dm_pieces;
	private final PEPiece[]				pe_pieces;
	private pieceFacade[]				piece_facades;
	
	private boolean	destroyed;
	
	protected
	PeerManagerImpl(
		PEPeerManager	_manager )
	{
		manager	= _manager;
		
		dm_pieces	= _manager.getDiskManager().getPieces();
		pe_pieces	= _manager.getPieces();
		
		manager.addListener(
			new PEPeerManagerListener()
			{
				 public void
				 peerAdded(
					PEPeerManager	manager, 
					PEPeer 			peer )
				 {
					 
				 }
				 
				 public void 
				 peerRemoved( 
					PEPeerManager 	manager, 
					PEPeer 			peer )
				 {
					 PeerImpl	dele = getPeerForPEPeer( peer );
					 
					 if ( dele != null ){
						 
						 dele.closed();
					 }
				 }
				 
				 public void 
				 peerDiscovered(
					PEPeerManager manager,
					PeerItem peer, 
					PEPeer finder) 
				 {
				 }
				 
				 public void 
				 peerSentBadData(
					PEPeerManager manager, PEPeer peer,	int pieceNumber) 
				 {
				 }
				 
				 public void 
				 pieceAdded( 
					 PEPeerManager 	manager, 
					 PEPiece 		piece, 
					 PEPeer 		for_peer )
				 {
				 }

				 public void 
				 pieceRemoved( 
					 PEPeerManager 	manager, 
					 PEPiece 		piece )
				 {
				 }
					
				 public void
				 destroyed()
				 {	
					 synchronized( foreign_map ){

						 destroyed	= true;

						 Iterator it = foreign_map.values().iterator();

						 while( it.hasNext()){

							 try{
								 ((PeerForeignDelegate)it.next()).stop();

							 }catch( Throwable e ){

								 Debug.printStackTrace( e );
							 }
						 }
					 }
				 }
			});
	}

	public PEPeerManager
	getDelegate()
	{
		return( manager );
	}

	public DiskManager
	getDiskManager()
	{
		return( new DiskManagerImpl( manager.getDiskManager()));
	}
	
	public PeerManagerStats
	getStats()
	{
		return(new PeerManagerStatsImpl( manager));
	}
	
	public boolean
	isSeeding()
	{
		// this is the wrong thing to check for seeding..
		return( manager.getDiskManager().getRemainingExcludingDND() == 0 ); //yuck
	}
	
	public boolean
	isSuperSeeding()
	{
		return( manager.isSuperSeedMode());
	}
	
	public Download
	getDownload()
	
		throws DownloadException
	{
		return( DownloadManagerImpl.getDownloadStatic( manager.getDiskManager().getTorrent()));
	}
	
	public Piece[]
	getPieces()
	{
		if ( piece_facades == null ){
			
			pieceFacade[]	pf = new pieceFacade[manager.getDiskManager().getNbPieces()];
			
			for (int i=0;i<pf.length;i++){
				
				pf[i] = new pieceFacade(i);
			}
			
			piece_facades	= pf;
		}
		
		return( piece_facades );
	}
	
	public PeerStats
	createPeerStats(
		Peer	peer )
	{
		PEPeer	delegate = mapForeignPeer( peer );
		
		return( new PeerStatsImpl( this, peer, manager.createPeerStats( delegate )));
	}
	
	
	public void 
	requestComplete(
		PeerReadRequest		request,
		PooledByteBuffer 	data,
		Peer 				sender)
	{
		manager.writeBlock( 
			request.getPieceNumber(), 
			request.getOffset(), 
			((PooledByteBufferImpl)data).getBuffer(), 
			mapForeignPeer( sender ),
            false);
		
		PeerForeignDelegate	delegate = lookupForeignPeer( sender );
		
		if ( delegate != null ){
			
			delegate.dataReceived();
		}
	}
	
	public void
	requestCancelled(
		PeerReadRequest		request,
		Peer				sender )
	{
		manager.requestCanceled((DiskManagerReadRequest)request );
	}

	protected int
	getPartitionID()
	{
		return( manager.getPartitionID());
	}
	
		// these are foreign peers
	
	public void
	addPeer(
		Peer		peer )
	{
			// no private check here, we come through here for webseeds for example
		
		manager.addPeer(mapForeignPeer( peer ));
	}
	
	public void
	removePeer(
		Peer		peer )
	{
		manager.removePeer(mapForeignPeer( peer ));
	}
  
	protected void
	removePeer(
		Peer		peer,
		String		reason )
	{
		manager.removePeer(mapForeignPeer( peer ), reason );
	}
	
	public void 
	addPeer( 
		String 	ip_address, 
		int 	tcp_port ) 
	{
		addPeer(
			ip_address, 
			tcp_port, 
			0, 
			NetworkManager.getCryptoRequired( NetworkManager.CRYPTO_OVERRIDE_NONE ));
	}
	
	public void 
	addPeer( 
		String 		ip_address, 
		int 		tcp_port, 
		boolean 	use_crypto ) 
	{
		addPeer( ip_address, tcp_port, 0, use_crypto );
	}
  
	public void 
	addPeer( 
		String 		ip_address, 
		int 		tcp_port, 
		int			udp_port,
		boolean 	use_crypto ) 
	{		
		addPeer( ip_address, tcp_port, udp_port, use_crypto, null );
	}

	public void 
	addPeer( 
		String 		ip_address, 
		int 		tcp_port, 
		int			udp_port,
		boolean 	use_crypto,
		Map			user_data )
	{
		checkIfPrivate();
		
		if ( pluginPeerSourceEnabled()){

			manager.addPeer( ip_address, tcp_port, udp_port, use_crypto, user_data );
		}
	}
		
	protected boolean
	pluginPeerSourceEnabled()
	{
		if ( manager.isPeerSourceEnabled( PEPeerSource.PS_PLUGIN )){
			
			return( true );
			
		}else{
		
			Debug.out( "Plugin peer source disabled for " + manager.getDisplayName());
			
			return( false );
		}
	}
	
	protected void
	checkIfPrivate()
	{
		Download dl;
		
		try{
			dl = getDownload();
			
		}catch( Throwable e ){
			
			// if this didn't work then nothing much else will so just fall through
			
			return;
		}
		
		Torrent t = dl.getTorrent();
		
		if ( t != null ){
			
			if (  TorrentUtils.isReallyPrivate( PluginCoreUtils.unwrap( t ))){
				
				throw( new RuntimeException( "Torrent is private, peer addition not permitted" ));
			}
		}
	}
	
	public Peer[]
	getPeers()
	{
		List	l = manager.getPeers();
		
		Peer[]	res= new Peer[l.size()];
		
			// this is all a bit shagged as we should maintain the PEPeer -> Peer link rather
			// than continually creating new PeerImpls...
		
		for (int i=0;i<res.length;i++){
			
			res[i] = getPeerForPEPeer((PEPeer)l.get(i));
		}
		
		return( res );
	}
	
	public Peer[]
	getPeers(
		String		address )
	{
		List	l = manager.getPeers( address );
		
		Peer[]	res= new Peer[l.size()];
		
			// this is all a bit shagged as we should maintain the PEPeer -> Peer link rather
			// than continually creating new PeerImpls...
		
		for (int i=0;i<res.length;i++){
			
			res[i] = getPeerForPEPeer((PEPeer)l.get(i));
		}
		
		return( res );
	}
	

	public PeerDescriptor[]
  	getPendingPeers()
  	{
  		return( manager.getPendingPeers());
  	}
	
	public PeerDescriptor[]
	getPendingPeers(
		String		address )
	{
		return( manager.getPendingPeers( address ));
	}
	
	public long
	getTimeSinceConnectionEstablished(
		Peer		peer )
	{
		if ( peer instanceof PeerImpl ){
			
			return(((PeerImpl)peer).getDelegate().getTimeSinceConnectionEstablished());
		}else{
			PeerForeignDelegate	delegate = lookupForeignPeer( peer );
			
			if ( delegate != null ){
				
				return( delegate.getTimeSinceConnectionEstablished());
				
			}else{
				
				return( 0 );
			}
		}
	}
	public PEPeer
	mapForeignPeer(
		Peer	_foreign )
	{
		if ( _foreign instanceof PeerImpl ){
			
			return(((PeerImpl)_foreign).getDelegate());
		}
		
		synchronized( foreign_map ){
			
			PEPeer	local = (PEPeer)foreign_map.get( _foreign );
			
			if( local == null ){
				
				if ( destroyed ){
					
					Debug.out( "Peer added to destroyed peer manager" );
					
					return( null );
				}
				
				local 	= new PeerForeignDelegate( this, _foreign );
				
				_foreign.setUserData( PeerManagerImpl.class, local );
				
				foreign_map.put( _foreign, local );
			}
			
			return( local );
		}
	}
	
	protected PeerForeignDelegate
	lookupForeignPeer(
		Peer	_foreign )
	{
		return((PeerForeignDelegate)_foreign.getUserData( PeerManagerImpl.class ));
	}
	
	public List
	mapForeignPeers(
		Peer[]	_foreigns )
	{
		List	res = new ArrayList();
		
		for (int i=0;i<_foreigns.length;i++){
		
			PEPeer	local = mapForeignPeer( _foreigns[i]);
			
				// could already be there if torrent contains two identical seeds (for whatever reason)
			
			if ( !res.contains( local )){
				
				res.add( local );
			}
		}
		
		return( res );
	}
	
	public static PeerImpl
	getPeerForPEPeer(
		PEPeer	pe_peer )
	{
		PeerImpl	peer = (PeerImpl)pe_peer.getData( PEPEER_DATA_KEY );
		
		if ( peer == null ){
			
			peer = new PeerImpl( pe_peer );
			
			pe_peer.setData( PEPEER_DATA_KEY, peer );
		}
		
		return( peer );
	}
	
	public int 
	getUploadRateLimitBytesPerSecond()
	{
		return( manager.getUploadRateLimitBytesPerSecond());
	}

	public int 
	getDownloadRateLimitBytesPerSecond()
	{
		return( manager.getDownloadRateLimitBytesPerSecond());
	}
	
	public void
	addListener(
		final PeerManagerListener	l )
	{
		try{
			this_mon.enter();

			final Map	peer_map = new HashMap();

			PEPeerManagerListener core_listener = new PEPeerManagerListener() {
				public void peerAdded( PEPeerManager manager, PEPeer peer ) {
					PeerImpl pi = getPeerForPEPeer( peer );
					peer_map.put( peer, pi );
					l.peerAdded( PeerManagerImpl.this, pi );
				}

				public void peerRemoved( PEPeerManager manager, PEPeer peer ) {
					PeerImpl  pi = (PeerImpl)peer_map.remove( peer );

					if ( pi == null ){
						// somewhat inconsistently we get told here about the removal of
						// peers that never connected (and weren't added)
						// Debug.out( "PeerManager: peer not found");
					}
					else{         
						l.peerRemoved( PeerManagerImpl.this, pi );
					}
				}
				
				public void 
				peerDiscovered(
					PEPeerManager manager,
					PeerItem peer, 
					PEPeer finder) 
				{
				}
				
				public void 
				pieceAdded( 
					PEPeerManager 	manager, 
					PEPiece 		piece, 
					PEPeer 			for_peer )
				{
				}
				  
				public void 
				pieceRemoved( 
					PEPeerManager 	manager, 
					PEPiece 		piece )
				{
				}
				
				public void 
				peerSentBadData(PEPeerManager manager, PEPeer peer,	int pieceNumber) 
				{
				}
				
				public void
				destroyed()
				{
				}
			};

			listener_map1.put( l, core_listener );

			manager.addListener( core_listener );
		}finally{

			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		PeerManagerListener	l )
	{
		try{
			this_mon.enter();
		
			PEPeerManagerListener core_listener	= (PEPeerManagerListener)listener_map1.remove( l );
		
			if ( core_listener != null ){
				
				manager.removeListener( core_listener );
			}
      
		}finally{
			this_mon.exit();
		}
	}
	
	public void
	addListener(
		final PeerManagerListener2	l )
	{
		try{
			this_mon.enter();

			CoreListener core_listener = new CoreListener( l );
			
			listener_map2.put( l, core_listener );

			manager.addListener( core_listener );
			
			manager.getDiskManager().addListener( core_listener );
		}finally{

			this_mon.exit();
		}
	}
	
	public void
	removeListener(
		PeerManagerListener2	l )
	{
		try{
			this_mon.enter();
		
			CoreListener core_listener	= listener_map2.remove( l );
		
			if ( core_listener != null ){
				
				manager.removeListener( core_listener );
				
				manager.getDiskManager().removeListener( core_listener );
			}
      
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected class
	pieceFacade
		implements Piece
	{
		private final int	index;
		
		protected
		pieceFacade(
			int		_index )
		{
			index	= _index;
		}
		
		public int
		getIndex()
		{
			return( index );
		}
		
		public int
		getLength()
		{
			return( dm_pieces[index].getLength());
		}
		
		public boolean
		isDone()
		{
			return( dm_pieces[index].isDone());
		}
		
		public boolean
		isNeeded()
		{
			return( dm_pieces[index].isNeeded());
		}
		
		public boolean
		isDownloading()
		{
			return( pe_pieces[index] != null );
		}
		
		public boolean
		isFullyAllocatable()
		{
			if ( pe_pieces[index] != null ){
				
				return( false );
			}
			
			return( dm_pieces[index].isInteresting());
		}
		
		public int
		getAllocatableRequestCount()
		{
			PEPiece	pe_piece = pe_pieces[index];
			
			if ( pe_piece != null ){
				
				return( pe_piece.getNbUnrequested());
			}
			
			if ( dm_pieces[index].isInteresting() ){
				
				return( dm_pieces[index].getNbBlocks());
			}
			
			return( 0 );
		}
		
		public Peer
		getReservedFor()
		{
			PEPiece piece = pe_pieces[index];

			if ( piece != null ){
							
				String ip = piece.getReservedBy();
				
				if ( ip != null ){
					
					List<PEPeer> peers = manager.getPeers( ip );
					
					if ( peers.size() > 0 ){
						
						return( getPeerForPEPeer( peers.get(0)));
					}
				}
			}
			
			return( null );
		}
		
		public void
		setReservedFor(
			Peer	peer )
		{
			PEPiece piece = pe_pieces[index];
			
			PEPeer mapped_peer = mapForeignPeer( peer );
			
			if ( piece != null && mapped_peer != null ){
				
				piece.setReservedBy( peer.getIp());
			
				mapped_peer.addReservedPieceNumber( index );
			}
		}
	}
	
	private class
	CoreListener
		implements PEPeerManagerListener, DiskManagerListener
	{
		private PeerManagerListener2		listener;
		private Map<PEPeer, Peer>			peer_map = new HashMap<PEPeer, Peer>();

		private
		CoreListener(
			PeerManagerListener2		_listener )
		{
			listener	= _listener;
		}
		
		public void 
		peerAdded( 
			PEPeerManager manager, PEPeer peer )
		{
			PeerImpl pi = getPeerForPEPeer( peer );
			
			peer_map.put( peer, pi );
			
			fireEvent(
				PeerManagerEvent.ET_PEER_ADDED,
				pi,
				null,
				null );
		}
	
		public void 
		peerRemoved( 
			PEPeerManager manager, 
			PEPeer peer ) 
		{
			PeerImpl  pi = (PeerImpl)peer_map.remove( peer );
	
			if ( pi == null ){
	
			}else{
				
				fireEvent(
					PeerManagerEvent.ET_PEER_REMOVED,
					pi,
					null,
					null );
			}
		}
		
		public void 
		peerDiscovered(
			PEPeerManager 	manager,
			PeerItem 		peer_item, 
			PEPeer 			finder ) 
		{
			PeerImpl	pi;
			
			if ( finder != null ){
				
				pi = getPeerForPEPeer( finder );
				
				peer_map.put( finder, pi );
				
			}else{
				
				pi = null;
			}
			
			fireEvent(
				PeerManagerEvent.ET_PEER_DISCOVERED,
				pi,
				peer_item,
				null );
		}
		
		public void 
		pieceAdded( 
			PEPeerManager 	manager, 
			PEPiece 		piece, 
			PEPeer 			for_peer )
		{
			PeerImpl pi = for_peer==null?null:getPeerForPEPeer( for_peer );
	
			fireEvent(
					PeerManagerEvent.ET_PIECE_ACTIVATED,
					pi,
					null,
					new pieceFacade( piece.getPieceNumber()));
		}
		  
		public void 
		pieceRemoved( 
			PEPeerManager 	manager, 
			PEPiece 		piece )
		{
			fireEvent(
					PeerManagerEvent.ET_PIECE_DEACTIVATED,
					null,
					null,
					new pieceFacade( piece.getPieceNumber()));
		}
		
		public void 
		peerSentBadData(
			PEPeerManager 	manager,
			PEPeer 			peer, 
			int 			pieceNumber) 
		{
			PeerImpl pi = getPeerForPEPeer( peer );
			
			peer_map.put( peer, pi );
			
			fireEvent(
				PeerManagerEvent.ET_PEER_SENT_BAD_DATA,
				pi,
				null,
				new Integer( pieceNumber ));
			
		}
		
			// disk manager methods
		
		public void
		stateChanged(
			int oldState, 
			int	newState )
		{
		}
		
		public void
		filePriorityChanged(
			DiskManagerFileInfo		file )
		{
		}

		public void
		pieceDoneChanged(
			DiskManagerPiece		piece )
		{
			fireEvent(
					PeerManagerEvent.ET_PIECE_COMPLETION_CHANGED,
					null,
					null,
					new pieceFacade( piece.getPieceNumber()));
		}
		
		public void
		fileAccessModeChanged(
			DiskManagerFileInfo		file,
			int						old_mode,
			int						new_mode )
		{
		}
		
		protected void
		fireEvent(
			final int			type,
			final Peer			peer,
			final PeerItem		peer_item,
			final Object		data )
		{
			listener.eventOccurred(
				new PeerManagerEvent()
				{
					public PeerManager
					getPeerManager()
					{
						return( PeerManagerImpl.this );
					}
					
					public int
					getType()
					{
						return( type );
					}
					
					public Peer
					getPeer()
					{
						return( peer );
					}
					
					public PeerDescriptor
					getPeerDescriptor()
					{
						return( peer_item );
					}
					
					public Object 
					getData() 
					{
						return( data );
					}
				});
		}
			
			
		public void
		destroyed()
		{
		}
	}
}
