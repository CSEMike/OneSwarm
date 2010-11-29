/*
 * Created on 13-Jul-2004
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

package com.aelitis.azureus.core.impl;

import java.util.ArrayList;
import java.util.List;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.impl.TransferSpeedValidator;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManagerFactory;
import org.gudy.azureus2.core3.global.GlobalManagerStats;
import org.gudy.azureus2.core3.global.GlobalMangerProgressListener;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilterManager;
import org.gudy.azureus2.core3.ipfilter.IpFilterManagerFactory;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncerResponse;
import org.gudy.azureus2.core3.tracker.host.TRHost;
import org.gudy.azureus2.core3.tracker.host.TRHostFactory;
import org.gudy.azureus2.core3.util.AEDiagnostics;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AETemporaryFileHandler;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.ListenerManager;
import org.gudy.azureus2.core3.util.ListenerManagerDispatcher;
import org.gudy.azureus2.core3.util.NonDaemonTaskRunner;
import org.gudy.azureus2.core3.util.SimpleTimer;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.core3.util.TimerEvent;
import org.gudy.azureus2.core3.util.TimerEventPerformer;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.platform.PlatformManagerListener;
import org.gudy.azureus2.plugins.PluginEvent;
import org.gudy.azureus2.plugins.PluginEventListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.PluginManagerDefaults;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.AzureusCoreListener;
import com.aelitis.azureus.core.AzureusCoreOperation;
import com.aelitis.azureus.core.AzureusCoreOperationListener;
import com.aelitis.azureus.core.AzureusCoreOperationTask;
import com.aelitis.azureus.core.dht.DHT;
import com.aelitis.azureus.core.instancemanager.AZInstanceManager;
import com.aelitis.azureus.core.instancemanager.AZInstanceManagerFactory;
import com.aelitis.azureus.core.nat.NATTraverser;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdmin;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterface;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminNetworkInterfaceAddress;
import com.aelitis.azureus.core.networkmanager.admin.NetworkAdminPropertyChangeListener;
import com.aelitis.azureus.core.peermanager.PeerManager;
import com.aelitis.azureus.core.peermanager.download.session.TorrentSessionManager;
import com.aelitis.azureus.core.peermanager.nat.PeerNATTraverser;
import com.aelitis.azureus.core.security.CryptoManager;
import com.aelitis.azureus.core.security.CryptoManagerFactory;
import com.aelitis.azureus.core.speedmanager.SpeedManager;
import com.aelitis.azureus.core.speedmanager.SpeedManagerAdapter;
import com.aelitis.azureus.core.speedmanager.SpeedManagerFactory;
import com.aelitis.azureus.core.update.AzureusRestarterFactory;
import com.aelitis.azureus.plugins.dht.DHTPlugin;
import com.aelitis.azureus.plugins.tracker.dht.DHTTrackerPlugin;

/**
 * @author parg
 *
 */

public class
AzureusCoreImpl
	implements 	AzureusCore
{
	private final static LogIDs LOGID = LogIDs.CORE;
	protected static AzureusCore		singleton;
	protected static AEMonitor			class_mon	= new AEMonitor( "AzureusCore:class" );

	private static final String DM_ANNOUNCE_KEY	= "AzureusCore:announce_key";

	public static AzureusCore
	create()

		throws AzureusCoreException
	{
		try{
			class_mon.enter();

			if ( singleton != null ){

				throw( new AzureusCoreException( "Azureus core already instantiated" ));
			}

			singleton	= new AzureusCoreImpl();

			return( singleton );

		}finally{

			class_mon.exit();
		}
	}

	public static boolean
	isCoreAvailable()
	{
		return( singleton != null );
	}

	public static AzureusCore
	getSingleton()

		throws AzureusCoreException
	{
		if ( singleton == null ){

			throw( new AzureusCoreException( "core not instantiated"));
		}

		return( singleton );
	}

	private PluginInitializer 	pi;
	private GlobalManager		global_manager;
	private AZInstanceManager	instance_manager;
	private SpeedManager		speed_manager;
	private CryptoManager		crypto_manager;
	private NATTraverser		nat_traverser;

	private final long create_time = SystemTime.getCurrentTime();


	private boolean				started;
	private boolean				stopped;
	private final List				listeners				= new ArrayList();
	private final List				lifecycle_listeners		= new ArrayList();
	private final List				operation_listeners		= new ArrayList();

	private final AESemaphore			stopping_sem	= new AESemaphore( "AzureusCore::stopping" );

	private final AEMonitor			this_mon		= new AEMonitor( "AzureusCore" );

	private final AzureusCoreOperation	initialisation_op = createOperation( AzureusCoreOperation.OP_INITIALISATION );

	protected
	AzureusCoreImpl()
	{
		COConfigurationManager.initialise();

		MessageText.loadBundle();

		AEDiagnostics.startup();

		AEDiagnostics.markDirty();

		AETemporaryFileHandler.startup();

		AEThread.setOurThread();

			// set up a backwards pointer from config -> app dir so we can derive one from the other. It'll get saved on closedown, no need to do so now

		COConfigurationManager.setParameter( "azureus.application.directory", SystemProperties.getApplicationPath());

		crypto_manager = CryptoManagerFactory.getSingleton();

		PlatformManagerFactory.getPlatformManager().addListener(
			new PlatformManagerListener()
			{
				public void
				eventOccurred(
					int		type )
				{
					if ( type == ET_SHUTDOWN ){

						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested shutdown"));
						}

						stop();

					}else if ( type == ET_SUSPEND ){

						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested suspend"));
						}

						COConfigurationManager.save();

					}else if ( type == ET_RESUME ){

						if (Logger.isEnabled()){
							Logger.log(new LogEvent(LOGID, "Platform manager requested resume"));
						}

						announceAll( true );
					}
				}
			});

			//ensure early initialization

		NetworkManager.getSingleton();

		PeerManager.getSingleton();

	    TorrentSessionManager.getSingleton().init();

		pi = PluginInitializer.getSingleton( this, initialisation_op );

		instance_manager = AZInstanceManagerFactory.getSingleton( this );

		speed_manager	=
			SpeedManagerFactory.createSpeedManager(
					this,
					new SpeedManagerAdapter()
					{
						private boolean setting_limits;

						public int
						getCurrentProtocolUploadSpeed(
							int	average_period )
						{
							if ( global_manager != null ){

								GlobalManagerStats stats = global_manager.getStats();

								return( stats.getProtocolSendRateNoLAN( average_period ));

							}else{

								return(0);
							}
						}

						public int
						getCurrentDataUploadSpeed(
							int	average_period )
						{
							if ( global_manager != null ){

								GlobalManagerStats stats = global_manager.getStats();

								return( stats.getDataSendRateNoLAN( average_period ));

							}else{

								return(0);
							}
						}

                        public int
                        getCurrentProtocolDownloadSpeed(
                        	int	average_period )
                        {
                            if( global_manager != null ){
                                GlobalManagerStats stats = global_manager.getStats();
                                return (stats.getProtocolReceiveRateNoLAN( average_period ) );
                            }else{
                                return(0);
                            }
                        }

                        public int
                        getCurrentDataDownloadSpeed(
                        	int	average_period )
                        {
                            if( global_manager != null ){
                                GlobalManagerStats stats = global_manager.getStats();
                                return (stats.getDataReceiveRateNoLAN( average_period ) );
                            }else{
                                return(0);
                            }
                        }

                        public int
						getCurrentUploadLimit()
						{
							String key = TransferSpeedValidator.getActiveUploadParameter( global_manager );

							int	k_per_second = COConfigurationManager.getIntParameter( key );

							int	bytes_per_second;

							if ( k_per_second == 0 ){

								bytes_per_second = Integer.MAX_VALUE;

							}else{

								bytes_per_second = k_per_second*1024;
							}

							return( bytes_per_second );
						}

						public void
						setCurrentUploadLimit(
							int		bytes_per_second )
						{
							if ( bytes_per_second != getCurrentUploadLimit()){

								String key = TransferSpeedValidator.getActiveUploadParameter( global_manager );

								int	k_per_second;

								if ( bytes_per_second == Integer.MAX_VALUE ){

									k_per_second	= 0;

								}else{

									k_per_second = bytes_per_second/1024;
								}

								COConfigurationManager.setParameter( key, k_per_second );
							}
						}

						public int
						getCurrentDownloadLimit()
						{
							return( TransferSpeedValidator.getGlobalDownloadRateLimitBytesPerSecond());
						}

						public void
						setCurrentDownloadLimit(
							int		bytes_per_second )
						{
							TransferSpeedValidator.setGlobalDownloadRateLimitBytesPerSecond( bytes_per_second );
						}

						public Object
						getLimits()
						{
							String up_key 	= TransferSpeedValidator.getActiveUploadParameter( global_manager );
							String down_key	= TransferSpeedValidator.getDownloadParameter();

							return(
								new Object[]{
									up_key,
									new Integer( COConfigurationManager.getIntParameter( up_key )),
									down_key,
									new Integer( COConfigurationManager.getIntParameter( down_key )),
								});
						}

						public void
						setLimits(
							Object		limits,
							boolean		do_up,
							boolean		do_down )
						{
							if ( limits == null ){

								return;
							}
							try{
								if ( setting_limits ){

									return;
								}

								setting_limits	= true;

								Object[]	bits = (Object[])limits;

								if ( do_up ){

									COConfigurationManager.setParameter((String)bits[0], ((Integer)bits[1]).intValue());
								}

								if ( do_down ){

									COConfigurationManager.setParameter((String)bits[2], ((Integer)bits[3]).intValue());
								}

							}finally{

								setting_limits	= false;

							}
						}
					});

		nat_traverser = new NATTraverser( this );

		PeerNATTraverser.initialise( this );

			// one off explicit GC to clear up initialisation mem

		SimpleTimer.addEvent(
				"AzureusCore:gc",
				SystemTime.getOffsetTime(60*1000),
				new TimerEventPerformer()
				{
					public void
					perform(
						TimerEvent event)
					{
						System.gc();
					}
				});
	}

	protected void
	announceAll(
		boolean	force )
	{
		Logger.log(	new LogEvent(LOGID, "Updating trackers" ));

		GlobalManager gm = getGlobalManager();

		if ( gm != null ){

			List	downloads = gm.getDownloadManagers();

			long now	= SystemTime.getCurrentTime();

			for (int i=0;i<downloads.size();i++){

				DownloadManager	dm = (DownloadManager)downloads.get(i);

				Long	last_announce_l = (Long)dm.getData( DM_ANNOUNCE_KEY );

				long	last_announce	= last_announce_l==null?create_time:last_announce_l.longValue();

				TRTrackerAnnouncer an = dm.getTrackerClient();

				if ( an != null ){

					TRTrackerAnnouncerResponse last_announce_response = an.getLastResponse();

					if ( 	now - last_announce > 15*60*1000 ||
							last_announce_response == null ||
							last_announce_response.getStatus() == TRTrackerAnnouncerResponse.ST_OFFLINE ||
							force ){

						dm.setData( DM_ANNOUNCE_KEY, new Long( now ));

						Logger.log(	new LogEvent(LOGID, "    updating tracker for " + dm.getDisplayName()));

						dm.requestTrackerAnnounce( true );
					}
				}
			}
		}

	    PluginInterface dht_tracker_pi = getPluginManager().getPluginInterfaceByClass( DHTTrackerPlugin.class );

	    if ( dht_tracker_pi != null ){

	    	((DHTTrackerPlugin)dht_tracker_pi.getPlugin()).announceAll();
	    }
	}

	public LocaleUtil
	getLocaleUtil()
	{
		return( LocaleUtil.getSingleton());
	}

	public void
	start()

		throws AzureusCoreException
	{
		AEThread.setOurThread();

		try{
			this_mon.enter();

			if ( started ){

				throw( new AzureusCoreException( "Core: already started" ));
			}

			if ( stopped ){

				throw( new AzureusCoreException( "Core: already stopped" ));
			}

			started	= true;

		}finally{

			this_mon.exit();
		}

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Loading of Plugins starts"));

		pi.loadPlugins(this, false);

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Loading of Plugins complete"));

		// Disable async loading of existing torrents, because there are many things
		// (like hosting) that require all the torrents to be loaded.  While we
		// can write code for each of these cases to wait until the torrents are
		// loaded, it's a pretty big job to find them all and fix all their quirks.
		// Too big of a job for this late in the release stage.
		// Other example is the tracker plugin that is coded in a way where it must
		// always publish a complete rss feed

		global_manager = GlobalManagerFactory.create(
				this,
				new GlobalMangerProgressListener()
				{
					public void
					reportCurrentTask(
						String currentTask )
					{
						initialisation_op.reportCurrentTask( currentTask );
					}

					public void
					reportPercent(
						int percent )
					{
						initialisation_op.reportPercent( percent );
					}
				}, 0);

		triggerLifeCycleComponentCreated(global_manager);

		pi.initialisePlugins();

		if (Logger.isEnabled())
			Logger.log(new LogEvent(LOGID, "Initializing Plugins complete"));

		try{
			PluginInterface dht_pi 	= getPluginManager().getPluginInterfaceByClass( DHTPlugin.class );

			if ( dht_pi != null ){

				dht_pi.addEventListener(
					new PluginEventListener()
					{
						private boolean	first_dht = true;

						public void
						handleEvent(
							PluginEvent	ev )
						{
							if ( ev.getType() == DHTPlugin.EVENT_DHT_AVAILABLE ){

								if ( first_dht ){

									first_dht	= false;

									DHT 	dht = (DHT)ev.getValue();

									speed_manager.setSpeedTester( dht.getSpeedTester());

									global_manager.addListener(
											new GlobalManagerAdapter()
											{
												@Override
												public void
												seedingStatusChanged(
													boolean seeding_only_mode )
												{
													checkConfig();
												}
											});

									COConfigurationManager.addAndFireParameterListeners(
										new String[]{	TransferSpeedValidator.AUTO_UPLOAD_ENABLED_CONFIGKEY,
														TransferSpeedValidator.AUTO_UPLOAD_SEEDING_ENABLED_CONFIGKEY },
										new ParameterListener()
										{
											public void
											parameterChanged(
												String parameterName )
											{
												checkConfig();
											}
										});

								}
							}
						}

						protected void
						checkConfig()
						{
							String	key = TransferSpeedValidator.getActiveAutoUploadParameter( global_manager );

							speed_manager.setEnabled( COConfigurationManager.getBooleanParameter( key ));
						}

					});
			}
		}catch( Throwable e ){
		}

	    new AEThread("Plugin Init Complete")
	       {
	        	@Override
				public void
	        	runSupport()
	        	{
	        		pi.initialisationComplete();

	        		for (int i=0;i<lifecycle_listeners.size();i++){

	        			try{
	        				((AzureusCoreLifecycleListener)lifecycle_listeners.get(i)).started( AzureusCoreImpl.this );

	        			}catch( Throwable e ){

	        				Debug.printStackTrace(e);
	        			}
	        		}
	        	}
	       }.start();

	   if ( COConfigurationManager.getBooleanParameter( "Resume Downloads On Start" )){

		   global_manager.resumeDownloads();
	   }

       //late inits
	   NetworkManager.getSingleton().initialize();

	   instance_manager.initialize();


	   Runtime.getRuntime().addShutdownHook( new AEThread("Shutdown Hook") {
	     @Override
		public void runSupport() {
			Logger.log(new LogEvent(LOGID, "Shutdown hook triggered" ));
			AzureusCoreImpl.this.stop();
	     }
	   });

	   AEDiagnostics.checkDumpsAndNatives();

	   NetworkAdmin na = NetworkAdmin.getSingleton();

	   na.runInitialChecks();

	   na.addPropertyChangeListener(
			   new NetworkAdminPropertyChangeListener()
			   {
				   private String	last_as;

				   public void
				   propertyChanged(
						   String		property )
				   {
					   NetworkAdmin na = NetworkAdmin.getSingleton();

					   if ( property.equals( NetworkAdmin.PR_NETWORK_INTERFACES )){

						   boolean	found_usable = false;

						   NetworkAdminNetworkInterface[] intf = na.getInterfaces();

						   for (int i=0;i<intf.length;i++){

							   NetworkAdminNetworkInterfaceAddress[] addresses = intf[i].getAddresses();

							   for (int j=0;j<addresses.length;j++){

								   if ( !addresses[j].isLoopback()){

									   found_usable = true;
								   }
							   }
						   }

						   		// ignore event if nothing usable

						   if ( !found_usable ){

							   return;
						   }

						   Logger.log(	new LogEvent(LOGID, "Network interfaces have changed (new=" + na.getNetworkInterfacesAsString() + ")"));

						   announceAll( false );

					   }else if ( property.equals( NetworkAdmin.PR_AS )){

						   String	as = na.getCurrentASN().getAS();

						   if ( last_as == null ){

							   last_as = as;

						   }else if ( !as.equals( last_as )){

							   Logger.log(	new LogEvent(LOGID, "AS has changed (new=" + as + ")" ));

							   last_as = as;

							   announceAll( false );
						   }
					   }
				   }
			   });
	}

	public boolean
	isStarted()
	{
		return( started );
	}

	public void triggerLifeCycleComponentCreated(AzureusCoreComponent component) {
		for (int i = 0; i < lifecycle_listeners.size(); i++) {

			try{
				((AzureusCoreLifecycleListener) lifecycle_listeners.get(i))
					.componentCreated(this, component);

			}catch( Throwable e ){

				Debug.printStackTrace(e);
			}
		}
	}

	private void
	runNonDaemon(
		final Runnable	r )

		throws AzureusCoreException
	{
		if ( !Thread.currentThread().isDaemon()){

			r.run();

		}else{

			final AESemaphore	sem = new AESemaphore( "AzureusCore:runNonDaemon" );

			final Throwable[]	error = {null};

			new AEThread( "AzureusCore:runNonDaemon" )
			{
				@Override
				public void
				runSupport()
				{
					try{

						r.run();

					}catch( Throwable e ){

						error[0]	= e;

					}finally{

						sem.release();
					}
				}
			}.start();

			sem.reserve();

			if ( error[0] != null ){

				if ( error[0] instanceof AzureusCoreException ){

					throw((AzureusCoreException)error[0]);

				}else{

					throw( new AzureusCoreException( "Operation failed", error[0] ));
				}
			}
		}
	}

	public void
	stop()

		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
			@Override
			public void runSupport() {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Stop operation starts"));

				stopSupport(true);
			}
		});
	}

	private void
	stopSupport(
		boolean		apply_updates )

		throws AzureusCoreException
	{
		try{
			this_mon.enter();

			if ( stopped ){

					// ensure config is saved as there may be pending changes to persist and we've got here
					// via a shutdown hook

				COConfigurationManager.save();

				Logger.log(new LogEvent(LOGID, "Waiting for stop to complete"));

				stopping_sem.reserve();

				return;
			}

			stopped	= true;

			if ( !started ){

				Logger.log(new LogEvent(LOGID, "Core not started"));

					// might have been marked dirty due to core being created to allow functions to be used but never started...

				if ( AEDiagnostics.isDirty()){

					AEDiagnostics.markClean();
				}

				stopping_sem.releaseForever();

				return;
			}

		}finally{

			this_mon.exit();
		}

		List	sync_listeners 	= new ArrayList();
		List	async_listeners	= new ArrayList();

		for (int i=0;i<lifecycle_listeners.size();i++){

			AzureusCoreLifecycleListener	l = (AzureusCoreLifecycleListener)lifecycle_listeners.get(i);

			if ( l.syncInvokeRequired()){
				sync_listeners.add( l );
			}else{
				async_listeners.add( l );
			}
		}

		try{
			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking synchronous 'stopping' listeners"));

			for (int i=0;i<sync_listeners.size();i++){
				try{
					((AzureusCoreLifecycleListener)sync_listeners.get(i)).stopping( this );

				}catch( Throwable e ){

					Debug.printStackTrace(e);
				}
			}

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking asynchronous 'stopping' listeners"));

				// in case something hangs during listener notification (e.g. version check server is down
				// and the instance manager tries to obtain external address) we limit overall dispatch
				// time to 10 seconds

			ListenerManager.dispatchWithTimeout(
					async_listeners,
					new ListenerManagerDispatcher()
					{
						@Override
						public void
						dispatch(
							Object		listener,
							int			type,
							Object		value )
						{
							((AzureusCoreLifecycleListener)listener).stopping( AzureusCoreImpl.this );
						}
					},
					10*1000 );


			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Stopping global manager"));

			global_manager.stopGlobalManager();

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking synchronous 'stopped' listeners"));

			for (int i=0;i<sync_listeners.size();i++){
				try{
					((AzureusCoreLifecycleListener)sync_listeners.get(i)).stopped( this );

				}catch( Throwable e ){

					Debug.printStackTrace(e);
				}
			}

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Invoking asynchronous 'stopped' listeners"));

			ListenerManager.dispatchWithTimeout(
					async_listeners,
					new ListenerManagerDispatcher()
					{
						@Override
						public void
						dispatch(
							Object		listener,
							int			type,
							Object		value )
						{
							((AzureusCoreLifecycleListener)listener).stopped( AzureusCoreImpl.this );
						}
					},
					10*1000 );

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Waiting for quiescence"));

			NonDaemonTaskRunner.waitUntilIdle();

				// shut down diags - this marks the shutdown as tidy and saves the config

			AEDiagnostics.markClean();

			if (Logger.isEnabled())
				Logger.log(new LogEvent(LOGID, "Stop operation completes"));

				// if any installers exist then we need to closedown via the updater

			if ( 	apply_updates &&
					getPluginManager().getDefaultPluginInterface().getUpdateManager().getInstallers().length > 0 ){

				AzureusRestarterFactory.create( this ).restart( true );
			}

			try{

				if (System.getProperty("oneswarm.integration.test") != null) {
					throw new Exception("Skipping thread shutdown check (integration test).");
				}

				ThreadGroup	tg = Thread.currentThread().getThreadGroup();

				Thread[]	threads = new Thread[tg.activeCount()+32];

				tg.enumerate( threads );

				for (int i=0;i<threads.length;i++){

					final Thread	t = threads[i];

					if ( t != null && t != Thread.currentThread() && !t.isDaemon() && !AEThread.isOurThread( t )){

						new AEThread( "VMKiller", true )
						{
							@Override
							public void
							runSupport()
							{
								try{
									Thread.sleep(10*1000);

									Debug.out( "Non-daemon thread found '" + t.getName() + "', force closing VM" );

									SESecurityManager.exitVM(0);

								}catch( Throwable e ){

								}
							}
						}.start();

						break;
					}
				}
			}catch( Throwable e ){
			}
		}finally{

			stopping_sem.releaseForever();
		}
	}


	public void
	requestStop()

		throws AzureusCoreException
	{
		if (stopped)
			return;

		runNonDaemon(new AERunnable() {
			@Override
			public void runSupport() {

				for (int i = 0; i < lifecycle_listeners.size(); i++) {

					if (!((AzureusCoreLifecycleListener) lifecycle_listeners.get(i))
							.stopRequested(AzureusCoreImpl.this)) {
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
									"Request to stop the core has been denied"));

						return;
					}
				}

				stop();
			}
		});
	}

	public void
	restart()

		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
			@Override
			public void runSupport() {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Restart operation starts"));

				checkRestartSupported();

				stopSupport(false);

				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, "Restart operation: stop complete,"
							+ "restart initiated"));

				AzureusRestarterFactory.create(AzureusCoreImpl.this).restart(false);
			}
		});
	}

	public void
	requestRestart()

		throws AzureusCoreException
	{
		runNonDaemon(new AERunnable() {
            @Override
			public void runSupport() {
                checkRestartSupported();

                for (int i = 0; i < lifecycle_listeners.size(); i++) {
                    AzureusCoreLifecycleListener l = (AzureusCoreLifecycleListener) lifecycle_listeners
                            .get(i);

                    if (!l.restartRequested(AzureusCoreImpl.this)) {

                        if (Logger.isEnabled())
                            Logger.log(new LogEvent(LOGID, LogEvent.LT_WARNING,
                                    "Request to restart the core"
                                            + " has been denied"));

                        return;
                    }
                }

                restart();
            }
        });
	}

	public void
	checkRestartSupported()

		throws AzureusCoreException
	{
		if ( getPluginManager().getPluginInterfaceByClass( "org.gudy.azureus2.update.UpdaterPatcher") == null ){
			Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
					"Can't restart without the 'azupdater' plugin installed"));

			throw( new  AzureusCoreException("Can't restart without the 'azupdater' plugin installed"));
		}
	}

	public GlobalManager
	getGlobalManager()

		throws AzureusCoreException
	{
		if ( global_manager == null ){

			throw( new AzureusCoreException( "Core not running" ));
		}

		return( global_manager );
	}

	public TRHost
	getTrackerHost()

		throws AzureusCoreException
	{
		return( TRHostFactory.getSingleton());
	}

	public PluginManagerDefaults
	getPluginManagerDefaults()

		throws AzureusCoreException
	{
		return( PluginManager.getDefaults());
	}

	public PluginManager
	getPluginManager()

		throws AzureusCoreException
	{
			// don't test for runnign here, the restart process calls this after terminating the core...

		return( PluginInitializer.getDefaultInterface().getPluginManager());
	}

	public IpFilterManager
	getIpFilterManager()

		throws AzureusCoreException
	{
		return( IpFilterManagerFactory.getSingleton());
	}

	public AZInstanceManager
	getInstanceManager()
	{
		return( instance_manager );
	}

	public SpeedManager
	getSpeedManager()
	{
		return( speed_manager );
	}

	public CryptoManager
	getCryptoManager()
	{
		return( crypto_manager );
	}

	public NATTraverser
	getNATTraverser()
	{
		return( nat_traverser );
	}

	public AzureusCoreOperation
	createOperation(
		final int		type )
	{
		AzureusCoreOperation	op =
			new AzureusCoreOperation()
			{
				public int
				getOperationType()
				{
					return( type );
				}

				public AzureusCoreOperationTask
				getTask()
				{
					return null;
				}

				public void
				reportCurrentTask(
					String task )
				{
					AzureusCoreImpl.this.reportCurrentTask( this, task );
				}

				public void
				reportPercent(
					int percent )
				{
					AzureusCoreImpl.this.reportPercent( this, percent );
				}
			};

		for (int i=0;i<operation_listeners.size();i++){

			try{
				((AzureusCoreOperationListener)operation_listeners.get(i)).operationCreated( op );

			}catch( Throwable e ){

				Debug.printStackTrace(e);
			}
		}

		return( op );
	}

	public void
	createOperation(
		final int					type,
		AzureusCoreOperationTask	task )
	{
		final AzureusCoreOperationTask[] f_task = { task };

		AzureusCoreOperation	op =
				new AzureusCoreOperation()
				{
					public int
					getOperationType()
					{
						return( type );
					}

					public AzureusCoreOperationTask
					getTask()
					{
						return( f_task[0] );
					}

					public void
					reportCurrentTask(
						String task )
					{
						AzureusCoreImpl.this.reportCurrentTask( this, task );
					}

					public void
					reportPercent(
						int percent )
					{
						AzureusCoreImpl.this.reportPercent( this, percent );
					}
				};


		for (int i=0;i<operation_listeners.size();i++){

				// don't catch exceptions here as we want errors from task execution to propagate
				// back to the invoker

			if (((AzureusCoreOperationListener)operation_listeners.get(i)).operationCreated( op )){

				f_task[0] = null;
			}
		}

			// nobody volunteeered to run it for us, we'd better do it

		if ( f_task[0] != null ){

			task.run( op );
		}
	}

	protected void
	reportCurrentTask(
		AzureusCoreOperation		op,
		String 						currentTask )
	{
		if ( op.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){

			PluginInitializer.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_TASK, currentTask );
		}

		for (int i=0;i<listeners.size();i++){

			try{
				((AzureusCoreListener)listeners.get(i)).reportCurrentTask( op, currentTask );

			}catch( Throwable e ){

				Debug.printStackTrace(e);
			}
		}
	}

	protected void
	reportPercent(
		AzureusCoreOperation		op,
		int 						percent )
	{
		if ( op.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){

			PluginInitializer.fireEvent( PluginEvent.PEV_INITIALISATION_PROGRESS_PERCENT, new Integer( percent ));
		}

		for (int i=0;i<listeners.size();i++){

			try{
				((AzureusCoreListener)listeners.get(i)).reportPercent( op, percent );

			}catch( Throwable e ){

				Debug.printStackTrace(e);
			}
		}
	}

	public void
	addLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.add(l);
	}

	public void
	removeLifecycleListener(
		AzureusCoreLifecycleListener	l )
	{
		lifecycle_listeners.remove(l);
	}

	public void
	addListener(
		AzureusCoreListener	l )
	{
		listeners.add( l );
	}

	public void
	removeListener(
		AzureusCoreListener	l )
	{
		listeners.remove( l );
	}

	public void
	addOperationListener(
		AzureusCoreOperationListener	l )
	{
		operation_listeners.add(l);
	}

	public void
	removeOperationListener(
		AzureusCoreOperationListener	l )
	{
		operation_listeners.remove(l);
	}
}
