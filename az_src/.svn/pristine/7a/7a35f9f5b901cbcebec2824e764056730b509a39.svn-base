/*
 * File    : DiskManagerFileInfoImpl.java
 * Created : 18-Oct-2003
 * By      : Olivier
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

package org.gudy.azureus2.core3.disk.impl;
/*
 * Created on 3 juil. 2003
 *
 */
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.*;

import com.aelitis.azureus.core.diskmanager.cache.CacheFile;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerException;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerFactory;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileOwner;
import com.aelitis.azureus.core.util.CopyOnWriteList;

/**
 * @author Olivier
 * 
 */
public class 
DiskManagerFileInfoImpl
	implements DiskManagerFileInfo, CacheFileOwner
{
  private File			file;
  private int			file_index;
  private CacheFile		cache_file;
  
  private String 		extension;
  private long 			downloaded;
  
  private DiskManagerHelper 	diskManager;
  private TOTorrentFile			torrent_file;
  
  private int 		priority 	= 0;
  
  protected boolean 	skipped 	= false;
  
  private CopyOnWriteList	listeners;
  
  public
  DiskManagerFileInfoImpl(
	DiskManagerHelper	_disk_manager,
  	File				_file,
  	int					_file_index,
	TOTorrentFile		_torrent_file,
	int					_storage_type )
  
  	throws CacheFileManagerException
  {
    diskManager 	= _disk_manager;
    torrent_file	= _torrent_file;
  	
    file		= _file;
    file_index	= _file_index;
    
    int	cache_st = DiskManagerUtil.convertDMStorageTypeToCache( _storage_type );
    
  	cache_file = CacheFileManagerFactory.getSingleton().createFile( this, _file, cache_st);
  	
  	if ( cache_st == CacheFile.CT_COMPACT || cache_st == CacheFile.CT_PIECE_REORDER_COMPACT ){
  		
  		skipped = true;
  	}
  }
  
  	public String
  	getCacheFileOwnerName()
  	{
  		return( diskManager.getInternalName());
  	}
  	
	public TOTorrentFile
	getCacheFileTorrentFile()
	{
		return( torrent_file );
	}
	
	public File 
	getCacheFileControlFileDir() 
	{
		return( diskManager.getDownloadState().getStateFile( ));
	}
	
	public int
	getCacheMode()
	{
		return( diskManager.getCacheMode());
	}
	
  public void
  flushCache()
	
	throws	Exception
  {
  	cache_file.flushCache();
  }
  
  public void 
  moveFile(
  	File	newFile,
  	boolean	link_only )
  
  	throws CacheFileManagerException
  {
	  if ( !link_only ){
		  
		  cache_file.moveFile( newFile );
	  }
	  
	  file	= newFile;
  }
  
  public void 
  renameFile(
  	String	new_name,
  	boolean	link_only )
  
  	throws CacheFileManagerException
  {
	  if ( !link_only ){
		  
		  cache_file.renameFile( new_name );
	  }
	  
	  file	= new File( file.getParentFile(), new_name );
  }
  
  public CacheFile
  getCacheFile()
  {
  	return( cache_file );
  }
  
  public void
  setAccessMode(
  	int		mode )
  
  	throws CacheFileManagerException
  {
	int	old_mode =  cache_file.getAccessMode();
	
  	cache_file.setAccessMode( mode==DiskManagerFileInfo.READ?CacheFile.CF_READ:CacheFile.CF_WRITE );
  	
  	if ( old_mode != mode ){
  		
  		diskManager.accessModeChanged( this, old_mode, mode );
  	}
  }
  
  public int 
  getAccessMode()
  {
  	int	mode = cache_file.getAccessMode();
  	
	return( mode == CacheFile.CF_READ?DiskManagerFileInfo.READ:DiskManagerFileInfo.WRITE);
  }

  /**
   * @return
   */
  public long getDownloaded() {
	return downloaded;
  }

  /**
   * @return
   */
  public String getExtension() {
	return extension;
  }

  /**
   * @return
   */
  public File 
  getFile(
	boolean	follow_link )
  	{
	  if ( follow_link ){
	  
		  File	res = getLink();
	  
		  if ( res != null ){
		
			  return( res );
		  }
	  }
	  
	  return( file );
  	}

  	public TOTorrentFile
	getTorrentFile()
	{
		return( torrent_file );
	}
	
	public boolean
	setLink(
		File	link_destination )
	{
		Debug.out( "setLink: download must be stopped" );
		
		return( false );
	}

	public boolean
	setLinkAtomic(
		File	link_destination )
	{
		Debug.out( "setLink: download must be stopped" );
		
		return( false );
	}
	
	public File
	getLink()
	{
		return( diskManager.getDownloadState().getFileLink( getFile( false )));
	}
	
	public boolean setStorageType(int type) {
		DiskManagerFileInfoSet set = diskManager.getFileSet();
		boolean[] toSet = new boolean[set.nbFiles()];
		toSet[file_index] = true;
		return set.setStorageTypes(toSet, type)[file_index];
	}
	
	public int
	getStorageType()
	{
		return( DiskManagerUtil.convertDMStorageTypeFromString( diskManager.getStorageType(file_index)));
	}
	
	protected boolean
	isLinked()
	{
		return( getLink() != null );
	}
	
  /**
   * @return
   */
  public int getFirstPieceNumber() {
    return torrent_file.getFirstPieceNumber();
  }
  
  
  public int getLastPieceNumber() {
    return torrent_file.getLastPieceNumber();
  }

  /**
   * @return
   */
  public long getLength() {
	return torrent_file.getLength();
  }

	public int	
	getIndex()
	{
		return( file_index );
	}
  /**
   * @return
   */
  public int getNbPieces() {
	return torrent_file.getNumberOfPieces();
  }


  /**
   * @param l
   */
  public void setDownloaded(long l) {
	downloaded = l;
  }

  /**
   * @param string
   */
  public void setExtension(String string) {
	extension = StringInterner.intern(string);
  }

  /**
   * @return
   */
  public int getPriority() {
	return priority;
  }

  /**
   * @param b
   */
  public void setPriority(int b) {
	priority = b;
	diskManager.priorityChanged( this );
  }

  /**
   * @return
   */
  public boolean isSkipped() {
	return skipped;
  }

  /**
   * @param skipped
   */
  public void setSkipped(boolean _skipped) {
	  
	int	existing_st = getStorageType();
	
	  // currently a non-skipped file must be linear
	
	if ( !_skipped && existing_st == ST_COMPACT ){
		if ( !setStorageType( ST_LINEAR )){
			return;
		}
	}
	
	if ( !_skipped && existing_st == ST_REORDER_COMPACT ){
		if ( !setStorageType( ST_REORDER )){
			return;
		}
	}
	
	skipped = _skipped;
	diskManager.skippedFileSetChanged( this );
	if(!_skipped)
	{
		boolean[] toCheck = new boolean[diskManager.getFileSet().nbFiles()];
		toCheck[file_index] = true;
		DiskManagerUtil.doFileExistenceChecks(diskManager.getFileSet(), toCheck, diskManager.getDownloadState().getDownloadManager(), true);                			
	}
  }

  public DiskManager getDiskManager() {
    return diskManager;
  }
  
  public DownloadManager	getDownloadManager()
  {
	  DownloadManagerState	state = diskManager.getDownloadState();
	  
	  if ( state == null ){
		  return( null );
	  }
	  
	  return( state.getDownloadManager());
  }
  
  	public void
  	dataWritten(
  		long		offset,
  		long		size )
  	{
  		if ( listeners != null ){
  			
  			Iterator	it = listeners.iterator();
  			
  			while( it.hasNext()){
  				
  				try{
  					((DiskManagerFileInfoListener)it.next()).dataWritten( offset, size );
  					
  				}catch( Throwable e ){
  					
  					Debug.printStackTrace(e);
  				}
  			}
  		}
  	}
  
  	public void
  	dataChecked(
  		long		offset,
  		long		size )
  	{
  		if ( listeners != null ){
  			
  			Iterator	it = listeners.iterator();
  			
  			while( it.hasNext()){
  				
  				try{
  					((DiskManagerFileInfoListener)it.next()).dataChecked( offset, size );
  					
  				}catch( Throwable e ){
  					
  					Debug.printStackTrace(e);
  				}
  			}
  		}
  	}
  	
	public DirectByteBuffer
	read(
		long	offset,
		int		length )
	
		throws IOException
	{
		DirectByteBuffer	buffer = 
			DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_DM_READ, length );
		
		try{
			cache_file.read( buffer, offset, CacheFile.CP_READ_CACHE );
			
		}catch( Throwable e ){
			
			buffer.returnToPool();
			
			Debug.printStackTrace(e);
			
			throw( new IOException( e.getMessage()));
		}
		
		return( buffer );	
	}
			
	public void
	close()
	{
		// this doesn't need to do anything as overall closure is handled by the disk manager closing
	}
	
	public void
	addListener(
		final DiskManagerFileInfoListener	listener )
	{
		if ( listeners == null ){
			
			listeners = new CopyOnWriteList();
		}
		
		synchronized( listeners ){
			
			if ( listeners.getList().contains( listener )){
				
				return;
			}
		}
		
		listeners.add( listener );
		
		new Runnable()
		{
			private long	file_start;
			private long	file_end;

			private long	current_write_start  	= -1;
			private long	current_write_end		= -1;
			private long	current_check_start  	= -1;
			private long	current_check_end		= -1;

			public void
			run()
			{
				TOTorrentFile[]	tfs = torrent_file.getTorrent().getFiles();
				
				long	torrent_offset = 0;
				
				for (int i=0;i<file_index;i++){
					
					torrent_offset += tfs[i].getLength();
				}
				
				file_start 	= torrent_offset;
				file_end	= file_start + torrent_file.getLength();
					
				DiskManagerPiece[]	pieces = diskManager.getPieces();
				
				int	first_piece = getFirstPieceNumber();
				int last_piece	= getLastPieceNumber();
				long	piece_size	= torrent_file.getTorrent().getPieceLength();
							
				for (int i=first_piece;i<=last_piece;i++){
				
					long	piece_offset = piece_size * i;
					
					DiskManagerPiece	piece = pieces[i];
					
					if ( piece.isDone()){
						
						long	bit_start 	= piece_offset;
						long	bit_end		= bit_start + piece.getLength();
						
						bitWritten( bit_start, bit_end, true );
						
					}else{
						
						int	block_offset = 0;
						
						for (int j=0;j<piece.getNbBlocks();j++){
							
							int	block_size = piece.getBlockSize(j);
							
							if ( piece.isWritten(j)){
								
								long	bit_start 	= piece_offset + block_offset;
								long	bit_end		= bit_start + block_size;
								
								bitWritten( bit_start, bit_end, false );
							}
							
							block_offset += block_size;
						}
					}
				}
				
				bitWritten( -1, -1, false );
			}
			
			protected void
			bitWritten(
				long	bit_start,
				long	bit_end,
				boolean	checked )
			{
				if ( current_write_start == -1 ){
					
					current_write_start	= bit_start;
					current_write_end	= bit_end;
					
				}else if ( current_write_end == bit_start ){
					
					current_write_end = bit_end;
					
				}else{
					
					if ( current_write_start < file_start ){
						
						current_write_start  = file_start;
					}
					
					if ( current_write_end > file_end ){
						
						current_write_end	= file_end;
					}
					
					if ( current_write_start < current_write_end ){
						
						try{
							listener.dataWritten( current_write_start-file_start, current_write_end-current_write_start );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
					
					current_write_start	= bit_start;
					current_write_end	= bit_end;
				}
				
					// checked case
				
				if ( checked && current_check_start == -1 ){
					
					current_check_start	= bit_start;
					current_check_end	= bit_end;
					
				}else if ( checked && current_check_end == bit_start ){
					
					current_check_end = bit_end;
					
				}else{
					
					if ( current_check_start < file_start ){
						
						current_check_start  = file_start;
					}
					
					if ( current_check_end > file_end ){
						
						current_check_end	= file_end;
					}
					
					if ( current_check_start < current_check_end ){
						
						try{
							listener.dataChecked( current_check_start-file_start, current_check_end-current_check_start );
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
					
					if ( checked ){
						current_check_start	= bit_start;
						current_check_end	= bit_end;
					}else{
						current_check_start	= -1;
						current_check_end	= -1;
					}
				}
			}
		}.run();
	}
	

	public void
	removeListener(
		DiskManagerFileInfoListener	listener )
	{	
		listeners.remove( listener );
	}
}
