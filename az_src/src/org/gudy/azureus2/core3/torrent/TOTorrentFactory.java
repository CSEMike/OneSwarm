/*
 * File    : TOTorrentFactory.java
 * Created : 5 Oct. 2003
 * By      : Parg 
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

package org.gudy.azureus2.core3.torrent;


import java.util.*;
import java.io.*;
import java.net.*;

import org.gudy.azureus2.core3.torrent.impl.*;

public class 
TOTorrentFactory 
{	
	public static final long	TO_DEFAULT_FIXED_PIECE_SIZE = 256*1024;
	
	public static final long	TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN = 32*1024;
	public static final long	TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX = 2*1024*1024;
	
	public static final long	TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER = 1024;
	public static final long	TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER = 2048;
	
	
	public static final long[]	STANDARD_PIECE_SIZES = { 32*1024, 48*1024, 64*1024, 96*1024, 
														 128*1024, 192*1024, 256*1024, 384*1024,
														 512*1024, 768*1024, 1024*1024,
														 1536*1024, 2*1024*1024, 3*1024*1024, 4*1024*1024 };
	
		// deserialisation methods
		
	public static TOTorrent
	deserialiseFromBEncodedFile(
		File		file )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( file ));
	}
	
		/**
		 * WARNING - take care if you use this that the data you're creating the torrent from doesn't contain
		 * unwanted attributes in it (e.g. "torrent filename"). You should almost definitely be using 
		 * TorrentUtils.deserialiseFromBEncodedInputStream
		 * @param is
		 * @return
		 * @throws TOTorrentException
		 */
	
	public static TOTorrent
	deserialiseFromBEncodedInputStream(
		InputStream		is )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( is ));
	}
    
    public static TOTorrent
    deserialiseFromBEncodedByteArray(
        byte[]      bytes )
        
        throws TOTorrentException
    {
        return( new TOTorrentDeserialiseImpl( bytes ));
    }
	
	public static TOTorrent
	deserialiseFromMap(
		Map			data )
		
		throws TOTorrentException
	{
		return( new TOTorrentDeserialiseImpl( data ));
	}
	
	public static TOTorrent
	deserialiseFromXMLFile(
		File		file )
		
		throws TOTorrentException
	{
		return( new TOTorrentXMLDeserialiser().deserialise( file ));
	}

		// construction methods: fixed piece size

	public static TOTorrentCreator
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url )
	
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, false, TO_DEFAULT_FIXED_PIECE_SIZE ));
	}
	
	public static TOTorrentCreator
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		boolean						add_hashes )
	
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, add_hashes, TO_DEFAULT_FIXED_PIECE_SIZE ));
	}	
	
	public static TOTorrentCreator
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_length )
		
		throws TOTorrentException
	{
		return( createFromFileOrDirWithFixedPieceLength( file, announce_url, false, piece_length ));
	}
	
	public static TOTorrentCreator
	createFromFileOrDirWithFixedPieceLength(
		File						file,
		URL							announce_url,
		boolean						add_hashes,
		long						piece_length )
	
		throws TOTorrentException
	{
		return( TOTorrentCreateImpl.create( file, announce_url, add_hashes, piece_length ));
	}
	
		// construction methods: variable piece size	

	public static TOTorrentCreator
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url )
	
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength( file, announce_url, false ));
	}
	
	public static TOTorrentCreator
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		boolean						add_hashes )
	
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength( 
					file, 
					announce_url,
					add_hashes,
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN, 
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX,
					TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER,
					TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER ));
					
	}	
	
	public static TOTorrentCreator
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		long						piece_min_size,
		long						piece_max_size,
		long						piece_num_lower,
		long						piece_num_upper )
			
		throws TOTorrentException
	{
		return( createFromFileOrDirWithComputedPieceLength(
						file, announce_url, false, piece_min_size, piece_max_size,
						piece_num_lower, piece_num_upper ));

	}
	
	public static TOTorrentCreator
	createFromFileOrDirWithComputedPieceLength(
		File						file,
		URL							announce_url,
		boolean						add_hashes,
		long						piece_min_size,
		long						piece_max_size,
		long						piece_num_lower,
		long						piece_num_upper )
	
		throws TOTorrentException
	{
		return( TOTorrentCreateImpl.create(
					file, announce_url, add_hashes, piece_min_size, piece_max_size,
					piece_num_lower, piece_num_upper ));

	}
	
	public static long
	getTorrentDataSizeFromFileOrDir(
		File			file_or_dir )
	{
		return( TOTorrentCreateImpl.getTorrentDataSizeFromFileOrDir( file_or_dir ));
	}	
	
	public static long
	getComputedPieceSize(
		long 	data_size )
	{
		return( TOTorrentCreateImpl.getComputedPieceSize( 
					data_size,
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MIN, 
					TO_DEFAULT_VARIABLE_PIECE_SIZE_MAX,
					TO_DEFAULT_VARIABLE_PIECE_NUM_LOWER,
					TO_DEFAULT_VARIABLE_PIECE_NUM_UPPER	));
	}
	
	public static long
	getPieceCount(
		long		total_size,
		long		piece_size )
	{
		return( TOTorrentCreateImpl.getPieceCount( total_size, piece_size ));
	}
}
