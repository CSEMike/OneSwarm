/*
 * Created on 05-May-2004
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

package org.gudy.azureus2.pluginsimpl.remote.ipfilter;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.ipfilter.*;

import org.gudy.azureus2.pluginsimpl.remote.*;


public class 
RPIPRange
	extends		RPObject
	implements 	IPRange 
{
	protected transient IPRange		delegate;

		// don't change these field names as they are visible on XML serialisation

	public String			description;
	public String			start_ip;
	public String			end_ip;
	
	public static RPIPRange
	create(
		IPRange		_delegate )
	{
		RPIPRange	res =(RPIPRange)_lookupLocal( _delegate );
		
		if ( res == null ){
			
			res = new RPIPRange( _delegate );
		}
		
		return( res );
	}
	
	protected
	RPIPRange(
		IPRange		_delegate )
	{
		super( _delegate );
	}
	
	protected void
	_setDelegate(
		Object		_delegate )
	{
		delegate = (IPRange)_delegate;
		
		description		= delegate.getDescription();
		start_ip		= delegate.getStartIP();	
		end_ip			= delegate.getEndIP();	
	}
	
	public Object
	_setLocal()
	
		throws RPException
	{
		return( _fixupLocal());
	}
	
	public RPReply
	_process(
		RPRequest	request	)
	{
		String	method = request.getMethod();	
		
		if ( method.equals( "delete")){
			
			delegate.delete();
			
			return( null );
		}

		throw( new RPException( "Unknown method: " + method ));
	}
	
	
		// ***************************************************
	
	public String
	getDescription()
	{
		return( description );
	}
	
	public void
	setDescription(
		String	str )
	{
		notSupported();
	}
	
	public void
	checkValid()
	{
		notSupported();
	}
	
	public boolean
	isValid()
	{
		notSupported();
		
		return( false );
	}
	
	public boolean
	isSessionOnly()
	{
		notSupported();
		
		return( false );
	}
	
	public String
	getStartIP()
	{
		return( start_ip );
	}
	
	public void
	setStartIP(
		String	str )
	{
		notSupported();
	}
		
	public String
	getEndIP()
	{
		return( end_ip );
	}
	
	public void
	setEndIP(
		String	str )
	{
		notSupported();
	}
  
	public void
	setSessionOnly(
		boolean sessionOnly )
	{
		notSupported();
	}
		
	public boolean 
	isInRange(
		String ipAddress )
	{
		notSupported();
		
		return( false );
	}
	
	public void
	delete()
	{
		_dispatcher.dispatch( new RPRequest( this, "delete", null )).getResponse();
	}
	
	public int
	compareTo(
		Object	other )
	{
		notSupported();
		
		return( -1 );		
	}
}