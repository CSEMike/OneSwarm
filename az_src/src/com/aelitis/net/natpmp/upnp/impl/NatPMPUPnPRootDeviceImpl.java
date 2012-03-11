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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Map;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.ThreadPool;

import com.aelitis.net.natpmp.NatPMPDevice;
import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.UPnPSpecificService;
import com.aelitis.net.upnp.services.UPnPWANConnection;
import com.aelitis.net.upnp.services.UPnPWANConnectionListener;
import com.aelitis.net.upnp.services.UPnPWANConnectionPortMapping;

public class 
NatPMPUPnPRootDeviceImpl
	implements UPnPRootDevice
{
	private UPnP			upnp;
	private NatPMPDevice	nat_device;
	
	private String			USN		= "natpmp";
	private URL				location;
	
	private UPnPDevice		device;
	private UPnPService[]	services;
	
	private ThreadPool		thread_pool;
	
	public
	NatPMPUPnPRootDeviceImpl(
		UPnP			_upnp,
		NatPMPDevice	_nat_device )
	
		throws Exception
	{
		upnp		= _upnp;
		nat_device	= _nat_device;
		
		location = new URL( "http://undefined/" );
		
		device = new NatPMPUPnPDevice();
		
		services = new UPnPService[]{ new NatPMPUPnPWANConnection() };
		
		thread_pool = new ThreadPool( "NatPMPUPnP", 1, true );
	}
	
	public UPnP
	getUPnP()
	{
		return( upnp );
	}
	
	public String
	getUSN()
	{
		return( USN );
	}
	
	public URL
	getLocation()
	{
		return( location );
	}
	
	public InetAddress
	getLocalAddress()
	{
		return( nat_device.getLocalAddress());
	}
	
	public NetworkInterface
	getNetworkInterface()
	{
		return( nat_device.getNetworkInterface());
	}
	
	public String
	getInfo()
	{
		return( "Nat-PMP" );
	}
	
	public UPnPDevice
	getDevice()
	{
		return( device );
	}
		
	public boolean
	isDestroyed()
	{
		return( false );
	}
	
	public Map
	getDiscoveryCache() 
	{
		return( null );
	}
	
	public void
	addListener(
		UPnPRootDeviceListener	l )
	{
	}
	
	public void
	removeListener(
		UPnPRootDeviceListener	l )
	{
	}
	
	protected class
	NatPMPUPnPDevice
		implements UPnPDevice
	{
		public String
		getDeviceType()
		{
			return( "NatPMP" );
		}
		
		public String
		getFriendlyName()
		{
			return( "NatPMP" );
		}
		
		public String
		getManufacturer()
		{
			return( "" );
		}
		
		public String
		getManufacturerURL()
		{
			return( "" );
		}
		
		public String
		getModelDescription()
		{
			return( "" );
		}
		
		public String
		getModelName()
		{
			return( "" );
		}
		
		public String
		getModelNumber()
		{
			return( "" );
		}
		
		public String
		getModelURL()
		{
			return( "" );
		}
		
		public String 
		getPresentation() 
		{
			return( "" );
		}
		
		public UPnPDevice[]
		getSubDevices()
		{
			return( new UPnPDevice[0]);
		}
		
		public UPnPService[]
		getServices()
		{
			return( services );
		}
		
		public UPnPRootDevice
		getRootDevice()
		{
			return( NatPMPUPnPRootDeviceImpl.this );
		}

		public UPnPDeviceImage[] getImages() {
			return new UPnPDeviceImage[0];
		}
	}
	
	protected class
	NatPMPUPnPWANConnection
		implements UPnPWANConnection, UPnPService
	{
		private NatPMPImpl	nat_impl;
		
		protected
		NatPMPUPnPWANConnection()
		
			throws UPnPException
		{
			nat_impl	= new NatPMPImpl( nat_device );
		}
		
		public UPnPDevice
		getDevice()
		{
			return( device );
		}
		
		public String
		getServiceType()
		{
				// pretend to be an ip connection
			
			return( "urn:schemas-upnp-org:service:WANIPConnection:1" );
		}

		public String 
		getConnectionType() 
		{
			return( "IP" );	// ??
		}
		
		public URL
		getControlURL()
		
			throws UPnPException
		{
			return( null );
		}
		
		public boolean 
		isConnectable() 
		{
			return( true );
		}
		
		public UPnPAction[]
		getActions()
		
			throws UPnPException
		{
			return( new UPnPAction[0] );
		}
		
		public UPnPAction
		getAction(
			String		name )
		
			throws UPnPException
		{
			return( null );
		}
		
		public UPnPStateVariable[]
		getStateVariables()
		
			throws UPnPException
		{
			return( new UPnPStateVariable[0] );
		}
		
		public UPnPStateVariable
		getStateVariable(
			String		name )
		
			throws UPnPException
		{
			return( null );
		}
							
			/**
			 * gets a specific service if such is supported
			 * @return
			 */
		public UPnPSpecificService
		getSpecificService()
		{
			return( this );
		}
		
		public UPnPService
		getGenericService()
		{
			return( this );
		}
		
		public boolean
		getDirectInvocations()
		{
			return( true );
		}
		
		public void
		setDirectInvocations(
			boolean	force )
		{
		}
		
		
		public void
		addPortMapping(
			final boolean		tcp,
			final int			port,
			final String		description )
		
			throws UPnPException
		{
			thread_pool.run(
				new AERunnable()
				{
					public void
					runSupport()
					{
						try{
							
							nat_impl.addPortMapping( tcp, port, description );
							
						}catch( UPnPException e ){
							
							e.printStackTrace();
						}
					}
				});
		}
		
		public UPnPWANConnectionPortMapping[]
		getPortMappings()
		
			throws UPnPException
		{
			return( nat_impl.getPortMappings());
		}
		
		public void
		deletePortMapping(
			final boolean		tcp,
			final int			port )
		
			throws UPnPException
		{
			thread_pool.run(
					new AERunnable()
					{
						public void
						runSupport()
						{
							try{
								nat_impl.deletePortMapping( tcp, port );
								
							}catch( UPnPException e ){
								
								e.printStackTrace();
							}
						}
					});
		}
		
		public String[]
		getStatusInfo()
		
			throws UPnPException
		{
			return( nat_impl.getStatusInfo());
		}
		
		public String
		getExternalIPAddress()
		
			throws UPnPException
		{
			return( nat_impl.getExternalIPAddress());
		}
		
		public void
		periodicallyRecheckMappings(
			boolean	on )
		{
		}
		
		public int
		getCapabilities()
		{
			return( CAP_ALL );
		}
		
		public void
		addListener(
			UPnPWANConnectionListener	listener )
		{
		}
		
		public void
		removeListener(
			UPnPWANConnectionListener	listener )
		{
		}
	}
}
