/*
 * Created on Apr 30, 2004
 * Created by Olivier Chalouhi
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
package org.gudy.azureus2.ui.swt.mainwindow;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.ipfilter.IpFilterManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.common.util.UserAlerts;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.auth.AuthenticatorWindow;
import org.gudy.azureus2.ui.swt.auth.CertificateTrustWindow;
import org.gudy.azureus2.ui.swt.networks.SWTNetworkSelection;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.progress.ProgressWindow;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.updater2.PreUpdateChecker;
import org.gudy.azureus2.ui.swt.updater2.SWTUpdateChecker;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.util.CopyOnWriteList;
import com.aelitis.azureus.launcher.Launcher;
import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.InitializerListener;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;


/**
 * this class initiatize all GUI and Core components which are :
 * 1. The SWT Thread
 * 2. Images
 * 3. The Splash Screen if needed
 * 4. The GlobalManager
 * 5. The Main Window in correct state or the Password window if needed.
 */
public class 
Initializer 
	implements AzureusCoreListener, IUIIntializer
{
	private static final LogIDs LOGID = LogIDs.GUI;
  private AzureusCore		azureus_core;
  private GlobalManager 	gm;
  private StartServer 		startServer;
  
  private CopyOnWriteList listeners = new CopyOnWriteList();
  
  private AEMonitor	listeners_mon	= new AEMonitor( "Initializer:l" );

  private String[] args;
  
  private AESemaphore semFilterLoader = new AESemaphore("filter loader");
  
  public 
  Initializer(
  		final AzureusCore		_azureus_core,
  		StartServer 			_server,
		String[] 				_args ) 
  {   
    azureus_core	= _azureus_core;
    startServer 	= _server;
    args 			= _args;
    
    Thread filterLoaderThread = new AEThread("filter loader", true) {
			public void runSupport() {
				try {
					azureus_core.getIpFilterManager().getIPFilter();
				} finally {
					semFilterLoader.releaseForever();
				}
			}
		};
		filterLoaderThread.setPriority(Thread.MIN_PRIORITY);
		filterLoaderThread.start();

    try {
      SWTThread.createInstance(this);
    } catch(SWTThreadAlreadyInstanciatedException e) {
    	Debug.printStackTrace( e );
    }
  }  

  public static boolean
  handleStopRestart(
  	final boolean	restart )
  {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT != null) {
			return functionsSWT.dispose(restart, true);
		}

		return false;
	}
	

  public void 
  run() 
  {
  	try{
  		String uiMode = UISwitcherUtil.openSwitcherWindow(false);
			if (uiMode.equals("az3")) {
				try {
					final Class az3Class = Class.forName("com.aelitis.azureus.ui.swt.Initializer");

					final Constructor constructor = az3Class.getConstructor(new Class[] {
						AzureusCore.class,
						Boolean.TYPE,
						String[].class
					});

					IUIIntializer initializer = (IUIIntializer) constructor.newInstance(new Object[] {
						azureus_core,
						new Boolean(false),
						args
					});

					initializer.run();
					return;
				} catch (Throwable t) {
					// use print stack trace because we don't want to introduce logger
					t.printStackTrace();
					// Ignore and use AZ2
				}
			}
			// else AZ2UI
  		
  		
  		// initialise the SWT locale util
	  	
	    new LocaleUtilSWT( azureus_core );

			Display display = SWTThread.getInstance().getDisplay();

			UIConfigDefaultsSWT.initialize();
			
	    //The splash window, if displayed will need some images. 
	    ImageRepository.loadImagesForSplashWindow(display);
	    
	    //Splash Window is not always shown
	    if (COConfigurationManager.getBooleanParameter("Show Splash", true) && System.getProperty("nolaunch_startup") == null ) {
	      SplashWindow.create(display,this);
	    }
	    	    
	    setNbTasks(6);
	    
	    nextTask(); 
	    reportCurrentTaskByKey("splash.firstMessageNoI18N");
	    
	    Alerts.init();
	    
	    final ArrayList logEvents = new ArrayList();
	    ILogEventListener logListener = null;
	    if (COConfigurationManager.getBooleanParameter("Open Console", false)) {
	    	logListener = new ILogEventListener() {
					public void log(LogEvent event) {
						logEvents.add(event);
					}
	    	};
	    	Logger.addListener(logListener);
	    }
	    final ILogEventListener finalLogListener = logListener;

	    ProgressWindow.register( azureus_core );
	    
	    new SWTNetworkSelection();
	    
	    new AuthenticatorWindow();
	    
	    new CertificateTrustWindow();

	    InstallPluginWizard.register( azureus_core, display );
	    
	    nextTask(); 	    
	    reportCurrentTaskByKey("splash.loadingImages");
	    
	    ImageRepository.loadImages(display);
	    
	    nextTask();
	    reportCurrentTaskByKey("splash.initializeGM");
	    
	    azureus_core.addListener( this );
	    
	    
	    AzureusCoreLifecycleListener starter = new AzureusCoreLifecycleAdapter() {
			public void componentCreated(AzureusCore core, AzureusCoreComponent comp) {
				if (comp instanceof GlobalManager) {

					gm = (GlobalManager) comp;

					nextTask();
					reportCurrentTask(MessageText.getString("splash.initializePlugins"));
				}
			}

			public void started(AzureusCore core) {
				if (gm == null)
					return;

				new UserAlerts(gm);

				nextTask();
				IpFilterManager ipFilterManager = azureus_core.getIpFilterManager();
				if (ipFilterManager != null) {
					String s = MessageText.getString("splash.loadIpFilters");
					do {
						reportCurrentTask(s);
						s += ".";
					} while (!semFilterLoader.reserve(3000));
				}
				
				nextTask();
				reportCurrentTaskByKey("splash.initializeGui");

				new Colors();

				Cursors.init();

				MainWindow mw = new MainWindow(core, Initializer.this,
						logEvents);
				
				mw.setVisible(false, false);

				if (finalLogListener != null)
					Logger.removeListener(finalLogListener);

				nextTask();

				reportCurrentTaskByKey("splash.openViews");

				SWTUpdateChecker.initialize();

				PreUpdateChecker.initialize( core, COConfigurationManager.getStringParameter("ui"));

				UpdateMonitor.getSingleton(core); // setup the update monitor

				//Tell listeners that all is initialized :
				Alerts.initComplete();

				// 7th task: just for triggering a > 100% completion
				nextTask();

				//Finally, open torrents if any.
				for (int i = 0; i < args.length; i++) {

					try {
						TorrentOpener.openTorrent(args[i]);

					} catch (Throwable e) {

						Debug.printStackTrace(e);
					}
				}
				
				if( COConfigurationManager.getBooleanParameter("Open URL on startup") )
				{
					if( COConfigurationManager.getBooleanParameter("OneSwarm restarting") )
					{
						COConfigurationManager.setParameter("OneSwarm restarting", false);
					}
					else
					{
						// taken from OneSwarmConstants.java since we can't use that directly here
						// keep this in sync with UIFunctionsImpl.java bringToFront()
						if( System.getProperty("nolaunch_startup") == null ) {
							Utils.launch("http://127.0.0.1:" + "29615/");
						}
					}
				}
			}

			public void stopping(AzureusCore core) {
				Alerts.stopInitiated();
			}

			public void stopped(AzureusCore core) {
			}

			public boolean syncInvokeRequired() {
				return (true);
			}

			public boolean stopRequested(AzureusCore _core)

			throws AzureusCoreException {
				return (handleStopRestart(false));
			}

			public boolean restartRequested(final AzureusCore core) {
				return (handleStopRestart(true));
			}
		};
		azureus_core.addLifecycleListener(starter);
	    
		System.out.println("starting core");
	    if( azureus_core.isStarted() == false )
	    {
	    	azureus_core.start();
	    }
	    else
	    {
	    	starter.componentCreated(azureus_core, azureus_core.getGlobalManager());
	    	starter.started(azureus_core);
	    }

  	}catch( Throwable e ){
  		Logger.log(new LogEvent(LOGID, "Initialization fails:", e));
  	} 
  }
  
  public void addListener(InitializerListener listener){
    try{
    	listeners_mon.enter();
    	
    	listeners.add(listener);
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void removeListener(InitializerListener listener) {
    try{
    	listeners_mon.enter();
		
    	listeners.remove(listener);
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void 
  reportCurrentTask(
	AzureusCoreOperation	operation,
	String 					currentTask )
  {
	  if ( operation.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){
		  reportCurrentTask( currentTask );
	  }
  }
	  
  public void 
  reportPercent(
	AzureusCoreOperation	operation,
	int 					percent )
  {
	  if ( operation.getOperationType() == AzureusCoreOperation.OP_INITIALISATION ){
		  reportPercent( percent );
	  }
  }
  
  public void reportCurrentTask(String currentTaskString) {
     try{
     	listeners_mon.enter();
     
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	    	try{
	    		InitializerListener listener = (InitializerListener) iter.next();
	      
	    		listener.reportCurrentTask(currentTaskString);
	    		
	    	}catch( Throwable e ){
	    		
	    		Debug.printStackTrace( e );
	    	}
	    }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  // AzureusCoreListener
  public void reportPercent(int percent) {
  	int overallPercent = overallPercent(percent);
    try{
    	listeners_mon.enter();
    
	    Iterator iter = listeners.iterator();
	    while(iter.hasNext()) {
	    	try{
	    		InitializerListener listener = (InitializerListener) iter.next();
	      
	    		listener.reportPercent(overallPercent);
	    		
	    	}catch( Throwable e ){
	    		
	    		Debug.printStackTrace( e );
	    	}
	    }

	    if (overallPercent > 100) {
	    	listeners.clear();
	    }
    }finally{
    	
    	listeners_mon.exit();
    }
  }
  
  public void 
  stopIt(
  	boolean	for_restart,
	boolean	close_already_in_progress ) 
  
  	throws AzureusCoreException
  {
    if ( azureus_core != null && !close_already_in_progress ){

    	if ( for_restart ){

    		azureus_core.checkRestartSupported();
    	}
    }
    
  	try{
	    Cursors.dispose();
	    
	    SWTThread.getInstance().terminate();  
	    
	}finally{
	  	
		try{
		    if ( azureus_core != null && !close_already_in_progress ){
	
		    	try{
			    	if ( for_restart ){
			    			
			    		azureus_core.restart();
			    			
			    	}else{
			    			
			    		azureus_core.stop();
			    	}
		    	}catch( Throwable e ){
		    		
		    			// don't let any failure here cause the stop operation to fail
		    		
		    		Debug.out( e );
		    	}
		    }
		}finally{
			
				// do this after closing core to minimise window when the we aren't 
				// listening and therefore another Azureus start can potentially get
				// in and screw things up
			
		    if ( startServer != null ){
			    
		    	startServer.stopIt();
		    }
		}
	}
  }
  
  private int nbTasks = 1;
  private int currentTask = 0;
  private int currentPercent = 0;
  private int lastTaskPercent = 0;
  
  private void setNbTasks(int _nbTasks) {
    currentTask = 0;
    nbTasks = _nbTasks;
  }
  
  private void nextTask() {
    currentTask++;
    currentPercent = 100 * currentTask / (nbTasks) ;
    //0% done of current task
    reportPercent(0);
  }
  
  private int overallPercent(int taskPercent) {
  	lastTaskPercent = taskPercent;
    //System.out.println("ST percent " + currentPercent + " / " + taskPercent + " : " + (currentPercent + (taskPercent / nbTasks)));
    return currentPercent + taskPercent / nbTasks;
  }
  
  private void reportCurrentTaskByKey(String key) {
    reportCurrentTask(MessageText.getString(key));
  }
 
  // @see com.aelitis.azureus.ui.IUIIntializer#increaseProgresss()
  public void increaseProgresss() {
  	if (lastTaskPercent < 100) {
  		reportPercent(lastTaskPercent + 1);
  	}
  }
  
	public void abortProgress() {
		currentTask = nbTasks;
		reportPercent(101);
	}
  
  public static void main(String args[]) 
  {
	  if(Launcher.checkAndLaunch(Initializer.class, args))
		  return;
	  
  	System.err.println("Shouldn't you be starting with org.gudy.azureus2.ui.swt.Main?");
 	AzureusCore		core = AzureusCoreFactory.create();

 	new Initializer( core, null,args);
  }
}
