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

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.OpenTorrentWindow;
import org.gudy.azureus2.ui.swt.URLTransfer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;

import com.aelitis.azureus.core.*;
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
  	Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				final Display display = SWTThread.getInstance().getDisplay();
		  	final AzureusCore azureus_core = AzureusCoreFactory.getSingleton();
		  	if (display == null || display.isDisposed() || azureus_core == null)
		  		return;
		  	
				new AEThread("TorrentOpener") {
					public void runSupport() {

						for (int i = 0; i < fileNames.length; i++) {

							try {
								TOTorrent t = TorrentUtils.readFromFile(new File(path,
										fileNames[i]), true);

								azureus_core.getTrackerHost().hostTorrent(t, true, true);

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

  public static void openDroppedTorrents(AzureusCore azureus_core,
			DropTargetEvent event, boolean bAllowShareAdd) {
		if (event.data == null)
			return;

		boolean bOverrideToStopped = event.detail == DND.DROP_COPY;

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
					String filename = source.getAbsolutePath();
					try {
						if (!TorrentUtils.isTorrentFile(filename) && bAllowShareAdd) {
							Logger.log(new LogEvent(LogIDs.GUI,
											"openDroppedTorrents: file not a torrent file, sharing"));
							ShareUtils.shareFile(azureus_core, filename);
						} else {
							openTorrentWindow(null, new String[] { filename },
									bOverrideToStopped);
						}
					} catch (Exception e) {
						Logger.log(new LogAlert(LogAlert.REPEATABLE,
								"Torrent open fails for '" + filename + "'", e));
					}
				} else if (source.isDirectory()) {
					
					String dir_name = source.getAbsolutePath();

					if (!bAllowShareAdd) {
						openTorrentWindow(dir_name, null, bOverrideToStopped);
					} else {
						String drop_action = COConfigurationManager.getStringParameter(
								"config.style.dropdiraction" );
	
						if (drop_action.equals("1")) {
							ShareUtils.shareDir(azureus_core, dir_name);
						} else if (drop_action.equals("2")) {
							ShareUtils.shareDirContents(azureus_core, dir_name, false);
						} else if (drop_action.equals("3")) {
							ShareUtils.shareDirContents(azureus_core, dir_name, true);
						} else {
							openTorrentWindow(dir_name, null, bOverrideToStopped);
						}
					}
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
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Shell shell = Utils.findAnyShell();
				AzureusCore core = AzureusCoreFactory.getSingleton();
				GlobalManager gm = null;
				try {
					gm = core.getGlobalManager();
				} catch (AzureusCoreException e) {
				}

				if (gm == null) {
					core.addLifecycleListener(new AzureusCoreLifecycleAdapter() {
						public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
							if (component instanceof UIFunctionsSWT) {
								openTorrentWindow(path, torrents, bOverrideStartModeToStopped);
							}
						}
					});
					return;
				}

				if (shell == null) {
					Debug.out("openTorrentWindow().. no shell");
					return;
				}

				OpenTorrentWindow.invoke(shell, gm, path, torrents,
						bOverrideStartModeToStopped, false, false);
			}
		});
	}
}
