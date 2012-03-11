/*
 * Created on May 25, 2004
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.helpers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.util.*;

/**
 * Watches a folder for new torrents and imports them.
 * NOTE: Folder-to-watch and other watching params are taken from a global
 *       config option right now, so starting multiple instances of
 *       TorrentFolderWatcher is useless as currently coded.
 */
public class TorrentFolderWatcher {
	private final static LogIDs LOGID = LogIDs.CORE;

	private final static String PARAMID_FOLDER = "Watch Torrent Folder";

	private GlobalManager global_manager;

	private boolean running = false;

	private final ArrayList to_delete = new ArrayList();

	protected AEMonitor this_mon = new AEMonitor("TorrentFolderWatcher");

	private FilenameFilter filename_filter = new FilenameFilter() {
		public boolean accept(File dir, String name) {
			String lc_name = name.toLowerCase();

			return (lc_name.endsWith(".torrent") || lc_name.endsWith(".tor"));
		}
	};

	private ParameterListener param_listener = new ParameterListener() {
		public void parameterChanged(String parameterName) {
			if (COConfigurationManager.getBooleanParameter(PARAMID_FOLDER)) {
				if (!running) {
					running = true;
					watch_thread.setDaemon(true);
					watch_thread.setPriority(Thread.MIN_PRIORITY);
					watch_thread.start();
				}
			} else
				running = false;
		}
	};

	private final Thread watch_thread = new AEThread("FolderWatcher") {
		public void runSupport() {
			while (running) {
				int sleepMS = COConfigurationManager
						.getIntParameter("Watch Torrent Folder Interval");
				try {
					Thread.sleep(sleepMS * 60000);
				} catch (Exception e) {
					Debug.printStackTrace(e);
				}

				importAddedFiles();
			}
		}
	};

	/**
	 * Start a folder watcher, which will auto-import torrents via the given
	 * manager.
	 * 
	 * @param global_manager
	 */
	public TorrentFolderWatcher(GlobalManager _global_manager) {
		this.global_manager = _global_manager;

		if (COConfigurationManager.getBooleanParameter(PARAMID_FOLDER)) {
			running = true;
			watch_thread.setDaemon(true);
			watch_thread.setPriority(Thread.MIN_PRIORITY);
			watch_thread.start();
		}

		COConfigurationManager.addParameterListener(PARAMID_FOLDER, param_listener);
	}

	/**
	 * Stop and terminate this folder importer watcher.
	 */
	public void destroy() {
		running = false;
		global_manager = null;
		COConfigurationManager.removeParameterListener(PARAMID_FOLDER,
				param_listener);
	}

	private void importAddedFiles() {

		try {
			this_mon.enter();

			if (!running)
				return;

			boolean save_torrents = COConfigurationManager
					.getBooleanParameter("Save Torrent Files");

			String torrent_save_path = COConfigurationManager
					.getStringParameter("General_sDefaultTorrent_Directory");

			int start_state = COConfigurationManager
					.getBooleanParameter("Start Watched Torrents Stopped")
					? DownloadManager.STATE_STOPPED : DownloadManager.STATE_QUEUED;

			String folder_path = COConfigurationManager
					.getStringParameter("Watch Torrent Folder Path");

			String data_save_path = COConfigurationManager
					.getStringParameter("Default save path");

			File folder = null;

			if (folder_path != null && folder_path.length() > 0) {
				folder = new File(folder_path);
				if (!folder.isDirectory()) {
					if (!folder.exists()) {
						FileUtil.mkdirs(folder);
					}
					if (!folder.isDirectory()) {
						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
									"[Watch Torrent Folder Path] " + "does not exist or "
											+ "is not a dir"));
						folder = null;
					}
				}
			}

			if (folder == null) {
				if (Logger.isEnabled())
					Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
							"[Watch Torrent Folder Path] not configured"));
				return;
			}

			File f = null;
			if (data_save_path != null && data_save_path.length() > 0) {
				f = new File(data_save_path);
				
				// Path is not an existing directory.
				if (!f.isDirectory()) {
					if (!f.exists()) {FileUtil.mkdirs(f);}
					
					// If path is still not a directory, abort.
					if (!f.isDirectory()) {
						if (Logger.isEnabled()) {
							Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
									"[Default save path] does not exist or is not a dir"));
						}
						Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
								"[Default save path] does not exist or is not a dir"));
						return;
					}
				}
			}
			
			// If we get here, and this is true, then data_save_path isn't valid.
			if (f == null){
				if (Logger.isEnabled()) {
 					Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"[Default save path] needs to be set for auto-.torrent-import to work"));
				}
				Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
						"[Default save path] needs to be set for auto-.torrent-import to work"));
			}
			
			// if we are saving torrents to the same location as we import them from
			// then we can't assume that its safe to delete the torrent after import! 

			if (torrent_save_path.length() == 0
					|| torrent_save_path.equals(folder_path)
					|| !new File(torrent_save_path).isDirectory()) {

				save_torrents = false;
			}

			//delete torrents from the previous import run

			for (int i = 0; i < to_delete.size(); i++) {

				TOTorrent torrent = (TOTorrent) to_delete.get(i);

				try {
					TorrentUtils.delete(torrent);

				} catch (Throwable e) {

					Debug.printStackTrace(e);
				}
			}

			to_delete.clear();

			String[] currentFileList = folder.list(filename_filter);
			if (currentFileList == null) {
				Logger.log(new LogEvent(LOGID, LogEvent.LT_ERROR,
						"There was a problem trying to get a listing of torrents from " + folder));
				return;
			}

			for (int i = 0; i < currentFileList.length; i++) {

				File file = new File(folder, currentFileList[i]);

				// make sure we've got a valid torrent file before proceeding

				try {

					TOTorrent torrent = TorrentUtils.readFromFile(file, false);

					if (global_manager.getDownloadManager(torrent) != null) {

						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, file.getAbsolutePath()
									+ " is already being downloaded"));

						// we can't touch the torrent file as it is (probably) 
						// being used for the download

					} else {
						
						byte[] hash = null;
						try {
							hash = torrent.getHash();
						} catch (Exception e) { }

						if (!save_torrents) {

							File imported = new File(folder, file.getName() + ".imported");

							TorrentUtils.move(file, imported);

							global_manager.addDownloadManager(imported.getAbsolutePath(), hash,
									data_save_path, start_state, true);

						} else {

							global_manager.addDownloadManager(file.getAbsolutePath(), hash,
									data_save_path, start_state, true);

							// add torrent for deletion, since there will be a 
							// saved copy elsewhere
							to_delete.add(torrent);
						}

						if (Logger.isEnabled())
							Logger.log(new LogEvent(LOGID, "Auto-imported "
									+ file.getAbsolutePath()));
					}

				} catch (Throwable e) {

					Debug.out("Failed to auto-import torrent file '"
							+ file.getAbsolutePath() + "' - "
							+ Debug.getNestedExceptionMessage(e));
					Debug.printStackTrace(e);
				}
			}
		} finally {
			this_mon.exit();
		}
	}

}