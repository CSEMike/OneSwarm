/*
 * Created on Sep 27, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.core.diskmanager.cache.impl;

import java.io.File;

import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.cache.CacheFile;
import com.aelitis.azureus.core.diskmanager.cache.CacheFileManagerException;
import com.aelitis.azureus.core.diskmanager.file.FMFile;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerException;

public class
CacheFileWithoutCacheMT
	implements CacheFile
{
	private static final int MAX_CLONES	= 20;
	
	private static int	num_clones;
	private static int	max_clone_depth;
	
	private CacheFileManagerImpl		manager;
	private FMFile						base_file;
	private FMFile[]					files;
	private int[]						files_use_count;
	private TOTorrentFile				torrent_file;
	private boolean						moving;
	
	protected
	CacheFileWithoutCacheMT(
		CacheFileManagerImpl	_manager,
		FMFile					_file,
		TOTorrentFile			_torrent_file )
	{
		manager			= _manager;
		base_file		= _file;
		torrent_file	= _torrent_file;
		
		files = new FMFile[]{ base_file };
		
		files_use_count = new int[]{ 0 };
	}

	public TOTorrentFile
	getTorrentFile()
	{
		return( torrent_file );
	}
	
	public boolean
	exists()
	{
		return( base_file.exists());
	}
	
	public void
	moveFile(
		File		new_file )
	
		throws CacheFileManagerException
	{
		try{
			synchronized( this ){

				moving = true;
			}
			
			while( true ){
								
				synchronized( this ){
				
					boolean	surviving = false;

					for (int i=1;i<files_use_count.length;i++){
						
						if ( files_use_count[i] > 0 ){
							
							surviving = true;
							
							break;
						}
					}
				
					if ( !surviving ){
						
						for (int i=1;i<files_use_count.length;i++){
							
							FMFile file = files[i];
							
							if ( file.isClone()){
								
								// System.out.println( "Destroyed clone " + file.getName());
								
								synchronized( CacheFileWithoutCacheMT.class ){
									
									num_clones--;
								}
							}
							
							file.close();
						}
						
						files = new FMFile[]{ base_file };
						
						files_use_count = new int[]{ files_use_count[0] };
						
						base_file.moveFile( new_file );
	
						break;
					}
				}
				
				try{
					System.out.println( "CacheFileWithoutCacheMT: waiting for clones to die" );
					
					Thread.sleep(250);
					
				}catch( Throwable e ){
					
				}
			}
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
			
		}finally{
			
			synchronized( this ){

				moving = false;
			}
		}
	}
	
	public void
	renameFile(
		String		new_file )
	
		throws CacheFileManagerException
	{
		try{
			synchronized( this ){

				moving = true;
			}
			
			while( true ){
								
				synchronized( this ){
				
					boolean	surviving = false;

					for (int i=1;i<files_use_count.length;i++){
						
						if ( files_use_count[i] > 0 ){
							
							surviving = true;
							
							break;
						}
					}
				
					if ( !surviving ){
						
						for (int i=1;i<files_use_count.length;i++){
							
							FMFile file = files[i];
							
							if ( file.isClone()){
								
								// System.out.println( "Destroyed clone " + file.getName());
								
								synchronized( CacheFileWithoutCacheMT.class ){
									
									num_clones--;
								}
							}
							
							file.close();
						}
						
						files = new FMFile[]{ base_file };
						
						files_use_count = new int[]{ files_use_count[0] };
						
						base_file.renameFile( new_file );
	
						break;
					}
				}
				
				try{
					System.out.println( "CacheFileWithoutCacheMT: waiting for clones to die" );
					
					Thread.sleep(250);
					
				}catch( Throwable e ){
					
				}
			}
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
			
		}finally{
			
			synchronized( this ){

				moving = false;
			}
		}
	}
	
	
	
	public void
	setAccessMode(
		int		mode )
	
		throws CacheFileManagerException
	{
		try{
			synchronized( this ){
			
				for (int i=0;i<files.length;i++){
					
					files[i].setAccessMode( mode==CF_READ?FMFile.FM_READ:FMFile.FM_WRITE );
				}
			}
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}	
	}
	
	public int
	getAccessMode()
	{
		return( base_file.getAccessMode()==FMFile.FM_READ?CF_READ:CF_WRITE );
	}
	
	public void
	setStorageType(
		int		type )
	
		throws CacheFileManagerException
	{
		throw( new CacheFileManagerException( this, "Not Implemented" ));	
	}
	
	public int
	getStorageType()
	{
		return( CacheFileManagerImpl.convertFileToCacheType( base_file.getStorageType()));
	}

	public long
	getLength()
	
		throws CacheFileManagerException
	{
		try{
						
			return( base_file.exists() ? base_file.getLength() : 0);
			
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
			base_file.setLength( length );
			
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
			base_file.setPieceComplete( piece_number, piece_data );
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);
		}
	}
	
	protected FMFile
	getFile()
	
		throws CacheFileManagerException
	{
		synchronized( this ){
			
			if ( moving ){
				
				files_use_count[0]++;
				
				return( files[0] );
			}
			
			int	min_index	= -1;
			int	min			= Integer.MAX_VALUE;
			
			for (int i=0;i<files_use_count.length;i++){
				
				int	count = files_use_count[i];
				
				if ( count < min ){
					
					min			= count;
					min_index	= i;
				}
			}
			
			if ( min == 0 || files_use_count.length == MAX_CLONES ){
				
				files_use_count[min_index]++;
				
				return( files[min_index] );
			}
			
				// all files already in use
			
			try{
				FMFile clone = base_file.createClone();
								
				//System.out.println( "Created clone " + clone.getName());
				
				int	old_num	= files.length;
				int	new_num = old_num + 1;
				
				synchronized( CacheFileWithoutCacheMT.class ){
					
					num_clones++;
					
					if ( num_clones % 100 == 0 ){
						
						//System.out.println( "File clones=" + num_clones );
					}
					
					if ( new_num == MAX_CLONES || new_num > max_clone_depth ){
						
						max_clone_depth = new_num;
						
						//System.out.println( "Clone depth of " + new_num + " for " + clone.getName());
					}
				}

				FMFile[]	new_files 			= new FMFile[ new_num ];		
				int[]		new_files_use_count = new int[new_num];
				
				System.arraycopy(files, 0, new_files, 0, old_num );
				System.arraycopy(files_use_count, 0, new_files_use_count, 0, old_num );
				
				new_files[old_num]				= clone;
				new_files_use_count[old_num] 	= 1;
				
				files			= new_files;
				files_use_count	= new_files_use_count;
				
				return( clone );
				
			}catch( FMFileManagerException e ){
				
				manager.rethrow( this, e );
				
				return( null );
			}
		}
	}
	
	protected void
	releaseFile(
		FMFile	file )
	{
		synchronized( this ){

			for (int i=0;i<files_use_count.length;i++){

				if ( files[i] == file ){
					
					int count = files_use_count[i];
					
					if ( count > 0 ){
						
						count--;
					}
					
					files_use_count[i] = count;
					
					break;
				}
			}
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
		
		FMFile file = null;
		
		try{	
			file	= getFile();
			
			file.read( buffers, position );
			
			manager.fileBytesRead( read_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
			
		}finally{
			
			releaseFile( file );
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

		FMFile file = null;
		
		try{	
			file	= getFile();
			
			file.read( buffer, position );
			
			manager.fileBytesRead( read_length );

		}catch( FMFileManagerException e ){
				
			manager.rethrow(this,e);
			
		}finally{
			
			releaseFile( file );
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
			base_file.write( buffer, position );
			
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
			base_file.write( buffers, position );
			
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
			base_file.write( buffer, position );
			
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
			base_file.write( buffers, position );
			
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
			base_file.flush();
		
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
			synchronized( this ){
				
				for (int i=0;i<files.length;i++){
					
					FMFile file = files[i];
					
					if ( file.isClone()){
					
						// System.out.println( "Destroyed clone " + file.getName());
						
						synchronized( CacheFileWithoutCacheMT.class ){
							
							num_clones--;
						}
					}
					
					file.close();
				}
			}			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);			
		}
	}
	
	public boolean
	isOpen()
	{
		return( base_file.isOpen());
	}
	
	public void
	delete()
	
		throws CacheFileManagerException
	{
		try{
			
			base_file.delete();
			
		}catch( FMFileManagerException e ){
			
			manager.rethrow(this,e);			
		}
	}
}
