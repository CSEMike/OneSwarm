/*
 * Created on 3 mai 2004
 * Created by Olivier Chalouhi
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
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.OpenTorrentWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.shells.CoreWaiterSWT;

import com.aelitis.azureus.core.*;
import com.aelitis.azureus.core.vuzefile.VuzeFile;
import com.aelitis.azureus.core.vuzefile.VuzeFileComponent;
import com.aelitis.azureus.core.vuzefile.VuzeFileHandler;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

/**
 * @author Olivier Chalouhi
 * @author TuxPaper (openTorrentWindow)
 */
public class TorrentOpener {
	/**
	 * Open a torrent.  Possibly display a window if the user config says so
	 * 
	 * @param torrentFile Torrent to open (file, url, etc)
	 */
	public static void openTorrent(String torrentFile) {
		openTorrentWindow(null, new String[] { torrentFile }, false);
	}
	
	public static void openTorrents(String[] torrentFiles) {
		openTorrentWindow(null, torrentFiles, false);
	}
  
	/**
	 * Open the torrent window
	 *
	 */
  public static void openTorrentWindow() {
  	openTorrentWindow(null, null, false);
  }

  protected static void 
  openTorrentsForTracking(
    final String path, 
    final String fileNames[] )
  {
  	CoreWaiterSWT.waitForCoreRunning(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				final Display display = SWTThread.getInstance().getDisplay();
		  	if (display == null || display.isDisposed() || core == null)
		  		return;
		  	
				new AEThread("TorrentOpener") {
					public void runSupport() {

						for (int i = 0; i < fileNames.length; i++) {

							try {
								TOTorrent t = TorrentUtils.readFromFile(new File(path,
										fileNames[i]), true);

								core.getTrackerHost().hostTorrent(t, true, true);

							} catch (Throwable e) {
								Logger.log(new LogAlert(LogAlert.UNREPEATABLE,
										"Torrent open fails for '" + path + File.separator
												+ fileNames[i] + "'", e));
							}
						}
					}
				}.start();
			}
		});
  }
  
  public static void 
  openTorrentTrackingOnly() 
  {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final Shell shell = Utils.findAnyShell();
		  	if (shell == null)
		  		return;

				FileDialog fDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
				fDialog.setFilterPath(getFilterPathTorrent());
				fDialog
						.setFilterExtensions(new String[] { "*.torrent", "*.tor", Constants.FILE_WILDCARD });
				fDialog.setFilterNames(new String[] { "*.torrent", "*.tor", Constants.FILE_WILDCARD });
				fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
				String path = setFilterPathTorrent(fDialog.open());
				if (path == null)
					return;

				TorrentOpener.openTorrentsForTracking(path, fDialog.getFileNames());
			}
		});
  }

  public static void openTorrentSimple() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final Shell shell = Utils.findAnyShell();
				if (shell == null)
					return;

				FileDialog fDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
				fDialog.setFilterPath(getFilterPathTorrent());
				fDialog.setFilterExtensions(new String[] {
						"*.torrent",
						"*.tor",
						Constants.FILE_WILDCARD });
				fDialog.setFilterNames(new String[] {
						"*.torrent",
						"*.tor",
						Constants.FILE_WILDCARD });
				fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
				String path = setFilterPathTorrent(fDialog.open());
				if (path == null)
					return;

				openTorrentWindow(path, fDialog.getFileNames(), false);
			}
		});
	}

  public static void openDroppedTorrents(DropTargetEvent event, boolean deprecated_sharing_param ){
		if (event.data == null)
			return;

		final boolean bOverrideToStopped = event.detail == DND.DROP_COPY;

		if (event.data instanceof String[] || event.data instanceof String) {
			final String[] sourceNames = (event.data instanceof String[])
					? (String[]) event.data : new String[] { (String) event.data };
			if (sourceNames == null)
				event.detail = DND.DROP_NONE;
			if (event.detail == DND.DROP_NONE)
				return;

			for (int i = 0; (i < sourceNames.length); i++) {
				final File source = new File(sourceNames[i]);
				String sURL = UrlUtils.parseTextForURL(sourceNames[i], true);

				if (sURL != null && !source.exists()) {
					openTorrentWindow(null, new String[] { sURL }, bOverrideToStopped);
				} else if (source.isFile()) {
					
						// go async as vuze file handling can require UI access which then blocks
						// if this is happening during init
					
					new AEThread2( "asyncOpen", true )
					{
						public void
						run()
						{
							String filename = source.getAbsolutePath();
							
							VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
							
							if ( vfh.loadAndHandleVuzeFile( filename, VuzeFileComponent.COMP_TYPE_NONE ) != null ){
								
								return;
							}
							
							
							try {
								openTorrentWindow(null, new String[] { filename }, bOverrideToStopped);
				
							} catch (Exception e) {
								Logger.log(new LogAlert(LogAlert.REPEATABLE,
										"Torrent open fails for '" + filename + "'", e));
							}
						}
					}.start();
					
				} else if (source.isDirectory()) {
					
					String dir_name = source.getAbsolutePath();

					openTorrentWindow(dir_name, null, bOverrideToStopped);
				}
			}
		} else if (event.data instanceof URLTransfer.URLType) {
			openTorrentWindow(null,
					new String[] { ((URLTransfer.URLType) event.data).linkURL },
					bOverrideToStopped);
		}
	}
  
  
  public static String getFilterPathData() {
    String before = COConfigurationManager.getStringParameter("previous.filter.dir.data");
    if( before != null && before.length() > 0 ) {
      return before;
    }
    String def;
		try {
			def = COConfigurationManager.getDirectoryParameter("Default save path");
	    return def;
		} catch (IOException e) {
			return "";
		}
  }
  
  public static String getFilterPathTorrent() {
    String before = COConfigurationManager.getStringParameter("previous.filter.dir.torrent");
    if( before != null && before.length() > 0 ) {
      return before;
    }
    return COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory");
  }
  
  public static String setFilterPathData( String path ) {
    if( path != null && path.length() > 0 ) {
      File test = new File( path );
      if( !test.isDirectory() ) test = test.getParentFile();
      String now = "";
      if( test != null ) now = test.getAbsolutePath();
      String before = COConfigurationManager.getStringParameter("previous.filter.dir.data");
      if( before == null || before.length() == 0 || !before.equals( now ) ) {
        COConfigurationManager.setParameter( "previous.filter.dir.data", now );
        COConfigurationManager.save();
      }
    }
    return path;
  }
  
  public static String setFilterPathTorrent( String path ) {
    if( path != null && path.length() > 0 ) {
      File test = new File( path );
      if( !test.isDirectory() ) test = test.getParentFile();
      String now = "";
      if( test != null ) now = test.getAbsolutePath();
      String before = COConfigurationManager.getStringParameter("previous.filter.dir.torrent");
      if( before == null || before.length() == 0 || !before.equals( now ) ) {
        COConfigurationManager.setParameter( "previous.filter.dir.torrent", now );
        COConfigurationManager.save();
      }
      return now;
    }
    return path;
  }

  private static void openTorrentWindow(final String path,
			final String[] torrents, final boolean bOverrideStartModeToStopped)
	{
  	// loadVuzeFile takes a long time if it's fetching a URL, so prevent it
  	// from blocking the calling thread (like the SWT Thread)
  	new AEThread2("openTorrentWindow", true) {
			public void run() {
				_openTorrentWindow(path, torrents, bOverrideStartModeToStopped);
			}
		}.start();
	}

  private static void _openTorrentWindow(final String path,
			String[] torrents, final boolean bOverrideStartModeToStopped)
	{
	  		// this is a good place to trim out any .vuze files
	  
	  	if ( torrents != null && torrents.length > 0 ){
	  		
	  		VuzeFileHandler vfh = VuzeFileHandler.getSingleton();
	  		
	  		List	non_vuze_files 	= new ArrayList();
	  		List	vuze_files		= new ArrayList();
	  		
	  		for (int i=0;i<torrents.length;i++){
	  			
	  			String	torrent = torrents[i];
	  			
	  			try{			
	  				VuzeFile vf = vfh.loadVuzeFile( torrent );

	  				if ( vf == null ){
	  					
	  					non_vuze_files.add( torrent );
	  					
	  				}else{
	  					
	  					vuze_files.add( vf );
	  				}
	  			}catch( Throwable e ){
	  				
	  				Debug.printStackTrace(e);
	  				
	  				non_vuze_files.add( torrent );
	  			}
	  		}
	  		
	  		if ( vuze_files.size() > 0 ){
	  			
	  			VuzeFile[]	vfs = new VuzeFile[vuze_files.size()];
	  			
	  			vuze_files.toArray( vfs );
	  			
	  			vfh.handleFiles( vfs, VuzeFileComponent.COMP_TYPE_NONE );
	  		}
	  		
	  		if ( non_vuze_files.size() == 0 && vuze_files.size() > 0 ){
	  			
	  			return;
	  		}
	  		
	  		String[]	t = new String[non_vuze_files.size()];
	  		
	  		non_vuze_files.toArray( t );
	  		
	  		torrents = t;
	  	}
	  
	  	final String[] f_torrents = torrents;
	  	
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Shell shell = Utils.findAnyShell();
				if (!AzureusCoreFactory.isCoreRunning()) {
					// not running, wait until running, then either
					// wait for UIFunctionsManager to be initialized,
					// or open immediately
					AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
						public void azureusCoreRunning(AzureusCore core) {
							if (UIFunctionsManager.getUIFunctions() == null) {
								core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
									public void componentCreated(AzureusCore core,
											AzureusCoreComponent component) {
										if (component instanceof UIFunctionsSWT) {
											openTorrentWindow(path, f_torrents,
													bOverrideStartModeToStopped);
										}
									}
								});
							} else {
								openTorrentWindow(path, f_torrents, bOverrideStartModeToStopped);
							}
						}
					});
				}

				if (shell == null) {
					Debug.out("openTorrentWindow().. no shell");
					return;
				}

				OpenTorrentWindow.invoke(shell,
						AzureusCoreFactory.getSingleton().getGlobalManager(), path,
						f_torrents, bOverrideStartModeToStopped, false, false);
			}
		});
	}

	public static boolean doesDropHaveTorrents(DropTargetEvent event) {
		boolean isTorrent = false;
		if (event.data == null && event.currentDataType != null) {
			Object object = URLTransfer.getInstance().nativeToJava(event.currentDataType);
			if (object instanceof URLTransfer.URLType) {
				isTorrent = true;
			}
		} else if (event.data instanceof String[] || event.data instanceof String) {
			final String[] sourceNames = (event.data instanceof String[])
					? (String[]) event.data : new String[] {
						(String) event.data
					};
			for (String name : sourceNames) {
				String sURL = UrlUtils.parseTextForURL(name, true);
				if (sURL != null) {
					isTorrent = true;
					break;
				}
			}
		} else if (event.data instanceof URLTransfer.URLType) {
			isTorrent = true;
		}
		return isTorrent;
	}
}
