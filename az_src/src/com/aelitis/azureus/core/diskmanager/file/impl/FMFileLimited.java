/*
 * File    : FMFileManagerLimited.java
 * Created : 12-Feb-2004
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

package com.aelitis.azureus.core.diskmanager.file.impl;

/**
 * @author parg
 *
 */

//import java.nio.ByteBuffer;
import java.io.File;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

import com.aelitis.azureus.core.diskmanager.file.*;

public class 
FMFileLimited
	extends FMFileImpl
{
	protected
	FMFileLimited(
		FMFileOwner			_owner,
		FMFileManagerImpl	_manager,
		File				_file,
		int					_type )
	
		throws FMFileManagerException
	{
		super( _owner, _manager, _file, _type );
	}
	
	protected
	FMFileLimited(
		FMFileLimited	basis )
	
		throws FMFileManagerException
	{
		super( basis );
	}
	
	public FMFile
	createClone()
	
		throws FMFileManagerException
	{
		return( new FMFileLimited( this ));
	}
	
	public void
	ensureOpen(
		String	reason )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			if ( isOpen()){
			
				usedSlot();
			
			}else{
			
				getSlot();
			
				try{
	
				  super.ensureOpen( reason );
					
				}finally{
					
					if ( !isOpen()){
						
						releaseSlot();
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	getSlot()
	{
		getManager().getSlot(this);
	}
	
	protected void
	releaseSlot()
	{
		getManager().releaseSlot(this);
	}
	
	protected void
	usedSlot()
	{	
		getManager().usedSlot(this);
	}
		
	public void
	setAccessMode(
		int		mode )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			if ( mode != getAccessMode()){
		
				close(false);
			}
		
			setAccessModeSupport( mode );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public long
	getLength()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:getLength" );
		
			return( getLengthSupport());
			
		}finally{
			
			this_mon.exit();
		}
	}

	public void
	setLength(
		long		length )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:setLength" );
			
			setLengthSupport( length );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	setPieceComplete(
		int					piece_number,
		DirectByteBuffer	piece_data )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			if ( isPieceCompleteProcessingNeeded( piece_number )){
		
				ensureOpen( "FMFileLimited:setPieceComplete" );

				boolean	switched_mode = false;
				
				if ( getAccessMode() != FM_WRITE ){
					
					setAccessMode( FM_WRITE );
					
					switched_mode = true;
					
						// switching mode closes the file...
					
					ensureOpen( "FMFileLimited:setPieceComplete2" );
				}
				
				try{
				
					setPieceCompleteSupport( piece_number, piece_data );
					
				}finally{
					
					if ( switched_mode ){
						
						setAccessMode( FM_READ );
					}
				}
			}
		}finally{
			
			this_mon.exit();
		}	
	}
	
	public void
	read(
		DirectByteBuffer[]	buffers,
		long				offset )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:read" );
			
			readSupport( buffers, offset );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	read(
		DirectByteBuffer	buffer,
		long		offset )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:read" );
			
			readSupport( buffer, offset );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	
	public void
	write(
		DirectByteBuffer	buffer,
		long		position )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:write" );
			
			writeSupport( buffer, position );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	write(
		DirectByteBuffer[]	buffers,
		long				position )
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();
		
			ensureOpen( "FMFileLimited:write" );
			
			writeSupport( buffers, position );
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public void
	close()
	
		throws FMFileManagerException
	{
		try{
			this_mon.enter();		

			close(true);
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	close(
		boolean	explicit )
	
		throws FMFileManagerException
	{	
		try{
			this_mon.enter();
		
			boolean	was_open = isOpen();
		
			try{
				closeSupport( explicit );
				
			}finally{
	
				if ( was_open ){
					
					releaseSlot();
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
}
