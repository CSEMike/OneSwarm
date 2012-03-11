/*
 * Created on 16-Dec-2005
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

package com.aelitis.azureus.plugins.extseed.impl.webseed;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.plugins.extseed.ExternalSeedException;
import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.impl.ExternalSeedReaderImpl;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloader;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloaderListener;
import com.aelitis.azureus.plugins.extseed.util.ExternalSeedHTTPDownloaderRange;

public class 
ExternalSeedReaderWebSeed
	extends ExternalSeedReaderImpl
{
	private URL			url;
	private int			port;
	private String		url_prefix;
	
	private boolean	supports_503;
	
	protected
	ExternalSeedReaderWebSeed(
		ExternalSeedPlugin 		_plugin,
		Torrent					_torrent,	
		URL						_url,
		Map						_params )
	{
		super( _plugin, _torrent, _url.getHost(), _params );

		supports_503		= getBooleanParam( _params, "supports_503", true );
		
		url		= _url;
		
		port	= url.getPort();
		
		if ( port == -1 ){
			
			port = url.getDefaultPort();
		}
		
		try{
			String hash_str = URLEncoder.encode(new String(_torrent.getHash(), "ISO-8859-1"), "ISO-8859-1").replaceAll("\\+", "%20");

			url_prefix = url.toString()+"?info_hash=" + hash_str;
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public boolean
	sameAs(
		ExternalSeedReader	other )
	{
		if ( other instanceof ExternalSeedReaderWebSeed ){
			
			return( url.toString().equals(((ExternalSeedReaderWebSeed)other).url.toString()));
		}
		
		return( false );
	}
	
	public String
	getName()
	{
		return( "WS: " + url );
	}
	
	public URL 
	getURL() 
	{
		return( url );
	}
	
	public int
	getPort()
	{
		return( port );
	}

	
	protected int
	getPieceGroupSize()
	{
		return( 1 );
	}
	
	protected boolean
	getRequestCanSpanPieces()
	{
		return( false );
	}
	
	protected void
	readData(
		int									piece_number,
		int									piece_offset,
		int									length,
		ExternalSeedHTTPDownloaderListener	listener )
	
		throws ExternalSeedException
	{		
		long	piece_end	= piece_offset + length - 1;
			
		String	str = url_prefix + "&piece=" + piece_number + "&ranges=" + piece_offset + "-" + piece_end;
				
		setReconnectDelay( RECONNECT_DEFAULT, false );
		
		ExternalSeedHTTPDownloader	http_downloader = null;
		
		try{
			http_downloader = new ExternalSeedHTTPDownloaderRange( new URL( str ), getUserAgent());

				// unfortunately using HttpURLConnection it isn't possible to read the 503 response as per
				// protocol - however, for az http web seeds we don't uses 503 anyway so we cna use URLCon. The
				// main benefit here is we also get http proxying which we don't get with our direct socket
				// support...
			
			if ( supports_503 ){
				
				http_downloader.downloadSocket( length, listener, isTransient() );

			}else{
				
				http_downloader.download( length, listener, isTransient() );
			}			
			
       }catch( ExternalSeedException ese ){

			if ( http_downloader.getLastResponse() == 503 && http_downloader.getLast503RetrySecs() >= 0 ){
				
				int	retry_secs = http_downloader.getLast503RetrySecs();
				
				setReconnectDelay( retry_secs * 1000, true );
				
				throw( new ExternalSeedException( "Server temporarily unavailable, retrying in " + retry_secs + " seconds" ));
				
			}else{
				
				throw( ese );
			}
		}catch( MalformedURLException e ){
			
			throw( new ExternalSeedException( "URL encode fails", e ));
		}
	}
}
