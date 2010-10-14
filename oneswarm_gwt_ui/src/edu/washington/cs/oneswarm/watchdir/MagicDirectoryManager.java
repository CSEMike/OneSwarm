package edu.washington.cs.oneswarm.watchdir;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.config.impl.StringListImpl;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilEncodingException;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentCreator;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.TOTorrentProgressListener;
import org.gudy.azureus2.core3.util.Base32;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.ipc.IPCInterface;
import org.gudy.azureus2.plugins.torrent.TorrentException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreComponent;
import com.aelitis.azureus.core.AzureusCoreException;
import com.aelitis.azureus.core.AzureusCoreLifecycleListener;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.FileCollection;
import edu.washington.cs.oneswarm.f2f.permissions.GroupBean;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;
import edu.washington.cs.oneswarm.ui.gwt.CoreInterface;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.FileTypeFilter;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicPath;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicPathParseException;
import edu.washington.cs.oneswarm.ui.gwt.client.newui.settings.MagicWatchType;
import edu.washington.cs.oneswarm.ui.gwt.rpc.OneSwarmConstants.InOrderType;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendErrorLog;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager;
import edu.washington.cs.oneswarm.ui.gwt.server.PreviewImageGenerator;
import edu.washington.cs.oneswarm.ui.gwt.server.BackendTaskManager.CancellationListener;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager;
import edu.washington.cs.oneswarm.ui.gwt.server.ffmpeg.FFMpegAsyncOperationManager.DataNotAvailableException;

public final class MagicDirectoryManager extends Thread implements ParameterListener, DirectoryWatcherListener {

	private static Logger logger = Logger.getLogger(MagicDirectoryManager.class.getName());

	// HACK! this is from OneSwarmConstants, but isdal has build problems using
	// it there.
	public final static String BITTORRENT_MAGNET_PREFIX = "urn_btih_";

	static volatile MagicDirectoryManager mInstance = null;

	StringList watch_dirs = null;

	class FileChange {
		public FileChange(UpdatingFileTree tree, MagicWatchType type, File baseDir) {
			this.tree = tree;
			this.type = type;
			this.baseDir = baseDir;
		}

		public UpdatingFileTree tree;
		public MagicWatchType type;
		public File baseDir;
	}

	LinkedBlockingQueue<FileChange> additionsToProcess = new LinkedBlockingQueue<FileChange>();
	LinkedBlockingQueue<FileChange> removalsToProcess = new LinkedBlockingQueue<FileChange>();

	final List<String> mExclusions = Collections.synchronizedList(new ArrayList<String>());

	Map<String, DirectoryWatcher> watchers = new HashMap<String, DirectoryWatcher>();
	
	/**
	 * Used to avoid the CPU overhead of deciding torrents for unchanged directory trees. 
	 */
	Set<Long> previouslyCheckedTrees = new HashSet<Long>();
	
	boolean stopping = false;

	boolean stopWatching = false;

	private long nextSyncTime;

	private MagicDirectoryManager() {
		logger.fine("MagicDirectoryManager()");

		watch_dirs = COConfigurationManager.getStringListParameter("Magic Watch Directories");
		if (watch_dirs == null) {
			watch_dirs = new StringListImpl();
		}

		for (int i = 0; i < watch_dirs.size(); i++) {
			logger.info(watch_dirs.get(i) + " wdir" + " " + watch_dirs.get(i).getClass().getName());
		}

		load_exclusions();

		AzureusCoreImpl.getSingleton().addLifecycleListener(new AzureusCoreLifecycleListener() {
			public void componentCreated(AzureusCore core, AzureusCoreComponent component) {
			}

			public boolean restartRequested(AzureusCore core) throws AzureusCoreException {
				return true;
			}

			public void started(AzureusCore core) {
			}

			public boolean stopRequested(AzureusCore core) throws AzureusCoreException {
				stopping = true;
				return true;
			}

			public void stopped(AzureusCore core) {
				logger.fine("got stopped");
				stopping = true;
			}

			public void stopping(AzureusCore core) {
				logger.fine("got stopping");
				stopping = true;
			}

			public boolean syncInvokeRequired() {
				return false;
			}
		});

		// DEBUG
		// watch_dirs.add("/tmp/test/");
		COConfigurationManager.addParameterListener("Magic Watch Directories", this);

		setName("Magic directory scanner");
		setDaemon(true);
		start();
	}

	private void load_exclusions() {
		if (AzureusCoreImpl.isCoreAvailable() == false) {
			logger.warning("couldn't load exclusions: core not available yet!");
			return;
		}

		if (AzureusCoreImpl.getSingleton().isStarted() == false) {
			logger.warning("couldn't load exclusions: core not started yet!");
			return;
		}

		for (DownloadManager dm : (List<DownloadManager>) AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers()) {
			mExclusions.add(dm.getSaveLocation().getAbsolutePath());
		}

		AzureusCoreImpl.getSingleton().getGlobalManager().addListener(new GlobalManagerListener() {
			public void destroyInitiated() {
			}

			public void destroyed() {
			}

			public void downloadManagerAdded(DownloadManager dm) {
				mExclusions.add(dm.getSaveLocation().getAbsolutePath());
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void seedingStatusChanged(boolean seeding_only_mode) {
			}
		});

		synchronized (mExclusions) {
			for (String s : mExclusions) {
				logger.finest("exclusion: " + s);
			}
		}
	}

	public static MagicDirectoryManager get() {
		if (mInstance == null) {
			mInstance = new MagicDirectoryManager();
		}
		return mInstance;
	}

	public void parameterChanged(String parameterName) {
		logger.finer("param changed: " + parameterName);

		if (parameterName.equals("Magic Watch Directories")) {
			synchronized (watch_dirs) {
				watch_dirs = COConfigurationManager.getStringListParameter("Magic Watch Directories");
				if (watch_dirs == null) {
					watch_dirs = new StringListImpl();
				}
			}

			logger.finer("magic watch dirs param changed, calling sync");

			// take this as a signal that the user wants to try again
			stopWatching = false;

			// not strictly necessary, but makes testing easier (and possibly avoids weird bugs)
			previouslyCheckedTrees.clear(); 
			
			nextSyncTime = System.currentTimeMillis();

		}
	}


	/**
	 * We used to be much more proactive here -- maintaining file tree info and
	 * periodically refreshing it. This used a lot of memory for large directory
	 * trees, so now we simply refresh all watch directories periodically.
	 */
	private void sync() {
		logger.fine("sync()");
		synchronized (watch_dirs) {
			long startSyncTime = System.currentTimeMillis();

			/**
			 * Since we are updating now, just remove all existing ones and
			 * create new ones.
			 */
			for (DirectoryWatcher existing : watchers.values()) {
				existing.setDone();
			}
			watchers.clear();

			/**
			 * We may have built up a backlog of things to hash -- these will
			 * regenerate once we do the initial scan of the newly created watch
			 * directories
			 */
			additionsToProcess.clear();

			/**
			 * first pass: anything to add
			 */
			logger.fine(watch_dirs.size() + " watchdirs");
			for (int i = 0; i < watch_dirs.size(); i++) {
				MagicPath magic = null;

				try {
					if (watch_dirs.get(i) == null) {
						continue;
					}
					if (watch_dirs.get(i).length() == 0) {
						continue;
					}
					magic = new MagicPath(watch_dirs.get(i));

				} catch (MagicPathParseException e) {
					logger.warning(e.toString() + " on " + watch_dirs.get(i));
					watch_dirs = new StringListImpl();
					break;
				}

				String path = magic.getPath();
				if (watchers.containsKey(path) == false && !alreadyWatched(path)) {
					try {
						logger.fine("new directoryWatcher: " + path);
						DirectoryWatcher dw = new DirectoryWatcher(magic, 60);
						// dw.start();
						additionsToProcess.put(new FileChange(dw.mTree, magic.getType(), new File(magic.getPath())));
						// dw.addListener(this);
						// watchers.put(path, dw);

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			int preferredIntervalMinutes = 0;
			try {
				preferredIntervalMinutes = COConfigurationManager.getIntParameter("oneswarm.watchdir.refresh.interval");
			} catch (Exception e) {
				e.printStackTrace();
			}
			long duration = System.currentTimeMillis() - startSyncTime;
			long interval = 120*1000;
			// if( duration < 1000 ) {
			// interval = 30*1000;
			// } else { // huge scan time!
			// at least once/hour, 10X the scan time, but no less than 60
			// seconds
				interval = (long)Math.min( Math.max(120*1000, 20*duration), 60*60*1000);
				interval = Math.max(120*1000, interval);
			// }

			if (preferredIntervalMinutes > 0) {
				logger.finest("Using provided setting for watch directory refresh interval: " + interval);
				interval = preferredIntervalMinutes * 60 * 1000;
			}

			nextSyncTime = System.currentTimeMillis() + interval;
			logger.fine("Watch directories sync took: " + duration + " (wall) next in: " + interval + " (" + new Date(nextSyncTime) + ")");
		}
	}

	private boolean alreadyWatched(String path) {
		synchronized (watch_dirs) {
			for (String comp : watchers.keySet()) {
				if (path.startsWith(comp))
					return true;
			}
		}

		return false;
	}

	public void run() {
		// long last_audio_bind = System.currentTimeMillis() + (120 * 1000);
		int errors = 0;

		/**
		 * Sleep for a bit during start to speed up UI loading tasks -- this
		 * doesn't need to happen immediately
		 */
		try {
			Thread.sleep(30 * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}

		sync();

		// try {
		// if(
		// COConfigurationManager.getBooleanParameter("oneswarm.v06.firstrun") )
		// {
		// COConfigurationManager.setParameter("oneswarm.v06.firstrun", false);
		// logger.info("First run of 0.6, trying to auto-add tags for audio");
		// autotag_existing_audio();
		// }
		// } catch( Exception e ) {
		// e.printStackTrace();
		// }

		while (!stopping) {
			try {
				if (stopWatching) {
					Thread.sleep(10 * 1000);
					continue;
				}

				try {
					process_additions();
					
					// we'll no longer have any removals now that we need to do full scans to avoid memory usage
					//process_removals();

					if (System.currentTimeMillis() > nextSyncTime) {
						sync();
					} else {
						Thread.sleep(1000);
					}
				} catch (TOTorrentException e) {
					if (e.getReason() == TOTorrentException.RT_CANCELLED) {
						; // this is okay actually -- skips just this one.
					} else {
						throw e;
					}
				}
			} catch (Exception e) {
				BackendErrorLog.get().logException(e);

				errors++;
				if (errors > 3) {
					for (DirectoryWatcher dw : watchers.values()) {
						dw.removeListener(this);
						dw.setDone();
					}
					watchers.clear();

					BackendErrorLog.get().logString("Previous errors have caused OneSwarm to stop monitoring current watch directories.");
					stopWatching = true;
				}

				try {
					Thread.sleep(1 * 1000);
				} catch (Exception e2) {
				}
			}
		}
	}

	static class MutableBoolean {
		public MutableBoolean(boolean v) {
			val = v;
		}

		public boolean val;
	}

	public static void autotag_existing_audio() {

		final MutableBoolean stopIt = new MutableBoolean(false);
		int ourID = BackendTaskManager.get().createTask("Auto-tagging audio files...", new CancellationListener() {
			public void cancelled(int inID) {
				synchronized (stopIt) {
					stopIt.val = true;
				}
			}
		});
		List<DownloadManager> managers = (List<DownloadManager>) AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers();
		for (int i = 0; i < managers.size(); i++) {
			DownloadManager dm = managers.get(i);

			BackendTaskManager.get().getTask(ourID).setProgress(Math.round(((double) i / (double) managers.size()) * 100.0) + "%");

			if (dm.getDownloadState() == null) {
				logger.warning("null download state for: " + dm.getDisplayName());
				continue;
			}

			synchronized (stopIt) {
				if (stopIt.val == true) {
					break;
				}
			}

			if (dm.getDownloadState().getAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE) == null) {
				bind_audio_xml(dm, true);
			} // if no current tags
		}
		BackendTaskManager.get().removeTask(ourID);
	}

	public static String bind_audio_scan() {
		long start = System.currentTimeMillis();
		StringBuilder out = new StringBuilder();
		out.append("bind audio scan...");
		String metainfoPath = SystemProperties.getMetaInfoPath();
		UpdatingFileTree metainfo = new UpdatingFileTree(new File(metainfoPath));
		List<UpdatingFileTree> q = new ArrayList<UpdatingFileTree>();
		q.add(metainfo);
		while (q.isEmpty() == false) {
			UpdatingFileTree curr = q.remove(0);
			if (curr.isDirectory()) {
				q.addAll(curr.getChildren());
			} else if (curr.getThisFile().getName().equals(PreviewImageGenerator.AUDIO_INFO_FILE)) {
				String hashStr = curr.getThisFile().getParentFile().getName();
				byte[] hashBytes = Base32.decode(hashStr);
				DownloadManager dm = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(hashBytes));
				// old metainfo and/or bogus directory name
				if (dm == null) {
					continue;
				}
				logger.finest("rebind audio considering: " + dm.getDisplayName());
				out.append("considered: " + dm.getDisplayName());
				if (dm.getDownloadState().getAttribute(FileCollection.ONESWARM_ARTIST_ATTRIBUTE) == null && dm.getDownloadState().getAttribute(FileCollection.ONESWARM_ALBUM_ATTRIBUTE) == null) {
					logger.finer("should rebind audio " + dm.getDisplayName());
					out.append("rebinding for: " + dm.getDisplayName());
					bind_audio_xml(dm);
				}
			}
		}
		String str = "binding audio scan took: " + (System.currentTimeMillis() - start);
		logger.info(str);
		out.append(str);
		return out.toString();
	}

	private void process_removals() throws Exception {
		FileChange change = removalsToProcess.poll(1, TimeUnit.SECONDS);
		if (change == null) {
			return;
		}

		logger.fine("process removals: " + change.tree);

		String comp_path = change.tree.thisFile.getAbsolutePath();
		List<DownloadManager> toRemove = new ArrayList<DownloadManager>();
		GlobalManager gm = AzureusCoreImpl.getSingleton().getGlobalManager();
		for (DownloadManager dm : (List<DownloadManager>) gm.getDownloadManagers()) {
			if (dm.getSaveLocation().getAbsolutePath().startsWith(comp_path)) {
				logger.fine("saveLocation.startsWith() delete of: " + dm.getSaveLocation().getAbsolutePath() + " / " + comp_path);
				toRemove.add(dm);
			} else if (comp_path.startsWith(dm.getSaveLocation().getAbsolutePath())) // deleted
			// a
			// file
			// in
			// the
			// torrent
			// directory
			// we
			// created,
			// need
			// to
			// remove
			// (and
			// possibly
			// rehash)
			{
				logger.fine("comp_path.startsWith delete of: " + dm.getSaveLocation().getAbsolutePath() + " / " + comp_path);
				toRemove.add(dm);
			}
		}

		for (DownloadManager dm : toRemove) {
			logger.info("delete causes removal of: " + dm.getTorrentFileName());
			try {
				gm.removeDownloadManager(dm, true, false); // remove torrent
				// file (which we
				// created), but not
				// data
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void process_additions() throws Exception {
		FileChange change = null;
		change = additionsToProcess.poll();
		if (change == null) {
			return;
		}

		if (change.tree == null) {
			logger.warning("Change with null file tree!");
			return;
		}

		logger.fine("dequeued: " + change.tree.thisFile.getAbsoluteFile() + " lastmod: " + change.tree.thisFile.lastModified() + " now " + System.currentTimeMillis());
		if (MagicDecider.isExcluded(mExclusions, change.tree)) {
			logger.fine(change.tree.thisFile.getAbsoluteFile() + " excluded");
			return;
		}

		boolean shouldExcludeFirstLevel = false;
		for (int i = 0; i < this.watch_dirs.size(); i++) {
			if ((new File(watch_dirs.get(i))).equals(change.tree.getThisFile())) {
				shouldExcludeFirstLevel = true;
				logger.finest("excluding first level based on watchdir match: " + watch_dirs.get(i));
				break;
			}
		}

		logger.fine("should exclude first level?: " + shouldExcludeFirstLevel);

		Long digest = new Long( change.tree.modifiedChecksum() );
		if( previouslyCheckedTrees.contains(digest) ) {
			logger.fine("Skipping decideTorrents() for tree: " + change.baseDir.getAbsolutePath() + " as modified checksum matches (" + digest + ")");
			return;
		}
		
		List<File> torrents = MagicDecider.decideTorrents(change.tree, change.type, mExclusions, watch_dirs);

		previouslyCheckedTrees.add(digest);
		
		logger.fine("got " + torrents.size() + " from " + change.tree.thisFile.getName() + " digest: " + digest);
		logger.finer("digests has: " + previouslyCheckedTrees.size());

		for (File f : torrents) {
			mExclusions.add(f.getAbsolutePath());
		}

		for (File p : torrents) {
			// perhaps moved since we scanned it.
			if (p.exists() && (p.length() > 0 || p.isDirectory())) {
				logger.info("create: " + p.getAbsolutePath());
				String tag = computeTags(change.baseDir.getParent(), p.getPath());
				String[] tags = null;
				if (tag != null) {
					tags = new String[] { tag };
				}

				if (COConfigurationManager.getBooleanParameter("oneswarm.directory.tags") == false) {
					tags = null;
				}

				create_swarm_synchronously(p, tags);

				logger.fine("done create");
			} else {
				logger.fine("Skipping create of: " + p.getAbsolutePath() + " exists? " + (p.exists()) + " len? " + p.length());
			}
		}
	}

	boolean cancelled = false;

	private void create_swarm_synchronously(final File file, String[] tags) throws Exception {
		/**
		 * Trying to create these causes a TOTorrentException
		 */
		if (file.isDirectory() == false) {
			if (file.length() == 0) {
				return;
			}
		}

		TOTorrentCreator currentCreator = null;
		cancelled = false;
		try {
			currentCreator = TOTorrentFactory.createFromFileOrDirWithComputedPieceLength(file, new URL("http://tracker.invalid/announce"), true);
		} catch (java.net.MalformedURLException e) {
			throw new TOTorrentException("malformed tracker url (should _never_ happen)", 0);
		}
		final TOTorrentCreator creator_shadow = currentCreator;

		final BackendTaskManager tasks = BackendTaskManager.get();
		final int task_id = tasks.createTask("Hashing...", new CancellationListener() {
			public void cancelled(int inID) {
				mExclusions.add(file.getAbsolutePath());
				creator_shadow.cancel();
			}
		});
		tasks.getTask(task_id).setSummary("Watch directory hash: " + file.getName());

		currentCreator.addListener(new TOTorrentProgressListener() {
			public void reportCurrentTask(String task_description) {
				System.out.println("creating: " + task_description);
			}

			public void reportProgress(int percent_complete) {
				if ((percent_complete % 10) == 0) {
					logger.fine("progress: " + percent_complete);
				}

				if (tasks.getTask(task_id) != null) {
					tasks.getTask(task_id).setProgress(percent_complete + "%");
				}

				if (stopping && !cancelled) {
					creator_shadow.cancel();
					cancelled = true;
				}
			}
		});
		TOTorrent created = null;
		try {
			created = currentCreator.create();
		} catch (TOTorrentException e) {
			if (e.getReason() == TOTorrentException.RT_ZERO_LENGTH) {
				logger.warning("Skipping creation of zero-length swarm: " + file.getAbsolutePath());
				return;
			}
			throw e;
		}
		logger.finer("create finished, removing task_id: " + task_id);
		tasks.removeTask(task_id);
		if (created == null || cancelled) {
			System.err.println("created == null, canceled?");
			return;
		}

		String configSavePath = COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory");
		File outTorrent = null;
		if (configSavePath == null) {
			outTorrent = new File(file.getParentFile().getAbsolutePath(), file.getName() + ".torrent");
		} else {
			outTorrent = new File(configSavePath, file.getName() + ".torrent");
		}
		logger.finer("saving to: " + outTorrent.getAbsolutePath());

		try {
			LocaleTorrentUtil.setDefaultTorrentEncoding(created);
		} catch (LocaleUtilEncodingException e1) {
			e1.printStackTrace();
		}

		logger.finer("setdefaultencoding, serializing...");

		created.serialiseToBEncodedFile(outTorrent);

		logger.finest("done that");

		/**
		 * very small chance of this happening -- most of the time the quit will
		 * come during the hashing (which will cancel it, which will result in
		 * null and immediate return)
		 */
		if (!stopping) {
			generate_preview_for_torrent(created, file);

			logger.finer("settings perms");
			ArrayList<GroupBean> typed = new ArrayList<GroupBean>();
			typed.add(GroupBean.ALL_FRIENDS);
			PermissionsDAO.get().setGroupsForHash(ByteFormatter.encodeString(created.getHash()), typed, true);

			/**
			 * Finally add that swarm and make sure the permissions are f2f only
			 */
			GlobalManager gm = AzureusCoreImpl.getSingleton().getGlobalManager();

			logger.finer("calling add download manager, file: " + file.getAbsolutePath() + " save: " + file.getParentFile().getAbsolutePath());

			try {
				final DownloadManager dm = gm.addDownloadManager(outTorrent.getAbsolutePath(), created.getHash(), file.getAbsolutePath(), org.gudy.azureus2.core3.download.DownloadManager.STATE_WAITING, true, true, null);

				if (tags != null) {
					DownloadManagerState dmState = dm.getDownloadState();
					if (dmState != null) {
						dm.getDownloadState().setListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE, tags);
						logger.finer("set tags: " + tags.length + " first: " + tags[0]);
					}
				}

				dm.addListener(new DownloadManagerListener() {
					public void completionChanged(DownloadManager manager, boolean completed) {
					}

					public void downloadComplete(DownloadManager manager) {
					}

					public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
					}

					public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
					}

					public void stateChanged(DownloadManager manager, int state) {
						if (state == org.gudy.azureus2.core3.download.DownloadManager.STATE_SEEDING) {
							logger.fine("binding audio data for: " + dm.getDisplayName());
							MagicDirectoryManager.bind_audio_xml(dm);

							dm.stopIt(org.gudy.azureus2.core3.download.DownloadManager.STATE_STOPPED, false, false);
							dm.removeListener(this);
						}
					}
				});

				dm.setForceStart(true);
			} catch (Exception e) {
				logger.warning(e.toString());
				e.printStackTrace();
				throw e;
			}

			// logger.finest("force start");
			// dm.setForceStart(true);
		} else {
			logger.finer("was stopping");
		}

		// coreInterface.getF2FInterface().setTorrentPrivacy(dm.getTorrent().getHash(),
		// false, true);
		// start doesn't matter here, f2f should start it automatically if
		// there's a request
	}

	public static void generate_preview_for_torrent(TOTorrent created, File file) throws TOTorrentException {

		logger.finer("generating preview..2.");
		try {
			File largestFile = null;
			long largest = 0;
			for (TOTorrentFile f : created.getFiles()) {
				if (f == null) {
					continue;
				}
	
				if (InOrderType.getType(f.getRelativePath()) == null) {
					continue;
				}
				if (f.getLength() > largest) {
					largest = f.getLength();
					if (file.isDirectory() == false) {
						largestFile = new File(file.getParent(), f.getRelativePath());
					} else {
						largestFile = new File(file, f.getRelativePath());
					}
				}
			}
			
			if( largestFile != null ) {
				logger.finer("largest is: " + largestFile.getAbsolutePath());
			}
	
			try {
				FFMpegAsyncOperationManager.getInstance().getPreviewImage(created.getHash(), largestFile, 10, TimeUnit.SECONDS);
			} catch (TorrentException e) {
				// this should never happen...
				e.printStackTrace();
			} catch (DataNotAvailableException e) {
				logger.finest("unable to create preview for file: " + largest);
			}
		} catch( NullPointerException e ) {
			logger.warning("Preview generation null pointer: " + e.toString());
			e.printStackTrace();
		}

	}

	// public static boolean generate_audio_preview( File largestFile, File
	// imageFile ) {
	// try {
	// System.out.println(largestFile.getAbsolutePath());
	// AudioFile f = AudioFileIO.read(largestFile);
	// logger.finest("read audio file");
	// byte [] binaryData = f.getTag().getFirstArtwork().getBinaryData();
	//			
	// try {
	// FFMpegTools.writeTransformedImage(new ByteArrayInputStream(binaryData),
	// imageFile, false);
	// logger.fine("wrote preview from " + largestFile.getName() + " to " +
	// imageFile.getAbsolutePath());
	// return true;
	// } catch( Exception e ) {
	// logger.warning("error writing out preview: " + e.toString());
	// throw e;
	// }
	// } catch( Exception e ) {
	// logger.warning("error reading audio tags during preview generation: " +
	// e.toString());
	// }
	//		
	// return false;
	// }

	public static boolean generate_audio_info_xml(File saveLocation, TOTorrent inTorrent, File metaFile) {

		Map<String, Properties> audio_file_properties = new HashMap<String, Properties>();

		if (inTorrent.isSimpleTorrent()) {
			Properties p = new Properties();
			InOrderType type = InOrderType.getType(saveLocation.getName());
			if (type != null) {

				if (type.getFileTypeFilter().equals(FileTypeFilter.Audio)) {
					try {
						AudioFile f = AudioFileIO.read(saveLocation);
						Tag tag = f.getTag();

						if (tag != null) {
							AudioHeader audioHeader = f.getAudioHeader();
							setPropsFromTagAndHeader(p, audioHeader, tag);
						}

						if (p.size() > 0) {
							audio_file_properties.put(saveLocation.getName(), p);
						}
					} catch (Exception e) {
						System.err.println("audio tag parse error: " + e.toString());
					}
				}
			}
		} else {
			for (TOTorrentFile torrent_file : inTorrent.getFiles()) {
				Properties p = new Properties();
				audio_file_properties.put(torrent_file.getRelativePath(), p);

				InOrderType type = InOrderType.getType(torrent_file.getRelativePath());
				if (type != null) {

					if (type.getFileTypeFilter().equals(FileTypeFilter.Audio)) {
						try {
							File file = new File(saveLocation, torrent_file.getRelativePath());
							AudioFile f = AudioFileIO.read(file);
							Tag tag = f.getTag();

							if (tag != null) {
								AudioHeader audioHeader = f.getAudioHeader();
								setPropsFromTagAndHeader(p, audioHeader, tag);

								if (p.size() > 0) {
									audio_file_properties.put(saveLocation.getName(), p);
								}
							}
						} catch (Exception e) {
							System.err.println("audio tag parse error: " + e.toString());
						}
					} // if it's an audio type
				} // if this file has a recognizable type
			} // for over torrent files
		}

		if (audio_file_properties.size() > 0) {
			try {
				XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(metaFile)));
				encoder.writeObject(audio_file_properties);
				encoder.close();
				logger.fine("wrote audio properties xml for: " + (new String(inTorrent.getName(), "UTF-8")));
			} catch (Exception e) {
				try {
					logger.warning("error writing audio properties for: " + new String(inTorrent.getName(), "UTF-8") + " / " + e.toString());
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
				return false;
			}

			return true;
		}
		return false;
	}

	/**
	 * TODO: all this code needs massively refactored (put all the extra xml
	 * generating info functions in the same place)
	 */
	public static void bind_audio_xml(DownloadManager real_dl) {
		bind_audio_xml(real_dl, false);
	}

	public static void bind_audio_xml(DownloadManager real_dl, boolean force) {
		try {
			/**
			 * Don't do this multiple times.
			 */
			if (real_dl.getDownloadState() != null && force == false) {
				if (real_dl.getDownloadState().getAttribute(FileCollection.ONESWARM_ALBUM_ATTRIBUTE) != null) {
					return;
				}
				if (real_dl.getDownloadState().getAttribute(FileCollection.ONESWARM_ARTIST_ATTRIBUTE) != null) {
					return;
				}
			}

			File metaInfoDir = CoreInterface.getMetaInfoDir(real_dl.getTorrent().getHash());
			File audio_xml = new File(metaInfoDir, PreviewImageGenerator.AUDIO_INFO_FILE);
			if (audio_xml.exists() == false) {
				logger.finest("no xml data for: " + real_dl.getDisplayName() + " / skipping audio info bind");
				return;
			}

			XMLDecoder decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(audio_xml)));
			Map<String, Properties> audio_file_properties = (Map<String, Properties>) decoder.readObject();
			decoder.close();

			boolean setArtist = false, setAlbum = false;
			for (Properties p : audio_file_properties.values()) {
				for (String attrib : new String[] { FileCollection.ONESWARM_ARTIST_ATTRIBUTE }) {
					if (p.getProperty(attrib) != null) {
						real_dl.getDownloadState().setAttribute(FileCollection.ONESWARM_ARTIST_ATTRIBUTE, p.getProperty(attrib));
						logger.fine("bound artist: " + p.getProperty(attrib) + " for " + real_dl.getDisplayName());
						setArtist = true;

						if (COConfigurationManager.getBooleanParameter("oneswarm.add.id3.tags")) {
							logger.fine("adding id3 tag for album: " + p.getProperty(attrib).replaceAll("/", "-"));
							String[] tags = real_dl.getDownloadState().getListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE);
							if (tags == null) {
								tags = new String[0];
							}
							String[] neu = new String[tags.length + 1];
							System.arraycopy(tags, 0, neu, 0, tags.length);
							neu[tags.length] = "Artists/" + p.getProperty(attrib).replaceAll("/", "-");
							real_dl.getDownloadState().setListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE, neu);
						}

						break;
					}
				}

				for (String attrib : new String[] { FileCollection.ONESWARM_ALBUM_ATTRIBUTE }) // to
				// deal
				// with
				// old
				// versions
				{
					if (p.getProperty(attrib) != null) {
						real_dl.getDownloadState().setAttribute(FileCollection.ONESWARM_ALBUM_ATTRIBUTE, p.getProperty(attrib));
						logger.fine("bound album: " + p.getProperty(attrib) + " for " + real_dl.getDisplayName());
						setAlbum = true;

						if (COConfigurationManager.getBooleanParameter("oneswarm.add.id3.tags")) {
							logger.fine("adding id3 tag for album: " + p.getProperty(attrib).replaceAll("/", "-"));
							String[] tags = real_dl.getDownloadState().getListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE);
							if (tags == null) {
								tags = new String[0];
							}
							String[] neu = new String[tags.length + 1];
							System.arraycopy(tags, 0, neu, 0, tags.length);
							neu[tags.length] = "Albums/" + p.getProperty(attrib).replaceAll("/", "-");
							real_dl.getDownloadState().setListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE, neu);
						}

						break;
					}
				}
				if (setArtist && setAlbum) {
					break;
				}
			}

			/**
			 * Need to regenerate our file list to incorporate this new info
			 */
			if (setArtist || setAlbum) {
				PluginInterface f2fIf = AzureusCoreImpl.getSingleton().getPluginManager().getPluginInterfaceByID("osf2f");
				if (f2fIf != null) {
					if (f2fIf.isOperational() == true) {
						IPCInterface ipc = f2fIf.getIPC();
						if (ipc != null) {
							ipc.invoke("refreshFileLists", new Object[0]);
						} else {
							logger.warning("f2f IPC is null, couldn't regenerate file list after audio binding");
						}
					} else {
						logger.warning("Couldn't regenerate file list after audio info binding, f2f plugin is not operational");
					}
				} else {
					logger.warning("Couldn't regenerate file list after audio info binding, f2f plugin interface is null");
				}
			}

		} catch (Exception e) {
			logger.warning("error binding audio xml to download manager: " + real_dl.getDisplayName() + " / " + e.toString());
		}
	}

	private static void setPropsFromTagAndHeader(Properties p, AudioHeader audioHeader, Tag tag) {
		String artist = tag.getFirstArtist();
		if (artist != null) {
			p.setProperty(FileCollection.ONESWARM_ARTIST_ATTRIBUTE, tag.getFirstArtist());
		}
		String album = tag.getFirstAlbum();
		if (album != null) {
			p.setProperty(FileCollection.ONESWARM_ALBUM_ATTRIBUTE, album);
		}
		String firstYear = tag.getFirstYear();
		if (firstYear != null) {
			p.setProperty("year", firstYear);
		}
		String firstGenre = tag.getFirstGenre();
		if (firstGenre != null) {
			p.setProperty("genre", firstGenre);
		}
		String firstTitle = tag.getFirstTitle();
		if (firstTitle != null) {
			p.setProperty("title", firstTitle);
		}

		if (audioHeader != null) {
			int trackLength = audioHeader.getTrackLength();
			p.setProperty("length", Integer.toString(trackLength));
			String format = audioHeader.getFormat();
			if (format != null) {
				p.setProperty("format", format);
			}
			long bitRateAsNumber = audioHeader.getBitRateAsNumber();
			p.setProperty("bitrate", Long.toString(bitRateAsNumber));
			String encodingType = audioHeader.getEncodingType();
			if (encodingType != null) {
				p.setProperty("encoding", encodingType);
			}
		}
	}

	public void deleteFileObserved(DirectoryWatcher watcher, UpdatingFileTree inAbsolutePath) {
		logger.finest("delete observed: " + inAbsolutePath.thisFile.getAbsolutePath());

		try {
			removalsToProcess.put(new FileChange(inAbsolutePath, watcher.getWatchType(), new File(watcher.getPath())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void newFileObserved(DirectoryWatcher watcher, UpdatingFileTree inAbsolutePath) {
		logger.finest("change event: " + inAbsolutePath.thisFile.getAbsolutePath());
		/**
		 * TODO: optimization: use prefix tree
		 */
		synchronized (mExclusions) {
			for (String pre : mExclusions) {
				if (inAbsolutePath.thisFile.getAbsolutePath().startsWith(pre)) {
					// System.out.println("excluded by " + pre);
					return;
				}
			}
		}

		try {
			additionsToProcess.put(new FileChange(inAbsolutePath, watcher.getWatchType(), new File(watcher.getPath())));
			logger.finer("put into to check out q: " + inAbsolutePath.thisFile.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final void main(String[] args) throws Exception {
		LogManager.getLogManager().readConfiguration(new FileInputStream("./logging.properties"));
		UpdatingFileTree tree = new UpdatingFileTree(new File("/Volumes/x/watch_test"), new UpdatingFileTreeListener() {
			public void broadcastChange(UpdatingFileTree path, boolean isDelete) {
				System.out.println("change: " + path.getThisFile().getName() + " " + isDelete);
			}
		});

		while (true) {
			tree.update();
			Thread.sleep(5 * 1000);
		}
		//		
		// int tried = 0, success = 0;
		//		
		// List<UpdatingFileTree> stack = new ArrayList<UpdatingFileTree>();
		// stack.add(tree);
		//		
		// while( stack.isEmpty() == false )
		// {
		// UpdatingFileTree kid = stack.remove(0);
		// InOrderType type = InOrderType.getType(kid.getThisFile().getName());
		// if( type != null ) {
		//				
		// if(
		// type.getFileTypeFilter().equals(MagicDecider.FileTypeFilter.Audio) )
		// {
		// tried++;
		//
		// try {
		// AudioFile f = AudioFileIO.read(kid.getThisFile());
		// Tag tag = f.getTag();
		//						
		// if( tag != null )
		// {
		// String artist = tag.getFirstArtist();
		// String album = tag.getFirstAlbum();
		// System.out.println(artist + " " + album);
		// if( artist != null || album != null )
		// success++;
		// }
		// } catch( Exception e ) {
		// System.err.println("audio tag parse error: " + e.toString());
		// }
		// }
		// // if( tried > 100 )
		// // break;
		// }
		//			
		// stack.addAll(kid.getChildren());
		// }

	}

	public static String computeTags(String baseParent, String path) {
		try {
			StringBuilder tagPath = null;
			if (path.startsWith(baseParent) && path.equals(baseParent) == false) {
				// String subbed = path.substring(basePath.length(),
				// path.length());
				String subbed = path.substring(baseParent.length(), path.length());

				StringTokenizer toks = new StringTokenizer(subbed, File.separator);
				int howmany = toks.countTokens();
				// System.out.println(subbed + " sep: " + File.separator +
				// " has " + toks.countTokens() + " toks: ");
				tagPath = new StringBuilder();
				for (int i = 0; i < howmany - 1; i++) {
					tagPath.append(toks.nextToken());
					if (i < howmany - 2) {
						tagPath.append("/");
					}
				}
				System.out.println("for " + path + " tagPath: " + tagPath);
				if (tagPath.length() == 0) {
					tagPath = null;
				}
			}

			if (tagPath != null) {
				return tagPath.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
