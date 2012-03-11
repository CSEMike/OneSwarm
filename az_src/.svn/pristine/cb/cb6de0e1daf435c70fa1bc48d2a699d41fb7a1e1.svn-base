/*
 * Created on 09-May-2005
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

package org.gudy.azureus2.pluginsimpl.remote.disk;

import java.io.File;

import org.gudy.azureus2.plugins.disk.DiskManagerChannel;
import org.gudy.azureus2.plugins.disk.DiskManagerFileInfo;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.download.DownloadException;
import org.gudy.azureus2.pluginsimpl.remote.RPException;
import org.gudy.azureus2.pluginsimpl.remote.RPObject;
import org.gudy.azureus2.pluginsimpl.remote.RPReply;
import org.gudy.azureus2.pluginsimpl.remote.RPRequest;

public class 
RPDiskManagerFileInfo 
	extends		RPObject
	implements 	DiskManagerFileInfo 
{
	protected transient DiskManagerFileInfo		delegate;

		// don't change these field names as they are visible on XML serialisation

	public int			access_mode;
	public long			downloaded;
	public long			length;
	public File			file;
	public int			first_piece_number;
	public int			num_pieces;
	public boolean		is_priority;
	public boolean		is_skipped;
	
	public static RPDiskManagerFileInfo
	create(
		DiskManagerFileInfo		_delegate )
	{
		RPDiskManagerFileInfo	res =(RPDiskManagerFileInfo)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPDiskManagerFileInfo( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPDiskManagerFileInfo(
		DiskManagerFileInfo		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (DiskManagerFileInfo)_delegate;
		
		access_mode				= delegate.getAccessMode();
		downloaded				= delegate.getDownloaded();
		length					= delegate.getLength();
		file					= delegate.getFile();
		first_piece_number		= delegate.getFirstPieceNumber();
		num_pieces				= delegate.getNumPieces();
		is_priority				= delegate.isPriority();
		is_skipped				= delegate.isSkipped();
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();	

		throw( new RPException( "Unknown method: " + method ));
	}
	
	
		// ***************************************************
	
	public void 
	setPriority(boolean b)
	{
		notSupported();
	}
	
	public void setSkipped(boolean b)
	{
		notSupported();
	}
	
	public int 
	getNumericPriorty() 
	{
		notSupported();
		
		return( 0 );
	}
	
	public void 
	setNumericPriority(
		int priority) 
	{
		notSupported();
	}
	
	public void
	setDeleted(boolean b)
	{
		notSupported();
	}
	public boolean
	isDeleted()
	{
		notSupported();
		
		return( false );
	}
	
	public int getAccessMode()
	{
		return( access_mode );
	}
	
	public long getDownloaded()
	{
		return( downloaded );
	}
	
	public long getLength() 
	{
		return( length );
	}
	
	public File getFile()
	{
		return( file );
	}
		
	public File
	getFile(
		boolean	follow_link )
	{
		if ( follow_link ){
			
			notSupported();
		}

		return( file );
	}		
	
	public int getFirstPieceNumber()
	{
		return( first_piece_number );
	}
	
	public long getPieceSize()
	{
		notSupported();
		
		return(-1);
	}
	
	public int getNumPieces()
	{
		return( num_pieces );
	}
		
	public boolean isPriority()
	{
		return( is_priority );
	}
	
	public boolean isSkipped()
	{
		return( is_skipped );
	}
	
	public int
	getIndex()
	{
		notSupported();
		
		return( -1 );
	}
	
	public void
	setLink(
		File	link_destination )
	{
		notSupported();
	}
	
	public File
	getLink()
	{
		notSupported();
		
		return( null );
	}
	
	public byte[] 
	getDownloadHash()
    {
		notSupported();
		
		return( null );
    }
	
	public Download 
	getDownload()
         throws DownloadException
    {
		notSupported();
		
		return( null );
    }
	
	public DiskManagerChannel
	createChannel()
	{
		notSupported();
		
		return( null );		
	}
}
