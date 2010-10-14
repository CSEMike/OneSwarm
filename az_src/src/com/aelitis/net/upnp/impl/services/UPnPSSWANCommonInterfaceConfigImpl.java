/*
 * Created on 16-Sep-2005
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

package com.aelitis.net.upnp.impl.services;

import com.aelitis.net.upnp.UPnPAction;
import com.aelitis.net.upnp.UPnPActionArgument;
import com.aelitis.net.upnp.UPnPActionInvocation;
import com.aelitis.net.upnp.UPnPException;
import com.aelitis.net.upnp.UPnPService;
import com.aelitis.net.upnp.services.UPnPWANCommonInterfaceConfig;

public class 
UPnPSSWANCommonInterfaceConfigImpl 
	implements UPnPWANCommonInterfaceConfig
{
	private UPnPServiceImpl		service;
	
	protected
	UPnPSSWANCommonInterfaceConfigImpl(
		UPnPServiceImpl		_service )
	{
		service = _service;
	}
	
	public UPnPService
	getGenericService()
	{
		return( service );
	}
	
	public long[]
	getCommonLinkProperties()
	
		throws UPnPException
	{
		UPnPAction act = service.getAction( "GetCommonLinkProperties" );
		
		if ( act == null ){
			
			service.getDevice().getRootDevice().getUPnP().log( "Action 'GetCommonLinkProperties' not supported, binding not established" );
			
			throw( new UPnPException( "GetCommonLinkProperties not supported" ));
			
		}else{
					
			UPnPActionInvocation inv = act.getInvocation();
						
			UPnPActionArgument[]	args = inv.invoke();
			
			long[]	res = new long[2];
			
			for (int i=0;i<args.length;i++){
				
				UPnPActionArgument	arg = args[i];
			
				String	name = arg.getName();
				
				if ( name.equalsIgnoreCase("NewLayer1UpstreamMaxBitRate")){
					
					res[1] = Long.parseLong( arg.getValue());
					
				}else if ( name.equalsIgnoreCase("NewLayer1DownstreamMaxBitRate")){
					
					res[0] = Long.parseLong( arg.getValue());
				}
			}
			
			return( res );
		}
	}
}
