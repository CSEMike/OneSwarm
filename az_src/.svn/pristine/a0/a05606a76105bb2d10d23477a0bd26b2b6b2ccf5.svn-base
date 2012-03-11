/*
 * File    : UDPURLConnection.java
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

package org.gudy.azureus2.core3.util.protocol.vuze;

/**
 * @author parg
 *
 */

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;

import org.gudy.azureus2.core3.util.Constants;

import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;


class 
VuzeURLConnection 
	extends HttpURLConnection 
{
	private URL		url;
	
	private int		response_code	= HTTP_OK;
	private String	response_msg	= "OK";
	
	private InputStream					input_stream;
	private Map<String,List<String>> 	headers = new HashMap<String, List<String>>();

	VuzeURLConnection(
		URL 	u )
	{
		super(u);
		
		url		= u;
	}

	public void 
	connect() 
		throws IOException 
	{
		String str = url.toExternalForm();
		
		int	pos = str.indexOf( "=" );
		
		str = str.substring( pos+1 );
		
		byte[]	bytes = str.getBytes( Constants.BYTE_ENCODING );
		
		VuzeFile vf = VuzeFileHandler.getSingleton().loadVuzeFile( bytes );
		
		if ( vf == null ){
			
			throw( new IOException( "Invalid vuze file" ));
		}
		
		input_stream = new ByteArrayInputStream( bytes );
	}
	
   public Map<String,List<String>> 
    getHeaderFields() 
    {
        return( headers );
    }
    
    public String
    getHeaderField(
    	String	name )
    {
    	List<String> values = headers.get( name );
    	
    	if ( values == null || values.size() == 0 ){
    		
    		return( null );
    	}
    	
    	return( values.get( values.size()-1 ));
    }
    
    public void
    setHeaderField(
    	String	name,
    	String	value )
    {
       	List<String> values = headers.get( name );
       	
       	if ( values == null ){
       		
       		values = new ArrayList<String>();
       		
       		headers.put( name, values );
       	}

       	values.add( value );
    }
    
	public InputStream
	getInputStream()
	
		throws IOException
	{
		if ( input_stream == null ){
			
			connect();
		}
		
		return( input_stream );
	}
	
	public void
	setResponse(
		int		_code,
		String	_msg )
	{
		response_code		= _code;
		response_msg		= _msg;
	}
	
	public int
	getResponseCode()
	{
		return( response_code );
	}
	
	public String
	getResponseMessage()
	{
		return( response_msg );
	}
		
	public boolean
	usingProxy()
	{
		return( false );
	}
	
	public void
	disconnect()
	{
	}
}
