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

import java.util.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManagerFactory;

import com.aelitis.azureus.ui.IUIIntializer;
import com.aelitis.azureus.ui.UIFunctionsManager;

/**
 * The main SWT Thread, the only one that should run any GUI code.
 */
public class SWTThread {
  private static final int	FREQ_PER_SEC_LIMIT = 10;
  
  private static SWTThread instance;
  
  public static SWTThread getInstance() {
    return instance;
  }
  
  public static void createInstance(IUIIntializer initializer) throws SWTThreadAlreadyInstanciatedException {
//    if(instance != null) {
//      throw new SWTThreadAlreadyInstanciatedException();
//    }
    
    // set SWT specific config parameter defaults
    
    boolean bGTKTableBug_default = SWT.getPlatform().equals("gtk");
  
    COConfigurationManager.setBooleanDefault( "SWT_bGTKTableBug", bGTKTableBug_default );
    
    	//Will only return on termination
    
    new SWTThread(initializer);

  }
  
  Display display;
  private boolean sleak;
  private boolean terminated;
  private Thread runner;
  private final IUIIntializer initializer;
  
  private Map	freq_map = new HashMap();
  
  private 
  SWTThread(
  	final IUIIntializer app ) 
  { 
    this.initializer = app;
	instance = this;
    Display.setAppName(Constants.AZUREUS_NAME);
    
    try {
      display = Display.getCurrent();
	  if ( display == null ){
		  display = new Display();
	      sleak = false;
	  }else{
		  sleak = true;
	  }
    } catch(Exception e) { 
      display = new Display();
      sleak = false;
    } catch (UnsatisfiedLinkError ue) {
    	String sMin = "3.4";
			try {
				sMin = "" +  (((int)(SWT.getVersion() / 100)) / 10.0);
			} catch (Throwable t) {
			}
			try{
				String tempDir = System.getProperty ("swt.library.path");
				if (tempDir == null) {
					System.getProperty ("java.io.tmpdir");
				}
				Debug.out("Loading SWT Libraries failed. "
						+ "Typical causes:\n\n" 
						+ "(1) swt.jar is not for your os architecture (" 
						+ System.getProperty("os.arch") + ").  " 
						+ "You can get a new swt.jar (Min Version: " + sMin + ") " 
						+ "from http://eclipse.org/swt"
						+ "\n\n"
						+ "(2) No write access to '" + tempDir 
						+ "'. SWT will extract libraries contained in the swt.jar to this dir.\n", ue);
				if (!terminated) {
					app.stopIt(false, false);
					terminated = true;
				}
				PlatformManagerFactory.getPlatformManager().dispose();
			} catch (Throwable t) {
			}
			return;
		}
    
    Display.setAppName("OneSwarm");
    
    AEDiagnostics.addEvidenceGenerator(new AEDiagnosticsEvidenceGenerator() {
			public void generate(IndentWriter writer) {
				writer.println("SWT");

				try {
					writer.indent();

					writer.println("SWT Version:" + SWT.getVersion() + "/"
							+ SWT.getPlatform());
					
					writer.println("org.eclipse.swt.browser.XULRunnerPath: "
							+ System.getProperty("org.eclipse.swt.browser.XULRunnerPath", ""));
					writer.println("MOZILLA_FIVE_HOME: "
							+ SystemProperties.getEnvironmentalVariable("MOZILLA_FIVE_HOME"));
					
				} finally {

					writer.exdent();
				}
			}
		});
    
	if (Constants.isOSX) {
		
		// On Cocoa, we get a Close trigger on display.  Need to check if all
		// platforms send this.
		display.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				event.doit = UIFunctionsManager.getUIFunctions().dispose(false, false);
			}
		});

		String platform = SWT.getPlatform();
		// use reflection here so we decouple generic SWT from OSX specific stuff to an extent

		if (platform.equals("carbon")) {
			try {

				Class<?> ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CarbonUIEnhancer");

				Constructor<?> constructor = ehancerClass.getConstructor(new Class[] {});

				constructor.newInstance(new Object[] {});

			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}
		} else if (platform.equals("cocoa")) {
			try {

				Class<?> ehancerClass = Class.forName("org.gudy.azureus2.ui.swt.osx.CocoaUIEnhancer");

				Method mGetInstance = ehancerClass.getMethod("getInstance", new Class[0]);
				Object claObj = mGetInstance.invoke(null, new Object[0] );

				Method mHookAppMenu = claObj.getClass().getMethod("hookApplicationMenu", new Class[] {});
				if (mHookAppMenu != null) {
					mHookAppMenu.invoke(claObj, new Object[0]);
				}

				Method mHookDocOpen = claObj.getClass().getMethod("hookDocumentOpen", new Class[] {});
				if (mHookDocOpen != null) {
					mHookDocOpen.invoke(claObj, new Object[0]);
				}
				
			} catch (Throwable e) {

				Debug.printStackTrace(e);
			}
		}
	}   
    
    if (app != null) {
			runner = new Thread(new AERunnable() {
				public void runSupport() {
					app.run();
				}
			}, "Main Thread");
			runner.start();
		}
    
    
    if(!sleak) {
      while(!display.isDisposed() && !terminated) {
        try {
            if (!display.readAndDispatch()) { 
              display.sleep();
            }
        }
        catch (Throwable e) {
					if (Constants.isOSX && (e instanceof SWTException)
							&& e.getMessage().endsWith(" is disposed")
							&& (terminated || Debug.getStackTrace(e).indexOf("DropTarget") > 0)) {
						Logger.log(new LogEvent(LogIDs.GUI,
								"Weird non-critical display disposal in readAndDispatch"));
					} else {
						// Must use printStackTrace() (no params) in order to get 
						// "cause of"'s stack trace in SWT < 3119
						if (SWT.getVersion() < 3119)
							e.printStackTrace();
						Logger.log(new LogAlert(LogAlert.UNREPEATABLE,MessageText.getString("SWT.alert.erroringuithread"),e));
						
					}
				}
      }
      
     
      if(!terminated) {
        
        // if we've falled out of the loop without being explicitly terminated then
        // this appears to have been caused by a non-specific exit/restart request (as the
        // specific ones should terminate us before disposing of the window...)
        
        app.stopIt( false, false );
        terminated = true;
      }

      try {
      	if (!display.isDisposed())
      		display.dispose();
      } catch (Throwable t) {
      	// Must use printStackTrace() (no params) in order to get 
      	// "cause of"'s stack trace in SWT < 3119
      	if (SWT.getVersion() < 3119)
      		t.printStackTrace();
      	else
      		Debug.printStackTrace(t);
      }

       // dispose platform manager here
      PlatformManagerFactory.getPlatformManager().dispose();
    }
  }
  
  
  
  public void terminate() {
    terminated = true;
    // must dispose here in case another window has take over the
    // readAndDispatch/sleep loop
    if (!display.isDisposed()) {
    	display.dispose();
    }
  }
  
  public Display getDisplay() {
    return display;
  }
  
  public boolean isTerminated() {
  	return terminated;
  }

	public IUIIntializer getInitializer() {
		return initializer;
	}
	
	public void
	limitFrequencyAsyncExec(
		Object		owner,
		Display		display,
		AERunnable	target )
	{
		if ( display.isDisposed()){
			
			return;
		}
		
		int	now = (int)( SystemTime.getCurrentTime()/1000 );
		
		boolean	do_it	= true;
		
		synchronized( freq_map ){
			
			if ( freq_map.size() > 1024 ){
				
				Debug.out( "Frequency map is overloaded - check your logic!!!!" );
				
			}else{
				
				int[]	data = (int[])freq_map.get( owner );
			
				if ( data == null ){
					
					data = new int[]{ now, 0 };
					
					freq_map.put( owner, data );
				}
				
				if ( data[0] == now ){
					
					data[1]++;
					
					if ( data[1] > FREQ_PER_SEC_LIMIT ){
						
						do_it	= false;
						
						Debug.out( "SWT frequency limit exceeded for " + owner.getClass());
					}
				}else{
					
					data[0] = now;
					data[1] = 1;
				}
			}
		}
		
		if ( do_it ){
			
			display.asyncExec( target );
		}
	}
	
	public void removeLimitedFrequencyOwner(Object owner)
	{
		synchronized (freq_map)
		{
			freq_map.remove(owner);
		}		
	}
}
