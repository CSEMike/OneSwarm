/*
 * Created on Jul 16, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 */


package com.aelitis.azureus.core.lws;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.logging.LogRelation;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.TOTorrentListener;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;


public class 
LWSTorrent 
	extends LogRelation
	implements TOTorrent
{
	private static TOTorrentAnnounceURLGroup announce_group = 
		new TOTorrentAnnounceURLGroup()
		{
			private TOTorrentAnnounceURLSet[]	sets = new TOTorrentAnnounceURLSet[0];
			
			public TOTorrentAnnounceURLSet[]
           	getAnnounceURLSets()
			{
				return( sets );
			}           	
 
           	public void
           	setAnnounceURLSets(
           		TOTorrentAnnounceURLSet[]	_sets )
           	{
           		sets	= _sets;
           	}
           		
           	public TOTorrentAnnounceURLSet
           	createAnnounceURLSet(
           		final URL[]	_urls )
           	{
           		return( 
           			new TOTorrentAnnounceURLSet()
           			{
           				private URL[] urls = _urls;
           				
           				public URL[]
           				getAnnounceURLs()
           				{
           					return( urls );
           				}
           				    	
           				public void
           				setAnnounceURLs(
           					URL[]		_urls )
           				{
           					urls = _urls;
           				}
           			});
           	}
		};
		
	private static void 
	notSupported()
	{
		Debug.out( "Not Supported" );
	}
	
	private LightWeightSeed		lws;	
	

		
	protected
	LWSTorrent(
		LightWeightSeed		_lws )
	{
		lws				= _lws;
	}
	
	protected TOTorrent
	getDelegate()
	{
		return( lws.getTOTorrent( true ));
	}
	
	public byte[] 
	getName() 
	{
		return( lws.getName().getBytes());
	}

	public String getUTF8Name() {
		return lws.getName();
	}

	public boolean
	isSimpleTorrent()
	{
		return( getDelegate().isSimpleTorrent());
	}
	
	public byte[]
	getComment()
	{
	 	return( getDelegate().getComment());
	}
	
	public void
	setComment(
		String		comment )
	{
		getDelegate().setComment(comment);
	}
	
	public long
	getCreationDate()
	{
		return( getDelegate().getCreationDate());
	}
	
	public void
	setCreationDate(
		long		date )
	{
		getDelegate().setCreationDate(date);
	}
	
	public byte[]
	getCreatedBy()
	{
		return( getDelegate().getCreatedBy());
	}
	
  	public void
	setCreatedBy(
		byte[]		cb )
   	{
  		getDelegate().setCreatedBy( cb );
   	}
  	
	public boolean
	isCreated()
	{
		return( true );
	}
	
	public URL
	getAnnounceURL()
	{
		return( lws.getAnnounceURL());
	}
	
	public boolean
	setAnnounceURL(
		URL		url )
	{
		notSupported();
					
		return( false );
	}
	
	public TOTorrentAnnounceURLGroup
	getAnnounceURLGroup()
	{
		return( announce_group );
	}
	 
	public byte[][]
	getPieces()
	
		throws TOTorrentException
	{
		return( getDelegate().getPieces());
	}
	
	
	public void
	setPieces(
		byte[][]	pieces )
	
		throws TOTorrentException
	{
		getDelegate().setPieces(pieces);
	}
	
	public long
	getPieceLength()
	{
		return( getDelegate().getPieceLength());
	}
	
	public int
	getNumberOfPieces()
	{
		return( getDelegate().getNumberOfPieces());
	}
	
	public long
	getSize()
	{
		return( lws.getSize());
	}
	
	public TOTorrentFile[]
	getFiles()
	{
		return( getDelegate().getFiles());
	}
	 
	public byte[]
	getHash()
				
		throws TOTorrentException
	{
		return( lws.getHash().getBytes());
	}
	
	public HashWrapper
	getHashWrapper()
				
		throws TOTorrentException
	{
		return( lws.getHash());
	}
	
   	public void 
	setHashOverride(
		byte[] hash ) 
	
		throws TOTorrentException 
	{
		throw( new TOTorrentException( "Not supported", TOTorrentException.RT_HASH_FAILS ));
	}
   	
	public boolean
	hasSameHashAs(
		TOTorrent		other )
	{
		try{
			byte[]	other_hash = other.getHash();
				
			return( Arrays.equals( getHash(), other_hash ));
				
		}catch( TOTorrentException e ){
			
			Debug.printStackTrace( e );
			
			return( false );
		}
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
		notSupported();
	}
	
	public void
	setAdditionalStringProperty(
		String		name,
		String		value )
	{
		getDelegate().setAdditionalStringProperty(name, value);
	}
	
	public String
	getAdditionalStringProperty(
		String		name )
	{
		return( getDelegate().getAdditionalStringProperty( name ));
	}
	
	public void
	setAdditionalByteArrayProperty(
		String		name,
		byte[]		value )
	{
		getDelegate().setAdditionalByteArrayProperty(name, value);
	}
	
	public byte[]
	getAdditionalByteArrayProperty(
		String		name )
	{
		return( getDelegate().getAdditionalByteArrayProperty( name ));
	}
	
	public void
	setAdditionalLongProperty(
		String		name,
		Long		value )
	{
		getDelegate().setAdditionalLongProperty(name, value);
	}
	
	public Long
	getAdditionalLongProperty(
		String		name )
	{
		return( getDelegate().getAdditionalLongProperty( name ));
	}
	
	
	public void
	setAdditionalListProperty(
		String		name,
		List		value )
	{
		getDelegate().setAdditionalListProperty(name, value);
	}
	
	public List
	getAdditionalListProperty(
		String		name )
	{
		return( getDelegate().getAdditionalListProperty( name ));
	}
	
	public void
	setAdditionalMapProperty(
		String		name,
		Map			value )
	{
		getDelegate().setAdditionalMapProperty(name, value);
	}
	
	public Map
	getAdditionalMapProperty(
			String		name )
	{
		return( getDelegate().getAdditionalMapProperty( name ));
	}
	
	public Object
	getAdditionalProperty(
		String		name )
	{
		if ( name.equals( "url-list" ) || name.equals( "httpseeds" )){
			
			return( null );
		}
		
		return( getDelegate().getAdditionalProperty( name ));
	}
	
	public void
	setAdditionalProperty(
		String		name,
		Object		value )
	{
		getDelegate().setAdditionalProperty(name, value);
	}
	
	public void
	removeAdditionalProperty(
		String name )
	{
		getDelegate().removeAdditionalProperty(name);
	}
	
	public void
	removeAdditionalProperties()
	{
		getDelegate().removeAdditionalProperties();
	}
	
	public void
	serialiseToBEncodedFile(
		File		file )
	
		throws TOTorrentException
	{
		getDelegate().serialiseToBEncodedFile( file );
	}
	
	public void
	addListener(
		TOTorrentListener		l )
	{
		getDelegate().addListener( l );
	}

	public void
	removeListener(
		TOTorrentListener		l )
	{
		getDelegate().removeListener( l );
	}
	
	public Map
	serialiseToMap()
	
		throws TOTorrentException
	{
		return( getDelegate().serialiseToMap());
	}
	
	public void
	serialiseToXMLFile(
		File		file )
	
		throws TOTorrentException
	{
		getDelegate().serialiseToXMLFile( file );
	}
	
	public AEMonitor
	getMonitor()
	{
		return( getDelegate().getMonitor());
	}
	
	public void
	print()
	{
		getDelegate().print();
	}
	
	public String 
	getRelationText() 
	{
		return "LWTorrent: '" + new String(getName()) + "'";  
	}
	
	public Object[] 
	getQueryableInterfaces() 
	{
		return new Object[] { lws };
	}
}
