/*
 * Created on Mar 19, 2008
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


package com.aelitis.azureus.plugins.net.buddy.swt;

import java.net.URL;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntry;
import org.gudy.azureus2.ui.swt.plugins.UISWTStatusEntryListener;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;

import com.aelitis.azureus.core.security.CryptoHandler;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.security.CryptoManagerKeyListener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPlugin;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginAZ2Listener;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginBuddy;
import com.aelitis.azureus.plugins.net.buddy.BuddyPluginListener;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTracker;
import com.aelitis.azureus.plugins.net.buddy.tracker.BuddyPluginTrackerListener;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;


public class 
BuddyPluginView
	implements UISWTViewEventListener
{
	private BuddyPlugin		plugin;
	private UISWTInstance	ui_instance;
	
	private BuddyPluginViewInstance		current_instance;
	
	private Image iconNLI;
	private Image iconIDLE;
	private Image iconIN;
	private Image iconOUT;
		
	public
	BuddyPluginView(
		BuddyPlugin		_plugin,
		UISWTInstance	_ui_instance )
	{
		plugin			= _plugin;
		ui_instance		= _ui_instance;
		
		plugin.getAZ2Handler().addListener(
			new BuddyPluginAZ2Listener()
			{
				public void
				chatCreated(
					final BuddyPluginAZ2.chatInstance		chat )
				{
					final Display display = ui_instance.getDisplay();
					
					if ( !display.isDisposed()){
						
						display.asyncExec(
							new Runnable()
							{
								public void
								run()
								{
									if ( !display.isDisposed()){
									
										new BuddyPluginViewChat( plugin, display, chat );
									}
								}
							});
					}
				}
				
				public void
				chatDestroyed(
					BuddyPluginAZ2.chatInstance		chat )
				{
				}
			});
		

		SimpleTimer.addEvent("BuddyStatusInit", SystemTime.getOffsetTime(1000),
				new TimerEventPerformer() {
					public void perform(
							TimerEvent event ) 
					{
						UISWTStatusEntry label = ui_instance.createStatusEntry();

						label.setText(MessageText.getString("azbuddy.tracker.bbb.status.title"));

						new statusUpdater(ui_instance);
					}
				});

		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				ImageLoader imageLoader = ImageLoader.getInstance();

				iconNLI = imageLoader.getImage( "bbb_nli" );
				iconIDLE = imageLoader.getImage( "bbb_idle" );
				iconIN = imageLoader.getImage( "bbb_in" );
				iconOUT = imageLoader.getImage( "bbb_out" );
			}
		});
	}
	
	public boolean 
	eventOccurred(
		UISWTViewEvent event )
	{
		switch( event.getType() ){

			case UISWTViewEvent.TYPE_CREATE:{
				
				if ( current_instance != null ){
					
					return( false );
				}
								
				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
				

				current_instance = new BuddyPluginViewInstance(plugin, ui_instance, (Composite)event.getData());
				
				break;
			}
			case UISWTViewEvent.TYPE_CLOSE:
			case UISWTViewEvent.TYPE_DESTROY:{
				
				try{
					if ( current_instance != null ){
						
						current_instance.destroy();
					}
				}finally{
					
					current_instance = null;
				}
				
				break;
			}
		}
		
		return true;
	}
	
	protected class
	statusUpdater
		implements BuddyPluginTrackerListener
	{
		private UISWTStatusEntry	label;
		private UISWTStatusEntry	status;
		private BuddyPluginTracker	tracker;
		
		private TimerEventPeriodic	update_event;

		private CryptoManager	crypto;
		private boolean			crypto_ok;
		private boolean			has_buddies;
		
		protected
		statusUpdater(
			UISWTInstance		instance )
		{
			status	= ui_instance.createStatusEntry();
			label 	= ui_instance.createStatusEntry();
			
			label.setText( MessageText.getString( "azbuddy.tracker.bbb.status.title" ));
			label.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.title.tooltip" ));
			
			tracker = plugin.getTracker();
				
			status.setText( "" );
			
			status.setImageEnabled( true );
			
			tracker.addListener( this );
			
			has_buddies = plugin.getBuddies().size() > 0;
			
			status.setVisible( tracker.isEnabled() && has_buddies);
			label.setVisible( tracker.isEnabled() && has_buddies);
		
			/*
			MenuItem mi = plugin.getPluginInterface().getUIManager().getMenuManager().addMenuItem(
									status.getMenuContext(),
									"dweeble" );
			
			mi.addListener(
				new MenuItemListener()
				{
					public void
					selected(
						MenuItem			menu,
						Object 				target )
					{
						System.out.println( "whee" );
					}
				});
			*/
			
			UISWTStatusEntryListener click_listener = 
				new UISWTStatusEntryListener()
			{
					public void 
					entryClicked(
						UISWTStatusEntry entry )
					{
						try{
							plugin.getPluginInterface().getUIManager().openURL(
									new URL( "http://wiki.vuze.com" ));
							
						}catch( Throwable e ){
							
							Debug.printStackTrace(e);
						}
					}
				};
				
			status.setListener( click_listener );
			label.setListener( click_listener );
	
			
			plugin.addListener( 
				new BuddyPluginListener()
				{
					public void
					initialised(
						boolean		available )
					{
					}
					
					public void
					buddyAdded(
						BuddyPluginBuddy	buddy )
					{
						if ( !has_buddies ){
							
							has_buddies = true;
						
							updateStatus();
						}
					}
					
					public void
					buddyRemoved(
						BuddyPluginBuddy	buddy )
					{
						has_buddies	= plugin.getBuddies().size() > 0;	
						
						if ( !has_buddies ){
							
							updateStatus();
						}
					}

					public void
					buddyChanged(
						BuddyPluginBuddy	buddy )
					{
					}
					
					public void
					messageLogged(
						String		str,
						boolean		error )
					{
					}
					
					public void
					enabledStateChanged(
						boolean enabled )
					{
					}
				});
			
			crypto = CryptoManagerFactory.getSingleton();
			
			crypto.addKeyListener(
				new CryptoManagerKeyListener()
				{
					public void
					keyChanged(
						CryptoHandler		handler )
					{
					}
					
					public void
					keyLockStatusChanged(
						CryptoHandler		handler )
					{
						boolean	ok = crypto.getECCHandler().isUnlocked();
						
						if ( ok != crypto_ok ){
							
							crypto_ok = ok;
							
							updateStatus();
						}
					}
				});
			
			crypto_ok = crypto.getECCHandler().isUnlocked();
				
			updateStatus();
		}
				
		public void
		networkStatusChanged(
			BuddyPluginTracker	tracker,
			int					new_status )
		{
			updateStatus();
		}
		
		protected synchronized void
		updateStatus()
		{
			if ( tracker.isEnabled() && has_buddies ){
				
				status.setVisible( true );
				label.setVisible( true );
				
				if ( has_buddies && !crypto_ok ){
					
					status.setImage( iconNLI );
					
					status.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.nli" ));

					disableUpdates();
					
				}else{
					
					int	network_status = tracker.getNetworkStatus();
					
					if ( network_status == BuddyPluginTracker.BUDDY_NETWORK_IDLE ){
						
						status.setImage( iconIDLE );
						
						status.setTooltipText( MessageText.getString( "azbuddy.tracker.bbb.status.idle" ));
						
						disableUpdates();
						
					}else if ( network_status == BuddyPluginTracker.BUDDY_NETWORK_INBOUND ){
						
						status.setImage( iconIN );
						
						enableUpdates();
						
					}else{
						
						status.setImage( iconOUT );
						
						enableUpdates();
					}
				}
			}else{
				
				disableUpdates();
				
				status.setVisible( false );
				label.setVisible( false );
			}
		}
		
		protected void
		enableUpdates()
		{
			if ( update_event == null ){
				
				update_event = SimpleTimer.addPeriodicEvent(
					"Buddy:GuiUpdater",
					2500,
					new TimerEventPerformer()
					{
						public void 
						perform(
							TimerEvent event ) 
						{	
							synchronized( statusUpdater.this ){
								
								if ( tracker.isEnabled() && ( crypto_ok || !has_buddies )){
									
									String	tt;
															
									int ns = tracker.getNetworkStatus();
									
									if ( ns == BuddyPluginTracker.BUDDY_NETWORK_IDLE ){
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.idle" );
									
									}else if ( ns == BuddyPluginTracker.BUDDY_NETWORK_INBOUND ){
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.in" ) + ": " + DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker.getNetworkReceiveBytesPerSecond());
										
									}else{
										
										tt = MessageText.getString( "azbuddy.tracker.bbb.status.out" ) + ": " + DisplayFormatters.formatByteCountToKiBEtcPerSec( tracker.getNetworkSendBytesPerSecond());
									}
																			
									status.setTooltipText( tt );
								}
							}
						}					
					});
			}
		}
		
		protected void
		disableUpdates()
		{
			if ( update_event != null ){

				update_event.cancel();
				
				update_event = null;
			}
		}
		
		public void 
		enabledStateChanged(
			BuddyPluginTracker 		tracker,
			boolean 				enabled ) 
		{
			updateStatus();
		}
	}
}
