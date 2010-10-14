/*
 * File    : TOTorrentCreateImpl.java
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

package org.gudy.azureus2.core3.torrent.impl;

import java.io.*;
import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TOTorrentCreateImpl
	extends		TOTorrentImpl
	implements	TOTorrentFileHasherListener
{	
	protected File							torrent_base;
	protected long							piece_length;
	
	protected TOTorrentFileHasher			file_hasher;
	
	protected long	total_file_size		= -1;
	protected long	total_file_count	= 0;
	
	protected long							piece_count;
	protected boolean						add_other_hashes;
	
	protected List							progress_listeners = new ArrayList();
	
	protected int	reported_progress;
		
	protected Set	ignore_set = new HashSet();
	
	protected boolean	cancelled;
	
	public static TOTorrentCreator
	create(
		File						_torrent_base,
		URL							_announce_url,
		boolean						_add_other_hashes,
		long						_piece_length )
		
		throws TOTorrentException
	{
		TOTorrentCreateImpl	t = 
			new TOTorrentCreateImpl(
					_torrent_base,
					_announce_url,
					_add_other_hashes,
					_piece_length );
		
		return( new TOTorrentCreatorImpl( t ));
	}
	
	public static TOTorrentCreator
	create(	
		File						_torrent_base,
		URL							_announce_url,
		boolean						_add_other_hashes,
		long						_piece_min_size,
		long						_piece_max_size,
		long						_piece_num_lower,
		long						_piece_num_upper )
	
		throws TOTorrentException
	{
		TOTorrentCreateImpl	t = 
			new TOTorrentCreateImpl(
					_torrent_base,
					_announce_url,
					_add_other_hashes,
					_piece_min_size,
					_piece_max_size,
					_piece_num_lower,
					_piece_num_upper );
		
		return( new TOTorrentCreatorImpl( t ));
	}
	
	
	protected
	TOTorrentCreateImpl(
		File						_torrent_base,
		URL							_announce_url,
		boolean						_add_other_hashes,
		long						_piece_length )
		
		throws TOTorrentException
	{
		super( _torrent_base.getName(), _announce_url, _torrent_base.isFile());
			
		torrent_base		= _torrent_base;
		piece_length		= _piece_length;
		add_other_hashes	= _add_other_hashes;
	}
	
	protected
	TOTorrentCreateImpl(	
		File						_torrent_base,
		URL							_announce_url,
		boolean						_add_other_hashes,
		long						_piece_min_size,
		long						_piece_max_size,
		long						_piece_num_lower,
		long						_piece_num_upper )
		
		throws TOTorrentException
	{
		super( _torrent_base.getName(), _announce_url, _torrent_base.isFile());
		
		torrent_base		= _torrent_base;
		add_other_hashes	= _add_other_hashes;
		
		long	total_size = calculateTotalFileSize( _torrent_base );
		
		piece_length = getComputedPieceSize( total_size, _piece_min_size, _piece_max_size, _piece_num_lower, _piece_num_upper );
	}
	
	protected void
	create()
	
		throws TOTorrentException
	{
		constructFixed( torrent_base, piece_length );
	}
	
	protected void
	constructFixed(
		File		_torrent_base,
		long		_piece_length )
	
		throws TOTorrentException
	{
		setIgnoreList();

		setCreationDate( SystemTime.getCurrentTime() / 1000);
		
		setCreatedBy( Constants.AZUREUS_NAME + "/" + Constants.AZUREUS_VERSION );
		
		setPieceLength( _piece_length );
		
		report( "Torrent.create.progress.piecelength", _piece_length );
		
		piece_count = calculateNumberOfPieces( _torrent_base,_piece_length );
		
		if ( piece_count == 0 ){
			
			throw( new TOTorrentException( "TOTorrentCreate: specified files have zero total length",
											TOTorrentException.RT_ZERO_LENGTH ));
		}
		
		report( "Torrent.create.progress.hashing");

		for (int i=0;i<progress_listeners.size();i++){
					
			((TOTorrentProgressListener)progress_listeners.get(i)).reportProgress( 0 );
		}
			
		boolean add_other_per_file_hashes 	= add_other_hashes&&!getSimpleTorrent();
		
		file_hasher = 
			new TOTorrentFileHasher(
					add_other_hashes,
					add_other_per_file_hashes,
					(int)_piece_length, 
					progress_listeners.size()==0?null:this );
		
		if ( cancelled ){
			
			throw( new TOTorrentException( 	"TOTorrentCreate: operation cancelled",
											TOTorrentException.RT_CANCELLED ));
		}
		
		if ( getSimpleTorrent()){
							
			long length = file_hasher.add( _torrent_base );
		
			setFiles( new TOTorrentFileImpl[]{ new TOTorrentFileImpl( this, 0, length, new byte[][]{ getName()})});
			
			setPieces( file_hasher.getPieces());

		}else{
		
			Vector	encoded = new Vector();
		
			processDir( file_hasher, _torrent_base, encoded, "" );
		
			TOTorrentFileImpl[] files = new TOTorrentFileImpl[ encoded.size()];
		
			encoded.copyInto( files );
		
			setFiles( files );
		}
										 
		setPieces( file_hasher.getPieces());
		
		if ( add_other_hashes ){
			
			byte[]	sha1_digest = file_hasher.getSHA1Digest();
			byte[]	ed2k_digest = file_hasher.getED2KDigest();
			
			addAdditionalInfoProperty( "sha1", sha1_digest );
			addAdditionalInfoProperty( "ed2k", ed2k_digest );
			
			//System.out.println( "overall:sha1 = " + ByteFormatter.nicePrint( sha1_digest, true));
			//System.out.println( "overall:ed2k = " + ByteFormatter.nicePrint( ed2k_digest, true));
		}
	}
	
	protected void
	processDir(
		TOTorrentFileHasher	hasher,
		File				dir,
		Vector				encoded,
		String				root )
		
		throws TOTorrentException
	{
		File[]	dir_file_list = dir.listFiles();
		
		if ( dir_file_list == null ){
			
			throw( new TOTorrentException( "TOTorrentCreate: directory '" + dir.getAbsolutePath() + "' returned error when listing files in it",
					TOTorrentException.RT_FILE_NOT_FOUND ));
			
		}
			// sort contents so that multiple encodes of a dir always
			// generate same torrent
		
		List file_list = new ArrayList(Arrays.asList(dir_file_list));
		
		Collections.sort(file_list);
		
		long	offset	= 0;
		
		for (int i=0;i<file_list.size();i++){
			
			File	file = (File)file_list.get(i);
			
			String	file_name = file.getName();
			
			if ( !(file_name.equals( "." ) || file_name.equals( ".." ))){
				
				if ( file.isDirectory()){
					
					if ( root.length() > 0 ){
						
						file_name = root + File.separator + file_name ;
					}
					
					processDir( hasher, file, encoded, file_name );
					
				}else{
						
					if ( !ignoreFile( file_name )){
								
						if ( root.length() > 0 ){
						
							file_name = root + File.separator + file_name;
						}
						
						long length = hasher.add( file );
							
						TOTorrentFileImpl	tf = new TOTorrentFileImpl( this, offset, length, file_name);
						
						offset += length;
						
						if ( add_other_hashes ){
							
							byte[]	ed2k_digest	= hasher.getPerFileED2KDigest();
							byte[]	sha1_digest	= hasher.getPerFileSHA1Digest();
							
							//System.out.println( "file:ed2k = " + ByteFormatter.nicePrint( ed2k_digest, true ));
							//System.out.println( "file:sha1 = " + ByteFormatter.nicePrint( sha1_digest, true ));		
						
							tf.setAdditionalProperty( "sha1", sha1_digest );
							tf.setAdditionalProperty( "ed2k", ed2k_digest );
						}
						
						encoded.addElement( tf );
					}
				}
			}
		}
	}
	
	public void
	pieceHashed(
		int		piece_number )
	{
		for (int i=0;i<progress_listeners.size();i++){
		
			int	this_progress = (int)((piece_number*100)/piece_count );
			
			if ( this_progress != reported_progress ){
				
				reported_progress = this_progress;
				
				((TOTorrentProgressListener)progress_listeners.get(i)).reportProgress( reported_progress );
			}
		}
	}
	
	protected long
	calculateNumberOfPieces(
		File				_file,
		long				_piece_length )
		
		throws TOTorrentException
	{
		long	res = getPieceCount(calculateTotalFileSize( _file ), _piece_length );
		
		report( "Torrent.create.progress.piececount", ""+res );
		
		return( res );
	}
	
	protected long
	calculateTotalFileSize(
		File				file )
		
		throws TOTorrentException
	{
		if ( total_file_size == -1 ){
			
			total_file_size = getTotalFileSize( file );		
		}
		
		return( total_file_size );
	}
	
	protected long
	getTotalFileSize(
		File				file )
		
		throws TOTorrentException
	{
		report( "Torrent.create.progress.parsingfiles" );
		
		long res = getTotalFileSizeSupport( file );
		
		report( "Torrent.create.progress.totalfilesize", res );

		report( "Torrent.create.progress.totalfilecount", ""+total_file_count );
		
		return( res );
	}
	
	protected long
	getTotalFileSizeSupport(
		File				file )
		
		throws TOTorrentException
	{
		String	name = file.getName();
		
		if ( name.equals( "." ) || name.equals( ".." )){
																				
			return( 0 );
		}
		
		if ( !file.exists()){
			
			throw( new TOTorrentException( "TOTorrentCreate: file '" + file.getName() + "' doesn't exist",
											TOTorrentException.RT_FILE_NOT_FOUND ));
		}
		
		if ( file.isFile()){
			
			if ( !ignoreFile( name )){
				
				total_file_count++;
			
				return( file.length());
				
			}else{
				
				return( 0 );
			}
		}else{
			
			File[]	dir_files = file.listFiles();
		
			if ( dir_files == null ){
				
				throw( new TOTorrentException( "TOTorrentCreate: directory '" + file.getAbsolutePath() + "' returned error when listing files in it",
						TOTorrentException.RT_FILE_NOT_FOUND ));
				
			}
			
			long	length = 0;
			
			for (int i=0;i<dir_files.length;i++){
				
				length += getTotalFileSizeSupport( dir_files[i] );
			}
			
			return( length );
		}
	}
	
	protected void
	report(
		String	resource_key )
	{
		report( resource_key, null );
	}
	
	protected void
	report(
		String	resource_key,
		long	bytes )
	{
		if ( progress_listeners.size() > 0 ){
			
			report( resource_key, DisplayFormatters.formatByteCountToKiBEtc( bytes ));
		}
	}
	
	protected void
	report(
		String	resource_key,
		String	additional_text )
	{
		if ( progress_listeners.size() > 0 ){
			
			String	prefix = MessageText.getString(resource_key);
			
			for (int i=0;i<progress_listeners.size();i++){
				
				((TOTorrentProgressListener)progress_listeners.get(i)).reportCurrentTask( prefix + (additional_text==null?"":additional_text ));
			}
		}
	}
	
	protected static long
	getTorrentDataSizeFromFileOrDirSupport(
		File				file )
	{
		String	name = file.getName();
		
		if ( name.equals( "." ) || name.equals( ".." )){
			
			return( 0 );
		}
		
		if ( !file.exists()){
		
			return(0);
		}
		
		if ( file.isFile()){
						
			return( file.length());
			
		}else{
			
			File[]	dir_files = file.listFiles();
			
			long	length = 0;
			
			for (int i=0;i<dir_files.length;i++){
				
				length += getTorrentDataSizeFromFileOrDirSupport( dir_files[i] );
			}
			
			return( length );
		}
	}
	
	public static long
	getTorrentDataSizeFromFileOrDir(
		File			file_or_dir )
	{
		return( getTorrentDataSizeFromFileOrDirSupport( file_or_dir ));
	}	
	
	public static long
	getComputedPieceSize(
		long 	total_size,
		long	_piece_min_size,
		long	_piece_max_size,
		long	_piece_num_lower,
		long	_piece_num_upper )
	{
		long	piece_length = -1;
		
		long	current_piece_size = _piece_min_size;
		
		while( current_piece_size <= _piece_max_size ){
			
			long	pieces = total_size / current_piece_size;
			
			if ( pieces <= _piece_num_upper ){
				
				piece_length = current_piece_size;
				
				break;
			}
			
			current_piece_size = current_piece_size << 1;
		}
		
		// if we haven't set a piece length here then there are too many pieces even
		// at maximum piece size. Go for largest piece size
		
		if ( piece_length == -1 ){
			
			// just go for the maximum piece size
			
			piece_length = 	_piece_max_size;
		}
		
		return( piece_length );
	}
	
	public static long
	getPieceCount(
		long		total_size,
		long		piece_size )
	{
		return( (total_size + (piece_size-1))/piece_size );
	}
	
	protected void
	setIgnoreList()
	{
		try{
			ignore_set = TorrentUtils.getIgnoreSet();
			
		}catch( NoClassDefFoundError e ){
			
			return;
		}
	}
	
	protected boolean
	ignoreFile(
		String		file )
	{
		if ( ignore_set.contains(file.toLowerCase())){

			report( "Torrent.create.progress.ignoringfile", " '" + file + "'" );
			
			return( true );
		}
		
		return( false );
	}
	
	protected void
	cancel()
	{
		if ( !cancelled ){
		
			report( "Torrent.create.progress.cancelled");

			cancelled	= true;
			
			if ( file_hasher != null ){
				
				file_hasher.cancel();
			}
		}
	}
	
	protected void
	addListener(
		TOTorrentProgressListener	listener )
	{
		progress_listeners.add( listener );
	}
	
	protected void
	removeListener(
		TOTorrentProgressListener	listener )
	{
		progress_listeners.remove( listener );
	}
}
