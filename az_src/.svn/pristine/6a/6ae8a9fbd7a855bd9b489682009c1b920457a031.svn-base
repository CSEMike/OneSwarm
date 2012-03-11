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

package org.gudy.azureus2.core3.util.protocol.i2p;

/**
 * @author parg
 *
 */

import java.io.IOException;
import java.net.*;

import org.gudy.azureus2.core3.util.Debug;

public class 
Handler 
	extends URLStreamHandler 
{
	public URLConnection 
	openConnection(URL u)
	{	
		String	str = u.toString();
		
		str = "http" + str.substring( 3 );
		
		try{
			return( new URL(str).openConnection());
			
		}catch( MalformedURLException e ){
			
			Debug.printStackTrace(e);
			
			return( null );
			
		}catch( IOException  e ){
			
			Debug.printStackTrace(e);
			
			return( null );
		}
	}

}
