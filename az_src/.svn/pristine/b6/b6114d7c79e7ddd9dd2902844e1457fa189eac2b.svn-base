/*
 * Created on 28.11.2003
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
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
package org.gudy.azureus2.ui.common.util;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.disk.DiskManagerListener;
import org.gudy.azureus2.core3.disk.DiskManagerPiece;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerDiskListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerAdapter;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;

import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;

import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import java.applet.Applet;
import java.applet.AudioClip;
import java.io.File;
import java.net.URL;

/**
 * Contains methods to alert the user of certain events.
 * @author Rene Leonhardt
 */

public class 
UserAlerts 
{
  	private AudioClip 	audio_clip 		= null;
  	private String		audio_resource	= "";
  	
    private AEMonitor	this_mon 	= new AEMonitor( "UserAlerts" );
    
    private boolean startup = true;
    
	public 
	UserAlerts(
		GlobalManager	global_manager ) 
 	{	
		final DownloadManagerAdapter download_manager_listener = 
			new DownloadManagerAdapter()
			{
			public void downloadComplete(DownloadManager manager) {
				if (!manager.getDownloadState().getFlag(
						DownloadManagerState.FLAG_LOW_NOISE)) {

					activityFinished(true, manager.getDisplayName(), manager);
				}
			}

			// @see org.gudy.azureus2.core3.download.impl.DownloadManagerAdapter#stateChanged(org.gudy.azureus2.core3.download.DownloadManager, int)
			public void stateChanged(final DownloadManager manager, int state) {

				boolean lowNoise = manager.getDownloadState().getFlag(
						DownloadManagerState.FLAG_LOW_NOISE);
				if (lowNoise) {
					return;
				}

				// if state == STARTED, then open the details window (according to config)
				if (state == DownloadManager.STATE_DOWNLOADING
						|| state == DownloadManager.STATE_SEEDING) {
					Utils.execSWTThread(new AERunnable() {
						public void runSupport() {
							boolean complete = manager.isDownloadComplete(false);

							if ((!complete && COConfigurationManager.getBooleanParameter("Open Details"))
									|| (complete && COConfigurationManager.getBooleanParameter("Open Seeding Details"))) {
								UIFunctionsManagerSWT.getUIFunctionsSWT().openView(
										UIFunctions.VIEW_DM_DETAILS, manager);
							}

							if (((!complete) && COConfigurationManager.getBooleanParameter("Open Bar Incomplete"))
									|| (complete && COConfigurationManager.getBooleanParameter("Open Bar Complete"))) {

								DownloadBar.open(manager, Utils.findAnyShell());
							}
						}
					});
				}
			}
		}; 
		
		final DiskManagerListener	disk_listener = 
			new DiskManagerListener()
			{
				public void
				stateChanged(
					int oldState, 
					int	newState )
				{	
				}
				
				public void
				filePriorityChanged(
					DiskManagerFileInfo		file )
				{
				}
	
				public void
				pieceDoneChanged(
					DiskManagerPiece		piece )
				{
				}
				
				public void
				fileAccessModeChanged(
					DiskManagerFileInfo		file,
					int						old_mode,
					int						new_mode )
				{
					DownloadManager dm = file.getDownloadManager();
				
					if ( 	old_mode == DiskManagerFileInfo.WRITE &&
							new_mode == DiskManagerFileInfo.READ &&
							file.getDownloaded() == file.getLength()){
						
						if( dm == null || !dm.getDownloadState().getFlag( DownloadManagerState.FLAG_LOW_NOISE )){

							activityFinished(false, file.getFile(true).getName(), file.getDiskManager());
						}
					}
				
					/*
					System.out.println( 
						"amc:" + 
						file.getDownloadManager().getDisplayName() + "/" + 
						file.getName() + ":" + old_mode + " -> " + new_mode );
					*/
				}
			};
			
		final DownloadManagerDiskListener dm_disk_listener = 
			new DownloadManagerDiskListener()
			{
				public void
				diskManagerAdded(
					DiskManager	dm )
				{
					dm.addListener( disk_listener );
				}
				
				public void
				diskManagerRemoved(
					DiskManager	dm )
				{
					dm.removeListener( disk_listener );
				}

			};

    	global_manager.addListener(
    		new GlobalManagerAdapter()
    		{
				public void 
				downloadManagerAdded(DownloadManager manager) 
				{
				// don't pop up for non-persistent as these get added late in the day every time
				// so we'll notify for each download every startup

				if (!startup && manager.isPersistent()) {

					boolean bPopup = COConfigurationManager.getBooleanParameter("Popup Download Added");

					if (bPopup) {

							if( !manager.getDownloadState().getFlag( DownloadManagerState.FLAG_LOW_NOISE )){

							String popup_text = MessageText.getString("popup.download.added",
										new String[] { manager.getDisplayName()
									});
							UIFunctionsManager.getUIFunctions().forceNotify(
									UIFunctions.STATUSICON_NONE, null, popup_text, null,
									new Object[] {
										manager
									}, -1);
						}
					}
				}

				manager.addListener(download_manager_listener);

				manager.addDiskListener(dm_disk_listener);
			}

				public void 
				downloadManagerRemoved(DownloadManager manager) 
				{
					manager.removeListener(download_manager_listener);
					
					manager.removeDiskListener( dm_disk_listener );
				}  
				
				public void
				destroyed()
				{
					tidyUp();
				} 		
			}); 
    	startup = false;
     }

  	protected void activityFinished(boolean	download, String item_name, Object relatedObject)
  	{
  		final String sound_enabler;
  		final String sound_file;
  		final String default_sound 	= "org/gudy/azureus2/ui/icons/downloadFinished.wav";
  		
  		final String speech_enabler;
  		final String speech_text;
  		
  		final String popup_enabler;
  		final String popup_def_text;
  		
  		if ( download ){
	 		sound_enabler 	= "Play Download Finished";
	  		sound_file		= "Play Download Finished File";
	  		
	  		speech_enabler 	= "Play Download Finished Announcement";
	  		speech_text		= "Play Download Finished Announcement Text";
	  		
	  		popup_enabler   = "Popup Download Finished";
	  		popup_def_text  = "popup.download.finished";
	  		
  		}else{
	 		sound_enabler 	= "Play File Finished";
	  		sound_file		= "Play File Finished File";
	  		
	  		speech_enabler 	= "Play File Finished Announcement";
	  		speech_text		= "Play File Finished Announcement Text";
	  		
	  		popup_enabler   = "Popup File Finished";
	  		popup_def_text  = "popup.file.finished";
  		}
  		
  		try{
  			this_mon.enter();
  			
  			if (COConfigurationManager.getBooleanParameter(popup_enabler)) {
  				String popup_text = MessageText.getString(popup_def_text, new String[]{item_name});
					UIFunctionsManager.getUIFunctions().forceNotify(
							UIFunctions.STATUSICON_NONE, null, popup_text, null,
							new Object[] {
								relatedObject
							}, -1);
  			}

            if(Constants.isOSX) { // OS X cannot concurrently use SWT and AWT
                new AEThread("DownloadSound") {
                    public void runSupport()
                    {
                        try {
                            if(COConfigurationManager.getBooleanParameter( speech_enabler ))
                                Runtime.getRuntime().exec(new String[]{"say", COConfigurationManager.getStringParameter( speech_text )}); // Speech Synthesis services

                            if(COConfigurationManager.getBooleanParameter( sound_enabler ))
                                Runtime.getRuntime().exec(new String[]{"osascript", "-e" ,"beep"}); // Beep alert type is in accordance with System Preferences

                            Thread.sleep(2500);
                        }
                        catch(Throwable e) {}
                    }
                }.start();
            }
	    	else if( COConfigurationManager.getBooleanParameter( sound_enabler, false)){

	    		String	file = COConfigurationManager.getStringParameter( sound_file );

    			file = file.trim();

	    			// turn "<default>" into blank

	    		if ( file.startsWith( "<" )){

	    			file	= "";
	    		}

	    		if ( audio_clip == null || !file.equals( audio_resource )){

	    			audio_clip	= null;

	    				// try explicit file

	    			if ( file.length() != 0 ){

	    				File	f = new File( file );

	    				try{

			    			if ( f.exists()){

		    					URL	file_url = f.toURL();

		    					audio_clip = Applet.newAudioClip( file_url );
			    			}

	    				}catch( Throwable  e ){

	    					Debug.printStackTrace(e);

	    				}finally{

	    					if ( audio_clip == null ){
	    						Logger.log(new LogAlert(relatedObject, LogAlert.UNREPEATABLE,
										LogAlert.AT_ERROR, "Failed to load audio file '" + file
												+ "'"));
	    					}
	    				}
	    			}

	    				// either non-explicit or explicit missing

	    			if ( audio_clip == null ){

	    				audio_clip = Applet.newAudioClip(UserAlerts.class.getClassLoader().getResource( default_sound ));

	    			}

	    			audio_resource	= file;
	    		}

	    		if ( audio_clip != null ){

	            	new AEThread("DownloadSound")
					{
		        		public void
						runSupport()
		        		{
		        			try{
		        				audio_clip.play();

		        				Thread.sleep(2500);

		        			}catch( Throwable e ){

		        			}
		        		}
		        	}.start();
		        }
	    	}
  		}catch( Throwable e ){
  			
  			Debug.printStackTrace( e );
  			
  		}finally{
  			
  			this_mon.exit();
  		}
  	}
  	
  	protected void
  	tidyUp()
  	{
		/*
		The Java audio system keeps some threads running even after playback is finished.
		One of them, named "Java Sound event dispatcher", is *not* a daemon
		thread and keeps the VM alive.
		We have to locate and interrupt it explicitely.
		*/
		
		try{
		
			ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
			
			Thread[] threadList = new Thread[threadGroup.activeCount()];
			
			threadGroup.enumerate(threadList);
			
			for (int i = 0;	i < threadList.length;	i++){
			
				if(threadList[i] != null && "Java Sound event dispatcher".equals(threadList[i].getName())){
									
					threadList[i].interrupt();
				}
			}
		}catch( Throwable e ){
			
			Debug.printStackTrace( e );
		}
  	}
  	
  	
  	/**
  	 * Grab the user's attention in a platform dependent way
  	 * @param type one of <code>PlatformManager.USER_REQUEST_INFO</code>, 
  	 * 										<code>PlatformManager.USER_REQUEST_WARNING</code>, OR 
  	 * 										<code>PlatformManager.USER_REQUEST_QUESTION</code>
  	 * @param data user-defined data object;
  	 * 				see the platform-specific <code>PlatformManager</code> for what may be supported
  	 */
  	public static void requestUserAttention(int type, Object data) {

  		PlatformManager pm = PlatformManagerFactory.getPlatformManager();
  		if (true == pm.hasCapability(PlatformManagerCapabilities.RequestUserAttention)) {
  			try {
  				pm.requestUserAttention(type, data);
  			} catch (PlatformManagerException e) {
  				Debug.printStackTrace(e);
  			}
  		}
  	}
}