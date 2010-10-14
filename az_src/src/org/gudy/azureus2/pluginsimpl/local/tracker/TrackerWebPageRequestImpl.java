/*
 * File    : TrackerWebPageRequestImpl.java
 * Created : 08-Dec-2003
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

package org.gudy.azureus2.pluginsimpl.local.tracker;

/**
 * @author parg
 *
 */

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

public class 
TrackerWebPageRequestImpl
	implements TrackerWebPageRequest
{
	private Tracker				tracker;
	private TrackerWebContext	context;
	private InetSocketAddress	client_address;
	private String				user;
	private String				url;
	private URL					absolute_url;
	private String				header;
	private InputStream			is;
	
	protected
	TrackerWebPageRequestImpl(
		Tracker				_tracker,
		TrackerWebContext	_context,
		InetSocketAddress	_client_address,
		String				_user,
		String				_url,
		URL					_absolute_url,
		String				_header,
		InputStream			_is )
	{
		tracker			= _tracker;
		context			= _context;
		client_address	= _client_address;
		user			= _user;
		url				= _url;
		absolute_url	= _absolute_url;
		header			= _header;
		is				= _is;
	}
	
	public Tracker
	getTracker()
	{
		return( tracker );
	}
	
	public TrackerWebContext
	getContext()
	{
		return( context );
	}
	
	public String
	getURL()
	{
		return( url );
	}
	
	public URL
	getAbsoluteURL()
	{
		return( absolute_url );
	}
	
	public String
	getClientAddress()
	{
		return( client_address.getAddress().getHostAddress());
	}

	public InetSocketAddress
	getClientAddress2()
	{
		return( client_address );
	}
	
	public String
	getUser()
	{
		return( user );
	}
	
	public InputStream
	getInputStream()
	{
		return( is );
	}
	
	public String
	getHeader()
	{
		return( header );
	}
	
	public Map 
	getHeaders()
	{
        Map headers = new HashMap();

        String[] header_parts = header.split("\r\n");

        headers.put("status", header_parts[0].trim());

        for (int i = 1;i<header_parts.length;i++) {

        	String[] key_value = header_parts[i].split(":",2);

            headers.put(key_value[0].trim().toLowerCase(), key_value[1].trim());
        }

        return headers;
	}
}	
