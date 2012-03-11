/*
 * File    : TRHostExternalTorrent.java
 * Created : 19-Nov-2003
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

package org.gudy.azureus2.core3.tracker.host.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.*;
import java.io.*;

import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public class 
TRHostExternalTorrent 
	implements TOTorrent
{
	private byte[]		name;
	private byte[]		hash;
	private HashWrapper	hash_wrapper;
	private URL			announce_url;
	
	protected Map		additional_properties = new HashMap();
	
	protected AEMonitor this_mon 	= new AEMonitor( "TRHostExternalTorrent" );

	protected
	TRHostExternalTorrent(
		byte[]	_hash,
		URL		_announce_url  )
	{
		hash			= _hash;
		hash_wrapper	= new HashWrapper( hash );
		announce_url	= _announce_url;
		
		name = ByteFormatter.nicePrint( hash, true ).getBytes();
		
		try{
			LocaleTorrentUtil.setDefaultTorrentEncoding( this );
			
		}catch( LocaleUtilEncodingException e ){
			
			Debug.printStackTrace( e );
		}
	}
	
	public byte[]
	getName()
	{
		return( name );
	}
	
	public String getUTF8Name() {
		return null;
	}

	
	public boolean
	isSimpleTorrent()
	{
		return( true );
	}
	

	public byte[]
	getComment()
	{
		return( null );
	}

	public void
	setComment(
		String		comment )
	{		
	}
	
	
	public long
	getCreationDate()
	{
		return(0);
	}
	
	public void
	setCreationDate(
		long		date )
	{
	}
	
	public byte[]
	getCreatedBy()
	{
		return( null );
	}
	
 	public void
	setCreatedBy(
		byte[]		cb )
   	{
   	}
 	
	public boolean
	isCreated()
	{
		return( false );
	}
	
	public URL
	getAnnounceURL()
	{
		return( announce_url );
	}

	public boolean
	setAnnounceURL(
		URL		url )
	{
		return false;
	}
		
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup()
	{
		return( 
			new TOTorrentAnnounceURLGroup()
			{
				public TOTorrentAnnounceURLSet[]
               	getAnnounceURLSets()
				{
					return( new TOTorrentAnnounceURLSet[0] );
				}
 
               	public void
               	setAnnounceURLSets(
               		TOTorrentAnnounceURLSet[]	sets )
				{
				}
               		
               	public TOTorrentAnnounceURLSet
               	createAnnounceURLSet(
               		URL[]	urls )
				{
					return( new TOTorrentAnnounceURLSet()
							{
								public URL[]
						       	getAnnounceURLs()
								{
									return( new URL[0]);
								}
						       	
						       	public void
						       	setAnnounceURLs(
						       		URL[]		urls )
								{
								}
							});
				}
			});
	}
  
	public void
	addTorrentAnnounceURLSet( 
		URL[] urls )
	{
	}
  
	
	public byte[][]
	getPieces()
	{
		return( new byte[0][] );
	}

	public void
	setPieces(
		byte[][]	b )
	{
	}
	
	public int
	getNumberOfPieces()
	{
		return( 0);
	}

	public long
	getPieceLength()
	{
		return( -1 );
	}

	public long
	getSize()
	{
		return( -1 );
	}
	

	public TOTorrentFile[]
	getFiles()
	{
		return( new TOTorrentFile[0]);
	}
	 
	public byte[]
	getHash()
	
		throws TOTorrentException
	{
		return( hash );
	}
	
	public HashWrapper
	getHashWrapper()

		throws TOTorrentException
	{
		return( hash_wrapper );
	}
	
   	public void 
	setHashOverride(
		byte[] hash ) 
	
		throws TOTorrentException 
	{
		throw( new TOTorrentException( "Not supported", TOTorrentException.RT_HASH_FAILS ));
	}
   	
	public boolean
	getPrivate()
	{
		return( false );
	}
	
	public void
	setPrivate(
		boolean	_private )
	
		throws TOTorrentException
	{
	}
	
	public boolean
	hasSameHashAs(
		TOTorrent		other )
	{
		try{
			byte[]	other_hash = other.getHash();
			
			return( Arrays.equals( hash, other_hash ));
			
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
			return( false );
		}
	}
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
	{
		try{
		
			additional_properties.put(name,value.getBytes(Constants.DEFAULT_ENCODING));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
	}
		
	public String
	getAdditionalStringProperty(
		String		name )
	{
		try{
			Object	obj = additional_properties.get(name);

			if ( obj == null ){
				
				return( null );
			}
			
			if ( !( obj instanceof byte[] )){
				
				Debug.out( "property '" + name + "' is not a byte[]: " + obj );
				
				return( null );
			}
			
			return( new String((byte[])obj,Constants.DEFAULT_ENCODING));
			
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
			
			return( null );
		}
	}
		
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value )
	{
		additional_properties.put(name,value);
	}
		
	public byte[]
	getAdditionalByteArrayProperty(
		String		name )
	{
		return( (byte[])additional_properties.get(name));
	}
	
	public void
	setAdditionalLongProperty(
		String		name,
		Long		value )
	{
		additional_properties.put(name,value);
	}
		
	public void
	setAdditionalProperty(
		String		name,
		Object		value )
	{
		if ( value instanceof String ){
			
			setAdditionalStringProperty(name,(String)value);
			
		}else{
		
			additional_properties.put( name, value );
		}
	}
	
	public Long
	getAdditionalLongProperty(
		String		name )
	{
		return((Long)additional_properties.get(name));
	}
		
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value )
	{
		additional_properties.put(name,value);
	}
		
	public List
	getAdditionalListProperty(
		String		name )
	{
		return((List)additional_properties.get(name));
	}
		
	public void
	setAdditionalMapProperty(
		String	name,
		Map		value )
	{
		additional_properties.put(name,value);
	}
		
	public Map
	getAdditionalMapProperty(
		String		name )
	{
		return( (Map)additional_properties.get(name));
	}
		
	public Object
	getAdditionalProperty(
		String		name )
	{
		return( additional_properties.get(name));
	}
	
	public void
	removeAdditionalProperty(
		String name )
	{
		additional_properties.remove( name );
	}

	public void
	removeAdditionalProperties()
	{
		additional_properties.clear();
	}
	
	public void
	serialiseToBEncodedFile(
		File		file )
		  
		throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}


	public Map
	serialiseToMap()
		  
		throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}

   public void
   serialiseToXMLFile(
	   File		file )
		  
	   throws TOTorrentException
	{
		throw( new TOTorrentException("External Torrent", TOTorrentException.RT_WRITE_FAILS ));
	}

 	public void
	addListener(
		TOTorrentListener		l )
	{
	}

	public void
	removeListener(
		TOTorrentListener		l )
	{
	}
	
   	public AEMonitor
	getMonitor()
   	{
   		return( this_mon );
   	}
   	
	public void
	print()
	{
	}
}
