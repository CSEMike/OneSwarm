/*
 * Created on 20-Dec-2005
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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;


import com.aelitis.azureus.core.instancemanager.AZInstance;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerAdapter;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerListener;
import com.aelitis.azureus.core.instancemanager.AZInstanceTracked;
import com.aelitis.azureus.core.util.NetUtils;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.net.udp.mc.MCGroup;
import com.aelitis.net.udp.mc.MCGroupAdapter;
import com.aelitis.net.udp.mc.MCGroupException;
import com.aelitis.net.udp.mc.MCGroupFactory;

public class 
AZInstanceManagerImpl 
	implements AZInstanceManager, MCGroupAdapter
{
	private static final boolean DISABLE_LAN_LOCAL_STUFF	= false;
	
	static{
		if ( DISABLE_LAN_LOCAL_STUFF ){
			System.out.println( "**** LAN LOCAL STUFF DISABLED ****" );
		}
	}
	
	private static final LogIDs LOGID = LogIDs.NET;
	
	private String				MC_GROUP_ADDRESS 	= "239.255.067.250";	// 239.255.000.000-239.255.255.255 
	private int					MC_GROUP_PORT		= 16680;				//
	private int					MC_CONTROL_PORT		= 0;

	private static final int	MT_VERSION		= 1;
	
	private static final int	MT_ALIVE		= 1;
	private static final int	MT_BYE			= 2;
	private static final int	MT_REQUEST		= 3;
	private static final int	MT_REPLY		= 4;
	
	private static final int	MT_REQUEST_SEARCH	= 1;
	private static final int	MT_REQUEST_TRACK	= 2;
	
	private static final long	ALIVE_PERIOD	= 30*60*1000;
	
	private static AZInstanceManagerImpl	singleton;
	
	private List	listeners	= new ArrayList();
	
	private static AEMonitor	class_mon = new AEMonitor( "AZInstanceManager:class" );
	
	private static String	socks_proxy	= null;
	
	static{
		COConfigurationManager.addAndFireParameterListeners(
			new String[]{ "Proxy.Data.Enable", "Proxy.Host", "Proxy.Data.Same", "Proxy.Data.Host" },
			new ParameterListener()
			{
				public void 
				parameterChanged(
					String parameterName )
				{
					if ( !COConfigurationManager.getBooleanParameter("Proxy.Data.Enable")){
						
						socks_proxy = null;
						
						return;
					}
					
					if ( COConfigurationManager.getBooleanParameter("Proxy.Data.Same")){
						
						socks_proxy = COConfigurationManager.getStringParameter( "Proxy.Host" );
						
					}else{
						
						socks_proxy = COConfigurationManager.getStringParameter( "Proxy.Data.Host" );

					}
					
					if ( socks_proxy != null ){
						
						socks_proxy	= socks_proxy.trim();
					}
				}
			});
	}
	
	public static AZInstanceManager
	getSingleton(
		AZInstanceManagerAdapter	core )
	{
		try{
			class_mon.enter();
			
			if ( singleton == null ){
				
				singleton = new AZInstanceManagerImpl( core );
			}
		}finally{
			
			class_mon.exit();
		}
		
		return( singleton );
	}
	
	private AZInstanceManagerAdapter	adapter;
	
	private MCGroup	 	mc_group;
	private long		search_id_next;
	private List		requests = new ArrayList();
	
	private AZMyInstanceImpl		my_instance;
	private Map						other_instances	= new HashMap();
	
	private volatile boolean		initialised;
	private volatile Map			tcp_lan_to_ext	= new HashMap();
	private volatile Map			udp_lan_to_ext	= new HashMap();
	private volatile Map			udp2_lan_to_ext	= new HashMap();
	private volatile Map			tcp_ext_to_lan	= new HashMap();
	private volatile Map			udp_ext_to_lan	= new HashMap();
	private volatile Map			udp2_ext_to_lan	= new HashMap();
	
	private volatile Set			lan_addresses	= new HashSet();
	private volatile Set			ext_addresses	= new HashSet();
	
	private volatile List			lan_subnets		= new ArrayList();
	private volatile List			explicit_peers 	= new ArrayList();
	
	private volatile boolean		include_well_known_lans	= true;
	
	private AESemaphore	initial_search_sem	= new AESemaphore( "AZInstanceManager:initialSearch" );
	private boolean		init_wait_abandoned;
	
	private AEMonitor	this_mon = new AEMonitor( "AZInstanceManager" );

	private boolean		closing;
	
	protected
	AZInstanceManagerImpl(
		AZInstanceManagerAdapter	_adapter )
	{
		adapter			= _adapter;
		
		my_instance	= new AZMyInstanceImpl( adapter, this );
		
		new AZPortClashHandler( this );
	}
	
	public void
	initialize()
	{		
		try{
			initialised = true;
			
			boolean	enable = System.getProperty( "az.instance.manager.enable", "1" ).equals( "1" );
			
			if ( enable ){
				
				mc_group = 
					MCGroupFactory.getSingleton(
						this,
						MC_GROUP_ADDRESS,
						MC_GROUP_PORT,
						MC_CONTROL_PORT,
						null );
			}else{
				
				mc_group = getDummyMCGroup();
			}
			
			adapter.addListener(
				new AZInstanceManagerAdapter.StateListener()
				{
					public void 
					started() 
					{						
					}
					
					public void
					stopped()
					{
						closing	= true;
						
						sendByeBye();
					}
				});
			
			SimpleTimer.addPeriodicEvent(
				"InstManager:timeouts",
				ALIVE_PERIOD,
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent	event )
					{
						checkTimeouts();
													
						sendAlive();				
					}
				});
		
		}catch( Throwable e ){
			
			if ( mc_group == null ){
				
				mc_group = getDummyMCGroup();
			}
			
			initial_search_sem.releaseForever();
			
			Debug.printStackTrace(e);
		}
		
		new AEThread2( "AZInstanceManager:initialSearch", true )
		{
			public void
			run()
			{
				try{
					search();
					
						// pick up our own details as soon as we can
					
					addAddresses( my_instance );
					
				}finally{
					
					initial_search_sem.releaseForever();
				}
			}
		}.start();
	}
	
	private MCGroup
	getDummyMCGroup()
	{
		return( 
			new MCGroup()
			{
				public int
				getControlPort()
				{
					return( MC_CONTROL_PORT );
				}
				
				public void
				sendToGroup(
					byte[]	data )
				{
				}
								
				public void
				sendToGroup(
					String	param_data )
				{	
				}
				
				public void
				sendToMember(
					InetSocketAddress	address,
					byte[]				data )
				
					throws MCGroupException
				{
				}
			});
	}
	
	public long 
	getClockSkew() 
	{
		try{	    	
	    	DHTPlugin dht = adapter.getDHTPlugin();
		    
	    	if ( dht != null ){
	    		
		    	return( dht.getClockSkew());
		    }
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
		
	    return( 0 );
	}
	
	public void
	trace(
		String	str )
	{
		if ( Logger.isEnabled()){
				
			Logger.log(new LogEvent( LOGID, str )); 
		}
	}
	
	public void
	log(
		Throwable e )
	{
		Debug.printStackTrace(e);
	}
	
	public boolean
	isInitialized()
	{
		return( initial_search_sem.isReleasedForever());
	}
	
	public void
	updateNow()
	{
		sendAlive();
	}
	
	protected boolean
	isClosing()
	{
		return( closing );
	}
	
	protected void
	sendAlive()
	{
		sendMessage( MT_ALIVE );
	}
	
	protected void
	sendAlive(
		InetSocketAddress	target )
	{
		sendMessage( MT_ALIVE, target );
	}
	
	protected void
	sendByeBye()
	{
		sendMessage( MT_BYE );
	}
	
	protected void
	sendByeBye(
		InetSocketAddress	target )
	{
		sendMessage( MT_BYE, target );
	}
	
	protected void
	sendMessage(
		int		type )
	{
		sendMessage( type, (Map)null );
	}
	
	protected void
	sendMessage(
		int					type,
		InetSocketAddress	target )
	{
		sendMessage( type, null, target );
	}
	
	protected void
	sendMessage(
		int		type,
		Map		body )
	{
		sendMessage( type, body, null );
	}
	
	protected void
	sendMessage(
		int					type,
		Map					body,
		InetSocketAddress	member )
	{
		Map	map = new HashMap();
		
		map.put( "ver", new Long(MT_VERSION));
		map.put( "type", new Long(type));
		
		Map	originator = new HashMap();
		
		map.put( "orig", originator );
		
		my_instance.encode( originator );
		
		if ( body != null ){
			
			map.put( "body", body );
		}
				
		try{
			
			if ( member == null ){
				
				byte[]	data = BEncoder.encode( map );

				mc_group.sendToGroup( data );
				
				if ( explicit_peers.size() > 0 ){
					
					map.put( "explicit", new Long(1));
				
					byte[]	explicit_data = BEncoder.encode( map );
					
					Iterator	it = explicit_peers.iterator();
				
					while( it.hasNext()){
					
						mc_group.sendToMember((InetSocketAddress)it.next(), explicit_data );
					}
				}
			}else{
				
				if ( explicit_peers.contains( member )){
					
					map.put( "explicit", new Long(1));
				}
				
				byte[]	explicit_data = BEncoder.encode( map );

				mc_group.sendToMember( member, explicit_data );
			}
		}catch( Throwable e ){
			
		}
	}

	public void
	received(
		NetworkInterface	network_interface,
		InetAddress			local_address,
		InetSocketAddress	originator,
		byte[]				data,
		int					length )
	{
		try{
			Map	map = BDecoder.decode( data, 0, length );
						
			long	version = ((Long)map.get( "ver" )).longValue();
			long	type	= ((Long)map.get( "type" )).longValue();
			
			InetAddress	originator_address = originator.getAddress();
			
			if ( map.get( "explicit" ) != null ){
				
				addInstanceSupport( originator_address, false );
			}
			
			AZOtherInstanceImpl	instance = AZOtherInstanceImpl.decode( originator_address, (Map)map.get( "orig" ));
			
			if ( instance != null ){
				
				if ( type == MT_ALIVE ){
					
					checkAdd( instance );
					
				}else if ( type == MT_BYE ){
					
					checkRemove( instance );
					
				}else{
					
					checkAdd( instance );
					
					Map	body = (Map)map.get( "body" );
					
					if ( type == MT_REQUEST ){
						
						String	originator_id	= instance.getID();
						
						if ( !originator_id.equals( my_instance.getID())){
							
							Map	reply = requestReceived( instance, body );
						
							if ( reply != null ){
							
								reply.put( "oid", originator_id.getBytes());
								reply.put( "rid", body.get( "rid" ));
								
								sendMessage( MT_REPLY, reply, originator );
							}
						}
					}else if ( 	type == MT_REPLY ){
						
						String	originator_id	= new String((byte[])body.get( "oid" ));
						
						if ( originator_id.equals( my_instance.getID())){
							
							long req_id = ((Long)body.get("rid")).longValue();
							
							try{
								this_mon.enter();
								
								for (int i=0;i<requests.size();i++){
									
									request	req = (request)requests.get(i);
									
									if ( req.getID() == req_id ){
										
										req.addReply( instance, body );
									}
								}
							}finally{
								
								this_mon.exit();
							}
						}
					}
				}
			}
		}catch( Throwable e ){
			
			Debug.out( "Invalid packet received from " + originator, e );
		}
	}
	

	protected Map
	requestReceived(
		AZInstance		instance,
		Map				body )
	{
		// System.out.println( "received result: " + ST + "/" + AL );
		

		long	type = ((Long)body.get( "type")).longValue();
		
		if ( type == MT_REQUEST_SEARCH ){
			
			return( new HashMap());
			
		}else if ( type == MT_REQUEST_TRACK ){
							
			byte[]	hash = (byte[])body.get( "hash" );
			
			boolean	seed = ((Long)body.get( "seed" )).intValue() == 1;
			
			AZInstanceTracked.TrackTarget target = adapter.track( hash );
				
			if ( target != null ){
				
				try{					
					informTracked( new trackedInstance( instance, target, seed ));
						
				}catch( Throwable e ){
						
					Debug.printStackTrace(e);
				}
				
				Map	reply = new HashMap();
				
				reply.put( "seed", new Long( target.isSeed()?1:0));		
				
				return( reply );
				
			}else{
				
				return( null );
			}
		}else{
			
			return( null );
		}
	}
	
	public void
	interfaceChanged(
		NetworkInterface	network_interface )
	{
		sendAlive();
	}
	
	protected AZOtherInstanceImpl
	checkAdd(
		AZOtherInstanceImpl	inst )
	{
		if ( inst.getID().equals( my_instance.getID())){
			
			return( inst );
		}
		
		boolean	added 	= false;
		boolean	changed	= false;
		
		try{
			this_mon.enter();
			
			AZOtherInstanceImpl	existing = (AZOtherInstanceImpl)other_instances.get( inst.getID());
			
			if ( existing == null ){
				
				added	= true;
			
				other_instances.put( inst.getID(), inst );
								
			}else{
								
				changed = existing.update( inst );

				inst	= existing;
			}
		}finally{
			
			this_mon.exit();
		}
		
		if ( added ){
			
			informAdded( inst );
			
		}else if ( changed ){
			
			informChanged( inst );
		}
		
		return( inst );
	}
	
	protected void
	checkRemove(
		AZOtherInstanceImpl	inst )
	{
		if ( inst.getID().equals( my_instance.getID())){
			
			return;
		}
		
		boolean	removed = false;
		
		try{
			this_mon.enter();
			
			removed = other_instances.remove( inst.getID()) != null;
			
		}finally{
			
			this_mon.exit();
		}
		
		if ( removed ){
			
			informRemoved( inst );
		}
	}
	
	public AZInstance
	getMyInstance()
	{
		return( my_instance );
	}
	
	protected void
	search()
	{
		sendRequest( MT_REQUEST_SEARCH );
	}
	
	public int
  	getOtherInstanceCount()
  	{
		waitForInit();
		
  		try{
  			this_mon.enter();

  			return( other_instances.size());
  			
  		}finally{
  			
  			this_mon.exit();
  		}
  	}
	
	public AZInstance[]
	getOtherInstances()
	{
		waitForInit();
		
		try{
			this_mon.enter();

			return((AZInstance[])other_instances.values().toArray( new AZInstance[other_instances.size()]));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	private void
	waitForInit()
	{
		if ( init_wait_abandoned ){
			
			return;
		}
		
  		if ( !initial_search_sem.reserve(2500)){
  			Debug.out( "Instance manager - timeout waiting for initial search" );
  			
  			init_wait_abandoned = true;
  		}
	}
	
	protected void
	addAddresses(
		AZInstance	inst )
	{
		InetAddress	internal_address 	= inst.getInternalAddress();
		InetAddress	external_address	= inst.getExternalAddress();
		int			tcp					= inst.getTCPListenPort();
		int			udp					= inst.getUDPListenPort();
		int			udp2				= inst.getUDPNonDataListenPort();
		
		modifyAddresses( internal_address, external_address, tcp, udp, udp2, true );
	}
	
	protected void
	removeAddresses(
		AZOtherInstanceImpl	inst )
	{
		List		internal_addresses 	= inst.getInternalAddresses();
		InetAddress	external_address	= inst.getExternalAddress();
		int			tcp					= inst.getTCPListenPort();
		int			udp					= inst.getUDPListenPort();
		int			udp2				= inst.getUDPNonDataListenPort();

		for (int i=0;i<internal_addresses.size();i++){
			
			modifyAddresses( (InetAddress)internal_addresses.get(i), external_address, tcp, udp, udp2, false );
		}
	}
	
	protected void
	modifyAddresses(
		InetAddress		internal_address,
		InetAddress		external_address,
		int				tcp,
		int				udp,
		int				udp2,
		boolean			add )	
	{
		if ( internal_address.isAnyLocalAddress()){
			
			try{
				internal_address = NetUtils.getLocalHost();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
		
		try{
			this_mon.enter();
 
			InetSocketAddress	int_tcp = new InetSocketAddress(internal_address, tcp);
			InetSocketAddress	ext_tcp = new InetSocketAddress(external_address, tcp);
			InetSocketAddress	int_udp = new InetSocketAddress(internal_address, udp);
			InetSocketAddress	ext_udp = new InetSocketAddress(external_address, udp);
			InetSocketAddress	int_udp2 = new InetSocketAddress(internal_address, udp2);
			InetSocketAddress	ext_udp2 = new InetSocketAddress(external_address, udp2);

				// not the most efficient code in the world this... will need rev
			
			tcp_ext_to_lan 	= modifyAddress( tcp_ext_to_lan, ext_tcp, int_tcp, add );
			tcp_lan_to_ext 	= modifyAddress( tcp_lan_to_ext, int_tcp, ext_tcp, add );
			udp_ext_to_lan 	= modifyAddress( udp_ext_to_lan, ext_udp, int_udp, add );
			udp_lan_to_ext 	= modifyAddress( udp_lan_to_ext, int_udp, ext_udp, add );
			udp2_ext_to_lan = modifyAddress( udp2_ext_to_lan, ext_udp2, int_udp2, add );
			udp2_lan_to_ext = modifyAddress( udp2_lan_to_ext, int_udp2, ext_udp2, add );

			if ( !lan_addresses.contains( internal_address )){
				
				Set	new_lan_addresses = new HashSet( lan_addresses );
				
				new_lan_addresses.add( internal_address );
				
				lan_addresses	= new_lan_addresses;
			}
			
			if ( !ext_addresses.contains( external_address )){
				
				Set	new_ext_addresses = new HashSet( ext_addresses );
				
				new_ext_addresses.add( external_address );
				
				ext_addresses	= new_ext_addresses;
			}
		}finally{
			
			this_mon.exit();
		}
	}
		
	protected Map
	modifyAddress(
		Map					map,
		InetSocketAddress	key,
		InetSocketAddress	value,
		boolean				add )
	{
		// System.out.println( "ModAddress: " + key + " -> " + value + " - " + (add?"add":"remove"));
		
		InetSocketAddress	old_value = (InetSocketAddress)map.get(key);

		boolean	same = old_value != null && old_value.equals( value );
		
		Map	new_map = map;
		
		if ( add ){
			
			if ( !same ){
				
				new_map	= new HashMap( map );
	
				new_map.put( key, value );
			}
		}else{
			
			if ( same ){
				
				new_map	= new HashMap( map );
				
				new_map.remove( key );
			}
		}	
		
		return( new_map );
	}
	
	public InetSocketAddress
	getLANAddress(
		InetSocketAddress	external_address,
		int					address_type )
	{
		Map	map;
		
		if ( address_type == AT_TCP ){
			map = tcp_ext_to_lan;
		}else if ( address_type == AT_UDP ){
			map = udp_ext_to_lan;
		}else{
			map = udp2_ext_to_lan;
		}
		
		if ( map.size() == 0 ){
			
			return( null );
		}
		
		return((InetSocketAddress)map.get( external_address ));
	}
	
	public InetSocketAddress
	getExternalAddress(
		InetSocketAddress	lan_address,
		int					address_type )
	{
		Map	map;
		
		if ( address_type == AT_TCP ){
			map = tcp_lan_to_ext;
		}else if ( address_type == AT_UDP ){
			map = udp_lan_to_ext;
		}else{
			map = udp2_lan_to_ext;
		}
		
		if ( map.size() == 0 ){
			
			return( null );
		}
		
		return((InetSocketAddress)map.get( lan_address ));	
	}
	
	public boolean
	isLANAddress(
		InetAddress			address )
	{
		if ( DISABLE_LAN_LOCAL_STUFF ){
			
			return( false );
		}
		
		if ( address == null ){
			
			return( false );
		}
		
		String	sp = socks_proxy;
		
		if ( sp != null ){
			
			if ( sp.equals( address.getHostAddress())){
				
				return( false );
			}
		}
		
		if ( include_well_known_lans ){
		
			if ( 	address.isLoopbackAddress() || 
					address.isLinkLocalAddress() ||
					address.isSiteLocalAddress()){
					
				return( true );
			}
		}
		
		String	host_address = address.getHostAddress();
		
		for (int i=0;i<lan_subnets.size();i++){
			
			Pattern	p = (Pattern)lan_subnets.get(i);
			
			if ( p.matcher( host_address ).matches()){
								
				return( true );
			}
		}
		
		if ( lan_addresses.contains( address )){
			
			return( true );
		}
		
		if ( explicit_peers.size() > 0 ){
			
			Iterator	it = explicit_peers.iterator();
			
			while( it.hasNext()){

				if (((InetSocketAddress)it.next()).getAddress().equals( address )){
					
					return( true );
				}
			}
		}
		
		return( false );
	}
	
	public boolean
	addLANSubnet(
		String	subnet )
	
		throws PatternSyntaxException
	{
		String	str = "";
		
		for (int i=0;i<subnet.length();i++){
			
			char	c = subnet.charAt(i);
			
			if ( c == '*' ){
				
				str += ".*?";
				
			}else if ( c == '.' ){
				
				str += "\\.";
			
			}else{
				
				str += c;
			}
		}
		
		Pattern pattern = Pattern.compile( str );
		
		for (int i=0;i<lan_subnets.size();i++){
			
			if ( pattern.pattern().equals(((Pattern)lan_subnets.get(i)).pattern())){
				
				return( false );
			}
		}
		
		try{
			this_mon.enter();
			
			List	new_nets = new ArrayList( lan_subnets );
			
			new_nets.add( pattern );
			
			lan_subnets	= new_nets;
			
		}finally{
			
			this_mon.exit();
		}
		
		return( true );
	}
	
	public void
	setIncludeWellKnownLANs(
		boolean	include )
	{
		include_well_known_lans	= include;
	}
	
	public boolean
	getIncludeWellKnownLANs()
	{
		return( include_well_known_lans );
	}
	
	public boolean
	addInstance(
		InetAddress			explicit_address )
	{
		return( addInstanceSupport( explicit_address, true ));
	}
	
	protected boolean
	addInstanceSupport(
		InetAddress			explicit_address,
		boolean				force_send_alive )
	{
		final InetSocketAddress	sad = new InetSocketAddress( explicit_address, MC_GROUP_PORT );
		
		boolean	new_peer = false;
		
		if ( !explicit_peers.contains( sad )){
			
			try{
				this_mon.enter();
	
				List	new_peers = new ArrayList( explicit_peers );
				
				new_peers.add( sad );
				
				explicit_peers	= new_peers;
				
			}finally{
				
				this_mon.exit();
			}
							
			new_peer = true;

		}
		
		if ( force_send_alive || new_peer ){
			
				// if not yet initialised then we'll send out our details in a mo anyway.
				// plus we need to wait for init to occur to ensure dht plugin initialised
				// before trying to get external address
			
			if ( initialised ){
				
					// take this off the current thread as there are potential deadlock issues
					// regarding this during initialisation as sending the event attempts to
					// get the external address, this may hit DHT and the current thread
					// maybe initialising the DHT...
				
				new DelayedEvent(
						"AZInstanceManagerImpl:delaySendAlive", 
						0,
						new AERunnable()
						{
							public void 
							runSupport()
							{
								sendAlive( sad );
							}
						});
			}
		}
		
		return( new_peer );
	}
	
	public boolean
	isExternalAddress(
		InetAddress			address )
	{
		return( ext_addresses.contains( address ));
	}
	
	public AZInstanceTracked[]
	track(
		byte[]							hash,
		AZInstanceTracked.TrackTarget	target )
	{
		if ( mc_group == null || getOtherInstances().length == 0 ){
			
			return( new AZInstanceTracked[0]);
		}
		
		Map	body = new HashMap();
		
		body.put( "hash", hash );
		
		body.put( "seed", new Long( target.isSeed()?1:0 ));
		
		Map	replies = sendRequest( MT_REQUEST_TRACK, body ); 
				
		AZInstanceTracked[]	res = new AZInstanceTracked[replies.size()];
		
		Iterator	it = replies.entrySet().iterator();
		
		int	pos = 0;
		
		while( it.hasNext()){
			
			Map.Entry	entry = (Map.Entry)it.next();
			
			AZInstance	inst 	= (AZInstance)entry.getKey();
			Map			reply	= (Map)entry.getValue();
	
			boolean	seed = ((Long)reply.get( "seed" )).intValue() == 1;
	
			res[ pos++ ] = new trackedInstance( inst, target, seed );
		}
		
		return( res );
	}
	
	protected void
	checkTimeouts()
	{
		long	now = SystemTime.getCurrentTime();
	
		List	removed = new ArrayList();
		
		try{
			this_mon.enter();

			Iterator	it = other_instances.values().iterator();
			
			while( it.hasNext()){
				
				AZOtherInstanceImpl	inst = (AZOtherInstanceImpl)it.next();
	
				if ( now - inst.getAliveTime() > ALIVE_PERIOD * 2.5 ){
					
					removed.add( inst );
					
					it.remove();
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		for (int i=0;i<removed.size();i++){
			
			AZOtherInstanceImpl	inst = (AZOtherInstanceImpl)removed.get(i);
			
			informRemoved( inst );
		}
	}
	
	protected void
	informRemoved(
		AZOtherInstanceImpl	inst )
	{
		removeAddresses( inst );
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceLost( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	informAdded(
		AZInstance	inst )
	{
		addAddresses( inst );

		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceFound( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	informChanged(
		AZInstance	inst )
	{
		addAddresses( inst );
		
		if ( inst == my_instance ){
			
			sendAlive();
		}
		
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceChanged( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	informTracked(
		AZInstanceTracked	inst )
	{
		for (int i=0;i<listeners.size();i++){
			
			try{
				((AZInstanceManagerListener)listeners.get(i)).instanceTracked( inst );
				
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected Map
	sendRequest(
		int		type )
	{
		return( new request( type, new HashMap()).getReplies());
	}
	
	protected Map
	sendRequest(
		int		type,
		Map		body )
	{
		return( new request( type, body ).getReplies());
	}
	
	protected class
	request
	{
		private long	id;
		
		private Set	reply_instances	= new HashSet();
		
		private Map	replies			= new HashMap();
		
		protected
		request(
			int			type,
			Map			body  )
		{
			try{
				this_mon.enter();

				id	= search_id_next++;
						
				requests.add( this );
	
			}finally{
				
				this_mon.exit();
			}
			
			body.put( "type", new Long( type ));
			
			body.put( "rid", new Long( id ));
			
			sendMessage( MT_REQUEST, body );
		}
		
		protected long
		getID()
		{
			return( id );
		}
		
		protected void
		addReply(
			AZInstance	instance,
			Map			body )
		{
			try{
				this_mon.enter();
				
				if ( !reply_instances.contains( instance.getID())){
						
					reply_instances.add( instance.getID());
					
					replies.put( instance, body );
				}
						
			}finally{
				
				this_mon.exit();
			}
		}
		
		protected Map
		getReplies()
		{
			try{
				Thread.sleep( 2500 );
				
			}catch( Throwable e ){
				
			}
			
			try{
				this_mon.enter();

				requests.remove( this );
				
				return( replies );	
				
			}finally{
				
				this_mon.exit();
			}
		}
	}

	public void
	addListener(
		AZInstanceManagerListener	l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		AZInstanceManagerListener	l )
	{
		listeners.remove( l );
	}
	
	protected static class
	trackedInstance
		implements AZInstanceTracked
	{
		private AZInstance		instance;
		private TrackTarget		target;
		private boolean			seed;
		
		protected
		trackedInstance(
			AZInstance		_instance,
			TrackTarget		_target,
			boolean			_seed )
		{
			instance		= _instance;
			target			= _target;
			seed			= _seed;
		}
		public AZInstance
		getInstance()
		{
			return( instance );
		}
		
		public TrackTarget
		getTarget()
		{
			return( target );
		}
		
		public boolean
		isSeed()
		{
			return( seed );
		}
	}
}
