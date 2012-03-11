/*
 * Created on Jan 30, 2008
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


package com.aelitis.azureus.plugins.net.netstatus;


import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread2;

import org.gudy.azureus2.core3.util.Constants;

import org.gudy.azureus2.plugins.*;

import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIInstance;
import org.gudy.azureus2.plugins.ui.UIManagerListener;
import org.gudy.azureus2.plugins.ui.config.ActionParameter;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.ParameterListener;
import org.gudy.azureus2.plugins.ui.config.StringParameter;
import org.gudy.azureus2.plugins.ui.model.BasicPluginConfigModel;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;

import com.aelitis.azureus.plugins.net.netstatus.swt.NetStatusPluginView;

public class 
NetStatusPlugin
	implements Plugin
{
	public static final String VIEW_ID = "aznetstatus";
	
	private PluginInterface	plugin_interface;
	
	private LoggerChannel	logger;
	
	private StringParameter ping_target;
	
	private ActionParameter test_button;
	private StringParameter test_address;
	
	private NetStatusProtocolTester		protocol_tester;
	private AESemaphore					protocol_tester_sem	= new AESemaphore( "ProtTestSem" );
	
	public static void
	load(
		PluginInterface		plugin_interface )
	{
		String name = 
			plugin_interface.getUtilities().getLocaleUtilities().getLocalisedMessageText( "Views.plugins." + VIEW_ID + ".title" );
		
		plugin_interface.getPluginProperties().setProperty( "plugin.version", 	"1.0" );
		plugin_interface.getPluginProperties().setProperty( "plugin.name", 		name );

	}
	
	public void
	initialize(
		final PluginInterface		_plugin_interface )
	{
		plugin_interface	= _plugin_interface;
				
		logger = plugin_interface.getLogger().getChannel( "NetStatus" );
		
		logger.setDiagnostic();
				
		BasicPluginConfigModel config = plugin_interface.getUIManager().createBasicPluginConfigModel( "Views.plugins." + VIEW_ID + ".title" );
		
		ping_target = config.addStringParameter2( "plugin.aznetstatus.pingtarget", "plugin.aznetstatus.pingtarget", "www.google.com" );
		
		if ( Constants.isCVSVersion()){
			
			test_address = config.addStringParameter2( "plugin.aznetstatus.test_address", "plugin.aznetstatus.test_address", "" );
	
			test_button = config.addActionParameter2( "test", "test " );
			
			test_button.setEnabled( false );
			
			test_button.addListener(
				new ParameterListener()
				{
					public void
					parameterChanged(
						Parameter	param )
					{
						protocol_tester.runTest( 
								test_address.getValue().trim(),
								new NetStatusProtocolTesterListener()
								{
									public void 
									sessionAdded(
											NetStatusProtocolTesterBT.Session session ) 
									{
									}
									
									public void
									complete(
										NetStatusProtocolTesterBT	tester )
									{
									}
									
									public void 
									log(
										String str ) 
									{
										logger.log( str );
									}
									
									public void 
									logError(
										String str ) 
									{
										logger.log( str );
									}
									
									public void 
									logError(
										String 		str,
										Throwable 	e )
									{
										logger.log( str, e );
									}
								});
					}
				});
		}
		
		plugin_interface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						
						UISWTInstance swt_ui = (UISWTInstance)instance;
						
						NetStatusPluginView view = new NetStatusPluginView( NetStatusPlugin.this );

						swt_ui.addView(	UISWTInstance.VIEW_MAIN, VIEW_ID, view );
						
						//swt_ui.openMainView( VIEW_ID, view, null );
					}
				}

				public void
				UIDetached(
					UIInstance		instance )
				{
				}
			});
		
		plugin_interface.addListener(
			new PluginListener()
			{
				public void
				initializationComplete()
				{
					new AEThread2( "NetstatusPlugin:init", true )
					{
						public void
						run()
						{
							try{
								protocol_tester = new NetStatusProtocolTester( NetStatusPlugin.this, plugin_interface );
								
								if ( test_button != null ){
									
									test_button.setEnabled( true );
								}
							}finally{
								
								protocol_tester_sem.releaseForever();
							}
						}
					}.start();
				}
				
				public void
				closedownInitiated()
				{				
				}
				
				public void
				closedownComplete()
				{				
				}
			});
	}
	
	public NetStatusProtocolTester
	getProtocolTester()
	{
		protocol_tester_sem.reserve();
		
		return( protocol_tester );
	}
	
	public String
	getPingTarget()
	{
		return( ping_target.getValue());
	}
	
	public void
	setBooleanParameter(
		String	name,
		boolean	value )
	{
		plugin_interface.getPluginconfig().setPluginParameter( name , value );
	}
	
	public boolean
	getBooleanParameter(
		String	name,
		boolean	def )
	{
		return( plugin_interface.getPluginconfig().getPluginBooleanParameter( name, def ));
	}
	
	public void
	log(
		String		str )
	{
		logger.log( str );
	}
	
	public void
	log(
		String		str,
		Throwable	e )
	{
		logger.log( str );
		logger.log( e );
	}
}
