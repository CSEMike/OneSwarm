/*
 * Created on 15-Dec-2005
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

package com.aelitis.azureus.plugins.extseed.impl.getright;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.torrent.Torrent;

import com.aelitis.azureus.plugins.extseed.ExternalSeedPlugin;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReader;
import com.aelitis.azureus.plugins.extseed.ExternalSeedReaderFactory;

public class 
ExternalSeedReaderFactoryGetRight
	implements ExternalSeedReaderFactory
{
	public
	ExternalSeedReaderFactoryGetRight()
	{
	}
	
	public ExternalSeedReader[]
   	getSeedReaders(
   		ExternalSeedPlugin		plugin,
   		Download				download )
  	{		
  		Torrent	torrent = download.getTorrent();
  		
  		try{
  			Map	config = new HashMap();
  			
  			Object	obj = torrent.getAdditionalProperty( "url-list" );
  			
  			if ( obj != null ){
  				
  				config.put( "url-list", obj );
  			}
  			
  			return( getSeedReaders( plugin, download, config ));
  			
  		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( new ExternalSeedReader[0] );  	
	}
	
	public ExternalSeedReader[]
  	getSeedReaders(
  		ExternalSeedPlugin		plugin,
  		Download				download,
  		Map						config )
	{				
		try{
			Object	obj = config.get( "url-list" );
            
            /* resolve url-list according to specification 
             * (http://www.getright.com/seedtorrent.html)
             */ 
            if ( obj instanceof byte[] ){
                List l = new ArrayList();
                l.add(obj);
                obj = l;
            }
			
			if ( obj instanceof List ){
				
				List	urls = (List)obj;

				List	readers = new ArrayList();
				
				Object	_params = config.get( "url-list-params" );
				
				Map	params = _params instanceof Map?(Map)_params:new HashMap();
				
				for (int i=0;i<urls.size();i++){
					
					try{	
						String	url_str = new String((byte[])urls.get(i));
						
							// avoid java encoding ' ' as '+' as this is not conformant with Apache (for example)
						
						url_str = url_str.replaceAll( " ", "%20");

						URL	url = new URL( url_str );
						
						String	protocol = url.getProtocol().toLowerCase();
																		
						if ( protocol.equals( "http" )){
							
							readers.add( new ExternalSeedReaderGetRight(plugin, download.getTorrent(), url, params ));
							
						}else{
							
							plugin.log( download.getName() + ": GR unsupported protocol: " + url );
						}
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
				
				ExternalSeedReader[]	res = new ExternalSeedReader[ readers.size() ];
				
				readers.toArray( res );
				
				return( res );
			}
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
		
		return( new ExternalSeedReader[0] );
	}
	
	public static void
	main(
		String[]	args )
	{
		try{
			COConfigurationManager.preInitialise();
			
			File file = new File  ( "C:\\temp\\httpseed.torrent");
			
			TOTorrent	torrent = TOTorrentFactory.deserialiseFromBEncodedFile( file );
			
			Map	map = torrent.serialiseToMap();
			
			List	urls = new ArrayList();
			
			urls.add( "http://127.0.0.1:888/files/%DF%26%5B7w%C9%13I%88%8D%EC%E5b%2C9%0F%8D%0Co%BC/" );
			
			map.put( "url-list", urls);
			
			Map params = new HashMap();
			
			map.put( "url-list-params", params );
			
			torrent = TOTorrentFactory.deserialiseFromMap( map );
			
			torrent.serialiseToBEncodedFile( file );
			
		}catch( Throwable e ){
			
			e.printStackTrace();
		}
	}
}
