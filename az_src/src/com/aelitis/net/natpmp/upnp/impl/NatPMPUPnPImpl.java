/*
 * Created on 12 Jun 2006
 * Created by Paul Gardner
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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

package com.aelitis.net.natpmp.upnp.impl;

import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.net.natpmp.NatPMPDevice;
import com.aelitis.net.natpmp.upnp.NatPMPUPnP;
import com.aelitis.net.upnp.UPnP;
import com.aelitis.net.upnp.UPnPListener;

public class 
NatPMPUPnPImpl 
	implements NatPMPUPnP
{
	private UPnP			upnp;
	private NatPMPDevice	nat_device;

	private NatPMPUPnPRootDeviceImpl	root_device;
	
	private List			listeners = new ArrayList();
	
	private boolean			enabled	= true;
	
	private boolean			started;
	
	public
	NatPMPUPnPImpl(
		UPnP			_upnp,
		NatPMPDevice	_nat_device )
	{
		upnp		= _upnp;
		nat_device	= _nat_device;
	}
	
	protected void
	start()
	{
		SimpleTimer.addPeriodicEvent(
			"NATPMP:search",
			60*1000,
			new TimerEventPerformer()
			{
				public void
				perform(
					TimerEvent	event )
				{
					search();
				}
			});
		
		search();
	}
	
	public void
	setEnabled(
		boolean	_enabled )
	{
		enabled	= _enabled;
	}
	
	public boolean
	isEnabled()
	{
		return( enabled );
	}
	
	protected void
	search()
	{
		if ( !enabled ){
			
			return;
		}
		
		if ( root_device != null ){
			
			return;
		}
		
		try{
			boolean found = nat_device.connect();
			
			if ( found ){
			
				root_device	= new NatPMPUPnPRootDeviceImpl(upnp, nat_device );
				
				for (int i=0;i<listeners.size();i++){
					
					try{
						((UPnPListener)listeners.get(i)).rootDeviceFound( root_device );
						
					}catch( Throwable e ){
						
						Debug.printStackTrace(e);
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	public synchronized void
	addListener(
		UPnPListener	listener )
	{
		listeners.add( listener );
		
		if ( root_device == null ){
			
			if ( listeners.size() == 1 && !started ){
			
				started	= true;
				
				start();
			}
		}else{
			
			listener.rootDeviceFound( root_device );
		}
	}
	
	public synchronized void
	removeListener(
		UPnPListener	listener )
	{
		listeners.remove( listener );
	}
}
