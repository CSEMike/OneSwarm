/*
 * Created on Jul 1, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package org.gudy.azureus2.core3.tracker.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;

import org.gudy.azureus2.core3.util.AsyncController;

public interface 
TRTrackerServerListener2 
{
	public boolean
	handleExternalRequest(
		ExternalRequest		request )
	
		throws IOException;
	
	
	public interface
	ExternalRequest
	{
		public InetSocketAddress
		getClientAddress();
		
		public InetSocketAddress
		getLocalAddress();

		public String
		getUser();
		
		public String
		getURL();
		
		public URL
		getAbsoluteURL();
		
		public String
		getHeader();
		
		public InputStream
		getInputStream();
		
		public OutputStream
		getOutputStream();
		
		public AsyncController
		getAsyncController();
		
		public boolean
		canKeepAlive();
		
		public void
		setKeepAlive(
			boolean		ka );
	}
}
