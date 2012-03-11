/*
 * Created on Jan 31, 2008
 * Created by Paul Gardner
 * 
 * Copyright 2008 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.plugins.net.netstatus.swt;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.*;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.admin.*;
import com.aelitis.azureus.plugins.net.netstatus.NetStatusPlugin;
import com.aelitis.azureus.plugins.net.netstatus.NetStatusProtocolTesterBT;
import com.aelitis.azureus.plugins.net.netstatus.NetStatusProtocolTesterListener;

public class 
NetStatusPluginTester 
{
	public static final int		TEST_PING_ROUTE		= 0x00000001;
	public static final int		TEST_NAT_PROXIES	= 0x00000002;
	public static final int		TEST_OUTBOUND		= 0x00000004;
	public static final int		TEST_INBOUND		= 0x00000008;
	public static final int		TEST_BT_CONNECT		= 0x00000010;

	
	private static final int	ROUTE_TIMEOUT	= 120*1000;

	private NetStatusPlugin		plugin;
	private int					test_types;
	private loggerProvider		logger;
	
	private volatile boolean	test_cancelled;
	
	public
	NetStatusPluginTester(
		NetStatusPlugin		_plugin,
		int					_test_types,
		loggerProvider		_logger )
	{
		plugin		= _plugin;
		test_types	= _test_types;
		logger		= _logger;
	}
	
	protected boolean
	doTest(
		int		type )
	{
		return((test_types & type ) != 0 );
	}
	
	public void
	run(AzureusCore core)
	{
		final NetworkAdmin	admin = NetworkAdmin.getSingleton();
		
		boolean	checked_public	= false;

		Set<InetAddress>	public_addresses = new HashSet<InetAddress>();
		
		InetAddress def_pa = admin.getDefaultPublicAddress();
		
		if ( def_pa != null ){
			
			log( "Default public address is " + def_pa.getHostAddress());
			
			addPublicAddress( public_addresses, def_pa );
			
			checked_public = true;
		}
		
		if ( doTest( TEST_PING_ROUTE )){
			
			log( "Testing routing for the following interfaces:" );
			
			NetworkAdminNetworkInterface[] interfaces = admin.getInterfaces();
			
			for (int i=0;i<interfaces.length;i++){
				
				NetworkAdminNetworkInterface	intf = interfaces[i];
				
				NetworkAdminNetworkInterfaceAddress[] addresses = intf.getAddresses();
				
				String	a_str = "";
				
				for (int j=0;j<addresses.length;j++){
					
					NetworkAdminNetworkInterfaceAddress address = addresses[j];
					
					InetAddress ia = address.getAddress();
					
					if ( ia.isLoopbackAddress() || ia instanceof Inet6Address ){
						
					}else{
						
						a_str += (a_str.length()==0?"":",") + ia.getHostAddress();
					}
				}
				
				if ( a_str.length() > 0 ){
					
					log( "    " + intf.getName() + "/" + intf.getDisplayName() + ": " + a_str );
				}
			}
			
			if ( admin.canPing()){
				
				log( "Running ping tests" );
				
				try{
					InetAddress	target_address = InetAddress.getByName( plugin.getPingTarget());
					
					final Map	active_pings = new HashMap();
					
					admin.pingTargets(
						target_address, 
						ROUTE_TIMEOUT, 
						new NetworkAdminRoutesListener()
						{
							private int	timeouts;
							
							public boolean
							foundNode(
								NetworkAdminNetworkInterfaceAddress		intf,
								NetworkAdminNode[]						route,
								int										distance,
								int										rtt )
							{
								if ( test_cancelled ){
									
									return( false );
								}
								
								synchronized( active_pings ){
									
									active_pings.put( intf, route );
								}
								
								log( "  " + intf.getAddress().getHostAddress() + " -> " + route[route.length-1].getAddress().getHostAddress());
								
								return( false );
							}
							
							public boolean
							timeout(
								NetworkAdminNetworkInterfaceAddress		intf,
								NetworkAdminNode[]						route,
								int										distance )
							{
								if ( test_cancelled ){
									
									return( false );
								}
								
								log( "  " + intf.getAddress().getHostAddress() + " - timeout" );
								
								timeouts++;
								
								if ( timeouts >= 3 ){
									
									return( false );
								}
								
								return( true );
							}
						});
		
					if ( test_cancelled ){
						
						return;
					}
					
					int	num_routes = active_pings.size();
					
					if ( num_routes == 0 ){
						
						logError( "No active pings found!" );
						
					}else{
						
						log( "Found " + num_routes + " pings(s)" );
						
						Iterator it = active_pings.entrySet().iterator();
						
						while( it.hasNext()){
							
							Map.Entry entry = (Map.Entry)it.next();
							
							NetworkAdminNetworkInterfaceAddress address = (NetworkAdminNetworkInterfaceAddress)entry.getKey();
							
							NetworkAdminNode[]	route = (NetworkAdminNode[])entry.getValue();
							
							String	node_str = "";
							
							for (int i=0;i<route.length;i++){
								
								node_str += (i==0?"":",") + route[i].getAddress().getHostAddress();
							}
							
							log( "    " + address.getInterface().getName() + "/" + address.getAddress().getHostAddress() + " - " + node_str );
						}
					}
				}catch( Throwable e ){
					
					logError( "Pinging failed: " + Debug.getNestedExceptionMessage(e));
				}
			}else{
				
				logError( "Can't run ping test as not supported" );
			}
			
			if ( test_cancelled ){
				
				return;
			}
			
			if ( admin.canTraceRoute()){
				
				log( "Running trace route tests" );
				
				try{
					InetAddress	target_address = InetAddress.getByName( plugin.getPingTarget());
					
					final Map	active_routes = new HashMap();
					
					admin.getRoutes( 
						target_address, 
						ROUTE_TIMEOUT, 
						new NetworkAdminRoutesListener()
						{
							private String	last_as = "";
							
							public boolean
							foundNode(
								NetworkAdminNetworkInterfaceAddress		intf,
								NetworkAdminNode[]						route,
								int										distance,
								int										rtt )
							{
								if ( test_cancelled ){
									
									return( false );
								}
								
								synchronized( active_routes ){
									
									active_routes.put( intf, route );
								}
								
								InetAddress ia = route[route.length-1].getAddress();
								
								String	as = "";
								
								if ( !ia.isLinkLocalAddress() && !ia.isSiteLocalAddress()){
									
									try{
										NetworkAdminASN asn = admin.lookupASN( ia );
										
										as = asn.getString();
										
										if ( as.equals( last_as )){
											
											as = "";
											
										}else{
											
											last_as = as;
										}
									}catch( Throwable e ){
										
									}
								}
								
								log( "  " + intf.getAddress().getHostAddress() + " -> " + ia.getHostAddress() + " (hop=" + distance + ")" + (as.length()==0?"":( " - " + as )));
								
								return( true );
							}
							
							public boolean
							timeout(
								NetworkAdminNetworkInterfaceAddress		intf,
								NetworkAdminNode[]						route,
								int										distance )
							{
								if ( test_cancelled ){
									
									return( false );
								}
								
								log( "  " + intf.getAddress().getHostAddress() + " - timeout (hop=" + distance + ")" );
		
									// see if we're getting nowhere
								
								if ( route.length == 0 && distance >= 5 ){
								
									logError( "    giving up, no responses" );
									
									return( false );
								}
								
									// see if we've got far enough
								
								if ( route.length >= 5 && distance > 6 ){
									
									log( "    truncating, sufficient responses" );
		
									return( false );
								}
								
								return( true );
							}
						});
		
					if ( test_cancelled ){
						
						return;
					}
					
					int	num_routes = active_routes.size();
					
					if ( num_routes == 0 ){
						
						logError( "No active routes found!" );
						
					}else{
						
						log( "Found " + num_routes + " route(s)" );
						
						Iterator it = active_routes.entrySet().iterator();
						
						while( it.hasNext()){
							
							Map.Entry entry = (Map.Entry)it.next();
							
							NetworkAdminNetworkInterfaceAddress address = (NetworkAdminNetworkInterfaceAddress)entry.getKey();
							
							NetworkAdminNode[]	route = (NetworkAdminNode[])entry.getValue();
							
							String	node_str = "";
							
							for (int i=0;i<route.length;i++){
								
								node_str += (i==0?"":",") + route[i].getAddress().getHostAddress();
							}
							
							log( "    " + address.getInterface().getName() + "/" + address.getAddress().getHostAddress() + " - " + node_str );
						}
					}
				}catch( Throwable e ){
					
					logError( "Route tracing failed: " + Debug.getNestedExceptionMessage(e));
				}
			}else{
					
				logError( "Can't run trace route test as not supported" );
			}
			
			if ( test_cancelled ){
				
				return;
			}
		}
		
		if ( doTest( TEST_NAT_PROXIES )){
	
			checked_public = true;
			
			NetworkAdminNATDevice[] nat_devices = admin.getNATDevices(core);
			
			log( nat_devices.length + " NAT device" + (nat_devices.length==1?"":"s") + " found" );
			
			for (int i=0;i<nat_devices.length;i++){
				
				NetworkAdminNATDevice device = nat_devices[i];
				
				InetAddress ext_address = device.getExternalAddress();
				
				addPublicAddress( public_addresses, ext_address );
				
				log( "    " + device.getString());
			}
			
			NetworkAdminSocksProxy[] socks_proxies = admin.getSocksProxies();
			
			if ( socks_proxies.length == 0 ){
				
				log( "No SOCKS proxy found" );

			}else if (  socks_proxies.length == 1 ){
				
				log( "One SOCKS proxy found" );
				
			}else{
				
				log( socks_proxies.length + " SOCKS proxies found" );
			}
			
			for (int i=0;i<socks_proxies.length;i++){
				
				NetworkAdminSocksProxy proxy = socks_proxies[i];
				
				log( "    " + proxy.getString());
			}
			
			NetworkAdminHTTPProxy http_proxy = admin.getHTTPProxy();
			
			if ( http_proxy == null ){
				
				log( "No HTTP proxy found" );
				
			}else{
				
				log( "HTTP proxy found" );
				
				log( "    " + http_proxy.getString());
			}
		}
		
		InetAddress[] bind_addresses = admin.getAllBindAddresses( false );
		
		int	num_binds = 0;
		
		for ( int i=0;i<bind_addresses.length;i++ ){
		
			if ( bind_addresses[i] != null ){
				
				num_binds++;
			}
		}
		
		if ( num_binds == 0 ){
			
			log( "No explicit bind address set" );
			
		}else{
		
			log( num_binds + " bind addresses" );
			
			for ( int i=0;i<bind_addresses.length;i++ ){
		
				if ( bind_addresses[i] != null ){
				
					log( "    " + bind_addresses[i].getHostAddress());
				}
			}
		}
		
		if ( doTest( TEST_OUTBOUND )){

			checked_public = true;
			
			NetworkAdminProtocol[] outbound_protocols = admin.getOutboundProtocols(core);
			
			if ( outbound_protocols.length == 0 ){
				
				log( "No outbound protocols" );
				
			}else{
				
				for (int i=0;i<outbound_protocols.length;i++){
					
					if ( test_cancelled ){
						
						return;
					}
					
					NetworkAdminProtocol protocol = outbound_protocols[i];
					
					log( "Testing " + protocol.getName());
					
					try{
						InetAddress public_address = 
							protocol.test( 
								null,
								new NetworkAdminProgressListener()
								{
									public void 
									reportProgress(
										String task )
									{
										log( "    " + task );
									}
								});
						
						logSuccess( "    Test successful" );
						
						addPublicAddress( public_addresses, public_address );
						
					}catch( Throwable e ){
						
						logError( "    Test failed", e );
					}
				}
			}
		}
		
		if ( doTest( TEST_INBOUND )){

			checked_public = true;
			
			NetworkAdminProtocol[] inbound_protocols = admin.getInboundProtocols(core);
			
			if ( inbound_protocols.length == 0 ){
				
				log( "No inbound protocols" );
				
			}else{
				
				for (int i=0;i<inbound_protocols.length;i++){
					
					if ( test_cancelled ){
						
						return;
					}
					
					NetworkAdminProtocol protocol = inbound_protocols[i];
					
					log( "Testing " + protocol.getName());
					
					try{
						InetAddress public_address = 
							protocol.test( 
								null,
								new NetworkAdminProgressListener()
								{
									public void 
									reportProgress(
										String task )
									{
										log( "    " + task );
									}
								});
						
						logSuccess( "    Test successful" );
	
						addPublicAddress( public_addresses, public_address );
						
					}catch( Throwable e ){
						
						logError( "    Test failed", e );
						logInfo(  "    Check your port forwarding for " + protocol.getTypeString() + " " + protocol.getPort());
					}
				}
			}
		}
		
		if ( checked_public ){
			
			if ( public_addresses.size() == 0 ){
				
				log( "No public addresses found" );
				
			}else{
				
				Iterator<InetAddress>	it = public_addresses.iterator();
				
				log( public_addresses.size() + " public/external addresses found" );
				
				while( it.hasNext()){
					
					InetAddress	pub_address = it.next();
					
					log( "    " + pub_address.getHostAddress());
					
					try{
						NetworkAdminASN asn = admin.lookupASN(pub_address);
						
						log( "    AS details: " + asn.getString());
						
					}catch( Throwable e ){
						
						logError( "    failed to lookup AS", e );
					}
				}
			}
		}
		
		if ( doTest( TEST_BT_CONNECT )){

			log( "Distributed protocol test" );
			
			NetStatusProtocolTesterBT bt_test = 
				plugin.getProtocolTester().runTest(
				
					new NetStatusProtocolTesterListener()
					{
						private List	sessions = new ArrayList();
						
						public void
						complete(
							NetStatusProtocolTesterBT	tester )
						{
							log( "Results" );
							
							if ( tester.getOutboundConnects() < 4 ){
								
								log( "    insufficient outbound connects for analysis" );
								
								return;
							}
							
							int outgoing_seed_ok		= 0;
							int outgoing_leecher_ok		= 0;
							int outgoing_seed_bad		= 0;
							int outgoing_leecher_bad	= 0;
							
							int incoming_connect_ok	= 0;						
							
							for (int i=0;i<sessions.size();i++){
								
								NetStatusProtocolTesterBT.Session session = (NetStatusProtocolTesterBT.Session)sessions.get(i);
								
								if ( session.isOK()){
									
									if ( session.isInitiator()){
										
										if ( session.isSeed()){
											
											outgoing_seed_ok++;
											
										}else{
											
											outgoing_leecher_ok++;
										}
									}else{
									
										incoming_connect_ok++;
									}
								}else{
	
									if ( session.isConnected()){
										
										if ( session.isInitiator()){
											
											if ( session.isSeed()){
												
												outgoing_seed_bad++;
												
											}else{
												
												outgoing_leecher_bad++;
											}
										}else{
											
											incoming_connect_ok++;										
										}
									}
								}
								
								log( "  " + 
										( session.isInitiator()?"Outbound":"Inbound" ) + "," + 
										( session.isSeed()?"Seed":"Leecher") + "," + 
										session.getProtocolString());
							}
							
							if ( incoming_connect_ok == 0 ){
								
								logError( "  No incoming connections received, likely NAT problems" );
							}
							
							if ( 	outgoing_leecher_ok > 0 &&
									outgoing_seed_ok == 0 &&
									outgoing_seed_bad > 0 ){
								
								logError( "  Outgoing seed connects appear to be failing while non-seeds succeed" );
							}
						}
						
						public void
						sessionAdded(
							NetStatusProtocolTesterBT.Session	session )
						{
							synchronized( sessions ){
								
								sessions.add( session );
							}
						}
						
						public void
						log(
							String		str )
						{
							NetStatusPluginTester.this.log( "  " + str );
						}
						
						public void
						logError(
							String		str )
						{
							NetStatusPluginTester.this.logError( "  " + str );
						}
						
						public void
						logError(
							String		str,
							Throwable	e )
						{
							NetStatusPluginTester.this.logError( "  " + str, e );
						}
					});
			
			while( !bt_test.waitForCompletion( 5000 )){
				
				if ( isCancelled()){
					
					bt_test.destroy();
					
					break;
				}
				
				log( "    Status: " + bt_test.getStatus());
			}
		}
	}
	
	protected void
	addPublicAddress(
		Set<InetAddress>	addresses,
		InetAddress			address )
	{
		if ( address == null ){
			
			return;
		}
			
		if ( 	address.isAnyLocalAddress() ||
				address.isLoopbackAddress() ||
				address.isLinkLocalAddress()||
				address.isSiteLocalAddress()){
			
				return;
		}
		
		addresses.add( address );
	}
	
	public void
	cancel()
	{
		test_cancelled	= true;
	}
	
	public boolean
	isCancelled()
	{
		return( test_cancelled );
	}
	
	protected void
	log(
		String	str )
	{
		logger.log( str );
	}
	
	protected void
	logSuccess(
		String	str )
	{
		logger.logSuccess( str );
	}
	
	protected void
	logInfo(
		String	str )
	{
		logger.logInfo( str );
	}
	
	protected void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str + ": " + e.getLocalizedMessage());
	}
	
	protected void
	logError(
		String	str )
	{
		logger.logFailure( str );
	}
	
	protected void
	logError(
		String		str,
		Throwable	e )
	{
		logger.logFailure( str + ": " + e.getLocalizedMessage());
	}
	
	public interface
	loggerProvider
	{
		public void
		log(
			String	str );
		
		public void
		logSuccess(
			String	str );
		
		public void
		logInfo(
			String	str );
		
		public void
		logFailure(
			String	str );
	}
	
}
