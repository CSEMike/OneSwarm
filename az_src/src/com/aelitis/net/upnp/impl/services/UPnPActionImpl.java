/*
 * Created on 15-Jun-2004
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

package com.aelitis.net.upnp.impl.services;

/**
 * @author parg
 *
 */

import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

import com.aelitis.net.upnp.*;

public class 
UPnPActionImpl
	implements UPnPAction
{
	protected UPnPServiceImpl		service;
	protected String				name;
	
	protected 
	UPnPActionImpl(
		UPnPServiceImpl					_service,
		SimpleXMLParserDocumentNode		node )
	{
		service	= _service;
		
		name	= node.getChild( "name" ).getValue().trim();
	}

	public String
	getName()
	{
		return( name );
	}
	
	public UPnPService
	getService()
	{
		return( service );
	}
	
	public UPnPActionInvocation
	getInvocation()
	{
		return( new UPnPActionInvocationImpl( this ));
	}
}
