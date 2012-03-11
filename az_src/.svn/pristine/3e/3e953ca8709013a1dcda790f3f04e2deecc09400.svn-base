/*
 * Created on 23-Dec-2005
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

package com.aelitis.azureus.core.instancemanager.impl;

import java.util.*;


import com.aelitis.azureus.core.instancemanager.AZInstance;

public abstract class 
AZInstanceImpl 
	implements AZInstance
{
	protected
	AZInstanceImpl()
	{
	}
		
	protected void
	encode(
		Map<String,Object>		map )
	{
		map.put( "id", getID().getBytes());				
					
		map.put( "ai", getApplicationID().getBytes());
		
		map.put( "iip", getInternalAddress().getHostAddress().getBytes());
		
		map.put( "eip", getExternalAddress().getHostAddress().getBytes());
		
		map.put( "tp", new Long( getTCPListenPort()));
		
        map.put( "dp", new Long( getUDPListenPort()));
        
        map.put( "dp2", new Long( getUDPNonDataListenPort()));
        
        Map<String,Object> props = getProperties();
        
        if ( props != null ){
        	
        	map.put( "pr", props );
        }
	}
	
	public String
	getString()
	{
		String	id = getID();
		
		if ( id.length() > 8 ){
			
			id = id.substring(0,8) + "...";
		}
		
		return( "id=" + id + 
				",ap=" + getApplicationID() + 
				",int=" + getInternalAddress().getHostAddress() + 
				",ext=" + getExternalAddress().getHostAddress() +	
				",tcp=" + getTCPListenPort() + 
				",udp=" + getUDPListenPort() +
				",udp2=" + getUDPNonDataListenPort() + 
				",props=" + getProperties());
	}
}
