/*
 * Created on Jul 16, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.lws;

import java.io.File;
import java.util.List;

import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerCheckRequest;
import org.gudy.azureus2.core3.disk.DiskManagerCheckRequestListener;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfoSet;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequest;
import org.gudy.azureus2.core3.disk.DiskManagerReadRequestListener;
import org.gudy.azureus2.core3.disk.DiskManagerWriteRequest;
import org.gudy.azureus2.core3.disk.DiskManagerWriteRequestListener;
import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerHelper;
import org.gudy.azureus2.core3.disk.impl.DiskManagerImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerPieceImpl;
import org.gudy.azureus2.core3.disk.impl.DiskManagerRecheckScheduler;
import org.gudy.azureus2.core3.disk.impl.DiskManagerUtil;
import org.gudy.azureus2.core3.disk.impl.access.DMAccessFactory;
import org.gudy.azureus2.core3.disk.impl.access.DMChecker;
import org.gudy.azureus2.core3.disk.impl.access.DMReader;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMap;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapper;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapperFactory;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapperFile;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DirectByteBuffer;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.core3.util.SystemTime;

import com.aelitis.azureus.core.diskmanager.access.DiskAccessController;
import com.aelitis.azureus.core.diskmanager.cache.CacheFile;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileOwner;


public class 
LWSDiskManager 
	implements DiskManagerHelper
{
	private static sePiece	piece = new sePiece();	

	private LightWeightSeed			lws;
	private DiskAccessController	disk_access_controller;
	private File					save_file;
	private DMReader 				reader;
	private DMChecker 				checker_use_accessor;
	private DMPieceMapper			piece_mapper;
	private DMPieceMap				piece_map_use_accessor;
	
	private sePiece[]					pieces;
	private DiskManagerFileInfoImpl[]	files;
	private String						internal_name;
	private DownloadManagerState		download_state;
	
	private boolean	started;
	private int 	state 			= DiskManager.INITIALIZING;
	private String	error_message	= "";
	
	protected
	LWSDiskManager(
		LightWeightSeed				_lws,
		File						_save_file )
	{
		lws						= _lws;
		save_file				= _save_file;

		disk_access_controller	= DiskManagerImpl.getDefaultDiskAccessController();
				
		download_state	= new LWSDiskManagerState();
		
		TOTorrent	torrent = lws.getTOTorrent( false );
		
		pieces = new sePiece[ torrent.getNumberOfPieces() ];
		
		for (int i=0;i<pieces.length;i++){
			
			pieces[i] = piece;
		}
	}
	
	public String
	getName()
	{
		return( lws.getName());
	}
	

	public int
	getCacheMode()
	{
		return( CacheFileOwner.CACHE_MODE_NORMAL );
	}
	
	public long[]
	getReadStats()
	{
		if ( reader == null ){
	
			return( new long[]{ 0, 0 });
		}
				
		return( reader.getStats());
	}
	
	public void
	start()
	{
		try{
			TOTorrent	torrent = lws.getTOTorrent( false );
			
			internal_name = ByteFormatter.nicePrint(torrent.getHash(),true);
			
			LocaleUtilDecoder	locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );
			
			piece_mapper = DMPieceMapperFactory.create( torrent );
			
			piece_mapper.construct( locale_decoder, save_file.getName());
			
			files = getFileInfo( piece_mapper.getFiles(), save_file );
								
			reader = DMAccessFactory.createReader( this );
			
			reader.start();
			
			if ( state != DiskManager.FAULTY ){
			
				started = true;

				state = DiskManager.READY;
			}						
		}catch( Throwable e ){
			
			setFailed( "start failed - " + Debug.getNestedExceptionMessage(e));
		}
	}
	
	protected DiskManagerFileInfoImpl[]
	getFileInfo(
		DMPieceMapperFile[]		pm_files,
		File					save_location )
	{
		boolean	ok = false;
		
		DiskManagerFileInfoImpl[]	local_files = new DiskManagerFileInfoImpl[pm_files.length];
		

		try{
			TOTorrent	torrent = lws.getTOTorrent( false );
			
			if ( torrent.isSimpleTorrent()){
				
				save_location = save_location.getParentFile();
			}
		
			for (int i = 0; i < pm_files.length; i++) {
				
				DMPieceMapperFile pm_info = pm_files[i];
				
				File	relative_file = pm_info.getDataFile();
				
				long target_length = pm_info.getLength();
							
				DiskManagerFileInfoImpl	file_info = 
					new DiskManagerFileInfoImpl( 
											this, 
											new File( save_location, relative_file.toString()), 
											i,
											pm_info.getTorrentFile(),
											DiskManagerFileInfo.ST_LINEAR );
				
				local_files[i] = file_info;

				CacheFile	cache_file	= file_info.getCacheFile();
				File		data_file	= file_info.getFile(true);
	
				if ( !cache_file.exists()){
						
					throw( new Exception( "File '" + data_file + "' doesn't exist" ));
				}
				
				if ( cache_file.getLength() != target_length ){
				
					throw( new Exception( "File '" + data_file + "' doesn't exist" ));
				
				}
				
				pm_info.setFileInfo( file_info );
			}
		
			ok	= true;
			
			return( local_files );
			
		}catch( Throwable e ){
			
			setFailed( "getFiles failed - " + Debug.getNestedExceptionMessage( e ));
			
			return( null );
			
		}finally{
			
			if ( !ok ){
				
				for (int i=0;i<local_files.length;i++){
					
					if ( local_files[i] != null ){
						
						local_files[i].close();
					}
				}
			}
		}
	}
	                          	
	public void
	setPieceDone(
		DiskManagerPieceImpl    dmPiece,
		boolean                 done )
	{
	}
	
	public boolean
	stop(
		boolean	closing )
	{
		started = false;
		
		if ( reader != null ){
		
			reader.stop();
		
			reader = null;
		}
		
		if ( files != null ){
			
			for (int i=0;i<files.length;i++){
				
				try{
					files[i].getCacheFile().close();
					
				}catch( Throwable e ){
					
					e.printStackTrace();
				}
			}
		}

		return( false );
	}

    public boolean 
    isStopped() 
    {
    	return( !started );
    }
    
	public boolean
	filesExist()
	{
		throw( new RuntimeException( "filesExist not implemented" ));
	}


	public DiskManagerWriteRequest
	createWriteRequest(
		int 				pieceNumber,
		int 				offset,
		DirectByteBuffer 	data,
		Object 				user_data )
	{
		throw( new RuntimeException( "createWriteRequest not implemented" ));
	}
	
	public void 
	enqueueWriteRequest(
		DiskManagerWriteRequest			request,
		DiskManagerWriteRequestListener	listener )
	{
		throw( new RuntimeException( "enqueueWriteRequest not implemented" ));
	}

	public boolean
	hasOutstandingWriteRequestForPiece(
		int		piece_number )
	{
		throw( new RuntimeException( "hasOutstandingWriteRequestForPiece not implemented" ));
	}
	
	public boolean
	hasOutstandingReadRequestForPiece(
		int		piece_number )
	{
		throw( new RuntimeException( "hasOutstandingReadRequestForPiece not implemented" ));
	}
	
	public boolean
	hasOutstandingCheckRequestForPiece(
		int		piece_number )
	{
		throw( new RuntimeException( "hasOutstandingCheckRequestForPiece not implemented" ));
	}

	public DirectByteBuffer 
	readBlock(
		int pieceNumber, 
		int offset, 
		int length )
	{
		return( reader.readBlock( pieceNumber, offset, length ));
	}
	
	public DiskManagerReadRequest
	createReadRequest(
		int pieceNumber,
		int offset,
		int length )
	{
		return( reader.createReadRequest( pieceNumber, offset, length ));
	}
	
	public void 
	enqueueReadRequest( 
		DiskManagerReadRequest 			request, 
		DiskManagerReadRequestListener 	listener )
	{
		reader.readBlock( request, listener );
	}
	
	public DiskManagerCheckRequest
	createCheckRequest(
		int 		pieceNumber,
		Object		user_data )
	{
		DMChecker	checker = getChecker();
		
		return( checker.createCheckRequest( pieceNumber, user_data));
	}
	
	public void
	enqueueCheckRequest(
		DiskManagerCheckRequest			request,
		DiskManagerCheckRequestListener	listener )
	{
		DMChecker	checker = getChecker();

		checker.enqueueCheckRequest( request, listener );
	}
	
	public void
	enqueueCompleteRecheckRequest(
		DiskManagerCheckRequest			request,
		DiskManagerCheckRequestListener	listener )
	{
		throw( new RuntimeException( "enqueueCompleteRecheckRequest not implemented" ));
	}
	
	public void
	setPieceCheckingEnabled(
		boolean		enabled )
	{	
	}
	
	public void
    saveResumeData(
    	boolean interim_save )
	{	
	}

	public DiskManagerPiece[] 
	getPieces()
	{
		return( pieces );
	}

	public DiskManagerPiece
	getPiece(
		int	index )
	{
		return( pieces[index] );
	}
		
	public boolean
	isInteresting(
		int	piece_num )
	{
		return( false );
	}
	
	public boolean
	isDone(
		int	piece_num )
	{
		return( false );
	}
	
	public int 
	getNbPieces()
	{
		return( pieces.length );
	}
	
	public DiskManagerFileInfo[]
	getFiles()
	{
		return( files );
	}

	public DiskManagerFileInfoSet 
	getFileSet() 
	{
		throw( new RuntimeException( "getFileSet not implemented" ));
	}
	
	public int
	getState()
	{
		return( state );
	}
	
	public long
	getTotalLength()
	{
		return( piece_mapper.getTotalLength());
	}
	
	public int
	getPieceLength()
	{
		return( piece_mapper.getPieceLength());
	}
	
	public int 
	getPieceLength(
		int piece_number) 
	{
		if ( piece_number == pieces.length-1 ){
			
			return( piece_mapper.getLastPieceLength());
			
		}else{
			
			return( piece_mapper.getPieceLength());
		}
	}
	
	public int 
	getLastPieceLength()
	{
		return( piece_mapper.getLastPieceLength());
	}

	public long
	getRemaining()
	{
		return( 0 );
	}
	
	public long
	getRemainingExcludingDND()
	{
		return( 0 );
	}
	
	public int
	getPercentDone()
	{
		return( 100 );
	}
	
	public String
	getErrorMessage()
	{
		return( error_message );
	}
  
	public void
	downloadEnded()
	{
	}

	public void
	moveDataFiles(
		File	new_parent_dir )
	{
		throw( new RuntimeException( "moveDataFiles not implemented" ));
	}
	
	public void 
	moveDataFiles(
		File 	new_parent_dir, 
		String 	new_name )
	{
		throw( new RuntimeException( "moveDataFiles not implemented" ));
	}
	
	public int 
	getCompleteRecheckStatus()
	{
		return( -1 );
	}
	
	public boolean 
	checkBlockConsistencyForWrite(
		String				originator,
		int 				pieceNumber, 
		int 				offset, 
		DirectByteBuffer 	data )
	{
		long	pos = 
			pieceNumber * piece_mapper.getPieceLength() + offset + data.remaining( DirectByteBuffer.AL_EXTERNAL );
			
		return( pos <= piece_mapper.getTotalLength());
	}
	
	public boolean 
	checkBlockConsistencyForRead(
		String	originator,
	    boolean	peer_request,
		int 	pieceNumber, 
		int		offset, 
		int 	length )
	{
		return( DiskManagerUtil.checkBlockConsistencyForRead( this, originator, peer_request, pieceNumber, offset, length));
	}	
   
	public boolean 
	checkBlockConsistencyForHint(
		String	originator,
		int 	pieceNumber, 
		int		offset, 
		int 	length )
	{
		return( DiskManagerUtil.checkBlockConsistencyForHint( this, originator, pieceNumber, offset, length));
	}	
	
	public void
	addListener(
		DiskManagerListener	l )
	{
		
	}
	
	public void
	removeListener(
		DiskManagerListener	l )
	{	
	}
	
	public boolean
	hasListener(
		DiskManagerListener	l )
	{
		return( false );
	}
  
	public void 
	saveState()
	{	
	}
	
	public DiskAccessController
	getDiskAccessController()
	{
		return( disk_access_controller );
	}
	
	public DMPieceMap  
	getPieceMap()
	{
		DMPieceMap	map = piece_map_use_accessor;
		
		if ( map == null ){
				
			piece_map_use_accessor = map = piece_mapper.getPieceMap();			
		}
		
		return( map );
	}
	
	public DMPieceList
	getPieceList(
		int	piece_number )
	{
		DMPieceMap	map = getPieceMap();
		
		return( map.getPieceList( piece_number ));
	}
		
	
	protected DMChecker
	getChecker()
	{
		DMChecker	checker = checker_use_accessor;
		
		if ( checker == null ){
			
			checker = checker_use_accessor = DMAccessFactory.createChecker( this );
		}
		
		return( checker );
	}
	
	public byte[]
	getPieceHash(
		int	piece_number )
	
		throws TOTorrentException
	{
		return( lws.getTorrent().getPieces()[piece_number] );
	}
	
	public DiskManagerRecheckScheduler
	getRecheckScheduler()
	{
		throw( new RuntimeException( "getPieceHash not implemented" ));
	}
	
	public void
	downloadRemoved()
	{   
	}
	
	public void
	setFailed(
		String		reason )
	{
		started = false;
		
		state	= FAULTY;
		
		error_message	= reason;
	}
	
	public void
	setFailed(
		DiskManagerFileInfo		file,
		String					reason )
	{
		started = false;
		
		state	= FAULTY;
		
		error_message	= reason;
	}
	
	public long
	getAllocated()
	{
		return( 0 );
	}
	
	public void
	setAllocated(
		long		num )
	{
	}
	
	public void
	setPercentDone(
		int			num )
	{
	}
		
	public TOTorrent
	getTorrent()
	{
		return( lws.getTOTorrent( false ));
	}
	
	public String[]
  	getStorageTypes()
	{
		throw( new RuntimeException( "getStorageTypes not implemented" ));
	}
  	
	public String 
	getStorageType(
		int fileIndex) 
	{
		throw( new RuntimeException( "getStorageType not implemented" ));
	}
	
  	public void
  	accessModeChanged(
  		DiskManagerFileInfoImpl		file,
  		int							old_mode,
  		int							new_mode )
  	{
  	}
  	
  	public void
    skippedFileSetChanged(
  	   	DiskManagerFileInfo	file )
  	{
  	}
  	
  	public void 
  	priorityChanged(
  		DiskManagerFileInfo	file )
  	{	
  	}
  	
  	public File
  	getSaveLocation()
  	{
  		return( save_file );
  	}
  	
	public String
	getInternalName()
	{
		return( internal_name );
	}
	
	public DownloadManagerState
	getDownloadState()
	{
		return( download_state );
	}
	
	public void
	generateEvidence(
		IndentWriter		writer )
	{
	}
	
	protected static class
	sePiece
		implements DiskManagerPiece
	{
		public void			clearChecking(){throw( new RuntimeException( "clearChecking not implemented" ));}
		public boolean		isNeedsCheck(){throw( new RuntimeException( "isNeedsCheck not implemented" ));}
		public int			getLength(){throw( new RuntimeException( "getLength not implemented" ));}
		public int			getNbBlocks(){throw( new RuntimeException( "getNbBlocks not implemented" ));}
		public int			getPieceNumber(){throw( new RuntimeException( "getPieceNumber not implemented" ));}
		public int			getBlockSize(int b ){throw( new RuntimeException( "getBlockSize not implemented" ));}
		public boolean		isWritten(){throw( new RuntimeException( "isWritten not implemented" ));}
		public int			getNbWritten(){throw( new RuntimeException( "getNbWritten not implemented" ));}
		public boolean[]	getWritten(){throw( new RuntimeException( "getWritten not implemented" ));}
		public void			reDownloadBlock(int blockNumber){throw( new RuntimeException( "reDownloadBlock not implemented" ));}
		public void			reset(){throw( new RuntimeException( "reset not implemented" ));}
		public boolean		isDownloadable(){ return( false );}
		public void			setDownloadable(){throw( new RuntimeException( "setRequestable not implemented" ));}
		public DiskManager	getManager(){throw( new RuntimeException( "getManager not implemented" ));}
		public boolean		calcNeeded(){throw( new RuntimeException( "calcNeeded not implemented" ));}
		public void			clearNeeded(){throw( new RuntimeException( "clearNeeded not implemented" ));}
		public boolean		isNeeded(){throw( new RuntimeException( "isNeeded not implemented" ));}
		public void			setNeeded(){throw( new RuntimeException( "setNeeded not implemented" ));}
		public void			setNeeded(boolean b){throw( new RuntimeException( "setNeeded not implemented" ));}	
		public void			setWritten(int b){throw( new RuntimeException( "setWritten not implemented" ));}
		public boolean		isWritten(int blockNumber){throw( new RuntimeException( "isWritten not implemented" ));}
		public boolean		calcChecking(){throw( new RuntimeException( "calcChecking not implemented" ));}
		public boolean		isChecking(){return( false );}
		public void			setChecking(){throw( new RuntimeException( "setChecking not implemented" ));}
		public void			setChecking(boolean b){throw( new RuntimeException( "setChecking not implemented" ));}
		public boolean		calcDone(){throw( new RuntimeException( "calcDone not implemented" ));}
		public boolean		isDone(){ return( true );}
		public boolean isInteresting(){ return( false );}	
		public boolean		isSkipped(){ return false; }
		public String		getString(){ return( "" );}
		public short		getReadCount(){ return 0 ;}
		public void			setReadCount(short c){}

		public void			
		setDone(
			boolean 	b) 
		{
			// get here when doing delayed rechecks
			
			if ( !b ){
				
				Debug.out( "Piece failed recheck" );
			}
			
			//throw( new RuntimeException( "setDone not implemented" ));
		}
	}
}