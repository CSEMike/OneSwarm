/*
 * Created on Oct 5, 2009
 * Created by Paul Gardner
 * 
 * Copyright 2009 Vuze, Inc.  All rights reserved.
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


package com.aelitis.azureus.core.pairing.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.core3.util.AEVerifier;
import org.gudy.azureus2.core3.util.AsyncDispatcher;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.core3.util.BEncoder;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DelayedEvent;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.core3.util.TimerEventPeriodic;
import org.gudy.azureus2.core3.util.UrlUtils;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.clientid.ClientIDException;
import org.gudy.azureus2.plugins.clientid.ClientIDGenerator;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.UIManagerEvent;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.BooleanParameter;
import org.gudy.azureus2.plugins.ui.config.ConfigSection;
import org.gudy.azureus2.plugins.ui.config.HyperlinkParameter;
import org.gudy.azureus2.plugins.ui.config.InfoParameter;
import org.gudy.azureus2.plugins.ui.config.LabelParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.plugins.utils.DelayedTask;
import org.gudy.azureus2.plugins.utils.StaticUtilities;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.pluginsimpl.local.clientid.ClientIDManagerImpl;
import org.gudy.azureus2.pluginsimpl.local.utils.resourcedownloader.ResourceDownloaderFactoryImpl;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminHTTPProxy;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterface;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminSocksProxy;
import com.aelitis.azureus.core.pairing.*;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;
import com.aelitis.azureus.plugins.upnp.UPnPPluginService;
import com.aelitis.azureus.util.JSONUtils;
import com.aelitis.net.upnp.UPnPRootDevice;

public class 
PairingManagerImpl
	implements PairingManager
{
	private static final boolean DEBUG	= false;
	
	private static final String	SERVICE_URL;
	
	static{
		String url = System.getProperty( "az.pairing.url", "" );
		
		if ( url.length() == 0 ){
			
			SERVICE_URL = Constants.PAIRING_URL;
			
		}else{
			
			SERVICE_URL = url;
		}
	}
	
	private static final PairingManagerImpl	singleton = new PairingManagerImpl();
	
	public static PairingManager
	getSingleton()
	{
		return( singleton );
	}
	
	private static final int	GLOBAL_UPDATE_PERIOD	= 60*1000;
	private static final int	CD_REFRESH_PERIOD		= 23*60*60*1000;
	private static final int	CD_REFRESH_TICKS		= CD_REFRESH_PERIOD / GLOBAL_UPDATE_PERIOD;
	
	private static final int	CONNECT_TEST_PERIOD_MILLIS	= 30*60*1000;
	
	private AzureusCore	azureus_core;
	
	private BooleanParameter 	param_enable;

	
	private InfoParameter		param_ac_info;
	private InfoParameter		param_status_info;
	private InfoParameter		param_last_error;
	private HyperlinkParameter	param_view;
	
	private BooleanParameter 	param_e_enable;
	private StringParameter		param_public_ipv4;
	private StringParameter		param_public_ipv6;
	private StringParameter		param_host;
	
	private StringParameter		param_local_ipv4;
	private StringParameter		param_local_ipv6;

	private Map<String,PairedServiceImpl>		services = new HashMap<String, PairedServiceImpl>();
	
	private AESemaphore	init_sem = new AESemaphore( "PM:init" );
	
	private TimerEventPeriodic	global_update_event;
	
	private InetAddress		current_v4;
	private InetAddress		current_v6;
	
	private String			local_v4	= "";
	private String			local_v6	= "";
	
	private boolean	update_outstanding;
	private boolean	updates_enabled;

	private static final int MIN_UPDATE_PERIOD_DEFAULT	= 10*1000;
	private static final int MAX_UPDATE_PERIOD_DEFAULT	= 60*60*1000;
		
	private int min_update_period	= MIN_UPDATE_PERIOD_DEFAULT;
	private int max_update_period	= MAX_UPDATE_PERIOD_DEFAULT;
	
	
	private AsyncDispatcher	dispatcher = new AsyncDispatcher();
	
	private boolean			must_update_once;
	private boolean			update_in_progress;
	private TimerEvent		deferred_update_event;
	private long			last_update_time		= -1;
	private int				consec_update_fails;
	
	private String			last_message;
	
	private Map<String,Object[]>	local_address_checks = new HashMap<String, Object[]>();

	private CopyOnWriteList<PairingManagerListener>	listeners = new CopyOnWriteList<PairingManagerListener>();
	
	protected
	PairingManagerImpl()
	{
		must_update_once = COConfigurationManager.getBooleanParameter( "pairing.updateoutstanding" );

		PluginInterface default_pi = PluginInitializer.getDefaultInterface();
		
		final UIManager	ui_manager = default_pi.getUIManager();
		
		BasicPluginConfigModel configModel = ui_manager.createBasicPluginConfigModel(
				ConfigSection.SECTION_CONNECTION, "Pairing");

		configModel.addHyperlinkParameter2( "ConfigView.label.please.visit.here", MessageText.getString( "ConfigView.section.connection.pairing.url" ));

		param_enable = configModel.addBooleanParameter2( "pairing.enable", "pairing.enable", false );
		
		String	access_code = readAccessCode();
		
		param_ac_info = configModel.addInfoParameter2( "pairing.accesscode", access_code);
		
		param_status_info 	= configModel.addInfoParameter2( "pairing.status.info", "" );
		
		param_last_error	= configModel.addInfoParameter2( "pairing.last.error", "" );
		
		param_view = configModel.addHyperlinkParameter2( "pairing.view.registered", SERVICE_URL + "/web/view?ac=" + access_code);

		if ( access_code.length() == 0 ){
			
			param_view.setEnabled( false );
		}
		
		final ActionParameter ap = configModel.addActionParameter2( "pairing.ac.getnew", "pairing.ac.getnew.create" );
		
		ap.addListener(
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter 	param ) 
				{
					try{
						ap.setEnabled( false );
						
						allocateAccessCode( false );
						
						SimpleTimer.addEvent(
							"PM:enabler",
							SystemTime.getOffsetTime(30*1000),
							new TimerEventPerformer()
							{
								public void 
								perform(
									TimerEvent event ) 
								{
									ap.setEnabled( true );
								}
							});
						
					}catch( Throwable e ){
						
						ap.setEnabled( true );
						
						String details = MessageText.getString(
								"pairing.alloc.fail",
								new String[]{ Debug.getNestedExceptionMessage( e )});
						
						ui_manager.showMessageBox(
								"pairing.op.fail",
								"!" + details + "!",
								UIManagerEvent.MT_OK );
					}
				}
			});
		
		LabelParameter	param_e_info = configModel.addLabelParameter2( "pairing.explicit.info" );
		
		param_e_enable = configModel.addBooleanParameter2( "pairing.explicit.enable", "pairing.explicit.enable", false );
		
		param_public_ipv4	= configModel.addStringParameter2( "pairing.ipv4", "pairing.ipv4", "" );
		param_public_ipv6	= configModel.addStringParameter2( "pairing.ipv6", "pairing.ipv6", "" );
		param_host			= configModel.addStringParameter2( "pairing.host", "pairing.host", "" );
		
		LabelParameter spacer = configModel.addLabelParameter2( "blank.resource" );
		
		param_local_ipv4	= configModel.addStringParameter2( "pairing.local.ipv4", "pairing.local.ipv4", "" );
		param_local_ipv6	= configModel.addStringParameter2( "pairing.local.ipv6", "pairing.local.ipv6", "" );

		
		param_public_ipv4.setGenerateIntermediateEvents( false );
		param_public_ipv6.setGenerateIntermediateEvents( false );
		param_host.setGenerateIntermediateEvents( false );
		
		ParameterListener change_listener = 
			new ParameterListener()
			{
				public void 
				parameterChanged(
					Parameter param )
				{
					updateNeeded();
					
					if ( param == param_enable ){
						
						fireChanged();
					}
				}
			};
			
		param_enable.addListener( change_listener );
		param_e_enable.addListener(	change_listener );
		param_public_ipv4.addListener(	change_listener );
		param_public_ipv6.addListener(	change_listener );
		param_local_ipv4.addListener(	change_listener );
		param_local_ipv6.addListener(	change_listener );
		param_host.addListener(	change_listener );
		
		param_e_enable.addEnabledOnSelection( param_public_ipv4 );
		param_e_enable.addEnabledOnSelection( param_public_ipv6 );
		param_e_enable.addEnabledOnSelection( param_local_ipv4 );
		param_e_enable.addEnabledOnSelection( param_local_ipv6 );
		param_e_enable.addEnabledOnSelection( param_host );
		
		configModel.createGroup(
			"pairing.group.explicit",
			new Parameter[]{
				param_e_info,
				param_e_enable,
				param_public_ipv4,	
				param_public_ipv6,
				param_host,
				spacer,
				param_local_ipv4,	
				param_local_ipv6,
			});
		
		AzureusCoreFactory.addCoreRunningListener(
			new AzureusCoreRunningListener()
			{
				public void 
				azureusCoreRunning(
					AzureusCore core )
				{
					initialise( core );
				}
			});
	}
	
	
	protected void
	initialise(
		AzureusCore		_core )
	{
		synchronized( this ){
			
			azureus_core	= _core;
		}
		
		try{
			PluginInterface default_pi = PluginInitializer.getDefaultInterface();

			DelayedTask dt = default_pi.getUtilities().createDelayedTask(
				new Runnable()
				{
					public void 
					run() 
					{
						new DelayedEvent( 
							"PM:delayinit",
							10*1000,
							new AERunnable()
							{
								public void
								runSupport()
								{
									enableUpdates();
								}
							});
					}
				});
			
			dt.queue();
			
		}finally{
			
			init_sem.releaseForever();
		}
	}
	
	protected void
	waitForInitialisation()
	
		throws PairingException
	{
		if ( !init_sem.reserve( 30*1000 )){
		
			throw( new PairingException( "Timeout waiting for initialisation" ));
		}
	}
	
	public boolean
	isEnabled()
	{
		return( param_enable.getValue());
	}
	
	public void
	setEnabled(
		boolean	enabled )
	{
		param_enable.setValue( enabled );
	}
	
	public void 
	setGroup(
		String group ) 
	{
		COConfigurationManager.setParameter( "pairing.groupcode", group );
		
		updateNeeded();
	}
	
	public String
	getGroup()
	{
		return( COConfigurationManager.getStringParameter( "pairing.groupcode", null ));
	}

	public List<PairedNode> 
	listGroup() 
	
		throws PairingException 
	{
		try{
			URL url = new URL( SERVICE_URL + "/remote/listGroup?gc=" + getGroup());
			
			
			InputStream is =  new ResourceDownloaderFactoryImpl().create( url ).download();
			
			Map json = JSONUtils.decodeJSON( new String( FileUtil.readInputStreamAsByteArray( is ), "UTF-8" ));
			
			List<Map>	list = (List<Map>)json.get( "result" );
			
			List<PairedNode>	result = new ArrayList<PairedNode>();
			
			String my_ac = peekAccessCode();

			if ( list != null ){
				
				for ( Map m: list ){
					
					PairedNodeImpl node = new PairedNodeImpl( m );
							
					if ( my_ac == null || !my_ac.equals( node.getAccessCode())){
					
						result.add( node );
					}
				}
			}
			
			return( result );
			
		}catch( Throwable e ){
			
			throw( new PairingException( "Failed to list group", e ));
		}
	}
	
	protected void
	setStatus(
		String		str )
	{
		String last_status = param_status_info.getValue();
		
		if ( !last_status.equals( str )){
			
			param_status_info.setValue( str );
		
			fireChanged();
		}
	}
	
	public String
	getStatus()
	{
		return( param_status_info.getValue());
	}
	
	protected void
	setLastServerError(
		String	error )
	{
		String last_error = param_last_error.getValue();
	
		if ( error == null ){
			
			error = "";
		}
		
		if ( !last_error.equals( error )){
			
			param_last_error.setValue( error );
			
			fireChanged();
		}
	}
	
	public String
	getLastServerError()
	{
		String last_error = param_last_error.getValue();

		if ( last_error.length() == 0 ){
			
			last_error = null;
		}
		
		return( last_error );
	}

	public boolean 
	hasActionOutstanding() 
	{
		synchronized( this ){
			
			if ( !isEnabled()){
				
				return( false );
			}
			
			return( !updates_enabled || update_outstanding || deferred_update_event != null || update_in_progress );
		}
	}
	
	protected String
	readAccessCode()
	{
		return( COConfigurationManager.getStringParameter( "pairing.accesscode", "" ));
	}
	
	protected void
	writeAccessCode(
		String		ac )
	{
		COConfigurationManager.setParameter( "pairing.accesscode", ac );
		
			// try not to loose this!
		
		COConfigurationManager.save();
		
		param_ac_info.setValue( ac );
		 
		param_view.setHyperlink( SERVICE_URL + "/web/view?ac=" + ac );
				
		param_view.setEnabled( ac.length() > 0 );
	}
	
	protected String
	allocateAccessCode(
		boolean		updating )
	
		throws PairingException
	{
		Map<String,Object>	request = new HashMap<String, Object>();
		
		String existing = readAccessCode();
		
		request.put( "ac", existing );
		
		Map<String,Object> response = sendRequest( "allocate", request );
		
		try{
			String code = getString( response, "ac" );
			
			writeAccessCode( code );
				
			if ( !updating ){
			
				updateNeeded();
			}
			
			fireChanged();
			
			return( code );
			
		}catch( Throwable e ){
			
			throw( new PairingException( "allocation failed", e ));
		}
	}

	public String
	peekAccessCode()
	{
		return( readAccessCode());
	}
	
	public String
	getAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		String ac = readAccessCode();
		
		if ( ac == null || ac.length() == 0 ){
			
			ac = allocateAccessCode( false );
		}
		
		return( ac );
	}
	
	public void
	getAccessCode(
		final PairingManagerListener 	listener )
	
		throws PairingException
	{
		new AEThread2( "PM:gac", true )
		{
			public void
			run()
			{
				try{
					getAccessCode();
					
				}catch( Throwable e ){
					
				}finally{
					
					listener.somethingChanged( PairingManagerImpl.this );
				}
			}
		}.start();
	}
	
	public String
	getReplacementAccessCode()
	
		throws PairingException
	{
		waitForInitialisation();
		
		String new_code = allocateAccessCode( false );
		
		return( new_code );
	}
	
	public PairedService
	addService(
		String		sid )
	{
		synchronized( this ){
						
			PairedServiceImpl	result = services.get( sid );
			
			if ( result == null ){
				
				if ( DEBUG ){
					System.out.println( "PS: added " + sid );
				}
				
				result = new PairedServiceImpl( sid );
				
				services.put( sid, result );
			}
			
			return( result );
		}
	}
	
	public PairedService
	getService(
		String		sid )
	{
		synchronized( this ){
			
			PairedService	result = services.get( sid );
			
			return( result );
		}
	}
	
	protected void
	remove(
		PairedServiceImpl	service )
	{
		synchronized( this ){

			String sid = service.getSID();
			
			if ( services.remove( sid ) != null ){
				
				if ( DEBUG ){
					System.out.println( "PS: removed " + sid );
				}
			}
		}
		
		updateNeeded();
	}
	
	protected void
	sync(
		PairedServiceImpl	service )
	{
		updateNeeded();
	}
	
	protected InetAddress
	updateAddress(
		InetAddress		current,
		InetAddress		latest,
		boolean			v6 )
	{
		if ( v6 ){
			
			if ( latest instanceof Inet4Address ){
				
				return( current );
			}
		}else{
			
			if ( latest instanceof Inet6Address ){
				
				return( current );
			}
		}
		
		if ( current == latest ){
			
			return( current );
		}
		
		if ( current == null || latest == null ){
			
			return( latest );
		}
		
		if ( !current.equals( latest )){
			
			return( latest );
		}
		
		return( current );
	}	
	
	protected void
	updateGlobals(
		boolean	is_updating )	
	{
		final long now = SystemTime.getMonotonousTime();
		
		synchronized( this ){
						
			NetworkAdmin network_admin = NetworkAdmin.getSingleton();
					
			InetAddress latest_v4 = azureus_core.getInstanceManager().getMyInstance().getExternalAddress();
			
			InetAddress temp_v4 = updateAddress( current_v4, latest_v4, false );
			
			InetAddress latest_v6 = network_admin.getDefaultPublicAddressV6();
	
			InetAddress temp_v6 = updateAddress( current_v6, latest_v6, true );
			
			final TreeSet<String>	latest_v4_locals = new TreeSet<String>();
			final TreeSet<String>	latest_v6_locals = new TreeSet<String>();
			
			NetworkAdminNetworkInterface[] interfaces = network_admin.getInterfaces();
			
			List<Runnable>	to_do = new ArrayList<Runnable>();
			
			Set<String> existing_checked = new HashSet<String>( local_address_checks.keySet());
			
			for ( NetworkAdminNetworkInterface intf: interfaces ){
				
				NetworkAdminNetworkInterfaceAddress[] addresses = intf.getAddresses();
				
				for ( NetworkAdminNetworkInterfaceAddress address: addresses ){
					
					final InetAddress ia = address.getAddress();
					
					if ( ia.isLoopbackAddress()){
						
						continue;
					}
					
					if ( ia.isLinkLocalAddress() || ia.isSiteLocalAddress()){
						
						final String a_str = ia.getHostAddress();
						
						existing_checked.remove( a_str );
						
						Object[] check;
						
						synchronized( local_address_checks ){
						
							check = local_address_checks.get( a_str );
						}
						
						boolean run_check = check == null || now - ((Long)check[0]) > CONNECT_TEST_PERIOD_MILLIS;
						
						if ( run_check ){
						
							to_do.add(
								new Runnable()
								{
									public void
									run()
									{
										Socket socket = new Socket();
										
										String	result = a_str;
										
										try{
											socket.bind( new InetSocketAddress( ia, 0 ));
																								
											socket.connect(  new InetSocketAddress( "www.google.com", 80 ), 10*1000 );
											
											result += "*";
											
										}catch( Throwable e ){
											
										}finally{
											try{
												socket.close();
											}catch( Throwable e ){
											}
											
										}
										
										synchronized( local_address_checks ){
											
											local_address_checks.put( a_str, new Object[]{ new Long(now), result });
											
											if ( ia instanceof Inet4Address ){
												
												latest_v4_locals.add( result );
												
											}else{
												
												latest_v6_locals.add( result );
											}
										}
									}
								});
							
						}else{
							
							synchronized( local_address_checks ){
						
								if ( ia instanceof Inet4Address ){
								
									latest_v4_locals.add((String)check[1]);
									
								}else{
									
									latest_v6_locals.add((String)check[1]);
								}
							}
						}
					}
				}
			}
			
			if ( to_do.size() > 0 ){
				
				final AESemaphore	sem = new AESemaphore( "PM:check" );
				
				for ( final Runnable r: to_do ){
					
					new AEThread2( "PM:check:", true )
					{
						public void
						run()
						{
							try{
								r.run();
							}finally{
								
								sem.release();
							}
						}
					}.start();
				}
				
				for (int i=0;i<to_do.size();i++){
					
					sem.reserve();
				}
			}
			
			for ( String excess: existing_checked ){
				
				local_address_checks.remove( excess );
			}
			
			String v4_locals_str = getString( latest_v4_locals );
			String v6_locals_str = getString( latest_v6_locals );
			
			
			if (	temp_v4 != current_v4 ||
					temp_v6 != current_v6 ||
					!v4_locals_str.equals( local_v4 ) ||
					!v6_locals_str.equals( local_v6 )){
				
				current_v4	= temp_v4;
				current_v6	= temp_v6;
				local_v4	= v4_locals_str;
				local_v6	= v6_locals_str;
				
				if ( !is_updating ){
				
					updateNeeded();
				}
			}

		}
	}
	
	protected String
	getString(
		Set<String>	set )
	{
		String	str = "";
		
		for ( String s: set ){
			
			str += (str.length()==0?"":",") + s;
		}
		
		return( str );
	}
	
	protected void
	enableUpdates()
	{		
		synchronized( this ){
			
			updates_enabled = true;

			if ( update_outstanding ){
				
				update_outstanding = false;
				
				updateNeeded();
			}
		}
	}
	
	protected void
	updateNeeded()
	{
		if ( DEBUG ){
			System.out.println( "PS: updateNeeded" );
		}
		
		synchronized( this ){
			
			if ( updates_enabled ){
				
				dispatcher.dispatch(
					new AERunnable()
					{
						public void
						runSupport()
						{
							doUpdate();
						}
					});
						
				
			}else{
				
				setStatus( MessageText.getString( "pairing.status.initialising" ));
				
				update_outstanding	= true;
			}
		}
	}
	
	protected void
	doUpdate()
	{
		long	now = SystemTime.getMonotonousTime();

		synchronized( this ){
			
			if ( deferred_update_event != null ){
				
				return;
			}
			
			long	time_since_last_update = now - last_update_time;
			
			if ( last_update_time > 0 &&  time_since_last_update < min_update_period ){
				
				deferUpdate(  min_update_period - time_since_last_update  );
				
				return;
			}
			
			update_in_progress = true;
		}
		
		try{
			Map<String,Object>	payload = new HashMap<String, Object>();
						
			boolean	is_enabled = param_enable.getValue();
			
			synchronized( this ){
				
				List<Map<String,String>>	list =  new ArrayList<Map<String,String>>();
				
				payload.put( "s", list );
				
				if ( services.size() > 0 && is_enabled ){
					
					if ( global_update_event == null ){
						
						global_update_event = 
							SimpleTimer.addPeriodicEvent(
							"PM:updater",
							GLOBAL_UPDATE_PERIOD,
							new TimerEventPerformer()
							{
								private int	tick_count;
								
								public void 
								perform(
									TimerEvent event ) 
								{
									tick_count++;
									
									updateGlobals( false );
									
									if ( tick_count % CD_REFRESH_TICKS == 0 ){
										
										updateNeeded();
									}
								}
							});
						
						updateGlobals( true );
					}
					
					for ( PairedServiceImpl service: services.values()){
						
						list.add( service.toMap());
					}
				}else{
					
						// when we get to zero services we want to push through the
						// last update to remove cd
					
					if ( global_update_event == null ){
						
						if ( consec_update_fails == 0 && !must_update_once ){
					
							update_in_progress = false;
							
							setStatus( MessageText.getString( is_enabled?"pairing.status.noservices":"pairing.status.disabled" ));
							
							return;
						}
					}else{
					
						global_update_event.cancel();
					
						global_update_event = null;
					}
				}
				
				last_update_time = now;
			}
			
				// we need a valid access code here!
			
			String ac = readAccessCode();
			
			if ( ac.length() == 0 ){
				
				ac = allocateAccessCode( true );			
			}
			
			payload.put( "ac", ac );
			
			String	gc = getGroup();
			
			if ( gc != null && gc.length() > 0 ){
				
				payload.put( "gc", gc );
			}
			
			synchronized( this ){

				if ( current_v4 != null ){
				
					payload.put( "c_v4", current_v4.getHostAddress());
				}
				
				if ( current_v6 != null ){
					
					payload.put( "c_v6", current_v6.getHostAddress());
				}
			
				if ( local_v4.length() > 0 ){
					
					payload.put( "l_v4", local_v4 );
				}
				
				if ( local_v6.length() > 0 ){
					
					payload.put( "l_v6", local_v6 );
				}
				
				if ( param_e_enable.getValue()){
				
					String host = param_host.getValue().trim();
					
					if ( host.length() > 0 ){
						
						payload.put( "e_h", host );
					}
					
					String v4 = param_public_ipv4.getValue().trim();
					
					if ( v4.length() > 0 ){
						
						payload.put( "e_v4", v4 );
					}
					
					String v6 = param_public_ipv6.getValue().trim();
					
					if ( v6.length() > 0 ){
						
						payload.put( "e_v6", v6 );
					}
					
					String l_v4 = param_local_ipv4.getValue().trim();
					
					if ( l_v4.length() > 0 ){
						
						payload.put( "e_l_v4", l_v4 );
					}
					
					String l_v6 = param_local_ipv6.getValue().trim();
					
					if ( l_v6.length() > 0 ){
						
						payload.put( "e_l_v6", l_v6 );
					}
				}
				
					// grab some UPnP info for diagnostics
				
				try{
				    PluginInterface pi_upnp = azureus_core.getPluginManager().getPluginInterfaceByClass( UPnPPlugin.class );

				    if ( pi_upnp != null ){
				    	
				        UPnPPlugin upnp = (UPnPPlugin)pi_upnp.getPlugin();

				        if ( upnp.isEnabled()){
				        	
				        	List<Map<String,String>>	upnp_list = new ArrayList<Map<String,String>>();
				        	
				        	payload.put( "upnp", upnp_list );
				        	
				        	UPnPPluginService[] services = upnp.getServices();
				        	
				        	Set<UPnPRootDevice> devices = new HashSet<UPnPRootDevice>();
				        	
				        	for ( UPnPPluginService service: services ){
				        		
				        		UPnPRootDevice root_device = service.getService().getGenericService().getDevice().getRootDevice();
				        		
				        		if ( !devices.contains( root_device )){
				        			
				        			devices.add( root_device );
				        	
					        		Map<String,String>	map = new HashMap<String, String>();
					        	
					        		upnp_list.add( map );
					        		
					        		map.put( "i", root_device.getInfo());
				        		}
				        	}
				        }
				    }
				}catch( Throwable e ){					
				}
				
				try{
					NetworkAdmin admin = NetworkAdmin.getSingleton();
					
					NetworkAdminHTTPProxy http_proxy = admin.getHTTPProxy();
					
					if ( http_proxy != null ){
						
						payload.put( "hp", http_proxy.getName());
					}
					
					NetworkAdminSocksProxy[] socks_proxies = admin.getSocksProxies();
					
					if ( socks_proxies.length > 0 ){
						
						payload.put( "sp", socks_proxies[0].getName());
					}
				}catch( Throwable e ){	
				}
				
				payload.put( "_enabled", is_enabled?1L:0L );
			}
			
			if ( DEBUG ){
				System.out.println( "PS: doUpdate: " + payload );
			}
			
			sendRequest( "update", payload );
			
			synchronized( this ){

				consec_update_fails	= 0;
				
				must_update_once = false;
				
				if ( deferred_update_event == null ){
										
					COConfigurationManager.setParameter( "pairing.updateoutstanding", false );
				}

				update_in_progress = false;
				
				if ( global_update_event == null ){
					
					setStatus( MessageText.getString( is_enabled?"pairing.status.noservices":"pairing.status.disabled" ));
					
				}else{
					
					setStatus( 
						MessageText.getString( 
							"pairing.status.registered", 
							new String[]{ new SimpleDateFormat().format(new Date( SystemTime.getCurrentTime() ))}));
				}
			}
		}catch( Throwable e ){
			
			synchronized( this ){
				
				try{
					consec_update_fails++;
		
					long back_off = min_update_period;
					
					for (int i=0;i<consec_update_fails;i++){
						
						back_off *= 2;
						
						if ( back_off > max_update_period ){
						
							back_off = max_update_period;
							
							break;
						}
					}
					
					deferUpdate( back_off );
					
				}finally{
				
					update_in_progress = false;
				}
			}
		}finally{
			
			synchronized( this ){
				
				if ( update_in_progress ){
				
					Debug.out( "Something didn't clear update_in_progress!!!!" );
					
					update_in_progress = false;
				}
			}
		}
	}
	
	protected void
	deferUpdate(
		long	millis )
	{
		millis += 5000;
		
		long target = SystemTime.getOffsetTime( millis );
		
		deferred_update_event = 
			SimpleTimer.addEvent(
				"PM:defer",
				target,
				new TimerEventPerformer()
				{
					public void 
					perform(
						TimerEvent event )
					{
						synchronized( PairingManagerImpl.this ){
							
							deferred_update_event = null;
						}
						
						COConfigurationManager.setParameter( "pairing.updateoutstanding", false );
						
						updateNeeded();
					}
				});
		
		setStatus( 
				MessageText.getString( 
					"pairing.status.pending", 
					new String[]{ new SimpleDateFormat().format(new Date( target ))}));

		COConfigurationManager.setParameter( "pairing.updateoutstanding", true );
	}
	
	
	private Map<String, Object> 
	sendRequest(
		String 				command,
		Map<String, Object> payload )
		
		throws PairingException
	{
		try{
			Map<String, Object> request = new HashMap<String, Object>();

			CryptoManager cman = CryptoManagerFactory.getSingleton();

			String azid = Base32.encode( cman.getSecureID());

			payload.put( "_azid", azid );

			try{
				String pk = Base32.encode( cman.getECCHandler().getPublicKey( "pairing" ));

				payload.put( "_pk", pk );
				
			}catch( Throwable e ){	
			}
			
			request.put( "req", payload );
			
			String request_str = Base32.encode( BEncoder.encode( request ));
			
			String	sig = null;
			
			try{
				sig = Base32.encode( cman.getECCHandler().sign( request_str.getBytes( "UTF-8" ), "pairing" ));
				
			}catch( Throwable e ){
			}
			
			String other_params = 
				"&ver=" + UrlUtils.encode( Constants.AZUREUS_VERSION ) + 
				"&app=" + UrlUtils.encode( SystemProperties.getApplicationName()) +
				"&locale=" + UrlUtils.encode( MessageText.getCurrentLocale().toString());

			if ( sig != null ){
				
				other_params += "&sig=" + sig;
			}
			
			URL target = new URL( SERVICE_URL + "/client/" + command + "?request=" + request_str + other_params );
			
			Properties	http_properties = new Properties();
			
			http_properties.put( ClientIDGenerator.PR_URL, target );
				
			try{
				ClientIDManagerImpl.getSingleton().generateHTTPProperties( http_properties );
				
			}catch( ClientIDException e ){
				
				throw( new IOException( e.getMessage()));
			}
			
			target = (URL)http_properties.get( ClientIDGenerator.PR_URL );
			
			HttpURLConnection connection = (HttpURLConnection)target.openConnection();
			
			connection.setConnectTimeout( 30*1000 );
			
			InputStream is = connection.getInputStream();
			
			Map<String,Object> response = (Map<String,Object>)BDecoder.decode( new BufferedInputStream( is ));
			
			synchronized( this ){
				
				Long	min_retry = (Long)response.get( "min_secs" );
				
				if ( min_retry != null ){
					
					min_update_period	= min_retry.intValue()*1000;
				}
				
				Long	max_retry = (Long)response.get( "max_secs" );
				
				if ( max_retry != null ){
					
					max_update_period	= max_retry.intValue()*1000;
				}
			}
			
			final String message = getString( response, "message" );
			
			if ( message != null ){
				
				if ( last_message == null || !last_message.equals( message )){
					
					last_message = message;
				
					try{
						byte[] message_sig = (byte[])response.get( "message_sig" );
						
						AEVerifier.verifyData( message, message_sig );
						
						new AEThread2( "PairMsg", true )
						{
							public void
							run()
							{
								UIManager ui_manager = StaticUtilities.getUIManager( 120*1000 );
								
								if ( ui_manager != null ){
								
									ui_manager.showMessageBox(
											"pairing.server.warning.title",
											"!" + message + "!",
											UIManagerEvent.MT_OK );
								}
							}
						}.start();
						
					}catch( Throwable e ){
					}
				}
			}
			
			String error = getString( response, "error" );
			
			if ( error != null ){
				
				throw( new PairingException( error ));
			}
			
			setLastServerError( null );
						
			return((Map<String,Object>)response.get( "rep" ));
			
		}catch( Throwable e ){
						
			setLastServerError( Debug.getNestedExceptionMessage( e ));
			
			if ( e instanceof PairingException ){
				
				throw((PairingException)e);
			}
			
			throw( new PairingException( "invocation failed", e ));
		}
	}
	
	
	public PairingTest 
	testService(
		String 					sid, 
		PairingTestListener 	listener )
	
		throws PairingException 
	{
		return( new TestServiceImpl( sid, listener ));
	}
	
	protected void
	fireChanged()
	{
		for ( PairingManagerListener l: listeners ){
			
			try{
				l.somethingChanged( this );
				
			}catch( Throwable e ){
				
				Debug.out( e );
			}
		}
	}
		
	public void
	addListener(
		PairingManagerListener		l )
	{
		listeners.add( l );
	}
	
	public void
	removeListener(
		PairingManagerListener		l )
	{
		listeners.remove( l );
	}
	
	protected String
	getString(
		Map<String,Object>	map,
		String				name )
	
		throws IOException
	{
		byte[]	bytes = (byte[])map.get(name);
		
		if ( bytes == null ){
			
			return( null );
		}
		
		return( new String( bytes, "UTF-8" ));
	}
	
	protected class
	TestServiceImpl
		implements PairingTest
	{
		final private String					sid;
		final private PairingTestListener		listener;
		
		private volatile int		outcome = OT_PENDING;
		private volatile String		error_message;
		
		private volatile boolean	cancelled;
		
		protected
		TestServiceImpl(
			String 					_sid, 
			PairingTestListener 	_listener )
		{
			sid			= _sid;
			listener	= _listener;
			
			new AEThread2( "PM:test" )
			{
				public void
				run()
				{
					try{
						String	access_code		= null;
						long	sid_wait_start	= -1;
						
						while( true ){
							
							if ( !isEnabled()){
								
								throw( new Exception( "Pairing is disabled" ));
							}
							
							access_code = peekAccessCode();
							
							if ( access_code != null ){
								
								if ( !hasActionOutstanding()){
									
									if ( getService( sid ) != null ){
									
										break;
										
									}else{
										
										long	now = SystemTime.getMonotonousTime();
										
										if ( sid_wait_start == -1 ){
											
											sid_wait_start = now;
											
										}else{
											
											if ( now - sid_wait_start > 5000 ){
												
												break;
											}
										}
									}
								}
							}
							
							Thread.sleep( 500 );
							
							if ( cancelled ){
								
								outcome = OT_CANCELLED;
								
								return;
							}
						}
						
						PairedService service = getService( sid );
						
						if ( service == null ){
							
							throw( new Exception( "Service not found" ));
						}
						
						listener.testStarted( TestServiceImpl.this );
						
						String other_params = 
							"&ver=" + UrlUtils.encode( Constants.AZUREUS_VERSION ) + 
							"&app=" + UrlUtils.encode( SystemProperties.getApplicationName()) +
							"&locale=" + UrlUtils.encode( MessageText.getCurrentLocale().toString());
						
						URL target = new URL( SERVICE_URL + "/web/test?sid=" + sid + "&ac=" + access_code + "&format=bencode" + other_params );
						
						HttpURLConnection connection = (HttpURLConnection)target.openConnection();
						
						connection.setConnectTimeout( 10*1000 );
						
						try{
							InputStream is = connection.getInputStream();
							
							Map<String,Object> response = (Map<String,Object>)BDecoder.decode( new BufferedInputStream( is ));
							
							response = BDecoder.decodeStrings( response );
							
							Long code = (Long)response.get( "code" );
							
							if ( code == null ){
								
								throw( new Exception( "Code missing from reply" ));
							}
												
							error_message = (String)response.get( "msg" );
							
							if ( code == 1 ){
								
								outcome = OT_SUCCESS;
								
							}else if ( code == 2 ){
								
								outcome = OT_SERVER_OVERLOADED;
								
							}else if ( code == 3 ){
								
								outcome = OT_SERVER_FAILED;
								
							}else if ( code == 4 ){
								
								outcome = OT_FAILED;
								
								error_message = "Connect timeout";
								
							}else if ( code == 5 ){
								
								outcome = OT_FAILED;
								
							}else{
								
								outcome = OT_SERVER_FAILED;;
								
								error_message = "Unknown response code " + code;
							}
						}catch( SocketTimeoutException e ){
							
							outcome = OT_SERVER_UNAVAILABLE;
							
							error_message = "Connect timeout";
							
						}
					}catch( Throwable e ){
						
						outcome = OT_SERVER_UNAVAILABLE;
						
						error_message = Debug.getNestedExceptionMessage( e );
						
					}finally{
						
						listener.testComplete( TestServiceImpl.this );
					}
				}
			}.start();
		}
		
		public int
		getOutcome()
		{
			return( outcome );
		}
		
		public String
		getErrorMessage()
		{
			return( error_message );
		}
		
		public void
		cancel()
		{
			cancelled	= true;
		}
	}
	
	protected class
	PairedServiceImpl
		implements PairedService, PairingConnectionData
	{
		private String				sid;
		private Map<String,String>	attributes	= new HashMap<String, String>();
		
		protected
		PairedServiceImpl(
			String		_sid )
		{
			sid		= _sid;
		}
		
		public String
		getSID()
		{
			return( sid );
		}
		
		public PairingConnectionData
		getConnectionData()
		{
			return( this );
		}
		
		public void
		remove()
		{
			PairingManagerImpl.this.remove( this );
		}
		
		public void
		setAttribute(
			String		name,
			String		value )
		{
			synchronized( this ){
				
				if ( DEBUG ){
					System.out.println( "PS: " + sid + ": " + name + " -> " + value );
				}
				
				if  ( value == null ){
					
					attributes.remove( name );
					
				}else{
					
					attributes.put( name, value );
				}
			}
		}
		
		public String
		getAttribute(
			String		name )
		{
			return( attributes.get( name ));
		}
		
		public void
		sync()
		{
			PairingManagerImpl.this.sync( this );
		}
		
		protected Map<String,String>
		toMap()
		{
			Map<String,String> result = new HashMap<String, String>();
			
			result.put( "sid", sid );
			
			synchronized( this ){
			
				result.putAll( attributes );
			}
			
			return( result );
		}
	}
	
	private class
	PairedNodeImpl
		implements PairedNode
	{
		private Map		map;
		
		protected
		PairedNodeImpl(
			Map		_map )
		{
			map	= _map;
		}
		
		public String
		getAccessCode()
		{
			return((String)map.get( "ac" ));
		}
		
		public List<InetAddress>
		getAddresses()
		{
			Set<InetAddress> addresses = new HashSet<InetAddress>();
			
			addAddress( addresses, "c_v4" );
			addAddress( addresses, "c_v6" );
			addAddress( addresses, "l_v4" );
			addAddress( addresses, "l_v6" );
			addAddress( addresses, "e_v4" );
			addAddress( addresses, "e_v6" );
			addAddress( addresses, "e_l_v4" );
			addAddress( addresses, "e_l_v6" );
			addAddress( addresses, "e_h" );
			
			return( new ArrayList<InetAddress>( addresses ));
		}
		
		private void
		addAddress(
			Set<InetAddress>	addresses,
			String				key )
		{
			String str = (String)map.get( key );
			
			if ( str != null ){
				
				String[] bits = str.split(",");
				
				for ( String bit: bits ){
					
					bit = bit.trim();
					
					if ( bit.length() == 0 ){
						
						continue;
					}
					
					
					if ( bit.endsWith( "*" )){
						
						bit = bit.substring( 0, bit.length()-1 );
					}

					try{
						addresses.add( InetAddress.getByName( bit ));
						
					}catch( Throwable e ){
					}
				}
			}
		}
		
		public List<PairedService>
		getServices()
		{
			Map<String,Map> smap = (Map)map.get( "services" );
			
			List<PairedService>	services = new ArrayList<PairedService>();
			
			for ( Map.Entry<String,Map> entry: smap.entrySet()){
				
				services.add( new PairedService2Impl( entry.getKey(), entry.getValue()));
			}
			
			return( services );
		}
	}
	
	private class
	PairedService2Impl
		implements PairedService
	{
		private String		sid;
		private Map			map;
		
		protected
		PairedService2Impl(
			String		_sid,
			Map			_map )
		{
			sid		= _sid;
			map		= _map;
		}
		
		public String
		getSID()
		{
			return( sid );
		}
		
		public PairingConnectionData
		getConnectionData()
		{
			return( new PairingConnectionData2( map ));
		}
		
		public void
		remove()
		{
			throw( new RuntimeException( "Not supported" ));
		}
	}
	
	private class
	PairingConnectionData2
		implements PairingConnectionData
	{
		private Map		map;
		
		protected
		PairingConnectionData2(
			Map		_map )
		{
			map		= _map;
		}
		
		public void
		setAttribute(
			String		name,
			String		value )
		{
			throw( new RuntimeException( "Not supported" ));
		}
		
		public String
		getAttribute(
			String		name )
		{
			return( (String)map.get( name ));
		}
		
		public void
		sync()
		{
			throw( new RuntimeException( "Not supported" ));
		}
	}
}
