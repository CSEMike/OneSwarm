/*
 * File    : ManagerUtils.java
 * Created : 7 d�c. 2003}
 * By      : Olivier
 *
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views.utils;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.tracker.host.TRHostException;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.PlatformManager;
import org.gudy.azureus2.platform.PlatformManagerCapabilities;
import org.gudy.azureus2.platform.PlatformManagerFactory;
import org.gudy.azureus2.ui.swt.Alerts;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;

import org.gudy.azureus2.plugins.platform.PlatformManagerException;

/**
 * @author Olivier
 *
 */
public class ManagerUtils {
  
  public static void run(DownloadManager dm) {
    if(dm != null) {
    	Utils.launch(dm.getSaveLocation().toString());
    }
  }
  
 /**
  * Opens the parent folder of dm's path
  * @param dm DownloadManager instance
  */
	public static void open(DownloadManager dm) {open(dm, false);}
	
	public static void open(DownloadManager dm, boolean open_containing_folder_mode) {
		if (dm != null) {
			if (open_containing_folder_mode) {
				Utils.launch(dm.getSaveLocation().getParent());
			}
			else {
				open(dm.getSaveLocation());
			}
		}
	}
	
	public static void open(File f) {
		while (f != null && !f.exists())
			f = f.getParentFile();

		if (f == null)
			return;

		PlatformManager mgr = PlatformManagerFactory.getPlatformManager();

		if (mgr.hasCapability(PlatformManagerCapabilities.ShowFileInBrowser)) {
			try {
				PlatformManagerFactory.getPlatformManager().showFile(f.toString());
				return;
			} catch (PlatformManagerException e) {
				Debug.printStackTrace(e);
			}
		}

		if (f.isDirectory()) {
			Utils.launch(f.toString()); // default launcher
		} else {
			Utils.launch(f.getParent().toString());
		}
	}
  
  public static boolean isStartable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (state != DownloadManager.STATE_STOPPED) {
      return false;
    }
    return true;
  }
  
  public static boolean isStopable(DownloadManager dm) {
    if(dm == null)
      return false;
    int state = dm.getState();
    if (	state == DownloadManager.STATE_STOPPED ||
    		state == DownloadManager.STATE_STOPPING	) {
      return false;
    }
    return true;
  }
  
  public static boolean isStopped(DownloadManager dm) {
	    if(dm == null)
	      return false;
	    int state = dm.getState();
	    if (	state == DownloadManager.STATE_STOPPED ||
	    		state == DownloadManager.STATE_ERROR	) {
	      return true;
	    }
	    return false;
	  }
  
  public static boolean
  isForceStartable(
  	DownloadManager	dm )
  {
    if(dm == null){
        return false;
  	}
    
    int state = dm.getState();
    
    if (	state != DownloadManager.STATE_STOPPED && state != DownloadManager.STATE_QUEUED &&
            state != DownloadManager.STATE_SEEDING && state != DownloadManager.STATE_DOWNLOADING){

    	return( false );
    }
    
    return( true );
  }
  
  public static void 
  host(
  	AzureusCore		azureus_core,
	DownloadManager dm,
	Composite 		panel) 
  {
    if(dm == null)
      return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().hostTorrent(torrent, true, false );
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  public static void 
  publish(
  		AzureusCore		azureus_core,
		DownloadManager dm,
		Composite		 panel) 
  {
    if(dm == null)
     return;
    TOTorrent torrent = dm.getTorrent();
    if (torrent != null) {
      try {
      	azureus_core.getTrackerHost().publishTorrent(torrent);
      } catch (TRHostException e) {
        MessageBox mb = new MessageBox(panel.getShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText(MessageText.getString("MyTorrentsView.menu.host.error.title"));
        mb.setMessage(MessageText.getString("MyTorrentsView.menu.host.error.message").concat("\n").concat(e.toString()));
        mb.open();
      }
    }
  }
  
  
  public static void 
  start(
  		DownloadManager dm) 
  {
    if (dm != null && dm.getState() == DownloadManager.STATE_STOPPED) {
    	
      dm.setStateWaiting();
    }
  }

  public static void 
  queue(
  		DownloadManager dm,
		Composite panel) 
  {
    if (dm != null) {
    	if (dm.getState() == DownloadManager.STATE_STOPPED){
    		
    		dm.setStateQueued();
    		
    		/* parg - removed this - why would we want to effectively stop + restart
    		 * torrents that are running? This is what happens if the code is left in.
    		 * e.g. select two torrents, one stopped and one downloading, then hit "queue"
    		 
    		 }else if (	dm.getState() == DownloadManager.STATE_DOWNLOADING || 
    				dm.getState() == DownloadManager.STATE_SEEDING) {
    		
    			stop(dm,panel,DownloadManager.STATE_QUEUED);
    		*/
      }
    }
  }
  
  public static void stop(DownloadManager dm, Shell shell) {
  	stop(dm, shell, DownloadManager.STATE_STOPPED);
  }
  
  public static void 
  stop(
  		final DownloadManager dm,
		Shell shell,
		int stateAfterStopped ) 
  {
		if (dm == null) {
			return;
		}

		int state = dm.getState();

		if (state == DownloadManager.STATE_STOPPED
				|| state == DownloadManager.STATE_STOPPING
				|| state == stateAfterStopped) {
			return;
		}

		boolean stopme = true;
		if (state == DownloadManager.STATE_SEEDING) {

			if (dm.getStats().getShareRatio() >= 0
					&& dm.getStats().getShareRatio() < 1000
					&& COConfigurationManager.getBooleanParameter("Alert on close", false)) {
				if (shell == null) {
					shell = Utils.findAnyShell();
				}
				MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING
						| SWT.YES | SWT.NO);
				mb.setText(MessageText.getString("seedmore.title"));
				mb.setMessage(MessageText.getString("seedmore.shareratio")
						+ (dm.getStats().getShareRatio() / 10) + "%.\n"
						+ MessageText.getString("seedmore.uploadmore"));
				int action = mb.open();
				stopme = action == SWT.YES;
			} else if (dm.getDownloadState().isOurContent()
					&& dm.getStats().getAvailability() < 2) {
				TRTrackerScraperResponse scrape = dm.getTrackerScrapeResponse();
				int numSeeds = scrape.getSeeds();
	      long seedingStartedOn = dm.getStats().getTimeStartedSeeding();
	      if ((numSeeds > 0) &&
	          (seedingStartedOn > 0) &&
	          (scrape.getScrapeStartTime() > seedingStartedOn))
	        numSeeds--;
	      
	      if (numSeeds == 0) {
	  			stopme = Utils.execSWTThreadWithBool("stopSeeding",
	  					new AERunnableBoolean() {
	  						public boolean runSupport() {
	  							String title = MessageText.getString("Content.alert.notuploaded.title");
	  							String text = MessageText.getString("Content.alert.notuploaded.text",
	  									new String[] {
	  										dm.getDisplayName(),
	  										MessageText.getString("Content.alert.notuploaded.stop")
	  									});

	  							MessageBoxShell mb = new MessageBoxShell(Utils.findAnyShell(),
	  									title, text, new String[] {
	  										MessageText.getString("Content.alert.notuploaded.button.stop"),
	  										MessageText.getString("Content.alert.notuploaded.button.continue")
	  									}, 1, null, null, false, 0);
	  							mb.setRelatedObject(dm);

	  							return mb.open() == 0;
	  						}
	  					});
	      }
			}
		}
		
		if (stopme) {
			asyncStop(dm, stateAfterStopped);
		}
	}

  public static void remove(final DownloadManager dm, Shell shell,
			final boolean bDeleteTorrent, final boolean bDeleteData) {
  	remove(dm, shell, bDeleteTorrent, bDeleteData, null);
	}
  
  public static void remove(final DownloadManager dm, Shell shell,
			final boolean bDeleteTorrent, final boolean bDeleteData,
			final AERunnable deleteFailed) {

		if (!dm.getDownloadState().getFlag(DownloadManagerState.FLAG_LOW_NOISE)) {
			if (COConfigurationManager.getBooleanParameter("confirm_torrent_removal")) {

				String title = MessageText.getString("deletedata.title");
				String text = MessageText.getString("deletetorrent.message1")
						+ dm.getDisplayName() + " :\n" + dm.getTorrentFileName()
						+ MessageText.getString("deletetorrent.message2");

				MessageBoxShell mb = new MessageBoxShell(shell, title, text,
						new String[] {
							MessageText.getString("Button.yes"),
							MessageText.getString("Button.no"),
						}, 1);
				mb.setRelatedObject(dm);
				mb.setLeftImage(SWT.ICON_WARNING);

				int result = mb.open();
				if (result != 0) {
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
					return;
				}
			}

			boolean confirmDataDelete = COConfigurationManager.getBooleanParameter(
					"Confirm Data Delete");

			if (confirmDataDelete && bDeleteData) {
				String path = dm.getSaveLocation().toString();

				String title = MessageText.getString("deletedata.title");
				String text = MessageText.getString("deletedata.message1")
						+ dm.getDisplayName() + " :\n" + path
						+ MessageText.getString("deletedata.message2");

				MessageBoxShell mb = new MessageBoxShell(shell, title, text,
						new String[] {
							MessageText.getString("Button.yes"),
							MessageText.getString("Button.no"),
						}, 1);
				mb.setRelatedObject(dm);
				mb.setLeftImage(SWT.ICON_WARNING);

				int result = mb.open();
				if (result != 0) {
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
					return;
				}
			}
		}

		asyncStopDelete(dm, DownloadManager.STATE_STOPPED, bDeleteTorrent,
				bDeleteData, deleteFailed);
	}
  
  public static void asyncStopDelete(final DownloadManager dm,
			final int stateAfterStopped, final boolean bDeleteTorrent,
			final boolean bDeleteData, final AERunnable deleteFailed) {

		new AEThread("asyncStop", true) {
			public void runSupport() {

				try {
					dm.getGlobalManager().removeDownloadManager(dm, bDeleteTorrent,
							bDeleteData);
				} catch (GlobalManagerDownloadRemovalVetoException f) {
					if (!f.isSilent()) {
						Alerts.showErrorMessageBoxUsingResourceString(new Object[] {
							dm
						}, "globalmanager.download.remove.veto", f);
					}
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
				} catch (Exception ex) {
					Debug.printStackTrace(ex);
					if (deleteFailed != null) {
						deleteFailed.runSupport();
					}
				}
			}
		}.start();
	}
  
  	public static void
	asyncStop(
		final DownloadManager	dm,
		final int 				stateAfterStopped )
  	{
    	new AEThread( "asyncStop", true )
		{
    		public void
			runSupport()
    		{
    			dm.stopIt( stateAfterStopped, false, false );
    		}
		}.start();
  	}
  	
  	public static void
	asyncStartAll()
  	{
     	new AEThread( "asyncStartAll", true )
		{
    		public void
			runSupport()
    		{
    			AzureusCoreFactory.getSingleton().getGlobalManager().startAllDownloads();
    		}
		}.start();
  	}
  	
  	public static void
	asyncStopAll()
  	{
		new AEThread( "asyncStopAll", true )
		{
			public void
			runSupport()
			{
       			AzureusCoreFactory.getSingleton().getGlobalManager().stopAllDownloads();
			}
			
		}.start();
  	}
  	
  	public static void
	asyncPause()
  	{
     	new AEThread( "asyncPause", true )
		{
    		public void
			runSupport()
    		{
    			AzureusCoreFactory.getSingleton().getGlobalManager().pauseDownloads();
    		}
		}.start();
  	}
  	
  	public static void
  	asyncResume() {
     	new AEThread( "asyncResume", true )
		{
    		public void
			runSupport()
    		{
    			AzureusCoreFactory.getSingleton().getGlobalManager().resumeDownloads();
    		}
		}.start();
  	}
}
