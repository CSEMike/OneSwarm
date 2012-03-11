/*
 * Created on 02-Nov-2004
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

package com.aelitis.azureus.core.diskmanager.cache.impl;

/**
 * @author parg
 *
 */

import java.io.File;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.FMFile;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class 
CacheFileWithoutCache
	implements CacheFile
{
	protected CacheFileManagerImpl		manager;
	protected FMFile					file;
	protected TOTorrentFile				torrent_file;

	protected
	CacheFileWithoutCache(
		CacheFileManagerImpl	_manager,
		FMFile					_file,
		TOTorrentFile			_torrent_file )
	{
		manager			= _manager;
		file			= _file;
		torrent_file	= _torrent_file;
		// System.out.println( "without cache = " + file.getFile().toString());
	}

	public TOTorrentFile
	getTorrentFile()
	{
		return( torrent_file );
	}
	
	public boolean
	exists()
	{
		return( file.exists());
	}
	
	public void
	moveFile(
		File		new_file )
	
		throws CacheFileManagerException
	{
		try{
			file.moveFile( new_file );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}	
	}
	
	public void
	renameFile(
		String		new_file )
	
		throws CacheFileManagerException
	{
		try{
			file.renameFile( new_file );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}	
	}
	
	public void
	setAccessMode(
		int		mode )
	
		throws CacheFileManagerException
	{
		try{
			
			file.setAccessMode( mode==CF_READ?FMFile.FM_READ:FMFile.FM_WRITE );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}	
	}
	
	public int
	getAccessMode()
	{
		return( file.getAccessMode()==FMFile.FM_READ?CF_READ:CF_WRITE );
	}
	
	public void
	setStorageType(
		int		type )
	
		throws CacheFileManagerException
	{
		try{
			
			file.setStorageType( CacheFileManagerImpl.convertCacheToFileType( type ));
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}			
	}
	
	public int
	getStorageType()
	{
		return( CacheFileManagerImpl.convertFileToCacheType( file.getStorageType()));
	}

	public long
	getLength()
	
		throws CacheFileManagerException
	{
		try{
						
			return( file.exists() ? file.getLength() : 0);
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
			
			return( 0 );
		}
	}
	
	public long
	compareLength(
		long	compare_to )
	
		throws CacheFileManagerException
	{
		return( getLength() - compare_to );
	}
	
	public void
	setLength(
		long		length )
	
		throws CacheFileManagerException
	{
		try{
						
			file.setLength( length );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}
	}
	
	public void
	setPieceComplete(
		int					piece_number,
		DirectByteBuffer	piece_data )
	
		throws CacheFileManagerException
	{
		try{
			file.setPieceComplete( piece_number, piece_data );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}
	}
	
	public void
	read(
		DirectByteBuffer[]	buffers,
		long				position,
		short				policy )
	
		throws CacheFileManagerException
	{
		int	read_length	= 0;
		
		for (int i=0;i<buffers.length;i++){
			
			read_length += buffers[i].remaining(DirectByteBuffer.SS_CACHE);
		}
		
		try{			
			file.read( buffers, position );
			
			manager.fileBytesRead( read_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
		}
	}
	
	public void
	read(
		DirectByteBuffer	buffer,
		long				position,
		short				policy )
	
		throws CacheFileManagerException
	{
		int	read_length	= buffer.remaining(DirectByteBuffer.SS_CACHE);

		try{			
			file.read( buffer, position );
			
			manager.fileBytesRead( read_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
		}
	}
	
	public void
	write(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		int	write_length = buffer.remaining(DirectByteBuffer.SS_CACHE);
		
		try{			
			file.write( buffer, position );
			
			manager.fileBytesWritten( write_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
		}
	}
	
	public void
	write(
		DirectByteBuffer[]	buffers,
		long				position )
	
		throws CacheFileManagerException
	{
		int	write_length	= 0;
		
		for (int i=0;i<buffers.length;i++){
			
			write_length += buffers[i].remaining(DirectByteBuffer.SS_CACHE);
		}
		
		try{			
			file.write( buffers, position );
			
			manager.fileBytesWritten( write_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
		}
	}
	
	public void
	writeAndHandoverBuffer(
		DirectByteBuffer	buffer,
		long				position )
	
		throws CacheFileManagerException
	{
		int	write_length = buffer.remaining(DirectByteBuffer.SS_CACHE);
		
		boolean	write_ok	= false;
		
		try{			
			file.write( buffer, position );
			
			manager.fileBytesWritten( write_length );

			write_ok	= true;
			
		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
			
		}finally{
			
			if ( write_ok ){
				
				buffer.returnToPool();
			}
		}
	}
	
	public void
	writeAndHandoverBuffers(
		DirectByteBuffer[]	buffers,
		long				position )
	
		throws CacheFileManagerException
	{
		int	write_length	= 0;
		
		for (int i=0;i<buffers.length;i++){
			
			write_length += buffers[i].remaining(DirectByteBuffer.SS_CACHE);
		}
		
		boolean	write_ok	= false;
		
		try{			
			file.write( buffers, position );
			
			manager.fileBytesWritten( write_length );

			write_ok	= true;
			
		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
			
		}finally{
			
			if ( write_ok ){
				
				for (int i=0;i<buffers.length;i++){

					buffers[i].returnToPool();
				}
			}
		}
	}
	
	public void
	flushCache()
	
		throws CacheFileManagerException
	{
		try{
			file.flush();
		
		}catch( FMFileManagerException e ){
		
			manager.rethrow(this,e);
		}
	}
	
	public void
	clearCache()
	
		throws CacheFileManagerException
	{
	}
	
	public void
	close()
	
		throws CacheFileManagerException
	{
		try{
			
			file.close();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);			
		}
	}
	
	public boolean
	isOpen()
	{
		return( file.isOpen());
	}
	
	public void
	delete()
	
		throws CacheFileManagerException
	{
		try{
			
			file.delete();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);			
		}
	}
}
