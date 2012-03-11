/*
 * File    : Handler.java
 * Created : 19-Jan-2004
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

package org.gudy.azureus2.core3.util.protocol.dht;

/**
 * @author parg
 *
 */

import java.io.IOException;
import java.net.*;

import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;

public class 
Handler 
	extends URLStreamHandler 
{
	public URLConnection 
	openConnection(URL u)
	{	
			// format is dht://<hash>[.dht]/[params]
			// treat as magnet magnet:?xt=urn:btih:<hash32>/params
		
		URL magnet_url;
		
		try{
			String	str = u.toString();
			
			str = str.substring( 6 );
			
			int	param_pos = str.indexOf( '/' );
			
			String hash = param_pos==-1?str:str.substring( 0, param_pos );
			
			hash = hash.trim();
			
			int	dot_pos = hash.indexOf( '.' );
			
			if ( dot_pos != -1 ){
				
				hash = hash.substring( 0, dot_pos ).trim();
			}
			
			if ( hash.length() == 40 ){
				
				hash = Base32.encode( ByteFormatter.decodeString( hash ));
			}
			
			magnet_url = new URL( "magnet:?xt=urn:btih:" + hash + "/" + (param_pos==-1?"":str.substring(param_pos+1)));
			
		}catch( Throwable e ){
			
			Debug.out( "Failed to transform dht url '" + u + "'", e );
			
			return( null );
		}
		
			//	System.out.println( "Transformed " + u + " -> " + magnet_url );
		
		try{
			return( magnet_url.openConnection());
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace(e);
			
			return( null );
			
		}catch( IOException  e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}

}
