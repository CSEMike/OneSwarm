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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.tracker.server.TRTrackerServerListener2;
import org.gudy.azureus2.core3.util.AsyncController;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;

public class 
TrackerWebPageRequestImpl
	implements TrackerWebPageRequest
{
	private Tracker				tracker;
	private TrackerWebContext	context;
	
	private TRTrackerServerListener2.ExternalRequest	request;
	
	
	protected
	TrackerWebPageRequestImpl(
		Tracker										_tracker,
		TrackerWebContext							_context,
		TRTrackerServerListener2.ExternalRequest	_request )
	{
		tracker		= _tracker;
		context		= _context;
		request		= _request;
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
		return( request.getURL());
	}
	
	public URL
	getAbsoluteURL()
	{
		return( request.getAbsoluteURL());
	}
	
	public String
	getClientAddress()
	{
		return( request.getClientAddress().getAddress().getHostAddress());
	}

	public InetSocketAddress
	getClientAddress2()
	{
		return( request.getClientAddress());
	}
	
	public InetSocketAddress
	getLocalAddress()
	{
		return( request.getLocalAddress());
	}
	
	public String
	getUser()
	{
		return( request.getUser());
	}
	
	public InputStream
	getInputStream()
	{
		return( request.getInputStream());
	}
	
	protected OutputStream
	getOutputStream()
	{
		return( request.getOutputStream());
	}
	
	protected AsyncController
	getAsyncController()
	{
		return( request.getAsyncController());
	}
	
	public boolean
	canKeepAlive()
	{
		return( request.canKeepAlive());
	}
	
	public void
	setKeepAlive(
		boolean	ka )
	{
		request.setKeepAlive( ka );
	}
	
	public String
	getHeader()
	{
		return( request.getHeader());
	}
	
	public Map 
	getHeaders()
	{
        Map headers = new HashMap();

        String[] header_parts = request.getHeader().split("\r\n");

        headers.put("status", header_parts[0].trim());

        for (int i = 1;i<header_parts.length;i++) {

        	String[] key_value = header_parts[i].split(":",2);

            headers.put(key_value[0].trim().toLowerCase( MessageText.LOCALE_ENGLISH ), key_value[1].trim());
        }

        return headers;
	}
}	
