package edu.washington.cs.oneswarm.f2f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.category.Category;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.download.DownloadManagerStats;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.impl.TOTorrentImpl;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1SourceFinder;
import edu.washington.cs.oneswarm.f2f.network.OverlayManager;
import edu.washington.cs.oneswarm.f2f.network.OverlayTransport;
import edu.washington.cs.oneswarm.f2f.permissions.PermissionsDAO;

public class FileListManager {
	private static final String FILE_MISSING_CHECK_NEEDED = "file_check_needed";

	private static Logger logger = Logger.getLogger(FileListManager.class.getName());

	private static final int MAX_FILE_LIST_REFRESH_RATE = 5000;
	public static final int MAX_SEARCH_HITS = 30;
	private static final int MAX_SEND_FILE_LIST_RATE = 30 * 1000;

	private ConcurrentHashMap<Long, byte[]> hashhashToInfoHashMapping = new ConcurrentHashMap<Long, byte[]>();

	private final ConcurrentHashMap<Long, String> hashhashToTorrentName = new ConcurrentHashMap<Long, String>();
	private final ConcurrentHashMap<Friend, FileList> incomingFileLists = new ConcurrentHashMap<Friend, FileList>();
	private Semaphore initialFileListSemaphore = new Semaphore(0);
	private volatile long lastTimeFileListSentToFriends = 0;
	private final MetaInfoManager metaInfoManager;
	private volatile FileList ownF2FFileList;
	private final PermissionsDAO permissionsManager;

	/**
	 * Only run refresh every 500 ms (after last one finished, and only one at a
	 * time)
	 */
	private final FileListRefresher refreshRateLimiter = new FileListRefresher("FileListManager refresh rate limiter");

	private volatile FileList searchableFileList;
	private volatile Timer updateRateLimiter = null;
	private long lastFileListRefreshMs = 0;

	private final HashMap<DownloadManager, Boolean> includedInFileList = new HashMap<DownloadManager, Boolean>();

	private NegativeHitCache negativeHitCache = new NegativeHitCache();

	public FileListManager(PermissionsDAO permissionsManager) {
		this.permissionsManager = permissionsManager;
		this.metaInfoManager = new MetaInfoManager();

		refreshRateLimiter.setDaemon(true);
		refreshRateLimiter.start();

		scheduleFileListRefresh();
		AzureusCoreImpl.getSingleton().getGlobalManager().addListener(new GlobalManagerListener() {
			/*
			 * a refresh is needed if we are should be in the file list but
			 * aren't
			 */
			private void checkIfRefreshNeeded(DownloadManager dm) {
				boolean completedOrRunning = completedOrDownloading(dm);
				if (completedOrRunning && !includedInFileList.containsKey(dm)) {
					scheduleFileListRefresh();
				} else if (!completedOrRunning && includedInFileList.containsKey(dm)) {
					scheduleFileListRefresh();
				}
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}

			public void downloadManagerAdded(final DownloadManager dm) {
				scheduleFileListRefresh();

				dm.addListener(new DownloadManagerListener() {
					public void stateChanged(DownloadManager manager, int state) {
						if (manager.getState() == DownloadManager.STATE_ERROR) {
							manager.getDownloadState().setBooleanAttribute(FILE_MISSING_CHECK_NEEDED, true);
						}

						logger.fine("download state changed, refresh might be needed");
						checkIfRefreshNeeded(dm);
					}

					public void positionChanged(DownloadManager download, int oldPosition, int newPosition) {
					}

					public void filePriorityChanged(DownloadManager download, DiskManagerFileInfo file) {
					}

					public void downloadComplete(DownloadManager manager) {
						logger.fine("download completed, refresh might be needed");
						checkIfRefreshNeeded(dm);
					}

					public void completionChanged(DownloadManager manager, boolean bCompleted) {
						logger.fine("download completion changed, refresh might be needed");
						checkIfRefreshNeeded(dm);
					}
				});

				dm.addPeerListener(new DownloadManagerPeerListener() {
					public void peerRemoved(PEPeer peer) {
						checkIfRefreshNeeded(dm);
					}

					public void peerManagerWillBeAdded(PEPeerManager manager) {
					}

					public void peerManagerRemoved(PEPeerManager manager) {
					}

					public void peerManagerAdded(PEPeerManager manager) {
					}

					public void peerAdded(PEPeer peer) {
						logger.fine("peer added, refresh might be needed");
						checkIfRefreshNeeded(dm);
					}
				});

			}

			public void downloadManagerRemoved(DownloadManager dm) {
				scheduleFileListRefresh();
			}

			public void seedingStatusChanged(boolean seeding_only_mode) {
			}
		});
	}

	private FileList generateFileListForFriend(FileList baseFileList, Friend f) {
		long time = System.currentTimeMillis();
		List<FileCollection> forFriend = new LinkedList<FileCollection>();

		for (FileCollection c : baseFileList.getElements()) {
			if (permissionsManager.hasPermissions(f.getPublicKey(), c.getUniqueIdBytes())) {
				forFriend.add(c);
			} else {
				logger.fine("friend: " + f.getNick() + " has no access to file: " + c.getName());
			}
		}
		logger.fine("created file list for " + f.getNick() + " num swarms=" + forFriend.size() + " time=" + (System.currentTimeMillis() - time));
		return new FileList(forFriend);
	}

	@SuppressWarnings( { "unchecked" })
	private void generateOwnLists() {
		long time = System.currentTimeMillis();
		includedInFileList.clear();
		List<FileCollection> allFiles = new LinkedList<FileCollection>();
		List<FileCollection> searchableFiles = new LinkedList<FileCollection>();

		logger.finest("Getting downloads list...");

		List<DownloadManager> downloads = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManagers();
		logger.finest("got it");

		for (DownloadManager download : downloads) {
			try {
				logger.finest("considering download: " + download.getDisplayName());

				TOTorrent t = download.getTorrent();

				if (t == null) {
					logger.warning("Null torrent for download: " + download.getDisplayName());
					continue;
				}

				boolean completedOrDownloading = completedOrDownloading(download);

				/*
				 * check if we marked this as a potential problem torrent
				 */
				if (completedOrDownloading) {
					if (download.getDownloadState().getBooleanAttribute(FILE_MISSING_CHECK_NEEDED)) {
						logger.finest("marked as potential problem torrent, checking if files exists: " + download.getDisplayName());
						if (download.filesExist()) {
							download.getDownloadState().setBooleanAttribute(FILE_MISSING_CHECK_NEEDED, false);
						} else {
							logger.finest("files missing: " + download.getDisplayName());
							continue;
						}
					}
				}

				boolean autoAdded = download.getDownloadState().getBooleanAttribute(Sha1SourceFinder.ONESWARM_AUTO_ADDED);
				// it is "allowed" if the f2f network and peer source is enabled
				logger.finest("getting networks and sources");

				String[] networks = download.getDownloadState().getNetworks();
				String[] peerSources = download.getDownloadState().getPeerSources();
				logger.finest("done");
				boolean allowed = OverlayTransport.checkOSF2FAllowed(peerSources, networks);
				if (allowed) {

					DiskManagerFileInfo[] files = download.getDiskManagerFileInfo();

					String[] sha1List = null;
					String[] ed2kList = null;
					String hashesAdded = download.getDownloadState().getAttribute(Sha1HashManager.OS_HASHES_ADDED);
					if (hashesAdded != null && (hashesAdded.equals(Sha1HashManager.OS_HASHES_TYPE_LOCAL) || hashesAdded.equals(Sha1HashManager.OS_HASHES_TYPE_TORRENT))) {
						sha1List = download.getDownloadState().getListAttribute(TOTorrentImpl.OS_SHA1);
						ed2kList = download.getDownloadState().getListAttribute(TOTorrentImpl.OS_ED2K);
					}
					ArrayList<FileListFile> subListSearchable = new ArrayList<FileListFile>();

					for (int i = 0; i < files.length; i++) {
						DiskManagerFileInfo torrentFile = files[i];
						FileListFile f = new FileListFile(torrentFile.getTorrentFile().getRelativePath(), torrentFile.getLength());
						/*
						 * skipped files are not searchable or sent to friends
						 * 
						 * don't add files unless they are completed or
						 * downloading
						 */
						boolean includeFile = false;
						if (!torrentFile.isSkipped()) {
							if (completedOrDownloading) {
								includeFile = true;
							} else {
								if (torrentFile.getDownloaded() == torrentFile.getLength()) {
									includeFile = true;
								}
							}
						}
						if (includeFile) {
							subListSearchable.add(f);
						}

						/*
						 * add sha1 for sha1 search matching, slightly different
						 * if simple of non simple torrent
						 */
						if (sha1List != null && sha1List.length > i) {
							f.setSha1Hash(Base64.decode(sha1List[i]));
						}
						if (ed2kList != null && ed2kList.length > i) {
							f.setEd2kHash(Base64.decode(ed2kList[i]));
						}

						// end sha1 + ed2k hashes
					}

					if (subListSearchable.size() == 0) {
						logger.finest("no files completed or downloaded in torrent, skipping");
						continue;
					}

					byte[] infohash = t.getHash();

					String name = download.getDisplayName();
					final byte[] co = t.getComment();
					String comment = "";
					if (co != null) {
						comment = new String(co);
					}
					Category c = download.getDownloadState().getCategory();
					String category = "";
					if (c != null) {
						category = new String(category.getBytes());
					}
					String uniqueID = new String(Base64.encode(infohash));
					// System.out.println(uniqueID);
					logger.finest("creating FileCollection...");

					FileCollection allFilesCollection = new FileCollection(FileCollection.TYPE_BITTORRENT, uniqueID, name, comment, category, subListSearchable, download.getCreationTime());

					DownloadManager real_dl = AzureusCoreImpl.getSingleton().getGlobalManager().getDownloadManager(new HashWrapper(t.getHash()));
					if (real_dl == null) {
						continue;
					}

					if (real_dl.getDownloadState() != null) {
						Object album = real_dl.getDownloadState().getAttribute(FileCollection.ONESWARM_ALBUM_ATTRIBUTE);
						if (album != null) {
							if (album instanceof String) {
								allFilesCollection.setOptionalField(FileCollection.ONESWARM_ALBUM_ATTRIBUTE, (String) album);
								logger.finest("album info found, setting album to: " + album);
							}
						}
						Object artist = real_dl.getDownloadState().getAttribute(FileCollection.ONESWARM_ARTIST_ATTRIBUTE);
						if (artist != null) {
							if (artist instanceof String) {
								allFilesCollection.setOptionalField(FileCollection.ONESWARM_ARTIST_ATTRIBUTE, (String) artist);
								logger.finest("artist info found, setting artist to: " + artist);
							}
						}

						List<List<String>> tags = new LinkedList<List<String>>();
						for (String tagpath : real_dl.getDownloadState().getListAttribute(FileCollection.ONESWARM_TAGS_ATTRIBUTE)) {
							tags.add(Arrays.asList(tagpath.split("/")));
						}
						allFilesCollection.setDirectoryTags(tags);
						logger.finest("added " + tags.size() + " tags to " + allFilesCollection.getName());
					}
					if (!autoAdded) {
						allFiles.add(allFilesCollection);
					}
					logger.finest("done");

					if (permissionsManager.hasAllFriendsPermission(infohash) && !autoAdded) {
						searchableFiles.add(allFilesCollection);
						logger.finest("adding to searchable files: " + download.getDisplayName());
					}
					// add to the hash map so we can find the metainfohash of a
					// metainfohashhash

					long key = getInfoHashhash(infohash);
					hashhashToInfoHashMapping.put(key, infohash);
					hashhashToTorrentName.put(key, download.getDisplayName());
					if (!autoAdded) {
						includedInFileList.put(download, true);
					}
				} else {
					logger.finest("Not allowed: " + download.getDisplayName());
				}
			} catch (TOTorrentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		long generateComplete = System.currentTimeMillis();
		lastFileListRefreshMs = generateComplete - time;
		logger.info("created own file list: num swarms=" + allFiles.size() + " time=" + lastFileListRefreshMs);

		for (FileList friendsList : incomingFileLists.values()) {
			addInfoHashHashes(friendsList);
		}
		logger.fine("added friends files to hashhash mapping, time=" + (System.currentTimeMillis() - generateComplete));
		ownF2FFileList = new FileList(allFiles);
		searchableFileList = new FileList(searchableFiles);
		negativeHitCache = new NegativeHitCache();
	}

	public FileList getFileListToSendToFriend(Friend f) {
		/*
		 * makes the call block until the first file list is created
		 */
		waitForFileListCreation();

		if (!f.isCanSeeFileList()) {
			// is the friend can't see out list, just send an empty one
			return (new FileList());
		} else if (ownF2FFileList == null) {
			Debug.out("Tried to send file list to friend, but our file list is null");
			return (new FileList());
		} else {
			return (generateFileListForFriend(ownF2FFileList, f));
		}
	}

	public FileList getFriendsList(Friend f) {
		return incomingFileLists.get(f);
	}

	public byte[] getMetainfoHash(long metainfohashhash) {
		logger.fine("got request for: " + metainfohashhash);
		return hashhashToInfoHashMapping.get(metainfohashhash);
	}

	public MetaInfoManager getMetaInfoManager() {
		return metaInfoManager;
	}

	public FileList getOwnFileList() {
		/*
		 * makes the call block until the first file list is created
		 */
		waitForFileListCreation();
		return ownF2FFileList;

	}

	public String getTorrentNameFromInfoHashHash(long infoHashHash) {
		return hashhashToTorrentName.get(infoHashHash);
	}

	private long searchesTotal = 0;

	long getSearchesTotal() {
		return searchesTotal;
	}

	long getSearchCacheHits() {
		return searchCacheHits;
	}

	private long searchCacheHits = 0;

	public List<FileCollection> handleSearch(Friend f, String searchString) {
		searchesTotal++;
		/*
		 * start by checking the negative cache
		 */
		if (negativeHitCache.get(searchString) != null) {
			searchCacheHits++;
			return new LinkedList<FileCollection>();
		}

		FileList matches = searchableFileList.searchMatches(searchString);

		long matchingFiles = matches.getFileNum();
		if (matchingFiles == 0) {
			negativeHitCache.put(searchString, true);
			return matches.getElements();
		} else if (matchingFiles <= MAX_SEARCH_HITS) {
			return matches.getElements();
		} else {
			// TODO: do something smart here, current policy is:
			// prefer matches on the torrent name
			// if match on torrent name, select largest file in torrent that
			// matches search
			// if we have room left, just select largest file in matching
			// torrents
			// if still room left, add files that match

			// the idea is that we want to return as many unique torrents as
			// possible so we can rank by number of sources,
			// we might want to think about this some more...

			int added = 0;
			HashMap<String, FileCollection> selected = new HashMap<String, FileCollection>();

			ArrayList<FileCollection> shuffled = new ArrayList<FileCollection>(matches.getElements());

			ArrayList<FileCollection> torrentMatchNotFileMatch = new ArrayList<FileCollection>();
			ArrayList<FileCollection> torrentMatchAndFileMatch = new ArrayList<FileCollection>();
			ArrayList<FileCollection> fileMatchNotTorrentMatch = new ArrayList<FileCollection>();
			Collections.shuffle(shuffled);
			logger.fine("got text search, found matches: " + matches.getFileNum());
			// first, largest file that both matches the torrent name and the
			// filename
			logger.fine("adding largest file with match in torrent+file " + getFileNum(selected.values()));
			for (int i = 0; i < shuffled.size(); i++) {
				FileCollection collection = shuffled.get(i);
				if (collection.nameMatch(searchString)) {
					FileCollection largest = collection.getLargestFile(searchString);
					if (largest != null) {
						selected.put(largest.getUniqueID(), largest);
						added += largest.getFileNum();

						// save these for later
						FileCollection fileMatches = collection.fileMatches(searchString);
						// but remove this one so we don't add it again
						fileMatches.getChildren().remove(largest.getChildren().get(0));
						torrentMatchAndFileMatch.add(fileMatches);
					} else {
						torrentMatchNotFileMatch.add(collection);
					}

					if (added >= MAX_SEARCH_HITS) {
						List<FileCollection> s = new ArrayList<FileCollection>(selected.values());

						logger.fine("only room for largest file matching file in torrent+file match: " + added + "|" + getFileNum(selected.values()));
						return s;
					}
				} else {
					// matches file but not torrent, save for later
					fileMatchNotTorrentMatch.add(collection);
				}
			}

			logger.fine("adding largest file with match in torrent but not file" + getFileNum(selected.values()));
			// second, if we have a match in torrent name, but not in the file
			// name, just add the largest file from these
			for (int i = 0; i < torrentMatchNotFileMatch.size(); i++) {
				FileCollection largestFile = torrentMatchNotFileMatch.get(i).getLargestFile();
				selected.put(largestFile.getUniqueID(), largestFile);
				added += largestFile.getFileNum();
				if (added >= MAX_SEARCH_HITS) {
					List<FileCollection> s = new ArrayList<FileCollection>(selected.values());
					return s;
				}
			}
			logger.fine("adding files with match in torrent+file but not largest " + getFileNum(selected.values()));
			// third, add files that match on file name and torrent name but is
			// not the largest
			for (int i = 0; i < torrentMatchAndFileMatch.size(); i++) {
				FileCollection c = torrentMatchAndFileMatch.get(i);
				List<FileListFile> files = c.getChildren();

				FileCollection target = selected.get(c.getUniqueID());
				while (added < MAX_SEARCH_HITS && files.size() > 0) {
					target.getChildren().add(files.remove(0));
					added++;
				}

			}

			logger.fine("adding files with match in file: " + getFileNum(selected.values()));
			// third, add files that match on file name
			for (int i = 0; i < fileMatchNotTorrentMatch.size(); i++) {
				FileCollection c = fileMatchNotTorrentMatch.get(i);
				List<FileListFile> files = c.getChildren();

				// begin with the biggest one
				if (!selected.containsKey(c.getUniqueID())) {
					FileCollection largestFile = c.getLargestFile();
					selected.put(largestFile.getUniqueID(), largestFile);
					c.getChildren().remove(largestFile.getChildren().get(0));
					added++;
				}

				// then continue with the rest
				FileCollection target = selected.get(c.getUniqueID());
				while (added < MAX_SEARCH_HITS && files.size() > 0) {
					target.getChildren().add(files.remove(0));
					added++;
				}
			}

			logger.fine("returning matches: " + getFileNum(selected.values()));

			List<FileCollection> s = new ArrayList<FileCollection>(selected.values());
			return s;
		}
	}

	public List<byte[]> receivedFriendFileList(Friend f, int type, byte[] data, boolean use_extended_filelists) throws IOException {
		if (data != null && data.length != 0) {
			FileList friendsList = null;
			if (use_extended_filelists == false) {
				// System.out.println("decoding basic");
				friendsList = FileListManager.decode_basic(data);
			} else {
				// System.out.println("decoding extended");
				friendsList = FileListManager.decode_extended(data);
			}
			return receivedFriendFileList(f, type, friendsList);
		}
		return new LinkedList<byte[]>();
	}

	public List<byte[]> receivedFriendFileList(Friend f, int type, FileList friendsList) {
		incomingFileLists.put(f, friendsList);
		return addInfoHashHashes(friendsList);
	}

	private List<byte[]> addInfoHashHashes(FileList friendsList) {
		List<byte[]> newInfoHashes = new LinkedList<byte[]>();
		// check what's new here
		for (FileCollection torrent : friendsList.getElements()) {
			byte[] infohash = Base64.decode(torrent.getUniqueID());
			long infoHashhash = getInfoHashhash(infohash);
			if (ownF2FFileList == null) {
				newInfoHashes.add(infohash);
			} else if (!ownF2FFileList.contains(infohash)) {
				newInfoHashes.add(infohash);
			}
			// and add the torrent name to the dictionary
			if (!hashhashToTorrentName.containsKey(infoHashhash)) {
				hashhashToTorrentName.put(infoHashhash, torrent.getName());
			}
			if (!hashhashToInfoHashMapping.containsKey(infoHashhash)) {
				hashhashToInfoHashMapping.put(infoHashhash, infohash);
			}
		}

		return newInfoHashes;
	}

	private void refresh() {
		logger.fine("Refreshing file list");
		long t = System.currentTimeMillis();
		boolean releaseFileListLock = ownF2FFileList == null;

		logger.finer("Refreshing own lists...");
		generateOwnLists();
		logger.fine("Refreshing file list took " + (System.currentTimeMillis() - t) + " ms");

		if (releaseFileListLock) {
			initialFileListSemaphore.release();
		}

		// check if the overlayManager is available
		final OverlayManager overlayManager = OSF2FMain.getSingelton().getOverlayManager();
		if (overlayManager == null) {
			return;
		}

		// check when we sent our list to friends last
		long timeSinceLast = System.currentTimeMillis() - lastTimeFileListSentToFriends;

		if (timeSinceLast > MAX_SEND_FILE_LIST_RATE) {
			logger.fine("sending file list to friends");
			overlayManager.triggerFileListUpdates();
			lastTimeFileListSentToFriends = System.currentTimeMillis();
		} else {
			// ok, we already did this pretty recently
			// check for scheduled updates
			if (updateRateLimiter != null) {
				logger.fine("not sending file list to friends, an update is already scheduled");
			} else {
				// schedule an update
				logger.fine("scheduling file list update");
				updateRateLimiter = new Timer("FileListUpdateScheduler", true);
				updateRateLimiter.schedule(new TimerTask() {
					@Override
					public void run() {
						logger.fine("sending file list to friends");
						lastTimeFileListSentToFriends = System.currentTimeMillis();
						overlayManager.triggerFileListUpdates();
						updateRateLimiter.cancel();
						updateRateLimiter = null;
					}
				}, MAX_SEND_FILE_LIST_RATE);
			}
		}
		logger.fine("Refreshing file done");
	}

	public void scheduleFileListRefresh() {

		logger.fine("Scheduling file list refresh");
		refreshRateLimiter.schedule();
	}

	public void waitForFileListCreation() {
		if (ownF2FFileList == null) {
			try {
				if (!initialFileListSemaphore.tryAcquire()) {
					logger.fine("waiting for file list to get created");
					initialFileListSemaphore.acquire();
				}

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			initialFileListSemaphore.release();
		}
	}

	public static boolean completedOrDownloading(DownloadManager dm) {
		if (dm.getState() == DownloadManager.STATE_ERROR) {
			return false;
		}

		/*
		 * we respond to searches if we either are completed
		 */
		boolean completed = dm.getAssumedComplete();
		if (completed) {
			return true;
		}
		/*
		 * or that are downloading at >1 KB/s and there are seeds
		 */
		DownloadManagerStats dmStats = dm.getStats();
		if (dmStats != null && dmStats.getDataReceiveRate() > 1 && dm.getNbSeeds() > 0) {
			return true;
		}

		return false;
	}

	public static FileList decode_basic(byte[] data) throws IOException {
		return decode(data, false);
	}

	public static FileList decode_extended(byte[] data) throws IOException {
		return decode(data, true);
	}

	static FileList decode(byte[] data, boolean include_extended_info) throws IOException {
		if (data == null || data.length < 1) {
			return new FileList();
		}
		long time = System.currentTimeMillis();
		boolean gzip = data[0] == 1;
		try {
			ByteArrayInputStream bin = new ByteArrayInputStream(data, 1, data.length - 1);
			DataInputStream in;
			if (gzip) {
				in = new DataInputStream(new GZIPInputStream(bin));
			} else {
				in = new DataInputStream(bin);
			}
			// TODO: further sanity checks for value
			int numCollections = in.readInt();
			if (numCollections < 0) {
				throw new IOException("Number of collections must be positive.");
			}

			List<FileCollection> collections = new LinkedList<FileCollection>();
			for (int i = 0; i < numCollections; i++) {
				collections.add(readCollection(in, include_extended_info));
			}
			in.close();
			FileList list = new FileList(collections);

			logger.fine("decoded " + (include_extended_info ? "extended" : "") + " file list, gzip=" + gzip + " num swarms=" + list.getElements().size() + " time=" + (System.currentTimeMillis() - time));

			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new IOException("filelist decode error");
		}
	}

	public static byte[] encode_basic(FileList list, boolean gzip) {
		return encode(list, gzip, false);
	}

	public static byte[] encode_extended(FileList list, boolean gzip) {
		return encode(list, gzip, true);
	}

	static byte[] encode(FileList list, boolean gzip, boolean include_extended_info) {
		// format:
		// 1 byte flags, bit 0 set means gzipped
		// 4 bytes (java int): number of collections
		// collections[]
		// collection format:
		// 1 byte type
		// 20 bytes (hash): collection id
		// 2 bytes (java short): collection name len
		// x bytes collection UTF-8
		// 2 bytes (java short):description length
		// x bytes description UTF-8 encoded
		// 8 bytes (java long) date added
		// 4 bytes (java int): number of files
		// OPTIONAL -- support for extended tags (see writeCollection for info)
		// files[]
		// files format:
		// 8 bytes file size (java long)
		// 2 bytes filename length (java short)
		// x bytes UTF8 encoded file name

		try {
			if (list == null) {
				list = new FileList();
			}

			long time = System.currentTimeMillis();
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out;

			if (gzip) {
				out = new DataOutputStream(new GZIPOutputStream(buf));
			} else {
				out = new DataOutputStream(buf);
			}
			if (list.getElements() != null) {
				int numCollections = list.getElements().size();
				out.writeInt(numCollections);
				for (FileCollection c : list.getElements()) {
					writeCollection(out, c, include_extended_info);
				}
			} else {
				out.writeInt(0);
			}

			out.close();
			buf.close();
			byte[] byteArray = buf.toByteArray();
			byte[] ret = new byte[byteArray.length + 1];
			if (gzip) {
				ret[0] = (byte) 1;
			} else {
				ret[0] = (byte) 0;
			}
			System.arraycopy(byteArray, 0, ret, 1, byteArray.length);
			logger.fine("encoded " + (include_extended_info ? "extended " : "") + "file list, gzip=" + gzip + " num swarms=" + list.getElements().size() + " time=" + (System.currentTimeMillis() - time));
			return ret;
		} catch (IOException e) {
			Debug.out("error encoding file list", e);
		}
		return null;
	}

	private static int getFileNum(Collection<FileCollection> list) {
		int num = 0;
		for (FileCollection f : list) {
			num += f.getFileNum();
		}
		return num;
	}

	public long getInfoHashhash(byte[] infohash) {

		ByteArrayInputStream b = new ByteArrayInputStream(infohash);
		DataInputStream i = new DataInputStream(b);
		long val = 0;
		try {
			val = i.readLong();
		} catch (IOException e) {
			Debug.out("error when getting infohashhash", e);
		}
		/*
		 * add the info hash to the map, just in case we get responses before
		 * the file list had time to refresh
		 */
		hashhashToInfoHashMapping.put(val, infohash);

		// System.out.println("hashhash: " + val);
		return val;

	}

	public static void main(String[] args) {
		try {
			// List<FileListFile> files = new LinkedList<FileListFile>();
			// for (int i = 0; i < 17; i++) {
			// files.add(new FileListFile("test file " + i, 1000 * i));
			// }
			// FileCollection testCollection = new FileCollection((byte) 0, new
			// String(Base64.encode(new byte[20])),
			// "test collection with 17 files",
			// "this is a long comment.... but it is worth it", "", files,
			// System.currentTimeMillis());
			//
			// List<FileCollection> c = new LinkedList<FileCollection>();
			// c.add(testCollection);
			//
			// List<FileListFile> files2 = new LinkedList<FileListFile>();
			// files.add(new FileListFile("TestFile.tar", 1024 * 1024 * 1024));
			//
			// FileCollection testCollection2 = new FileCollection((byte) 0, new
			// String(Base64.encode(new byte[20])), "test2", "", "", files2,
			// System.currentTimeMillis());
			// c.add(testCollection2);
			// FileList f = new FileList(c);
			// byte[] bytes = encode_basic(f, false);
			//
			// FileList f2 = decode_basic(bytes);
			// System.out.println("f2=" + f2.getListId() + " f1=" +
			// f.getListId() + " equals=" + (f2.getListId() == f.getListId()));
			//
			// FileList f3 = new FileList(new LinkedList<FileCollection>());
			// bytes = encode_basic(f3, true);
			// FileList f4 = decode_basic(bytes);
			// System.out.println("empty equals:" + (f3.getListId() ==
			// f4.getListId()));
			//
			// FileList f5 = new FileList();
			// bytes = encode_basic(f5, true);
			// FileList f6 = decode_basic(bytes);

			// RandomAccessFile foo = new RandomAccessFile("/tmp/foo", "r");
			// byte [] b = new byte[(int)foo.length()];
			byte[] b = ByteFormatter
					.decodeString("011F8B0800000000000000636060E064F0EFBAA2F9D839A3AAEE9BF3A1AB7D719CBAA25F8319244B933293F3730B14D28B52134B528B4B1432324B8AF5720B8C19808051FEAFDC0B661003C46350B8E18F5FC383E025B52BE635E69D76939D92FD725984E94B333F06FEA0CC94F4D4A0D2BCBCD422BDDCFC32B0C10A52DD3BD3E10637AF5C804D9999DA058FCF3E176C3C39D42FDEEEBCC6B1ACE4A300039BA15E56416A3ACC94BDF3E0A6D8331E83CB3202214B41667231C3EF3F321B0C2689CE8F0CBE395DB9E9DD3607C7536B19D88C500C3932136E888D633D5C166148F36D368589B34D1D82D7F11928FBF31C373966F88A81CD18C590135170431CD609C0651186287E499B9EE07B99BBC6A0AFF9DAFA5FD511A745B731B099A018722E1C6E885DAB045C1661C8FF6D93C3A79D8B556078E81E5314F9CA668BB90D03038FB3A7AFBBA1B191919E639827D4A81E164D98518C374EBD4053C3C8C0C4C0060CE5CCD4620696E2BCFC7206EE1F07673EBD73C9E94A9CA7418B90CC3A9EE77F1A1984F3F3528BCB138B72E333521353801193550075AA767A2DDC7C06C6DED9D89522DC1DF4F367D11DBEDF176ECCAE927CE75152B125467F2B834C4946AA6E4146664E7E717E41466A51B16E416251624A7E855E7A661AD49E02FD1AB83D0C3AA204F430C12D64646005D14600BF52B6C6FC020000");
			// foo.read(b);
			FileList fl = FileListManager.decode_extended(b);
			System.out.println(fl.getFileNum() + " files read");
			System.out.println(fl);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static FileCollection readCollection(DataInputStream in, boolean include_extended_info) throws IOException {
		byte type = in.readByte();
		byte[] uniqueId = new byte[20];
		in.readFully(uniqueId);
		String name = readString(in);
		String desc = readString(in);
		long dataAdded = in.readLong();
		int numFiles = in.readInt();
		List<FileListFile> files = new LinkedList<FileListFile>();
		for (int i = 0; i < numFiles; i++) {
			files.add(readFile(in));
		}

		FileCollection coll = new FileCollection(type, new String(Base64.encode(uniqueId)), name, desc, "", files, dataAdded);

		/**
		 * Directory information. Format is: 2 byte number of directory entries.
		 * Per directory entry: 2 byte number of entries in path String-encoded
		 * (using read/writeString()) path entries)
		 */
		if (include_extended_info) {
			short howmany = in.readShort();
			List<List<String>> all_tags = new LinkedList<List<String>>();
			for (short i = 0; i < howmany; i++) {
				short entries = in.readShort();
				List<String> this_tag = new LinkedList<String>();
				for (short eItr = 0; eItr < entries; eItr++) {
					this_tag.add(readString(in));
				}
				all_tags.add(this_tag);
			}
			coll.setDirectoryTags(all_tags);
		}

		return coll;
	}

	private static FileListFile readFile(DataInputStream in) throws IOException {
		long fileSize = in.readLong();
		String str = readString(in);
		return new FileListFile(str, fileSize);
	}

	private static String readString(DataInputStream in) throws IOException {
		short encodedStringLength = in.readShort();
		byte[] encodedString = new byte[encodedStringLength];
		in.readFully(encodedString);
		String str = new String(encodedString, "UTF-8");
		return str;
	}

	private static void writeCollection(DataOutputStream out, FileCollection c, boolean include_extended_info) throws IOException {
		if (c.getUniqueIdBytes().length != 20) {
			throw new IOException("File collections unique id must be 20 bytes");
		}
		out.write(c.getType());
		out.write(c.getUniqueIdBytes());
		writeString(out, c.getName());
		writeString(out, c.getDescription());
		out.writeLong(c.getAddedTimeUTC());
		int fileNum = c.getChildren().size();
		out.writeInt(fileNum);
		for (FileListFile file : c.getChildren()) {
			writeFile(out, file);
		}

		if (include_extended_info) {
			out.writeShort(c.getDirectoryTags().size());
			for (List<String> tag : c.getDirectoryTags()) {
				out.writeShort((short) tag.size());
				for (String entry : tag) {
					writeString(out, entry);
				}
			}
		}
	}

	private static void writeFile(DataOutputStream out, FileListFile file) throws IOException {
		long fileSize = file.getLength();
		out.writeLong(fileSize);
		writeString(out, file.getFileName());
	}

	private static void writeString(DataOutputStream out, String str) throws IOException {
		byte[] encodedFileName = str.getBytes("UTF-8");
		short encodedLength = (short) encodedFileName.length;
		out.writeShort(encodedLength);
		out.write(encodedFileName, 0, encodedLength);
	}

	private final class FileListRefresher extends Thread {
		private volatile boolean doRefresh = false;

		private long lastRefreshCompleted = 0;
		private volatile long lastRefreshRequested = 0;

		private FileListRefresher(String name) {
			super(name);
		}

		private boolean delayDueToRateLimit() {
			/*
			 * we are skipping the refresh this time if we completed a refresh
			 * very recently
			 */
			long timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshCompleted;
			if (timeSinceLastRefresh < Math.max(MAX_FILE_LIST_REFRESH_RATE, lastFileListRefreshMs * 5)) {
				logger.finest("skipping file list refresh, last refresh was " + timeSinceLastRefresh + " ms ago");
				return true;
			}
			/*
			 * or if the requests for refreshing are too frequent
			 */
			long timeSinceLastRequest = System.currentTimeMillis() - lastRefreshRequested;
			if (timeSinceLastRequest < MAX_FILE_LIST_REFRESH_RATE) {
				// check, if we did a refresh kinda recently we should wait with
				// this one
				if (timeSinceLastRefresh < 10 * MAX_FILE_LIST_REFRESH_RATE) {
					logger.finest("skipping file list refresh, last request was " + timeSinceLastRequest + " ms ago, refresh: " + timeSinceLastRefresh + " ms ago");
					return true;
				}
			}
			return false;
		}

		public void schedule() {
			doRefresh = true;
			lastRefreshRequested = System.currentTimeMillis();
		}

		public void run() {
			try {
				while (true) {
					if (doRefresh) {
						if (!delayDueToRateLimit()) {
							logger.finer("refresh rate limiter triggered");

							doRefresh = false;
							try {
								refresh();
								lastRefreshCompleted = System.currentTimeMillis();
							} catch (Exception e) {
								e.printStackTrace();

							}
						} else {
							logger.finer("file refresh delayed 500 ms due to rate limit");
						}
					}

					Thread.sleep(MAX_FILE_LIST_REFRESH_RATE);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isInitialFileListGenerated() {
		return ownF2FFileList != null;
	}

	private class NegativeHitCache extends LinkedHashMap<String, Boolean> {
		public final static int MAX_SIZE = 500;
		private static final long serialVersionUID = 1L;

		public NegativeHitCache() {
			super(MAX_SIZE, 0.75f, true);
		}

		protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
			return size() > MAX_SIZE;
		}
	}
}
