package edu.washington.cs.oneswarm.f2f.multisource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Base64;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.HashWrapper;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.impl.AzureusCoreImpl;

import edu.washington.cs.oneswarm.f2f.FileListFile;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1HashManager.Sha1HashJobListener;
import edu.washington.cs.oneswarm.f2f.multisource.Sha1PieceRequestTranslator.PieceTranslationExcetion;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter;
import edu.washington.cs.oneswarm.f2f.share.DownloadManagerStarter.DownloadManagerStartListener;

public class Sha1DownloadManager
{
	public static final String	ONESWARM_AUTO_ADDED							 = "oneswarm.auto.added";

	public static String				MULTI_TORRENT_SOURCE_DOWNLOAD_DIR = "oneswarm.multi.torrent.download.temp.dir";

	private final static Logger logger														= Logger.getLogger(Sha1DownloadManager.class.getName());

	private final AzureusCore	 core;

	private Set<Sha1Peer>			 currentPeers											= new HashSet<Sha1Peer>();

	private Sha1HashManager		 sha1HashManager									 = Sha1HashManager.getInstance();

	public Sha1DownloadManager() {
		core = AzureusCoreImpl.getSingleton();
		core.getGlobalManager().addListener(new GlobalManagerListener() {
			public void seedingStatusChanged(boolean seedingOnlyMode) {
			}

			public void downloadManagerRemoved(DownloadManager dm) {
			}

			public void downloadManagerAdded(DownloadManager dm) {
				// first tell the hash manager that we got a new download manager
				sha1HashManager.downloadManagerAdded(dm, true);

				// basically we want to check if we need to add peers each time
				// a download starts
				dm.addListener(new DownloadManagerListener() {
					public void stateChanged(DownloadManager manager, int state) {
						checkForNewSha1Matches(manager);
					}

					private void checkForNewSha1Matches(DownloadManager manager) {
						if (manager.getState() == DownloadManager.STATE_DOWNLOADING) {
							logger.fine("download manager state changed, checking for sha1 matches: "
									+ manager.getDisplayName());
							try {
								downloadManagerDownloading(manager);
							} catch (TOTorrentException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					public void positionChanged(DownloadManager download,
							int oldPosition, int newPosition) {
					}

					public void filePriorityChanged(DownloadManager download,
							DiskManagerFileInfo file) {
						checkForNewSha1Matches(download);
					}

					public void downloadComplete(DownloadManager manager) {
					}

					public void completionChanged(DownloadManager manager,
							boolean bCompleted) {
					}
				});
			}

			public void destroyed() {
			}

			public void destroyInitiated() {
			}
		});
	}

	public void addHashJobListener(Sha1HashJobListener l) {
		sha1HashManager.addJobListener(l);
	}

	public static File getMultiTorrentDownloadDir() throws IOException {
		String tempDir = COConfigurationManager.getStringParameter(MULTI_TORRENT_SOURCE_DOWNLOAD_DIR);
		if (tempDir == null || tempDir.length() == 0) {
			String downloadDir = COConfigurationManager.getStringParameter("Default save path");
			File tempDirFile = new File(downloadDir, "additional_source_temp");
			tempDir = tempDirFile.getCanonicalPath();
		}
		File downloadDir = new File(tempDir);
		if (!downloadDir.isDirectory()) {
			downloadDir.mkdirs();
		}
		if (!downloadDir.isDirectory()) {
			String msg = "unable to create multi_torrent source temp dir: '"
					+ tempDir + "'";
			logger.warning(msg);
			throw new IOException(msg);
		}
		return downloadDir;
	}

	@SuppressWarnings("unchecked")
	private void downloadManagerDownloading(final DownloadManager download)
			throws TOTorrentException {
		if (download.isDownloadComplete(false)) {
			logger.fine("skipping download manager, already completed...");
			return;
		}
		List<DownloadManager> downloadManagers = core.getGlobalManager().getDownloadManagers();

		// create a library of sha1->download manager mappings
		logger.finer("creating sha1 map");
		HashMap<HashWrapper, List<DownloadManager>> sha1Mappings = new HashMap<HashWrapper, List<DownloadManager>>();
		for (DownloadManager d : downloadManagers) {
			if (!download.equals(d)) {
				HashWrapper[] sha1Found = getHashesFromDownload(d,
						FileListFile.KEY_SHA1_HASH, false);
				/*
				 * add everything we found
				 */
				for (int i = 0; i < sha1Found.length; i++) {
					HashWrapper sha1 = sha1Found[i];
					if (sha1 == null) {
						continue;
					}
					if (!sha1Mappings.containsKey(sha1)) {
						sha1Mappings.put(sha1, new LinkedList<DownloadManager>());
					}
					sha1Mappings.get(sha1).add(d);
				}
			}
		}

		/*
		 * now, scan all files in the current torrent and check for matches
		 */
		logger.finest("scanning for matches");
		HashWrapper[] torrentSha1s = getHashesFromDownload(download,
				FileListFile.KEY_SHA1_HASH, false);
		final HashSet<DownloadManager> interestingDms = new HashSet<DownloadManager>();
		for (int i = 0; i < torrentSha1s.length; i++) {
			HashWrapper t = torrentSha1s[i];
			if (t == null) {
				continue;
			}
			List<DownloadManager> matchingDms = sha1Mappings.get(t);
			if (matchingDms != null) {
				interestingDms.addAll(matchingDms);
			}
		}

		/*
		 * last, we have a set of interesting dms,
		 */
		logger.fine("found " + interestingDms.size()
				+ " interesting download managers");
		DownloadManagerStarter.startDownload(download,
				new DownloadManagerStartListener() {
					public void downloadStarted() {
						for (final DownloadManager srcDm : interestingDms) {
							DownloadManagerStarter.startDownload(srcDm,
									new DownloadManagerStartListener() {
										public void downloadStarted() {
											addPeer(srcDm, download);
											addPeer(download, srcDm);
										}
									});
						}
					}
				});

	}	

	static HashWrapper[] getHashesFromDownload(DownloadManager d, String type,
			boolean excludeSkipped) {
		String hashesAdded = d.getDownloadState().getAttribute(
				Sha1HashManager.OS_HASHES_ADDED);
		if (hashesAdded == null) {
			return new HashWrapper[0];
		}

		DiskManagerFileInfo[] files = d.getDiskManagerFileInfo();
		HashWrapper[] hashes = new HashWrapper[files.length];
		String[] base64Hashes = d.getDownloadState().getListAttribute(type);
		for (int i = 0; i < base64Hashes.length; i++) {
			if (base64Hashes[i] == null) {
				continue;
			}
			if (excludeSkipped && files[i].isSkipped()) {
				continue;
			}
			hashes[i] = new HashWrapper(Base64.decode(base64Hashes[i]));
		}
		return hashes;
	}

	private void addPeer(DownloadManager source, final DownloadManager destination) {
		if (destination.isDownloadComplete(false)) {
			return;
		}
		boolean add = false;
		try {
			Sha1Peer peer = new Sha1Peer(this, source, destination);

			synchronized (currentPeers) {
				logger.finer("got request to add peer, currently active="
						+ currentPeers.size());
				if (!currentPeers.contains(peer)) {
					currentPeers.add(peer);
					add = true;
				}
			}
			if (add) {
				logger.finer("adding peer");
				destination.getPeerManager().addPeer(peer);
			} else {
				logger.finer("not adding peer (already exists)");
			}
		} catch (PieceTranslationExcetion e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void peerClosed(Sha1Peer sha1Peer) {
		synchronized (currentPeers) {
			currentPeers.remove(sha1Peer);
		}

	}

}
