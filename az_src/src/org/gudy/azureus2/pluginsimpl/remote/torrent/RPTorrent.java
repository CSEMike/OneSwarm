/*
 * File    : PRTorrent.java
 * Created : 28-Jan-2004
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

package org.gudy.azureus2.pluginsimpl.remote.torrent;

import java.net.*;

/**
 * @author parg
 *
 */

import java.util.Map;
import java.io.File;

import org.gudy.azureus2.plugins.torrent.*;

import org.gudy.azureus2.pluginsimpl.remote.*;


public class 
RPTorrent
	extends		RPObject
	implements 	Torrent
{
	protected transient Torrent		delegate;

		// don't change these field names as they are visible on XML serialisation
	
	public String		name;
	public long			size;
	public byte[]		hash;
	
	
	public static RPTorrent
	create(
		Torrent		_delegate )
	{
		RPTorrent	res =(RPTorrent)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPTorrent( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPTorrent(
		Torrent		_delegate )
	{
		super( _delegate );
		
		delegate	= _delegate;		
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (Torrent)_delegate;
		
		name		= delegate.getName();
		size		= delegate.getSize();
		hash		= delegate.getHash();
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
		
		/*
		 if ( method.equals( "getPluginProperties")){
		 
		 return( new RPReply( delegate.getPluginProperties()));
		 }
		 */
		
		throw( new RPException( "Unknown method: " + method ));
	}
	
	public String
	getName()
	{
		return( name );
	}
	
	public URL
	getAnnounceURL()
	{
		notSupported();
		
		return( null );
	}
		
	public void
	setAnnounceURL(
		URL		url )
	{
		notSupported();
	}
	
	public TorrentAnnounceURLList
	getAnnounceURLList()
	{
		notSupported();
		
		return( null );
	}
	
	public boolean
	isDecentralised()
	{
		notSupported();
		
		return( false );
	}
	
	public boolean
	isDecentralisedBackupEnabled()
	{
		notSupported();
		
		return( false );
	}	
	
	public void
	setDecentralisedBackupRequested(
		boolean	requested )
	{
		notSupported();		
	}
	
	public boolean
	isDecentralisedBackupRequested()
	{
		notSupported();
		
		return( false );	
	}

	public boolean
	isPrivate()
	{
		notSupported();
		
		return( false );
	}
	
	public boolean
	wasCreatedByUs()
	{
		notSupported();
		
		return( false );
	}
	
	public void
	setPrivate(
		boolean	priv )
	{
		notSupported();
	}
	
	public byte[]
	getHash()
	{
		return( hash );
	}	
	
	public long
	getSize()
	{
		return( size );
	}
	
	public String
	getComment()
	{
		notSupported();
		
		return(null);
	}	
	
	public void
	setComment(
		String	comment )
	{
		notSupported();
	}
	
	public long
	getCreationDate()
	{
		notSupported();
		
		return(0);
	}
	
	public String
	getCreatedBy()
	{
		notSupported();
		
		return(null);
	}	
	public long
	getPieceSize()
	{
		notSupported();
		
		return(0);
	}	
	public long
	getPieceCount()
	{
		notSupported();
		
		return(0);
	}	
	
	public byte[][]
	getPieces()
	{
		notSupported();
		
		return(null);
	}	
	
	public URL
	getMagnetURI()
	{
		notSupported();
		
		return( null );
	}
	
	public String
	getEncoding()
	{
		notSupported();
		
		return( null );
	}
	
	public void
	setEncoding(String encoding)
	{
		notSupported();
	}
	
	public void
	setDefaultEncoding()
	{
		notSupported();
	}
	
	public TorrentFile[]
	getFiles()
	{
		notSupported();
		
		return(null);
	}	
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		notSupported();
		
		return(null);
	}
	
	public Torrent
	removeAdditionalProperties()
	{
		notSupported();
		
		return( null );
	}

	public void
	setPluginStringProperty(
		String		name,
		String		value )
	{
		notSupported();
	}
	
	public String
	getPluginStringProperty(
		String		name )
	{
		notSupported();
		
		return( null );
	}
	
	public void
	setMapProperty(
		String		name,
		Map			value )
	{
		notSupported();
	}
	
	public Map
	getMapProperty(
		String		name )
	{
		notSupported();

		return( null );
	}
	
	public Map
	writeToMap()
	
		throws TorrentException
	{
		notSupported();

		return( null );
	}
	
	public byte[]
	writeToBEncodedData()
				
		throws TorrentException
	{
		notSupported();

		return( null );
	}
	
	public void
	writeToFile(
		File		file )
	
		throws TorrentException
	{
		notSupported();
	}
  	
	public void
	save()
		throws TorrentException
	{
		notSupported();
	}
	
	public void
	setComplete(
		File		data_dir )
	
		throws TorrentException
	{
		notSupported();
	}
	
	public boolean
	isComplete()
	{
		notSupported();
		
		return( false );
	}
	public boolean isSimpleTorrent() {notSupported(); return false;} 
}
