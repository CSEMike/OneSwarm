/*
 * Created on 27-Apr-2004
 * Created by Paul Gardner
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.html.impl;

/**
 * @author parg
 *
 */

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.gudy.azureus2.core3.html.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;

public class 
HTMLPageImpl
	extends HTMLChunkImpl
	implements HTMLPage
{
	public
	HTMLPageImpl(
		InputStream		is,
		String			charset,
		boolean			close_file )
	
		throws HTMLException
	{
		BufferedReader	br = null;
		
		StringBuffer	res = new StringBuffer(1024);
		
		try{
			
			if ( charset == null ){
				
				br = new BufferedReader( new InputStreamReader(is));
			}else{
				
				br = new BufferedReader( new InputStreamReader(is, charset));
			}
			while(true){
				
				String	line = br.readLine();
				
				if ( line == null ){
					
					break;
				}
				
				res.append( line );
			}
			
			setContent( res.toString());
			
		}catch( IOException e ){
			
			throw( new HTMLException( "Error reading HTML page", e ));
			
		}finally{
			
			if ( br != null && close_file ){
				
				try{
					
					br.close();
					
				}catch( IOException e ){
					
					Debug.printStackTrace( e );
				}
			}
		}
	}
	public URL
	getMetaRefreshURL()
	{
		return( getMetaRefreshURL( null ));
	}
	
	public URL
	getMetaRefreshURL(
		URL		base_url )
	{
	       // "<META HTTP-EQUIV=\"refresh\" content=\"5; URL=xxxxxxx>";
	       
		String[]	tags = getTags( "META" );
		
		for (int i=0;i<tags.length;i++){
			
			String	tag 	= tags[i];
			
			String	lc_tag	= tag.toLowerCase( MessageText.LOCALE_ENGLISH );
			
			int pos = lc_tag.indexOf("http-equiv=\"refresh\"");
							
			int	url_start = lc_tag.indexOf( "url=" );
			
			if ( pos != -1 && url_start != -1 ){
				
				url_start += 4;
				
				int	e1 = lc_tag.indexOf( "\"", url_start );
				int e2 = lc_tag.indexOf( ">", url_start );
				
				if ( e1 != -1 ){
					
					e2 = e1;
				
					try{
						String mr_url = tag.substring(url_start, e2).trim();
						
						String lc = mr_url.toLowerCase();
						
						if ( ! ( lc.startsWith( "http:" ) || lc.startsWith( "https:" ))){
							
							if ( base_url != null ){
								
								String s = base_url.toExternalForm();
								
								int p = s.indexOf( '?' );
								
								if ( p != -1 ){
									
									s = s.substring( 0, p );
								}
								
								if ( s.endsWith( "/" ) && mr_url.startsWith( "/" )){
									
									mr_url = mr_url.substring( 1 );
								}
								
								mr_url = s + mr_url;
							}
						}
						
						return( new URL( mr_url ));
						
					}catch( MalformedURLException e ){
						
						Debug.printStackTrace( e );
					}
				}
			}
		}
				
		return( null );
	}
}
