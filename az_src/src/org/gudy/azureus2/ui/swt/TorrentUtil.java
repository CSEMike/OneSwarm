/*
 * Created on 9 Jul 2007
 * Created by Allan Crooks
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
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
 */
package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.category.CategoryManager;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogAlert;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.peer.PEPeerSource;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.util.TRTrackerUtils;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;
import org.gudy.azureus2.ui.swt.minibar.DownloadBar;
import org.gudy.azureus2.ui.swt.shells.InputShell;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.ViewUtils;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableView;

/**
 * @author Allan Crooks
 *
 */
public class TorrentUtil {

	// selected_dl_types -> 0 (determine that automatically), +1 (downloading), +2 (seeding), +3 (mixed - not used by anything yet) 
	public static void fillTorrentMenu(final Menu menu,
			final DownloadManager[] dms, final AzureusCore azureus_core,
			final Composite composite, boolean include_show_details,
			int selected_dl_types, final TableView tv) {

		final boolean isSeedingView;
		switch (selected_dl_types) {
			case 1:
				isSeedingView = false;
				break;
			case 2:
				isSeedingView = true;
				break;
			case 0: {
				if (dms.length == 1) {
					isSeedingView = dms[0].isDownloadComplete(false);
					break;
				}
			}
			default:
				// I was going to raise an error, but let's not be too hasty. :)
				isSeedingView = false;
		}

		boolean hasSelection = (dms.length > 0);

		boolean isTrackerOn = TRTrackerUtils.isTrackerEnabled();
		int userMode = COConfigurationManager.getIntParameter("User Mode");

		// Enable/Disable Logic
		boolean bChangeDir = hasSelection;

		boolean start, stop, changeUrl, barsOpened, forceStart;
		boolean forceStartEnabled, recheck, manualUpdate, fileMove, fileRescan;

		changeUrl = barsOpened = manualUpdate = fileMove = fileRescan = true;
		forceStart = forceStartEnabled = recheck = start = stop = false;

		boolean canSetSuperSeed = false;
		boolean superSeedAllYes = true;
		boolean superSeedAllNo = true;

		boolean upSpeedDisabled = false;
		long totalUpSpeed = 0;
		boolean upSpeedUnlimited = false;
		long upSpeedSetMax = 0;

		boolean downSpeedDisabled = false;
		long totalDownSpeed = 0;
		boolean downSpeedUnlimited = false;
		long downSpeedSetMax = 0;

		boolean allScanSelected = true;
		boolean allScanNotSelected = true;

		boolean allStopped = true;

		if (hasSelection) {
			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = (DownloadManager) dms[i];

				try {
					int maxul = dm.getStats().getUploadRateLimitBytesPerSecond();
					if (maxul == 0) {
						upSpeedUnlimited = true;
					}
					else {
						if (maxul > upSpeedSetMax) {
							upSpeedSetMax = maxul;
						}
					}
					if (maxul == -1) {
						maxul = 0;
						upSpeedDisabled = true;
					}
					totalUpSpeed += maxul;

					int maxdl = dm.getStats().getDownloadRateLimitBytesPerSecond();
					if (maxdl == 0) {
						downSpeedUnlimited = true;
					}
					else {
						if (maxdl > downSpeedSetMax) {
							downSpeedSetMax = maxdl;
						}
					}
					if (maxdl == -1) {
						maxdl = 0;
						downSpeedDisabled = true;
					}
					totalDownSpeed += maxdl;

				}
				catch (Exception ex) {
					Debug.printStackTrace(ex);
				}

				if (dm.getTrackerClient() == null) {
					changeUrl = false;
				}

				if (barsOpened && !DownloadBar.getManager().isOpen(dm)) {
					barsOpened = false;
				}

				stop = stop || ManagerUtils.isStopable(dm);

				start = start || ManagerUtils.isStartable(dm);

				recheck = recheck || dm.canForceRecheck();

				forceStartEnabled = forceStartEnabled || ManagerUtils.isForceStartable(dm);

				forceStart = forceStart || dm.isForceStart();

				boolean stopped = ManagerUtils.isStopped(dm);

				allStopped &= stopped;

				fileMove = fileMove && stopped && dm.isPersistent();

				if (userMode > 1) {
					TRTrackerAnnouncer trackerClient = dm.getTrackerClient();

					if (trackerClient != null) {
						boolean update_state = ((SystemTime.getCurrentTime() / 1000 - trackerClient.getLastUpdateTime() >= TRTrackerAnnouncer.REFRESH_MINIMUM_SECS));
						manualUpdate = manualUpdate & update_state;
					}

				}
				int state = dm.getState();
				bChangeDir &= (state == DownloadManager.STATE_ERROR || state == DownloadManager.STATE_STOPPED || state == DownloadManager.STATE_QUEUED)
						&& dm.isDownloadComplete(false);

				/**
				 * Only perform a test on disk if:
				 *    1) We are currently set to allow the "Change Data Directory" option, and
				 *    2) We've only got one item selected - otherwise, we may potentially end up checking massive
				 *       amounts of files across multiple torrents before we generate a menu.
				 */
				if (bChangeDir && dms.length == 1) {
					bChangeDir = !dm.filesExist();
				}

				boolean scan = dm.getDownloadState().getFlag(DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES);

				// include DND files in incomplete stat, since a recheck may
				// find those files have been completed
				boolean incomplete = !dm.isDownloadComplete(true);

				allScanSelected = incomplete && allScanSelected && scan;
				allScanNotSelected = incomplete && allScanNotSelected && !scan;

				PEPeerManager pm = dm.getPeerManager();

				if (pm != null) {

					if (pm.canToggleSuperSeedMode()) {

						canSetSuperSeed = true;
					}

					if (pm.isSuperSeedMode()) {

						superSeedAllYes = false;

					}
					else {

						superSeedAllNo = false;
					}
				}
				else {
					superSeedAllYes = false;
					superSeedAllNo = false;
				}
			}

			fileRescan = allScanSelected || allScanNotSelected;

		}
		else { // empty right-click
			barsOpened = false;
			forceStart = false;
			forceStartEnabled = false;

			start = false;
			stop = false;
			fileMove = false;
			fileRescan = false;
			upSpeedDisabled = true;
			downSpeedDisabled = true;
			changeUrl = false;
			recheck = false;
			manualUpdate = false;
		}

		// === Root Menu ===

		if (bChangeDir) {
			MenuItem menuItemChangeDir = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(menuItemChangeDir, "MyTorrentsView.menu.changeDirectory");
			menuItemChangeDir.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					changeDirSelectedTorrents(dms, composite.getShell());
				}
			});
		}

		// Open Details
		if (include_show_details) {
			final MenuItem itemDetails = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemDetails, "MyTorrentsView.menu.showdetails");
			menu.setDefaultItem(itemDetails);
			Utils.setMenuItemImage(itemDetails, "details");
			itemDetails.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager dm) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					if (uiFunctions != null) {
						uiFunctions.openManagerView(dm);
					}
				}
			});
			itemDetails.setEnabled(hasSelection);
		}

		// Open Bar
		final MenuItem itemBar = new MenuItem(menu, SWT.CHECK);
		Messages.setLanguageText(itemBar, "MyTorrentsView.menu.showdownloadbar");
		Utils.setMenuItemImage(itemBar, "downloadBar");
		itemBar.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				if (DownloadBar.getManager().isOpen(dm)) {
					DownloadBar.close(dm);
				}
				else {
					DownloadBar.open(dm, menu.getShell());
				}
			} // run
		});
		itemBar.setEnabled(hasSelection);
		itemBar.setSelection(barsOpened);

		// ---
		new MenuItem(menu, SWT.SEPARATOR);

		// Run Data File
		final MenuItem itemOpen = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemOpen, "MyTorrentsView.menu.open");
		Utils.setMenuItemImage(itemOpen, "run");
		itemOpen.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				runTorrents(dms);
			}
		});
		itemOpen.setEnabled(hasSelection);

		// Explore (or open containing folder)
		final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
		final MenuItem itemExplore = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemExplore, "MyTorrentsView.menu." + (use_open_containing_folder ? "open_parent_folder" : "explore"));
		itemExplore.addListener(SWT.Selection, new DMTask(dms, false) {
			public void run(DownloadManager dm) {
				ManagerUtils.open(dm, use_open_containing_folder);
			}
		});
		itemExplore.setEnabled(hasSelection);

		// === advanced menu ===

		final MenuItem itemAdvanced = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemAdvanced, "MyTorrentsView.menu.advancedmenu"); //$NON-NLS-1$
		itemAdvanced.setEnabled(hasSelection);

		final Menu menuAdvanced = new Menu(menu.getShell(), SWT.DROP_DOWN);
		itemAdvanced.setMenu(menuAdvanced);

		// advanced > Download Speed Menu //

		long maxDownload = COConfigurationManager.getIntParameter("Max Download Speed KBs", 0) * 1024;
		long maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs", 0) * 1024;

		ViewUtils.addSpeedMenu(menu.getShell(), menuAdvanced, true, hasSelection, downSpeedDisabled, downSpeedUnlimited,
				totalDownSpeed, downSpeedSetMax, maxDownload, upSpeedDisabled, upSpeedUnlimited, totalUpSpeed,
				upSpeedSetMax, maxUpload, dms.length, new ViewUtils.SpeedAdapter() {
					public void setDownSpeed(final int speed) {
						DMTask task = new DMTask(dms) {
							public void run(DownloadManager dm) {
								dm.getStats().setDownloadRateLimitBytesPerSecond(speed);
							}
						};
						task.go();
					}

					public void setUpSpeed(final int speed) {
						DMTask task = new DMTask(dms) {
							public void run(DownloadManager dm) {
								dm.getStats().setUploadRateLimitBytesPerSecond(speed);
							}
						};
						task.go();
					}
				});

		// advanced > Tracker Menu //
		final Menu menuTracker = new Menu(menu.getShell(), SWT.DROP_DOWN);
		final MenuItem itemTracker = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemTracker, "MyTorrentsView.menu.tracker");
		itemTracker.setMenu(menuTracker);

		final MenuItem itemChangeTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemChangeTracker, "MyTorrentsView.menu.changeTracker"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemChangeTracker, "add_tracker");
		itemChangeTracker.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				TRTrackerAnnouncer tc = dm.getTrackerClient();
				if (tc != null) new TrackerChangerWindow(composite.getDisplay(), tc);
			}
		});
		itemChangeTracker.setEnabled(changeUrl);

		final MenuItem itemEditTracker = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemEditTracker, "MyTorrentsView.menu.editTracker");
		Utils.setMenuItemImage(itemEditTracker, "edit_trackers");
		itemEditTracker.addListener(SWT.Selection, new DMTask(dms) {
			public void run(final DownloadManager dm) {
				if (dm.getTorrent() != null) {
					final TOTorrent torrent = dm.getTorrent();

					java.util.List group = TorrentUtils.announceGroupsToList(torrent);

					new MultiTrackerEditor(null, group, new TrackerEditorListener() {
						public void trackersChanged(String str, String str2, java.util.List group) {
							TorrentUtils.listToAnnounceGroups(group, torrent);

							try {
								TorrentUtils.writeToFile(torrent);
							}
							catch (Throwable e) {
								Debug.printStackTrace(e);
							}

							if (dm.getTrackerClient() != null) dm.getTrackerClient().resetTrackerUrl(true);
						}
					}, true);
				}
			} // run
		});
		itemEditTracker.setEnabled(hasSelection);

		final MenuItem itemManualUpdate = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemManualUpdate, "GeneralView.label.trackerurlupdate"); //$NON-NLS-1$
		//itemManualUpdate.setImage(ImageRepository.getImage("edit_trackers"));
		itemManualUpdate.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				dm.requestTrackerAnnounce(false);
			}
		});
		itemManualUpdate.setEnabled(manualUpdate);

		boolean scrape_enabled = COConfigurationManager.getBooleanParameter("Tracker Client Scrape Enable");

		boolean scrape_stopped = COConfigurationManager.getBooleanParameter("Tracker Client Scrape Stopped Enable");

		boolean manualScrape = (!scrape_enabled) || ((!scrape_stopped) && allStopped);

		final MenuItem itemManualScrape = new MenuItem(menuTracker, SWT.PUSH);
		Messages.setLanguageText(itemManualScrape, "GeneralView.label.trackerscrapeupdate");
		//itemManualUpdate.setImage(ImageRepository.getImage("edit_trackers"));
		itemManualScrape.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				dm.requestTrackerScrape(true);
			}
		});
		itemManualScrape.setEnabled(manualScrape);

		// advanced > files

		final MenuItem itemFiles = new MenuItem(menuAdvanced, SWT.CASCADE);
		Messages.setLanguageText(itemFiles, "ConfigView.section.files");

		final Menu menuFiles = new Menu(composite.getShell(), SWT.DROP_DOWN);
		itemFiles.setMenu(menuFiles);

		final MenuItem itemFileMoveData = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemFileMoveData, "MyTorrentsView.menu.movedata");
		itemFileMoveData.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				if (dms != null && dms.length > 0) {

					DirectoryDialog dd = new DirectoryDialog(composite.getShell());

					dd.setFilterPath(TorrentOpener.getFilterPathData());

					dd.setText(MessageText.getString("MyTorrentsView.menu.movedata.dialog"));

					String path = dd.open();

					if (path != null) {

						TorrentOpener.setFilterPathData(path);

						File target = new File(path);

						for (int i = 0; i < dms.length; i++) {

							try {
								dms[i].moveDataFiles(target);

							}
							catch (Throwable e) {

								Logger.log(new LogAlert(dms[i], LogAlert.REPEATABLE,
										"Download data move operation failed", e));
							}
						}
					}
				}
			}
		});
		itemFileMoveData.setEnabled(fileMove);

		final MenuItem itemFileMoveTorrent = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemFileMoveTorrent, "MyTorrentsView.menu.movetorrent");
		itemFileMoveTorrent.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				if (dms != null && dms.length > 0) {

					DirectoryDialog dd = new DirectoryDialog(composite.getShell());

					dd.setFilterPath(TorrentOpener.getFilterPathTorrent());

					dd.setText(MessageText.getString("MyTorrentsView.menu.movedata.dialog"));

					String path = dd.open();

					if (path != null) {

						File target = new File(path);

						TorrentOpener.setFilterPathTorrent(target.toString());

						for (int i = 0; i < dms.length; i++) {

							try {
								dms[i].moveTorrentFile(target);

							}
							catch (Throwable e) {

								Logger.log(new LogAlert(dms[i], LogAlert.REPEATABLE,
										"Download torrent move operation failed", e));
							}
						}
					}
				}
			}
		});
		itemFileMoveTorrent.setEnabled(fileMove);
		
		final MenuItem itemCheckFilesExist = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemCheckFilesExist, "MyTorrentsView.menu.checkfilesexist");
		itemCheckFilesExist.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				dm.filesExist();
			}
		});

		final MenuItem itemFileRescan = new MenuItem(menuFiles, SWT.CHECK);
		Messages.setLanguageText(itemFileRescan, "MyTorrentsView.menu.rescanfile");
		itemFileRescan.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				dm.getDownloadState().setFlag(DownloadManagerState.FLAG_SCAN_INCOMPLETE_PIECES,
						itemFileRescan.getSelection());
			}
		});
		itemFileRescan.setSelection(allScanSelected);
		itemFileRescan.setEnabled(fileRescan);

		MenuItem itemFileClearResume = new MenuItem(menuFiles, SWT.PUSH);
		Messages.setLanguageText(itemFileClearResume, "MyTorrentsView.menu.clear_resume_data");
		itemFileClearResume.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				dm.getDownloadState().clearResumeData();
			}
		});
		itemFileClearResume.setEnabled(allStopped);

		// === advanced > export ===
		// =========================

		if (userMode > 0) {
			final MenuItem itemExport = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemExport, "MyTorrentsView.menu.exportmenu"); //$NON-NLS-1$
			Utils.setMenuItemImage(itemExport, "export");
			itemExport.setEnabled(hasSelection);

			final Menu menuExport = new Menu(composite.getShell(), SWT.DROP_DOWN);
			itemExport.setMenu(menuExport);

			// Advanced > Export > Export XML
			final MenuItem itemExportXML = new MenuItem(menuExport, SWT.PUSH);
			Messages.setLanguageText(itemExportXML, "MyTorrentsView.menu.export");
			itemExportXML.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager[] dms) {
					DownloadManager dm = dms[0]; // First only.
					if (dm != null) new ExportTorrentWizard(azureus_core, itemExportXML.getDisplay(), dm);
				}
			});

			// Advanced > Export > Export Torrent
			final MenuItem itemExportTorrent = new MenuItem(menuExport, SWT.PUSH);
			Messages.setLanguageText(itemExportTorrent, "MyTorrentsView.menu.exporttorrent");
			itemExportTorrent.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager[] dms) {
					// FileDialog for single download
					// DirectoryDialog for multiple.
					File[] destinations = new File[dms.length];
					if (dms.length == 1) {
						FileDialog fd = new FileDialog(composite.getShell(), SWT.SAVE);
						fd.setFileName(dms[0].getTorrentFileName());
						String path = fd.open();
						if (path == null) {return;}
						destinations[0] = new File(path);
					}
					else {
						DirectoryDialog dd = new DirectoryDialog(composite.getShell(), SWT.SAVE);
						String path = dd.open();
						if (path == null) {return;}
						for (int i=0; i<dms.length; i++) {
							destinations[i] = new File(path, new File(dms[i].getTorrentFileName()).getName());
						}
					}
					
					int i=0;
					try {
						for (; i<dms.length; i++) {
							File target = destinations[i];
							if (target.exists()) {
								MessageBox mb = new MessageBox(composite.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
								mb.setText(MessageText.getString("exportTorrentWizard.process.outputfileexists.title"));
								mb.setMessage(MessageText.getString("exportTorrentWizard.process.outputfileexists.message") + "\n" + destinations[i].getName());

								int result = mb.open();
								if (result == SWT.NO) {return;}

								if (!target.delete()) {
									throw (new Exception("Failed to delete file"));
								}
							} // end deal with clashing torrent

							// first copy the torrent - DON'T use "writeTorrent" as this amends the
							// "filename" field in the torrent
							TorrentUtils.copyToFile(dms[i].getDownloadState().getTorrent(), target);

							// now remove the non-standard entries
							TOTorrent dest = TOTorrentFactory.deserialiseFromBEncodedFile(target);
							dest.removeAdditionalProperties();
							dest.serialiseToBEncodedFile(target);
						} // end for
					} // end try
					catch (Throwable e) {
						Logger.log(new LogAlert(dms[i], LogAlert.UNREPEATABLE, "Torrent export failed", e));
					}
				} // end run()
			}); // end DMTask
			
			// Advanced > Export > WebSeed URL
			final MenuItem itemWebSeed = new MenuItem(menuExport, SWT.PUSH);
			Messages.setLanguageText(itemWebSeed, "MyTorrentsView.menu.exporthttpseeds");
			itemWebSeed.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager[] dms) {
					final String	NL = "\r\n";
					String	data = "";
					
					boolean http_enable = COConfigurationManager.getBooleanParameter( "HTTP.Data.Listen.Port.Enable" );
					
					String	port;
					
					if ( http_enable ){
						
						int	p = COConfigurationManager.getIntParameter( "HTTP.Data.Listen.Port" );
						int o = COConfigurationManager.getIntParameter( "HTTP.Data.Listen.Port.Override" );
					    
						if ( o == 0 ){
							
							port = String.valueOf( p );
							
						}else{
							
							port = String.valueOf( o );
						}
					}else{

						data = "You need to enable the HTTP port or modify the URL(s) appropriately" + NL + NL;
						
						port = "<port>";
					}
				    
					String	ip = COConfigurationManager.getStringParameter( "Tracker IP", "" );
					
					if ( ip.length() == 0 ){
						
						data += "You might need to modify the host address in the URL(s)" + NL + NL;
						
						try{
						
							InetAddress ia = AzureusCoreFactory.getSingleton().getInstanceManager().getMyInstance().getExternalAddress();
						
							if ( ia != null ){
								
								ip = IPToHostNameResolver.syncResolve( ia.getHostAddress(), 10000 );
							}
						}catch( Throwable e ){
							
						}
						
						if ( ip.length() == 0 ){
							
							ip = "<host>";
						}
					}
					
					String	base = "http://" + UrlUtils.convertIPV6Host(ip) + ":" + port + "/";
					
					for (int i=0;i<dms.length;i++){
						
						DownloadManager dm = dms[i];
						
						if ( dm == null ){
							continue;
						}
						
						TOTorrent torrent = dm.getTorrent();
						
						if ( torrent == null ){
							
							continue;
						}
						
						data += base + "webseed" + NL;
						
						try{
							data += base + "files/" + URLEncoder.encode( new String( torrent.getHash(), "ISO-8859-1"), "ISO-8859-1" ) + "/" + NL + NL;
							
						}catch( Throwable e ){
							
						}
					}
					
					if ( data.length() > 0){
						ClipboardCopy.copyToClipBoard( data );
					}
				}
			});
		} // export menu

		
		// === advanced > options ===
		// ===========================

		if (userMode > 0) {
			final MenuItem itemExportXML = new MenuItem(menuAdvanced, SWT.PUSH);
			Messages.setLanguageText(itemExportXML, "MainWindow.menu.view.configuration");
			itemExportXML.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager[] dms) {
					UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
					
					uiFunctions.showMultiOptionsView( dms );
				}
			});
		}
		// === advanced > peer sources ===
		// ===============================

		if (userMode > 0) {
			final MenuItem itemPeerSource = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemPeerSource, "MyTorrentsView.menu.peersource"); //$NON-NLS-1$

			final Menu menuPeerSource = new Menu(composite.getShell(), SWT.DROP_DOWN);
			itemPeerSource.setMenu(menuPeerSource);

			for (int i = 0; i < PEPeerSource.PS_SOURCES.length; i++) {

				final String p = PEPeerSource.PS_SOURCES[i];
				String msg_text = "ConfigView.section.connection.peersource." + p;
				final MenuItem itemPS = new MenuItem(menuPeerSource, SWT.CHECK);
				itemPS.setData("peerSource", p);
				Messages.setLanguageText(itemPS, msg_text); //$NON-NLS-1$
				itemPS.addListener(SWT.Selection, new DMTask(dms) {
					public void run(DownloadManager dm) {
						dm.getDownloadState().setPeerSourceEnabled(p, itemPS.getSelection());
					}
				});
				itemPS.setSelection(true);

				boolean bChecked = hasSelection;
				boolean bEnabled = !hasSelection;
				if (bChecked) {
					bEnabled = true;

					// turn on check if just one dm is not enabled
					for (int j = 0; j < dms.length; j++) {
						DownloadManager dm = (DownloadManager) dms[j];

						if (!dm.getDownloadState().isPeerSourceEnabled(p)) {
							bChecked = false;
						}
						if (!dm.getDownloadState().isPeerSourcePermitted(p)) {
							bEnabled = false;
						}
					}
				}

				itemPS.setSelection(bChecked);
				itemPS.setEnabled(bEnabled);
			}
		}

		// === advanced > networks ===
		// ===========================

		if (userMode > 1) {
			final MenuItem itemNetworks = new MenuItem(menuAdvanced, SWT.CASCADE);
			Messages.setLanguageText(itemNetworks, "MyTorrentsView.menu.networks"); //$NON-NLS-1$

			final Menu menuNetworks = new Menu(composite.getShell(), SWT.DROP_DOWN);
			itemNetworks.setMenu(menuNetworks);

			for (int i = 0; i < AENetworkClassifier.AT_NETWORKS.length; i++) {
				final String nn = AENetworkClassifier.AT_NETWORKS[i];
				String msg_text = "ConfigView.section.connection.networks." + nn;
				final MenuItem itemNetwork = new MenuItem(menuNetworks, SWT.CHECK);
				itemNetwork.setData("network", nn);
				Messages.setLanguageText(itemNetwork, msg_text); //$NON-NLS-1$
				itemNetwork.addListener(SWT.Selection, new DMTask(dms) {
					public void run(DownloadManager dm) {
						dm.getDownloadState().setNetworkEnabled(nn, itemNetwork.getSelection());
					}
				});
				boolean bChecked = hasSelection;
				if (bChecked) {
					// turn on check if just one dm is not enabled
					for (int j = 0; j < dms.length; j++) {
						DownloadManager dm = (DownloadManager) dms[j];

						if (!dm.getDownloadState().isNetworkEnabled(nn)) {
							bChecked = false;
							break;
						}
					}
				}

				itemNetwork.setSelection(bChecked);
			}
		}

		// superseed
		if (userMode > 1 && isSeedingView) {

			final MenuItem itemSuperSeed = new MenuItem(menuAdvanced, SWT.CHECK);

			Messages.setLanguageText(itemSuperSeed, "ManagerItem.superseeding");

			boolean enabled = canSetSuperSeed && (superSeedAllNo || superSeedAllYes);

			itemSuperSeed.setEnabled(enabled);

			final boolean selected = superSeedAllNo;

			if (enabled) {

				itemSuperSeed.setSelection(selected);

				itemSuperSeed.addListener(SWT.Selection, new DMTask(dms) {
					public void run(DownloadManager dm) {
						PEPeerManager pm = dm.getPeerManager();

						if (pm != null) {

							if (pm.isSuperSeedMode() == selected && pm.canToggleSuperSeedMode()) {

								pm.setSuperSeedMode(!selected);
							}
						}
					}
				});
			}
		}

		 final MenuItem itemPositionManual = new MenuItem(menuAdvanced, SWT.PUSH);
		Messages.setLanguageText(itemPositionManual,
				"MyTorrentsView.menu.reposition.manual");
		itemPositionManual.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				InputShell is = new InputShell(
						"MyTorrentsView.dialog.setPosition.title",
						"MyTorrentsView.dialog.setPosition.text");

				String sReturn = is.open();
				if (sReturn == null)
					return;

				int newPosition = -1;
				try {
					newPosition = Integer.valueOf(sReturn).intValue();
				} catch (NumberFormatException er) {
					// Ignore
				}

				int size = azureus_core.getGlobalManager().downloadManagerCount(
						isSeedingView);
				if (newPosition > size)
					newPosition = size;

				if (newPosition <= 0) {
					MessageBox mb = new MessageBox(composite.getShell(), SWT.ICON_ERROR
							| SWT.OK);
					mb.setText(MessageText.getString("MyTorrentsView.dialog.NumberError.title"));
					mb.setMessage(MessageText.getString("MyTorrentsView.dialog.NumberError.text"));

					mb.open();
					return;
				}

				moveSelectedTorrentsTo(tv, dms, newPosition);
			}
		});

		// back to main menu
		if (userMode > 0 && isTrackerOn) {
			// Host
			final MenuItem itemHost = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemHost, "MyTorrentsView.menu.host");
			Utils.setMenuItemImage(itemHost, "host");
			itemHost.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					hostTorrents(dms, azureus_core, composite);
				}
			});

			// Publish
			final MenuItem itemPublish = new MenuItem(menu, SWT.PUSH);
			Messages.setLanguageText(itemPublish, "MyTorrentsView.menu.publish");
			Utils.setMenuItemImage(itemPublish, "publish");
			itemPublish.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					publishTorrents(dms, azureus_core, composite);
				}
			});

			itemHost.setEnabled(hasSelection);
			itemPublish.setEnabled(hasSelection);
		}
		/*  Do we really need the Move submenu?  There's shortcut keys and toolbar
		 *  buttons..

		 new MenuItem(menu, SWT.SEPARATOR);

		 final MenuItem itemMove = new MenuItem(menu, SWT.CASCADE);
		 Messages.setLanguageText(itemMove, "MyTorrentsView.menu.move");
		 Utils.setMenuItemImage(itemMove, "move");
		 itemMove.setEnabled(hasSelection);

		 final Menu menuMove = new Menu(composite.getShell(), SWT.DROP_DOWN);
		 itemMove.setMenu(menuMove);

		 final MenuItem itemMoveTop = new MenuItem(menuMove, SWT.PUSH);
		 Messages.setLanguageText(itemMoveTop, "MyTorrentsView.menu.moveTop");
		 Utils.setMenuItemImage(itemMoveTop, "top");
		 itemMoveTop.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event event) {
		 moveSelectedTorrentsTop();
		 }
		 });
		 itemMoveTop.setEnabled(moveUp);

		 final MenuItem itemMoveUp = new MenuItem(menuMove, SWT.PUSH);
		 Messages.setLanguageText(itemMoveUp, "MyTorrentsView.menu.moveUp");
		 Utils.setMenuItemImage(itemMoveUp, "up");
		 itemMoveUp.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event event) {
		 moveSelectedTorrentsUp();
		 }
		 });

		 final MenuItem itemMoveDown = new MenuItem(menuMove, SWT.PUSH);
		 Messages.setLanguageText(itemMoveDown, "MyTorrentsView.menu.moveDown");
		 Utils.setMenuItemImage(itemMoveDown, "down");
		 itemMoveDown.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event event) {
		 moveSelectedTorrentsDown();
		 }
		 });

		 final MenuItem itemMoveEnd = new MenuItem(menuMove, SWT.PUSH);
		 Messages.setLanguageText(itemMoveEnd, "MyTorrentsView.menu.moveEnd");
		 Utils.setMenuItemImage(itemMoveEnd, "bottom");
		 itemMoveEnd.addListener(SWT.Selection, new Listener() {
		 public void handleEvent(Event event) {
		 moveSelectedTorrentsEnd();
		 }
		 });
		 itemMoveEnd.setEnabled(moveDown);
		 */
		/*  //TODO ensure that all limits combined don't go under the min 5kbs ?
		 //Disable at the end of the list, thus the first item of the array is instanciated last.
		 itemsSpeed[0] = new MenuItem(menuSpeed,SWT.PUSH);
		 Messages.setLanguageText(itemsSpeed[0],"MyTorrentsView.menu.setSpeed.disable");
		 itemsSpeed[0].setData("maxul", new Integer(-1));    
		 itemsSpeed[0].addListener(SWT.Selection,itemsSpeedListener);
		 */

		// Category
		Menu menuCategory = new Menu(composite.getShell(), SWT.DROP_DOWN);
		final MenuItem itemCategory = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemCategory, "MyTorrentsView.menu.setCategory"); //$NON-NLS-1$
		//itemCategory.setImage(ImageRepository.getImage("speed"));
		itemCategory.setMenu(menuCategory);
		itemCategory.setEnabled(hasSelection);

		addCategorySubMenu(dms, menuCategory, composite);

		// Rename
		final MenuItem itemRename = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemRename, "MyTorrentsView.menu.rename");
		itemRename.setEnabled(hasSelection);

		final Menu menuRename = new Menu(composite.getShell(), SWT.DROP_DOWN);
		itemRename.setMenu(menuRename);

		DownloadManager first_selected = (dms.length == 0) ? null : dms[0];

		// Rename -> Displayed Name
		final MenuItem itemRenameDisplayed = new MenuItem(menuRename, SWT.CASCADE);
		Messages.setLanguageText(itemRenameDisplayed, "MyTorrentsView.menu.rename.displayed");
		itemRenameDisplayed.setEnabled(hasSelection);
		if (itemRenameDisplayed.isEnabled()) {
			itemRenameDisplayed.setData("suggested_text", first_selected.getDisplayName());
			itemRenameDisplayed.setData("display_name", Boolean.valueOf(true));
			itemRenameDisplayed.setData("save_name", Boolean.valueOf(false));
			itemRenameDisplayed.setData("msg_key", "displayed");
		}

		// Rename -> Save Name
		final MenuItem itemRenameSavePath = new MenuItem(menuRename, SWT.CASCADE);
		Messages.setLanguageText(itemRenameSavePath, "MyTorrentsView.menu.rename.save_path");
		itemRenameSavePath.setEnabled(fileMove && dms.length == 1);
		if (itemRenameSavePath.isEnabled()) {
			itemRenameSavePath.setData("suggested_text", first_selected.getAbsoluteSaveLocation().getName());
			itemRenameSavePath.setData("display_name", Boolean.valueOf(false));
			itemRenameSavePath.setData("save_name", Boolean.valueOf(true));
			itemRenameSavePath.setData("msg_key", "save_path");
		}

		// Rename -> Both
		final MenuItem itemRenameBoth = new MenuItem(menuRename, SWT.CASCADE);
		Messages.setLanguageText(itemRenameBoth, "MyTorrentsView.menu.rename.displayed_and_save_path");
		itemRenameBoth.setEnabled(fileMove && dms.length == 1);
		if (itemRenameBoth.isEnabled()) {
			itemRenameBoth.setData("suggested_text", first_selected.getAbsoluteSaveLocation().getName());
			itemRenameBoth.setData("display_name", Boolean.valueOf(true));
			itemRenameBoth.setData("save_name", Boolean.valueOf(true));
			itemRenameBoth.setData("msg_key", "displayed_and_save_path");
		}

		Listener rename_listener = new Listener() {
			public void handleEvent(Event event) {
				MenuItem mi = (MenuItem) event.widget;
				String suggested = (String) mi.getData("suggested_text");
				final boolean change_displayed_name = ((Boolean) mi.getData("display_name")).booleanValue();
				final boolean change_save_name = ((Boolean) mi.getData("save_name")).booleanValue();
				String msg_key_prefix = "MyTorrentsView.menu.rename." + (String) mi.getData("msg_key") + ".enter.";
				SimpleTextEntryWindow text_entry = new SimpleTextEntryWindow(composite.getDisplay());
				text_entry.setTitle(msg_key_prefix + "title");
				text_entry.setMessages(new String[] { msg_key_prefix + "message", msg_key_prefix + "message.2" });
				text_entry.setPreenteredText(suggested, false);
				text_entry.prompt();
				if (text_entry.hasSubmittedInput()) {
					String value = text_entry.getSubmittedInput();
					final String value_to_set = (value.length() == 0) ? null : value;
					DMTask task = new DMTask(dms) {
						public void run(DownloadManager dm) {
							if (change_displayed_name) {
								dm.getDownloadState().setDisplayName(value_to_set);
							}
							if (change_save_name) {
								try {
									dm.renameDownload((value_to_set == null) ? dm.getDisplayName() : value_to_set);
								}
								catch (Exception e) {
									Logger.log(new LogAlert(dm, LogAlert.REPEATABLE,
											"Download data rename operation failed", e));
								}
							}
						}
					};
					task.go();
				}
			}
		};

		itemRenameDisplayed.addListener(SWT.Selection, rename_listener);
		itemRenameSavePath.addListener(SWT.Selection, rename_listener);
		itemRenameBoth.addListener(SWT.Selection, rename_listener);

		// Edit Comment
		final MenuItem itemEditComment = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemEditComment, "MyTorrentsView.menu.edit_comment");
		itemEditComment.setEnabled(dms.length > 0);

		itemEditComment.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				promptUserForComment(dms);
			}
		});

		// ---
		new MenuItem(menu, SWT.SEPARATOR);

		// Queue
		final MenuItem itemQueue = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemQueue, "MyTorrentsView.menu.queue"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemQueue, "start");
		itemQueue.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				queueTorrents(dms, menu.getShell());
			}
		});
		itemQueue.setEnabled(start);

		// Force Start
		if (userMode > 0) {
			final MenuItem itemForceStart = new MenuItem(menu, SWT.CHECK);
			Messages.setLanguageText(itemForceStart, "MyTorrentsView.menu.forceStart");
			Utils.setMenuItemImage(itemForceStart, "forcestart");
			itemForceStart.addListener(SWT.Selection, new DMTask(dms) {
				public void run(DownloadManager dm) {
					if (ManagerUtils.isForceStartable(dm)) {
						dm.setForceStart(itemForceStart.getSelection());
					}
				}
			});
			itemForceStart.setSelection(forceStart);
			itemForceStart.setEnabled(forceStartEnabled);
		}

		// Stop
		final MenuItem itemStop = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemStop, "MyTorrentsView.menu.stop"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemStop, "stop");
		itemStop.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				stopTorrents(dms, menu.getShell());
			}
		});
		itemStop.setEnabled(stop);

		// Force Recheck
		final MenuItem itemRecheck = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRecheck, "MyTorrentsView.menu.recheck");
		Utils.setMenuItemImage(itemRecheck, "recheck");
		itemRecheck.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				if (dm.canForceRecheck()) {
					dm.forceRecheck();
				}
			}
		});
		itemRecheck.setEnabled(recheck);

		// Remove
		final MenuItem itemRemove = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(itemRemove, "MyTorrentsView.menu.remove"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemRemove, "delete");
		itemRemove.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				removeTorrent(dm, false, false, menu.getShell());
			}
		});
		itemRemove.setEnabled(hasSelection);

		// === Remove And ===
		// ==================

		final MenuItem itemRemoveAnd = new MenuItem(menu, SWT.CASCADE);
		Messages.setLanguageText(itemRemoveAnd, "MyTorrentsView.menu.removeand"); //$NON-NLS-1$
		Utils.setMenuItemImage(itemRemoveAnd, "delete");
		itemRemoveAnd.setEnabled(hasSelection);

		final Menu menuRemove = new Menu(composite.getShell(), SWT.DROP_DOWN);
		itemRemoveAnd.setMenu(menuRemove);

		// Remove And > Delete Torrent
		final MenuItem itemDeleteTorrent = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteTorrent, "MyTorrentsView.menu.removeand.deletetorrent"); //$NON-NLS-1$
		itemDeleteTorrent.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				removeTorrent(dm, true, false, menu.getShell());
			}
		});

		// Remove And > Delete Data
		final MenuItem itemDeleteData = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteData, "MyTorrentsView.menu.removeand.deletedata");
		itemDeleteData.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				removeTorrent(dm, false, true, menu.getShell());
			}
		});

		// Remove And > Delete Both
		final MenuItem itemDeleteBoth = new MenuItem(menuRemove, SWT.PUSH);
		Messages.setLanguageText(itemDeleteBoth, "MyTorrentsView.menu.removeand.deleteboth");
		itemDeleteBoth.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager dm) {
				removeTorrent(dm, true, true, menu.getShell());
			}
		});
	}

	private static void addCategorySubMenu(final DownloadManager[] dms, Menu menuCategory, final Composite composite) {
		MenuItem[] items = menuCategory.getItems();
		int i;
		for (i = 0; i < items.length; i++) {
			items[i].dispose();
		}

		Category[] categories = CategoryManager.getCategories();
		Arrays.sort(categories);
		
		// Ensure that there is at least one user category available.
		boolean allow_category_selection = categories.length > 0;
		if (allow_category_selection) {
			boolean user_category_found = false;
			for (i=0; i<categories.length; i++) {
				if (categories[i].getType() == Category.TYPE_USER) {
					user_category_found = true;
					break;
				}
			}
			// It may be the categories array just contains "uncategorised".
			allow_category_selection = user_category_found;
		}

		if (allow_category_selection) {
			final Category catUncat = CategoryManager.getCategory(Category.TYPE_UNCATEGORIZED);
			if (catUncat != null) {
				final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
				Messages.setLanguageText(itemCategory, catUncat.getName());
				itemCategory.addListener(SWT.Selection, new DMTask(dms) {
					public void run(DownloadManager dm) {
						dm.getDownloadState().setCategory(catUncat);
					}
				});

				new MenuItem(menuCategory, SWT.SEPARATOR);
			}

			for (i = 0; i < categories.length; i++) {
				final Category category = categories[i];
				if (category.getType() == Category.TYPE_USER) {
					final MenuItem itemCategory = new MenuItem(menuCategory, SWT.PUSH);
					itemCategory.setText(category.getName());
					itemCategory.addListener(SWT.Selection, new DMTask(dms) {
						public void run(DownloadManager dm) {
							dm.getDownloadState().setCategory(category);
						}
					});
				}
			}

			new MenuItem(menuCategory, SWT.SEPARATOR);
		}

		final MenuItem itemAddCategory = new MenuItem(menuCategory, SWT.PUSH);
		Messages.setLanguageText(itemAddCategory, "MyTorrentsView.menu.setCategory.add");

		itemAddCategory.addListener(SWT.Selection, new DMTask(dms) {
			public void run(DownloadManager[] dms) {
				CategoryAdderWindow adderWindow = new CategoryAdderWindow(composite.getDisplay());
				Category newCategory = adderWindow.getNewCategory();
				if (newCategory != null) assignToCategory(dms, newCategory);
			}
		});

	}

  private static void moveSelectedTorrentsTo(TableView tv,
			DownloadManager[] dms, int iNewPos) {
    if (dms == null || dms.length == 0) {
      return;
    }
    
    TableColumnCore sortColumn = tv == null ? null : tv.getSortColumn();
    boolean isSortAscending = sortColumn == null ? true
				: sortColumn.isSortAscending();

    for (int i = 0; i < dms.length; i++) {
      DownloadManager dm = dms[i];
      int iOldPos = dm.getPosition();
      
      dm.getGlobalManager().moveTo(dm, iNewPos);
      if (isSortAscending) {
        if (iOldPos > iNewPos)
          iNewPos++;
      } else {
        if (iOldPos < iNewPos)
          iNewPos--;
      }
    }

    if (tv != null) {
      boolean bForceSort = sortColumn.getName().equals("#");
      tv.columnInvalidate("#");
      tv.refreshTable(bForceSort);
    }
  }

	private static void changeDirSelectedTorrents(DownloadManager[] dms, Shell shell) {
		if (dms.length <= 0) return;

		String sDefPath = COConfigurationManager.getBooleanParameter("Use default data dir") ? COConfigurationManager
				.getStringParameter("Default save path") : "";

		if (sDefPath.length() > 0) {
			File f = new File(sDefPath);

			if (!f.exists()) {
				FileUtil.mkdirs(f);
			}
		}

		DirectoryDialog dDialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
		dDialog.setFilterPath(sDefPath);
		dDialog.setMessage(MessageText.getString("MainWindow.dialog.choose.savepath"));
		String sSavePath = dDialog.open();
		if (sSavePath != null) {
			for (int i = 0; i < dms.length; i++) {
				DownloadManager dm = dms[i];
				if (dm.getState() == DownloadManager.STATE_ERROR) {

					dm.setTorrentSaveDir(sSavePath);

					if (dm.filesExist()) {

						dm.stopIt(DownloadManager.STATE_STOPPED, false, false);

						ManagerUtils.queue(dm, shell);
					}
				}
			}
		}
	}

	public static void runTorrents(Object[] download_managers) {
		for (int i = download_managers.length - 1; i >= 0; i--) {
			DownloadManager dm = (DownloadManager) download_managers[i];
			if (dm != null) {
				ManagerUtils.run(dm);
			}
		}
	}

	public static void hostTorrents(final Object[] download_managers, final AzureusCore azureus_core,
			final Composite composite) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				ManagerUtils.host(azureus_core, dm, composite);
			}
		};
		task.go();
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.showMyTracker();
		}
	}

	public static void publishTorrents(final Object[] download_managers, final AzureusCore azureus_core,
			final Composite composite) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				ManagerUtils.publish(azureus_core, dm, composite);
			}
		};
		task.go();
		UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		if (uiFunctions != null) {
			uiFunctions.showMyTracker();
		}
	}

	public static void removeTorrent(final DownloadManager dm, final boolean bDeleteTorrent, final boolean bDeleteData,
			Shell shell) {
		ManagerUtils.remove(dm, shell, bDeleteTorrent, bDeleteData);
	}

	public static void removeTorrents(Object[] download_managers, final Shell shell) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				removeTorrent(dm, false, false, shell);
			}
		};
		task.go();
	}

	public static void stopTorrents(Object[] download_managers, final Shell shell) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				ManagerUtils.stop(dm, shell);
			}
		};
		task.go();
	}

	public static void queueTorrents(Object[] download_managers, final Shell shell) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				ManagerUtils.queue(dm, shell);
			}
		};
		task.go();
	}

	public static void resumeTorrents(Object[] download_managers) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				ManagerUtils.start(dm);
			}
		};
		task.go();
	}

	// Category Stuff
	public static void assignToCategory(Object[] download_managers, final Category category) {
		DMTask task = new DMTask(toDMS(download_managers)) {
			public void run(DownloadManager dm) {
				dm.getDownloadState().setCategory(category);
			}
		};
		task.go();
	}
	
	public static void promptUserForComment(DownloadManager[] dms) {
		if (dms.length == 0) {return;}
		DownloadManager dm = dms[0];
		
		// Create dialog box.
		String suggested = dm.getDownloadState().getUserComment(); 
		String msg_key_prefix = "MyTorrentsView.menu.edit_comment.enter.";
		SimpleTextEntryWindow text_entry = new SimpleTextEntryWindow(Display.getCurrent());
		text_entry.setTitle(msg_key_prefix + "title");
		text_entry.setMessage(msg_key_prefix + "message");
		text_entry.setPreenteredText(suggested, false);
		text_entry.setMultiLine(true);
		text_entry.prompt();
		
		if (text_entry.hasSubmittedInput()) {
			String value = text_entry.getSubmittedInput();
			final String value_to_set = (value.length() == 0) ? null : value;
			DMTask task = new DMTask(dms) {
				public void run(DownloadManager dm) {
					dm.getDownloadState().setUserComment(value_to_set);
				}
			};
			task.go();
		}
	}


	private static DownloadManager[] toDMS(Object[] objects) {
		if (objects instanceof DownloadManager[]) { return (DownloadManager[]) objects; }
		DownloadManager[] result = new DownloadManager[objects.length];
		System.arraycopy(objects, 0, result, 0, result.length);
		return result;
	}

	private abstract static class DMTask implements Listener {
		private DownloadManager[] dms;

		private boolean ascending;

		public DMTask(DownloadManager[] dms) {
			this(dms, true);
		}

		public DMTask(DownloadManager[] dms, boolean ascending) {
			this.dms = dms;
			this.ascending = ascending;
		}

		// One of the following methods should be overridden.
		public void run(DownloadManager dm) {
		}

		public void run(DownloadManager[] dm) {
		}

		public void handleEvent(Event event) {
			go();
		}

		public void go() {
			try {
				DownloadManager dm = null;
				for (int i = 0; i < dms.length; i++) {
					dm = dms[ascending ? i : (dms.length - 1) - i];
					if (dm == null) {
						continue;
					}
					this.run(dm);
				}
				this.run(dms);
			}
			catch (Exception e) {
				Debug.printStackTrace(e);
			}
		}
	}

	/**
	 * quick check to see if a file might be a torrent
	 * @param torrentFile
	 * @param deleteFileOnCancel
	 * @param parentShell non-null: display a window if it's not a torrent
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	public static boolean isFileTorrent(File torrentFile, Shell parentShell,
			String torrentName) {
		String sFirstChunk = null;
		try {
			sFirstChunk = FileUtil.readFileAsString(torrentFile, 16384).toLowerCase();
		} catch (IOException e) {
			Debug.out("warning", e);
		}
		if (sFirstChunk == null) {
			sFirstChunk = "";
		}

		if (!sFirstChunk.startsWith("d")) {
			if (parentShell != null) {
  			boolean isHTML = sFirstChunk.indexOf("<html") >= 0;
  			MessageBoxShell boxShell = new MessageBoxShell(parentShell,
  					MessageText.getString("OpenTorrentWindow.mb.notTorrent.title"),
  					MessageText.getString("OpenTorrentWindow.mb.notTorrent.text",
  							new String[] {
									torrentName,
									isHTML ? "" : MessageText.getString("OpenTorrentWindow.mb.notTorrent.cannot.display")
  							}), new String[] {
  						MessageText.getString("Button.ok")
  					}, 0);
  			if (isHTML) {
  				boxShell.setHtml(sFirstChunk);
  			}
  			boxShell.open();
			}

			return false;
		}

		return true;
	}
}
