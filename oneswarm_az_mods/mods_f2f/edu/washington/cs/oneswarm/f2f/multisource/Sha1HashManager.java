package edu.washington.cs.oneswarm.f2f.multisource;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.HashWrapper;

import edu.washington.cs.oneswarm.f2f.FileListFile;

public class Sha1HashManager
{
	private static Sha1HashManager	 instance								= new Sha1HashManager();

	private final static Logger			logger									= Logger.getLogger(Sha1HashManager.class.getName());

	public static final String			 OS_HASHES_ADDED				 = "os_hashes";

	public static final String			 OS_HASHES_TYPE_LOCAL		= "local";

	public static final String			 OS_HASHES_TYPE_REMOTE	 = "remote";

	public static final String			 OS_HASHES_TYPE_TORRENT	= "torrent";

	private Sha1Calculator					 calc										= new Sha1Calculator();

	private DownloadManagerListener	downloadManagerListener = new CompletionListener();

	private Set<Sha1HashJobListener> jobListeners						= new HashSet<Sha1HashJobListener>();

	private Set<HashWrapper>				 submittedJobs					 = new HashSet<HashWrapper>();

	private Timer										initialDelayTimer			 = new Timer(
																															 "sha1 calc delay",
																															 true);

	private Sha1HashManager() {
	}

	public void addJobListener(Sha1HashJobListener listener) {
		synchronized (this) {
			jobListeners.add(listener);
		}
	}

	void downloadManagerAdded(final DownloadManager dm, boolean addListener) {
		/*
		 * check if the download state has the sha1 hashes
		 */
		DownloadManagerState ds = dm.getDownloadState();
		String hashedAdded = ds.getAttribute(OS_HASHES_ADDED);
		if (hashedAdded == null) {
			logger.finer("no hashes in dm: " + dm.getDisplayName());
			/*
			 * no hashes added, add them from the .torrent file
			 */
			try {
				List<String> sha1s = getHashesFromTorrent(dm.getTorrent(),
						FileListFile.KEY_SHA1_HASH);
				if (sha1s.size() > 0) {
					logger.finest("adding hashes from torrent");
					List<String> ed2ks = getHashesFromTorrent(dm.getTorrent(),
							FileListFile.KEY_ED2K_HASH);
					ds.setListAttribute(FileListFile.KEY_SHA1_HASH,
							sha1s.toArray(new String[sha1s.size()]));
					ds.setListAttribute(FileListFile.KEY_ED2K_HASH,
							ed2ks.toArray(new String[ed2ks.size()]));
					ds.setAttribute(OS_HASHES_ADDED, OS_HASHES_TYPE_TORRENT);
				} else {
					// the user might have added them after
					String[] remoteSha1s = ds.getListAttribute(FileListFile.KEY_SHA1_HASH);
					String[] remoteED2Ks = ds.getListAttribute(FileListFile.KEY_ED2K_HASH);
					if (remoteSha1s != null && remoteSha1s.length > 0
							&& remoteED2Ks != null && remoteED2Ks.length > 0) {
						ds.setAttribute(OS_HASHES_ADDED, OS_HASHES_TYPE_REMOTE);
						logger.finer("got remote hashes");
					} else {
						logger.finest("no remote hashes");
					}

					if (dm.isDownloadComplete(true)) {
						if (hashedAdded == null
								|| hashedAdded.equals(OS_HASHES_TYPE_REMOTE)) {
							// need to calc hashes but wait until after 2 minutes to not slow down initial startup
							logger.finest("need to calcluate hashes, hash job queued");
							initialDelayTimer.schedule(new TimerTask() {
								@Override
								public void run() {
									try {
										queueHashCalc(dm);
									} catch (TOTorrentException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}, 2 * 60 * 1000);
						}
					} else {
						// if the download is running, add a listener
						if (addListener) {
							dm.addListener(downloadManagerListener);
						}
					}
				}
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (hashedAdded.equals(OS_HASHES_TYPE_REMOTE)) {
			String[] remoteSha1s = ds.getListAttribute(FileListFile.KEY_SHA1_HASH);
			if (remoteSha1s.length == 0) {
				ds.setAttribute(OS_HASHES_ADDED, null);
			}
		}
	}

	private void queueHashCalc(final DownloadManager dm)
			throws TOTorrentException {
		HashWrapper torrentHash = new HashWrapper(dm.getTorrent().getHash());
		synchronized (this) {
			if (!submittedJobs.contains(torrentHash)) {
				submittedJobs.add(torrentHash);
				List<Sha1CalcListener> listeners = new LinkedList<Sha1CalcListener>();
				listeners.add(new Sha1CalcListener() {
					public void completed(Sha1Result result) {
						logger.finer("hashing completed for: " + dm.getDisplayName());
						final DownloadManagerState ds = dm.getDownloadState();
						String[] sha1s = new String[result.sha1Values.size()];
						String[] ed2ks = new String[result.ed2kValues.size()];
						for (int i = 0; i < sha1s.length; i++) {
							sha1s[i] = new String(Base64.encode(result.sha1Values.get(i)));
							ed2ks[i] = new String(Base64.encode(result.ed2kValues.get(i)));
							logger.finest("hash for file " + i + ": sha1=" + sha1s[i]
									+ " ed2k=" + ed2ks[i]);
						}
						ds.setListAttribute(FileListFile.KEY_SHA1_HASH, sha1s);
						ds.setListAttribute(FileListFile.KEY_ED2K_HASH, ed2ks);
						ds.setAttribute(OS_HASHES_ADDED, OS_HASHES_TYPE_LOCAL);
						logger.fine("added hashes for: " + dm.getDisplayName());
					}

					public void errorOccured(Throwable cause) {
						cause.printStackTrace();
					}

					public void progress(double fraction) {
					}
				});
				for (Sha1HashJobListener l : jobListeners) {
					Sha1CalcListener cl = l.jobAdded(dm.getDisplayName());
					if (cl != null) {
						listeners.add(cl);
					}
				}
				logger.fine("submitting hash job: " + dm.getDisplayName());
				calc.getHashesFromDownload(dm, listeners);
			}
		}
	}

	public void stop() {
		calc.stop();
	}

	@SuppressWarnings("unchecked")
	private static List<String> getHashesFromTorrent(TOTorrent torrent,
			String type) throws TOTorrentException {
		List<String> sha1Found = new LinkedList<String>();

		if(torrent == null){
			Debug.out("torrent is null!");
			return sha1Found;
		}

		Map torrentMap = (Map) torrent.serialiseToMap().get("info");

		if (torrent.isSimpleTorrent()) {
			String torrentSha1 = getHashFromTorrentMap(torrentMap, type);
			if (torrentSha1 != null) {
				sha1Found.add(torrentSha1);
			}
		} else {
			Object filePropertiesObj = torrentMap.get("files");
			if (filePropertiesObj == null || !(filePropertiesObj instanceof List)) {
				return sha1Found;
			}

			List fileProperties = (List) filePropertiesObj;
			if (fileProperties != null) {
				for (Object pObj : fileProperties) {
					if (pObj != null && pObj instanceof Map) {
						Map pMap = (Map) pObj;
						String torrentSha1 = getHashFromTorrentMap(pMap, type);
						if (torrentSha1 != null) {
							sha1Found.add(torrentSha1);
						}
					}
				}
			}
		}
		return sha1Found;
	}

	@SuppressWarnings("unchecked")
	private static String getHashFromTorrentMap(Map torrentMap, String type) {
		if (torrentMap.containsKey(type)) {
			return new String(Base64.encode((byte[]) torrentMap.get(type)));
		} else {
			return null;
		}
	}

	public static Sha1HashManager getInstance() {
		return instance;
	}

	private class CompletionListener
		implements DownloadManagerListener
	{
		public void completionChanged(DownloadManager manager, boolean bCompleted) {
		}

		public void downloadComplete(DownloadManager dm) {
			// each time a download completes, check if we need to add the sha1 hashes (but only when all files are available)
			logger.fine("download completed: " + dm.getDisplayName());
			try {
				synchronized (Sha1HashManager.this) {
					if (dm.isDownloadComplete(true)) {
						DownloadManagerState ds = dm.getDownloadState();
						String hashedAdded = ds.getAttribute(OS_HASHES_ADDED);
						if (hashedAdded == null
								|| hashedAdded.equals(OS_HASHES_TYPE_REMOTE)) {
							// need to calc hashes
							queueHashCalc(dm);
						} else {
							logger.fine("hashes already calculated: " + dm.getDisplayName());
						}
					} else {
						logger.fine("skipping hash calc: " + dm.getDisplayName());
					}
				}
			} catch (TOTorrentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void filePriorityChanged(DownloadManager download,
				DiskManagerFileInfo file) {
		}

		public void positionChanged(DownloadManager download, int oldPosition,
				int newPosition) {
		}

		public void stateChanged(DownloadManager manager, int state) {
		}
	}

	public interface Sha1CalcListener
	{
		public void completed(Sha1Result result);

		public void errorOccured(Throwable cause);

		public void progress(double fraction);
	}

	public interface Sha1HashJobListener
	{
		public Sha1CalcListener jobAdded(String name);
	}

	public static class Sha1Result
	{
		private final List<byte[]> ed2kValues;

		private final List<byte[]> sha1Values;

		Sha1Result(List<byte[]> sha1Values, List<byte[]> ed2kValues) {
			this.sha1Values = sha1Values;
			this.ed2kValues = ed2kValues;
		}

		public List<byte[]> getEd2kValues() {
			return ed2kValues;
		}

		public List<byte[]> getSha1Values() {
			return sha1Values;
		}
	}

}
