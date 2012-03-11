/*
 * Created on 14-Jun-2004
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
*  AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.net.udp.mc.impl;

import java.net.*;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import com.aelitis.net.udp.mc.MCGroup;
import com.aelitis.net.udp.mc.MCGroupAdapter;
import com.aelitis.net.udp.mc.MCGroupException;


/**
 * @author parg
 *
 */

public class 
MCGroupImpl 
	implements MCGroup
{
	private final static int		TTL					= 4;
	
	private final static int		PACKET_SIZE		= 8192;
			

	private static Map			singletons	= new HashMap();
	private static AEMonitor	class_mon 	= new AEMonitor( "MCGroup:class" );

	public static MCGroupImpl
	getSingleton(
		MCGroupAdapter		adapter,
		String				group_address,
		int					group_port,
		int					control_port,
		String[]			interfaces )
	
		throws MCGroupException
	{
		try{
			class_mon.enter();
		
			String	key = group_address + ":" + group_port + ":" + control_port;
			
			MCGroupImpl	singleton = (MCGroupImpl)singletons.get( key );
			
			if ( singleton == null ){
				
				if ( control_port == 0 ){
					
					int	last_allocated = COConfigurationManager.getIntParameter( "mcgroup.ports." + key, 0 );
					
					if ( last_allocated != 0 ){
												
						try{
							DatagramSocket test_socket = new DatagramSocket( null );
							
							test_socket.setReuseAddress( false );
								
							test_socket.bind( new InetSocketAddress( last_allocated ));
							
							test_socket.close();

							control_port = last_allocated;
							
						}catch( Throwable e ){
							
							e.printStackTrace();
						}
					}
				}
				
				singleton = new MCGroupImpl( adapter, group_address, group_port, control_port, interfaces );
				
				if ( control_port == 0 ){
					
					control_port = singleton.getControlPort();
					
					COConfigurationManager.setParameter( "mcgroup.ports." + key, control_port );
				}
				
				singletons.put( key, singleton );
			}
			
			return( singleton );
			
		}finally{
			
			class_mon.exit();
		}
	}
	
	private MCGroupAdapter		adapter;
	
	private String				group_address_str;
	private int					group_port;
	private int					control_port;
	protected InetSocketAddress 	group_address;
	private String[]				selected_interfaces;
	

	private boolean		ttl_problem_reported	= true;	// remove these diagnostic reports on win98
	private boolean		sso_problem_reported	= true; // remove these diagnostic reports on win98
			
	protected AEMonitor		this_mon	= new AEMonitor( "MCGroup" );

	private Map	current_registrations = new HashMap();
	
	private AsyncDispatcher		async_dispatcher = new AsyncDispatcher();
	
	
	public
	MCGroupImpl(
		MCGroupAdapter		_adapter,
		String				_group_address,
		int					_group_port,
		int					_control_port,
		String[]			_interfaces )
	
		throws MCGroupException
	{	
		adapter	= _adapter;

		group_address_str	= _group_address;
		group_port			= _group_port;
		control_port		= _control_port;
		selected_interfaces	= _interfaces;
		
		try{	
			group_address = new InetSocketAddress(InetAddress.getByName(group_address_str), 0 );

			processNetworkInterfaces( true );
					
			SimpleTimer.addPeriodicEvent(
				"MCGroup:refresher",
				60*1000,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event )
					{
						try{
							processNetworkInterfaces( false );
							
						}catch( Throwable e ){
							
							adapter.log(e);
						}
					}
				});
						
		}catch( Throwable e ){
						
			throw( new MCGroupException( "Failed to initialise MCGroup", e ));
		}
	}
	
	protected void
	processNetworkInterfaces(
		boolean		start_of_day )
	
		throws SocketException
	{
		Map			new_registrations	= new HashMap();
		
		List		changed_interfaces	= new ArrayList();
		
		try{
			this_mon.enter();
			
			Enumeration network_interfaces = NetworkInterface.getNetworkInterfaces();
			
			while (network_interfaces.hasMoreElements()){
				
				final NetworkInterface network_interface = (NetworkInterface)network_interfaces.nextElement();
	
				if ( !interfaceSelected( network_interface )){
					
					if ( start_of_day ){
						
						adapter.trace( "ignoring interface " + network_interface.getName() + ":" + network_interface.getDisplayName() + ", not selected" );
					}
					
					continue;
				}
				
				Set old_address_set = (Set)current_registrations.get( network_interface );
					
				if ( old_address_set == null ){
				
					old_address_set	= new HashSet();
				}
				
				Set	new_address_set = new HashSet();
				
				new_registrations.put( network_interface, new_address_set );
				
				Enumeration ni_addresses = network_interface.getInetAddresses();
				
				while (ni_addresses.hasMoreElements()){
					
					final InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
	
					new_address_set.add( ni_address );

					if ( old_address_set.contains( ni_address )){
								
							// already established
						
						continue;
					}
						// turn on loopback to see if it helps for local host UPnP devices
						// nah, turn it off again, it didn;t
					
					if ( ni_address.isLoopbackAddress()){
						
						if ( start_of_day ){
							
							adapter.trace( "ignoring loopback address " + ni_address + ", interface " + network_interface.getName());
						}
						
						continue;
					}
					
					if ( ni_address instanceof Inet6Address ){
			
						if ( start_of_day ){
							
							adapter.trace( "ignoring IPv6 address " + ni_address + ", interface " + network_interface.getName());
						}
						
						continue;
					}
					
					if ( !start_of_day ){
						
						if ( !changed_interfaces.contains( network_interface )){
							
							changed_interfaces.add( network_interface );
						}
					}
					
					try{
							// set up group
						
						final MulticastSocket mc_sock = new MulticastSocket( group_port );
										
						mc_sock.setReuseAddress(true);
						
							// windows 98 doesn't support setTimeToLive
						
						try{
							mc_sock.setTimeToLive(TTL);
							
						}catch( Throwable e ){
							
							if ( !ttl_problem_reported ){
								
								ttl_problem_reported	= true;
								
								adapter.log( e );
							}
						}
						
						String	addresses_string = "";
							
						Enumeration it = network_interface.getInetAddresses();
						
						while (it.hasMoreElements()){
							
							InetAddress addr = (InetAddress)it.nextElement();
							
							addresses_string += (addresses_string.length()==0?"":",") + addr;
						}
						
						adapter.trace( "group = " + group_address +"/" + 
										network_interface.getName()+":"+ 
										network_interface.getDisplayName() + "-" + addresses_string +": started" );
						
						mc_sock.joinGroup( group_address, network_interface );
					
						mc_sock.setNetworkInterface( network_interface );
						
							// note that false ENABLES loopback mode which is what we want 
						
						mc_sock.setLoopbackMode(false);
											
						Runtime.getRuntime().addShutdownHook(
								new AEThread("MCGroup:VMShutdown")
								{
									public void
									runSupport()
									{
										try{
											mc_sock.leaveGroup( group_address, network_interface );
											
										}catch( Throwable e ){
											
											adapter.log( e );
										}
									}
								});
						
						new AEThread2("MCGroup:MCListener", true )
							{
								public void
								run()
								{
									handleSocket( network_interface, ni_address, mc_sock, true );
								}
							}.start();
						
					}catch( Throwable e ){
						
						adapter.log( e );
					}						
				
						// now do the incoming control listener
					
					try{
						final DatagramSocket control_socket = new DatagramSocket( null );
							
						control_socket.setReuseAddress( true );
							
						control_socket.bind( new InetSocketAddress(ni_address, control_port ));
		
						if ( control_port == 0 ){
							
							control_port	= control_socket.getLocalPort();
							
							// System.out.println( "local port = " + control_port );
						}
						
						new AEThread2( "MCGroup:CtrlListener", true )
							{
								public void
								run()
								{
									handleSocket( network_interface, ni_address, control_socket, false );
								}
							}.start();
														
					}catch( Throwable e ){
					
						adapter.log( e );
					}
				}
			}
		}finally{
			
			current_registrations	= new_registrations;
			
			this_mon.exit();
		}
		
		for (int i=0;i<changed_interfaces.size();i++){
			
			adapter.interfaceChanged((NetworkInterface)changed_interfaces.get(i));
		}
	}
	
	public int
	getControlPort()
	{
		return( control_port );
	}
	
	protected boolean
	interfaceSelected(
		NetworkInterface	ni )
	{
		if ( selected_interfaces != null && selected_interfaces.length > 0 ){
			
			boolean	ok 	= false;
			
			for (int i=0;i<selected_interfaces.length;i++){
			
				if ( ni.getName().equalsIgnoreCase( selected_interfaces[i] )){
					
					ok	= true;
					
					break;
				}
			}
			
			return( ok );
		}else{
			
			return( true );
		}
	}
	
	protected boolean
	validNetworkAddress(
		final NetworkInterface	network_interface,
		final InetAddress		ni_address )
	{
		try{
			this_mon.enter();
		
			Set	set = (Set)current_registrations.get( network_interface );
			
			if ( set == null ){
				
				return( false );
			}
			
			return( set.contains( ni_address ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	

	public void
	sendToGroup(
		final byte[]	data )
	{	
			// have debugs showing the send-to-group operation hanging and blocking AZ close, make async
		
		async_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendToGroupSupport( data );
				}
			});
	}
	
	private void
	sendToGroupSupport(
		byte[]	data )
	{	
		try{
			Enumeration	x = NetworkInterface.getNetworkInterfaces();
			
			while( x != null && x.hasMoreElements()){
				
				NetworkInterface	network_interface = (NetworkInterface)x.nextElement();
				
				if ( !interfaceSelected( network_interface )){
					
					continue;
				}
				
				Enumeration ni_addresses = network_interface.getInetAddresses();
				
				boolean	ok = false;
				
				while( ni_addresses.hasMoreElements()){
					
					InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
				
					if ( !( ni_address instanceof Inet6Address || ni_address.isLoopbackAddress())){
						
						ok	= true;
						
						break;
					}
				}
				
				if ( !ok ){
					
					continue;
				}
				
				try{
					
					MulticastSocket mc_sock = new MulticastSocket(null);
	
					mc_sock.setReuseAddress(true);
					
					try{
						mc_sock.setTimeToLive( TTL );
						
					}catch( Throwable e ){
						
						if ( !ttl_problem_reported ){
							
							ttl_problem_reported	= true;
							
							adapter.log( e );
						}
					}
					
					mc_sock.bind( new InetSocketAddress( control_port ));
	
					mc_sock.setNetworkInterface( network_interface );
					
					// System.out.println( "sendToGroup: ni = " + network_interface.getName() + ", data = " + new String(data));
					
					DatagramPacket packet = new DatagramPacket(data, data.length, group_address.getAddress(), group_port );
					
					mc_sock.send(packet);
					
					mc_sock.close();
						
				}catch( Throwable e ){
				
					if ( !sso_problem_reported ){
						
						sso_problem_reported	= true;
					
						adapter.log( e );
					}
				}
			}
		}catch( Throwable e ){
		}
	}
	
	public void
	sendToGroup(
		final String	param_data )
	{	
			// have debugs showing the send-to-group operation hanging and blocking AZ close, make async
		
		async_dispatcher.dispatch(
			new AERunnable()
			{
				public void
				runSupport()
				{
					sendToGroupSupport( param_data );
				}
			});
	}
	
	private void
	sendToGroupSupport(
		String	param_data )
	{	
		try{
			Enumeration	x = NetworkInterface.getNetworkInterfaces();
			
			while( x != null && x.hasMoreElements()){
				
				NetworkInterface	network_interface = (NetworkInterface)x.nextElement();
				
				if ( !interfaceSelected( network_interface )){
					
					continue;
				}
				
				Enumeration ni_addresses = network_interface.getInetAddresses();
								
				InetAddress	an_address = null;
				
				while( ni_addresses.hasMoreElements()){
					
					InetAddress ni_address = (InetAddress)ni_addresses.nextElement();
				
					if ( !( ni_address instanceof Inet6Address || ni_address.isLoopbackAddress())){
						
						an_address	= ni_address;
						
						break;
					}
				}
				
				if ( an_address == null){
					
					continue;
				}
				
				try{
					
					MulticastSocket mc_sock = new MulticastSocket(null);
	
					mc_sock.setReuseAddress(true);
					
					try{
						mc_sock.setTimeToLive( TTL );
						
					}catch( Throwable e ){
						
						if ( !ttl_problem_reported ){
							
							ttl_problem_reported	= true;
							
							adapter.log( e );
						}
					}
					
					mc_sock.bind( new InetSocketAddress( control_port ));
	
					mc_sock.setNetworkInterface( network_interface );
					
					byte[]	data = param_data.replaceAll("%AZINTERFACE%", an_address.getHostAddress()).getBytes();
					
					// System.out.println( "sendToGroup: ni = " + network_interface.getName() + ", data = " + new String(data));
					
					DatagramPacket packet = new DatagramPacket(data, data.length, group_address.getAddress(), group_port );
					
					mc_sock.send(packet);
					
					mc_sock.close();
						
				}catch( Throwable e ){
				
					if ( !sso_problem_reported ){
						
						sso_problem_reported	= true;
					
						adapter.log( e );
					}
				}
			}
		}catch( Throwable e ){
		}
	}
	
	protected void
	handleSocket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		DatagramSocket		socket,
		boolean				log_on_stop )
	{
		long	successful_accepts 	= 0;
		long	failed_accepts		= 0;

		int	port = socket.getLocalPort();
		
		try{
				// introduce a timeout so that when a Network interface changes we don't sit here
				// blocking forever and thus never realise that we should shutdown
			
			socket.setSoTimeout( 30000 );
			
		}catch( Throwable e ){
			
		}
		
		while(true){
			
			if ( !validNetworkAddress( network_interface, local_address )){
				
				if ( log_on_stop ){
					
					adapter.trace( 
							"group = " + group_address +"/" + 
							network_interface.getName()+":"+ 
							network_interface.getDisplayName() + " - " + local_address + ": stopped" );
				}
				
				return;
			}
			
			try{
				byte[] buf = new byte[PACKET_SIZE];
				
				DatagramPacket packet = new DatagramPacket(buf, buf.length );
									
				socket.receive( packet );
					
				successful_accepts++;
				
				failed_accepts	 = 0;
				
				receivePacket( network_interface, local_address, packet );
				
			}catch( SocketTimeoutException e ){
				
			}catch( Throwable e ){
				
				failed_accepts++;
				
				adapter.trace( "MCGroup: receive failed on port " + port + ":" + e.getMessage()); 

				if (( failed_accepts > 100 && successful_accepts == 0 ) || failed_accepts > 1000 ){
					
					adapter.trace( "    too many failures, abandoning" );

					break;
				}
			}
		}
	}
	
	protected void
	receivePacket(
		NetworkInterface	network_interface,
		InetAddress			local_address,
	    DatagramPacket		packet )
	{
		byte[]	data 	= packet.getData();
		int		len		= packet.getLength();
		
		// System.out.println( "receive: add = " + local_address + ", data = " + new String( data, 0, len ));

		adapter.received( 
				network_interface, 
				local_address, 
				(InetSocketAddress)packet.getSocketAddress(), 
				data, 
				len );
	}
	
	public void
	sendToMember(
		InetSocketAddress	address,
		byte[]				data )
	
		throws MCGroupException
	{
		DatagramSocket	reply_socket	= null;
			
		// System.out.println( "sendToMember: add = " + address + ", data = " +new String( data ));

		try{
			reply_socket = new DatagramSocket( null );
			
			reply_socket.setReuseAddress(true);

			reply_socket.bind( new InetSocketAddress( group_port ));
			
			DatagramPacket reply_packet = new DatagramPacket(data,data.length,address);
						
			reply_socket.send( reply_packet );
			
		}catch( Throwable e ){
			
			throw( new MCGroupException( "sendToMember failed", e ));
			
		}finally{
			
			if ( reply_socket != null ){
				
				try{
					reply_socket.close();
					
				}catch( Throwable e ){
				}
			}
		}	
	}
}
