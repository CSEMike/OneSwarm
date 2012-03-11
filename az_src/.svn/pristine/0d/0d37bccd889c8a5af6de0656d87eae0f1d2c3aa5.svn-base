/*
 * Created on Jul 15, 2007
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


package org.gudy.azureus2.core3.disk.impl.piecemapper.impl;

import org.gudy.azureus2.core3.disk.impl.DiskManagerFileInfoImpl;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceList;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMap;
import org.gudy.azureus2.core3.disk.impl.piecemapper.DMPieceMapEntry;
import org.gudy.azureus2.core3.torrent.TOTorrent;

public class 
DMPieceMapSimple
	implements DMPieceMap
{
	final private int		piece_length;
	final private int		piece_count;
	final private int		last_piece_length;
	
	final private DiskManagerFileInfoImpl	file;
	
	protected
	DMPieceMapSimple(
		TOTorrent				torrent,
		DiskManagerFileInfoImpl	_file )
	{
		piece_length	= (int)torrent.getPieceLength();
		
		piece_count		= torrent.getNumberOfPieces();
		
		int lpl = (int)( torrent.getSize() % piece_length );
		
		if ( lpl == 0 ){
			
			lpl = piece_length;
		}
	
		last_piece_length = lpl;
		
		file	= _file;
	}
	
	public DMPieceList
	getPieceList(
		int	piece_number )
	{
		return( new pieceList( piece_number ));
	}
	
	protected class
	pieceList
		implements DMPieceList, DMPieceMapEntry
	{
		private int	piece_number;
		
		protected
		pieceList(
			int	_piece_number )
		{
			piece_number	= _piece_number;
		}
		
		public int 
		size()
		{
			return( 1 );
		}
		
		public DMPieceMapEntry 
		get(
			int index )
		{
			return( this );
		}
		
		public int 
		getCumulativeLengthToPiece(
			int file_index )
		{
			return( getLength());
		}
		
			// map entry
		
		public DiskManagerFileInfoImpl 
		getFile()
		{
			return( file );
		}

		public long 
		getOffset()
		{
			return(((long)piece_number) * piece_length );
		}

		public int
		getLength()
		{
			if ( piece_number == piece_count - 1 ){
				
				return( last_piece_length );
			}else{
				
				return( piece_length );
			}
		}
	}
}
