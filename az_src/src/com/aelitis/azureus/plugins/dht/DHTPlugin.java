/*
 * Created on 24-Jan-2005
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

package com.aelitis.azureus.plugins.dht;



import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;

import org.gudy.azureus2.core3.util.Debug;

import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.logging.LoggerChannelListener;

import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.components.UITextField;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;


import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.dht.DHTLogger;

import com.aelitis.azureus.core.dht.control.DHTControlActivity;
import com.aelitis.azureus.core.dht.nat.DHTNATPuncher;
import com.aelitis.azureus.core.dht.transport.DHTTransportContact;
import com.aelitis.azureus.core.dht.transport.DHTTransportFullStats;
import com.aelitis.azureus.core.dht.transport.udp.DHTTransportUDP;
import com.aelitis.azureus.core.dht.transport.udp.impl.DHTTransportUDPImpl;

import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.impl.udp.UDPNetworkManager;
import com.aelitis.azureus.core.versioncheck.VersionCheckClient;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginImpl;
import com.aelitis.azureus.plugins.dht.impl.DHTPluginImplAdapter;

import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

/**
 * @author parg
 *
 */

public class 
DHTPlugin
	implements Plugin
{
		// data will be the DHT instance created
	
	public static final int			EVENT_DHT_AVAILABLE		= PluginEvent.PEV_FIRST_USER_EVENT;
	
	public static final int			STATUS_DISABLED			= 1;
	public static final int			STATUS_INITALISING		= 2;
	public static final int			STATUS_RUNNING			= 3;
	public static final int			STATUS_FAILED			= 4;
	
	public static final byte		FLAG_SINGLE_VALUE	= DHT.FLAG_SINGLE_VALUE;
	public static final byte		FLAG_DOWNLOADING	= DHT.FLAG_DOWNLOADING;
	public static final byte		FLAG_SEEDING		= DHT.FLAG_SEEDING;
	public static final byte		FLAG_MULTI_VALUE	= DHT.FLAG_MULTI_VALUE;
	public static final byte		FLAG_STATS			= DHT.FLAG_STATS;
	
	public static final byte		DT_NONE				= DHT.DT_NONE;
	public static final byte		DT_FREQUENCY		= DHT.DT_FREQUENCY;
	public static final byte		DT_SIZE				= DHT.DT_SIZE;
	
	public static final int			MAX_VALUE_SIZE		= DHT.MAX_VALUE_SIZE;

	private static final String	PLUGIN_VERSION	= "1.0";
	private static final String	PLUGIN_NAME		= "Distributed DB";
	private static final String	PLUGIN_CONFIGSECTION_ID	= "plugins.dht";
	
	private static final boolean	TRACE_NON_MAIN 		= false;
	private static final boolean	MAIN_DHT_ENABLE		= true;
	private static final boolean	CVS_DHT_ENABLE		= true;
	private static final boolean	MAIN_DHT_V6_ENABLE	= true;
	
	
	static{
		
		if ( TRACE_NON_MAIN ){
			
			System.out.println( "**** DHTPlugin - tracing non-main network actions ****" );
		}
	}
		
	private PluginInterface		plugin_interface;
	
	private int					status		= STATUS_INITALISING;
	private DHTPluginImpl[]		dhts;
	private DHTPluginImpl		main_dht;
	private DHTPluginImpl		cvs_dht;
	private DHTPluginImpl		main_v6_dht;
	
	private ActionParameter		reseed;
		
	private boolean				enabled;
	private int					dht_data_port;
	
	private boolean				got_extended_use;
	private boolean				extended_use;
	
	private AESemaphore			init_sem = new AESemaphore("DHTPlugin:init" );
	
	private AEMonitor			port_change_mon	= new AEMonitor( "DHTPlugin:portChanger" );
	private boolean				port_changing;
	private int					port_change_outstanding;
	
	private BooleanParameter	ipfilter_logging;
	private BooleanParameter	warn_user;
	
	private UPnPMapping			upnp_mapping;
	
	private LoggerChannel		log;
	private DHTLogger			dht_log;
	
	private List				listeners	= new ArrayList();
	

		
	public void
	initialize(
		PluginInterface 	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	PLUGIN_VERSION );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		PLUGIN_NAME );

		dht_data_port = UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();

		log = plugin_interface.getLogger().getTimeStampedChannel(PLUGIN_NAME);
		
		UIManager	ui_manager = plugin_interface.getUIManager();

		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( PLUGIN_NAME);

		model.setConfigSectionID(PLUGIN_CONFIGSECTION_ID);
		
		BasicPluginConfigModel	config = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, PLUGIN_CONFIGSECTION_ID);
			
		config.addLabelParameter2( "dht.info" );
		
		final BooleanParameter	enabled_param = config.addBooleanParameter2( "dht.enabled", "dht.enabled", true );
		
		plugin_interface.getPluginconfig().addListener(
				new PluginConfigListener()
				{
					public void
					configSaved()
					{
						int	new_dht_data_port = UDPNetworkManager.getSingleton().getUDPNonDataListeningPortNumber();
						
						if ( new_dht_data_port != dht_data_port ){
							
							changePort( new_dht_data_port );
						}
					}
				});
		
		LabelParameter	reseed_label = config.addLabelParameter2( "dht.reseed.label" );
		
		final StringParameter	reseed_ip	= config.addStringParameter2( "dht.reseed.ip", "dht.reseed.ip", "" );
		final IntParameter		reseed_port	= config.addIntParameter2( "dht.reseed.port", "dht.reseed.port", 0 );
		
		reseed = config.addActionParameter2( "dht.reseed.info", "dht.reseed");

		reseed.setEnabled( false );
		
		config.createGroup( "dht.reseed.group",
				new Parameter[]{ reseed_label, reseed_ip, reseed_port, reseed });
		
		ipfilter_logging = config.addBooleanParameter2( "dht.ipfilter.log", "dht.ipfilter.log", true );

		warn_user = config.addBooleanParameter2( "dht.warn.user", "dht.warn.user", true );

		final BooleanParameter	advanced = config.addBooleanParameter2( "dht.advanced", "dht.advanced", false );

		LabelParameter	advanced_label = config.addLabelParameter2( "dht.advanced.label" );

		final StringParameter	override_ip	= config.addStringParameter2( "dht.override.ip", "dht.override.ip", "" );

		config.createGroup( "dht.advanced.group",
				new Parameter[]{ advanced_label, override_ip });

		advanced.addEnabledOnSelection( advanced_label );
		advanced.addEnabledOnSelection( override_ip );
		
		final StringParameter	command = config.addStringParameter2( "dht.execute.command", "dht.execute.command", "print" );
		
		ActionParameter	execute = config.addActionParameter2( "dht.execute.info", "dht.execute");
		
		final BooleanParameter	logging = config.addBooleanParameter2( "dht.logging", "dht.logging", false );

		config.createGroup( "dht.diagnostics.group",
				new Parameter[]{ command, execute, logging });

		logging.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					if ( dhts != null ){
						
						for (int i=0;i<dhts.length;i++){
							
							dhts[i].setLogging( logging.getValue());
						}
					}
				}
			});
		
		final DHTPluginOperationListener log_polistener =
			new DHTPluginOperationListener()
			{
				public void
				diversified()
				{
				}
				
				public void
				valueRead(
					DHTPluginContact	originator,
					DHTPluginValue		value )
				{
					log.log( "valueRead: " + new String(value.getValue()) + " from " + originator.getName());
					
					if ( ( value.getFlags() & DHTPlugin.FLAG_STATS ) != 0 ){
						
						DHTPluginKeyStats stats = decodeStats( value );
						
						log.log( "    stats: size=" + stats.getSize());
					}
				}
				
				public void
				valueWritten(
					DHTPluginContact	target,
					DHTPluginValue		value )
				{
					log.log( "valueWritten:" + new String( value.getValue()) + " to " + target.getName());
				}
				
				public void
				complete(
					byte[]	key,
					boolean	timeout_occurred )
				{
					log.log( "complete: timeout = " + timeout_occurred );
				}
			};
			
		execute.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					AEThread2 t = 
						new AEThread2( "DHT:commandrunner", true )
						{
							public void
							run()
							{
								if ( dhts == null ){
									
									return;
								}
								
								for (int i=0;i<dhts.length;i++){

									DHT	dht = dhts[i].getDHT();
									
									DHTTransportUDP	transport = (DHTTransportUDP)dht.getTransport();
									
									String	c = command.getValue().trim();
									
									String	lc = c.toLowerCase();
									
									if ( lc.equals("print")){
										
										dht.print();
										
										dhts[i].logStats();
										
									}else if ( lc.equals( "testca" )){
																
										((DHTTransportUDPImpl)transport).testExternalAddressChange();
										
									}else if ( lc.equals( "testnd" )){
										
										((DHTTransportUDPImpl)transport).testNetworkAlive( false );
										
									}else if ( lc.equals( "testna" )){
										
										((DHTTransportUDPImpl)transport).testNetworkAlive( true );
				
									}else{
										
										int pos = c.indexOf( ' ' );
										
										if ( pos != -1 ){
											
											String	lhs = lc.substring(0,pos);
											String	rhs = c.substring(pos+1);
											
											if ( lhs.equals( "set" )){
												
												pos	= rhs.indexOf( '=' );
												
												if ( pos != -1 ){
													
													DHTPlugin.this.put( 
															rhs.substring(0,pos).getBytes(),
															"DHT Plugin: set",
															rhs.substring(pos+1).getBytes(),
															(byte)0,
															log_polistener );
												}
											}else if ( lhs.equals( "get" )){
												
												DHTPlugin.this.get(
													rhs.getBytes(), "DHT Plugin: get", (byte)0, 1, 10000, true, false, log_polistener );
	
											}else if ( lhs.equals( "query" )){
												
												DHTPlugin.this.get(
													rhs.getBytes(), "DHT Plugin: get", DHTPlugin.FLAG_STATS, 1, 10000, true, false, log_polistener );

											}else if ( lhs.equals( "punch" )){

												Map	originator_data = new HashMap();
												
												originator_data.put( "hello", "mum" );

												DHTNATPuncher puncher = dht.getNATPuncher();
												
												if ( puncher != null ){
												
													puncher.punch( "Test", transport.getLocalContact(), null, originator_data );
												}
											}else if ( lhs.equals( "stats" )){
												
												try{
													pos = rhs.indexOf( ":" );
													
													DHTTransportContact	contact;
													
													if ( pos == -1 ){
													
														contact = transport.getLocalContact();
														
													}else{
														
														String	host = rhs.substring(0,pos);
														int		port = Integer.parseInt( rhs.substring(pos+1));
														
														contact = 
																transport.importContact(
																		new InetSocketAddress( host, port ),
																		transport.getProtocolVersion());
													}
													
													DHTTransportFullStats stats = contact.getStats();
														
													log.log( "Stats:" + (stats==null?"<null>":stats.getString()));
														
													DHTControlActivity[] activities = dht.getControl().getActivities();
														
													for (int j=0;j<activities.length;j++){
															
														log.log( "    act:" + activities[j].getString());
													}
											
												}catch( Throwable e ){
													
													Debug.printStackTrace(e);
												}
											}
										}
									}
								}
							}
						};
											
					t.start();
				}
			});
		
		reseed.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						reseed.setEnabled( false );						

						AEThread2 t = 
							new AEThread2( "DHT:reseeder", true )
							{
								public void
								run()
								{
									try{
										String	ip 	= reseed_ip.getValue().trim();

										if ( dhts == null ){
											
											return;
										}
									
										int		port = reseed_port.getValue();
									
										for (int i=0;i<dhts.length;i++){
											
											DHTPluginImpl	dht = dhts[i];
										
											if ( ip.length() == 0 || port == 0 ){
												
												dht.checkForReSeed( true );
												
											}else{
												
												if ( dht.importSeed( ip, port ) != null ){
													
													dht.integrateDHT( false, null );
												}
											}
										}
										
									}finally{
										
										reseed.setEnabled( true );
									}
								}
							};
													
						t.start();
					}
				});
		
		model.getActivity().setVisible( false );
		model.getProgress().setVisible( false );
		
		log.addListener(
				new LoggerChannelListener()
				{
					public void
					messageLogged(
						int		type,
						String	message )
					{
						model.getLogArea().appendText( message+"\n");
					}
					
					public void
					messageLogged(
						String		str,
						Throwable	error )
					{
						model.getLogArea().appendText( error.toString()+"\n");
					}
				});

		dht_log = 
			new DHTLogger()
			{
				public void
				log(
					String	str )
				{
					log.log( str );
				}
			
				public void
				log(
					Throwable e )
				{
					log.log( e );
				}
				
				public void
				log(
					int		log_type,
					String	str )
				{
					if ( isEnabled( log_type )){
						
						log.log( str );
					}
				}
			
				public boolean
				isEnabled(
					int	log_type )
				{
					if ( log_type == DHTLogger.LT_IP_FILTER ){
						
						return( ipfilter_logging.getValue());
					}
					
					return( true );
				}
					
				public PluginInterface
				getPluginInterface()
				{
					return( log.getLogger().getPluginInterface());
				}
			};
		
		
		if (!enabled_param.getValue()){
			
			model.getStatus().setText( "Disabled" );

			status	= STATUS_DISABLED;
			
			init_sem.releaseForever();
			
			return;
		}
		
		PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );
		
		if ( pi_upnp == null ){

			log.log( "UPnP plugin not found, can't map port" );
			
		}else{
			
			upnp_mapping = ((UPnPPlugin)pi_upnp.getPlugin()).addMapping( 
							plugin_interface.getPluginName(), 
							false, 
							dht_data_port, 
							true );
		}

		setPluginInfo();
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					String	ip = null;
					
					if ( advanced.getValue()){
						
						ip = override_ip.getValue().trim();
						
						if ( ip.length() == 0 ){
							
							ip = null;
						}
					}
					
					initComplete( model.getStatus(), logging.getValue(), ip );
				}
				
				public void
				closedownInitiated()
				{
					if ( dhts != null ){
						
						for (int i=0;i<dhts.length;i++){
							
							dhts[i].closedownInitiated();
						}
					}
				}
				
				public void
				closedownComplete()
				{
				}
			});
		
		final int sample_frequency		= 60*1000;
		final int sample_stats_ticks	= 15;	// every 15 mins

		plugin_interface.getUtilities().createTimer("DHTStats", true ).addPeriodicEvent(
				sample_frequency,
				new UTTimerEventPerformer()
				{
					public void
					perform(
						UTTimerEvent		event )
					{
						if ( dhts != null ){
							
							for (int i=0;i<dhts.length;i++){
								
								dhts[i].updateStats( sample_stats_ticks );
							}
						}
						
						setPluginInfo();
					}
				});

	}
	
	protected void
	changePort(
		int	_new_port )
	{
			// don't check for new_port being dht_data_port here as we want to continue to pick up
			// changes that occurred during dht init
		
		try{
			port_change_mon.enter();
		
			port_change_outstanding	= _new_port;
			
			if ( port_changing ){
								
				return;
			}
			
			port_changing			= true;
			
		}finally{
			
			port_change_mon.exit();
		}
		
		new AEThread2("DHTPlugin:portChanger", true )
		{
			public void
			run()
			{
				while( true ){
					
					int	new_port;
				
					try{
						port_change_mon.enter();

						new_port	= port_change_outstanding;
						
					}finally{
						
						port_change_mon.exit();
					}	
						
					try{
						dht_data_port	= new_port;
						
						if ( upnp_mapping != null ){
							
							if ( upnp_mapping.getPort() != new_port ){
								
								upnp_mapping.setPort( new_port );
							}
						}
						
						if ( status == STATUS_RUNNING ){
							
							if ( dhts != null ){
								
								for (int i=0;i<dhts.length;i++){
									
									if ( dhts[i].getPort() != new_port ){
										
										dhts[i].setPort( new_port );
									}
								}
							}
						}
					}finally{
						
						try{
							port_change_mon.enter();

							if ( new_port == port_change_outstanding ){
								
								port_changing	= false;
								
								break;
							}
							
						}finally{
							
							port_change_mon.exit();
						}						
					}
				}
			}
		}.start();
	}
	
	protected void
	initComplete(
		final UITextField		status_area,
		final boolean			logging,
		final String			override_ip )
	{
		AEThread2 t = 
			new AEThread2( "DHTPlugin.init", true )
			{
				public void
				run()
				{
					try{							
						
						enabled = VersionCheckClient.getSingleton().DHTEnableAllowed();
						
						if ( enabled ){
							
							status_area.setText( "Initialising" );
							
							List	plugins = new ArrayList();
							
								// adapter only added to first DHTPluginImpl we create
							
							DHTPluginImplAdapter adapter = 
					        		new DHTPluginImplAdapter()
					        		{
					        			public void
					        			localContactChanged(
					        				DHTPluginContact	local_contact )
					        			{
					        				for (int i=0;i<listeners.size();i++){
					        					
					        					((DHTPluginListener)listeners.get(i)).localAddressChanged( local_contact );
					        				}
					        			}
					        		};
					        		
							if ( MAIN_DHT_ENABLE ){
								
								main_dht = 
									new DHTPluginImpl(
												plugin_interface,
												AzureusCoreFactory.getSingleton().getNATTraverser(),
												adapter,
												DHTTransportUDP.PROTOCOL_VERSION_MAIN,
												DHT.NW_MAIN,
												false,
												override_ip,
												dht_data_port,
												reseed,
												warn_user,
												logging,
												log, dht_log );
																
								plugins.add( main_dht );
								
								adapter = null;
							}
							
							if ( MAIN_DHT_V6_ENABLE ){
								
								if ( NetworkAdmin.getSingleton().hasIPV6Potential()){
									
									main_v6_dht = 
										new DHTPluginImpl(
											plugin_interface,
											AzureusCoreFactory.getSingleton().getNATTraverser(),
											adapter,
											DHTTransportUDP.PROTOCOL_VERSION_MAIN,
											DHT.NW_MAIN_V6,
											true,
											null,
											dht_data_port,
											reseed,
											warn_user,
											logging,
											log, dht_log );
																
									plugins.add( main_v6_dht );
									
									adapter = null;
								}
							}
							
							if ( Constants.isCVSVersion() && CVS_DHT_ENABLE ){
								
								cvs_dht = 
									new DHTPluginImpl(
										plugin_interface,
										AzureusCoreFactory.getSingleton().getNATTraverser(),
										adapter,
										DHTTransportUDP.PROTOCOL_VERSION_CVS,
										DHT.NW_CVS,
										false,
										override_ip,
										dht_data_port,
										reseed,
										warn_user,
										logging,
										log, dht_log );
							
								plugins.add( cvs_dht );
								
								adapter = null;
							}
							
							DHTPluginImpl[]	_dhts = new DHTPluginImpl[plugins.size()];
							
							plugins.toArray( _dhts );
												
							dhts = _dhts;
							
							status = dhts[0].getStatus();
							
							status_area.setText( dhts[0].getStatusText());			
							
						}else{
							
							status	= STATUS_DISABLED;

							status_area.setText( "Disabled administratively due to network problems" );
						}
					}catch( Throwable e ){
						
						enabled	= false;
						
						status	= STATUS_DISABLED;

						status_area.setText( "Disabled due to error during initialisation" );

						log.log( e );
						
						Debug.printStackTrace(e);
						
					}finally{
						
						init_sem.releaseForever();
					}
					
						// pick up any port changes that occurred during init
					
					if ( status == STATUS_RUNNING ){
					
						changePort( dht_data_port );
					}
				}
			};
					
		t.start();
	}
	
	protected void
	setPluginInfo()
	{
		boolean	reachable	= plugin_interface.getPluginconfig().getPluginBooleanParameter( "dht.reachable." + DHT.NW_MAIN, true );

		plugin_interface.getPluginconfig().setPluginParameter( 
				"plugin.info", 
				reachable?"1":"0" );
	}

	public boolean
	isEnabled()
	{
		if ( plugin_interface.isInitialisationThread()){
			
			if ( !init_sem.isReleasedForever()){

				Debug.out( "Initialisation deadlock detected" );
				
				return( true );
			}
		}
		
		init_sem.reserve();
		
		return( enabled );
	}
	
	public boolean
	peekEnabled()
	{
		if ( init_sem.isReleasedForever()){
			
			return( enabled );
		}
		
		return( true );	// don't know yet
	}
	
	public boolean
	isExtendedUseAllowed()
	{
		if ( !isEnabled()){
			
			return( false );
		}
		
		if ( !got_extended_use){
		
			got_extended_use	= true;
			
			extended_use = VersionCheckClient.getSingleton().DHTExtendedUseAllowed();
		}
		
		return( extended_use );
	}
	
	public boolean
	isReachable()
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		return( dhts[0].isReachable());
	}
	
	public boolean
	isDiversified(
		byte[]		key )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		return( dhts[0].isDiversified( key ));
	}
	
	public void
	put(
		final byte[]						key,
		final String						description,
		final byte[]						value,
		final byte							flags,
		final DHTPluginOperationListener	listener)
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dhts[0].put( key, description, value, flags, listener );
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread2( "multi-dht: put", true )
			{
				public void
				run()
				{
					dhts[f_i].put( key, description, value, flags, 
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put valueRead" );
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put valueWritten - " + description + " " + target.getAddress() + " <- " + new String(value.getValue()));
									}
								}
								
								public void
								complete(
									byte[]	key,
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":put complete - " + description + " -> timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
	}
	
	public void
	get(
		final byte[]								key,
		final String								description,
		final byte									flags,
		final int									max_values,
		final long									timeout,
		final boolean								exhaustive,
		final boolean								high_priority,
		final DHTPluginOperationListener			listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
			
		if ( main_dht != null && main_v6_dht == null ){
			
			main_dht.get( key, description, flags, max_values, timeout, exhaustive, high_priority, listener );
			
		}else if ( main_dht == null && main_v6_dht != null ){
			
			main_v6_dht.get( key, description, flags, max_values, timeout, exhaustive, high_priority, listener );

		}else{
			
				// both DHTs active. Initially (at least :) V6 is going to be very sparse. We therefore
				// don't want to be blocking the "get" operation waiting for V6 to timeout when V4 is
				// returning hits 
			
			final	byte[]	v4_key	= key;
			final	byte[]	v6_key	= (byte[])key.clone();
			
			DHTPluginOperationListener	dual_listener =
				new DHTPluginOperationListener()
				{
					private long start_time = SystemTime.getCurrentTime();

					private int	complete_count 	= 0;
					private int	result_count	= 0;
					
					public void
					diversified()
					{
					}
					
					public void
					valueRead(
						DHTPluginContact	originator,
						DHTPluginValue		value )
					{
						if ( TRACE_NON_MAIN ){
							System.out.println( "DHT_dual:get valueRead - " + description + " " + originator.getAddress() + " -> " + new String(value.getValue()));
						}
												
						synchronized( this ){

							result_count++;
	
								// only report if not yet complete
							
							if ( complete_count < 2 ){
						
								listener.valueRead( originator, value );
							}
						}
					}
					
					public void
					valueWritten(
						DHTPluginContact	target,
						DHTPluginValue		value )
					{
						Debug.out( "eh?" );
					}
					
					public void
					complete(
						final byte[]		timeout_key,
						final boolean		timeout_occurred )
					{
							// we are guaranteed to come through here at least twice
						
						if ( TRACE_NON_MAIN ){
							System.out.println( "DHT_dual:get complete - " + description + " -> timeout=" + timeout_occurred );
						}
						
						synchronized( this ){
							
							complete_count++;
							
							if ( complete_count == 2 ){
								
								if ( TRACE_NON_MAIN ){
									System.out.println( "    completion informed" );
								}

									// if we have reported any results then we can't report 
									// timeout!
								
								listener.complete( key, result_count>0?false:timeout_occurred );
								
								return;
								
							}else if ( complete_count > 2 ){
								
								return;
							}
						
								// One of the two gets, see how much longer we're happy to hang around for
								// Only of interest if timeout then uninterested as the other will be 
								// about to timeout
								
							if ( timeout_occurred ){
								
								return;
							}
							
								// ignore a v6 completion ahead of a v4
							
							if ( timeout_key == v6_key ){
								
								return;
							}

							long	now = SystemTime.getCurrentTime();
							
							long	elapsed = now - start_time;
							
							long	rem = timeout - elapsed;
							
							if ( rem <= 0 ){
								
								complete( timeout_key, true );
								
							}else{
								
								SimpleTimer.addEvent(
									"DHTPlugin:dual_dht_early_timeout",
									now + rem,
									new TimerEventPerformer()
									{
										public void 
										perform(
											TimerEvent event) 
										{
											complete( timeout_key, true );
										}
									});
							}
						}
					}
				};
			
				// hack - use different keys so we can distinguish which completion event we
				// have received above
				
			main_dht.get( v4_key, description, flags, max_values, timeout, exhaustive, high_priority, dual_listener );
			
			main_v6_dht.get( v6_key, description, flags, max_values, timeout, exhaustive, high_priority, dual_listener );
		}
		
			// we don't really care about cvs as this is just for load testing not results

		if ( cvs_dht != null ){
			
			new AEThread2( "multi-dht: get", true )
			{
				public void
				run()
				{
					cvs_dht.get( 
							key, description, flags, max_values, timeout, exhaustive, high_priority,
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_CVS:get valueRead - " + description + " " + originator.getAddress() + " -> " + new String(value.getValue()));
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_CVS:get valueWritten" );
									}
								}
								
								public void
								complete(
									byte[]	key,
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_CVS:get complete - " + description + " -> timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
	}
	
	public boolean
	hasLocalKey(
		byte[]		hash )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		return( dhts[0].getLocalValue( hash ) != null );
	}
	
	public void
	remove(
		final byte[]						key,
		final String						description,
		final DHTPluginOperationListener	listener )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
				
		dhts[0].remove( key, description, listener );
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread2( "multi-dht: remove", true )
			{
				public void
				run()
				{
					dhts[f_i].remove( 
							key, description, 
							new DHTPluginOperationListener()
							{
								public void
								diversified()
								{
								}
								
								public void
								valueRead(
									DHTPluginContact	originator,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove valueRead" );
									}
								}
								
								public void
								valueWritten(
									DHTPluginContact	target,
									DHTPluginValue		value )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove valueWritten" );
									}
								}
								
								public void
								complete(
									byte[]	key,
									boolean	timeout_occurred )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":remove complete, timeout=" + timeout_occurred );
									}
								}
							});
				}
			}.start();
		}
	}
	
	public DHTPluginContact
	importContact(
		InetSocketAddress				address )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}

			// first DHT will do here
		
		return( dhts[0].importContact( address ));
	}
	
	public DHTPluginContact
	getLocalAddress()
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}

			// first DHT will do here
		
		return( dhts[0].getLocalAddress());
	}
	
		// direct read/write support
	
	public void
	registerHandler(
		byte[]							handler_key,
		final DHTPluginTransferHandler	handler )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		for (int i=0;i<dhts.length;i++){
			
			dhts[i].registerHandler( handler_key, handler );
		}
	}
	
	public byte[]
	read(
		final DHTPluginProgressListener	listener,
		final DHTPluginContact			target,
		final byte[]					handler_key,
		final byte[]					key,
		final long						timeout )
	{
		if ( !isEnabled()){
			
			throw( new RuntimeException( "DHT isn't enabled" ));
		}
		
		for (int i=1;i<dhts.length;i++){

			final int f_i	= i;
			
			new AEThread2( "mutli-dht: readXfer", true )
			{
				public void
				run()
				{
					dhts[f_i].read( 
							new DHTPluginProgressListener()
							{
								public void
								reportSize(
									long	size )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: size = " + size );
									}
								}
								
								public void
								reportActivity(
									String	str )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: act = " + str );
									}
								}
								
								public void
								reportCompleteness(
									int		percent )
								{
									if ( TRACE_NON_MAIN ){
										System.out.println( "DHT_" + f_i + ":readXfer: % = " + percent );
									}
								}
							},
							target, handler_key, key, timeout );
				}
			}.start();
		}
		
		return( dhts[0].read( listener, target, handler_key, key, timeout ));
	}

	public int
	getStatus()
	{
		return( status );
	}
	
	public DHT[]
	getDHTs()
	{
		if ( dhts == null ){
			
			return( new DHT[0] );
		}
		
		DHT[]	res = new DHT[ dhts.length ];
		
		for (int i=0;i<res.length;i++){
			
			res[i] = dhts[i].getDHT();
		}
		
		return( res );
	}
	
	public DHTPluginKeyStats
	decodeStats(
		DHTPluginValue		value )
	{
		return( dhts[0].decodeStats( value ));
	}
	
	public void
	addListener(
		DHTPluginListener	l )
	{
		listeners.add(l);
	}
	
	public void
	removeListener(
		DHTPluginListener	l )
	{
		listeners.remove(l);
	}
	
	public void
	log(
		String	str )
	{
		log.log( str );
	}
}
