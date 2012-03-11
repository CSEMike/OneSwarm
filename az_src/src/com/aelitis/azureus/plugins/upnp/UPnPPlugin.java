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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.plugins.upnp;

/**
 * @author parg
 *
 */

import java.util.*;
import java.net.URL;

import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.network.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.model.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.UTTimer;
import org.gudy.azureus2.plugins.utils.UTTimerEvent;
import org.gudy.azureus2.plugins.utils.UTTimerEventPerformer;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderFactory;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.net.natpmp.NATPMPDeviceAdapter;
import com.aelitis.net.natpmp.NatPMPDeviceFactory;
import com.aelitis.net.natpmp.upnp.NatPMPUPnP;
import com.aelitis.net.natpmp.upnp.NatPMPUPnPFactory;
import com.aelitis.net.upnp.*;
import com.aelitis.net.upnp.services.*;

public class 
UPnPPlugin
	implements Plugin, UPnPListener, UPnPMappingListener, UPnPWANConnectionListener
{
	private static final String UPNP_PLUGIN_CONFIGSECTION_ID 	= "UPnP";
	private static final String NATPMP_PLUGIN_CONFIGSECTION_ID 	= "NATPMP";

	private static final String		STATS_DISCOVER 	= "discover";
	private static final String		STATS_FOUND		= "found";
	private static final String		STATS_READ_OK 	= "read_ok";
	private static final String		STATS_READ_BAD 	= "read_bad";
	private static final String		STATS_MAP_OK 	= "map_ok";
	private static final String		STATS_MAP_BAD 	= "map_bad";
	
	private static final String[]	STATS_KEYS = { STATS_DISCOVER, STATS_FOUND, STATS_READ_OK, STATS_READ_BAD, STATS_MAP_OK, STATS_MAP_BAD };
	
	private PluginInterface		plugin_interface;
	private LoggerChannel 		log;
	
	private UPnPMappingManager	mapping_manager	= UPnPMappingManager.getSingleton( this );
	
	private UPnP				upnp;
	private UPnPLogListener		upnp_log_listener;
	
	private NatPMPUPnP	nat_pmp_upnp;
	
	private BooleanParameter	natpmp_enable_param;
	private StringParameter		nat_pmp_router;
	
	private BooleanParameter 	upnp_enable_param;
	private BooleanParameter 	trace_to_log;
	
	private BooleanParameter	alert_success_param;
	private BooleanParameter	grab_ports_param;
	private BooleanParameter	alert_other_port_param;
	private BooleanParameter	alert_device_probs_param;
	private BooleanParameter	release_mappings_param;
	private StringParameter		selected_interfaces_param;
	private StringParameter		selected_addresses_param;
	
	private BooleanParameter	ignore_bad_devices;
	private LabelParameter		ignored_devices_list;
	
	private List<UPnPMapping>		mappings	= new ArrayList<UPnPMapping>();
	private List<UPnPPluginService>	services	= new ArrayList<UPnPPluginService>();
	
	private Map<URL,String> 	root_info_map		= new HashMap<URL, String>();
	private Map<String,String> log_no_repeat_map	= new HashMap<String,String>();
	
	protected AEMonitor	this_mon 	= new AEMonitor( "UPnPPlugin" );
	   
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		"Universal Plug and Play (UPnP)" );
	}
	
	public void
	initialize(
		PluginInterface	_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
		
		log = plugin_interface.getLogger().getTimeStampedChannel("UPnP");
		log.setDiagnostic();
		log.setForce(true);

		UIManager	ui_manager = plugin_interface.getUIManager();
		
		final BasicPluginViewModel model = 
			ui_manager.createBasicPluginViewModel( 
					"UPnP");
		model.setConfigSectionID(UPNP_PLUGIN_CONFIGSECTION_ID);
		
		BasicPluginConfigModel	upnp_config = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, UPNP_PLUGIN_CONFIGSECTION_ID );
		
			// NATPMP
		
		BasicPluginConfigModel	natpmp_config = ui_manager.createBasicPluginConfigModel( UPNP_PLUGIN_CONFIGSECTION_ID, NATPMP_PLUGIN_CONFIGSECTION_ID );

		natpmp_config.addLabelParameter2( "natpmp.info" );
		
		ActionParameter	natpmp_wiki = natpmp_config.addActionParameter2( "Utils.link.visit", "MainWindow.about.internet.wiki" );
		
		natpmp_wiki.setStyle( ActionParameter.STYLE_LINK );
		
		natpmp_wiki.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					try{
						plugin_interface.getUIManager().openURL( new URL( "http://wiki.vuze.com/w/NATPMP" ));
						
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
				}
			});
		
		natpmp_enable_param = 
			natpmp_config.addBooleanParameter2( "natpmp.enable", "natpmp.enable", false );
		
		nat_pmp_router = 	natpmp_config.addStringParameter2( "natpmp.routeraddress", "natpmp.routeraddress", "" );
		
		natpmp_enable_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					setNATPMPEnableState();
				}
			});
		
		natpmp_enable_param.addEnabledOnSelection( nat_pmp_router );
		
			// UPNP
		
		upnp_config.addLabelParameter2( "upnp.info" );
		upnp_config.addHyperlinkParameter2("upnp.wiki_link", "http://wiki.vuze.com/w/UPnP");
		
		
		upnp_enable_param = 
			upnp_config.addBooleanParameter2( "upnp.enable", "upnp.enable", true );
		
		grab_ports_param = upnp_config.addBooleanParameter2( "upnp.grabports", "upnp.grabports", false );
		
		release_mappings_param	 = upnp_config.addBooleanParameter2( "upnp.releasemappings", "upnp.releasemappings", true );

		ActionParameter refresh_param = upnp_config.addActionParameter2( "upnp.refresh.label", "upnp.refresh.button" );
		
		refresh_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					UPnPPlugin.this.refreshMappings();
				}
			});

		// Auto-refresh mappings every minute when enabled.
		final BooleanParameter auto_refresh_on_bad_nat_param = upnp_config.addBooleanParameter2( "upnp.refresh_on_bad_nat", "upnp.refresh_mappings_on_bad_nat", false);
		plugin_interface.getUtilities().createTimer("upnp mapping auto-refresh", true).addPeriodicEvent(1*60*1000, new UTTimerEventPerformer() {
			private long last_bad_nat = 0;
			public void perform(UTTimerEvent event) {
				if (upnp == null) {return;}
				if (!auto_refresh_on_bad_nat_param.getValue()) {return;}
				if (!upnp_enable_param.getValue()) {return;}
				int status = plugin_interface.getConnectionManager().getNATStatus();
				if (status == ConnectionManager.NAT_BAD) {
					// Only try to refresh the mappings if this is the first bad NAT
					// message we've been given in the last 15 minutes - we don't want
					// to endlessly retry performing the mappings
					long now = plugin_interface.getUtilities().getCurrentSystemTime();
					if (last_bad_nat + (15*60*1000) < now ) {
						last_bad_nat = now;
						log.log(LoggerChannel.LT_WARNING, "NAT status is firewalled - trying to refresh UPnP mappings");
						refreshMappings(true);
					}
				}
			}
		});
		
		upnp_config.addLabelParameter2( "blank.resource" );
		
		alert_success_param = upnp_config.addBooleanParameter2( "upnp.alertsuccess", "upnp.alertsuccess", false );
		
		alert_other_port_param = upnp_config.addBooleanParameter2( "upnp.alertothermappings", "upnp.alertothermappings", true );
		
		alert_device_probs_param = upnp_config.addBooleanParameter2( "upnp.alertdeviceproblems", "upnp.alertdeviceproblems", true );
		
		selected_interfaces_param 	= upnp_config.addStringParameter2( "upnp.selectedinterfaces", "upnp.selectedinterfaces", "" );
		selected_addresses_param 	= upnp_config.addStringParameter2( "upnp.selectedaddresses", "upnp.selectedaddresses", "" );

		ignore_bad_devices = upnp_config.addBooleanParameter2( "upnp.ignorebaddevices", "upnp.ignorebaddevices", true );
		
		ignored_devices_list = upnp_config.addLabelParameter2( "upnp.ignorebaddevices.info" );

		ActionParameter reset_param = upnp_config.addActionParameter2( "upnp.ignorebaddevices.reset", "upnp.ignorebaddevices.reset.action" );
		
		reset_param.addListener(
			new ParameterListener()
			{
				public void
				parameterChanged(
					Parameter	param )
				{
					PluginConfig pc = plugin_interface.getPluginconfig();
					
					for (int i=0;i<STATS_KEYS.length;i++){
						
						String	key = "upnp.device.stats." + STATS_KEYS[i];
						
						pc.setPluginMapParameter( key, new HashMap());
					}
					
					pc.setPluginMapParameter( "upnp.device.ignorelist", new HashMap());
					
					updateIgnoreList();
				}
			});
		
		trace_to_log = upnp_config.addBooleanParameter2("upnp.trace_to_log", "upnp.trace_to_log", false);

		final boolean	enabled = upnp_enable_param.getValue();
		
		upnp_enable_param.addEnabledOnSelection( alert_success_param );
		upnp_enable_param.addEnabledOnSelection( grab_ports_param );
		upnp_enable_param.addEnabledOnSelection( refresh_param );
		upnp_enable_param.addEnabledOnSelection( alert_other_port_param );
		upnp_enable_param.addEnabledOnSelection( alert_device_probs_param );
		upnp_enable_param.addEnabledOnSelection( release_mappings_param );
		upnp_enable_param.addEnabledOnSelection( selected_interfaces_param );
		upnp_enable_param.addEnabledOnSelection( selected_addresses_param );
		upnp_enable_param.addEnabledOnSelection( ignore_bad_devices );
		upnp_enable_param.addEnabledOnSelection( ignored_devices_list );
		upnp_enable_param.addEnabledOnSelection( reset_param );
		upnp_enable_param.addEnabledOnSelection( trace_to_log );
		
		natpmp_enable_param.setEnabled( enabled );
		
		model.getStatus().setText( enabled?"Running":"Disabled" );
		
		upnp_enable_param.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	p )
					{
						boolean	e = upnp_enable_param.getValue();
						
						natpmp_enable_param.setEnabled( e );
						
						model.getStatus().setText( e?"Running":"Disabled" );
						
						if ( e ){
							
							startUp();
							
						}else{
							
							closeDown( true );
						}
						
						setNATPMPEnableState();
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

		// startup() used to be called on initializationComplete()
		// Moved to delayed task because rootDeviceFound can take
		// a lot of CPU cycle.  Let's hope nothing breaks
		DelayedTask dt = plugin_interface.getUtilities().createDelayedTask(new Runnable() {
			public void 
			run() 
			{
				if ( enabled ){
					
					updateIgnoreList();
					
					startUp();			
				}
			}
		});
		dt.queue();

		plugin_interface.addListener(
				new PluginListener()
				{
					public void
					initializationComplete()
					{	
					}
					
					public void
					closedownInitiated()
					{
						if ( services.size() == 0 ){
							
							plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", "" );
						}
					}
					
					public void
					closedownComplete()
					{
						closeDown( true );
					}
				});
	}
	
	protected void
	updateIgnoreList()
	{
		try{
			String	param = "";
			
			if ( ignore_bad_devices.getValue()){

				PluginConfig pc = plugin_interface.getPluginconfig();
	
				Map	ignored = pc.getPluginMapParameter( "upnp.device.ignorelist", new HashMap());
				
				Iterator	it = ignored.entrySet().iterator();
				
				while( it.hasNext()){
					
					Map.Entry	entry = (Map.Entry)it.next();
								
					Map	value = (Map)entry.getValue();
					
					param += "\n    " + entry.getKey() + ": " + new String((byte[])value.get( "Location" ));
				}
				
				if ( ignored.size() > 0 ){
					
					log.log( "Devices currently being ignored: " + param );
				}
			}
			
			String	text = 
				plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
					"upnp.ignorebaddevices.info",
					new String[]{ param });
			
			ignored_devices_list.setLabelText( text );
			
		}catch( Throwable e ){
			
			Debug.printStackTrace(e);
		}
	}
	
	protected void
	ignoreDevice(
		String		USN,
		URL			location )
	{
			// only take note of this if enabled to do so
		
		if ( ignore_bad_devices.getValue()){
			
			try{
				PluginConfig pc = plugin_interface.getPluginconfig();
	
				Map	ignored = pc.getPluginMapParameter( "upnp.device.ignorelist", new HashMap());
		
				Map	entry = (Map)ignored.get( USN );
				
				if ( entry == null ){
					
					entry	= new HashMap();
					
					entry.put( "Location", location.toString().getBytes());
					
					ignored.put( USN, entry );
					
					pc.setPluginMapParameter( "upnp.device.ignorelist", ignored );
					
					updateIgnoreList();
					
					String	text = 
						plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
							"upnp.ignorebaddevices.alert",
							new String[]{ location.toString() });
	
					log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
	
				}
			}catch( Throwable e ){
				
				Debug.printStackTrace(e);
			}
		}
	}
	
	protected void
	startUp()
	{
		if ( upnp != null ){
			
				// already started up, must have been re-enabled
			
			refreshMappings();
			
			return;
		}
		
		final LoggerChannel	core_log		= plugin_interface.getLogger().getChannel("UPnP Core");

		try{
			upnp = UPnPFactory.getSingleton(
					new UPnPAdapter()
					{
						Set	exception_traces = new HashSet();
						
						public SimpleXMLParserDocument
						parseXML(
							String	data )
						
							throws SimpleXMLParserDocumentException
						{
							return( plugin_interface.getUtilities().getSimpleXMLParserDocumentFactory().create( data ));
						}
						
						public ResourceDownloaderFactory
						getResourceDownloaderFactory()
						{
							return( plugin_interface.getUtilities().getResourceDownloaderFactory());
						}
						
						public UTTimer
						createTimer(
							String	name )
						{
							return( plugin_interface.getUtilities().createTimer( name, true ));
						}
						
						public void
						createThread(
							String		name,
							Runnable	runnable )
						{
							plugin_interface.getUtilities().createThread( name, runnable );
						}
						
						public Comparator
						getAlphanumericComparator()
						{
							return( plugin_interface.getUtilities().getFormatters().getAlphanumericComparator( true ));
						}

						public void
						trace(
							String	str )
						{
							core_log.log( str );
							if (trace_to_log.getValue()) {
								upnp_log_listener.log(str);
							}
						}
						
						public void
						log(
							Throwable	e )
						{
							String	nested = Debug.getNestedExceptionMessage(e);
							
							if ( !exception_traces.contains( nested )){
								
								exception_traces.add( nested );
								
								if ( exception_traces.size() > 128 ){
									
									exception_traces.clear();
								}
								
								core_log.log( e );
																
							}else{
								
								core_log.log( nested );
							}
						}
						
						public void
						log(
							String	str )
						{
							log.log( str );
						}
						
						public String
						getTraceDir()
						{
							return( plugin_interface.getUtilities().getAzureusUserDir());
						}
					},
					getSelectedInterfaces());
				
			upnp.addRootDeviceListener( this );
			
			upnp_log_listener =
				new UPnPLogListener()
				{
					public void
					log(
						String	str )
					{
						log.log( str );
					}
					
					public void
					logAlert(
						String	str,
						boolean	error,
						int		type )
					{
						boolean	logged = false;
						
						if ( alert_device_probs_param.getValue()){
							
							if ( type == UPnPLogListener.TYPE_ALWAYS ){
								
								log.logAlertRepeatable(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );
								
								logged	= true;
								
							}else{
								
								boolean	do_it	= false;
								
								if ( type == UPnPLogListener.TYPE_ONCE_EVER ){
									
									byte[] fp = 
										plugin_interface.getUtilities().getSecurityManager().calculateSHA1(
											str.getBytes());
									
									String	key = "upnp.alert.fp." + plugin_interface.getUtilities().getFormatters().encodeBytesToString( fp );
									
									PluginConfig pc = plugin_interface.getPluginconfig();
									
									if ( !pc.getPluginBooleanParameter( key, false )){
										
										pc.setPluginParameter( key, true );
										
										do_it	= true;
									}
								}else{
									
									do_it	= true;
								}
							
								if ( do_it ){						
									
									log.logAlert(						
										error?LoggerChannel.LT_ERROR:LoggerChannel.LT_WARNING,
										str );	
									
									logged	= true;
								}
							}		
						}
						
						if ( !logged ){
							
							log.log( str );
						}
					}
				};
			
			upnp.addLogListener( upnp_log_listener );

			mapping_manager.addListener(
				new UPnPMappingManagerListener()
				{
					public void
					mappingAdded(
						UPnPMapping		mapping )
					{
						addMapping( mapping );
					}
				});
			
			UPnPMapping[]	upnp_mappings = mapping_manager.getMappings();
			
			for (int i=0;i<upnp_mappings.length;i++){
				
				addMapping( upnp_mappings[i] );
			}
			
			setNATPMPEnableState();

		}catch( Throwable e ){
			
			log.log( e );
		}
	}
	
	protected void
	closeDown(
		final boolean	end_of_day )
	{
			// problems here at end of day regarding devices that hang and cause AZ to hang around
			// got ages before terminating
		
		final AESemaphore sem = new AESemaphore( "UPnPPlugin:closeTimeout" );
		
			
		new AEThread( "UPnPPlugin:closeTimeout" , true )
		{
			public void
			runSupport()
			{
				try{
					for (int i=0;i<mappings.size();i++){
						
						UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
						
						if ( !mapping.isEnabled()){
							
							continue;
						}
						
						for (int j=0;j<services.size();j++){
							
							UPnPPluginService	service = (UPnPPluginService)services.get(j);
							
							service.removeMapping( log, mapping, end_of_day );
						}
					}
				}finally{
					
					sem.release();
				}
			}
		}.start();
	
	
		if ( !sem.reserve( end_of_day?15*1000:0 )){
		
			String	msg = "A UPnP device is taking a long time to release its port mappings, consider disabling this via the UPnP configuration.";

			if ( upnp_log_listener != null ){
				
				upnp_log_listener.logAlert( msg, false, UPnPLogListener.TYPE_ONCE_PER_SESSION );
				
			}else{
				
				log.logAlertRepeatable( LoggerChannel.LT_WARNING, msg );
			}
		}	
	}

	public boolean
	deviceDiscovered(
		String		USN,
		URL			location )
	{
		String[]	addresses = getSelectedAddresses();
		
		if ( addresses.length > 0 ){
			
			String	address = location.getHost();
			
			boolean found	= false;
			
			boolean	all_exclude = true;
			
			for (int i=0;i<addresses.length;i++){
				
				String	this_address = addresses[i];
				
				boolean	include = true;

				if ( this_address.startsWith( "+" )){
					
					this_address = this_address.substring(1);
					
					all_exclude = false;
					
				}else if ( this_address.startsWith( "-" )){
					
					this_address = this_address.substring(1);
					
					include = false;
					
				}else{
					
					all_exclude = false;
				}
				
				if ( this_address.equals( address )){
				
					if ( !include ){
						
						logNoRepeat( USN, "Device '" + location + "' is being ignored as excluded in address list" );
						
						return( false );
					}
					
					found = true;
					
					break;
				}
			}
			
			if ( !found ){
				
				if ( all_exclude ){
					
					// if all exclude then we let others through
				}else{
					
					logNoRepeat( USN, "Device '" + location + "' is being ignored as not in address list" );
					
					return( false );
				}
			}
		}
		
		if ( !ignore_bad_devices.getValue()){
			
			return( true );
		}
		
		incrementDeviceStats( USN, STATS_DISCOVER );

		boolean	ok = checkDeviceStats( USN, location );
		
		String	stats = "";
		
		for (int i=0;i<STATS_KEYS.length;i++){

			stats += (i==0?"":",")+STATS_KEYS[i] + "=" + getDeviceStats( USN, STATS_KEYS[i] );
		}

		if ( !ok ){
			
			logNoRepeat( USN, "Device '" + location + "' is being ignored: " + stats );
			
		}else{
			
			
			logNoRepeat( USN, "Device '" + location +"' is ok: " + stats );
		}
		
		return( ok );
	}
	
	protected void
	logNoRepeat(
		String	usn,
		String	msg )
	{
		synchronized( log_no_repeat_map ){
			
			String	last = (String)log_no_repeat_map.get( usn );
			
			if ( last != null && last.equals( msg )){
				
				return;
			}
			
			log_no_repeat_map.put( usn, msg );
		}
		
		log.log( msg );
	}

	public void
	rootDeviceFound(
		UPnPRootDevice	device )
	{
		incrementDeviceStats( device.getUSN(), "found" );

		checkDeviceStats( device );
		
		try{
			int	interesting = processDevice( device.getDevice() );
			
			if ( interesting > 0 ){
					
				try{
					this_mon.enter();
				
					root_info_map.put( device.getLocation(), device.getInfo());
				
					Iterator<String>	it = root_info_map.values().iterator();
					
					String	all_info = "";
						
					List	reported_info = new ArrayList();
					
					while( it.hasNext()){
						
						String	info = (String)it.next();
						
						if ( info != null && !reported_info.contains( info )){
							
							reported_info.add( info );
							
							all_info += (all_info.length()==0?"":",") + info;
						}
					}
					
					if ( all_info.length() > 0 ){
						
						plugin_interface.getPluginconfig().setPluginParameter( "plugin.info", all_info );
					}
					
				}finally{
					
					this_mon.exit();
				}
			}			
		}catch( Throwable e ){
			
			log.log( "Root device processing fails", e );
		}
	}
	
	protected boolean
	checkDeviceStats(
		UPnPRootDevice	root )
	{
		return( checkDeviceStats( root.getUSN(), root.getLocation()));
	}
	
	protected boolean
	checkDeviceStats(
		String	USN,
		URL		location )
	{
		long	discovers 	= getDeviceStats( USN, STATS_DISCOVER );
		long	founds		= getDeviceStats( USN, STATS_FOUND );
		
		if ( discovers > 3 && founds == 0 ){
			
				// discovered but never found - something went wrong with the device
				// construction process
			
			ignoreDevice( USN, location );
			
			return( false );
			
		}else if ( founds > 0 ){
			
				// found ok before, reset details in case now its screwed
			
			setDeviceStats( USN, STATS_DISCOVER, 0 );
			setDeviceStats( USN, STATS_FOUND, 0 );
		}
		
		long	map_ok	 	= getDeviceStats( USN, STATS_MAP_OK );
		long	map_bad		= getDeviceStats( USN, STATS_MAP_BAD );

		if ( map_bad > 5 && map_ok == 0 ){
			
			ignoreDevice( USN, location );
			
			return( false );
			
		}else if ( map_ok > 0 ){
			
			setDeviceStats( USN, STATS_MAP_OK, 0 );
			setDeviceStats( USN, STATS_MAP_BAD, 0 );
		}
		
		return( true );
	}
	
	protected long
	incrementDeviceStats(
		String		USN,
		String		stat_key )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		Long	count = (Long)counts.get( USN );
		
		if ( count == null ){
			
			count = new Long(1);
			
		}else{
			
			count = new Long( count.longValue() + 1 );
		}
		
		counts.put( USN, count );
		
		pc.getPluginMapParameter( key, counts );
				
		return( count.longValue());
	}
	
	protected long
	getDeviceStats(
		String		USN,
		String		stat_key )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		Long	count = (Long)counts.get( USN );
		
		if ( count == null ){
			
			return( 0 );
		}
		
		return( count.longValue());
	}
		
	protected void
	setDeviceStats(
		String		USN,
		String		stat_key,
		long		value )
	{
		String	key = "upnp.device.stats." + stat_key;
		
		PluginConfig pc = plugin_interface.getPluginconfig();

		Map	counts = pc.getPluginMapParameter( key, new HashMap());
		
		counts.put( USN, new Long( value ));
		
		pc.getPluginMapParameter( key, counts );
	}
	
	public void
	mappingResult(
		UPnPWANConnection	connection,
		boolean				ok )
	{
		UPnPRootDevice	root = connection.getGenericService().getDevice().getRootDevice();
		
		incrementDeviceStats( root.getUSN(), ok?STATS_MAP_OK:STATS_MAP_BAD );
		
		checkDeviceStats( root );
	}
	
	public void
	mappingsReadResult(
		UPnPWANConnection	connection,
		boolean				ok )
	{
		UPnPRootDevice	root = connection.getGenericService().getDevice().getRootDevice();

		incrementDeviceStats( root.getUSN(), ok?STATS_READ_OK:STATS_READ_BAD );
	}
	
	protected String[]
	getSelectedInterfaces()
	{
		String	si = selected_interfaces_param.getValue().trim();
		
		StringTokenizer	tok = new StringTokenizer( si, ";" );
		
		List	res = new ArrayList();
		
		while( tok.hasMoreTokens()){
			
			String	s = tok.nextToken().trim();
			
			if ( s.length() > 0 ){
				
				res.add( s );
			}
		}
		
		return( (String[])res.toArray( new String[res.size()]));
	}
	
	protected String[]
	getSelectedAddresses()
	{
		String	si = selected_addresses_param.getValue().trim();

		StringTokenizer	tok = new StringTokenizer( si, ";" );

		List	res = new ArrayList();

		while( tok.hasMoreTokens()){

			String	s = tok.nextToken().trim();

			if ( s.length() > 0 ){

				res.add( s );
			}
		}

		return( (String[])res.toArray( new String[res.size()]));
	}
	
	protected int
	processDevice(
		UPnPDevice		device )
	
		throws UPnPException
	{			
		int	interesting = processServices( device, device.getServices());
			
		UPnPDevice[]	kids = device.getSubDevices();
				
		for (int i=0;i<kids.length;i++){
			
			interesting += processDevice( kids[i] );
		}
		
		return( interesting );
	}
	
	protected int
	processServices(
		UPnPDevice		device,
		UPnPService[] 	device_services )
	
		throws UPnPException
	{
		int	interesting = 0;
		
		for (int i=0;i<device_services.length;i++){
			
			UPnPService	s = device_services[i];
			
			String	service_type = s.getServiceType();
			
			if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANIPConnection:1") || 
					service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANPPPConnection:1")){
				
				final UPnPWANConnection	wan_service = (UPnPWANConnection)s.getSpecificService();
								
				device.getRootDevice().addListener(
					new UPnPRootDeviceListener()
					{
						public void
						lost(
							UPnPRootDevice	root,
							boolean			replaced )
						{
							removeService( wan_service, replaced );
						}
					});
				
				addService( wan_service );
				
				interesting++;
				
			}else if ( 	service_type.equalsIgnoreCase( "urn:schemas-upnp-org:service:WANCommonInterfaceConfig:1")){ 
				
				/* useless stats
				try{
					UPnPWANCommonInterfaceConfig	config = (UPnPWANCommonInterfaceConfig)s.getSpecificService();
				
					long[]	speeds = config.getCommonLinkProperties();
					
					if ( speeds[0] > 0 && speeds[1] > 0 ){
						
						log.log( "Device speed: down=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[0]/8) + ", up=" + 
									plugin_interface.getUtilities().getFormatters().formatByteCountToKiBEtcPerSec(speeds[1]/8));
					}
				}catch( Throwable e ){
					
					log.log(e);
				}
				*/
			}
		}
		
		return( interesting );
	}
	
	protected void
	addService(
		UPnPWANConnection	wan_service )
	
		throws UPnPException
	{
		wan_service.addListener( this );
			
		mapping_manager.serviceFound( wan_service );

		try{
			this_mon.enter();
		
			log.log( "    Found " + ( wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection" ));
			
			UPnPWANConnectionPortMapping[] ports;
			
			String	usn = wan_service.getGenericService().getDevice().getRootDevice().getUSN();
			
			if ( getDeviceStats( usn, STATS_READ_OK ) == 0 && getDeviceStats( usn, STATS_READ_BAD ) > 2 ){
				
				ports = new UPnPWANConnectionPortMapping[0];
				
				wan_service.periodicallyRecheckMappings( false );
				
				log.log( "    Not reading port mappings from device due to previous failures" );
				
			}else{
				
				ports = wan_service.getPortMappings();
			}
			
			for (int j=0;j<ports.length;j++){
				
				log.log( "      mapping [" + j  + "] " + ports[j].getExternalPort() + "/" + 
								(ports[j].isTCP()?"TCP":"UDP" ) + " [" + ports[j].getDescription() + "] -> " + ports[j].getInternalHost());
			}
			
			services.add(new UPnPPluginService( wan_service, ports, alert_success_param, grab_ports_param, alert_other_port_param, release_mappings_param ));
			
			if ( services.size() > 1 ){
				
					// check this isn't a single device with multiple services
				
				String	new_usn = wan_service.getGenericService().getDevice().getRootDevice().getUSN();
				
				boolean	multiple_found = false;
				
				for (int i=0;i<services.size()-1;i++){
					
					UPnPPluginService	service = (UPnPPluginService)services.get(i);
					
					String existing_usn = service.getService().getGenericService().getDevice().getRootDevice().getUSN();
					
					if ( !new_usn.equals( existing_usn )){
						
						multiple_found = true;
						
						break;
					}
				}
				
				if ( multiple_found ){
					
					PluginConfig pc = plugin_interface.getPluginconfig();
					
					if ( !pc.getPluginBooleanParameter( "upnp.device.multipledevices.warned", false )){
						
						pc.setPluginParameter( "upnp.device.multipledevices.warned", true );
						
						String	text = MessageText.getString( "upnp.alert.multipledevice.warning" );
																	
						log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
					}
				}
			}
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	removeService(
		UPnPWANConnection	wan_service,
		boolean				replaced )
	{
		try{
			this_mon.enter();
			
			String	name = wan_service.getGenericService().getServiceType().indexOf("PPP") == -1? "WANIPConnection":"WANPPPConnection";
			
			String	text = 
				MessageText.getString( 
						"upnp.alert.lostdevice", 
						new String[]{ name, wan_service.getGenericService().getDevice().getRootDevice().getLocation().getHost()});
			
			log.log( text );
			
			if ( (!replaced) && alert_device_probs_param.getValue()){
				
				log.logAlertRepeatable( LoggerChannel.LT_WARNING, text );
			}
					
			for (int i=0;i<services.size();i++){
				
				UPnPPluginService	ps = (UPnPPluginService)services.get(i);
				
				if ( ps.getService() == wan_service ){
					
					services.remove(i);
					
					break;
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	addMapping(
		UPnPMapping		mapping )
	{
		try{
			this_mon.enter();
		
			mappings.add( mapping );
			
			log.log( "Mapping request: " + mapping.getString() + ", enabled = " + mapping.isEnabled());
			
			mapping.addListener( this );
			
			checkState();
			
		}finally{
			
			this_mon.exit();
		}
	}	
	
	public void
	mappingChanged(
		UPnPMapping	mapping )
	{
		checkState();
	
	}
	
	public void
	mappingDestroyed(
		UPnPMapping	mapping )
	{
		try{
			this_mon.enter();
		
			mappings.remove( mapping );
			
			log.log( "Mapping request removed: " + mapping.getString());

			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				service.removeMapping( log, mapping, false );
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	protected void
	checkState()
	{		
		try{
			this_mon.enter();
		
			for (int i=0;i<mappings.size();i++){
				
				UPnPMapping	mapping = (UPnPMapping)mappings.get(i);
	
				for (int j=0;j<services.size();j++){
					
					UPnPPluginService	service = (UPnPPluginService)services.get(j);
					
					service.checkMapping( log, mapping );
				}
			}
		}finally{
			
			this_mon.exit();
		}
	}
	
	public String[]
	getExternalIPAddresses()
	{
		List	res = new ArrayList();
		
		try{
			this_mon.enter();
			
			for (int j=0;j<services.size();j++){
				
				UPnPPluginService	service = (UPnPPluginService)services.get(j);
				
				try{
					String	address = service.getService().getExternalIPAddress();
				
					if ( address != null ){
						
						res.add( address );
					}
				}catch( Throwable e ){
					
					Debug.printStackTrace(e);
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		return((String[])res.toArray( new String[res.size()]));
	}
	
	public UPnPPluginService[]
	getServices()
	{
		try{
			this_mon.enter();
						
			return((UPnPPluginService[])services.toArray( new UPnPPluginService[services.size()] ));
			
		}finally{
			
			this_mon.exit();
		}
	}
	
	public UPnPPluginService[]
	getServices(
		UPnPDevice	device )
	{
		String	target_usn = device.getRootDevice().getUSN();
		
		List<UPnPPluginService> res = new ArrayList<UPnPPluginService>();
		
		try{
			this_mon.enter();
						
			for ( UPnPPluginService service: services ){
				
				String	this_usn = service.getService().getGenericService().getDevice().getRootDevice().getUSN();
				
				if ( this_usn.equals( target_usn )){
					
					res.add( service );
				}
			}
		}finally{
			
			this_mon.exit();
		}
		
		return( res.toArray( new UPnPPluginService[res.size()] ));
	}
	
		// for external use, e.g. webui
	
	public UPnPMapping
	addMapping(
		String		desc_resource,
		boolean		tcp,
		int			port,
		boolean		enabled )
	{
		return( mapping_manager.addMapping( desc_resource, tcp, port, enabled ));
	}
	
	public UPnPMapping
	getMapping(
		boolean	tcp,
		int		port )
	{
		return( mapping_manager.getMapping( tcp, port ));
	}
	
	public UPnPMapping[]
	getMappings()
	{
		return( mapping_manager.getMappings());
	}
	
	public boolean
	isEnabled()
	{
		return( upnp_enable_param.getValue());
	}
	
	protected void
	setNATPMPEnableState()
	{
		boolean	enabled = natpmp_enable_param.getValue() && upnp_enable_param.getValue();
		
		try{
			if ( enabled ){
				
				if ( nat_pmp_upnp == null ){
			
					nat_pmp_upnp = 
						NatPMPUPnPFactory.create( 
							upnp, 
							NatPMPDeviceFactory.getSingleton(
								new NATPMPDeviceAdapter()
								{
									public String 
									getRouterAddress() 
									{
										return( nat_pmp_router.getValue());
									}
									
									public void
									log(
										String	str )
									{
										log.log( "NAT-PMP: " + str );
									}
								}));
			
					nat_pmp_upnp.addListener( this );
				}
				
				nat_pmp_upnp.setEnabled( true );
			}else{
				
				if ( nat_pmp_upnp != null ){
					
					nat_pmp_upnp.setEnabled( false );
				}
			}
		}catch( Throwable e ){
			
			log.log( "Failed to initialise NAT-PMP subsystem", e );
		}
	}
	protected void
	logAlert(
		int			type,
		String		resource,
		String[]	params )
	{
		String	text = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText(
					resource, params );

		log.logAlertRepeatable( type, text );
	}
	
	/**
	 * Provided for use by other plugins.
	 */
	public void refreshMappings() {
		refreshMappings(false);
	}

	/**
	 * Provided for use by other plugins.
	 */
	public void refreshMappings(boolean force) {
		if (force) {
			closeDown(true);
			startUp();
		}
		else {
			this.upnp.reset();
		}
	}
}
